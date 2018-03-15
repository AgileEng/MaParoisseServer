/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 20.06.2010 17:42:00
 */
package eu.agileeng.persistent.dao.document.contractdeTravail;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.AEWarning;
import eu.agileeng.domain.business.ContractDeTravailReason;
import eu.agileeng.domain.business.EmploymentClassification;
import eu.agileeng.domain.contact.Employee;
import eu.agileeng.domain.contact.Person;
import eu.agileeng.domain.document.AEDocument;
import eu.agileeng.domain.document.AEDocumentDescriptor;
import eu.agileeng.domain.document.AEDocumentFilter;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.AEDocumentsList;
import eu.agileeng.domain.document.social.AESocialDocumentFilter;
import eu.agileeng.domain.document.social.contractdetravail.ContractDeTravail;
import eu.agileeng.domain.document.social.contractdetravail.ContractDeTravailAssistence;
import eu.agileeng.domain.document.social.contractdetravail.ContractDeTravailType;
import eu.agileeng.domain.measurement.UnitOfMeasurement;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.document.AEDocumentDAO;
import eu.agileeng.persistent.dao.oracle.EmployeeDAO;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AETimePeriod;

/**
 *
 */
public class CotractDeTravailDAO extends AEDocumentDAO {
	private static String selectSQL = 
		"select document.*, ContractDeTravail.* from ContractDeTravail inner join document on ContractDeTravail.iddocument = document.id"
		+ " where ContractDeTravail.iddocument = ?";
	
	private static String selectSQLByEmployee = 
		"select top 1 document.*, ContractDeTravail.* from ContractDeTravail inner join document on ContractDeTravail.iddocument = document.id"
		+ " where ContractDeTravail.EmployeeID = ? order by document.id desc";

	private static String selectSQLActual = 
		"select ContractDeTravail.*, document.* "
		+ " from ContractDeTravail inner join document on ContractDeTravail.iddocument = document.id "
		+ " inner join Employee on ContractDeTravail.EmployeeID = Employee.ID "
		+ " where ContractDeTravail.EmployeeID = ? and ContractDeTravail.Modification_ID is null ";
//		+ " and Employee.DateRelease is null";
		
	private static String insertSQL = "insert into ContractDeTravail "
		+ "(IDDocument, EmployeeID, WorkingDuringVacation, ProffesionDuration, FormerEmployee, "
		+ " DateOfEntry, DeclarationDUE, DeclarationDUEDate, DeclarationDUENumber, Employment, "
		+ " Qualification, EmploymentClassification_ID, Coefficient, AE_Level, Echelon, "
		+ " Position, AE_Index, NightWork, SubType_ID, AppointmentFrom, AppointmentTo, Reason_ID, "
		+ " AbsentEmployee_ID, AbsentEmployee_Title, AbsentEmployee_Name, AbsentEmployee_FirstName, AbsentQualification, AppointmentPeriod, FullTime, "
		+ " HoursPerWeek, TrialPeriodQty, TrialPeriodUOM_ID, Renewable, ContractAssistence_ID, "
		+ " GrossAmount, Currency_ID, PerQty, PerUOM_ID, VerifiedBy_ID, VerifiedBy_Name, BankAccount_IBAN, "
		+ " BankAccount_BIC, BankAccount_BankName, AddSalaryPerc, EffectiveDate, ReplacementPartiel, "
		+ " CdiNonAnnualise, DaysPerWeek, AppointmentFixed, AbsentEchelon, Mutuelle, AppointmentStartDateTime, "
		+ " AbsentEmployment, AbsentEmploymentClassification_ID, AbsentCoefficient, AbsentLevel, AbsentPosition, AbsentIndex, "
		+ " VerifiedBySalutation_ID, cddReplacementNpDurationUoM) "
		+ " values (?, ?, ?, ?, ?, "
		+ " ?, ?, ?, ?, ?, "
		+ " ?, ?, ?, ?, ?, "
		+ " ?, ?, ?, ?, ?, ?, ?, "
		+ " ?, ?, ?, ?, ?, ?, ?, "
		+ " ?, ?, ?, ?, ?, "
		+ " ?, ?, ?, ?, ?, ?, ?, "
		+ " ?, ?, ?, ?, ?, "
		+ " ?, ?, ?, ?, ?, ?, "
		+ " ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static String updateSQL = "update ContractDeTravail set "
		+ "IDDocument = ?, EmployeeID = ?, WorkingDuringVacation = ?, ProffesionDuration = ?, FormerEmployee = ?, "
		+ " DateOfEntry = ?, DeclarationDUE = ?, DeclarationDUEDate = ?, DeclarationDUENumber = ?, Employment = ?, "
		+ " Qualification = ?, EmploymentClassification_ID = ?, Coefficient = ?, AE_Level = ?, Echelon = ?, "
		+ " Position = ?, AE_Index = ?, NightWork = ?, SubType_ID = ?, AppointmentFrom = ?, AppointmentTo = ?, Reason_ID = ?, "
		+ " AbsentEmployee_ID = ?, AbsentEmployee_Title = ?, AbsentEmployee_Name = ?, AbsentEmployee_FirstName = ?, AbsentQualification = ?, AppointmentPeriod = ?, FullTime = ?, "
		+ " HoursPerWeek = ?, TrialPeriodQty = ?, TrialPeriodUOM_ID = ?, Renewable = ?, ContractAssistence_ID = ?, "
		+ " GrossAmount = ?, Currency_ID = ?, PerQty = ?, PerUOM_ID = ?, VerifiedBy_ID = ?, VerifiedBy_Name = ?, BankAccount_IBAN = ?, "
		+ " BankAccount_BIC = ?, BankAccount_BankName = ?, AddSalaryPerc = ?, "
		+ " EffectiveDate = ?, ReplacementPartiel = ?, CdiNonAnnualise = ?, DaysPerWeek = ?, "
		+ " AppointmentFixed = ?, AbsentEchelon = ?, Mutuelle = ?, AppointmentStartDateTime = ?, "
		+ " AbsentEmployment = ?, AbsentEmploymentClassification_ID = ?, AbsentCoefficient = ?, AbsentLevel = ?, "
		+ " AbsentPosition = ?, AbsentIndex = ?, VerifiedBySalutation_ID = ?, cddReplacementNpDurationUoM = ? " 
		+ " where IDDocument = ?";
	
	/*
	 * Keep synchronised: selectSQL2, selectSQLCountAll, selectSQLCountNotValidated
	 */
	private static String selectSQL2 = 	
		"select ContractDeTravail.*, document.* "
		+ " from ContractDeTravail inner join document on ContractDeTravail.iddocument = document.id "
		+ " inner join Employee on ContractDeTravail.EmployeeID = Employee.ID "
		+ " where document.owner_id = ? and document.type_id = ? ";
	
	/*
	 * Keep synchronised: selectSQL2, selectSQLCountAll, selectSQLCountNotValidated
	 */
	private static String selectSQLCountAll = 	
		"select count(Document.ID) as count "
		+ " from ContractDeTravail inner join document on ContractDeTravail.iddocument = document.id "
		+ " inner join Employee on ContractDeTravail.EmployeeID = Employee.ID "
		+ " where document.owner_id = ? and document.type_id = ? ";
	
	/*
	 * Keep synchronised: selectSQL2, selectSQLCountAll, selectSQLCountNotValidated
	 */
	private static String selectSQLCountValidated = 	
		"select count(Document.ID) as count "
		+ " from ContractDeTravail inner join document on ContractDeTravail.iddocument = document.id "
		+ " inner join Employee on ContractDeTravail.EmployeeID = Employee.ID "
		+ " where document.owner_id = ? and document.type_id = ? and document.VALIDATED = 1";
	
	private static String updateSQLModification = 	
		"update ContractDeTravail set Modification_ID = ?, Modification_Date = ? where IDDocument = ?";

	private static String selectSQLImported = 	
		"SELECT TOP 1 [ID] FROM [AccBureau].[dbo].[Document] "
		+ " where (PROPERTIES & 8 = 8) and OWNER_ID = ?";
	
	private static String selectEmployeeIdSQL = 
		"select EmployeeID from ContractDeTravail where IDDocument = ?";
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public CotractDeTravailDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	@Override
	public AEDocument load(AEDescriptor docDescr) throws AEException {
		ContractDeTravail doc = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, docDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				doc = new ContractDeTravail();
				build(doc, rs);
			}
			return doc;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEDocument loadByEmployee(AEDescriptor emplDescr) throws AEException {
		ContractDeTravail doc = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLByEmployee);
			ps.setLong(1, emplDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				doc = new ContractDeTravail();
				build(doc, rs);
			}
			return doc;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public boolean alreadyImported(long ownerId) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLImported);
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			if(rs.next()) {
				return true;
			} else {
				return false;
			}
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEDocument loadActualAttestation(long employeeId) throws AEException {
		ContractDeTravail doc = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLActual);
			ps.setLong(1, employeeId);
			rs = ps.executeQuery();
			if(rs.next()) {
				doc = new ContractDeTravail();
				build(doc, rs);
			}
			return doc;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	@Override
	public AEDocumentsList loadContratDeTravail(AEDocumentFilter filter) throws AEException {
		AESocialDocumentFilter socialFilter = (AESocialDocumentFilter) filter;
		AEDocumentsList docsList = new AEDocumentsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = selectSQL2;
			if(socialFilter.getEmployee() != null) {
				Employee employee = socialFilter.getEmployee();
				if(!AEStringUtil.isEmpty(employee.getName())) {
					sql += " and Employee.NAME like '%" + employee.getName() + "%' ";
				}
				if(!AEStringUtil.isEmpty(employee.getUIN())) {
					sql += " and Employee.UIN like '%" + employee.getUIN() + "%' ";
				}
				if(employee.getDateEntry() != null) {
					sql += " and Employee.DateEntry = " + AEPersistentUtil.escapeToDate(employee.getDateEntry());
				}
				if(employee.getDateRelease() != null) {
					sql += " and Employee.DateRelease = " + AEPersistentUtil.escapeToDate(employee.getDateRelease());
				}
				if(employee.getModificationDate() != null) {
					sql += " and ContractDeTravail.Modification_Date = " + AEPersistentUtil.escapeToDate(employee.getModificationDate());
				}
				if(socialFilter.getEffectiveDate() != null) {
					sql += " and ContractDeTravail.EffectiveDate = " + AEPersistentUtil.escapeToDate(socialFilter.getEffectiveDate());
				}
			}
			
			// validated
			if(AEDocumentFilter.Validated.PROCESSED.equals(socialFilter.getValidated())) {
				sql += " and document.validated = 1";
			} else if(AEDocumentFilter.Validated.NOT_PROCESSED.equals(socialFilter.getValidated())) {
				sql += " and (document.validated = 0 or document.validated is null)";
			}
				
			sql += " order by DateOfEntry desc";
			ps = getAEConnection().prepareStatement(sql);
			ps.setLong(1, socialFilter.getCompany().getDescriptor().getID());
			ps.setLong(2, socialFilter.getDocType().getSystemID());
			rs = ps.executeQuery();
			while(rs.next()) {
				ContractDeTravail doc = new ContractDeTravail();
				doc.setBuildLazzy(true);
				build(doc, rs);
				docsList.add(doc);
			}
			return docsList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	@Override
	public void insert(AEDocument doc) throws AEException {
		assert(doc != null && !doc.isPersistent());
		assert doc instanceof ContractDeTravail : "doc instanceof ContractDeTravail failed";
		ContractDeTravail cdtDoc = (ContractDeTravail) doc;
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// insert into common table
			super.insert(cdtDoc);
			
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(insertSQL);
			build(cdtDoc, ps, 1);

			// execute
			ps.executeUpdate();
			
			// set view state
			doc.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}

	public int build(ContractDeTravail doc, PreparedStatement ps, int i) throws SQLException, AEException {
		// IDDocument
		ps.setLong(i++, doc.getID());
		// EmployeeID
		if(doc.getEmployee() != null) {
			ps.setLong(i++, doc.getEmployee().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		// WorkingDuringVacation
		ps.setInt(i++, doc.isWorkDuringVacation() ? 1 : 0);
		// ProffesionDuration
		if(doc.getProfessionDuration() != null) {
			ps.setDouble(i++, doc.getProfessionDuration());
		} else {
			ps.setNull(i++, Types.NUMERIC);
		}
		// FormerEmployee
		ps.setInt(i++, doc.isFormerEmployee() ? 1 : 0);
		// DateOfEntry
		if(doc.getDateOfEntry() != null) {
			ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(doc.getDateOfEntry()));
		} else {
			ps.setNull(i++, Types.TIMESTAMP);
		}
		// DeclarationDUE
		ps.setInt(i++, doc.isDeclarationDUE() ? 1 : 0);
		// DeclarationDUEDate
		ps.setDate(i++, AEPersistentUtil.getSQLDate(doc.getDeclarationDUEDate()));
		// DeclarationDUENumber
		ps.setString(i++, doc.getDeclarationDUENumber());
		// Employment
		ps.setString(i++, doc.getEmployment());
		// Qualification
		ps.setString(i++, doc.getQualification());
		// EmploymentClassification_ID
		if(doc.getEmploymentClassification() != null) {
			ps.setLong(i++, doc.getEmploymentClassification().getSystemID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		// Coefficient
		if(doc.getCoefficient() != null) {
			ps.setDouble(i++, doc.getCoefficient());
		} else {
			ps.setNull(i++, Types.NUMERIC);
		}
		// AE_Level
		ps.setString(i++, doc.getLevel());
		// Echelon
		ps.setString(i++, doc.getEchelon());
		// Position
		ps.setString(i++, doc.getPosition());
		// AE_Index
		ps.setString(i++, doc.getIndex());
		// NightWork
		ps.setInt(i++, doc.isNightWork() ? 1 : 0) ;
		// SubType_ID
		if(doc.getSubType() != null) {
			ps.setLong(i++, doc.getSubType().getSystemID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		// AppointmentFrom
		ps.setDate(i++, AEPersistentUtil.getSQLDate(doc.getAppointmentFrom()));
		// AppointmentTo
		ps.setDate(i++, AEPersistentUtil.getSQLDate(doc.getAppointmentTo()));
		// Reason_ID
		if(doc.getReason() != null) {
			ps.setLong(i++, doc.getReason().getSystemID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		// AbsentEmployee_ID, AbsentEmployee_Name, AbsentEmployee_FirstName
		if(doc.getAbsentEmployee() != null) {
			ps.setLong(i++, doc.getAbsentEmployee().getDescriptor().getID());
			ps.setString(i++, doc.getAbsentEmployee().getDescriptor().getCode());
			ps.setString(i++, doc.getAbsentEmployee().getDescriptor().getName());
			ps.setString(i++, doc.getAbsentEmployee().getDescriptor().getDescription());
		} else {
			ps.setNull(i++, Types.BIGINT);
			ps.setNull(i++, Types.NVARCHAR);
			ps.setNull(i++, Types.NVARCHAR);
			ps.setNull(i++, Types.NVARCHAR);
		}
		// AbsentQualification
		ps.setString(i++, doc.getAbsentQualification());
		// AppointmentPeriod
		ps.setLong(i++, doc.getAppointmentPeriod());
		// FullTime
		ps.setInt(i++, doc.isFullTime() ? 1 : 0) ;
		// HoursPerWeek
		if(doc.getHoursPerWeek() != null) {
			ps.setDouble(i++, doc.getHoursPerWeek());
		} else {
			ps.setNull(i++, Types.NUMERIC);
		}
		// TrialPeriodQty
		if(doc.getTrialPeriodQty() != null) {
			ps.setDouble(i++, doc.getTrialPeriodQty());
		} else {
			ps.setNull(i++, Types.NUMERIC);
		}
		// TrialPeriodUOM_ID
		if(doc.getTrialPeriodUOM() != null) {
			ps.setDouble(i++, doc.getTrialPeriodUOM().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		// Renewable
		ps.setInt(i++, doc.isRenewable()? 1 : 0);
		// ContractAssistence_ID
		if(doc.getContractAssistence() != null) {
			ps.setLong(i++, doc.getContractAssistence().getSystemID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		// GrossAmount
		if(doc.getGrossAmount() != null) {
			ps.setDouble(i++, doc.getGrossAmount());
		} else {
			ps.setNull(i++, Types.NUMERIC);
		}
		// Currency_ID
		if(doc.getCurrency() != null) {
			ps.setDouble(i++, doc.getCurrency().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		// PerQty
		if(doc.getPerQty() != null) {
			ps.setDouble(i++, doc.getPerQty());
		} else {
			ps.setNull(i++, Types.NUMERIC);
		}
		// PerUOM_ID
		if(doc.getPerUOF() != null) {
			ps.setDouble(i++, doc.getPerUOF().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		// VerifiedBy_ID, VerifiedBy_Name
		if(doc.getVerifiedBy() != null) {
			ps.setLong(i++, doc.getVerifiedBy().getDescriptor().getID());
			ps.setString(i++, doc.getVerifiedBy().getDescriptor().getName());
		} else {
			ps.setNull(i++, Types.BIGINT);
			ps.setNull(i++, Types.NVARCHAR);
		}
		// BankAccount_IBAN
		ps.setString(i++, doc.getBankAccount().getIban());
		// BankAccount_BIC
		ps.setString(i++, AEStringUtil.EMPTY_STRING);
		// BankAccount_BankName
		ps.setString(i++, doc.getBankAccount().getName());//.getCompany().getDescriptor().getName());
		//AddSalaryPerc
		if(doc.getAddSalaryPerc() != null) {
			ps.setDouble(i++, doc.getAddSalaryPerc());
		} else {
			ps.setNull(i++, Types.NUMERIC);
		}
		//Modification_ID - can be updated only via special procedure
//		if(doc.getModificationDescr() != null) {
//			ps.setLong(i++, doc.getModificationDescr().getID());
//		}  else {
//			ps.setNull(i++, Types.BIGINT);
//		}
		//EffectiveDate
		if(doc.getEffectiveDate() != null) {
			ps.setDate(i++, AEPersistentUtil.getSQLDate(doc.getEffectiveDate()));
		}  else {
			ps.setNull(i++, Types.DATE);
		}
		
		//ReplacementPartiel
		ps.setBoolean(i++, doc.isReplacementPartiel());
		
		// CdiNonAnnualise
		ps.setBoolean(i++, doc.isCdiNonAnnualise());
		
		// DaysPerWeek
		if(doc.getDaysPerWeek() != null) {
			ps.setDouble(i++, doc.getDaysPerWeek());
		} else {
			ps.setNull(i++, Types.NUMERIC);
		}
		
		// AppointmentFixed
		ps.setBoolean(i++, doc.isAppointmentFixed());
		
		// AbsentEchelon
		ps.setString(i++, doc.getAbsentEchelon());
		
		// Mutuelle
		ps.setBoolean(i++, doc.isMutuelle());
		
		// AppointmentStartDateTime
		ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(doc.getAppointmentStartDateTime()));
		
		// AbsentEmployment
		ps.setString(i++, doc.getAbsentEmployment());
		
		// AbsentEmploymentClassification_ID
		if(doc.getAbsentEmploymentClassification() != null) {
			ps.setLong(i++, doc.getAbsentEmploymentClassification().getSystemID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		
		// Coefficient
		if(doc.getAbsentCoefficient() != null) {
			ps.setDouble(i++, doc.getAbsentCoefficient());
		} else {
			ps.setNull(i++, Types.NUMERIC);
		}
		
		// AbsentLevel
		ps.setString(i++, doc.getAbsentLevel());
		
		// AbsentPosition
		ps.setString(i++, doc.getAbsentPosition());

		// AbsetIndex
		ps.setString(i++, doc.getAbsentIndex());
		
		// VerifiedBySalutation_ID
		if(doc.getVerifiedBySalutation() != null) {
			ps.setLong(i++, doc.getVerifiedBySalutation().getTypeID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		
		// cddReplacementNpDurationUoM
		if(doc.getCddReplacementNpDurationUoM() != null) {
			ps.setLong(i++, doc.getCddReplacementNpDurationUoM().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		
		// return the current ps position 
		return i;
	}

	public void build(ContractDeTravail doc, ResultSet rs) throws SQLException, AEException {
		// build common attributes
		super.build(doc, rs);

		// IDDocument - already set
		
		// EmployeeID
		long employeeID = rs.getLong("EmployeeID");
		if(!rs.wasNull() || employeeID > 0) {
			EmployeeDAO emplDAO = DAOFactory.getInstance().getEmployeeDAO(getAEConnection());
			if(doc.isBuildLazzy()) {
				doc.setEmployee(emplDAO.loadLazzy(Employee.lazyDescriptor(employeeID)));
			} else {
				doc.setEmployee(emplDAO.load(Employee.lazyDescriptor(employeeID)));
			}
		}
		// WorkingDuringVacation
		doc.setWorkDuringVacation(rs.getInt("WorkingDuringVacation") != 0);
		// ProffesionDuration
		doc.setProfessionDuration(rs.getDouble("ProffesionDuration"));
		// FormerEmployee
		doc.setFormerEmployee(rs.getInt("FormerEmployee") != 0);
		// DateOfEntry
		doc.setDateOfEntry(rs.getTimestamp("DateOfEntry"));
		// DeclarationDUE
		doc.setDeclarationDUE(rs.getInt("DeclarationDUE") != 0);
		// DeclarationDUEDate
		doc.setDeclarationDUEDate(rs.getDate("DeclarationDUEDate"));
		// DeclarationDUENumber
		doc.setDeclarationDUENumber(rs.getString("DeclarationDUENumber"));
		// Employment
		doc.setEmployment(rs.getString("Employment"));
		// Qualification
		doc.setQualification(rs.getString("Qualification"));
		// EmploymentClassification_ID
		EmploymentClassification emplClass = null;
		long emplClassID = rs.getLong("EmploymentClassification_ID");
		if(!rs.wasNull() && emplClassID > 0) {
			emplClass = new EmploymentClassification(EmploymentClassification.System.valueOf(emplClassID));
		}
		doc.setEmploymentClassification(emplClass);
		// Coefficient
		doc.setCoefficient(rs.getDouble("Coefficient"));
		// AE_Level
		doc.setLevel(rs.getString("AE_Level"));
		// Echelon
		doc.setEchelon(rs.getString("Echelon"));
		// Position
		doc.setPosition(rs.getString("Position"));
		// AE_Index
		doc.setIndex(rs.getString("AE_Index"));
		// NightWork
		doc.setNightWork(rs.getInt("NightWork") != 0);
		// SubType_ID
		ContractDeTravailType contrType = null;
		long contrTypeID = rs.getLong("SubType_ID");
		if(!rs.wasNull() && contrTypeID > 0) {
			contrType = new ContractDeTravailType(ContractDeTravailType.System.valueOf(contrTypeID));
		}
		doc.setSubType(contrType);
		// AppointmentFrom
		doc.setAppointmentFrom(rs.getDate("AppointmentFrom"));
		// AppointmentTo
		doc.setAppointmentTo(rs.getDate("AppointmentTo"));
		// Reason_ID
		ContractDeTravailReason contrReason = null;
		long contrReasonID = rs.getLong("Reason_ID");
		if(!rs.wasNull() && contrReasonID > 0) {
			contrReason = new ContractDeTravailReason(ContractDeTravailReason.System.valueOf(contrReasonID));
		}
		doc.setReason(contrReason);
		// AbsentEmployee_ID, AbsentEmployee_Name
		AEDescriptive absentEmpl = null;
		long absentEmplID = rs.getLong("AbsentEmployee_ID");
		if(!rs.wasNull() && absentEmplID > 0) {
			EmployeeDAO emplDAO = DAOFactory.getInstance().getEmployeeDAO(getAEConnection());
			absentEmpl = emplDAO.loadDescriptive(absentEmplID);
		}
		if(absentEmpl == null) {
			absentEmpl = Employee.lazyDescriptor(absentEmplID);
			absentEmpl.getDescriptor().setCode(rs.getString("AbsentEmployee_Title"));
			absentEmpl.getDescriptor().setName(rs.getString("AbsentEmployee_Name"));
			absentEmpl.getDescriptor().setDescription(rs.getString("AbsentEmployee_FirstName"));
		}
		doc.setAbsentEmployee(absentEmpl);
		// AbsentQualification
		doc.setAbsentQualification(rs.getString("AbsentQualification"));
		// AppointmentPeriod
		doc.setAppointmentPeriod(rs.getLong("AppointmentPeriod"));
		// FullTime
		doc.setFullTime(rs.getInt("FullTime") != 0);
		// HoursPerWeek
		doc.setHoursPerWeek(rs.getDouble("HoursPerWeek"));
		// TrialPeriodQty
		doc.setTrialPeriodQty(rs.getDouble("TrialPeriodQty"));
		// TrialPeriodUOM_ID
		long trialPerUOMID = rs.getLong("TrialPeriodUOM_ID");
		if(!rs.wasNull() && trialPerUOMID > 0) {
			doc.setTrialPeriodUOM(UnitOfMeasurement.getByID(trialPerUOMID));
		} else {
			doc.setTrialPeriodUOM(null);
		}
		// Renewable
		doc.setRenewable(rs.getInt("Renewable") != 0);
		// ContractAssistence_ID
		ContractDeTravailAssistence contrAssistent = null;
		long contrAssistentID = rs.getLong("ContractAssistence_ID");
		if(!rs.wasNull() && contrAssistentID > 0) {
			contrAssistent = new ContractDeTravailAssistence(ContractDeTravailAssistence.System.valueOf(contrAssistentID));
		}
		doc.setContractAssistence(contrAssistent);
		// GrossAmount
		double grossAmount = rs.getDouble("GrossAmount");
		if(!rs.wasNull()) {
			doc.setGrossAmount(grossAmount);
		}
		// Currency_ID
		long currencyID = rs.getLong("Currency_ID");
		if(!rs.wasNull() && currencyID > 0) {
			doc.setCurrency(UnitOfMeasurement.getByID(currencyID));
		} else {
			doc.setCurrency(null);
		}
		// PerQty
		doc.setPerQty(rs.getDouble("PerQty"));
		// PerUOM_ID
		long perUOMID = rs.getLong("PerUOM_ID");
		if(!rs.wasNull() && perUOMID > 0) {
			doc.setPerUOF(UnitOfMeasurement.getByID(perUOMID));
		} else {
			doc.setPerUOF(null);
		}
		// VerifiedBy_ID, VerifiedBy_Name
		AEDescriptive verEmpl = null;
		long verEmplID = rs.getLong("VerifiedBy_ID");
		if(!rs.wasNull() && verEmplID > 0) {
			EmployeeDAO emplDAO = DAOFactory.getInstance().getEmployeeDAO(getAEConnection());
			verEmpl = emplDAO.loadDescriptive(verEmplID);
		}
		if(verEmpl == null) {
			verEmpl = Employee.lazyDescriptor(AEPersistentUtil.NEW_ID);
			verEmpl.getDescriptor().setName(rs.getString("VerifiedBy_Name"));
		}
		doc.setVerifiedBy(verEmpl);
		// BankAccount_IBAN
		doc.getBankAccount().setIban(rs.getString("BankAccount_IBAN"));
		// BankAccount_BIC
		// doc.getBankAccount().setBIC(rs.getString("BankAccount_BIC"));
		// BankAccount_BankName
		doc.getBankAccount().setName(rs.getString("BankAccount_BankName"));
		//AddSalaryPerc
		Double addSalaryPerc = rs.getDouble("AddSalaryPerc");
		if(!rs.wasNull()) {
			doc.setAddSalaryPerc(addSalaryPerc);
		}
		//Modification_ID and Modification_Date - can be updated only via special procedure
		long modificationId = rs.getLong("Modification_ID");
		if(!rs.wasNull()) {
			AEDocumentDescriptor modDescr = ContractDeTravail.lazyDescriptor(modificationId);
			modDescr.setDate(rs.getDate("Modification_Date"));
			doc.setModificationDescr(modDescr);
		}
		//EffectiveDate
		Date effectiveDate = rs.getDate("EffectiveDate");
		if(!rs.wasNull()) {
			doc.setEffectiveDate(effectiveDate);
		}
		//ReplacementPartiel
		doc.setReplacementPartiel(rs.getBoolean("ReplacementPartiel"));
		
		// CdiNonAnnualise
		doc.setCdiNonAnnualise(rs.getBoolean("CdiNonAnnualise"));
		
		// DaysPerWeek
		double daysPerWeek = rs.getDouble("DaysPerWeek");
		if(!rs.wasNull()) {
			doc.setDaysPerWeek(daysPerWeek);
		}
		
		// AppointmentFixed
		doc.setAppointmentFixed(rs.getBoolean("AppointmentFixed"));
		
		// AbsentEchelon
		doc.setAbsentEchelon(rs.getString("AbsentEchelon"));
		
		// Mutuelle
		doc.setMutuelle(rs.getBoolean("Mutuelle"));
		
		// AppointmentStartDateTime
		doc.setAppointmentStartDateTime(rs.getTimestamp("AppointmentStartDateTime"));
		
		// AbsentEmployment
		doc.setAbsentEmployment(rs.getString("AbsentEmployment"));
		
		// AbsentEmploymentClassification_ID
		EmploymentClassification absentEmplClass = null;
		long absentEmplClassID = rs.getLong("AbsentEmploymentClassification_ID");
		if(!rs.wasNull() && absentEmplClassID > 0) {
			absentEmplClass = new EmploymentClassification(
					EmploymentClassification.System.valueOf(absentEmplClassID));
		}
		doc.setAbsentEmploymentClassification(absentEmplClass);
		
		// AbsentCoefficient
		doc.setAbsentCoefficient(rs.getDouble("AbsentCoefficient"));
		
		// AbsentLevel
		doc.setAbsentLevel(rs.getString("AbsentLevel"));
		
		// AbsentPosition
		doc.setAbsentPosition(rs.getString("AbsentPosition"));
		
		// AbsentIndex
		doc.setAbsentIndex(rs.getString("AbsentIndex"));
		
		// VerifiedBySalutation_ID
		long verifiedBySalutationId = rs.getLong("VerifiedBySalutation_ID");
		if(!rs.wasNull()) {
			doc.setVerifiedBySalutation(Person.SalutationType.valueOf(verifiedBySalutationId));
		}
		
		// cddReplacementNpDurationUoM
		long cddReplacementNpDurationUoMId = rs.getLong("cddReplacementNpDurationUoM");
		if(!rs.wasNull()) {
			doc.setCddReplacementNpDurationUoM(UnitOfMeasurement.getByID(cddReplacementNpDurationUoMId));
		}
		
		// set this record in view state
		doc.setView();
	}
	
	@Override
	public void update(AEDocument doc) throws AEException {
		assert(doc != null);
		assert(doc.isPersistent());
		assert doc instanceof ContractDeTravail : "doc instanceof ContractDeTravail failed";
		ContractDeTravail cdtDoc = (ContractDeTravail) doc;
		PreparedStatement ps = null;
		try {
			// update document table
			super.update(cdtDoc);
			
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			int i = build(cdtDoc, ps, 1);
			ps.setLong(i++, doc.getID());
			
			// execute
			ps.executeUpdate();

			// set view state
			doc.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void update(AEDescriptor doc, AEDocumentDescriptor modification) throws AEException {
		assert(doc != null);
		assert(doc.isPersistent());
		assert(modification != null);
		assert(modification.isPersistent());
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQLModification);

			// build statement
			ps.setLong(1, modification.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(modification.getDate()));
			ps.setLong(3, doc.getID());
			
			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public Long loadEmployeeId(long id) throws AEException {
		Long employeeId = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectEmployeeIdSQL);
			ps.setLong(1, id);
			rs = ps.executeQuery();
			if(rs.next()) {
				long pId = rs.getLong("EmployeeID");
				if(!rs.wasNull()) {
					employeeId = pId;
				}
			}
			return employeeId;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void validateExistingContract(Employee emplDescr) throws AEException, AEWarning {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			final String sql = 
				"select empl.id from ContractDeTravail contr inner join Employee empl "
				+ " on contr.EmployeeID = empl.ID where " // there is contract for this employee
				+ " empl.COMPANY_ID = ? " // for our company
				+ " and empl.person_id = ? " // for this person
				+ " and empl.DateRelease is null " // there is no date release (actual contract)
				+ " and empl.id != ?"; // it is not current contract (in case of editing)
			ps = getAEConnection().prepareStatement(sql);
			ps.setLong(1, emplDescr.getCompany() != null ? emplDescr.getCompany().getDescriptor().getID() : -1);
			ps.setLong(2, emplDescr.getPerson() != null ? emplDescr.getPerson().getDescriptor().getID() : -1);
			ps.setLong(3, emplDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				throw AEError.System.SOCIAL_WARNING_ACTIVE_CONTRACT_EXIST.toWarning();
			}
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AETimePeriod getLastCDDPeriod(ContractDeTravail contract) throws AEException {
		AETimePeriod timePeriod = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			final String sql = 
				"select TOP 1 empl.* from "
				+ " Document doc inner join ContractDeTravail contr on doc.ID = contr.IDDocument "
				+ " inner join Employee empl on contr.EmployeeID = empl.ID "
				+ " inner join Person person on empl.PERSON_ID = person.PartyID "
				+ " where " 
				+ " doc.OWNER_ID = ? and doc.TYPE_ID = " + AEDocumentType.System.ContractDeTravail.getSystemID() 
			  	+ " and (contr.SubType_ID = " + ContractDeTravailType.System.TEMPORARY_SEASONAL.getSystemID() 
			  	+ " or contr.SubType_ID = " + ContractDeTravailType.System.TEMPORARY_PEAK.getSystemID() + " ) "  
				+ " and person.PartyID = ? "
				+ " and empl.DateRelease is not null " 
				+ " order by empl.DateRelease desc"; 
			ps = getAEConnection().prepareStatement(sql);
			ps.setLong(1, contract.getCompany() != null ? contract.getCompany().getDescriptor().getID() : -1);
			ps.setLong(2, contract.getEmployee().getPerson() != null ? contract.getEmployee().getPerson().getDescriptor().getID() : -1);
			rs = ps.executeQuery();
			if(rs.next()) {
				Date dateFrom = rs.getDate("DateEntry");
				Date dateTo = rs.getDate("DateRelease");
				if(dateFrom != null && dateTo != null && !dateTo.before(dateFrom)) {
					timePeriod = new AETimePeriod(dateFrom, dateTo);
				}
			}
			return timePeriod;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	@Override
	public long countAll(AEDescriptor ownerDescr, AEDocumentType docType) throws AEException {
		long count = 0;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCountAll);
			ps.setLong(1, ownerDescr.getID());
			ps.setLong(2, docType.getSystemID());
			rs = ps.executeQuery();
			if(rs.next()) {
				count = rs.getLong("count");
			}
			return count;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	@Override
	public long countValidated(AEDescriptor ownerDescr, AEDocumentType docType) throws AEException {
		long count = 0;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCountValidated);
			ps.setLong(1, ownerDescr.getID());
			ps.setLong(2, docType.getSystemID());
			rs = ps.executeQuery();
			if(rs.next()) {
				count = rs.getLong("count");
			}
			return count;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
}
