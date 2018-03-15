/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 09.12.2009 21:03:10
 */
package eu.agileeng.services.contact.ejb;

import java.util.Date;

import javax.ejb.Stateless;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.contact.Contact;
import eu.agileeng.domain.contact.ContactsList;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.oracle.ContactDAO;
import eu.agileeng.services.AEBean;
import eu.agileeng.services.AEInvocationContext;


/**
 *
 */
@SuppressWarnings("serial")
@Stateless
public class ContactBean extends AEBean implements ContactRemote, ContactLocal {

	/**
	 * 
	 */
	public ContactBean() {
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.services.contact.ContactService#manage(eu.agileeng.domain.contact.ContactsList, eu.agileeng.domain.AEDescriptor)
	 */
	@Override
	public ContactsList manage(ContactsList contactsList, AEDescriptor toDescr) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();
			
			// manage in transaction
			manage(contactsList, toDescr, localConnection);
			
			localConnection.commit();
			return contactsList;
		} catch (Throwable t) {
			t.printStackTrace();
			if(localConnection != null) {
				localConnection.rollback();
			}
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.services.contact.ejb.ContactLocal#manage(eu.agileeng.domain.contact.ContactsList, eu.agileeng.domain.AEDescriptor, eu.agileeng.persistent.AEConnection)
	 */
	@Override
	public void manage(ContactsList contactsList, AEDescriptor toDescr, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();
			
			// manage in transaction
			ContactDAO contactDAO = daoFactory.getContactDAO(localConnection);
			ContactsList contactsForInsert = new ContactsList();
			ContactsList contactsForUpdate = new ContactsList();
			ContactsList contactsForDelete = new ContactsList();
			for (Contact contact : contactsList) {
				switch(contact.getPersistentState()) {
				case NEW:
					prepareInsert(contact, null, aeConnection);
					contactsForInsert.add(contact);
					break;
				case UPDATED:
					prepareUpdate(contact, null, aeConnection);
					contactsForUpdate.add(contact);
					break;
				case DELETED:
					contactsForDelete.add(contact);
					break;
				case VIEW:
					break;
				default:
					assert(false);
					break;
				}
			}
			contactDAO.insert(contactsForInsert, toDescr);	
			contactDAO.update(contactsForUpdate);
			contactDAO.delete(contactsForDelete);	
			
			localConnection.commit();
		} catch (Throwable t) {
			t.printStackTrace();
			if(localConnection != null) {
				localConnection.rollback();
			}
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	/**
 	 * private where no need for different  
	 */
	private void prepareInsert(AEDomainObject aeObject, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		/**
		 * validate insert
		 */
		
		/**
		 * prepare
		 */
		Date dateNow = new Date();
		aeObject.setCreator("sys");
		aeObject.setTimeCreated(dateNow);
		aeObject.setModifier("sys");
		aeObject.setTimeModified(dateNow);
	}
	
	/**
 	 * private where no need for different  
	 */
	private void prepareUpdate(AEDomainObject aeObject, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		/**
		 * validate update
		 */
		
		/**
		 * prepare
		 */
		Date dateNow = new Date();
		aeObject.setCreator("sys");
		aeObject.setTimeCreated(dateNow);
		aeObject.setModifier("sys");
		aeObject.setTimeModified(dateNow);
	}

	@Override
	public ContactsList load(AEDescriptor toDescr, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			localConnection = daoFactory.getConnection(aeConnection);
			
			ContactDAO contactDAO = daoFactory.getContactDAO(localConnection);
			ContactsList contacts = contactDAO.load(toDescr);
			return contacts;
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
}
