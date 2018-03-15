package eu.agileeng.domain.business.bank;

import java.util.ArrayList;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

public class BankRecognitionRulesList extends ArrayList<BankRecognitionRule> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2328732067215000961L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (BankRecognitionRule rule : this) {
				jsonArray.put(rule.toJSONObject());
			}
		}

		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);

			BankRecognitionRule rule = new BankRecognitionRule();
			rule.create(jsonItem);

			add(rule);
		}
	}

}
