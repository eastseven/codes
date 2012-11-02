package org.dongq.database;

import java.util.List;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;

//select username from all_users where username like 'QUICKRIDE%' order by username asc;
public class DatabaseToolTest {

	private static final Log log = LogFactory.getLog(DatabaseToolTest.class);
	
	private Table table = null;
	private List<Table> tables = null;
	private ExportTool tool = new ExportTool();
	
	@Before
	public void init() {
		//tool.mkdir(ExportTool.sqlScriptDir);
		tables = tool.getTables();
		table = tables.get(new Random().nextInt(tables.size()));
	}

	@Test
	public void testStart() {
		log.info("\nExport Tool Starting...\n");
		tool.start();
		log.info("\nExport Tool Done\n");
	}
	
//	@Test
//	public void testGetSequences() {
//		List<Sequence> sequences = tool.getSequences();
//		Assert.assertTrue(CollectionUtils.isNotEmpty(sequences));
//		for (Sequence sequence : sequences) {
//			System.out.println(sequence.getScript());
//		}
//	}
//	
//	@Test
//	public void printDatabaseInfo() throws Exception {
//		ResultSet rs = null;
//		PreparedStatement stmt = null;
//		Connection conn = new ConnectionFactory().getConnect();
//		DatabaseMetaData dmd = conn.getMetaData();
//		
//		rs = dmd.getTableTypes();
//		while(rs.next()) {
//			System.out.println(rs.getString("TABLE_TYPE"));
//		}
//		
//		DbUtils.closeQuietly(conn, stmt, rs);
//	}
//	
//	@Test
//	public void testExportData() {
//		Assert.assertTrue(CollectionUtils.isNotEmpty(tables));
//		tool.exportData(tables);
//	}
//	
//	@Test
//	public void testExportTable() {
//		Assert.assertTrue(CollectionUtils.isNotEmpty(tables));
//		tool.exportTable(tables);
//	}
//	
//	@Test
//	public void testCreateTableScript() {
//		Assert.assertNotNull(table);
//		String script = tool.generateTableScript(table);
//		Assert.assertTrue(StringUtils.isNotBlank(script));
//		log.info(script);
//	}
//	
//	@Test
//	public void testCreateDatabaseScript() {
//		String username = "quickride";
//		String password = "201210301234";
//		String script = tool.generateDatabaseScript(username, password);
//		String filename = ExportTool.databaseFileName;
//		tool.createDatabaseFile(filename, script);
//		Assert.assertTrue(StringUtils.isNotBlank(script));
//		
//	}
	
}
