package org.dongq;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

public final class Main {

	static final char[] array = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
	
	public static void main(String[] args) {
		int n = 123;
		to2(n);
		System.out.println("");
		to16(n);
		System.out.println("");
		
		System.out.println("a~z: " + (int)'a' + "~" + (int)'z');
		System.out.println("A~Z: " + (int)'A' + "~" + (int)'Z');
		System.out.println("0~9: " + (int)'0' + "~" + (int)'9');
		
		System.out.println("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ: " + array.length);
		
		for(int index = 12345; index < 54321; index++) {
			String s = to36(index);
			to10(s);
		}
		
		//System.out.println((int)Math.pow(2, 2));
	}
	
	static void to10(String n) {
		int result = 0;
		char[] s = StringUtils.reverse(n).toCharArray();
		for(int index = 0; index < s.length; index++) {
			int i = ArrayUtils.indexOf(array, s[index]);
			//result += i * (int)Math.pow(36, index);
			int pow = (int)Math.pow(36, index);
			//System.out.println(i + " : " + pow);
			result += i * pow;
		}
		System.out.println(n + " = " + result);
	}
	
	static String to36(int n) {
		int shang = n / 36;
		int yu = n % 36;
		String s = String.valueOf(array[yu]);
		while(shang != 0) {
			yu = shang % 36;
			shang = shang / 36;
			s = String.valueOf(array[yu]) + s;
		}
		System.out.println(n + " = " + s );
		return s;
	}
	
	static void to2(int n) {
		if(n >= 2) {
			to2(n/2);
		}
		
		System.out.print(n%2);
	}
	
	static void to16(int n) {
		if(n >= 16) {
			to16(n/16);
		}
		if(n%16 < 10) {
			System.out.print(n%16);
		} else {
			
			System.out.print((char)(n%16 + 55));
		}
	}
}
