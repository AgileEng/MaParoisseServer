package eu.agileeng.domain.jcr;

import javax.jcr.Node;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.AEObject;

public class JcrUnstructuredNode extends JcrNode {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8622679988527590427L;

	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// TODO
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		
		
		return json;
	}
	
	@Override
	protected void create(Node node) throws AEException {
		super.create(node);
		
		setProperty(AEObject.Property.SYSTEM);
		setDescription("Système");
	}
}
