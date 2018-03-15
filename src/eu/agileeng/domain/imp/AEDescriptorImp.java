/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 21.11.2009 16:39:53
 */
package eu.agileeng.domain.imp;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.DomainModel.DomainClass;

/**
 *
 */
@SuppressWarnings("serial")
public class AEDescriptorImp implements AEDescriptor {

	private long id;
	
	private long sysId;
	
	private DomainClass clazz;
	
	private String name;
	
	private String code;
	
	private String description;
	
	private long properties;
	
	/**
	 * 
	 */
	public AEDescriptorImp() {
	}
	
	public AEDescriptorImp(long id, DomainClass clazz) {
		this.clazz = clazz;
		this.id = id;
	}
	
	public AEDescriptorImp(long id) {
		this.id = id;
	}

	/**
	 * @return the iD
	 */
	public long getID() {
		return id;
	}

	/**
	 * @param iD the iD to set
	 */
	public void setID(long id) {
		this.id = id;
	}

	/**
	 * @return the clazz
	 */
	public DomainClass getClazz() {
		return clazz;
	}

	/**
	 * @param clazz the clazz to set
	 */
	public void setClazz(DomainClass clazz) {
		this.clazz = clazz;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEDescriptor#getCode()
	 */
	@Override
	public String getCode() {
		return code;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEDescriptor#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEDescriptor#setCode(java.lang.String)
	 */
	@Override
	public void setCode(String code) {
		this.code = code;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEDescriptor#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AEDescriptorImp other = (AEDescriptorImp) obj;
		if (clazz == null) {
			if (other.clazz != null)
				return false;
		} else if (!clazz.equals(other.clazz))
			return false;
		if (id != other.id)
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEDescriptor#isPersistent()
	 */
	@Override
	public boolean isPersistent() {
		return getID() > 0;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEDescriptive#getDescriptor()
	 */
	@Override
	public AEDescriptor getDescriptor() {
		return this;
	}

	/**
	 * @return the properties
	 */
	public long getProperties() {
		return properties;
	}

	/**
	 * @param properties the properties to set
	 */
	public void setProperties(long properties) {
		this.properties = properties;
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = new JSONObject();
		
		json.put("id", getID());
		json.put("code", getCode());
		json.put("name", getName());
		json.put("description", getDescription());
		json.put("properties", getProperties());
		json.put("sysId", getSysId());
		
		return json;
	}

	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		setID(jsonObject.optLong("id"));
		setCode(jsonObject.optString("code"));
		setName(jsonObject.optString("name"));
		setDescription(jsonObject.optString("description"));
		setSysId(jsonObject.optLong("sysId"));
	}

	@Override
	public void setSysId(long sysId) {
		this.sysId = sysId;
	}

	@Override
	public long getSysId() {
		return this.sysId;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}
	
	public AEDescriptor withName(String name) {
		setName(name);
		return this;
	}
	
	public AEDescriptorImp withDescriptor(String descriptor) {
		setDescription(descriptor);
		return this;
	}

	@Override
	public AEDescriptor withCode(String code) {
		setCode(code);
		return this;
	}

	@Override
	public AEDescriptor withId(long id) {
		setID(id);
		return this;
	}

	@Override
	public AEDescriptor withDescription(String description) {
		setDescription(description);
		return this;
	}
}
