package org.dongq.database;

public class Column {

	private int index;

	private String name;

	private String typeName;

	private int dataType;

	private int columnSize;

	public Column(int index, String name, String typeName, int dataType, int columnSize) {
		super();
		this.index = index;
		this.name = name;
		this.typeName = typeName;
		this.dataType = dataType;
		this.columnSize = columnSize;
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

	@Override
	public String toString() {
		return "Column [index=" + index + ", name=" + name + ", typeName="
				+ typeName + ", dataType=" + dataType + ", columnSize="
				+ columnSize + "]";
	}

}
