package eu.agileeng.domain.facturation;

import java.util.ArrayList;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

public class AEClientsList extends ArrayList<AEClient> implements AEList {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3499859275612970058L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (AEClient client : this) {
				jsonArray.put(client.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);
			
			AEClient client = new AEClient();
			client.create(jsonItem);
			
			add(client);
		}
	}
}
