package eu.agileeng.domain.cash;

import java.util.ArrayList;
import java.util.List;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.util.AEObjectUtil;
import eu.agileeng.util.json.JSONSerializable;

@SuppressWarnings("serial")
public class CashJETProp extends AEDomainObject implements JSONSerializable {

	private long propDefId;
	
	private AEDescriptor toCJEntryDescr;
	
	private AEDescriptor toCJETransactionDescr;
	
	private String xType;
	
	private boolean required;
	
	private String value;
	
	public CashJETProp() {
		super(DomainClass.CashJETransaction);
	}

	public AEDescriptor getToCJEntryDescr() {
		return toCJEntryDescr;
	}

	public void setToCJEntryDescr(AEDescriptor toCJEntryDescr) {
		this.toCJEntryDescr = toCJEntryDescr;
	}

	public AEDescriptor getToCJETransactionDescr() {
		return toCJETransactionDescr;
	}

	public void setToCJETransactionDescr(AEDescriptor toCJETransactionDescr) {
		this.toCJETransactionDescr = toCJETransactionDescr;
	}

	public long getPropDefId() {
		return propDefId;
	}

	public void setPropDefId(long propId) {
		this.propDefId = propId;
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		setID(jsonObject.optLong("id"));
		setName(jsonObject.optString("name"));
		setRequired(jsonObject.optBoolean("required"));
		setxType(jsonObject.optString("xType"));
		setValue(jsonObject.optString("_value"));
	}

	public static CashJETProp create(CashJETProp propDef, String value) throws AEException {
		CashJETProp prop = (CashJETProp) AEObjectUtil.deepCopy(propDef);
		prop.setID(AEPersistentUtil.NEW_ID);
		prop.setPropDefId(propDef.getID());
		prop.setValue(value);
		return prop;
	}
	
	public static List<CashJETProp> create(JSONArray jsonAttrArray) throws JSONException {
		List<CashJETProp> propsList = new ArrayList<CashJETProp>();
		if(jsonAttrArray != null && jsonAttrArray.length() > 0) {
			for (int i = 0; i < jsonAttrArray.length(); i++) {
				JSONObject jsonAttr = jsonAttrArray.getJSONObject(i);
				CashJETProp prop = new CashJETProp();
				prop.create(jsonAttr);
				propsList.add(prop);
			}
		}
		return propsList;
	}
	
	public String getxType() {
		return xType;
	}

	public void setxType(String xType) {
		this.xType = xType;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
