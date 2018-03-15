package eu.agileeng.domain.acc.cashbasis;

import java.util.ArrayList;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEList;

public class AccJournalEntryTeplatesList extends ArrayList<AccJournalEntryTemplate> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = -716747138772116222L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (AccJournalEntryTemplate item : this) {
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
			
			AccJournalEntryTemplate ftt = new AccJournalEntryTemplate();
			ftt.create(jsonItem);
			
			add(ftt);
		}
	}
	
	
	public void assignTo(AEDescriptive financialTransactionTemplate) {
		for (AccJournalEntryTemplate jet : this) {
			jet.setFinancialTransactionTemplate(financialTransactionTemplate.getDescriptor());
		}
	}
}
