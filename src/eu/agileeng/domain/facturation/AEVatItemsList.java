package eu.agileeng.domain.facturation;

import java.util.ArrayList;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;
import eu.agileeng.util.AEMath;

public class AEVatItemsList extends ArrayList<AEVatItem> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1271813104460435358L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (AEVatItem item : this) {
				jsonArray.put(item.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);
			
			AEVatItem item = new AEVatItem();
			item.create(jsonItem);
			
			add(item);
		}
	}
	
	public AEVatItem getById(long vatId) {
		AEVatItem found = null;
		for (AEVatItem item : this) {
			if(item.getID() == vatId) {
				found = item;
				break;
			}
		}
		return found;
	}
	
	public double calculateVatAmount() {
		double vatAmount = 0.0;
		for (AEVatItem item : this) {
			vatAmount += item.getVatAmount();
		}
		return AEMath.round(vatAmount, 2);
	}
}
