/**
 * 
 */
package eu.agileeng.domain.cash;

import java.util.ArrayList;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

/**
 * @author vvatov
 *
 */
public class CFCModel extends ArrayList<CFCColumn> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1647326537007088727L;

	/**
	 * 
	 */
	public CFCModel() {
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEList#toJSONArray()
	 */
	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (CFCColumn column : this) {
				jsonArray.put(column.toJSONObject());
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
			
			CFCColumn col = new CFCColumn();
			col.create(jsonItem);
			
			add(col);
		}
	}
	
	public CFCColumn getColumn(long id) {
		CFCColumn col = null;
		for (CFCColumn cfcColumn : this) {
			if(cfcColumn.getID() == id) {
				col = cfcColumn;
				break;
			}
		}
		return col;
	}
}
