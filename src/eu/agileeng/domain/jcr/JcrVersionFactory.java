package eu.agileeng.domain.jcr;

import javax.jcr.Node;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEException;

public class JcrVersionFactory extends JcrNodeFactory {

	private static JcrVersionFactory inst = new JcrVersionFactory();
	
	private JcrVersionFactory() {
	}

	protected static JcrVersionFactory getInstance() {
		return inst;
	}

	@Override
	public JcrNode createNode(Node node) throws AEException {
		JcrVersion jcrVersion = new JcrVersion();
		jcrVersion.create(node);
		return jcrVersion;
	}

	@Override
	public JcrNode createNode(JSONObject json) throws JSONException {
		JcrVersion jcrVersion = new JcrVersion();
		jcrVersion.create(json);
		return jcrVersion;
	}
}
