/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 01.06.2010 20:34:21
 */
package eu.agileeng.security.ejb.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.contact.Person;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.security.AuthLoginToken;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.security.AuthPrincipalsList;
import eu.agileeng.security.AuthRole;

/**
 *
 */
public class AuthPrincipalDAO extends AbstractDAO {

	private static String selectSQL = "select * from AuthPrincipal where id = ?";
	
	private static String selectSQLToken = "select * from AuthPrincipal where name = ?";
	
	private static String selectSQLAll = "select * from AuthPrincipal"; //where is_system = 0";
	
	private static String selectSQLToCompany = 
		"select principal.* from AuthPrincipal principal inner join AuthPrincipalEmployment employment "
		+ " on principal.ID = employment.ID_PRINCIPAL where employment.ID_COMPANY = ?";
	
	private static String selectSQLMembershup = 
		"select principal.* from AuthMembership membership inner join AuthPrincipal principal on "  
		+ " membership.PRINCIPAL_ID = principal.ID where membership.ROLE_ID = ?";
	
	private static String selectSQLMembershipByRoleSysId = 
		"select principal.* " 
		+ " from AuthRole authRole inner join AuthMembership membership " 
		+ " on authRole.ID = membership.ROLE_ID inner join AuthPrincipal principal " 
		+ " on membership.PRINCIPAL_ID = principal.ID where authRole.SYS_ID = ? and principal.description = ?";
	
	private static String selectSQLAccountants = 
		"select principal.* "
		+ " from AuthPrincipal principal " 
		+ " inner join AuthMembership membership on principal.ID = membership.PRINCIPAL_ID "     
		+ " inner join AuthRole authRole on authRole.ID = membership.ROLE_ID " 
	    + " inner join AuthPrincipalEmployment empl on principal.ID = empl.ID_PRINCIPAL " 
	    + " inner join Party party on empl.ID_COMPANY = party.ID "
	    + " where authRole.SYS_ID = " + AuthRole.System.accountant.getSystemID() 
	    + " and party.ID = ?";
	
	private static String selectSQLSocials = 
		"select principal.* "
		+ " from AuthPrincipal principal " 
		+ " inner join AuthMembership membership on principal.ID = membership.PRINCIPAL_ID "     
		+ " inner join AuthRole authRole on authRole.ID = membership.ROLE_ID " 
	    + " inner join AuthPrincipalEmployment empl on principal.ID = empl.ID_PRINCIPAL " 
	    + " inner join Party party on empl.ID_COMPANY = party.ID "
	    + " where authRole.SYS_ID = " + AuthRole.System.social.getSystemID() 
	    + " and party.ID = ?";
	
	private static String selectSQLOperativeSocial = 
		"select principal.* "
		+ " from AuthPrincipal principal " 
		+ " inner join AuthMembership membership on principal.ID = membership.PRINCIPAL_ID "     
		+ " inner join AuthRole authRole on authRole.ID = membership.ROLE_ID " 
	    + " inner join AuthPrincipalEmployment empl on principal.ID = empl.ID_PRINCIPAL " 
	    + " inner join Party party on empl.ID_COMPANY = party.ID "
	    + " where authRole.SYS_ID = " + AuthRole.System.operative_social.getSystemID() 
	    + " and party.ID = ?";
	
	private static String selectSQLOperativeAccountant = 
		"select principal.* "
		+ " from AuthPrincipal principal " 
		+ " inner join AuthMembership membership on principal.ID = membership.PRINCIPAL_ID "     
		+ " inner join AuthRole authRole on authRole.ID = membership.ROLE_ID " 
	    + " inner join AuthPrincipalEmployment empl on principal.ID = empl.ID_PRINCIPAL " 
	    + " inner join Party party on empl.ID_COMPANY = party.ID "
	    + " where authRole.SYS_ID = " + AuthRole.System.operative_accountant.getSystemID() 
	    + " and party.ID = ?";
	
	private static String insertSQL = "insert into AuthPrincipal (CODE, NAME, DESCRIPTION, ID_COMPANY, ID_PERSON, "
		+ "PASSWORD, IS_SYSTEM, IS_ACTIVE, FIRST_NAME, MIDDLE_NAME, LAST_NAME, E_MAIL, PHONE "
		+ ") values ("
		+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static String updateSQLWithoutPassword = 
		"update AuthPrincipal set CODE = ?, NAME = ?, DESCRIPTION = ?, FIRST_NAME = ?, MIDDLE_NAME = ?, LAST_NAME = ?, E_MAIL = ?, PHONE = ? where id = ?";
	
	private static String updateSQLExternalRef = 
		"update AuthPrincipal set external_id = ?, external_system = ? where id = ?";
	
	private static String selectSQLAppModuleName = "SELECT name from AppModule where ID = ?";
	
	private static String loginFailedSQL = "Update AuthPrincipal set LAST_FAILED_LOGIN_TIME = ?, "
		+ "FAILED_LOGIN_COUNT = FAILED_LOGIN_COUNT + 1 where ID = ?";
	
	private static String loginSuccessedSQL = "Update AuthPrincipal set LAST_LOGIN_TIME = ?, LAST_HOST_ADDRESS = ?, "
		+ "FAILED_LOGIN_COUNT = 0 where ID = ?";
	
	private static String lockSQL = "Update AuthPrincipal set IS_LOCKED = 1 where ID = ?";
	
	private static String unlockSQL = "Update AuthPrincipal set FAILED_LOGIN_COUNT = 0, IS_LOCKED = 0 where ID = ?";
	
	private static String validateHouseSQL = "Select id from AuthPrincipal where ID = ? and description = ?";
	
	private static String validateName = "Select id from AuthPrincipal where name = ?";
	
	private static String selectSQLAlreadyImported = 
		"Select id from AuthPrincipal where external_id = ? and external_system = ?";
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public AuthPrincipalDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}
	
	protected void build(AuthPrincipal as, ResultSet rs) throws SQLException, AEException {
		// ID
		as.setID(rs.getLong("ID"));
	    // CODE
		as.setCode(rs.getString("CODE"));
	    // NAME
		as.setName(rs.getString("NAME"));
	    // DESCRIPTION
		as.setDescription(rs.getString("DESCRIPTION"));
	    // ID_COMPANY
		long compId = rs.getLong("ID_COMPANY");
		if(!rs.wasNull()) {
			as.setCompany(Organization.lazyDescriptor(compId));
		}
	    // ID_PERSON
		long personId = rs.getLong("ID_PERSON");
		if(!rs.wasNull()) {
			as.setPerson(Person.lazyDescriptor(personId));
		}
	    // PASSWORD
		as.setPassword(rs.getString("PASSWORD"));
	    // IS_SYSTEM
		as.setSystem(rs.getBoolean("IS_SYSTEM"));
	    // IS_ACTIVE
		as.setActive(rs.getBoolean("IS_ACTIVE"));
		// FIRST_NAME
		as.setFirstName(rs.getString("FIRST_NAME"));
		// MIDDLE_NAME
		as.setMiddleName(rs.getString("MIDDLE_NAME"));
		// LAST_NAME
		as.setLastName(rs.getString("LAST_NAME"));
		// E_MAIL
		as.setEMail(rs.getString("E_MAIL"));
		// PHONE
		as.setPhone(rs.getString("PHONE"));
		//IS_LOCKED
		as.setLocked(rs.getBoolean("IS_LOCKED"));
		//IS_LOGGED_IN
		as.setLoggedIn(rs.getBoolean("IS_LOGGED_IN"));
		//LAST_HOST_ADDRESS
		as.setLastHostAddress(rs.getString("LAST_HOST_ADDRESS"));
		//LAST_PASSWORD_CHANGE_TIME
		as.setLastPasswordChangeTime(rs.getTimestamp("LAST_PASSWORD_CHANGE_TIME"));
		//LAST_LOGIN_TIME
		as.setLastLoginTime(rs.getTimestamp("LAST_LOGIN_TIME"));
		//LAST_FAILED_LOGIN_TIME
		as.setLastFailedLoginTime(rs.getTimestamp("LAST_FAILED_LOGIN_TIME"));
		//EXPIRATION_TIME
		as.setExpirationTime(rs.getTimestamp("EXPIRATION_TIME"));
		//FAILED_LOGIN_COUNT
		as.setFailedLoginCount(rs.getInt("FAILED_LOGIN_COUNT")); 
	}
	
	public void insert(AuthPrincipal as) throws AEException {
		assert(!as.isPersistent());
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);

			int i = 1;
			buildInsert(as, ps, i);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				
				//required
				as.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set view state
			as.setView();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	protected int buildUpdate(AuthPrincipal as, PreparedStatement ps, int i) throws SQLException, AEException {
		// FIRST_NAME = ?, MIDDLE_NAME = ?, LAST_NAME = ?, E_MAIL = ?, PHONE = ?
		
	    // CODE
		ps.setString(i++, as.getCode());

		// NAME
		ps.setString(i++, as.getName());

	    // DESCRIPTION
		ps.setString(i++, as.getDescription());

//	    // ID_COMPANY
//		if(as.getCompany() != null) {
//			ps.setLong(i++, as.getCompany().getDescriptor().getID());
//		} else {
//			ps.setNull(i++, java.sql.Types.INTEGER);
//		}
//
//		// ID_PERSON
//		if(as.getPerson() != null) {
//			ps.setLong(i++, as.getPerson().getDescriptor().getID());
//		} else {
//			ps.setNull(i++, java.sql.Types.INTEGER);
//		}

		// DONT UPDATE PASSWORD!!!
//		ps.setString(i++, as.getPassword());

//	    // IS_SYSTEM
//		ps.setBoolean(i++, as.isSystem());
//
//	    // IS_ACTIVE
//		ps.setBoolean(i++, as.isActive());
		
		// FIRST_NAME
		ps.setString(i++, as.getFirstName());
		
		// MIDDLE_NAME
		ps.setString(i++, as.getMiddleName());
		
		// LAST_NAME
		ps.setString(i++, as.getLastName());
		
		// E_MAIL
		ps.setString(i++, as.getEMail());
		
		// PHONE
		ps.setString(i++, as.getPhone());
		
		// return the current ps position 
		return i;
	}
	
	protected int buildInsert(AuthPrincipal as, PreparedStatement ps, int i) throws SQLException, AEException {
	    // CODE
		ps.setString(i++, as.getCode());

		// NAME
		ps.setString(i++, as.getName());

	    // DESCRIPTION
		ps.setString(i++, as.getDescription());

	    // ID_COMPANY
		if(as.getCompany() != null) {
			ps.setLong(i++, as.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.INTEGER);
		}

		// ID_PERSON
		if(as.getPerson() != null) {
			ps.setLong(i++, as.getPerson().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.INTEGER);
		}

		// PASSWORD
		ps.setString(i++, as.getPassword());

	    // IS_SYSTEM
		ps.setBoolean(i++, as.isSystem());

	    // IS_ACTIVE
		ps.setBoolean(i++, as.isActive());
		
		// FIRST_NAME
		ps.setString(i++, as.getFirstName());
		
		// MIDDLE_NAME
		ps.setString(i++, as.getMiddleName());
		
		// LAST_NAME
		ps.setString(i++, as.getLastName());
		
		// E_MAIL
		ps.setString(i++, as.getEMail());
		
		// PHONE
		ps.setString(i++, as.getPhone());
		
		// return the current ps position 
		return i;
	}
	
	public void update(AuthPrincipal as) throws AEException {
		assert(as != null);
		assert(as.isPersistent());
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQLWithoutPassword);

			// build statement
			int i = buildUpdate(as, ps, 1);
			ps.setLong(i++, as.getID());

			// execute
			ps.executeUpdate();
			
			// set view state
			as.setView();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void updateExternalReference(long principalId, long externalId, String externalSystem) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQLExternalRef);
			ps.setLong(1, externalId);
			ps.setString(2, externalSystem);
			ps.setLong(3, principalId);

			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public AuthPrincipal load(AEDescriptor asDescr) throws AEException {
		AuthPrincipal as = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, asDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				as = new AuthPrincipal();
				build(as, rs);
			}
			
			// set view state
			as.setView();
			
			return as;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AuthPrincipal load(AuthLoginToken loginToken) throws AEException {
		AuthPrincipal as = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLToken);
			ps.setString(1, loginToken.getUsername());
			rs = ps.executeQuery();
			if(rs.next()) {
				as = new AuthPrincipal();
				build(as, rs);
				as.setView();
			}
			return as;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AuthPrincipalsList loadAll() throws AEException {
		AuthPrincipalsList principalsList = new AuthPrincipalsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAll);
			rs = ps.executeQuery();
			while(rs.next()) {
				AuthPrincipal ap = new AuthPrincipal();
				build(ap, rs);
				ap.setView();
				principalsList.add(ap);
			}
			return principalsList;
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
	 * @see eu.agileeng.persistent.dao.oracle.PartyDAO#loadDescriptor(long)
	 */
	public AEDescriptive loadDescriptive(long id) throws AEException {
		AEDescriptor subjDescr = null;
		try {
			AuthPrincipal subj = load(new AEDescriptorImp(id, DomainClass.AuthSubject));
			if(subj != null) {
				subjDescr = subj.getDescriptor();
			}
			return subjDescr;
		} finally {
			close();
		}
	}
	
	public AuthPrincipalsList loadAssociatedSubjects(AEDescriptor roleDescr) throws AEException {
		AuthPrincipalsList subjList = new AuthPrincipalsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLMembershup);
			ps.setLong(1, roleDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				AuthPrincipal as = new AuthPrincipal();
				build(as, rs);
				as.setView();
				subjList.add(as);
			}
			return subjList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AuthPrincipalsList loadCompanyPrincipals(AEDescriptor compDescr) throws AEException {
		AuthPrincipalsList subjList = new AuthPrincipalsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			if(compDescr != null) {
				ps = getAEConnection().prepareStatement(selectSQLToCompany);
				ps.setLong(1, compDescr.getID());
				rs = ps.executeQuery();
				while(rs.next()) {
					AuthPrincipal as = new AuthPrincipal();
					build(as, rs);
					as.setView();
					subjList.add(as);
				}
			}
			return subjList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AuthPrincipalsList loadAccountants(AEDescriptor compDescr) throws AEException {
		AuthPrincipalsList pList = new AuthPrincipalsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			if(compDescr != null) {
				ps = getAEConnection().prepareStatement(selectSQLAccountants);
				ps.setLong(1, compDescr.getID());
				rs = ps.executeQuery();
				while(rs.next()) {
					AuthPrincipal as = new AuthPrincipal();
					build(as, rs);
					as.setView();
					pList.add(as);
				}
			}
			return pList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AuthPrincipalsList loadSocials(AEDescriptor compDescr) throws AEException {
		AuthPrincipalsList pList = new AuthPrincipalsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			if(compDescr != null) {
				ps = getAEConnection().prepareStatement(selectSQLSocials);
				ps.setLong(1, compDescr.getID());
				rs = ps.executeQuery();
				while(rs.next()) {
					AuthPrincipal as = new AuthPrincipal();
					build(as, rs);
					as.setView();
					pList.add(as);
				}
			}
			return pList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AuthPrincipalsList loadOperativeSocial(AEDescriptor compDescr) throws AEException {
		AuthPrincipalsList pList = new AuthPrincipalsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			if(compDescr != null) {
				ps = getAEConnection().prepareStatement(selectSQLOperativeSocial);
				ps.setLong(1, compDescr.getID());
				rs = ps.executeQuery();
				while(rs.next()) {
					AuthPrincipal as = new AuthPrincipal();
					build(as, rs);
					as.setView();
					pList.add(as);
				}
			}
			return pList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AuthPrincipalsList loadOperativeAccountant(AEDescriptor compDescr) throws AEException {
		AuthPrincipalsList pList = new AuthPrincipalsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			if(compDescr != null) {
				ps = getAEConnection().prepareStatement(selectSQLOperativeAccountant);
				ps.setLong(1, compDescr.getID());
				rs = ps.executeQuery();
				while(rs.next()) {
					AuthPrincipal as = new AuthPrincipal();
					build(as, rs);
					as.setView();
					pList.add(as);
				}
			}
			return pList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AuthPrincipalsList loadPrincipalsByRoleSysId(long roleSysId, String accCompany) throws AEException {
		AuthPrincipalsList subjList = new AuthPrincipalsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLMembershipByRoleSysId);
			ps.setLong(1, roleSysId);
			ps.setString(2, accCompany);
			rs = ps.executeQuery();
			while(rs.next()) {
				AuthPrincipal as = new AuthPrincipal();
				build(as, rs);
				as.setView();
				subjList.add(as);
			}
			return subjList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public String loadAppModuleName(long appModuleId) throws AEException {
		String name = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAppModuleName);
			ps.setLong(1, appModuleId);
			rs = ps.executeQuery();
			if(rs.next()) {
				name = rs.getString("name");
			}
			return name;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void loginFailed(long id) throws AEException {
		PreparedStatement ps = null;
		try {
			ps = getAEConnection().prepareStatement(loginFailedSQL);
			ps.setTimestamp(1, AEPersistentUtil.getTimestamp(new Date()));
			ps.setLong(2, id);
			ps.executeUpdate();
		} catch (SQLException e) {
			// don't throw this exeption!!!
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void loginSuccessed(String lastHostAddress, long id) throws AEException {
		PreparedStatement ps = null;
		try {
			ps = getAEConnection().prepareStatement(loginSuccessedSQL);
			ps.setTimestamp(1, AEPersistentUtil.getTimestamp(new Date()));
			ps.setString(2, lastHostAddress);
			ps.setLong(3, id);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void lock(long id) throws AEException {
		PreparedStatement ps = null;
		try {
			ps = getAEConnection().prepareStatement(lockSQL);
			ps.setLong(1, id);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void unlock(long id) throws AEException {
		PreparedStatement ps = null;
		try {
			ps = getAEConnection().prepareStatement(unlockSQL);
			ps.setLong(1, id);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String deactivateSQL = "Update AuthPrincipal set IS_ACTIVE = 0 where ID = ?";
	public void deactivate(long id) throws AEException {
		PreparedStatement ps = null;
		try {
			ps = getAEConnection().prepareStatement(deactivateSQL);
			ps.setLong(1, id);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String activateSQL = "Update AuthPrincipal set IS_ACTIVE = 1 where ID = ?";
	public void activate(long id) throws AEException {
		PreparedStatement ps = null;
		try {
			ps = getAEConnection().prepareStatement(activateSQL);
			ps.setLong(1, id);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String resetPasswordSQL = "Update AuthPrincipal set password = ?, LAST_PASSWORD_CHANGE_TIME = ? where ID = ?";
	public void resetPassword(long id, String password) throws AEException {
		PreparedStatement ps = null;
		try {
			ps = getAEConnection().prepareStatement(resetPasswordSQL);
			ps.setString(1, password);
			ps.setDate(2, AEPersistentUtil.getSQLDate(new Date()));
			ps.setLong(3, id);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void validateAccHouse(long id, String houseId) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(validateHouseSQL);
			ps.setLong(1, id);
			ps.setString(2, houseId);
			rs = ps.executeQuery();
			if(!rs.next()) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void validateName(String name) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(validateName);
			ps.setString(1, name);
			rs = ps.executeQuery();
			if(rs.next()) {
				throw AEError.System.PRINCIPAL_NAME_NOT_UNIQUE.toException();
			}
		} catch (SQLException e) {
			throw new AEException(e);
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
				ret = AuthPrincipal.lazyDescriptor(rs.getLong("id"));
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
	
	private static String setExpirationTimeSQL = "Update AuthPrincipal set expiration_time = ? where ID = ?";
	public void setExpirationTime(long id, Date expirationTime) throws AEException {
		PreparedStatement ps = null;
		try {
			ps = getAEConnection().prepareStatement(setExpirationTimeSQL);
			if(expirationTime != null) {
				ps.setTimestamp(1, AEPersistentUtil.getTimestamp(expirationTime));
			} else {
				ps.setNull(1, Types.TIMESTAMP);
			}
			ps.setLong(2, id);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
}
