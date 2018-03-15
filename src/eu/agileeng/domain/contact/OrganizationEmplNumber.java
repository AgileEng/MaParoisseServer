/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 09.11.2009 17:47:47
 */
package eu.agileeng.domain.contact;

import eu.agileeng.domain.DomainModel;
import eu.agileeng.domain.EnumeratedType;

/**
 *
 */
@SuppressWarnings("serial")
public class OrganizationEmplNumber extends EnumeratedType {

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
	 * 
	 */
	public OrganizationEmplNumber() {
		super(DomainModel.DomainClass.OPGANIZATION_EMPL_NUMBER);
	}
}
