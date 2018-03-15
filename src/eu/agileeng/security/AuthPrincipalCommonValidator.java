/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 23.05.2010 13:53:29
 */
package eu.agileeng.security;

import eu.agileeng.domain.AEDescriptorsList;
import eu.agileeng.domain.AEDescriptorsSet;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AEValidator;

/**
 *
 */
public class AuthPrincipalCommonValidator implements AEValidator {

	private static AuthPrincipalCommonValidator inst = new AuthPrincipalCommonValidator();

	/**
	 * 
	 */
	private AuthPrincipalCommonValidator() {
	}

	public static AuthPrincipalCommonValidator getInstance() {
		return inst;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.util.AEPredicate#evaluate(java.lang.Object)
	 */
	@Override
	public void validate(Object o) throws AuthException {
		
		if(!(o instanceof AuthPrincipal)) {
			throw new AuthException("Security Violation, Incorrect Authentication Principal. ");
		}
		
		// the principal
		AuthPrincipal authPrincipal = (AuthPrincipal) o;
		
		// must be authenticated
		if(!authPrincipal.isAuthenticated()) {
			throw new AuthException("Security Violation, The principal is not authenticated. ");
		}
		
		// the login name cannot be empty
		if(AEStringUtil.isEmpty(authPrincipal.getName())) {
			throw new AuthException("Security Violation, The login name cannot be empty. ");
		}

		// at least one role
		AEDescriptorsList roles = authPrincipal.getRolesList();
		if(roles == null || roles.isEmpty()) {
			throw new AuthException("Security Violation, The principal must be associated with at least one role. ");
		}
		
		// must be active and not locked
		// don't check for expired account
		if(!authPrincipal.isActive() || authPrincipal.isLocked()) { 
			throw new AuthException("Security Violation, The principal is disabled. ");
		}

		if(!authPrincipal.isMemberOf(AuthRole.System.technician) 
				&& !authPrincipal.isMemberOf(AuthRole.System.administrator)
				&& !authPrincipal.isMemberOf(AuthRole.System.power_user)) {
			
			// at least one associated customer (tenant)
			AEDescriptorsSet companies = authPrincipal.getCompaniesSet();
			if(companies == null || companies.isEmpty()) {
				throw new AuthException("Security Violation, The principal must be associated with at least one customer. ");
			}
		}

//		// assigned customer
//		AEDescriptive tenant = authPrincipal.getCompany().getDescriptor();
//		if(tenant == null || !tenant.getDescriptor().isPersistent()) {
//			throw new AuthException("Security Violation:  The principal must be assigned to a valid customer");
//		}
//		
//		// assigned customer is a valid one
//		if(!companies.contains(tenant)) {
//			throw new AuthException("Security Violation: Assigned customer is not associated to the principal");
//		}
	}
}
