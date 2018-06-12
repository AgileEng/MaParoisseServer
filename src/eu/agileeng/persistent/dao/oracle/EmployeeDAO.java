/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 08.06.2010 19:24:51
 */
package eu.agileeng.persistent.dao.oracle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;

import org.jboss.logging.Logger;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.contact.Address;
import eu.agileeng.domain.contact.AddressesList;
import eu.agileeng.domain.contact.Contact;
import eu.agileeng.domain.contact.ContactsList;
import eu.agileeng.domain.contact.Employee;
import eu.agileeng.domain.contact.EmployeeList;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.contact.Person;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.social.contractdetravail.ContractDeTravailType;
import eu.agileeng.domain.document.social.findutravail.FinDuTravail;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.persistent.dao.DAOFactory;

/**
 *
 */
public class EmployeeDAO extends AbstractDAO {

	protected static Logger logger = Logger.getLogger(EmployeeDAO.class);
	
	private static String selectSQL = "select * from employee where id = ?";

	private static String selectSQLCompany = 
			"select empl.* from Employee empl inner join ContractDeTravail contr "
					+ " on empl.ID = contr.EmployeeID where empl.COMPANY_ID = ?";

	private static String selectSQLCompanyUniq = 
			"select * from Employee "
					+ " inner join "
					+ " (SELECT MAX(ID) as employeeId "
					+ "   FROM [AccBureau].[dbo].[Employee] " 
					+ "   where COMPANY_ID = ? "
					+ "   group by PERSON_ID, COMPANY_ID) as empl "
					+ " on ID = empl.employeeId "
					+ " order by LastName desc ";

	private static String selectSQLMaxFtpId = 
			"select max(ftp_id) as ftp_id from Employee where COMPANY_ID = ?";

	private static String selectSQLByUIN = 
			"SELECT TOP 1 PERSON_ID "
					+ " FROM [AccBureau].[dbo].[Employee] where UIN = ? and COMPANY_ID = ? "
					+ " order by ID desc";

	private static String selectSQLCurrent = 
			"select * from Employee where COMPANY_ID = ? and DateRelease is null";

	private static String selectSQLCurrentEx = 
			"select * from Employee where COMPANY_ID = ? and (DateRelease is null or DateRelease >= ?)";

	private static String selectSQLCurrentEx2 = 
			"select empl.* from Employee empl inner join ContractDeTravail contr "
					+ " on empl.ID = contr.EmployeeID "
					+ " where empl.COMPANY_ID = ? ";
	//+ " and (DateRelease is null or DateRelease >= ?)";

	private static String selectSQLNumberOfWeeks = 
			"select NumberOfWeeks from Employee where id = ?";

	private static String updateSQLNumberOfWeeks = 
			"update employee set NumberOfWeeks = ? where id = ?";

	private static String selectSQLOld = 
			"select * from Employee inner join"
					+ "(select MAX(Employee.ID) as empl_id from Employee where COMPANY_ID = ?" // and DateRelease is not null "
					+ " group by Employee.PERSON_ID) release_empl on Employee.ID = empl_id"; 
	//		"select top 1 * from Employee where COMPANY_ID = ? and DateRelease is not null "
	//		+ " order by DateEntry desc";

	private static String insertSQL = "insert into employee (UIN, NAME, CODE, TIME_CREATED, CREATOR, "
			+ "TIME_MODIFIED, MODIFIER, PROPERTIES, CLASS_ID, DESCRIPTION, NOTE, COMPANY_ID, PERSON_ID, POSITION_NAME, "
			+ "FirstName, MiddleName, LastName, GirlName, "
			+ "SalutationID, DateOfBirth, PlaceOfBirth, NationalityTypeID, NationalityStr, DocTypeID, "
			+ "DocNumber, DocDateOfExpiry, DocIssuedByStr, LeTexteBool, DateEntry, DateRelease, DateModification, "
			+ "ContractType, ReasonRelease, HasUIN) "
			+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static String updateSQL = "update employee set UIN = ?, NAME = ?, CODE = ?, TIME_CREATED = ?, CREATOR = ?, "
			+ " TIME_MODIFIED = ?, MODIFIER = ?, PROPERTIES = ?, CLASS_ID = ?, DESCRIPTION = ?, NOTE = ?, "
			+ " COMPANY_ID = ?, PERSON_ID = ?, POSITION_NAME = ?, "
			+ " FirstName = ?, MiddleName = ?, LastName = ?, GirlName = ?, "
			+ " SalutationID = ?, DateOfBirth  = ?, PlaceOfBirth  = ?, "
			+ " NationalityTypeID = ?, NationalityStr = ?, DocTypeID = ?, DocNumber = ?, DocDateOfExpiry = ?, "
			+ " DocIssuedByStr = ?, LeTexteBool  = ?, DateEntry = ?, DateRelease = ?, DateModification = ?, "
			+ " ContractType = ?, ReasonRelease = ?, HasUIN = ? "
			+ " where id = ?";

	private static String updateSQLFtpId = 
			"update employee set FTP_ID = ? where COMPANY_ID = ? and PERSON_ID = ?";

	private static String selectPersonIdSQL = 
			"select PERSON_ID from employee where id = ?";

	private static String nullifyEntryDateSQL = 
			"update employee set dateentry = null where id = ?";

	private static String nullifyModificationSQL = 
			"update employee set datemodification = null where id = ?";

	private static String nullifyReleaseDateSQL = 
			"update employee set daterelease = null where id = ?";

	private static String deleteSQL = "delete from employee where id = ?";

	private static String updateModificationDateSQL = "update employee set DateModification = ? where id = ?";

	private ContactDAO contactDAO = null;

	private AddressDAO addressDAO = null;


	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public EmployeeDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	protected void build(Employee e, ResultSet rs) throws SQLException, AEException {
		// build common attributes
		super.build(e, rs);

		// build additional attributes
		DAOFactory daoFactory = DAOFactory.getInstance();

		// ID_ORGANIZATION
		long compID = rs.getLong("COMPANY_ID");
		if(!rs.wasNull()) {
			if(e.isBuildLazzy()) {
				e.setCompany(Organization.lazyDescriptor(compID));
			} else {
				e.setCompany(daoFactory.getOrganizationDAO(getAEConnection()).loadDescriptive(compID));
			}
		}

		// ID_PERSON
		long personID = rs.getLong("PERSON_ID");
		if(!rs.wasNull()) {
			if(e.isBuildLazzy()) {
				e.setPerson(Person.lazyDescriptor(personID));
			} else {
				e.setPerson(daoFactory.getPersonDAO(getAEConnection()).loadDescriptive(personID));
			}
		}

		//FirstName
		e.setFirstName(rs.getString("FirstName"));

		//LastName
		e.setLastName(rs.getString("LastName"));

		//SalutationID
		try {
			e.setSalutation(Person.SalutationType.valueOf(rs.getInt("SalutationID")));
		} catch(Exception exc) {
			logger.warnf("read Employee::SalutationID field failed: {}", exc.getMessage());
		}
		
		try {
			//UIN
			e.setUIN(rs.getString("UIN"));
			// POSITION_NAME
			e.setPosition(rs.getString("POSITION_NAME"));
			//MiddleName
			e.setMiddleName(rs.getString("MiddleName"));
			//GirlName
			e.setGirlName(rs.getString("GirlName"));
			//DateOfBirth
			e.setDateOfBirth(rs.getDate("DateOfBirth"));
			//PlaceOfBirth
			e.setPlaceOfBirth(rs.getString("PlaceOfBirth"));
			//NationalityTypeID
			e.setNationalityType(Person.NationalityType.valueOf(rs.getInt("NationalityTypeID")));
			//NationalityStr
			e.setNationality(rs.getString("NationalityStr"));
			//DocTypeID
			e.setDocType(AEDocumentType.valueOf(rs.getLong("DocTypeID")));
			//DocNumber
			e.setDocNumber(rs.getString("DocNumber"));
			//DocDateOfExpiry
			e.setDocDateOfExpiry(rs.getDate("DocDateOfExpiry"));
			//DocIssuedByStr
			e.setDocIssuedByString(rs.getString("DocIssuedByStr"));
			//LeTexte
			e.setLeTexte(rs.getBoolean("LeTexteBool"));
			//DateEntry
			e.setDateEntry(rs.getDate("DateEntry"));
			//DateRelease
			e.setDateRelease(rs.getDate("DateRelease"));
			//DateModification
			e.setModificationDate(rs.getDate("DateModification"));
			//NumberOfWeeks
			int numberOfWeeks = rs.getInt("NumberOfWeeks");
			if(!rs.wasNull()) {
				e.setNumberOfWeeks(numberOfWeeks);
			}
			//ContractType
			long contractType = rs.getLong("ContractType");
			if(!rs.wasNull()) {
				e.setContractType(new ContractDeTravailType(ContractDeTravailType.System.valueOf(contractType)));
			}
			//ReasonRelease 
			long reasonRelease = rs.getLong("ReasonRelease");
			if(!rs.wasNull()) {
				e.setReasonRelease(FinDuTravail.Reason.valueOf(reasonRelease));
			}
			//HasUIN
			e.setHasUIN(rs.getBoolean("HasUIN"));

			// FTP_ID
			long ftpId = rs.getLong("FTP_ID");
			if(!rs.wasNull()) {
				e.setFtpId(ftpId);
			}
		} catch (Exception ex) {};

		// set this record in view state
		e.setView();
	}

	public void insert(Employee e) throws AEException {
		assert(!e.isPersistent());
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);

			int i = 1;
			build(e, ps, i);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);

				//required
				e.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// ftpId
			updateFtpId(e);

			// set view state
			e.setView();
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw new AEException(ex.getMessage(), ex);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}

	protected int build(Employee e, PreparedStatement ps, int i) throws SQLException, AEException {
		//UIN
		ps.setString(i++, e.getUIN());
		// NAME
		ps.setString(i++, e.getName());
		// CODE
		ps.setString(i++, e.getCode());
		// TIME_CREATED
		ps.setTimestamp(i++, AEPersistentUtil.getTimestampNotNull(e.getTimeCreated()));
		// CREATOR
		ps.setString(i++, e.getCreator());
		// TIME_MODIFIED
		ps.setTimestamp(i++, AEPersistentUtil.getTimestampNotNull(e.getTimeModified()));
		// MODIFIER
		ps.setString(i++, e.getModifier());
		// PROPERTIES
		ps.setLong(i++, e.getProperties());
		// CLASS_ID
		if(e.getClazz() != null) {
			ps.setLong(i++, e.getClazz().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		// DESCRIPTION
		ps.setString(i++, e.getDescription());
		// NOTE
		ps.setString(i++, e.getNote());
		// ID_COMPANY
		if(e.getCompany() != null) {
			ps.setLong(i++, e.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		// ID_PERSON
		if(e.getPerson() != null) {
			ps.setLong(i++, e.getPerson().getDescriptor().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		// POSITION_NAME
		ps.setString(i++, e.getPosition());
		//FirstName
		ps.setString(i++, e.getFirstName());
		//MiddleName
		ps.setString(i++, e.getMiddleName());
		//LastName
		ps.setString(i++, e.getLastName());
		//GirlName
		ps.setString(i++, e.getGirlName());
		//SalutationID
		if(e.getSalutation() != null) {
			ps.setLong(i++, e.getSalutation().getTypeID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		//DateOfBirth
		ps.setDate(i++, AEPersistentUtil.getSQLDate(e.getDateOfBirth()));
		//PlaceOfBirth
		ps.setString(i++, e.getPlaceOfBirth());
		//NationalityTypeID
		if(e.getNationalityType() != null) {
			ps.setLong(i++, e.getNationalityType().getTypeID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		//NationalityStr
		ps.setString(i++, e.getNationality());
		//DocTypeID
		if(e.getDocType() != null) {
			ps.setLong(i++, e.getDocType().getSystemID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		//DocNumber
		ps.setString(i++, e.getDocNumber());
		//DocDateOfExpiry
		ps.setDate(i++, AEPersistentUtil.getSQLDate(e.getDocDateOfExpiry()));
		//DocIssuedByStr
		ps.setString(i++, e.getDocIssuedByString());
		// Le Texte
		ps.setBoolean(i++, e.isLeTexte());
		//DateEntry
		ps.setDate(i++, AEPersistentUtil.getSQLDate(e.getDateEntry()));
		//DateRelease
		ps.setDate(i++, AEPersistentUtil.getSQLDate(e.getDateRelease()));
		//DateModification
		ps.setDate(i++, AEPersistentUtil.getSQLDate(e.getModificationDate()));
		//		//NumberOfWeeks
		//		if(e.getNumberOfWeeks() != null) {
		//			ps.setInt(i++, e.getNumberOfWeeks());
		//		} else {
		//			ps.setNull(i++, Types.INTEGER);
		//		}
		//ContractType
		if(e.getContractType() != null) {
			ps.setLong(i++, e.getContractType().getSystemID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		//ReasonRelease 
		if(e.getReasonRelease() != null) {
			ps.setLong(i++, e.getReasonRelease().getTypeID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		//HasUIN
		ps.setBoolean(i++, e.isHasUIN());

		// return the current ps position 
		return i;
	}

	public void update(Employee e) throws AEException {
		assert(e != null);
		assert(e.isPersistent());
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			int i = build(e, ps, 1);
			ps.setLong(i++, e.getID());

			// execute
			ps.executeUpdate();

			// set view
			e.setView();
		} catch (SQLException ex) {
			ex.printStackTrace();
			throw new AEException(ex.getMessage(), ex);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	public void updateFtpId(Employee e) throws AEException {
		assert(e != null);
		assert(e.isPersistent());
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQLFtpId);

			// build statement
			// "update employee set FTP_ID = ? where COMPANY_ID = ? and PERSON_ID = ?";
			if(e.getFtpId() != null) {
				ps.setLong(1, e.getFtpId());
			} else {
				ps.setNull(1, Types.BIGINT);
			}
			ps.setLong(2, e.getCompany().getDescriptor().getID());
			ps.setLong(3, e.getPerson().getDescriptor().getID());

			// execute
			ps.executeUpdate();
		} catch (SQLException ex) {
			throw new AEException(ex);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	public void delete(long id) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQL);

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

	public void updateNumberOfWeeks(AEDescriptive emplDescr, int numberOfWeeks) throws AEException {
		assert(emplDescr != null);
		assert(emplDescr.getDescriptor().getID() > 0);
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQLNumberOfWeeks);

			// build statement
			ps.setInt(1, numberOfWeeks);
			ps.setLong(2, emplDescr.getDescriptor().getID());

			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	public void updateModificationDate(AEDescriptive emplDescr, Date modifDate) throws AEException {
		assert(emplDescr != null);
		assert(emplDescr.getDescriptor().getID() > 0);
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateModificationDateSQL);

			// build statement
			ps.setDate(1, AEPersistentUtil.getSQLDate(modifDate));
			ps.setLong(2, emplDescr.getDescriptor().getID());

			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	public Employee load(AEDescriptor emplDescr) throws AEException {
		Employee empl = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, emplDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				empl = new Employee();
				build(empl, rs);

				// set view state
				empl.setView();
			}

			return empl;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public Employee loadFull(AEDescriptor emplDescr) throws AEException {
		Employee e = load(emplDescr);
		if(e != null) {
			postLoad(e);
		}
		return e;
	}

	public Employee loadLazzy(AEDescriptor emplDescr) throws AEException {
		Employee ampl = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, emplDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				ampl = new Employee();
				ampl.setBuildLazzy(true);
				build(ampl, rs);
			}

			// set view state
			ampl.setView();

			return ampl;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public Long loadPersonID(String uin, long companyId) throws AEException {
		Long id = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLByUIN);
			ps.setString(1, uin);
			ps.setLong(2, companyId);
			rs = ps.executeQuery();
			if(rs.next()) {
				id = new Long(rs.getLong("person_id"));
			}
			return id;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public Integer loadNumberOfWeeks(AEDescriptor emplDescr) throws AEException {
		Integer num = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLNumberOfWeeks);
			ps.setLong(1, emplDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				int numberOfWeeks = rs.getInt("NumberOfWeeks");
				if(!rs.wasNull()) {
					num = numberOfWeeks;
				}
			}
			return num;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public AEDescriptive loadDescriptive(long id) throws AEException {
		AEDescriptor emplDescr = null;
		try {
			Employee empl = load(Employee.lazyDescriptor(id));
			if(empl != null) {
				emplDescr = empl.getDescriptor();
			}
			return emplDescr;
		} finally {
			close();
		}
	}

	public EmployeeList loadToCompany(AEDescriptor compDescr) throws AEException {
		EmployeeList emplList = new EmployeeList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCompany);
			ps.setLong(1, compDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				Employee empl = new Employee();
				build(empl, rs);
				postLoad(empl);
				empl.setView();
				emplList.add(empl);
			}
			return emplList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public EmployeeList loadToCompanyUniq(AEDescriptor compDescr) throws AEException {
		EmployeeList emplList = new EmployeeList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCompanyUniq);
			ps.setLong(1, compDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				Employee empl = new Employee();
				build(empl, rs);
				empl.setView();
				emplList.add(empl);
			}
			return emplList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public EmployeeList loadCurrentEmployees(AEDescriptor compDescr) throws AEException {
		EmployeeList emplList = new EmployeeList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCurrent);
			ps.setLong(1, compDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				Employee empl = new Employee();
				build(empl, rs);
				postLoad(empl);
				empl.setView();
				emplList.add(empl);
			}
			return emplList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public EmployeeList loadCurrentEmployeesLazzy(AEDescriptor compDescr) throws AEException {
		EmployeeList emplList = new EmployeeList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCurrentEx2);
			ps.setLong(1, compDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				Employee empl = new Employee();
				build(empl, rs);
				empl.setView();
				emplList.add(empl);
			}
			return emplList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public EmployeeList loadCurrentEmployees(AEDescriptor compDescr, Date date) throws AEException {
		EmployeeList emplList = new EmployeeList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCurrentEx);
			ps.setLong(1, compDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(date));
			rs = ps.executeQuery();
			while(rs.next()) {
				Employee empl = new Employee();
				build(empl, rs);
				postLoad(empl);
				empl.setView();
				emplList.add(empl);
			}
			return emplList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public EmployeeList loadOldEmployees(AEDescriptor compDescr) throws AEException {
		EmployeeList emplList = new EmployeeList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLOld);
			ps.setLong(1, compDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				Employee empl = new Employee();
				build(empl, rs);
				postLoad(empl);
				empl.setView();
				emplList.add(empl);
			}
			return emplList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	private void loadContact(Employee employee) throws AEException {
		try {
			if(contactDAO == null) {
				DAOFactory daoFactory = DAOFactory.getInstance();
				contactDAO = daoFactory.getContactDAO(getAEConnection());
			}
			ContactsList contactsList = contactDAO.load(employee.getDescriptor());
			employee.setContact(contactsList.getContact(Contact.Type.BUSINESS));
		} finally {
			close();
		}
	}

	private void loadAddress(Employee employee) throws AEException {
		try {
			if(addressDAO == null) {
				DAOFactory daoFactory = DAOFactory.getInstance();
				addressDAO = daoFactory.getAddressDAO(getAEConnection());
			}
			AddressesList addressesList = addressDAO.load(employee.getDescriptor());
			employee.setAddress(addressesList.getAddress(Address.Type.BUSINESS));
		} finally {
			close();
		}
	}

	protected void postLoad(Employee employee) throws AEException {
		try {
			loadContact(employee);
			loadAddress(employee);
		} finally {
			close();
		}
	}

	public Long loadPersonId(long id) throws AEException {
		Long personId = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectPersonIdSQL);
			ps.setLong(1, id);
			rs = ps.executeQuery();
			if(rs.next()) {
				long pId = rs.getLong("PERSON_ID");
				if(!rs.wasNull()) {
					personId = pId;
				}
			}
			return personId;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public void nullifyEntryDate(long id) throws AEException {
		PreparedStatement ps = null;
		try {
			ps = getAEConnection().prepareStatement(nullifyEntryDateSQL);
			ps.setLong(1, id);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	public void nullifyModification(long id) throws AEException {
		PreparedStatement ps = null;
		try {
			ps = getAEConnection().prepareStatement(nullifyModificationSQL);
			ps.setLong(1, id);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	public void nullifyReleaseDate(long id) throws AEException {
		PreparedStatement ps = null;
		try {
			ps = getAEConnection().prepareStatement(nullifyReleaseDateSQL);
			ps.setLong(1, id);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	public Long loadMaxFtpId(AEDescriptive owner) throws AEException {
		Long ftpId = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// should generate ftp id
			ps = getAEConnection().prepareStatement(selectSQLMaxFtpId);
			ps.setLong(1, owner.getDescriptor().getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				long l = rs.getLong("ftp_id");
				if(!rs.wasNull()) {
					ftpId = l;
				}
			}
			return ftpId;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
}
