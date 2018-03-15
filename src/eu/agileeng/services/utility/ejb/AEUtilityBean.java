package eu.agileeng.services.utility.ejb;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.accbureau.AEApp;
import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDescriptorsList;
import eu.agileeng.domain.AEDescriptorsSet;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.AEWarning;
import eu.agileeng.domain.bookmark.AEBookmark;
import eu.agileeng.domain.bookmark.AEBookmarksList;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistent.State;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.oracle.OrganizationDAO;
import eu.agileeng.persistent.dao.utility.AEBookmarkDAO;
import eu.agileeng.persistent.dao.utility.MBAopcaDAO;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.security.AuthPrincipalsList;
import eu.agileeng.security.AuthRole;
import eu.agileeng.security.AuthRolesSet;
import eu.agileeng.security.ejb.AuthLocal;
import eu.agileeng.security.ejb.dao.AuthPrincipalDAO;
import eu.agileeng.security.ejb.dao.AuthRoleDAO;
import eu.agileeng.services.AEBean;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AEInvocationContextValidator;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.party.ejb.PartyLocal;

@Stateless
public class AEUtilityBean extends AEBean implements AEUtilityLocal, AEUtilityRemote {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1487120370343563638L;

	@EJB private PartyLocal partyService;
	@EJB private AuthLocal authService;
	
	@Override
	public AEResponse loadBookmarks(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Get arguments
			 */
			JSONObject arguments = aeRequest.getArguments();
			long sOwnerId = arguments.getLong("sOwnerId");
			long ownerId = arguments.getLong("ownerId");
			AEDescriptive ownerDescr = Organization.lazyDescriptor(ownerId);
			
			/**
			 * Validation
			 */
			// whether this user is ownered by this customer
			ap.ownershipValidator(sOwnerId);
			
			// whether this article is ownered by this customer
			if(sOwnerId != ownerId) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
			
			/**
			 * Factories, conection and DAOs
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			
			// get connection
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			// DAOs
			AEBookmarkDAO bookmarkDAO = daoFactory.getAEBookmarkDAO(localConnection);
			
			/**
			 * Load data
			 */
			AEBookmarksList bookmarksList = bookmarkDAO.load(ownerDescr.getDescriptor());

			/**
			 * return response
			 */
			JSONObject payload = new JSONObject();
			payload.put(AEBookmark.JSONKey.bookmarks, bookmarksList.toJSONArray());
			return new AEResponse(payload);
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse saveBookmarks(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Get arguments
			 */
			JSONObject arguments = aeRequest.getArguments();
			long sOwnerId = arguments.getLong("sOwnerId");
			
			// owner
			long ownerId = arguments.getLong("ownerId");
			
			// bookmarks
			JSONArray bookmarksJSONArray = arguments.getJSONArray(AEBookmark.JSONKey.bookmarks);
			AEBookmarksList bookmarksList = new AEBookmarksList();
			bookmarksList.create(bookmarksJSONArray);
			
			/**
			 * Validation
			 */
			// whether this user is ownered by this customer
			ap.ownershipValidator(sOwnerId);
			
			// whether this article is ownered by this customer
			if(sOwnerId != ownerId) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
			
			// the owner
			for (Iterator<AEBookmark> iterator = bookmarksList.iterator(); iterator.hasNext();) {
				AEBookmark aeBookmark = (AEBookmark) iterator.next();
				if(aeBookmark.getCompany() == null) {
					throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
				}
				if(aeBookmark.getCompany().getDescriptor().getID() != ownerId) {
					throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
				}
			}
			
			// CUID is allowed for administrator or poweruser or accounting's role 
			if(!(ap.hasAdministratorRights() 
					|| ap.hasPowerUserRights() 
					|| ap.isMemberOf(AuthRole.System.accountant)
					|| ap.isMemberOf(AuthRole.System.social))) {
				
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException(); 
			}
			
			/**
			 * Factories, conection and DAOs
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			
			// get connection
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			// DAOs
			AEBookmarkDAO bookmarkDAO = daoFactory.getAEBookmarkDAO(localConnection);
			
			/**
			 * Save data
			 */
			
			// begin transaction
			localConnection.beginTransaction();
			
			// process the list
			for (Iterator<AEBookmark> iterator = bookmarksList.iterator(); iterator.hasNext();) {
				AEBookmark aeBookmark = (AEBookmark) iterator.next();
				switch(aeBookmark.getPersistentState()) {
				case NEW:
					bookmarkDAO.insert(aeBookmark);
					break;
				case UPDATED:
					bookmarkDAO.update(aeBookmark);
					break;
				case DELETED:
					bookmarkDAO.delete(aeBookmark.getDescriptor());
					iterator.remove();
				default:
					// may be internal error
					assert(false);
					break;
				}
			}

			// commit
			localConnection.commit();
			
			/**
			 * return response
			 */
			JSONObject payload = new JSONObject();
			payload.put(AEBookmark.JSONKey.bookmarks, bookmarksList.toJSONArray());
			return new AEResponse(payload);
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse importAopca(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection connMonEntreprise = null;
		AEConnection connAopca = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			AuthPrincipal ap = invContext.getAuthPrincipal();
			if(!ap.hasAdministratorRights() && !ap.hasPowerUserRights()) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
			if(!"aopca.png".equalsIgnoreCase(ap.getDescription())) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
			
			/**
			 * Extract request arguments  
			 */
//			JSONObject arguments = aeRequest.getArguments();
			
			/**
			 * Get connections 
			 */
			try {
				InitialContext ic = new InitialContext();
				DataSource dsAopca = (DataSource) ic.lookup("java:Aopca");
				connAopca = new AEConnection(dsAopca.getConnection(), true);
				ic.close();
			} catch (NamingException e) {
				throw new AEException(e);
			}
			
			DAOFactory daoFactory = DAOFactory.getInstance();
			connMonEntreprise = daoFactory.getConnection();
			
			/**
			 * Begin MonEntreprise transaction
			 */
			connMonEntreprise.beginTransaction();
	
			/**
			 * Import customers
			 */
			importAopcaCustomers(connMonEntreprise, connAopca, invContext);
			
			/**
			 * Import users
			 */
			importAopcaUsers(connMonEntreprise, connAopca, invContext);
			
			/**
			 * Commit
			 */
			connMonEntreprise.commit();
			
			// log the result
			AEApp.logger().info("importAopca successed");
			
			/**
			 * return empty response
			 */
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Throwable t) {
			AEConnection.rollback(connMonEntreprise);
			throw new AEException(t);
		} finally {
			AEConnection.close(connAopca);
			AEConnection.close(connMonEntreprise);
		}
	}
	
	private void importAopcaCustomers(
			AEConnection connMonEntreprise, 
			AEConnection connAopca, 
			AEInvocationContext invContext) throws AEException, AEWarning, JSONException {
		
		OrganizationDAO orgDAO = DAOFactory.getInstance().getOrganizationDAO(connMonEntreprise);
		
		// AopcaDAO
		MBAopcaDAO aopcaDAO = MBAopcaDAO.getInstance(connAopca);
		JSONArray aopcaCustomersJsonArray = aopcaDAO.loadCompanies();
		for (int i = 0; i < aopcaCustomersJsonArray.length(); i++) {
			JSONObject aopcaCustomer = aopcaCustomersJsonArray.getJSONObject(i);
			
			// whether already imported or not
			AEDescriptor alreadyImported = orgDAO.loadImported(
					aopcaCustomer.getLong("externalId"), 
					aopcaCustomer.getString("externalSystem")); 
			if(alreadyImported != null) {
				continue;
			}
			
			// set new
			aopcaCustomer.put("id", AEPersistentUtil.getTmpID());
			aopcaCustomer.put("dbState", AEPersistentUtil.DB_ACTION_INSERT);
			
			// init
			aopcaCustomer.put("properties", 0L);
			aopcaCustomer.put("customer", 1);
			aopcaCustomer.put("template", false);
			aopcaCustomer.put("active", true);
			aopcaCustomer.put("note", "Auto imported from MonBureau Aopca");
			
			JSONObject coaJson = new JSONObject();
			coaJson.put("dbState", AEPersistentUtil.DB_ACTION_INSERT);
			coaJson.put("name", "coa");
			coaJson.put("accounts", new JSONArray());
			coaJson.put("lengthG", 6);
			coaJson.put("lengthA", 6);
			coaJson.put("system", false);
			coaJson.put("active", true);
			coaJson.put("modifiable", true);
			aopcaCustomer.put("chartOfAccounts", coaJson);
			
			// insert into MonEntreprise
			JSONObject arguments = new JSONObject();
			arguments.put("customer", aopcaCustomer);
			AERequest savePartyRequest = new AERequest(arguments);
			savePartyRequest.setAuthPrincipal(invContext.getAuthPrincipal());
			try {
				partyService.saveCustomer(savePartyRequest, invContext);				
			} catch(Throwable t) {
				throw new AEException(
						"import " + aopcaCustomer.getString("externalId") + " failed: " + t.getMessage());
			}
		}
				
		AEApp.logger().info("importAopcaCustomers successed");
	}
	
	private void importAopcaUsers(
			AEConnection connMonEntreprise, 
			AEConnection connAopca, 
			AEInvocationContext invContext) throws AEException, AEWarning, JSONException {
		
		
		AuthPrincipalDAO authPrDAO = DAOFactory.getInstance().getAuthPrincipalDAO(connMonEntreprise);
		OrganizationDAO orgDAO = DAOFactory.getInstance().getOrganizationDAO(connMonEntreprise);
		AuthRoleDAO authRoleDAO = DAOFactory.getInstance().getAuthRoleDAO(connMonEntreprise);
		
		AuthRolesSet authRolesList = authRoleDAO.loadAll();
		
		// AopcaDAO
		MBAopcaDAO aopcaDAO = MBAopcaDAO.getInstance(connAopca);
		AuthPrincipalsList usersList = aopcaDAO.loadUsers();
		for (AuthPrincipal aopcaUser : usersList) {
			
			// whether already imported or not
			AEDescriptor alreadyImported = authPrDAO.loadImported(
					aopcaUser.getExternalPrincipal().getID(), 
					aopcaUser.getExternalPrincipal().getName()); 
			if(alreadyImported != null) {
				continue;
			}
			
			// validate uniqueness
			try {
				authPrDAO.validateName(aopcaUser.getName());
			} catch (Throwable t) {
				AEApp.logger().error("validating " + aopcaUser.getName() + " failed: " + t.getMessage(), t);
				continue;
			}
			
			// set new
			aopcaUser.setID(AEPersistentUtil.getTmpID());
			aopcaUser.setPersistentState(State.NEW);
			
			// init
			aopcaUser.setNote("Auto imported from MonBureau Aopca");
			aopcaUser.setDescription("aopca.png"); // Urgent and Required
			aopcaUser.setSystem(false);
			
			// load companies
			AEDescriptorsSet companies = new AEDescriptorsSet();
			List<Long> companiesIDs = aopcaDAO.loadCompaniesTo(aopcaUser.getExternalPrincipal().getID());
			for (Long id : companiesIDs) {
				AEDescriptor meOrg = orgDAO.loadImported(id, "MonBureauAopca");
				if(meOrg != null) {
					companies.add(meOrg);
				}
			}
			aopcaUser.setCompaniesSet(companies);
			
			// load roles
			AEDescriptorsList roles = new AEDescriptorsList();
			List<Long> rolesIDs = aopcaDAO.loadRolesTo(aopcaUser.getExternalPrincipal().getID());
			for (Long id : rolesIDs) {
				AuthRole role = null;
				switch (id.intValue()) {
					case 1: {
						role = authRolesList.getBySystemId(AuthRole.System.power_user);
					}
					case 2: {
						role = authRolesList.getBySystemId(AuthRole.System.social);
					}
					case 4: {
						role = authRolesList.getBySystemId(AuthRole.System.operative_social);
					}
				}
				if(role != null) {
					roles.add(role.getDescriptor());
				}
			}
			aopcaUser.setRolesList(roles);
			
			// insert into MonEntreprise
			JSONObject arguments = new JSONObject();
			arguments.put("principal", aopcaUser.toJSONObject());
			AERequest saveUserRequest = new AERequest(arguments);
			saveUserRequest.setAuthPrincipal(invContext.getAuthPrincipal());
			try {
				AEResponse savePrincipalResponse = authService.savePrincipal(saveUserRequest, invContext);
				JSONObject savedPrincipalJson = savePrincipalResponse.getPayload();
				aopcaUser.setID(savedPrincipalJson.getLong("id"));
				authPrDAO.updateExternalReference(
						aopcaUser.getID(), 
						aopcaUser.getExternalPrincipal().getID(), 
						aopcaUser.getExternalPrincipal().getName());
			} catch(Throwable t) {
				AEApp.logger().error("saving " + aopcaUser.getName() + " failed: " + t.getMessage(), t);
				continue;
			}
		}
				
		AEApp.logger().info("importAopcaUsers successed");
	}
	
	@Override
	public void checkDBStatus() throws AEException {
		AEConnection localConnection = null;
		Statement s = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();
			
			// get connection
			localConnection = daoFactory.getConnection();
			s = localConnection.createStatement();
			//check if table exists
			if (!s.execute("IF object_id('AuthPrincipal') is not null select 1 else select 0")) {
				throw new AEException("Table 'AuthPrincipal' doesn't exist");
			}
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(s);
			AEConnection.close(localConnection);
		}
	}
}
