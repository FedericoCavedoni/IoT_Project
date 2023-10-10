package it.unipi.iot.coap;


import it.unipi.iot.Log;
import it.unipi.iot.database.DevicesManager;

public class DevicesList { 
	
	public static void registerBarrier(String ip) {
		Log.logInfoMessage("Trying to register a barrier...");
		DevicesManager.insertDevice("barrier", ip);
	}
	
	public static void registerAlarm(String ip) {
		Log.logInfoMessage("Trying to register an alarm...");
		DevicesManager.insertDevice("alarm", ip);
	}
	
	public static void registerSnowMachine(String ip) {
		Log.logInfoMessage("Trying to register a snow machine...");
		DevicesManager.insertDevice("snow machine", ip);
	}

}
