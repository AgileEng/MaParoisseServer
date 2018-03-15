package eu.agileeng.domain.contact;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.util.json.JSONUtil;

/**
 * Represents an Organization's template (attribute container) used for 
 * Organization's building.
 * 
 * @author vvatov
 *
 */
public class OrganizationTemplate extends AEDomainObject {
	
	private static final long serialVersionUID = -6437646474951376066L;
	
	static public enum JSONKey {
		postCode,
		town,
		nature,
		statut;
	}
	
	/**
	 * Attributes are organized as JSON object for easily adding new ones.
	 * Always <code>not null</code>.
	 */
	private JSONObject attributes = new JSONObject();
	
	protected OrganizationTemplate(DomainClass clazz) {
		super(clazz);
	}

	public OrganizationTemplate() {
		this(DomainModel.DomainClass.ORGANIZATION_TEMPLATE);
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();

		JSONUtil.apply(json, attributes);
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// postalCode
		String postalCode = jsonObject.optString(OrganizationTemplate.JSONKey.postCode.name());
		if(postalCode != null) {
			getAttributes().put(jsonObject.optString(OrganizationTemplate.JSONKey.postCode.name()), postalCode);
		}
		
		// city
		String city = jsonObject.optString(OrganizationTemplate.JSONKey.town.name());
		if(city != null) {
			getAttributes().put(jsonObject.optString(OrganizationTemplate.JSONKey.town.name()), city);
		}
		
		// nature
		String nature = jsonObject.optString(OrganizationTemplate.JSONKey.nature.name());
		if(nature != null) {
			getAttributes().put(jsonObject.optString(OrganizationTemplate.JSONKey.nature.name()), nature);
		}
		
		// statut
		String statut = jsonObject.optString(OrganizationTemplate.JSONKey.statut.name());
		if(statut != null) {
			getAttributes().put(jsonObject.optString(OrganizationTemplate.JSONKey.statut.name()), nature);
		}
	}

	/**
	 * @return the not <code>null</code> attributeHolder
	 */
	public JSONObject getAttributes() {
		return attributes;
	}
}
