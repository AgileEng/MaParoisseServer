/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 18.11.2009 15:38:38
 */
package eu.agileeng.domain.contact;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.util.json.JSONSerializable;

/**
 *
 */
@SuppressWarnings("serial")
public class Contact extends AEDomainObject implements JSONSerializable {
	
	public static final String key_contact = "contact";

	public static final String key_contactId = "contactId";
	
	public static final String key_phone = "phone";
	
	public static final String key_fax = "fax";
	
	public static final String key_email = "email";
	
	public static final String key_mobile = "mobile";
	
	public static final String key_homepage = "homepage";
	
	public static enum Type {
		BUSINESS(10),
		ORGANIZATION(20),
		PRIVATE(30),
		MESSAGING(40);
		
		private long id;
		
		private Type(long id) {
			this.id = id;
		}
		
		public final long getID() {
			return this.id;
		}
		
		public final static Type valueOf(long id) {
			Type ret = null;
			for (Type type : Type.values()) {
				if(type.getID() == id) {
					ret = type;
					break;
				}
			}
			return ret;
		}
	}
	
	private String phone;
	
	private String mobile;
	
	private String eMail;
	
	private String homepage;
	
	private String fax;
	
	private Contact.Type type;
	
	/**
	 * @param clazz
	 */
	public Contact() {
		super(DomainClass.CONTACT);
	}
	
	/**
	 * @param clazz
	 */
	public Contact(Contact.Type type) {
		this();
		this.type = type;
	}

	/**
	 * @return the phone
	 */
	public String getPhone() {
		return phone;
	}

	/**
	 * @param phone the phone to set
	 */
	public void setPhone(String phone) {
		this.phone = phone;
	}

	/**
	 * @return the mobile
	 */
	public String getMobile() {
		return mobile;
	}

	/**
	 * @param mobile the mobile to set
	 */
	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	/**
	 * @return the eMail
	 */
	public String geteMail() {
		return eMail;
	}

	/**
	 * @param eMail the eMail to set
	 */
	public void seteMail(String eMail) {
		this.eMail = eMail;
	}

	/**
	 * @return the homepage
	 */
	public String getHomepage() {
		return homepage;
	}

	/**
	 * @param homepage the homepage to set
	 */
	public void setHomepage(String homepage) {
		this.homepage = homepage;
	}

	/**
	 * @return the fax
	 */
	public String getFax() {
		return fax;
	}

	/**
	 * @param fax the fax to set
	 */
	public void setFax(String fax) {
		this.fax = fax;
	}

	/**
	 * @return the type
	 */
	public Contact.Type getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(Contact.Type type) {
		this.type = type;
	}
	
	public void create() throws JSONException {
		
	}

	/**
	 * Don't change
	 */
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = new JSONObject();

		json.put(key_contactId, getID());
		
		// mail
		json.put(key_email, geteMail());
		
		//phone
		json.put(key_phone, getPhone());
		
		//fax
		json.put(key_fax, getFax());
		
		//mobile
		json.put(key_mobile, getMobile());
		
		//homepage
		json.put(key_homepage, getHomepage());
		
		return json;
	}
	
	/**
	 * Don't change
	 * 
	 * @return
	 * @throws JSONException
	 */
	public JSONObject toJSONObjectExt() throws JSONException {
		JSONObject json = super.toJSONObject();

		json.put(key_contactId, getID());
		
		// mail
		json.put(key_email, geteMail());
		
		//phone
		json.put(key_phone, getPhone());
		
		//fax
		json.put(key_fax, getFax());
		
		//mobile
		json.put(key_mobile, getMobile());
		
		//homepage
		json.put(key_homepage, getHomepage());
		
		return json;
	}

	/**
	 * Don't change
	 */
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		if(jsonObject != null) {
			// id
			if(jsonObject.has(key_contactId)) {
				this.setID(jsonObject.getLong(key_contactId));
			}
			
			// mail
			if(jsonObject.has(key_email)) {
				this.seteMail(jsonObject.getString(key_email));
			}
			
			// phone
			if(jsonObject.has(key_phone)) {
				this.setPhone(jsonObject.getString(key_phone));
			}
			
			// fax
			if(jsonObject.has(key_fax)) {
				this.setFax(jsonObject.getString(key_fax));
			}
			
			//mobile
			if(jsonObject.has(key_mobile)) {
				this.setMobile(jsonObject.optString(key_mobile));
			}
			
			//homepage
			if(jsonObject.has(key_homepage)) {
				this.setHomepage(jsonObject.optString(key_homepage));
			}
		}
	}
	
	/**
	 * Don't change
	 * 
	 * @param jsonObject
	 * @throws JSONException
	 */
	public void createExt(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		this.create(jsonObject);
	}
}
