package org.dongq.database;

import java.sql.Types;

public class Column {

	private int index;

	private String name;

	private String typeName;

	private int dataType;

	private int columnSize;

	private String tableName;
	
	public Column(int index, String name, String typeName, int dataType, int columnSize, String tableName) {
		super();
		this.index = index;
		this.name = name;
		this.typeName = typeName;
		this.dataType = dataType;
		this.columnSize = columnSize;
		this.tableName = tableName;
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

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public int getDataType() {
		return dataType;
	}

	public void setDataType(int dataType) {
		this.dataType = dataType;
	}

	public int getColumnSize() {
		return columnSize;
	}

	public void setColumnSize(int columnSize) {
		this.columnSize = columnSize;
	}

	public String getTableName() {
		return tableName;
	}
	
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	
	public String getAddScript() {
		String script = "";
		script += "ALTER TABLE "+this.tableName+" ADD ("+this.name + " " + this.typeName;
		if(this.dataType == Types.CHAR)    script += "("+this.columnSize+")";
		if(this.dataType == Types.VARCHAR) script += "("+this.columnSize+")";
		script += ");";
		return script;
	}
	
	@Override
	public String toString() {
		return "Column [index=" + index + ", name=" + name + ", typeName="
				+ typeName + ", dataType=" + dataType + ", columnSize="
				+ columnSize + ", tableName=" + tableName + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Column) {
			Column t = (Column) obj;
			return t.getName().equalsIgnoreCase(name);
		}
		return super.equals(obj);
	}
}
