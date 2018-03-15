package eu.agileeng.domain.jcr;

import javax.jcr.Item;
import javax.jcr.Node;

import eu.agileeng.domain.AEException;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.security.AuthRole;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.util.AEValidator;

public class StructureChangeValidator implements AEValidator {

	AEInvocationContext invContext = null;
	
	/**
	 * 
	 */
	public StructureChangeValidator(AEInvocationContext invContext) {
		this.invContext = invContext;
	}
	
	@Override
	public void validate(Object o) throws AEException {
		try {
			// validate Item
			if(!(o instanceof Item)) {
				throw new AEException("StructureChangeValidator :: the object is not an instance of javax.jcr.Item");
			}
			Item item = (Item) o;

			// validate Node
			if (!(item instanceof Node)) {
				throw new AEException(item.getPath() + " is not a Node");
			}
			Node node = (Node) item;
			
			// if folder
			if(JcrNode.JcrNodeType.folder.equals(node.getPrimaryNodeType().getName())) {
				// validate Role
				// Create or delete a folder is only for social collaborators. 
				// Customers are only authorized to add new files.
				AuthPrincipal ap = invContext.getAuthPrincipal();
				if(AuthRole.isOperative(ap)) {
					throw new AEException("Vous n'avez pas les droits suffisants pour effectuer cette opération.");
				}
			}
		} catch (Exception e) {
			throw new AEException(e);
		}
	}
}
