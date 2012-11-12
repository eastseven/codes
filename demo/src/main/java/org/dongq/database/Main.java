package org.dongq.database;

public class Main {

	public static void main(String[] args) {
		ExportTool exp = new ExportTool();
		
		boolean exportTable = true;
		boolean exportData = true;
		boolean exportSequence = true;
		exp.start(exportTable, exportData, exportSequence);
	}

}
