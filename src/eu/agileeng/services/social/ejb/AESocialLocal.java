/**
 * 
 */
package eu.agileeng.services.social.ejb;

import java.util.Date;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.document.AEDocumentDescriptor;
import eu.agileeng.domain.document.social.SocialTimeSheet;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.social.AESocialService;

/**
 * @author vvatov
 *
 */
public interface AESocialLocal extends AESocialService {
	public void delete(AEDocumentDescriptor docDescr, AEDescriptor ownerDescr, boolean deleteEmployee, AEInvocationContext invContext, AEConnection aeConnection) throws AEException;
	public SocialTimeSheet createSchedule(AEDescriptor ownerDescr, Date startDateRaw, Date endDateRaw, AEConnection aeConnection) throws AEException;
}
