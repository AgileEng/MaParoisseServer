/**
 * 
 */
package eu.agileeng.domain.acc;

import java.util.ArrayList;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

/**
 * @author vvatov
 *
 */
public class AccJournalItemsList extends ArrayList<AccJournalItem> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = 531311768254114856L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (AccJournalItem item : this) {
				jsonArray.put(item.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		this.clear();
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObj = (JSONObject) jsonArray.get(i);
			AccJournalItem item = new AccJournalItem();
			item.create(jsonObj);
			add(item);
		}
	}
}
