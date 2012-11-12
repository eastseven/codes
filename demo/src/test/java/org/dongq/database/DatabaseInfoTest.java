package org.dongq.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.apache.commons.dbutils.DbUtils;
import org.junit.Test;

public class DatabaseInfoTest {

	@Test
	public void testSth() {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			ExportTool exp = new ExportTool();
			conn = JdbcConnectionFactory.getConnect();
			DatabaseMetaData dmd = conn.getMetaData();
			
			List<Table> tables = exp.getTables(conn, "HR");
			for (Table table : tables) {
				System.out.println(table.getName());
				String parentCatalog = null;
				String parentSchema = "HR";
				String parentTable = table.getName();
				String foreignCatalog = null;
				String foreignSchema = "HR";
				String foreignTable = null;
				
				int index = 1;

				System.out.println("");
				rs = dmd.getPrimaryKeys(parentCatalog, parentSchema, parentTable);
				while(rs.next()) {
					System.out.println(index+".PrimaryKeys:");
					System.out.println("表名称: "+rs.getString("TABLE_NAME"));
					System.out.println("列名称: "+rs.getString("COLUMN_NAME"));
					System.out.println("主键中的序列号: "+rs.getShort("KEY_SEQ"));
					System.out.println("主键的名称: "+rs.getString("PK_NAME"));
					
					index++;
				}
				
				System.out.println("");
				index = 1;
				rs = dmd.getCrossReference(parentCatalog, parentSchema, parentTable, foreignCatalog, foreignSchema, foreignTable);
				while(rs.next()) {
					System.out.println(index+".CrossReference:");
					System.out.println("父键表名称: "+rs.getString("PKTABLE_NAME"));
					System.out.println("父键列名称: "+rs.getString("PKCOLUMN_NAME"));
					System.out.println("被导入的外键表名称: "+rs.getString("FKTABLE_NAME"));
					System.out.println("被导入的外键列名称: "+rs.getString("FKCOLUMN_NAME"));
					
					System.out.println("外键的名称: "+rs.getString("FK_NAME"));
					System.out.println("父键的名称: "+rs.getString("PK_NAME"));
					
					index++;
				}
				
				System.out.println("");
				index = 1;
				rs = dmd.getExportedKeys(parentCatalog, parentSchema, parentTable);
				while(rs.next()) {
					System.out.println(index+".ExportedKeys:");
					System.out.println("父键表名称: "+rs.getString("PKTABLE_NAME"));
					System.out.println("父键列名称: "+rs.getString("PKCOLUMN_NAME"));
					System.out.println("被导入的外键表名称: "+rs.getString("FKTABLE_NAME"));
					System.out.println("被导入的外键列名称: "+rs.getString("FKCOLUMN_NAME"));
					
					System.out.println("外键的名称: "+rs.getString("FK_NAME"));
					System.out.println("父键的名称: "+rs.getString("PK_NAME"));
					
					index++;
				}
				
				System.out.println("");
				index = 1;
				rs = dmd.getImportedKeys(parentCatalog, parentSchema, parentTable);
				while(rs.next()) {
					System.out.println(index+".ImportedKeys:");
					System.out.println("父键表名称: "+rs.getString("PKTABLE_NAME"));
					System.out.println("父键列名称: "+rs.getString("PKCOLUMN_NAME"));
					System.out.println("被导入的外键表名称: "+rs.getString("FKTABLE_NAME"));
					System.out.println("被导入的外键列名称: "+rs.getString("FKCOLUMN_NAME"));
					
					System.out.println("外键的名称: "+rs.getString("FK_NAME"));
					System.out.println("父键的名称: "+rs.getString("PK_NAME"));
					
					index++;
				}
				
				System.out.println("----------");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			DbUtils.closeQuietly(conn, stmt, rs);
		}
	}
}
