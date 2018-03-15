/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 21.11.2009 13:39:30
 */
package eu.agileeng.domain;

import eu.agileeng.util.AEPredicate;

/**
 *
 */
@SuppressWarnings("serial")
public class EnumTypeClassPredicate implements AEPredicate {

	private DomainModel.DomainClass clazz;
	
	/**
	 * 
	 */
	public EnumTypeClassPredicate(DomainModel.DomainClass domainClass) {
		this.clazz = domainClass;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.util.AEPredicate#evaluate()
	 */
	@Override
	public boolean evaluate(Object o) {
		boolean res = false;
		if(this.clazz != null && o instanceof EnumeratedType) {
			EnumeratedType et = (EnumeratedType) o;
			if(this.clazz.equals(et.getClazz())) {
				res = true;
			}
		}
		return res;
	}

}
