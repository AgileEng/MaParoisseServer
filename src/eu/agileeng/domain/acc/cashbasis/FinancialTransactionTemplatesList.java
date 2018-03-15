package eu.agileeng.domain.acc.cashbasis;

import java.util.ArrayList;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEList;

public class FinancialTransactionTemplatesList extends ArrayList<FinancialTransactionTemplate> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = 717187438896007764L;
	
	public FinancialTransactionTemplatesList() {
	}
	
	public FinancialTransactionTemplatesList createFrom(JSONArray jsonArray) throws JSONException {
		create(jsonArray);
		return this;
	}
	
	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (FinancialTransactionTemplate item : this) {
				jsonArray.put(item.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEList#create(org.apache.tomcat.util.json.JSONArray)
	 */
	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);
			
			FinancialTransactionTemplate ftt = new FinancialTransactionTemplate();
			ftt.create(jsonItem);
			
			add(ftt);
		}
	}
	
	public FinancialTransactionTemplatesList toAppModuleTemplate(AEDescriptive appModuleTemplate) {
		for (FinancialTransactionTemplate ftt : this) {
			ftt.setAppModuleTemplate(appModuleTemplate);
		}
		return this;
	}
}
