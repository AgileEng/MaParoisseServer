package eu.agileeng.domain.jcr;

import java.io.File;

import javax.jcr.Node;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.AEObject;

public class JcrFile extends JcrHierarchyNode {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5280477845351285439L;

	private File content;
	
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
			setDescription("Fichier");
		}
	}
	
	public File getContent() {
		return content;
	}

	public void setContent(File content) {
		this.content = content;
	}
}
