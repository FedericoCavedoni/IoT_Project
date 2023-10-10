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
#include <stdlib.h>
#include <time.h>
/*---------------------------------------------------------------------------*/
#define LOG_MODULE "temperature-sensor"
#ifdef MQTT_CLIENT_CONF_LOG_LEVEL
#define LOG_LEVEL MQTT_CLIENT_CONF_LOG_LEVEL
#else
#define LOG_LEVEL LOG_LEVEL_DBG
#endif

#define PI 3.14159265

/*---------------------------------------------------------------------------*/
/* MQTT broker address. */
#define MQTT_CLIENT_BROKER_IP_ADDR "fd00::1"

static const char *broker_ip = MQTT_CLIENT_BROKER_IP_ADDR;

// Defaukt config values
#define DEFAULT_BROKER_PORT         1883
#define DEFAULT_PUBLISH_INTERVAL    (3 * CLOCK_SECOND) // TODO settare al publish interval che scegliamo


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
PROCESS_NAME(temperature_sensor_process);
AUTOSTART_PROCESSES(&temperature_sensor_process);

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
static char sub_topic[BUFFER_SIZE]; 

// Periodic timer to check the state of the MQTT client
#define STATE_MACHINE_PERIODIC     (CLOCK_SECOND >> 1)
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
PROCESS(temperature_sensor_process, "Temperature sensor");
/*---------------------------------------------------------------------------*/

static int temperature = 0;
static int adaptiveCoeff = 1;
static int oldTemperature = 0;
static int rateOver = 0;

void updateRate();

// Function called for handling an incoming message
static void pub_handler(const char *topic, uint16_t topic_len, const uint8_t *chunk, uint16_t chunk_len) {
    char str[25] = "";

	oldTemperature = temperature;

    parseAndConvert(chunk, (size_t)chunk_len, str, 64);

	if(strcmp(topic, "set_temperature") == 0) {
        sscanf(str,"{temperature: %d}", &temperature);
		LOG_INFO("Received SET Temperature Command, Temperature set at value: %d\n", temperature);

    sprintf(app_buffer, "{nodeId: %d, temperature: %d}", node_id, temperature);
	mqtt_publish(&conn, NULL, pub_topic, (uint8_t *)app_buffer, strlen(app_buffer), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);

	} else {
		LOG_ERR("Topic not valid!\n");
	}

	updateRate();
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
            process_poll(&temperature_sensor_process);
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

void getTemperature(){
	oldTemperature = temperature;

	int randomNumber = rand() % 5;
	randomNumber -= 2; // generate a random number between -2 and 2

	//update the temperature with a probability of 40%
	if(rand()%10 < 4){
		temperature += randomNumber;
	}
}

void updateRate(){
	if(oldTemperature == temperature){
		rateOver++;
		if(rateOver>4){
			adaptiveCoeff++;
			adaptiveCoeff = (adaptiveCoeff > 4)?4:adaptiveCoeff;
			rateOver = 0;
		}
		
	}else{	
		adaptiveCoeff = 1;
		rateOver = 0;
	}
}

/*---------------------------------------------------------------------------*/
PROCESS_THREAD(temperature_sensor_process, ev, data)
{
    PROCESS_BEGIN();

    // Initialize the ClientID as MAC address
	snprintf(client_id, BUFFER_SIZE, "%02x%02x%02x%02x%02x%02x",
		     linkaddr_node_addr.u8[0], linkaddr_node_addr.u8[1],
		     linkaddr_node_addr.u8[2], linkaddr_node_addr.u8[5],
		     linkaddr_node_addr.u8[6], linkaddr_node_addr.u8[7]);

    // Broker registration					 
    mqtt_register(&conn, &temperature_sensor_process, client_id, mqtt_event, MAX_TCP_SEGMENT_SIZE);
	LOG_INFO("Broker registration...");

    state=STATE_INIT;
				    
	// Initialize periodic timer to check the status 
	etimer_set(&periodic_timer, DEFAULT_PUBLISH_INTERVAL);

    while(1)
    {
        PROCESS_YIELD();

        if((ev == PROCESS_EVENT_TIMER && data == &periodic_timer) || ev == PROCESS_EVENT_POLL) {
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
                strcpy(sub_topic,"set_temperature");

                status = mqtt_subscribe(&conn, NULL, sub_topic, MQTT_QOS_LEVEL_0);

                printf("Subscribing!\n");
                if(status == MQTT_STATUS_OUT_QUEUE_FULL) {
                    LOG_ERR("Tried to subscribe but command queue was full!\n");
                    PROCESS_EXIT();
                }
                
                state = STATE_SUBSCRIBED;
            }

            if(state == STATE_SUBSCRIBED){
                sprintf(pub_topic, "%s", "temperature");

				getTemperature();
				updateRate();

				LOG_INFO("New value of temperature: %d\n", temperature);
				
				sprintf(app_buffer, "{nodeId: %d, temperature: %d}", node_id, temperature);  // TODO cambiare dopo aver deciso l'encoding
				mqtt_publish(&conn, NULL, pub_topic, (uint8_t *)app_buffer, strlen(app_buffer), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);
            }

            else if ( state == STATE_DISCONNECTED ){
				LOG_ERR("Disconnected from MQTT broker\n");	
				state = STATE_INIT;
			}

            etimer_set(&periodic_timer, DEFAULT_PUBLISH_INTERVAL*adaptiveCoeff);
        }
    }

    PROCESS_END();
}