package eu.agileeng.domain.acc;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.imp.AEDescriptorImp;

public class AccAttribute extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2171040656908809076L;

	protected AccAttribute(DomainClass clazz) {
		super(DomainClass.AccAttriute);
	}
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.AccAttriute);
	}
}
