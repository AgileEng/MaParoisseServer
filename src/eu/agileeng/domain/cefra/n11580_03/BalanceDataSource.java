package eu.agileeng.domain.cefra.n11580_03;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.acc.AccAccountBalanceExt;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AETimePeriod;

public class BalanceDataSource extends ReportDataSource {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2626926063200514515L;
	
	private AETimePeriod period;
	
	private String fromAccountCode;
	
	private String toAccountCode;
	
	private Map<String, AccAccountBalanceExt> balanceSheet;
	
	private Map<Integer, AccAccountBalanceExt> balanceSheetTotals = new HashMap<Integer, AccAccountBalanceExt>(6);

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

	/**
	 * @return the balance
	 */
	public Map<String, AccAccountBalanceExt> getBalanceSheet() {
		return balanceSheet;
	}

	/**
	 * @param balance the balance to set
	 */
	public void setBalanceSheet(Map<String, AccAccountBalanceExt> balanceSheet) {
		this.balanceSheet = balanceSheet;
	}

	public String getPeriodAsStringShort() {
		StringBuilder sb = new StringBuilder();
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.FRANCE);
		
		String fromDate = dateFormat.format(this.period.getStartDate());
		String toDate = dateFormat.format(this.period.getEndDate());
		sb.append(fromDate).append(" - ").append(toDate).append(", ").append(getYear());
		
		return sb.toString();
	}
	
	public List<String> getAccCodes() {
		List<String> accCodesList = null;
		if(this.balanceSheet != null) {
			accCodesList = new ArrayList<String>(this.balanceSheet.keySet());
		} else {
			accCodesList = new ArrayList<String>();
		}
		Collections.sort(accCodesList);
		return accCodesList;
	}
	
	private AccAccountBalanceExt getAccAccountBalance(String accCode) {
		AccAccountBalanceExt b = null;
		if(this.balanceSheet != null) {
			if(!AEStringUtil.isEmpty(accCode)) {
				if(accCode.length() < AccAccount.accLenght) {
					b = new AccAccountBalanceExt();
					double dtOpeningBalance = 0.0;
					double ctOpeningBalance = 0.0;
					double dtTurnover = 0.0;
					double ctTurnover = 0.0;
					double dtFinalBalance = 0.0;
					double ctFinalBalance = 0.0;
					Set<String> keys = this.balanceSheet.keySet();
					for (String key : keys) {
						if(key.startsWith(accCode)) {
							AccAccountBalanceExt item = this.balanceSheet.get(key);
							b = new AccAccountBalanceExt();
							
							// opening balance
							dtOpeningBalance += item.getDebitOpeningBalance();
							ctOpeningBalance += item.getCreditOpeningBalance();
							
							// turnover
							dtTurnover += item.getDebitTurnover();
							ctTurnover += item.getCreditTurnover();
							
							// final balance
							dtFinalBalance += item.getDebitFinalBalance();
							ctFinalBalance += item.getCreditFinalBalance();
						}
					}
					// opening balance
					b.setDebitOpeningBalance(dtOpeningBalance);
					b.setCreditOpeningBalance(ctOpeningBalance);
					
					// turnover
					b.setDebitTurnover(dtTurnover);
					b.setCreditTurnover(ctTurnover);
					
					// final balance
					b.setDebitFinalBalance(dtFinalBalance);
					b.setCreditFinalBalance(ctFinalBalance);
				} else {
					b = this.balanceSheet.get(accCode);
				}
			}
		}
		return b;
	}
	
	public String getAccName(String accCode) {
		String ret = null;
		AccAccountBalanceExt b = getAccAccountBalance(accCode);
		if(b != null) {
			ret = b.getAccAccount().getDescriptor().getName();
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getDebitTurnover(String accCode) {
		String ret = null;
		AccAccountBalanceExt b = getAccAccountBalance(accCode);
		if(b != null) {
			double amount = b.getDebitOpeningBalance() + b.getDebitTurnover();
			if(!AEMath.isZeroAmount(amount)) {
				ret = AEMath.toAmountFrenchString(amount);
			}
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getCreditTurnover(String accCode) {
		String ret = null;
		AccAccountBalanceExt b = getAccAccountBalance(accCode);
		if(b != null) {
			double amount = b.getCreditOpeningBalance() + b.getCreditTurnover();
			if(!AEMath.isZeroAmount(amount)) {
				ret = AEMath.toAmountFrenchString(amount);
			}
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getDebitBalance(String accCode) {
		String ret = null;
		AccAccountBalanceExt b = getAccAccountBalance(accCode);
		if(b != null) {
			double amount = b.getDebitFinalBalance();
			if(!AEMath.isZeroAmount(amount)) {
				ret = AEMath.toAmountFrenchString(amount);
			}
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getCreditBalance(String accCode) {
		String ret = null;
		AccAccountBalanceExt b = getAccAccountBalance(accCode);
		if(b != null) {
			double amount = b.getCreditFinalBalance();
			if(!AEMath.isZeroAmount(amount)) {
				ret = AEMath.toAmountFrenchString(amount);
			}
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getDebitTurnover(int lineCode) {
		String ret = null;
		AccAccountBalanceExt b = balanceSheetTotals.get(lineCode);
		if(b != null) {
			double amount = b.getDebitTurnover();
			if(!AEMath.isZeroAmount(amount)) {
				ret = AEMath.toAmountFrenchString(amount);
			}
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getCreditTurnover(int lineCode) {
		String ret = null;
		AccAccountBalanceExt b = balanceSheetTotals.get(lineCode);
		if(b != null) {
			double amount = b.getCreditTurnover();
			if(!AEMath.isZeroAmount(amount)) {
				ret = AEMath.toAmountFrenchString(amount);
			}
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getDebitBalance(int lineCode) {
		String ret = null;
		AccAccountBalanceExt b = balanceSheetTotals.get(lineCode);
		if(b != null) {
			double amount = b.getDebitFinalBalance();
			if(!AEMath.isZeroAmount(amount)) {
				ret = AEMath.toAmountFrenchString(amount);
			}
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getCreditBalance(int lineCode) {
		String ret = null;
		AccAccountBalanceExt b = balanceSheetTotals.get(lineCode);
		if(b != null) {
			double amount = b.getCreditFinalBalance();
			if(!AEMath.isZeroAmount(amount)) {
				ret = AEMath.toAmountFrenchString(amount);
			}
		}
		return AEStringUtil.trim(ret);
	}
	
	public void setup() {
		// lineCode 1, bilan total
		AccAccountBalanceExt bilanTotal = new AccAccountBalanceExt();
		balanceSheetTotals.put(1, bilanTotal);
		
		// lineCode 2, extra comptable
		AccAccountBalanceExt extraComptTotal = new AccAccountBalanceExt();
		balanceSheetTotals.put(2, extraComptTotal);
		
		// lineCode 3, charge total
		AccAccountBalanceExt chargeTotal = new AccAccountBalanceExt();
		balanceSheetTotals.put(3, chargeTotal);
		
		// lineCode 4, produit total
		AccAccountBalanceExt produitTotal = new AccAccountBalanceExt();
		balanceSheetTotals.put(4, produitTotal);
		
		// lineCode 5, charge and produit total
		AccAccountBalanceExt chargeProduitTotal = new AccAccountBalanceExt();
		balanceSheetTotals.put(5, chargeProduitTotal);

		// lineCode 6, general total
		AccAccountBalanceExt generalTotal = new AccAccountBalanceExt();
		balanceSheetTotals.put(6, generalTotal);

		// iterate and sum
		List<String> accCodes = getAccCodes();
		for (String accCode : accCodes) {
			AccAccountBalanceExt b = getAccAccountBalance(accCode);
			if(b != null) {
				// lineCode 1, bilan total
				if(accCode.startsWith("1") 
						|| accCode.startsWith("2") 
						|| accCode.startsWith("3") 
						|| accCode.startsWith("4") 
						|| accCode.startsWith("5")) {
					
					bilanTotal.setDebitTurnover(bilanTotal.getDebitTurnover() + b.getDebitOpeningBalance() + b.getDebitTurnover());
					bilanTotal.setCreditTurnover(bilanTotal.getCreditTurnover() + b.getCreditOpeningBalance() + b.getCreditTurnover());
					bilanTotal.setDebitFinalBalance(bilanTotal.getDebitFinalBalance() + b.getDebitFinalBalance());
					bilanTotal.setCreditFinalBalance(bilanTotal.getCreditFinalBalance() + b.getCreditFinalBalance());
				}
				
				// lineCode 2, total extra comptable
				if(accCode.startsWith("8") || accCode.startsWith("9")) {
					extraComptTotal.setDebitTurnover(extraComptTotal.getDebitTurnover() + b.getDebitOpeningBalance() + b.getDebitTurnover());
					extraComptTotal.setCreditTurnover(extraComptTotal.getCreditTurnover() + b.getCreditOpeningBalance() + b.getCreditTurnover());
					extraComptTotal.setDebitFinalBalance(extraComptTotal.getDebitFinalBalance() + b.getDebitFinalBalance());
					extraComptTotal.setCreditFinalBalance(extraComptTotal.getCreditFinalBalance() + b.getCreditFinalBalance());
				}
				
				// lineCode 3, charge total
				if(accCode.startsWith("6")) {
					chargeTotal.setDebitTurnover(chargeTotal.getDebitTurnover() + b.getDebitOpeningBalance() + b.getDebitTurnover());
					chargeTotal.setCreditTurnover(chargeTotal.getCreditTurnover() + b.getCreditOpeningBalance() + b.getCreditTurnover());
					chargeTotal.setDebitFinalBalance(chargeTotal.getDebitFinalBalance() + b.getDebitFinalBalance());
					chargeTotal.setCreditFinalBalance(chargeTotal.getCreditFinalBalance() + b.getCreditFinalBalance());
				}
				
				// lineCode 4, produit total
				if(accCode.startsWith("7")) {
					produitTotal.setDebitTurnover(produitTotal.getDebitTurnover() + b.getDebitOpeningBalance() + b.getDebitTurnover());
					produitTotal.setCreditTurnover(produitTotal.getCreditTurnover() + b.getCreditOpeningBalance() + b.getCreditTurnover());
					produitTotal.setDebitFinalBalance(produitTotal.getDebitFinalBalance() + b.getDebitFinalBalance());
					produitTotal.setCreditFinalBalance(produitTotal.getCreditFinalBalance() + b.getCreditFinalBalance());
				}
				
				
				// lineCode 5, charge and produit total
				if(accCode.startsWith("6") || accCode.startsWith("7")) {
					chargeProduitTotal.setDebitTurnover(chargeProduitTotal.getDebitTurnover() + b.getDebitOpeningBalance() + b.getDebitTurnover());
					chargeProduitTotal.setCreditTurnover(chargeProduitTotal.getCreditTurnover() + b.getCreditOpeningBalance() + b.getCreditTurnover());
					chargeProduitTotal.setDebitFinalBalance(chargeProduitTotal.getDebitFinalBalance() + b.getDebitFinalBalance());
					chargeProduitTotal.setCreditFinalBalance(chargeProduitTotal.getCreditFinalBalance() + b.getCreditFinalBalance());
				}

				// lineCode 6, general total
				generalTotal.setDebitTurnover(generalTotal.getDebitTurnover() + b.getDebitOpeningBalance() + b.getDebitTurnover());
				generalTotal.setCreditTurnover(generalTotal.getCreditTurnover() + b.getCreditOpeningBalance() + b.getCreditTurnover());
				generalTotal.setDebitFinalBalance(generalTotal.getDebitFinalBalance() + b.getDebitFinalBalance());
				generalTotal.setCreditFinalBalance(generalTotal.getCreditFinalBalance() + b.getCreditFinalBalance());
			}
		}
	}
}
