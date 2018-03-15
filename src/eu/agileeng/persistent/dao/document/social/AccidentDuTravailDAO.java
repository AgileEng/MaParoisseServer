package eu.agileeng.persistent.dao.document.social;

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
import eu.agileeng.domain.document.social.accidentdutravail.AccidentDuTravail;
import eu.agileeng.domain.document.social.accidentdutravail.AccidentDuTravail.TypeArret;
import eu.agileeng.domain.document.social.accidentdutravail.AccidentDuTravail.TypeDeDeclaration;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.document.AEDocumentDAO;
import eu.agileeng.persistent.dao.oracle.EmployeeDAO;
import eu.agileeng.util.AEStringUtil;

public class AccidentDuTravailDAO extends AEDocumentDAO {
	
	private static String selectSQL = 
		"select document.*, Social_AccidentDuTravail.* from Social_AccidentDuTravail "
		+ " inner join document on Social_AccidentDuTravail.id = document.id"
		+ " where Social_AccidentDuTravail.id = ?";

	private static String insertSQL = "insert into Social_AccidentDuTravail "
		+ "(ID, EmployeeID, TypeDeDeclarationID, TypeArretID, DateDeAccident, DateDuDernier, TravailNonRepris, "
		+ " DateDeFin, DateDeReprise, MiTemps) "
		+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static String updateSQL = "update Social_AccidentDuTravail set "
		+ "ID = ?, EmployeeID = ?, TypeDeDeclarationID = ?, TypeArretID = ?, DateDeAccident = ?, DateDuDernier = ?, TravailNonRepris = ?, "
		+ " DateDeFin = ?, DateDeReprise = ?, MiTemps = ? where ID = ?";
	
	/*
	 * Keep synchronised: selectSQL2, selectSQLCountAll, selectSQLCountNotValidated
	 */
	private static String selectSQL2 = 	
		"select Social_AccidentDuTravail.*, document.* "
		+ " from Social_AccidentDuTravail inner join document on Social_AccidentDuTravail.id = document.id "
		+ " inner join Employee on Social_AccidentDuTravail.EmployeeID = Employee.ID "
		+ " where document.owner_id = ? and document.type_id= ? ";

	/*
	 * Keep synchronised: selectSQL2, selectSQLCountAll, selectSQLCountNotValidated
	 */
	private static String selectSQLCountAll = 	
		"select count(Document.ID) as count "
		+ " from Social_AccidentDuTravail inner join document on Social_AccidentDuTravail.id = document.id "
		+ " inner join Employee on Social_AccidentDuTravail.EmployeeID = Employee.ID "
		+ " where document.owner_id = ? and document.type_id= ? ";
	
	/*
	 * Keep synchronised: selectSQL2, selectSQLCountAll, selectSQLCountNotValidated
	 */
	private static String selectSQLCountValidated = 	
		"select count(Document.ID) as count "
		+ " from Social_AccidentDuTravail inner join document on Social_AccidentDuTravail.id = document.id "
		+ " inner join Employee on Social_AccidentDuTravail.EmployeeID = Employee.ID "
		+ " where document.owner_id = ? and document.type_id= ? and document.VALIDATED = 1";
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public AccidentDuTravailDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	@Override
	public AEDocument load(AEDescriptor docDescr) throws AEException {
		AccidentDuTravail doc = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, docDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				doc = new AccidentDuTravail();
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
				if(socialFilter.getDateDuDernier() != null) {
					sql += " and Social_AccidentDuTravail.DateDuDernier = " + AEPersistentUtil.escapeToDate(socialFilter.getDateDuDernier());
				}
				if(socialFilter.getDateDeFin() != null) {
					sql += " and Social_AccidentDuTravail.DateDeFin = " + AEPersistentUtil.escapeToDate(socialFilter.getDateDeFin());
				}
				if(socialFilter.getDateDeReprise() != null) {
					sql += " and Social_AccidentDuTravail.DateDeReprise = " + AEPersistentUtil.escapeToDate(socialFilter.getDateDeReprise());
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
				AccidentDuTravail doc = new AccidentDuTravail();
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
		assert doc instanceof AccidentDuTravail : "doc instanceof AccidentDuTravail failed";
		AccidentDuTravail adtDoc = (AccidentDuTravail) doc;
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// insert into common table
			super.insert(adtDoc);
			
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(insertSQL);
			build(adtDoc, ps, 1);

			// execute
			ps.executeUpdate();
			
			// set view state
			doc.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}

	public int build(AccidentDuTravail doc, PreparedStatement ps, int i) throws SQLException, AEException {
		// ID
		ps.setLong(i++, doc.getID());
		// EmployeeID
		if(doc.getEmployee() != null) {
			ps.setLong(i++, doc.getEmployee().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		//TypeDeDeclarationID
		if(doc.getTypeDeDeclaration() != null) {
			ps.setLong(i++, doc.getTypeDeDeclaration().getTypeID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		//TypeArretID
		if(doc.getTypeArret() != null) {
			ps.setLong(i++, doc.getTypeArret().getTypeID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		//DateDeAccident
		ps.setDate(i++, AEPersistentUtil.getSQLDate(doc.getDateDeAccident()));
		//DateDuDernier
		ps.setDate(i++, AEPersistentUtil.getSQLDate(doc.getDateDuDernier()));
		//TravailNonRepris
		ps.setBoolean(i++, doc.isTravailNonRepris());
		//DateDeFin
		ps.setDate(i++, AEPersistentUtil.getSQLDate(doc.getDateDeFin()));
		//DateDeReprise
		ps.setDate(i++, AEPersistentUtil.getSQLDate(doc.getDateDeReprise()));
		//MiTemps
		ps.setBoolean(i++, doc.isMiTemps());
		
		// return the current ps position 
		return i;
	}

	public void build(AccidentDuTravail doc, ResultSet rs) throws SQLException, AEException {
		// build common attributes
		super.build(doc, rs);

		// ID  already set
		
		// EmployeeID
		long employeeID = rs.getLong("EmployeeID");
		if(!rs.wasNull() && employeeID > 0) {
			EmployeeDAO emplDAO = DAOFactory.getInstance().getEmployeeDAO(getAEConnection());
			doc.setEmployee(emplDAO.load(Employee.lazyDescriptor(employeeID)));
		}
		
		//TypeDeDeclarationID
		long typeDeDeclarationID = rs.getLong("TypeDeDeclarationID"); 
		if(!rs.wasNull() && typeDeDeclarationID > 0) {
			doc.setTypeDeDeclaration(TypeDeDeclaration.valueOf(typeDeDeclarationID));
		}
		//TypeArretID
		long typeArretID = rs.getLong("TypeArretID"); 
		if(!rs.wasNull() && typeArretID > 0) {
			doc.setTypeArret(TypeArret.valueOf(typeArretID));
		}
		//DateDeAccident
		doc.setDateDeAccident(rs.getDate("DateDeAccident"));
		//DateDuDernier
		doc.setDateDuDernier(rs.getDate("DateDuDernier"));
		//TravailNonRepris
		doc.setTravailNonRepris(rs.getBoolean("TravailNonRepris"));
		//DateDeFin
		doc.setDateDeFin(rs.getDate("DateDeFin"));
		//DateDeReprise
		doc.setDateDeReprise(rs.getDate("DateDeReprise"));
		//MiTemps
		doc.setMiTemps(rs.getBoolean("MiTemps"));
		
		// set this record in view state
		doc.setView();
	}
	
	@Override
	public void update(AEDocument doc) throws AEException {
		assert(doc != null);
		assert(doc.isPersistent());
		assert doc instanceof AccidentDuTravail : "doc instanceof AccidentDuTravail failed";
		AccidentDuTravail adtDoc = (AccidentDuTravail) doc;
		PreparedStatement ps = null;
		try {
			// update document table
			super.update(adtDoc);
			
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			int i = build(adtDoc, ps, 1);
			ps.setLong(i++, doc.getID());
			
			// execute
			ps.executeUpdate();

			// set view state
			doc.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
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
