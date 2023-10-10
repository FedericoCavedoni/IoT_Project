package it.unipi.iot.coap.handler;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.Request;

import it.unipi.iot.database.DevicesManager;
import it.unipi.iot.services.TemperatureServices;

public class SnowMachineHandler extends Thread{
	private static int status = 1; // 0 ON, 1 OFF
	private static int newStatus;
	private static int auto = 1;
	
	public void run() {
		// check for the presence of snow machines
		while(DevicesManager.getSnowMachines().size() == 0) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e1) {
				System.err.println("Exception raised in SnowMachineHandler...");
			}
		}
		while(true) {
			try {
				Thread.sleep(5000);
				if(auto == 1) {
					if(TemperatureServices.getAvgTemperatureSamples(5) <= -2.5 && BarrierHandler.getBarrierStatus() == 1) {
						// turn on the snow machine
						newStatus = 0;
						if(status != newStatus) {
							for(int i = 0; i < DevicesManager.getSnowMachines().size(); i++) {
								changeStatusSnowMachine(DevicesManager.getSnowMachines().get(i), 0);
							}
						}
					} else { // turn off the snow machine
						newStatus = 1;
						if(status != newStatus) {
							for(int i = 0; i < DevicesManager.getSnowMachines().size(); i++) {
								changeStatusSnowMachine(DevicesManager.getSnowMachines().get(i), 1);
							}
						}
					}
				}
	
			} catch(Exception e) {
				System.err.println("Exception raised in SnowMachineHandler...");
			}
			
		}
	}

	public static void changeStatusSnowMachine(CoapClient snowMachine, int stat) {
		if(snowMachine == null) {
			return;
		}
		String msg = "";
		if(stat == 0) {
			msg = "{command: ON }";
		} else {
			msg = "{command: OFF }";
		}
		
		status = stat;
		Request request = Request.newPut();
		request.setPayload(msg.getBytes());
		
		status = stat;
		
		CoapResponse response = snowMachine.advanced(request);
	}

	public static void setAuto(int value) {
		auto = value;
	}
}
