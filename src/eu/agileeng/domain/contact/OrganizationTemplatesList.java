package eu.agileeng.domain.contact;

import java.util.ArrayList;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

public class OrganizationTemplatesList extends ArrayList<OrganizationTemplate> implements AEList {

	private static final long serialVersionUID = 7573098293110552071L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (OrganizationTemplate item : this) {
				jsonArray.put(item.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);
			
			OrganizationTemplate ot = new OrganizationTemplate();
			ot.create(jsonItem);
			
			add(ot);
		}
	}

}
