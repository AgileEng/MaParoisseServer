/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 17.11.2009 16:26:52
 */
package eu.agileeng.persistent.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;
import org.jboss.logging.Logger;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.contact.Party;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistent;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.oracle.OrganizationDAO;
import eu.agileeng.util.AEDateUtil;


/**
 *
 */
public abstract class AbstractDAO {

	protected static Logger logger = Logger.getLogger(AbstractDAO.class);
	
	private AEConnection aEConnection;
	
	/**
	 * 
	 */
	protected AbstractDAO(AEConnection aeConnection) throws AEException {
		assert(aeConnection != null) : "aeConnection cannot be null";
		this.aEConnection = aeConnection;
	}
	
	protected void close() {
		AEConnection.close(aEConnection);
		if(aEConnection.isConnectionOwner()) {
			aEConnection = null;
		}
	}

	/**
	 * @return the aEConnection
	 */
	protected final AEConnection getAEConnection() {
		if(aEConnection == null) {
			try {
				aEConnection = DAOFactory.getInstance().getConnection();
			} catch (AEException e) {
				e.printStackTrace();
			}
		}
		return aEConnection;
	}
	
	protected void build(AEDomainObject c, ResultSet rs) throws SQLException {
	    // Get result set meta data
        ResultSetMetaData rsmd = rs.getMetaData();
        int numColumns = rsmd.getColumnCount();
    
        // Get the column names: column indices start from 1
        for (int i = 1; i < (numColumns + 1); i++) {
            String columnName = rsmd.getColumnName(i);
            if("ID".equalsIgnoreCase(columnName)) {
            	c.setID(rs.getLong(i));
            } else if("CLASS_ID".equalsIgnoreCase(columnName)) {
            	c.setClazz(DomainClass.valueOf(rs.getLong(i)));
            } else if("CODE".equalsIgnoreCase(columnName)) {
            	c.setCode(rs.getString(i));
            } else if("NAME".equalsIgnoreCase(columnName)) {
            	c.setName(rs.getString(i));
            } else if("TIME_CREATED".equalsIgnoreCase(columnName)) {
            	c.setTimeCreated(rs.getTimestamp(i));
            } else if("CREATOR".equalsIgnoreCase(columnName)) {
            	c.setCreator(rs.getString(i));
            } else if("TIME_MODIFIED".equalsIgnoreCase(columnName)) {
            	c.setTimeModified(rs.getTimestamp(i));
            } else if("MODIFIER".equalsIgnoreCase(columnName)) {
            	c.setModifier(rs.getString(i));
            } else if("PROPERTIES".equalsIgnoreCase(columnName)) {
            	c.setProperties(rs.getLong(i));
            } else if("DESCRIPTION".equalsIgnoreCase(columnName)) {
            	c.setDescription(rs.getString(i));
            } else if("NOTE".equalsIgnoreCase(columnName)) {
            	c.setNote(rs.getString(i));
            } else if("OWNER_ID".equalsIgnoreCase(columnName)) {
            	long compID = rs.getLong("OWNER_ID");
            	try {
					OrganizationDAO orgDAO = DAOFactory.getInstance().getOrganizationDAO(getAEConnection());
					Party org = orgDAO.load(new AEDescriptorImp(compID, DomainClass.ORGANIZATION));
					if(org != null) {
						c.setCompany(org.getDescriptor());
					}
				} catch (AEException e) {
				}
             } else if("ID_COMPANY".equalsIgnoreCase(columnName)) {
            	long compID = rs.getLong("ID_COMPANY");
            	try {
					OrganizationDAO orgDAO = DAOFactory.getInstance().getOrganizationDAO(getAEConnection());
					Party org = orgDAO.load(new AEDescriptorImp(compID, DomainClass.ORGANIZATION));
					if(org != null) {
						c.setCompany(org.getDescriptor());
					}
				} catch (AEException e) {
				}
             } 
        }	
	}
	
	protected void psSetString(PreparedStatement ps, int i, JSONObject json, String field) throws SQLException, JSONException {
		if(json.has(field)) {
			try {
				ps.setString(i, json.getString(field));
			} catch(JSONException e) {
				ps.setNull(i, java.sql.Types.NVARCHAR);
			}
		} else {
			ps.setNull(i, java.sql.Types.NVARCHAR);
		}
	}
	
	protected void psSetLong(PreparedStatement ps, int i, JSONObject json, String field) throws SQLException, JSONException {
		if(json.has(field)) {
			try {
				ps.setLong(i, json.getLong(field));
			} catch(JSONException e) {
				ps.setNull(i, java.sql.Types.BIGINT);
			}
		} else {
			ps.setNull(i, java.sql.Types.BIGINT);
		}
	}
	
	/**
	 * Gets specified <code>field</code> from specified <code>json</code> as Long.
	 * Missing <code>field</code> or Long value <= 0 will be treated as <code>NULL</code>.
	 * 
	 * @param ps
	 * @param i
	 * @param json
	 * @param field
	 * @throws SQLException
	 * @throws JSONException
	 */
	protected void psSetId(PreparedStatement ps, int i, JSONObject json, String field) throws SQLException, JSONException {
		if(json.has(field)) {
			try {
				long id = json.getLong(field);
				if(AEPersistent.ID.isPersistent(id)) {
					ps.setLong(i, id);
				} else {
					ps.setNull(i, java.sql.Types.BIGINT);
				}
			} catch(JSONException e) {
				ps.setNull(i, java.sql.Types.BIGINT);
			}
		} else {
			ps.setNull(i, java.sql.Types.BIGINT);
		}
	}
	
	protected void psSetDate(PreparedStatement ps, int i, JSONObject json, String field) throws SQLException, JSONException {
		if(json.has(field)) {
			ps.setDate(i, AEPersistentUtil.getSQLDate(AEDateUtil.parseDateStrict(json.getString(field))));
		} else {
			ps.setNull(i, java.sql.Types.DATE);
		}
	}
	
	protected void psSetInt(PreparedStatement ps, int i, JSONObject json, String field) throws SQLException, JSONException {
		if(json.has(field)) {
			ps.setInt(i, json.getInt(field));
		} else {
			ps.setNull(i, java.sql.Types.INTEGER);
		}
	}
	
	protected void setDBActionNone(JSONObject jsonObj) throws JSONException {
		jsonObj.put("dbState", (int) 0);
	}
	
	protected void psSetBoolean(PreparedStatement ps, int i, JSONObject json, String field) throws SQLException, JSONException {
		if(json.has(field)) {
			ps.setBoolean(i, json.getBoolean(field));
		} else {
			ps.setNull(i, java.sql.Types.BOOLEAN);
		}
	}
}
