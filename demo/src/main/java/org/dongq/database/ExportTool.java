package org.dongq.database;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
	
	private static final String schema = "QUICKRIDE";
	private static final String dataSqlScriptDir = "data-sql-script";
	private static final String tableSqlScriptDir = "table-sql-script";
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	/**
	 * 生成建表脚本文件
	 */
	public void exportTable() {
		try {
			File scriptDir = new File(tableSqlScriptDir);
			
			if(!scriptDir.exists()) {
				scriptDir.mkdirs();
			} else {
				FileUtils.deleteQuietly(scriptDir);
				scriptDir.mkdirs();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
	/**
	 * 生成表数据脚本<br>
	 * insert 格式<br>
	 * 忽略lob类型的字段
	 */
	public void exportData() {
		try {
			File scriptDir = new File(dataSqlScriptDir);
			
			if(!scriptDir.exists()) {
				scriptDir.mkdirs();
			} else {
				FileUtils.deleteQuietly(scriptDir);
				scriptDir.mkdirs();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		List<Table> tables = getTables();
		for (Table table : tables) {
			createDataFileOfTable(table);
		}
		log.info("export data finish...");
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
						table.getColumns().add(new Column(index, name, typeName, dataType));
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
		final String prefix = ",";
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
	 * 根据取出的数据类型，转换为相应格式的字符串
	 * @param type 字段类型，同java.sql.Types
	 * @param columnData ResultSet中取出的数据库表中的字段值
	 * @return
	 */
	public String getColumnDataString(int type, Object columnData) {
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
	
	/**
	 * 为当前表创建一个数据脚本
	 * @param table
	 */
	public void createDataFileOfTable(Table table) {
		try {
			List<String> list = getDataOfTable(table);
			
			String filename = table.getName() + ".sql";
			File file = new File(dataSqlScriptDir+"/"+filename);
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
	
}
