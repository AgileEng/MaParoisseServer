package eu.agileeng.persistent.dao.acc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEException;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.AbstractDAO;

public class VATItemDAO extends AbstractDAO {

	private static String insertSQL = "insert into VATRate "
		+ "(CODE, NAME, DESCRIPTION, CATEGORY, RATE, VALID_FROM, VALID_TILL, ACTIVE) "
		+ " values (?, ?, ?, ?, ?, ?, ?, ?)";

	private static String updateSQL = "update VATRate set "
		+ "CODE = ?, NAME = ?, DESCRIPTION = ?, CATEGORY = ?, RATE = ?, VALID_FROM = ?, VALID_TILL = ?, ACTIVE = ?"
		+ " where ID = ?";

	private static String deleteSQL = "delete from VATRate where ID = ?";

	private static String selectSQL = "select * from VATRate";
	
	private static String selectSQLById = "select * from VATRate where ID = ?";
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public VATItemDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	public JSONArray load() throws AEException, JSONException {
		JSONArray vatItems = new JSONArray();
		JSONObject vatItem = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			rs = ps.executeQuery();
			while(rs.next()) {
				vatItem = new JSONObject();
				build(vatItem, rs);
				vatItem.put("dbState", 0);
				vatItems.put(vatItem);
			}
			return vatItems;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public JSONObject load(long id) throws AEException, JSONException {
		JSONObject vatItem = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLById);
			ps.setLong(1, id);
			rs = ps.executeQuery();
			if(rs.next()) {
				vatItem = new JSONObject();
				build(vatItem, rs);
				vatItem.put("dbState", 0);
			}
			return vatItem;
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void insert(JSONObject vatItem) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);
			build(vatItem, ps, 0);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				vatItem.put("id", id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			vatItem.put("dbState", 0);
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

	public void delete(JSONObject vatItem) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQL);

			// build statement
			int i = 1;
			ps.setLong(i++, vatItem.getLong("id"));

			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} catch (JSONException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	public int build(JSONObject vatItem, PreparedStatement ps, int i) throws SQLException, AEException, JSONException {
		//CODE
		if(vatItem.has("code")) {
			ps.setString(++i, vatItem.getString("code"));
		} else {
			ps.setNull(++i, Types.NVARCHAR);
		}
		//NAME
		if(vatItem.has("name")) {
			ps.setString(++i, vatItem.getString("name"));
		} else {
			ps.setNull(++i, Types.NVARCHAR);
		}
		//DESCRIPTION
		if(vatItem.has("description")) {
			ps.setString(++i, vatItem.getString("description"));
		} else {
			ps.setNull(++i, Types.NVARCHAR);
		}
		//CATEGORY
		if(vatItem.has("category")) {
			ps.setString(++i, vatItem.getString("category"));
		} else {
			ps.setNull(++i, Types.NVARCHAR);
		}
		//RATE
		if(vatItem.has("rate")) {
			ps.setDouble(++i, vatItem.getDouble("rate"));
		} else {
			ps.setNull(++i, Types.NVARCHAR);
		}
		//VALID_FROM
		ps.setNull(++i, Types.DATE);
		//VALID_TILL
		ps.setNull(++i, Types.DATE);
		//ACTIVE
		if(vatItem.has("active")) {
			ps.setInt(++i, vatItem.getBoolean("active") ? 1 : 0);
		} else {
			ps.setNull(++i, Types.INTEGER);
		}

		// return the current ps position 
		return i;
	}

	public void build(JSONObject vatItem, ResultSet rs) throws SQLException, AEException, JSONException {
		// ID
		vatItem.put("id", rs.getLong("ID"));
		// CODE
		vatItem.put("code", rs.getString("CODE"));
		// NAME
		vatItem.put("name", rs.getString("NAME"));
		// DESCRIPTION
		vatItem.put("description", rs.getString("DESCRIPTION"));
		// CATEGORY
		vatItem.put("category", rs.getString("CATEGORY"));
		// RATE
		vatItem.put("rate", rs.getDouble("RATE"));
		// VALID_FROM
		// VALID_TILL
		// ACTIVE
		vatItem.put("active", rs.getInt("ACTIVE") != 0);
		
		vatItem.put("option", "(" + vatItem.getString("code") + ") " + vatItem.getDouble("rate") + "%");
	}

	public void update(JSONObject vatItem) throws AEException, JSONException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			int i = build(vatItem, ps, 0);
			ps.setLong(++i, vatItem.getLong("id"));

			// execute
			ps.executeUpdate();
			
			// set view state
			vatItem.put("dbState", 0);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
}
