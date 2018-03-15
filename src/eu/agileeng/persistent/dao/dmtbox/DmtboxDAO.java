package eu.agileeng.persistent.dao.dmtbox;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.AbstractDAO;

public class DmtboxDAO extends AbstractDAO {
	
	private static String selectOwnerIdByVirtualBoxIdSQL = 
			"SELECT OWNER_ID " 
				    + "FROM Dmtbox_VirtualBox "
				    + "WHERE ID = ?";
	
	private static String insertVirtualBoxSQL = 
			"insert into Dmtbox_VirtualBox (OWNER_ID, VIRTUAL_BOX_NAME) values (?, ?)";
	
	private static String selectVirtualBoxByOwnerId = "select * from Dmtbox_VirtualBox where OWNER_ID = ?";
	
	private static String selectByVirtualBoxIdAndOwnerId = "select ID from Dmtbox_VirtualBox where OWNER_ID = ? and ID = ?";
	
	private static String deleteVirtualBoxId = "delete from Dmtbox_VirtualBox where OWNER_ID = ? and ID = ?";
	
	public DmtboxDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}
	
	public Long loadOwnerIdByVirtualBoxId(long virtualBoxId) throws AEException {
		Long ownerId = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectOwnerIdByVirtualBoxIdSQL);
			ps.setLong(1, virtualBoxId);
			rs = ps.executeQuery();
			if(rs.next()) {
				ownerId = rs.getLong("OWNER_ID");
			}
			return ownerId;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	} 

	public Long insertVirtualBox(AEDescriptor ownerDescr, String virtualBoxName) throws AEException {
		Long virtualBoxId = null;
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertVirtualBoxSQL);
			
			// build
			ps.setLong(1, ownerDescr.getID());
			ps.setString(2, virtualBoxName);

			// execute
			ps.executeUpdate();
			
			// get generated virtual box id
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				virtualBoxId = rs.getLong(1);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			return virtualBoxId;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public JSONArray loadVirtualBoxByOwnerId(long ownerId) throws AEException {
		JSONArray virtualBox = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectVirtualBoxByOwnerId);
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			virtualBox = new JSONArray();
			while (rs.next()) {
				JSONObject vb = new JSONObject();
				vb.put("virtualBoxId", rs.getLong("ID"));
				vb.put("virtualBoxName", rs.getString("VIRTUAL_BOX_NAME"));
				
				virtualBox.put(vb);
			}
			return virtualBox;
		} catch (JSONException e) {
			throw new AEException(e);
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	} 
	
	public long loadByVirtualBoxIdAndOwnerId(long ownerId, long virtualBoxId) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		long id = 0;
		try {
			ps = getAEConnection().prepareStatement(selectByVirtualBoxIdAndOwnerId);
			ps.setLong(1, ownerId);
			ps.setLong(2, virtualBoxId);
			rs = ps.executeQuery();
			
			if (rs.next()) {
				id = rs.getLong("ID");
			}
			
			return id;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void deleteVirtualBox(long ownerId, long virtualBoxId) throws AEException {
		PreparedStatement ps = null;
		
		
		try {
			ps = getAEConnection().prepareStatement(deleteVirtualBoxId);
			ps.setLong(1, ownerId);
			ps.setLong(2, virtualBoxId);
			ps.executeUpdate();
			
			
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
		
			AEConnection.close(ps);
			close();
		}
	}
	
}
