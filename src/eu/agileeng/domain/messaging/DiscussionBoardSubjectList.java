package eu.agileeng.domain.messaging;

import java.util.ArrayList;
import java.util.Collection;

import eu.agileeng.domain.AEList;
//import eu.agileeng.domain.request.RequestResult;
import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

public class DiscussionBoardSubjectList extends ArrayList<DiscussionBoardSubject> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1357947263538065215L;

	public DiscussionBoardSubjectList() {
		// TODO Auto-generated constructor stub
	}

	public DiscussionBoardSubjectList(int arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public DiscussionBoardSubjectList(Collection<? extends DiscussionBoardSubject> arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();
		for (DiscussionBoardSubject s : this) {
			jsonArray.put(s.toJSONObject());
		}
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		this.clear();
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObj = (JSONObject) jsonArray.get(i);
			DiscussionBoardSubject s = new DiscussionBoardSubject();
			s.create(jsonObj);
			add(s);
		}
	}

}
