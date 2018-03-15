/**
 * 
 */
package eu.agileeng.domain.acc;

import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel;
import eu.agileeng.domain.acc.cashbasis.AccJournalEntryTemplate;
import eu.agileeng.domain.acc.cashbasis.FinancialTransaction;
import eu.agileeng.domain.acc.cashbasis.AccJournalEntryTemplate.AccountIdentificationRule;
import eu.agileeng.domain.acc.cashbasis.AccJournalEntryTemplate.JournalIdentificationRule;
import eu.agileeng.domain.business.bank.BankAccount;
import eu.agileeng.domain.contact.Contributor;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEPersistent;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;

/**
 * @author vvatov
 *
 */
public class AccJournalItem extends AEDomainObject {

	static public enum JSONKey {
		batchId,
		dateCreation,
		contributorId,
		supplierId,
		bankAccountId,
		tally;
	}
	
	private JournalIdentificationRule journalIdentificationRule = JournalIdentificationRule.NA;
	
	private AEDescriptor journal;
	
	private AEDescriptor accPeriod;
	
	private long batchId = AEPersistentUtil.NEW_ID;
	
	private long entryId = AEPersistentUtil.NEW_ID;
	
	private long entryIdGui = AEPersistentUtil.NEW_ID;
	
	/**
	 * Accounting date
	 */
	private Date date;
	
	/**
	 * When this item is created
	 */
	private Date dateCreation;
	
	private AccountIdentificationRule accountIdentificationRule = AccountIdentificationRule.NA;
	
	private AEDescriptor account;
	
	private AEDescriptor auxiliary;
	
	private AEDescriptor reference;
	
	private AEDescriptor currency = new AEDescriptorImp().withCode("EUR");
	
	private boolean debit;
	
	private boolean credit;
	
	private Double dtAmount;
	
	private Double ctAmount;
	
	private Date paymentDueDate;
	
	private AEDescriptor issuerPaymentType;
	
	private AEDescriptor recipientPaymentType;
	
	private AEDescriptor quete;
	
	private AEDescriptor contributor;
	
	private AEDescriptor supplier;
	
	private AEDescriptor bankAccount;
	
	boolean tally;
	
	private boolean yearEndClosing;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2363593996514156860L;

	/**
	 * @param clazz
	 */
	public AccJournalItem() {
		super(DomainModel.DomainClass.AccJournalItem);
	}
	
	/**
	 * 
	 * @param accPeriod
	 */
	public AccJournalItem(AccPeriod accPeriod) {
		this();
		if(accPeriod != null) {
			setAccPeriod(accPeriod.getDescriptor());
		}
	}
	
	public AEDescriptor getJournal() {
		return journal;
	}
	
	public AEDescriptor grantJournal() {
		if(this.journal == null) {
			this.journal = new AEDescriptorImp();
		}
		return this.journal;
	}

	public void setJournal(AEDescriptor journal) {
		this.journal = journal;
	}

	public long getBatchId() {
		return batchId;
	}

	public void setBatchId(long batchId) {
		this.batchId = batchId;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public AEDescriptor getAccount() {
		return account;
	}
	
	public AEDescriptor grantAccount() {
		if(this.account == null) {
			this.account = new AEDescriptorImp();
		}
		return this.account;
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

	public AEDescriptor getReference() {
		return reference;
	}
	
	public AEDescriptor grantReference() {
		if(this.reference == null) {
			this.reference = new AEDescriptorImp();
		}
		return this.reference;
	}

	public void setReference(AEDescriptor reference) {
		this.reference = reference;
	}

	public AEDescriptor getCurrency() {
		return currency;
	}
	
	public AEDescriptor grantCurrency() {
		if(this.currency == null) {
			this.currency = new AEDescriptorImp();
		}
		return currency;
	}

	public void setCurrency(AEDescriptor currency) {
		this.currency = currency;
	}

	public Double getDtAmount() {
		return dtAmount;
	}

	public void setDtAmount(Double dtAmount) {
		this.dtAmount = dtAmount;
	}

	public Double getCtAmount() {
		return ctAmount;
	}

	public void setCtAmount(Double ctAmount) {
		this.ctAmount = ctAmount;
	}

	public long getEntryId() {
		return entryId;
	}

	public void setEntryId(long transactionId) {
		this.entryId = transactionId;
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();

		json.put("entryId", getEntryId());
		json.put(JSONKey.batchId.name(), getBatchId());
		json.put("hrOrder", getBatchId());
		if(getAccount() != null) {
			json.put("accId", getAccount().getID());
			json.put("accCode", getAccount().getCode());
			json.put("accName", getAccount().getName());
		} else {
			json.put("accId", AEStringUtil.EMPTY_STRING);
			json.put("accCode", AEStringUtil.EMPTY_STRING);
			json.put("accName", AEStringUtil.EMPTY_STRING);
		}
		if(getDate() != null) {
			json.put("date", AEDateUtil.convertToString(getDate(), AEDateUtil.SYSTEM_DATE_FORMAT));
		} else {
			json.put("date", AEStringUtil.EMPTY_STRING);
		}
		if(getDateCreation() != null) {
			json.put(JSONKey.dateCreation.name(), AEDateUtil.convertToString(getDateCreation(), AEDateUtil.SYSTEM_DATE_FORMAT));
		} else {
			json.put(JSONKey.dateCreation.name(), AEStringUtil.EMPTY_STRING);
		}
		if(getJournal() != null) {
			json.put("journal", getJournal().getCode());
		} else {
			json.put("journal", AEStringUtil.EMPTY_STRING);
		}
		if(getAuxiliary() != null) {
			json.put("auxiliary", getAuxiliary().getCode());
		} else {
			json.put("auxiliary", AEStringUtil.EMPTY_STRING);
		}
		if(getReference() != null) {
			json.put("reference", getReference().getDescription());
		} else {
			json.put("reference", AEStringUtil.EMPTY_STRING);
		}
		json.put("currency", "EUR"); 
		if(getDtAmount() != null) {
			json.put("dtAmount", getDtAmount());
		} else {
			json.put("dtAmount", 0.0);
		}
		if(getCtAmount() != null) {
			json.put("ctAmount", getCtAmount());
		} else {
			json.put("ctAmount", 0.0); 
		}
		json.put("debit", isDebit());
		json.put("credit", isCredit()); 

		if(accountIdentificationRule != null) {
			json.put(AccJournalEntryTemplate.JSONKey.accountIdentificationRuleId.name(), accountIdentificationRule.getId());
		} else {
			json.put(AccJournalEntryTemplate.JSONKey.accountIdentificationRuleId.name(), AccJournalEntryTemplate.AccountIdentificationRule.NA.getId());
		}
		
		if(journalIdentificationRule != null) {
			json.put(AccJournalEntryTemplate.JSONKey.journalIdentificationRuleId.name(), journalIdentificationRule.getId());
		} else {
			json.put(AccJournalEntryTemplate.JSONKey.journalIdentificationRuleId.name(), AccJournalEntryTemplate.JournalIdentificationRule.NA.getId());
		}
		
		// quete
		if(getQuete() != null) {
			json.put(AccJournalEntryTemplate.JSONKey.codeQuete.name(), getQuete().getCode());
		}
		
		// contributor
		if(getContributor() != null) {
			json.put(JSONKey.contributorId.name(), getContributor().getID());
		}
		
		// supplier
		if(getSupplier() != null) {
			json.put(JSONKey.supplierId.name(), getSupplier().getID());
		}
		
		// bankAcount
		if(getBankAccount() != null) {
			json.put(JSONKey.bankAccountId.name(), getBankAccount().getID());
		}
		
		// tally
		json.put(JSONKey.tally.name(), isTally());
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// entryId
		setEntryId(jsonObject.optLong("entryId"));
		
		// account
		if(jsonObject.has("accId")) {
			long accId = jsonObject.getLong("accId");
			if(AEPersistent.ID.isPersistent(accId)) {
				setAccount(AccAccount.lazyDescriptor(accId));
			}
		}

		// date
		if(jsonObject.has("date")) {
			String date = jsonObject.optString("date");
			setDate(AEDateUtil.parseDateStrict(date));
		}

		// dateCreation
		if(jsonObject.has(JSONKey.dateCreation.name())) {
			String dateCreation = jsonObject.getString(JSONKey.dateCreation.name());
			setDateCreation(AEDateUtil.parseDateStrict(dateCreation));
		}

		// journal
		if(jsonObject.has("journal")) {
			String journal = jsonObject.optString("journal");
			setJournal(new AEDescriptorImp().withCode(journal));
		}
		
		// auxiliary
		
		// reference
		
		// accPeriod;
		
		// batchId

		// paymentDueDate
		
		// issuerPaymentType

		// recipientPaymentType

		// currency
		if(jsonObject.has("currency")) {
			String currencyCode = jsonObject.getString("currency");
			if(!AEStringUtil.isEmpty(currencyCode)) {
				setCurrency(new AEDescriptorImp().withCode(currencyCode));
			}
		}
		
		// dtAmount
		if(jsonObject.has("dtAmount")) {
			String dtAmount = jsonObject.getString("dtAmount");
			try {
				setDtAmount(AEMath.parseDouble(dtAmount, true));
			} catch (AEException e) {
				throw new JSONException("Invalid Debit Amount");
			}
		}

		// ctAmount
		if(jsonObject.has("ctAmount")) {
			String ctAmount = jsonObject.getString("ctAmount");
			try {
				setCtAmount(AEMath.parseDouble(ctAmount, true));
			} catch (AEException e) {
				throw new JSONException("Invalid Credit Amount");
			}
		}
		
		setDebit(jsonObject.optBoolean("debit"));
		setCredit(jsonObject.optBoolean("credit"));

		// accountIdentificationRuleId
		if(jsonObject.has(AccJournalEntryTemplate.JSONKey.accountIdentificationRuleId.name())) {
			Long id = jsonObject.getLong(AccJournalEntryTemplate.JSONKey.accountIdentificationRuleId.name());
			setAccountIdentificationRule(AccJournalEntryTemplate.AccountIdentificationRule.valueOf(id));
		}
		
		// journalIdentificationRuleId
		if(jsonObject.has(AccJournalEntryTemplate.JSONKey.journalIdentificationRuleId.name())) {
			Long id = jsonObject.getLong(AccJournalEntryTemplate.JSONKey.journalIdentificationRuleId.name());
			setJournalIdentificationRule(AccJournalEntryTemplate.JournalIdentificationRule.valueOf(id));
		}
		
		// quete
		String codeQuete = jsonObject.optString(AccJournalEntryTemplate.JSONKey.codeQuete.name());
		if(!AEStringUtil.isEmpty(codeQuete)) {
			setQuete(new AEDescriptorImp().withCode(codeQuete));
		}
		
		// contributor
		if(jsonObject.has(JSONKey.contributorId.name())) {
			Long id = jsonObject.getLong(JSONKey.contributorId.name());
			if(AEPersistent.ID.isPersistent(id)) {
				setContributor(Contributor.lazyDescriptor(id));
			}
		}
		
		// supplier
		if(jsonObject.has(FinancialTransaction.JSONKey.supplier.name())) {
			JSONObject supplJson = jsonObject.getJSONObject(FinancialTransaction.JSONKey.supplier.name());
			Long id = supplJson.optLong(AEDomainObject.JSONKey.id.name());
			String name = supplJson.optString(AEDomainObject.JSONKey.name.name());
//			if(AEPersistent.ID.isPersistent(id)) {
			setSupplier(new AEDescriptorImp().withId(id).withName(name));
//			}
		} else if(jsonObject.has(JSONKey.supplierId.name())) {
			Long id = jsonObject.optLong(JSONKey.supplierId.name());
			if(AEPersistent.ID.isPersistent(id)) {
				setSupplier(new AEDescriptorImp().withId(id));
			}
		}
		
		// bankAccount
		if(jsonObject.has(JSONKey.bankAccountId.name())) {
			Long id = jsonObject.optLong(JSONKey.bankAccountId.name());
			if(AEPersistent.ID.isPersistent(id)) {
				setBankAccount(BankAccount.lazyDescriptor(id));
			}
		}
	}

	public AEDescriptor getAccPeriod() {
		return accPeriod;
	}

	public void setAccPeriod(AEDescriptor accPeriod) {
		this.accPeriod = accPeriod;
	}

	public Date getPaymentDueDate() {
		return paymentDueDate;
	}

	public void setPaymentDueDate(Date paymentDueDate) {
		this.paymentDueDate = paymentDueDate;
	}

	public AEDescriptor getIssuerPaymentType() {
		return issuerPaymentType;
	}

	public void setIssuerPaymentType(AEDescriptor issuerPaymentType) {
		this.issuerPaymentType = issuerPaymentType;
	}

	public AEDescriptor getRecipientPaymentType() {
		return recipientPaymentType;
	}

	public void setRecipientPaymentType(AEDescriptor recipientPaymentType) {
		this.recipientPaymentType = recipientPaymentType;
	}

	/**
	 * @return the debit
	 */
	public boolean isDebit() {
		return debit;
	}

	/**
	 * @param debit the debit to set
	 */
	public void setDebit(boolean debit) {
		this.debit = debit;
	}

	/**
	 * @return the credit
	 */
	public boolean isCredit() {
		return credit;
	}

	/**
	 * @param credit the credit to set
	 */
	public void setCredit(boolean credit) {
		this.credit = credit;
	}

	/**
	 * @return the accountIdentificationRule
	 */
	public AccountIdentificationRule getAccountIdentificationRule() {
		return accountIdentificationRule;
	}

	/**
	 * @param accountIdentificationRule the accountIdentificationRule to set
	 */
	public void setAccountIdentificationRule(
			AccountIdentificationRule accountIdentificationRule) {
		this.accountIdentificationRule = accountIdentificationRule;
	}

	/**
	 * @return the journalIdentificationRule
	 */
	public JournalIdentificationRule getJournalIdentificationRule() {
		return journalIdentificationRule;
	}

	/**
	 * @param journalIdentificationRule the journalIdentificationRule to set
	 */
	public void setJournalIdentificationRule(
			JournalIdentificationRule journalIdentificationRule) {
		this.journalIdentificationRule = journalIdentificationRule;
	}

	/**
	 * @return the dateCreation
	 */
	public Date getDateCreation() {
		return dateCreation;
	}

	/**
	 * @param dateCreation the dateCreation to set
	 */
	public void setDateCreation(Date dateCreation) {
		this.dateCreation = dateCreation;
	}

	/**
	 * @return the quete
	 */
	public AEDescriptor getQuete() {
		return quete;
	}

	/**
	 * @param quete the quete to set
	 */
	public void setQuete(AEDescriptor quete) {
		this.quete = quete;
	}

	/**
	 * @return the contributor
	 */
	public AEDescriptor getContributor() {
		return contributor;
	}

	/**
	 * @param contributor the contributor to set
	 */
	public void setContributor(AEDescriptor contributor) {
		this.contributor = contributor;
	}

	/**
	 * @return the supplier
	 */
	public AEDescriptor getSupplier() {
		return supplier;
	}

	/**
	 * @param supplier the supplier to set
	 */
	public void setSupplier(AEDescriptor supplier) {
		this.supplier = supplier;
	}

	/**
	 * @return the bankAccount
	 */
	public AEDescriptor getBankAccount() {
		return bankAccount;
	}

	/**
	 * @param bankAccount the bankAccount to set
	 */
	public void setBankAccount(AEDescriptor bankAccount) {
		this.bankAccount = bankAccount;
	}

	/**
	 * @return the entryIdGui
	 */
	public long getEntryIdGui() {
		return entryIdGui;
	}

	/**
	 * @param entryIdTmp the entryIdTmp to set
	 */
	public void setEntryIdGui(long entryIdGui) {
		this.entryIdGui = entryIdGui;
	}

	/**
	 * @return the tally
	 */
	public boolean isTally() {
		return tally;
	}

	/**
	 * @param tally the tally to set
	 */
	public void setTally(boolean tally) {
		this.tally = tally;
	}

	/**
	 * @return the yearEndClosing
	 */
	public boolean isYearEndClosing() {
		return yearEndClosing;
	}

	/**
	 * @param yearEndClosing the yearEndClosing to set
	 */
	public void setYearEndClosing(boolean yearEndClosing) {
		this.yearEndClosing = yearEndClosing;
	}
	
	
}
