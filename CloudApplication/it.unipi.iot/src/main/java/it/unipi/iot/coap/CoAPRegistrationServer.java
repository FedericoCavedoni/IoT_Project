package it.unipi.iot.coap;

import java.nio.charset.StandardCharsets;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;

import it.unipi.iot.Log;

public class CoAPRegistrationServer extends CoapServer{
	
	public CoAPRegistrationServer() {
        this.add(new CoapRegistrationResource());
    }

    class CoapRegistrationResource extends CoapResource {
        public CoapRegistrationResource(){
            super("registration");
            setObservable(true);
        }
        
        public void handlePOST(CoapExchange exchange) {
        	String actuatorType = exchange.getRequestText();
        	String ip = exchange.getSourceAddress().getHostAddress();
            boolean success = true;
            
            if(actuatorType.equals("barrier")) {
            	DevicesList.registerBarrier(ip);
            	Log.logInfoMessage("Barrier successfully registered");
            	System.out.println();
            } else if(actuatorType.equals("alarm")) {
            	DevicesList.registerAlarm(ip);
            	Log.logInfoMessage("Alarm successfully registered");
            } else if(actuatorType.equals("snow_machine")) {
            	DevicesList.registerSnowMachine(ip);
            	Log.logInfoMessage("SnowMachine successfully registered");
            } else {
            	success = false;
            }
            
            if(success == true) {
            	exchange.respond(ResponseCode.CREATED, "Success".getBytes(StandardCharsets.UTF_8));
            } else {
            	exchange.respond(ResponseCode.NOT_ACCEPTABLE, "Not success".getBytes(StandardCharsets.UTF_8));
            }

        }

    }
}