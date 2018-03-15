package eu.agileeng.domain.messaging;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;
import eu.agileeng.domain.AEList;


public class DiscussionBoardTasksList extends ArrayList<DiscussionBoardTask> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2524800015023290893L;

	public DiscussionBoardTasksList() {
		// TODO Auto-generated constructor stub
	}

	public DiscussionBoardTasksList(int arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public DiscussionBoardTasksList(
			Collection<? extends DiscussionBoardTask> arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();
		for (DiscussionBoardTask t : this) {
			jsonArray.put(t.toJSONObject());
		}
		
		
		return jsonArray;
	}
	
	public JSONObject toMetaData() throws JSONException {
		JSONObject json = new JSONObject();
		
		if (this.size() > 0) {
			json.put("columns", this.get(0).genDynamicColumnsModel());
			json.put("results", this.toJSONArray());
			
			JSONObject metaData = new JSONObject();
			
			metaData.put("fields", this.get(0).genDynamicFieldsModel());
			metaData.put("root", "results");
			
			json.put("metaData", metaData);
		}
		
		return json;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		this.clear();
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObj = (JSONObject) jsonArray.get(i);
			DiscussionBoardTask t = new DiscussionBoardTask();
			t.create(jsonObj);
			add(t);
		}
	}

}
