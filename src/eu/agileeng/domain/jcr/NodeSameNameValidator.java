package eu.agileeng.domain.jcr;

import javax.jcr.Node;

import eu.agileeng.domain.AEException;
import eu.agileeng.util.AEValidator;

public class NodeSameNameValidator implements AEValidator {

	private Node node;

	/**
	 * 
	 */
	public NodeSameNameValidator(Node node) {
		this.node = node;
	}

	@Override
	public void validate(Object o) throws AEException {
		try {
			String name = (String) o;
			if(this.node.hasNode(name)) {
				throw new AEException("The destiation already contains an item named '" + name + "'");
			}
		} catch (Exception e) {
			throw new AEException(e);
		}
	}
}
