/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 09.12.2009 18:46:23
 */
package eu.agileeng.services.address.ejb;

import javax.ejb.Local;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.contact.AddressesList;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.services.AEService;


/**
 *
 */
@Local
public interface AddressLocal extends AEService {
	public void manage(AddressesList addressesList, AEDescriptor toDescr, AEConnection aeConnection) throws AEException;
	public AddressesList load(AEDescriptor toDescr, AEConnection aeConnection) throws AEException;
}
