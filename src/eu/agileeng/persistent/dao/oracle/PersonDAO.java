/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 22.11.2009 14:25:14
 */
package eu.agileeng.persistent.dao.oracle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.tomcat.util.json.JSONException;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDescriptorsList;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.contact.PartiesList;
import eu.agileeng.domain.contact.Party;
import eu.agileeng.domain.contact.Person;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;


/**
 *
 */
public class PersonDAO extends PartyDAO {
	private static String selectSQL = 
		"select party.*, person.* from Person inner join Party on Person.PartyID = Party.ID  where party.ID = ?";
	
	private static String selectSQLEmployees = 
		"select party.*, person.* from Person inner join Party on Person.PartyID = Party.ID "  
	    + " where Party.ID in (select distinct(e.PERSON_ID) from Employee e where e.COMPANY_ID = ?)";
	
	private static String selectSQLAll = "select * from party where class_id = ?";
	
	private static String selectSQLSubject = 
		"select party.* from SubjectCompAssoc as assoc inner join Party as party "
		+ "on assoc.ID_Company = party.id where ID_Subject = ?";

	private static String insertSQL = "insert into Person (PartyID, FirstName, MiddleName, "
		+ "LastName, GirlName, SalutationID, DateOfBirth, PlaceOfBirth, NationalityTypeID, "
		+ "NationalityStr, DocTypeID, DocNumber, DocDateOfExpiry, DocIssuedByStr, HasUIN) "
		+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static String updateSQL = "update Person set PartyID = ?, FirstName = ?, MiddleName = ?, "
		+ "LastName = ?, GirlName = ?, SalutationID = ?, DateOfBirth = ?, PlaceOfBirth = ?, NationalityTypeID = ?, "
		+ "NationalityStr = ?, DocTypeID = ?, DocNumber = ?, DocDateOfExpiry = ?, DocIssuedByStr = ?, HasUIN = ? "
		+ "where PartyID = ?";
	
//	private static String deleteSQL = "delete from Person where PartyId = ?";

	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	PersonDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	@Override
	public void insert(Party party) throws AEException {
		assert(party != null);
		assert(!party.isPersistent());
		assert party instanceof Person : "party instanceof Person failed";
		Person person = (Person) party;
		PreparedStatement ps = null;
		try {
			// insert super
			super.insert(party);
			
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(insertSQL);
			build(person, ps, 1);

			// execute
			ps.executeUpdate();
			
			// set view state
			party.setView();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	@Override
	public void build(Party party, ResultSet rs) throws SQLException, AEException {
		// build common attributes
		super.build(party, rs);

		// build additional attributes
		Person person = (Person) party;
		//PartyID
		//???
		//FirstName
		person.setFirstName(rs.getString("FirstName"));
		//MiddleName
		person.setMiddleName(rs.getString("MiddleName"));
		//LastName
		person.setLastName(rs.getString("LastName"));
		//GirlName
		person.setGirlName(rs.getString("GirlName"));
		//SalutationID
		person.setSalutation(Person.SalutationType.valueOf(rs.getInt("SalutationID")));
		//DateOfBirth
		person.setDateOfBirth(rs.getDate("DateOfBirth"));
		//PlaceOfBirth
		person.setPlaceOfBirth(rs.getString("PlaceOfBirth"));
		//NationalityTypeID
		person.setNationalityType(Person.NationalityType.valueOf(rs.getInt("NationalityTypeID")));
		//NationalityStr
		person.setNationality(rs.getString("NationalityStr"));
		//DocTypeID
		person.setDocType(AEDocumentType.valueOf(rs.getLong("DocTypeID")));
		//DocNumber
		person.setDocNumber(rs.getString("DocNumber"));
		//DocDateOfExpiry
		person.setDocDateOfExpiry(rs.getDate("DocDateOfExpiry"));
		//DocIssuedByStr
		person.setDocIssuedByString(rs.getString("DocIssuedByStr"));
		//HasUIN
		person.setHasUIN(rs.getBoolean("HasUIN"));
		
		// set this record in view state
		party.setView();
	}
	
	@Override
	public void update(Party party) throws AEException {
		assert(party != null);
		assert(party.isPersistent());
		assert party instanceof Person : "party instanceof Person failed";
		Person person = (Person) party;
		PreparedStatement ps = null;
		try {
			// update document table
			super.update(person);
			
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			int i = build(person, ps, 1);
			ps.setLong(i++, party.getID());
			
			// execute
			ps.executeUpdate();

			// set view state
			party.setView();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.dao.oracle.PartyDAO#load(eu.agileeng.domain.AEDescriptor)
	 */
	@Override
	public Party load(AEDescriptor partyDescr) throws AEException {
		Person person = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, partyDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				person = new Person();
				build(person, rs);
				postLoad(person);
				person.setView();
			}
			return person;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.dao.oracle.PartyDAO#loadDescriptor(long)
	 */
	@Override
	public AEDescriptive loadDescriptive(long id) throws AEException {
		AEDescriptor personDescr = null;
		try {
			Party person = load(new AEDescriptorImp(id, DomainClass.PERSON));
			if(person != null) {
				personDescr = person.getDescriptor();
			}
			return personDescr;
		} finally {
			close();
		}
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.dao.oracle.PartyDAO#loadAll()
	 */
	@Override
	public PartiesList loadAll() throws AEException {
		PartiesList partList = new PartiesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAll);
			ps.setLong(1, DomainModel.DomainClass.PERSON.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				Person person = new Person();
				super.build(person, rs);
				postLoad(person);
				person.setView();
				
				partList.add(person);
			}
			return partList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.dao.oracle.PartyDAO#loadToSubject(eu.agileeng.domain.AEDescriptor)
	 */
	@Override
	public PartiesList loadToPrincipal(AEDescriptor principalDescr) throws AEException {
		PartiesList orgList = new PartiesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLSubject);
			ps.setLong(1, principalDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				Party comp = new Organization();
				super.build(comp, rs);
				orgList.add(comp);
			}
			return orgList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private int build(Person person, PreparedStatement ps, int i) throws SQLException, AEException {
		//PartyID
		ps.setLong(i++, person.getID());
		//FirstName
		ps.setString(i++, person.getFirstName());
		//MiddleName
		ps.setString(i++, person.getMiddleName());
		//LastName
		ps.setString(i++, person.getLastName());
		//GirlName
		ps.setString(i++, person.getGirlName());
		//SalutationID
		if(person.getSalutation() != null) {
			ps.setLong(i++, person.getSalutation().getTypeID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		//DateOfBirth
		ps.setDate(i++, AEPersistentUtil.getSQLDate(person.getDateOfBirth()));
		//PlaceOfBirth
		ps.setString(i++, person.getPlaceOfBirth());
		//NationalityTypeID
		if(person.getNationalityType() != null) {
			ps.setLong(i++, person.getNationalityType().getTypeID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		//NationalityStr
		ps.setString(i++, person.getNationality());
		//DocTypeID
		if(person.getDocType() != null) {
			ps.setLong(i++, person.getDocType().getSystemID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		//DocNumber
		ps.setString(i++, person.getDocNumber());
		//DocDateOfExpiry
		ps.setDate(i++, AEPersistentUtil.getSQLDate(person.getDocDateOfExpiry()));
		//DocIssuedByStr
		ps.setString(i++, person.getDocIssuedByString());
		//HasUIN
		ps.setBoolean(i++, person.isHasUIN());

		return i;
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.dao.oracle.PartyDAO#loadAll()
	 */
	public PartiesList loadEmployees(AEDescriptor companyDescr) throws AEException {
		PartiesList personsList = new PartiesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLEmployees);
			ps.setLong(1, companyDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				Person person = new Person();
				build(person, rs);
				postLoad(person);
				person.setView();
				
				personsList.add(person);
			}
			return personsList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	@Override
	public void delete(AEDescriptor personDescr) throws AEException, JSONException {
		super.delete(personDescr);
	}

	@Override
	public AEDescriptorsList loadToPrincipalDescriptor(AEDescriptor principalDescr) throws AEException {
		return new AEDescriptorsList();
	}
}
