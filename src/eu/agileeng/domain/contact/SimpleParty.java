package eu.agileeng.domain.contact;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.imp.AEDescriptorImp;

public class SimpleParty extends AEDomainObject {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1781967889854257562L;

	static public enum JSONKey {
	}
	
	public SimpleParty() {
		super(DomainClass.SimpleParty);
	}
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.SimpleParty);
	}
}
