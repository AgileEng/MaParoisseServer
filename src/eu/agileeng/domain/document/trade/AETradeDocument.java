package eu.agileeng.domain.document.trade;

import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.document.AEDocument;
import eu.agileeng.domain.document.AEDocumentController;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.json.JSONUtil;

public class AETradeDocument extends AEDocument {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7227882871713220828L;
	
	private AEDescriptive issuer;
	
	private AEDescriptive issuerAcc;
	
	private AEDescriptive recipient;
	
	private AEDescriptive recipientAcc;
	
	private double taxableAmount;
	
	private AEDescriptive vatDescriptor;
	
	private double vatAmount;
	
	private double amount;
	
	private Date paymentDueDate;
	
	private AEDescriptive currency;
	
	private boolean paid;
	
	private AEDescriptive vatAccount;
	
	/**
	 * no arg constructor.
	 * Try to use it only during DB Fetching
	 */
	public AETradeDocument() {
		super(AEDocumentType.valueOf(AEDocumentType.System.AEPurchaseInvoice));
	}
	
	public AETradeDocument(AEDocumentType docType) {
		super(docType);
	}

	/**
	 * @return the documentController
	 */
	@Override
	public AEDocumentController getDocumentController() {
		if(super.getDocumentController() == null) {
			super.setDocumentController(new AETradeDocumentController(this));
		}
		return super.getDocumentController();
	}

	public AEDescriptive getIssuer() {
		return issuer;
	}

	public void setIssuer(AEDescriptive issuer) {
		this.issuer = issuer;
	}

	public AEDescriptive getIssuerAcc() {
		return issuerAcc;
	}

	public void setIssuerAcc(AEDescriptive issuerAcc) {
		this.issuerAcc = issuerAcc;
	}

	public AEDescriptive getRecipient() {
		return recipient;
	}

	public void setRecipient(AEDescriptive recipient) {
		this.recipient = recipient;
	}

	public AEDescriptive getRecipientAcc() {
		return recipientAcc;
	}

	public void setRecipientAcc(AEDescriptive recipientAcc) {
		this.recipientAcc = recipientAcc;
	}

	public double getTaxableAmount() {
		return taxableAmount;
	}

	public void setTaxableAmount(double taxableAmount) {
		this.taxableAmount = taxableAmount;
	}

	public AEDescriptive getVatDescriptor() {
		return vatDescriptor;
	}

	public void setVatDescriptor(AEDescriptive vatDescriptor) {
		this.vatDescriptor = vatDescriptor;
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

	public Date getPaymentDueDate() {
		return paymentDueDate;
	}

	public void setPaymentDueDate(Date paymentDueDate) {
		this.paymentDueDate = paymentDueDate;
	}

	public AEDescriptive getCurrency() {
		return currency;
	}

	public void setCurrency(AEDescriptive currency) {
		this.currency = currency;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		if(jsonObject.has("payDueDate")) {
			setPaymentDueDate(AEDateUtil.parseDateStrict(jsonObject.getString("payDueDate")));
		}
		
		if(jsonObject.has("taxableAmount")) {
			try {
				setTaxableAmount(JSONUtil.normDouble(jsonObject, "taxableAmount"));
			} catch(JSONException e) {};
		}
		
		if(jsonObject.has("vatAmount")) {
			try {
				setVatAmount(JSONUtil.normDouble(jsonObject, "vatAmount"));
			} catch(JSONException e){}
		}

		if(jsonObject.has("amount")) {
			try {
				setAmount(JSONUtil.normDouble(jsonObject, "amount"));
			} catch(JSONException e) {};
		}
		
		if(jsonObject.has("issuerId")) {
			try {
				long issuerId = jsonObject.getLong("issuerId");
				if(issuerId > 0) {
					setIssuer(Organization.lazyDescriptor(issuerId));
				}
			} catch (JSONException e) {
			};
		}
		
		if(jsonObject.has("issuerAccId")) {
			try {
				long issuerAccId = jsonObject.getLong("issuerAccId");
				if(issuerAccId > 0) {
					setIssuerAcc(new AEDescriptorImp(issuerAccId, DomainClass.AccAccount));
				}
			} catch(JSONException e) {
			}
		}

		if(jsonObject.has("recipientId")) {
			try{
				long recipientId = jsonObject.getLong("recipientId");
				if(recipientId > 0) {
					setRecipient(Organization.lazyDescriptor(recipientId));
				}
			} catch(JSONException e) {
				
			}
		}
		
		if(jsonObject.has("recipientAccId")) {
			try {
				long recipientAccId = jsonObject.getLong("recipientAccId");
				if(recipientAccId > 0) {
					setRecipientAcc(new AEDescriptorImp(recipientAccId, DomainClass.AccAccount));
				}
			} catch (JSONException e) {
			}
		}
		
		if(jsonObject.has("currency")) {
			AEDescriptorImp currDescr = new AEDescriptorImp();
			currDescr.setCode(jsonObject.getString("currency"));
			setCurrency(currDescr);
		}
		
		if(jsonObject.has("vatAccId")) {
			try {
				long vatAccId = jsonObject.getLong("vatAccId");
				if(vatAccId > 0) {
					setVatAccount(new AEDescriptorImp(vatAccId, DomainClass.AccAccount));
				}
			} catch(JSONException e) {
			}
		}
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();

		if(getPaymentDueDate() != null) {
			json.put("payDueDate", AEDateUtil.convertToString(getPaymentDueDate(), AEDateUtil.SYSTEM_DATE_FORMAT));
		}
		json.put("taxableAmount", getTaxableAmount());
		json.put("vatAmount", getVatAmount());
		json.put("amount", getAmount());
		if(getIssuer() != null) {
			json.put("issuerId", getIssuer().getDescriptor().getID());
		}
		if(getIssuerAcc() != null) {
			json.put("issuerAccId", getIssuerAcc().getDescriptor().getID());
		}
		if(getRecipient() != null) {
			json.put("recipientId", getRecipient().getDescriptor().getID());
		}
		if(getRecipientAcc() != null) {
			json.put("recipientAccId", getRecipientAcc().getDescriptor().getID());
		}
		if(getCurrency() != null) {
			json.put("currency", getCurrency().getDescriptor().getCode());
		}
		if(getVatAccount() != null) {
			json.put("vatAccId", getVatAccount().getDescriptor().getID());
		}
		
		return json;
	}
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.AeDocument);
	}

	public boolean isPaid() {
		return paid;
	}

	public void setPaid(boolean paid) {
		this.paid = paid;
	}
	
	public void calculate() throws AEException {
		double taxableAmount = 0.0;
		double vatAmount = 0.0;
		double amount = 0.0;
		AEDocumentItemsList items = getItems();
		for (AEDocumentItem aeDocumentItem : items) {
			taxableAmount += aeDocumentItem.getTaxableAmount();
			vatAmount += aeDocumentItem.getVatAmount();
			amount += aeDocumentItem.getAmount();
		}
		setTaxableAmount(taxableAmount);
		setVatAmount(vatAmount);
		setAmount(amount);
		setUpdated();
	}

	public AEDescriptive getVatAccount() {
		return vatAccount;
	}

	public void setVatAccount(AEDescriptive vatAccount) {
		this.vatAccount = vatAccount;
	}
}
