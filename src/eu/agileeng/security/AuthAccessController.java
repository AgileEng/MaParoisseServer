package eu.agileeng.security;

import java.security.PermissionCollection;
import java.util.HashMap;
import java.util.Map;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.services.AEInvocationContext;


/**
 * <p> The AuthAccessController class is used for access control operations and decisions.
 * This class is implementation of owasp's best practice: Implies policy is persisted/centralized in some way
 *
 *<p>
 *TODO 
 *<li>Make it persistent and ensure the framework to use it
 *<li>Introduce the Cache for all the all app and use it here
 *
 * @author Vesko Vatov
 */
public final class AuthAccessController {

	private static final Map<Long, PermissionCollection> accessList = createAccessList();
	
	private static final Map<Long, PermissionCollection> denyList = createDenyList();
	
	// TODO 
	// 1. Make it persistent and ensure the framework to use it
	// 2. Introduce the Cache for all app and use it here
	
	// available
	// System/Configuration
	//   - System/Configuration/FinancialTransactionTemplate
	//   - System/Configuration/Customer
	//   - System/Configuration/Customer/ChartOfAccount
	//   - System/Configuration/Customer/InitialBalance
	//   - System/Configuration/Customer/ChartOfAccountModel
	//   - System/Configuration/Customer/SocialInfo
	//   - System/Configuration/Customer/Councill
	//   - System/Configuration/Customer/BankAccount
	//   - System/Configuration/Customer/Journal
	//   - System/Configuration/Customer/AccPeriod
	// System/Party
	//   - System/Party/Person
	//   - System/Party/Organization
	//   - System/Party/Customer
	//   - System/Party/OrganizationTemplate
	// System/FinancialTransaction
	//   - System/FinancialTransaction/Contributor
	//   - System/FinancialTransaction/Attachment
	//   - System/FinancialTransaction/BordereauParoisse
	// System/TmpRepository
	// System/FileAttachment
	synchronized private static Map<Long, PermissionCollection> createAccessList() {
		Map<Long, PermissionCollection> al = new HashMap<Long, PermissionCollection>();
		
		// administrator
		// Grants full permissions over configurations and data with business rules applied
		AuthPermissionCollection sysAdmin = new AuthPermissionCollection();
		sysAdmin.add(new AuthPermission("System/-", AuthPermission.ALL));
		sysAdmin.add(new AuthPermission("MaParoisse/-", AuthPermission.ALL)); 
		al.put(AuthRole.System.administrator.getSystemID(), sysAdmin);
		
		// Poweruser
		// Grants all SystemAdmin permissions with the exception of global system configurations
		AuthPermissionCollection powerUser = new AuthPermissionCollection();
		powerUser.add(new AuthPermission("System/-", AuthPermission.ALL));
		powerUser.add(new AuthPermission("MaParoisse/-", AuthPermission.ALL)); 
		al.put(AuthRole.System.power_user.getSystemID(), powerUser);
		
		// Operative
		// User
		AuthPermissionCollection user = new AuthPermissionCollection();
		user.add(new AuthPermission("System/-", AuthPermission.ALL));
		user.add(new AuthPermission("MaParoisse/-", AuthPermission.ALL));
		al.put(AuthRole.System.operative.getSystemID(), user);
		
		// ReadOnly Operative User
		AuthPermissionCollection readOnlyUser = new AuthPermissionCollection();
		readOnlyUser.add(new AuthPermission("System/-", AuthPermission.READ));
		readOnlyUser.add(new AuthPermission("MaParoisse/-", AuthPermission.READ));
		al.put(AuthRole.System.operative_read_only.getSystemID(), readOnlyUser);
		
		return al;
	}
	
	synchronized private static Map<Long, PermissionCollection> createDenyList() {
		Map<Long, PermissionCollection> al = new HashMap<Long, PermissionCollection>();
		
		// Administrator
		AuthPermissionCollection sysAdmin = new AuthPermissionCollection();
		al.put(AuthRole.System.administrator.getSystemID(), sysAdmin);
		
		// Poweruser
		AuthPermissionCollection powerUser = new AuthPermissionCollection();
		powerUser.add(new AuthPermission("System/Configuration/Customer/ChartOfAccountModel", AuthPermission.SAVE_AND_DELETE));
		al.put(AuthRole.System.power_user.getSystemID(), powerUser);
		
		// Operative User
		AuthPermissionCollection user = new AuthPermissionCollection();
		user.add(new AuthPermission("System/Configuration/Customer/ChartOfAccountModel", AuthPermission.SAVE_AND_DELETE));

		user.add(new AuthPermission("System/Bibliotheque", AuthPermission.DELETE));
		user.add(new AuthPermission("System/Bibliotheque/-", AuthPermission.DELETE));
		user.add(new AuthPermission("System/Bibliotheque", AuthPermission.SAVE));
		user.add(new AuthPermission("System/Bibliotheque/-", AuthPermission.SAVE));
		user.add(new AuthPermission("System/Bibliotheque", AuthPermission.CREATE));
		user.add(new AuthPermission("System/Bibliotheque/-", AuthPermission.CREATE));
		
		user.add(new AuthPermission("System/Security", AuthPermission.ALL));
		user.add(new AuthPermission("System/Security/-", AuthPermission.ALL));
		
		user.add(new AuthPermission("MaParoisse/60a1/315b3", AuthPermission.ALL));
		user.add(new AuthPermission("MaParoisse/60a1/315b3/-", AuthPermission.ALL));
		user.add(new AuthPermission("MaParoisse/60a1/325b3", AuthPermission.ALL));
		user.add(new AuthPermission("MaParoisse/60a1/325b3/-", AuthPermission.ALL));
		user.add(new AuthPermission("MaParoisse/10a1/10a2", AuthPermission.ALL));
		user.add(new AuthPermission("MaParoisse/10a1/10a2/-", AuthPermission.ALL));
		al.put(AuthRole.System.operative.getSystemID(), user);
		
		// ReadOnly Operative User
		AuthPermissionCollection readOnlyUser = new AuthPermissionCollection();
		
		readOnlyUser.add(new AuthPermission("System/-", AuthPermission.SAVE_AND_DELETE));
		readOnlyUser.add(new AuthPermission("MaParoisse/-", AuthPermission.SAVE_AND_DELETE));
		
		readOnlyUser.add(new AuthPermission("System/Configuration/Customer/ChartOfAccountModel", AuthPermission.SAVE_AND_DELETE));

		readOnlyUser.add(new AuthPermission("System/Bibliotheque", AuthPermission.DELETE));
		readOnlyUser.add(new AuthPermission("System/Bibliotheque/-", AuthPermission.DELETE));
		readOnlyUser.add(new AuthPermission("System/Bibliotheque", AuthPermission.SAVE));
		readOnlyUser.add(new AuthPermission("System/Bibliotheque/-", AuthPermission.SAVE));
		readOnlyUser.add(new AuthPermission("System/Bibliotheque", AuthPermission.CREATE));
		readOnlyUser.add(new AuthPermission("System/Bibliotheque/-", AuthPermission.CREATE));
		
		readOnlyUser.add(new AuthPermission("System/Security", AuthPermission.ALL));
		readOnlyUser.add(new AuthPermission("System/Security/-", AuthPermission.ALL));
		
		readOnlyUser.add(new AuthPermission("MaParoisse/60a1/315b3", AuthPermission.ALL));
		readOnlyUser.add(new AuthPermission("MaParoisse/60a1/315b3/-", AuthPermission.ALL));
		readOnlyUser.add(new AuthPermission("MaParoisse/60a1/325b3", AuthPermission.ALL));
		readOnlyUser.add(new AuthPermission("MaParoisse/60a1/325b3/-", AuthPermission.ALL));
		readOnlyUser.add(new AuthPermission("MaParoisse/10a1/10a2", AuthPermission.ALL));
		readOnlyUser.add(new AuthPermission("MaParoisse/10a1/10a2/-", AuthPermission.ALL));
		al.put(AuthRole.System.operative_read_only.getSystemID(), readOnlyUser);
		
		return al;
	}
	
	/**
	 * Don't allow anyone to instantiate an AccessController
	 */
	private AuthAccessController() {
	}
	
	/**
	 * Determines whether the access request indicated by the
	 * specified permission should be allowed or denied, based on
	 * the specified invocation context.
	 * 
	 * This method quietly returns if the access request
	 * is permitted, or throws a suitable AuthException otherwise.
	 *
	 * 1. The user is granted permissions that are a unuion of group's permission
	 *    that the user is member of;
	 * 2. Individual user's psrmissions overrides all implied group permissions. 
	 *
	 * @param perm the requested permission.
	 *
	 * @exception AuthException if the specified permission
	 *            is not permitted, based on the specified invocation context.
	 * @exception NullPointerException if the specified permission or specified <code>context</code>
	 *            is/are <code>null</code>.
	 */
	public static void checkPermission(AuthPermission perm, AEInvocationContext context) throws AuthException {
        if (perm == null) {
            throw new NullPointerException("Security Violation: Permission can't be null");
        }
        if (context == null) {
            throw new NullPointerException("Security Violation: Invocation context can't be null");
        }

        // get the principal
        AuthPrincipal authPrincipal = context.getAuthPrincipal();
        
        if (authPrincipal == null) {
        	throw new AuthException("Security Violation: The principal can't be null");
        }
        
        // check the permission
        authPrincipal.checkPermission(perm);
	}

	/**
	 * TODO: Synchronize in case of dynamic environment
	 * 
	 * @param roleDescriptor
	 * @return
	 */
	/*synchronized*/ static PermissionCollection getPermissionCollection(AEDescriptor roleDescriptor) {
		return accessList.get(roleDescriptor.getSysId());
	}
	
	/*synchronized*/ static PermissionCollection getPermissionCollectionDeny(AEDescriptor roleDescriptor) {
		return denyList.get(roleDescriptor.getSysId());
	}
}
