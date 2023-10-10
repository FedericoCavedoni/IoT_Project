package it.unipi.iot.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import it.unipi.iot.model.SeismicSample;


public class SeismicManager {
	public static void insertSeismicSample(SeismicSample seismicSample) {
		DBDriver dbDriver = DBDriver.getInstance();
		try
		{
			Connection connection = dbDriver.getConnection();
			PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO seismic (node, value, time) VALUES (?, ?, ?)");
			preparedStatement.setInt(1, seismicSample.getNodeId());
			preparedStatement.setInt(2, seismicSample.getFrequency());
			preparedStatement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
			preparedStatement.executeUpdate();
			connection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static SeismicSample getFrequencyByIndex(int index) {
		DBDriver dbDriver = DBDriver.getInstance();
		SeismicSample seismicSample = new SeismicSample();
		
		try {
			Connection connection = dbDriver.getConnection();
			
			PreparedStatement preparedStatement = null;
			if(index == 1) {
				preparedStatement = connection.prepareStatement("SELECT node, value FROM seismic ORDER BY time DESC LIMIT ?");
				preparedStatement.setInt(1, index);
			} else {
				preparedStatement = connection.prepareStatement("SELECT node, value FROM seismic ORDER BY time DESC LIMIT ?, 1");
				preparedStatement.setInt(1, index - 1);
			}
			ResultSet resultSet = preparedStatement.executeQuery();
			if(resultSet.first() == false) {
				seismicSample.setNodeId(0);
				seismicSample.setFrequency(0);
			} else {
				seismicSample.setNodeId(resultSet.getInt("node"));
				seismicSample.setFrequency(resultSet.getInt("value"));
			}
			
			preparedStatement.close();
			connection.close();
			
			return seismicSample;
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
