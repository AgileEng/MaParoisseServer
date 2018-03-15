/**
 * 
 */
package eu.agileeng.persistent.dao.acc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEException;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;

/**
 * @author vvatov
 *
 */
public class ChartOfAccountsDAO extends AbstractDAO {

	private static String insertSQL = "insert into ChartOfAccounts "
		+ "(CODE, NAME, DESCRIPTION, OWNER_ID, IS_SYSTEM, IS_ACTIVE, IS_MODIFIABLE, lengthG, lengthA, MODEL_ID) "
		+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static String insertSQLGOA = "Insert Into GroupOfAccounts "
		+ " (PARENT_ID, MODILE_ID, OWNER_ID, SYS_ID, CODE, NAME, DESCRIPTION, "
		+ " IS_SYSTEM, IS_ACTIVE, IS_READ_ONLY, DATE_CLOSED)"
		+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static String updateSQL = "update ChartOfAccounts set "
		+ "CODE = ?, NAME = ?, DESCRIPTION = ?, OWNER_ID = ?, IS_SYSTEM = ?, IS_ACTIVE = ?, IS_MODIFIABLE = ?, "
		+ "lengthG = ?, lengthA = ?,  MODEL_ID = ?"
		+ " where ID = ?";
	
	private static String updateSQLGOA = "update GroupOfAccounts set"
		+ " PARENT_ID = ?, MODILE_ID = ?, OWNER_ID = ?, SYS_ID = ?, CODE = ?, NAME = ?, DESCRIPTION = ?, "
		+ " IS_SYSTEM = ?, IS_ACTIVE = ?, IS_READ_ONLY = ?, DATE_CLOSED = ? "
		+ " where id = ?";

	private static String deleteSQL = "delete from ChartOfAccounts where ID = ?";
	
	private static String deleteSQLGOA = "delete from GroupOfAccounts where ID = ?";

	private static String selectSQLModels = "select * from ChartOfAccounts where owner_id is null";
	
	private static String selectSQLGOA = "select * from GroupOfAccounts where owner_id = ? and sys_id = ?";
	
	private static String closeSQLGOA = 
		"update GroupOfAccounts set DATE_CLOSED = ? where owner_id = ? and sys_id = ?";
	
	private static String selectSQLCustomer = "select * from ChartOfAccounts where owner_id = ?";
	
	private static String selectSQLCOAByModel = "select * from ChartOfAccounts where MODEL_ID = ?";
	
//	private static String selectSQLGOADateClosed = 
//		"SELECT DATE_CLOSED FROM GroupOfAccounts where OWNER_ID = ? and SYS_ID = ?";
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public ChartOfAccountsDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	public JSONArray loadModels() throws AEException, JSONException {
		JSONArray coaModelsArray = new JSONArray();
		JSONObject coaModel = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLModels);
			rs = ps.executeQuery();
			while(rs.next()) {
				coaModel = new JSONObject();
				buildCOA(coaModel, rs);
				coaModel.put("dbState", 0);
				coaModelsArray.put(coaModel);
			}
			return coaModelsArray;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	/**
	 * Loads an array with Individual COAs made by COA Model identified by specified <code>id</code>.
	 * 
	 * @param id
	 * @return a <code>not null</code>, may be empty array with Individual COAs, made by
	 * 	CAO Model with specified <code>id</code>.
	 * @throws AEException
	 * @throws JSONException
	 */
	public JSONArray loadCOAByModelId(long id) throws AEException, JSONException {
		JSONArray coaArray = new JSONArray();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCOAByModel);
			ps.setLong(1, id);
			rs = ps.executeQuery();
			while(rs.next()) {
				JSONObject coa = new JSONObject();
				buildCOA(coa, rs);
				coa.put("dbState", 0);
				coaArray.put(coa);
			}
			return coaArray;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public JSONObject loadByCustomer(long customerId) throws AEException, JSONException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			JSONObject coaModel = new JSONObject();
			ps = getAEConnection().prepareStatement(selectSQLCustomer);
			ps.setLong(1, customerId);
			rs = ps.executeQuery();
			if(rs.next()) {
				buildCOA(coaModel, rs);
				coaModel.put("dbState", 0);
			}
			return coaModel;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void insertCOA(JSONObject coa) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);
			buildCOA(coa, ps, 0);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				coa.put("id", id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			coa.put("dbState", 0);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} catch (JSONException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}

	public void insertGOA(JSONObject goa) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLGOA);
			buildGOA(goa, ps, 0);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				goa.put("id", id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			goa.put("dbState", 0);
		} catch (Exception e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void deleteCOA(long coaId) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQL);

			// build statement
			ps.setLong(1, coaId);

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
	
	public void deleteGOA(long goaId) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQLGOA);

			// build statement
			ps.setLong(1, goaId);

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

	public int buildCOA(JSONObject coa, PreparedStatement ps, int i) throws SQLException, AEException, JSONException {
		//CODE
		if(coa.has("code")) {
			ps.setString(++i, coa.getString("code"));
		} else {
			ps.setNull(++i, Types.NVARCHAR);
		}
		//NAME
		if(coa.has("name")) {
			ps.setString(++i, coa.getString("name"));
		} else {
			ps.setNull(++i, Types.NVARCHAR);
		}
		//DESCRIPTION
		if(coa.has("description")) {
			ps.setString(++i, coa.getString("description"));
		} else {
			ps.setNull(++i, Types.NVARCHAR);
		}
		//OWNER_ID
		if(coa.has("ownerId") && coa.getLong("ownerId") > 0) {
			ps.setLong(++i, coa.getLong("ownerId"));
		} else {
			ps.setNull(++i, Types.INTEGER);
		}
		//IS_SYSTEM
		if(coa.has("system")) {
			ps.setInt(++i, coa.getBoolean("system") ? 1 : 0);
		} else {
			ps.setNull(++i, Types.INTEGER);
		}
		//IS_ACTIVE
		if(coa.has("active")) {
			ps.setInt(++i, coa.getBoolean("active") ? 1 : 0);
		} else {
			ps.setNull(++i, Types.INTEGER);
		}
		//IS_MODIFIABLE
		if(coa.has("modifiable")) {
			ps.setInt(++i, coa.getBoolean("modifiable") ? 1 : 0);
		} else {
			ps.setNull(++i, Types.INTEGER);
		}
		//lengthG
		psSetInt(ps, ++i, coa, "lengthG");
		//lengthA
		psSetInt(ps, ++i, coa, "lengthA");
		
		//modelId
		psSetId(ps, ++i, coa, "modelId");

		// return the current ps position 
		return i;
	}

	private int buildGOA(JSONObject goa, PreparedStatement ps, int i) throws SQLException, AEException, JSONException {
		//PARENT_ID
		psSetLong(ps, ++i, goa, "parentId");
		//MODILE_ID
		psSetLong(ps, ++i, goa, "moduleId");
		//OWNER_ID
		psSetLong(ps, ++i, goa, "ownerId");
		//SYS_ID
		psSetLong(ps, ++i, goa, "sysId");
		//CODE
		psSetString(ps, ++i, goa, "code");
		//NAME
		psSetString(ps, ++i, goa, "name");
		//DESCRIPTION
		psSetString(ps, ++i, goa, "description");
		//IS_SYSTEM
		ps.setBoolean(++i, false);
		//IS_ACTIVE
		ps.setBoolean(++i, true);
		//IS_READ_ONLY
		ps.setBoolean(++i, false);
		//DATE_CLOSED
		psSetDate(ps, ++i, goa, "dateClosed");

		// return the current ps position 
		return i;
	}
	
	public void buildCOA(JSONObject coa, ResultSet rs) throws SQLException, AEException, JSONException {
		// ID
		coa.put("id", rs.getLong("ID"));
		
		// CODE
		coa.put("code", rs.getString("CODE"));
		
		// NAME
		coa.put("name", rs.getString("NAME"));
		
		// DESCRIPTION
		coa.put("description", rs.getString("DESCRIPTION"));
		
		//OWNER_ID
		coa.put("ownerId", rs.getString("OWNER_ID"));

		//IS_SYSTEM
		coa.put("system", rs.getInt("IS_SYSTEM") != 0);
		
		//IS_ACTIVE
		coa.put("active", rs.getInt("IS_ACTIVE") != 0);
		
		//IS_MODIFIABLE
		coa.put("modifiable", rs.getInt("IS_MODIFIABLE") != 0);
		
		//lengthG
		coa.put("lengthG", rs.getInt("lengthG"));

		//lengthA
		coa.put("lengthA", rs.getInt("lengthA"));
		
		//MODEL_ID
		coa.put("modelId", rs.getLong("MODEL_ID"));
	}

	public void updateCOA(JSONObject coa) throws AEException, JSONException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			int i = buildCOA(coa, ps, 0);
			ps.setLong(++i, coa.getLong("id"));

			// execute
			ps.executeUpdate();
			
			// set view state
			coa.put("dbState", 0);
		} catch (Exception e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void updateGOA(JSONObject goa) throws AEException, JSONException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQLGOA);

			// build statement
			int i = buildGOA(goa, ps, 0);
			ps.setLong(++i, goa.getLong("id"));

			// execute
			ps.executeUpdate();
			
			// set view state
			goa.put("dbState", 0);
		} catch (Exception e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public JSONObject loadGOA(long ownerId, long sysId) throws AEException, JSONException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			JSONObject jsonGOA = new JSONObject();
			ps = getAEConnection().prepareStatement(selectSQLGOA);
			ps.setLong(1, ownerId);
			ps.setLong(2, sysId);
			rs = ps.executeQuery();
			if(rs.next()) {
				buildGOA(jsonGOA, rs);
				jsonGOA.put("dbState", 0);
			}
			return jsonGOA;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void buildGOA(JSONObject coa, ResultSet rs) throws SQLException, AEException, JSONException {
		// ID
		coa.put("id", rs.getLong("ID"));
		// PARENT_ID
		coa.put("parentId", rs.getLong("PARENT_ID"));
		// MODILE_ID
		coa.put("moduleId", rs.getLong("MODILE_ID"));
		// OWNER_ID
		coa.put("ownerId", rs.getLong("OWNER_ID"));
		// SYS_ID
		coa.put("sysId", rs.getLong("SYS_ID"));
		// CODE
		coa.put("code", rs.getString("CODE"));
		// NAME
		coa.put("name", rs.getString("NAME"));
		// DESCRIPTION
		coa.put("description", rs.getString("DESCRIPTION"));
		// IS_SYSTEM
		coa.put("system", rs.getBoolean("IS_SYSTEM") ? 1 : 0);
		// IS_ACTIVE
		coa.put("active", rs.getBoolean("IS_ACTIVE") ? 1 : 0);
		// IS_READ_ONLY
		coa.put("modifiable", rs.getBoolean("IS_READ_ONLY") ? 0 : 1);
		// DATE_CLOSED
		Date dateClosed = rs.getDate("DATE_CLOSED");
		if(!rs.wasNull()) {
			coa.put("dateClosed", dateClosed);
		}
	}
	
	public void closeGOA(long ownerId, long sysId, Date date) throws AEException, JSONException {
		PreparedStatement ps = null;
		try {
			ps = getAEConnection().prepareStatement(closeSQLGOA);
			
			ps.setDate(1, AEPersistentUtil.getSQLDate(date));
			ps.setLong(2, ownerId);
			ps.setLong(3, sysId);
			
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
//	public Date getDateClosed(long ownerId, long sysId) throws AEException, JSONException {
//		Date dateClosed = null;
//		PreparedStatement ps = null;
//		ResultSet rs = null;
//		try {
//			ps = getAEConnection().prepareStatement(selectSQLGOADateClosed);
//			ps.setLong(1, ownerId);
//			ps.setLong(2, sysId);
//			rs = ps.executeQuery();
//			if(rs.next() && !rs.wasNull()) {
//				dateClosed = rs.getDate("DATE_CLOSED");
//			}
//			return dateClosed;
//		} catch (SQLException e) {
//			e.printStackTrace();
//			throw new AEException(e.getMessage(), e);
//		} finally {
//			AEConnection.close(rs);
//			AEConnection.close(ps);
//			close();
//		}
//	}
}
