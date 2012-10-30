package org.dongq.database;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Lists;

public class ExportTool {

	private static final Log log = LogFactory.getLog(ExportTool.class);
	
	private static final String prefix = ",";
	private static final String schema = "QUICKRIDE";
	
	public static final String dataSqlScriptDir = "data";
	public static final String tableSqlScriptDir = "table";
	public static final String sqlScriptDir = "sql-script";
	public static final String createDatabaseScript = "create-database.sql";
	
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public void start() {
		String password = new SimpleDateFormat("yyyyMMddHHmm").format(new java.util.Date());
		String username = "quickride"+password;
		
		mkdir(sqlScriptDir);
		List<Table> tables = getTables();
		
		//导出表、导出数据
		exportTable(tables);
		exportData(tables);
		
		//建库、导入表、导入数据
		String dbScript = generateCreateDatabaseScript(username, password);
		generateCreateDatabaseScriptFile(dbScript);
		
		
	}
	
	/**
	 * 生成建表脚本文件
	 */
	public void exportTable(List<Table> tables) {
		mkdir(sqlScriptDir + "/" + tableSqlScriptDir);
		
		for (Table table : tables) {
			String tableName = table.getName();
			String script = createTableScript(table);
			createTableScriptFile(tableName, script);
		}
		
		log.info("export table script finish...");
	}

	/**
	 * 生成表数据脚本<br>
	 * insert 格式<br>
	 * 忽略lob类型的字段
	 */
	public void exportData(List<Table> tables) {
		mkdir(sqlScriptDir + "/" + dataSqlScriptDir);
		
		for (Table table : tables) {
			createDataFileOfTable(table);
		}
		
		log.info("export data finish...");
	}

	public List<Sequence> getSequences() {
		List<Sequence> sequences = Lists.newArrayList();
		
		ResultSet rs = null;
		Connection conn = null;
		PreparedStatement stmt = null;
		final String sql = "select * from all_sequences where SEQUENCE_OWNER = ? order by SEQUENCE_NAME asc";
		
		try {
			conn = new ConnectionFactory().getConnect();
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, schema);
			rs = stmt.executeQuery();
			
			while(rs.next()) {
				Sequence seq = new Sequence();
				seq.setName(rs.getString("SEQUENCE_NAME"));
				seq.setIncrementBy(rs.getInt("INCREMENT_BY"));
				seq.setCacheSize(rs.getInt("CACHE_SIZE"));
				seq.setLastNumber(rs.getInt("LAST_NUMBER"));
				seq.setMaxValue(new BigInteger(rs.getObject("MAX_VALUE").toString()));
				seq.setMinValue(rs.getInt("MIN_VALUE"));
				
				sequences.add(seq);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			DbUtils.closeQuietly(conn, stmt, rs);
		}
		
		return sequences;
	}
	
	public List<Table> getTables() {
		List<Table> tables = Lists.newArrayList();
		try {
			Connection conn = new ConnectionFactory().getConnect();
			DatabaseMetaData dmd = conn.getMetaData();
			log.info(dmd.getDatabaseProductVersion());
			ResultSet rs = dmd.getTables(null, schema, null, new String[] {"TABLE"});
			
			while(rs.next()) {
				String tableName = rs.getString("TABLE_NAME");
				if(tableName.contains("$")) continue;
				tables.add(new Table(tableName));
			}
			
			if(CollectionUtils.isNotEmpty(tables)) {
				for (Table table : tables) {
					String tableName = table.getName();
					rs = dmd.getColumns(null, schema, tableName, null);
					int index = 0;
					while(rs.next()) {
						String name = rs.getString("COLUMN_NAME");
						String typeName = rs.getString("TYPE_NAME");
						int dataType = rs.getInt("DATA_TYPE");
						int columnSize = rs.getInt("COLUMN_SIZE");
						table.getColumns().add(new Column(index, name, typeName, dataType, columnSize));
						index++;
					}
				}
			}
			
			DbUtils.close(rs);
			DbUtils.close(conn);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		return tables;
	}
	
	/**
	 * 取得table的所有数据
	 * @param table
	 * @return List<String> insert 格式的sql语句集合
	 */
	public List<String> getDataOfTable(Table table) {
		List<String> data = Lists.newArrayList();
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try {
			final String tableName = table.getName();
			final String sql = table.getSelectSQL();
			final Set<Column> columns = table.getColumns();
			
			conn = new ConnectionFactory().getConnect();
			stmt = conn.prepareStatement(sql);
			rs = stmt.executeQuery();
			
			
			while(rs.next()) {
				String insert = "insert into " + tableName + "(";
				String insertContent = "";
				String values = " values(";
				String valuesContent = "";
				
				for (Column column : columns) {
					String name = column.getName();
					Object columnData = rs.getObject(name);
					insertContent += prefix + name;
					valuesContent += prefix + getColumnDataString(column.getDataType(), columnData);
				}
				
				if(insertContent.startsWith(prefix)) insertContent = insertContent.replaceFirst(prefix, "");
				insert += insertContent + ")";
				if(valuesContent.startsWith(prefix)) valuesContent = valuesContent.replaceFirst(prefix, "");
				values += valuesContent + ");";
				
				String insertSql = insert + values;
				//log.info(insertSql);
				data.add(insertSql);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			DbUtils.closeQuietly(conn, stmt, rs);
		}
		
		return data;
	}
	
	/**
	 * 为当前表创建一个数据脚本
	 * @param table
	 */
	public void createDataFileOfTable(Table table) {
		try {
			List<String> list = getDataOfTable(table);
			
			String filename = table.getName() + ".sql";
			File file = new File(sqlScriptDir + "/" + dataSqlScriptDir + "/" + filename);
			if(!file.exists()) file.createNewFile();
			FileWriter writer = new FileWriter(file, true);
			BufferedWriter buffer = new BufferedWriter(writer);
			for (String str : list) {
				buffer.write(str);
				buffer.newLine();
			}
			IOUtils.closeQuietly(buffer);
			
			long fileSize = FileUtils.sizeOf(file);
			log.info(filename + " size " + FileUtils.byteCountToDisplaySize(fileSize));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 生成建表脚本
	 * @param table
	 * @return
	 */
	public String createTableScript(Table table) {
		String script = "create table " + table.getName() + " (";
		
		String columnsContent = "";
		Set<Column> columns = table.getColumns();
		for (Column column : columns) {
			columnsContent += "," + column.getName() + " " + column.getTypeName();
			if(column.getDataType() == Types.DATE) continue;
			columnsContent += "("+column.getColumnSize()+")";
		}
		
		if(columnsContent.startsWith(prefix)) {
			columnsContent = columnsContent.replaceFirst(prefix, "");
		}
		script += columnsContent + ");";
		
		return script;
	}
	
	/**
	 * 创建建表脚本文件
	 * @param tableName
	 * @param script
	 */
	public void createTableScriptFile(String tableName, String script) {
		try {
			
			String filename = tableName + ".sql";
			File file = new File(sqlScriptDir + "/" + tableSqlScriptDir + "/" + filename);
			if(!file.exists()) file.createNewFile();
			FileWriter writer = new FileWriter(file, true);
			BufferedWriter buffer = new BufferedWriter(writer);
			buffer.write(script);
			buffer.newLine();
			IOUtils.closeQuietly(buffer);
			
			long fileSize = FileUtils.sizeOf(file);
			log.info(filename + " size " + FileUtils.byteCountToDisplaySize(fileSize));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void generateCreateDatabaseScriptFile(String script) {
		try {
			
			mkdir(sqlScriptDir);
			
			String filename = sqlScriptDir + "/" + createDatabaseScript;
			File file = new File(filename);
			if(!file.exists()) file.createNewFile();
			FileWriter writer = new FileWriter(file, true);
			BufferedWriter buffer = new BufferedWriter(writer);
			buffer.write(script);
			buffer.newLine();
			IOUtils.closeQuietly(buffer);
			
			long fileSize = FileUtils.sizeOf(file);
			log.info(filename + " size " + FileUtils.byteCountToDisplaySize(fileSize));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String generateCreateDatabaseScript(String username, String password) {
		String script = "";
		//1.create user
		//2.grant session
		//3.grant create table
		//4.grant tablespace
		script += "create user ${username} indentified by ${password} ;";
		script += "grant create session to ${username};";
		script += "grant create table to ${username};";
		script += "grant unlimited tablespace to ${username};";

		final String regex1 = "\\$\\{username\\}";
		final String regex2 = "\\$\\{password\\}";
		script = script.replaceAll(regex1, username);
		script = script.replaceAll(regex2, password);
		
		return script;
	}
	
	/**
	 * 根据取出的数据类型，转换为相应格式的字符串
	 * @param type 字段类型，同java.sql.Types
	 * @param columnData ResultSet中取出的数据库表中的字段值
	 * @return
	 */
	private String getColumnDataString(int type, Object columnData) {
		String value = "";
		
		if(columnData == null) return "null";
		
		switch (type) {
		case Types.DATE:
			Date d = (Date) columnData;
			value = " to_date('"+sdf.format(d)+"','yyyy-MM-dd HH24:mm:ss') ";
			break;
		case Types.DECIMAL:
			value = columnData.toString();
			break;
		case Types.VARCHAR:
			value = "'"+columnData+"'";
			break;
		case Types.BLOB:
			value = "null";
			break;
		case Types.CLOB:
			value = "null";
			break;
		default:
			
			break;
		}
		
		return value;
	}
	
	public void mkdir(String dir) {
		try {
			File scriptDir = new File(dir);
			if(scriptDir.exists()) {
				FileUtils.deleteQuietly(scriptDir);
			}
			scriptDir.mkdirs();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
