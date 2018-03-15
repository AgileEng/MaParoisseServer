/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 20.05.2010 17:18:04
 */
package eu.agileeng.security;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.contact.SubjectCompAssoc;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.AEService;

/**
 *
 */
public interface AuthService extends AEService {
	/**
	 * Checks user-role assignment for specified 
	 * <code>authSubjectDescr</code> and <code>authRoleDescr</code>.
	 * 
	 * @param authSubjectDescr the subject (user or group) under assignment check.
	 * @param authRoleDescr the role under assignment check.
	 * @return Returns <code>true</code> if specified <code>authSubject</code> is
	 * 			assigned to the specified <code>authRoleDescr</code>, 
	 * 			otherwise returns <code>false</code>.
	 * @throws AuthException in case of method failure.
	 */
	public boolean hasRole(AEDescriptor authSubjectDescr, AEDescriptor authRoleDescr) throws AuthException;
	
//	/**
//	 * Returns true if specified <code>authSubjectDescr</code> is permitted to perform 
//	 * an action or access a resource summarized 
//	 * by the specified <code>authPermissionDescr</code>. 
//	 * 
//	 * @param authSubjectDescr the subject (user or group) under authorization.
//	 * @param authPermissionDescr the permission under authorization.
//	 * @return
//	 * @throws AuthException in case of method failure
//	 */
//	public boolean isPermitted(AEDescriptor authSubjectDescr, AEDescriptor authPermissionDescr) throws AuthException;
	
	/**
	 * Attempts to login specified <code>authLoginToken</code>.
	 * 
	 * @param authLoginToken
	 * @return Authenticated subject in case of success.
	 * @throws AuthException in case of failure or authentication failure.
	 */
	public AuthPrincipal login(AuthLoginToken authLoginToken) throws AuthException;
	
	public AEResponse savePrincipal(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse loadPrincipal(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public SubjectCompAssoc save(SubjectCompAssoc assoc, AEInvocationContext invContext) throws AEException;
	
	public AuthPrincipal loadAuthSubject(AEDescriptor asDescr, AEInvocationContext invContext) throws AEException;
	
	public AuthPrincipalsList loadAuthPrincipalsAll(AEInvocationContext invContext) throws AEException;
	
	public AEResponse loadAuthPrincipalsManageableBy(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AuthRole save(AuthRole ar, AEInvocationContext invContext) throws AEException;
	
	public AuthRole loadAuthRole(AEDescriptor arDescr, AEInvocationContext invContext) throws AEException;
	
	public AEResponse loadAuthRolesAll(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AuthSubjectRoleAssoc save(AuthSubjectRoleAssoc assoc, AEInvocationContext invContext) throws AEException;
	
	public AuthSubjectRoleAssoc load(AEDescriptor assocDescr, AEInvocationContext invContext) throws AEException;
	
	public AuthRolesSet getAssociatedRoles(AEDescriptor subjDescr, AEInvocationContext invContext) throws AEException; 
	
	public AuthPrincipalsList getAssociatedSubjects(AEDescriptor roleDescr, AEInvocationContext invContext) throws AEException;
	
	public AEResponse loadAppConfig(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse saveAppConfig(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse unlock(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse logoff(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse loadPrincipalsInitialData(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
}
