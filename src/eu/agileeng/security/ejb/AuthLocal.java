/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 20.05.2010 18:23:13
 */
package eu.agileeng.security.ejb;

import javax.ejb.Local;

import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEException;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.security.AuthService;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;

/**
 *
 */
@Local
public interface AuthLocal extends AuthService {
	
	public AEResponse deactivate(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse resetPassword(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public void createDefaultAuthPrincipal(JSONObject customer, AEInvocationContext invContext, AEConnection aeConnection) throws AEException;
	
}
