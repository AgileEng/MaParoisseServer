package eu.agileeng.domain.document.trade;

import java.util.ArrayList;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

public class AETradeDocumentResultsList extends
		ArrayList<AETradeDocumentResult> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1184600649155946804L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (AETradeDocumentResult result : this) {
				jsonArray.put(result.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);
			
			AETradeDocumentResult result = new AETradeDocumentResult();
			result.create(jsonItem);
			
			add(result);
		}
	}
}
