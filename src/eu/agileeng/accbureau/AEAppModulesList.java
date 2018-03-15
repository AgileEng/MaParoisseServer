package eu.agileeng.accbureau;

import java.util.ArrayList;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

public class AEAppModulesList extends ArrayList<AEAppModule> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (AEAppModule item : this) {
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
			
			AEAppModule am = new AEAppModule();
			am.create(jsonItem);
			
			add(am);
		}
	}
	
	public AEAppModule getById(long appModuleId) {
		AEAppModule retModule = null;
		for (AEAppModule appModule : this) {
			if(appModule.getID() == appModuleId) {
				retModule = appModule;
				break;
			}
		}
		return retModule;
	}
}
