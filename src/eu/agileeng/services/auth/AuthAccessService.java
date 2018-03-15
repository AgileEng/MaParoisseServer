package eu.agileeng.services.auth;

import eu.agileeng.accbureau.AEAppModulesList;
import eu.agileeng.domain.AEException;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AEService;

public interface AuthAccessService extends AEService {
	AuthAccessValidator getPath(String code, AEInvocationContext invContext)
			throws AEException;

	AuthAccessValidator getValidator(String code, AEInvocationContext invContext)
			throws AEException;

	AEAppModulesList getAvailableModules(AEInvocationContext invContext)
			throws AEException;
}
