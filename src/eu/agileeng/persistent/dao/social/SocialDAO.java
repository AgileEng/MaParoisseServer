package eu.agileeng.persistent.dao.social;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.contact.Employee;
import eu.agileeng.domain.contact.EmployeeList;
import eu.agileeng.domain.document.AEDocument;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.AEDocumentsList;
import eu.agileeng.domain.document.AEPrintTemplatesList;
import eu.agileeng.domain.document.social.AESocialDocument;
import eu.agileeng.domain.document.social.SocialDayType;
import eu.agileeng.domain.document.social.SocialPeriodType;
import eu.agileeng.domain.document.social.SocialPrintTemplate;
import eu.agileeng.domain.document.social.SocialTemplatesSet;
import eu.agileeng.domain.document.social.SocialTimeSheet;
import eu.agileeng.domain.document.social.SocialTimeSheetEntry;
import eu.agileeng.domain.document.social.contractdetravail.ContractDeTravail;
import eu.agileeng.domain.document.social.contractdetravail.ContractDeTravailType;
import eu.agileeng.domain.social.SalaryGrid;
import eu.agileeng.domain.social.SalaryGridItem;
import eu.agileeng.domain.social.SalaryGridItemsList;
import eu.agileeng.domain.social.SalaryGridsList;
import eu.agileeng.domain.social.SocialInfo;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;

public class SocialDAO extends AbstractDAO {
	
	private static final String selectSQLDocsToTransfer = 
		"	select doc.ID, doc.TYPE_ID, doc.OWNER_ID from Document doc "
		+ "    inner join Party p on doc.OWNER_ID = p.ID "
		+ "	   inner join Social_CompanyInfo socInfo on p.ID = socInfo.OWNER_ID "
		+ " where "
		+ "    socInfo.emplIdSeed is not null "
		+ "    and doc.TYPE_ID in (1010, 1015, 1020, 1030, 1050) "
		+ "    and (doc.ftp_posted is null or doc.ftp_posted = 0) ";
	
	private static final String updateSQLFtpPosted = 
		"update Document set FTP_POSTED = ?, FTP_FILE = ? where ID = ?";
	
	/////////////////////////////////////////////////////
	private static final String insertSQL = 
		"insert into Social_SalaryGrid (ACC_HOUSE, OWNER_ID, CODE, NAME, DESCRIPTION, YEAR, "
		+ " ACTIVE, RATE_MAX_EXTRA_HOURS) values "
		+ " (?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String updateSQL = 
		"update Social_SalaryGrid set ACC_HOUSE = ?, OWNER_ID = ?, CODE = ?, NAME = ?, DESCRIPTION = ?, "
		+ " YEAR = ?, ACTIVE = ?, RATE_MAX_EXTRA_HOURS = ?"
		+ " where id = ?";
	
	private static final String selectSQLToAccHouse = 
		"select * from Social_SalaryGrid where ACC_HOUSE = ?";
	
	private static final String selectSQL = 
		"select * from Social_SalaryGrid where ID = ?";
	
	/////////////////////////////////////////////////////
	private static final String insertSQLItem = 
		"insert into Social_SalaryGridItem (SalaryGrid_ID, Code, Name, Description, "
		+ "Niveau, Echelon, Coefficient, PointValue, Salary)"
		+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String updateSQLItem = 
		"update Social_SalaryGridItem set SalaryGrid_ID = ?, Code = ?, Name = ?, Description = ?, "
		+ "Niveau = ?, Echelon = ?, Coefficient = ?, PointValue = ?, Salary = ? "
		+ " where id = ?";
	
	private static final String selectSQLItemsToSG = 
		"select * from Social_SalaryGridItem where SalaryGrid_ID = ?";
	
	private static final String deleteSQLItem = 
		"delete from Social_SalaryGridItem where id = ?";

	////////////
	private static final String insertSQLSocialInfo = 
		"insert into Social_CompanyInfo (OWNER_ID, SALARY_GRID_ID, NightWorkBeginId, NightWorkEnd, StartDate, "
		+ "LogoUrl, GenerateContract, Societe, SocieteTerm, Password, TEMPLATES_SET_ID, URSSAF_NUMBER, URSSAF_PRETEXT, "
		+ "ActivitePrincipale, Cctv, URSSAF_TEXT, ActivitePrincipaleText, emplIdSeed) values "
		+ "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String updateSQLSocialInfo = 
		"update Social_CompanyInfo set OWNER_ID = ?, SALARY_GRID_ID = ?, NightWorkBeginId = ?, NightWorkEnd = ?, StartDate = ?, "
		+ " LogoUrl = ?, GenerateContract = ?, Societe = ?, SocieteTerm = ?, Password = ?, TEMPLATES_SET_ID = ?, "
		+ " URSSAF_NUMBER = ?, URSSAF_PRETEXT = ?, ActivitePrincipale = ?, Cctv = ?, URSSAF_TEXT = ?, ActivitePrincipaleText = ?, "
		+ " emplIdSeed = ? "
		+ " where ID = ?";
	
	private static final String selectSQLEmplIdSeed = 
		"select emplIdSeed from Social_CompanyInfo where OWNER_ID = ?";
	
	private static final String updateSQLScheduleStartDate = 
		"update Social_CompanyInfo set ScheduleStartDate = ? where OWNER_ID = ?";
	
	private static final String selectSQLSocialInfo = 
		"select * from Social_CompanyInfo where OWNER_ID = ?";
	
	////////////////////
	
	private static final String findSalaryItem = 
		"select * from Social_SalaryGridItem item inner join Social_SalaryGrid grid "
		+ " on item.SalaryGrid_ID = grid.ID "
		+ " where grid.id = ?";
	
	
	/////////////////////////////////////////////////////
	private static final String insertSQLTimeSheetEntry = 
		"insert into Social_TimeSheet (PeriodType, DayType, Code, Description, Employee_ID, Date, "
		+ " WeekNumber, DayOfWeek, FromTime_1, ToTime_1, FromTime_2, ToTime_2, Modifiable, "
		+ " FromTimeActual_1, ToTimeActual_1, FromTimeActual_2, ToTimeActual_2, "
		+ " FromTimeActual_3, ToTimeActual_3, ActualType_1, ActualType_2, ActualType_3, "
		+ " workingHours, workingHoursActual, Type_1, Type_2, ActualValidated) "
		+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String updateSQLTimeSheetEntry = 
		"update Social_TimeSheet set PeriodType = ?, DayType = ?, Code = ?, Description = ?, Employee_ID = ?, Date = ?, "
		+ " WeekNumber = ?, DayOfWeek = ?, FromTime_1 = ?, ToTime_1 = ?, FromTime_2 = ?, ToTime_2 = ?, Modifiable = ?, "
		+ " FromTimeActual_1 = ?, ToTimeActual_1= ?, FromTimeActual_2 = ?, ToTimeActual_2 = ?, "
		+ " FromTimeActual_3 = ?, ToTimeActual_3 = ?, ActualType_1 = ?, ActualType_2 = ?, ActualType_3 = ?, "
		+ " workingHours = ?, workingHoursActual = ?, Type_1 = ?, Type_2 = ?, ActualValidated = ? "
		+ " where id = ?";
	
	private static final String updateSQLTimeSheetEntryActual = "update Social_TimeSheet set FromTimeActual_1 = ?, ToTimeActual_1 = ?,"
			+ " ActualType_1 = ?, FromTimeActual_2 = ?, ToTimeActual_2 = ?, ActualType_2 = ?, FromTimeActual_3 = ?,"
			+ " ToTimeActual_3 = ?, ActualType_3 = ?, ActualValidated = ?, Description = ? where ID = ?";
	
	private static final String updateSQLTimeSheetEntryPlanByUniqueIndex = 
			"update Social_TimeSheet set FromTime_1 = ?, ToTime_1 = ?, FromTime_2 = ?, ToTime_2 = ?, workingHours = ?, Type_1 = ?, Type_2 = ? "
			+ " where Employee_ID = ? and PeriodType = ? and Date = ? and DayOfWeek = ?";
	
	private static final String deleteSQLTimeSheetEntry = 
		"delete from Social_TimeSheet where id = ?";
	
	private static final String selectSQLTimeSheet = 
		"select * from Social_TimeSheet where Employee_ID = ? and PeriodType = ? order by Date ASC";
	
	private static final String selectSQLTimeSheetPlan = 
		"select * from Social_TimeSheet where Employee_ID = ? and PeriodType = ? and Date >= ? order by Date ASC";
	
	private static final String selectSQLTimeSheetPlanByPeriod = 
			"select * from Social_TimeSheet where Employee_ID = ? and PeriodType = ? and Date >= ? and Date <= ? order by Date ASC";
	
	private static final String selectSQLTimeSheetsPlanForDate = 
			"select * from Social_TimeSheet where Employee_ID = ? and PeriodType = ? and Date = ?";
	
	private static final String selectSQLTimeSheetsTemplateForDate = 
			"select * from Social_TimeSheet where Employee_ID = ? and PeriodType = ? and WeekNumber = ? and DayOfWeek = ?";
	
	private static final String selectSQLTimeSheetEntriesForPeriod = 
			"select * from Social_TimeSheet where PeriodType = ? and Employee_ID = ? and Date >= ? and Date <= ?";
	
	private static final String selectSQLTimeSheetsForPeriodType = 
			"select * from Social_TimeSheet where Employee_ID = ? and PeriodType = ?";
	
	////////////////////////////////////////////////////
	
	private static final String selectSQLTimeSheetActual = 
		"select ts.*, empl.NAME as empl_name "
		+ " from Social_TimeSheet ts inner join Employee empl "
		+ " on ts.Employee_ID = empl.ID where "
		+ " empl.COMPANY_ID = ? "
		+ " and DayOfWeek >= 1 and DayOfWeek <= 7 "
		+ " and ts.PeriodType = ? and ts.date >= ? and ts.date <= ?";
	
	////////////////////////////////////////////////////
	private static final String selectSQLPrintTemplateContractCDI = 
		"select * from Social_PrintTemplates "
		+ " where DOC_TYPE_ID = ? and doc_subtype_id = ? and s_fulltime = ? and templates_set_id = ?";
	
	private static final String selectSQLPrintTemplateContractCDD = 
		"select * from Social_PrintTemplates "
		+ " where DOC_TYPE_ID = ? "
		+ " and (doc_subtype_id = " + Long.toString(ContractDeTravailType.System.TEMPORARY.getSystemID()) + " or doc_subtype_id = ?) "
		+ " and s_fulltime = ? and templates_set_id = ?";
	
	private static final String selectSQLPrintTemplateById = 
		"select * from Social_PrintTemplates where id = ?";
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */ 
	public SocialDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	public void insert(SalaryGrid sg) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);
			build(sg, ps, 1);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				sg.setID(rs.getLong(1));
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set the View state
			sg.setView();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void insert(SocialInfo si) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLSocialInfo);
			build(si, ps, 1);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				si.setID(rs.getLong(1));
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set the View state
			si.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void update(SalaryGrid sg) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(updateSQL);
			int i = build(sg, ps, 1);
			ps.setLong(i++, sg.getID());
			
			// execute
			ps.executeUpdate();
			
			// set the View state
			sg.setView();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void update(SocialInfo si) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(updateSQLSocialInfo);
			int i = build(si, ps, 1);
			ps.setLong(i++, si.getID());
			
			// execute
			ps.executeUpdate();
			
			// set the View state
			si.setView();
		} catch (SQLException e) {
			throw new AEException();
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void updateFtpPosted(List<AEDescriptor> ftpPosted, boolean isPosted, String fileName) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement
			ps = getAEConnection().prepareStatement(updateSQLFtpPosted);
			for (AEDescriptor docDescr : ftpPosted) {
				// bind
				ps.setInt(1, isPosted ? 1 : 0);
				if(!AEStringUtil.isEmpty(fileName)) {
					ps.setString(2, fileName);
				} else {
					ps.setNull(2, java.sql.Types.NVARCHAR);
				}
				ps.setLong(3, docDescr.getID());
				
				// execute
				ps.executeUpdate();
			}
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	protected int build(SalaryGrid sg, PreparedStatement ps, int i) throws SQLException, AEException {
		// ACC_HOUSE
		if(sg.getAccHouse() != null) {
			ps.setString(i++, sg.getAccHouse().getDescriptor().getName());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		// OWNER_ID
		if(sg.getCompany() != null) {
			ps.setLong(i++, sg.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		// CODE
		ps.setString(i++, sg.getCode());
		// NAME
		ps.setString(i++, sg.getName());
		// DESCRIPTION
		ps.setString(i++, sg.getDescription());
		// YEAR
		ps.setLong(i++, sg.getYear());
		// ACTIVE
		ps.setBoolean(i++, sg.isActive());
		// RATE_MAX_EXTRA_HOURS
		if(sg.getRateMaxExtraHours() != null) {
			ps.setDouble(i++, sg.getRateMaxExtraHours());
		} else {
			ps.setNull(i++, java.sql.Types.NUMERIC);
		}
		
		// return the current ps position 
		return i;
	}
	
	protected void build(SalaryGridItem sgItem, ResultSet rs) throws SQLException, AEException {
		super.build(sgItem, rs);
		
		// SalaryGrid_ID
		long sgID = rs.getLong("SalaryGrid_ID");
		if(!rs.wasNull()) {
			sgItem.setSalaryGrid(SalaryGrid.lazyDescriptor(sgID));
		}
		
		// Niveau
		sgItem.setNiveau(rs.getString("Niveau"));
		
		// Echelon
		String _echelon = rs.getString("Echelon");
		if(!rs.wasNull()) {
			sgItem.setEchelon(_echelon);
		} else {
			sgItem.setEchelon(null);
		}
		
		// Coefficient
		double coeff = rs.getDouble("Coefficient");
		if(!rs.wasNull()) {
			sgItem.setCoefficient(coeff);
		}
		
		// PointValue
		double pointValue = rs.getDouble("PointValue");
		if(!rs.wasNull()) {
			sgItem.setPointValue(pointValue);
		}
		
		// Salary
		sgItem.setSalary(rs.getDouble("Salary"));
	}
	
	public SalaryGridItemsList loadSGItems(AEDescriptor toSGDescr) throws AEException {
		SalaryGridItemsList sgItemsList = new SalaryGridItemsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLItemsToSG);
			ps.setLong(1, toSGDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				SalaryGridItem sgItem = new SalaryGridItem();
				build(sgItem, rs);
				sgItem.setView();
				sgItemsList.add(sgItem);
			}
		    return sgItemsList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	/////////////////////////////////////////////
	protected int build(SalaryGridItem sgItem, PreparedStatement ps, int i) throws SQLException, AEException {
		// SalaryGrid_ID
		if(sgItem.getSalaryGrid() != null) {
			ps.setLong(i++, sgItem.getSalaryGrid().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// Code
		ps.setString(i++, sgItem.getCode());
		
		// Name
		ps.setString(i++, sgItem.getName());
		
		// Description
		ps.setString(i++, sgItem.getDescription());
		
		// Niveau
		ps.setString(i++, sgItem.getNiveau());
		
		// Echelon
		if(!AEStringUtil.isEmpty(sgItem.getEchelon())) {
			ps.setString(i++, sgItem.getEchelon());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// Coefficient
		if(sgItem.getCoefficient() != null) {
			ps.setDouble(i++, sgItem.getCoefficient());
		} else {
			ps.setNull(i++, java.sql.Types.NUMERIC);
		}
		
		// PointValue
		if(sgItem.getPointValue() != null) {
			ps.setDouble(i++, sgItem.getPointValue());
		} else {
			ps.setNull(i++, java.sql.Types.NUMERIC);
		}
		
		// Salary
		if(sgItem.getSalary() != null) {
			ps.setDouble(i++, sgItem.getSalary());
		} else {
			ps.setNull(i++, java.sql.Types.NUMERIC);
		}
		
		// return the current ps position 
		return i;
	}
	
	protected void build(SalaryGrid sg, ResultSet rs) throws SQLException, AEException {
		super.build(sg, rs);
		
		// ACC_HOUSE
		String accHouse = rs.getString("ACC_HOUSE");
		if(!rs.wasNull()) {
			sg.setAccHouse(SalaryGrid.lazyAccHouseDescriptor(accHouse));
		}
		
		// YEAR
		sg.setYear(rs.getInt("YEAR"));
		
		//ACTIVE
		sg.setActive(rs.getBoolean("ACTIVE"));
		
		// RATE_MAX_EXTRA_HOURS
		double rate = rs.getDouble("RATE_MAX_EXTRA_HOURS");
		if(!rs.wasNull()) {
			sg.setRateMaxExtraHours(rate);
		}
	}
	
	public void insert(SalaryGridItem sgItem) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLItem);
			build(sgItem, ps, 1);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				sgItem.setID(rs.getLong(1));
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set the View state
			sgItem.setView();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	protected void build(SocialInfo si, ResultSet rs) throws SQLException, AEException {
		super.build(si, rs);
		
		// SalaryGrid
		long salaryGridId = rs.getLong("SALARY_GRID_ID");
		if(!rs.wasNull()) {
			si.setSalaryGrid(SalaryGrid.lazyDescriptor(salaryGridId));
		}
		
		// NightWorkBeginId
		si.setNightWorkBegin(rs.getTimestamp("NightWorkBeginId"));
		
		// NightWorkEnd
		si.setNightWorkEnd(rs.getTimestamp("NightWorkEnd"));
		
		// StartDate
		si.setStartDate(rs.getDate("StartDate"));
		
		// ScheduleStartDate
		si.setScheduleStartDate(rs.getDate("ScheduleStartDate"));
		
		// LogoUrl
		si.setLogoUrl(rs.getString("LogoUrl"));
		
		// GenerateContract
		si.setGenerateContract(rs.getBoolean("GenerateContract"));
		
		// Societe
		si.setSociete(rs.getString("Societe"));
		
		// SocieteTerm
		si.setSocieteTerm(rs.getString("SocieteTerm"));
		
		// Password
		si.setPassword(rs.getString("Password"));
		
		// TEMPLATES_SET_ID
		SocialTemplatesSet tSet = SocialTemplatesSet.valueOf((int) rs.getLong("TEMPLATES_SET_ID"));
		if(SocialTemplatesSet.isDefined(tSet)) {
			si.setTemplatesSet(tSet);
		}
		
		// URSSAF_NUMBER
		si.setUrssafNumber(rs.getString("URSSAF_NUMBER"));
		
		// URSSAF_PRETEXT
		si.setUrssafPretext(rs.getString("URSSAF_PRETEXT"));
		
		// ActivitePrincipale
		si.setActivitePrincipale(rs.getString("ActivitePrincipale"));
		
		// Cctv
		si.setCctv(rs.getBoolean("Cctv"));
		
		// URSSAF_TEXT
		si.setUrssafText(rs.getString("URSSAF_TEXT"));
		
		// ActivitePrincipaleText
		si.setActivitePrincipaleText(rs.getString("ActivitePrincipaleText"));
		
		// emplIdSeed
		long id = rs.getLong("emplIdSeed");
		if(!rs.wasNull()) {
			si.setEmplIdSeed(id);
		}
	}
	
	protected int build(SocialInfo si, PreparedStatement ps, int i) throws SQLException, AEException {
		// OWNER_ID
		if(si.getCompany() != null) {
			ps.setLong(i++, si.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// SALARY_GRID_ID
		if(si.getSalaryGrid() != null) {
			ps.setLong(i++, si.getSalaryGrid().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// NightWorkBeginId
		if(si.getNightWorkBegin() != null) {
			ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(si.getNightWorkBegin()));
		} else {
			ps.setNull(i++, java.sql.Types.TIMESTAMP);
		}
		
		// NightWorkEnd
		if(si.getNightWorkEnd() != null) {
			ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(si.getNightWorkEnd()));
		} else {
			ps.setNull(i++, java.sql.Types.TIMESTAMP);
		}
		
		// StartDate
		if(si.getStartDate() != null) {
			ps.setDate(i++, AEPersistentUtil.getSQLDate(si.getStartDate()));
		} else {
			ps.setNull(i++, java.sql.Types.TIMESTAMP);
		}
		
		// LogoUrl
		ps.setString(i++, si.getLogoUrl());
		
		// GenerateContract
		ps.setBoolean(i++, si.isGenerateContract());
		
		// Societe
		ps.setString(i++, si.getSociete());
		
		// SocieteTerm
		ps.setString(i++, si.getSocieteTerm());
		
		// Password
		ps.setString(i++, si.getPassword());
		
		// TEMPLATES_SET_ID
		if(SocialTemplatesSet.isDefined(si.getTemplatesSet())) {
			ps.setLong(i++, si.getTemplatesSet().getId());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// URSSAF_NUMBER
		ps.setString(i++, si.getUrssafNumber());
		
		// URSSAF_PRETEXT
		ps.setString(i++, si.getUrssafPretext());
		
		// ActivitePrincipale
		ps.setString(i++, si.getActivitePrincipale());
		
		// Cctv
		ps.setBoolean(i++, si.isCctv());
		
		// URSSAF_TEXT
		ps.setString(i++, si.getUrssafText());
		
		// ActivitePrincipaleText
		ps.setString(i++, si.getActivitePrincipaleText());
		
		// emplIdSeed
		if(si.getEmplIdSeed() != null) {
			ps.setLong(i++, si.getEmplIdSeed());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// return the current ps position 
		return i;
	}
	
	public void update(SalaryGridItem sgItem) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(updateSQLItem);
			int i = build(sgItem, ps, 1);
			ps.setLong(i++, sgItem.getID());
			
			// execute
			ps.executeUpdate();
			
			// set the View state
			sgItem.setView();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public SalaryGridsList loadAllSalaryGridsLazy(AEDescriptor accHouseDescr) throws AEException {
		SalaryGridsList sgList = new SalaryGridsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLToAccHouse);
			ps.setString(1, accHouseDescr.getName());
			rs = ps.executeQuery();
			while(rs.next()) {
				SalaryGrid sg = new SalaryGrid();
				
				// build the instance
				build(sg, rs);
				
				// set in view state
				sg.setView();
				
				// add to the list
				sgList.add(sg);
			}
		    return sgList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public SalaryGrid loadSalaryGrid(AEDescriptor sgDescr) throws AEException {
		SalaryGrid sg = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, sgDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				sg = new SalaryGrid();
				
				// build the instance
				build(sg, rs);
				
				// set in view state
				sg.setView();
			}
		    return sg;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public SocialInfo loadSocialInfo(AEDescriptor ownerDescr) throws AEException {
		SocialInfo si = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLSocialInfo);
			ps.setLong(1, ownerDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				si = new SocialInfo();
				
				// build the instance
				build(si, rs);
				
				// set in view state
				si.setView();
			}
		    return si;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public Long loadEmplIdentSeed(AEDescriptor ownerDescr) throws AEException {
		Long emplIdentSeed = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLEmplIdSeed);
			ps.setLong(1, ownerDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				long l = rs.getLong("emplIdSeed");
				if(!rs.wasNull()) {
					emplIdentSeed = l;
				}
			}
		    return emplIdentSeed;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void delete(long id) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQLItem);

			// build statement
			ps.setLong(1, id);
			
			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public SalaryGridItem findSgItem(long salaryGridId, String echelon, Double coefficient) throws AEException {
		SalaryGridItem sgItem = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = findSalaryItem;
			if(echelon != null) {
				 sql += " and item.Echelon = '" + echelon + "' ";
			} else {
				sql += " and item.Echelon IS NULL ";
			}
			if(coefficient != null) {
				sql += " and item.Coefficient = " + coefficient + " ";
			} else {
				sql += " and item.Coefficient IS NULL ";
			}
			ps = getAEConnection().prepareStatement(sql);
			ps.setLong(1, salaryGridId);
			rs = ps.executeQuery();
			int count = 0;
			while(rs.next()) {
				sgItem = new SalaryGridItem();
				build(sgItem, rs);
				sgItem.setView();
				count++;
			}
			return count == 1 ? sgItem : null;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void insert(SocialTimeSheetEntry tsEntry) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLTimeSheetEntry);
			build(tsEntry, ps, 1);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				tsEntry.setID(rs.getLong(1));
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set the View state
			tsEntry.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void insertLazzy(List<SocialTimeSheetEntry> entries) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement
			ps = getAEConnection().prepareStatement(insertSQLTimeSheetEntry);
			
			// insert
			for (Iterator<SocialTimeSheetEntry> iterator = entries.iterator(); iterator.hasNext();) {
				SocialTimeSheetEntry tsEntry = (SocialTimeSheetEntry) iterator.next();
				
				build(tsEntry, ps, 1);
				
				// execute
				ps.executeUpdate();
			}
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void insertNotExisted(List<SocialTimeSheetEntry> entries) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement
			ps = getAEConnection().prepareStatement(insertSQLTimeSheetEntry);
			
			// insert
			for (Iterator<SocialTimeSheetEntry> iterator = entries.iterator(); iterator.hasNext();) {
				SocialTimeSheetEntry tsEntry = (SocialTimeSheetEntry) iterator.next();
				
				build(tsEntry, ps, 1);
				
				// execute
				try {
					ps.executeUpdate();
				} catch (SQLException e) {
					// nothing to do
				}
			}
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	/**
	 * Insert or update if SocialTimeSheetEntry exists.
	 * The unique index: (employeeId, periodType, Date)
	 * 
	 * @param entries
	 * @throws AEException
	 */
	public void insertOrUpdatePlanIfExists(List<SocialTimeSheetEntry> entries) throws AEException {
		PreparedStatement psInsert = null;
		PreparedStatement psUpdate = null;
	    ResultSet rs = null;
		try {
			// prepare statement
			psInsert = getAEConnection().prepareStatement(insertSQLTimeSheetEntry);
			psUpdate = getAEConnection().prepareStatement(updateSQLTimeSheetEntryPlanByUniqueIndex);
			
			// insert
			for (Iterator<SocialTimeSheetEntry> iterator = entries.iterator(); iterator.hasNext();) {
				SocialTimeSheetEntry tsEntry = (SocialTimeSheetEntry) iterator.next();
				
				try {
					// to insert
					build(tsEntry, psInsert, 1);
					psInsert.executeUpdate();
					
					rs = psInsert.getGeneratedKeys();
					if (rs.next()) {
						tsEntry.setID(rs.getLong(1));
					} else {
						throw new AEException(getClass().getName() + "::insert: No keys were generated");
					}
					
					tsEntry.setView();
				} catch (SQLException e) {
					// else update by unique index
					int i = buildForUpdatePlan(tsEntry, psUpdate, 1);
					psUpdate.setLong(i++, tsEntry.getEmployee().getDescriptor().getID());
					psUpdate.setLong(i++, tsEntry.getPeriodType().getId());
					psUpdate.setDate(i++, AEPersistentUtil.getSQLDate(tsEntry.getDate()));
					psUpdate.setLong(i++, tsEntry.getDayOfWeek());
					
					psUpdate.executeUpdate();
					
					tsEntry.setView();
				}
			}
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(psInsert);
			AEConnection.close(psUpdate);
			AEConnection.close(rs);
			close();
		}
	}
	
	protected int build(SocialTimeSheetEntry tsEntry, PreparedStatement ps, int i) throws SQLException, AEException {
		//PeriodType
		if(tsEntry.getPeriodType() != null) {
			ps.setInt(i++, (int)tsEntry.getPeriodType().getId());
		} else {
			ps.setNull(i++, java.sql.Types.INTEGER);
		}
		//DayType
		if(tsEntry.getDayType() != null) {
			ps.setInt(i++, (int)tsEntry.getDayType().getId());
		} else {
			ps.setNull(i++, java.sql.Types.INTEGER);
		}
		//Code
		ps.setString(i++, tsEntry.getCode());
		//Description
		ps.setString(i++, tsEntry.getDescription());
		//Employee_ID
		if(tsEntry.getEmployee() != null) {
			ps.setLong(i++, tsEntry.getEmployee().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		//Date
		if(tsEntry.getDate() != null) {
			ps.setDate(i++, AEPersistentUtil.getSQLDate(tsEntry.getDate()));
		} else {
			ps.setNull(i++, java.sql.Types.TIMESTAMP);
		}
		//WeekNumber
		ps.setInt(i++, tsEntry.getWeekNumber());
		//DayOfWeek
		ps.setInt(i++, tsEntry.getDayOfWeek());
		//FromTime_1
		if(tsEntry.getFromTime_1() != null) {
			ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(tsEntry.getFromTime_1()));
		} else {
			ps.setNull(i++, java.sql.Types.TIMESTAMP);
		}
		//ToTime_1
		if(tsEntry.getToTime_1() != null) {
			ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(tsEntry.getToTime_1()));
		} else {
			ps.setNull(i++, java.sql.Types.TIMESTAMP);
		}
		//FromTime_2
		if(tsEntry.getFromTime_2() != null) {
			ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(tsEntry.getFromTime_2()));
		} else {
			ps.setNull(i++, java.sql.Types.TIMESTAMP);
		}
		//ToTime_2
		if(tsEntry.getToTime_2() != null) {
			ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(tsEntry.getToTime_2()));
		} else {
			ps.setNull(i++, java.sql.Types.TIMESTAMP);
		}
		//Modifiable
		ps.setBoolean(i++, tsEntry.isModifiable());

		//FromTimeActual_1
		if(tsEntry.getFromTimeActual_1() != null) {
			ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(tsEntry.getFromTimeActual_1()));
		} else {
			ps.setNull(i++, java.sql.Types.TIMESTAMP);
		}
		//ToTimeActual_1
		if(tsEntry.getToTimeActual_1() != null) {
			ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(tsEntry.getToTimeActual_1()));
		} else {
			ps.setNull(i++, java.sql.Types.TIMESTAMP);
		}
		//FromTimeActual_2
		if(tsEntry.getFromTimeActual_2() != null) {
			ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(tsEntry.getFromTimeActual_2()));
		} else {
			ps.setNull(i++, java.sql.Types.TIMESTAMP);
		}
		//ToTimeActual_2
		if(tsEntry.getToTimeActual_2() != null) {
			ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(tsEntry.getToTimeActual_2()));
		} else {
			ps.setNull(i++, java.sql.Types.TIMESTAMP);
		}
		
		// FromTimeActual_3
		if(tsEntry.getFromTimeActual_3() != null) {
			ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(tsEntry.getFromTimeActual_3()));
		} else {
			ps.setNull(i++, java.sql.Types.TIMESTAMP);
		}
		// ToTimeActual_3
		if(tsEntry.getToTimeActual_3() != null) {
			ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(tsEntry.getToTimeActual_3()));
		} else {
			ps.setNull(i++, java.sql.Types.TIMESTAMP);
		}

		// ActualType_1
		if(tsEntry.getActualType_1() != null) {
			ps.setLong(i++, tsEntry.getActualType_1().getId());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// ActualType_2
		if(tsEntry.getActualType_2() != null) {
			ps.setLong(i++, tsEntry.getActualType_2().getId());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// ActualType_3
		if(tsEntry.getActualType_3() != null) {
			ps.setLong(i++, tsEntry.getActualType_3().getId());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// workingHours
		if(tsEntry.getWorkingHours() != null) {
			ps.setDouble(i++, tsEntry.getWorkingHours());
		} else {
			ps.setNull(i++, java.sql.Types.NUMERIC);
		}
		
		// workingHoursActual
		if(tsEntry.getWorkingHoursActual() != null) {
			ps.setDouble(i++, tsEntry.getWorkingHoursActual());
		} else {
			ps.setNull(i++, java.sql.Types.NUMERIC);
		}
		
		// Type_1
		if(tsEntry.getType_1() != null) {
			ps.setLong(i++, tsEntry.getType_1().getId());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// Type_2
		if(tsEntry.getType_2() != null) {
			ps.setLong(i++, tsEntry.getType_2().getId());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// ActualValidated
		ps.setBoolean(i++, tsEntry.isActualValidated());
		
		// return the current ps position 
		return i;
	}
	
	protected int buildForUpdatePlan(SocialTimeSheetEntry tsEntry, PreparedStatement ps, int i) throws SQLException, AEException {
		//FromTime_1
		if(tsEntry.getFromTime_1() != null) {
			ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(tsEntry.getFromTime_1()));
		} else {
			ps.setNull(i++, java.sql.Types.TIMESTAMP);
		}
		
		//ToTime_1
		if(tsEntry.getToTime_1() != null) {
			ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(tsEntry.getToTime_1()));
		} else {
			ps.setNull(i++, java.sql.Types.TIMESTAMP);
		}
		
		//FromTime_2
		if(tsEntry.getFromTime_2() != null) {
			ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(tsEntry.getFromTime_2()));
		} else {
			ps.setNull(i++, java.sql.Types.TIMESTAMP);
		}
		
		//ToTime_2
		if(tsEntry.getToTime_2() != null) {
			ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(tsEntry.getToTime_2()));
		} else {
			ps.setNull(i++, java.sql.Types.TIMESTAMP);
		}
		
		// workingHours
		if(tsEntry.getWorkingHours() != null) {
			ps.setDouble(i++, tsEntry.getWorkingHours());
		} else {
			ps.setNull(i++, java.sql.Types.NUMERIC);
		}
		
		// Type_1
		if(tsEntry.getType_1() != null) {
			ps.setLong(i++, tsEntry.getType_1().getId());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// Type_2
		if(tsEntry.getType_2() != null) {
			ps.setLong(i++, tsEntry.getType_2().getId());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// return the current ps position 
		return i;
	}
	
	protected void build(SocialTimeSheetEntry tsEntry, ResultSet rs) throws SQLException, AEException {
		super.build(tsEntry, rs);
		
		//PeriodType
		int periodTypeId = rs.getInt("PeriodType");
		if(!rs.wasNull()) {
			tsEntry.setPeriodType(SocialPeriodType.valueOf(periodTypeId));
		}
		//DayType
		int dayTypeId = rs.getInt("DayType");
		if(!rs.wasNull()) {
			tsEntry.setDayType(SocialDayType.valueOf(dayTypeId));
		}
		//Employee_ID
		long employeeId = rs.getLong("Employee_ID");
		if(!rs.wasNull()) {
			tsEntry.setEmployee(Employee.lazyDescriptor(employeeId));
		}
		//Date
		tsEntry.setDate(rs.getTimestamp("Date"));
		//WeekNumber
		tsEntry.setWeekNumber(rs.getInt("WeekNumber"));
		//DayOfWeek
		tsEntry.setDayOfWeek(rs.getInt("DayOfWeek"));
		//FromTime_1
		tsEntry.setFromTime_1(rs.getTimestamp("FromTime_1"));
		//ToTime_1
		tsEntry.setToTime_1(rs.getTimestamp("ToTime_1"));
		//FromTime_2
		tsEntry.setFromTime_2(rs.getTimestamp("FromTime_2"));
		//ToTime_2
		tsEntry.setToTime_2(rs.getTimestamp("ToTime_2"));
		//Modifiable
		tsEntry.setModifiable(rs.getBoolean("Modifiable"));
		//FromTimeActual_1
		tsEntry.setFromTimeActual_1(rs.getTimestamp("FromTimeActual_1"));
		//ToTimeActual_1
		tsEntry.setToTimeActual_1(rs.getTimestamp("ToTimeActual_1"));
		//FromTimeActual_2
		tsEntry.setFromTimeActual_2(rs.getTimestamp("FromTimeActual_2"));
		//ToTimeActual_2
		tsEntry.setToTimeActual_2(rs.getTimestamp("ToTimeActual_2"));
		
		//FromTimeActual_3
		tsEntry.setFromTimeActual_3(rs.getTimestamp("FromTimeActual_3"));
		//ToTimeActual_3
		tsEntry.setToTimeActual_3(rs.getTimestamp("ToTimeActual_3"));
		
		// ActualType_1
		long actualTypeId = rs.getLong("ActualType_1");
		if(!rs.wasNull() && actualTypeId > 0) {
			tsEntry.setActualType_1(SocialDayType.valueOf((int)actualTypeId));
		}
		// ActualType_2
		actualTypeId = rs.getLong("ActualType_2");
		if(!rs.wasNull() && actualTypeId > 0) {
			tsEntry.setActualType_2(SocialDayType.valueOf((int)actualTypeId));
		}
		// ActualType_3
		actualTypeId = rs.getLong("ActualType_3");
		if(!rs.wasNull() && actualTypeId > 0) {
			tsEntry.setActualType_3(SocialDayType.valueOf((int)actualTypeId));
		}
		
		// workingHours
		double workingHours = rs.getDouble("workingHours");
		if(!rs.wasNull()) {
			tsEntry.setWorkingHours(workingHours);
		}
		
		// workingHoursActual
		double workingHoursActual = rs.getDouble("workingHoursActual");
		if(!rs.wasNull()) {
			tsEntry.setWorkingHoursActual(workingHoursActual);
		}
		
		// Type_1
		long typeId = rs.getLong("Type_1");
		if(!rs.wasNull() && typeId > 0) {
			tsEntry.setType_1(SocialDayType.valueOf((int)typeId));
		}
		
		// Type_2
		typeId = rs.getLong("Type_2");
		if(!rs.wasNull() && typeId > 0) {
			tsEntry.setType_2(SocialDayType.valueOf((int)typeId));
		}
		
		// ActualValidated
		tsEntry.setActualValidated(rs.getBoolean("ActualValidated"));
	}
	
	public void update(SocialTimeSheetEntry tsEntry) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(updateSQLTimeSheetEntry);
			int i = build(tsEntry, ps, 1);
			ps.setLong(i++, tsEntry.getID());
			
			// execute
			ps.executeUpdate();
			
			// set the View state
			tsEntry.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void updateSocialTimeSheetEntryActual(SocialTimeSheetEntry tsEntry) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(updateSQLTimeSheetEntryActual);
			int i = 1;
			
			ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(tsEntry.getFromTimeActual_1()));
			ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(tsEntry.getToTimeActual_1()));
			ps.setLong(i++, tsEntry.getActualType_1().getId());
			
			ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(tsEntry.getFromTimeActual_2()));
			ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(tsEntry.getToTimeActual_2()));
			ps.setLong(i++, tsEntry.getActualType_2().getId());
			
			ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(tsEntry.getFromTimeActual_3()));
			ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(tsEntry.getToTimeActual_3()));
			ps.setLong(i++, tsEntry.getActualType_3().getId());
			
			ps.setBoolean(i++, tsEntry.isActualValidated());
			
			ps.setString(i++, tsEntry.getDescription());
			//where
			ps.setLong(i++, tsEntry.getID());
			
			// execute
			ps.executeUpdate();
			
			// set the View state
			tsEntry.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void deleteSocialTimeSheetEntry(AEDescriptor tsEntryDescr) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQLTimeSheetEntry);

			// build statement
			ps.setLong(1, tsEntryDescr.getID());
			
			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void loadTimeSheetTemplate(SocialTimeSheet timeSheet) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLTimeSheet);
			ps.setLong(1, timeSheet.getEmployee().getDescriptor().getID());
			ps.setInt(2, timeSheet.getPeriodType().getId());
			rs = ps.executeQuery();
			while(rs.next()) {
				SocialTimeSheetEntry tsEntry = new SocialTimeSheetEntry();
				build(tsEntry, rs);
				tsEntry.setView();
				timeSheet.add(tsEntry);
			}
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void loadTimeSheetPlan(SocialTimeSheet timeSheet) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLTimeSheetPlan);
			ps.setLong(1, timeSheet.getEmployee().getDescriptor().getID());
			ps.setInt(2, timeSheet.getPeriodType().getId());
			ps.setDate(3, AEPersistentUtil.getSQLDate(timeSheet.getStartDate()));
			rs = ps.executeQuery();
			while(rs.next()) {
				SocialTimeSheetEntry tsEntry = new SocialTimeSheetEntry();
				build(tsEntry, rs);
				tsEntry.setView();
				timeSheet.add(tsEntry);
			}
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void loadTimeSheetPlanByPeriod(SocialTimeSheet timeSheet) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLTimeSheetPlanByPeriod);
			ps.setLong(1, timeSheet.getEmployee().getDescriptor().getID());
			ps.setInt(2, timeSheet.getPeriodType().getId());
			ps.setDate(3, AEPersistentUtil.getSQLDate(timeSheet.getStartDate()));
			ps.setDate(4, AEPersistentUtil.getSQLDate(timeSheet.getEndDate()));
			rs = ps.executeQuery();
			while(rs.next()) {
				SocialTimeSheetEntry tsEntry = new SocialTimeSheetEntry();
				build(tsEntry, rs);
				tsEntry.setView();
				timeSheet.add(tsEntry);
			}
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public SocialTimeSheet loadMultiEmployeesTimeSheet(SocialPeriodType periodType, int weekNumber, int dayOfWeek, EmployeeList emplDescrList) throws AEException {
		SocialTimeSheet multiEmplTimeSheet = new SocialTimeSheet();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLTimeSheetsTemplateForDate);
			
			// iterate employees and load their time sheet entries for specified forDate
			for (Iterator<Employee> iterator = emplDescrList.iterator(); iterator.hasNext();) {
				Employee empl = iterator.next();

				int reqWeekNumber = weekNumber;
				int emplNumberOfWeeks = (int) AEMath.longValue(empl.getNumberOfWeeks());

				// create entry
				SocialTimeSheetEntry tsEntry = new SocialTimeSheetEntry();
				if(emplNumberOfWeeks > 0.0) {
					// find for the entry in DB 
					if(weekNumber > emplNumberOfWeeks) {
						reqWeekNumber = weekNumber % emplNumberOfWeeks;
					}

					ps.setLong(1, empl.getDescriptor().getID());
					ps.setInt(2, periodType.getId());
					ps.setInt(3, reqWeekNumber);
					ps.setInt(4, dayOfWeek);
					rs = ps.executeQuery();
					if(rs.next()) {
						build(tsEntry, rs);
						
						tsEntry.setView();
					}
				}
				tsEntry.setEmployee(empl.getDescriptor());
				
				// add to the list
				multiEmplTimeSheet.add(tsEntry);

				// close rs for the next iteratoin
				AEConnection.close(rs);
			}
			return multiEmplTimeSheet;
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public SocialTimeSheet loadMultiEmployeesTimeSheet(SocialPeriodType periodType, Date forDate, EmployeeList emplDescrList) throws AEException {
		SocialTimeSheet multiEmplTimeSheet = new SocialTimeSheet();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLTimeSheetsPlanForDate);
			
			// iterate employees and load their time sheet entries for specified forDate
			for (Iterator<Employee> iterator = emplDescrList.iterator(); iterator.hasNext();) {
				Employee empl = iterator.next();

				// create entry
				SocialTimeSheetEntry tsEntry = new SocialTimeSheetEntry();
				
				ps.setLong(1, empl.getDescriptor().getID());
				ps.setInt(2, periodType.getId());
				ps.setDate(3, AEPersistentUtil.getSQLDate(forDate));
				rs = ps.executeQuery();
				if(rs.next()) {
					build(tsEntry, rs);
					
					tsEntry.setView();
				}
				tsEntry.setEmployee(empl.getDescriptor());
				
				// add to the list
				multiEmplTimeSheet.add(tsEntry);
				
				// close rs for next iteratoin
				AEConnection.close(rs);
			}
			return multiEmplTimeSheet;
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public SocialTimeSheet loadMultiEmployeesTimeSheet(SocialPeriodType periodType, EmployeeList emplDescrList) throws AEException {
		SocialTimeSheet multiEmplTimeSheet = new SocialTimeSheet();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLTimeSheetsForPeriodType);
			
			// iterate employees and load their time sheet entries for specified forDate
			for (Iterator<Employee> iterator = emplDescrList.iterator(); iterator.hasNext();) {
				Employee empl = iterator.next();

				// set ps parameters
				ps.setLong(1, empl.getDescriptor().getID());
				ps.setInt(2, periodType.getId());
				rs = ps.executeQuery();
				while(rs.next()) {
					// create entry
					SocialTimeSheetEntry tsEntry = new SocialTimeSheetEntry();
					
					// build it
					build(tsEntry, rs);
					
					// 
					tsEntry.setView();
					tsEntry.setEmployee(empl.getDescriptor());
					
					// add to the list
					multiEmplTimeSheet.add(tsEntry);
				}
				
				// close rs for next iteratoin
				AEConnection.close(rs);
			}
			return multiEmplTimeSheet;
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public SocialTimeSheet loadMultiEmployeesTimeSheet(SocialPeriodType periodType, Date fromDate, Date toDate, EmployeeList emplDescrList) throws AEException {
		SocialTimeSheet multiEmplTimeSheet = new SocialTimeSheet();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLTimeSheetEntriesForPeriod);
			
			// iterate employees and load their time sheet entries for specified forDate
			for (Iterator<Employee> iterator = emplDescrList.iterator(); iterator.hasNext();) {
				Employee empl = iterator.next();
				ps.setLong(1, periodType.getId());
				ps.setLong(2, empl.getID());
				ps.setDate(3, AEPersistentUtil.getSQLDate(fromDate));
				ps.setDate(4, AEPersistentUtil.getSQLDate(toDate));
				rs = ps.executeQuery();
				while(rs.next()) {
					SocialTimeSheetEntry tsEntry = new SocialTimeSheetEntry();
					build(tsEntry, rs);
					
					tsEntry.setEmployee(empl);
					tsEntry.setView();
					
					multiEmplTimeSheet.add(tsEntry);
				}
				
				// close rs for next iteratoin
				AEConnection.close(rs);
			}
			return multiEmplTimeSheet;
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void loadTimeSheetActual(AEDescriptor ownerDescr, SocialTimeSheet timeSheet, Date startDate, Date endDate) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLTimeSheetActual);
			
			ps.setLong(1, ownerDescr.getID());
			ps.setInt(2, SocialPeriodType.PLAN.getId());
			ps.setDate(3, AEPersistentUtil.getSQLDate(startDate));
			ps.setDate(4, AEPersistentUtil.getSQLDate(endDate));
			rs = ps.executeQuery();
			while(rs.next()) {
				SocialTimeSheetEntry tsEntry = new SocialTimeSheetEntry();
				build(tsEntry, rs);
				
				Employee e = new Employee();
				e.setID(tsEntry.getEmployee().getDescriptor().getID());
				e.setName(rs.getString("empl_name"));
				tsEntry.setEmployee(e);
				
				tsEntry.setView();
				timeSheet.add(tsEntry);
			}
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void updateScheduleStartDate(AEDescriptor ownerDescr, Date scheduleStartDate) throws AEException {
		PreparedStatement ps = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(updateSQLScheduleStartDate);
			ps.setDate(1, AEPersistentUtil.getSQLDate(scheduleStartDate));
			ps.setLong(2, ownerDescr.getID());
			
			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEPrintTemplatesList loadPrintTemplates(AEDescriptor ownerDescr, AESocialDocument socDoc, long templatesSetId) throws AEException {
		AEPrintTemplatesList printTemplates = new AEPrintTemplatesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			if(socDoc instanceof ContractDeTravail) { 
				ContractDeTravail contract = (ContractDeTravail) socDoc;
				if(contract.getSubType().isPermanent()) {
					String sql = selectSQLPrintTemplateContractCDI;
					if(!contract.isFullTime()) {
						sql += (" and S_NOT_ANNUALISE = " + (contract.isCdiNonAnnualise() ? 1 : 0));
					}
					ps = getAEConnection().prepareStatement(sql);
				} else {
					String sql = selectSQLPrintTemplateContractCDD;
					if(ContractDeTravailType.System.TEMPORARY_REPLACEMENT.getSystemID() == contract.getSubType().getSystemID()) {
						sql += (" and s_term = " + (contract.isAppointmentFixed() ? 1 : 0));
					}
					ps = getAEConnection().prepareStatement(sql);
				}
				ps.setLong(1, socDoc.getType().getSystemID());
				ps.setLong(2, contract.getSubType().getSystemID());
				ps.setLong(3, contract.isFullTime() ? 1 : 0);
				ps.setLong(4, templatesSetId);
			}
			rs = ps.executeQuery();
			while(rs.next()) {
				SocialPrintTemplate template = new SocialPrintTemplate();
				build(template, rs);
				template.setView();
				printTemplates.add(template);
			}
		    return printTemplates;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public SocialPrintTemplate loadPrintTemplate(long templateId) throws AEException {
		SocialPrintTemplate template = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLPrintTemplateById);
			ps.setLong(1, templateId);
			rs = ps.executeQuery();
			while(rs.next()) {
				template = new SocialPrintTemplate();
				build(template, rs);
				template.setView();
			}
		    return template;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	protected void build(SocialPrintTemplate t, ResultSet rs) throws SQLException, AEException {
		super.build(t, rs);
		
		// DOC_TYPE_ID
		long docTypeId = rs.getLong("DOC_TYPE_ID");
		if(!rs.wasNull() && docTypeId > 0) {
			t.setDocType(AEDocumentType.valueOf(docTypeId));
		}
		
		// DOC_SUBTYPE_ID
		long subTypeId = rs.getLong("DOC_SUBTYPE_ID");
		if(!rs.wasNull() && subTypeId > 0) {
			t.setSubType(new ContractDeTravailType(ContractDeTravailType.System.valueOf(subTypeId)));
		}
		
		// TEMPLATE_URL
		t.setTemplateURL(rs.getString("TEMPLATE_URL"));

		// LOGO_URL
		t.setLogoURL(rs.getString("LOGO_URL"));
		
		// S_FULLTIME
		t.setFullTime(rs.getInt("S_FULLTIME") != 0);
		
		// S_TERM
		t.setTerm(rs.getInt("S_TERM"));
		
		// TEMPLATES_SET_ID
		t.setTemplateSet(SocialTemplatesSet.valueOf(rs.getInt("TEMPLATES_SET_ID")));
	}
	
	public AEDocumentsList loadDocsToTransfer() throws AEException {
		AEDocumentsList docsList = new AEDocumentsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLDocsToTransfer);
			rs = ps.executeQuery();
			while(rs.next()) {
				AEDocument doc = new AEDocument();
				
				// build
				super.build(doc, rs);
				doc.setType(AEDocumentType.valueOf(rs.getLong("TYPE_ID")));

				doc.setView();
				docsList.add(doc);
			}
		    return docsList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
}
