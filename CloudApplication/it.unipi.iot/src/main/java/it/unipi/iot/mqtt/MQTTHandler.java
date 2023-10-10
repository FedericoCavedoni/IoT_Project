package it.unipi.iot.mqtt;


import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.google.gson.Gson;

import it.unipi.iot.Log;
import it.unipi.iot.database.SeismicManager;
import it.unipi.iot.database.TemperatureManager;
import it.unipi.iot.database.WindManager;
import it.unipi.iot.model.SeismicSample;
import it.unipi.iot.model.TemperatureSample;
import it.unipi.iot.model.WindSample;

public class MQTTHandler implements MqttCallback{
	private final String BROKER = "tcp://127.0.0.1:1883";
	private final String CLIENT_ID = "MQTTHandler";
	
	private final static String TOPIC_TEMPERATURE = "temperature";
	private final static String TOPIC_WIND = "wind";
	private final static String TOPIC_SEISMIC = "seismic";
	
	private static final int MAX_RECONNECTION = 5;
	
	private static MqttClient mqttClient = null;
	private Gson parser;
	 
	public MQTTHandler(){
		Log.createInstance();
		do {
			try {
				mqttClient = new MqttClient(BROKER, CLIENT_ID);
				mqttClient.setCallback( this );
				
				connect();
			} catch (MqttException me) {
				System.err.println(">> I could not connect, Retrying ...");
			}
		} while(!mqttClient.isConnected());
	}
	
	// utility function to create the connection with the broker
	private static void connect() throws MqttException {
		mqttClient.connect();
		
		mqttClient.subscribe(TOPIC_TEMPERATURE);
		Log.logInfoMessage("Subscribed to topic: " + TOPIC_TEMPERATURE);
		mqttClient.subscribe(TOPIC_WIND);
		Log.logInfoMessage("Subscribed to topic: " + TOPIC_WIND);
		mqttClient.subscribe(TOPIC_SEISMIC);
		Log.logInfoMessage("Subscribed to topic: " + TOPIC_SEISMIC);
	}


	public void connectionLost(Throwable cause) {
		System.err.println(">> Connection with the broker lost!");
		System.err.println(cause.getMessage());
		
		int i = 0;
		do {
			if(i > MAX_RECONNECTION) {
				System.err.println(">> Cannot recover the connection!");
				System.exit(-1);
			}
			try {
				 Thread.sleep(3000); //try every 3000 seconds to recover the connection
				 System.out.println(">> Trying to recover the connection!");
				 connect();
				 
				 i++;
			} catch(MqttException me) {
				System.err.println(">> I could not connect, Retrying ...");
			} catch(InterruptedException ie) {
				System.err.println(">> Error with the timer");
			}
		} while(!mqttClient.isConnected());
		
		System.out.println(">> Connection with the broker restored!");
	}

	public void messageArrived(String topic, MqttMessage message) throws Exception {
		parser = new Gson();
		
		String payload = new String(message.getPayload());
		Log.logInfoMessage("Message received from topic: " + TOPIC_SEISMIC);
		Log.logInfoMessage("The payload is: " + payload);

		
		if(topic.equals("temperature")) {
			Log.logInfoMessage("Inserting sample: " + payload + " in temperature");
			TemperatureSample temperatureSample = parser.fromJson(payload, TemperatureSample.class);
			Log.logInfoMessage("Sample: " + temperatureSample.getNodeId() + " " + temperatureSample.getTemperature());
			TemperatureManager.insertTemperatureSample(temperatureSample);
		}
		if(topic.equals("wind")) {
			Log.logInfoMessage("Inserting sample: " + payload + " in wind");
			WindSample windSample = parser.fromJson(payload, WindSample.class);
			Log.logInfoMessage("Sample: " + windSample.getNodeId() + " " + windSample.getSpeed());
			WindManager.insertWindSample(windSample);
		}
		if(topic.equals("seismic")) {
			Log.logInfoMessage("Inserting sample: " + payload + " in seismic");
			SeismicSample seismicSample = parser.fromJson(payload, SeismicSample.class);
			Log.logInfoMessage("Sample: " + seismicSample.getNodeId() + " " + seismicSample.getFrequency());
			SeismicManager.insertSeismicSample(seismicSample);
		}
	}

	public void deliveryComplete(IMqttDeliveryToken token) {
		Log.logInfoMessage("Message correctly delivered!");
	}
	
	public void publishMessage (final String topic, final String message)
    {
        try
        {
            mqttClient.publish(topic, new MqttMessage(message.getBytes()));
        }
        catch(MqttException e)
        {
            e.printStackTrace();
        }
    }
}
