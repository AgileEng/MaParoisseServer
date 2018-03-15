package eu.agileeng.services.auth.ejb;

import javax.ejb.Local;

import eu.agileeng.accbureau.AEAppModulesList;
import eu.agileeng.domain.AEException;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.auth.AuthAccessService;
import eu.agileeng.services.auth.AuthAccessValidator;

@Local
public interface AuthAccessLocal extends AuthAccessService {

	AuthAccessValidator getPath(String code, AEInvocationContext invContext)
			throws AEException;

	AuthAccessValidator getValidator(String code, AEInvocationContext invContext)
			throws AEException;

	AEAppModulesList getAvailableModules(AEInvocationContext invContext)
			throws AEException;

}
