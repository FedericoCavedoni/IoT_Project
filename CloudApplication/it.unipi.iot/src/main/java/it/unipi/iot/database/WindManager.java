package it.unipi.iot.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import it.unipi.iot.model.WindSample;

public class WindManager {
	public static void insertWindSample(WindSample windSample) {
		DBDriver dbDriver = DBDriver.getInstance();
		try
		{
			Connection connection = dbDriver.getConnection();
			PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO wind (node, value, time) VALUES (?, ?, ?)");
			preparedStatement.setInt(1, windSample.getNodeId());
			preparedStatement.setInt(2, windSample.getSpeed());
			preparedStatement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
			preparedStatement.executeUpdate();
			connection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static WindSample getSpeedByIndex(int index) {
		DBDriver dbDriver = DBDriver.getInstance();
		WindSample windSample = new WindSample();
		
		try {
			Connection connection = dbDriver.getConnection();

			PreparedStatement preparedStatement = null;
			if(index == 1) {
				preparedStatement = connection.prepareStatement("SELECT node, value FROM wind ORDER BY time DESC LIMIT ?");
				preparedStatement.setInt(1, index);
			} else {
				preparedStatement = connection.prepareStatement("SELECT node, value FROM wind ORDER BY time DESC LIMIT ?, 1");
				preparedStatement.setInt(1, index - 1);
			}
			ResultSet resultSet = preparedStatement.executeQuery();
			if(resultSet.first() == false) {
				windSample.setSpeed(0);
				windSample.setNodeId(0);
			} else {
				windSample.setSpeed(resultSet.getInt("value"));
				windSample.setNodeId(resultSet.getInt("node"));
			}
			
			preparedStatement.close();
			connection.close();
			
			return windSample;
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
