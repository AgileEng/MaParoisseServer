/**
 * 
 */
package eu.agileeng.domain.cash;

import java.util.ArrayList;
import java.util.Date;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

/**
 * @author vvatov
 *
 */
public class CFCRow extends ArrayList<CFCCell> implements AEList {

	public final static long DATE_ENTRY = 0;
	
	public final static long SUM = 10;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3367096702344500400L;

	private Date date;
	
	private int index = 0;
	
	private long sysId = DATE_ENTRY;
	
	/**
	 * 
	 */
	public CFCRow(Date date, int index) {
		this.date = date;
		this.index = index;
	}
	
	/**
	 * 
	 */
	public CFCRow(long sysId) {
		this.sysId = sysId;
	}

	/**
	 * 
	 */
	public CFCRow() {
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEList#toJSONArray()
	 */
	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (CFCCell cell : this) {
				jsonArray.put(cell.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEList#create(org.apache.tomcat.util.json.JSONArray)
	 */
	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);
			
			CFCCell cell = new CFCCell();
			cell.create(jsonItem);
			
			add(cell);
		}
	}

	public int getIndex() {
		return index;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public long getSysId() {
		return sysId;
	}

	public void setSysId(long sysId) {
		this.sysId = sysId;
	}
	
	public CFCCell getCell(long colId) {
		CFCCell retCell = null;
		for (CFCCell cell : this) {
			if(cell.getColumnId() == colId) {
				retCell = cell;
				break;
			}
		}
		return retCell;
	}
}
