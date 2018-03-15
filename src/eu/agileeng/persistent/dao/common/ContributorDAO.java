package eu.agileeng.persistent.dao.common;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.acc.cashbasis.AccJournalEntryTemplate.JournalIdentificationRule;
import eu.agileeng.domain.contact.Contributor;
import eu.agileeng.domain.contact.ContributorDonation;
import eu.agileeng.domain.contact.ContributorDonationsList;
import eu.agileeng.domain.contact.ContributorsList;
import eu.agileeng.domain.contact.Employee;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.contact.Person;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.oracle.EmployeeDAO;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEStringUtil;

public class ContributorDAO extends EmployeeDAO {
	
	private static String selectSQLContributors = 
			"select empl.id, empl.FirstName, empl.LastName, empl.NAME, empl.PERSON_ID, empl.COMPANY_ID, contr.id as contr_id, contr.owner_id as contr_owner_id from Contributor contr " 
			+ "inner join Employee empl on contr.EMPLOYEE_ID = empl.ID "
			+ "and empl.ID in (select MAX(empl.id) from Employee empl group by empl.PERSON_ID) " 
			+ "where empl.COMPANY_ID = ? ";
	
	private static String selectSQLDonationsAccountancy = 
			"select personDonation.*, "
			+ " employees.id, employees.FirstName, employees.LastName, employees.NAME, employees.PERSON_ID, employees.COMPANY_ID from "
			+ " ( "
			+ " 	select Sum(xxx.debitTurnover) as debitTurnover, Sum(xxx.creditTurnover) as creditTurnover, xxx.accountId as accountId, xxx.accountCode as accountCode, xxx.accountName as accountName, xxx.personId as personId from "
			+ " 	( "
			+ " 		select accountancy.*, contr.EMPLOYEE_ID as empl_id, empl.PERSON_ID as personId from " 
			+ " 		( "
			+ " 			select gj.CONTRIBUTOR_ID as contr_id, sum(gj.DEBIT_AMOUNT) debitTurnover, sum(gj.CREDIT_AMOUNT) as creditTurnover, acc.id as accountId, acc.CODE as accountCode, acc.NAME as accountName "
			+ " 			from GeneralJournal gj inner join Account acc on gj.ACCOUNT_ID = acc.ID " 
			+ " 			where gj.OWNER_ID = ? and (gj.ENTRY_DATE between ? and ?) and (acc.CODE like ? or acc.CODE like ? or acc.CODE like ? ) "
			+ " 			group by gj.CONTRIBUTOR_ID, acc.id, acc.code, acc.name "
			+ " 		) as accountancy " 
			+ " 		left outer join Contributor contr on accountancy.contr_id = contr.ID "
			+ " 		left outer join Employee empl on contr.EMPLOYEE_ID = empl.ID "
			+ " 	) as xxx group by xxx.accountId, xxx.accountCode, xxx.accountName, xxx.personId "
			+ " ) as personDonation "
			+ " left outer join " 
			+ " ( "
			+ "     select empl.id, empl.FirstName, empl.LastName, empl.NAME, empl.PERSON_ID, empl.COMPANY_ID, contr.id as contr_id " 
			+ "     from Contributor contr inner join Employee empl on contr.EMPLOYEE_ID = empl.ID "
			+ "     and empl.ID in (select MAX(empl.id) from Employee empl group by empl.PERSON_ID) " 
			+ "     where empl.COMPANY_ID = ? "
			+ " ) as employees on personDonation.personId = employees.PERSON_ID; "; 
	
	private static String selectSQLDonations = 
//			"select don.ID as don_id, don.YEAR as don_year, don.amount as don_amount, don.amount_acc as don_amount_acc, don.amount_change as don_amount_change, contr.id as max_contr_id, "
//			+ " empl.id, empl.FirstName, empl.LastName, empl.NAME, empl.PERSON_ID, empl.COMPANY_ID from Contributor contr  "
//			+ " inner join Employee empl on contr.EMPLOYEE_ID = empl.ID " 
//			+ " and empl.ID in (select MAX(empl.id) from Contributor contr inner join Employee empl on contr.EMPLOYEE_ID = empl.ID group by empl.PERSON_ID) "
//			+ " left outer join ContributorDonation don on don.person_id = empl.PERSON_ID "
//			+ " where contr.owner_id = ? and don.YEAR = ?";
			
			"with maxEmplId (id) as ( "
			+ "	select MAX(empl.id) from Contributor contr " 
			+ "	inner join Employee empl on contr.EMPLOYEE_ID = empl.ID and contr.OWNER_ID = ? "
			+ "	group by empl.PERSON_ID " 
			+ ") " 
            + " "
			+ "select "
			+ "		don.id as don_id, don.YEAR don_year, don.AMOUNT as don_amount, don.AMOUNT_ACC as don_amount_acc, don.AMOUNT_CHANGE as don_amount_change, don.AMOUNT_RECEIPTED as don_amount_receipted, " 
			+ "		contr.id as max_contr_id, " 
			+ "		empl.id, empl.FirstName, empl.LastName, empl.NAME, empl.PERSON_ID, empl.COMPANY_ID " 
			+ "from Contributor contr inner join Employee empl on contr.EMPLOYEE_ID = empl.ID and empl.ID in (select * from maxEmplId) "
			+ "left outer join ( "
			+ "		select don.* " 
			+ "		from Contributor contr inner join Employee empl on contr.EMPLOYEE_ID = empl.ID and empl.ID in (select * from maxEmplId) "
			+ "		left outer join ContributorDonation don on don.person_id = empl.PERSON_ID " 
			+ "		where contr.owner_id = ? and don.YEAR = ? "
			+ ") don on don.person_id = empl.PERSON_ID " 
			+ "where contr.owner_id = ? ";
	
	private static String selectSQLContributor = 
			"select empl.id, empl.FirstName, empl.LastName, empl.NAME, empl.PERSON_ID, empl.COMPANY_ID, contr.id as contr_id, contr.owner_id as contr_owner_id from Contributor contr " 
			+ " inner join Employee empl on contr.EMPLOYEE_ID = empl.ID "
			+ " where contr.id = ?";
	
	private static String selectSQLContributorDonations = 
			"select * from ContributorDonation where person_id = ? and year = ?";
	
	private static String insertSQL = "insert into Contributor (EMPLOYEE_ID, OWNER_ID) values (?, ?)";
	
	private static String deleteSQL = "delete from Contributor where ID = ?";
	
	//// ContributorDonation
	private static String insertSQLDonation = "insert into ContributorDonation (YEAR, AMOUNT, PERSON_ID, amount_acc, amount_change) values (?, ?, ?, ?, ?)";
	
	private static String updateSQLDonation = "update ContributorDonation set AMOUNT = ?, amount_acc = ?, amount_change = ? where id = ?";
	
	public ContributorDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}
	
	public ContributorsList loadContributorsToCompany(AEDescriptor compDescr) throws AEException {
		ContributorsList contrList = new ContributorsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLContributors);
			ps.setLong(1, compDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {				
				// create and build contributor
				Contributor contr = new Contributor();
				build(contr, rs);
				
				contrList.add(contr);
			}
			return contrList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public ContributorDonationsList loadDonationsToCompany(AEDescriptor compDescr, int year) throws AEException {
		ContributorDonationsList contrList = new ContributorDonationsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLDonations);
			ps.setLong(1, compDescr.getID());
			ps.setLong(2, compDescr.getID());
			ps.setInt(3, year);
			ps.setLong(4, compDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {				
				// create and build contributor
				ContributorDonation contr = new ContributorDonation();
				buildDonation(contr, rs);
				
				contr.setYear(year);
				
				contrList.add(contr);
			}
			return contrList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public ContributorDonationsList loadDonationsAccountancyToCompany(AEDescriptor compDescr, int year) throws AEException {
		ContributorDonationsList contrList = new ContributorDonationsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			Date date = AEDateUtil.getFirstDate(0, year);
			
			ps = getAEConnection().prepareStatement(selectSQLDonationsAccountancy);
			ps.setLong(1, compDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(AEDateUtil.beginOfTheYear(date)));
			ps.setDate(3, AEPersistentUtil.getSQLDate(AEDateUtil.endOfTheYear(date)));
			ps.setString(4, "704%");
			ps.setString(5, "77%");
			ps.setString(6, "7018%");
			ps.setLong(7, compDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {				
				// create and build contributor
				ContributorDonation contr = new ContributorDonation();
				buildAccountancy(contr, rs);
				contr.setYear(year);
				
				contrList.add(contr);
			}
			return contrList;
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public Contributor loadContributor(AEDescriptor contrDescr) throws AEException {
		Contributor contr = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLContributor);
			ps.setLong(1, contrDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {				
				// create and build contributor
				contr = new Contributor();
				build(contr, rs);
			}
			return contr;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String selectSQLPersonDescr = 
			"select empl.PERSON_ID from Contributor contr inner join Employee empl on contr.EMPLOYEE_ID = empl.ID where contr.id = ?";
	public AEDescriptor loadPersonDescr(AEDescriptor contrDescr) throws AEException {
		AEDescriptor personDescr = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLPersonDescr);
			ps.setLong(1, contrDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {				
				personDescr = Person.lazyDescriptor(rs.getLong("PERSON_ID"));
			}
			return personDescr;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public List<ContributorDonation> loadContributorDonations(AEDescriptor tenantDescr, AEDescriptor personDescr, int year) throws AEException {
		List<ContributorDonation> donations = new ArrayList<ContributorDonation>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// "select * from ContributorDonation where person_id = ? and year = ?";
			ps = getAEConnection().prepareStatement(selectSQLContributorDonations);
			ps.setLong(1, personDescr.getID());
			ps.setInt(2, year);
			rs = ps.executeQuery();
			while(rs.next()) {	
				ContributorDonation donation = new ContributorDonation();

				donation.setID(rs.getLong("id"));
				donation.amount = rs.getDouble("amount");
				donation.setAmountReceipted(rs.getDouble("amount_receipted"));
				
				donations.add(donation);
			}
			return donations;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String updateSQLDonationReceipted = "update ContributorDonation set AMOUNT_RECEIPTED = AMOUNT where id = ?";
	public long updateReceipted(List<ContributorDonation> cotrDonations) throws AEException {
		assert(cotrDonations != null);
		PreparedStatement ps = null;
		try {
			long updated = 0L;
			for (ContributorDonation contributorDonation : cotrDonations) {
				// create statement
				ps = getAEConnection().prepareStatement(updateSQLDonationReceipted);

				// build statement
				ps.setLong(1, contributorDonation.getID());

				// execute
				int i = ps.executeUpdate();
				updated += i;
			}
			return updated;
		} catch (SQLException ex) {
			throw new AEException(ex);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String selectSQLPaymentMethods = 
		  "select distinct gj.JOURNAL_IDENT_RULE_ID "
		+ "	 from GeneralJournal gj "
		+ "	 inner join Contributor contr on gj.CONTRIBUTOR_ID = contr.id "
		+ "	 inner join Employee empl on contr.EMPLOYEE_ID = empl.ID "
		+ "where "
		+ "	 gj.OWNER_ID = ? " 
		+ "	 and gj.ENTRY_DATE between ? and ? "
		+ "	 and empl.PERSON_ID = ? ";
	public Set<JournalIdentificationRule> loadJournalIdentRules(AEDescriptor tenantDescr, AEDescriptor personDescr, int year) throws AEException {
		Set<JournalIdentificationRule> rules = new HashSet<JournalIdentificationRule>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLPaymentMethods);
			ps.setLong(1, tenantDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(AEDateUtil.getFirstDate(0, year)));
			ps.setDate(3, AEPersistentUtil.getSQLDate(AEDateUtil.getLastDate(11, year)));
			ps.setLong(4, personDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {	
				long ruleId = rs.getLong("JOURNAL_IDENT_RULE_ID");
				JournalIdentificationRule rule = JournalIdentificationRule.valueOf(ruleId);
				
				rules.add(rule);
			}
			return rules;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String selectSQLExistContributorDonations = 
			"select * from ContributorDonation where person_id = ? "
			+ " and (ABS(ISNULL(AMOUNT, 0.0)) >= 0.005 or ABS(ISNULL(AMOUNT_ACC, 0.0)) >= 0.005 or ABS(ISNULL(AMOUNT_CHANGE, 0.0)) >= 0.005) ";
	public boolean existDonations(AEDescriptor personDescr) throws AEException {
		boolean exist = false;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLExistContributorDonations);
			ps.setLong(1, personDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {	
				exist = true;
			}
			return exist;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String deleteSQLByOwnerAndPerson = 
			"delete from ContributorDonation where person_id = ? ";
	public void deleteDonations(AEDescriptor personDescr) throws AEException {
		PreparedStatement ps = null;
		try {
			ps = getAEConnection().prepareStatement(deleteSQLByOwnerAndPerson);
			ps.setLong(1, personDescr.getID());
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	protected void build(Contributor c, ResultSet rs) throws SQLException, AEException {
		// deal with employee
		Employee empl = new Employee();
		empl.setBuildLazzy(true);
		super.build(empl, rs);
		super.postLoad(empl);
		c.setEmployee(empl);
		
		// build contributor
		c.setID(rs.getLong("contr_id"));
		
		// OWNER_ID
		try {
			long ownerId = rs.getLong("contr_owner_id");
			if(!rs.wasNull()) {
				c.setCompany(Organization.lazyDescriptor(ownerId));
			}
		} catch (Exception e) {}
		
		// set this in view state
		c.setView();
	}
	
	/**
	 * 
	 * 
	 * @param c
	 * @param rs
	 * @throws SQLException
	 * @throws AEException
	 */
	protected void buildAccountancy(ContributorDonation c, ResultSet rs) throws SQLException, AEException {
		// deal with employee
		Employee empl = new Employee();
		empl.setBuildLazzy(true);
		super.build(empl, rs);
		super.postLoad(empl);
		c.setEmployee(empl);
		
		// build contributor
		if(empl.getPerson() != null) {
			c.setID(empl.getPerson().getDescriptor().getID());
		} else {
			c.setID(AEPersistentUtil.getTmpID());
		}
		
		// creditTurnover
		c.amount = rs.getDouble("creditTurnover") - rs.getDouble("debitTurnover");
		
		// account
		AEDescriptor account = AccAccount.lazyDescriptor(rs.getLong("accountId"))
				.withCode(rs.getString("accountCode"))
				.withName(rs.getString("accountName"));
		c.setAccount(account);
		
		// init not identified donation
		if(!empl.isPersistent()) {
			empl.setLastName("Donateurs non identifiés pour le compte " + account.getCode());
		}
		
		// set this in view state
		c.setView();
	}
	
	/**
	 * 
	 * 
	 * @param c
	 * @param rs
	 * @throws SQLException
	 * @throws AEException
	 */
	protected void buildDonation(ContributorDonation c, ResultSet rs) throws SQLException, AEException {
		// deal with employee
		Employee empl = new Employee();
		empl.setBuildLazzy(true);
		super.build(empl, rs);
		super.postLoad(empl);
		c.setEmployee(empl);

		// donation id
		long id = rs.getLong("don_id");
		if(!rs.wasNull()) {
			c.setID(id);
			c.setView();
		} else {
			c.setID(AEPersistentUtil.getTmpID());
		}

		// amount
		c.amount = rs.getDouble("don_amount");
		
		// amountAcc
		c.amountAcc = rs.getDouble("don_amount_acc");
		
		// amountChange
		c.amountChange = rs.getDouble("don_amount_change");
		
		// amountReceipted
		try {
			c.setAmountReceipted(rs.getDouble("don_amount_receipted"));
		} catch(SQLException e) {
			// nothing
		}

		// set this in view state
		c.setView();
	}
	
	protected int buildInsert(Contributor c, PreparedStatement ps, int i) throws SQLException, AEException {
		// EMPLOYEE_ID
		if(c.getEmployee() != null) {
			ps.setLong(i++, c.getEmployee().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		
		// OWNER_ID
		if(c.getCompany() != null) {
			ps.setLong(i++, c.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		
		// return the current ps position 
		return i;
	}
	
	protected int buildInsert(ContributorDonation cd, PreparedStatement ps, int i) throws SQLException, AEException {
		// YEAR	int	Unchecked
		ps.setInt(i++, cd.getYear());
		
		// AMOUNT	numeric(20, 10)	Checked
		ps.setDouble(i++, cd.amount);
		
		// PERSON_ID	bigint	Unchecked
		ps.setLong(i++, cd.getEmployee().getPerson().getDescriptor().getID());
		
		// AMOUNT_ACC	numeric(20, 10)	Checked
		ps.setDouble(i++, cd.amountAcc);
		
		// AMOUNT_CHANGE	numeric(20, 10)	Checked
		ps.setDouble(i++, cd.amountChange);
		
		// return the current ps position 
		return i;
	}
	
	protected int buildUpdate(ContributorDonation cd, PreparedStatement ps, int i) throws SQLException, AEException {
		// AMOUNT	numeric(20, 10)	Checked
		ps.setDouble(i++, cd.amount);
		
		// AMOUNT_ACC	numeric(20, 10)	Checked
		ps.setDouble(i++, cd.amountAcc);
		
		// AMOUNT_CHANGE	numeric(20, 10)	Checked
		ps.setDouble(i++, cd.amountChange);
		
		// return the current ps position 
		return i;
	}
	
	public void insert(Contributor c) throws AEException {
		assert(!c.isPersistent());

		if(!c.isValid()) {
			throw AEError.System.INVALID_REQUEST.toException();
		}
		
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);

			int i = 1;
			buildInsert(c, ps, i);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				
				//required
				c.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set view state
			c.setView();
		} catch (SQLException ex) {
			throw new AEException(ex);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	private static String selectSQLContrinutorEmployeesIDsByOwnerIdAndPersonId = 
			"select empl.ID from Contributor contr "
			+ " inner join Employee empl on contr.EMPLOYEE_ID = empl.id "
			+ " inner join Person pers on empl.PERSON_ID = pers.PartyID "
			+ " where contr.OWNER_ID = ? and pers.PartyID = ? ";
	public List<Long> loadEmployeeIDsByPersonId(AEDescriptor tenantDescr, AEDescriptor personDescr) throws AEException {
		List<Long> emplListId = new ArrayList<Long>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLContrinutorEmployeesIDsByOwnerIdAndPersonId);
			ps.setLong(1, tenantDescr.getID());
			ps.setLong(2, personDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				long id = rs.getLong("id");
				emplListId.add(id);
			}
			return emplListId;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String deleteSQLContributor = 
			"delete from Contributor where OWNER_ID = ? and EMPLOYEE_ID in (select id from Employee where OWNER_ID = ? and PERSON_ID = ?)";
	public int delete(AEDescriptor ownerDescr, AEDescriptor personDescr) throws AEException {
		assert(!ownerDescr.isPersistent());
		assert(!personDescr.isPersistent());
		
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(deleteSQLContributor);

			ps.setLong(1, ownerDescr.getID());
			ps.setLong(2, ownerDescr.getID());
			ps.setLong(3, personDescr.getID());
			
			// execute
			int count = ps.executeUpdate();
			
			// return count
			return count;
		} catch (SQLException ex) {
			throw AEError.System.CONTRIBUTOR_IN_USE.toException();
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void insert(ContributorDonation cd) throws AEException {
		assert(!cd.isPersistent());
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLDonation);

			int i = 1;
			buildInsert(cd, ps, i);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				
				//required
				cd.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set view state
			cd.setView();
		} catch (SQLException ex) {
			throw new AEException(ex);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	private static String updateSQL = "update Contributor set EMPLOYEE_ID = ? where ID = ? and OWNER_ID = ?";
	public void update(Contributor c) throws AEException {
		assert(c != null);
		assert(c.isPersistent());
		
		if(!c.isValid()) {
			throw AEError.System.INVALID_REQUEST.toException();
		}
		
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			ps.setLong(1, c.getEmployee().getID());
			ps.setLong(2, c.getID());
			ps.setLong(3, c.getCompany().getDescriptor().getID());

			// execute
			ps.executeUpdate();
			
			// set view
			c.setView();
		} catch (SQLException ex) {
			throw new AEException(ex);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void update(ContributorDonation cd) throws AEException {
		assert(cd != null);
		assert(cd.isPersistent());
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQLDonation);

			// build statement
			int i = buildUpdate(cd, ps, 1);
			ps.setLong(i++, cd.getID());

			// execute
			ps.executeUpdate();
			
			// set view
			cd.setView();
		} catch (SQLException ex) {
			throw new AEException(ex);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	@Deprecated
	public void delete(long id) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQL);

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
	
	private static String selectSQLLastEmployeeId = 
			"select max(empl.ID) as id from Contributor contr " 
			+ " inner join Employee empl on contr.EMPLOYEE_ID = empl.ID " 
			+ " where contr.OWNER_ID = ? and empl.PERSON_ID = ? ";
	public long getLastEmployeeId(AEDescriptor tenantDescr, AEDescriptor personDescr) throws AEException {
		long lastEmployeeId = AEPersistentUtil.NEW_ID;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(selectSQLLastEmployeeId);

			// build statement
			ps.setLong(1, tenantDescr.getID());
			ps.setLong(2, personDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				lastEmployeeId = rs.getLong("id");
			}
			
			return lastEmployeeId;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String selectSQLCheckUniqueness = 
		"select contr.id, empl.ID, empl.PERSON_ID, empl.FirstName, empl.LastName, empl.NAME, empl.PERSON_ID, addr.STREET "
		+ "	from Contributor contr "
		+ " inner join Employee empl on contr.EMPLOYEE_ID = empl.ID and empl.ID in (select MAX(empl.id) from Employee empl where empl.COMPANY_ID = ? group by empl.PERSON_ID) " 
		+ " inner join Address addr on empl.CLASS_ID = addr.TO_CLASS_ID and empl.ID = addr.TO_OBJ_ID "
		+ " where empl.COMPANY_ID = ? and empl.PERSON_ID != ? and UPPER(LTRIM(RTRIM(empl.FirstName))) = ? and UPPER(LTRIM(RTRIM(empl.LastName))) = ? and UPPER(REPLACE(addr.STREET, ' ', '')) = ?";
	
	public boolean contributorUniqueness(AEDescriptor tenantDescr, AEDescriptor personDescr, String firstName, String lastName, String street) throws AEException {
		boolean uniqueness = true;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCheckUniqueness);
			
			ps.setLong(1, tenantDescr.getID());
			ps.setLong(2, tenantDescr.getID());
			ps.setLong(3, personDescr.getID());
			ps.setString(4, AEStringUtil.trim(firstName).toUpperCase());
			ps.setString(5, AEStringUtil.trim(lastName).toUpperCase());
			ps.setString(6, AEStringUtil.trim(street).toUpperCase().replaceAll(" ", AEStringUtil.EMPTY_STRING));
			
			rs = ps.executeQuery();
			if(rs.next()) {				
				uniqueness = false;
			}
			
			return uniqueness;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
}
