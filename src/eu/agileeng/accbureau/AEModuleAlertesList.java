package eu.agileeng.accbureau;

import java.util.ArrayList;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

public class AEModuleAlertesList extends ArrayList<AEModuleAlert> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7730934033616066717L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (AEModuleAlert item : this) {
				jsonArray.put(item.toJSONObject());
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
			
			AEModuleAlert ma = new AEModuleAlert();
			ma.create(jsonItem);
			
			add(ma);
		}
	}
}
