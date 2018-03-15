/**
 * 
 */
package eu.agileeng.domain.business.bank;

import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.acc.AccPeriod;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.json.JSONUtil;

/**
 * Represents the db stored balance for one month
 * 
 * @author vvatov
 *
 */
public class BankAccountBalance extends AEDomainObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2669312271204755460L;
	
	private AEDescriptive bankAccount;
	
	private AEDescriptive accPeriod;
	
	private double openingBalance;
	
	private double debitTurnover;
	
	private double creditTurnover;
	
	private double finalBalance; 
	
	private double bankOpeningBalance;
	
	private Date bankOpeningBalanceDate;
	
	private double bankFinalBalance; 
	
	private Date bankFinalBalanceDate; 

	/**
	 * @param clazz
	 */
	public BankAccountBalance() {
		super(DomainClass.BankAccountBalance);
	}

	public AEDescriptive getAccPeriod() {
		return accPeriod;
	}

	public void setAccPeriod(AEDescriptive accPeriod) {
		this.accPeriod = accPeriod;
	}

	public double getOpeningBalance() {
		return openingBalance;
	}

	public void setOpeningBalance(double openingBalance) {
		this.openingBalance = openingBalance;
	}

	public double getDebitTurnover() {
		return debitTurnover;
	}

	public void setDebitTurnover(double debitTurnover) {
		this.debitTurnover = debitTurnover;
	}

	public double getCreditTurnover() {
		return creditTurnover;
	}

	public void setCreditTurnover(double creditTurnover) {
		this.creditTurnover = creditTurnover;
	}

	public double getFinalBalance() {
		return finalBalance;
	}

	public void setFinalBalance(double closingBalance) {
		this.finalBalance = closingBalance;
	}

	public AEDescriptive getBankAccount() {
		return bankAccount;
	}

	public void setBankAccount(AEDescriptive bankAccount) {
		this.bankAccount = bankAccount;
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();

		if(getBankAccount() != null) {
			json.put("bankAccId", getBankAccount().getDescriptor().getID());
		}
		if(getAccPeriod() != null) {
			json.put("accPeriodId", getAccPeriod().getDescriptor().getID());
		}
		json.put("openingBalance", getOpeningBalance());
		json.put("debitTurnover", getDebitTurnover());
		json.put("creditTurnover", getCreditTurnover());
		json.put("finalBalance", getFinalBalance());
		json.put("bankOpeningBalance", getBankOpeningBalance());
		if(getBankOpeningBalanceDate() != null) {
			json.put(
					"bankOpeningBalanceDate", 
					AEDateUtil.convertToString(getBankOpeningBalanceDate(), AEDateUtil.SYSTEM_DATE_FORMAT));	
		} else {
			json.put(
					"bankOpeningBalanceDate", 
					AEStringUtil.EMPTY_STRING);
		}
		json.put("bankFinalBalance", getBankFinalBalance());
		if(getBankFinalBalanceDate() != null) {
			json.put(
					"bankFinalBalanceDate", 
					AEDateUtil.convertToString(getBankFinalBalanceDate(), AEDateUtil.SYSTEM_DATE_FORMAT));	
		} else {
			json.put(
					"bankFinalBalanceDate", 
					AEStringUtil.EMPTY_STRING);
		}

		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		if(jsonObject.has("bankAccId")) {
			long bankAccId = jsonObject.optLong("bankAccId");
			if(bankAccId > 0) {
				setBankAccount(BankAccount.lazyDescriptor(bankAccId));
			}
		}
		if(jsonObject.has("accPeriodId")) {
			long accPeriodId = jsonObject.optLong("accPeriodId");
			if(accPeriodId > 0) {
				setAccPeriod(AccPeriod.lazyDescriptor(accPeriodId));
			}
		}
		setOpeningBalance(JSONUtil.normDouble(jsonObject, "openingBalance"));
		setDebitTurnover(JSONUtil.normDouble(jsonObject, "debitTurnover"));
		setCreditTurnover(JSONUtil.normDouble(jsonObject, "creditTurnover"));
		setFinalBalance(JSONUtil.normDouble(jsonObject, "finalBalance"));
		
		setBankOpeningBalance(JSONUtil.normDouble(jsonObject, "bankOpeningBalance"));
		if(jsonObject.has("bankOpeningBalanceDate")) {
			try {
				setBankOpeningBalanceDate(
						AEDateUtil.parseDateStrict(jsonObject.optString("bankOpeningBalanceDate")));
			} catch(Exception e) {}
		}
		setBankFinalBalance(JSONUtil.normDouble(jsonObject, "bankFinalBalance"));
		if(jsonObject.has("bankFinalBalanceDate")) {
			try {
				setBankFinalBalanceDate(
						AEDateUtil.parseDateStrict(jsonObject.optString("bankFinalBalanceDate")));
			} catch(Exception e) {}
		}
	}
	
	/**
	 * Calculates debitTurnover, creditTurnover and finalBalance
	 * from current openingBalance and specified <code>bankTransList</code>.
	 * 
	 * @param bankTransList
	 */
	public void calculate(BankTransactionsList bankTransList) {
		this.debitTurnover = 0.0;
		this.creditTurnover = 0.0;
		if(bankTransList != null && !bankTransList.isEmpty()) {
			for (BankTransaction bankTransaction : bankTransList) {
				// filter 
				if(BankTransaction.CODE_MOVEMENT.equals(bankTransaction.getCode().trim())) {
					if(bankTransaction.getDtAmount() != null && !bankTransaction.getDtAmount().isNaN()) {
						this.debitTurnover += bankTransaction.getDtAmount();
					}
					this.debitTurnover = AEMath.round(this.debitTurnover, 2);

					if(bankTransaction.getCtAmount() != null && !bankTransaction.getCtAmount().isNaN()) {
						this.creditTurnover += bankTransaction.getCtAmount();
					}
					this.creditTurnover = AEMath.round(this.creditTurnover, 2);
				}
			}
		}
		this.finalBalance = AEMath.round(this.openingBalance + this.debitTurnover - this.creditTurnover, 2);
	}

	/**
	 * Calculates final balance from current openingBalance, debitTurnover and creditTurnover
	 */
	public void calculateFinalBalance() {
		this.finalBalance = AEMath.round(this.openingBalance + this.debitTurnover - this.creditTurnover, 2);
	}
	
	public double getBankOpeningBalance() {
		return bankOpeningBalance;
	}

	public void setBankOpeningBalance(double bankOpeningBalance) {
		this.bankOpeningBalance = bankOpeningBalance;
	}

	public double getBankFinalBalance() {
		return bankFinalBalance;
	}

	public void setBankFinalBalance(double bankFinalBalance) {
		this.bankFinalBalance = bankFinalBalance;
	}

	public Date getBankOpeningBalanceDate() {
		return bankOpeningBalanceDate;
	}

	public void setBankOpeningBalanceDate(Date bankOpeningBalanceDate) {
		this.bankOpeningBalanceDate = bankOpeningBalanceDate;
	}

	public Date getBankFinalBalanceDate() {
		return bankFinalBalanceDate;
	}

	public void setBankFinalBalanceDate(Date bankFinalBalanceDate) {
		this.bankFinalBalanceDate = bankFinalBalanceDate;
	}
	
	public boolean isNull() {
		return openingBalance == 0.0 
				&& debitTurnover == 0.0
				&& creditTurnover == 0.0
				&& finalBalance == 0.0 
				&& bankOpeningBalance == 0.0
				&& bankOpeningBalanceDate == null
				&& bankFinalBalance == 0.0 
		        && bankFinalBalanceDate == null;
	}
}
