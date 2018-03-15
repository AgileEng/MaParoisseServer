/**
 * 
 */
package eu.agileeng.accbureau;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.imp.AEDescriptorImp;

/**
 * @author vvatov
 *
 */
public class AEAppModule extends AEDomainObject {
	
	public static final String SECURITY = "System/Security";
	
	
	/**
	 * 
	 * 
	 * @author vvatov
	 */
	static public enum AppModuleRelation {
		NA(0L),
		FROM_IS_TEMPLATE_TO(10L);

		private long id;

		private AppModuleRelation(long id) {
			this.id = id;
		}

		public final long getId() {
			return this.id;
		}

		public static AppModuleRelation valueOf(long id) {
			AppModuleRelation ret = null;
			for (AppModuleRelation inst : AppModuleRelation.values()) {
				if(inst.getId() == id) {
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
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static public enum JSONKey {
		appModuleId,
		appModuleCode,
		appModuleNamePath,
		appModuleCodePath;
	}
	
	/**
	 * @param clazz
	 */
	public AEAppModule() {
		super(DomainClass.AEAppModule);
	}
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.AEAppModule);
	}
}
