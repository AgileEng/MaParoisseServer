package eu.agileeng.domain.messaging;

import java.util.ArrayList;
import java.util.Collection;

import eu.agileeng.domain.AEList;
import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

public class DiscussionBoardPostsList extends ArrayList<DiscussionBoardPost> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3079885233727656602L;

	public DiscussionBoardPostsList() {
		// TODO Auto-generated constructor stub
	}

	public DiscussionBoardPostsList(int initialCapacity) {
		super(initialCapacity);
		// TODO Auto-generated constructor stub
	}

	public DiscussionBoardPostsList(Collection<? extends DiscussionBoardPost> c) {
		super(c);
		// TODO Auto-generated constructor stub
	}

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();
		for (DiscussionBoardPost p : this) {
			jsonArray.put(p.toJSONObject());
		}
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		this.clear();
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObj = (JSONObject) jsonArray.get(i);
			DiscussionBoardPost p = new DiscussionBoardPost();
			p.create(jsonObj);
			add(p);
		}

	}

}
