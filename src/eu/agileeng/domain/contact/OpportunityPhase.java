/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 20.11.2009 15:37:14
 */
package eu.agileeng.domain.contact;

import eu.agileeng.domain.EnumeratedType;
import eu.agileeng.domain.DomainModel.DomainClass;

/**
 *
 */
@SuppressWarnings("serial")
public class OpportunityPhase extends EnumeratedType {

	static public enum Instance {
		NA(0L),
		QUERY_SEARCH(5L),
		LEAD(10L);
		
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
	public OpportunityPhase() {
		super(DomainClass.OPPORTUNITY_PHASE);
	}
}
