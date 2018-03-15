package eu.agileeng.domain.acc.cashbasis;

import java.util.ArrayList;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEList;
import eu.agileeng.util.AEStringUtil;

public class QuetesList extends ArrayList<Quete> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2367247202144125641L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (Quete item : this) {
				jsonArray.put(item.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);
			
			Quete q = new Quete();
			q.create(jsonItem);
			
			add(q);
		}
	}
	
	public QuetesList get(Quete.Type type, String code) {
		QuetesList qList = new QuetesList();
		if(type != null) {
			for (Quete quete : this) {
				if(type.equals(quete.getType()) && AEStringUtil.equals(AEStringUtil.trim(code), AEStringUtil.trim(quete.getCode()))) {
					qList.add(quete);
				}
			}
		}
		return qList;
	}
	
	public void propagateFinancialTransaction(AEDescriptive finTrans) {
		for (Quete q : this) {
			q.setFinancialTransaction(finTrans.getDescriptor());
		}
	}
}
