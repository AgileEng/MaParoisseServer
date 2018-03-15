package eu.agileeng.domain;

import java.util.ArrayList;
import java.util.List;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.acc.AccAccountBalance;

public class AccAccountBalancesList extends ArrayList<AccAccountBalance> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = -477836075737784084L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();
		for (AccAccountBalance ab : this) {
			jsonArray.put(ab.toJSONObject());
		}
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		this.clear();
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObj = (JSONObject) jsonArray.get(i);
			AccAccountBalance ab = new AccAccountBalance();
			ab.create(jsonObj);
			add(ab);
		}
	}
	
	public AccAccountBalance getBalance(String accCode) {
		AccAccountBalance ret = null;
		for (AccAccountBalance b : this) {
			if(accCode != null && accCode.equals(b.getAccAccount().getDescriptor().getCode())) {
				ret = b;
				break;
			}
		}
		return ret;
	}
	
	public AccAccountBalancesList getBalances(List<String> accCodes) {
		AccAccountBalancesList ret = new AccAccountBalancesList();
		for (AccAccountBalance b : this) {
			for (String accCode : accCodes) {
				if(accCode.equals(b.getAccAccount().getDescriptor().getCode())) {
					ret.add(b);
				}
			}
		}
		return ret;
	}
}
