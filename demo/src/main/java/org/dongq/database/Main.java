package org.dongq.database;

import java.io.File;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;

public class Main {

	public static void main(String[] args) {
		start();
	}

	public static void start() {
		ExportTool exp = new ExportTool();
		/*
		 */
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
		//3.创建新库
		//final String delimiter = ";";
		
		final String username = "quickride" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
		final String password = username;
		final String regex1 = "\\$\\{username\\}";
		final String regex2 = "\\$\\{password\\}";
		try {
			System.out.println("username: "+username);
			File file = new File(ExportTool.databaseFileName);
			String script = FileUtils.readFileToString(file);
			script = script.replaceAll(regex1, username).replaceAll(regex2, password);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}
