/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 09.12.2009 17:27:59
 */
package eu.agileeng.services.party.ejb;

import java.util.Date;

import javax.ejb.Local;

import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.contact.Employee;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.contact.Person;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.party.PartyService;
import eu.agileeng.util.AETimePeriod;


/**
 *
 */
@Local
public interface PartyLocal extends PartyService {
	public void manage(Organization organization, AEInvocationContext invContext, AEConnection aeConnection) throws AEException;
	public void loadCustomerInDepth(JSONObject customer, AERequest aeRequest, AEInvocationContext invContext, AEConnection aeConnection) throws AEException;
	public void manage(Person person, AEInvocationContext invContext, AEConnection aeConnection) throws AEException;
	public void save(Employee empl, AEInvocationContext invContext, AEConnection aeConnection) throws AEException;
	public void deleteEmployee(long id, AEInvocationContext invContext, AEConnection aeConnection) throws AEException;
	public void deletePerson(long id, AEInvocationContext invContext, AEConnection aeConnection) throws AEException;
	public AETimePeriod getFiancialPeriod(AEDescriptor compDescr, Date endDate, AEInvocationContext invContext, AEConnection aeConnection) throws AEException;
	public Long generateEmplFtpId(AEDescriptor ownerDescr, AEInvocationContext invContext, AEConnection aeConnection) throws AEException;
}
