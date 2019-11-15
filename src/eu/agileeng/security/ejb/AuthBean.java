/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 20.05.2010 18:24:58
 */
package eu.agileeng.security.ejb;

import java.util.Date;
import java.util.Iterator;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONObject;
import org.jboss.logging.Logger;

import eu.agileeng.accbureau.AEApp;
import eu.agileeng.accbureau.AEAppConfig;
import eu.agileeng.accbureau.AEAppConfigList;
import eu.agileeng.accbureau.AEAppModule;
import eu.agileeng.accbureau.AEAppModulesList;
import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDescriptorsList;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.contact.SubjectCompAssoc;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.ee.cdi.event.LogEvent;
import eu.agileeng.ee.cdi.event.Login;
import eu.agileeng.ee.cdi.event.Logoff;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistent;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.app.AppDAO;
import eu.agileeng.persistent.dao.oracle.OrganizationDAO;
import eu.agileeng.persistent.dao.oracle.SubjectCompAssocDAO;
import eu.agileeng.security.AuthException;
import eu.agileeng.security.AuthLoginToken;
import eu.agileeng.security.AuthPermission;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.security.AuthPrincipalsList;
import eu.agileeng.security.AuthRole;
import eu.agileeng.security.AuthRolesSet;
import eu.agileeng.security.AuthSubjectRoleAssoc;
import eu.agileeng.security.PasswordHash;
import eu.agileeng.security.AuthPrincipal.AppType;
import eu.agileeng.security.ejb.dao.AuthPrincipalDAO;
import eu.agileeng.security.ejb.dao.AuthRoleDAO;
import eu.agileeng.security.ejb.dao.AuthSubjectRoleAssocDAO;
import eu.agileeng.services.AEBean;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AEInvocationContextValidator;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.party.ejb.PartyLocal;
import eu.agileeng.services.utility.ejb.AEUtilityLocal;
import eu.agileeng.util.AECollectionUtil;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.json.JSONUtil;

/**
 *
 */
@Stateless
@Named
public class AuthBean extends AEBean implements AuthLocal {

	private static final Logger logger = Logger.getLogger(AuthBean.class);
	
	private static final long serialVersionUID = -3322469967344491085L;
	
	public static final int FAILED_LOGIN_MAX = 5;
	
	@EJB private AEUtilityLocal utilityService;
	@EJB private PartyLocal partyService;
	
    @Inject
    @Login
    Event<LogEvent> loginEvent;

    @Inject
    @Logoff
    Event<LogEvent> logoffEvent;
	
	///////////  cdi test finish
	
	/* (non-Javadoc)
	 * @see eu.agileeng.security.AuthService#hasRole(eu.agileeng.domain.AEDescriptor, eu.agileeng.domain.AEDescriptor)
	 */
	@Override
	public boolean hasRole(AEDescriptor authSubjectDescr, AEDescriptor authRoleDescr) throws AuthException {
		return false;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.security.AuthService#login(eu.agileeng.security.AuthLoginToken)
	 */
	@Override
	public AuthPrincipal login(AuthLoginToken authLoginToken) throws AuthException {
		AEConnection localConnection = null;
		String password = null;
		try {
			// load principal
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			AuthPrincipalDAO asDAO = daoFactory.getAuthPrincipalDAO(localConnection);
			AuthPrincipal as = asDAO.load(authLoginToken);

			if(as == null) {
				throw new AuthException(AEError.System.INVALID_CREDENTIALS);
			}
			
			password = new String(authLoginToken.getPassword());
			
			// don't allow empty password
			if (AEStringUtil.isEmpty(password)) {
				asDAO.loginFailed(as.getID());
				throw new AuthException("Connexion impossible, Mot de passé non renseigné: " + authLoginToken.getUsername());
			}
			
			// don't let disabled users log in
			if (!as.isActive()) {
				asDAO.loginFailed(as.getID());
				throw new AuthException("Connexion impossible, Ce nom d’utilisateur est désactivé: " + authLoginToken.getUsername());
			}
			
			// don't let locked users log in
			if (as.isLocked()) {
				asDAO.loginFailed(as.getID());
				throw new AuthException("Connexion impossible, Ce nom d’utilisateur a été bloqué: " + authLoginToken.getUsername());
			}
			
			// don't look for expired account: expired user must change its password			
			
			// verify password
			if (verifyPassword(as, password)) {
				asDAO.loginSuccessed(authLoginToken.getRemoteAddress(), as.getID());
			} else {
				asDAO.loginFailed(as.getID());
				as.setFailedLoginCount(as.getFailedLoginCount() + 1);
				if (as.getFailedLoginCount() >= FAILED_LOGIN_MAX) {
					asDAO.lock(as.getID());
					AEApp.logger().info("User locked: " + as.getName());
				}
				throw new AuthException("Connexion impossible. Nom d’utilisateur ou mot de passe erroné.");
			}
			
			// load
			loadInDepth(as, localConnection);
			
			// module configuration
			if(AuthRole.isOperative(as) && !AECollectionUtil.isEmpty(as.getCompaniesSet())) {
				AppDAO appDAO = daoFactory.getAppDAO(localConnection);
				AEAppConfigList configList = appDAO.loadByCompany(as.getCompaniesSet().iterator().next().getDescriptor());
				as.setAppConfigList(configList);
			}
			
			AEDescriptorsList hiddenModules = new AEDescriptorsList();
			if(!"secal.png".equalsIgnoreCase(as.getDescription())) {
				hiddenModules.add(AEAppModule.lazyDescriptor(AEApp.PURCHASE_FNP_MODULE_ID));
				hiddenModules.add(AEAppModule.lazyDescriptor(AEApp.PERIMES_MODULE_ID));
				hiddenModules.add(AEAppModule.lazyDescriptor(AEApp.STOCKS_MODULE_ID));
				hiddenModules.add(AEAppModule.lazyDescriptor(AEApp.SUIVI_GPL_MODULE_ID));
				hiddenModules.add(AEAppModule.lazyDescriptor(AEApp.DONNEES_MODULE_ID));
				hiddenModules.add(AEAppModule.lazyDescriptor(AEApp.PAM_MODULE_ID));
				hiddenModules.add(AEAppModule.lazyDescriptor(AEApp.IDE_MODULE_ID));
			}
			as.setHiddenModules(hiddenModules);
			
			// final setup
			as.setAuthenticated(true);
			
			// clear password !!!
			as.setPassword(null);
			
//			// hardcode appType
//			if("fabrique".equalsIgnoreCase(as.getName())) {
//				as.setAppType(AppType.fabrique);
//			} else if("mense".equalsIgnoreCase(as.getName())) {
//				as.setAppType(AppType.mense);
//			}
			
			// notify observers
			LogEvent afterLogin = new LogEvent();
			afterLogin.setPrincipalName(authLoginToken.getUsername());
			loginEvent.fire(afterLogin);

			return as;
		} catch (Throwable t) {
			AEApp.logger().
				fatal(authLoginToken.getUsername() + ", " + authLoginToken.getRemoteAddress(), 
				t);
			throw new AuthException(t.getMessage(), t);
		} finally {
			password = null;
			AEConnection.close(localConnection);
		}
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.security.AuthService#loadAuthRole(eu.agileeng.domain.AEDescriptor, eu.agileeng.services.AEInvocationContext)
	 */
	@Override
	public AuthRole loadAuthRole(AEDescriptor arDescr, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// TODO authorize
			
			// get connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			AuthRoleDAO arDAO = daoFactory.getAuthRoleDAO(localConnection);
			AuthRole ar = arDAO.load(arDescr);

			return ar;
		} catch (Throwable t) {
			throw new AEException("Cannot load AuthRole. ", t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.security.AuthService#loadAuthSubject(eu.agileeng.domain.AEDescriptor, eu.agileeng.services.AEInvocationContext)
	 */
	@Override
	public AuthPrincipal loadAuthSubject(AEDescriptor asDescr, AEInvocationContext invContext) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			/**
			 * Authorize 
			 */
			checkPermission(new AuthPermission(AEAppModule.SECURITY, AuthPermission.READ), invContext);
			
			// get connection
			localConnection = daoFactory.getConnection();
			
			// load subject
			AuthPrincipalDAO asDAO = daoFactory.getAuthPrincipalDAO(localConnection);
			AuthPrincipal as = asDAO.load(asDescr);
			
			// load in depth
			if(as != null) {
				loadInDepth(as, localConnection);
			}
			
			return as;
		} catch (Throwable t) {
			throw new AEException("Cannot load loadAuthSubject. ", t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/**
	 * Loads all data related to the specified <code>authPrincipal</code>.
	 * Note: This methods loads data without authorization check.
	 * 
	 * @param authPrincipal
	 * @param aeConnection
	 * @throws AEException
	 */
	private void loadInDepth(AuthPrincipal authPrincipal, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			AEDescriptor authPrincipalDescr = authPrincipal.getDescriptor();
			
			// load associated roles
			AuthRoleDAO roleDAO = daoFactory.getAuthRoleDAO(localConnection);
			AuthRolesSet rolesList = roleDAO.loadAssociatedRoles(authPrincipalDescr);
			for (AuthRole authRole : rolesList) {
				authPrincipal.addRole(authRole.getDescriptor());
			}
			
			// load assigned customers
			OrganizationDAO compDAO = daoFactory.getOrganizationDAO(localConnection);
			AEDescriptorsList customersList = null;
			if(authPrincipal.isMemberOf(AuthRole.System.technician) || authPrincipal.isMemberOf(AuthRole.System.administrator)) {
				// load all customers 
				// the members of specified roles have full rights over each customer
				customersList = compDAO.loadAllCustomers(); 
			} else {
				// load assigned to authPrincipalDescr customers
				customersList = compDAO.loadToPrincipalDescriptor(authPrincipalDescr);
			}
			
			// register customers to specified principal
			for (AEDescriptor party : customersList) {
				authPrincipal.addCompany(party.getDescriptor());
			}
		} catch (Throwable t) {
			logger.errorv("{0} in {1}#{2}: {3}", t.getClass().getSimpleName(), this.getClass().getSimpleName(), "loadInDepth", t.getMessage());
			throw new AEException(t); // keep the cause message
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.security.AuthService#save(eu.agileeng.security.AuthSubject, eu.agileeng.services.AEInvocationContext)
	 */
	@Override
	public AEResponse savePrincipal(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// common validation
			// validate invocation context
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// authorization 
			checkPermission(new AuthPermission(AEAppModule.SECURITY, AuthPermission.SAVE), invContext);
			
			// get connection and begin transaction
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			localConnection.beginTransaction();

			// parse and translate JSON attributes
			JSONObject jsonPrincipal = aeRequest.getArguments().optJSONObject("principal");
			AuthPrincipal principal = new AuthPrincipal();
			principal.create(jsonPrincipal);
			
			// save in transaction
			switch(principal.getPersistentState()) {
			case NEW:
				insert(principal, invContext, localConnection);
				break;
			case UPDATED:
				update(principal, invContext, localConnection);
				break;
			default:
				break;
			}

			// manage roles
			AuthSubjectRoleAssocDAO rolesDAO = daoFactory.getAuthSubjectRoleAssocDAO(localConnection);
			rolesDAO.deleteTo(principal.getDescriptor());
			for (AEDescriptor role : principal.getRolesList()) {
				AuthSubjectRoleAssoc assoc =  new AuthSubjectRoleAssoc(principal.getDescriptor(), role.getDescriptor());
				insert(assoc, invContext, localConnection);
			}

			// manage companies
			if(principal.getCompany() != null 
					&& principal.getCompany().getDescriptor().isPersistent()
					&& !principal.getCompaniesSet().contains(principal.getCompany().getDescriptor())) {
				
				principal.getCompaniesSet().add(principal.getCompany().getDescriptor());
			}
			SubjectCompAssocDAO compDAO = daoFactory.getSubjectCompAssocDAO(localConnection);
			compDAO.deleteTo(principal.getDescriptor());
			for (AEDescriptor company : principal.getCompaniesSet()) {
				SubjectCompAssoc assoc = new SubjectCompAssoc(principal.getDescriptor(), company.getDescriptor());
				prepareInsert(assoc, invContext, localConnection);
				compDAO.insert(assoc);
			}
			
			localConnection.commit();
			
			// return response
			JSONObject payload = principal.toJSONObject();
			return new AEResponse(payload);
		} catch (AEException e) {
			localConnection.rollback();
			throw e;
		} catch (Throwable t) {
			localConnection.rollback();
			throw new AEException((int) AEError.System.NA.getSystemID(), t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.security.AuthService#save(eu.agileeng.security.AuthRole, eu.agileeng.services.AEInvocationContext)
	 */
	@Override
	public AuthRole save(AuthRole ar, AEInvocationContext invContext) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// TODO as validation
						
			// TODO authorize
			
			// get connection and begin transaction
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			// save in transaction
			switch(ar.getPersistentState()) {
			case NEW:
				insert(ar, invContext, localConnection);
				break;
			case UPDATED:
				update(ar, invContext, localConnection);
				break;
			default:
				// internal error
				assert(false);
				break;
			}

			localConnection.commit();
			return ar;
		} catch (AEException e) {
			localConnection.rollback();
			throw e;
		} catch (Throwable t) {
			localConnection.rollback();
			throw new AEException((int) AEError.System.NA.getSystemID(), t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	/**
 	 * private where no need for different  
	 */
	private void prepareInsert(AEDomainObject aeObject, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		/**
		 * validate insert
		 */
		
		/**
		 * prepare
		 */
		Date dateNow = new Date();
		aeObject.setCreator(invContext.getAuthPrincipal().getName());
		aeObject.setTimeCreated(dateNow);
		aeObject.setModifier(invContext.getAuthPrincipal().getName());
		aeObject.setTimeModified(dateNow);
	}
	
	/**
 	 * private where no need for different  
	 */
	private void insert(AuthPrincipal as, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConn = null;
		try {
			// get connection wrapper to execute this method
			localConn = daoFactory.getConnection(aeConnection);
			localConn.beginTransaction();
			
			// check password
			if(!AuthPrincipal.isValidWindowsPassword(as.getPassword())) {
				AEError.System.INVALID_PARAMETER.toException();
			}
			
			// Hash the password
			as.setPassword(PasswordHash.createHash(as.getPassword()));
			
			// prepare
			prepareInsert(as, invContext, aeConnection);
			
			// insert
			AuthPrincipalDAO asDAO = daoFactory.getAuthPrincipalDAO(localConn);
			asDAO.insert(as);
			
			// commit
			localConn.commit();
		} catch (Throwable t) {
			localConn.rollback();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConn);
		}
	}
	
	/**
 	 * private where no need for different  
	 */
	private void prepareUpdate(AEDomainObject aeObject, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		/**
		 * validate update
		 */
		
		/**
		 * prepare
		 */
		Date dateNow = new Date();
		aeObject.setModifier(invContext.getAuthPrincipal().getName());
		aeObject.setTimeModified(dateNow);
	}
	
	/**
 	 * private where no need for different  
	 */
	private void update(AuthPrincipal as, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConn = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// TODO authorize
			
			// get connection wrapper to execute this method
			localConn = daoFactory.getConnection(aeConnection);
			localConn.beginTransaction();
			
			// DAO
			AuthPrincipalDAO asDAO = daoFactory.getAuthPrincipalDAO(localConn);
			
			// validate built-in principals update
			validateBuiltInPrincipal(as.getDescriptor(), localConn, invContext);
			
			// prepare
			prepareUpdate(as, invContext, aeConnection);

			// update
			asDAO.update(as);
			
			// commit
			localConn.commit();
			
			// workaround about locked: refresh locked field
 			AuthPrincipal ap = asDAO.load(as.getDescriptor());
			as.setLocked(ap.isLocked());
		} catch (Throwable t) {
			localConn.rollback();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConn);
		}
	}

	/**
 	 * private where no need for different  
	 */
	private void insert(AuthRole ar, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConn = null;
		try {
			// get connection wrapper to execute this method
			localConn = daoFactory.getConnection(aeConnection);
			localConn.beginTransaction();
			
			// prepare
			prepareInsert(ar, invContext, aeConnection);
			
			// insert
			AuthRoleDAO arDAO = daoFactory.getAuthRoleDAO(localConn);
			arDAO.insert(ar);
			
			// commit
			localConn.commit();
		} catch (Throwable t) {
			localConn.rollback();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConn);
		}
	}
	
	/**
 	 * private where no need for different  
	 */
	private void update(AuthRole ar, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConn = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// TODO authorize
			
			// get connection wrapper to execute this method
			localConn = daoFactory.getConnection(aeConnection);
			localConn.beginTransaction();
			
			// prepare
			prepareUpdate(ar, invContext, aeConnection);
			
			// update
			AuthRoleDAO arDAO = daoFactory.getAuthRoleDAO(localConn);
			arDAO.update(ar);
			
			// commit
			localConn.commit();
		} catch (Throwable t) {
			localConn.rollback();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConn);
		}
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.security.AuthService#loadAuthRoleAll(eu.agileeng.services.AEInvocationContext)
	 */
	@Override
	public AEResponse loadAuthRolesAll(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// validate invocation context
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// TODO authorize
			
			// get roles
			localConnection = daoFactory.getConnection();
			AuthRoleDAO arDAO = daoFactory.getAuthRoleDAO(localConnection);
			AuthRolesSet rolesList = arDAO.loadAll();
			
			// create and return response
			JSONObject payload = new JSONObject();
			payload.put("roles", rolesList.toJSONArray());
			return new AEResponse(payload);
		} catch (Throwable t) {
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.security.AuthService#loadAuthSubjectAll(eu.agileeng.services.AEInvocationContext)
	 */
	@Override
	public AuthPrincipalsList loadAuthPrincipalsAll(AEInvocationContext invContext) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// TODO authorize
			
			// get connection
			localConnection = daoFactory.getConnection();
			AuthPrincipalDAO asDAO = daoFactory.getAuthPrincipalDAO(localConnection);
			
			AuthPrincipalsList subjList = asDAO.loadAll();
			for (AuthPrincipal authSubject : subjList) {
				loadInDepth(authSubject, localConnection);
			}
			
			return subjList;
		} catch (Throwable t) {
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.security.AuthService#save(eu.agileeng.security.AuthSubjectRoleAssoc, eu.agileeng.services.AEInvocationContext)
	 */
	@Override
	public AuthSubjectRoleAssoc save(AuthSubjectRoleAssoc assoc, AEInvocationContext invContext) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// TODO as validation
						
			// TODO authorize
			
			// get connection and begin transaction
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			// save in transaction
			switch(assoc.getPersistentState()) {
				case NEW:
					insert(assoc, invContext, localConnection);
					break;
				case UPDATED:
					update(assoc, invContext, localConnection);
					break;
				default:
					// internal error
					assert(false);
					break;
			}

			localConnection.commit();
			return assoc;
		} catch (AEException e) {
			localConnection.rollback();
			e.printStackTrace();
			throw e;
		} catch (Throwable t) {
			localConnection.rollback();
			throw new AEException((int) AEError.System.NA.getSystemID(), t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	/**
 	 * private where no need for different  
	 */
	private void insert(AuthSubjectRoleAssoc assoc, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConn = null;
		try {
			// get connection wrapper to execute this method
			localConn = daoFactory.getConnection(aeConnection);
			localConn.beginTransaction();
			
			// prepare
			prepareInsert(assoc, invContext, aeConnection);
			
			// insert
			AuthSubjectRoleAssocDAO assocDAO = daoFactory.getAuthSubjectRoleAssocDAO(localConn);
			assocDAO.insert(assoc);
			
			// commit
			localConn.commit();
		} catch (Throwable t) {
			localConn.rollback();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConn);
		}
	}
	
	/**
 	 * private where no need for different  
	 */
	private void update(AuthSubjectRoleAssoc assoc, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConn = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// TODO authorize
			
			// get connection wrapper to execute this method
			localConn = daoFactory.getConnection(aeConnection);
			localConn.beginTransaction();
			
			// prepare
			prepareUpdate(assoc, invContext, aeConnection);
			
			// update
			AuthSubjectRoleAssocDAO assocDAO = daoFactory.getAuthSubjectRoleAssocDAO(localConn);
			assocDAO.update(assoc);
			
			// commit
			localConn.commit();
		} catch (Throwable t) {
			localConn.rollback();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConn);
		}
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.security.AuthService#load(eu.agileeng.domain.AEDescriptor, eu.agileeng.services.AEInvocationContext)
	 */
	@Override
	public AuthSubjectRoleAssoc load(AEDescriptor assocDescr, AEInvocationContext invContext) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// TODO authorize
			
			// get connection
			localConnection = daoFactory.getConnection();
			AuthSubjectRoleAssocDAO assocDAO = daoFactory.getAuthSubjectRoleAssocDAO(localConnection);
			AuthSubjectRoleAssoc assoc = assocDAO.load(assocDescr);

			return assoc;
		} catch (Throwable t) {
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.security.AuthService#getAssociatedRoles(eu.agileeng.domain.AEDescriptor, eu.agileeng.services.AEInvocationContext)
	 */
	@Override
	public AuthRolesSet getAssociatedRoles(AEDescriptor subjDescr, AEInvocationContext invContext) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// TODO authorize
			
			// get connection
			localConnection = daoFactory.getConnection();
			AuthRoleDAO arDAO = daoFactory.getAuthRoleDAO(localConnection);
			return arDAO.loadAssociatedRoles(subjDescr);
		} catch (Throwable t) {
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.security.AuthService#getAssociatedSubjects(eu.agileeng.domain.AEDescriptor, eu.agileeng.services.AEInvocationContext)
	 */
	@Override
	public AuthPrincipalsList getAssociatedSubjects(AEDescriptor roleDescr, AEInvocationContext invContext) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// TODO authorize
			
			// get connection
			localConnection = daoFactory.getConnection();
			AuthPrincipalDAO asDAO = daoFactory.getAuthPrincipalDAO(localConnection);
			return asDAO.loadAssociatedSubjects(roleDescr);
		} catch (Throwable t) {
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.services.party.PartyService#save(eu.agileeng.domain.contact.SubjectCompAssoc, eu.agileeng.services.AEInvocationContext)
	 */
	@Override
	public SubjectCompAssoc save(SubjectCompAssoc assoc, AEInvocationContext invContext) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();
			
			// real work here
			SubjectCompAssocDAO assocDAO = daoFactory.getSubjectCompAssocDAO(localConnection);
			AEPersistent.State state = assoc.getPersistentState();
			switch(state) {
				case NEW:
					prepareInsert(assoc, invContext, localConnection);
					assocDAO.insert(assoc);
					break;
				case UPDATED:
					prepareUpdate(assoc, invContext, localConnection);
					assocDAO.update(assoc);
					break;
				case DELETED:
					break;
				case VIEW:
					// manage in depth
					break;
				default:
					// must be never hapen
					assert(false);
			}
			
			localConnection.commit();
			return assoc;	
		} catch (Exception e) {
			e.printStackTrace();
			if(localConnection != null) {
				localConnection.rollback();
			}
			throw new AEException(e.getMessage(), e);
		} finally { 
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadAuthPrincipalsManageableBy(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// get connection
			localConnection = daoFactory.getConnection();
			
			// validate caler
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// TODO authorize
			
			// principals
			AuthPrincipalDAO apDAO = daoFactory.getAuthPrincipalDAO(localConnection);
			JSONObject jsonPrincipal = aeRequest.getArguments().optJSONObject("principal");
			AuthPrincipal principal = new AuthPrincipal();
			principal.create(jsonPrincipal);
			long maxRole = principal.getMaxRole();
			AEDescriptor compDescr = null;
			if(principal.getCompany() != null) {
				compDescr = principal.getCompany().getDescriptor();
			}
			
			AuthPrincipalsList principalsList = new AuthPrincipalsList();
			if(maxRole == AuthRole.System.power_user.getSystemID() || maxRole == AuthRole.System.administrator.getSystemID()) {

				// load all accountants (principals with role accountant) 
				principalsList.addAllWithoutDupl(apDAO.loadPrincipalsByRoleSysId(
						AuthRole.System.accountant.getSystemID(),
						principal.getDescription()));

				// so load all social (principals with role social) 
				principalsList.addAllWithoutDupl(apDAO.loadPrincipalsByRoleSysId(
						AuthRole.System.social.getSystemID(),
						principal.getDescription()));

				// and load all principals to the selected company
				principalsList.addAllWithoutDupl(apDAO.loadCompanyPrincipals(compDescr));
			} else {
				if(principal.isMemberOf(AuthRole.System.accountant)) {
					// accountant, so load all accountants (principals with role accountant) 
					principalsList.addAllWithoutDupl(apDAO.loadPrincipalsByRoleSysId(
							AuthRole.System.accountant.getSystemID(),
							principal.getDescription()));
				}  
				
				if(principal.isMemberOf(AuthRole.System.social)) {
					// social, so load all social (principals with role social) 
					principalsList.addAllWithoutDupl(apDAO.loadPrincipalsByRoleSysId(
							AuthRole.System.social.getSystemID(),
							principal.getDescription()));
				}
				
				principalsList.addAllWithoutDupl(apDAO.loadCompanyPrincipals(compDescr));
			}
			for (AuthPrincipal authPrincipal : principalsList) {
				loadInDepth(authPrincipal, localConnection);
			}
			
			// roles
			AuthRoleDAO arDAO = daoFactory.getAuthRoleDAO(localConnection);
			AuthRolesSet rolesList = new AuthRolesSet();
			AuthRolesSet allRolesList = arDAO.loadAll();
			
			if(maxRole == AuthRole.System.power_user.getSystemID()
					|| maxRole == AuthRole.System.administrator.getSystemID()) {

				for (AuthRole authRole : allRolesList) {
					if(authRole.getSysId() > maxRole) {
						rolesList.add(authRole);
					}
				}
			} else {
				if(principal.isMemberOf(AuthRole.System.accountant)) {
					// accountant, so load all accountant related roles 
					rolesList.addAllWithoutDupl(allRolesList.getAccountantRoles());
				}  
				
				if(principal.isMemberOf(AuthRole.System.social)) {
					// social, so load all social related roles
					rolesList.addAllWithoutDupl(allRolesList.getSocialRoles());
				}
			}
			
			// customers
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);
			JSONArray customersArray = orgDAO.loadCustomersByPrincipal(principal.getDescriptor());
			
			// create and return response
			JSONObject payload = new JSONObject();
			payload.put("principals", AuthPrincipal.toJSONArray(principalsList));
			payload.put("roles", rolesList.toJSONArray());
			payload.put("customers", customersArray);
			return new AEResponse(payload);
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadAppConfig(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate context
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// get connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			long ownerId = aeRequest.getArguments().optLong("ownerId");
			AppDAO appDAO = daoFactory.getAppDAO(localConnection);
			
			AEAppModulesList allModulesList = appDAO.loadAll();
			if(!"secal.png".equalsIgnoreCase(invContext.getAuthPrincipal().getDescription())) {
				for (Iterator<AEAppModule> iterator = allModulesList.iterator(); iterator.hasNext();) {
					AEAppModule appModule = (AEAppModule) iterator.next();
					if(appModule.getID() == AEApp.PERIMES_MODULE_ID
							|| appModule.getID() == AEApp.STOCKS_MODULE_ID
							|| appModule.getID() == AEApp.SUIVI_GPL_MODULE_ID
							|| appModule.getID() == AEApp.DONNEES_MODULE_ID
							|| appModule.getID() == AEApp.PAM_MODULE_ID
							|| appModule.getID() == AEApp.PURCHASE_FNP_MODULE_ID) {
						
						iterator.remove();
					}
				}
			}
			AEAppConfigList forbiddenModulesList = appDAO.loadByCompany(Organization.lazyDescriptor(ownerId));
			AEAppConfigList availableModulesList = new AEAppConfigList();
			
			// process available modules
			for (Iterator<AEAppModule> iterator = allModulesList.iterator(); iterator.hasNext();) {
				AEAppModule appModule = (AEAppModule) iterator.next();
				if(!forbiddenModulesList.containsModule(appModule)) {
					AEAppConfig appConfig = new AEAppConfig();
					appConfig.setID(AEPersistentUtil.getTmpID());
					appConfig.setModule(appModule);
					
					availableModulesList.add(appConfig);
				}
			}
			
			// create and return response
			JSONObject payload = new JSONObject();
			payload.put("availableModules", availableModulesList.toJSONArray());
			payload.put("forbiddenModules", forbiddenModulesList.toJSONArray());
			return new AEResponse(payload);
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse saveAppConfig(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate context
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// get connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			long ownerId = aeRequest.getArguments().optLong("ownerId");
			JSONArray appConfigJSONArrary = aeRequest.getArguments().optJSONArray("forbiddenModules");
			AEAppConfigList appConfigList = new AEAppConfigList();
			appConfigList.create(appConfigJSONArrary);
			for (AEAppConfig aeAppConfig : appConfigList) {
				aeAppConfig.setCompany(Organization.lazyDescriptor(ownerId));
			}
			
			// replace forbidden configuration in transaction
			localConnection.beginTransaction();
			daoFactory.getAppDAO(localConnection).replace(appConfigList, ownerId);
			localConnection.commit();
			
			// create and return response
			JSONObject payload = new JSONObject();
			payload.put("availableModules", new JSONArray());
			payload.put("forbiddenModules", new JSONArray());
			return new AEResponse(payload);
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	/**
	 * Dont allow empty passwords
	 * 
	 * @param user
	 * @param password
	 * @return
	 */
	private boolean verifyPassword(AuthPrincipal user, String password) {
		try {
			if(AEStringUtil.isEmpty(user.getPassword())) {
				if(!AEStringUtil.isEmpty(password)) {
					return false;
				}
			} else if(!PasswordHash.validatePassword(password, user.getPassword())) {// !user.getPassword().equals(password)) {
				return false;
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public AEResponse unlock(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// authorize
			checkPermission(new AuthPermission(AEAppModule.SECURITY, AuthPermission.UPDATE), invContext);
			
			/**
			 * get attributes
			 */
			long userId = aeRequest.getArguments().getLong("userId");
			
			/**
			 * DAOFactory and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(null);
			
			AuthPrincipalDAO apDAO = daoFactory.getAuthPrincipalDAO(localConnection);
			
			// validate built-in principals update
			validateBuiltInPrincipal(AuthPrincipal.lazyDescriptor(userId), localConnection, invContext);
			
			/**
			 * begin transaction
			 */
			localConnection.beginTransaction();
			
			/**
			 * do the work
			 */
			apDAO.unlock(userId);
			apDAO.activate(userId);
			
			/**
			 * Commit
			 */
			localConnection.commit();
			
			// create and return response
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse logoff(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		try {
			// validate invocation context
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// AuthPrincipal
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			// final setup
			ap.setAuthenticated(false);
			
			// notify observers
			LogEvent afterLogoff = new LogEvent();
			afterLogoff.setPrincipalName(ap.getName());
			logoffEvent.fire(afterLogoff);
			
			// create and return response
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Exception e) {
			throw new AEException("Logoff failed", e);
		}
	}

	@Override
	public AEResponse loadPrincipalsInitialData(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Authorize 
			 */
			checkPermission(new AuthPermission(AEAppModule.SECURITY, AuthPermission.READ), invContext);

			//
			DAOFactory daoFactory = DAOFactory.getInstance();
			
			// principals
			AuthPrincipalDAO aoDAO = daoFactory.getAuthPrincipalDAO(localConnection);
			AuthPrincipalsList principals = aoDAO.loadAll();

			// roles
			AuthRoleDAO arDAO = daoFactory.getAuthRoleDAO(localConnection);
			AuthRolesSet rolesList = arDAO.loadAll();
			for (Iterator<AuthRole> iterator = rolesList.iterator(); iterator.hasNext();) {
				AuthRole authRole = (AuthRole) iterator.next();
				if(authRole.getSysId() == AuthRole.System.technician.getSystemID()) {
					iterator.remove();
				}
			}
			
			// customers
			aeRequest.getArguments().put("lazzyLoad", true);
			AEResponse customers = partyService.loadCustomers(aeRequest, invContext);
			
			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject()
				.put("principals", principals.toJSONArray())
				.put("roles", rolesList.toJSONArray());
			JSONUtil.apply(payload, customers.getPayload());
					
			return new AEResponse(payload);
		} catch (Exception e) {
			logger.error(e);
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadPrincipal(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// common validation
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// authorization 
			checkPermission(new AuthPermission(AEAppModule.SECURITY, AuthPermission.READ), invContext);
			
			// get connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			// parse and translate JSON attributes
			JSONObject jsonPrincipal = aeRequest.getArguments().getJSONObject("principal");
			AEDescriptor principalDescr = new AEDescriptorImp();
			principalDescr.create(jsonPrincipal);
			
			// load
			AuthPrincipal principal = loadAuthSubject(principalDescr, invContext);
			
			// return response
			JSONObject payload = new JSONObject();
			if(principal != null) {
				payload.put("principal", principal.toJSONObject());
			} else {
				payload.put("principal", new JSONObject());
			}
			return new AEResponse(payload);
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse deactivate(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// authorize
			checkPermission(new AuthPermission(AEAppModule.SECURITY, AuthPermission.UPDATE), invContext);
			
			/**
			 * get attributes
			 */
			long userId = aeRequest.getArguments().getLong("userId");
			
			/**
			 * self deactivation is not possibble
			 */
			if(invContext.getAuthPrincipal().getID() == userId) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}
			
			/**
			 * DAOFactory and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			
			AuthPrincipalDAO apDAO = daoFactory.getAuthPrincipalDAO(localConnection);
			
			// validate built-in principals update
			validateBuiltInPrincipal(AuthPrincipal.lazyDescriptor(userId), localConnection, invContext);
			
			/**
			 * begin transaction
			 */
			localConnection.beginTransaction();
			
			/**
			 * do the work
			 */
			apDAO.deactivate(userId);
			
			/**
			 * Commit
			 */
			localConnection.commit();
			
			// create and return response
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse resetPassword(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			/**
			 * get attributes
			 */
			long userId = aeRequest.getArguments().getLong("userId");
			String password = aeRequest.getArguments().getString("psswrd");
			
			// authorize
			// check permission only if the request is not self reset password
			AuthPrincipal ap = invContext.getAuthPrincipal();
			if(ap.getID() != userId) {
				checkPermission(new AuthPermission(AEAppModule.SECURITY, AuthPermission.UPDATE), invContext);
			}
			
			/**
			 * DAOFactory and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			
			AuthPrincipalDAO apDAO = daoFactory.getAuthPrincipalDAO(localConnection);
			
			// validate built-in principals update
			validateBuiltInPrincipal(AuthPrincipal.lazyDescriptor(userId), localConnection, invContext);
			
			/**
			 * begin transaction
			 */
			localConnection.beginTransaction();
			
			/**
			 * do the work
			 */
			// check the password
			if(!AuthPrincipal.isValidWindowsPassword(password)) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}
			
			// Hash the password
			String hPassword = PasswordHash.createHash(password);
			
			// update
			apDAO.resetPassword(userId, hPassword);
			apDAO.setExpirationTime(userId, null);
			
			/**
			 * Commit
			 */
			localConnection.commit();
			
			// create and return response
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public void createDefaultAuthPrincipal(JSONObject customer, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// authorize
			checkPermission(new AuthPermission(AEAppModule.SECURITY, AuthPermission.SAVE), invContext);
			
			/**
			 * DAOFactory and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			AuthPrincipalDAO apDAO = daoFactory.getAuthPrincipalDAO(localConnection);
			AuthRoleDAO arDAO = daoFactory.getAuthRoleDAO(localConnection);
			
			/**
			 * Create principal
			 */
			AuthPrincipal ap = new AuthPrincipal();

			// role
			AuthRolesSet rolesList = arDAO.loadAll();
			AuthRole operativeRole = rolesList.getBySystemId(AuthRole.System.operative);
			if(operativeRole == null) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}
			ap.addRole(operativeRole.getDescriptor());
			
			// company
			ap.addCompany(Organization.lazyDescriptor(customer.getLong(AEDomainObject.JSONKey.id.name())));
			
			ap.setName(customer.getString(AEDomainObject.JSONKey.code.name()));
			ap.setPassword(customer.getString(AEDomainObject.JSONKey.code.name()) + "-" + "Az");
			ap.setFirstName(null);
			ap.setMiddleName(null);
			ap.setLastName(customer.getString(AEDomainObject.JSONKey.code.name()));
			ap.setCode(null);
			ap.setDescription(null);
			ap.setEMail(null);
			ap.setPhone(null);
			ap.setLocked(false);
			ap.setActive(true);
			
			/**
			 * begin transaction
			 */
			localConnection.beginTransaction();
			
			/**
			 * do the work
			 */
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			JSONObject apJson = ap.toJSONObject();
			apJson.put(AuthPrincipal.JSONKey.password.name(), ap.getPassword());
			AERequest aeRequest = new AERequest(new JSONObject().put("principal", apJson));
			AEResponse aeResponse = savePrincipal(aeRequest, invContext);
			
			// Important!!! set expiration date
			apDAO.setExpirationTime(aeResponse.getPayload().getLong(AEDomainObject.JSONKey.id.name()), new Date(0L));
			
			/**
			 * Commit
			 */
			localConnection.commit();
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	private void validateBuiltInPrincipal(AEDescriptive ap, AEConnection aeConnection, AEInvocationContext invContext) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AuthPrincipalDAO apDAO = daoFactory.getAuthPrincipalDAO(aeConnection);
		
		// validate built-in principals update
		AuthPrincipal asDb = apDAO.load(ap.getDescriptor());
		if(asDb != null && asDb.isSystem()) {
			boolean bEditable = false;
			if(invContext != null && invContext.getAuthPrincipal() != null && invContext.getAuthPrincipal().getID() == asDb.getID()) {
				bEditable = true;
			}
			if(!bEditable) {
				throw new AEException("Ce profil d’utilisateur ne peut être modifié!");
			}
		}
	}
}
