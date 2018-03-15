/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 23.05.2010 12:18:52
 */
package eu.agileeng.services.imp;

import eu.agileeng.domain.AEException;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.util.AEDynamicProperties;
import eu.agileeng.util.AEValidator;

/**
 *
 */
@SuppressWarnings("serial")
public class AEInvocationContextImp implements AEInvocationContext {

	private AuthPrincipal authSubject;
	
	private AEDynamicProperties dynProperties = new AEDynamicProperties();
	
	/**
	 * 
	 */
	public AEInvocationContextImp() {
	}

	/**
	 * @param authSubject
	 */
	public AEInvocationContextImp(AuthPrincipal authSubject) {
		this.authSubject = authSubject;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.services.AEInvocationContext#containsProperty(java.lang.String)
	 */
	@Override
	public boolean containsProperty(String propName) {
		return dynProperties.containsKey(propName);
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.services.AEInvocationContext#getAuthSubject()
	 */
	@Override
	public AuthPrincipal getAuthPrincipal() {
		return this.authSubject;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.services.AEInvocationContext#getProperty(java.lang.String)
	 */
	@Override
	public Object getProperty(String propName) {
		return this.dynProperties.get(propName);
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.services.AEInvocationContext#setAuthSubject(eu.agileeng.security.AuthSubject)
	 */
	@Override
	public void setAuthSubject(AuthPrincipal authSubject) {
		this.authSubject = authSubject;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.services.AEInvocationContext#setProperty(java.lang.String, java.lang.Object)
	 */
	@Override
	public void setProperty(String propName, Object propValue) {
		this.dynProperties.put(propName, propValue);
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.util.AEValidatable#validate(eu.agileeng.util.AEPredicate)
	 */
	@Override
	public void validateWith(AEValidator validator) throws AEException {
		if(validator != null) {
			validator.validate(this);
		}
	}

	@Override
	public Object removeProperty(String propName) {
		return this.dynProperties.remove(propName);
	}

	@Override
	public void setAEConnection(eu.agileeng.persistent.AEConnection aeConnection) {
		if(aeConnection != null) {
			setProperty(AEInvocationContext.AEConnection, aeConnection);
		}
	}

	@Override
	public eu.agileeng.persistent.AEConnection getAEConnection() {
		return (eu.agileeng.persistent.AEConnection) getProperty(AEInvocationContext.AEConnection);
	}

	@Override
	public eu.agileeng.persistent.AEConnection removeAEConnection() {
		return (eu.agileeng.persistent.AEConnection) removeProperty(AEInvocationContext.AEConnection);
	}
}
