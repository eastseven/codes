package org.dongq.database;

import java.util.List;
import java.util.Random;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DatabaseToolTest {

	private static final Log log = LogFactory.getLog(DatabaseToolTest.class);
	
	private Table table;
	
	private ExportTool tool = new ExportTool();
	
	@Before
	public void init() {
		List<Table> tables = tool.getTables();
		table = tables.get(new Random().nextInt(tables.size()));
	}
	
	@Test
	public void testExportData() {
		tool.exportData();
	}
	
	@Test
	public void testExportTable() {
		tool.exportTable();
	}
	
	@Test
	public void testCreateTableScript() {
		String script = tool.createTableScript(table);
		Assert.assertTrue(StringUtils.isNotBlank(script));
		log.info(script);
	}
}
