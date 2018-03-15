package eu.agileeng.domain.jcr;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

public abstract class JcrHierarchyNode extends JcrNode {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6510854667042183771L;

	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		return json;
	}
}
