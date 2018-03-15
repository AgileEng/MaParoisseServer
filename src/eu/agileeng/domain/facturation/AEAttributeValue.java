package eu.agileeng.domain.facturation;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.util.AEValue;

public class AEAttributeValue extends AEDomainObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private AEDescriptive attribute;
	
	private AEValue value;

	public AEAttributeValue() {
		super(DomainClass.AEAttributeValue);
	}
	
	public AEValue getValue() {
		return value;
	}

	public void setValue(AEValue value) {
		this.value = value;
	}

	public AEDescriptive getAttribute() {
		return attribute;
	}

	public void setAttribute(AEDescriptive attribute) {
		this.attribute = attribute;
	}
}
