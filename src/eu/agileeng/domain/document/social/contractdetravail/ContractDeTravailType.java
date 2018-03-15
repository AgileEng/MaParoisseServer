/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 20.06.2010 12:17:48
 */
package eu.agileeng.domain.document.social.contractdetravail;

import eu.agileeng.domain.EnumeratedType;
import eu.agileeng.domain.DomainModel.DomainClass;

/**
 *
 */
@SuppressWarnings("serial")
public class ContractDeTravailType extends EnumeratedType {

	public static final long TEMPORARY_GROUP_MIN = 10;
	public static final long TEMPORARY_GROUP_MAX = 99;
	
	public static final long PERMANENT_GROUP_MIN = 100;
	public static final long PERMANETT_GROUP_MAX = 199;
	
	static public enum System {
		NA(0L),
		USER_DEFINED(5),
		TEMPORARY_PEAK(9), //subtype of temporary contract (CDD) KEEP the group range!!!
		TEMPORARY_SEASONAL(10), //subtype of temporary contract (CDD) KEEP the group range!!!
		TEMPORARY_REPLACEMENT(11), //subtype of temporary contract (CDD) KEEP the group range!!!
		TEMPORARY(99), //subtype of temporary contract (CDD) KEEP the group range!!!
		PERMANENT(100); //CDI KEEP the group range!!!
		
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
	public ContractDeTravailType() {
		super(DomainClass.ContractDeTravailType);
	}
	
	public ContractDeTravailType(ContractDeTravailType.System system) {
		this();
		setSystemID(system.getSystemID());
	}
	
	public ContractDeTravailType.System getSystem() {
		return System.valueOf(getSystemID());
	}
	
	public boolean isTemporary() {
		return (TEMPORARY_GROUP_MIN <= getSystemID() && getSystemID() <= TEMPORARY_GROUP_MAX);
	}
	
	public boolean isPermanent() {
		return (PERMANENT_GROUP_MIN <= getSystemID() && getSystemID() <= PERMANETT_GROUP_MAX);
	}
}
