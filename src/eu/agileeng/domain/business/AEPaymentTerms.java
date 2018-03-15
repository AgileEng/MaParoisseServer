package eu.agileeng.domain.business;

import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.util.AEDateUtil;

/**
 * @author vvatov
 * 
 * Be sure to be very specific in the wording of the terms of payment. 
 * For example, we do not speak of "payment at 30 days" 
 * but "payment at 30 days net", 
 * "payment at 30 days end of month" 
 * or "payment at 30 days end of month 10."
 * 
 * Example:
 * An invoice dated May 8, 2009 will expire on the following dates depending on the time of settlement provided:
 *	Expected conditions	Matureness	Settlement period in principle
 *	cash	 					May 8, 2009	 		0
 *	30 days net	 				June 8, 2009	 	30 days
 *	30 days end of the month	June 30, 2009	 	53 days (23 * 30)
 *	30 days end of month 10	 	July 10, 2009	 	63 days (23 * 30 +10)
 *	60 days net	 				July 8, 2009	 	60 days
 */
public class AEPaymentTerms extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9198952408315864619L;

	static public enum JSONKey {
		payTypeId,
		byDecade,
		delay,
		isDefault,
		endOfMonth,
		dayOfMonth,
		useFinancialMonth,
		paymentTerm,
		paymentTerms
	};
	
	// code, name and description are already defined in super
	
	// The default payment type: cheque, card, bank transfer ...
	private long payTypeId;
	
	// by decade ???
	private boolean byDecade;
	
	// delay in days
	private long delay;
	
	// whether this payment term is default or not
	private boolean isDefault;
	
	// whether the delay is net or endOfMonth 
	private boolean endOfMonth;
	
	// used this formulation: 
	// on the dayOfMonth(th) of the calendar month that is after 
	// delay and endOfMonth applied
	// 
	// 0 for: dont use day of months
	private int dayOfMonth;
	
	// whether to use financial month (30 days) or not
	private boolean useFinancialMonth;
	
	private AEDescriptive accAccount;
	
	public AEPaymentTerms() {
		super(DomainClass.AEPaymentTerms);
	}

	public long getDelay() {
		return delay;
	}

	public void setDelay(long delay) {
		this.delay = delay;
	}

	public boolean isDefault() {
		return isDefault;
	}

	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}

	public boolean isEndOfMonth() {
		return endOfMonth;
	}

	public void setEndOfMonth(boolean endOfMonth) {
		this.endOfMonth = endOfMonth;
	}

	public AEDescriptive getAccAccount() {
		return accAccount;
	}

	public void setAccAccount(AEDescriptive accAccount) {
		this.accAccount = accAccount;
	}
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.AEPaymentTerms);
	}
	
	public Date applyTerms(Date date) {
		Date resDate = null;
		if(date != null) {
			if(isEndOfMonth()) {
				resDate = AEDateUtil.getLastDate(
						AEDateUtil.getMonthInYear(date) - 1, AEDateUtil.getYear(date));
			} else {
				resDate = date;
			}
			resDate = AEDateUtil.addDaysToDate(resDate, (int) getDelay());
		}
		return resDate;
	}

	public boolean isByDecade() {
		return byDecade;
	}

	public void setByDecade(boolean byDecade) {
		this.byDecade = byDecade;
	}

	public int getDayOfMonth() {
		return dayOfMonth;
	}

	public void setDayOfMonth(int dayOfMonth) {
		this.dayOfMonth = dayOfMonth;
	}

	public boolean isUseFinancialMonth() {
		return useFinancialMonth;
	}

	public void setUseFinancialMonth(boolean useFinancialMonth) {
		this.useFinancialMonth = useFinancialMonth;
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		json.put("id", this.getID());
		json.put("code", this.getCode());
		json.put("name", this.getName());
		json.put("description", this.getDescription());
		json.put("properties", this.getProperties());
		json.put("byDecade", this.isByDecade());
		json.put("delay", this.getDelay());
		json.put("isDefault", this.isDefault());
		json.put("isEndOfMonth", this.isEndOfMonth());
		json.put("dayOfMonth", this.getDayOfMonth());
		json.put("isFinancialMonth", this.isUseFinancialMonth());
		json.put("paymentTypeId", this.getPayTypeId());
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		this.setProperties(jsonObject.optLong("properties"));
		this.setByDecade(jsonObject.optBoolean("byDecade"));
		this.setDelay(jsonObject.optLong("delay"));
		this.setDefault(jsonObject.optBoolean("isDefault"));
		this.setEndOfMonth(jsonObject.optBoolean("isEndOfMonth"));
		this.setDayOfMonth(jsonObject.optInt("dayOfMonth"));
		this.setUseFinancialMonth(jsonObject.optBoolean("isFinancialMonth"));
		this.setPayTypeId(jsonObject.optLong("paymentTypeId"));
	}

	public long getPayTypeId() {
		return payTypeId;
	}

	public void setPayTypeId(long payTypeId) {
		this.payTypeId = payTypeId;
	}
}
