package it.unipi.iot.services;

import it.unipi.iot.database.SeismicManager;

public class SeismicServices {
	public static double getAvgFrequencySamples(int numSamples) {
		int avg = 0;
		for(int i = 1; i <= numSamples; i++) {
			avg = avg + SeismicManager.getFrequencyByIndex(i).getFrequency();
		}
		return (avg/numSamples);
	}
}
