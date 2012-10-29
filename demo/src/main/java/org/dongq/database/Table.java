package org.dongq.database;

import java.util.Set;

import com.google.common.collect.Sets;

public class Table {

	private String name;

	private Set<Column> columns = Sets.newLinkedHashSet();
	
	public Table(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<Column> getColumns() {
		return columns;
	}

	public void setColumns(Set<Column> columns) {
		this.columns = columns;
	}

	public String getSelectSQL() {
		return "select * from " + this.name + " order by "+this.columns.iterator().next().getName()+" asc";
	}
	
	@Override
	public String toString() {
		return "Table [name=" + name + ", columns=" + columns + "]";
	}

}
