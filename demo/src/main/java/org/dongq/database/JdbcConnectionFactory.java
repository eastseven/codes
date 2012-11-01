package org.dongq.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class JdbcConnectionFactory {

	private Connection conn;
	
	private Properties loadProperties(String fileName) {
		Properties p = new Properties();
		InputStream in = this.getClass().getClassLoader().getResourceAsStream(fileName);
		if (in != null) {
			try {
				p.load(in);
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return p;
	}
	
	public Connection getConnect() throws ClassNotFoundException, SQLException {
		if (this.conn == null) {
			Properties p = loadProperties("jdbc.properties");
			Class.forName(p.getProperty("jdbc.driverClassName"));
			this.conn = DriverManager.getConnection(p.getProperty("jdbc.url"),
													p.getProperty("jdbc.username"), 
													p.getProperty("jdbc.password"));
		}
		return this.conn;
	}
	
}
