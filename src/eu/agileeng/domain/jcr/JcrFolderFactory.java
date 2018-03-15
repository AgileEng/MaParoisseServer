package eu.agileeng.domain.jcr;

import javax.jcr.Node;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEException;

/**
 * Stateless service
 * 
 * @author vvatov
 */
public class JcrFolderFactory extends JcrNodeFactory {
	
	private static JcrFolderFactory inst = new JcrFolderFactory();
	
	private JcrFolderFactory() {
	}

	protected static JcrFolderFactory getInstance() {
		return inst;
	}
	
	@Override
	public JcrNode createNode(Node node) throws AEException {
		JcrFolder jcrFolder = new JcrFolder();
		jcrFolder.create(node);
		return jcrFolder;
	}

	@Override
	public JcrNode createNode(JSONObject json) throws JSONException {
		JcrFolder jcrFolder = new JcrFolder();
		jcrFolder.create(json);
		return jcrFolder;
	}
}
