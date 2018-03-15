package eu.agileeng.domain.facturation;

import java.util.ArrayList;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

public class AEFacturesList extends ArrayList<AEFacture> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8860850583861917081L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (AEFacture facture : this) {
				jsonArray.put(facture.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);
			
			AEFacture facture = new AEFacture();
			facture.create(jsonItem);
			
			add(facture);
		}
	}
}
