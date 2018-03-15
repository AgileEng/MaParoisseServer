/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 20.11.2009 15:41:55
 */
package eu.agileeng.domain.contact;

import eu.agileeng.domain.EnumeratedType;
import eu.agileeng.domain.DomainModel.DomainClass;

/**
 *
 */
@SuppressWarnings("serial")
public class OpportunityStatus extends EnumeratedType {

	static public enum Instance {
		NA(0L);
		
		private long id;
		
		private Instance(long id) {
			this.id = id;
		}
		
		public final long getID() {
			return this.id;
		}
	}
	
	/**
	 * @param clazz
	 */
	public OpportunityStatus() {
		super(DomainClass.OPPORTUNITY_STATUS);
	}
}
