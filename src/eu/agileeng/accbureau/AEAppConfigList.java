package eu.agileeng.accbureau;

import java.util.ArrayList;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

public class AEAppConfigList extends ArrayList<AEAppConfig> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (AEAppConfig item : this) {
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
			
			AEAppConfig ac = new AEAppConfig();
			ac.create(jsonItem);
			
			add(ac);
		}
	}
	
	public boolean containsModule(AEAppModule appModule) {
		boolean bRet = false;
		if(appModule != null) {
			for (AEAppConfig appConfig : this) {
				if(appConfig.getModule() != null) {
					if(appConfig.getModule().getDescriptor().getID() == appModule.getID()) {
						bRet = true;
						break;
					}
				}
			}
		}
		return bRet;
	}
}
