/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 23.05.2010 13:07:42
 */
package eu.agileeng.services;

import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.services.imp.AEInvocationContextImp;

/**
 *
 */
public class AEServiceUtil {

	/**
	 * 
	 */
	public AEServiceUtil() {
	}

	public static AEInvocationContext getInvocationContext(AuthPrincipal authSubject) {
		return new AEInvocationContextImp(authSubject);
	}
}
