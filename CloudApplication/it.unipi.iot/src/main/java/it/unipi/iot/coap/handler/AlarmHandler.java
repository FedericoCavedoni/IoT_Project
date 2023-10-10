package it.unipi.iot.coap.handler;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.Request;

import it.unipi.iot.database.DevicesManager;
import it.unipi.iot.services.SeismicServices;
import it.unipi.iot.services.TemperatureServices;
import it.unipi.iot.services.WindServices;

public class AlarmHandler extends Thread{
	private static int status = 1; // 0 ON, 1 OFF
	private static int newStatus;
	private static int auto = 1;
	
	public void run() {
		// check for the presence of alarms
		while(DevicesManager.getAlarms().size() == 0) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e1) {
				System.err.println("Exception raised in AlarmHandler...");
			}
		}
		while(true) {
			try {
				Thread.sleep(5000);
				if(auto == 1) {
					if((WindServices.getAvgSpeedSamples(3) > 50.0 && TemperatureServices.getAvgTemperatureSamples(5) > 35.0) || 
							(WindServices.getAvgSpeedSamples(3) > 50.0 && SeismicServices.getAvgFrequencySamples(2) > 30) ||
							(TemperatureServices.getAvgTemperatureSamples(5) > 35.0 && SeismicServices.getAvgFrequencySamples(2) > 30)) {
						// turn on the alarm
						newStatus = 0;
						if(newStatus != status) {
							for(int i = 0; i < DevicesManager.getAlarms().size(); i++) {
								changeStatusAlarms(DevicesManager.getAlarms().get(i), newStatus);
							}
							for(int i = 0; i < DevicesManager.getBarriers().size(); i++) {
								BarrierHandler.changeStatusBarrier(DevicesManager.getBarriers().get(i), 0);
							}
						}
					} else { // turn off the alarm
						newStatus = 1;
						if(newStatus != status) {
							for(int i = 0; i < DevicesManager.getAlarms().size(); i++) {
								changeStatusAlarms(DevicesManager.getAlarms().get(i), newStatus);
							}
							for(int i = 0; i < DevicesManager.getBarriers().size(); i++) {
								BarrierHandler.changeStatusBarrier(DevicesManager.getBarriers().get(i), 1);
							}
						}
					}
				}
			} catch(Exception e) {
				System.err.println("Exception raised in AlarmHandler...");
			}
		}
	}

	public static void changeStatusAlarms(CoapClient alarm, int stat) {
		if(alarm == null) {
			return;
		}
		
		String msg = "";
		if(stat == 0) {
			msg = "{command: ON }";
		} else {
			msg = "{command: OFF }";
		}
		
		Request request = Request.newPut();
		request.setPayload(msg.getBytes());
		
		status = stat;
		
		CoapResponse response = alarm.advanced(request);
	}
	
	public static void setAuto(int value) {
		auto = value;
	}
}
