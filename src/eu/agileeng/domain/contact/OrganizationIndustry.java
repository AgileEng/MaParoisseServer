/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 18.11.2009 17:13:26
 */
package eu.agileeng.domain.contact;

import eu.agileeng.domain.EnumeratedType;
import eu.agileeng.domain.DomainModel.DomainClass;

/**
 * Keeps data for ACTIVITE and CODE_ACTIVITE
 */
@SuppressWarnings("serial")
public class OrganizationIndustry extends EnumeratedType {

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
	public OrganizationIndustry() {
		super(DomainClass.OPGANIZATION_INDYSTRY);
	}

}
