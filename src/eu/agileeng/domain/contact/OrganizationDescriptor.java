/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 12.06.2010 17:36:28
 */
package eu.agileeng.domain.contact;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.DomainModel;
import eu.agileeng.domain.imp.AEDescriptorImp;

/**
 *
 */
@SuppressWarnings("serial")
public class OrganizationDescriptor extends AEDescriptorImp {
	
	private boolean active = true;
	
	/**
	 * 
	 */
	public OrganizationDescriptor(long id) {
		super(id, DomainModel.DomainClass.ORGANIZATION);
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);

		// salutationID
		if(jsonObject.has("active")) {
			this.setActive(jsonObject.optBoolean("active"));
		}
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		json.put("active", isActive());
		
		return json;
	}

	/**
	 * @return the active
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * @param active the active to set
	 */
	public void setActive(boolean active) {
		this.active = active;
	}
}
