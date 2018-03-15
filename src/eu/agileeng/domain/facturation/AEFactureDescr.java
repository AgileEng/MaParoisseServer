package eu.agileeng.domain.facturation;

import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.document.AEDocumentDescriptor;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.imp.AEDocumentDescriptorImp;

public class AEFactureDescr implements AEDocumentDescriptor {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5604568294107730668L;

	private AEDocumentDescriptor docDescr;

	private AEFactureUtil.FactureState state;
	
	private AEFactureUtil.FactureSubType subType;

	public AEFactureDescr() {
		this.docDescr = new AEDocumentDescriptorImp();
	}
	
	AEFactureDescr(AEDocumentDescriptor docDescr) {
		this.docDescr = docDescr;
	}
	
	@Override
	public void setID(long id) {
		docDescr.setID(id);
	}

	@Override
	public long getID() {
		return docDescr.getID();
	}

	@Override
	public void setClazz(DomainClass clazz) {
		docDescr.setClazz(clazz);
	}

	@Override
	public DomainClass getClazz() {
		return docDescr.getClazz();
	}

	@Override
	public void setCode(String code) {
		docDescr.setCode(code);
	}

	@Override
	public String getCode() {
		return docDescr.getCode();
	}

	@Override
	public void setName(String name) {
		docDescr.setName(name);
	}

	@Override
	public String getDescription() {
		return docDescr.getDescription();
	}

	@Override
	public void setDescription(String description) {
		docDescr.setDescription(description);
	}

	@Override
	public String getName() {
		return docDescr.getName();
	}

	@Override
	public boolean isPersistent() {
		return docDescr.isPersistent();
	}

	@Override
	public void setProperties(long properties) {
		docDescr.setProperties(properties);
	}

	@Override
	public long getProperties() {
		return docDescr.getProperties();
	}

	@Override
	public void setSysId(long sysId) {
		docDescr.setSysId(sysId);
	}

	@Override
	public long getSysId() {
		return docDescr.getSysId();
	}

	@Override
	public AEDescriptor getDescriptor() {
		return this;
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = docDescr.toJSONObject();
		if(getState() != null) {
			json.put(AEFacture.JSONKey.state, getState().getStateId());
		}
		if(getSubType() != null) {
			json.put(AEFacture.JSONKey.subType, getSubType().getSubTypeId());
		}
		return json;
	}

	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		docDescr.create(jsonObject);
		if(jsonObject.has(AEFacture.JSONKey.state)) {
			setState(AEFactureUtil.FactureState.valueOf(jsonObject.optLong(AEFacture.JSONKey.state)));
		}
		if(jsonObject.has(AEFacture.JSONKey.subType)) {
			setSubType(AEFactureUtil.FactureSubType.valueOf(jsonObject.optLong(AEFacture.JSONKey.subType)));
		}
	}

	@Override
	public void setDocumentType(AEDocumentType docType) {
		docDescr.setDocumentType(docType);
	}

	@Override
	public AEDocumentType getDocumentType() {
		return docDescr.getDocumentType();
	}

	@Override
	public Date getDate() {
		return docDescr.getDate();
	}

	@Override
	public void setDate(Date date) {
		docDescr.setDate(date);
	}

	@Override
	public String getNumber() {
		return docDescr.getNumber();
	}

	@Override
	public void setNumber(String number) {
		docDescr.setNumber(number);
	}

	public AEFactureUtil.FactureState getState() {
		return state;
	}

	public void setState(AEFactureUtil.FactureState state) {
		this.state = state;
	}

	public void setDocDescr(AEDocumentDescriptorImp docDescr) {
		this.docDescr = docDescr;
	}
	
	public boolean isFormalFacture() {
		return isFacture() && AEFactureUtil.FactureState.VALIDATED == getState();
	}
	
	public boolean isFacture() {
		return AEDocumentType.System.AEFactureSale.getSystemID() == getDocumentType().getSystemID();
	}
	
	public boolean isDevis() {
		return AEDocumentType.System.AEDevisSale.getSystemID() == getDocumentType().getSystemID();
	}

	public AEFactureUtil.FactureSubType getSubType() {
		return subType;
	}

	public void setSubType(AEFactureUtil.FactureSubType subType) {
		this.subType = subType;
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
