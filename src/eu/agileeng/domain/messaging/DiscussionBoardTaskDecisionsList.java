package eu.agileeng.domain.messaging;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;


public class DiscussionBoardTaskDecisionsList extends ArrayList<DiscussionBoardTaskDecision> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8511813884103070293L;

	public DiscussionBoardTaskDecisionsList() {
		// TODO Auto-generated constructor stub
	}

	public DiscussionBoardTaskDecisionsList(int arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public DiscussionBoardTaskDecisionsList(Collection<? extends DiscussionBoardTaskDecision> arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();
		for (DiscussionBoardTaskDecision d : this) {
			jsonArray.put(d.toJSONObject());
		}
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		this.clear();
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObj = (JSONObject) jsonArray.get(i);
			DiscussionBoardTaskDecision d = new DiscussionBoardTaskDecision();
			d.create(jsonObj);
			add(d);
		}
	}
	
	public JSONObject appendToJSON(JSONObject json) throws JSONException {
		
		for (DiscussionBoardTaskDecision d : this) {
			d.appendToJSON(json);
		}
		
		return json;
	}

}
