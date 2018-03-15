package eu.agileeng.domain.document.social;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.AEList;
import eu.agileeng.domain.contact.Employee;
import eu.agileeng.persistent.AEPersistent;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEPredicate;
import eu.agileeng.util.AEValidatable;
import eu.agileeng.util.AEValidator;

public class SocialTimeSheet extends ArrayList<SocialTimeSheetEntry> implements AEList, AEValidatable {

	private static final long serialVersionUID = -7502039088319292863L;

	static public enum JsonKey {
		period,
		xType,
		periodType,
		startDate,
		ownerId,
		numberOfWeeks,
		employeeId,
		employee,
		days,
		timeSheet,
		day,
		fromDate,
		toDate,
		month,
		year,
		roll,
		startWeekIndex
	}
	
	public static enum Weekday {
		NULL,
 		MONDAY, 
 		TUESDAY, 
 		WEDNESDAY, 
 		THURSDAY, 
 		FRIDAY, 
 		SATURDAY, 
 		SUNDAY,
 		SUM;
 
 		public static final EnumSet<Weekday> REALDAYS = EnumSet.range(MONDAY, SUNDAY);
 		
 		public static final EnumSet<Weekday> SUMDAYS = EnumSet.of(SUM);
 	}
	
	public static enum DetailsLevel {
		NULL, // 0: not defined
 		HOUR, // 1
 		DAY,  // 2
 		WEEK; // 3
 	}
	
	private SocialPeriodType periodType;
	
	private AEDescriptive employee;
	
	private AEDescriptive owner;
	
	private Date startDate;
	
	private Date endDate;

	private int numberOfWeeks;
	
	public SocialTimeSheet(AEDescriptive employee, SocialPeriodType periodType, Date startDate, int numberOfWeeks) {
		this(employee, periodType, startDate);
		this.numberOfWeeks = numberOfWeeks;
	}
	
	public SocialTimeSheet(AEDescriptive employee, SocialPeriodType periodType, Date startDate) {
		this(employee, periodType);
		this.startDate = startDate;
	}
	
	public SocialTimeSheet(AEDescriptive employee, SocialPeriodType periodType) {
		this(periodType);
		this.employee = employee;
	}
	
	public SocialTimeSheet(SocialPeriodType periodType) {
		this.periodType = periodType;
	}
	
	public SocialTimeSheet() {
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEList#toJSONArray()
	 */
	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (SocialTimeSheetEntry tsEntry : this) {
				jsonArray.put(tsEntry.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEList#create(org.apache.tomcat.util.json.JSONArray)
	 */
	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);
		
			SocialTimeSheetEntry tsEntry = new SocialTimeSheetEntry();
			tsEntry.create(jsonItem);
			if(!SocialPeriodType.ACTUAL.equals(getPeriodType())) {
				create(tsEntry);
			}
			
			add(tsEntry);
		}
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEList#create(org.apache.tomcat.util.json.JSONArray)
	 */
	public void create(JSONObject period) throws JSONException {
		
		// xType
		setType(SocialPeriodType.valueOf(period.getInt(SocialTimeSheet.JsonKey.xType.toString())));
		
		// employee
		long employeeId = period.optLong(SocialTimeSheet.JsonKey.employeeId.toString());
		if(employeeId > 0) {
			setEmployee(Employee.lazyDescriptor(employeeId));
		}
		
		// startDate
		if(SocialPeriodType.TEMPLATE.equals(getPeriodType())) {
			setStartDate(AEDateUtil.parseDateStrict(period.getString(SocialTimeSheet.JsonKey.startDate.toString())));
		} else {
			setStartDate(AEDateUtil.parseDateStrict(period.optString(SocialTimeSheet.JsonKey.startDate.toString())));
		}
		
		// numberOfWeeks
		if(SocialPeriodType.TEMPLATE.equals(getPeriodType())) {
			setNumberOfWeeks(period.getInt(SocialTimeSheet.JsonKey.numberOfWeeks.toString()));
		} else {
			setNumberOfWeeks(period.optInt(SocialTimeSheet.JsonKey.numberOfWeeks.toString()));
		}
		
		// days
		if(period.has(SocialTimeSheet.JsonKey.days.toString())) {
			create(period.getJSONArray(SocialTimeSheet.JsonKey.days.toString()));
		}
	}

	public JSONObject toJSONObject() throws JSONException {
		JSONObject jsonPeriod = new JSONObject();

		// xType
		if(getPeriodType() != null) {
			jsonPeriod.put(SocialTimeSheet.JsonKey.xType.toString(), getPeriodType().getId());
		}
		
		// employee
		if(getEmployee() != null) {
			jsonPeriod.put(SocialTimeSheet.JsonKey.employeeId.toString(), getEmployee().getDescriptor().getID());
			if(getEmployee() instanceof Employee) {
				Employee empl = (Employee) getEmployee();
				jsonPeriod.put(SocialTimeSheet.JsonKey.employee.toString(), empl.toJSONObject());
			}
		}
		
		// startDate
		if(getStartDate() != null) {
			jsonPeriod.put(SocialTimeSheet.JsonKey.startDate.toString(), AEDateUtil.formatToSystem(getStartDate()));
		}
		
		// numberOfWeeks
		jsonPeriod.put(SocialTimeSheet.JsonKey.numberOfWeeks.toString(), getNumberOfWeeks());
		
		// days
		JSONArray jsonDays = toJSONArray();
		jsonPeriod.put(SocialTimeSheet.JsonKey.days.toString(), jsonDays);
		
		return jsonPeriod;
	}
	
	@Override
	public void validateWith(AEValidator validator) throws AEException {
		validator.validate(this);
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public int getNumberOfWeeks() {
		return numberOfWeeks;
	}

	public void setNumberOfWeeks(int numberOfWeeks) {
		this.numberOfWeeks = numberOfWeeks;
	}

	public SocialPeriodType getPeriodType() {
		return periodType;
	}

	public void setType(SocialPeriodType periodType) {
		this.periodType = periodType;
	}

	public AEDescriptive getEmployee() {
		return employee;
	}

	public void setEmployee(AEDescriptive employee) {
		this.employee = employee;
	}
	
	public void create(SocialTimeSheetEntry tsEntry) {
		tsEntry.setPeriodType(periodType);
		tsEntry.setEmployee(employee);
		tsEntry.setDateRelativeTo(startDate);
	}
	
	public SocialTimeSheetEntry create(int weekNumber, int dayOfWeek) {
		SocialTimeSheetEntry tsEntry = new SocialTimeSheetEntry();
		
		tsEntry.setDayType(SocialDayType.WORK);
		tsEntry.setWeekNumber(weekNumber);
		tsEntry.setDayOfWeek(dayOfWeek);
		
		create(tsEntry);
		
		return tsEntry;
	}
	
	public void createTemplate() {
		for(int week = 1; week <= numberOfWeeks; week++) {
			for(int day = Weekday.MONDAY.ordinal(); day <= Weekday.SUM.ordinal(); day++) {
				SocialTimeSheetEntry tsEntry = create(week, day);				
				add(tsEntry);
			}
		}
	}
	
	public void pushWeek() {
		int _weekNumber = 0;
		
		// detect new weekNumber
		if(SocialPeriodType.TEMPLATE.equals(getPeriodType())) {
			if(this.numberOfWeeks >= 4) {
				return;
			}
			_weekNumber = ++this.numberOfWeeks;
		} else {
			// real week
			
		}
		
		for(int day = Weekday.MONDAY.ordinal(); day <= Weekday.SUM.ordinal(); day++) {
			SocialTimeSheetEntry tsEntry = create(_weekNumber, day);
			add(tsEntry);
		}
		
		Collections.sort(this, SocialTimeSheetEntryComparator.getInst());
	}
	
	public void popWeek() {
		int _weekNumber = 0;
		
		// detect weekNumber to pop
		if(SocialPeriodType.TEMPLATE.equals(getPeriodType())) {
			if(this.numberOfWeeks <= 0) {
				return;
			}
			_weekNumber = this.numberOfWeeks--;
		} else {
			// real week
			
		}

		for (Iterator<SocialTimeSheetEntry> iterator = this.iterator(); iterator.hasNext();) {
			SocialTimeSheetEntry entry = iterator.next();
			if(entry.getWeekNumber() == _weekNumber){
				entry.setPersistentState(AEPersistent.State.DELETED);
			}
 		}
		
		Collections.sort(this, SocialTimeSheetEntryComparator.getInst());
	}
	
	public void resetAsPlan() {
		this.periodType = SocialPeriodType.PLAN;
		for (Iterator<SocialTimeSheetEntry> iterator = this.iterator(); iterator.hasNext();) {
			SocialTimeSheetEntry entry = iterator.next();
			entry.resetAsNew();
			create(entry);
 		}
	}
	
	public Map<Integer, SocialTimeSheetEntry> getWeek(long weekNumber) {
		Map<Integer, SocialTimeSheetEntry> week = new HashMap<Integer, SocialTimeSheetEntry>();
		for (Iterator<SocialTimeSheetEntry> iterator = this.iterator(); iterator.hasNext();) {
			SocialTimeSheetEntry entry = iterator.next();
			if(entry.getWeekNumber() == weekNumber){
				week.put(entry.getDayOfWeek(), entry);
			}
 		}
		return week;
	}
	
	public Map<Integer, List<SocialTimeSheetEntry>> toWeeks() {
		Map<Integer, List<SocialTimeSheetEntry>> weeks = new HashMap<Integer, List<SocialTimeSheetEntry>>();
		for (Iterator<SocialTimeSheetEntry> iterator = this.iterator(); iterator.hasNext();) {
			SocialTimeSheetEntry entry = iterator.next();
			int weekNumber = entry.getWeekNumber();
			if(weeks.containsKey(weekNumber)) {
				List<SocialTimeSheetEntry> entriesList = weeks.get(weekNumber);
				entriesList.add(entry);
			} else {
				List<SocialTimeSheetEntry> entriesList = new ArrayList<SocialTimeSheetEntry>();
				entriesList.add(entry);
				weeks.put(weekNumber, entriesList);
			}
 		}
		return weeks;
	}
	
	public Map<SocialTimeSheetTemplateKey, SocialTimeSheetEntry> toDays() {
		Map<SocialTimeSheetTemplateKey, SocialTimeSheetEntry> days = new HashMap<SocialTimeSheetTemplateKey, SocialTimeSheetEntry>();
		for (Iterator<SocialTimeSheetEntry> iterator = this.iterator(); iterator.hasNext();) {
			SocialTimeSheetEntry entry = iterator.next();
			int weekNumber = entry.getWeekNumber();
			int dayOfWeek = entry.getDayOfWeek();
			days.put(new SocialTimeSheetTemplateKey(weekNumber, dayOfWeek), entry);
 		}
		return days;
	}
	
	public static Map<AEDescriptor, List<SocialTimeSheetEntry>> toEmployees(List<SocialTimeSheetEntry> entries) {
		Map<AEDescriptor, List<SocialTimeSheetEntry>> employees = new HashMap<AEDescriptor, List<SocialTimeSheetEntry>>();
		if(entries != null && !entries.isEmpty()) {
			for (Iterator<SocialTimeSheetEntry> iterator = entries.iterator(); iterator.hasNext();) {
				SocialTimeSheetEntry entry = iterator.next();
				if(entry.getEmployee() != null) {
					AEDescriptor emplDescr = entry.getEmployee().getDescriptor();
					if(employees.containsKey(emplDescr)) {
						List<SocialTimeSheetEntry> entriesList = employees.get(emplDescr);
						entriesList.add(entry);
					} else {
						List<SocialTimeSheetEntry> entriesList = new ArrayList<SocialTimeSheetEntry>();
						entriesList.add(entry);
						employees.put(emplDescr, entriesList);
					}
				}
			}
		}
		return employees;
	}
	
	public static List<SocialTimeSheetEntry> filterByDay(List<SocialTimeSheetEntry> entries, int dayOfWeek) {
		List<SocialTimeSheetEntry> ret = new ArrayList<SocialTimeSheetEntry>();
		for (SocialTimeSheetEntry entry : entries) {
			if(entry.getDayOfWeek() == dayOfWeek) {
				ret.add(entry);
			}
		}
		return ret;
	}
	
	/**
	 * Use this method only for whole weeks
	 * 
	 * @param week
	 * @return
	 */
	public static double getPlanHours(List<SocialTimeSheetEntry> wholeWeeks) {
		double hours = 0.0;
		for (SocialTimeSheetEntry dayEntry : wholeWeeks) {
			if(dayEntry.getDayOfWeek() == Weekday.SUM.ordinal()) {
				hours += AEMath.doubleValue(dayEntry.getWorkingHours());
			}
		}
		return hours;
	}
	
	public static double getRestPeriod(SocialTimeSheetEntry entry1, SocialTimeSheetEntry entry2) {
		double rest = 0.0;

		return rest;
	}
	
	public List<SocialTimeSheetEntry> filter(AEPredicate predicate) {
		List<SocialTimeSheetEntry> res = new ArrayList<SocialTimeSheetEntry>();
		
		// check predicate
		if(predicate == null) {
			return new ArrayList<SocialTimeSheetEntry>(this);
		}
		
		// apply predicate
		for (SocialTimeSheetEntry entry : this) {
			if(predicate.evaluate(entry)) {
				res.add(entry);
			}
		}
		
		return res;
	}
	
	/**
	 * Syncronises the summary week's hours  
	 */
	public void synchHours() {
		// group by week
		Map<Integer, List<SocialTimeSheetEntry>> weeks = toWeeks();
		Set<Integer> weeksSet = weeks.keySet();
		
		// synchHours
		for (Integer weekNumber : weeksSet) {
			List<SocialTimeSheetEntry> week = weeks.get(weekNumber);
			
			double weekWorkingHours = 0.0;
			boolean calculate = false;
			SocialTimeSheetEntry weekSumEntry = null;
			for (SocialTimeSheetEntry dayEntry : week) {
				// synch day hours
				dayEntry.synchHours();
				
				// process the day in the week
				if(dayEntry.isSum()) {
					// week sum entry
					weekSumEntry = dayEntry;
				} else if(dayEntry.isReal()) {
					// real day entry
					if(dayEntry.getWorkingHours() != null) {
						calculate = true;
						weekWorkingHours += AEMath.doubleValue(dayEntry.getWorkingHours());
					}
				}
			}
			
			if(calculate && weekSumEntry != null) {
				weekSumEntry.setWorkingHours(weekWorkingHours);
				weekSumEntry.setUpdated();
			}
		}
	}
	
	public SocialTimeSheet.DetailsLevel getDetailsLevel() {
		SocialTimeSheet.DetailsLevel level = DetailsLevel.NULL;

		// check for entries
		if(!isEmpty()) {
			// check for HOUR level
			for (SocialTimeSheetEntry dayEntry : this) {
				if(dayEntry.isReal() && dayEntry.hasPlan()) {
					level = DetailsLevel.HOUR; 
				}
			}
			
			if(DetailsLevel.NULL.equals(level)) {
				// check for DAY level
				for (SocialTimeSheetEntry dayEntry : this) {
					if(dayEntry.isSum() && dayEntry.getWorkingHours() != null) {
						level = DetailsLevel.DAY; 
					}
				}
			}
			
			if(DetailsLevel.NULL.equals(level)) {
				// WEEK level
				level = DetailsLevel.WEEK; 
			}
		}
		
		return level;
	}
	
	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	/**
	 * @return the owner
	 */
	public AEDescriptive getOwner() {
		return owner;
	}

	/**
	 * @param owner the owner to set
	 */
	public void setOwner(AEDescriptive owner) {
		this.owner = owner;
	}
}
