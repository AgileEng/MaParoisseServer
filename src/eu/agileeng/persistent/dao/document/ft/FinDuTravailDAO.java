/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 23.05.2010 10:48:37
 */
package eu.agileeng.persistent.dao.document.ft;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.contact.Employee;
import eu.agileeng.domain.document.AEDocument;
import eu.agileeng.domain.document.AEDocumentFilter;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.AEDocumentsList;
import eu.agileeng.domain.document.social.AESocialDocumentFilter;
import eu.agileeng.domain.document.social.findutravail.FinDuTravail;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.document.AEDocumentDAO;
import eu.agileeng.persistent.dao.oracle.EmployeeDAO;
import eu.agileeng.util.AEStringUtil;

/**
 *
 */
public class FinDuTravailDAO extends AEDocumentDAO {
	private static String selectSQL = 
		"select document.*, Social_FinDuTravail.* from Social_FinDuTravail "
		+ " inner join document on Social_FinDuTravail.id = document.id"
		+ " where Social_FinDuTravail.id = ?";

	private static String insertSQL = "insert into Social_FinDuTravail "
		+ " (ID, EmployeeID, EmployeePositionID, EmployeePositionName, "
		+ " SupervisorID, SupervisorName, DateEntry, DateRelease, "
		+ " Student, ReasonID, ReasonDescr, StillPresent) "
		+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static String updateSQL = "update Social_FinDuTravail set "
		+ " ID = ?, EmployeeID = ?, EmployeePositionID = ?, EmployeePositionName = ?, "
		+ " SupervisorID = ?, SupervisorName = ?, DateEntry = ?, DateRelease = ?, "
		+ " Student = ?, ReasonID = ?, ReasonDescr  = ?, StillPresent = ? "
		+ " where ID = ?";
	
	/*
	 * Keep synchronised: selectSQL2, selectSQLCountAll, selectSQLCountNotValidated
	 */
	private static String selectSQL2 = 	
		"select Social_FinDuTravail.*, document.* "
		+ " from Social_FinDuTravail inner join document on Social_FinDuTravail.id = document.id "
		+ " inner join Employee on Social_FinDuTravail.EmployeeID = Employee.ID "
		+ " where document.owner_id = ? and document.type_id= ?";
	
	/*
	 * Keep synchronised: selectSQL2, selectSQLCountAll, selectSQLCountNotValidated
	 */
	private static String selectSQLCountAll = 	
		"select count(Document.ID) as count "
		+ " from Social_FinDuTravail inner join document on Social_FinDuTravail.id = document.id "
		+ " inner join Employee on Social_FinDuTravail.EmployeeID = Employee.ID "
		+ " where document.owner_id = ? and document.type_id= ?";
	
	/*
	 * Keep synchronised: selectSQL2, selectSQLCountAll, selectSQLCountNotValidated
	 */
	private static String selectSQLCountValidated = 	
		"select count(Document.ID) as count "
		+ " from Social_FinDuTravail inner join document on Social_FinDuTravail.id = document.id "
		+ " inner join Employee on Social_FinDuTravail.EmployeeID = Employee.ID "
		+ " where document.owner_id = ? and document.type_id= ? and document.VALIDATED = 1";
	
	private static String selectEmployeeIdSQL = 
		"select EmployeeID from Social_FinDuTravail where id = ?";

	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public FinDuTravailDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	@Override
	public AEDocument load(AEDescriptor docDescr) throws AEException {
		FinDuTravail doc = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, docDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				doc = new FinDuTravail();
				build(doc, rs);
			}
			return doc;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	@Override
	public AEDocumentsList loadContratDeTravail(AEDocumentFilter filter) throws AEException {
		AESocialDocumentFilter socialFilter = (AESocialDocumentFilter) filter;
		AEDocumentsList docsList = new AEDocumentsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = selectSQL2;
			if(socialFilter.getEmployee() != null) {
				Employee employee = socialFilter.getEmployee();
				if(!AEStringUtil.isEmpty(employee.getName())) {
					sql += " and Employee.NAME like '%" + employee.getName() + "%' ";
				}
				if(!AEStringUtil.isEmpty(employee.getUIN())) {
					sql += " and Employee.UIN like '%" + employee.getUIN() + "%' ";
				}
				if(employee.getDateEntry() != null) {
					sql += " and Employee.DateEntry = " + AEPersistentUtil.escapeToDate(employee.getDateEntry());
				}
				if(employee.getDateRelease() != null) {
					sql += " and Employee.DateRelease = " + AEPersistentUtil.escapeToDate(employee.getDateRelease());
				}
				if(employee.getContractType() != null) {
					sql += " and Employee.ContractType = " + employee.getContractType().getSystemID();
				}
				if(socialFilter.getReasonRelease() != null) {
					sql += " and Social_FinDuTravail.ReasonID = " + socialFilter.getReasonRelease().getTypeID();
				}
				if(socialFilter.getDateFrom() != null) {
					sql += " and document.doc_date = " + AEPersistentUtil.escapeToDate(socialFilter.getDateFrom());
				}
			}
			// validated
			if(AEDocumentFilter.Validated.PROCESSED.equals(socialFilter.getValidated())) {
				sql += " and document.validated = 1";
			} else if(AEDocumentFilter.Validated.NOT_PROCESSED.equals(socialFilter.getValidated())) {
				sql += " and (document.validated = 0 or document.validated is null)";
			}
			ps = getAEConnection().prepareStatement(sql);
			ps.setLong(1, filter.getCompany().getDescriptor().getID());
			ps.setLong(2, filter.getDocType().getSystemID());
			rs = ps.executeQuery();
			while(rs.next()) {
				FinDuTravail doc = new FinDuTravail();
				build(doc, rs);
				docsList.add(doc);
			}
			return docsList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	@Override
	public void insert(AEDocument doc) throws AEException {
		assert(doc != null && !doc.isPersistent());
		assert doc instanceof FinDuTravail : "doc instanceof FTDocument failed";
		FinDuTravail ftDoc = (FinDuTravail) doc;
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// insert into common table
			super.insert(ftDoc);
			
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(insertSQL);
			build(ftDoc, ps, 1);

			// execute
			ps.executeUpdate();
			
			// set view state
			doc.setView();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}

	public int build(FinDuTravail doc, PreparedStatement ps, int i) throws SQLException, AEException {
		// ID
		ps.setLong(i++, doc.getID());
		// EmployeeID
		if(doc.getEmployee() != null) {
			ps.setLong(i++, doc.getEmployee().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		//EmployeePositionID, EmployeePositionName
		if(doc.getEmployeePosition() != null) {
			ps.setLong(i++, doc.getEmployeePosition().getDescriptor().getID());
			ps.setString(i++, doc.getEmployeePosition().getDescriptor().getName());
		} else {
			ps.setNull(i++, Types.BIGINT);
			ps.setNull(i++, Types.NVARCHAR);
		}
		//SupervisorID, SupervisorName
		if(doc.getSupervisor() != null) {
			ps.setLong(i++, doc.getSupervisor().getDescriptor().getID());
			ps.setString(i++, doc.getSupervisor().getDescriptor().getName());
		} else {
			ps.setNull(i++, Types.BIGINT);
			ps.setNull(i++, Types.NVARCHAR);
		}
		//DateEntry
		ps.setDate(i++, AEPersistentUtil.getSQLDate(doc.getDateEntry()));
		//DateRelease
		ps.setDate(i++, AEPersistentUtil.getSQLDate(doc.getDateRelease()));
		//Student
		ps.setBoolean(i++, doc.isStudent());
		//ReasonID, ReasonDescr
		if(doc.getReason() != null) {
			ps.setLong(i++, doc.getReason().getDescriptor().getID());
			ps.setString(i++, doc.getReason().getDescriptor().getDescription());
		} else {
			ps.setNull(i++, Types.BIGINT);
			ps.setNull(i++, Types.NVARCHAR);
		}
		//StillPresent
		ps.setBoolean(i++, doc.isStillPresent());
		
		// return the current ps position 
		return i;
	}

	public void build(FinDuTravail doc, ResultSet rs) throws SQLException, AEException {
		// build common attributes
		super.build(doc, rs);

		// ID  already set
		
		// EmployeeID
		long employeeID = rs.getLong("EmployeeID");
		if(!rs.wasNull() && employeeID > 0) {
			EmployeeDAO emplDAO = DAOFactory.getInstance().getEmployeeDAO(getAEConnection());
			doc.setEmployee(emplDAO.load(Employee.lazyDescriptor(employeeID)));
		}
		
		//EmployeePositionID, EmployeePositionName
		AEDescriptor emplPosition = new AEDescriptorImp();
		emplPosition.setID(rs.getLong("EmployeePositionID"));
		emplPosition.setName(rs.getString("EmployeePositionName"));
		doc.setEmployeePosition(emplPosition);

		//SupervisorID, SupervisorName
		AEDescriptor supervisor = new AEDescriptorImp();
		supervisor.setID(rs.getLong("SupervisorID"));
		supervisor.setName(rs.getString("SupervisorName"));
		doc.setSupervisor(supervisor);
		
		//DateEntry
		doc.setDateEntry(rs.getDate("DateEntry"));

		//DateRelease
		doc.setDateRelease(rs.getDate("DateRelease"));

		//Student
		doc.setStudent(rs.getBoolean("Student"));

		//ReasonID, ReasonDescr
		AEDescriptor reason = new AEDescriptorImp();
		reason.setID(rs.getLong("ReasonID"));
		reason.setDescription(rs.getString("ReasonDescr"));
		doc.setReason(reason);
		
		//StillPresent
		doc.setStillPresent(rs.getBoolean("StillPresent"));
		
		// set this record in view state
		doc.setView();
	}
	
	@Override
	public void update(AEDocument doc) throws AEException {
		assert(doc != null);
		assert(doc.isPersistent());
		assert doc instanceof FinDuTravail : "doc instanceof FTDocument failed";
		FinDuTravail ftDoc = (FinDuTravail) doc;
		PreparedStatement ps = null;
		try {
			// update document table
			super.update(ftDoc);
			
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			int i = build(ftDoc, ps, 1);
			ps.setLong(i++, doc.getID());
			
			// execute
			ps.executeUpdate();

			// set view state
			doc.setView();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public Long loadEmployeeId(long id) throws AEException {
		Long employeeId = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectEmployeeIdSQL);
			ps.setLong(1, id);
			rs = ps.executeQuery();
			if(rs.next()) {
				long pId = rs.getLong("EmployeeID");
				if(!rs.wasNull()) {
					employeeId = pId;
				}
			}
			return employeeId;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	@Override
	public long countAll(AEDescriptor ownerDescr, AEDocumentType docType) throws AEException {
		long count = 0;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCountAll);
			ps.setLong(1, ownerDescr.getID());
			ps.setLong(2, docType.getSystemID());
			rs = ps.executeQuery();
			if(rs.next()) {
				count = rs.getLong("count");
			}
			return count;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	@Override
	public long countValidated(AEDescriptor ownerDescr, AEDocumentType docType) throws AEException {
		long count = 0;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCountValidated);
			ps.setLong(1, ownerDescr.getID());
			ps.setLong(2, docType.getSystemID());
			rs = ps.executeQuery();
			if(rs.next()) {
				count = rs.getLong("count");
			}
			return count;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
}
