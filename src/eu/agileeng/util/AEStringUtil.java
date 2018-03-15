/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 26.11.2009 16:50:02
 */
package eu.agileeng.util;

import java.io.Serializable;

/**
 *
 */
@SuppressWarnings("serial")
public class AEStringUtil implements Serializable {

	public final static String EMPTY_STRING = "";
	
	/**
	 * 
	 */
	private AEStringUtil() {
	}

	public static boolean isEmpty(String inStr) {
		return inStr != null ? inStr.isEmpty() : true;
	}
	
	public static boolean isEmpty(StringBuffer inStr) {
		return inStr != null ? inStr.length() < 1 : true;
	}
	
	public static boolean isEmpty(StringBuilder inStr) {
		return inStr != null ? inStr.length() < 1 : true;
	}
	
	public static String trim(String inStr) {
		return inStr != null ? inStr.trim() : EMPTY_STRING;
	}
	
	public static boolean isEmptyTrimed(String inStr) {
		return inStr != null ? inStr.trim().isEmpty() : true;
	}
	
	/**
	 * Compare two strings even if one or both are null. Null values are considered equal
	 * 
	 * 
	 * @return a equals b
	 * 
	 */
	public static boolean equals(String a, String b) {
		if (a == b) {
			//both are null or the same String
			return true;
		} else if ((a != null) && (b != null)) {
			//compare strings
			return a.equals(b);
		} else {
			//one of them is null
			return false;
		}
	}
	
	public static StringBuilder getStringBuilder(StringBuilder sb) {
		if(sb == null) {
			return new StringBuilder();
		} else {
			sb.delete(0, sb.length());
		    return sb;
		}
	}
}
