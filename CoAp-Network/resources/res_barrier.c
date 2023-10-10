#include <stdlib.h>
#include <string.h>
#include "coap-engine.h"
#include "contiki.h"
#include "dev/leds.h"
#include <string.h>
#include "../../util/util.c"

#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_APP

static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_barrier,
         "title=\"Barrier\";rt=\"Text\"",
        NULL,
        res_put_handler,
        res_put_handler, 
        NULL);

static int led_state = 0;

static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
    const uint8_t* chunk;
    char str[256];
    char command[64];
    int len = coap_get_payload(request, &chunk);
    
    parseAndConvert(chunk, len, str, 256);

    sscanf(str, "{command: %s }", command);

    coap_set_status_code(response, CHANGED_2_04);

    if (strcmp(command, "open") == 0 && led_state == 0) {
		leds_set(LEDS_GREEN);
        LOG_INFO("Barrier opened succesfully\n");
        led_state = 1;
	}
	else if (strcmp(command, "close") == 0 && led_state == 1){
		leds_set(LEDS_RED);
        LOG_INFO("Barrier closed succesfully\n");
        led_state = 0;
	}
	else if((strcmp(command, "open") != 0) && (strcmp(command, "close") != 0)){
		printf("Action not allowed \n");
		coap_set_status_code(response, BAD_REQUEST_4_00);
	}
}
