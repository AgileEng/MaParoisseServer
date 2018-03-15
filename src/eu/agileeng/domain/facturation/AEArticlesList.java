package eu.agileeng.domain.facturation;

import java.util.ArrayList;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

public class AEArticlesList extends ArrayList<AEArticle> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7423929865156319903L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (AEArticle article : this) {
				jsonArray.put(article.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);
			
			AEArticle article = new AEArticle();
			article.create(jsonItem);
			
			add(article);
		}
	}
}
