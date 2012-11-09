package org.dongq.database;

public class Main {

	public static void main(String[] args) {
		ExportTool exp = new ExportTool();
		
		boolean exportTable = false;
		boolean exportData = false;
		boolean exportSequence = true;
		exp.start(exportTable, exportData, exportSequence);
	}

}
