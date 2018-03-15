/**
 * 
 */
package eu.agileeng.domain.jcr;

import java.util.ArrayList;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

/**
 * @author vvatov
 *
 */
public class JcrNodeList extends ArrayList<JcrNode> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = 29564421332196756L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (JcrNode jcrNode : this) {
				jsonArray.put(jcrNode.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonNode = (JSONObject) jsonArray.get(i);
			JcrNodeFactory nodeFactory = JcrNodeFactory.getInstance(jsonNode.optString("primaryNodeType"));
			JcrNode jcrNode = nodeFactory.createNode(jsonNode);
			add(jcrNode);
		}
	}
}
