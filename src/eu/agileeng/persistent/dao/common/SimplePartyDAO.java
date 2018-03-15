package eu.agileeng.persistent.dao.common;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.contact.SimplePartiesList;
import eu.agileeng.domain.contact.SimpleParty;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.AbstractDAO;

public class SimplePartyDAO extends AbstractDAO {

	private static String insertSQL = "insert into SimpleParty (OWNER_ID, NAME) values (?, ?)";
	
	private static String updateSQL = "update SimpleParty set OWNER_ID = ?, NAME = ? where ID = ?";
	
	private static String selectSQLToCompany = "select * from SimpleParty where OWNER_ID = ?";
	
	private static String selectSQL = "select * from SimpleParty where id = ?";
	
	public SimplePartyDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	public void insert(SimpleParty sp) throws AEException {
		assert(sp != null);
		assert(!sp.isPersistent());
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);

			int i = 1;
			build(sp, ps, i);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				
				//required
				sp.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set view state
			sp.setView();
		} catch (SQLException ex) {
			throw new AEException(ex);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void update(SimpleParty sp) throws AEException {
		assert(sp != null);
		assert(sp.isPersistent());
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			int i = build(sp, ps, 1);
			ps.setLong(i++, sp.getID());

			// execute
			ps.executeUpdate();
			
			// set view
			sp.setView();
		} catch (SQLException ex) {
			throw new AEException(ex);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	protected int build(SimpleParty sp, PreparedStatement ps, int i) throws SQLException, AEException {
		// OWNER_ID
		if(sp.getCompany() != null) {
			ps.setLong(i++, sp.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		
		// NAME
		ps.setString(i++, sp.getName());
		
		// return the current ps position 
		return i;
	}
	
	public SimplePartiesList loadToCompany(AEDescriptor compDescr) throws AEException {
		SimplePartiesList spList = new SimplePartiesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLToCompany);
			ps.setLong(1, compDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {				
				// create and build contributor
				SimpleParty sp = new SimpleParty();
				build(sp, rs);
				
				spList.add(sp);
			}
			return spList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	protected void build(SimpleParty sp, ResultSet rs) throws SQLException {
		super.build(sp, rs);
		
		// set View
		sp.setView();
	}
	
	public SimpleParty load(AEDescriptor spDescr) throws AEException {
		SimpleParty sp = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, spDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {				
				// create and build contributor
				sp = new SimpleParty();
				build(sp, rs);
			}
			return sp;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
}
