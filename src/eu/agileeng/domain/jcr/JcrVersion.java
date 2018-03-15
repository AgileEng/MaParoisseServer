package eu.agileeng.domain.jcr;

import javax.jcr.Node;
import javax.jcr.version.Version;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.AEObject;
import eu.agileeng.util.AEDateUtil;

public class JcrVersion extends JcrNode {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7420277783453929689L;

	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// TODO
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		// TODO
		
		return json;
	}
	
	@Override
	protected void create(Node node) throws AEException {
		super.create(node);

		if(!hasProperty(AEObject.Property.SYSTEM)) {
			setDescription("Version");
		}

		try {
			Version version = (Version) node;
			
			// JcrNode.JSONKey.ae_lastModified
			if(node.hasProperty(JcrNode.JcrProperty.created)) {
				javax.jcr.Property prop = node.getProperty(JcrNode.JcrProperty.created);
				jcrProperties.put(
						JcrNode.JSONKey.ae_lastModified,
						AEDateUtil.formatDateTimeToSystem(prop.getValue().getDate().getTime()));
			}
			
			// JcrNode.JcrProperty.ae_size
			Node frozenNode = version.getFrozenNode();
			if(frozenNode.hasProperty(JcrNode.JcrProperty.ae_size)) {
				javax.jcr.Property size = frozenNode.getProperty(JcrNode.JcrProperty.ae_size);
				jcrProperties.put(
						JcrNode.JcrProperty.ae_size,
						size.getValue().getLong());
			}
			
			// JcrNode.JcrProperty.ae_size
			if(frozenNode.hasProperty(JcrNode.JcrProperty.ae_lastModifiedBy)) {
				javax.jcr.Property prop = frozenNode.getProperty(JcrNode.JcrProperty.ae_lastModifiedBy);
				jcrProperties.put(
						JcrNode.JcrProperty.ae_lastModifiedBy,
						prop.getValue().getString());
			}
			
			if(frozenNode.hasProperty(JcrNode.JcrProperty.description)) {
				javax.jcr.Property prop = frozenNode.getProperty(JcrNode.JcrProperty.description);
				jcrProperties.put(
						JcrNode.JcrProperty.description,
						prop.getValue().getString());
			}
		} catch (Exception e) {
			throw new AEException(e);
		}
	}
}
