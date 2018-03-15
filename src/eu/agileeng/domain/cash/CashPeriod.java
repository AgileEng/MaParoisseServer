/**
 * 
 */
package eu.agileeng.domain.cash;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.acc.AccPeriod;
import eu.agileeng.domain.imp.AEDescriptorImp;

/**
 * @author vvatov
 *
 */
public class CashPeriod extends AccPeriod {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public CashPeriod() {
		super(DomainClass.CashPeriod);
	}
	
	
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.CashPeriod);
	}
}
