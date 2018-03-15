package eu.agileeng.domain.jcr;

import javax.jcr.Node;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEException;

public class JcrUnstructuredFactory extends JcrNodeFactory {

	private static JcrUnstructuredFactory inst = new JcrUnstructuredFactory();
	
	private JcrUnstructuredFactory() {
	}

	protected static JcrUnstructuredFactory getInstance() {
		return inst;
	}
	
	@Override
	public JcrNode createNode(Node node) throws AEException {
		JcrUnstructuredNode jcrNode = new JcrUnstructuredNode();
		jcrNode.create(node);
		return jcrNode;
	}

	@Override
	public JcrNode createNode(JSONObject json) throws JSONException {
		JcrUnstructuredNode jcrNode = new JcrUnstructuredNode();
		jcrNode.create(json);
		return jcrNode;
	}
}
