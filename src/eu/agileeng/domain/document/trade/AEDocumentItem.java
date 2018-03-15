package eu.agileeng.domain.document.trade;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AEValue;
import eu.agileeng.util.json.JSONUtil;

public class AEDocumentItem extends AEDomainObject {

	public static final String ACC_TYPE_DEBIT = "Dt";
	
	public static final String ACC_TYPE_CREDIT = "Ct";
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8269565493936548597L;

	private AEDescriptive document;
	
	private AEDescriptive account;
	
	private AEDescriptive accountSecondary;
	
	private AEDescriptive party;
	
	private String accType;
	
	private double taxableAmount;
	
	private double vatAmount;
	
	private double amount;
	
	private AEDescriptive vatDescriptor;
	
	private double vatRate;
	
	private boolean paid;
	
	public static enum MDNType {
		NULL,
		ACCOUNT,
		VALUE,
		EXPRESSION,
		ATTRIBUTE
	}
	
	private AEDocumentItem.MDNType mdNType;
	
	private AEValue mdValue;
	
	protected AEDocumentItem(DomainClass clazz) {
		super(clazz);
	}
	
	public AEDocumentItem() {
		this(DomainModel.DomainClass.AEInvoiceItem);
	}

	public AEDescriptive getDocument() {
		return document;
	}

	public void setDocument(AEDescriptive document) {
		this.document = document;
	}

	public AEDescriptive getAccount() {
		return account;
	}

	public void setAccount(AEDescriptive account) {
		this.account = account;
	}

	public double getTaxableAmount() {
		return taxableAmount;
	}

	public void setTaxableAmount(double taxableAmount) {
		this.taxableAmount = taxableAmount;
	}

	public double getVatAmount() {
		return vatAmount;
	}

	public void setVatAmount(double vatAmount) {
		this.vatAmount = vatAmount;
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	public AEDescriptive getVatDescriptor() {
		return vatDescriptor;
	}

	public void setVatDescriptor(AEDescriptive vatDescriptor) {
		this.vatDescriptor = vatDescriptor;
	}

	public double getVatRate() {
		return vatRate;
	}

	public void setVatRate(double vatRate) {
		this.vatRate = vatRate;
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();

		json.put("sIndex", getSequenceNumber());
		if(getAccount() != null) {
			json.put("accId", getAccount().getDescriptor().getID());
			json.put("abstractAccId", -1 * getAccount().getDescriptor().getID());
		}
		if(getAccountSecondary() != null) {
			json.put("accIdSecond", getAccountSecondary().getDescriptor().getID());
		}
		json.put("accType", getAccType());
		json.put("taxableAmount", getTaxableAmount());
		json.put("vatAmount", getVatAmount());
		json.put("amount", getAmount());
		if(getVatDescriptor() != null) {
			json.put("vatCode", getVatDescriptor().getDescriptor().getCode());
		}
		json.put("vatRate", getVatRate());
		if(getParty() != null) {
			json.put("partyId", getParty().getDescriptor().getID());
			json.put("abstractAccId", getParty().getDescriptor().getID());
		}
		
		// meta data
		if(getMDNType() != null) {
			json.put("mdNType", getMDNType().toString());			
		}
		if(getMDValue() != null) {
			json.put("mdXType", getMDValue().getXType().toString());
		}
		if(!AEValue.isNull(getMDValue())) {
			json.put("mdValue", getMDValue().toString());
		}
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);

		// sequence number
		if(jsonObject.has("sIndex")) {
			this.setSequenceNumber(jsonObject.optInt("sIndex"));
		}
		
		// ACC_ID
		if(jsonObject.has("accId") && jsonObject.optLong("accId") > 0) {
			setAccount(new AEDescriptorImp(jsonObject.optLong("accId")));
		}
		// ACC_TYPE
		if(jsonObject.has("accType")) {
			setAccType(jsonObject.getString("accType"));
		}
		// accIdSecond
		if(jsonObject.has("accIdSecond") && jsonObject.optLong("accIdSecond") > 0) {
			setAccountSecondary(AccAccount.lazyDescriptor(jsonObject.optLong("accIdSecond")));
		}
		// TAXABLE_AMOUNT
		if(jsonObject.has("taxableAmount")) {
			setTaxableAmount(JSONUtil.normDouble(jsonObject, "taxableAmount"));
		}
		// VAT_CODE
		if(jsonObject.has("vatCode")) {
			AEDescriptorImp vatDescr = new AEDescriptorImp();
			vatDescr.setCode(jsonObject.optString("vatCode"));
			setVatDescriptor(vatDescr);
		}
		// VAT_RATE
		if(jsonObject.has("vatRate")) {
			setVatRate(JSONUtil.normDouble(jsonObject, "vatRate"));
		}
		// VAT_AMOUNT
		if(jsonObject.has("vatAmount")) {
			setVatAmount(JSONUtil.normDouble(jsonObject, "vatAmount"));
		}
		// AMOUNT
		if(jsonObject.has("amount")) {
			setAmount(JSONUtil.normDouble(jsonObject, "amount"));
		}
		// party
		if(jsonObject.has("partyId") && jsonObject.optLong("partyId") > 0) {
			try {
				long partyId = jsonObject.optLong("partyId");
				setParty(Organization.lazyDescriptor(partyId));
			} catch(Exception e) {
			}
		}
		
		// meta data
		if(jsonObject.has("mdNType") && !AEStringUtil.isEmpty(jsonObject.optString("mdNType"))) {
			this.setMDNType(MDNType.valueOf(jsonObject.optString("mdNType")));
		}
		try {
			if(jsonObject.has("mdValue") && jsonObject.has("mdXType") && !AEStringUtil.isEmpty(jsonObject.optString("mdXType"))) {
				AEValue value = new AEValue(
						jsonObject.optString("mdValue"), 
						jsonObject.optString("mdXType"));
				setMDValue(value);
			}
		} catch(Exception e) {
		}
	}

	public String getAccType() {
		return accType;
	}

	public void setAccType(String accType) {
		this.accType = accType;
	}

	public AEDescriptive getParty() {
		return party;
	}

	public void setParty(AEDescriptive party) {
		this.party = party;
	}

	public boolean isPaid() {
		return paid;
	}

	public void setPaid(boolean paid) {
		this.paid = paid;
	}

	public AEDescriptive getAccountSecondary() {
		return accountSecondary;
	}

	public void setAccountSecondary(AEDescriptive accountSecondary) {
		this.accountSecondary = accountSecondary;
	}

	public AEDocumentItem.MDNType getMDNType() {
		return mdNType;
	}

	public void setMDNType(AEDocumentItem.MDNType mdNype) {
		this.mdNType = mdNype;
	}

	public AEValue getMDValue() {
		return mdValue;
	}

	public void setMDValue(AEValue mdValue) {
		this.mdValue = mdValue;
	}
}
