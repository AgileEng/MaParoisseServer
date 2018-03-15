/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 20.05.2010 17:21:10
 */
package eu.agileeng.security;

import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;

/**
 *
 */
@SuppressWarnings("serial")
public class AuthException extends AEException {

	/**
	 * @param message
	 */
	public AuthException(String message) {
		super(message);
	}

	public AuthException(long code, String message) {
		super(code, message);
	}
	
	public AuthException(String message, Throwable t) {
		super(message, t);
	}
	
	public AuthException(int code, String message, Throwable cause) {
		super(code, message, cause);
	}
	
	public AuthException(long code, String message, Throwable cause) {
		super((int) code, message, cause);
	}
	
	public AuthException() {
		this(AEError.System.UNSUFFICIENT_RIGHTS.getSystemID(), AEError.System.UNSUFFICIENT_RIGHTS.getMessage());
	}
	
	/**
	 * 
	 * @param code
	 * @param message
	 */
	public AuthException(AEError.System aeError) {
		super(aeError);
	}
}
