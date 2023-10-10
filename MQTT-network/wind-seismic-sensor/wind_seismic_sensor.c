#include "contiki.h"
#include "net/routing/routing.h"
#include "mqtt.h"
#include "net/ipv6/uip.h"
#include "net/ipv6/uip-icmp6.h"
#include "net/ipv6/sicslowpan.h"
#include "sys/etimer.h"
#include "sys/ctimer.h"
#include "lib/sensors.h"
#include "dev/button-hal.h"
#include "dev/leds.h"
#include "os/sys/log.h"
#include "mqtt-client.h"
#include <sys/node-id.h>
#include "../../util/util.c"

#include <string.h>
#include <strings.h>
#include <math.h>
/*---------------------------------------------------------------------------*/
#define LOG_MODULE "wind-sensor"
#ifdef MQTT_CLIENT_CONF_LOG_LEVEL
#define LOG_LEVEL MQTT_CLIENT_CONF_LOG_LEVEL
#else
#define LOG_LEVEL LOG_LEVEL_DBG
#endif


/*---------------------------------------------------------------------------*/
/* MQTT broker address. */
#define MQTT_CLIENT_BROKER_IP_ADDR "fd00::1"

static const char *broker_ip = MQTT_CLIENT_BROKER_IP_ADDR;

// Defaukt config values
#define DEFAULT_BROKER_PORT         1883
#define DEFAULT_PUBLISH_INTERVAL	(3 * CLOCK_SECOND)
#define DEFAULT_PUBLISH_INTERVAL_WIND    (4 * CLOCK_SECOND) // TODO settare al publish interval che scegliamo
#define INTERVAL_TO_THE_SECOND_PUBLISH	(1 * CLOCK_SECOND)

// We assume that the broker does not require authentication

/*---------------------------------------------------------------------------*/
/* Various states */
static uint8_t state;

#define STATE_INIT    		  0
#define STATE_NET_OK    	  1
#define STATE_CONNECTING      2
#define STATE_CONNECTED       3
#define STATE_SUBSCRIBED      4
#define STATE_DISCONNECTED    5

/*---------------------------------------------------------------------------*/
PROCESS_NAME(wind_seismic_sensor_process);
AUTOSTART_PROCESSES(&wind_seismic_sensor_process);

/*---------------------------------------------------------------------------*/
/* Maximum TCP segment size for outgoing segments of our socket */
#define MAX_TCP_SEGMENT_SIZE    32
#define CONFIG_IP_ADDR_STR_LEN   64
/*---------------------------------------------------------------------------*/
/*
 * Buffers for Client ID and Topics.
 * Make sure they are large enough to hold the entire respective string
 */
#define BUFFER_SIZE 64

static char client_id[BUFFER_SIZE];
static char pub_topic[BUFFER_SIZE];
static char sub_topic_1[BUFFER_SIZE]; 
static char sub_topic_2[BUFFER_SIZE]; 

// Periodic timer to check the state of the MQTT client
#define STATE_MACHINE_PERIODIC     (CLOCK_SECOND >> 1)
static struct etimer periodic_timer_wind;
static struct etimer periodic_timer_seismic;
static struct etimer periodic_timer;

/*---------------------------------------------------------------------------*/
/*
 * The main MQTT buffers.
 * We will need to increase if we start publishing more data.
 */
#define APP_BUFFER_SIZE 512
static char app_buffer[APP_BUFFER_SIZE];
/*---------------------------------------------------------------------------*/
static struct mqtt_message *msg_ptr = 0; 
static struct mqtt_connection conn;

mqtt_status_t status;
char broker_address[CONFIG_IP_ADDR_STR_LEN];


/*---------------------------------------------------------------------------*/
PROCESS(wind_seismic_sensor_process, "wind seismic sensor");
/*---------------------------------------------------------------------------*/

// declaration of variables to handle wind sensor
static int wind = 0;
static int numOverWind = 0;
static int adaptiveCoeffWind = 1;
static int oldWind = 0;
static int rateOverWind = 0;

// declaration of variables to handle seismic sensor
static int seismic = 0;
static int numOverSeismic = 0;
static int adaptiveCoeffSeismic = 1;
static int oldSeismic = 0;
static int rateOverSeismic = 0;

void updateRateWind();
void updateRateSeismic();

static int numTopic = 2;


// Function called for handling an incoming message
static void pub_handler(const char *topic, uint16_t topic_len, const uint8_t *chunk, uint16_t chunk_len) {
    char str[25] = "";

    parseAndConvert(chunk, (size_t)chunk_len, str, 64);

	if(strcmp(topic, "set_wind") == 0) {
		oldWind = wind;

        sscanf(str,"{wind: %d}", &wind);
		LOG_INFO("Received SET Wind Command, Wind set at value: %d\n", wind);

		sprintf(app_buffer, "{nodeId: %d, speed: %d}", node_id, wind);
		mqtt_publish(&conn, NULL, pub_topic, (uint8_t *)app_buffer, strlen(app_buffer), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);
		updateRateWind();
	} else if(strcmp(topic, "set_frequency") == 0) {
		oldSeismic = seismic;

		sscanf(str,"{frequency: %d}", &seismic);
		LOG_INFO("Received SET Frequency Command, Frequency set at value: %d\n", seismic);

    	sprintf(app_buffer, "{nodeId: %d, frequency: %d}", node_id, seismic);  
		mqtt_publish(&conn, NULL, pub_topic, (uint8_t *)app_buffer, strlen(app_buffer), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);
		updateRateSeismic();
	} else {
		LOG_ERR("Topic not valid!\n");
	}

}


// This function is called each time occurs a MQTT event
static void mqtt_event(struct mqtt_connection *m, mqtt_event_t event, void *data) {
	switch(event) {
		case MQTT_EVENT_CONNECTED: 
		{
			printf("Application has a MQTT connection\n");

            state = STATE_CONNECTED;
            break;
		}
		case MQTT_EVENT_DISCONNECTED: 
		{
			printf("MQTT Disconnect. Reason %u\n", *((mqtt_event_t *)data));

            state = STATE_DISCONNECTED;
            process_poll(&wind_seismic_sensor_process);
            break;
		}
		case MQTT_EVENT_PUBLISH: 
		{
			msg_ptr = data;
			pub_handler(msg_ptr->topic, strlen(msg_ptr->topic), msg_ptr->payload_chunk, msg_ptr->payload_length);
			break;
		}
		case MQTT_EVENT_SUBACK:  
		{
			#if MQTT_311
			mqtt_suback_event_t *suback_event = (mqtt_suback_event_t *)data;
			if(suback_event->success) 
			{
				LOG_INFO("Application has subscribed to the topic\n");
			} 
			else 
			{
				LOG_ERR("Application failed to subscribe to topic (ret code %x)\n", suback_event->return_code);
			}
			#else
			LOG_INFO("Application has subscribed to the topic\n");
			#endif
			break;
		}
		case MQTT_EVENT_UNSUBACK:
		{
			LOG_INFO("Application is unsubscribed to topic successfully\n");
			break;
		}
		case MQTT_EVENT_PUBACK: 
		{
			LOG_INFO("Publishing complete.\n");
			break;
		}
		default:
		{
			LOG_INFO("Application got a unhandled MQTT event: %i\n", event);
			break;
		}
	}
}

static bool have_connectivity(void) {
	if(uip_ds6_get_global(ADDR_PREFERRED) == NULL || uip_ds6_defrt_choose() == NULL) {
		LOG_INFO("Not connected yet...\n");
		return false;
	}
	return true;
}

void getWind(void) {
	int random_number= (rand()%10)+1;
	oldWind = wind;

	if(rand()%100 < 20){
		if(rand()%100 < 75){
			wind += random_number;
		} else{
			wind = (wind-random_number < 0)? 0:wind-random_number;
		}
	}

	if(wind > 70){
		numOverWind++;
	}

	if(numOverWind > 5){
		int random = (rand()%20)+10;
		wind = ((wind - random) < 0)? 0: wind-random;
		numOverWind = 0;
	}
}

void changeSeismic(void) {

	oldSeismic = seismic;

	if(rand() % 100 < 5){
		int randFreq = rand()%11;
		randFreq = randFreq + 25;

		seismic += randFreq;
	}

	if(seismic > 30){
		numOverSeismic++;
	}

	if(numOverSeismic > 3){
		if(rand() % 100 < 80){
			int randFreq = rand()%11;

			randFreq = randFreq + 20;
			seismic = (seismic-randFreq <0)?0:seismic-randFreq;

			numOverSeismic = 0;
		}
	}
}

void updateRateWind(){
	if(oldWind == wind){
		rateOverWind++;
		if(rateOverWind>4){
			adaptiveCoeffWind++;
			adaptiveCoeffWind = (adaptiveCoeffWind > 4)?4:adaptiveCoeffWind;
			rateOverWind = 0;
		}
		
	}else{
		adaptiveCoeffWind = 1;
		rateOverWind = 0;
	}
}

void updateRateSeismic(){
	if(oldSeismic == seismic){
		rateOverSeismic++;
		if(rateOverSeismic>4){
			adaptiveCoeffSeismic = adaptiveCoeffSeismic + 2;
			adaptiveCoeffSeismic = (adaptiveCoeffSeismic > 4)?4:adaptiveCoeffSeismic;
			rateOverSeismic = 0;
		}
		
	}else{
		adaptiveCoeffSeismic = 1;
		rateOverSeismic = 0;
	}
}

/*---------------------------------------------------------------------------*/
PROCESS_THREAD(wind_seismic_sensor_process, ev, data)
{
    PROCESS_BEGIN();

    // Initialize the ClientID as MAC address
	snprintf(client_id, BUFFER_SIZE, "%02x%02x%02x%02x%02x%02x",
		     linkaddr_node_addr.u8[0], linkaddr_node_addr.u8[1],
		     linkaddr_node_addr.u8[2], linkaddr_node_addr.u8[5],
		     linkaddr_node_addr.u8[6], linkaddr_node_addr.u8[7]);

    // Broker registration					 
    mqtt_register(&conn, &wind_seismic_sensor_process, client_id, mqtt_event, MAX_TCP_SEGMENT_SIZE);
	LOG_INFO("Broker registration...\n");

    state=STATE_INIT;
				    
	// Initialize periodic timer to check the status
	etimer_set(&periodic_timer, DEFAULT_PUBLISH_INTERVAL);

    while(1)
    {
        PROCESS_YIELD();

        if((ev == PROCESS_EVENT_TIMER && data == &periodic_timer) ||
			(ev == PROCESS_EVENT_TIMER && data == &periodic_timer_wind) || 
			ev == PROCESS_EVENT_POLL) {
            if(state==STATE_INIT) {
				if(have_connectivity()==true) { 
					state = STATE_NET_OK;
				}
			}  

            if(state == STATE_NET_OK) {
				LOG_INFO("Connecting to MQTT broker\n"); 
			  	memcpy(broker_address, broker_ip, strlen(broker_ip));
			  
			  	mqtt_connect(&conn, broker_address, DEFAULT_BROKER_PORT,
						   (DEFAULT_PUBLISH_INTERVAL * 3) / CLOCK_SECOND,
						   MQTT_CLEAN_SESSION_ON);

			  	state = STATE_CONNECTING;
			} 

			if(state == STATE_CONNECTED){
                // Subscribe to a topic
                if(numTopic == 2){
					strcpy(sub_topic_1,"set_wind");

					status = mqtt_subscribe(&conn, NULL, sub_topic_1, MQTT_QOS_LEVEL_0);

					printf("Subscribing to set_wind!\n");
					if(status == MQTT_STATUS_OUT_QUEUE_FULL) {
						LOG_ERR("1)Tried to subscribe but command queue was full!\n");
						PROCESS_EXIT();
					}
					
					numTopic--;
				} else if(numTopic == 1){
					strcpy(sub_topic_2,"set_frequency");

					status = mqtt_subscribe(&conn, NULL, sub_topic_2, MQTT_QOS_LEVEL_0);

					printf("Subscribing to set_seismic!\n");
					if(status == MQTT_STATUS_OUT_QUEUE_FULL) {
						LOG_ERR("2)Tried to subscribe but command queue was full!\n");
						PROCESS_EXIT();
					}
					
					numTopic--;
					state = STATE_SUBSCRIBED;

					etimer_set(&periodic_timer_wind, DEFAULT_PUBLISH_INTERVAL_WIND);
				}
            }

            if(state == STATE_SUBSCRIBED){
                if(data == &periodic_timer_wind){
					sprintf(pub_topic, "%s", "wind");
				
					getWind();

					updateRateWind();

					LOG_INFO("New value of wind: %d\n", wind);
					
					sprintf(app_buffer, "{nodeId: %d, speed: %d}", node_id, wind);  
					mqtt_publish(&conn, NULL, pub_topic, (uint8_t *)app_buffer, strlen(app_buffer), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);
					
					etimer_set(&periodic_timer_seismic, INTERVAL_TO_THE_SECOND_PUBLISH*adaptiveCoeffSeismic);

					// wait one second to publish the frequency information
					PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&periodic_timer_seismic));

					//publish seismic information
					sprintf(pub_topic, "%s", "seismic");
				
					changeSeismic();

					updateRateSeismic();

					LOG_INFO("New value of seismic: %d\n", seismic);
					
					sprintf(app_buffer, "{nodeId: %d, frequency: %d}", node_id, seismic);
					mqtt_publish(&conn, NULL, pub_topic, (uint8_t *)app_buffer, strlen(app_buffer), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);
					
					etimer_set(&periodic_timer_wind, DEFAULT_PUBLISH_INTERVAL_WIND*adaptiveCoeffWind);
				}
            }

            else if ( state == STATE_DISCONNECTED ){
				LOG_ERR("Disconnected from MQTT broker\n");	
				state = STATE_INIT;
			}

			if(state != STATE_SUBSCRIBED){
				etimer_set(&periodic_timer, DEFAULT_PUBLISH_INTERVAL);
			}
        }
    }

    PROCESS_END();
}