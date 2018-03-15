package eu.agileeng.domain.business;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

public class AEPaymentTermsList extends ArrayList<AEPaymentTerms> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6147556798148789644L;
	
	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (AEPaymentTerms pt : this) {
				jsonArray.put(pt.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);
			
			AEPaymentTerms pt = new AEPaymentTerms();
			pt.create(jsonItem);
			
			add(pt);
		}
	}
	
	public AEPaymentTerms getById(long id) {
		AEPaymentTerms found = null;
		for (Iterator<AEPaymentTerms> iterator = this.iterator(); iterator.hasNext();) {
			AEPaymentTerms pt = iterator.next();
			if(pt != null && pt.getID() == id) {
				found = pt;
				break;
			}
		}
		return found;
	}
}
