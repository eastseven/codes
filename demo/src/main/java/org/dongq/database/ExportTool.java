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
import java.util.Collection;
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
	public static final String schema = "QUICKRIDE";//HR
	
	public static final String sqlScriptDir = "sql-script";
	
	public static final String dataDir = sqlScriptDir + "/data";
	public static final String tableDir = sqlScriptDir + "/table";
	public static final String sequenceDir = sqlScriptDir + "/sequence";
	
	public static final String databaseFileName = sqlScriptDir + "/database.sql";
	public static final String tablesFileName = sqlScriptDir + "/all-tables.sql";
	public static final String tablesKeyFileName = sqlScriptDir + "/all-tables-key.sql";
	public static final String sequencesFileName = sqlScriptDir + "/all-sequences.sql";
	public static final String allDataFileName = sqlScriptDir + "/all-data.sql";

	public static final String tablesAddFileName = sqlScriptDir + "/all-tables-add.sql";//
	public static final String sequencesAddFileName = sqlScriptDir + "/all-sequences-add.sql";//
	
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static void main(String[] args) {
		ExportTool exp = new ExportTool();
		exp.mkdirs();
		exp.sync();
	}
	
	public void start() {
		start(true, true, true);
	}
	
	public void start(boolean exportTable, boolean exportData, boolean exportSequence) {
		
		mkdirs();
		
		//导出表、导出数据
		if(exportTable) {
			List<Table> tables = getTables();
			exportTable(tables);
			
		}
		if(exportData) {
			List<Table> tables = getTables();
			exportData(tables);
		}

		if(exportSequence) {
			List<Sequence> sequences = getSequences();
			exportSequence(sequences);
		}
		
		String script = generateDatabaseScript();
		createDatabaseFile(databaseFileName, script);
		
		sync();
	}
	
	/**
	 * 比较数据库表字段,
	 * target - source = new fields
	 */
	public void sync() {
		try {
			log.info("比较数据库表字段 开始");
			
			log.info("Sequence:");
			List<Sequence> sourceSequences = getSequences(JdbcConnectionFactory.getConnectForSource(), schema+"PRODUCT");
			List<Sequence> targetSequences = getSequences(JdbcConnectionFactory.getConnectForTarget(), schema);
			log.info("Sequence Source size: " + CollectionUtils.size(sourceSequences) + ", Target size: " + CollectionUtils.size(targetSequences));
			syncSequences(sourceSequences, targetSequences);
			
			log.info("Table:");
			List<Table> sourceTables = getTables(JdbcConnectionFactory.getConnectForSource(), schema+"PRODUCT"); 
			List<Table> targetTables = getTables(JdbcConnectionFactory.getConnectForTarget(), schema);
			log.info("Table Source size: " + CollectionUtils.size(sourceTables) + ", Target size: " + CollectionUtils.size(targetTables));
			syncTables(sourceTables, targetTables);
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			log.info("比较数据库表字段 结束");
		}
	}
	
	@SuppressWarnings("unchecked")
	public void syncTables(List<Table> sourceTables, List<Table> targetTables) {
		try {
			List<String> scripts = Lists.newArrayList();
			//新增的table
			//TODO 暂时不考虑主外键的问题
			Collection<Table> tables = CollectionUtils.subtract(targetTables, sourceTables);
			if(CollectionUtils.isNotEmpty(tables)) {
				for (Table table : tables) {
					String script = generateTableScript(table);
					scripts.add(script);
					log.info(script);
				}
			}
			
			//新增的column
			for (Table sourceTable : sourceTables) {
				int index = targetTables.indexOf(sourceTable);
				if(index != -1) {
					Table targetTable = targetTables.get(index);
					Set<Column> targetColumns = targetTable.getColumns();
					Set<Column> sourceColumns = sourceTable.getColumns();
					
					Collection<Column> columns = CollectionUtils.subtract(targetColumns, sourceColumns);
					if(CollectionUtils.isNotEmpty(columns)) {
						for (Column column : columns) {
							scripts.add(column.getAddScript());
							log.info(column.getAddScript());
						}
					}
				}
			}
			
			if(CollectionUtils.isNotEmpty(scripts)) createFile(tablesAddFileName, scripts);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void syncSequences(List<Sequence> sourceSequences, List<Sequence> targetSequences) {
		try {
			Collection<Sequence> sequences = CollectionUtils.subtract(targetSequences, sourceSequences);
			if(CollectionUtils.isNotEmpty(sequences)) {
				List<String> scripts = Lists.newArrayList();
				for (Sequence sequence : sequences) {
					String script = sequence.getScript();
					scripts.add(script);
				}
				
				createFile(sequencesAddFileName, scripts);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 生成建表脚本文件
	 */
	public void exportTable(List<Table> tables) {
		List<String> scripts = Lists.newArrayList();
		List<String> keys = Lists.newArrayList();
		for (Table table : tables) {
			String filename = table.getFileName();
			String script = generateTableScript(table);
			scripts.add(script);
			
			if(CollectionUtils.isNotEmpty(table.getPrimaryKeys())) {
				for (PrimarKeyColumn pk : table.getPrimaryKeys()) {
					keys.add(pk.getScript());
				}
			}
			
			if(CollectionUtils.isNotEmpty(table.getForeignKeys())) {
				for(ForeignKeyColumn fk : table.getForeignKeys()) {
					keys.add(fk.getScript());
				}
			}
			
			createTableFile(filename, script);
		}
		try {
			createFile(tablesFileName, scripts);
			createFile(tablesKeyFileName, keys);
		} catch (Exception e) {
			e.printStackTrace();
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
		
		try {
			File all = new File(allDataFileName);
			
			if(!all.exists()) all.createNewFile();
			
			FileWriter writer = new FileWriter(all, true);
			BufferedWriter buffer = new BufferedWriter(writer);
			
			File dir = new File(dataDir);
			File[] files = dir.listFiles();
			for (File file : files) {
				String str = FileUtils.readFileToString(file, "utf-8");
				buffer.write(str);
			}
			
			IOUtils.closeQuietly(buffer);
			IOUtils.closeQuietly(writer);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void exportSequence(List<Sequence> sequences) {
		List<String> scripts = Lists.newArrayList();
		for (Sequence sequence : sequences) {
			String filename = sequence.getFileName();
			String script = sequence.getScript();
			scripts.add(sequence.getDropScript());
			scripts.add(script);
			createSequenceFile(filename, script);
		}
		
		try {
			createFile(sequencesFileName, scripts);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		log.info("export sequence finish...");
	}

	/**
	 * 取原来数据库的序列对象
	 * @return
	 */
	public List<Sequence> getSequences() {
		List<Sequence> sequences = Lists.newArrayList();
		try {
			sequences = getSequences(JdbcConnectionFactory.getConnect(), schema);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sequences;
	}
	
	public List<Sequence> getSequences(Connection conn, String schema) {
		List<Sequence> sequences = Lists.newArrayList();
		
		ResultSet rs = null;
		PreparedStatement stmt = null;
		final String sql = "select * from all_sequences where SEQUENCE_OWNER = ? order by SEQUENCE_NAME asc";
		
		try {
			//conn = JdbcConnectionFactory.getConnect();
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
	
	/**
	 * 取原来数据库的表对象
	 * @return
	 */
	public List<Table> getTables() {
		List<Table> tables = Lists.newArrayList();
		try {
			tables = getTables(JdbcConnectionFactory.getConnect(), schema);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return tables;
	}
	
	public List<Table> getTables(Connection conn, String schema) {
		List<Table> tables = Lists.newArrayList();
		try {
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
					while(rs.next()) {
						String name = rs.getString("COLUMN_NAME");
						String typeName = rs.getString("TYPE_NAME");
						int dataType = rs.getInt("DATA_TYPE");
						int columnSize = rs.getInt("COLUMN_SIZE");
						int index = rs.getInt("ORDINAL_POSITION");
						table.getColumns().add(new Column(index, name, typeName, dataType, columnSize, tableName));
					}
					
					//主键
					//TODO 未考虑联合主键的问题
					rs = dmd.getPrimaryKeys(null, schema, tableName);
					while(rs.next()) {
						String pkName = rs.getString("PK_NAME");
						String pkColumnName = rs.getString("COLUMN_NAME");
						int index = rs.getInt("KEY_SEQ");
						table.getPrimaryKeys().add(new PrimarKeyColumn(index, pkName, tableName, pkColumnName));
					}
					//外键
					rs = dmd.getImportedKeys(null, schema, tableName);
					while(rs.next()) {
						int index = rs.getInt("KEY_SEQ");
						String columnName = rs.getString("FKCOLUMN_NAME");
						String fkTableName = rs.getString("PKTABLE_NAME");
						String fkColumnName = rs.getString("PKCOLUMN_NAME");
						String fkName = rs.getString("FK_NAME");
						table.getForeignKeys().add(new ForeignKeyColumn(index, fkName, tableName, columnName, fkTableName, fkColumnName));
					}
				}
			}
			
			DbUtils.close(rs);
			//DbUtils.close(conn);
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
			if(column.getDataType() == Types.DECIMAL) continue;
			if(column.getDataType() == Types.NUMERIC) continue;
			if(column.getDataType() == Types.INTEGER)  continue;
			if(column.getDataType() == Types.BIGINT)  continue;
			
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
			IOUtils.closeQuietly(writer);
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
		IOUtils.closeQuietly(writer);
		long fileSize = FileUtils.sizeOf(file);
		log.info(file.getAbsolutePath() + " size " + FileUtils.byteCountToDisplaySize(fileSize));
	}
	
	protected void createFile(String filename, List<String> scripts) throws IOException {
		File file = new File(filename);
		if(!file.exists()) file.createNewFile();
		FileWriter writer = new FileWriter(file, true);
		BufferedWriter buffer = new BufferedWriter(writer);
		for (String script : scripts) {
			buffer.write(script);
			buffer.newLine();
		}
		IOUtils.closeQuietly(buffer);
		IOUtils.closeQuietly(writer);
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
		case Types.CHAR:
			value = "'"+columnData+"'";
			break;
		case Types.BLOB:
			value = "null";
			break;
		case Types.CLOB:
			value = "null";
			break;
		default:
			value = "null";
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
