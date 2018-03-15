package eu.agileeng.domain.acc.cashbasis;

import java.util.Date;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.acc.AccAccountBalance;
import eu.agileeng.domain.acc.AccJournalEntry;
import eu.agileeng.domain.acc.cashbasis.FinancialTransactionTemplate.PaymentMethod;
import eu.agileeng.domain.contact.Contributor;
import eu.agileeng.domain.contact.SimpleParty;
import eu.agileeng.domain.document.AEDocumentDescriptor;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.imp.AEDocumentDescriptorImp;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEPersistent;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.json.JSONUtil;

public class FinancialTransaction extends AEDomainObject {

	private static final long serialVersionUID = 6364141006706871266L;

	static public enum JSONKey {
		appModuleId,
		accAccountExpIncId,
		bankAccountId,
		accJournalEntry,
		ftTemplateId,
		dateTransaction,
		dateAccounting,
		amount,
		financialTransactions,
		financialTransaction,
		paymentMethodId,
		accExpIncAccounts,
		bankAccounts,
		contributor,
		supplier,
		suppliers,
		contributors,
		fileAttachment,
		accBalance;
	}
	
	/**
	 * The type of this FinancialTransaction
	 */
	private AEDescriptor appModule;
	
	/**
	 * The personalized (concrete) 6 or 7 account for concrete expense or income in
	 * case of subdivision.
	 */
	private AEDescriptor accAccountExpInc;
	
	private JSONArray expIncAccounts;
	
	/**
	 * 
	 */
	private FinancialTransactionTemplate.PaymentMethod paymentMethod = FinancialTransactionTemplate.PaymentMethod.NA;
	
	/**
	 * The bank account in case of bank payment.
	 */
	private AEDescriptor bankAccount;
	
	/**
	 * Available bank Accounts for this Company
	 */
	private JSONArray bankAccounts;
	
	/**
	 * The cash acc account in case of cash payment.
	 */
	private AEDescriptor accAccountCash;
	
	/**
	 * Available Acc Cash Accounts for this Company
	 */
	private JSONArray cashAccounts;
	
	/**
	 * Available Other Receivable and Payable Accounts for this Company
	 */
	private JSONArray otherReceivablePayableAccounts;
	
	/**
	 * The Other Receivable and Payable Account 
	 */
	private AEDescriptor accAccountOtherReceivablePayable;
	
	/**
	 * The logging of this financial transaction into accounting journal items. 
	 */
	private AccJournalEntry accJournalEntry;
	
	/**
	 * Which template is used for this financial transaction
	 * 
	 */
	private AEDescriptor ftTemplate;
	
	/**
	 * The date of occurrence of the transaction
	 */
	private Date dateTransaction;
	
	/**
	 * The date of accounting of this transaction 
	 */
	private Date dateAccounting;
	
	/**
	 * The amount of this transaction
	 */
	private double amount;
	
	/**
	 * Optional, can be null
	 */
	private Contributor contributor;
	
	/**
	 * Optional, can be null
	 */
	private SimpleParty supplier;
	
	/**
	 * Optional, can be null
	 */
	private QuetesList quetesList;
	
	private AccAccountBalance accAccountBalance;
	
	/**
	 * Construcor
	 */
	public FinancialTransaction() {
		super(DomainClass.FinancialTransaction);
	}
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.FinancialTransaction);
	}

	/**
	 * @return the paymentMethod
	 */
	public FinancialTransactionTemplate.PaymentMethod getPaymentMethod() {
		return paymentMethod;
	}

	/**
	 * @param paymentMethod the paymentMethod to set
	 */
	public void setPaymentMethod(FinancialTransactionTemplate.PaymentMethod paymentMethod) {
		this.paymentMethod = paymentMethod;
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject jsonObject = super.toJSONObject();

		// appModule
		if(this.appModule != null) {
			jsonObject.put(JSONKey.appModuleId.name(), this.appModule.getID());
		}
		
		// paymentMethod
		if(this.paymentMethod != null) {
			jsonObject.put(JSONKey.paymentMethodId.name(), this.paymentMethod.getId());
		}
		
		// accAccountExpIncId
		if(getAccAccountExpInc() != null) {
			jsonObject.put(JSONKey.accAccountExpIncId.name(), getAccAccountExpInc().getID());
		}
		
		// bankAccount
		if(getBankAccount() != null) {
			jsonObject.put(JSONKey.bankAccountId.name(), getBankAccount().getID());
		}
		
		// accJournalEntry
		if(getAccJournalEntry() != null) {
			jsonObject.put(JSONKey.accJournalEntry.name(), getAccJournalEntry().toJSONObject());
		}
		
		// ftTemplate
		if(getFtTemplate() != null) {
			jsonObject.put(JSONKey.ftTemplateId.name(), getFtTemplate().getID());
		}

		// dateTransaction
		if(getDateTransaction() != null) {
			jsonObject.put(JSONKey.dateTransaction.name(), AEDateUtil.formatToSystem(getDateTransaction()));
		}
		
		// dateAccounting
		if(getDateAccounting() != null) {
			jsonObject.put(JSONKey.dateAccounting.name(), AEDateUtil.formatToSystem(getDateAccounting()));
		}
		
		// amount
		jsonObject.put(JSONKey.amount.name(), getAmount());
		
		// accExpIncAccounts
		if(getExpIncAccounts() != null) {
			jsonObject.put(FinancialTransaction.JSONKey.accExpIncAccounts.name(), getExpIncAccounts());
		}
		
		// bankAccounts
		if(getBankAccounts() != null) {
			jsonObject.put(FinancialTransaction.JSONKey.bankAccounts.name(), getBankAccounts());
		}
		
		// contributor
		if(getContributor() != null) {
			jsonObject.put(FinancialTransaction.JSONKey.contributor.name(), getContributor().toJSONObject());
		}
		
		// quetes
		if(getQuetesList() != null) {
			jsonObject.put(Quete.JSONKey.quetes.name(), getQuetesList().toJSONArray());
		}
		
		// supplier
		if(getSupplier() != null) {
			jsonObject.put(FinancialTransaction.JSONKey.supplier.name(), getSupplier().toJSONObject());
		}
		
		//
		if(getAccAccountBalance() != null) {
			jsonObject.put(FinancialTransaction.JSONKey.accBalance.name(), getAccAccountBalance().toJSONObject());
		}
		
		return jsonObject;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// payment method (required)
		setPaymentMethod(PaymentMethod.valueOf(jsonObject.getLong(JSONKey.paymentMethodId.name())));
		
		// contributor
		if(jsonObject.has(FinancialTransaction.JSONKey.contributor.name())) {
			Contributor contr = new Contributor();
			contr.create(jsonObject.getJSONObject(JSONKey.contributor.name()));
			this.setContributor(contr);
		}
		
		// quetes
		if(jsonObject.has(Quete.JSONKey.quetes.name())) {
			QuetesList qList = new QuetesList();
			qList.create(jsonObject.getJSONArray(Quete.JSONKey.quetes.name()));
			this.setQuetesList(qList);
		}
		
		// dateAccounting
		setDateAccounting(AEDateUtil.parseDateStrict(jsonObject.getString(JSONKey.dateAccounting.name())));
		
		// dateTransaction
		setDateTransaction(AEDateUtil.parseDateStrict(jsonObject.getString(JSONKey.dateTransaction.name())));
		
		//  ftTemplateId
		if(jsonObject.has(FinancialTransaction.JSONKey.ftTemplateId.name())) {
			long ftTemplateId = jsonObject.getLong(FinancialTransaction.JSONKey.ftTemplateId.name());
			AEDescriptor ftTemplateDescr = FinancialTransactionTemplate.lazyDescriptor(ftTemplateId);
			setFtTemplate(ftTemplateDescr);
		}
		
		// accAccountExpIncId
		if(jsonObject.has(FinancialTransaction.JSONKey.accAccountExpIncId.name())) {
			long accAccountExpIncId = jsonObject.getLong(FinancialTransaction.JSONKey.accAccountExpIncId.name());
			if(AEPersistent.ID.isPersistent(accAccountExpIncId)) {
				AEDescriptor accAccountDescr = AccAccount.lazyDescriptor(accAccountExpIncId);
				setAccAccountExpInc(accAccountDescr);
			}
		}
		
		// amount
		try {
			setAmount(JSONUtil.parseDoubleStrict(jsonObject, FinancialTransaction.JSONKey.amount.name()));
		} catch (AEException e) {
		}
		
		// accJournalEntry
		JSONObject accJournalEntryJson = jsonObject.getJSONObject(AccJournalEntry.JSONKey.accJournalEntry.name());
		AccJournalEntry accJournalEntry = new AccJournalEntry();
		accJournalEntry.create(accJournalEntryJson);
		accJournalEntry.setCompany(getCompany());
		setAccJournalEntry(accJournalEntry);
		
		// bankAccountId
		if(jsonObject.has(FinancialTransaction.JSONKey.bankAccountId.name())) {
			long bankAccountId = jsonObject.getLong(FinancialTransaction.JSONKey.bankAccountId.name());
			if(AEPersistent.ID.isPersistent(bankAccountId)) {
				AEDescriptor bankAccountDescr = AccAccount.lazyDescriptor(bankAccountId);
				setBankAccount(bankAccountDescr);
			}
		}
		
		// supplier
		if(jsonObject.has(FinancialTransaction.JSONKey.supplier.name())) {
			JSONObject supplJson = jsonObject.getJSONObject(JSONKey.supplier.name());
			
			SimpleParty supplier = null;
			Long id = JSONUtil.optLong(supplJson, AEDomainObject.JSONKey.id.name());
			if(id == null) {
				id = -1L;
				supplier = new SimpleParty();
				String name = supplJson.optString(AEDomainObject.JSONKey.name.name());
				supplier.setID(id);
				supplier.setName(name);
				supplier.setCompany(getCompany());
			} else if(AEPersistent.ID.isPersistent(id)) {
				supplier = new SimpleParty();
				String name = supplJson.optString(AEDomainObject.JSONKey.name.name());
				supplier.setID(id);
				supplier.setName(name);
				supplier.setCompany(getCompany());
			}

			this.setSupplier(supplier);
		}
	}

	/**
	 * @return the accAccountExpInc
	 */
	public AEDescriptor getAccAccountExpInc() {
		return accAccountExpInc;
	}

	/**
	 * @param accAccountExpInc the accAccountExpInc to set
	 */
	public void setAccAccountExpInc(AEDescriptor accAccountExpInc) {
		this.accAccountExpInc = accAccountExpInc;
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
	 * @return the accJournalEntry
	 */
	public AccJournalEntry getAccJournalEntry() {
		return accJournalEntry;
	}

	/**
	 * @param accJournalEntry the accJournalEntry to set
	 */
	public void setAccJournalEntry(AccJournalEntry accJournalEntry) {
		this.accJournalEntry = accJournalEntry;
	}

	/**
	 * @return the ftTemplate
	 */
	public AEDescriptor getFtTemplate() {
		return ftTemplate;
	}

	/**
	 * @param ftTemplate the ftTemplate to set
	 */
	public void setFtTemplate(AEDescriptor ftTemplate) {
		this.ftTemplate = ftTemplate;
	}

	/**
	 * @return the dateTransaction
	 */
	public Date getDateTransaction() {
		return dateTransaction;
	}

	/**
	 * @param dateTransaction the dateTransaction to set
	 */
	public void setDateTransaction(Date dateTransaction) {
		this.dateTransaction = dateTransaction;
	}

	/**
	 * @return the dateAccounting
	 */
	public Date getDateAccounting() {
		return dateAccounting;
	}

	/**
	 * @param dateAccounting the dateAccounting to set
	 */
	public void setDateAccounting(Date dateAccounting) {
		this.dateAccounting = dateAccounting;
	}

	/**
	 * @return the amount
	 */
	public double getAmount() {
		return amount;
	}

	/**
	 * @param amount the amount to set
	 */
	public void setAmount(double amount) {
		this.amount = amount;
	}

	/**
	 * @return the expIncAccounts
	 */
	public JSONArray getExpIncAccounts() {
		return expIncAccounts;
	}

	/**
	 * @param expIncAccounts the expIncAccounts to set
	 */
	public void setExpIncAccounts(JSONArray expIncAccounts) {
		this.expIncAccounts = expIncAccounts;
	}

	/**
	 * @return the bankAccounts
	 */
	public JSONArray getBankAccounts() {
		return bankAccounts;
	}

	/**
	 * @param bankAccounts the bankAccounts to set
	 */
	public void setBankAccounts(JSONArray bankAccounts) {
		this.bankAccounts = bankAccounts;
	}
	
	@Override
	public AEDescriptor getDescriptor() {
		AEDocumentDescriptor aeDescr = new AEDocumentDescriptorImp(getID(), AEDocumentType.valueOf(AEDocumentType.System.FinancialTransaction));
		createDescriptor(aeDescr);
		
		// additional initialization
		aeDescr.setDate(getDateTransaction());
		
		return aeDescr;
	}

	/**
	 * @return the accAccountCash
	 */
	public AEDescriptor getAccAccountCash() {
		return accAccountCash;
	}

	/**
	 * @param accAccountCash the accAccountCash to set
	 */
	public void setAccAccountCash(AEDescriptor accAccountCash) {
		this.accAccountCash = accAccountCash;
	}

	/**
	 * @return the cashAccounts
	 */
	public JSONArray getCashAccounts() {
		return cashAccounts;
	}

	/**
	 * @param cashAccounts the cashAccounts to set
	 */
	public void setCashAccounts(JSONArray cashAccounts) {
		this.cashAccounts = cashAccounts;
	}

	/**
	 * @return the appModule
	 */
	public AEDescriptor getAppModule() {
		return appModule;
	}

	/**
	 * @param appModule the appModule to set
	 */
	public void setAppModule(AEDescriptor appModule) {
		this.appModule = appModule;
	}

	/**
	 * @return the otherReceivablePayableAccounts
	 */
	public JSONArray getOtherReceivablePayableAccounts() {
		return otherReceivablePayableAccounts;
	}

	/**
	 * @param otherReceivablePayableAccounts the otherReceivablePayableAccounts to set
	 */
	public void setOtherReceivablePayableAccounts(JSONArray otherReceivablePayableAccounts) {
		this.otherReceivablePayableAccounts = otherReceivablePayableAccounts;
	}

	/**
	 * @return the accAccountOtherReceivablePayable
	 */
	public AEDescriptor getAccAccountOtherReceivablePayable() {
		return accAccountOtherReceivablePayable;
	}

	/**
	 * @param accAccountOtherReceivablePayable the accAccountOtherReceivablePayable to set
	 */
	public void setAccAccountOtherReceivablePayable(AEDescriptor accAccountOtherReceivablePayable) {
		this.accAccountOtherReceivablePayable = accAccountOtherReceivablePayable;
	}

	/**
	 * @return the contributor
	 */
	public Contributor getContributor() {
		return contributor;
	}

	/**
	 * @param contributor the contributor to set
	 */
	public void setContributor(Contributor contributor) {
		this.contributor = contributor;
	}

	/**
	 * @return the quetesList
	 */
	public QuetesList getQuetesList() {
		return quetesList;
	}

	/**
	 * @param quetesList the quetesList to set
	 */
	public void setQuetesList(QuetesList quetesList) {
		this.quetesList = quetesList;
	}

	/**
	 * @return the supplier
	 */
	public SimpleParty getSupplier() {
		return supplier;
	}

	/**
	 * @param supplier the supplier to set
	 */
	public void setSupplier(SimpleParty supplier) {
		this.supplier = supplier;
	}

	/**
	 * @param accAccountBalance the accAccountBalance to set
	 */
	public void setAccAccountBalance(AccAccountBalance accAccountBalance) {
		this.accAccountBalance = accAccountBalance;
	}

	/**
	 * @return the accAccountBalance
	 */
	public AccAccountBalance getAccAccountBalance() {
		return accAccountBalance;
	}
}
