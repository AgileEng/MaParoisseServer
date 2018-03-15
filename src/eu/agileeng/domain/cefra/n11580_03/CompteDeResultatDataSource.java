package eu.agileeng.domain.cefra.n11580_03;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AETimePeriod;

public class CompteDeResultatDataSource extends ReportDataSource {

	/**
	 * 
	 */
	private static final long serialVersionUID = -467265517742769299L;
	
	private AETimePeriod period;
	
	private Map<String, Double> currentYear;
	
	private AETimePeriod previousPeriod;
	
	private Map<String, Double> previousYear;
	
	private Map<String, String> accNamesMap = new HashMap<String, String>();
	
	private double profitCurrent;
	
	private double lossCurrent;
	
	private double profitPrevious;
	
	private double lossPrevious;
	
	private String profitAccount;
	
	private String lossAccount;

	public CompteDeResultatDataSource() {
		accNamesMap.put("6", "CHARGES");
		accNamesMap.put("60", "Achats");
		accNamesMap.put("61", "Services extérieures");
		accNamesMap.put("62", "Autres services extérieures");
		accNamesMap.put("63", "Impôts et taxes");
		accNamesMap.put("64", "Charges de personnel");
		accNamesMap.put("65", "Versement au fonds pastoral");
		accNamesMap.put("66", "Charges financières");
		accNamesMap.put("67", "Charges exceptionnelles");
		accNamesMap.put("68", "Dotations aux amortissements et provisions");
		accNamesMap.put("69", "");
		accNamesMap.put("7", "PRODUITS");
		accNamesMap.put("70", "Recettes ordinaires");
		accNamesMap.put("71", "");
		accNamesMap.put("72", "");
		accNamesMap.put("73", "");
		accNamesMap.put("74", "Subventions");
		accNamesMap.put("75", "Produits accessoires");
		accNamesMap.put("76", "Produits financiers");
		accNamesMap.put("77", "Recettes et quêtes exceptionnelles");
		accNamesMap.put("78", "Reprises sur amortissements et provisions");
		accNamesMap.put("79", "PRODUITS");
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
	 * @return the previousPeriod
	 */
	public AETimePeriod getPreviousPeriod() {
		return previousPeriod;
	}

	/**
	 * @param previousPeriod the previousPeriod to set
	 */
	public void setPreviousPeriod(AETimePeriod previousPeriod) {
		this.previousPeriod = previousPeriod;
	}

	/**
	 * @return the currentYear
	 */
	public Map<String, Double> getCurrentYear() {
		return currentYear;
	}

	/**
	 * @param currentYear the currentYear to set
	 */
	public void setCurrentYear(Map<String, Double> currentYear) {
		this.currentYear = currentYear;
	}

	/**
	 * @return the previousYear
	 */
	public Map<String, Double> getPreviousYear() {
		return previousYear;
	}

	/**
	 * @param previousYear the previousYear to set
	 */
	public void setPreviousYear(Map<String, Double> previousYear) {
		this.previousYear = previousYear;
	}
	
	public boolean hasData(String accCode) {
		return (currentYear != null && currentYear.containsKey(accCode)) 
				|| (previousYear != null && previousYear.containsKey(accCode));
	}
	
	private double currentValue(String accCode) {
		Double value = null;
		if(currentYear != null) {
			value = currentYear.get(accCode);
			if(value != null && !AEMath.isZeroAmount(value)) {
				if(accCode.startsWith("7")) {
					value *= -1.0;
				}
			}
		}
		return AEMath.doubleValue(value);
	}
	
	private double previousValue(String accCode) {
		Double value = null;
		if(previousYear != null) {
			value = previousYear.get(accCode);
			if(value != null && !AEMath.isZeroAmount(value)) {
				if(accCode.startsWith("7")) {
					value *= -1.0;
				}
			}
		}
		return AEMath.doubleValue(value);
	}
	
	public String getCurrentValue(String accCode) {
		String ret = null;
		double value = currentValue(accCode);
		if(!AEMath.isZeroAmount(value)) {
			ret = AEMath.toAmountFrenchString(value);
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getPreviousValue(String accCode) {
		String ret = null;
		double value = previousValue(accCode);
		if(!AEMath.isZeroAmount(value)) {
			ret = AEMath.toAmountFrenchString(value);
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getDiffAmount(String accCode) {
		String ret = null;
		
		double currentValue = currentValue(accCode);
		double previousValue = previousValue(accCode);
		double diff = currentValue - previousValue;
		if(!AEMath.isZeroAmount(diff)) {
			ret = AEMath.toAmountFrenchString(diff);
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getDiffPercent(String accCode) {
		String ret = null;
		
		double currentValue = currentValue(accCode);
		double previousValue = previousValue(accCode);
		double diff = currentValue - previousValue;
		double diffPercent = 0.0;
		if(!AEMath.isZeroAmount(previousValue)) {
			diffPercent = diff / previousValue * 100;
		}
		
		// don't show zero percent
		if(!AEMath.isZeroAmount(diffPercent)) {
			ret = AEMath.toAmountFrenchString(diffPercent);
		}
		return AEStringUtil.trim(ret);
	}
	
	public List<String> getAccCodes(int level) {
		// collect as set
		Set<String> accCodes = new HashSet<String>();
		if(currentYear != null) {
			Set<String> keys = currentYear.keySet();
			for (String accCode : keys) {
				if(accCode.length() == level) {
					accCodes.add(accCode);
				}
			}
		}
		if(previousYear != null) {
			Set<String> keys = previousYear.keySet();
			for (String accCode : keys) {
				if(accCode.length() == level) {
					accCodes.add(accCode);
				}
			}
		}
		
		// to list and sort
		List<String> ret = new ArrayList<String>(accCodes);
		Collections.sort(ret);
		
		return ret;
	}
	
	public String getName(String accCode) {
		String ret = null;
		if(accNamesMap != null) {
			ret = accNamesMap.get(accCode);
		}
		return AEStringUtil.trim(ret);
	}

	/**
	 * @return the accNamesMap
	 */
	public Map<String, String> getAccNamesMap() {
		return accNamesMap;
	}
	
	public String getPeriodAsStringShort() {
		StringBuilder sb = new StringBuilder();
		SimpleDateFormat dateFormat = new SimpleDateFormat("MMMMM dd", Locale.FRANCE);
		
		String fromDate = dateFormat.format(this.period.getStartDate());
		String toDate = dateFormat.format(period.getEndDate());
		sb.append(fromDate).append(" - ").append(toDate);
		
		return sb.toString();
	}
	
	public String getCurrentDateYear() {
		String ret = Integer.toString(AEDateUtil.getYear(period.getStartDate()));
		return AEStringUtil.trim(ret);
	}
	
	public String getPrevousDateYear() {
		String ret = Integer.toString(AEDateUtil.getYear(previousPeriod.getStartDate()));
		return AEStringUtil.trim(ret);
	}
	
	public String getPeriodDatesAsString() {
		String ret = null;
		Date startDate = this.getPeriod().getStartDate();
		Date endDate = this.getPeriod().getEndDate();
		ret = AEDateUtil.formatToFrenchWithoutYear(startDate) + " - " + AEDateUtil.formatToFrenchWithoutYear(endDate);
		
		return AEStringUtil.trim(ret);
	}
	
	public double sumExpensesCurrent() {
		return currentValue("6");
	}
	
	public double sumIncomeCurrent() {
		return currentValue("7");
	}
	
	public double sumExpensesPrevious() {
		return previousValue("6");
	}
	
	public double sumIncomePrevious() {
		return previousValue("7");
	}

	/**
	 * @return the profitCurrent
	 */
	public String getProfitCurrent() {
		String ret = null;
		if(!AEMath.isZeroAmount(profitCurrent)) {
			ret = AEMath.toAmountFrenchString(profitCurrent);
		}
		return AEStringUtil.trim(ret);
	}

	/**
	 * @param profitCurrent the profitCurrent to set
	 */
	public void setProfitCurrent(double profitCurrent) {
		this.profitCurrent = profitCurrent;
	}

	/**
	 * @return the lossCurrent
	 */
	public String getLossCurrent() {
		String ret = null;
		if(!AEMath.isZeroAmount(lossCurrent)) {
			ret = AEMath.toAmountFrenchString(lossCurrent);
		}
		return AEStringUtil.trim(ret);
	}

	/**
	 * @param lossCurrent the lossCurrent to set
	 */
	public void setLossCurrent(double lossCurrent) {
		this.lossCurrent = lossCurrent;
	}

	/**
	 * @return the profitPrevious
	 */
	public String getProfitPrevious() {
		String ret = null;
		if(!AEMath.isZeroAmount(profitPrevious)) {
			ret = AEMath.toAmountFrenchString(profitPrevious);
		}
		return AEStringUtil.trim(ret);
	}

	/**
	 * @param profitPrevious the profitPrevious to set
	 */
	public void setProfitPrevious(double profitPrevious) {
		this.profitPrevious = profitPrevious;
	}

	/**
	 * @return the lossPrevious
	 */
	public String getLossPrevious() {
		String ret = null;
		if(!AEMath.isZeroAmount(lossPrevious)) {
			ret = AEMath.toAmountFrenchString(lossPrevious);
		}
		return AEStringUtil.trim(ret);
	}

	/**
	 * @param lossPrevious the lossPrevious to set
	 */
	public void setLossPrevious(double lossPrevious) {
		this.lossPrevious = lossPrevious;
	}

	/**
	 * @return the profitAccount
	 */
	public String getProfitAccount() {
		return profitAccount;
	}

	/**
	 * @param profitAccount the profitAccount to set
	 */
	public void setProfitAccount(String profitAccount) {
		this.profitAccount = profitAccount;
	}

	/**
	 * @return the lossAccount
	 */
	public String getLossAccount() {
		return lossAccount;
	}

	/**
	 * @param lossAccount the lossAccount to set
	 */
	public void setLossAccount(String lossAccount) {
		this.lossAccount = lossAccount;
	}
	
	public String getTotalCurrent() {
		String ret = null;
		double d = Math.max(sumExpensesCurrent(), sumIncomeCurrent()); 
		if(!AEMath.isZeroAmount(d)) {
			ret = AEMath.toAmountFrenchString(d);
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getTotalPrevious() {
		String ret = null;
		double d = Math.max(sumExpensesPrevious(), sumIncomePrevious()); 
		if(!AEMath.isZeroAmount(d)) {
			ret = AEMath.toAmountFrenchString(d);
		}
		return AEStringUtil.trim(ret); 
	}
}
