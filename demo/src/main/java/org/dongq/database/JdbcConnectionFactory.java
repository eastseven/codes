package org.dongq.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class JdbcConnectionFactory {

	private static Properties config = new Properties();
	
	static {
		try {
			InputStream in = JdbcConnectionFactory.class.getClassLoader().getResourceAsStream("jdbc.properties");
			config.load(in);
			in.close();
			
			Class.forName(config.getProperty("jdbc.driverClassName"));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static Properties loadProperties(String fileName) {
		InputStream in = JdbcConnectionFactory.class.getClassLoader().getResourceAsStream(fileName);
		if (in != null) {
			try {
				config.load(in);
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return config;
	}
	
	public static Connection getConnect() throws ClassNotFoundException, SQLException {
		return DriverManager.getConnection(config.getProperty("jdbc.source.url"), config.getProperty("jdbc.source.username"), config.getProperty("jdbc.source.password"));
	}
	
	public static Connection getConnect(String username, String password) throws ClassNotFoundException, SQLException {
		return DriverManager.getConnection(config.getProperty("jdbc.source.url"), username, password);
	}
	
	public static Connection getConnect(String url, String username, String password) throws ClassNotFoundException, SQLException {
		return DriverManager.getConnection(url, username, password);
	}
	
	public static Connection getConnectForSource() throws ClassNotFoundException, SQLException {
		return DriverManager.getConnection(config.getProperty("jdbc.source.url"), config.getProperty("jdbc.source.username"), config.getProperty("jdbc.source.password"));
	}
	
	public static Connection getConnectForTarget() throws ClassNotFoundException, SQLException {
		return DriverManager.getConnection(config.getProperty("jdbc.target.url"), config.getProperty("jdbc.target.username"), config.getProperty("jdbc.target.password"));
	}
}
