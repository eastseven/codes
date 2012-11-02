package org.dongq.database;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.junit.Before;
import org.junit.Test;

//select count(1) from all_tables where owner=
//select count(1) from all_sequences where sequence_owner=
//select username from all_users where username like 'QUICKRIDE%' order by username asc;
public class ImportToolTest {

	private static final Log log = LogFactoryImpl.getLog(ImportToolTest.class);
	
	private ImportTool tool = null;
	
	@Before
	public void init() {
		tool = new ImportTool();
	}
	
	@Test
	public void testStart() {
		log.info("\nImport Tool starting...\n");
		tool.start();
		log.info("\nImport Tool done\n");
	}
}
