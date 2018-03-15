package eu.agileeng.domain.cefra.n11580_03;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import eu.agileeng.domain.acc.AccJournalItem;
import eu.agileeng.domain.acc.AccJournalResult;
import eu.agileeng.domain.acc.AccJournalResultsList;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AETimePeriod;

public class GrandLivreDataSource extends ReportDataSource {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2155865591604283161L;

	/**
	 * 
	 */
	
	private AETimePeriod period;
	
	private String fromAccountCode;
	
	private String toAccountCode;
	
	/**
	 * Accounts
	 */
	private List<String> accounts = new ArrayList<String>();
	
	/**
	 * Balance in the begining of the financial year
	 */
	private Map<String, AccJournalResult> initialBalance = new HashMap<String, AccJournalResult>();
	
	/**
	 * Accumulated turnovers between initial balance and begin of the period
	 */
	private Map<String, AccJournalResult> accumulatedTurnoverBefore = new HashMap<String, AccJournalResult>();
	
	/**
	 * Turnover in the period
	 */
	private Map<String, AccJournalResultsList> grandLivre = new HashMap<String, AccJournalResultsList>();

	/**
	 * Total records
	 */
	private Map<String, AccJournalResult> total = new HashMap<String, AccJournalResult>();
	
	public GrandLivreDataSource() {

	}
	
	/**
	 * @return the period
	 */
	public AETimePeriod getPeriod() {
		return period;
	}

	/**
	 * @param period the period to set
	 */
	public void setPeriod(AETimePeriod period) {
		this.period = period;
	}

	/**
	 * @return the List<String>
	 */
	public List<String> getAccountsList() {
		return accounts;
	}
	
	public AccJournalResultsList getAccJournalResultsList(String accCode) {
		return grandLivre.get(accCode);
	}
	
	public static String getDate(AccJournalResult ajResult) {
		String ret = null;
		if(ajResult != null && ajResult.getAccJournalItem() != null && ajResult.getAccJournalItem().getDate() != null) {
			ret = AEDateUtil.formatToFrench(ajResult.getAccJournalItem().getDate());
		}
		return AEStringUtil.trim(ret);
	}
	
	public static String getJournal(AccJournalResult ajResult) {
		String ret = null;
		if(ajResult != null && ajResult.getAccJournalItem() != null && ajResult.getAccJournalItem().getJournal() != null) {
			ret = ajResult.getAccJournalItem().getJournal().getCode();
		}
		return AEStringUtil.trim(ret);
	}
	
	public static String getAccount(AccJournalResult ajResult) {
		String ret = null;
		if(ajResult != null && ajResult.getAccJournalItem() != null && ajResult.getAccJournalItem().getAccount() != null) {
			ret = ajResult.getAccJournalItem().getAccount().getCode() + " - " + ajResult.getAccJournalItem().getAccount().getName();
		}
		return AEStringUtil.trim(ret);
	}
	
	public static String getDebitAmount(AccJournalResult ajResult) {
		String ret = null;
		if(ajResult != null && ajResult.getAccJournalItem() != null && ajResult.getAccJournalItem().getDtAmount() != null) {
			double amount = ajResult.getAccJournalItem().getDtAmount();
			if(!AEMath.isZeroAmount(amount)) {
				ret = AEMath.toAmountFrenchString(amount);
			}
		}
		return AEStringUtil.trim(ret);
	}
	
	public static String getCreditAmount(AccJournalResult ajResult) {
		String ret = null;
		if(ajResult != null && ajResult.getAccJournalItem() != null && ajResult.getAccJournalItem().getCtAmount() != null) {
			double amount = ajResult.getAccJournalItem().getCtAmount();
			if(!AEMath.isZeroAmount(amount)) {
				ret = AEMath.toAmountFrenchString(amount);
			}
		}
		return AEStringUtil.trim(ret);
	}
	
	public static String getBalance(AccJournalResult ajResult) {
		String ret = null;
		if(ajResult != null && ajResult.getAccJournalItem() != null && ajResult.getClosingBalance() != null) {
			double amount = ajResult.getClosingBalance();
			if(AEMath.isPositiveAmount(amount)) {
				ret = AEMath.toAmountFrenchString(amount) + " D";
			} else if(AEMath.isNegativeAmount(amount)) {
				ret = AEMath.toAmountFrenchString(Math.abs(amount)) + " C";
			} else {
				ret = AEMath.toAmountFrenchString(amount);
			};
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getDebitAmount(String accCode, int rowType) {
		String ret = null;
		AccJournalResult result = null;
		switch(rowType) {
			case 1:
				if(this.initialBalance != null) {
					result = this.initialBalance.get(accCode); 
				}
				if(this.accumulatedTurnoverBefore != null) {
					result = this.accumulatedTurnoverBefore.get(accCode); 
				}
				break;
			case 2:
				if(this.accumulatedTurnoverBefore != null) {
					result = this.accumulatedTurnoverBefore.get(accCode); 
				}
				break;
			case 3:
				if(this.total != null) {
					result = this.total.get(accCode); 
				}
				break;
			default:
				break;
		}
		if(result != null) {
			double amount = result.getDtAmount();
			if(!AEMath.isZeroAmount(amount)) {
				ret = AEMath.toAmountFrenchString(amount);
			}
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getCreditAmount(String accCode, int rowType) {
		String ret = null;
		AccJournalResult result = null;
		switch(rowType) {
			case 1:
				if(this.initialBalance != null) {
					result = this.initialBalance.get(accCode); 
				}
				if(this.accumulatedTurnoverBefore != null) {
					result = this.accumulatedTurnoverBefore.get(accCode); 
				}
				break;
			case 2:
				if(this.accumulatedTurnoverBefore != null) {
					result = this.accumulatedTurnoverBefore.get(accCode); 
				}
				break;
			case 3:
				if(this.total != null) {
					result = this.total.get(accCode); 
				}
				break;
			default:
				break;
		}
		if(result != null) {
			double amount = result.getCtAmount();
			if(!AEMath.isZeroAmount(amount)) {
				ret = AEMath.toAmountFrenchString(amount);
			}
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getBalance(String accCode, int rowType) {
		String ret = null;
		AccJournalResult result = null;
		switch(rowType) {
			case 1:
				if(this.initialBalance != null) {
					result = this.initialBalance.get(accCode); 
				}
				if(this.accumulatedTurnoverBefore != null) {
					result = this.accumulatedTurnoverBefore.get(accCode); 
				}
				break;
			case 2:
				if(this.accumulatedTurnoverBefore != null) {
					result = this.accumulatedTurnoverBefore.get(accCode); 
				}
				break;
			case 3:
				if(this.total != null) {
					result = this.total.get(accCode); 
				}
				break;
			default:
				break;
		}
		if(result != null) {
			double amount = result.getClosingBalance();
			if(AEMath.isPositiveAmount(amount)) {
				ret = AEMath.toAmountFrenchString(amount) + " D";
			} else if(AEMath.isNegativeAmount(amount)) {
				ret = AEMath.toAmountFrenchString(Math.abs(amount)) + " C";
			} else {
				ret = AEMath.toAmountFrenchString(amount);
			};
		}
		return AEStringUtil.trim(ret);
	}
	
	public static String getDescription(AccJournalResult ajResult) {
		String ret = null;
		if(ajResult != null && ajResult.getAccJournalItem() != null) {
			ret = ajResult.getAccJournalItem().getDescription();
		}
		return AEStringUtil.trim(ret);
	}
	
	public static String getDonateur(AccJournalResult ajResult) {
		String ret = null;
		if(ajResult != null && ajResult.getContributor() != null) {
			ret = ajResult.getContributor().getDescriptor().getName();
		}
		return AEStringUtil.trim(ret);
	}
	
	public static String getSupplier(AccJournalResult ajResult) {
		String ret = null;
		if(ajResult != null && ajResult.getSupplier() != null) {
			ret = ajResult.getSupplier().getDescriptor().getName();
		}
		return AEStringUtil.trim(ret);
	}
	
	public static String getCodeQuete(AccJournalResult ajResult) {
		String ret = null;
		if(ajResult != null && ajResult.getAccJournalItem() != null && ajResult.getAccJournalItem().getQuete() != null) {
			ret = ajResult.getAccJournalItem().getQuete().getCode();
		}
		return AEStringUtil.trim(ret);
	}

	/**
	 * @return the fromAccountCode
	 */
	public String getFromAccountCode() {
		return fromAccountCode;
	}

	/**
	 * @param fromAccountCode the fromAccountCode to set
	 */
	public void setFromAccountCode(String fromAccountCode) {
		this.fromAccountCode = fromAccountCode;
	}

	/**
	 * @return the toAccountCode
	 */
	public String getToAccountCode() {
		return toAccountCode;
	}

	/**
	 * @param toAccountCode the toAccountCode to set
	 */
	public void setToAccountCode(String toAccountCode) {
		this.toAccountCode = toAccountCode;
	}
	
	public String getPeriodAsStringShort() {
		StringBuilder sb = new StringBuilder();
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.FRANCE);
		
		String fromDate = dateFormat.format(this.period.getStartDate());
		String toDate = dateFormat.format(this.period.getEndDate());
		sb.append(fromDate).append(" - ").append(toDate).append(", ").append(getYear());
		
		return sb.toString();
	}
	
	public String getStartDate() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		Date date = AEDateUtil.addDaysToDate(this.period.getStartDate(), -1);
		String dateStr = dateFormat.format(date);
		return dateStr;
	}

	/**
	 * @return the grandLivre
	 */
	public Map<String, AccJournalResultsList> getGrandLivre() {
		return grandLivre;
	}

	/**
	 * @param grandLivre the grandLivre to set
	 */
	public void setGrandLivre(Map<String, AccJournalResultsList> grandLivre) {
		this.grandLivre = grandLivre;
	}

	/**
	 * @return the initialBalance
	 */
	public Map<String, AccJournalResult> getInitialBalance() {
		return initialBalance;
	}

	/**
	 * @param initialBalance the initialBalance to set
	 */
	public void setInitialBalance(Map<String, AccJournalResult> initialBalance) {
		this.initialBalance = initialBalance;
	}

	/**
	 * @return the accumulatedTurnoverBefore
	 */
	public Map<String, AccJournalResult> getAccumulatedTurnoverBefore() {
		return accumulatedTurnoverBefore;
	}

	/**
	 * @param accumulatedTurnoverBefore the accumulatedTurnoverBefore to set
	 */
	public void setAccumulatedTurnoverBefore(
			Map<String, AccJournalResult> accumulatedTurnoverBefore) {
		this.accumulatedTurnoverBefore = accumulatedTurnoverBefore;
	}
	
	public void calculate() {
		List<String> accountsList = getAccountsList();
		for (Iterator<String> iterator = accountsList.iterator(); iterator.hasNext();) {
			String accCode = (String) iterator.next();
			AccJournalResult total = new AccJournalResult().withAccJournalItem(new AccJournalItem());
			double progresiveBalance = 0.0;
			
			// initial balance
			if(this.initialBalance != null) {
				AccJournalResult initBalance = this.initialBalance.get(accCode);
				if(initBalance != null) {
					initBalance.setOpeningBalance(0.0);
					initBalance.calculateClosingBalance();
					progresiveBalance = initBalance.getClosingBalance();
				}
			}
			
			// accumulated turnover
			if(this.accumulatedTurnoverBefore != null) {
				AccJournalResult accumuledTurnover = this.accumulatedTurnoverBefore.get(accCode);
				if(accumuledTurnover == null) {
					accumuledTurnover = new AccJournalResult().withAccJournalItem(new AccJournalItem());
					this.accumulatedTurnoverBefore.put(accCode, accumuledTurnover);
				}
				if(accumuledTurnover != null) {
					if(this.initialBalance != null) {
						AccJournalResult initBalance = this.initialBalance.get(accCode);
						if(initBalance != null) {
							accumuledTurnover.getAccJournalItem().setDtAmount(initBalance.getDtAmount() + accumuledTurnover.getDtAmount());
							accumuledTurnover.getAccJournalItem().setCtAmount(initBalance.getCtAmount() + accumuledTurnover.getCtAmount());
						}
					}
					accumuledTurnover.calculateClosingBalance();
					progresiveBalance = accumuledTurnover.getClosingBalance();
					
					// process total
					total.getAccJournalItem().setDtAmount(total.getDtAmount() + accumuledTurnover.getDtAmount());
					total.getAccJournalItem().setCtAmount(total.getCtAmount() + accumuledTurnover.getCtAmount());
				}
			}

			// turnover in the period
			if(this.grandLivre != null) {
				AccJournalResultsList turnoverList = this.grandLivre.get(accCode);
				if(turnoverList != null) {
					for (Iterator<AccJournalResult> iterator2 = turnoverList.iterator(); iterator2.hasNext();) {
						AccJournalResult accJournalResult = (AccJournalResult) iterator2.next();
						
						accJournalResult.setOpeningBalance(progresiveBalance);
						accJournalResult.calculateClosingBalance();
						progresiveBalance = accJournalResult.getClosingBalance();
						
						// process total
						total.getAccJournalItem().setDtAmount(total.getDtAmount() + accJournalResult.getDtAmount());
						total.getAccJournalItem().setCtAmount(total.getCtAmount() + accJournalResult.getCtAmount());
					}
				}
			}
			
			// total
			if(this.total != null) {
				total.calculateClosingBalance();
				this.total.put(accCode, total);
			}
		}
	}

	/**
	 * @return the total
	 */
	public Map<String, AccJournalResult> getTotal() {
		return total;
	}

	/**
	 * @param total the total to set
	 */
	public void setTotal(Map<String, AccJournalResult> total) {
		this.total = total;
	}

	/**
	 * @return the accounts
	 */
	public List<String> getAccounts() {
		return accounts;
	}

	/**
	 * @param accounts the accounts to set
	 */
	public void setAccounts(List<String> accounts) {
		this.accounts = accounts;
	}
	
	/**
	 * Detects whether specified <code>account</code> has to be shown in the report or not.
	 * An account must be shown if there are operations or there is a balance value.
	 * 
	 * @param accCode
	 * @return
	 */
	public boolean hasToBeShown(String accCode) {
		boolean bRes = false;
		
		// have operations or has a balance value
		AccJournalResultsList ajrl = getAccJournalResultsList(accCode);
		if(ajrl != null && !ajrl.isEmpty()) {
			bRes = true;
		} else if(!AEStringUtil.isEmpty(getDebitAmount(accCode, 1))) {
			bRes = true;
		} else if(!AEStringUtil.isEmpty(getCreditAmount(accCode, 1))) {
			bRes = true;
		} else if(!"0,00".equalsIgnoreCase(getBalance(accCode, 1)) && !AEStringUtil.isEmpty(getBalance(accCode, 1))) {
			bRes = true;
		}
		
		return bRes;
	}
}
