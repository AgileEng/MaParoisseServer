/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 09.12.2009 21:01:36
 */
package eu.agileeng.services.contact;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.contact.ContactsList;
import eu.agileeng.services.AEService;

/**
 *
 */
public interface ContactService extends AEService {
	public ContactsList manage(ContactsList contactsList, AEDescriptor toDescr) throws AEException;

}
