package eu.agileeng.services.utility;

import eu.agileeng.domain.AEException;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.AEService;

public interface AEUtilityService extends AEService {
	public AEResponse loadBookmarks(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse saveBookmarks(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse importAopca(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public void checkDBStatus() throws AEException;
}
