package org.dongq.database;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
	
	public static final String sqlScriptDir = "sql-script";
	public static final String dataDir = sqlScriptDir + "/data";
	public static final String tableDir = sqlScriptDir + "/table";
	public static final String sequenceDir = sqlScriptDir + "/sequence";
	public static final String databaseFileName = sqlScriptDir + "/database.sql";
	
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public void start() {
		
		mkdirs();
		
		List<Table> tables = getTables();
		List<Sequence> sequences = getSequences();
		
		//导出表、导出数据
		exportTable(tables);
		exportData(tables);
		exportSequence(sequences);
		
		String script = generateDatabaseScript();
		createDatabaseFile(databaseFileName, script);
	}
	
	/**
	 * 生成建表脚本文件
	 */
	public void exportTable(List<Table> tables) {
		for (Table table : tables) {
			String filename = table.getFileName();
			String script = generateTableScript(table);
			createTableFile(filename, script);
		}
		
		log.info("export table script finish...");
	}

	/**
	 * 生成表数据脚本<br>
	 * insert 格式<br>
	 * 忽略lob类型的字段
	 */
	public void exportData(List<Table> tables) {
		for (Table table : tables) {
			createDataFile(table);
		}
		
		log.info("export data finish...");
	}
	
	public void exportSequence(List<Sequence> sequences) {
		for (Sequence sequence : sequences) {
			String filename = sequence.getFileName();
			String script = sequence.getScript();
			createSequenceFile(filename, script);
		}

		log.info("export sequence finish...");
	}

	public List<Sequence> getSequences() {
		List<Sequence> sequences = Lists.newArrayList();
		
		ResultSet rs = null;
		Connection conn = null;
		PreparedStatement stmt = null;
		final String sql = "select * from all_sequences where SEQUENCE_OWNER = ? order by SEQUENCE_NAME asc";
		
		try {
			conn = JdbcConnectionFactory.getConnect();
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
			Connection conn = JdbcConnectionFactory.getConnect();
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
	public List<String> generateDataScript(Table table) {
		List<String> data = Lists.newArrayList();
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try {
			final String tableName = table.getName();
			final String sql = table.getSelectSQL();
			final Set<Column> columns = table.getColumns();
			
			conn = JdbcConnectionFactory.getConnect();
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
	 * 生成建表脚本
	 * @param table
	 * @return
	 */
	public String generateTableScript(Table table) {
		String script = "create table " + table.getName() + " (";
		
		String columnsContent = "";
		Set<Column> columns = table.getColumns();
		for (Column column : columns) {
			columnsContent += "," + column.getName() + " " + column.getTypeName();
			if(column.getDataType() == Types.DATE) continue;
			if(column.getDataType() == Types.CLOB) continue;
			if(column.getDataType() == Types.BLOB) continue;
			columnsContent += "("+column.getColumnSize()+")";
		}
		
		if(columnsContent.startsWith(prefix)) {
			columnsContent = columnsContent.replaceFirst(prefix, "");
		}
		script += columnsContent + ");";
		
		return script;
	}
	
	/**
	 * 为当前表创建一个数据脚本<br>
	 * sql-script/data/filename.sql
	 * @param table
	 */
	public void createDataFile(Table table) {
		try {
			List<String> list = generateDataScript(table);
			if(CollectionUtils.isEmpty(list)) return;
			String filename = table.getFileName();
			File file = new File(dataDir + "/" + filename);
			if(!file.exists()) file.createNewFile();
			FileWriter writer = new FileWriter(file, true);
			BufferedWriter buffer = new BufferedWriter(writer);
			for (String str : list) {
				buffer.write(str);
				buffer.newLine();
			}
			IOUtils.closeQuietly(buffer);
			
			long fileSize = FileUtils.sizeOf(file);
			log.info(file.getAbsolutePath() + " size " + FileUtils.byteCountToDisplaySize(fileSize));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 创建Table脚本文件<br>
	 * sql-script/table/filename.sql
	 * @param filename
	 * @param script
	 */
	public void createTableFile(String filename, String script) {
		try {
			filename = tableDir + "/" + filename;
			createFile(filename, script);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 创建Sequence脚本文件<br>
	 * sql-script/sequence/filename.sql
	 * @param filename
	 * @param script
	 */
	public void createSequenceFile(String filename, String script) {
		try {
			filename = sequenceDir + "/" + filename;
			createFile(filename, script);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 创建Database脚本文件<br>
	 * sql-script/database.sql
	 * @param filename
	 * @param script
	 */
	public void createDatabaseFile(String filename, String script) {
		try {
			createFile(filename, script);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void createFile(String filename, String script) throws IOException {
		
		File file = new File(filename);
		if(!file.exists()) file.createNewFile();
		FileWriter writer = new FileWriter(file, true);
		BufferedWriter buffer = new BufferedWriter(writer);
		buffer.write(script);
		buffer.newLine();
		IOUtils.closeQuietly(buffer);
		
		long fileSize = FileUtils.sizeOf(file);
		log.info(file.getAbsolutePath() + " size " + FileUtils.byteCountToDisplaySize(fileSize));
	}
	
	public String generateDatabaseScript() {
		String script = "";
		//1.create user
		script += "create user ${username} identified by ${password} ;";
		//2.grant session
		script += "grant connect, resource, dba to ${username};";

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
			value = " to_date('"+sdf.format(d)+"','yyyy-mm-dd hh24:mi:ss') ";
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
	
	public void mkdirs() {
		mkdir(sqlScriptDir);
		
		mkdir(dataDir);
		mkdir(tableDir);
		mkdir(sequenceDir);
	}
	
	public void mkdir(String dir) {
		try {
			File scriptDir = new File(dir);
			if(scriptDir.exists()) {
				FileUtils.deleteQuietly(scriptDir);
			}
			boolean bln = scriptDir.mkdirs();
			log.info(scriptDir.getAbsolutePath() + " mkdirs " + bln);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
