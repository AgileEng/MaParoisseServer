package eu.agileeng.domain.jcr;

import javax.jcr.Item;
import javax.jcr.Node;

import eu.agileeng.domain.AEException;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.dmtbox.ejb.DmtboxBean;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AEValidator;

public class NodeProtectedValidator implements AEValidator {

	AEInvocationContext invContext = null;
	
	/**
	 * 
	 */
	public NodeProtectedValidator(AEInvocationContext invContext) {
		this.invContext = invContext;
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

			// validate file or folder
			if(!JcrNode.JcrNodeType.file.equals(node.getPrimaryNodeType().getName())
					&& !JcrNode.JcrNodeType.folder.equals(node.getPrimaryNodeType().getName())) {
				throw new AEException(node.getPath() + " is not a File/Folder");
			}

			// validate not system: JcrNode.JcrProperty.ae_system
			if(!this.invContext.getAuthPrincipal().hasPowerUserRights()) {
				if(node.hasProperty(JcrNode.JcrProperty.ae_system)) {
					javax.jcr.Property isSystemProperty = node.getProperty(JcrNode.JcrProperty.ae_system);
					if(isSystemProperty != null) {
						boolean isSystem = isSystemProperty.getValue().getBoolean();
						if(isSystem) {
							throw new AEException(
									"The System and Locked Nodes are protected and cannot be changed!");
						}
					}
				}
			}

			// validate Role
			// this rule is not valid for Demat's box folder
			String nodePath = AEStringUtil.trim(node.getPath());
			if(!nodePath.contains(DmtboxBean.DEMATBOX) && !nodePath.contains("Dematbox")) {
				if(node.hasProperty(JcrNode.JcrProperty.ae_createdByRole)) {
					javax.jcr.Property createdByRoleProperty = node.getProperty(JcrNode.JcrProperty.ae_createdByRole);
					if(createdByRoleProperty != null) {
						long role = createdByRoleProperty.getValue().getLong();
						if(role < this.invContext.getAuthPrincipal().getMaxRole()) {
							throw new AEException("Vous n'avez pas les droits suffisants pour effectuer cette opération.");
						}
					}
				}
			}
		} catch (Exception e) {
			throw new AEException(e);
		}
	}

}
