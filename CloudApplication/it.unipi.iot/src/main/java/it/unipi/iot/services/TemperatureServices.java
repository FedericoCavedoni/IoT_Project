package it.unipi.iot.services;

import it.unipi.iot.database.TemperatureManager;

public class TemperatureServices {
	public static double getAvgTemperatureSamples(int numSamples) {
		int avg = 0;
		for(int i = 1; i <= numSamples; i++) {
			avg = avg + TemperatureManager.getTemperatureByIndex(i).getTemperature();
		}
		return (avg/numSamples);
	}
}
