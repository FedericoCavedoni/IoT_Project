package it.unipi.iot.services;

import it.unipi.iot.database.WindManager;

public class WindServices {
	public static double getAvgSpeedSamples(int numSamples) {
		int avg = 0;
		for(int i = 1; i <= numSamples; i++) {
			avg = avg + WindManager.getSpeedByIndex(i).getSpeed();
		}
		return (avg/numSamples);
	}
}
