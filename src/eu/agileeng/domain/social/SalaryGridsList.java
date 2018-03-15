package eu.agileeng.domain.social;

import java.util.ArrayList;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

public class SalaryGridsList extends ArrayList<SalaryGrid> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3956794398315794245L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (SalaryGrid item : this) {
				jsonArray.put(item.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);
			
			SalaryGrid item = new SalaryGrid();
			item.create(jsonItem);
			
			add(item);
		}
	}
}
