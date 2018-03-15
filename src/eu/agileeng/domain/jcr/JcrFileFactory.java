package eu.agileeng.domain.jcr;

import javax.jcr.Node;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEException;

public class JcrFileFactory extends JcrNodeFactory {

	private static JcrFileFactory inst = new JcrFileFactory();
	
	private JcrFileFactory() {
	}

	protected static JcrFileFactory getInstance() {
		return inst;
	}

	@Override
	public JcrNode createNode(Node node) throws AEException {
		JcrFile jcrFile = new JcrFile();
		jcrFile.create(node);
		return jcrFile;
	}

	@Override
	public JcrNode createNode(JSONObject json) throws JSONException {
		JcrFile jcrFile = new JcrFile();
		jcrFile.create(json);
		return jcrFile;
	}
}
