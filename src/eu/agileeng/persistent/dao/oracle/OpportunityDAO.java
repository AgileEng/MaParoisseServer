/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 22.11.2009 15:22:48
 */
package eu.agileeng.persistent.dao.oracle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.avi.OpportunitiesList;
import eu.agileeng.domain.avi.Opportunity;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.contact.Person;
import eu.agileeng.domain.document.AEDocumentFilter;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.oracle.SequenceDAO.Sequence;


/**
 *
 */
public class OpportunityDAO extends AbstractDAO {
	private static String selectSQL = "select * from Opportunity ";
	
	private static String selectSQL1 = "select * from Opportunity where id = ?";
	
	private static String insertSQL = "insert into OPPORTUNITY (ID, CODE, NAME, CREATOR, TIME_CREATED, MODIFIER, "
		+ "TIME_MODIFIED, PROPERTIES, CUSTOMER_ID, REPONSIBLE_PERSON_ID, PHASE_ID, STATUS_ID, "
		+ "TASK_CATEGORY_ID, NEXT_ACTIVITY_TIME, NOTE) values ("
		+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static String updateSQL = 
		"update OPPORTUNITY set "
		+ "NEXT_ACTIVITY_TIME = ?, NOTE = ?, TASK_CATEGORY_ID = ? "
		+ " where id = ?";

	private static String selectSQL2 = "select * from Opportunity where NEXT_ACTIVITY_TIME >= ? and NEXT_ACTIVITY_TIME <= ?";
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	OpportunityDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	public OpportunitiesList load(AEDescriptor opportDescr) throws AEException {
		OpportunitiesList opportList = new OpportunitiesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL1);
			ps.setLong(1, opportDescr.getID());
			rs = ps.executeQuery(selectSQL);
			while(rs.next()) {
				Opportunity opport = new Opportunity();
				build(opport, rs);
				opportList.add(opport);
			}
			return opportList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public OpportunitiesList load() throws AEException {
		OpportunitiesList opportList = new OpportunitiesList();
		Statement s = null;
		ResultSet rs = null;
		try {
			s = getAEConnection().createStatement();
			rs = s.executeQuery(selectSQL);
			while(rs.next()) {
				Opportunity opport = new Opportunity();
				build(opport, rs);
				opportList.add(opport);
			}
			return opportList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(s);
			close();
		}
	}
	
	public OpportunitiesList load(AEDocumentFilter filter) throws AEException {
		OpportunitiesList opportList = new OpportunitiesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL2);
			ps.setDate(1, AEPersistentUtil.getSQLDate(filter.getDateFrom()));
			ps.setDate(2, AEPersistentUtil.getSQLDate(filter.getDateTo()));
			rs = ps.executeQuery();
			while(rs.next()) {
				Opportunity opport = new Opportunity();
				build(opport, rs);
				opportList.add(opport);
			}
			return opportList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public void build(Opportunity o, ResultSet rs) throws SQLException {
		// build common attributes
		super.build(o, rs);

		DAOFactory daoFactory = DAOFactory.getInstance();
		
		// build additional attributes
		// CUSTOMER_ID
		try {
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(getAEConnection());
			o.setCustomer((Organization) orgDAO.load(
					new AEDescriptorImp(rs.getLong("CUSTOMER_ID"), DomainClass.ORGANIZATION)));
		} catch (AEException e) {
			e.printStackTrace();
		}
		// REPONSIBLE_PERSON_ID
		try {
			PersonDAO personDAO = daoFactory.getPersonDAO(getAEConnection());
			o.setResponsiblePerson((Person) personDAO.load(
					new AEDescriptorImp(rs.getLong("REPONSIBLE_PERSON_ID"), DomainClass.PERSON)));
		} catch (AEException e) {
			e.printStackTrace();
		}
		// PHASE_ID
		// STATUS_ID
		// TASK_CATEGORY_ID
//		o.getTask().setTaskCategory(
//				(TaskCategory) AEServiceLocator.getInstance()
//					.getCacheService().getEnumeratedType(Long.valueOf(rs.getLong("TASK_CATEGORY_ID"))));
		// NEXT_ACTIVITY_TIME
		o.getTask().setTimeNextActivity(rs.getDate("NEXT_ACTIVITY_TIME"));
		
		// set this record in view state
		o.setView();
	}
	
	public void insert(OpportunitiesList oppList) throws AEException {
		assert(oppList != null);
		PreparedStatement ps = null;
		try {
			// get ids
			SequenceDAO seqDAO = DAOFactory.getInstance().getSequenceDAO(getAEConnection());
			for (Opportunity opport : oppList) {
				assert(!opport.isPersistent()); 
				opport.setID(seqDAO.nextValue(Sequence.COMMON));
			}
			
			// insert
			ps = getAEConnection().prepareStatement(insertSQL);
			for (Opportunity opport : oppList) {
				assert(!opport.isPersistent()); 
				assert(opport.getClazz() != null);

				// build prepared statement
				build(ps, opport);
				
				// execute
				ps.executeUpdate();
				
				// set view state
				opport.setView();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void update(OpportunitiesList oppList) throws AEException {
		assert(oppList != null);
		PreparedStatement ps = null;
		try {
			// update
			ps = getAEConnection().prepareStatement(updateSQL);
			for (Opportunity opport : oppList) {
				assert(!opport.isPersistent()); 

				// build prepared statement
				int i = 1;
				ps.setTimestamp(i++, AEPersistentUtil.getTimestampNotNull(opport.getTask().getTimeNextActivity()));
				ps.setString(i++, opport.getNote());
				if(opport.getTask() != null && opport.getTask().getTaskCategory() != null) {
					ps.setLong(i++, opport.getTask().getTaskCategory().getID());
				} else {
					ps.setNull(i++, Types.BIGINT);
				}
				ps.setLong(i++, opport.getID());
				
				// execute
				ps.executeUpdate();
				
				// set view state
				opport.setView();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	public void delete(OpportunitiesList oppList) throws AEException {
		assert(oppList != null);
		PreparedStatement ps = null;
		try {
			// update
			ps = getAEConnection().prepareStatement(insertSQL);
			for (Opportunity opport : oppList) {
				assert(!opport.isPersistent()); 

				// build prepared statement
//				build(ps, contact);
				
				// execute
				ps.executeUpdate();
				
				// set view state
				opport.setView();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	private void build(PreparedStatement ps, Opportunity opport) throws SQLException {
		int i = 1;
		
		//ID
		ps.setLong(i++, opport.getID());
		// CODE
		ps.setString(i++, opport.getCode());
		// NAME
		ps.setString(i++, opport.getName());
		// CREATOR
		ps.setString(i++, opport.getCreator());
		// TIME_CREATED
		ps.setTimestamp(i++, AEPersistentUtil.getTimestampNotNull(opport.getTimeCreated()));
		// MODIFIER
		ps.setString(i++, opport.getModifier());
		// TIME_MODIFIED
		ps.setTimestamp(i++, AEPersistentUtil.getTimestampNotNull(opport.getTimeModified()));
		// PROPERTIES
		ps.setLong(i++, opport.getProperties());
		//CUSTOMER_ID
		if(opport.getCustomer() != null) {
			ps.setLong(i++, opport.getCustomer().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		//REPONSIBLE_PERSON_ID
		if(opport.getResponsiblePerson() != null) {
			ps.setLong(i++, opport.getResponsiblePerson().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		//PHASE_ID
		if(opport.getPhase() != null) {
			ps.setLong(i++, opport.getPhase().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		//STATUS_ID
		if(opport.getStatus() != null) {
			ps.setLong(i++, opport.getStatus().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		//TASK_CATEGORY_ID
		if(opport.getTask() != null && opport.getTask().getTaskCategory() != null) {
			ps.setLong(i++, opport.getTask().getTaskCategory().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		//NEXT_ACTIVITY_TIME
		ps.setDate(i++, AEPersistentUtil.getSQLDate(opport.getTask().getTimeNextActivity()));
		//NOTE
		ps.setString(i++, opport.getNote());
	}
}
