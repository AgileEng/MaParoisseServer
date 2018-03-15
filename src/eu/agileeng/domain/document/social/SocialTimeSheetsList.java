package eu.agileeng.domain.document.social;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

public class SocialTimeSheetsList extends ArrayList<SocialTimeSheet> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5665733790130092611L;

	public SocialTimeSheetsList() {
	}

	public SocialTimeSheetsList(int initialCapacity) {
		super(initialCapacity);
	}

	public SocialTimeSheetsList(Collection<? extends SocialTimeSheet> c) {
		super(c);
	}

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (SocialTimeSheet sheet : this) {
				jsonArray.put(sheet.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);
			
			SocialTimeSheet sheet = new SocialTimeSheet();
			sheet.create(jsonItem);
			
			add(sheet);
		}
	}
}
