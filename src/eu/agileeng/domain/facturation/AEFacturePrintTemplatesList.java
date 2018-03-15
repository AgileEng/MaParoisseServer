package eu.agileeng.domain.facturation;

import java.util.ArrayList;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

public class AEFacturePrintTemplatesList extends ArrayList<AEFacturePrintTemplate> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3498260232720628772L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (AEFacturePrintTemplate t : this) {
				jsonArray.put(t.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);
			
			AEFacturePrintTemplate t = new AEFacturePrintTemplate();
			t.create(jsonItem);
			
			add(t);
		}
	}
}
