CONTIKI_PROJECT = actuators
all: $(CONTIKI_PROJECT)
CONTIKI = ../../..

# The BR is either native or embedded, and in the latter case must support SLIP

CFLAGS += -DPROJECT_CONF_H=\"project-conf.h\"
MODULES_REL += ./resources

# Include RPL BR module
include $(CONTIKI)/Makefile.dir-variables

MODULES += $(CONTIKI_NG_APP_LAYER_DIR)/coap

# Include optional target-specific module
include $(CONTIKI)/Makefile.identify-target
MODULES_REL += $(TARGET)

include $(CONTIKI)/Makefile.include
