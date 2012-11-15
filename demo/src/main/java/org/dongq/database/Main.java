package org.dongq.database;

import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.google.common.collect.Lists;

public class Main {

	public static void main(String[] args) {
		start();
	}

	public static void start() {
		ExportTool exp = new ExportTool();
		ImportTool imp = new ImportTool();
		/*
		//1.导出产品库数据
		try {
			System.out.println("1.导出产品库数据");
			exp.exportData(exp.getTables(JdbcConnectionFactory.getConnectForSource(), ExportTool.schema));
			System.out.println("1.导出产品库数据 完成");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		//2.导出开发库表及序列对象
		try {
			System.out.println("2.导出开发库表及序列对象");
			exp.exportTable(exp.getTables(JdbcConnectionFactory.getConnectForTarget(), ExportTool.schema));
			exp.exportSequence(exp.getSequences(JdbcConnectionFactory.getConnectForTarget()));
			System.out.println("2.导出开发库表及序列对象 完成");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		*/
		//3.创建新库
		final String url = "jdbc:oracle:thin:@192.168.1.100:1521:orcl";
		final String username = "quickride" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
		final String password = username;
		final String delimiter = ";";
		final String regex1 = "\\$\\{username\\}";
		final String regex2 = "\\$\\{password\\}";
		try {
			System.out.println("username: "+username);
			File file = new File(ExportTool.databaseFileName);
			String script = FileUtils.readFileToString(file);
			script = script.replaceAll(regex1, username).replaceAll(regex2, password);
			String[] sqls = script.split(delimiter);

			for (String sql : sqls) {
				imp.executeSql(sql, JdbcConnectionFactory.getConnect(url, "quickride", "quickride"));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//3.将开发库脚本导入新库
		//3.将产品库数据导入新库
		Connection conn = null;
		try {
			File tables = new File(ExportTool.tablesFileName);
			File sequences = new File(ExportTool.sequencesFileName);
			File data = new File(ExportTool.allDataFileName);
			File keys = new File(ExportTool.tablesKeyFileName);
			
			List<String> sqls = Lists.newArrayList();
			List<String> list = Lists.newArrayList();
			
			//create table
			list = IOUtils.readLines(new FileReader(tables));
			for (String sql : list) {
				sqls.add(sql.replaceAll(delimiter, ""));
			}
			conn = JdbcConnectionFactory.getConnect(url, username, password);
			imp.executeSqls(sqls, conn);
			System.out.println("create table done...");
			
			//create sequence
			sqls.clear();
			list = IOUtils.readLines(new FileReader(sequences));
			for (String sql : list) {
				sqls.add(sql.replaceAll(delimiter, ""));
			}
			conn = JdbcConnectionFactory.getConnect(url, username, password);
			imp.executeSqls(sqls, conn);
			System.out.println("create sequence done...");
			
			//insert data
			sqls.clear();
			list = IOUtils.readLines(new FileReader(data));
			System.out.println("insert data size: "+list.size());
			List<List<String>> array = Lists.newArrayList();
			final int n = 1000;
			int count = list.size() / n;
			for(int index = 0; index < count; index++) {
				List<String> group = Lists.newArrayList();
				for(int i = 0; i < n; i++) {
					String sql = list.get(i + index * n);
					group.add(sql.replaceAll(delimiter, ""));
				}
				array.add(group);
			}
			if(list.size() % n != 0) {
				List<String> group = Lists.newArrayList();
				for(int index = count * n + list.size() % n; index < list.size(); index++) {
					String sql = list.get(index);
					group.add(sql.replaceAll(delimiter, ""));
				}
				array.add(group);
			}
			
			for (List<String> g : array) {
				imp.executeSqls(g, JdbcConnectionFactory.getConnect(url, username, password));
				System.out.println("insert data done...");
			}
			
			//add primary and foreign key
			sqls.clear();
			list = IOUtils.readLines(new FileReader(keys));
			for (String sql : list) {
				sqls.add(sql.replaceAll(delimiter, ""));
			}
			conn = JdbcConnectionFactory.getConnect(url, username, password);
			imp.executeSqls(sqls, conn);
			System.out.println("add primary and foreign key done...");
		} catch (Exception e) {
			e.printStackTrace();
			try {
				//drop user
				String sql = "drop user "+username+" cascade";
				imp.executeSql(sql, JdbcConnectionFactory.getConnect(url, "quickride", "quickride"));
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		} finally {
			DbUtils.closeQuietly(conn);
		}
	}
}
