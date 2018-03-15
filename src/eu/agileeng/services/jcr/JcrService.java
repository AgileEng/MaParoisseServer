/**
 * 
 */
package eu.agileeng.services.jcr;

import eu.agileeng.domain.AEException;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.AEService;

/**
 * @author vvatov
 *
 */
public interface JcrService extends AEService {
	public AEResponse addNode(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse getNode(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse getNodeData(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse deleteNode(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse search(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse moveNode(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse copyNode(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse renameNode(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse checkout(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse cancelCheckout(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse checkin(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse versionHistory(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse restoreVersion(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
}
