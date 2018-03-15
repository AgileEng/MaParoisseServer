/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 09.12.2009 18:46:56
 */
package eu.agileeng.services.address.ejb;

import java.util.Date;

import javax.ejb.Stateless;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.contact.Address;
import eu.agileeng.domain.contact.AddressesList;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.oracle.AddressDAO;
import eu.agileeng.services.AEBean;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.util.AEStringUtil;


/**
 *
 */
@SuppressWarnings("serial")
@Stateless
public class AddressBean extends AEBean implements AddressRemote, AddressLocal {

	/**
	 * 
	 */
	public AddressBean() {
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.services.address.ejb.AddressLocal#manage(eu.agileeng.domain.contact.AddressesList, eu.agileeng.persistent.AEConnection)
	 */
	@Override
	public void manage(AddressesList addressesList, AEDescriptor toDescr, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();
			
			// manage in transaction
			AddressDAO addressDAO = daoFactory.getAddressDAO(localConnection);
			AddressesList addressesForInsert = new AddressesList();
			AddressesList addressesForUpdate = new AddressesList();
			AddressesList addressesForDelete = new AddressesList();
			for (Address address : addressesList) {
				switch(address.getPersistentState()) {
				case NEW:
					prepareInsert(address, null, aeConnection);
					addressesForInsert.add(address);
					break;
				case UPDATED:
					prepareUpdate(address, null, aeConnection);
					addressesForUpdate.add(address);
					break;
				case DELETED:
					addressesForDelete.add(address);
					break;
				case VIEW:
					break;
				default:
					assert(false);
					break;
				}
			}
			addressDAO.insert(addressesForInsert, toDescr);	
			addressDAO.update(addressesForUpdate);
			addressDAO.delete(addressesForDelete);	
			
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

	/* (non-Javadoc)
	 * @see eu.agileeng.services.address.ejb.AddressRemote#manage(eu.agileeng.domain.contact.AddressesList, eu.agileeng.domain.AEDescriptor)
	 */
	@Override
	public AddressesList manage(AddressesList addressesList, AEDescriptor toDescr) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();
			
			// manage in transaction
			manage(addressesList, toDescr, localConnection);
			
			localConnection.commit();
			return addressesList;
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
		aeObject.setCreator(AEStringUtil.EMPTY_STRING);
		aeObject.setTimeCreated(dateNow);
		aeObject.setModifier(AEStringUtil.EMPTY_STRING);
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
		aeObject.setCreator(AEStringUtil.EMPTY_STRING);
		aeObject.setTimeCreated(dateNow);
		aeObject.setModifier(AEStringUtil.EMPTY_STRING);
		aeObject.setTimeModified(dateNow);
	}

	@Override
	public AddressesList load(AEDescriptor toDescr, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			localConnection = daoFactory.getConnection(aeConnection);
	
			AddressDAO addressDAO = daoFactory.getAddressDAO(localConnection);
			AddressesList addresses = addressDAO.load(toDescr);
			return addresses;
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
}
