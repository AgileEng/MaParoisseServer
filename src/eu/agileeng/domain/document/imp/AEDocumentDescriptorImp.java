/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 23.05.2010 19:02:28
 */
package eu.agileeng.domain.document.imp;

import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.DomainModel;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.document.AEDocumentDescriptor;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEStringUtil;

/**
 *
 */
@SuppressWarnings("serial")
public class AEDocumentDescriptorImp implements AEDocumentDescriptor {

	private AEDescriptor descr;
	
	private Date date;
	
	private String number; 
	
	private AEDocumentType docType;
	
	/**
	 * 
	 */
	public AEDocumentDescriptorImp() {
		this(-1, AEDocumentType.valueOf(AEDocumentType.System.NA));
	}

	public AEDocumentDescriptorImp(long id, long docTypeID) {
		this(id, AEDocumentType.valueOf(docTypeID));
	}
	
	public AEDocumentDescriptorImp(long id, AEDocumentType docType) {
		this.descr = new AEDescriptorImp(id, DomainModel.DomainClass.AeDocument);
		this.docType = docType;
	}	
	
	/* (non-Javadoc)
	 * @see eu.agileeng.domain.document.AEDocumentDescriptor#getDocumentType()
	 */
	@Override
	public AEDocumentType getDocumentType() {
		return this.docType;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.document.AEDocumentDescriptor#setDocumentType(eu.agileeng.domain.document.AEDocumentType)
	 */
	@Override
	public void setDocumentType(AEDocumentType docType) {
		this.docType = docType;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEDescriptor#getClazz()
	 */
	@Override
	public DomainClass getClazz() {
		return descr.getClazz();
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEDescriptor#getCode()
	 */
	@Override
	public String getCode() {
		return descr.getCode();
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEDescriptor#getID()
	 */
	@Override
	public long getID() {
		return descr.getID();
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEDescriptor#getName()
	 */
	@Override
	public String getName() {
		return descr.getName();
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEDescriptor#isPersistent()
	 */
	@Override
	public boolean isPersistent() {
		return descr.isPersistent();
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEDescriptor#setClazz(eu.agileeng.domain.DomainModel.DomainClass)
	 */
	@Override
	public void setClazz(DomainClass clazz) {
		descr.setClazz(clazz);
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEDescriptor#setCode(java.lang.String)
	 */
	@Override
	public void setCode(String code) {
		descr.setCode(code);
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEDescriptor#setID(long)
	 */
	@Override
	public void setID(long id) {
		descr.setID(id);
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEDescriptor#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		descr.setName(name);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((descr == null) ? 0 : descr.hashCode());
		result = prime * result + ((docType == null) ? 0 : docType.hashCode());
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
		AEDocumentDescriptorImp other = (AEDocumentDescriptorImp) obj;
		if (descr == null) {
			if (other.descr != null)
				return false;
		} else if (!descr.equals(other.descr))
			return false;
		if (docType == null) {
			if (other.docType != null)
				return false;
		} else if (!docType.equals(other.docType))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEDescriptive#getDescriptor()
	 */
	@Override
	public AEDescriptor getDescriptor() {
		if(AEStringUtil.isEmpty(getDescription())) {
			StringBuffer description = new StringBuffer();
			description.append(getNumber() != null ? getNumber() : AEStringUtil.EMPTY_STRING);
			if(!AEStringUtil.isEmpty(description)) {
				description.append(" / ");
			}
			try {
				description.append(AEDateUtil.formatToSystem(getDate()));
			} catch(Exception e){}
			setDescription(description.toString());
		}
		return this;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEDescriptor#getProperties()
	 */
	@Override
	public long getProperties() {
		return descr.getProperties();
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEDescriptor#setProperties(long)
	 */
	@Override
	public void setProperties(long properties) {
		descr.setProperties(properties);
	}

	@Override
	public void setSysId(long sysId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getSysId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = null;
		if(this.descr != null) {
			json = this.descr.toJSONObject();
		} else {
			json = new JSONObject();
		}
		if(this.docType != null) {
			json.put("docType", this.docType.getSystemID());
		}
		if(this.date != null) {
			json.put("date", AEDateUtil.formatToSystem(this.date));
		}
		if(!AEStringUtil.isEmpty(this.number)) {
			json.put("number", this.number);
		}
		return json;
	}

	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		this.descr.create(jsonObject);

		setDocumentType(AEDocumentType.valueOf(jsonObject.optLong("docType")));
		setNumber(jsonObject.optString("number"));
		setDate(AEDateUtil.parseDateStrict(jsonObject.optString("date")));
		
		StringBuffer description = new StringBuffer();
		description.append(getNumber());
		if(!AEStringUtil.isEmpty(description)) {
			description.append(" / ");
		}
		if(getDate() != null) {
			description.append(AEDateUtil.formatToSystem(getDate()));
		}
		setDescription(description.toString());
	}

	@Override
	public String getDescription() {
		return descr.getDescription();
	}

	@Override
	public void setDescription(String description) {
		descr.setDescription(description);
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
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
	public AEDescriptor withName(String name) {
		setName(name);
		return this;
	}
	
	@Override
	public AEDescriptor withDescription(String description) {
		setDescription(description);
		return this;
	}
}
