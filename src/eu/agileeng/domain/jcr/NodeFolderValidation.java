package eu.agileeng.domain.jcr;

import javax.jcr.Item;
import javax.jcr.Node;

import eu.agileeng.domain.AEException;
import eu.agileeng.util.AEValidator;

public class NodeFolderValidation implements AEValidator {
	
	static private final NodeFolderValidation inst = new NodeFolderValidation(); 
	
	/**
	 * 
	 */
	private NodeFolderValidation() {
	}
	
	public final static NodeFolderValidation getInstance() {
		return inst;
	}
	
	@Override
	public void validate(Object o) throws AEException {
		try {
			// validate Item
			if(!(o instanceof Item)) {
				throw new AEException("JcrNodeProtectedValidator :: the object is not an instance of javax.jcr.Item");
			}
			Item item = (Item) o;

			// validate Node
			if (!(item instanceof Node)) {
				throw new AEException(item.getPath() + " is not a Node");
			}
			Node node = (Node) item;

			// validate folder
			if(!JcrNode.JcrNodeType.folder.equals(node.getPrimaryNodeType().getName())) {
				throw new AEException(node.getPath() + " is not a Folder");
			}
		} catch (Exception e) {
			throw new AEException(e);
		}
	}

}
