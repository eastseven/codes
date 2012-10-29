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
import java.util.Random;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Lists;

public class Main {

	private static final Log log = LogFactory.getLog(Main.class);
	
	private static final String schema = "QUICKRIDE";
	private static final String dir = "data-sql-script";
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static void main(String[] args) throws Exception {
		log.info("hello world");
		
		new Main().exportData();
	}

	public void exportData() {
		try {
			File scriptDir = new File(dir);
			
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
	
	public void createDataFileOfTable(Table table) {
		try {
			List<String> list = getDataOfTable(table);
			
			String filename = table.getName() + ".sql";
			File file = new File(dir+"/"+filename);
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
	
	public void test2() throws Exception {
		List<Table> tables = getTables();
		
		Connection conn = new ConnectionFactory().getConnect();
		ResultSet rs = null;
		PreparedStatement stmt = null;
		
		Table table = tables.get(new Random().nextInt(tables.size()));
		System.out.println(table);
		String sql = table.getSelectSQL();
		stmt = conn.prepareStatement(sql);
		rs = stmt.executeQuery();
		rs.setFetchSize(10);
		final String prefix = ",";

		List<String> list = Lists.newArrayList();
		//final String encoding = "utf-8";
		//Writer writer = new FileWriter(table.getName()+".sql");
		//WriterOutputStream output = new WriterOutputStream(writer);
		
		while(rs.next()) {
			String insert = "insert into " + table.getName() + "(";
			String values = " values(";
			String data = "";
			String columns = "";
			for (Column c : table.getColumns()) {
				columns += ","+c.getName();
				Object columnData = rs.getObject(c.getName());
				data += "," + getColumnDataString(c.getDataType(), columnData);
			}
			
			if(data.startsWith(prefix)) data = data.replaceFirst(prefix, "");
			values += data + ");";
			if(columns.startsWith(prefix)) columns = columns.replaceFirst(prefix, "");
			insert += columns + ")";
			
			data = insert + values + "\n";
			System.out.println(data);
			
			list.add(data);
			//IOUtils.write(data, output, encoding);
		}
		
		//IOUtils.closeQuietly(output);
		DbUtils.closeQuietly(conn, stmt, rs);
		
		String filename = table.getName() + ".sql";
		File file = new File(filename);
		if(!file.exists()) file.createNewFile();
		FileWriter writer = new FileWriter(file, true);
		BufferedWriter buffer = new BufferedWriter(writer);
		for (String str : list) {
			buffer.write(str);
		}
		IOUtils.closeQuietly(buffer);
	}
	
	public void test() throws Exception {
		Connection conn = new ConnectionFactory().getConnect();
		ResultSet rs = null;
		
		DatabaseMetaData dmd = conn.getMetaData();
		log.info(dmd.getDatabaseProductVersion());
		
		//Catalogs
		rs = dmd.getCatalogs();
		while(rs.next()) {
			log.info("TABLE_CAT: "+rs.getString("TABLE_CAT"));
		}
		DbUtils.close(rs);
		
		//Schemas 
		rs = dmd.getSchemas();
		while(rs.next()) {
			String schem = rs.getString("TABLE_SCHEM");
			log.info("TABLE_SCHEM: "+schem);
		}
		DbUtils.close(rs);
		
		//Tables
		String catalog = null;
		String schemaPattern = "QUICKRIDE";
		String tableNamePattern = null;
		String[] types = {"TABLE"};
		rs = dmd.getTables(catalog, schemaPattern, tableNamePattern, types);
		while(rs.next()) {
			String tableName = rs.getString("TABLE_NAME");
			log.info(tableName);
		}
		DbUtils.close(rs);
		
		//Columns
		rs = dmd.getColumns(catalog, schemaPattern, "RENT_ORDER", null);
		while(rs.next()) {
			String name = rs.getString("COLUMN_NAME");
			int dataType = rs.getInt("DATA_TYPE");
			String typeName = rs.getString("TYPE_NAME");
			log.info(name + " " + typeName+"["+dataType+"] ");
		}
		DbUtils.close(rs);
		
		DbUtils.close(conn);
	}
}
