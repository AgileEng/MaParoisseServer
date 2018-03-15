/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 23.05.2010 12:11:05
 */
package eu.agileeng.services;

import java.io.Serializable;

import eu.agileeng.persistent.AEConnection;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.util.AEValidatable;

/**
 * Context information passed to <code>AEService's</code> methods.
 */
public interface AEInvocationContext extends Serializable, AEValidatable {
	
	public final static String AEConnection = eu.agileeng.persistent.AEConnection.class.getName();  
	
	public final static String JCRSession = eu.agileeng.persistent.jcr.JcrSession.class.getName();  
	
	public final static String HttpSessionId = javax.servlet.http.HttpSession.class.getName();  
	
	public final static String METoken = "METoken";
	
	public void setAuthSubject(AuthPrincipal authSubject);
	
	public AuthPrincipal getAuthPrincipal();
	
	public void setProperty(String propName, Object propValue);
	
	public Object getProperty(String propName);
	
	public boolean containsProperty(String propName);
	
	public Object removeProperty(String propName);
	
	public void setAEConnection(AEConnection aeConnection);
	
	public AEConnection getAEConnection();
	
	public AEConnection removeAEConnection();
}
