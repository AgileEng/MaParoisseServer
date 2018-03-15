/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 20.06.2010 12:03:53
 */
package eu.agileeng.domain.business;

import eu.agileeng.domain.EnumeratedType;
import eu.agileeng.domain.DomainModel.DomainClass;

/**
 *
 */
@SuppressWarnings("serial")
public class EmploymentClassification extends EnumeratedType {
	static public enum System {
		NA(0L),
		USER_DEFINED(5),
		EMPLOYEE(10L),
		WORKER(20L),
		FOREMAN(30L),
		CADRE(40L);
		
		private long systemID;
		
		private System(long systemID) {
			this.systemID = systemID;
		}
		
		public final long getSystemID() {
			return this.systemID;
		}
		
		public static System valueOf(long systemID) {
			System ret = null;
			for (System inst : System.values()) {
				if(inst.getSystemID() == systemID) {
					ret = inst;
					break;
				}
			}
			if(ret == null) {
				ret = NA;
			}
			return ret;
		}
	}
	
	/**
	 * @param clazz
	 */
	public EmploymentClassification() {
		super(DomainClass.EmploymentClassification);
	}
	
	public EmploymentClassification(EmploymentClassification.System system) {
		this();
		setSystemID(system.getSystemID());
	}
	
	public EmploymentClassification.System getSystem() {
		return System.valueOf(getSystemID());
	}
}
