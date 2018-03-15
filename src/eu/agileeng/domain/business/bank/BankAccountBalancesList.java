package eu.agileeng.domain.business.bank;

import java.util.ArrayList;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

public class BankAccountBalancesList extends ArrayList<BankAccountBalance> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4964521096506302844L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (BankAccountBalance item : this) {
				jsonArray.put(item.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);
			
			BankAccountBalance bb = new BankAccountBalance();
			bb.create(jsonItem);
			
			add(bb);
		}
	}

}
