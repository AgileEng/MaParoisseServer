package eu.agileeng.domain.document;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.util.AEStringUtil;

public class AEPrintTemplate extends AEDomainObject {

	private static final long serialVersionUID = -1243284631090922233L;

	static public enum JsonKey {
		docType,
		templateURL,
		logoURL
	}
	
	private AEDocumentType docType;

	private String templateURL;

	private String logoURL;
	
	public AEPrintTemplate() {
		super(DomainClass.AEPrintTemplate);
	}

	public AEDocumentType getDocType() {
		return docType;
	}

	public void setDocType(AEDocumentType docType) {
		this.docType = docType;
	}

	public String getTemplateURL() {
		return templateURL;
	}

	public void setTemplateURL(String templateURL) {
		this.templateURL = templateURL;
	}

	public String getLogoURL() {
		return logoURL;
	}

	public void setLogoURL(String logoURL) {
		this.logoURL = logoURL;
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		// docType
		if(getDocType() != null) {
			json.put(AEPrintTemplate.JsonKey.docType.toString(), getDocType().getSystemID());
		}
		
		// templateURL
		if(!AEStringUtil.isEmpty(getTemplateURL())) {
			json.put(AEPrintTemplate.JsonKey.templateURL.toString(), getTemplateURL());
		}
			
		// logoURL
		if(!AEStringUtil.isEmpty(getLogoURL())) {
			json.put(AEPrintTemplate.JsonKey.logoURL.toString(), getLogoURL());
		}
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// docType
		if(jsonObject.has(AEPrintTemplate.JsonKey.docType.toString())) {
			long docTypeId = jsonObject.optLong(AEPrintTemplate.JsonKey.docType.toString());
			if(docTypeId > 0) {
				setDocType(AEDocumentType.valueOf(docTypeId));
			}
		}
		
		// templateURL
		if(jsonObject.has(AEPrintTemplate.JsonKey.templateURL.toString())) {
			String templateURL = jsonObject.optString(AEPrintTemplate.JsonKey.templateURL.toString());
			if(!AEStringUtil.isEmpty(templateURL)) {
				setTemplateURL(templateURL);
			}
		}
			
		// logoURL
		if(jsonObject.has(AEPrintTemplate.JsonKey.logoURL.toString())) {
			String logoURL = jsonObject.optString(AEPrintTemplate.JsonKey.logoURL.toString());
			if(!AEStringUtil.isEmpty(logoURL)) {
				setLogoURL(logoURL);
			}
		}
	}
}
