package eu.agileeng.domain.acc;

import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.accbureau.AEApp;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.acc.cashbasis.FinancialTransactionTemplate;
import eu.agileeng.domain.acc.cashbasis.FinancialTransactionTemplate.PaymentMethod;
import eu.agileeng.domain.business.bank.BankAccount;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEPersistent;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.json.JSONUtil;

public class AccJournalFilter extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1796242984014021124L;
	
	static public enum JSONKey {
		accJournalFilter,
		monthFilter,
		dateSpanFilter,
		paymentMethodId,
		bankAccountId,
		timeControl,
		year, 
		month,
		dateFrom,
		dateTo, 
		description, 
		amountFrom, 
		amountTo,
		journalCode,
		accCodeFrom,
		accCodeTo,
		tally;
	}
	
	static public enum TallyState {
		all,
		checked,
		unchecked;
	}
	
	private FinancialTransactionTemplate.PaymentMethod paymentMethod;
	
	private AEDescriptor bankAccount;
	
	private AEDescriptor accJournal;
	
	private Date dateFrom;
	
	private Date dateTo;
	
	private Double amountFrom;

	private Double amountTo;
	
	private View view = View.COARSE;
	
	private String accCodeFrom;
	
	private String accCodeTo;
	
	private TallyState tallyState = TallyState.all;
	
	/**
	 * Percent or Amount mode
	 * 
	 * @author vvatov
	 */
	static public enum View {
		NA(0L),
		COARSE(10L),
		DETAILED(20L);
				
		private long viewId;
		
		private View(long viewId) {
			this.viewId = viewId;
		}
		
		public final long getViewId() {
			return this.viewId;
		}
		
		public static View valueOf(long viewId) {
			View ret = null;
			for (View inst : View.values()) {
				if(inst.getViewId() == viewId) {
					ret = inst;
					break;
				}
			}
			if(ret == null) {
				ret = NA;
			}
			return ret;
		}
	}
	
	public AccJournalFilter() {
		super(DomainClass.AccJournalFilter);
	}


	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// private FinancialTransactionTemplate.PaymentMethod paymentMethod;
		if(jsonObject.has(JSONKey.paymentMethodId.name())) {
			Long tId = JSONUtil.optLong(jsonObject, JSONKey.paymentMethodId.name());
			if(tId != null && AEPersistent.ID.isPersistent(tId)) {
				setPaymentMethod(PaymentMethod.valueOf(tId));
			}
		}
		
		// private AEDescriptor bankAccount;
		if(jsonObject.has(JSONKey.bankAccountId.name())) {
			Long tId = JSONUtil.optLong(jsonObject, JSONKey.bankAccountId.name());
			if(tId != null && AEPersistent.ID.isPersistent(tId)) {
				setBankAccount(BankAccount.lazyDescriptor(tId));
			}
		}
		
		/**
		 * Period
		 */
		String timeControl = null;
		if(jsonObject.has(JSONKey.timeControl.name())) {
			timeControl = jsonObject.getString(JSONKey.timeControl.name());
			if(JSONKey.monthFilter.name().equalsIgnoreCase(timeControl)) {
				// month and year
				int month = jsonObject.getInt(JSONKey.month.name()) - 1; // zero based month
				int year = jsonObject.getInt(JSONKey.year.name());
				
				setDateFrom(AEDateUtil.getClearDateTime(AEDateUtil.getFirstDate(month, year)));
				setDateTo(AEDateUtil.getClearDateTime(AEDateUtil.getLastDate(month, year)));
			} else if(JSONKey.dateSpanFilter.name().equalsIgnoreCase(timeControl)) {
				// period
				setDateFrom(
						AEDateUtil.getClearDateTime(
								AEDateUtil.parseDateStrict(jsonObject.getString(JSONKey.dateFrom.name()))));
				setDateTo(
						AEDateUtil.getClearDateTime(
								AEDateUtil.parseDateStrict(jsonObject.getString(JSONKey.dateTo.name()))));
			}
		}
		
		// private Double amountFrom;
		if(jsonObject.has(JSONKey.amountFrom.name())) {
			try {
				setAmountFrom(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.amountFrom.name()));
			} catch (AEException e) {
				AEApp.logger().error(e.getMessage(), e);
			}
		}

		// private Double amountTo;
		if(jsonObject.has(JSONKey.amountTo.name())) {
			try {
				setAmountTo(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.amountTo.name()));
			} catch (AEException e) {
				AEApp.logger().error(e.getMessage(), e);
			}
		}
		
		// accJournal
		if(jsonObject.has(JSONKey.journalCode.name())) {
			String tCode = jsonObject.optString(JSONKey.journalCode.name());
			if(!AEStringUtil.isEmpty(tCode)) {
				setAccJournal(new AEDescriptorImp().withCode(tCode));
			}
		}
		
		// accCodeFrom
		if(jsonObject.has(JSONKey.accCodeFrom.name())) {
			String tCode = jsonObject.optString(JSONKey.accCodeFrom.name());
			if(!AEStringUtil.isEmpty(tCode)) {
				switch(tCode.length()) {
					case 1:
						tCode += "000";
						break;
					case 2:
						tCode += "00";
						break;
					case 3:
						tCode += "0";
						break;
					default:
						break;
				}
				setAccCodeFrom(tCode);
			}
		}
		
		// accCodeTo
		if(jsonObject.has(JSONKey.accCodeTo.name())) {
			String tCode = jsonObject.optString(JSONKey.accCodeTo.name());
			switch(tCode.length()) {
				case 1:
					tCode += "000";
					break;
				case 2:
					tCode += "00";
					break;
				case 3:
					tCode += "0";
					break;
				default:
					break;
			}
			if(!AEStringUtil.isEmpty(tCode)) {
				setAccCodeTo(tCode);
			}
		}
		
		// tallyButton
		if(jsonObject.has(JSONKey.tally.name())) {
			this.tallyState = TallyState.valueOf(jsonObject.optString(JSONKey.tally.name(), TallyState.all.name()));
		}
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		return json;
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


	/**
	 * @return the dateFrom
	 */
	public Date getDateFrom() {
		return dateFrom;
	}


	/**
	 * @param dateFrom the dateFrom to set
	 */
	public void setDateFrom(Date dateFrom) {
		this.dateFrom = dateFrom;
	}


	/**
	 * @return the dateTo
	 */
	public Date getDateTo() {
		return dateTo;
	}


	/**
	 * @param dateTo the dateTo to set
	 */
	public void setDateTo(Date dateTo) {
		this.dateTo = dateTo;
	}


	/**
	 * @return the amountFrom
	 */
	public Double getAmountFrom() {
		return amountFrom;
	}


	/**
	 * @param amountFrom the amountFrom to set
	 */
	public void setAmountFrom(Double amountFrom) {
		this.amountFrom = amountFrom;
	}


	/**
	 * @return the amountTo
	 */
	public Double getAmountTo() {
		return amountTo;
	}


	/**
	 * @param amountTo the amountTo to set
	 */
	public void setAmountTo(Double amountTo) {
		this.amountTo = amountTo;
	}

	/**
	 * @return the view
	 */
	public View getView() {
		return view;
	}


	/**
	 * @param view the view to set
	 */
	public void setView(View view) {
		this.view = view;
	}


	/**
	 * @return the accJournal
	 */
	public AEDescriptor getAccJournal() {
		return accJournal;
	}


	/**
	 * @param accJournal the accJournal to set
	 */
	public void setAccJournal(AEDescriptor accJournal) {
		this.accJournal = accJournal;
	}


	/**
	 * @return the accCodeFrom
	 */
	public String getAccCodeFrom() {
		return accCodeFrom;
	}


	/**
	 * @param accCodeFrom the accCodeFrom to set
	 */
	public void setAccCodeFrom(String accCodeFrom) {
		this.accCodeFrom = accCodeFrom;
	}


	/**
	 * @return the accCodeTo
	 */
	public String getAccCodeTo() {
		return accCodeTo;
	}


	/**
	 * @param accCodeTo the accCodeTo to set
	 */
	public void setAccCodeTo(String accCodeTo) {
		this.accCodeTo = accCodeTo;
	}


	/**
	 * @return the tallyState
	 */
	public TallyState getTallyState() {
		return tallyState;
	}


	/**
	 * @param tallyState the tallyState to set
	 */
	public void setTallyState(TallyState tallyState) {
		this.tallyState = tallyState;
	}
}
