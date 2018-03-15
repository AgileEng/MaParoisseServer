package eu.agileeng.domain.jcr;

import javax.jcr.Item;
import javax.jcr.Node;

import eu.agileeng.domain.AEException;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.security.AuthRole;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.dmtbox.ejb.DmtboxBean;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AEValidator;

public class DeleteNodeValidator implements AEValidator {

	AEInvocationContext invContext = null;

	/**
	 * 
	 */
	public DeleteNodeValidator(AEInvocationContext invContext) {
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
			
			// this rule is not valid for Demat's box folder
			String nodePath = AEStringUtil.trim(node.getPath());
			if(!nodePath.contains(DmtboxBean.DEMATBOX)) {
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
