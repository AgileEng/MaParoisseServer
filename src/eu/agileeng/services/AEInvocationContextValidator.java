/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 23.05.2010 13:58:34
 */
package eu.agileeng.services;

import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.security.AuthPrincipalCommonValidator;
import eu.agileeng.util.AEValidator;

/**
 *
 */
public class AEInvocationContextValidator implements AEValidator {

	private static AEInvocationContextValidator inst = new AEInvocationContextValidator();
	
	/**
	 * 
	 */
	private AEInvocationContextValidator() {
	}

	public static AEInvocationContextValidator getInstance() {
		return inst;
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.util.AEPredicate#evaluate(java.lang.Object)
	 */
	@Override
	public void validate(Object o) throws AEException {
		if(!(o instanceof AEInvocationContext)) {
			logger.errorv("o is not instanceof AEInvocationContext in {0}#{1}: {2}", this.getClass().getSimpleName(), "validate", AEError.System.INVALID_SESSION.getMessage());
			throw new AEException(AEError.System.INVALID_SESSION);
		}
		
		AEInvocationContext invContext = (AEInvocationContext) o;
		if(invContext.getAuthPrincipal() == null) {
			logger.errorv("invContext.getAuthPrincipal is null in {0}#{1}: {2}", this.getClass().getSimpleName(), "validate", AEError.System.INVALID_SESSION.getMessage());
			throw new AEException(AEError.System.INVALID_SESSION);
		}
		
		AuthPrincipalCommonValidator commonValidator = AuthPrincipalCommonValidator.getInstance();
		invContext.getAuthPrincipal().validateWith(commonValidator);
	}
}
