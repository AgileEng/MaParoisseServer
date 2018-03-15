/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 18.06.2010 10:34:54
 */
package eu.agileeng.domain.document.social.contractdetravail;

import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.business.ContractDeTravailReason;
import eu.agileeng.domain.business.EmploymentClassification;
import eu.agileeng.domain.business.bank.BankAccount;
import eu.agileeng.domain.contact.Employee;
import eu.agileeng.domain.contact.Person;
import eu.agileeng.domain.document.AEDocumentDescriptor;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.imp.AEDocumentDescriptorImp;
import eu.agileeng.domain.document.social.AESocialDocument;
import eu.agileeng.domain.measurement.UnitOfMeasurement;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.json.JSONUtil;

/**
 * Contract Work.
 * The company owner or this contract should be inserted into this company field.
 */
@SuppressWarnings("serial")
public class ContractDeTravail extends AESocialDocument {

	static public class JSONKey {
		 public static final String student = "student";
		 public static final String workingDuringVacation = "workingDuringVacation";
		 public static final String proffesionDuration = "proffesionDuration";
		 public static final String formerEmployee = "formerEmployee";
		 public static final String dateOfEntry = "dateOfEntry";
		 public static final String timeOfEntry = "timeOfEntry";
		 public static final String declarationDUE = "declarationDUE";
		 public static final String declarationDUEDate = "declarationDUEDate";
		 public static final String declarationDUENumber = "declarationDUENumber";
		 public static final String employment = "employment";
		 public static final String qualification = "qualification";
		 public static final String employmentClassification_ID = "employmentClassification_ID";
		 public static final String coefficient = "coefficient";
		 public static final String ae_level = "ae_level";
		 public static final String echelon = "echelon";
		 public static final String position = "position";
		 public static final String ae_index = "ae_index";
		 public static final String addSalaryPerc = "addSalaryPerc";
		 public static final String nightWork = "nightWork";
		 public static final String subTypeId = "subTypeId";
		 public static final String appointmentFixed = "appointmentFixed";
		 public static final String appointmentFrom = "appointmentFrom";
		 public static final String appointmentTo = "appointmentTo";
		 public static final String reasonId = "reasonId";
		 public static final String absentEmployeeId = "absentEmployeeId";
		 public static final String absentEmployeeTitle = "absentEmployeeTitle";
		 public static final String absentEmployeeName = "absentEmployeeName";
		 public static final String absentEmployeeFirstName = "absentEmployeeFirstName";
		 public static final String absentQualification = "absentQualification";
		 public static final String absentEchelon = "absentEchelon";
		 
		 public static final String absentEmployment = "absentEmployment";
		 public static final String absentEmploymentClassification_ID = "absentEmploymentClassification_ID";
		 public static final String absentCoefficient = "absentCoefficient";
		 public static final String absentLevel = "absentLevel";
		 public static final String absentPosition = "absentPosition";
		 public static final String absentIndex = "absentIndex";
		 
		 public static final String appointmentPeriod = "appointmentPeriod";
		 public static final String appointmentStartDate = "appointmentStartDate";
		 public static final String appointmentStartTime = "appointmentStartTime";
		 public static final String fullTime = "fullTime";
		 public static final String hoursPerWeek = "hoursPerWeek";
		 public static final String trialPeriodQty = "trialPeriodQty";
		 public static final String trialPeriodUOMId = "trialPeriodUOMId";
		 public static final String renewable = "renewable";
		 public static final String contractAssistenceId = "contractAssistenceId";
		 public static final String grossAmount = "grossAmount";
//		 public static final String currencyId = "currencyId";
		 public static final String perQty = "perQty";
		 public static final String perUOMId = "perUOMId";
		 public static final String verifiedById = "verifiedById";
		 public static final String verifiedByName = "verifiedByName";
		 public static final String verifiedBySalutation = "salutationBossId";
		 public static final String modificationId = "modificationId";
		 public static final String modificationDate = "modificationDate";
		 public static final String effectiveDate = "effectiveDate";
		 public static final String replacementPartiel = "remplacementPartiel";
		 public static final String cdiNonAnnualise = "cdiNonAnnualise";
		 public static final String daysPerWeek = "daysPerWeek";
		 public static final String mutuelle = "mutuelle";
		 public static final String cddReplacementNpDurationUoM = "cddReplacementNpDurationUoM";
	}
	
	private transient boolean buildLazzy;
	
	private boolean student;
	
	private boolean workDuringVacation;
	
	private Double professionDuration; // anciennete dans la profession
	
//	private int specialityDuration; // 
	
	private boolean formerEmployee; // Ce salarie a-t-il deja travaille ...
	
	private Date dateOfEntry; // Date and Heure d'Embauche
	
	private boolean declarationDUE;
	
	private Date declarationDUEDate;
	
	private String declarationDUENumber;
	
	private String employment; // emploi
	
	private String qualification; 
	
	private EmploymentClassification employmentClassification = 
		new EmploymentClassification(EmploymentClassification.System.EMPLOYEE);
	
	private Double coefficient;
	
	private String level; // niveau
	
	private String echelon;
	
	private String position;
	
	private String index; // indice
	
	private String absentEmployment; // absent emploi
	private EmploymentClassification absentEmploymentClassification =
		new EmploymentClassification(EmploymentClassification.System.EMPLOYEE);
	private Double absentCoefficient;
	private String absentLevel; // absent niveau
	private String absentPosition;
	private String absentIndex; // absent indice
	
	private Double addSalaryPerc; //additional salary
	
	private boolean nightWork; // travail de nuit
	
	// Type de contract de travail
	private ContractDeTravailType subType = 
		new ContractDeTravailType(ContractDeTravailType.System.PERMANENT); 
	
	private Date appointmentFrom; //period from for this contract
	
	private Date appointmentTo;   //period to for this contract. null in case of permanent contract
	
	/**
	 * Use the instance COMMON when no need for choice and type the text into Note field.
	 * Use the field Note of this class as motif.
	 */
	private ContractDeTravailReason reason; 
	
	/**
	 * In case of TEMPORARY_REPLACEMENT contract type.
	 * The intention is to be an AEDescriptor instance ... 
	 */
	private AEDescriptive absentEmployee; 
	
	private String absentQualification;
	
	private String absentEchelon;
	
	/**
	 * The <code>true</code> value of the <code>appointmentPeriod</code> 
	 * means that this appointment is from <code>appointmentFrom</code> to <code>appointmentTo</code>. 
	 * The <code>false</code> value of <code>appointmentPeriod</code> 
	 * means that this appointment is minimum to <code>appointmentTo</code>. 
	 */
	private long appointmentPeriod;
	
	private UnitOfMeasurement cddReplacementNpDurationUoM;
	
	private Date appointmentStartDateTime;
	
	private boolean fullTime = true;
	
	private Double hoursPerWeek;
	
	private Double trialPeriodQty;
	
	private UnitOfMeasurement trialPeriodUOM = UnitOfMeasurement.day;
	
	private boolean renewable; // in cacse of CDD
	
	// contract aide
	private ContractDeTravailAssistence contractAssistence = 
		new ContractDeTravailAssistence(ContractDeTravailAssistence.System.AUCUN); 

	private Double grossAmount; // brute
	
	private UnitOfMeasurement currency;
	
	private Double perQty = new Double(1.0); 
	
	private UnitOfMeasurement perUOF = UnitOfMeasurement.month;
	
	private AEDescriptive verifiedBy;
	
	private Person.SalutationType verifiedBySalutation = Person.SalutationType.NA;
	
	private BankAccount bankAccount = new BankAccount();
	
	/**
	 * Reference to the modification/anex if exist.
	 * Only the actual contract should not have modification.  
	 */
	private AEDocumentDescriptor modificationDescr;
	
	private Date effectiveDate;
	
	private boolean replacementPartiel;
	
	private boolean cdiNonAnnualise;
	
	private Double daysPerWeek;
	
	private boolean appointmentFixed;
	
	private boolean mutuelle;
	
	/**
	 * no arg constructor.
	 * Try to use it only when will be additional doc type initialization
	 */
	public ContractDeTravail() {
		super(AEDocumentType.valueOf(AEDocumentType.System.ContractDeTravail));
	}
	
	public ContractDeTravail(AEDocumentType docType) {
		super(docType);
	}

	/**
	 * @return the student
	 */
	public boolean isStudent() {
		return student;
	}

	/**
	 * @param student the student to set
	 */
	public void setStudent(boolean student) {
		this.student = student;
	}

	/**
	 * @return the workDuringVacation
	 */
	public boolean isWorkDuringVacation() {
		return workDuringVacation;
	}

	/**
	 * @param workDuringVacation the workDuringVacation to set
	 */
	public void setWorkDuringVacation(boolean workDuringVacation) {
		this.workDuringVacation = workDuringVacation;
	}

	/**
	 * @return the professionDuration
	 */
	public Double getProfessionDuration() {
		return professionDuration;
	}

	/**
	 * @param professionDuration the professionDuration to set
	 */
	public void setProfessionDuration(Double professionDuration) {
		this.professionDuration = professionDuration;
	}

	/**
	 * @return the formerEmployee
	 */
	public boolean isFormerEmployee() {
		return formerEmployee;
	}

	/**
	 * @param formerEmployee the formerEmployee to set
	 */
	public void setFormerEmployee(boolean formerEmployee) {
		this.formerEmployee = formerEmployee;
	}

	/**
	 * @return the dateOfEntry
	 */
	public Date getDateOfEntry() {
		return dateOfEntry;
	}

	/**
	 * @param dateOfEntry the dateOfEntry to set
	 */
	public void setDateOfEntry(Date dateOfEntry) {
		this.dateOfEntry = dateOfEntry;
	}

	/**
	 * @return the declarationDUE
	 */
	public boolean isDeclarationDUE() {
		return declarationDUE;
	}

	/**
	 * @param declarationDUE the declarationDUE to set
	 */
	public void setDeclarationDUE(boolean declarationDUE) {
		this.declarationDUE = declarationDUE;
	}

	/**
	 * @return the declarationDUEDate
	 */
	public Date getDeclarationDUEDate() {
		return declarationDUEDate;
	}

	/**
	 * @param declarationDUEDate the declarationDUEDate to set
	 */
	public void setDeclarationDUEDate(Date declarationDUEDate) {
		this.declarationDUEDate = declarationDUEDate;
	}

	/**
	 * @return the declarationDUENumber
	 */
	public String getDeclarationDUENumber() {
		return declarationDUENumber;
	}

	/**
	 * @param declarationDUENumber the declarationDUENumber to set
	 */
	public void setDeclarationDUENumber(String declarationDUENumber) {
		this.declarationDUENumber = declarationDUENumber;
	}

	/**
	 * @return the employment
	 */
	public String getEmployment() {
		return employment;
	}

	/**
	 * @param employment the employment to set
	 */
	public void setEmployment(String employment) {
		this.employment = employment;
	}

	/**
	 * @return the qualification
	 */
	public String getQualification() {
		return qualification;
	}

	/**
	 * @param qualification the qualification to set
	 */
	public void setQualification(String qualification) {
		this.qualification = qualification;
	}

	/**
	 * @return the employmentClassification
	 */
	public EmploymentClassification getEmploymentClassification() {
		return employmentClassification;
	}

	/**
	 * @param employmentClassification the employmentClassification to set
	 */
	public void setEmploymentClassification(
			EmploymentClassification employmentClassification) {
		this.employmentClassification = employmentClassification;
	}

	/**
	 * @return the coefficient
	 */
	public Double getCoefficient() {
		return coefficient;
	}

	/**
	 * @param coefficient the coefficient to set
	 */
	public void setCoefficient(Double coefficient) {
		this.coefficient = coefficient;
	}

	/**
	 * @return the level
	 */
	public String getLevel() {
		return level;
	}

	/**
	 * @param level the level to set
	 */
	public void setLevel(String level) {
		this.level = level;
	}

	/**
	 * @return the echelon
	 */
	public String getEchelon() {
		return echelon;
	}

	/**
	 * @param echelon the echelon to set
	 */
	public void setEchelon(String echelon) {
		this.echelon = echelon;
	}

	/**
	 * @return the position
	 */
	public String getPosition() {
		return position;
	}

	/**
	 * @param position the position to set
	 */
	public void setPosition(String position) {
		this.position = position;
	}

	/**
	 * @return the index
	 */
	public String getIndex() {
		return index;
	}

	/**
	 * @param index the index to set
	 */
	public void setIndex(String index) {
		this.index = index;
	}

	/**
	 * @return the nightWork
	 */
	public boolean isNightWork() {
		return nightWork;
	}

	/**
	 * @param nightWork the nightWork to set
	 */
	public void setNightWork(boolean nightWork) {
		this.nightWork = nightWork;
	}

	/**
	 * @return the appointmentFrom
	 */
	public Date getAppointmentFrom() {
		return appointmentFrom;
	}

	/**
	 * @param appointmentFrom the appointmentFrom to set
	 */
	public void setAppointmentFrom(Date appointmentFrom) {
		this.appointmentFrom = appointmentFrom;
	}

	/**
	 * @return the appointmentTo
	 */
	public Date getAppointmentTo() {
		return appointmentTo;
	}

	/**
	 * @param appointmentTo the appointmentTo to set
	 */
	public void setAppointmentTo(Date appointmentTo) {
		this.appointmentTo = appointmentTo;
	}

	/**
	 * @return the reason
	 */
	public ContractDeTravailReason getReason() {
		return reason;
	}

	/**
	 * @param reason the reason to set
	 */
	public void setReason(ContractDeTravailReason reason) {
		this.reason = reason;
	}

	/**
	 * @return the absentEmployee
	 */
	public AEDescriptive getAbsentEmployee() {
		return absentEmployee;
	}

	/**
	 * @param absentEmployee the absentEmployee to set
	 */
	public void setAbsentEmployee(AEDescriptive absentEmployee) {
		this.absentEmployee = absentEmployee;
	}

	/**
	 * @return the appointmentPeriod
	 */
	public long getAppointmentPeriod() {
		return appointmentPeriod;
	}

	/**
	 * @param appointmentPeriod the appointmentPeriod to set
	 */
	public void setAppointmentPeriod(long appointmentPeriod) {
		this.appointmentPeriod = appointmentPeriod;
	}

	/**
	 * @return the fullTime
	 */
	public boolean isFullTime() {
		return fullTime;
	}

	/**
	 * @param fullTime the fullTime to set
	 */
	public void setFullTime(boolean fullTime) {
		this.fullTime = fullTime;
	}

	/**
	 * @return the hoursPerWeek
	 */
	public Double getHoursPerWeek() {
		return hoursPerWeek;
	}

	/**
	 * @param hoursPerWeek the hoursPerWeek to set
	 */
	public void setHoursPerWeek(Double hoursPerWeek) {
		this.hoursPerWeek = hoursPerWeek;
	}

	/**
	 * @return the trialPeriod
	 */
	public Double getTrialPeriod() {
		return trialPeriodQty;
	}

	/**
	 * @param trialPeriod the trialPeriod to set
	 */
	public void setTrialPeriod(Double trialPeriod) {
		this.trialPeriodQty = trialPeriod;
	}

	/**
	 * @return the subType
	 */
	public ContractDeTravailType getSubType() {
		return subType;
	}

	/**
	 * @param subType the subType to set
	 */
	public void setSubType(ContractDeTravailType subType) {
		this.subType = subType;
	}

	/**
	 * @return the trialPeriodQty
	 */
	public Double getTrialPeriodQty() {
		return trialPeriodQty;
	}

	/**
	 * @param trialPeriodQty the trialPeriodQty to set
	 */
	public void setTrialPeriodQty(Double trialPeriodQty) {
		this.trialPeriodQty = trialPeriodQty;
	}

	/**
	 * @return the trialPeriodUOM
	 */
	public UnitOfMeasurement getTrialPeriodUOM() {
		return trialPeriodUOM;
	}

	/**
	 * @param trialPeriodUOM the trialPeriodUOM to set
	 */
	public void setTrialPeriodUOM(UnitOfMeasurement trialPeriodUOM) {
		this.trialPeriodUOM = trialPeriodUOM;
	}

	/**
	 * @return the renewable
	 */
	public boolean isRenewable() {
		return renewable;
	}

	/**
	 * @param renewable the renewable to set
	 */
	public void setRenewable(boolean renewable) {
		this.renewable = renewable;
	}

	/**
	 * @return the contractAssistence
	 */
	public ContractDeTravailAssistence getContractAssistence() {
		return contractAssistence;
	}

	/**
	 * @param contractAssistence the contractAssistence to set
	 */
	public void setContractAssistence(ContractDeTravailAssistence contractAssistence) {
		this.contractAssistence = contractAssistence;
	}

	/**
	 * @return the grossAmount
	 */
	public Double getGrossAmount() {
		return grossAmount;
	}

	/**
	 * @param grossAmount the grossAmount to set
	 */
	public void setGrossAmount(Double grossAmount) {
		this.grossAmount = grossAmount;
	}

	/**
	 * @return the currency
	 */
	public UnitOfMeasurement getCurrency() {
		return currency;
	}

	/**
	 * @param currency the currency to set
	 */
	public void setCurrency(UnitOfMeasurement currency) {
		this.currency = currency;
	}

	/**
	 * @return the perQty
	 */
	public Double getPerQty() {
		return perQty;
	}

	/**
	 * @param perQty the perQty to set
	 */
	public void setPerQty(Double perQty) {
		this.perQty = perQty;
	}

	/**
	 * @return the perUOF
	 */
	public UnitOfMeasurement getPerUOF() {
		return perUOF;
	}

	/**
	 * @param perUOF the perUOF to set
	 */
	public void setPerUOF(UnitOfMeasurement perUOF) {
		this.perUOF = perUOF;
	}

	/**
	 * @return the verifiedBy
	 */
	public AEDescriptive getVerifiedBy() {
		return verifiedBy;
	}

	/**
	 * @param verifiedBy the verifiedBy to set
	 */
	public void setVerifiedBy(AEDescriptive verifiedBy) {
		this.verifiedBy = verifiedBy;
	}

	/**
	 * @return the bankAccount
	 */
	public BankAccount getBankAccount() {
		return bankAccount;
	}

	/**
	 * @param bankAccount the bankAccount to set
	 */
	public void setBankAccount(BankAccount bankAccount) {
		this.bankAccount = bankAccount;
	}

	/**
	 * @return the absentQualification
	 */
	public String getAbsentQualification() {
		return absentQualification;
	}

	/**
	 * @param absentQualification the absentQualification to set
	 */
	public void setAbsentQualification(String absentQualification) {
		this.absentQualification = absentQualification;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
//		name : 'student', type : 'boolean', defaultValue : false},
		if(jsonObject.has(JSONKey.student)) {
			setStudent(jsonObject.optBoolean(JSONKey.student));
		}
//		{name : 'workingDuringVacation', type : 'boolean', defaultValue : false},
		if(jsonObject.has(JSONKey.workingDuringVacation)) {
			setWorkDuringVacation(jsonObject.optBoolean(JSONKey.workingDuringVacation));
		}
//		{name : 'proffesionDuration', type : 'double'},
		if(jsonObject.has(JSONKey.proffesionDuration)) {
			setProfessionDuration(JSONUtil.normDouble(jsonObject, JSONKey.proffesionDuration));
		}
//		{name : 'formerEmployee', type : 'boolean', defaultValue : false},
		if(jsonObject.has(JSONKey.formerEmployee)) {
			setFormerEmployee(jsonObject.optBoolean(JSONKey.formerEmployee));
		}
//		{name : 'dateOfEntry', type : 'date', dateFormat : 'd-m-Y'},
		String sDate = AEStringUtil.EMPTY_STRING;
		String sTime = AEStringUtil.EMPTY_STRING;
		if(jsonObject.has(JSONKey.dateOfEntry)) {
			sDate = jsonObject.optString(JSONKey.dateOfEntry);
		}
		if(jsonObject.has(JSONKey.timeOfEntry)) {
			sTime = jsonObject.optString(JSONKey.timeOfEntry);
		}
		setDateOfEntry(AEDateUtil.parseDateStrict((sDate + " " + sTime).trim()));
//		{name : 'declarationDUE', 	type : 'boolean', defaultValue : false},
		if(jsonObject.has(JSONKey.declarationDUE)) {
			setDeclarationDUE(jsonObject.optBoolean(JSONKey.declarationDUE));
		}
//		{name : 'declarationDUEDate', type : 'date', dateFormat : 'd-m-Y'},
		if(jsonObject.has(JSONKey.declarationDUEDate)) {
			setDeclarationDUEDate(AEDateUtil.parseDateStrict(jsonObject.getString(JSONKey.declarationDUEDate)));
		}
//		{name : 'declarationDUENumber', type : 'string'},
		if(jsonObject.has(JSONKey.declarationDUENumber)) {
			setDeclarationDUENumber(jsonObject.optString(JSONKey.declarationDUENumber));
		}
//		{name : 'employment ', type : 'string'},
		if(jsonObject.has(JSONKey.employment)) {
			setEmployment(jsonObject.optString(JSONKey.employment));
		}
//		{name : 'qualification', type : 'string'},
		if(jsonObject.has(JSONKey.qualification)) {
			setQualification(jsonObject.optString(JSONKey.qualification));
		}
//		{name : 'employmentClassification_ID', type : 'long'}, // {10, employed} {20, worker} {30, control } {40, cadre}
		if(jsonObject.has(JSONKey.employmentClassification_ID)) {
			setEmploymentClassification(
					new EmploymentClassification(
							EmploymentClassification.System.valueOf(jsonObject.optLong(JSONKey.employmentClassification_ID))));
		}
//		{name : 'coefficient', type : 'double'},
		if(jsonObject.has(JSONKey.coefficient)) {
			setCoefficient(JSONUtil.normDouble(jsonObject, JSONKey.coefficient));
		}
//		{name : 'ae_level', type : 'string'},
		if(jsonObject.has(JSONKey.ae_level)) {
			setLevel(jsonObject.optString(JSONKey.ae_level));
		}
//		{name : 'echelon', type : 'string'},
		if(jsonObject.has(JSONKey.echelon)) {
			setEchelon(jsonObject.optString(JSONKey.echelon));
		}
//		{name : 'position', type : 'string'},
		if(jsonObject.has(JSONKey.position)) {
			setPosition(jsonObject.optString(JSONKey.position));
		}
//		{name : 'ae_index', type : 'string'},
		if(jsonObject.has(JSONKey.ae_index)) {
			setIndex(jsonObject.optString(JSONKey.ae_index));
		}
				
		if(jsonObject.has(JSONKey.addSalaryPerc)) {
			try {
				setAddSalaryPerc(jsonObject.getDouble(JSONKey.addSalaryPerc));
			} catch (Exception e) {
				// in case of nonparseable value, 
			}
		}
//		{name : 'nightWork', type : 'boolean', defaultValue : false},
		if(jsonObject.has(JSONKey.nightWork)) {
			setNightWork(jsonObject.optBoolean(JSONKey.nightWork));
		}
//		{name : 'subTypeId', type : 'long'}, // {10, CDI} {20, CDD}
		if(jsonObject.has(JSONKey.subTypeId)) {
			setSubType(new ContractDeTravailType(
					ContractDeTravailType.System.valueOf(
							jsonObject.optLong(JSONKey.subTypeId))));
		}
//		{name : 'appointmentFrom', type : 'date', dateFormat : 'd-m-Y'},
		if(jsonObject.has(JSONKey.appointmentFrom)) {
			setAppointmentFrom(AEDateUtil.parseDateStrict(jsonObject.optString(JSONKey.appointmentFrom)));
		}
//		{name : 'appointmentTo', type : 'date', dateFormat : 'd-m-Y'},
		if(jsonObject.has(JSONKey.appointmentTo)) {
			setAppointmentTo(AEDateUtil.parseDateStrict(jsonObject.optString(JSONKey.appointmentTo)));
		}
//		{name : 'reasonId', type : 'long'}, // {10, ...} {20, ...}
		if(jsonObject.has(JSONKey.reasonId)) {
			long reasonId = jsonObject.optLong(JSONKey.reasonId);
			if(reasonId > 0) {
				setReason(new ContractDeTravailReason(ContractDeTravailReason.System.valueOf(reasonId)));
			}
		}
//		{name : 'absentEmployeeId', type : 'long'},
//		{name : 'absentEmployeeName', type : 'string'},
		if(jsonObject.has(JSONKey.absentEmployeeId) 
				|| jsonObject.has(JSONKey.absentEmployeeTitle)
				|| jsonObject.has(JSONKey.absentEmployeeName) 
				|| jsonObject.has(JSONKey.absentEmployeeFirstName)) {
			
			AEDescriptor absentEmpl = Employee.lazyDescriptor(jsonObject.optLong(JSONKey.absentEmployeeId));
			absentEmpl.setCode(jsonObject.optString(JSONKey.absentEmployeeTitle));
			absentEmpl.setName(jsonObject.optString(JSONKey.absentEmployeeName));
			absentEmpl.setDescription(jsonObject.optString(JSONKey.absentEmployeeFirstName));
			setAbsentEmployee(absentEmpl);
		}
//		{name : 'absentQualification', type : 'string'},
		if(jsonObject.has(JSONKey.absentQualification)) {
			setAbsentQualification(jsonObject.optString(JSONKey.absentQualification));
		}
//		{name : 'absentEchelon', type : 'string'},
		if(jsonObject.has(JSONKey.absentEchelon)) {
			setAbsentEchelon(jsonObject.optString(JSONKey.absentEchelon));
		}
//		{name : 'absentEmployment ', type : 'string'},
		if(jsonObject.has(JSONKey.absentEmployment)) {
			setAbsentEmployment(jsonObject.optString(JSONKey.absentEmployment));
		}

//		{name : 'absentEmploymentClassification_ID', type : 'long'},
		if(jsonObject.has(JSONKey.absentEmploymentClassification_ID)) {
			setAbsentEmploymentClassification(
					new EmploymentClassification(
							EmploymentClassification.System.valueOf(
									jsonObject.optLong(JSONKey.absentEmploymentClassification_ID))));
		}
//		{name : 'absentCoefficient', type : 'double'},
		if(jsonObject.has(JSONKey.absentCoefficient)) {
			setAbsentCoefficient(JSONUtil.normDouble(jsonObject, JSONKey.absentCoefficient));
		}
//		{name : 'absentLevel', type : 'string'},
		if(jsonObject.has(JSONKey.absentLevel)) {
			setAbsentLevel(jsonObject.optString(JSONKey.absentLevel));
		}

//		{name : 'absentPosition', type : 'string'},
		if(jsonObject.has(JSONKey.absentPosition)) {
			setAbsentPosition(jsonObject.optString(JSONKey.absentPosition));
		}
//		{name : 'absentIndex', type : 'string'},
		if(jsonObject.has(JSONKey.absentIndex)) {
			setAbsentIndex(jsonObject.optString(JSONKey.absentIndex));
		}
		
//		{name : 'appointmentPeriod', type : 'long'},
		if(jsonObject.has(JSONKey.appointmentPeriod)) {
			setAppointmentPeriod(jsonObject.optLong(JSONKey.appointmentPeriod));
		}
		
		// cddReplacementNpDurationUoM
		if(jsonObject.has(JSONKey.cddReplacementNpDurationUoM)) {
			setCddReplacementNpDurationUoM(UnitOfMeasurement.getByID(jsonObject.optLong(JSONKey.cddReplacementNpDurationUoM)));
		}
		
//		{name : 'fullTime', type : 'boolean', defaultValue : true},
		if(jsonObject.has(JSONKey.fullTime)) {
			setFullTime(jsonObject.optBoolean(JSONKey.fullTime));
		}
//		{name : 'hoursPerWeek', type : 'double'},
		if(jsonObject.has(JSONKey.hoursPerWeek)) {
			setHoursPerWeek(JSONUtil.normDouble(jsonObject, JSONKey.hoursPerWeek));
		}
//		{name : 'trialPeriodQty', type : 'double'},
		if(jsonObject.has(JSONKey.trialPeriodQty)) {
			setTrialPeriodQty(JSONUtil.normDouble(jsonObject, JSONKey.trialPeriodQty));
		}
//		{name : 'trialPeriodUOMId', type : 'long'}, // {100, hour} {110, day} {130, month}
		if(jsonObject.has(JSONKey.trialPeriodUOMId)) {
			this.setTrialPeriodUOM(new UnitOfMeasurement(jsonObject.getLong(JSONKey.trialPeriodUOMId)));
		}
//		{name : 'renewable', type : 'boolean', defaultValue : false},
		if(jsonObject.has(JSONKey.renewable)) {
			setRenewable(jsonObject.optBoolean(JSONKey.renewable));
		}
//		{name : 'contractAssistenceId', type : 'long'},
		if(jsonObject.has(JSONKey.contractAssistenceId)) {
			setContractAssistence(new ContractDeTravailAssistence(
					ContractDeTravailAssistence.System.valueOf(jsonObject.optLong(JSONKey.contractAssistenceId))));
		}
//		{name : 'grossAmount', type : 'double'},
		if(jsonObject.has(JSONKey.grossAmount)) {
			// setGrossAmount(JSONUtil.normDouble(jsonObject, JSONKey.grossAmount));
			try {
				setGrossAmount(JSONUtil.parseDouble(jsonObject, JSONKey.grossAmount));
			} catch (AEException e) {
				throw new JSONException(e.getMessage());
			}
		}
//		{name : 'currencyId', type : 'long'},
		
//		{name : 'perQty', type : 'double'},
		if(jsonObject.has(JSONKey.perQty)) {
			setPerQty(JSONUtil.normDouble(jsonObject, JSONKey.perQty));
		}
//		{name : 'perUOMId', type : 'long'}, // {100, hour} {110, day} {130, month}
		if(jsonObject.has(JSONKey.perUOMId)) {
			this.setPerUOF(new UnitOfMeasurement(jsonObject.getLong(JSONKey.perUOMId)));
		}
//      JSONKey.verifiedBySalutation
		if(jsonObject.has(JSONKey.verifiedBySalutation)) {
			long salutationId = jsonObject.optLong(JSONKey.verifiedBySalutation);
			Person.SalutationType salType = Person.SalutationType.valueOf(salutationId);
			if(salType != null && !Person.SalutationType.NA.equals(salType)) {
				setVerifiedBySalutation(salType);
			}
		}
//		{name : 'verifiedById', type : 'long'},
//		{name : 'vrifiedByName', type : 'string'},
		if(jsonObject.has(JSONKey.verifiedById) || jsonObject.has(JSONKey.verifiedByName)) {
			AEDescriptor verifiedByEmpl = Employee.lazyDescriptor(jsonObject.optLong(JSONKey.verifiedById));
			verifiedByEmpl.setName(jsonObject.optString(JSONKey.verifiedByName));
			setVerifiedBy(verifiedByEmpl);
		}
//		{name : 'bankAccountIBAN', type : 'string'},
//		{name : 'bankAccountBIC', type : 'string'},
//		{name : 'bankAccountBankName', type : 'string'}]
		if(jsonObject.has(JSONKey.modificationId)) {
			long modificationId = jsonObject.optLong(JSONKey.modificationId);
			if(modificationId > 0) {
				AEDocumentDescriptor modDescr = ContractDeTravail.lazyDescriptor(modificationId);
				modDescr.setDate(AEDateUtil.parseDateStrict(jsonObject.optString(JSONKey.modificationDate)));
				setModificationDescr(modDescr);
			}
		}
		
		if(jsonObject.has(JSONKey.effectiveDate)) {
			sDate = jsonObject.optString(JSONKey.effectiveDate);
			setEffectiveDate(AEDateUtil.parseDateStrict(sDate));
		}
		
		if(jsonObject.has(JSONKey.replacementPartiel)) {
			setReplacementPartiel(jsonObject.optBoolean(JSONKey.replacementPartiel));
		}
		
		// daysPerWeek
		if(jsonObject.has(JSONKey.daysPerWeek)) {
			setDaysPerWeek(JSONUtil.normDouble(jsonObject, JSONKey.daysPerWeek));
		}
		
		// cdiNonAnnualise
		if(jsonObject.has(JSONKey.cdiNonAnnualise)) {
			setCdiNonAnnualise(jsonObject.optBoolean(JSONKey.cdiNonAnnualise));
		}
		
		if(jsonObject.has(JSONKey.appointmentFixed)) {
			setAppointmentFixed(jsonObject.optBoolean(JSONKey.appointmentFixed));
		}
		
		if(jsonObject.has(JSONKey.mutuelle)) {
			setMutuelle(jsonObject.optBoolean(JSONKey.mutuelle));
		}
		
		// AppointmentStartDateTime
		String appointmentStartDate = AEStringUtil.EMPTY_STRING;
		String appointmentStartTime = AEStringUtil.EMPTY_STRING;
		if(jsonObject.has(JSONKey.appointmentStartDate)) {
			appointmentStartDate = jsonObject.optString(JSONKey.appointmentStartDate);
		}
		if(jsonObject.has(JSONKey.appointmentStartTime)) {
			appointmentStartTime = jsonObject.optString(JSONKey.appointmentStartTime);
		}
		setAppointmentStartDateTime(AEDateUtil.parseDateStrict((appointmentStartDate + " " + appointmentStartTime).trim()));
	}	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		json.put(JSONKey.student, isStudent());
		json.put(JSONKey.workingDuringVacation, isWorkDuringVacation());
		if(getProfessionDuration() != null) {
			json.put(JSONKey.proffesionDuration, getProfessionDuration());
		}
		json.put(JSONKey.formerEmployee, isFormerEmployee());
		if(getDateOfEntry() != null) {
			json.put(JSONKey.dateOfEntry, AEDateUtil.formatToSystem(getDateOfEntry()));
		}
		if(getDateOfEntry() != null) {
			json.put(JSONKey.timeOfEntry, AEDateUtil.formatTimeToSystem(getDateOfEntry()));
		}
		json.put(JSONKey.declarationDUE, isDeclarationDUE());
		if(getDeclarationDUEDate() != null) {
			json.put(JSONKey.declarationDUEDate, AEDateUtil.formatToSystem(getDeclarationDUEDate()));
		}
		if(!AEStringUtil.isEmpty(getDeclarationDUENumber())) {
			json.put(JSONKey.declarationDUENumber, getDeclarationDUENumber());
		}
		if(!AEStringUtil.isEmpty(getEmployment())) {
			json.put(JSONKey.employment, getEmployment());
		}
		if(!AEStringUtil.isEmpty(getQualification())) {
			json.put(JSONKey.qualification, getQualification());
		}
//		{name : 'employmentClassification_ID', type : 'long'}, // {10, employed} {20, worker} {30, control } {40, cadre}
		if(getEmploymentClassification() != null) {
			json.put(JSONKey.employmentClassification_ID, getEmploymentClassification().getSystemID());
		}
		if(getCoefficient() != null) {
			json.put(JSONKey.coefficient, getCoefficient());
		}
		if(!AEStringUtil.isEmpty(getLevel())) {
			json.put(JSONKey.ae_level, getLevel());
		}
		if(!AEStringUtil.isEmpty(getEchelon())) {
			json.put(JSONKey.echelon, getEchelon());
		}
		if(!AEStringUtil.isEmpty(getPosition())) {
			json.put(JSONKey.position, getPosition());
		}
		if(!AEStringUtil.isEmpty(getIndex())) {
			json.put(JSONKey.ae_index, getIndex());
		}		
		if(getAddSalaryPerc() != null) {
			json.put(JSONKey.addSalaryPerc, getAddSalaryPerc());
		}
		json.put(JSONKey.nightWork, isNightWork());
		if(getSubType() != null) {
			json.put(JSONKey.subTypeId, getSubType().getSystemID());
		}
		if(getAppointmentFrom() != null) {
			json.put(JSONKey.appointmentFrom, AEDateUtil.formatToSystem(getAppointmentFrom()));
		}
		if(getAppointmentTo() != null) {
			json.put(JSONKey.appointmentTo, AEDateUtil.formatToSystem(getAppointmentTo()));
		}
		if(getReason() != null) {
			json.put(JSONKey.reasonId, getReason().getSystemID());
		}
		if(getAbsentEmployee() != null) {
			json.put(JSONKey.absentEmployeeId, getAbsentEmployee().getDescriptor().getID());
			json.put(JSONKey.absentEmployeeTitle, getAbsentEmployee().getDescriptor().getCode());
			json.put(JSONKey.absentEmployeeName, getAbsentEmployee().getDescriptor().getName());
			json.put(JSONKey.absentEmployeeFirstName, getAbsentEmployee().getDescriptor().getDescription());
		}
		if(!AEStringUtil.isEmpty(getAbsentQualification())) {
			json.put(JSONKey.absentQualification, getAbsentQualification());
		}
		if(!AEStringUtil.isEmpty(getAbsentEchelon())) {
			json.put(JSONKey.absentEchelon, getAbsentEchelon());
		}
		if(!AEStringUtil.isEmpty(getAbsentEmployment())) {
			json.put(JSONKey.absentEmployment, getAbsentEmployment());
		}
		if(getAbsentEmploymentClassification() != null) {
			json.put(
					JSONKey.absentEmploymentClassification_ID, 
					getAbsentEmploymentClassification().getSystemID());
		}
		if(getAbsentCoefficient() != null) {
			json.put(JSONKey.absentCoefficient, getAbsentCoefficient());
		}
		if(!AEStringUtil.isEmpty(getAbsentLevel())) {
			json.put(JSONKey.absentLevel, getAbsentLevel());
		}
		if(!AEStringUtil.isEmpty(getAbsentPosition())) {
			json.put(JSONKey.absentPosition, getAbsentPosition());
		}
		if(!AEStringUtil.isEmpty(getAbsentIndex())) {
			json.put(JSONKey.absentIndex, getAbsentIndex());
		}
		json.put(JSONKey.appointmentPeriod, getAppointmentPeriod());
		if(getCddReplacementNpDurationUoM() != null) {
			json.put(JSONKey.cddReplacementNpDurationUoM, getCddReplacementNpDurationUoM().getID());
		}	
		json.put(JSONKey.fullTime, isFullTime());
		if(getHoursPerWeek() != null) {
			json.put(JSONKey.hoursPerWeek, getHoursPerWeek());
		}
		if(getTrialPeriodQty() != null) {
			json.put(JSONKey.trialPeriodQty, getTrialPeriodQty());
		}
		if(getTrialPeriodUOM() != null) {
			json.put(JSONKey.trialPeriodUOMId, getTrialPeriodUOM().getID());
		}
		json.put(JSONKey.renewable, isRenewable());
		if(getContractAssistence() != null) {
			json.put(JSONKey.contractAssistenceId, getContractAssistence().getSystemID());
		}
		if(getGrossAmount() != null) {
			json.put(JSONKey.grossAmount, AEMath.toAmountString(getGrossAmount()));
		}
//		{name : 'currencyId', type : 'long'},
		
		if(getPerQty() != null) {
			json.put(JSONKey.perQty, getPerQty());
		}
		if(getPerQty() != null) {
			json.put(JSONKey.perUOMId, getPerUOF().getID());
		}
//      JSONKey.verifiedBySalutation
		if(getVerifiedBySalutation() != null && !Person.SalutationType.NA.equals(getVerifiedBySalutation())) {
			json.put(JSONKey.verifiedBySalutation, getVerifiedBySalutation().getTypeID());
		}
		if(getVerifiedBy() != null) {
			json.put(JSONKey.verifiedById, getVerifiedBy().getDescriptor().getID());
			json.put(JSONKey.verifiedByName, getVerifiedBy().getDescriptor().getName());
		}
//		{name : 'bankAccountIBAN', type : 'string'},
//		{name : 'bankAccountBIC', type : 'string'},
//		{name : 'bankAccountBankName', type : 'string'}]
		if(getModificationDescr() != null) {
			json.put(JSONKey.modificationId, getModificationDescr().getID());
			json.put(JSONKey.modificationDate, AEDateUtil.formatToSystem(getModificationDescr().getDate()));
		}
		if(getEffectiveDate() != null) {
			json.put(JSONKey.effectiveDate, AEDateUtil.formatToSystem(getEffectiveDate()));
		}
		
		json.put(JSONKey.replacementPartiel, isReplacementPartiel());
		
		if(getDaysPerWeek() != null) {
			json.put(JSONKey.daysPerWeek, getDaysPerWeek());
		}
		
		json.put(JSONKey.cdiNonAnnualise, isCdiNonAnnualise());
		
		json.put(JSONKey.appointmentFixed, isAppointmentFixed());
		
		json.put(JSONKey.mutuelle, isMutuelle());
		
		if(this.getAppointmentStartDateTime() != null) {
			json.put(JSONKey.appointmentStartDate.toString(), AEDateUtil.formatToSystem(this.getAppointmentStartDateTime()));
			json.put(JSONKey.appointmentStartTime.toString(), AEDateUtil.formatTimeToSystem(this.getAppointmentStartDateTime()));
		}
		
		return json;
	}

	public Double getAddSalaryPerc() {
		return addSalaryPerc;
	}

	public void setAddSalaryPerc(Double addSalaryPerc) {
		this.addSalaryPerc = addSalaryPerc;
	}

	public AEDocumentDescriptor getModificationDescr() {
		return modificationDescr;
	}

	public void setModificationDescr(AEDocumentDescriptor modificationDescr) {
		this.modificationDescr = modificationDescr;
	}
	
	public static AEDocumentDescriptor lazyDescriptor(long id) {
		return new AEDocumentDescriptorImp(id, AEDocumentType.valueOf(AEDocumentType.System.ContractDeTravail));
	}

	public Date getEffectiveDate() {
		return effectiveDate;
	}

	public void setEffectiveDate(Date effectiveDate) {
		this.effectiveDate = effectiveDate;
	}
	
	public final boolean isActual() {
		return getModificationDescr() == null;
	}

	public boolean isReplacementPartiel() {
		return replacementPartiel;
	}

	public void setReplacementPartiel(boolean replacementPartiel) {
		this.replacementPartiel = replacementPartiel;
	}

	public boolean isCdiNonAnnualise() {
		return cdiNonAnnualise;
	}

	public void setCdiNonAnnualise(boolean cdiNonAnnualise) {
		this.cdiNonAnnualise = cdiNonAnnualise;
	}

	public Double getDaysPerWeek() {
		return daysPerWeek;
	}

	public void setDaysPerWeek(Double daysPerWeek) {
		this.daysPerWeek = daysPerWeek;
	}

	public boolean isAppointmentFixed() {
		return appointmentFixed;
	}

	public void setAppointmentFixed(boolean appointmentFixed) {
		this.appointmentFixed = appointmentFixed;
	}

	public String getAbsentEchelon() {
		return absentEchelon;
	}

	public void setAbsentEchelon(String absentEchelon) {
		this.absentEchelon = absentEchelon;
	}

	public boolean isMutuelle() {
		return mutuelle;
	}

	public void setMutuelle(boolean mutuelle) {
		this.mutuelle = mutuelle;
	}

	public Date getAppointmentStartDateTime() {
		return appointmentStartDateTime;
	}

	public void setAppointmentStartDateTime(Date appointmentStartDateTime) {
		this.appointmentStartDateTime = appointmentStartDateTime;
	}

	public boolean isBuildLazzy() {
		return buildLazzy;
	}

	public void setBuildLazzy(boolean buildLazzy) {
		this.buildLazzy = buildLazzy;
	}

	public String getAbsentEmployment() {
		return absentEmployment;
	}

	public void setAbsentEmployment(String absentEmployment) {
		this.absentEmployment = absentEmployment;
	}

	public EmploymentClassification getAbsentEmploymentClassification() {
		return absentEmploymentClassification;
	}

	public void setAbsentEmploymentClassification(
			EmploymentClassification absentEmploymentClassification) {
		this.absentEmploymentClassification = absentEmploymentClassification;
	}

	public Double getAbsentCoefficient() {
		return absentCoefficient;
	}

	public void setAbsentCoefficient(Double absentCoefficient) {
		this.absentCoefficient = absentCoefficient;
	}

	public String getAbsentLevel() {
		return absentLevel;
	}

	public void setAbsentLevel(String absentLevel) {
		this.absentLevel = absentLevel;
	}

	public String getAbsentPosition() {
		return absentPosition;
	}

	public void setAbsentPosition(String absentPosition) {
		this.absentPosition = absentPosition;
	}

	public String getAbsentIndex() {
		return absentIndex;
	}

	public void setAbsentIndex(String absentIndex) {
		this.absentIndex = absentIndex;
	}

	public Person.SalutationType getVerifiedBySalutation() {
		return verifiedBySalutation;
	}

	public void setVerifiedBySalutation(Person.SalutationType verifiedBySalutation) {
		this.verifiedBySalutation = verifiedBySalutation;
	}

	public UnitOfMeasurement getCddReplacementNpDurationUoM() {
		return cddReplacementNpDurationUoM;
	}

	public void setCddReplacementNpDurationUoM(
			UnitOfMeasurement cddReplacementNpDurationUoM) {
		this.cddReplacementNpDurationUoM = cddReplacementNpDurationUoM;
	}
}
