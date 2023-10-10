package it.unipi.iot.coap.handler;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.Request;

import it.unipi.iot.database.DevicesManager;
import it.unipi.iot.services.WindServices;

public class BarrierHandler extends Thread{
	private static int status = 1; // 0 open, 1 close
	private static int auto = 1;
	
	public void run() {
		// check for the presence of barriers
		while(DevicesManager.getBarriers().size() == 0) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e1) {
				System.err.println("Exception raised in BarrierHandler...");
			}
		}
		while(true) {
			
			try {
				Thread.sleep(5000);
				if(auto == 1) {
					if(WindServices.getAvgSpeedSamples(3) > 65.0 && status == 0) { // turn on the barrier
						for(int i = 0; i < DevicesManager.getBarriers().size(); i++) {
							changeStatusBarrier(DevicesManager.getBarriers().get(i), 1);
						}
					} else if(WindServices.getAvgSpeedSamples(3) < 65.0 && status == 1){ // turn off the barrier
						for(int i = 0; i < DevicesManager.getBarriers().size(); i++) {
							changeStatusBarrier(DevicesManager.getBarriers().get(i), 0);
						}
					}
				}
				
			} catch(Exception e) {
				System.err.println("Exception raised in BarrierHandler...");
			}
	
		}
	}
	
	public static void changeStatusBarrier(CoapClient barrier, int stat) {
		if(barrier == null) {
			return;
		}
		
		String msg = "";
		if(stat == 0) {
			msg = "{command: open }";
		} else {
			msg = "{command: close }";
		}
		
		Request request = Request.newPut();
		request.setPayload(msg.getBytes());
		
		status = stat;
		
		CoapResponse response = barrier.advanced(request);
	}
	
	public static int getBarrierStatus() {
		return status;
	}
	
	public static void setAuto(int value) {
		auto = value;
	}
}
