package eu.agileeng.domain.acc.cashbasis;

import java.util.ArrayList;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

public class FinancialTransactionList extends ArrayList<FinancialTransaction> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8017660716663260189L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (FinancialTransaction item : this) {
				jsonArray.put(item.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);
			
			FinancialTransaction ftt = new FinancialTransaction();
			ftt.create(jsonItem);
			
			add(ftt);
		}
	}
}
