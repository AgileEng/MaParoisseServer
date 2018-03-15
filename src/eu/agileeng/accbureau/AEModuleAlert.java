package eu.agileeng.accbureau;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.imp.AEDescriptorImp;

public class AEModuleAlert extends AEDomainObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3752093845273272134L;

	static public class JSONKey {
		public static final String delay = "delay";
		public static final String sIndex = "sIndex";
		
		public static final String moduleAlerts = "moduleAlerts";
	}
	
	private AEDescriptive module;
	
	/**
	 * Delay in days
	 */
	private int delay;
	
	/**
	 * @param clazz
	 */
	public AEModuleAlert() {
		super(DomainClass.AEModuleAlert);
	}
	
	public AEModuleAlert(AEDescriptive module) {
		this();
		setModule(module);
	}

	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.AEModuleAlert);
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
		
		json.put(AEModuleAlert.JSONKey.delay, getDelay());
		
		json.put(AEModuleAlert.JSONKey.sIndex, getSequenceNumber());
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		if(jsonObject.has("moduleId")) {
			AEDescriptor moduleDescr = AEModuleAlert.lazyDescriptor(jsonObject.optLong("moduleId"));
			moduleDescr.setCode(jsonObject.optString("moduleCode"));
			moduleDescr.setName(jsonObject.optString("moduleName"));
			setModule(moduleDescr);
		}
		
		if(jsonObject.has(AEModuleAlert.JSONKey.delay)) {
			setDelay(jsonObject.optInt(AEModuleAlert.JSONKey.delay));
		}
		
		if(jsonObject.has(AEModuleAlert.JSONKey.sIndex)) {
			setSequenceNumber(jsonObject.optLong(AEModuleAlert.JSONKey.sIndex));
		}
	}

	public int getDelay() {
		return delay;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}
}
