/**
 * 
 */
package eu.agileeng.domain.business.bank;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.util.AEStringUtil;

/**
 * @author vvatov
 *
 */
public class BankRecognitionRule extends AEDomainObject {

	/**
	 * Private for this time
	 */
	static private class JSONKey {
		public static final String codeAFB = "codeAFB";
		public static final String textCondition = "textCondition";
		public static final String ruleConditionId = "ruleConditionId";
		
		public static final String bankAccountId = "bankAccountId";
		public static final String abstractAccId = "abstractAccId";
		public static final String accId = "accId";
		public static final String accCode = "accCode";
		
		public static final String auxiliaryId = "auxiliaryId";
		public static final String auxiliaryCode = "auxiliaryCode";
		
		public static final String isSystem = "isSystem";
		public static final String isActive = "isActive";
		
	}
	
	private String codeAFB;

	private String textCondition;
	
	private BankUtil.RuleCondition ruleCondition = BankUtil.RuleCondition.NA;

	private AEDescriptor bankAccount;
	
	private AEDescriptor account;
	
	private AEDescriptor auxiliary;
	
	private boolean isSystem;
	
	private boolean isActive;
		

	/**
	 * 
	 */
	private static final long serialVersionUID = -5704974936062383568L;

	public BankRecognitionRule() {
		super(DomainClass.BankRecognitionRule);
		setID(AEPersistentUtil.getTmpID());
	}

	public AEDescriptor getBankAccount() {
		return bankAccount;
	}

	public void setBankAccount(AEDescriptor bankAccount) {
		this.bankAccount = bankAccount;
	}

	public AEDescriptor getAccount() {
		return account;
	}

	public void setAccount(AEDescriptor account) {
		this.account = account;
	}

	public AEDescriptor getAuxiliary() {
		return auxiliary;
	}

	public void setAuxiliary(AEDescriptor auxiliary) {
		this.auxiliary = auxiliary;
	}
	
	public String getTextCondition() {
		return textCondition;
	}

	public void setTextCondition(String textCondition) {
		this.textCondition = textCondition;
	}
	
	public boolean isSystem() {
		return isSystem;
	}

	public void setSystem(boolean isSystem) {
		this.isSystem = isSystem;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}
	
	public BankUtil.RuleCondition getRuleCondition() {
		return ruleCondition;
	}

	public void setRuleCondition(BankUtil.RuleCondition ruleCondition) {
		this.ruleCondition = ruleCondition;
	}
	
	public String getCodeAFB() {
		return codeAFB;
	}

	public void setCodeAFB(String codeAFB) {
		this.codeAFB = codeAFB;
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		json.put(JSONKey.codeAFB, this.getCodeAFB());
		json.put(JSONKey.textCondition, this.getTextCondition());
		json.put(JSONKey.ruleConditionId, this.getRuleCondition().getConditionId());
		
		if(getBankAccount() != null) {
			json.put("bankAccountId", getBankAccount().getID());
		} else {
			json.put("bankAccountId", AEStringUtil.EMPTY_STRING);
		}
		
		if(getAccount() != null) {
			json.put("abstractAccId", -1 * getAccount().getID());
			json.put("accId", getAccount().getID());
			json.put("accCode", getAccount().getCode());
		} else {
			json.put("accId", AEStringUtil.EMPTY_STRING);
			json.put("accCode", AEStringUtil.EMPTY_STRING);
		}
		
		if(getAuxiliary() != null) {
			// abstractAccId should replace existing one
			json.put("abstractAccId", getAuxiliary().getDescriptor().getID());
			json.put("auxiliaryId", getAuxiliary().getID());
			json.put(JSONKey.auxiliaryCode, getAuxiliary().getCode());
		} else {
			json.put("auxiliaryId", -1);
			json.put(JSONKey.auxiliaryCode, AEStringUtil.EMPTY_STRING);
		}
		
		json.put(JSONKey.isActive, this.isActive());
		json.put(JSONKey.isSystem, this.isSystem());
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		//TODO: maybe some validation of sensitive fields
		this.setCodeAFB(jsonObject.optString(JSONKey.codeAFB));
		this.setTextCondition(jsonObject.optString(JSONKey.textCondition));
		this.setRuleCondition(BankUtil.RuleCondition.valueOf(jsonObject.optLong(JSONKey.ruleConditionId)));
		
		if(jsonObject.has("bankAccountId")) {
			 try {
				 long bankAccId = jsonObject.getLong("bankAccountId");
				 if(bankAccId > 0) {
					 AEDescriptor bankAccDescr = BankAccount.lazyDescriptor(bankAccId);
					 setBankAccount(bankAccDescr);
				 }
			 } catch (JSONException e) {
			 }
		}
		
		if(jsonObject.has("accId")) {
			 try {
				 long accId = jsonObject.getLong("accId");
				 if(accId > 0) {
					 AEDescriptor accDescr = AccAccount.lazyDescriptor(accId);
					 setAccount(accDescr);
				 }
			 } catch (JSONException e) {
			 }
		}
		
		if(jsonObject.has(JSONKey.auxiliaryCode) || jsonObject.has("auxiliaryId")) {
			long partyId = jsonObject.optLong("auxiliaryId");
			String auxCode = jsonObject.optString(JSONKey.auxiliaryCode);
			if(!AEStringUtil.isEmpty(auxCode) || partyId > 0) {
				AEDescriptor auxDescr = new AEDescriptorImp();
				auxDescr.setCode(auxCode);
				auxDescr.setID(partyId);
				setAuxiliary(auxDescr);
			}
		}
		
		this.setSystem(jsonObject.optBoolean(JSONKey.isSystem));
		this.setActive(jsonObject.optBoolean(JSONKey.isActive));
		
	}
}
