/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 09.12.2009 21:02:33
 */
package eu.agileeng.services.contact.ejb;

import javax.ejb.Local;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.contact.ContactsList;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.services.contact.ContactService;


/**
 *
 */
@Local
public interface ContactLocal extends ContactService {
	public void manage(ContactsList contactsList, AEDescriptor toDescr, AEConnection aeConnection) throws AEException;
	public ContactsList load(AEDescriptor toDescr, AEConnection aeConnection) throws AEException;
}
