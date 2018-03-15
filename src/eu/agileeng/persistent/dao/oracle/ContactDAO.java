/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 21.11.2009 16:28:48
 */
package eu.agileeng.persistent.dao.oracle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.contact.Contact;
import eu.agileeng.domain.contact.ContactsList;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;


/**
 *
 */
public class ContactDAO extends AbstractDAO {

	private static String selectSQL = 
		"select * from contact where to_obj_id = ? and to_class_id = ?";
	
	private static String insertSQL = "insert into contact (CLASS_ID, TYPE_ID, TO_OBJ_ID, TO_CLASS_ID, "
			+ "CODE, NAME, PHONE, MOBILE, E_MAIL, FAX, HOMEPAGE, TIME_CREATED, CREATOR, TIME_MODIFIED, "
			+ "MODIFIER, PROPERTIES) values ("
			+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static String updateSQL = "update contact set CLASS_ID = ?, TYPE_ID = ?, "
		+ "CODE = ?, NAME = ?, PHONE = ?, MOBILE = ?, E_MAIL = ?, FAX = ?, HOMEPAGE = ?, TIME_CREATED = ?, CREATOR = ?, "
		+ "TIME_MODIFIED = ?, MODIFIER = ?, PROPERTIES = ? where ID = ?";
	
	private static String deleteSQL = 
		"delete from contact where TO_OBJ_ID = ? and TO_CLASS_ID = ?";
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	ContactDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}
	
	public ContactsList load(AEDescriptor toDescr) throws AEException {
		ContactsList contactsList = new ContactsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, toDescr.getID());
			ps.setLong(2, toDescr.getClazz() != null ? toDescr.getClazz().getID() : 0L);
			rs = ps.executeQuery();
			while(rs.next()) {
				Contact c = new Contact();
				build(c, rs);
				contactsList.add(c);
			}
		    return contactsList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void build(Contact c, ResultSet rs) throws SQLException {
		// build common attributes
		super.build(c, rs);
		
		// TYPE_ID
		c.setType(Contact.Type.valueOf(rs.getLong("TYPE_ID")));
		// PHONE
		c.setPhone(rs.getString("PHONE"));
		// MOBILE
		c.setMobile(rs.getString("MOBILE"));
		// E_MAIL
		c.seteMail(rs.getString("E_MAIL"));
		// FAX
		c.setFax(rs.getString("FAX"));
		// HOMEPAGE
		c.setHomepage(rs.getString("HOMEPAGE"));
		
		// set this record in view state
		c.setView();
	}
	
	public void insert(ContactsList contacts, AEDescriptor toDescr) throws AEException {
		assert(contacts != null);
		assert(toDescr != null
				&& toDescr.isPersistent()
				&& toDescr.getClazz() != null);
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);
			for (Contact contact : contacts) {
				assert(!contact.isPersistent()); 
				assert(contact.getClazz() != null);
				assert(contact.getType() != null);

				// build prepared statement
				build(ps, contact, toDescr, 1);
				
				// execute
				ps.executeUpdate();
				
				// set generated key
				rs = ps.getGeneratedKeys();
				if (rs.next()) {
					// propagate generated key
					long id = rs.getLong(1);
					
					//required
					contact.setID(id);
				} else {
					throw new AEException(getClass().getName() + "::insert: No keys were generated");
				}
				
				// set view state
				contact.setView();
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
	
	public void update(ContactsList contacts) throws AEException {
		assert(contacts != null);
		PreparedStatement ps = null;
		try {
			// update
			ps = getAEConnection().prepareStatement(updateSQL);
			for (Contact contact : contacts) {
				assert(!contact.isPersistent()); 

				// build prepared statement
				int i = build(ps, contact, null, 1);
				ps.setLong(i++, contact.getID());
				
				// execute
				ps.executeUpdate();
				
				// set view state
				contact.setView();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	public void delete(ContactsList contacts) throws AEException {
		assert(contacts != null);
		PreparedStatement ps = null;
		try {
			// update
			ps = getAEConnection().prepareStatement(insertSQL);
			for (Contact contact : contacts) {
				assert(!contact.isPersistent()); 

				// build prepared statement
//				build(ps, contact);
				
				// execute
				ps.executeUpdate();
				
				// set view state
				contact.setView();
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
	
	private int build(PreparedStatement ps, Contact contact, AEDescriptor toDescr, int i) throws SQLException {
		// CLASS_ID
		if(contact.getClazz() != null) {
			ps.setLong(i++, contact.getClazz().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		// TYPE_ID
		if(contact.getType() != null) {
			ps.setLong(i++, contact.getType().getID());
		}  else {
			ps.setNull(i++, Types.BIGINT);
		}
		// TO_OBJ_ID, TO_CLASS_ID
		// IMPORTANT must exist during insert
		if(toDescr != null) {
			ps.setLong(i++, toDescr.getID());
			if(toDescr.getClazz() != null) {
				ps.setLong(i++, toDescr.getClazz().getID());
			} else {
				ps.setNull(i++, Types.BIGINT);
			}
		}
		// CODE
		ps.setString(i++, contact.getCode());
		// NAME
		ps.setString(i++, contact.getName());
		// PHONE
		ps.setString(i++, contact.getPhone());
		// MOBILE
		ps.setString(i++, contact.getMobile());
		// E-MAIL
		ps.setString(i++, contact.geteMail());
		// FAX
		ps.setString(i++, contact.getFax());
		// HOMEPAGE
		ps.setString(i++, contact.getHomepage());
		// TIME_CREATED
		ps.setTimestamp(i++, AEPersistentUtil.getTimestampNotNull(contact.getTimeCreated()));
		// CREATOR
		ps.setString(i++, contact.getCreator());
		// TIME_MODIFIED
		ps.setTimestamp(i++, AEPersistentUtil.getTimestampNotNull(contact.getTimeModified()));
		// MODIFIER
		ps.setString(i++, contact.getModifier());
		// PROPERTIES
		ps.setLong(i++, contact.getProperties());
		
		return i;
	}
}
