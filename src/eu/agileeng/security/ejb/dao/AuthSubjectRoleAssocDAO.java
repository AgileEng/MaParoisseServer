/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 03.06.2010 17:21:07
 */
package eu.agileeng.security.ejb.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.security.AuthRole;
import eu.agileeng.security.AuthSubjectRoleAssoc;

/**
 *
 */
public class AuthSubjectRoleAssocDAO extends AbstractDAO {

	private static String selectSQL = "select * from AuthMembership where id = ?";
	
	// private static String selectSQLAll = "select * from AuthSubjectRoleAssoc";
	
	private static String insertSQL = "insert into AuthMembership (CODE, NAME, DESCRIPTION, ROLE_ID, "
		+ " PRINCIPAL_ID) values (?, ?, ?, ?, ?)";
	
	private static String updateSQL = "update AuthMembership set CODE = ?, NAME = ?, "
		+ "DESCRIPTION = ?, ROLE_ID = ?, PRINCIPAL_ID = ? where id = ?";
	
	private static String deleteSQL = "delete from AuthMembership where PRINCIPAL_ID = ?";
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public AuthSubjectRoleAssocDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}
	
	protected void build(AuthSubjectRoleAssoc assoc, ResultSet rs) throws SQLException, AEException {
		// CODE
		assoc.setCode(rs.getString("CODE"));
		// NAME
		assoc.setName(rs.getString("NAME"));
		// DESCRIPTION
		assoc.setDescription(rs.getString("DESCRIPTION"));
		// ROLE_ID
		long roleId = rs.getLong("ROLE_ID");
		if(!rs.wasNull()) {
			assoc.setAuthRole(AuthRole.lazyDescriptor(roleId));
		}
		// PRINCIPAL_ID
		long principalId = rs.getLong("PRINCIPAL_ID");
		if(!rs.wasNull()) {
			assoc.setAuthSubject(AuthPrincipal.lazyDescriptor(principalId));
		}
	}
	
	public void insert(AuthSubjectRoleAssoc assoc) throws AEException {
		assert(!assoc.isPersistent());
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);

			int i = 1;
			build(assoc, ps, i);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				
				//required
				assoc.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set view state
			assoc.setView();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	protected int build(AuthSubjectRoleAssoc assoc, PreparedStatement ps, int i) throws SQLException, AEException {
		// CODE
		ps.setString(i++, assoc.getCode());
		// NAME
		ps.setString(i++, assoc.getName());
		// DESCRIPTION
		ps.setString(i++, assoc.getDescription());
		// ROLE_ID
		if(assoc.getAuthRole() != null) {
			ps.setLong(i++, assoc.getAuthRole().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.INTEGER);
		}
		// PRINCIPAL_ID
		if(assoc.getAuthSubject() != null) {
			ps.setLong(i++, assoc.getAuthSubject().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.INTEGER);
		}
		
		// return the current ps position 
		return i;
	}
	
	public void update(AuthSubjectRoleAssoc assoc) throws AEException {
		assert(assoc != null);
		assert(assoc.isPersistent());
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			int i = build(assoc, ps, 1);
			ps.setLong(i++, assoc.getID());

			// execute
			ps.executeUpdate();
			
			// set view state
			assoc.setView();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public AuthSubjectRoleAssoc load(AEDescriptor assocDescr) throws AEException {
		AuthSubjectRoleAssoc assoc = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, assocDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				assoc = new AuthSubjectRoleAssoc();
				build(assoc, rs);
			}
			
			// set view state
			assoc.setView();
			
			return assoc;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void deleteTo(AEDescriptor descrSubj) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQL);

			// build statement
			ps.setLong(1, descrSubj.getID());

			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
}
