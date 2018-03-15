package eu.agileeng.services.council.ejb;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONObject;
import org.jboss.logging.Logger;

import eu.agileeng.accbureau.AEApp;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.acc.AccPeriod;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.contact.Person;
import eu.agileeng.domain.council.Council;
import eu.agileeng.domain.council.CouncilMember;
import eu.agileeng.domain.council.CouncilMembersList;
import eu.agileeng.domain.council.CouncilValidator;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistent.State;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.council.CouncilDAO;
import eu.agileeng.security.AuthPermission;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.services.AEBean;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AEInvocationContextValidator;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.party.ejb.PartyLocal;
import eu.agileeng.util.AEDateUtil;

@Stateless
public class CouncilBean extends AEBean implements CouncilLocal, CouncilRemote {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7422003822024926658L;
	
	private static final Logger logger = Logger.getLogger(CouncilBean.class);
	
	@EJB private PartyLocal partyLocal;
	
	@Override
	public Council loadInitialData(JSONObject arguments, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			long ownerId = arguments.getLong("ownerId");
			
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			// authorize 
			authorize(new AuthPermission("System/Configuration/Customer/Councill", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));
			
			// Factories
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			CouncilDAO councilDAO = daoFactory.getCouncilDAO(localConnection);
			Council council = councilDAO.loadCurrentCouncil(ownerId);
			
			//create a council anyways
			if (council  == null) {
				council = new Council();
				council.setMembers(new CouncilMembersList());
			}
			
			return council;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
		
	}
	
	@Override
	public Council save(JSONObject arguments, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			// arguments
			long ownerId = arguments.getLong("ownerId");
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			// authorize 
			authorize(new AuthPermission("System/Configuration/Customer/Councill", AuthPermission.SAVE_AND_DELETE), invContext, Organization.lazyDescriptor(ownerId));
			
			//validate council
			CouncilValidator.getInstance().validate(arguments.getJSONObject("council"));
			
			// Factories
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			//save council object
			Council council = new Council();
			council.create(arguments.getJSONObject("council"));
			
			CouncilDAO councilDAO = daoFactory.getCouncilDAO(localConnection);
			
			localConnection.beginTransaction();
			
			switch(council.getPersistentState()) {
			case NEW: 
				//validate startDate
				if (council.getStartDate() == null) {
					if (council.getEndDate() != null) {
						Calendar cal = Calendar.getInstance();
						cal.setTime(council.getEndDate());
						
						cal.set(Calendar.YEAR, cal.get(Calendar.YEAR)-1);
						
						council.setStartDate(cal.getTime());
					} else {
						throw new AEException("Cannot save a council without end date!");
					}
				}
				
				//set owner id
				council.setOwnerId(ownerId);
				council.setCompany(Organization.lazyDescriptor(ownerId));
				
				//insert council
				councilDAO.insert(council); 
				council.getMembers().setCouncilId(council.getID());
				saveMembers(council, invContext, localConnection);
				break;
			case UPDATED:
				// validate tenant
				boolean isValid = councilDAO.validateCouncil(council.getDescriptor(), Organization.lazyDescriptor(ownerId));
				if(!isValid) {
					throw AEError.System.INVALID_REQUEST.toException();
				}
				
				if (council.getStartDate().after(council.getEndDate())) {
					councilDAO.updateCouncilEndDate(Organization.lazyDescriptor(ownerId), council.getEndDate());
					council = councilDAO.loadCurrentCouncil(ownerId);
//					throw AEError.System.INVALID_REQUEST.toException();
				} else {
					councilDAO.update(council);
					council.getMembers().setCouncilId(council.getID());
					saveMembers(council, invContext, localConnection);
				}
				break;
			case DELETED:
				break;
			default: 
				break;
			}
			
			localConnection.commit();
			
			return council;
		} catch (Throwable t) {
			if (localConnection != null) localConnection.rollback();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	private CouncilMembersList saveMembers(Council c, AEInvocationContext invContext, AEConnection localConnection) throws AEException {
		try {
			//Iterator iterator = (Iterator) c.getMembers().iterator();
			CouncilDAO councilDAO = DAOFactory.getInstance().getCouncilDAO(localConnection);
			for (Iterator<CouncilMember> iterator = c.getMembers().iterator(); iterator.hasNext();) {
				CouncilMember cm = iterator.next();
				switch(cm.getPersistentState()) {
				case NEW: 
					//set an empty person object
					cm.getEmployee().setPerson(new Person());
					
					//set a company object
					cm.getEmployee().setCompany(Organization.lazyDescriptor(c.getOwnerId())); 
					
					partyLocal.manage((Person) cm.getEmployee().getPerson(), invContext, localConnection);
					partyLocal.save(cm.getEmployee(), invContext, localConnection);
					
					councilDAO.insert(cm);
					break;
				case UPDATED:
					// validate council member
					boolean isValid = councilDAO.validateCouncilMember(cm.getDescriptor(), c.getDescriptor());
					if(!isValid) {
						throw AEError.System.INVALID_REQUEST.toException();
					}
					
					partyLocal.manage((Person) cm.getEmployee().getPerson(), invContext, localConnection);
					partyLocal.save(cm.getEmployee(), invContext, localConnection);
					
					councilDAO.update(cm);
					break;
				case DELETED: 
					councilDAO.deleteMember(cm.getID());
					partyLocal.deleteEmployee(cm.getEmployee().getID(), invContext, localConnection);
					partyLocal.deletePerson(cm.getEmployee().getPerson().getDescriptor().getID(), invContext, localConnection);
					
					//remove
					iterator.remove();
				default: 
					break;
				}
			}
			
			return c.getMembers();
		} catch (Throwable t) {
			throw new AEException(t);
		} 
	}
	
	@Override
	public Council closeCouncil(JSONObject arguments, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			long ownerId = arguments.getLong("ownerId");
			long cId = arguments.getLong("councilId");
			
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			// authorize 
			authorize(new AuthPermission("System/Configuration/Customer/Councill", AuthPermission.ALL), invContext, Organization.lazyDescriptor(ownerId));
			
			// Factories
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			//council DAO
			CouncilDAO councilDAO = daoFactory.getCouncilDAO(localConnection);
			
			Council currentCouncil = councilDAO.loadCurrentCouncil(ownerId);
			if (currentCouncil.getID() != cId) throw new AEException("Council ID mismatch!");
			
			localConnection.beginTransaction();
			
			//TODO: Before close validation
			councilDAO.closeCouncil(currentCouncil);
			
			//load next open council
			Council c = councilDAO.loadCurrentCouncil(ownerId);
			//trigger elections if there was no open council
			if (c == null) c = triggerElections(arguments, invContext, localConnection);
			
			localConnection.commit();
			
			return c;
		} catch (Throwable t) {
			if (localConnection != null) localConnection.rollback();
			throw new AEException(t);
		} finally {
			
			AEConnection.close(localConnection);
		}
	}
	
	private Council triggerElections(JSONObject arguments, AEInvocationContext invContext, AEConnection localConnection) throws AEException {
		//AEConnection localConnection = null;
		try {
			long ownerId = arguments.getLong("ownerId");
			
			CouncilDAO councilDAO = DAOFactory.getInstance().getCouncilDAO(localConnection);
			
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			// Factories
//			DAOFactory daoFactory = DAOFactory.getInstance();
//			localConnection = daoFactory.getConnection(
//					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
//			
//			//council DAO
//			CouncilDAO councilDAO = daoFactory.getCouncilDAO(localConnection);
			
			//the new council
			Council nCouncil = new Council();
			//the last council
			Council lastCouncil = councilDAO.loadLastCouncil(ownerId);
			
			nCouncil.setOwnerId(ownerId);
			
			//set start date of the new council 1 day after the end date of the last one
			Calendar cal = Calendar.getInstance();
			cal.setTime(lastCouncil.getEndDate());
			
			//increment one day after the previous end date
			cal.add(Calendar.DATE, 1);
			nCouncil.setStartDate(cal.getTime());
			
			//increment one year after the start date
			cal.add(Calendar.YEAR, 1);
			nCouncil.setEndDate(cal.getTime());
			
			//copy last council's members
			CouncilMembersList cml = new CouncilMembersList();
			cml.addAll(lastCouncil.getMembers());
			cml.setCouncilId(0);
			
			for (CouncilMember cm: cml) {
				cm.setPersistentState(State.NEW);
				cm.getEmployee().setPersistentState(State.NEW);
				cm.getEmployee().getAddress().setPersistentState(State.NEW);
				cm.getEmployee().getContact().setPersistentState(State.NEW);
			}
			
			nCouncil.setMembers(cml);
			
			//save
			//localConnection.beginTransaction();
			
			councilDAO.insert(nCouncil); 
			nCouncil.getMembers().setCouncilId(nCouncil.getID());
			saveMembers(nCouncil, invContext, localConnection);
			
			return nCouncil;
		} catch (Throwable t) {
			//localConnection.rollback();
			throw new AEException(t);
		} finally {
			//AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadEngTitres(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();

			// common attributes
			long ownerId = arguments.getLong(AEDomainObject.JSONKey.ownerId.name());
			long sOwnerId = arguments.getLong(AEDomainObject.JSONKey.sOwnerId.name());
			int year = arguments.optInt("year", AEDateUtil.getYear(new Date()));

			/**
			 * Authorize for concrete tenant
			 */
			// state validation
			if(ownerId != sOwnerId) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}

			// whether this principal is authorized for specified tenant
			ap.ownershipValidator(ownerId);
			
			/**
			 * Authorize 
			 */
			authorize(new AuthPermission("System/Configuration/Customer/EngTitres", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			CouncilDAO councilDAO = daoFactory.getCouncilDAO(localConnection);
			
			/**
			 * engagements
			 */
			JSONArray engagements = councilDAO.loadEngagements(ownerId, year);
			
			/**
			 * titres
			 */
			JSONArray titres = councilDAO.loadTitres(ownerId, year);
			
			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject()
				.put("engagements", engagements)
				.put("titres", titres)
				.put("year", year);
			return new AEResponse(payload);
		} catch (Exception e) {
			logger.error(e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse saveEngTitres(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();

			// common attributes
			long ownerId = arguments.getLong(AEDomainObject.JSONKey.ownerId.name());
			long sOwnerId = arguments.getLong(AEDomainObject.JSONKey.sOwnerId.name());
			int year = arguments.getInt("year");

			/**
			 * Authorize for concrete tenant
			 */
			// state validation
			if(ownerId != sOwnerId) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}

			// whether this principal is authorized for specified tenant
			ap.ownershipValidator(ownerId);
			
			/**
			 * Authorize 
			 */
			authorize(new AuthPermission("System/Configuration/Customer/EngTitres", AuthPermission.SAVE), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(invContext.getAEConnection());
			invContext.setAEConnection(localConnection);
			
			/**
			 * Validate acc period opened
			 */
			AccPeriod accPeriod = getAccPeriod(
					ownerId, 
					AEApp.ACCOUNTING_MODULE_ID, 
					AEDateUtil.getClearDate(AEDateUtil.getLastDate(11, year)), 
					localConnection);
			if(accPeriod != null && accPeriod.isClosed()) {
				throw AEError.System.CANNOT_INSERT_UPDATE_CLOSED_PERIOD.toException();
			}
			
			/**
			 * Start processing
			 */
			localConnection.beginTransaction();
			CouncilDAO councilDAO = daoFactory.getCouncilDAO(localConnection);
			
			/**
			 * engagements
			 */
			JSONArray engagements = arguments.optJSONArray("engagements");
			List<Integer> deletedEngagements = new ArrayList<Integer>();
			if(engagements != null) {
				for (int i = 0; i < engagements.length(); i++) {
					JSONObject e = engagements.getJSONObject(i);
					if(e.getLong(AEDomainObject.JSONKey.dbState.name()) == AEPersistentUtil.DB_ACTION_INSERT) {
						e.put(AEDomainObject.JSONKey.ownerId.name(), ownerId);
						e.put("year", year);
						councilDAO.insertEngagement(e);
					} else if(e.getLong(AEDomainObject.JSONKey.dbState.name()) == AEPersistentUtil.DB_ACTION_UPDATE) {
						councilDAO.updateEngagement(e);
					} else if(e.getLong(AEDomainObject.JSONKey.dbState.name()) == AEPersistentUtil.DB_ACTION_DELETE) {
						councilDAO.deleteEngagement(e);
						deletedEngagements.add(i);
					}
				}
			}
			for (Integer index : deletedEngagements) {
				engagements.remove(index);
			}
			
			/**
			 * titres
			 */
			JSONArray titres = arguments.optJSONArray("titres");
			List<Integer> deletedTitres = new ArrayList<Integer>();
			if(titres != null) {
				for (int i = 0; i < titres.length(); i++) {
					JSONObject t = titres.getJSONObject(i);
					if(t.getLong(AEDomainObject.JSONKey.dbState.name()) == AEPersistentUtil.DB_ACTION_INSERT) {
						t.put(AEDomainObject.JSONKey.ownerId.name(), ownerId);
						t.put("year", year);
						councilDAO.insertTitre(t);
					} else if(t.getLong(AEDomainObject.JSONKey.dbState.name()) == AEPersistentUtil.DB_ACTION_UPDATE) {
						councilDAO.updateTitre(t);
					} else if(t.getLong(AEDomainObject.JSONKey.dbState.name()) == AEPersistentUtil.DB_ACTION_DELETE) {
						councilDAO.deleteTitre(t);
						deletedTitres.add(i);
					}
				}
			}
			for (Integer index : deletedTitres) {
				titres.remove(index);
			}
			
			/**
			 * commit
			 */
			localConnection.commit();
			
			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject();
			if(engagements != null) {
				payload.put("engagements", engagements);
			}
			if(titres != null) {
				payload.put("titres", titres);
			}
			payload.put("year", year);
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
	public Council loadByDate(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			JSONObject arguments = aeRequest.getArguments();
			long ownerId = arguments.getLong("ownerId");
			Date date = AEDateUtil.parseDateStrict(arguments.getString("date"));
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			// authorize 
			authorize(new AuthPermission("System/Configuration/Customer/Councill", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));
			
			// Factories
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			CouncilDAO councilDAO = daoFactory.getCouncilDAO(localConnection);
			Council council = councilDAO.loadCouncil(ownerId, date);
			
			//create a council anyways
			if (council  == null) {
				council = new Council();
				council.setMembers(new CouncilMembersList());
			}
			
			return council;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
}
