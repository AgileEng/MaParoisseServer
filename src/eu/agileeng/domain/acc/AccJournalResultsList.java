package eu.agileeng.domain.acc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEList;

public class AccJournalResultsList extends ArrayList<AccJournalResult> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1184600649155946804L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (AccJournalResult result : this) {
				jsonArray.put(result.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);
			
			AccJournalResult result = new AccJournalResult();
			result.create(jsonItem);
			
			add(result);
		}
	}
	
	public final Set<AEDescriptor> getAccJournalEntryDescriptors() {
		Set<AEDescriptor> set = new HashSet<AEDescriptor>();
		for (AccJournalResult result : this) {
			if(result.getAccJournalItem() != null) {
				set.add(AccJournalEntry.lazyDescriptor(result.getAccJournalItem().getEntryId()));
			}
		}
		return set;
	}
}
