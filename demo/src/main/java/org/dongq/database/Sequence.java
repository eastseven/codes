package org.dongq.database;

import java.math.BigInteger;

/*
 SQL> desc all_sequences;
 Name					   Null?    Type
 ----------------------------------------- -------- ----------------------------
 SEQUENCE_OWNER 			   NOT NULL VARCHAR2(30)
 SEQUENCE_NAME				   NOT NULL VARCHAR2(30)
 MIN_VALUE					    NUMBER
 MAX_VALUE					    NUMBER
 INCREMENT_BY				   NOT NULL NUMBER
 CYCLE_FLAG					    VARCHAR2(1)
 ORDER_FLAG					    VARCHAR2(1)
 CACHE_SIZE				   NOT NULL NUMBER
 LAST_NUMBER				   NOT NULL NUMBER 

 select SEQUENCE_OWNER||','||SEQUENCE_NAME||','||MIN_VALUE||','||MAX_VALUE||','||INCREMENT_BY||','||LAST_NUMBER from all_sequences;
 */
public class Sequence {

	private String name;
	private int minValue;
	private BigInteger maxValue;
	private int incrementBy;
	private int cacheSize;

	private int lastNumber;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getMinValue() {
		return minValue;
	}

	public void setMinValue(int minValue) {
		this.minValue = minValue;
	}

	public BigInteger getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(BigInteger maxValue) {
		this.maxValue = maxValue;
	}

	public int getIncrementBy() {
		return incrementBy;
	}

	public void setIncrementBy(int incrementBy) {
		this.incrementBy = incrementBy;
	}

	public int getCacheSize() {
		return cacheSize;
	}

	public void setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
	}

	public int getLastNumber() {
		return lastNumber;
	}

	public void setLastNumber(int lastNumber) {
		this.lastNumber = lastNumber;
	}

	public String getFileName() {
		return this.name + ".sql";
	}
	
	public String getScript() {
		String script = "create sequence";
		
		script += " " + this.name;
		script += " minvalue " + this.minValue;
		script += " maxvalue " + this.maxValue.toString();
		script += " start with " + (this.lastNumber + 2);//避免id重复，以防万一
		script += " increment by " + this.incrementBy;
		script += " cache " + this.cacheSize + ";";
		return script;
	}
	
	@Override
	public String toString() {
		return "Sequence [name=" + name + ", minValue=" + minValue
				+ ", maxValue=" + maxValue + ", incrementBy=" + incrementBy
				+ ", cacheSize=" + cacheSize + ", lastNumber=" + lastNumber
				+ "]";
	}

}
