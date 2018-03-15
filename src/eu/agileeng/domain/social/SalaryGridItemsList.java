package eu.agileeng.domain.social;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.AEList;
import eu.agileeng.util.AEValidatable;
import eu.agileeng.util.AEValidator;

public class SalaryGridItemsList extends ArrayList<SalaryGridItem> implements AEList, AEValidatable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8622184865947321921L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (SalaryGridItem item : this) {
				jsonArray.put(item.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);
			
			SalaryGridItem item = new SalaryGridItem();
			item.create(jsonItem);
			
			add(item);
		}
	}

	@Override
	public void validateWith(AEValidator validator) throws AEException {
		if(validator != null) {
			for (Iterator<SalaryGridItem> iterator = this.iterator(); iterator.hasNext();) {
				SalaryGridItem sgItem = iterator.next();
				sgItem.validateWith(validator);
			}
		}
	}
}
