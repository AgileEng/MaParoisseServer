package eu.agileeng.persistent.dao.council;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.contact.Employee;
import eu.agileeng.domain.council.Council;
import eu.agileeng.domain.council.CouncilMember;
import eu.agileeng.domain.council.CouncilMember.MemberType;
import eu.agileeng.domain.council.CouncilMember.Position;
import eu.agileeng.domain.council.CouncilMembersList;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.util.AEDateUtil;

public class CouncilDAO extends AbstractDAO {

	// INSERT
	private static final String insertSQLCouncil = "INSERT INTO Council (OWNER_ID, START_DATE, END_DATE, CLOSED) VALUES(?, ?, ?, ?)";
	
	private static final String insertSQLCouncilMember = "INSERT INTO CouncilMember (COUNCIL_ID, EMPLOYEE_ID, START_DATE, END_DATE, TYPE_ID, POSITION_ID, ENTRY_DATE"
			+ ", FIRST_ELECTION_DATE, NEXT_RENEWAL_DATE)"
			+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	//UPDATE
	private static final String updateSQLCouncil = "UPDATE Council SET OWNER_ID = ?, START_DATE = ?, END_DATE = ?, CLOSED = ? WHERE ID = ?";
	private static final String updateSQLCouncilMember = "UPDATE CouncilMember SET COUNCIL_ID = ?, EMPLOYEE_ID = ?, START_DATE = ?, END_DATE = ?, TYPE_ID = ?,"
			+ " POSITION_ID = ?, ENTRY_DATE = ?, FIRST_ELECTION_DATE = ?, NEXT_RENEWAL_DATE = ? WHERE ID = ?";
	private static final String updateSQLCouncilMemberPosition = "UPDATE CouncilMember SET POSITION_ID = ? WHERE COUNCIL_ID = ? AND POSITION_ID = ? AND ID <> ? ";
	private static final String closeSQLCouncil = "UPDATE Council SET CLOSED = 1 WHERE OWNER_ID = ? AND ID = ?";
	
	//SELECT
	private static final String selectSQLCouncil = "SELECT * FROM Council WHERE ID = ?";
//	private static final String selectSQLCouncilMember = "SELECT * FROM CouncilMember WHERE ID = ?";
	private static final String selectSQLCouncilMembersList = "SELECT * FROM CouncilMember WHERE COUNCIL_ID = ?";
	private static final String selectSQLCurrentCouncil = "SELECT TOP(1) * FROM Council WHERE OWNER_ID = ? AND CLOSED = 0 ORDER BY ID ASC";
	private static final String selectSQLLastCouncil = "SELECT TOP(1) * FROM Council WHERE OWNER_ID = ? ORDER BY END_DATE DESC";
	
	//DELETE
	private static final String deleteSQLCouncil = "DELETE FROM Council WHERE ID = ?";
	private static final String deleteSQLCouncilMember = "DELETE FROM CouncilMember WHERE ID = ?";
	
	public CouncilDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}
	
	// Engagement
	private static final String selectSQLEngagementByTenant = "SELECT * FROM Engagement WHERE OWNER_ID = ? and year = ?";
	
	private static final String insertSQLEngagement = 
			"Insert into Engagement (OWNER_ID, NAME, AMOUNT_INITIAL, DATE, DURATION, AMOUNT_DUE_YEARLY, AMOUNT_DUE_MONTHLY, YEAR) "
			+ " values (?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String updateSQLEngagement = 
			"update Engagement set NAME = ?, AMOUNT_INITIAL = ?, DATE = ?, DURATION = ?, AMOUNT_DUE_YEARLY = ?, AMOUNT_DUE_MONTHLY = ? "
			+ " where id = ? and OWNER_ID = ? and YEAR = ?";
	
	private static final String deleteSQLEngagement = 
			"delete from Engagement where id = ? and OWNER_ID = ? and YEAR = ?";
	
	// Titre
	private static final String selectSQLTitreByTenant = "SELECT * FROM Titre WHERE OWNER_ID = ? and YEAR = ?";
	
	private static final String insertSQLTitre = 
			"Insert into Titre (OWNER_ID, DESCRIPTION, QTY, PRICE, YEAR) values (?, ?, ?, ?, ?)";
	
	private static final String updateSQLTitre = 
			"update Titre set DESCRIPTION = ?, QTY = ?, PRICE = ? where id = ? and OWNER_ID = ? and YEAR = ?";
	
	private static final String deleteSQLTitre = 
			"delete from Titre where id = ? and OWNER_ID = ? and YEAR = ?";
	
	//BUILD SQL
	
	//build council
	protected int build(Council c, PreparedStatement ps, int i) throws SQLException, AEException {
		//OWNER_ID
		ps.setLong(i++, c.getOwnerId());
		
		//START_DATE
		if (c.getStartDate() == null) {
			ps.setNull(i++, java.sql.Types.DATE);
		} else {
			ps.setDate(i++, AEPersistentUtil.getSQLDate(c.getStartDate()));
		}
		
		//END_DATE
		if (c.getEndDate() == null) {
			ps.setNull(i++, java.sql.Types.DATE);
		} else {
			ps.setDate(i++, AEPersistentUtil.getSQLDate(c.getEndDate()));
		}
		
		//CLOSED
		ps.setBoolean(i++, c.isClosed());
		
		// return the current ps position 
		return i;
	}
	
	//build council member
	protected int build(CouncilMember cm, PreparedStatement ps, int i) throws SQLException, AEException {
		
		//COUNCIL_ID
		ps.setLong(i++, cm.getCouncilId());
		//EMPLOYEE_ID
		ps.setLong(i++, cm.getEmployee().getID());
		//START_DATE
		if (cm.getStartDate() == null) {
			ps.setNull(i++, java.sql.Types.DATE);
		} else {
			Date sqlDate = new Date(cm.getStartDate().getTime());
			ps.setDate(i++, sqlDate);
		}
		//END_DATE
		if (cm.getEndDate() == null) {
			ps.setNull(i++, java.sql.Types.DATE);
		} else {
			Date sqlDate = new Date(cm.getEndDate().getTime());
			ps.setDate(i++, sqlDate);
		}
		//TYPE_ID
		ps.setLong(i++, cm.getTypeId().getId());
		//POSITION_ID
		ps.setLong(i++, cm.getPositionId().getId());
		
		//ENTRY_DATE 
		if (cm.getEntryDate() == null) {
			ps.setNull(i++, java.sql.Types.DATE);
		} else {
			Date sqlDate = new Date(cm.getEntryDate().getTime());
			ps.setDate(i++, sqlDate);
		}
		
		//FIRST_ELECTION_DATE
		if (cm.getFirstElectionDate() == null) {
			ps.setNull(i++, java.sql.Types.DATE);
		} else {
			Date sqlDate = new Date(cm.getFirstElectionDate().getTime());
			ps.setDate(i++, sqlDate);
		}
		
		//NEXT_RENEWAL_DATE
		if (cm.getNextRenewalDate() == null) {
			ps.setNull(i++, java.sql.Types.DATE);
		} else {
			Date sqlDate = new Date(cm.getNextRenewalDate().getTime());
			ps.setDate(i++, sqlDate);
		}
		
		// return the current ps position 
		return i;
	}
	
	//INSERT METHOD
	
	//insert council member
	public void insert(CouncilMember cm) throws AEException {
		assert(!cm.isPersistent());
		PreparedStatement ps = null;
	    ResultSet rs = null;
	    
	    try {
	    	//remove any conflicting council member positions
			if (cm.getPositionId().getId() != CouncilMember.Position.none.getId()) fixConfilctingMemberPosition(cm);
	    	
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLCouncilMember);

			int i = 1;
			build(cm, ps, i);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				
				//required
				cm.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set view state
			cm.setView();
		} catch (SQLException ex) {
			throw new AEException(ex.getMessage(), ex);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	//insert council
	public void insert(Council c) throws AEException {
		assert(!c.isPersistent());
		PreparedStatement ps = null;
	    ResultSet rs = null;
	    
	    try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLCouncil);

			int i = 1;
			build(c, ps, i);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				
				//required
				c.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set view state
			c.setView();
		} catch (SQLException ex) {
			throw new AEException(ex);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	//UPDATE METHODS
	
	//update council member
	public void update(CouncilMember cm) throws AEException {
		assert(cm != null);
		assert(cm.isPersistent());
		PreparedStatement ps = null;
		try {
			//remove any conflicting council member positions
			if (cm.getPositionId().getId() != CouncilMember.Position.none.getId()) {
				fixConfilctingMemberPosition(cm);
			}
			// create statement
			ps = getAEConnection().prepareStatement(updateSQLCouncilMember);

			// build statement
			int i = build(cm, ps, 1);
			ps.setLong(i++, cm.getID());
			
			// execute
			ps.executeUpdate();
			
			// set view
			cm.setView();
		} catch (SQLException ex) {
			throw new AEException(ex.getMessage(), ex);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	private void fixConfilctingMemberPosition(CouncilMember cm) throws AEException {
		assert(cm != null);
		assert(cm.isPersistent());
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQLCouncilMemberPosition);

			// build statement
			int i = 1;
			ps.setLong(i++, CouncilMember.Position.none.getId());
			ps.setLong(i++, cm.getCouncilId());
			ps.setLong(i++, cm.getPositionId().getId());
			ps.setLong(i++, cm.getID());

			// execute
			ps.executeUpdate();
			
			// set view
			cm.setView();
		} catch (SQLException ex) {
			throw new AEException(ex.getMessage(), ex);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	//update council 
		public void update(Council c) throws AEException {
			assert(c != null);
			assert(c.isPersistent());
			PreparedStatement ps = null;
			try {
				// create statement
				ps = getAEConnection().prepareStatement(updateSQLCouncil);

				// build statement
				int i = build(c, ps, 1);
				ps.setLong(i++, c.getID());

				// execute
				ps.executeUpdate();
				
				// set view
				c.setView();
			} catch (SQLException ex) {
				throw new AEException(ex.getMessage(), ex);
			} finally {
				AEConnection.close(ps);
				close();
			}
		}
		
		
	//SELECT METHODS
		
	//load council members 
	public CouncilMembersList loadCMList(long cid) throws AEException {
		CouncilMembersList cml = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCouncilMembersList);
			ps.setLong(1, cid);
			rs = ps.executeQuery();
			
			cml = new CouncilMembersList();
			
			while(rs.next()) {
				CouncilMember cm = new CouncilMember();
				build(cm, rs);
				
				cm.setView();
				
				cml.add(cm);
			}
			
			return cml;
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public Council load(long cid) throws AEException {
		Council c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCouncil);
			ps.setLong(1, cid);
			rs = ps.executeQuery();
			
			
			if(rs.next()) {
				c = new Council();
				build(c, rs);
				
				c.setMembers(loadCMList(c.getID()));
			}
			
			
			return c;
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public Council loadCurrentCouncil(long ownerId) throws AEException {
		Council c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCurrentCouncil);
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			
			
			if(rs.next()) {
				//create a council anyways
				c = new Council();
				
				build(c, rs);
				
				c.setMembers(loadCMList(c.getID()));
			}
			
			
			return c;
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public Council loadLastCouncil(long ownerId) throws AEException {
		Council c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLLastCouncil);
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			
			//create a council anyways
			c = new Council();
			
			if(rs.next()) {
				
				build(c, rs);
				
				c.setMembers(loadCMList(c.getID()));
			} else {
				c.setMembers(new CouncilMembersList());
			}
			
			
			return c;
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static final String selectSQLCouncilDate = 
			"SELECT * FROM Council WHERE OWNER_ID = ? and START_DATE <= ? and ? <= END_DATE";
	private static final String selectSQLLastCouncilDate = 
			"select top 1 * from Council where OWNER_ID = ? and END_DATE <= ? order by END_DATE desc";
	public Council loadCouncil(long ownerId, java.util.Date date) throws AEException {
		Council c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			Date clearSqlDate = AEPersistentUtil.getSQLDate(AEDateUtil.getClearDate(date));
			
			// select the council on specified date
			ps = getAEConnection().prepareStatement(selectSQLCouncilDate);
			ps.setLong(1, ownerId);
			ps.setDate(2, clearSqlDate);
			ps.setDate(3, clearSqlDate);
			rs = ps.executeQuery();
			
			//create a council anyways
			c = new Council();
			
			if(rs.next()) {
				build(c, rs);
				c.setMembers(loadCMList(c.getID()));
			} else {
				// there is no council on specified date
				// try to find the last council before specified date
				
				// first close resources
				AEConnection.close(rs);
				AEConnection.close(ps);
				
				// then select last council 
				ps = getAEConnection().prepareStatement(selectSQLLastCouncilDate);
				ps.setLong(1, ownerId);
				ps.setDate(2, clearSqlDate);
				rs = ps.executeQuery();
				if(rs.next()) {
					build(c, rs);
					c.setMembers(loadCMList(c.getID()));
				} else {
					c.setMembers(new CouncilMembersList());
				}
			}
			
			return c;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void closeCouncil(Council c) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(closeSQLCouncil);

			// build statement
			ps.setLong(1, c.getOwnerId());
			ps.setLong(2, c.getID());

			// execute
			ps.executeUpdate();
			
			
		} catch (SQLException ex) {
			throw new AEException(ex.getMessage(), ex);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void delete(long id) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQLCouncil);
	
			// build statement
			ps.setLong(1, id);
	
			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	
	}
	
	public void deleteMember(long id) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQLCouncilMember);
	
			// build statement
			ps.setLong(1, id);
	
			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	
	}
	
	//OBJECT BUILD METHODS
	
	//build council member
	protected void build(CouncilMember cm, ResultSet rs) throws SQLException, AEException {
		// build common attributes
		super.build(cm, rs);
		
		
		//COUNCIL_ID
		cm.setCouncilId(rs.getLong("COUNCIL_ID"));
		
		// build additional attributes
		DAOFactory daoFactory = DAOFactory.getInstance();
		

		// EMPLOYEE_ID
		long empID = rs.getLong("EMPLOYEE_ID");
		
		cm.setEmployee(daoFactory.getEmployeeDAO(getAEConnection()).loadFull(Employee.lazyDescriptor(empID)));
		
		// START_DATE
		Date sd = rs.getDate("START_DATE");
		if (!rs.wasNull()) {
			cm.setStartDate(new Date(sd.getTime()));
		}
		sd = null;
		
		//END_DATE
		Date ed = rs.getDate("END_DATE");
		if (!rs.wasNull()) {
			cm.setEndDate(new Date(ed.getTime()));
		}
		ed = null;
		
		//TYPE_ID
		cm.setTypeId(MemberType.findById(rs.getLong("TYPE_ID")));
		
		//POSITION_ID
		cm.setPositionId(Position.findById(rs.getLong("POSITION_ID")));
		
		//ENTRY_DATE 
		Date entd = rs.getDate("ENTRY_DATE");
		if (!rs.wasNull()) {
			cm.setEntryDate(new Date(entd.getTime()));
		}
		entd = null;
		
		//FIRST_ELECTION_DATE
		Date fed = rs.getDate("FIRST_ELECTION_DATE");
		if (!rs.wasNull()) {
			cm.setFirstElectionDate(new Date(fed.getTime()));
		}
		fed = null;
		
		//NEXT_RENEWAL_DATE
		Date nrd = rs.getDate("NEXT_RENEWAL_DATE");
		if (!rs.wasNull()) {
			cm.setNextRenewalDate(new Date(nrd.getTime()));
		}
		nrd = null;
		
		// set this record in view state
		cm.setView();
	}
	
	//build council 
	protected void build(Council c, ResultSet rs) throws SQLException, AEException {
		// build common attributes
		super.build(c, rs);
		
		//OWNER_ID
		c.setOwnerId(rs.getLong("OWNER_ID"));
		
		// START_DATE
		c.setStartDate(new Date(rs.getDate("START_DATE").getTime()));
		
		//END_DATE
		c.setEndDate(new Date(rs.getDate("END_DATE").getTime()));
		
		//CLOSED
		c.setClosed(rs.getBoolean("CLOSED"));
		
		// set this record in view state
		c.setView();
	}
	
	public void insertEngagement(JSONObject e) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;   
	    try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLEngagement);

			int i = 1;
			buildEngagementInsert(e, ps, i);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				
				//required
				e.put(AEDomainObject.JSONKey.id.name(), id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set view state
			e.put(AEDomainObject.JSONKey.dbState.name(), AEPersistentUtil.DB_ACTION_NONE);
		} catch (SQLException ex) {
			throw new AEException(ex);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void updateEngagement(JSONObject e) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;   
	    try {
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(updateSQLEngagement);

			buildEngagementUpdate(e, ps, 1);
			
			// execute
			ps.executeUpdate();
			
			// set view state
			e.put(AEDomainObject.JSONKey.dbState.name(), AEPersistentUtil.DB_ACTION_NONE);
		} catch (SQLException ex) {
			throw new AEException(ex);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void deleteEngagement(JSONObject e) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;   
	    try {
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(deleteSQLEngagement);

			ps.setLong(1, e.getLong(AEDomainObject.JSONKey.id.name()));
			ps.setLong(2, e.getLong(AEDomainObject.JSONKey.ownerId.name()));
			ps.setInt(3, e.getInt("year"));
			
			// execute
			ps.executeUpdate();
			
			// set view state
			e.put(AEDomainObject.JSONKey.dbState.name(), AEPersistentUtil.DB_ACTION_NONE);
		} catch (SQLException ex) {
			throw new AEException(ex);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}

	public JSONArray loadEngagements(long tenantId, int year) throws AEException {
		JSONArray eArr = new JSONArray();
		PreparedStatement ps = null;
		ResultSet rs = null;   
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(selectSQLEngagementByTenant);
			ps.setLong(1, tenantId);
			ps.setInt(2, year);
			rs = ps.executeQuery();
			while(rs.next()) {
				JSONObject e = new JSONObject();
				buildEngagement(e, rs);

				// set view state
				e.put(AEDomainObject.JSONKey.dbState.name(), AEPersistentUtil.DB_ACTION_NONE);
				
				// add to array
				eArr.put(e);
			}
			return eArr;
		} catch (SQLException ex) {
			throw new AEException(ex);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	protected int buildEngagementInsert(JSONObject e, PreparedStatement ps, int i) throws SQLException, AEException {
		// OWNER_ID	bigint	Unchecked
		ps.setLong(i++, e.getLong(AEDomainObject.JSONKey.ownerId.name()));
		
		// NAME	nvarchar(64)	Checked
		ps.setString(i++, e.optString(AEDomainObject.JSONKey.name.name()));
		
		// AMOUNT_INITIAL	numeric(20, 10)	Checked
		ps.setDouble(i++, e.optDouble("amountInitial"));
		
		// DATE	date	Checked
		ps.setDate(i++, AEPersistentUtil.getSQLDate(AEDateUtil.parseDate(e.optString("date"))));
		
		// DURATION	bigint	Checked
		ps.setLong(i++, e.optLong("duration"));
		
		// AMOUNT_DUE_YEARLY	numeric(20, 10)	Checked
		ps.setDouble(i++, e.optDouble("amountDueYearly"));
		
		// AMOUNT_DUE_MONTHLY	numeric(20, 10)	Checked
		ps.setDouble(i++, e.optDouble("amountDueMonthly"));
		
		// YEAR
		ps.setInt(i++, e.getInt("year"));
		
		// return the current ps position 
		return i;
	}
	
	protected int buildEngagementUpdate(JSONObject e, PreparedStatement ps, int i) throws SQLException, AEException {
		// NAME	nvarchar(64)	Checked
		ps.setString(i++, e.optString(AEDomainObject.JSONKey.name.name()));
		
		// AMOUNT_INITIAL	numeric(20, 10)	Checked
		ps.setDouble(i++, e.optDouble("amountInitial"));
		
		// DATE	date	Checked
		ps.setDate(i++, AEPersistentUtil.getSQLDate(AEDateUtil.parseDate(e.optString("date"))));
		
		// DURATION	bigint	Checked
		ps.setLong(i++, e.optLong("duration"));
		
		// AMOUNT_DUE_YEARLY	numeric(20, 10)	Checked
		ps.setDouble(i++, e.optDouble("amountDueYearly"));
		
		// AMOUNT_DUE_MONTHLY	numeric(20, 10)	Checked
		ps.setDouble(i++, e.optDouble("amountDueMonthly"));
		
		// where
		// ID
		ps.setLong(i++, e.getLong(AEDomainObject.JSONKey.id.name()));
		
		// OWNER_ID
		ps.setLong(i++, e.getLong(AEDomainObject.JSONKey.ownerId.name()));
		
		// YEAR
		ps.setInt(i++, e.getInt("year"));
		
		// return the current ps position 
		return i;
	}
	
	protected void buildEngagement(JSONObject e, ResultSet rs) throws SQLException, AEException {
		// ID
		e.put(AEDomainObject.JSONKey.id.name(), rs.getLong("ID"));
		
		// OWNER_ID	bigint	Unchecked
		e.put(AEDomainObject.JSONKey.ownerId.name(), rs.getLong("OWNER_ID"));
		
		// NAME	nvarchar(64)	Checked
		e.put(AEDomainObject.JSONKey.name.name(), rs.getString("NAME"));
		
		// AMOUNT_INITIAL	numeric(20, 10)	Checked
		e.put("amountInitial", rs.getDouble("AMOUNT_INITIAL"));
		
		// DATE	date	Checked
		Date d = rs.getDate("DATE");
		if(d != null) {
			e.put("date", AEDateUtil.formatToSystem(d));
		}
		
		// DURATION	bigint	Checked
		e.put("duration", rs.getLong("DURATION"));
		
		// AMOUNT_DUE_YEARLY	numeric(20, 10)	Checked
		e.put("amountDueYearly", rs.getDouble("AMOUNT_DUE_YEARLY"));
		
		// AMOUNT_DUE_MONTHLY	numeric(20, 10)	Checked
		e.put("amountDueMonthly", rs.getDouble("AMOUNT_DUE_MONTHLY"));
		
		// YEAR	int	Unchecked
		e.put("year", rs.getInt("YEAR"));
	}
	
	/////////////////////////////
	
	public void insertTitre(JSONObject t) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;   
	    try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLTitre);

			int i = 1;
			buildTitreInsert(t, ps, i);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				
				//required
				t.put(AEDomainObject.JSONKey.id.name(), id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set view state
			t.put(AEDomainObject.JSONKey.dbState.name(), AEPersistentUtil.DB_ACTION_NONE);
		} catch (SQLException ex) {
			throw new AEException(ex);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void updateTitre(JSONObject t) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;   
	    try {
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(updateSQLTitre);

			buildTitreUpdate(t, ps, 1);
			
			// execute
			ps.executeUpdate();
			
			// set view state
			t.put(AEDomainObject.JSONKey.dbState.name(), AEPersistentUtil.DB_ACTION_NONE);
		} catch (SQLException ex) {
			throw new AEException(ex);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void deleteTitre(JSONObject t) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;   
	    try {
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(deleteSQLTitre);

			ps.setLong(1, t.getLong(AEDomainObject.JSONKey.id.name()));
			ps.setLong(2, t.getLong(AEDomainObject.JSONKey.ownerId.name()));
			ps.setInt(3, t.getInt("year"));
			
			// execute
			ps.executeUpdate();
		} catch (SQLException ex) {
			throw new AEException(ex);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}

	public JSONArray loadTitres(long tenantId, int year) throws AEException {
		JSONArray tArr = new JSONArray();
		PreparedStatement ps = null;
		ResultSet rs = null;   
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(selectSQLTitreByTenant);
			ps.setLong(1, tenantId);
			ps.setInt(2, year);
			rs = ps.executeQuery();
			while(rs.next()) {
				JSONObject t = new JSONObject();
				buildTitre(t, rs);

				// set view state
				t.put(AEDomainObject.JSONKey.dbState.name(), AEPersistentUtil.DB_ACTION_NONE);
				
				// add to array
				tArr.put(t);
			}
			return tArr;
		} catch (SQLException ex) {
			throw new AEException(ex);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	protected int buildTitreInsert(JSONObject t, PreparedStatement ps, int i) throws SQLException, AEException {
		// OWNER_ID	bigint	Unchecked
		ps.setLong(i++, t.getLong(AEDomainObject.JSONKey.ownerId.name()));
		
		// DESCRIPTION	nvarchar(64)	Checked
		ps.setString(i++, t.optString(AEDomainObject.JSONKey.description.name()));
		
		// QTY	bigint	Checked
		ps.setLong(i++, t.optLong("qty"));
		
		// PRICE	numeric(20, 10)	Checked
		ps.setDouble(i++, t.optDouble("price"));
		
		// YEAR
		ps.setInt(i++, t.getInt("year"));
		
		// return the current ps position 
		return i;
	}
	
	protected int buildTitreUpdate(JSONObject t, PreparedStatement ps, int i) throws SQLException, AEException {		
		// DESCRIPTION	nvarchar(64)	Checked
		ps.setString(i++, t.optString(AEDomainObject.JSONKey.description.name()));
		
		// QTY	bigint	Checked
		ps.setLong(i++, t.optLong("qty"));
		
		// PRICE	numeric(20, 10)	Checked
		ps.setDouble(i++, t.optDouble("price"));
		
		// where
		// ID
		ps.setLong(i++, t.getLong(AEDomainObject.JSONKey.id.name()));
		
		// OWNER_ID	bigint	Unchecked
		ps.setLong(i++, t.getLong(AEDomainObject.JSONKey.ownerId.name()));
		
		// YEAR
		ps.setInt(i++, t.getInt("year"));
		
		// return the current ps position 
		return i;
	}
	
	protected void buildTitre(JSONObject t, ResultSet rs) throws SQLException, AEException {
		// ID
		t.put(AEDomainObject.JSONKey.id.name(), rs.getLong("ID"));
		
		// OWNER_ID	bigint	Unchecked
		t.put(AEDomainObject.JSONKey.ownerId.name(), rs.getLong("OWNER_ID"));
		
		// DESCRIPTION	nvarchar(64)	Checked
		t.put(AEDomainObject.JSONKey.description.name(), rs.getString("DESCRIPTION"));
		
		// QTY	bigint	Checked
		t.put("qty", rs.getLong("QTY"));
		
		// PRICE	numeric(20, 10)	Checked
		t.put("price", rs.getDouble("PRICE"));
		
		// YEAR	int	Unchecked
		t.put("year", rs.getInt("YEAR"));
	}
	
	
	private static final String selectSQLCouncilByTenant = "SELECT id FROM Council WHERE ID = ? and owner_id = ?";
	public boolean validateCouncil(AEDescriptor councilDescr, AEDescriptor tenantDescr) throws AEException {
		boolean isValid = false;
		PreparedStatement ps = null;
		ResultSet rs = null;   
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(selectSQLCouncilByTenant);
			ps.setLong(1, councilDescr.getID());
			ps.setLong(2, tenantDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				isValid = true;
			}
			return isValid;
		} catch (SQLException ex) {
			throw new AEException(ex);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}

	}
	
	private static final String selectSQLCouncilMemberByCouncil = "select id from CouncilMember where ID = ? and COUNCIL_ID = ?";
	public boolean validateCouncilMember(AEDescriptor councilMemberDescr, AEDescriptor councilDescr) throws AEException {
		boolean isValid = false;
		PreparedStatement ps = null;
		ResultSet rs = null;   
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(selectSQLCouncilMemberByCouncil);
			ps.setLong(1, councilMemberDescr.getID());
			ps.setLong(2, councilDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				isValid = true;
			}
			return isValid;
		} catch (SQLException ex) {
			throw new AEException(ex);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}

	}
	
	//insert council
	public void updateCouncilEndDate(AEDescriptor tenantDescr, java.util.Date endDate) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
	    
	    try {
	    	// Keep the ID of the "current" Council
	    	long currentId = 0;
	    	ps = getAEConnection().prepareStatement(
	    			"select id from council where owner_id = ? and CLOSED = 0");
	    	ps.setLong(1, tenantDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				currentId = rs.getLong("id");
			} else {
				throw new AEException("There is no default council!");
			}
			AEConnection.close(ps);
			AEConnection.close(rs);

			// Find the "candidate" Council. This is the last Council before endDate;
			long candidateId = 0;
			String sql = "SELECT ID FROM Council where ? <= start_date and owner_id = ?";
			ps = getAEConnection().prepareStatement(
					"select id from Council where "
					+ " end_date = (select max(end_date) from Council where id not in ( " + sql + " ) and owner_id = ?) "
					+ " and owner_id = ? order by id desc");
			ps.setDate(1, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(endDate)));
			ps.setLong(2, tenantDescr.getID());
			ps.setLong(3, tenantDescr.getID());
			ps.setLong(4, tenantDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				candidateId = rs.getLong("id");
			} else {
				candidateId = currentId;
			}
			AEConnection.close(ps);
			AEConnection.close(rs);
			
			// Transfer the members from "current" to "candidate"
			if(currentId != candidateId) {
				// Delete members of the "candidate".
				ps = getAEConnection().prepareStatement("delete from CouncilMember where council_id = ?");
				ps.setLong(1, candidateId);
				ps.executeUpdate();
				AEConnection.close(ps);
				AEConnection.close(rs);

				// Transfer the members from "current" to "candidate"
				ps = getAEConnection().prepareStatement("update CouncilMember set COUNCIL_ID = ? where council_id = ?");
				ps.setLong(1, candidateId);
				ps.setLong(2, currentId);
				ps.executeUpdate();
				AEConnection.close(ps);
				AEConnection.close(rs);
			}
			
			// delete unnecessary councils
			String sqlUnnecessaryCouncils = "SELECT ID FROM Council where ? <= start_date and owner_id = ? and id != ?";
	    	ps = getAEConnection().prepareStatement(sqlUnnecessaryCouncils);
			ps.setDate(1, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(endDate)));
			ps.setLong(2, tenantDescr.getID());
			ps.setLong(3, candidateId);
			rs = ps.executeQuery();
			boolean haveUnnecessaryCouncils = rs.next();
			AEConnection.close(ps);
			AEConnection.close(rs);
			if(haveUnnecessaryCouncils) {
				// Delete councils after the date
				ps = getAEConnection().prepareStatement(
						"delete from CouncilMember where council_id in (" + sqlUnnecessaryCouncils + ")");
				ps.setDate(1, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(endDate)));
				ps.setLong(2, tenantDescr.getID());
				ps.setLong(3, candidateId);
				ps.executeUpdate();
				AEConnection.close(ps);
				AEConnection.close(rs);

				ps = getAEConnection().prepareStatement(
						"delete from Council where id in (" + sqlUnnecessaryCouncils + ")");
				ps.setDate(1, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(endDate)));
				ps.setLong(2, tenantDescr.getID());
				ps.setLong(3, candidateId);
				ps.executeUpdate();
				AEConnection.close(ps);
				AEConnection.close(rs);
			}
			
			// Set-up candidate as current
			ps = getAEConnection().prepareStatement(
					"update council set CLOSED = 0 where id = ?");
			ps.setLong(1, candidateId);
			ps.executeUpdate();
			AEConnection.close(ps);
			AEConnection.close(rs);

			ps = getAEConnection().prepareStatement(
					"update council set END_DATE = ? where id = ?");
			ps.setDate(1, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(endDate)));
			ps.setLong(2, candidateId);
			ps.executeUpdate();
			AEConnection.close(ps);
			AEConnection.close(rs);
			
			if(currentId == candidateId) {
				ps = getAEConnection().prepareStatement(
						"update council set START_DATE = ? where id = ?");
				java.util.Date startDate = AEDateUtil.getClearDateTime(
						AEDateUtil.addDaysToDate(AEDateUtil.addYearsToDate(endDate, -1), -1));
				ps.setDate(1, AEPersistentUtil.getSQLDate(startDate));
				ps.setLong(2, candidateId);
				ps.executeUpdate();
				AEConnection.close(ps);
				AEConnection.close(rs);
			}

			// that is all
		} catch (SQLException ex) {
			throw new AEException(ex);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
}
