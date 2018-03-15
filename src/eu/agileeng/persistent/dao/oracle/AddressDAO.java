/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 25.11.2009 14:06:43
 */
package eu.agileeng.persistent.dao.oracle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.contact.Address;
import eu.agileeng.domain.contact.AddressesList;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;


/**
 *
 */
public class AddressDAO extends AbstractDAO {

	private static String selectSQL = 
		"select * from address where to_obj_id = ? and to_class_id = ?";
	
	private static String insertSQL = "insert into address (CLASS_ID, TYPE_ID, TO_OBJ_ID, TO_CLASS_ID, "
		+ "CODE, NAME, STREET, POSTAL_CODE, CITY, DISTRICT, STATE, PO_BOX, CREATOR, TIME_CREATED, "
		+ "MODIFIER, TIME_MODIFIED, PROPERTIES, SEC_STREET, COUNTRY_ID) values ("
		+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static String updateSQL = "update address set CLASS_ID = ?, TYPE_ID = ?, "
		+ "CODE = ?, NAME = ?, STREET = ?, POSTAL_CODE = ?, CITY = ?, DISTRICT = ?, "
		+ "STATE = ?, PO_BOX = ?, CREATOR = ?, TIME_CREATED = ?, "
		+ "MODIFIER = ?, TIME_MODIFIED = ?, PROPERTIES = ?, SEC_STREET = ?, COUNTRY_ID = ? where id = ?";
	
	private static String deleteSQL = 
		"delete from address where TO_OBJ_ID = ? and TO_CLASS_ID = ?";
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public AddressDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	public AddressesList load(AEDescriptor toDescr) throws AEException {
		AddressesList addressesList = new AddressesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, toDescr.getID());
			ps.setLong(2, toDescr.getClazz() != null ? toDescr.getClazz().getID() : 0L);
			rs = ps.executeQuery();
			while(rs.next()) {
				Address a = new Address();
				
				// build the instance
				build(a, rs);
				
				// set in view state
				a.setView();
				
				// add to the collection
				addressesList.add(a);
			}
		    return addressesList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void update(AddressesList addresses) throws AEException {
		assert(addresses != null);
		PreparedStatement ps = null;
		try {
			// update
			ps = getAEConnection().prepareStatement(updateSQL);
			for (Address address : addresses) {
				assert(!address.isPersistent()); 

				// build prepared statement
				int i = build(ps, address, null, 1);
				ps.setLong(i++, address.getID());
				
				// execute
				ps.executeUpdate();
				
				// set view state
				address.setView();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	public void delete(AddressesList addresses) throws AEException {
		assert(addresses != null);
		PreparedStatement ps = null;
		try {
			// update
			ps = getAEConnection().prepareStatement(insertSQL);
			for (Address address : addresses) {
				assert(!address.isPersistent()); 

				// build prepared statement
//				build(ps, contact);
				
				// execute
				ps.executeUpdate();
				
				// set view state
				address.setView();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	public void delete(AEDescriptor toDescr) throws AEException {
		PreparedStatement ps = null;
		try {
			// update
			ps = getAEConnection().prepareStatement(deleteSQL);

			// build prepared statement
			ps.setLong(1, toDescr.getID());
			ps.setLong(2, toDescr.getClazz().getID());

			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void build(Address a, ResultSet rs) throws SQLException {
		// build common attributes
		super.build(a, rs);
		
		// TYPE_ID
		a.setType(Address.Type.valueOf(rs.getLong("TYPE_ID")));
		//STREET
		a.setStreet(rs.getString("STREET"));
		//POSTAL_CODE
		a.setPostalCode(rs.getString("POSTAL_CODE"));
		//CITY
		a.setCity(rs.getString("CITY"));
		//DISTRICT
		a.setDistrict(rs.getString("DISTRICT"));
		//STATE
		a.setState(rs.getString("STATE"));
		//PO_BOX
		a.setpOBox(rs.getString("PO_BOX"));
		//SEC_STREET
		a.setSecondaryStreet(rs.getString("SEC_STREET"));
		//COUNTRY_ID
		a.setCountryID(rs.getLong("COUNTRY_ID"));
	}
	
	public void insert(AddressesList addresses, AEDescriptor toDescr) throws AEException {
		assert(addresses != null);
		assert(toDescr != null
				&& toDescr.isPersistent()
				&& toDescr.getClazz() != null);
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);
			for (Address address : addresses) {
				assert(!address.isPersistent()); 
				assert(address.getClazz() != null);
				assert(address.getType() != null);

				// build prepared statement
				build(ps, address, toDescr, 1);
				
				// execute
				ps.executeUpdate();
				
				// set generated key
				rs = ps.getGeneratedKeys();
				if (rs.next()) {
					// propagate generated key
					long id = rs.getLong(1);
					
					//required
					address.setID(id);
				} else {
					throw new AEException(getClass().getName() + "::insert: No keys were generated");
				}
				
				// set view state
				address.setView();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}	
	}
	
	private int build(PreparedStatement ps, Address address, AEDescriptor toDescr, int i) throws SQLException {
		// CLASS_ID
		if(address.getClazz() != null) {
			ps.setLong(i++, address.getClazz().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		// TYPE_ID
		if(address.getType() != null) {
			ps.setLong(i++, address.getType().getID());
		}  else {
			ps.setNull(i++, Types.BIGINT);
		}
		// TO_OBJ_ID, TO_CLASS_ID
		// IMPORTANT: Must exist only during insert
		if(toDescr != null) {
			ps.setLong(i++, toDescr.getID());
			if(toDescr.getClazz() != null) {
				ps.setLong(i++, toDescr.getClazz().getID());
			} else {
				ps.setNull(i++, Types.BIGINT);
			}
		}
		// CODE
		ps.setString(i++, address.getCode());
		// NAME
		ps.setString(i++, address.getName());
		// STREET
		ps.setString(i++, address.getStreet());
		// POSTAL_CODE
		ps.setString(i++, address.getPostalCode());
		// CITY
		ps.setString(i++, address.getCity());
		// DISTRICT
		ps.setString(i++, address.getDistrict());
		// STATE
		ps.setString(i++, address.getState());
		// PO_BOX
		ps.setString(i++, address.getpOBox());
		// CREATOR
		ps.setString(i++, address.getCreator());
		// TIME_CREATED
		ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(address.getTimeCreated()));
		// MODIFIER
		ps.setString(i++, address.getModifier());
		// TIME_MODIFIED
		ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(address.getTimeModified()));
		// PROPERTIES
		ps.setLong(i++, address.getProperties());
		// SEC_STREET
		ps.setString(i++, address.getSecondaryStreet());
		// COUNTRY_ID
		ps.setLong(i++, address.getCountryID());
		
		return i;
	}
}
