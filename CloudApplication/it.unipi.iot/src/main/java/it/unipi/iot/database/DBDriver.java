package it.unipi.iot.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBDriver {
	private static DBDriver dbDriver = null;
	
	private static String ip;
	private static int port;
	private static String username;
	private static String password;
	private static String databaseName;
	
	public static DBDriver getInstance() {
		if(dbDriver == null) {
			dbDriver = new DBDriver();
		}
		
		return dbDriver;
	}
	
	public DBDriver() {
		ip = "localhost";
		port = 3306;
		username = "root";
		password = "osboxes.org";
		databaseName = "project";
	}
	
	public Connection getConnection() throws SQLException {
		return DriverManager.getConnection("jdbc:mysql://"+ ip + ":" + port +
                "/" + databaseName + "?zeroDateTimeBehavior=CONVERT_TO_NULL&serverTimezone=CET",
        username, password);
	}
}
