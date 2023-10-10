package it.unipi.iot.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.californium.core.CoapClient;

public class DevicesManager {
	public static void insertDevice(String type, String ip) {
		DBDriver dbDriver = DBDriver.getInstance();
		try
		{
			Connection connection = dbDriver.getConnection();
			PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO device (type, ip) VALUES (?, ?)");
			preparedStatement.setString(1, type);
			preparedStatement.setString(2, ip);
			preparedStatement.executeUpdate();
			connection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static List<CoapClient> getAlarms(){
		List<CoapClient> alarms = new ArrayList<>();
		DBDriver dbDriver = DBDriver.getInstance();
		
		try {
			Connection connection = dbDriver.getConnection();
			PreparedStatement preparedStatement = connection.prepareStatement("SELECT type, ip FROM device WHERE type = 'alarm'");
			ResultSet resultSet = preparedStatement.executeQuery();
			while(resultSet.next()) {
				CoapClient alarm = new CoapClient("coap://[" + resultSet.getString("ip") + "]/alarm");
				alarms.add(alarm);
			}
			preparedStatement.close();
			connection.close();
			return alarms;
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static List<CoapClient> getBarriers(){
		List<CoapClient> barriers = new ArrayList<>();
		DBDriver dbDriver = DBDriver.getInstance();
		
		try {
			Connection connection = dbDriver.getConnection();
			PreparedStatement preparedStatement = connection.prepareStatement("SELECT type, ip FROM device WHERE type = 'barrier'");
			ResultSet resultSet = preparedStatement.executeQuery();
			while(resultSet.next()) {
				CoapClient barrier = new CoapClient("coap://[" + resultSet.getString("ip") + "]/barrier");
				barriers.add(barrier);
			}
			preparedStatement.close();
			connection.close();
			return barriers;
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static List<CoapClient> getSnowMachines(){
		List<CoapClient> snowMachines = new ArrayList<>();
		DBDriver dbDriver = DBDriver.getInstance();
		
		try {
			Connection connection = dbDriver.getConnection();
			PreparedStatement preparedStatement = connection.prepareStatement("SELECT type, ip FROM device WHERE type = 'snow machine'");
			ResultSet resultSet = preparedStatement.executeQuery();
			while(resultSet.next()) {
				CoapClient snowMachine = new CoapClient("coap://[" + resultSet.getString("ip") + "]/snow_machine");
				snowMachines.add(snowMachine);
			}
			preparedStatement.close();
			connection.close();
			return snowMachines;
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static void deleteDevices() {
		DBDriver dbDriver = DBDriver.getInstance();
		try {
			Connection connection = dbDriver.getConnection();
			PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM device");
			preparedStatement.executeUpdate();
			preparedStatement.close();
			connection.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
