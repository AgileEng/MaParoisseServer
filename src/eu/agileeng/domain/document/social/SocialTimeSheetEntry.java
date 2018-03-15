package eu.agileeng.domain.document.social;

import java.util.Calendar;
import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.contact.Employee;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AETimePeriod;

/**
 * The relation SocialTimeSheetEntry - Date is ONE TO ONE
 * 
 * @author vvatov
 */
public class SocialTimeSheetEntry extends AEDomainObject {

	private static final long serialVersionUID = 450261408548135522L;

	static public enum JsonKey {
		ownerId,
		xType,
		periodType,
		employeeId,
		employee,
		employeeName,
		date,
		timeInMiliseconds,
		weekNumber,
		dayOfWeek,
		fromTime_1,
		toTime_1,
		type_1,
		fromTime_2,
		toTime_2,
		type_2,
		fromTimeActual_1,
		toTimeActual_1,
		actualType_1,
		fromTimeActual_2,
		toTimeActual_2,
		actualType_2,
		fromTimeActual_3,
		toTimeActual_3,
		actualType_3,
		warnings,
		workingHours,
		workingHoursActual,
		actualValidated
	}
	
	private SocialDayType dayType = SocialDayType.NA;
	
	private SocialPeriodType periodType = SocialPeriodType.NA;
	
	private AEDescriptive employee;
	
	/**
	 * Should be always a valid date!
	 */
	private Date date;
	
	private int weekNumber;
	
	private int dayOfWeek;
	
	private Date fromTime_1;
	
	private Date toTime_1;
	
	private SocialDayType type_1 = SocialDayType.NA;
	
	private Date fromTime_2;
	
	private Date toTime_2;
	
	private SocialDayType type_2 = SocialDayType.NA;
	
	private Date fromTimeActual_1;
	
	private Date toTimeActual_1;
	
	private SocialDayType actualType_1 = SocialDayType.NA;
	
	private Date fromTimeActual_2;
	
	private Date toTimeActual_2;
	
	private SocialDayType actualType_2 = SocialDayType.NA;
	
	private Date fromTimeActual_3;
	
	private Date toTimeActual_3;
	
	private SocialDayType actualType_3 = SocialDayType.NA;
	
	private boolean modifiable = true;
	
	private Double workingHours;
	
	private Double workingHoursActual;
	
	private boolean actualValidated;

	public SocialTimeSheetEntry() {
		super(DomainClass.SocialTimeSheetEntry);
		setID(AEPersistentUtil.getTmpID());
	}
	
	public SocialTimeSheetEntry(AEDescriptive employee) {
		this();
		this.employee = employee;
	}

	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.SocialTimeSheetEntry);
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();

//		xType,
		if(this.getDayType() != null) {
			json.put(SocialTimeSheetEntry.JsonKey.xType.toString(), this.getDayType().getId());
		}
//		periodType,
		if(this.getPeriodType() != null) {
			json.put(SocialTimeSheetEntry.JsonKey.periodType.toString(), this.getPeriodType().getId());
		}
//		employeeId,
		if(this.getEmployee() != null) {
			json.put(SocialTimeSheetEntry.JsonKey.employeeId.toString(), this.getEmployee().getDescriptor().getID());
		}
//		employee,
		if(this.getEmployee() instanceof Employee) {
			Employee empl = (Employee) this.getEmployee();
			json.put(SocialTimeSheetEntry.JsonKey.employee.toString(), empl.toJSONObject());
		}
//		employeeName,
		if(this.getEmployee() != null) {
			json.put(SocialTimeSheetEntry.JsonKey.employeeName.toString(), this.getEmployee().getDescriptor().getName());
		}
//		date,
		if(this.getDate() != null) {
			json.put(SocialTimeSheetEntry.JsonKey.date.toString(), AEDateUtil.formatToSystem(this.getDate()));
			json.put(SocialTimeSheetEntry.JsonKey.timeInMiliseconds.toString(), this.getDate().getTime());
		}
//		weekNumber,
		json.put(SocialTimeSheetEntry.JsonKey.weekNumber.toString(), this.getWeekNumber());
//		dayOfWeek,
		json.put(SocialTimeSheetEntry.JsonKey.dayOfWeek.toString(), this.dayOfWeek);

//		fromTime_1,
		if(this.getFromTime_1() != null) {
			json.put(SocialTimeSheetEntry.JsonKey.fromTime_1.toString(), AEDateUtil.formatTimeToSystem(this.getFromTime_1()));
		}
//		toTime_1,
		if(this.getToTime_1() != null) {
			json.put(SocialTimeSheetEntry.JsonKey.toTime_1.toString(), AEDateUtil.formatTimeToSystem(this.getToTime_1()));
		}
//      type_1
		if(this.getType_1() != null) {
			json.put(SocialTimeSheetEntry.JsonKey.type_1.toString(), this.getType_1().getId());
		}
//		fromTime_2,
		if(this.getFromTime_2() != null) {
			json.put(SocialTimeSheetEntry.JsonKey.fromTime_2.toString(), AEDateUtil.formatTimeToSystem(this.getFromTime_2()));
		}
//		toTime_2
		if(this.getToTime_2() != null) {
			json.put(SocialTimeSheetEntry.JsonKey.toTime_2.toString(), AEDateUtil.formatTimeToSystem(this.getToTime_2()));
		}
//      type_2
		if(this.getType_2() != null) {
			json.put(SocialTimeSheetEntry.JsonKey.type_2.toString(), this.getType_2().getId());
		}
//		fromTimeActual_1,
		if(this.getFromTimeActual_1() != null) {
			json.put(SocialTimeSheetEntry.JsonKey.fromTimeActual_1.toString(), AEDateUtil.formatTimeToSystem(this.getFromTimeActual_1()));
		}
//		toTimeActual_1,
		if(this.getToTimeActual_1() != null) {
			json.put(SocialTimeSheetEntry.JsonKey.toTimeActual_1.toString(), AEDateUtil.formatTimeToSystem(this.getToTimeActual_1()));
		}
//      actualType_1
		if(this.getActualType_1() != null) {
			json.put(SocialTimeSheetEntry.JsonKey.actualType_1.toString(), this.getActualType_1().getId());
		}
		
//		fromTimeActual_2,
		if(this.getFromTimeActual_2() != null) {
			json.put(SocialTimeSheetEntry.JsonKey.fromTimeActual_2.toString(), AEDateUtil.formatTimeToSystem(this.getFromTimeActual_2()));
		}
//		toTimeActual_2
		if(this.getToTimeActual_2() != null) {
			json.put(SocialTimeSheetEntry.JsonKey.toTimeActual_2.toString(), AEDateUtil.formatTimeToSystem(this.getToTimeActual_2()));
		}
//      actualType_2
		if(this.getActualType_2() != null) {
			json.put(SocialTimeSheetEntry.JsonKey.actualType_2.toString(), this.getActualType_2().getId());
		}

//		fromTimeActual_3,
		if(this.getFromTimeActual_3() != null) {
			json.put(SocialTimeSheetEntry.JsonKey.fromTimeActual_3.toString(), AEDateUtil.formatTimeToSystem(this.getFromTimeActual_3()));
		}
//		toTimeActual_3
		if(this.getToTimeActual_3() != null) {
			json.put(SocialTimeSheetEntry.JsonKey.toTimeActual_3.toString(), AEDateUtil.formatTimeToSystem(this.getToTimeActual_3()));
		}
//      actualType_3
		if(this.getActualType_3() != null) {
			json.put(SocialTimeSheetEntry.JsonKey.actualType_3.toString(), this.getActualType_3().getId());
		}

		// workingHours
		if(getWorkingHours() != null) {
			json.put(SocialTimeSheetEntry.JsonKey.workingHours.toString(), this.getWorkingHours());
		}
	
		// workingHours
		if(getWorkingHoursActual() != null) {
			json.put(SocialTimeSheetEntry.JsonKey.workingHoursActual.toString(), this.getWorkingHoursActual());
		}
		
		// actualValidated
		json.put(SocialTimeSheetEntry.JsonKey.actualValidated.toString(), this.isActualValidated());

		
		return json;
	}

	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
//		xType,
		if(jsonObject.has(SocialTimeSheetEntry.JsonKey.xType.toString())) {
			setDayType(SocialDayType.valueOf(
					jsonObject.getInt(SocialTimeSheetEntry.JsonKey.xType.toString())));
		}
//		periodType,
		if(jsonObject.has(SocialTimeSheetEntry.JsonKey.periodType.toString())) {
			setPeriodType(SocialPeriodType.valueOf(jsonObject.getInt(SocialTimeSheetEntry.JsonKey.periodType.toString())));
		}
//      employee
		if(jsonObject.has(SocialTimeSheetEntry.JsonKey.employee.toString())) {
			JSONObject jsonEmployee = jsonObject.optJSONObject(SocialTimeSheetEntry.JsonKey.employee.toString());
			if(jsonEmployee != null) {
				Employee empl = new Employee();
				empl.create(jsonEmployee);
				setEmployee(empl);
			}
		}
//		employeeId, must be after employee creation
		if(jsonObject.has(SocialTimeSheetEntry.JsonKey.employeeId.toString())) {
			long emplId = jsonObject.getLong(SocialTimeSheetEntry.JsonKey.employeeId.toString());
			if(getEmployee() instanceof Employee) {
				Employee empl = (Employee) getEmployee();
				empl.setID(emplId);
			} else {
				setEmployee(Employee.lazyDescriptor(emplId));
			}
		}
//		employeeName, must be after employee creation
		if(jsonObject.has(SocialTimeSheetEntry.JsonKey.employeeName.toString())) {
			String emplName = jsonObject.optString(SocialTimeSheetEntry.JsonKey.employeeName.toString());
			if(!AEStringUtil.isEmpty(emplName)) {
				if(getEmployee() instanceof Employee) {
					Employee empl = (Employee) getEmployee();
					empl.setName(emplName);
				} if(getEmployee() instanceof AEDescriptor) {
					AEDescriptor empl = (AEDescriptor) getEmployee();
					empl.setName(emplName);
				}
			}
		}
//		date,
		if(jsonObject.has(SocialTimeSheetEntry.JsonKey.date.toString())) {
			Date date = AEDateUtil.parseDateStrict(jsonObject.optString(SocialTimeSheetEntry.JsonKey.date.toString()));
			setDate(AEDateUtil.getClearDateTime(date));
		}
//		weekNumber
		if(jsonObject.has(SocialTimeSheetEntry.JsonKey.weekNumber.toString())) {
			setWeekNumber(jsonObject.getInt(SocialTimeSheetEntry.JsonKey.weekNumber.toString()));
		}
//		dayOfWeek,
		if(jsonObject.has(SocialTimeSheetEntry.JsonKey.dayOfWeek.toString())) {
			setDayOfWeek(jsonObject.getInt(SocialTimeSheetEntry.JsonKey.dayOfWeek.toString()));
		}
//		fromTime_1,
		if(jsonObject.has(SocialTimeSheetEntry.JsonKey.fromTime_1.toString())) {
			String hhmm = jsonObject.optString(SocialTimeSheetEntry.JsonKey.fromTime_1.toString());
			if(!AEStringUtil.isEmpty(hhmm)) {
				setFromTime_1(AEDateUtil.parseTimeOnly(hhmm));
			}
		}
//		toTime_1,
		if(jsonObject.has(SocialTimeSheetEntry.JsonKey.toTime_1.toString())) {
			String hhmm = jsonObject.optString(SocialTimeSheetEntry.JsonKey.toTime_1.toString());
			if(!AEStringUtil.isEmpty(hhmm)) {
				setToTime_1(AEDateUtil.parseTimeOnly(hhmm));
			}
		}
//      type_1
		if(jsonObject.has(SocialTimeSheetEntry.JsonKey.type_1.toString())) {
			int typeId = jsonObject.optInt(SocialTimeSheetEntry.JsonKey.type_1.toString());
			if(typeId > 0) {
				setType_1(SocialDayType.valueOf(typeId));
			}
		}
//		fromTime_2,
		if(jsonObject.has(SocialTimeSheetEntry.JsonKey.fromTime_2.toString())) {
			String hhmm = jsonObject.optString(SocialTimeSheetEntry.JsonKey.fromTime_2.toString());
			if(!AEStringUtil.isEmpty(hhmm)) {
				setFromTime_2(AEDateUtil.parseTimeOnly(hhmm));
			}
		}
//		toTime_2
		if(jsonObject.has(SocialTimeSheetEntry.JsonKey.toTime_2.toString())) {
			String hhmm = jsonObject.optString(SocialTimeSheetEntry.JsonKey.toTime_2.toString());
			if(!AEStringUtil.isEmpty(hhmm)) {
				setToTime_2(AEDateUtil.parseTimeOnly(hhmm));
			}
		}
//      type_2
		if(jsonObject.has(SocialTimeSheetEntry.JsonKey.type_2.toString())) {
			int typeId = jsonObject.optInt(SocialTimeSheetEntry.JsonKey.type_2.toString());
			if(typeId > 0) {
				setType_2(SocialDayType.valueOf(typeId));
			}
		}
//		fromTimeActual_1,
		if(jsonObject.has(SocialTimeSheetEntry.JsonKey.fromTimeActual_1.toString())) {
			String hhmm = jsonObject.optString(SocialTimeSheetEntry.JsonKey.fromTimeActual_1.toString());
			if(!AEStringUtil.isEmpty(hhmm)) {
				setFromTimeActual_1(AEDateUtil.parseTimeOnly(hhmm));
			}
		}
//		toTimeActual_1,
		if(jsonObject.has(SocialTimeSheetEntry.JsonKey.toTimeActual_1.toString())) {
			String hhmm = jsonObject.optString(SocialTimeSheetEntry.JsonKey.toTimeActual_1.toString());
			if(!AEStringUtil.isEmpty(hhmm)) {
				setToTimeActual_1(AEDateUtil.parseTimeOnly(hhmm));
			}
		}
//      actualType_1
		if(jsonObject.has(SocialTimeSheetEntry.JsonKey.actualType_1.toString())) {
			int typeId = jsonObject.optInt(SocialTimeSheetEntry.JsonKey.actualType_1.toString());
			if(typeId > 0) {
				setActualType_1(SocialDayType.valueOf(typeId));
			}
		}
		
//		fromTimeActual_2,
		if(jsonObject.has(SocialTimeSheetEntry.JsonKey.fromTimeActual_2.toString())) {
			String hhmm = jsonObject.optString(SocialTimeSheetEntry.JsonKey.fromTimeActual_2.toString());
			if(!AEStringUtil.isEmpty(hhmm)) {
				setFromTimeActual_2(AEDateUtil.parseTimeOnly(hhmm));
			}
		}
//		toTimeActual_2
		if(jsonObject.has(SocialTimeSheetEntry.JsonKey.toTimeActual_2.toString())) {
			String hhmm = jsonObject.optString(SocialTimeSheetEntry.JsonKey.toTimeActual_2.toString());
			if(!AEStringUtil.isEmpty(hhmm)) {
				setToTimeActual_2(AEDateUtil.parseTimeOnly(hhmm));
			}
		}
//      actualType_2
		if(jsonObject.has(SocialTimeSheetEntry.JsonKey.actualType_2.toString())) {
			int typeId = jsonObject.optInt(SocialTimeSheetEntry.JsonKey.actualType_2.toString());
			if(typeId > 0) {
				setActualType_2(SocialDayType.valueOf(typeId));
			}
		}
		
//		fromTimeActual_3,
		if(jsonObject.has(SocialTimeSheetEntry.JsonKey.fromTimeActual_3.toString())) {
			String hhmm = jsonObject.optString(SocialTimeSheetEntry.JsonKey.fromTimeActual_3.toString());
			if(!AEStringUtil.isEmpty(hhmm)) {
				setFromTimeActual_3(AEDateUtil.parseTimeOnly(hhmm));
			}
		}
//		toTimeActual_3
		if(jsonObject.has(SocialTimeSheetEntry.JsonKey.toTimeActual_3.toString())) {
			String hhmm = jsonObject.optString(SocialTimeSheetEntry.JsonKey.toTimeActual_3.toString());
			if(!AEStringUtil.isEmpty(hhmm)) {
				setToTimeActual_3(AEDateUtil.parseTimeOnly(hhmm));
			}
		}
//      actualType_3
		if(jsonObject.has(SocialTimeSheetEntry.JsonKey.actualType_3.toString())) {
			int typeId = jsonObject.optInt(SocialTimeSheetEntry.JsonKey.actualType_3.toString());
			if(typeId > 0) {
				setActualType_3(SocialDayType.valueOf(typeId));
			}
		}
		
		// workingHours
		if(jsonObject.has(SocialTimeSheetEntry.JsonKey.workingHours.toString())) {
			try {
				Number workingHours = AEMath.parseNumber(
						jsonObject.getString(SocialTimeSheetEntry.JsonKey.workingHours.name()), 
						true);
				if(workingHours != null) {
					setWorkingHours(workingHours.doubleValue());
				}
			} catch (AEException e) {}
		}
	
		// workingHoursActual
		if(jsonObject.has(SocialTimeSheetEntry.JsonKey.workingHoursActual.toString())) {
			try {
				Number workingHoursActual = AEMath.parseNumber(
						jsonObject.getString(SocialTimeSheetEntry.JsonKey.workingHoursActual.name()), 
						true);
				if(workingHoursActual != null) {
					setWorkingHoursActual(workingHoursActual.doubleValue());
				}
			} catch (AEException e) {}
		}
		
		// actualValidated
		if(jsonObject.has(SocialTimeSheetEntry.JsonKey.actualValidated.toString())) {
			
			this.setActualValidated(jsonObject.getBoolean(SocialTimeSheetEntry.JsonKey.actualValidated.name()));
			
		}
	}

	public SocialDayType getDayType() {
		return dayType;
	}

	public void setDayType(SocialDayType type) {
		this.dayType = type;
	}

	public AEDescriptive getEmployee() {
		return employee;
	}

	public void setEmployee(AEDescriptive employee) {
		this.employee = employee;
	}

	public boolean isModifiable() {
		return modifiable;
	}

	public void setModifiable(boolean modifiable) {
		this.modifiable = modifiable;
	}

	public SocialPeriodType getPeriodType() {
		return periodType;
	}

	public void setPeriodType(SocialPeriodType periodType) {
		this.periodType = periodType;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public int getWeekNumber() {
		return weekNumber;
	}

	public void setWeekNumber(int weekNumber) {
		this.weekNumber = weekNumber;
	}

	public int getDayOfWeek() {
		return dayOfWeek;
	}

	public void setDayOfWeek(int dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}

	public Date getFromTime_1() {
		return fromTime_1;
	}

	public void setFromTime_1(Date fromTime_1) {
		this.fromTime_1 = fromTime_1;
	}

	public Date getToTime_1() {
		return toTime_1;
	}

	public void setToTime_1(Date toTime_1) {
		this.toTime_1 = toTime_1;
	}

	public Date getFromTime_2() {
		return fromTime_2;
	}

	public void setFromTime_2(Date fromTime_2) {
		this.fromTime_2 = fromTime_2;
	}

	public Date getToTime_2() {
		return toTime_2;
	}

	public void setToTime_2(Date toTime_2) {
		this.toTime_2 = toTime_2;
	}
	
	public void setDateRelativeTo(Date startDate) {
		if(SocialPeriodType.TEMPLATE.equals(getPeriodType())) {
			setDate(AEDateUtil.addDaysToDate(startDate, (weekNumber - 1) * 7 + (dayOfWeek - 1)));
		} else {
			setWeekNumber(AEDateUtil.getWeek(getDate()));
		}
	}

	public Date getFromTimeActual_1() {
		return fromTimeActual_1;
	}

	public void setFromTimeActual_1(Date fromTimeActual_1) {
		this.fromTimeActual_1 = fromTimeActual_1;
	}

	public Date getToTimeActual_1() {
		return toTimeActual_1;
	}

	public void setToTimeActual_1(Date toTimeActual_1) {
		this.toTimeActual_1 = toTimeActual_1;
	}

	public Date getFromTimeActual_2() {
		return fromTimeActual_2;
	}

	public void setFromTimeActual_2(Date fromTimeActual_2) {
		this.fromTimeActual_2 = fromTimeActual_2;
	}

	public Date getToTimeActual_2() {
		return toTimeActual_2;
	}

	public void setToTimeActual_2(Date toTimeActual_2) {
		this.toTimeActual_2 = toTimeActual_2;
	}

	public Date getFromTimeActual_3() {
		return fromTimeActual_3;
	}

	public void setFromTimeActual_3(Date fromTimeActual_3) {
		this.fromTimeActual_3 = fromTimeActual_3;
	}

	public Date getToTimeActual_3() {
		return toTimeActual_3;
	}

	public void setToTimeActual_3(Date toTimeActual_3) {
		this.toTimeActual_3 = toTimeActual_3;
	}

	public SocialDayType getActualType_1() {
		return actualType_1;
	}

	public void setActualType_1(SocialDayType actualType_1) {
		this.actualType_1 = actualType_1;
	}

	public SocialDayType getActualType_2() {
		return actualType_2;
	}

	public void setActualType_2(SocialDayType actualType_2) {
		this.actualType_2 = actualType_2;
	}

	public SocialDayType getActualType_3() {
		return actualType_3;
	}

	public void setActualType_3(SocialDayType actualType_3) {
		this.actualType_3 = actualType_3;
	}
	
	public AETimePeriod getPlanPeriod1() {
		Calendar calDate = AEDateUtil.getEuropeanCalendar(this.date);
		
		Date startTime = this.getFromTime_1();
		if(startTime != null) {
			Calendar calStart = AEDateUtil.getEuropeanCalendar(startTime);
			calStart.set(Calendar.YEAR, calDate.get(Calendar.YEAR));
			calStart.set(Calendar.MONTH, calDate.get(Calendar.MONTH));
			calStart.set(Calendar.DAY_OF_MONTH, calDate.get(Calendar.DAY_OF_MONTH));
			
			startTime = calStart.getTime();
		}
		
		Date endTime = this.getToTime_1();
		if(endTime != null) {
			Calendar calEnd = AEDateUtil.getEuropeanCalendar(endTime);
			calEnd.set(Calendar.YEAR, calDate.get(Calendar.YEAR));
			calEnd.set(Calendar.MONTH, calDate.get(Calendar.MONTH));
			calEnd.set(Calendar.DAY_OF_MONTH, calDate.get(Calendar.DAY_OF_MONTH));
			
			endTime = calEnd.getTime();
		}
		
		return new AETimePeriod(startTime, endTime);
	}
	
	public AETimePeriod getPlanPeriod2() {
		Calendar calDate = AEDateUtil.getEuropeanCalendar(this.date);
		
		Date startTime = this.getFromTime_2();
		if(startTime != null) {
			Calendar calStart = AEDateUtil.getEuropeanCalendar(startTime);
			calStart.set(Calendar.YEAR, calDate.get(Calendar.YEAR));
			calStart.set(Calendar.MONTH, calDate.get(Calendar.MONTH));
			calStart.set(Calendar.DAY_OF_MONTH, calDate.get(Calendar.DAY_OF_MONTH));
			
			startTime = calStart.getTime();
		}
		
		Date endTime = this.getToTime_2();
		if(endTime != null) {
			Calendar calEnd = AEDateUtil.getEuropeanCalendar(endTime);
			calEnd.set(Calendar.YEAR, calDate.get(Calendar.YEAR));
			calEnd.set(Calendar.MONTH, calDate.get(Calendar.MONTH));
			calEnd.set(Calendar.DAY_OF_MONTH, calDate.get(Calendar.DAY_OF_MONTH));
			
			endTime = calEnd.getTime();
		}
		
		return new AETimePeriod(startTime, endTime);
	}
	
	public void synchHours() {
		if(!isReal()) {
			return;
		}
		
		double hours = 0;
		boolean calculated = false;
		
		AETimePeriod period1 = getPlanPeriod1();
		if(period1.isValidAndNotNull()) {
			hours += period1.calcDurationInHours();
			calculated = true;
		}
		
		AETimePeriod period2 = getPlanPeriod2();
		if(period2.isValidAndNotNull()) {
			hours += period2.calcDurationInHours();
			calculated = true;
		}
		
		if(calculated) {
			this.workingHours = hours;
			setUpdated();
		}
	}
	
	public String getStringRepr() {
		StringBuilder strBuilder = new StringBuilder("(");
		
		if(SocialPeriodType.TEMPLATE.equals(getPeriodType())) {
			strBuilder.append("S").append(getWeekNumber()).append("-").append(getDayOfWeek());
		} else if(getDate() != null){
			strBuilder.append(AEDateUtil.formatToSystem(getDate()));
		}
		
		strBuilder.append(")");
		return strBuilder.toString();
	}
	
	public AETimePeriod getMaxPlanPeriod() {
		AETimePeriod max = null;
		
		AETimePeriod period1 = getPlanPeriod1();
		AETimePeriod period2 = getPlanPeriod2();
		
		if(period1.isValidAndNotNull() && !period2.isValidAndNotNull()) {
			max = period1;
		} else if(!period1.isValidAndNotNull() && period2.isValidAndNotNull()) {
			max = period2;
		} else if(period1.isValidAndNotNull() && period2.isValidAndNotNull()) {
			if(period1.before(period2)) {
				max = period2;
			} else {
				max = period1;
			}
		}
		
		return max;
	}
	
	public AETimePeriod getMinPlanPeriod() {
		AETimePeriod min = null;
		
		AETimePeriod period1 = getPlanPeriod1();
		AETimePeriod period2 = getPlanPeriod2();
		
		if(period1.isValidAndNotNull() && !period2.isValidAndNotNull()) {
			min = period1;
		} else if(!period1.isValidAndNotNull() && period2.isValidAndNotNull()) {
			min = period2;
		} else if(period1.isValidAndNotNull() && period2.isValidAndNotNull()) {
			if(period1.before(period2)) {
				min = period1;
			} else {
				min = period2;
			}
		}
		
		return min;
	}
	
	public boolean hasPlan() {
		AETimePeriod period1 = getPlanPeriod1();
		AETimePeriod period2 = getPlanPeriod2();
		return period1.isValidAndNotNull() || period2.isValidAndNotNull();
	}

	public Double getWorkingHours() {
		return workingHours;
	}

	public void setWorkingHours(Double workingHours) {
		this.workingHours = workingHours;
	}

	public Double getWorkingHoursActual() {
		return workingHoursActual;
	}

	public void setWorkingHoursActual(Double workingHoursActual) {
		this.workingHoursActual = workingHoursActual;
	}
	
	public boolean isSum() {
		return SocialTimeSheet.Weekday.SUM.ordinal() == getDayOfWeek();
	}
	
	public boolean isReal() {
		return getDayOfWeek() >= SocialTimeSheet.Weekday.MONDAY.ordinal() 
			&& getDayOfWeek() <= SocialTimeSheet.Weekday.SUNDAY.ordinal();
	}

	/**
	 * @return the type_1
	 */
	public SocialDayType getType_1() {
		return type_1;
	}

	/**
	 * @param type_1 the type_1 to set
	 */
	public void setType_1(SocialDayType type_1) {
		this.type_1 = type_1;
	}

	/**
	 * @return the type_2
	 */
	public SocialDayType getType_2() {
		return type_2;
	}

	/**
	 * @param type_2 the type_2 to set
	 */
	public void setType_2(SocialDayType type_2) {
		this.type_2 = type_2;
	}
	
	
	public boolean isActualValidated() {
		return actualValidated;
	}

	public void setActualValidated(boolean actualValidated) {
		this.actualValidated = actualValidated;
	}
}
