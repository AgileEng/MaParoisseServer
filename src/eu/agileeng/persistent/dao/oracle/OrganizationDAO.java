/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 22.11.2009 14:49:40
 */
package eu.agileeng.persistent.dao.oracle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDescriptorsList;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.business.DepartmentAGIRC;
import eu.agileeng.domain.business.DepartmentARRCO;
import eu.agileeng.domain.business.TaxTypeEnum;
import eu.agileeng.domain.cash.FinancesUtil;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.contact.PartiesList;
import eu.agileeng.domain.contact.Party;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AETimePeriod;


/**
 *
 */
public class OrganizationDAO extends PartyDAO {
	
//	tvaInVatId	bigint	Checked
//	tvaOutVatId	bigint	Checked
//	tvaIn1AccCode	nvarchar(32)	Checked
//	tvaIn1VatId	bigint	Checked
//	tvaIn2AccCode	nvarchar(32)	Checked
//	tvaIn2VatId	bigint	Checked
//	tvaOut1AccCode	nvarchar(32)	Checked
//	tvaOut1VatId	bigint	Checked
//	tvaOut2AccCode	nvarchar(32)	Checked
//	tvaOut2VatId	bigint	Checked
	
	private static String selectSQL = 
		"select party.*, company.* from Company as company inner join Party as party on "
		+ "company.PartyID = party.ID where party.ID = ?";
		
	private static String selectSQLAll = "select * from party where class_id = ?";
	
	private static String selectSQLCustomers = 
		"select org.*, party.* from Company org inner join party on org.PartyID = party.ID where org.customer = 1";
	
	private static String selectSQLCustomersCashSubAccount = 
		"select org.*, party.* "
		+ "from Company org inner join party on org.PartyID = party.ID "
		+ "where org.customer = 1 and org.cashAccMode = " + FinancesUtil.CASH_ACC_MODE_DAILY;
	
	private static String selectSQLCashAccMode = 
			"select cashAccMode from Company where PartyID = ?";
		
	private static String selectSQLFinancialYearInfo = 
		"select comp.finYearStartDate, comp.finYearDuration from "
		+ "Company comp inner join Party party on comp.PartyID = party.ID "
		+ "where party.ID = ?";
	
	private static String selectSQLCustomersByPrincipal = 
		"select distinct party.id, party.CODE, party.NAME "
		+ "from AuthPrincipal ap inner join AuthPrincipalEmployment ape on ape.ID_PRINCIPAL = ap.ID "
		+ "inner join Party party on ape.ID_COMPANY = party.ID "
		+ "inner join Company org on party.ID = org.PartyID "
		+ "where ap.DESCRIPTION = ? and org.customer = 1";
	
	private static String selectSQLCompanies = 
		"select org.*, party.* from Company org inner join party on org.PartyID = party.ID " +
		"where party.owner_id = ? and org.customer = ? and party.is_active = 1 order by party.name ASC";
	
	private static String selectSQLThirdPartyAccounts = 
			"select org.*, party.* , acc.code as acc_code, acc.description as acc_description from Company org inner join party on org.PartyID = party.ID " +
			" left outer join Account acc on org.compteGeneral_Id = acc.id " +
			" where party.owner_id = ? and org.customer = ? and party.is_active = 1 order by party.name ASC";
	
	private static String selectSQLClients = 
		"select org.*, party.* from Company org inner join party on org.PartyID = party.ID where party.owner_id = ? "
		+ " and org.customer = 10 and party.is_active = 1";
	
	private static String selectSQLSuppliers = 
		"select org.*, party.* from Company org inner join party on org.PartyID = party.ID where party.owner_id = ? "
		+ " and org.customer = 30 and party.is_active = 1";
	
	private static String selectSQLOneCustomer = 
		"select org.*, party.* from Company org inner join party on org.PartyID = party.ID "
		+ " where party.ID = ? and org.customer = 1";
	
	private static String selectSQLByPrincipal = 
		"select party.*, comp.* "
	    + " from AuthPrincipalEmployment as assoc inner join Company as comp "
		+ "	on assoc.ID_COMPANY = comp.PartyID inner join Party as party " 
		+ "	on comp.PartyID = party.id where assoc.ID_PRINCIPAL = ? order by party.name";

	private static String selectSQLByPrincipalDescriptor = 
		" select party.ID, party.CODE, party.NAME, party.DESCRIPTION, party.PROPERTIES, budgetConfig.hasBudgetConfig, party.IS_ACTIVE "
		+ "	from AuthPrincipalEmployment as assoc "
		+ "	inner join Party as party on assoc.ID_COMPANY = party.id " 
		+ " inner join Company as company on party.id = company.partyid "
		+ "	left outer join "
		+ "		(select 1 as hasBudgetConfig, OWNER_ID from BudgetGroup group by OWNER_ID) as budgetConfig " 
		+ "		on party.ID = budgetConfig.OWNER_ID "
		+ "where assoc.ID_PRINCIPAL = ? and company.customer = 1 order by party.name ";
	
	private static String selectSQLAllCustomers = 
			" select party.ID, party.CODE, party.NAME, party.DESCRIPTION, party.PROPERTIES, budgetConfig.hasBudgetConfig, party.IS_ACTIVE "
			+ "	from Party as party  " 
			+ " inner join Company as company on party.id = company.partyid "
			+ "	left outer join "
			+ "		(select 1 as hasBudgetConfig, OWNER_ID from BudgetGroup group by OWNER_ID) as budgetConfig " 
			+ "		on party.ID = budgetConfig.OWNER_ID "
			+ "where company.customer = 1 order by party.name ";
	
	private static String selectSQLCustomerUniqueness = 
			" select party.CODE, party.owner_id, company.customer "
			+ "	from Party as party  " 
			+ " inner join Company as company on party.id = company.partyid "
			+ "	where party.CODE = ? and party.owner_id is NULL and company.customer = 1";
	
	private static String insertSQL = 
		"insert into Company (PartyID, CEGIDNumber, taxType, taxNo, communiyVATNo, nafCode, "
		+ "collectiveConvention, salaryNumber, salaryDepartment, retirementDepartmentAGIRC, "
		+ "retirementDepartmentARRCO, retirementNoAGIRC, retirementNoARRCO, taxNoReceive, "
		+ "taxNoFile, taxNoKey, taxCenter, taxInspectionNumber, taxRegime, customer, siren, nic, "
		+ "clientAccCode, supplierAccCode, diversAccCode, cashAccCode, bankAccCode, bank1AccCode, "
		+ "bank2AccCode, tvaInAccCode, tvaOutAccCode, cashAdjAccCode, cashAdjNegAccCode, startDate, "
		+ "compteGeneral_Id, compteAuxiliare, payType_Id, payDelayDuration, payDelayUOM_Id, " 
		+ "tvaInVatId, tvaOutVatId, tvaIn1AccCode, tvaIn1VatId, tvaIn2AccCode, " 
		+ "tvaIn2VatId, tvaOut1AccCode, tvaOut1VatId, tvaOut2AccCode, tvaOut2VatId, supplierFNPAccCode, cashAccMode, "
		+ "supplierFNPVatCode, finYearStartDate, finYearDuration, bankAccMode, "
		+ "tvaOut3AccCode, tvaOut3VatId, tvaIn3AccCode, tvaIn3VatId, importConnectionURL, externalId, externalSystem, "
		+ "paroisseStatut, paroisseDoyenne, paroisseContactPerson) "
		+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
		+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " 
		+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static String updateSQL = 
		"update Company set PartyID = ?, CEGIDNumber = ?, taxType = ?, taxNo = ?, communiyVATNo = ?, nafCode  = ?, "
		+ "collectiveConvention = ?, salaryNumber = ?, salaryDepartment = ?, retirementDepartmentAGIRC = ?, "
		+ "retirementDepartmentARRCO = ?, retirementNoAGIRC = ?, retirementNoARRCO = ?, taxNoReceive = ?, "
		+ "taxNoFile = ?, taxNoKey = ?, taxCenter = ?, taxInspectionNumber = ?, taxRegime = ?, customer = ?, "
		+ "siren = ?, nic = ?, clientAccCode = ?, supplierAccCode = ?, diversAccCode = ?, "
		+ "cashAccCode = ?, bankAccCode = ?, bank1AccCode = ?, bank2AccCode  = ?, " 
		+ "tvaInAccCode = ?, tvaOutAccCode = ?, cashAdjAccCode = ?, cashAdjNegAccCode = ?, startDate = ?, "
		+ "compteGeneral_Id = ?, compteAuxiliare = ?, payType_Id = ?, payDelayDuration = ?, payDelayUOM_Id = ?, "
		+ "tvaInVatId = ?, tvaOutVatId = ?, tvaIn1AccCode = ?, tvaIn1VatId = ?, tvaIn2AccCode = ?, " 
		+ "tvaIn2VatId = ?, tvaOut1AccCode = ?, tvaOut1VatId = ?, tvaOut2AccCode = ?, tvaOut2VatId = ?, supplierFNPAccCode = ?, "
		+ "cashAccMode = ?, supplierFNPVatCode = ?, finYearStartDate = ?, finYearDuration = ?, bankAccMode = ?, "
		+ "tvaOut3AccCode = ?, tvaOut3VatId = ?, tvaIn3AccCode = ?, tvaIn3VatId = ?, importConnectionURL = ?, "
		+ "externalId = ?, externalSystem = ?, paroisseStatut = ?, paroisseDoyenne = ?, paroisseContactPerson = ? "
		+ "where PartyID = ?";
	
	private static String selectSQLPayDelay = 
		"SELECT payDelayDuration, payDelayUOM_Id FROM Company where PartyID = ?";
	
	private static String selectSQLAccInfo = 
		"SELECT acc.code, compteGeneral_Id, compteAuxiliare "
		+ " FROM Company inner join Account acc on compteGeneral_Id = acc.ID where PartyID = ?";
	
	private static String selectSQLByAccInfo = 
			"SELECT PartyId FROM Company " 
		    + "where compteGeneral_Id = ? and compteAuxiliare = ? ";
	
	private static String deleteSQL = "delete from Company where PartyId = ?";
	
	private static String selectSQLAlreadyImported = 
		"select PartyID from Company where externalId = ? and externalSystem = ?";
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	OrganizationDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	@Override
	public void insert(Party party) throws AEException {
		assert(party != null);
		assert(!party.isPersistent());
		assert party instanceof Organization : "party instanceof Organization failed";
		Organization org = (Organization) party;
		PreparedStatement ps = null;
		try {
			// insert super
			super.insert(party);
			
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(insertSQL);
			build(org, ps, 1);

			// execute
			ps.executeUpdate();
			
			// set view state
			party.setView();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	@Override
	public void build(Party party, ResultSet rs) throws SQLException, AEException {
		// build common attributes
		super.build(party, rs);

		// build additional attributes
		Organization org = (Organization) party;

		// PartyID
		// CEGIDNumber
		org.setCEGIDNumber(rs.getString("CEGIDNumber"));
		// taxType
		TaxTypeEnum taxType = null;
//		long taxTypeID = rs.getLong("taxType");
//		if(!rs.wasNull() && taxTypeID > 0) {
//			taxType = (TaxTypeEnum) AEServiceLocator.getInstance().getCacheService().getEnumeratedType(taxTypeID);
//		}
		org.setTaxType(taxType);
		// taxNo
		org.setTaxNo(rs.getString("taxNo"));
		// communiyVATNo
		org.setCommuniyVATNo(rs.getString("communiyVATNo"));
		// nafCode
		org.setNafCode(rs.getString("nafCode"));
		// collectiveConvention
		org.setCollectiveConvention(rs.getString("collectiveConvention"));
		// salaryNumber
		org.setSalaryNumber(rs.getString("salaryNumber"));
		// salaryDepartment
		org.setSalaryDepartment(rs.getString("salaryDepartment"));
		// retirementDepartmentAGIRC
		DepartmentAGIRC departmentAGIRC = null;
//		long departmentAGIRCID = rs.getLong("retirementDepartmentAGIRC");
//		if(!rs.wasNull() && departmentAGIRCID > 0) {
//			departmentAGIRC = 
//				(DepartmentAGIRC) AEServiceLocator.getInstance().getCacheService().getEnumeratedType(departmentAGIRCID);
//		}
		org.setDepartmentAGIRC(departmentAGIRC);
		// retirementDepartmentARRCO
		DepartmentARRCO departmentARRCO = null;
//		long departmentARRCOID = rs.getLong("retirementDepartmentARRCO");
//		if(!rs.wasNull() && departmentARRCOID > 0) {
//			departmentARRCO = 
//				(DepartmentARRCO) AEServiceLocator.getInstance().getCacheService().getEnumeratedType(departmentARRCOID);
//		}
		org.setDepartmentARRCO(departmentARRCO);
		// retirementNoAGIRC
		org.setRetirementNoAGIRC(rs.getString("retirementNoAGIRC"));
		// retirementNoARRCO
		org.setRetirementNoARRCO(rs.getString("retirementNoARRCO"));
		// taxNoReceive
		org.setTaxNoReceive(rs.getString("taxNoReceive"));
		// taxNoFile
		org.setTaxNoFile(rs.getString("taxNoFile"));
		// taxNoKey
		org.setTaxNoKey(rs.getString("taxNoKey"));
		// taxCenter
		org.setTaxCenter(rs.getString("taxCenter"));
		// taxInspectionNumber
		org.setTaxInspectionNumber(rs.getString("taxInspectionNumber"));
		// taxRegime
		org.setTaxRegime(rs.getString("taxRegime"));
		// startDate
		Date startDate = rs.getDate("startDate");
		if(!rs.wasNull()) {
			org.setStartDate(startDate);
		} else {
			org.setStartDate(null);
		}
		
		// set this record in view state
		party.setView();
	}
	
	@Override
	public void update(Party party) throws AEException {
		assert(party != null);
		assert(party.isPersistent());
		assert party instanceof Organization : "party instanceof Organization failed";
		Organization org = (Organization) party;
		PreparedStatement ps = null;
		try {
			// update document table
			super.update(org);
			
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			int i = build(org, ps, 1);
			ps.setLong(i++, party.getID());
			
			// execute
			ps.executeUpdate();

			// set view state
			party.setView();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.dao.oracle.PartyDAO#load(eu.agileeng.domain.AEDescriptor)
	 */
	@Override
	public Party load(AEDescriptor partyDescr) throws AEException {
		Organization org = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, partyDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				org = new Organization();
				build(org, rs);
				postLoad(org);
				org.setView();
			}
			return org;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEDescriptor loadImported(long externalId, String externalSystem) throws AEException {
		AEDescriptor ret = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAlreadyImported);
			ps.setLong(1, externalId);
			ps.setString(2, externalSystem);
			rs = ps.executeQuery();
			if(rs.next()) {
				ret = Organization.lazyDescriptor(rs.getLong("PartyID"));
			}
			return ret;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.dao.oracle.PartyDAO#loadDescriptor(long)
	 */
	@Override
	public AEDescriptive loadDescriptive(long id) throws AEException {
		AEDescriptor orgDescr = null;
		try {
			Party org = load(new AEDescriptorImp(id, DomainClass.ORGANIZATION));
			if(org != null) {
				orgDescr = org.getDescriptor();
			}
			return orgDescr;
		} finally {
			close();
		}
	}

	public AETimePeriod loadFinancialPeriod(AEDescriptor compDescr) throws AEException {
		AETimePeriod period = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLFinancialYearInfo);
			ps.setLong(1, compDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				Date finYearStartDate = rs.getDate("finYearStartDate");
				int finYearDuration = rs.getInt("finYearDuration");
				
				AETimePeriod currFinancialPeriod = new AETimePeriod();
				currFinancialPeriod.setStartDate(finYearStartDate);
				currFinancialPeriod.setDuration(finYearDuration);
				
				return currFinancialPeriod;
			}
			return period;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.dao.oracle.PartyDAO#loadAll()
	 */
	@Override
	public PartiesList loadAll() throws AEException {
		PartiesList partList = new PartiesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAll);
			ps.setLong(1, DomainModel.DomainClass.ORGANIZATION.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				Organization org = new Organization();
				super.build(org, rs);
				postLoad(org);
				org.setView();
				
				partList.add(org);
			}
			return partList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.dao.oracle.PartyDAO#loadToSubject(eu.agileeng.domain.AEDescriptor)
	 */
	@Override
	public PartiesList loadToPrincipal(AEDescriptor principalDescr) throws AEException {
		PartiesList orgList = new PartiesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLByPrincipal);
			ps.setLong(1, principalDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				Party org = new Organization();
				build(org, rs);
				orgList.add(org);
			}
			return orgList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private int build(Organization org, PreparedStatement ps, int i) throws SQLException, AEException {
		// PartyID
		ps.setLong(i++, org.getID());
		// CEGIDNumber
		ps.setString(i++, org.getCEGIDNumber());
		// taxType
		if(org.getTaxType() != null) {
			ps.setLong(i++, org.getTaxType().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		// taxNo
		ps.setString(i++, org.getTaxNo());
		// communiyVATNo
		ps.setString(i++, org.getCommuniyVATNo());
		// nafCode
		ps.setString(i++, org.getNafCode());
		// collectiveConvention
		ps.setString(i++, org.getCollectiveConvention());
		// salaryNumber
		ps.setString(i++, org.getSalaryNumber());
		// salaryDepartment
		ps.setString(i++, org.getSalaryDepartment());
		// retirementDepartmentAGIRC
		if(org.getDepartmentAGIRC() != null) {
			ps.setLong(i++, org.getDepartmentAGIRC().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		// retirementDepartmentARRCO
		if(org.getDepartmentARRCO() != null) {
			ps.setLong(i++, org.getDepartmentARRCO().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		// retirementNoAGIRC
		ps.setString(i++, org.getRetirementNoAGIRC());
		// retirementNoARRCO
		ps.setString(i++, org.getRetirementNoARRCO());
		// taxNoReceive
		ps.setString(i++, org.getTaxNoReceive());
		// taxNoFile
		ps.setString(i++, org.getTaxNoFile());
		// taxNoKey
		ps.setString(i++, org.getTaxNoKey());
		// taxCenter
		ps.setString(i++, org.getTaxCenter());
		// taxInspectionNumber
		ps.setString(i++, org.getTaxInspectionNumber());
		// taxRegime
		ps.setString(i++, org.getTaxRegime());
		
		return i;
	}
	
	public void insertOrganization(JSONObject org) throws AEException {
		assert(org != null);
		PreparedStatement ps = null;
		try {
			// insert super (party)
			super.insert(org);
			
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(insertSQL);
			build(org, ps, 1);

			// execute
			ps.executeUpdate();
			
			// set view state
			setDBActionNone(org);
		} catch (Exception e) {
			throw new AEException(e); // keep original message
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void checkCustomerUniqueness(JSONObject customer) throws AEException {
		assert(customer != null);
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement
			ps = getAEConnection().prepareStatement(selectSQLCustomerUniqueness);
			psSetString(ps, 1, customer, AEDomainObject.JSONKey.code.name());

			// execute
			rs = ps.executeQuery();
			if(rs.next()) {
				throw new AEException("Duplication! ");
			}
		} catch (Exception e) {
			throw new AEException(e); // keep original message
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void update(JSONObject org) throws AEException {
		assert(org != null);
		PreparedStatement ps = null;
		try {
			// insert super (party)
			super.update(org);
			
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(updateSQL);
			int i = build(org, ps, 1);
			ps.setLong(i++, org.getLong("id"));

			// execute
			ps.executeUpdate();
			
			// set view state
			setDBActionNone(org);
		} catch (Exception e) {
			throw new AEException(e); // keep original message
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void delete(JSONObject org) throws AEException {
		try {
			// delete super (party)
			AEDescriptor orgDescr = Organization.lazyDescriptor(org.optLong(AEDomainObject.JSONKey.id.name()));
			delete(orgDescr);
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			close();
		}
	}
	
	@Override
	public void deactivate(AEDescriptor orgDescr) throws AEException {
		try {
			super.deactivate(orgDescr);
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			close();
		}
	}
	
	@Override
	public void activate(AEDescriptor orgDescr) throws AEException {
		try {
			super.activate(orgDescr);
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			close();
		}
	}
	
	private int build(JSONObject org, PreparedStatement ps, int i) throws SQLException, AEException, JSONException {
		// PartyID
		psSetLong(ps, i++, org, AEDomainObject.JSONKey.id.name());
		// CEGIDNumber
		psSetString(ps, i++, org, AEDomainObject.JSONKey.code.name());
		// taxType
		ps.setNull(i++, Types.BIGINT);
		// taxNo
		ps.setNull(i++, Types.NVARCHAR);
		// communiyVATNo
		psSetString(ps, i++, org, "tva_intracon");
		// nafCode
		ps.setNull(i++, Types.NVARCHAR);
		// collectiveConvention
		ps.setNull(i++, Types.NVARCHAR);
		// salaryNumber
		ps.setNull(i++, Types.NVARCHAR);
		// salaryDepartment
		ps.setNull(i++, Types.NVARCHAR);
		// retirementDepartmentAGIRC
		ps.setNull(i++, java.sql.Types.BIGINT);
		// retirementDepartmentARRCO
		ps.setNull(i++, java.sql.Types.BIGINT);
		// retirementNoAGIRC
		ps.setNull(i++, Types.NVARCHAR);
		// retirementNoARRCO
		ps.setNull(i++, Types.NVARCHAR);
		// taxNoReceive
		ps.setNull(i++, Types.NVARCHAR);
		// taxNoFile
		ps.setNull(i++, Types.NVARCHAR);
		// taxNoKey
		ps.setNull(i++, Types.NVARCHAR);
		// taxCenter
		ps.setNull(i++, Types.NVARCHAR);
		// taxInspectionNumber
		ps.setNull(i++, Types.NVARCHAR);
		// taxRegime
		ps.setNull(i++, Types.NVARCHAR);
		// customer
		psSetLong(ps, i++, org, "customer");
		// siren
		psSetString(ps, i++, org, "siren");
		// nic
		psSetString(ps, i++, org, "nic");
		//clientAccCode
		psSetString(ps, i++, org, "defaultClientAccCode");
		//supplierAccCode
		psSetString(ps, i++, org, "defaultSuppplierAccCode");
		//diversAccCode
		psSetString(ps, i++, org, "defaultDiversAccCode");
		//cashAccCode
		psSetString(ps, i++, org, "defaultCashAccCode");
		//bankAccCode
		psSetString(ps, i++, org, "defaultBanqueAccCode");
		//bank1AccCode
		psSetString(ps, i++, org, "defaultBanque1AccCode");
		//bank2AccCode
		psSetString(ps, i++, org, "defaultBanque2AccCode");
		//tvaInAccCode
		psSetString(ps, i++, org, "defaultTVAInAccCode");
		//tvaOutAccCode
		psSetString(ps, i++, org, "defaultTVAOutAccCode");
		//cashAdjAccCode
		psSetString(ps, i++, org, "defaultCashAdjAccCode");
		//cashAdjNegAccCode
		psSetString(ps, i++, org, "defaultCashAdjNegAccCode");
		//startDate
		if(org.has("startDate")) {
			Date startDate = AEDateUtil.parseDateStrict(org.getString("startDate"));
			if(startDate != null) {
				ps.setDate(i++, AEPersistentUtil.getSQLDate(startDate));
			} else {
				ps.setNull(i++, java.sql.Types.DATE);
			}
		} else {
			ps.setNull(i++, java.sql.Types.DATE);
		}
		// compteGeneral_Id
		psSetId(ps, i++, org, "compteGeneralId");
		// compteAuxiliare
		psSetString(ps, i++, org, "compteAuxiliare");
		// payType_Id
		psSetLong(ps, i++, org, "payTypeId");
		// payDelayDuration
		psSetLong(ps, i++, org, "payDelayDuration");
		// payDelayUOM_Id
		psSetLong(ps, i++, org, "payDelayUOMId");
		
		// tvaInVatId
		psSetLong(ps, i++, org, "tvaInVatId");
		
		// tvaOutVatId
		psSetLong(ps, i++, org, "tvaOutVatId");
		
		// tvaIn1AccCode
		psSetString(ps, i++, org, "tvaIn1AccCode");
		
		// tvaIn1VatId
		psSetLong(ps, i++, org, "tvaIn1VatId");
		
		// tvaIn2AccCode
		psSetString(ps, i++, org, "tvaIn2AccCode");
		
		// tvaIn2VatId
		psSetLong(ps, i++, org, "tvaIn2VatId");
		
		// tvaOut1AccCode
		psSetString(ps, i++, org, "tvaOut1AccCode");
		
		// tvaOut1VatId
		psSetLong(ps, i++, org, "tvaOut1VatId");
		
		// tvaOut2AccCode
		psSetString(ps, i++, org, "tvaOut2AccCode");
		
		// tvaOut2VatId
		psSetLong(ps, i++, org, "tvaOut2VatId");
		
		// supplierFNPAccCode
		psSetString(ps, i++, org, "defaultSuppplierFNPAccCode");
		
		// cashAccMode
		psSetLong(ps, i++, org, "cashAccMode");
		
		// supplierFNPVatCode
		psSetString(ps, i++, org, "defaultSuppplierFNPVatCode");
		
		// finYearStartDate
		if(org.has("finYearStartDate")) {
			Date startDate = AEDateUtil.parseDateStrict(org.getString("finYearStartDate"));
			if(startDate != null) {
				ps.setDate(i++, AEPersistentUtil.getSQLDate(startDate));
			} else {
				ps.setNull(i++, java.sql.Types.DATE);
			}
		} else {
			ps.setNull(i++, java.sql.Types.DATE);
		}
		
		// finYearStartDate
		ps.setInt(i++, org.optInt("finYearDuration", 12));
		
		// bankAccMode
		psSetLong(ps, i++, org, "bankAccMode");
		
		// tvaOut3AccCode, tvaOut3VatId, tvaIn3AccCode, tvaIn3VatId
		psSetString(ps, i++, org, "tvaOut3AccCode");
		psSetLong(ps, i++, org, "tvaOut3VatId");
		psSetString(ps, i++, org, "tvaIn3AccCode");
		psSetLong(ps, i++, org, "tvaIn3VatId");
		
		// importConnectionURL
		String importConnectionURL = org.optString("importConnectionURL");
		if(!AEStringUtil.isEmpty(importConnectionURL)) {
			ps.setString(i++, importConnectionURL);
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// externalId
		if(org.has("externalId")) {
			long externalId = org.optLong("externalId");
			if(externalId > 0) {
				ps.setLong(i++, externalId);
			} else {
				ps.setNull(i++, java.sql.Types.NVARCHAR);
			}
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// externalSystem
		if(org.has("externalSystem")) {
			String externalSystem = org.optString("externalSystem");
			if(!AEStringUtil.isEmpty(externalSystem)) {
				ps.setString(i++, externalSystem);
			} else {
				ps.setNull(i++, java.sql.Types.NVARCHAR);
			}
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// paroisseStatut
		psSetString(ps, i++, org, Organization.JSONKey.paroisseStatut);
		
		// paroisseDoyenne
		psSetString(ps, i++, org, Organization.JSONKey.paroisseDoyenne);
		
		// paroisseContactPerson
		psSetString(ps, i++, org, Organization.JSONKey.paroisseContactPerson);
		
		return i;
	}
	
	public JSONArray loadCustomers() throws AEException, JSONException {
		JSONArray customersArray = new JSONArray();
		JSONObject org = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCustomers);
			rs = ps.executeQuery();
			while(rs.next()) {
				org = new JSONObject();
				build(org, rs);
				org.put("dbState", 0);
				
				// add to the customers collection
				customersArray.put(org);
			}
			return customersArray;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public JSONArray loadCustomersCashSubAccount() throws AEException, JSONException {
		JSONArray customersArray = new JSONArray();
		JSONObject org = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCustomersCashSubAccount);
			rs = ps.executeQuery();
			while(rs.next()) {
				org = new JSONObject();
				build(org, rs);
				org.put("dbState", 0);
				
				// add to the customers collection
				customersArray.put(org);
			}
			return customersArray;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public JSONArray loadCustomersByPrincipal(AEDescriptor principal) throws AEException, JSONException {
		JSONArray customersArray = new JSONArray();
		JSONObject org = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCustomersByPrincipal);
			ps.setString(1, principal.getDescription());
			rs = ps.executeQuery();
			while(rs.next()) {
				org = new JSONObject();
				org.put("id", rs.getLong("ID"));
				org.put("code", rs.getString("CODE"));
				org.put("name", rs.getString("NAME"));
				org.put("dbState", 0);
				
				// add to the customers collection
				customersArray.put(org);
			}
			return customersArray;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public JSONArray loadCompanies(long ownerId, long nature) throws AEException, JSONException {
		JSONArray companiesArray = new JSONArray();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCompanies);
			ps.setLong(1, ownerId);
			ps.setLong(2, nature);
			rs = ps.executeQuery();
			while(rs.next()) {
				JSONObject org = new JSONObject();
				build(org, rs);
				org.put("dbState", 0);
				
				// add to the companies collection
				companiesArray.put(org);
			}
			return companiesArray;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public JSONArray loadThirdPartyAccounts(long ownerId, long nature) throws AEException, JSONException {
		JSONArray companiesArray = new JSONArray();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLThirdPartyAccounts);
			ps.setLong(1, ownerId);
			ps.setLong(2, nature);
			rs = ps.executeQuery();
			while(rs.next()) {
				JSONObject org = new JSONObject();
				build(org, rs);
				try {
					org.put("compteGeneralCode", rs.getString("acc_code"));
					org.put("compteGeneralDescription", rs.getString("acc_description"));
				} catch (Exception e) {}
				org.put("dbState", 0);
				
				// add to the companies collection
				companiesArray.put(org);
			}
			return companiesArray;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public Date getStartDate(long ownerId) throws AEException, JSONException {
		Date startDate = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(
					"Select startDate from company where PartyID = ?");
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			if(rs.next()) {
				startDate = rs.getDate("startDate");
			}
			return startDate;
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public JSONArray loadClients(long ownerId) throws AEException, JSONException {
		JSONArray companiesArray = new JSONArray();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLClients);
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			while(rs.next()) {
				JSONObject org = new JSONObject();
				build(org, rs);
				org.put("dbState", 0);
				
				// add to the companies collection
				companiesArray.put(org);
			}
			return companiesArray;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public JSONArray loadSuppliers(long ownerId) throws AEException, JSONException {
		JSONArray companiesArray = new JSONArray();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLSuppliers);
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			while(rs.next()) {
				JSONObject org = new JSONObject();
				build(org, rs);
				org.put("dbState", 0);
				
				// add to the companies collection
				companiesArray.put(org);
			}
			return companiesArray;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public JSONArray loadCustomer(AEDescriptor orgDescr) throws AEException, JSONException {
		JSONArray customersArray = new JSONArray();
		JSONObject org = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLOneCustomer);
			ps.setLong(1, orgDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				org = new JSONObject();
				build(org, rs);
				org.put(AEDomainObject.JSONKey.dbState.name(), AEPersistentUtil.DB_ACTION_NONE);
				
				// add to the customers collection
				customersArray.put(org);
			}
			return customersArray;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public JSONObject loadCustomerOne(AEDescriptor customerDescr) throws AEException, JSONException {
		JSONArray customers = loadCustomer(customerDescr);
		JSONObject customer = null;
		if(customers.length() == 1) {
			customer = customers.getJSONObject(0); 
		}
		return customer;
	}
	
	private void build(JSONObject org, ResultSet rs) throws SQLException, AEException, JSONException {
		// ID
		org.put("id", rs.getLong("ID"));
		
		// CODE
		org.put("code", rs.getString("CODE"));
		
		// NAME
		org.put("name", rs.getString("NAME"));
		
		// DESCRIPTION
		org.put("description", rs.getString("DESCRIPTION"));
		
		// NOTE
		org.put("note", rs.getString("NOTE"));
		
		// communiyVATNo
		org.put("tva_intracon", rs.getString("communiyVATNo"));
		
//		//OWNER_ID
//		coa.put("ownerId", rs.getString("OWNER_ID"));
//
//		//IS_SYSTEM
//		coa.put("system", rs.getString("IS_SYSTEM"));
//		
//		//IS_ACTIVE
//		coa.put("active", rs.getInt("IS_ACTIVE") != 0);
//		
//		//IS_MODIFIABLE
//		coa.put("modifiable", rs.getInt("IS_MODIFIABLE") != 0);
		
		// customer
		org.put("customer", rs.getString("customer"));
		
		// siren
		org.put("siren", rs.getString("siren"));
		
		// nic
		org.put("nic", rs.getString("nic"));
		
		//clientAccCode
		org.put("defaultClientAccCode", rs.getString("clientAccCode"));
		
		//supplierAccCode
		org.put("defaultSuppplierAccCode", rs.getString("supplierAccCode"));
		
		//supplierFNPAccCode
		org.put("defaultSuppplierFNPAccCode", rs.getString("supplierFNPAccCode"));
		
		//supplierFNPVatCode
		org.put("defaultSuppplierFNPVatCode", rs.getString("supplierFNPVatCode"));

		//diversAccCode
		org.put("defaultDiversAccCode", rs.getString("diversAccCode"));
		
		//cashAccCode
		org.put("defaultCashAccCode", rs.getString("cashAccCode"));
		
		//bankAccCode
		org.put("defaultBanqueAccCode", rs.getString("bankAccCode"));
		
		//bank1AccCode
		org.put("defaultBanque1AccCode", rs.getString("bank1AccCode"));
		
		//bank2AccCode
		org.put("defaultBanque2AccCode", rs.getString("bank2AccCode"));
		
		//tvaInAccCode
		org.put("defaultTVAInAccCode", rs.getString("tvaInAccCode"));
		
		//tvaOutAccCode
		org.put("defaultTVAOutAccCode", rs.getString("tvaOutAccCode"));
		
		//cashAdjAccCode
		org.put("defaultCashAdjAccCode", rs.getString("cashAdjAccCode"));
		
		//cashAdjNegAccCode
		org.put("defaultCashAdjNegAccCode", rs.getString("cashAdjNegAccCode"));
		
		// OWNER_ID
		long ownerId = rs.getLong("OWNER_ID");
		if(!rs.wasNull()) {
			org.put("ownerId", ownerId);
		}
		
		// startDate
		Date startDate = rs.getDate("startDate");
		if(!rs.wasNull()) {
			org.put("startDate", AEDateUtil.convertToString(startDate, AEDateUtil.SYSTEM_DATE_FORMAT));
		}
		
		// compteGeneral_Id
		long compteGeneralId = rs.getLong("compteGeneral_Id");
		if(!rs.wasNull()) {
			org.put("compteGeneralId", compteGeneralId);
		}
		
		// compteAuxiliare
		org.put("compteAuxiliare", rs.getString("compteAuxiliare"));
		
		// payType_Id
		long payTypeId = rs.getLong("payType_Id");
		if(!rs.wasNull()) {
			org.put("payTypeId", payTypeId);
		}
		
		// payDelayDuration
		org.put("payDelayDuration", rs.getLong("payDelayDuration"));
		
		// payDelayUOM_Id
		long payDelayUOMId = rs.getLong("payDelayUOM_Id");
		if(!rs.wasNull()) {
			org.put("payDelayUOMId", payDelayUOMId);
		}
		
		// tvaInVatId
		org.put("tvaInVatId", rs.getLong("tvaInVatId"));
		
		// tvaOutVatId
		org.put("tvaOutVatId", rs.getLong("tvaOutVatId"));
		
		// tvaIn1AccCode
		org.put("tvaIn1AccCode", rs.getString("tvaIn1AccCode"));
		
		// tvaIn1VatId
		org.put("tvaIn1VatId", rs.getLong("tvaIn1VatId"));
		
		// tvaIn2AccCode
		org.put("tvaIn2AccCode", rs.getString("tvaIn2AccCode"));
		
		// tvaIn2VatId
		org.put("tvaIn2VatId", rs.getLong("tvaIn2VatId"));
		
		// tvaOut1AccCode
		org.put("tvaOut1AccCode", rs.getString("tvaOut1AccCode"));
		
		// tvaOut1VatId
		org.put("tvaOut1VatId", rs.getLong("tvaOut1VatId"));
		
		// tvaOut2AccCode
		org.put("tvaOut2AccCode", rs.getString("tvaOut2AccCode"));
		
		// tvaOut2VatId
		org.put("tvaOut2VatId", rs.getLong("tvaOut2VatId"));
		
		//IS_TEMPLATE
		org.put("template", rs.getBoolean("IS_TEMPLATE"));
		
		//IS_ACTIVE
		org.put("active", rs.getBoolean("IS_ACTIVE"));
		
		//cashAccMode
		org.put("cashAccMode", rs.getInt("cashAccMode"));
		
		//finYearStartDate
		Date finYearStartDate = null;
		try {
			finYearStartDate = rs.getDate("finYearStartDate");
		} catch(Exception e) {};
		if(finYearStartDate == null) {
			finYearStartDate = AEDateUtil.parseDate("01.10.2010");
		}
		org.put("finYearStartDate", AEDateUtil.convertToString(finYearStartDate, AEDateUtil.SYSTEM_DATE_FORMAT));
		
		//finYearDuration
		int finYearDuration = 0;
		try {
			finYearDuration = rs.getInt("finYearDuration");
		} catch(Exception e) {}
		if(finYearDuration < 1) {
			finYearDuration = 12;
		}
		org.put("finYearDuration", finYearDuration);
		
		// bankAccMode
		org.put("bankAccMode", rs.getInt("bankAccMode"));
		
		// tvaOut3AccCode, tvaOut3VatId, tvaIn3AccCode, tvaIn3VatId
		org.put("tvaOut3AccCode", rs.getString("tvaOut3AccCode"));
		org.put("tvaOut3VatId", rs.getLong("tvaOut3VatId"));
		org.put("tvaIn3AccCode", rs.getString("tvaIn3AccCode"));
		org.put("tvaIn3VatId", rs.getLong("tvaIn3VatId"));
		
		//importConnectionURL
		String importConnectionURL = rs.getString("importConnectionURL");
		if(!rs.wasNull()) {
			org.put("importConnectionURL", importConnectionURL);
		}
		
		// externalId
		long externalId = rs.getLong("externalId");
		if(!rs.wasNull()) {
			org.put("externalId", externalId);
		}
		
		// externalSystem
		String externalSystem = rs.getString("externalSystem");
		if(!rs.wasNull()) {
			org.put("externalSystem", externalSystem);
		}
		
		// paroisseStatut
		org.put("paroisseStatut", rs.getString(Organization.JSONKey.paroisseStatut));
		
		// paroisseDoyenne
		org.put("paroisseDoyenne", rs.getString(Organization.JSONKey.paroisseDoyenne));
		
		// paroisseContactPerson
		org.put("paroisseContactPerson", rs.getString(Organization.JSONKey.paroisseContactPerson));
	}
	
	public AETimePeriod loadPayDelay(AEDescriptor partyDescr) throws AEException {
		AETimePeriod payDelay = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLPayDelay);
			ps.setLong(1, partyDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				long duration = rs.getLong("payDelayDuration");
				if(!rs.wasNull()) {
					long unit = rs.getLong("payDelayUOM_Id");
					if(!rs.wasNull()) {
						payDelay = new AETimePeriod();
						payDelay.setDuration(duration);
						payDelay.setUnit(unit);
					}
				}
			}
			return payDelay;
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AccAccount loadAccAccount(AEDescriptor partyDescr) throws AEException {
		AccAccount account = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAccInfo);
			ps.setLong(1, partyDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				long accId = rs.getLong("compteGeneral_Id");
				if(!rs.wasNull()) {
					account = new AccAccount();
					account.setID(accId);
					account.setAuxiliare(rs.getString("compteAuxiliare"));
					account.setCode(rs.getString("code"));
				}
			}
			return account;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public int loadCashAccMode(AEDescriptor ownerDescr) throws AEException {
		int cashAccMode = FinancesUtil.CASH_ACC_MODE_NA;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCashAccMode);
			ps.setLong(1, ownerDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				cashAccMode = rs.getInt("cashAccMode");
			}
			return cashAccMode;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public Long loadIdByAccInfo(long accId, String auxCode) throws AEException {
		Long orgId = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLByAccInfo);
			ps.setLong(1, accId);
			ps.setString(2, auxCode);
			rs = ps.executeQuery();
			if(rs.next()) {
				orgId = new Long(rs.getLong("PartyId"));
			}
			return orgId;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	@Override
	public void delete(AEDescriptor compDescr) throws AEException, JSONException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQL);

			// build statement
			ps.setLong(1, compDescr.getID());

			// execute
			ps.executeUpdate();
			
			// delete party
			super.delete(compDescr);
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	/**
	 * Loads all customers <code>assigned</code> to specified <code>principalDescr</code>.
	 * The term <code>assigned</code> means that specified <code>principalDescr</code> has rights
	 * to operate (enter data/manage) over each item in the returned list.
	 */
	@Override
	public AEDescriptorsList loadToPrincipalDescriptor(AEDescriptor principalDescr) throws AEException {
		AEDescriptorsList orgList = new AEDescriptorsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLByPrincipalDescriptor);
			ps.setLong(1, principalDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				AEDescriptor org = Organization.lazyDescriptor(0);
				build(org, rs);
				
				long properties = org.getProperties();
				
				// has budget configuration
				rs.getLong("hasBudgetConfig");
				if(!rs.wasNull()) {
					properties |= (1L << 3);
				}
				
				// set properties
				org.setProperties(properties);
				
				orgList.add(org);
			}
			return orgList;
		} catch (SQLException e) {
			throw new AEException(e); // keep cause message
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	/**
	 * Loads all customers registered in DB.
	 */
	public AEDescriptorsList loadAllCustomers() throws AEException {
		AEDescriptorsList orgList = new AEDescriptorsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAllCustomers);
			rs = ps.executeQuery();
			while(rs.next()) {
				AEDescriptor org = Organization.lazyDescriptor(0);
				build(org, rs);
				
				long properties = org.getProperties();
				
				// has budget configuration
				rs.getLong("hasBudgetConfig");
				if(!rs.wasNull()) {
					properties |= (1L << 3);
				}
				
				// set properties
				org.setProperties(properties);
				
				orgList.add(org);
			}
			return orgList;
		} catch (SQLException e) {
			throw new AEException(e); // keep cause message
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
}
