package eu.agileeng.domain.document.trade;

import java.util.ArrayList;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

public class AEDocumentItemsList extends ArrayList<AEDocumentItem> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7589924365730513426L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (AEDocumentItem item : this) {
				jsonArray.put(item.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);
			
			AEDocumentItem item = new AEDocumentItem();
			item.create(jsonItem);
			
			add(item);
		}
	}
}
