package org.dongq.database;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;

import com.google.common.collect.Lists;

public class ImportTool {

	private static final Log log = LogFactoryImpl.getLog(ImportTool.class);
	
	private static final String delimiter = ";";
	final String regex1 = "\\$\\{username\\}";
	final String regex2 = "\\$\\{password\\}";
	
	public void start() {
		final String username = "quickride" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
		final String password = username;
		
		log.info(username);
		importUser(username, password);
		importSequences(username, password);
		importTables(username, password);
		
		check(username, password);
	}
	
	public void check(String username, String password) {
		
		try {
			
			//seq
			String sql = "select count(1) from all_sequences where sequence_owner = '"+ExportTool.schema+"'";
			int sequnecesOrigin = query(sql, JdbcConnectionFactory.getConnect());
			sql = "select count(1) from all_sequences where sequence_owner = '"+username.toUpperCase()+"'";
			int sequnecesNew = query(sql, JdbcConnectionFactory.getConnect(username, password));
			log.info("origin sequences is " + sequnecesOrigin + ", new sequences is " + sequnecesNew);
			
			//table
			sql = "select count(1) from all_tables where owner = '"+ExportTool.schema+"'";
			int tablesOrigin = query(sql, JdbcConnectionFactory.getConnect());
			sql = "select count(1) from all_tables where owner = '"+username.toUpperCase()+"'";
			int tablesNew = query(sql, JdbcConnectionFactory.getConnect(username, password));
			log.info("origin tables is " + tablesOrigin + ", new tables is " + tablesNew);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public void importUser(String username, String password) {
		try {
			
			File file = new File(ExportTool.databaseFileName);
			String script = FileUtils.readFileToString(file);
			script = script.replaceAll(regex1, username).replaceAll(regex2, password);
			String[] sqls = script.split(delimiter);

			for (String sql : sqls) {
				executeSql(sql, JdbcConnectionFactory.getConnect());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void importSequences(String username, String password) {
		try {
			File dir = new File(ExportTool.sequenceDir);
			File[] files = dir.listFiles();
			List<String> sqls = Lists.newArrayList();
			for (File file : files) {
				String script = FileUtils.readFileToString(file).replaceAll(delimiter, "");
				sqls.add(script);
			}
			
			executeSqls(sqls, JdbcConnectionFactory.getConnect(username, password));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void importTables(String username, String password) {
		try {
			File dir = new File(ExportTool.tableDir);
			File[] files = dir.listFiles();
			for (File file : files) {
				String script = FileUtils.readFileToString(file).replaceAll(delimiter, "");
				executeSql(script, JdbcConnectionFactory.getConnect(username, password));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void executeSql(String sql, Connection conn) {
		//Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try {
			stmt = conn.prepareStatement(sql);
			int update = stmt.executeUpdate();
			conn.commit();
			log.info(sql + " execute update : " + update);
		} catch (Exception e) {
			log.warn(e);
			try {
				DbUtils.rollback(conn);
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		} finally {
			DbUtils.closeQuietly(conn, stmt, rs);
		}
	}
	
	public void executeSqls(List<String> sqls, Connection conn) {
		Statement stmt = null;
		ResultSet rs = null;
		
		try {
			
			stmt = conn.createStatement();
			for (String sql : sqls) {
				stmt.addBatch(sql);
				//log.info("batch sql add : " + sql);
			}
			int[] update = stmt.executeBatch();
			conn.commit();
			log.info("execute batch update : " + update.length);
		} catch (Exception e) {
			log.warn(e);
			try {
				DbUtils.rollback(conn);
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		} finally {
			DbUtils.closeQuietly(conn, stmt, rs);
		}
	}
	
	public int query(String sql, Connection conn) {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try {
			
			stmt = conn.prepareStatement(sql);
			rs = stmt.executeQuery();
			if(rs.next()) {
				return rs.getInt(1);
			}
			
		} catch (Exception e) {
			log.warn(e);
			try {
				DbUtils.rollback(conn);
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		} finally {
			DbUtils.closeQuietly(conn, stmt, rs);
		}
		
		return 0;
	}
}
