package eu.agileeng.domain.contact;

import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;
import org.jboss.logging.Logger;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.json.JSONUtil;

public class ContributorDonation extends Contributor {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5576855452843037382L;
	
	protected static Logger LOG = Logger.getLogger(ContributorDonation.class);
	
	public Date date;
	
	private int year;
	
	public double amount;
	
	public double amountAcc;
	
	public double amountChange;
	
	// amount included in receipts
	private double amountReceipted;
	
	private AEDescriptor account;
	
	public ContributorDonation() {
		super(DomainClass.ContributorDonation);
	}

	/**
	 * @return the year
	 */
	public int getYear() {
		return year;
	}

	/**
	 * @param year the year to set
	 */
	public void setYear(int year) {
		this.year = year;
	}

	/**
	 * @return the account
	 */
	public AEDescriptor getAccount() {
		return account;
	}

	/**
	 * @param account the account to set
	 */
	public void setAccount(AEDescriptor account) {
		this.account = account;
	}
	
	@Override
	public void create(JSONObject json) throws JSONException {
		super.create(json);
		
		// public Date date;
		if(json.has("date")) {
			this.date = AEDateUtil.parseDateStrict(json.getString("date"));
		}
		
		// private int year;
		setYear(json.getInt("year"));
		
//		// public double amount;
//		try {
//			this.amount = AEMath.doubleValue(JSONUtil.parseDoubleStrict(json, "amount"));
//		} catch (AEException e) {
//			LOG.error("invalid amount ", e);
//		}
		
		// public double amountAcc;
		try {
			this.amountAcc = AEMath.doubleValue(JSONUtil.parseDoubleStrict(json, "amountAcc"));
		} catch (AEException e) {
			LOG.error("invalid amount ", e);
		}
		
		// public double amountChange;
		try {
			this.amountChange = AEMath.doubleValue(JSONUtil.parseDoubleStrict(json, "amountChange"));
		} catch (AEException e) {
			LOG.error("invalid amount ", e);
		}
		
		//private AEDescriptor account;
		if(json.has("accountId")) {
			long accId = json.getLong("accountId");
			AEDescriptor accDescr = AccAccount.lazyDescriptor(accId);
			setAccount(accDescr);
		}
		
		// At the end
		// Calculate amount
		amount = amountAcc + amountChange;
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		// public Date date;
		if(this.date != null) {
			json.put("date", AEDateUtil.formatToFrench(this.date));
		}
		
		// private int year;
		json.put("year", this.year);
		
		// public double amount;
		json.put("amount", amount);
		
		// public double amount;
		json.put("amountAcc", amountAcc);
		
		// public double amountChange;
		json.put("amountChange", amountChange);
		
		// public double amountEnd;
		json.put("amountEnd", amountAcc + amountChange);
		
		// amountReceipted;
		json.put("amountReceipted", getAmountReceipted());
		
		//private AEDescriptor account;
		if(getAccount() != null) {
			json.put("accountId", getAccount().getID());
			json.put("accountCode", getAccount().getCode());
			json.put("accountName", getAccount().getName());
		}
		
		return json;
	}

	/**
	 * @return the amountAcc
	 */
	public double getAmountAcc() {
		return amountAcc;
	}

	/**
	 * @param amountAcc the amountAcc to set
	 */
	public void setAmountAcc(double amountAcc) {
		this.amountAcc = amountAcc;
	}

	/**
	 * @return the amountReceipted
	 */
	public double getAmountReceipted() {
		return amountReceipted;
	}

	/**
	 * @param amountReceipted the amountReceipted to set
	 */
	public void setAmountReceipted(double amountReceipted) {
		this.amountReceipted = amountReceipted;
	}
}
