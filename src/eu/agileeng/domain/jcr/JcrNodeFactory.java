package eu.agileeng.domain.jcr;

import javax.jcr.Node;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEException;

public abstract class JcrNodeFactory {
	
	public static JcrNodeFactory getInstance(String primaryNodeType) {
		if(JcrNode.JcrNodeType.file.equals(primaryNodeType)) {
			return JcrFileFactory.getInstance();
		} else if(JcrNode.JcrNodeType.folder.equals(primaryNodeType)) {
			return JcrFolderFactory.getInstance();
		}  else if(JcrNode.JcrNodeType.version.equals(primaryNodeType)) {
			return JcrVersionFactory.getInstance();
		} else {
			return JcrUnstructuredFactory.getInstance();
		}
	}
	
	public abstract JcrNode createNode(Node node) throws AEException;
	
	public abstract JcrNode createNode(JSONObject json) throws JSONException;
}
