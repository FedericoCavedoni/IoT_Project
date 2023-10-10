-> BORDER ROUTER
sudo make TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM0 border-router.dfu-upload
make TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM0 connect-router
-> TEMPERATURE SENSOR
sudo make TARGET=nrf52840 BOARD=dongle temperature_sensor.dfu-upload PORT=/dev/ttyACM1
make login TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM1
-> WIND SEISMIC SENSOR
sudo make TARGET=nrf52840 BOARD=dongle wind_seismic_sensor.dfu-upload PORT=/dev/ttyACM2
make login TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM2
-> ALARM
sudo make TARGET=nrf52840 BOARD=dongle alarm.dfu-upload PORT=/dev/ttyACM3
make login TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM3
-> BARRIER
sudo make TARGET=nrf52840 BOARD=dongle barrier.dfu-upload PORT=/dev/ttyACM4
make login TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM4
-> SNOW
sudo make TARGET=nrf52840 BOARD=dongle snow.dfu-upload PORT=/dev/ttyACM5
make login TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM5
