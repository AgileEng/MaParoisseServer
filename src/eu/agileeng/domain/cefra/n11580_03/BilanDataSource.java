package eu.agileeng.domain.cefra.n11580_03;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import eu.agileeng.domain.AccAccountBalancesList;
import eu.agileeng.domain.acc.AccAccountBalance;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AETimePeriod;

public class BilanDataSource extends ReportDataSource {
	/**
	 * 
	 */
	private static final long serialVersionUID = -467265517742769299L;
	
	private AETimePeriod period;
	
	private Map<String, AccAccountBalance> currentYear;

	public BilanDataSource() {

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
	 * @return the currentYear
	 */
	public Map<String, AccAccountBalance> getCurrentYear() {
		return currentYear;
	}

	/**
	 * @param currentYear the currentYear to set
	 */
	public void setCurrentYear(Map<String, AccAccountBalance> currentYear) {
		this.currentYear = currentYear;
	}
	
	public boolean hasData(String accCode) {
		return (currentYear != null && currentYear.containsKey(accCode));
	}
	
	/**
	 * 
	 * @param accCode
	 * @returns Debit balance for specified account code
	 */
	private double debitBalance(String accCode) {
		Double value = null;
		if(currentYear != null) {
			AccAccountBalance b = currentYear.get(accCode);
			if(b != null) {
				value = b.getFinalBalance();
				if(value != null && !AEMath.isZeroAmount(value)) {
					value *= 1.0;
				}
			}
		}
		return AEMath.doubleValue(value);
	}
	
	public String getCurrentValue(String accCode) {
		String ret = null;
		double value = debitBalance(accCode);
		if(!AEMath.isZeroAmount(value)) {
			ret = AEMath.toAmountFrenchString(value);
		}
		return AEStringUtil.trim(ret);
	}
	
	@Deprecated
	public String getPreviousValue(String accCode) {
		return AEStringUtil.EMPTY_STRING;
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
		
		// to list and sort
		List<String> ret = new ArrayList<String>(accCodes);
		Collections.sort(ret);
		
		return ret;
	}
	
	public String getPeriodAsStringShort() {
		StringBuilder sb = new StringBuilder();
		SimpleDateFormat dateFormat = new SimpleDateFormat("MMMMM dd", Locale.FRANCE);
		
		String fromDate = dateFormat.format(this.period.getStartDate());
		String toDate = dateFormat.format(period.getEndDate());
		sb.append(fromDate).append(" - ").append(toDate);
		
		return sb.toString();
	}
	
	public String getPeriodAsString() {
		StringBuilder sb = new StringBuilder();
		String fromDate = AEDateUtil.formatToFrench(this.period.getStartDate());
		String toDate = AEDateUtil.formatToFrench(this.period.getEndDate());
		
		sb.append(fromDate).append(" - ").append(toDate);
		
		return sb.toString();
	}
	
	public String getCurrentDateYear() {
		String ret = Integer.toString(AEDateUtil.getYear(period.getStartDate()));
		return AEStringUtil.trim(ret);
	}
	
	@Deprecated
	public String getPrevousDateYear() {
		return AEStringUtil.EMPTY_STRING;
	}

	public boolean isActif(String accCode) {
		boolean actif = false;
		if(currentYear != null) {
			AccAccountBalance b = currentYear.get(accCode);
			if(b != null) {
				actif = b.getDebitTurnover() > b.getCreditTurnover();
			}
		}
		return actif;
	}
	
	public String getDebitBalance(String accCode) {
		String ret = null;
		double value = debitBalance(accCode);
		if(!AEMath.isZeroAmount(value)) {
			ret = AEMath.toAmountFrenchString(value);
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getDebitBalance(List<String> accCodes) {
		String ret = null;
		double value = 0.0;
		for (String accCode : accCodes) {
			value += debitBalance(accCode);
		}
		if(!AEMath.isZeroAmount(value)) {
			ret = AEMath.toAmountFrenchString(value);
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getCreditBalance(String accCode) {
		String ret = null;
		double value = debitBalance(accCode);
		if(!AEMath.isZeroAmount(value)) {
			// convert to credit
			value *= -1.0;
			ret = AEMath.toAmountFrenchString(value);
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getCreditBalance(List<String> accCodes) {
		String ret = null;
		double value = 0.0;
		for (String accCode : accCodes) {
			double cb =  debitBalance(accCode) * -1.0;
			value += cb;
		}
		if(!AEMath.isZeroAmount(value)) {
			ret = AEMath.toAmountFrenchString(value);
		}
		return AEStringUtil.trim(ret);
	}
	
	public AccAccountBalancesList getBalancesList(List<String> accCodes) {
		AccAccountBalancesList ret = new AccAccountBalancesList();
		if(currentYear != null) {
			for (String accCode : accCodes) {
				AccAccountBalance b = currentYear.get(accCode);
				if(b != null) {
					ret.add(b);
				}
			}
		}
		return ret;
	}
}
