package eu.agileeng.persistent.dao.document;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;

public class DocNumSequenceDAO extends AbstractDAO {

	public DocNumSequenceDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}
	
	private static String selectSQLId = 
			"select id from DocNumberSequence where OWNER_ID = ? and YEAR = ? and DOC_TYPE = ?";
	private static String updateSQLCurrentValue = 
			"update DocNumberSequence set CURRENT_VALUE = CURRENT_VALUE + 1 where id = ?";
	private static String selectSQLCurrentValue = 
			"select CURRENT_VALUE from DocNumberSequence where id = ?";
	public long nextValue(AEDescriptor tenantDescr, int year, AEDocumentType docType) throws AEException {
		long nextValue = AEPersistentUtil.NEW_ID;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			long id = AEPersistentUtil.NEW_ID;
			getAEConnection().beginTransaction();

			// select or insert
			ps = getAEConnection().prepareStatement(selectSQLId);
			ps.setLong(1, tenantDescr.getID());
			ps.setInt(2, year);
			ps.setLong(3, docType.getSystemID());
			rs = ps.executeQuery();
			if(rs.next()) {
				id = rs.getLong("id");
			} else {
				id = insert(tenantDescr, year, docType);
			}
			AEConnection.close(rs);
			AEConnection.close(ps);
			
			// update current value
			ps = getAEConnection().prepareStatement(updateSQLCurrentValue);
			ps.setLong(1, id);
			ps.executeUpdate();
			AEConnection.close(rs);
			AEConnection.close(ps);
			
			// select current value
			ps = getAEConnection().prepareStatement(selectSQLCurrentValue);
			ps.setLong(1, id);
			rs = ps.executeQuery();
			if(rs.next()) {
				nextValue = rs.getLong("CURRENT_VALUE");
			}
			AEConnection.close(rs);
			AEConnection.close(ps);
			
			getAEConnection().commit();
			return nextValue;
		} catch (SQLException e) {
			getAEConnection().rollback();
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}		
	}
	
	private static String updateSQLReturnValue = 
			"update DocNumberSequence set CURRENT_VALUE = CURRENT_VALUE - 1 where id = ?";
	public long returnValue(AEDescriptor tenantDescr, int year, AEDocumentType docType) throws AEException {
		long previousValue = AEPersistentUtil.NEW_ID;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			long id = AEPersistentUtil.NEW_ID;
			getAEConnection().beginTransaction();

			// select or insert
			ps = getAEConnection().prepareStatement(selectSQLId);
			ps.setLong(1, tenantDescr.getID());
			ps.setInt(2, year);
			ps.setLong(3, docType.getSystemID());
			rs = ps.executeQuery();
			if(rs.next()) {
				id = rs.getLong("id");
				
				// release
				AEConnection.close(rs);
				AEConnection.close(ps);
				
				// return current value
				ps = getAEConnection().prepareStatement(updateSQLReturnValue);
				ps.setLong(1, id);
				ps.executeUpdate();
				AEConnection.close(rs);
				AEConnection.close(ps);
				
				// select current value
				ps = getAEConnection().prepareStatement(selectSQLCurrentValue);
				ps.setLong(1, id);
				rs = ps.executeQuery();
				if(rs.next()) {
					previousValue = rs.getLong("CURRENT_VALUE");
				}
				AEConnection.close(rs);
				AEConnection.close(ps);
			}
			
			getAEConnection().commit();
			return previousValue;
		} catch (SQLException e) {
			getAEConnection().rollback();
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}		
	}
	
	private static String insertSQL = 
			"insert into DocNumberSequence (OWNER_ID, YEAR, DOC_TYPE, CURRENT_VALUE) values (?, ?, ?, ?)";
	private long insert(AEDescriptor tenantDescr, int year, AEDocumentType docType) throws AEException {
		long id = AEPersistentUtil.NEW_ID;
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);
			ps.setLong(1, tenantDescr.getID());
			ps.setInt(2, year);
			ps.setLong(3, docType.getSystemID());
			ps.setLong(4, 0L);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				id = rs.getLong(1);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			return id;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}

}
