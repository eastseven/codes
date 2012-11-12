package org.dongq.database;

public class PrimarKeyColumn {

	private int index;

	private String name;

	private String tableName;

	private String columnName;

	public PrimarKeyColumn(int index, String name, String tableName,
			String columnName) {
		super();
		this.index = index;
		this.name = name;
		this.tableName = tableName;
		this.columnName = columnName;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public String getScript() {
		return "alter table " + this.tableName + " add constraint " + this.name + " primary key("+this.columnName+");";
	}

	@Override
	public String toString() {
		return "PrimarKeyColumn [index=" + index + ", name=" + name
				+ ", tableName=" + tableName + ", columnName=" + columnName
				+ "]";
	}

}
