package it.unipi.iot.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import it.unipi.iot.model.TemperatureSample;

public class TemperatureManager {
	public static void insertTemperatureSample(TemperatureSample temperatureSample) {
		DBDriver dbDriver = DBDriver.getInstance();
		try
		{
			Connection connection = dbDriver.getConnection();
			PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO temperature (node, value, time) VALUES (?, ?, ?)");
			preparedStatement.setInt(1, temperatureSample.getNodeId());
			preparedStatement.setInt(2, temperatureSample.getTemperature());
			preparedStatement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
			preparedStatement.executeUpdate();
			preparedStatement.close();
			connection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static TemperatureSample getTemperatureByIndex(int index) {
		DBDriver dbDriver = DBDriver.getInstance();
		TemperatureSample temperatureSample = new TemperatureSample();
		
		try {
			Connection connection = dbDriver.getConnection();
			
			PreparedStatement preparedStatement = null;
			if(index == 1) {
				preparedStatement = connection.prepareStatement("SELECT node, value FROM temperature ORDER BY time DESC LIMIT ?");
				preparedStatement.setInt(1, index);
			} else {
				preparedStatement = connection.prepareStatement("SELECT node, value FROM temperature ORDER BY time DESC LIMIT ?, 1");
				preparedStatement.setInt(1, index - 1);
			}
			ResultSet resultSet = preparedStatement.executeQuery();
			if(resultSet.first() == false) {
				temperatureSample.setTemperature(0);
				temperatureSample.setNodeId(0);
			} else {
				temperatureSample.setTemperature(resultSet.getInt("value"));
				temperatureSample.setNodeId(resultSet.getInt("node"));
			}
			
			preparedStatement.close();
			connection.close();
			
			return temperatureSample;
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
