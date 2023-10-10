package it.unipi.iot;

import java.util.Scanner;

import it.unipi.iot.coap.CoAPRegistrationServer;
import it.unipi.iot.coap.handler.AlarmHandler;
import it.unipi.iot.coap.handler.BarrierHandler;
import it.unipi.iot.coap.handler.SnowMachineHandler;
import it.unipi.iot.database.DevicesManager;
import it.unipi.iot.database.SeismicManager;
import it.unipi.iot.database.TemperatureManager;
import it.unipi.iot.database.WindManager;
import it.unipi.iot.model.SeismicSample;
import it.unipi.iot.model.TemperatureSample;
import it.unipi.iot.model.WindSample;
import it.unipi.iot.mqtt.MQTTHandler;

public class SkiTrackMonitoring {
	
	static MQTTHandler mqtthandler = new MQTTHandler();
	static CoAPRegistrationServer coapRegistrationServer = new CoAPRegistrationServer();
	static BarrierHandler barrierHandler = new BarrierHandler();
	static AlarmHandler alarmHandler = new AlarmHandler();
	static SnowMachineHandler snowMachineHandler = new SnowMachineHandler();
	
	static Scanner scanner = new Scanner(System.in);
	
	public static void main(String[] args) {
		
		coapRegistrationServer.start();
		
		barrierHandler.start(); // starting the handler for the barriers
		alarmHandler.start(); // starting the handler for the alarm
		snowMachineHandler.start(); // starting the handler for the snow machines
		
		System.out.println();
		System.out.println("|****************************************************|");
		System.out.println("|*****************APPLICATION STARTED****************|");
		System.out.println("|****************************************************|");
		System.out.println();
		printCommands();
		
        String[] parts;
        int value;
		
		while(true){
			
			System.out.print(">> ");
            
        	String command = scanner.nextLine();
            parts = command.split(" ");
            
            switch (parts[0]) {
                case "!help":
                	helpFunction();
                	break;
                	
            	case "!getTemperature":	 
            		value = getValues("temperature");
            		System.out.println("The temperature has a value of: " + value +" degree");
            		break;
            		
            	case "!getWind":
            		value = getValues("wind");
            		System.out.println("The Wind has a value of: " + value +" Km/h");
            		break;
            		
            	case "!getFrequency":
            		value = getValues("frequency");
            		System.out.println("The Seismic frequency has a value of: " + value +" Hz");
            		break;
                	
                case "!setTemperature":
                	setValues("temperature");
                	break;
                	
                case "!setWind":
                	setValues("wind");
                	break;
                	
                case "!setFrequency":
                	setValues("frequency");
                	break;
                	
                case "!SnowMachineON":
                	changeStatus("snowMachine", "ON");
                	setAuto("snowMachine", "OFF");
                	break;
                	
                case "!SnowMachineOFF":
                	changeStatus("snowMachine", "OFF");
                	setAuto("snowMachine", "OFF");
                	break;
                	
                case "!BarrierON":
                	changeStatus("barrier", "ON");
                	setAuto("barrier", "OFF");
                	break;
                	
                case "!BarrierOFF":
                	changeStatus("barrier", "OFF");
                	setAuto("barrier", "OFF");
                	break;
                	
                case "!AlarmON":
                	changeStatus("alarm", "ON");
                	setAuto("alarm", "OFF");
                	break;
                	
                case "!AlarmOFF":
                	changeStatus("alarm", "OFF");
                	setAuto("alarm", "OFF");
                	break;
                	
                case "!SkiLaneON":
                	changeStatus("skiLane", "ON");
                	setAuto("skiLane", "OFF");
                	break;
                	
                case "!SkiLaneOFF":
                	changeStatus("skiLane", "OFF");
                	setAuto("skiLane", "OFF");
                	break;
                	
                case "!setAutoAll":
                	setAuto("all", "ON");
                	break;
                	
                case "!Exit":
                    System.out.println("Closing the program..!");
                    DevicesManager.deleteDevices();
                    scanner.close();
                    System.exit(0);
                	break;
                	
                default:
                    System.out.println("Command not recognized, try again");
                    break;
            }
		}
			
	}

	private static void printCommands() {
		System.out.println("|********************COMMAND LIST********************|\n"+
				">> !help\n"+
				">> !getTemperature\n"+
				">> !getWind\n"+
				">> !getFrequency\n"+
				">> !setTemperature\n"+
				">> !setWind\n"+
				">> !setFrequency\n"+
				">> !SnowMachineON\n"+
				">> !SnowMachineOFF\n"+
				">> !BarrierON\n"+
				">> !BarrierOFF\n"+
				">> !AlarmON\n"+
				">> !AlarmOFF\n"+
				">> !SkiLaneON\n"+
				">> !SkiLaneOFF\n"+
				">> !setAutoAll\n"+ 
				">> !Exit\n"+
				"|****************************************************|\n"
				);
	}
	
	private static void helpFunction() {
		System.out.println("|****************VERBOSE COMMAND LIST****************|\n"+
				">> !getTemperature --> Get the temperature from the temperature sensor\n"+
				">> !getWind --> Get the wind from the wind sensor\n"+
				">> !getFrequency --> Get the seismic frequency from the seismic sensor\n"+
				">> !setTemperature --> Set the temperature of the temperature sensor\n"+
				">> !setWind --> Set the wind of the wind sensor\n"+
				">> !setFrequency --> Set the seismic frequency of the seismic sensor\n"+
				">> !SnowMachineON --> Turn the Snow Machine ON\n"+
				">> !SnowMachineOFF --> Turn the Snow Machine OFF\n"+
				">> !BarrierON --> Open the security barrier\n"+
				">> !BarrierOFF --> Close the security barrier\n"+
				">> !AlarmON --> Activate the Alarm\n"+
				">> !AlarmOFF --> Disable the Alarm\n"+
				">> !SkiLaneON --> Open the ski lane, opening the barrier and turning the snow machine OFF\n"+
				">> !SkiLaneOFF --> Close the ski lane, closing the barrier and turning the snow machine ON\n"+
				">> !setAutoAll --> Set the automatic mode for all the actuator\n"+
				">> !Exit --> Close the program\n"+
				"|****************************************************|\n"
				);
		
	}
	
	private static int getValues(String topic) {
		switch(topic) {
			case "temperature":
				TemperatureSample t = TemperatureManager.getTemperatureByIndex(1);
				return t.getTemperature();
			case "wind":
				WindSample w = WindManager.getSpeedByIndex(1);
				return w.getSpeed();
			case "frequency":
				SeismicSample s = SeismicManager.getFrequencyByIndex(1);
				return s.getFrequency();
		}
		scanner.nextLine();
		return -1;
	}
	
	private static void setValues(String topic) {
		int value = 0;
		System.out.println("Print the desired value for "+topic+":");
		
		value = scanner.nextInt();
		
		String message;
		
		switch(topic) {
			case "temperature":
				message  = "{temperature: "+value+"}";
				mqtthandler.publishMessage("set_temperature", message);
				break;
				
			case "wind":
				message  = "{wind: "+value+"}";
				mqtthandler.publishMessage("set_wind", message);
				break;

			case "frequency":
				message  = "{frequency: "+value+"}";
				mqtthandler.publishMessage("set_frequency", message);
				break;

		}
		Log.logInfoMessage("Set command correctly delivered!");
		scanner.nextLine();
	}
	
	private static void changeStatus(String object, String command) {
		int value = 0;
		
		switch(command) {
			case "ON":
				value = 0;
				break;
			case "OFF":
				value = 1;
				break;
		}
		
		switch(object) {
			case "snowMachine":
				for(int i = 0; i < DevicesManager.getSnowMachines().size(); i++) {
					SnowMachineHandler.changeStatusSnowMachine(DevicesManager.getSnowMachines().get(i), value);
						}
				break;
				
			case "barrier":
				for(int i = 0; i < DevicesManager.getBarriers().size(); i++) {
					BarrierHandler.changeStatusBarrier(DevicesManager.getBarriers().get(i), value);
						}
				break;

			case "alarm":
				for(int i = 0; i < DevicesManager.getAlarms().size(); i++) {
					AlarmHandler.changeStatusAlarms(DevicesManager.getAlarms().get(i), value);
						}
				break;
				
			case "skiLane":
				int valueNegate = (value==0)? 1:0;
				
				for(int i = 0; i < DevicesManager.getBarriers().size(); i++) {
					BarrierHandler.changeStatusBarrier(DevicesManager.getBarriers().get(i), value);
						}
				
				for(int i = 0; i < DevicesManager.getSnowMachines().size(); i++) {
					SnowMachineHandler.changeStatusSnowMachine(DevicesManager.getSnowMachines().get(i), valueNegate);
						}
				break;

		}
	}
	
	private static void setAuto(String object, String command) {
		int value = 0;
		
		switch(command) {
			case "OFF":
				value = 0;
				break;
			case "ON":
				value = 1;
				break;
		}
		
		switch(object) {
			case "snowMachine":
				System.out.println("Auto mode for snow machine: " + command);
				SnowMachineHandler.setAuto(value);
				break;
				
			case "barrier":
				System.out.println("Auto mode for barrier: " + command);
				BarrierHandler.setAuto(value);
				break;

			case "alarm":
				System.out.println("Auto mode for alarm: " + command);
				AlarmHandler.setAuto(value);
				break;
				
			case "skiLane":
				System.out.println("Auto mode for ski Lane: " + command);
				SnowMachineHandler.setAuto(value);
				BarrierHandler.setAuto(value);
				break;
				
			case "all":
				System.out.println("Auto mode for all");
				SnowMachineHandler.setAuto(value);
				BarrierHandler.setAuto(value);
				AlarmHandler.setAuto(value);
				break;
		}
	}
}