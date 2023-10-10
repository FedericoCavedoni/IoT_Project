package it.unipi.iot;

import java.sql.Timestamp;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Log {
	private static Logger instance = null;
	
	public static void createInstance() {
		if(instance == null) {
			try {
				FileHandler fh = new FileHandler("log/LogFile.log");
				instance = Logger.getLogger(Log.class.getName());
				instance.addHandler(fh);
				SimpleFormatter formatter = new SimpleFormatter();  
		        fh.setFormatter(formatter);
		        instance.setUseParentHandlers(false);
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
	}
	
	public static void logSevereMessage(String msg) {
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		instance.severe("[" + timestamp + "] " + msg);
	}
	
	public static void logInfoMessage(String msg) {
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		instance.info("[" + timestamp + "] " + msg);
	}
}
