package org.dongq.database;

public class ForeignKeyColumn {

	private int index;

	private String name;

	private String tableName;

	private String columnName;

	private String fkTableName;

	private String fkColumnName;

	public ForeignKeyColumn(int index, String name, String tableName,
			String columnName, String fkTableName, String fkColumnName) {
		super();
		this.index = index;
		this.name = name;
		this.tableName = tableName;
		this.columnName = columnName;
		this.fkTableName = fkTableName;
		this.fkColumnName = fkColumnName;
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

	public String getFkTableName() {
		return fkTableName;
	}

	public void setFkTableName(String fkTableName) {
		this.fkTableName = fkTableName;
	}

	public String getFkColumnName() {
		return fkColumnName;
	}

	public void setFkColumnName(String fkColumnName) {
		this.fkColumnName = fkColumnName;
	}

	public String getScript() {
		return "alter table " + this.tableName + " add constraint " + this.name + " foreign key("+this.columnName+") references " + this.fkTableName + "("+this.fkColumnName+");";
	}
	
	@Override
	public String toString() {
		return "ForeignKeyColumn [index=" + index + ", name=" + name
				+ ", tableName=" + tableName + ", columnName=" + columnName
				+ ", fkTableName=" + fkTableName + ", fkColumnName="
				+ fkColumnName + "]";
	}

}
