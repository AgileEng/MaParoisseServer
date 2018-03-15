package eu.agileeng.domain.document;

import java.util.ArrayList;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

public class AEPrintTemplatesList extends ArrayList<AEPrintTemplate> implements AEList {

	private static final long serialVersionUID = -7842254540073777350L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (AEPrintTemplate template : this) {
				jsonArray.put(template.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonDoc = (JSONObject) jsonArray.get(i);
			
			AEPrintTemplate template = new AEPrintTemplate();
			template.create(jsonDoc);
			
			add(template);
		}
	}
}
