package eu.agileeng.domain.acc;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AETimePeriod;

/**
 * Represents the db stored balance for one month
 * 
 * @author vvatov
 *
 */
public class AccAccountBalance extends AEDomainObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2669312271204755460L;
	
	static public enum JSONKey {
		accBalance,
		accId,
		accCode,
		accName,
		accDescription,
		dateFrom,
		dateTo,
		openingBalance,
		debitTurnover,
		creditTurnover,
		finalBalance;
	}
	
	private AEDescriptive accAccount;
	
	private AETimePeriod period;
	
	private double openingBalance;
	
	private double debitTurnover;
	
	private double creditTurnover;
	
	private double finalBalance;
	
	private int accType;
	
	public static final int ACC_TYPE_CLASS = 10;
	
	public static final int ACC_TYPE_GROUP = 20;
	
	public static final int ACC_TYPE_ACCOUNT = 30;
	
	public static final int ACC_TYPE_SUBACCOUNT = 40;

	/**
	 * @param clazz
	 */
	public AccAccountBalance() {
		super(DomainClass.AccAccountBalance);
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

	public AEDescriptive getAccAccount() {
		return accAccount;
	}

	public void setAccAccount(AEDescriptive accAccount) {
		this.accAccount = accAccount;
	}
	
	public AccAccountBalance withAccAccount(AEDescriptive accAccount) {
		setAccAccount(accAccount);
		return this;
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();

		// private AEDescriptive accAccount;
		if(accAccount != null) {
			json.put(JSONKey.accId.name(), accAccount.getDescriptor().getID());
			json.put(JSONKey.accCode.name(), accAccount.getDescriptor().getCode());
			json.put(JSONKey.accName.name(), accAccount.getDescriptor().getName());
			json.put(JSONKey.accDescription.name(), accAccount.getDescriptor().getDescription());
		} else {
			json.put(JSONKey.accId.name(), AEStringUtil.EMPTY_STRING);
			json.put(JSONKey.accCode.name(), AEStringUtil.EMPTY_STRING);
			json.put(JSONKey.accName.name(), AEStringUtil.EMPTY_STRING);
			json.put(JSONKey.accDescription.name(), AEStringUtil.EMPTY_STRING);
		}
		
		// dateFrom;
		if(period != null && period.getStartDate() != null) {
			json.put(JSONKey.dateFrom.name(), AEDateUtil.formatToFrench(period.getStartDate()));
		} else {
			json.put(JSONKey.dateFrom.name(), AEStringUtil.EMPTY_STRING);
		}
		
		// dateTo;
		if(period != null && period.getEndDate() != null) {
			json.put(JSONKey.dateTo.name(), AEDateUtil.formatToFrench(period.getEndDate()));
		} else {
			json.put(JSONKey.dateTo.name(), AEStringUtil.EMPTY_STRING);
		}
		
		// private double openingBalance;
		json.put(JSONKey.openingBalance.name(), openingBalance);
		
		// private double debitTurnover;
		json.put(JSONKey.debitTurnover.name(), debitTurnover);
		
		// private double creditTurnover;
		json.put(JSONKey.creditTurnover.name(), creditTurnover);
		
		// private double finalBalance; 
		json.put(JSONKey.finalBalance.name(), finalBalance);
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
	}

	/**
	 * Calculates final balance from current openingBalance, debitTurnover and creditTurnover
	 */
	public void calculateFinalBalance() {
		this.finalBalance = AEMath.round(this.openingBalance + this.debitTurnover - this.creditTurnover, 2);
	}
	
	/**
	 * Transorms to opening balance
	 */
	public void transformToOpeningBalance() {
		this.openingBalance = 0.0;
		if(debitTurnover > creditTurnover) {
			this.debitTurnover = debitTurnover - creditTurnover;
			this.creditTurnover = 0.0;
		} else if(creditTurnover > debitTurnover) {
			this.creditTurnover = creditTurnover - debitTurnover;
			this.debitTurnover = 0.0;
		} else {
			this.debitTurnover = 0.0;
			this.creditTurnover = 0.0;
		}
		calculateFinalBalance();
	}
	
	public boolean isNull() {
		return openingBalance == 0.0 
				&& debitTurnover == 0.0
				&& creditTurnover == 0.0
				&& finalBalance == 0.0;
	}

	/**
	 * @return the datePeriod
	 */
	public AETimePeriod getPeriod() {
		return period;
	}

	/**
	 * @param datePeriod the datePeriod to set
	 */
	public void setPeriod(AETimePeriod period) {
		this.period = period;
	}

	public AccAccountBalance withPeriod(AETimePeriod period) {
		setPeriod(period);
		return this;
	}
	
	/**
	 * @return the accType
	 */
	public int getAccType() {
		return accType;
	}

	/**
	 * @param accType the accType to set
	 */
	public void setAccType(int accType) {
		this.accType = accType;
	}
}
