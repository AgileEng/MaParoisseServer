/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 09.06.2010 19:35:15
 */
package eu.agileeng.persistent.dao.oracle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.contact.SubjectCompAssoc;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.security.AuthPrincipal;

/**
 *
 */
public class SubjectCompAssocDAO extends AbstractDAO {

	private static String selectSQL = "select * from AuthPrincipalEmployment where id = ?";
	
	// private static String selectSQLAll = "select * from AuthSubjectRoleAssoc";
	
	private static String insertSQL = "insert into AuthPrincipalEmployment (CODE, NAME, DESCRIPTION, NOTE, "
		+ "ID_PRINCIPAL, ID_COMPANY) values (?, ?, ?, ?, ?, ?)";
	
	private static String updateSQL = "update AuthPrincipalEmployment set CODE = ?, NAME = ?, DESCRIPTION = ?, NOTE = ?, "
		+ "ID_PRINCIPAL = ?, ID_COMPANY = ?"
		+ " where id = ?";
	
	private static String deleteSQL = "delete from AuthPrincipalEmployment where ID_PRINCIPAL = ?";
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public SubjectCompAssocDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}
	
	protected void build(SubjectCompAssoc assoc, ResultSet rs) throws SQLException, AEException {
		// CODE
		assoc.setCode(rs.getString("CODE"));
		// NAME
		assoc.setName(rs.getString("NAME"));
		// DESCRIPTION
		assoc.setDescription(rs.getString("DESCRIPTION"));
		// NOTE
		assoc.setNote(rs.getString("NOTE"));
		// ID_PRINCIPAL
		long principalId = rs.getLong("ID_PRINCIPAL");
		if(!rs.wasNull()) {
			assoc.setAuthSubject(AuthPrincipal.lazyDescriptor(principalId));
		}
		// ID_COMPANY
		long compId = rs.getLong("ID_COMPANY");
		if(!rs.wasNull()) {
			assoc.setCompany(Organization.lazyDescriptor(compId));
		}
	}
	
	public void insert(SubjectCompAssoc assoc) throws AEException {
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
	
	protected int build(SubjectCompAssoc assoc, PreparedStatement ps, int i) throws SQLException, AEException {
		// CODE
		ps.setString(i++, assoc.getCode());
		// NAME
		ps.setString(i++, assoc.getName());
		// DESCRIPTION
		ps.setString(i++, assoc.getDescription());
		// NOTE
		ps.setString(i++, assoc.getNote());
		// ID_PRINCIPAL
		if(assoc.getAuthSubject() != null) {
			ps.setLong(i++, assoc.getAuthSubject().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.INTEGER);
		}
		// ID_COMPANY
		if(assoc.getCompany() != null) {
			ps.setLong(i++, assoc.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.INTEGER);
		}
		
		// return the current ps position 
		return i;
	}
	
	public void update(SubjectCompAssoc assoc) throws AEException {
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
	
	public SubjectCompAssoc load(AEDescriptor assocDescr) throws AEException {
		SubjectCompAssoc assoc = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, assocDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				assoc = new SubjectCompAssoc();
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
