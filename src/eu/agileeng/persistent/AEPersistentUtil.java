/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 26.11.2009 19:45:43
 */
package eu.agileeng.persistent;

import java.util.Date;
import java.util.List;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDescriptorsList;
import eu.agileeng.util.AECollectionUtil;
import eu.agileeng.util.AEDateUtil;

/**
 *
 */
public class AEPersistentUtil {

	public static final long NEW_ID = -1L; 
	
	public static final int DB_ACTION_NONE = 0; 
	
	public static final int DB_ACTION_INSERT = 1; 
	
	public static final int DB_ACTION_UPDATE = 2; 
	
	public static final int DB_ACTION_DELETE = 3; 
	
	private static volatile long tmpID = -999999999;

	private AEPersistentUtil() {
	}

	public static java.sql.Timestamp getTimestamp(java.util.Date date) {
		return date != null ? new java.sql.Timestamp(date.getTime()) : null;
	}
	
	public static java.sql.Timestamp getTimestampNotNull(java.util.Date date) {
		return new java.sql.Timestamp(date != null ? date.getTime() : (new java.util.Date()).getTime());
	}	
	
	public static java.sql.Date getSQLDate(java.util.Date date) {
		return date != null ? new java.sql.Date(date.getTime()) : null;
	}
	
	public static int getIntValue(boolean b) {
		return b ? 1 : 0;
	}
	
	public static String escapeToDate(Date date) {
		StringBuffer sb = new StringBuffer();
		sb
			.append("{ d \'")
			.append(AEDateUtil.convertToString(date, AEDateUtil.DB_DATE_FORMAT))
			.append("\' }");
		return sb.toString();
	}
	
	public synchronized static long getTmpID() {
		if(tmpID == 0) {
			tmpID = Long.MIN_VALUE;
		}
		tmpID = tmpID + 1;
		return tmpID;
	}
	
	public static String createInClause(AEDescriptorsList descrList) {
		StringBuffer sb = new StringBuffer();
		if(!AECollectionUtil.isEmpty(descrList)) {
			sb.append(" (");
			boolean first = true;
			for (AEDescriptor aeDescriptor : descrList) {
				if(first) {
					first = false;
					sb.append(aeDescriptor.getID());
				} else {
					sb.append(", ").append(aeDescriptor.getID());
				}
			}
			sb.append(") ");
		}
		return sb.toString();
	}
	
	public static String createInClause(List<Long> idList) {
		StringBuffer sb = new StringBuffer();
		if(!AECollectionUtil.isEmpty(idList)) {
			sb.append(" (");
			boolean first = true;
			for (Long id : idList) {
				if(first) {
					first = false;
					sb.append(id);
				} else {
					sb.append(", ").append(id);
				}
			}
			sb.append(") ");
		}
		return sb.toString();
	}
}
