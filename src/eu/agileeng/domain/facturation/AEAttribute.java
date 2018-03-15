package eu.agileeng.domain.facturation;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.util.AEValue;

public class AEAttribute extends AEDomainObject {
	
	private AEDescriptive toEntry;
	
	private AEValue.XType valueType;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4307377580597125193L;

	public AEAttribute() {
		super(DomainClass.AEAttribute);
	}

	public AEDescriptive getToEntry() {
		return toEntry;
	}

	public void setToEntry(AEDescriptive toEntry) {
		this.toEntry = toEntry;
	}
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.AEAttribute);
	}

	public AEValue.XType getValueType() {
		return valueType;
	}

	public void setValueType(AEValue.XType valueType) {
		this.valueType = valueType;
	}
}
