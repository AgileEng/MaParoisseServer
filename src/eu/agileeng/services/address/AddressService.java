/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 09.12.2009 18:42:27
 */
package eu.agileeng.services.address;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.contact.AddressesList;
import eu.agileeng.services.AEService;

/**
 *
 */
public interface AddressService extends AEService {
	public AddressesList manage(AddressesList addressesList, AEDescriptor toDescr) throws AEException;
}
