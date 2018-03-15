/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 22.05.2010 12:49:40
 */
package eu.agileeng.services.document.ejb;

import javax.ejb.Local;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.document.AEDocument;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.document.AEDocumentService;

/**
 *
 */
@Local
public interface AEDocumentLocal extends AEDocumentService {
	public AEDocument load(AEDescriptor docDescr, AEConnection aeconnection) throws AEException;
	public AEDocument loadHeader(AEDescriptor docDescr, AEInvocationContext invContext, AEConnection aeConnection) throws AEException;
	public void insert(AEDocument aeDocument, AEInvocationContext invContext, AEConnection aeConnection) throws AEException;
	public void processItems(AEDocument aeDocument, AEInvocationContext invContext, AEConnection aeConnection) throws AEException;
}
