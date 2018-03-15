/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 22.11.2009 14:18:56
 */
package eu.agileeng.persistent.dao.oracle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDescriptorsList;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.contact.OrganizationDescriptor;
import eu.agileeng.domain.contact.PartiesList;
import eu.agileeng.domain.contact.Party;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.util.AEStringUtil;


/**
 *
 */
public abstract class PartyDAO extends AbstractDAO {

	private static String insertSQL = "insert into party (UIN, NAME, CODE, TIME_CREATED, CREATOR, "
		+ "TIME_MODIFIED, MODIFIER, PROPERTIES, CLASS_ID, DESCRIPTION, NOTE, ID_Parent, OWNER_ID, "
		+ "IS_TEMPLATE, IS_ACTIVE) values ("
		+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static String updateSQL = "update party set UIN = ?, NAME = ?, CODE = ?, TIME_CREATED = ?, CREATOR = ?, "
		+ " TIME_MODIFIED = ?, MODIFIER = ?, PROPERTIES = ?, CLASS_ID = ?, DESCRIPTION = ?, NOTE = ?, ID_Parent = ?, OWNER_ID = ?, "
		+ " IS_TEMPLATE = ?, IS_ACTIVE = ? "
		+ " where id = ?";
	
	private static String deleteSQL = "delete from Party where id = ?";
	
	private static String deactivateSQL = 
		"update party set IS_ACTIVE = 0 where id = ?";
	
	private static String activateSQL = 
			"update party set IS_ACTIVE = 1 where id = ?";
	
	private static String selectSQLAllDescriptors = 
		"select id, class_id, name, properties from party where CLASS_ID = ? order by name asc";
	
	private static String selectSQLDescriptor = 
		"select id, class_id, name, properties from party where id = ?";
	
	private ContactDAO contactDAO = null;
	
	private AddressDAO addressDAO = null;
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	PartyDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}
	
	protected void build(Party p, ResultSet rs) throws SQLException, AEException {
		// build common attributes
		super.build(p, rs);
		
		// build additional attributes
		// UIN
		p.setUIN(rs.getString("UIN"));
		// ID_Parent
		long parentID = rs.getLong("ID_Parent");
		if(!rs.wasNull()) {
			p.setParent(load(new AEDescriptorImp(parentID, p.getClazz())));
		}
		//IS_TEMPLATE
		p.setTemplate(rs.getBoolean("IS_TEMPLATE"));
		//IS_ACTIVE
		p.setActive(rs.getBoolean("IS_ACTIVE"));
		
		// set this record in view state
		p.setView();
	}
	
	protected void build(AEDescriptor p, ResultSet rs) throws SQLException, AEException {
		// ID
		p.setID(rs.getLong("ID"));
		
		// CODE
		p.setCode(rs.getString("CODE"));
		
		// NAME
		p.setName(rs.getString("NAME"));
		
		// DESCRIPTION
		p.setDescription(rs.getString("DESCRIPTION"));
		
		// PROPERTIES
		p.setProperties(rs.getLong("PROPERTIES"));
		
		// IS_ACTIVE 
		// only in 
		if(p instanceof OrganizationDescriptor) {
			OrganizationDescriptor od = (OrganizationDescriptor) p;
			try {
				od.setActive(rs.getBoolean("IS_ACTIVE"));
			} catch (Exception e) {
			}
		}
	}
	
	private void loadContactsList(Party party) throws AEException {
		try {
			if(contactDAO == null) {
				DAOFactory daoFactory = DAOFactory.getInstance();
				contactDAO = daoFactory.getContactDAO(getAEConnection());
			}
			party.setContactsList(contactDAO.load(party.getDescriptor()));
		} finally {
			close();
		}
	}
	
	private void loadAddressesList(Party party) throws AEException {
		try {
			if(addressDAO == null) {
				DAOFactory daoFactory = DAOFactory.getInstance();
				addressDAO = daoFactory.getAddressDAO(getAEConnection());
			}
			party.setAddressesList(addressDAO.load(party.getDescriptor()));
		} finally {
			close();
		}
	}
	
	protected void postLoad(Party party) throws AEException {
		try {
			loadContactsList(party);
			loadAddressesList(party);
		} finally {
			close();
		}
		
	}
	
	protected void insert(Party party) throws AEException {
		assert(!party.isPersistent());
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);

			int i = 1;
			build(party, ps, i);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				
				//required
				party.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// IMPORTANT: 
			// 1. don't set specified "party" to "view"
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	protected int build(Party party, PreparedStatement ps, int i) throws SQLException, AEException {
		// UIN
		ps.setString(i++, party.getUIN());
		// NAME
		ps.setString(i++, party.getName());
		// CODE
		ps.setString(i++, party.getCode());
		// TIME_CREATED
		ps.setTimestamp(i++, AEPersistentUtil.getTimestampNotNull(party.getTimeCreated()));
		// CREATOR
		ps.setString(i++, party.getCreator());
		// TIME_MODIFIED
		ps.setTimestamp(i++, AEPersistentUtil.getTimestampNotNull(party.getTimeModified()));
		// MODIFIER
		ps.setString(i++, party.getModifier());
		// PROPERTIES
		ps.setLong(i++, party.getProperties());
		// CLASS_ID
		if(party.getClazz() != null) {
			ps.setLong(i++, party.getClazz().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		// DESCRIPTION
		ps.setString(i++, party.getDescription());
		// NOTE
		ps.setString(i++, party.getNote());
		// ID_PARENT
		if(party.getParent() != null) {
			ps.setLong(i++, party.getParent().getDescriptor().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		// OWNER_ID
		if(party.getCompany() != null) {
			ps.setLong(i++, party.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		//IS_TEMPLATE
		ps.setBoolean(i++, party.isTemplate());
		//IS_ACTIVE
		ps.setBoolean(i++, party.isActive());
		
		// return the current ps position 
		return i;
	}
	
	protected void update(Party party) throws AEException {
		assert(party != null);
		assert(party.isPersistent());
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			int i = build(party, ps, 1);
			ps.setLong(i++, party.getID());

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
	
	public abstract Party load(AEDescriptor partyDescr) throws AEException;
	
	public abstract AEDescriptive loadDescriptive(long id) throws AEException;
	
	public abstract PartiesList loadAll() throws AEException;
	
	public final AEDescriptorsList loadAllDescriptors(DomainClass clazz) throws AEException {
		AEDescriptorsList descrList = new AEDescriptorsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAllDescriptors);
			ps.setLong(1, clazz.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				AEDescriptor descr = new AEDescriptorImp();

				//id
				descr.setID(rs.getLong("id"));
				//class_id
				descr.setClazz(DomainClass.valueOf(rs.getLong("class_id")));
				//name
				descr.setName(rs.getString("name"));
				//properties
				descr.setProperties(rs.getLong("properties"));

				descrList.add(descr);
			}
			return descrList;
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
	 * 
	 * @param partyId
	 * @return NULL if Party with specified partyId doesn't exist
	 * @throws AEException
	 */
	public final AEDescriptor loadDescriptor(long partyId) throws AEException {
		AEDescriptor partyDescr = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLDescriptor);
			ps.setLong(1, partyId);
			rs = ps.executeQuery();
			if(rs.next()) {
				partyDescr = new AEDescriptorImp();
				//id
				partyDescr.setID(rs.getLong("id"));
				//class_id
				partyDescr.setClazz(DomainClass.valueOf(rs.getLong("class_id")));
				//name
				partyDescr.setName(rs.getString("name"));
				//properties
				partyDescr.setProperties(rs.getLong("properties"));
			}
			return partyDescr;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	protected void insert(JSONObject party) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			assert(party.getLong("id") <= 0);
			
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);

			int i = 1;
			build(party, ps, i);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				
				//required
				party.put("id", id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// IMPORTANT: 
			// 1. don't set specified "party" to "view"
		} catch (Exception e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public abstract PartiesList loadToPrincipal(AEDescriptor principalDescr) throws AEException;
	
	public abstract AEDescriptorsList loadToPrincipalDescriptor(AEDescriptor principalDescr) throws AEException;
	
	private int build(JSONObject party, PreparedStatement ps, int i) throws SQLException, AEException, JSONException {
		// UIN
		ps.setString(i++, AEStringUtil.EMPTY_STRING);
		// NAME
		psSetString(ps, i++, party, "name");
		// CODE
		psSetString(ps, i++, party, "code");
		// TIME_CREATED
		ps.setTimestamp(i++, AEPersistentUtil.getTimestampNotNull(new Date()));
		// CREATOR
		ps.setString(i++, AEStringUtil.EMPTY_STRING);
		// TIME_MODIFIED
		ps.setTimestamp(i++, AEPersistentUtil.getTimestampNotNull(new Date()));
		// MODIFIER
		ps.setString(i++, AEStringUtil.EMPTY_STRING);
		// PROPERTIES
		psSetLong(ps, i++, party, "properties");
		// CLASS_ID
		ps.setLong(i++, DomainClass.ORGANIZATION.getID());
		// DESCRIPTION
		psSetString(ps, i++, party, "description");
		// NOTE
		psSetString(ps, i++, party, "note");
		// ID_PARENT
		ps.setNull(i++, Types.BIGINT);
		// OWNER_ID
		if(party.has("ownerId") && party.optLong("ownerId") > 0) {
			ps.setLong(i++, party.optLong("ownerId"));
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		//IS_TEMPLATE
		ps.setBoolean(i++, party.optBoolean("template"));
		//IS_ACTIVE
		ps.setBoolean(i++, party.optBoolean("active"));
		
		// return the current ps position 
		return i;
	}
	
	protected void update(JSONObject party) throws AEException, JSONException {
		assert(party != null);
		PreparedStatement ps = null;
		try {
			assert(party.getLong("id") > 0);
			
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			int i = build(party, ps, 1);
			ps.setLong(i++, party.getLong("id"));

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
	
	protected void delete(AEDescriptor partyDescr) throws AEException, JSONException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQL);

			// build statement
			ps.setLong(1, partyDescr.getID());

			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	protected void deactivate(AEDescriptor partyDescr) throws AEException, JSONException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deactivateSQL);

			// build statement
			ps.setLong(1, partyDescr.getID());

			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	protected void activate(AEDescriptor partyDescr) throws AEException, JSONException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(activateSQL);

			// build statement
			ps.setLong(1, partyDescr.getID());

			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
}
