#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "dev/leds.h"
#include "coap-engine.h"
#include "net/ipv6/uip.h"
#include "net/ipv6/uip-ds6.h"
#include "net/ipv6/uip-debug.h"
#include "sys/etimer.h"
#include "routing/routing.h"
#include "coap-blocking-api.h"
#include "os/dev/button-hal.h"

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_APP

#define SERVER_EP "coap://[fd00::1]:5683"
#define CONNECTION_TRY_INTERVAL 1
#define REGISTRATION_TRY_INTERVAL 1
#define INTERVAL_BETWEEN_CONNECTION_TESTS 1
#define SENSOR_TYPE "alarm"

char *service_url = "/registration";
static bool registered = false;

//static struct etimer simulation_timer;
static struct etimer connectivity_timer;
static struct etimer wait_registration;

extern coap_resource_t res_alarm;

PROCESS(coap_alarm, "CoAP Alarm actuator");
AUTOSTART_PROCESSES(&coap_alarm);


/*---------------------------------------------------------------------------*/
static bool is_connected() {
	if(NETSTACK_ROUTING.node_is_reachable()) {
		LOG_INFO("The Border Router is reachable\n");
		return true;
  } 
  return false;
}

void client_chunk_handler(coap_message_t *response) {
	const uint8_t *chunk;
	if(registered == false){
		if(response == NULL) {
			LOG_INFO("Request timed out\n");
			etimer_set(&wait_registration, CLOCK_SECOND* REGISTRATION_TRY_INTERVAL);
			return;
		}

		int len = coap_get_payload(response, &chunk);

		if(strncmp((char*)chunk, "Success", len) == 0){
			registered = true;
		} else
			etimer_set(&wait_registration, CLOCK_SECOND* REGISTRATION_TRY_INTERVAL);
	}
}

PROCESS_THREAD(coap_alarm, ev, data){
  PROCESS_BEGIN();

  static coap_endpoint_t server_ep;
  static coap_message_t request[1];

  leds_set(LEDS_GREEN);// initial led state
  LOG_INFO("Alarm Actuator started\n");
  coap_activate_resource(&res_alarm, "alarm");

// try to connect to the border router
	etimer_set(&connectivity_timer, CLOCK_SECOND * INTERVAL_BETWEEN_CONNECTION_TESTS);
	PROCESS_WAIT_UNTIL(etimer_expired(&connectivity_timer));
	LOG_INFO("Waiting for connection with the Border Router\n");
	while(!is_connected()) {
		etimer_reset(&connectivity_timer);
		PROCESS_WAIT_UNTIL(etimer_expired(&connectivity_timer));
	}

  while(!registered) {
    	LOG_INFO("Sending registration message\n");
    	coap_endpoint_parse(SERVER_EP, strlen(SERVER_EP), &server_ep);
    	// Prepare the message
     	coap_init_message(request, COAP_TYPE_CON, COAP_POST, 0);
    	coap_set_header_uri_path(request, service_url);
    	coap_set_payload(request, (uint8_t *)SENSOR_TYPE, sizeof(SENSOR_TYPE) - 1);

    	COAP_BLOCKING_REQUEST(&server_ep, request, client_chunk_handler);

    	PROCESS_WAIT_UNTIL(etimer_expired(&wait_registration));
  }

  while(1) {
    PROCESS_WAIT_EVENT();
    if(ev == button_hal_press_event) {
		// changing the status of the alarm
		leds_toggle(LEDS_RED);
	  	leds_toggle(LEDS_GREEN);
    }

  }

  PROCESS_END();
}