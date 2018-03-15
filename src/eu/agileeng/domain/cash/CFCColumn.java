/**
 * 
 */
package eu.agileeng.domain.cash;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AEValue;

/**
 * @author vvatov
 *
 */
public class CFCColumn extends AEDomainObject {

	public static enum NType {
		NULL,
		ACCOUNT,
		VALUE,
		EXPRESSION,
		ATTRIBUTE,
		LABEL,
		EXPRESSION_COB
	}
	
	public static enum AType {
		Dt,
		Ct
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 9029323347841979845L;
	
	private AEDescriptive toCFC;
	
	private int colIndex = -1;
	
	private CFCColumn.NType nType;
	
	private AEValue value;
	
	private boolean hidden;
	
	private AEDescriptive accBank;
	
	private AEDescriptive accAccount;
	
	private AType accEntryType;

	/**
	 * @param clazz
	 */
	public CFCColumn() {
		super(DomainClass.CFCColumn);
	}

	public AEDescriptive getToCFC() {
		return toCFC;
	}

	public void setToCFC(AEDescriptive toCFC) {
		this.toCFC = toCFC;
	}

	public int getColIndex() {
		return colIndex;
	}

	public void setColIndex(int colIndex) {
		this.colIndex = colIndex;
	}

	public CFCColumn.NType getNType() {
		return nType;
	}

	public void setNType(CFCColumn.NType nType) {
		this.nType = nType;
	}

	public AEValue getValue() {
		return value;
	}

	public void setValue(AEValue value) {
		this.value = value;
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();

		if(getNType() != null) {
			json.put("nType", getNType().toString());			
		}
		
		json.put("sIndex", getColIndex());
		
		if(getValue() != null) {
			json.put("xType", getValue().getXType().toString());
		}

		if(!AEValue.isNull(getValue())) {
			json.put("value", getValue().toString());
		}
		
		json.put("hidden", isHidden());
		
		if(accBank != null) {
			json.put("accBankId", accBank.getDescriptor().getID());
		}
		
		if(getAccAccount() != null) {
			json.put("accAccountId", getAccAccount().getDescriptor().getID());
		}
		
		if(getAccEntryType() != null) {
			json.put("accEntryType", getAccEntryType().name());
		}
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		if(jsonObject.has("nType")) {
			this.setNType(NType.valueOf(jsonObject.optString("nType")));
		}
		
		if(jsonObject.has("sIndex")) {
			this.setColIndex(jsonObject.optInt("sIndex"));
		}
		
		try {
			if(jsonObject.has("value") && jsonObject.has("xType")) {
				AEValue value = new AEValue(
						jsonObject.getString("value"), 
						jsonObject.getString("xType"));
				setValue(value);
			} else {
				setValue(new AEValue(AEStringUtil.EMPTY_STRING, AEValue.XType.STRING));
			}
		} catch(Exception e) {
		}
		
		if(jsonObject.has("hidden")) {
			setHidden(jsonObject.optBoolean("hidden"));
		}
		
		if(jsonObject.has("accBankId")) {
			long accBankId = jsonObject.optLong("accBankId", AEPersistentUtil.NEW_ID);
			if(accBankId > 0) {
				this.accBank = AccAccount.lazyDescriptor(accBankId);
			}
		}
		
		if(jsonObject.has("accAccountId")) {
			long accAccountId = jsonObject.optLong("accAccountId", AEPersistentUtil.NEW_ID);
			if(accAccountId > 0) {
				setAccAccount(AccAccount.lazyDescriptor(accAccountId));
			}
		}
		
		if(jsonObject.has("accEntryType")) {
			String accEntryType = jsonObject.optString("accEntryType");
			if(!AEStringUtil.isEmpty(accEntryType)) {
				try {
					setAccEntryType(AType.valueOf(accEntryType));
				} catch (Exception e) {
				}
			}
		}
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public AEDescriptive getAccBank() {
		return accBank;
	}

	public void setAccBank(AEDescriptive accBank) {
		this.accBank = accBank;
	}

	public AEDescriptive getAccAccount() {
		return accAccount;
	}

	public void setAccAccount(AEDescriptive accAccount) {
		this.accAccount = accAccount;
	}

	public AType getAccEntryType() {
		return accEntryType;
	}

	public void setAccEntryType(AType accEntryType) {
		this.accEntryType = accEntryType;
	}
}
