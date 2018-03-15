/**
 * 
 */
package eu.agileeng.services;

/**
 * @author vvatov
 *
 */
public abstract class AEMessage {
	protected static final String HEAD = "HEAD";
	
	protected static final String BODY = "BODY";
	
	protected static final String SID = "sessionId";
	
	protected static final String SERVICE_TYPE = "service_type";
	
	protected static final String METHOD = "method";
	
	protected static final String ARGS = "args";
	
	protected static final String RESULT = "result";
	/**
	 * 
	 */
	protected AEMessage() {
	}
}
