package eu.agileeng.domain.contact;

import java.util.ArrayList;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

public class ContributorsList extends ArrayList<Contributor> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3858644212037502132L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (Contributor contributor : this) {
				jsonArray.put(contributor.toJSONObject());
			}
		}

		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);

			Contributor contributor = new Contributor();
			contributor.create(jsonItem);

			add(contributor);
		}
	}
}
