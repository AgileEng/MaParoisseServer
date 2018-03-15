/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 18.11.2009 15:30:41
 */
package eu.agileeng.domain.contact;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel;
import eu.agileeng.util.json.JSONSerializable;

/**
 *
 */
@SuppressWarnings("serial")
public class Address extends AEDomainObject implements JSONSerializable{

	public static final String key_addressId = "addressId";
	
	public static final String key_address = "address";
	
	public static final String key_secondaryAddress = "secondaryAddress";
	
	public static final String key_postCode = "postCode";
	
	public static final String key_town = "town";
	
	public static final String key_countryId = "countryId";
	
	private String street;
	
	private String secondaryStreet;
	
	private String postalCode;
	
	private String city;
	
	private String district;
	
	private String state;
	
	private long countryID;
	
	private String pOBox;
	
	private Address.Type type;
	
	public static enum Type {
		BUSINESS(10),
		VISIT(20),
		PRIVATE(30);
		
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
	
	/**
	 * @param type
	 */
	public Address(Address.Type type) {
		this();
		this.type = type;
	}
	
	/**
	 * @param clazz
	 */
	public Address() {
		super(DomainModel.DomainClass.ADDRESS);
	}

	/**
	 * @return the street
	 */
	public String getStreet() {
		return street;
	}

	/**
	 * @param street the street to set
	 */
	public void setStreet(String street) {
		this.street = street;
	}

	/**
	 * @return the postalCode
	 */
	public String getPostalCode() {
		return postalCode;
	}

	/**
	 * @param postalCode the postalCode to set
	 */
	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	/**
	 * @return the city
	 */
	public String getCity() {
		return city;
	}

	/**
	 * @param city the city to set
	 */
	public void setCity(String city) {
		this.city = city;
	}

	/**
	 * @return the district
	 */
	public String getDistrict() {
		return district;
	}

	/**
	 * @param district the district to set
	 */
	public void setDistrict(String district) {
		this.district = district;
	}

	/**
	 * @return the state
	 */
	public String getState() {
		return state;
	}

	/**
	 * @param state the state to set
	 */
	public void setState(String state) {
		this.state = state;
	}

	/**
	 * @return the pOBox
	 */
	public String getpOBox() {
		return pOBox;
	}

	/**
	 * @param pOBox the pOBox to set
	 */
	public void setpOBox(String pOBox) {
		this.pOBox = pOBox;
	}

	/**
	 * @return the type
	 */
	public Address.Type getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(Address.Type type) {
		this.type = type;
	}

	/**
	 * Don't change
	 */
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = new JSONObject();

		// id
		json.put(key_addressId, getID());
		
		// address
		json.put(key_address, getStreet());
		
		//secondaryAddress
		json.put(key_secondaryAddress, getSecondaryStreet());
		
		// postCode
		json.put(key_postCode, getPostalCode());
		
		//town
		json.put(key_town, getCity());
		
		// country
		json.put(key_countryId, getCountryID());
		
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

		// id
		json.put(key_addressId, getID());
		
		// address
		json.put(key_address, getStreet());
		
		//secondaryAddress
		json.put(key_secondaryAddress, getSecondaryStreet());
		
		// postCode
		json.put(key_postCode, getPostalCode());
		
		//town
		json.put(key_town, getCity());
		
		// country
		json.put(key_countryId, getCountryID());
		
		return json;
	}

	/**
	 * Don't change
	 */
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		if(jsonObject != null) {
			// id
			if(jsonObject.has(key_addressId)) {
				setID(jsonObject.getLong(key_addressId));
			}
			
			// address
			if(jsonObject.has(key_address)) {
				setStreet(jsonObject.getString(key_address));
			}
			
			//secondaryAddress
			if(jsonObject.has(key_secondaryAddress)) {
				setSecondaryStreet(jsonObject.getString(key_secondaryAddress));
			}
			
			// postCode
			if(jsonObject.has(key_postCode)) {
				setPostalCode(jsonObject.getString(key_postCode));
			}
			
			//town
			if(jsonObject.has(key_town)) {
				setCity(jsonObject.getString(key_town));
			}
			
			// country
			if(jsonObject.has(key_countryId)) {
				setCountryID(jsonObject.getLong(key_countryId));
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

	public String getSecondaryStreet() {
		return secondaryStreet;
	}

	public void setSecondaryStreet(String secondaryStreet) {
		this.secondaryStreet = secondaryStreet;
	}

	public long getCountryID() {
		return countryID;
	}

	public void setCountryID(long countryID) {
		this.countryID = countryID;
	}
}
