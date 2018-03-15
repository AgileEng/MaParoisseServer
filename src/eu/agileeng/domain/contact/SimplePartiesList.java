package eu.agileeng.domain.contact;

import java.util.ArrayList;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

public class SimplePartiesList extends ArrayList<SimpleParty> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3858644212037502132L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (SimpleParty sp : this) {
				jsonArray.put(sp.toJSONObject());
			}
		}

		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);

			SimpleParty sp = new SimpleParty();
			sp.create(jsonItem);

			add(sp);
		}
	}
}
