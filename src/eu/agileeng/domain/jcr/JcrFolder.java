package eu.agileeng.domain.jcr;

import javax.jcr.Node;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.AEObject;

public class JcrFolder extends JcrHierarchyNode {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6031252544230070686L;

	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		json.put(JcrNode.JSONKey.leaf, false);
		
		return json;
	}
	
	@Override
	protected void create(Node node) throws AEException {
		super.create(node);
		
		if(!hasProperty(AEObject.Property.SYSTEM)) {
			setDescription("Dossier");
		}
	}
}
