package eu.agileeng.domain.cefra.n11580_03;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.joda.time.DateMidnight;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.acc.AccJournalResult;
import eu.agileeng.domain.acc.AccJournalResultsList;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AETimePeriod;

public class JournauxDataSource extends ReportDataSource {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2155865591604283161L;

	/**
	 * 
	 */
	
	private AETimePeriod period;
	
	private AEDescriptor accJournal;
	
	private AccJournalResultsList accJournalResultsList;
	
	private Map<String, Map<DateMidnight, AccJournalResultsList>> accJournalResultsMap;

	public JournauxDataSource() {

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
	
	public String getPeriodAsStringShort() {
		StringBuilder sb = new StringBuilder();
		SimpleDateFormat dateFormat = new SimpleDateFormat("MMMMM dd", Locale.FRANCE);
		
		String fromDate = dateFormat.format(this.period.getStartDate());
		String toDate = dateFormat.format(period.getEndDate());
		sb.append(fromDate).append(" - ").append(toDate).append(", ").append(AEDateUtil.getYear(this.period.getStartDate()));
		
		return sb.toString();
	}
	
	public String getPeriodAsString() {
		StringBuilder sb = new StringBuilder();
		String fromDate = AEDateUtil.formatToFrench(this.period.getStartDate());
		String toDate = AEDateUtil.formatToFrench(period.getEndDate());		
		sb.append(fromDate).append(" - ").append(toDate);
		return sb.toString();
	}
	
	public String getAccJournalCode() {
		String ret = null;
		if(this.accJournal != null) {
			ret = this.accJournal.getCode();
		}
		return AEStringUtil.trim(ret);
	}

	/**
	 * @return the accJournalResultsList
	 */
	public AccJournalResultsList getAccJournalResultsList() {
		return accJournalResultsList;
	}

	/**
	 * @param accJournalResultsList the accJournalResultsList to set
	 */
	public void setAccJournalResultsList(AccJournalResultsList accJournalResultsList) {
		this.accJournalResultsList = accJournalResultsList;
	}
	
	public void groupAccJournalResultsInMap(){
		if(!this.accJournalResultsList.isEmpty()){
			Map<String, Map<DateMidnight, AccJournalResultsList>> accJournalResultsMap = new HashMap<String, Map<DateMidnight, AccJournalResultsList>>();

			Iterator<AccJournalResult> i = this.accJournalResultsList.iterator();
			while(i.hasNext()){
				AccJournalResult ajr = i.next();
				String journalKey = ajr.getAccJournalItem().getJournal().getCode();
				
				if(accJournalResultsMap.get(journalKey) == null){

					Map<DateMidnight, AccJournalResultsList> monthMap = new TreeMap<DateMidnight, AccJournalResultsList>(AEDateUtil.getDateMidnightComparator());
					
					AccJournalResultsList ajrl = new AccJournalResultsList();
					
					DateMidnight date = new DateMidnight(ajr.getAccJournalItem().getDate());
					date = date.withDayOfMonth(1);
					
					ajrl.add(ajr);
					
					monthMap.put(date, ajrl);
					accJournalResultsMap.put(journalKey, monthMap);
				} else {
					DateMidnight monthKey = new DateMidnight(ajr.getAccJournalItem().getDate());
					monthKey = monthKey.withDayOfMonth(1);
					
					if(accJournalResultsMap.get(journalKey).get(monthKey) == null){
						AccJournalResultsList ajrl = new AccJournalResultsList();
						ajrl.add(ajr);
						accJournalResultsMap.get(journalKey).put(monthKey, ajrl);
					} else {
						accJournalResultsMap.get(journalKey).get(monthKey).add(ajr);
					}
				}
			}
			
			this.accJournalResultsMap = accJournalResultsMap;
		}
	}
	
	private static double getTotalDebitAmountForList(AccJournalResultsList ajrl){
		Iterator<AccJournalResult> i = ajrl.iterator();
		double totalAmount = 0.0;
		while(i.hasNext()){
			AccJournalResult ajResult = i.next();
			if(ajResult != null && ajResult.getAccJournalItem() != null && ajResult.getAccJournalItem().getDtAmount() != null) {
				double amount = ajResult.getAccJournalItem().getDtAmount();
				totalAmount+=amount;
			}
		}
		return totalAmount;
	}
	
	private static double getTotalCreditAmountForList(AccJournalResultsList ajrl){
		Iterator<AccJournalResult> i = ajrl.iterator();
		double totalAmount = 0.0;
		while(i.hasNext()){
			AccJournalResult ajResult = i.next();
			if(ajResult != null && ajResult.getAccJournalItem() != null && ajResult.getAccJournalItem().getCtAmount() != null) {
				double amount = ajResult.getAccJournalItem().getCtAmount();
				totalAmount+=amount;
			}
		}
		return totalAmount;
	}
	
	public static String getTotalDebitAmountForMonth(AccJournalResultsList ajrl){
		String ret = null;
		double amount = getTotalDebitAmountForList(ajrl);
		if(!AEMath.isZeroAmount(amount)){
			ret = AEMath.toAmountFrenchString(amount);
		}
		return AEStringUtil.trim(ret);
	}
	
	public static String getTotalCreditAmountForMonth(AccJournalResultsList ajrl){
		String ret = null;
		double amount = getTotalCreditAmountForList(ajrl);
		if(!AEMath.isZeroAmount(amount)){
			ret = AEMath.toAmountFrenchString(amount);
		}
		return AEStringUtil.trim(ret);
	}
	
	public static String getTotalDebitAmountForJournal(Map<DateMidnight, AccJournalResultsList> journal){
		String ret = null;
		double totalAmount = 0.0;
		
		Iterator<Entry<DateMidnight, AccJournalResultsList>> i = journal.entrySet().iterator();
		while(i.hasNext()){
			Entry<DateMidnight, AccJournalResultsList> month = i.next();
			totalAmount+=getTotalDebitAmountForList(month.getValue());
		}
		if(!AEMath.isZeroAmount(totalAmount)){
			ret = AEMath.toAmountFrenchString(totalAmount);
		}
		
		return AEStringUtil.trim(ret);
	}
	
	public static String getTotalCreditAmountForJournal(Map<DateMidnight, AccJournalResultsList> journal){
		String ret = null;
		double totalAmount = 0.0;
		
		Iterator<Entry<DateMidnight, AccJournalResultsList>> i = journal.entrySet().iterator();
		while(i.hasNext()){
			Entry<DateMidnight, AccJournalResultsList> month = i.next();
			totalAmount+=getTotalCreditAmountForList(month.getValue());
		}
		if(!AEMath.isZeroAmount(totalAmount)){
			ret = AEMath.toAmountFrenchString(totalAmount);
		}
		
		return AEStringUtil.trim(ret);
	}
	
	public static String getTotalCreditAmountForReport(AccJournalResultsList ajrl){
		String ret = null;
		double sum = 0.0;
		Iterator<AccJournalResult> i = ajrl.iterator();
		while(i.hasNext()){
			AccJournalResult ajr = i.next();
			sum+=ajr.getCtAmount();
		}

		if(!AEMath.isZeroAmount(sum)){
			ret = AEMath.toAmountFrenchString(sum);
		}
		return AEStringUtil.trim(ret);
	}
	
	public static String getTotalDebitAmountForReport(AccJournalResultsList ajrl){
		String ret = null;
		double sum = 0.0;
		Iterator<AccJournalResult> i = ajrl.iterator();
		while(i.hasNext()){
			AccJournalResult ajr = i.next();
			sum+=ajr.getDtAmount();
		}

		if(!AEMath.isZeroAmount(sum)){
			ret = AEMath.toAmountFrenchString(sum);
		}
		return AEStringUtil.trim(ret);
	}
	
	public Map<String, Map<DateMidnight, AccJournalResultsList>> getAccJournalResultsMap(){
		return this.accJournalResultsMap;
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
}
