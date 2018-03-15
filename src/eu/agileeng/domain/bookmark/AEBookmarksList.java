package eu.agileeng.domain.bookmark;

import java.util.ArrayList;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

public class AEBookmarksList extends ArrayList<AEBookmark> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8633007249474799921L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (AEBookmark bookmark : this) {
				jsonArray.put(bookmark.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);
			
			AEBookmark bookmark = new AEBookmark();
			bookmark.create(jsonItem);
			
			add(bookmark);
		}
	}
}
