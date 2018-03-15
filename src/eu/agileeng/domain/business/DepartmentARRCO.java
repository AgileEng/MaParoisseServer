/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 19.06.2010 13:48:53
 */
package eu.agileeng.domain.business;

import eu.agileeng.domain.EnumeratedType;
import eu.agileeng.domain.DomainModel.DomainClass;

/**
 *
 */
@SuppressWarnings("serial")
public class DepartmentARRCO extends EnumeratedType {

	static public enum System {
		NA(0L),
		USER_DEFINED(5),
		AUCUNE(10L);
		
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
	public DepartmentARRCO() {
		super(DomainClass.DepartmentARRCO);
	}
}
