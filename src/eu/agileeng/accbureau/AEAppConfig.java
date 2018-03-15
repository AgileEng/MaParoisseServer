/**
 * 
 */
package eu.agileeng.accbureau;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.imp.AEDescriptorImp;

/**
 * @author vvatov
 *
 */
public class AEAppConfig extends AEDomainObject {	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private AEDescriptive module;
	
	/**
	 * @param clazz
	 */
	public AEAppConfig() {
		super(DomainClass.AEAppConfig);
	}
	
	public AEAppConfig(AEDescriptive module) {
		this();
		setModule(module);
	}

	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.AEAppConfig);
	}

	public AEDescriptive getModule() {
		return module;
	}

	public void setModule(AEDescriptive module) {
		this.module = module;
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();

		if(getModule() != null) {
			json.put("moduleId", getModule().getDescriptor().getID());
			json.put("moduleCode", getModule().getDescriptor().getCode());
			json.put("moduleName", getModule().getDescriptor().getName());
		}
		
		json.put("properties", getProperties());
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		if(jsonObject.has("moduleId")) {
			AEDescriptor moduleDescr = AEAppConfig.lazyDescriptor(jsonObject.optLong("moduleId"));
			setModule(moduleDescr);
		}
	}
}
