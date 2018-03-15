package eu.agileeng.domain.acc;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel;
import eu.agileeng.domain.acc.cashbasis.FinancialTransaction;
import eu.agileeng.domain.acc.cashbasis.FinancialTransactionTemplate;
import eu.agileeng.domain.acc.cashbasis.FinancialTransactionTemplate.PaymentMethod;
import eu.agileeng.persistent.AEPersistent;

public class AccJournal extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9210631188146668339L;

	static public enum JSONKey {
		paymentMethodId,
		bankAccountId,
		loadAccAccounts,
		loadSuppliers,
		accJournal;
	}
	
	private FinancialTransactionTemplate.PaymentMethod paymentMethod = FinancialTransactionTemplate.PaymentMethod.NA;
	
	private AEDescriptor bankAccount;
	
	/**
	 * @param clazz
	 */
	public AccJournal() {
		super(DomainModel.DomainClass.AccJournal);
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
	public void setPaymentMethod(
			FinancialTransactionTemplate.PaymentMethod paymentMethod) {
		this.paymentMethod = paymentMethod;
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
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject jsonObject = super.toJSONObject();
		
		// paymentMethod
		if(this.paymentMethod != null) {
			jsonObject.put(JSONKey.paymentMethodId.name(), this.paymentMethod.getId());
		}
		
		// bankAccount
		if(getBankAccount() != null) {
			jsonObject.put(JSONKey.bankAccountId.name(), getBankAccount().getID());
		}
		
		return jsonObject;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// payment method (required)
		setPaymentMethod(PaymentMethod.valueOf(jsonObject.getLong(JSONKey.paymentMethodId.name())));
		
		// bankAccountId (optional)
		if(jsonObject.has(FinancialTransaction.JSONKey.bankAccountId.name())) {
			long bankAccountId = jsonObject.optLong(FinancialTransaction.JSONKey.bankAccountId.name());
			if(AEPersistent.ID.isPersistent(bankAccountId)) {
				AEDescriptor bankAccountDescr = AccAccount.lazyDescriptor(bankAccountId);
				setBankAccount(bankAccountDescr);
			}
		}
	}
}
