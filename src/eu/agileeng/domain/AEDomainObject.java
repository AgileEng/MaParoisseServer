/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 09.11.2009 17:19:08
 */
package eu.agileeng.domain;

import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;
import org.jboss.logging.Logger;

import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.domain.imp.AEObjectImp;
import eu.agileeng.persistent.AEPersistent;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.imp.AEPersistentImp;
import eu.agileeng.util.AEValidatable;
import eu.agileeng.util.AEValidator;
import eu.agileeng.util.json.JSONSerializable;


/**
 *
 */
@SuppressWarnings("serial")
public abstract class AEDomainObject implements AEPersistent, AEObject, AEDescriptive, AEValidatable, JSONSerializable {

	protected static Logger logger = Logger.getLogger(AEDomainObject.class);
	
	static public enum JSONKey {
		ownerId,
		sOwnerId,
		compId,
		dbState,
		id,
		code,
		properties,
		name,
		note,
		description,
		active,
		system;
	}
	
	private AEPersistentImp aEPersistent;
	
	private AEObjectImp aEObject;
	
	/**
	 * 
	 */
	protected AEDomainObject(DomainClass clazz) {
		this.aEPersistent = new AEPersistentImp();
		this.aEObject = new AEObjectImp(clazz);
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#getCreator()
	 */
	@Override
	public String getCreator() {
		return aEPersistent.getCreator();
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#getID()
	 */
	@Override
	public long getID() {
		return this.aEPersistent.getID();
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#getModifier()
	 */
	@Override
	public String getModifier() {
		return this.aEPersistent.getModifier();
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#getTimeCreated()
	 */
	@Override
	public Date getTimeCreated() {
		return this.aEPersistent.getTimeCreated();
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#getTimeModified()
	 */
	@Override
	public Date getTimeModified() {
		return this.aEPersistent.getTimeModified();
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#isNew()
	 */
	@Override
	public boolean isNew() {
		return this.aEPersistent.isNew();
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#isPersistent()
	 */
	@Override
	public boolean isPersistent() {
		return this.aEPersistent.isPersistent();
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#isUpdated()
	 */
	@Override
	public boolean isUpdated() {
		return aEPersistent.isUpdated();
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#isView()
	 */
	@Override
	public boolean isView() {
		return this.aEPersistent.isView();
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#setCreator(java.lang.String)
	 */
	@Override
	public void setCreator(String creator) {
		this.aEPersistent.setCreator(creator);
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#setID(long)
	 */
	@Override
	public void setID(long id) {
		this.aEPersistent.setID(id);
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#setModifier(java.lang.String)
	 */
	@Override
	public void setModifier(String modifier) {
		this.aEPersistent.setModifier(modifier);
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#setTimeCreated(java.util.Date)
	 */
	@Override
	public void setTimeCreated(Date timeCreated) {
		this.aEPersistent.setTimeCreated(timeCreated);
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#setTimeModified(java.util.Date)
	 */
	@Override
	public void setTimeModified(Date timeModified) {
		this.aEPersistent.setTimeModified(timeModified);
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#setUpdated()
	 */
	@Override
	public void setUpdated() {
		this.aEPersistent.setUpdated();
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.IAEPersistent#setView()
	 */
	@Override
	public void setView() {
		this.aEPersistent.setView();
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.IAEObject#getAbbreviation()
	 */
	@Override
	public String getAbbreviation() {
		return this.aEObject.getAbbreviation();
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.IAEObject#getAlias()
	 */
	@Override
	public String getAlias() {
		return this.aEObject.getAlias();
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.IAEObject#getCode()
	 */
	@Override
	public String getCode() {
		return this.aEObject.getCode();
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.IAEObject#getDescription()
	 */
	@Override
	public String getDescription() {
		return this.aEObject.getDescription();
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.IAEObject#getName()
	 */
	@Override
	public String getName() {
		return this.aEObject.getName();
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.IAEObject#getOption()
	 */
	@Override
	public String getOption() {
		return this.aEObject.getOption();
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.IAEObject#setAbbreviation(java.lang.String)
	 */
	@Override
	public void setAbbreviation(String abbreviation) {
		this.aEObject.setAbbreviation(abbreviation);
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.IAEObject#setAlias(java.lang.String)
	 */
	@Override
	public void setAlias(String alias) {
		this.aEObject.setAlias(alias);
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.IAEObject#setCode(java.lang.String)
	 */
	@Override
	public void setCode(String code) {
		this.aEObject.setCode(code);
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.IAEObject#setDescription(java.lang.String)
	 */
	@Override
	public void setDescription(String description) {
		this.aEObject.setDescription(description);
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.IAEObject#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		this.aEObject.setName(name);
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.IAEObject#setOption(java.lang.String)
	 */
	@Override
	public void setOption(String option) {
		this.aEObject.setOption(option);
	}
	
	@Override
	public void setClazz(DomainClass clazz) {
		this.aEObject.setClazz(clazz);
	}
	
	@Override
	public DomainClass getClazz() {
		return this.aEObject.getClazz();
	}
	
	public void setNote(String note) {
		this.aEObject.setNote(note);
	}
	
	public String getNote() {
		return aEObject.getNote();
	}
	
	public void setProperties(long properties) {
		aEObject.setProperties(properties);
	}
	
	public long getProperties() {
		return aEObject.getProperties();
	}
	
	public void setProperty(long property) {
		aEObject.setProperty(property);
	}
	
	public void removeProperty(long property) {
		aEObject.removeProperty(property);
	}
	
	public boolean hasProperty(long property) {
		return aEObject.hasProperty(property);
	}
	
	public void setSequenceNumber(long number) {
		aEObject.setSequenceNumber(number);
	}
	
	public long getSequenceNumber() {
		return aEObject.getSequenceNumber();
	}
	
	@Override
	public AEDescriptor getDescriptor() {
		AEDescriptor aeDescr = new AEDescriptorImp(getID(), getClazz());
		createDescriptor(aeDescr);
		return aeDescr;
	}
	
	protected void createDescriptor(AEDescriptor aeDescr) {
		aeDescr.setCode(getCode());
		aeDescr.setName(getName());
		aeDescr.setDescription(getDescription());
	}
	
	@Override
	public AEPersistent.State getPersistentState() {
		return aEPersistent.getPersistentState();
	}
	
	@Override
	public void setPersistentState(AEPersistent.State persistentState) {
		this.aEPersistent.setPersistentState(persistentState);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getClazz() == null) ? 0 : getClazz().hashCode());
		result = prime * result + (int) (getID() ^ (getID() >>> 32));
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
		AEDomainObject other = (AEDomainObject) obj;
		if (getClazz() == null) {
			if (other.getClazz() != null)
				return false;
		} else if (!getClazz().equals(other.getClazz()))
			return false;
		if (getID() != other.getID())
			return false;
		return true;
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.util.AEValidatable#validate(eu.agileeng.util.AEPredicate)
	 */
	@Override
	public void validateWith(AEValidator validator) throws AEException {
		if(validator != null) {
			validator.validate(this);
		}
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEObject#getCompany()
	 */
	@Override
	public AEDescriptive getCompany() {
		return this.aEObject.getCompany();
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEObject#setCompany(eu.agileeng.domain.AEDescriptive)
	 */
	@Override
	public void setCompany(AEDescriptive company) {
		this.aEObject.setCompany(company);
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = new JSONObject();

		if(isView()) {
			json.put(AEDomainObject.JSONKey.dbState.name(), AEPersistentUtil.DB_ACTION_NONE);
		} else if(isNew()) {
			json.put(AEDomainObject.JSONKey.dbState.name(), AEPersistentUtil.DB_ACTION_INSERT);
		} else if(isUpdated()) {
			json.put(AEDomainObject.JSONKey.dbState.name(), AEPersistentUtil.DB_ACTION_UPDATE);
		} else {
			json.put(AEDomainObject.JSONKey.dbState.name(), AEPersistentUtil.DB_ACTION_DELETE);
		}
		json.put(AEDomainObject.JSONKey.id.name(), getID());
		json.put(AEDomainObject.JSONKey.code.name(), getCode());
		json.put(AEDomainObject.JSONKey.properties.name(), getProperties());
		json.put(AEDomainObject.JSONKey.name.name(), getName());
		json.put(AEDomainObject.JSONKey.description.name(), getDescription());
		json.put(AEDomainObject.JSONKey.note.name(), getNote());
		if(getCompany() != null) {
			json.put(AEDomainObject.JSONKey.compId.name(), getCompany().getDescriptor().getID());
			json.put(AEDomainObject.JSONKey.ownerId.name(), getCompany().getDescriptor().getID());
		}
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		if(jsonObject.has(AEDomainObject.JSONKey.dbState.name())) {
			int dbState = jsonObject.getInt(AEDomainObject.JSONKey.dbState.name());
			switch(dbState) {
				case AEPersistentUtil.DB_ACTION_NONE: {
					setPersistentState(AEPersistent.State.VIEW);
					break;
				}
				case AEPersistentUtil.DB_ACTION_INSERT: {
					setPersistentState(AEPersistent.State.NEW);
					break;
				}
				case AEPersistentUtil.DB_ACTION_UPDATE: {
					setPersistentState(AEPersistent.State.UPDATED);
					break;
				}
				case AEPersistentUtil.DB_ACTION_DELETE: {
					setPersistentState(AEPersistent.State.DELETED);
					break;
				}
			}
		}
		
		setID(jsonObject.optLong(AEDomainObject.JSONKey.id.name()));
		setProperties(jsonObject.optLong(AEDomainObject.JSONKey.properties.name()));
		setCode(jsonObject.optString(AEDomainObject.JSONKey.code.name()));
		setName(jsonObject.optString(AEDomainObject.JSONKey.name.name()));
		setDescription(jsonObject.optString(AEDomainObject.JSONKey.description.name()));
		setNote(jsonObject.optString(AEDomainObject.JSONKey.note.name()));
		if(jsonObject.has(AEDomainObject.JSONKey.ownerId.name())) {
			long ownerId = jsonObject.optLong(AEDomainObject.JSONKey.ownerId.name());
			if(ownerId > 0) {
				setCompany(Organization.lazyDescriptor(jsonObject.optLong("ownerId")));
			}
		}
	}
	
	@Override
	public void resetAsNew() {
		this.aEPersistent.resetAsNew();
	}
	
	@Override
	public AEPersistent withTmpId() {
		this.aEPersistent.withTmpId();
		return this;
	}
}
