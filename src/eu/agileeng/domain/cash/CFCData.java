/**
 * 
 */
package eu.agileeng.domain.cash;

import java.util.ArrayList;
import java.util.Date;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;

import eu.agileeng.domain.AEList;
import eu.agileeng.util.AEDateUtil;

/**
 * @author vvatov
 *
 */
public class CFCData extends ArrayList<CFCRow> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5091617312512488289L;

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEList#toJSONArray()
	 */
	@Override
	public JSONArray toJSONArray() throws JSONException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEList#create(org.apache.tomcat.util.json.JSONArray)
	 */
	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		// TODO Auto-generated method stub

	}
	
	public CFCRow getRow(Date date, int rowIndex) {
		CFCRow retRow = null;
		for (CFCRow cfcRow : this) {
			if(cfcRow.getIndex() == rowIndex
					&& AEDateUtil.areDatesEqual(cfcRow.getDate(), date)) {
				
				retRow = cfcRow;
				break;
			}
		}
		return retRow;
	}
	
	public CFCRow getRow(long sysId) {
		CFCRow retRow = null;
		for (CFCRow cfcRow : this) {
			if(cfcRow.getSysId() == sysId) {
				retRow = cfcRow;
				break;
			}
		}
		return retRow;
	}
	
	public CFCCell getCell(int rowIndex, int colIndex) {
		CFCCell retCell = null;
		
		for (CFCRow cfcRow : this) {
			for (CFCCell cfcCell : cfcRow) {
				if(cfcCell.getRowIndex() == rowIndex && cfcCell.getColumn().getColIndex() == colIndex) {
					retCell = cfcCell;
					break;
				}
			}
		}
		
		return retCell;
	}
	
	public CFCCell getCellBySysId(long sysId, int colIndex) {
		CFCCell retCell = null;
		
		for (CFCRow cfcRow : this) {
			for (CFCCell cfcCell : cfcRow) {
				if(cfcRow.getSysId() == sysId && cfcCell.getColumn().getColIndex() == colIndex) {
					retCell = cfcCell;
					break;
				}
			}
		}
		
		return retCell;
	}
}
