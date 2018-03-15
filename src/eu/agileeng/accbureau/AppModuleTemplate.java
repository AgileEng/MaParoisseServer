package eu.agileeng.accbureau;

import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEPersistent;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEStringUtil;

public class AppModuleTemplate extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3585507492084864291L;
	
	static public enum JSONKey {
		appModuleTemplate,
		appModuleId,
		appModuleNamePath,
		appModuleCodePath,
		appModuleCode,
		validFrom,
		validOn,
		validTo;
	}
	
	private AEDescriptive appModule;
	
	private String appModuleNamePath;
	
	private String appModuleCodePath;
	
	private Date validFrom;
	
	private Date validTo;
	
	/**
	 * @param clazz
	 */
	public AppModuleTemplate() {
		super(DomainClass.AppModuleTemplate);
	}
	
	public AppModuleTemplate(JSONObject jsonObject) throws JSONException {
		this();
		create(jsonObject);
	}
	
	public AppModuleTemplate(AEDescriptive appModule) {
		this();
		setAppModule(appModule);
	}
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.AppModuleTemplate);
	}

	/**
	 * @return the appModule
	 */
	public AEDescriptive getAppModule() {
		return appModule;
	}

	/**
	 * @param appModule the appModule to set
	 */
	public void setAppModule(AEDescriptive appModule) {
		this.appModule = appModule;
	}
	
	public AppModuleTemplate withAppModule(AEDescriptive appModule) {
		setAppModule(appModule);
		return this;
	}

	/**
	 * @return the validFrom
	 */
	public Date getValidFrom() {
		return validFrom;
	}

	/**
	 * @param validFrom the validFrom to set
	 */
	public void setValidFrom(Date validFrom) {
		this.validFrom = validFrom;
	}

	/**
	 * @return the validTo
	 */
	public Date getValidTo() {
		return validTo;
	}

	/**
	 * @param validTo the validTo to set
	 */
	public void setValidTo(Date validTo) {
		this.validTo = validTo;
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject jsonObject = super.toJSONObject();

		if(this.appModule != null) {
			jsonObject.put(JSONKey.appModuleId.name(), this.appModule.getDescriptor().getID());
		}
		
		if(!AEStringUtil.isEmpty(this.appModuleCodePath)) {
			jsonObject.put(JSONKey.appModuleCodePath.name(), getAppModuleCodePath());
		}
		
		if(!AEStringUtil.isEmpty(this.appModuleNamePath)) {
			jsonObject.put(JSONKey.appModuleNamePath.name(), getAppModuleNamePath());
		}
		
		if(this.validFrom != null) {
			jsonObject.put(JSONKey.validFrom.name(), AEDateUtil.formatToSystem(this.validFrom));
		}
		
		if(this.validTo != null) {
			jsonObject.put(JSONKey.validTo.name(), AEDateUtil.formatToSystem(this.validTo));
		}
		
		return jsonObject;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		long moduleId = jsonObject.optLong(JSONKey.appModuleId.name());
		if(AEPersistent.ID.isPersistent(moduleId)) {
			setAppModule(AEAppModule.lazyDescriptor(moduleId));
		}
		
		Date validFrom = AEDateUtil.parseDateStrict(jsonObject.optString(JSONKey.validFrom.name()));
		if(validFrom != null) {
			setValidFrom(validFrom);
		}
		
		Date validTo = AEDateUtil.parseDateStrict(jsonObject.optString(JSONKey.validTo.name()));
		if(validTo != null) {
			setValidTo(validTo);
		}
	}

	/**
	 * @return the appModuleNamePath
	 */
	public String getAppModuleNamePath() {
		return appModuleNamePath;
	}

	/**
	 * @param appModuleNamePath the appModuleNamePath to set
	 */
	public void setAppModuleNamePath(String appModuleNamePath) {
		this.appModuleNamePath = appModuleNamePath;
	}

	/**
	 * @return the appModuleCodePath
	 */
	public String getAppModuleCodePath() {
		return appModuleCodePath;
	}

	/**
	 * @param appModuleCodePath the appModuleCodePath to set
	 */
	public void setAppModuleCodePath(String appModuleCodePath) {
		this.appModuleCodePath = appModuleCodePath;
	}
}
