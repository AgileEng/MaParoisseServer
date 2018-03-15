/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 01.06.2010 20:37:12
 */
package eu.agileeng.security.ejb.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.security.AuthRole;
import eu.agileeng.security.AuthRolesSet;

/**
 *
 */
public class AuthRoleDAO extends AbstractDAO {

	private static String selectSQL = "select * from AuthRole where id = ?";
	
	private static String selectSQLAll = "select * from AuthRole where is_active = 1";
	
	private static String selectSQLAssociated = 
		"select roles.* from AuthMembership membership inner join AuthRole roles on " 
		+ " membership.ROLE_ID = roles.ID where membership.PRINCIPAL_ID = ?";
	
	private static String insertSQL = "insert into AuthRole (SYS_ID, CODE, NAME, DESCRIPTION, "
		+ " ID_COMPANY, IS_SYSTEM, IS_ACTIVE) values (?, ?, ?, ?, ?, ?, ?)";
	
	private static String updateSQL = "update AuthRole set SYS_ID = ?, CODE = ?, NAME = ?, DESCRIPTION = ?, "
		+ " ID_COMPANY = ?, IS_SYSTEM = ?, IS_ACTIVE = ?"
		+ " where id = ?";
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public AuthRoleDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}
	
	protected void build(AuthRole ar, ResultSet rs) throws SQLException, AEException {
		// ID
		ar.setID(rs.getLong("ID"));
		// SYS_ID
		ar.setSysId(rs.getLong("SYS_ID"));
	    // CODE
		ar.setCode(rs.getString("CODE"));
	    // NAME
		ar.setName(rs.getString("NAME"));
	    // DESCRIPTION
		ar.setDescription(rs.getString("DESCRIPTION"));
	    // ID_COMPANY
		long compId = rs.getLong("ID_COMPANY");
		if(!rs.wasNull()) {
			ar.setCompany(Organization.lazyDescriptor(compId));
		}
	    // IS_SYSTEM
		ar.setSystem(rs.getBoolean("IS_SYSTEM"));
	    // IS_ACTIVE
		ar.setActive(rs.getBoolean("IS_ACTIVE"));
	}
	
	public void insert(AuthRole ar) throws AEException {
		assert(!ar.isPersistent());
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);

			int i = 1;
			build(ar, ps, i);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				
				//required
				ar.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set view state
			ar.setView();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	protected int build(AuthRole ar, PreparedStatement ps, int i) throws SQLException, AEException {
		// SYS_ID
		ps.setLong(i++, ar.getSysId());
		
	    // CODE
		ps.setString(i++, ar.getCode());

		// NAME
		ps.setString(i++, ar.getName());

	    // DESCRIPTION
		ps.setString(i++, ar.getDescription());

	    // ID_COMPANY
		if(ar.getCompany() != null) {
			ps.setLong(i++, ar.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.INTEGER);
		}

	    // IS_SYSTEM
		ps.setBoolean(i++, ar.isSystem());

	    // IS_ACTIVE
		ps.setBoolean(i++, ar.isActive());
		
		// return the current ps position 
		return i;
	}
	
	public void update(AuthRole ar) throws AEException {
		assert(ar != null);
		assert(ar.isPersistent());
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			int i = build(ar, ps, 1);
			ps.setLong(i++, ar.getID());

			// execute
			ps.executeUpdate();
			
			// set view state
			ar.setView();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public AuthRole load(AEDescriptor arDescr) throws AEException {
		AuthRole ar = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, arDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				ar = new AuthRole();
				build(ar, rs);
			}
			// set view state
			ar.setView();
			return ar;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AuthRolesSet loadAll() throws AEException {
		AuthRolesSet rolesList = new AuthRolesSet();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAll);
			rs = ps.executeQuery();
			while(rs.next()) {
				AuthRole ar = new AuthRole();
				build(ar, rs);
				ar.setView();
				rolesList.add(ar);
			}
			return rolesList;
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
		AEDescriptor roleDescr = null;
		try {
			AuthRole role = load(new AEDescriptorImp(id, DomainClass.AuthRole));
			if(role != null) {
				roleDescr = role.getDescriptor();
			}
			return roleDescr;
		} finally {
			close();
		}
	}
	
	public AuthRolesSet loadAssociatedRoles(AEDescriptor subjDescr) throws AEException {
		AuthRolesSet rolesList = new AuthRolesSet();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAssociated);
			ps.setLong(1, subjDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				AuthRole ar = new AuthRole();
				build(ar, rs);
				ar.setView();
				rolesList.add(ar);
			}
			return rolesList;
		} catch (SQLException e) {
			throw new AEException(e); // keep cause message
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
}
