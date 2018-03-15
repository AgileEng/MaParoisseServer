/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 09.12.2009 17:29:11
 */
package eu.agileeng.services.party.ejb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;
import org.jboss.logging.Logger;

import eu.agileeng.accbureau.AEApp;
import eu.agileeng.accbureau.AEAppConfigList;
import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDescriptorsList;
import eu.agileeng.domain.AEDescriptorsSet;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.acc.AccPeriod;
import eu.agileeng.domain.cash.CFC;
import eu.agileeng.domain.cash.CFCColumn;
import eu.agileeng.domain.cash.CFCModel;
import eu.agileeng.domain.contact.Address;
import eu.agileeng.domain.contact.AddressesList;
import eu.agileeng.domain.contact.Contact;
import eu.agileeng.domain.contact.ContactsList;
import eu.agileeng.domain.contact.Employee;
import eu.agileeng.domain.contact.EmployeeList;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.contact.OrganizationDescriptor;
import eu.agileeng.domain.contact.OrganizationTemplatesList;
import eu.agileeng.domain.contact.PartiesList;
import eu.agileeng.domain.contact.Party;
import eu.agileeng.domain.contact.Person;
import eu.agileeng.domain.contact.SubjectCompAssoc;
import eu.agileeng.domain.document.AEDocument;
import eu.agileeng.domain.document.AEDocumentDescriptor;
import eu.agileeng.domain.document.AEDocumentFilter;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.AEDocumentsList;
import eu.agileeng.domain.document.social.contractdetravail.ContractDeTravail;
import eu.agileeng.domain.document.trade.AEDocumentItem;
import eu.agileeng.domain.document.trade.AEDocumentItemsList;
import eu.agileeng.domain.document.trade.AETradeDocument;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.domain.paroisse.ParoisseValidator;
import eu.agileeng.domain.social.SocialInfo;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistent;
import eu.agileeng.persistent.AEPersistent.State;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.acc.AccountDAO;
import eu.agileeng.persistent.dao.acc.ChartOfAccountsDAO;
import eu.agileeng.persistent.dao.cash.CFCDAO;
import eu.agileeng.persistent.dao.document.AEDocumentDAO;
import eu.agileeng.persistent.dao.document.trade.AEDocumentItemDAO;
import eu.agileeng.persistent.dao.document.trade.AETradeDocumentDAO;
import eu.agileeng.persistent.dao.mandat.MandatDAO;
import eu.agileeng.persistent.dao.oracle.AddressDAO;
import eu.agileeng.persistent.dao.oracle.ContactDAO;
import eu.agileeng.persistent.dao.oracle.EmployeeDAO;
import eu.agileeng.persistent.dao.oracle.OrganizationDAO;
import eu.agileeng.persistent.dao.oracle.OrganizationTemplateDAO;
import eu.agileeng.persistent.dao.oracle.PartyDAO;
import eu.agileeng.persistent.dao.oracle.PersonDAO;
import eu.agileeng.persistent.dao.oracle.SubjectCompAssocDAO;
import eu.agileeng.persistent.dao.pam.PAMDAO;
import eu.agileeng.persistent.dao.social.SocialDAO;
import eu.agileeng.security.AuthPermission;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.security.ejb.AuthLocal;
import eu.agileeng.services.AEBean;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AEInvocationContextValidator;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.acc.ejb.AccLocal;
import eu.agileeng.services.address.ejb.AddressLocal;
import eu.agileeng.services.contact.ejb.ContactLocal;
import eu.agileeng.services.imp.AEInvocationContextImp;
import eu.agileeng.services.jcr.ejb.JcrLocal;
import eu.agileeng.util.AECollectionUtil;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AETimePeriod;
import eu.agileeng.util.AEValue;
import eu.agileeng.util.LightStringTokenizer;
import eu.agileeng.util.PasswordGenerator;
import eu.agileeng.util.json.JSONUtil;


/**
 *
 */
@SuppressWarnings("serial")
@Stateless
public class PartyBean extends AEBean implements PartyLocal {

	private static Logger logger = Logger.getLogger(PartyBean.class);
	
	@EJB private AddressLocal addressService;
	@EJB private ContactLocal contactService;
	@EJB private AccLocal accLocal;
	@EJB private AuthLocal authLocal;
	@EJB private JcrLocal jcrLocal;
	//	@EJB private AEDocumentLocal aeDocLocal;

	/**
	 * 
	 */
	public PartyBean() {
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.services.party.PartyService#manage(eu.agileeng.domain.contact.Organization)
	 */
	@Override
	public Organization manage(Organization organization, AEInvocationContext invContext) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			// delegate to local implementation
			manage(organization, invContext, localConnection);

			localConnection.commit();
			return organization;	
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

	/* (non-Javadoc)
	 * @see eu.agileeng.services.party.PartyService#manage(eu.agileeng.domain.contact.Person)
	 */
	@Override
	public Person manage(Person person, AEInvocationContext invContext) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			manage(person, invContext, localConnection);

			localConnection.commit();
			return person;	
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

	/* (non-Javadoc)
	 * @see eu.agileeng.services.party.ejb.PartyLocal#manage(eu.agileeng.domain.contact.Organization, eu.agileeng.persistent.AEConnection)
	 */
	@Override
	public void manage(Organization organization, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		assert(organization != null);
		assert(organization.getPersistentState() != null);
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// TODO organization validation

			// TODO authorize

			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();

			// do the real work here
			AEPersistent.State state = organization.getPersistentState();
			switch(state) {
			case NEW:
				insert(organization, invContext, aeConnection);
				break;
			case UPDATED:
				update(organization, invContext, aeConnection);
				break;
			case DELETED:
				break;
			case VIEW:
				// manage in depth
				manageInDepth(organization, invContext, localConnection);
				break;
			default:
				// must be never hapen
				assert(false);
			}

			localConnection.commit();	
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

	/* (non-Javadoc)
	 * @see eu.agileeng.services.party.ejb.PartyLocal#manage(eu.agileeng.domain.contact.Person, eu.agileeng.persistent.AEConnection)
	 */
	@Override
	public void manage(Person person, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		assert(person != null);
		assert(person.getPersistentState() != null);
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// TODO person validation

			// TODO authorize

			// begin transaction
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();

			// do the real work here
			AEPersistent.State state = person.getPersistentState();
			switch(state) {
			case NEW:
				insert(person, invContext, localConnection);
				break;
			case UPDATED:
				update(person, invContext, localConnection);
				break;
			case DELETED:
				break;
			case VIEW:
				// manage in depth
				manageInDepth(person, invContext, localConnection);
				break;
			default:
				// must be never hapen
				assert(false);
			}

			localConnection.commit();	
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

	private void update(Organization org, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();

			// update organization
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);
			orgDAO.update(org);

			// manage contacts
			ContactsList contacts = org.getContactsList();
			if(!AECollectionUtil.isEmpty(contacts)) {
				contactService.manage(contacts, org.getDescriptor(), localConnection);
			}

			// manage addresses
			AddressesList addresses = org.getAddressesList();
			if(!AECollectionUtil.isEmpty(addresses)) {
				addressService.manage(addresses, org.getDescriptor(), localConnection);
			}

			localConnection.commit();
		} catch (Throwable t) {
			t.printStackTrace();
			localConnection.rollback();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private void manageInDepth(Party party, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();

			// manage contacts
			ContactsList contacts = party.getContactsList();
			if(!AECollectionUtil.isEmpty(contacts)) {
				contactService.manage(contacts, party.getDescriptor(), localConnection);
			}

			// manage addresses
			AddressesList addresses = party.getAddressesList();
			if(!AECollectionUtil.isEmpty(addresses)) {
				addressService.manage(addresses, party.getDescriptor(), localConnection);
			}

			localConnection.commit();
		} catch (Throwable t) {
			t.printStackTrace();
			localConnection.rollback();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private void update(Person person, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();

			// update organization
			PersonDAO personDAO = daoFactory.getPersonDAO(localConnection);
			prepareUpdate(person, invContext, localConnection);
			personDAO.update(person);

			// manage contacts
			ContactsList contacts = person.getContactsList();
			if(!AECollectionUtil.isEmpty(contacts)) {
				contactService.manage(contacts, person.getDescriptor(), localConnection);
			}

			// manage addresses
			AddressesList addresses = person.getAddressesList();
			if(!AECollectionUtil.isEmpty(addresses)) {
				addressService.manage(addresses, person.getDescriptor(), localConnection);
			}

			localConnection.commit();
		} catch (Throwable t) {
			t.printStackTrace();

			localConnection.rollback();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private void insert(Organization org, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();

			// prepare insert
			prepareInsert(org, invContext, aeConnection);

			// insert organization
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);
			orgDAO.insert(org);

			// manage contacts
			ContactsList contacts = org.getContactsList();
			if(!AECollectionUtil.isEmpty(contacts)) {
				contactService.manage(contacts, org.getDescriptor(), localConnection);
			}

			// manage addresses
			AddressesList addresses = org.getAddressesList();
			if(!AECollectionUtil.isEmpty(addresses)) {
				addressService.manage(addresses, org.getDescriptor(), localConnection);
			}

			localConnection.commit();
		} catch (Throwable t) {
			t.printStackTrace();

			localConnection.rollback();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private void insert(Person person, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();

			// prepare insert
			prepareInsert(person, invContext, aeConnection);

			// insert person
			PersonDAO personDAO = daoFactory.getPersonDAO(localConnection);
			personDAO.insert(person);
			AEDescriptor personDescr = person.getDescriptor();
			assert(personDescr != null
					&& personDescr.isPersistent()
					&& personDescr.getClazz() != null);

			// manage contacts
			ContactsList contacts = person.getContactsList();
			if(!AECollectionUtil.isEmpty(contacts)) {
				contactService.manage(contacts, person.getDescriptor(), localConnection);
			}

			// manage addresses
			AddressesList addresses = person.getAddressesList();
			if(!AECollectionUtil.isEmpty(addresses)) {
				addressService.manage(addresses, person.getDescriptor(), localConnection);
			}

			localConnection.commit();
		} catch (Throwable t) {
			t.printStackTrace();

			localConnection.rollback();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/**
	 * private where no need for different  
	 */
	private void prepareInsert(AEDomainObject domObj, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		/**
		 * validate insert
		 */

		/**
		 * prepare
		 */
		Date dateNow = new Date();
		domObj.setCreator(invContext.getAuthPrincipal().getName());
		domObj.setTimeCreated(dateNow);
		domObj.setModifier(invContext.getAuthPrincipal().getName());
		domObj.setTimeModified(dateNow);

		if(domObj instanceof Person) {
			buildFullName((Person) domObj);
		}
	}

	/**
	 * private where no need for different  
	 */
	private void prepareInsert(Employee empl, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		/**
		 * validate insert
		 */

		// prepare super
		prepareInsert((AEDomainObject)empl, invContext, aeConnection);

		/**
		 * deal with ftpId
		 */
		if(empl.getFtpId() == null) {
			// generate ftpId
			Long ftpId = generateEmplFtpId(empl.getCompany().getDescriptor(), invContext, aeConnection);
			if(ftpId != null) {
				empl.setFtpId(ftpId);
			}
		}
	}

	@Override
	public Long generateEmplFtpId(AEDescriptor ownerDescr, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			Long ftpId = null;

			// DB connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			// check whether to generate or not
			SocialDAO socDAO = daoFactory.getSocialDAO(localConnection);
			Long emplIdentSeed = socDAO.loadEmplIdentSeed(ownerDescr);
			if(emplIdentSeed != null) {
				// should generate
				ftpId = emplIdentSeed;

				// take in account already generated numbers
				EmployeeDAO emplDAO = daoFactory.getEmployeeDAO(localConnection);
				Long l = emplDAO.loadMaxFtpId(ownerDescr);
				if(l != null && l > emplIdentSeed) {
					ftpId = l;
				}
			}

			// next ID 
			if(ftpId != null) {
				ftpId++;
			}

			return ftpId;
		} catch (Throwable t) {
			AEApp.logger().error("generateEmplFtpId: ", t);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}	
	}

	private void buildFullName(Person person) {
		StringBuilder builder = new StringBuilder();
		if(!AEStringUtil.isEmpty(person.getLastName())) {
			builder.append(person.getLastName());
			if(!AEStringUtil.isEmpty(person.getFirstName())) {
				builder.append(" ");
			}
		}
		if(!AEStringUtil.isEmpty(person.getFirstName())) {
			builder.append(person.getFirstName());
		}
		person.setName(builder.toString());
	}



	/* (non-Javadoc)
	 * @see eu.agileeng.services.party.PartyService#load(eu.agileeng.domain.AEDescriptor, eu.agileeng.services.AEInvocationContext)
	 */
	@Override
	public Party load(AEDescriptor partyDescr, AEInvocationContext invContext) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// TODO authorize

			// get connection
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			PartyDAO partyDAO = null;
			switch(partyDescr.getClazz()) {
			case ORGANIZATION:
				partyDAO = daoFactory.getOrganizationDAO(localConnection);
				break;
			case PERSON:
				partyDAO = daoFactory.getPersonDAO(localConnection);
				break;
			default:
				throw new AEException("Internal error: Unknown partyDescr.getClazz()");
			}
			Party party = partyDAO.load(partyDescr);

			return party;
		} catch (Throwable t) {
			logger.error(t);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}	
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.services.party.PartyService#loadAll(eu.agileeng.domain.DomainModel.DomainClass, eu.agileeng.services.AEInvocationContext)
	 */
	@Override
	public PartiesList loadAll(DomainClass partyClazz, AEInvocationContext invContext) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// TODO authorize

			// get connection
			localConnection = daoFactory.getConnection();
			PartyDAO partyDAO = null;
			switch(partyClazz) {
			case ORGANIZATION:
				partyDAO = daoFactory.getOrganizationDAO(localConnection);
				break;
			case PERSON:
				partyDAO = daoFactory.getPersonDAO(localConnection);
				break;
			default:
				break;
			}
			return partyDAO.loadAll();
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}	
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.services.party.PartyService#loadEmployee(eu.agileeng.domain.AEDescriptor, eu.agileeng.services.AEInvocationContext)
	 */
	@Override
	public Employee loadEmployee(AEDescriptor emplDescr, AEInvocationContext invContext) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// TODO authorize

			// get connection
			localConnection = daoFactory.getConnection();
			EmployeeDAO emplDAO = daoFactory.getEmployeeDAO(localConnection);
			Employee empl = emplDAO.load(emplDescr);

			return empl;
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}	
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.services.party.PartyService#loadEmployeesToCompany(eu.agileeng.domain.AEDescriptor, eu.agileeng.services.AEInvocationContext)
	 */
	@Override
	public EmployeeList loadEmployeesToCompany(AEDescriptor compDescr, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// TODO authorize

			// get factories
			DAOFactory daoFactory = DAOFactory.getInstance();

			// get connection
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));

			// load employess
			EmployeeDAO emplDAO = daoFactory.getEmployeeDAO(localConnection);
			EmployeeList emplList = emplDAO.loadToCompany(compDescr);

			return emplList;
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}	
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.services.party.PartyService#save(eu.agileeng.domain.contact.Employee, eu.agileeng.services.AEInvocationContext)
	 */
	@Override
	public Employee save(Employee empl, AEInvocationContext invContext) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			// delegate to local save
			save(empl, invContext, localConnection);

			localConnection.commit();
			return empl;	
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
	public void save(Employee empl, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();

			//			// deal with person
			//			if(empl.getPerson() instanceof Person) {
			//				manage((Person) empl.getPerson(), invContext, localConnection);
			//			}
			//			
			//			// deal with company
			//			if(empl.getCompany() instanceof Organization) {
			//				manage((Organization) empl.getCompany(), invContext, localConnection);
			//			}

			// real work here
			EmployeeDAO emplDAO = daoFactory.getEmployeeDAO(localConnection);
			AEPersistent.State state = empl.getPersistentState();
			switch(state) {
			case NEW:
				prepareInsert(empl, invContext, localConnection);
				emplDAO.insert(empl);
				break;
			case UPDATED:
				prepareUpdate(empl, invContext, localConnection);
				emplDAO.update(empl);
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

			// manage contacts
			ContactsList contacts = empl.getContactsList();
			if(!AECollectionUtil.isEmpty(contacts)) {
				contactService.manage(contacts, empl.getDescriptor(), localConnection);
			}

			// manage addresses
			AddressesList addresses = empl.getAddressesList();
			if(!AECollectionUtil.isEmpty(addresses)) {
				addressService.manage(addresses, empl.getDescriptor(), localConnection);
			}

			localConnection.commit();
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
		if(AEStringUtil.isEmpty(aeObject.getCreator())) {
			aeObject.setCreator(invContext.getAuthPrincipal().getName());
		}
		if(aeObject.getTimeCreated() == null) {
			aeObject.setTimeCreated(dateNow);
		}
		aeObject.setModifier(invContext.getAuthPrincipal().getName());
		aeObject.setTimeModified(dateNow);
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.services.party.PartyService#loadCompaniesToSubject(eu.agileeng.domain.AEDescriptor, eu.agileeng.services.AEInvocationContext)
	 */
	@Override
	public PartiesList loadCompaniesToSubject(AEDescriptor subjDescr, AEInvocationContext invContext) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// TODO authorize

			// get connection
			localConnection = daoFactory.getConnection();

			PartyDAO compDAO = daoFactory.getOrganizationDAO(localConnection);
			PartiesList partiesList = compDAO.loadToPrincipal(subjDescr);

			return partiesList;
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}	
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.services.party.PartyService#loadSubjectCompAssoc(eu.agileeng.domain.AEDescriptor, eu.agileeng.services.AEInvocationContext)
	 */
	@Override
	public SubjectCompAssoc loadSubjectCompAssoc(AEDescriptor assocDescr, AEInvocationContext invContext) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// TODO authorize

			// get connection
			localConnection = daoFactory.getConnection();
			SubjectCompAssocDAO assocDAO = daoFactory.getSubjectCompAssocDAO(localConnection);
			SubjectCompAssoc assoc = assocDAO.load(assocDescr);

			return assoc;
		} catch (Throwable t) {
			t.printStackTrace();
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
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// TOTO validation

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

	/* (non-Javadoc)
	 * @see eu.agileeng.services.party.PartyService#loadAllDescriptors(eu.agileeng.domain.DomainModel.DomainClass, eu.agileeng.services.AEInvocationContext)
	 */
	@Override
	public AEDescriptorsList loadAllDescriptors(DomainClass partyClazz, AEInvocationContext invContext) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// TODO authorize

			// get connection
			localConnection = daoFactory.getConnection();
			PartyDAO partyDAO = null;
			switch(partyClazz) {
			case ORGANIZATION:
				partyDAO = daoFactory.getOrganizationDAO(localConnection);
				break;
			case PERSON:
				partyDAO = daoFactory.getPersonDAO(localConnection);
				break;
			default:
				break;
			}
			return partyDAO.loadAllDescriptors(partyClazz);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}	
	}

	@Override
	public AEResponse saveCustomer(AERequest request, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// authorize
			authorize(new AuthPermission("System/Configuration/Customer", AuthPermission.ALL), invContext);

			// get arguments
			JSONObject arguments = request.getArguments();
			JSONObject customer = arguments.getJSONObject(Organization.JSONKey.customer);
			Contact contact = new Contact(Contact.Type.BUSINESS);
			Address address = new Address(Address.Type.BUSINESS);
			
			// validate input
			ParoisseValidator.getInstance().validate(customer);

			// get connection and begin transaction
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			// init as customer
			customer.put(Organization.JSONKey.customer, 1);

			// process customer
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);
			int dbState = -1;
			if(customer.has(AEDomainObject.JSONKey.dbState.name())) {
				dbState = customer.getInt(AEDomainObject.JSONKey.dbState.name());
				switch(dbState) {
				case AEPersistentUtil.DB_ACTION_NONE: {
					contact.setView();
					address.setView();
					break;
				}
				case AEPersistentUtil.DB_ACTION_INSERT: {
					orgDAO.checkCustomerUniqueness(customer);
					orgDAO.insertOrganization(customer);
					authLocal.createDefaultAuthPrincipal(customer, invContext, localConnection);
					break;
				}
				case AEPersistentUtil.DB_ACTION_UPDATE: {
					contact.setView();
					contact.setUpdated();
					address.setView();
					address.setUpdated();
					orgDAO.update(customer);
					break;
				}
				case AEPersistentUtil.DB_ACTION_DELETE: {
					orgDAO.delete(customer);
					break;
				}
				}
			}

			// should be after customer processing
			AEDescriptor customerDescr = Organization.lazyDescriptor(customer.getLong(AEDomainObject.JSONKey.id.name()));

			// process contacts
			contact.create(customer);
			if(contact.getID() <= 0) {
				contact.setPersistentState(State.NEW);
			}
			contactService.manage(new ContactsList(contact) , customerDescr, localConnection);
			customer.put(Contact.key_contactId, contact.getID());

			// process address
			address.create(customer);
			if(address.getID() <= 0) {
				address.setPersistentState(State.NEW);
			}
			addressService.manage(new AddressesList(address) , customerDescr, localConnection);
			customer.put(Address.key_addressId, address.getID());

			// process customer's coa
			if(customer.has(Organization.JSONKey.chartOfAccounts)) {
				// authorize
				authorize(new AuthPermission("System/Configuration/Customer/ChartOfAccount", AuthPermission.ALL), invContext);

				JSONObject coa = customer.getJSONObject(Organization.JSONKey.chartOfAccounts);
				coa.put(AEDomainObject.JSONKey.ownerId.name(), customerDescr.getID());
				accLocal.saveCOA(coa, localConnection);
			}

			// process customer's social info
			if(customer.has(SocialInfo.JSONKey.socialInfo.toString())) {
				// authorize
				authorize(new AuthPermission("System/Configuration/Customer/SocialInfo", AuthPermission.SAVE), invContext);

				// prepare
				SocialInfo socialInfo = new SocialInfo();
				socialInfo.create(customer.getJSONObject(SocialInfo.JSONKey.socialInfo.toString()));
				socialInfo.setCompany(customerDescr);
				// generate password
				if(AEStringUtil.isEmpty(socialInfo.getPassword())) {
					PasswordGenerator passGen = new PasswordGenerator();
					socialInfo.setPassword(passGen.getPassword());
				}

				// process
				SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);
				if(socialInfo.getID() <= 0) {
					socialDAO.insert(socialInfo);
				} else {
					socialDAO.update(socialInfo);
				}

				// set to be returned
				customer.put(SocialInfo.JSONKey.socialInfo.toString(), socialInfo.toJSONObject());
			}

			if(dbState == AEPersistentUtil.DB_ACTION_INSERT) {
				// add to principal employment
				SubjectCompAssocDAO peDAO = daoFactory.getSubjectCompAssocDAO(localConnection);
				SubjectCompAssoc assoc = new SubjectCompAssoc(
						invContext.getAuthPrincipal().getDescriptor(), 
						customerDescr);
				prepareInsert(assoc, invContext, localConnection);
				peDAO.insert(assoc);
				
				// add to AuthPrincipal
				AuthPrincipal ap = invContext.getAuthPrincipal();
				if(ap != null) {
					AEDescriptorsSet descrSet = ap.getCompaniesSet();
					if(descrSet != null) {
						descrSet.add(customerDescr);
					}
				}
			}

			// commit transaction
			localConnection.commit();

			// create and return response
			JSONObject payload = new JSONObject();
			payload.put(Organization.JSONKey.customer, customer);
			return new AEResponse(payload);
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			logger.errorv("{0} in {1}#{2}: {3}", t.getClass().getSimpleName(), this.getClass().getSimpleName(), "saveCustomer", t.getMessage());
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/**
	 * Load the collection of all defined customers.
	 */
	@Override
	public AEResponse loadCustomers(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);
			
			// load customer(s)
			JSONObject arguments = aeRequest.getArguments();
			JSONArray customersArray = null;
			if(arguments.has(Organization.JSONKey.customerId)) {
				customersArray = orgDAO.loadCustomer(Organization.lazyDescriptor(arguments.getLong(Organization.JSONKey.customerId)));
			} else {
				customersArray = orgDAO.loadCustomers();
			}
			
			// load in depth
			if(!arguments.optBoolean("lazzyLoad", false)) {
				for (int i = 0; i < customersArray.length(); i++) {
					JSONObject customer = customersArray.getJSONObject(i);
					loadCustomerInDepth(customer, aeRequest, invContext, localConnection);
				}
			}
			
			JSONObject payload = new JSONObject();
			payload.put(Organization.JSONKey.customers, customersArray);
			return new AEResponse(payload);
		} catch (Throwable t) {
			logger.errorv("{0} in {1}#{2}: {3}", t.getClass().getSimpleName(), this.getClass().getSimpleName(), "loadCustomers", t.getMessage());
			throw new AEException(t); // keep source message
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse saveCompany(AERequest request) throws AEException {
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			// get arguments
			JSONObject arguments = request.getArguments();
			JSONObject company = arguments.getJSONObject("company");
			Contact contact = new Contact(Contact.Type.BUSINESS);
			Address address = new Address(Address.Type.BUSINESS);

			// get caler
			AEInvocationContext invContext = new AEInvocationContextImp(request.getAuthPrincipal());

			// validate caler
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// TODO authorize caler for this action

			// process organization
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);
			int dbState = -1;
			if(company.has("dbState")) {
				dbState = company.getInt("dbState");
				switch(dbState) {
				case AEPersistentUtil.DB_ACTION_NONE: {
					contact.setView();
					address.setView();
					break;
				}
				case AEPersistentUtil.DB_ACTION_INSERT: {
					orgDAO.insertOrganization(company);
					break;
				}
				case AEPersistentUtil.DB_ACTION_UPDATE: {
					contact.setView();
					contact.setUpdated();
					address.setView();
					address.setUpdated();
					orgDAO.update(company);
					break;
				}
				case AEPersistentUtil.DB_ACTION_DELETE: {
					orgDAO.delete(company);
					break;
				}
				}
			}

			// should be after customer processing
			AEDescriptor customerDescr = new AEDescriptorImp(company.getLong("id"), DomainClass.ORGANIZATION);

			// process contacts
			contact.create(company);
			contactService.manage(new ContactsList(contact) , customerDescr, localConnection);
			company.put(Contact.key_contactId, contact.getID());

			// process address
			address.create(company);
			addressService.manage(new AddressesList(address) , customerDescr, localConnection);
			company.put(Address.key_addressId, address.getID());

			// commit transaction
			localConnection.commit();

			// create and return response
			JSONObject payload = new JSONObject();
			payload.put("company", company);
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			if(localConnection != null) {
				localConnection.rollback();
			}
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadCompanies(AERequest request) throws AEException {
		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			// validate caler
			AEInvocationContext invContext = new AEInvocationContextImp(request.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// TODO authorize

			// load companies
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);
			JSONObject arguments = request.getArguments();
			JSONArray companiesArray = orgDAO.loadCompanies(
					arguments.getLong("ownerId"), 
					arguments.optLong("nature"));

			// load every company in depth
			for (int i = 0; i < companiesArray.length(); i++) {
				JSONObject company = companiesArray.getJSONObject(i);

				// should be after companies processing
				AEDescriptor companyDescr = new AEDescriptorImp(company.getLong("id"), DomainClass.ORGANIZATION);

				// load contact
				ContactsList contacts = contactService.load(companyDescr, localConnection);
				if(contacts != null && !contacts.isEmpty()) {
					Contact contact = contacts.get(0);
					JSONObject contactJSON = contact.toJSONObject();
					JSONUtil.apply(company, contactJSON);
				}

				// load address
				AddressesList addressesList = addressService.load(companyDescr, localConnection);
				if(addressesList != null && !addressesList.isEmpty()) {
					Address address = addressesList.get(0);
					JSONObject addressJSON = address.toJSONObject();
					JSONUtil.apply(company, addressJSON);
				}
			}

			// check for accounts
			JSONArray accountsArray = null;
			if(arguments.has("loadCOA") && arguments.optBoolean("loadCOA")) {
				ChartOfAccountsDAO coaModelDAO = daoFactory.getChartOfAccountsDAO(localConnection);
				AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);
				JSONObject coa = coaModelDAO.loadByCustomer(arguments.getLong("ownerId"));
				if(coa.has("id")) {
					accountsArray = accountDAO.loadAccounts(coa.getLong("id"));
				}
			}

			JSONObject payload = new JSONObject();
			payload.put("companies", companiesArray);
			if(accountsArray != null) {
				payload.put("accounts", accountsArray);
			}
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadSupplyData(AERequest request) throws AEException {
		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			// validate caler
			AEInvocationContext invContext = new AEInvocationContextImp(request.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// TODO authorize

			// load companies
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);
			JSONObject arguments = request.getArguments();

			JSONArray companiesArray = null;
			if(arguments.optLong("docType") == 100 || arguments.optLong("docType") == 110) {
				companiesArray = orgDAO.loadCompanies(arguments.getLong("ownerId"), 30);
			} else if(arguments.optLong("docType") == 200) {
				companiesArray = orgDAO.loadCompanies(arguments.getLong("ownerId"), 10);
			}

			// load accounts
			JSONArray itemAccounts = null;
			if(arguments.optLong("docType") == 100 || arguments.optLong("docType") == 110) {
				itemAccounts = accLocal.loadSupplyAccountsByOwner(
						arguments.getLong("ownerId"), 
						localConnection);
			} else if(arguments.optLong("docType") == 200) {
				itemAccounts = accLocal.loadSaleAccountsByOwner(
						arguments.getLong("ownerId"), 
						localConnection);
			}

			// load period
			AccPeriod accPeriod = null;
			if(arguments.optLong("docType") == AEDocumentType.System.AEPurchaseInvoice.getSystemID()) {
				accPeriod = getFirstOpenPeriod(
						arguments.getLong("ownerId"),
						AEApp.PURCHASE_MODULE_ID,
						localConnection);
			} else if(arguments.optLong("docType") == AEDocumentType.System.AEPurchaseInvoiceFNP.getSystemID()) {
				accPeriod = getFirstOpenPeriod(
						arguments.getLong("ownerId"),
						AEApp.PURCHASE_FNP_MODULE_ID,
						localConnection);
			} else if(arguments.optLong("docType") == AEDocumentType.System.AESaleInvoice.getSystemID()) {
				accPeriod = getFirstOpenPeriod(
						arguments.getLong("ownerId"),
						AEApp.SALE_MODULE_ID,
						localConnection);
			}
			if(accPeriod == null) {
				throw new AEException("Internal Error: Cannot detect accounting period!");
			}

			// load templates
			AETradeDocumentDAO docDAO = daoFactory.getAETradeDocumentDAO(localConnection);
			AEDocumentsList docTemplates = null;
			if(arguments.optLong("docType") == 100 || arguments.optLong("docType") == 110) {
				docTemplates = docDAO.loadSupplyTemplates(arguments.getLong("ownerId"));
			} else if(arguments.optLong("docType") == 200) {
				docTemplates = docDAO.loadSaleTemplates(arguments.getLong("ownerId"));
			}

			// create and return response
			JSONObject payload = new JSONObject();
			payload.put("companies", companiesArray);
			payload.put("accounts", itemAccounts);
			payload.put("templates", docTemplates.toJSONArray());
			if(accPeriod != null) {
				payload.put("period", AEDateUtil.convertToString(accPeriod.getStartDate(), "MM/yyyy"));
				payload.put("startDate", AEDateUtil.convertToString(accPeriod.getStartDate(), AEDateUtil.SYSTEM_DATE_FORMAT));
				payload.put("startDateMonth", AEDateUtil.getMonthInYear(accPeriod.getStartDate()));
				payload.put("startDateYear", AEDateUtil.getYear(accPeriod.getStartDate()));
				payload.put("endDate", AEDateUtil.convertToString(accPeriod.getEndDate(), AEDateUtil.SYSTEM_DATE_FORMAT));
			}
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse deleteCompany(AERequest request) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContext invContext = new AEInvocationContextImp(request.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// get arguments
			JSONObject arguments = request.getArguments();
			JSONObject company = arguments.getJSONObject("company");
			AEDescriptor companyDescr = Organization.lazyDescriptor(company.optLong("id"));

			// get connection and begin transaction
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			// delete organization
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);
			orgDAO.delete(company);

			// delete contacts
			ContactDAO contactDAO = daoFactory.getContactDAO(localConnection);
			contactDAO.delete(companyDescr);

			// delete addresses
			AddressDAO addressDAO = daoFactory.getAddressDAO(localConnection);
			addressDAO.delete(companyDescr);

			// commit transaction
			localConnection.commit();

			// create and return response
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Throwable t) {
			if(localConnection != null) {
				localConnection.rollback();
			}
			throw new AEException(
					"The party cannot be deleted because it is used by another object(s). You can deactivate it.");
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse deactivateCompany(AERequest request, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// get arguments
			JSONObject arguments = request.getArguments();
			long ownerId = arguments.getLong("sOwnerId");
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);

			// get connection and begin transaction
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			// get in-memory reference to this company
			OrganizationDescriptor ownerDescrRef = null;
			AuthPrincipal ap = invContext.getAuthPrincipal();
			AEDescriptorsSet companiesSet = ap.getCompaniesSet();
			for (AEDescriptor compDescr : companiesSet) {
				if(compDescr.equals(ownerDescr) && compDescr instanceof OrganizationDescriptor) {
					ownerDescrRef = (OrganizationDescriptor) compDescr;
					break;
				}
			}
			
			// check in-memory reference
			if(ownerDescrRef == null || !ownerDescrRef.isActive()) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}
			
			// deactivate organization
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);
			orgDAO.deactivate(ownerDescrRef);
			
			// update in-memory data
			ownerDescrRef.setActive(false);

			// commit transaction
			localConnection.commit();

			// create and return response
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse activateCompany(AERequest request, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// get arguments
			JSONObject arguments = request.getArguments();
			long ownerId = arguments.getLong("sOwnerId");
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);

			// get connection and begin transaction
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			// get in-memory reference to this company
			OrganizationDescriptor ownerDescrRef = null;
			AuthPrincipal ap = invContext.getAuthPrincipal();
			AEDescriptorsSet companiesSet = ap.getCompaniesSet();
			for (AEDescriptor compDescr : companiesSet) {
				if(compDescr.equals(ownerDescr) && compDescr instanceof OrganizationDescriptor) {
					ownerDescrRef = (OrganizationDescriptor) compDescr;
					break;
				}
			}
			
			// check in-memory reference
			if(ownerDescrRef == null || ownerDescrRef.isActive()) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}
			
			// activate organization
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);
			orgDAO.activate(ownerDescrRef);
			
			// update in-memory data
			ownerDescrRef.setActive(true);

			// commit transaction
			localConnection.commit();

			// create and return response
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	
	@Override
	public AEResponse newCustomerByTemplate(AERequest request) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContext invContext = new AEInvocationContextImp(request.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// get arguments
			JSONObject arguments = request.getArguments();
			Long ownerId = arguments.getLong("ownerId");

			// get connection and begin transaction
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			JSONObject customer = null;

			// TODO
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);
			JSONArray customersArray = orgDAO.loadCustomer(Organization.lazyDescriptor(ownerId));
			for (int i = 0; i < customersArray.length(); i++) {
				customer = customersArray.getJSONObject(i);
				customer.put("mapId", customer.getLong("id"));
				customer.put("properties", 0);
				customer.put(
						"name", 
						customer.optString("name") + " - Copie");
				customer.put("code", AEStringUtil.EMPTY_STRING);

				/**
				 * Customer
				 */
				orgDAO.insertOrganization(customer);
				AEDescriptor customerDescr = new AEDescriptorImp(customer.getLong("id"), DomainClass.ORGANIZATION);

				/**
				 * principal employment
				 */
				SubjectCompAssocDAO peDAO = daoFactory.getSubjectCompAssocDAO(localConnection);
				SubjectCompAssoc assoc = new SubjectCompAssoc(
						request.getAuthPrincipal().getDescriptor(), 
						customerDescr);
				prepareInsert(assoc, invContext, localConnection);
				peDAO.insert(assoc);

				/**
				 * COA
				 */
				JSONObject coa = accLocal.loadCOA(ownerId, localConnection);
				coa.put("mapId", coa.getLong("id"));
				coa.put("ownerId", customer.getLong("id"));
				coa.put("dbState", AEPersistentUtil.DB_ACTION_INSERT);

				/**
				 * coa accounts
				 */
				JSONArray accounts = coa.optJSONArray("accounts");
				Map<Long, Long> accountsMap = new HashMap<Long, Long>();
				if(accounts != null) {
					for (int j = 0; j < accounts.length(); j++) {
						JSONObject acc = accounts.getJSONObject(j);
						acc.put("dbState", AEPersistentUtil.DB_ACTION_INSERT);
						acc.put("mapId", acc.getLong("id"));
						acc.put("coaId", coa.getLong("id"));
						acc.put("system", false);
					}
				}	
				accLocal.saveCOA(coa, localConnection);
				if(accounts != null) {
					for (int j = 0; j < accounts.length(); j++) {
						JSONObject acc = accounts.getJSONObject(j);
						accountsMap.put(acc.optLong("mapId"), acc.optLong("id"));
					}
				}	

				/**
				 * GOA
				 */
				AEResponse aeResponseGOA = accLocal.loadGOA(request);
				ChartOfAccountsDAO coaDAO = daoFactory.getChartOfAccountsDAO(localConnection);
				Map<Long, Long> accountsGOAMap = new HashMap<Long, Long>();
				Map<Long, Long> attributesGOAMap = new HashMap<Long, Long>();
				Map<Long, Long> partiesMap = new HashMap<Long, Long>();

				/**
				 * GOA Accounts
				 */
				// assets accounts
				JSONObject assetsGOA = coaDAO.loadGOA(
						ownerId, 
						request.getArguments().optJSONObject("assetsGOA").optLong("sysId"));
				// {"moduleId":0,"dbState":0,"modifiable":1,"ownerId":21,"code":"assets","dateClosed":"2011-05-31","id":15,"parentId":1,"system":0,"sysId":2,"description":"","name":"CashDesk Assets","active":1}
				if(assetsGOA.has("id")) {
					assetsGOA.put("mapId", assetsGOA.getLong("id"));
					assetsGOA.put("ownerId", customer.getLong("id"));
					assetsGOA.remove("moduleId");
					assetsGOA.put("dbState", AEPersistentUtil.DB_ACTION_INSERT);
					JSONArray assetsArray = aeResponseGOA.getPayload().optJSONObject("assetsGOA").optJSONArray("accounts");
					if(assetsArray != null) {
						for (int j = 0; j < assetsArray.length(); j++) {
							JSONObject account = assetsArray.getJSONObject(j);
							// {"id":297,"dbState":0,"description":"Compte Compagnie","name":"Gasoil","vat":{"id":0,"rate":0},"coaId":15,"attributes":[{"id":55,"xType":"text","dbState":0,"name":"Litrage GO","required":true,"accId":297}],"sIndex":0,"code":"4675000000","accId":15195}
							account.put("mapId", account.getLong("id"));
							account.put("coaId", assetsGOA.getLong("id"));
							account.put("accId", accountsMap.get(account.getLong("accId")));
							account.put("dbState", AEPersistentUtil.DB_ACTION_INSERT);

							// attributes
							JSONArray attrArray = account.optJSONArray("attributes");
							if(attrArray != null) {
								for (int k = 0; k < attrArray.length(); k++) {
									JSONObject attribute = attrArray.getJSONObject(k);
									attribute.put("mapId", attribute.getLong("id"));
									attribute.put("dbState", AEPersistentUtil.DB_ACTION_INSERT);
								}
							}
						}
					}
					assetsGOA.put("accounts", assetsArray);
					accLocal.saveGOA(assetsGOA, customer.getLong("id"), localConnection);

					// 
					if(assetsArray != null) {
						for (int j = 0; j < assetsArray.length(); j++) {
							JSONObject account = assetsArray.getJSONObject(j);
							accountsGOAMap.put(account.getLong("mapId"), account.getLong("id"));

							// attributes
							JSONArray attrArray = account.optJSONArray("attributes");
							if(attrArray != null) {
								for (int k = 0; k < attrArray.length(); k++) {
									JSONObject attribute = attrArray.getJSONObject(k);
									attributesGOAMap.put(attribute.getLong("mapId"), attribute.getLong("id"));
								}
							}
						}
					}
				}

				// expenses accounts
				JSONObject expensesGOA = coaDAO.loadGOA(
						ownerId, 
						request.getArguments().optJSONObject("expensesGOA").optLong("sysId"));
				if(expensesGOA.has("id")) {
					expensesGOA.put("mapId", expensesGOA.getLong("id"));
					expensesGOA.put("ownerId", customer.getLong("id"));
					expensesGOA.remove("moduleId");
					expensesGOA.put("dbState", AEPersistentUtil.DB_ACTION_INSERT);
					JSONArray expensesArray = aeResponseGOA.getPayload().optJSONObject("expensesGOA").optJSONArray("accounts");
					if(expensesArray != null) {
						for (int j = 0; j < expensesArray.length(); j++) {
							JSONObject account = expensesArray.getJSONObject(j);

							account.put("mapId", account.getLong("id"));
							account.put("coaId", expensesGOA.getLong("id"));
							account.put("accId", accountsMap.get(account.getLong("accId")));
							account.put("dbState", AEPersistentUtil.DB_ACTION_INSERT);

							// attributes
							JSONArray attrArray = account.optJSONArray("attributes");
							if(attrArray != null) {
								for (int k = 0; k < attrArray.length(); k++) {
									JSONObject attribute = attrArray.getJSONObject(k);
									attribute.put("dbState", AEPersistentUtil.DB_ACTION_INSERT);
									attribute.put("mapId", attribute.getLong("id"));
								}
							}
						}
					}			
					expensesGOA.put("accounts", expensesArray);
					accLocal.saveGOA(expensesGOA, customer.getLong("id"), localConnection);

					// 
					if(expensesArray != null) {
						for (int j = 0; j < expensesArray.length(); j++) {
							JSONObject account = expensesArray.getJSONObject(j);
							accountsGOAMap.put(account.getLong("mapId"), account.getLong("id"));

							// attributes
							JSONArray attrArray = account.optJSONArray("attributes");
							if(attrArray != null) {
								for (int k = 0; k < attrArray.length(); k++) {
									JSONObject attribute = attrArray.getJSONObject(k);
									attributesGOAMap.put(attribute.getLong("mapId"), attribute.getLong("id"));
								}
							}
						}
					}
				}


				/**
				 * CFC
				 */
				CFCDAO cfcDAO = daoFactory.getCFCDAO(localConnection);
				CFC cfc = cfcDAO.loadCFC(Organization.lazyDescriptor(ownerId));
				if(cfc != null) {
					cfc.setCfcModel(cfcDAO.loadCFCModel(cfc.getDescriptor()));

					cfc.setMapId(cfc.getID());
					cfc.setCompany(customerDescr);
					cfc.setPersistentState(AEPersistent.State.NEW);
					cfcDAO.insertCFC(cfc);

					if(cfc.getCfcModel() != null) {
						CFCModel cfcModel = cfc.getCfcModel();
						for (Iterator<CFCColumn> iterator = cfcModel.iterator(); iterator.hasNext();) {
							CFCColumn cfcColumn = (CFCColumn) iterator.next();
							cfcColumn.setToCFC(cfc.getDescriptor());
							if(cfcColumn.getAccBank() != null) {
								Long newAccBankId = accountsMap.get(cfcColumn.getAccBank().getDescriptor().getID());
								if(newAccBankId != null) {
									AEDescriptive newAccBank = AccAccount.lazyDescriptor(newAccBankId);
									cfcColumn.setAccBank(newAccBank);
								} else {
									cfcColumn.setAccBank(null);
								}
							}
							if(!AEValue.isNull(cfcColumn.getValue()) && CFCColumn.NType.ACCOUNT.equals(cfcColumn.getNType())) {
								Long newAccGOAId = accountsGOAMap.get(cfcColumn.getValue().optLong());
								AEValue nv = new AEValue();
								if(newAccGOAId != null) {
									nv.set(Long.toString(newAccGOAId));
								}
								cfcColumn.setValue(nv);
							}
							cfcDAO.insertCFCColumn(cfcColumn);
						}
					}
				}

				/**
				 * Mandat
				 */
				MandatDAO mandatDAO = daoFactory.getMandatDAO(localConnection);
				CFC mandat = mandatDAO.loadMandat(Organization.lazyDescriptor(ownerId));
				if(mandat != null) {
					mandat.setCfcModel(mandatDAO.loadMandatModel(mandat.getDescriptor()));

					mandat.setMapId(mandat.getID());
					mandat.setCompany(customerDescr);
					mandat.setPersistentState(AEPersistent.State.NEW);
					mandatDAO.insertMandat(mandat);

					if(mandat.getCfcModel() != null) {
						CFCModel mandatModel = mandat.getCfcModel();
						for (Iterator<CFCColumn> iterator = mandatModel.iterator(); iterator.hasNext();) {
							CFCColumn mandatColumn = (CFCColumn) iterator.next();
							mandatColumn.setToCFC(mandat.getDescriptor());
							if(mandatColumn.getAccBank() != null) {
								Long newAccBankId = accountsMap.get(mandatColumn.getAccBank().getDescriptor().getID());
								if(newAccBankId != null) {
									AEDescriptive newAccBank = AccAccount.lazyDescriptor(newAccBankId);
									mandatColumn.setAccBank(newAccBank);
								} else {
									mandatColumn.setAccBank(null);
								}
							}
							if(!AEValue.isNull(mandatColumn.getValue()) && CFCColumn.NType.ACCOUNT.equals(mandatColumn.getNType())) {
								Long newAccGOAId = accountsGOAMap.get(mandatColumn.getValue().optLong());
								AEValue nv = new AEValue();
								if(newAccGOAId != null) {
									nv.set(Long.toString(newAccGOAId));
								}
								mandatColumn.setValue(nv);
							} else if(!AEValue.isNull(mandatColumn.getValue()) && CFCColumn.NType.ATTRIBUTE.equals(mandatColumn.getNType())) {
								Long newAttrId = attributesGOAMap.get(mandatColumn.getValue().optLong());
								AEValue nv = new AEValue();
								if(newAttrId != null) {
									nv.set(Long.toString(newAttrId));
								}
								mandatColumn.setValue(nv);
							}
							mandatDAO.insertMandatColumn(mandatColumn);
						}
					}
				}

				/**
				 * suppliers
				 */
				JSONArray suppliersArray = orgDAO.loadCompanies(arguments.getLong("ownerId"), 30);
				for (int j = 0; j < suppliersArray.length(); j++) {
					JSONObject company = suppliersArray.getJSONObject(j);

					// load contact
					ContactsList contacts = contactService.load(
							Organization.lazyDescriptor(company.getLong("id")), 
							localConnection);
					for (Contact contact : contacts) {
						contact.setPersistentState(AEPersistent.State.NEW);
					}

					// load address
					AddressesList addressesList = addressService.load(
							Organization.lazyDescriptor(company.getLong("id")), 
							localConnection);
					for (Address address : addressesList) {
						address.setPersistentState(AEPersistent.State.NEW);
					}

					// {"ownerId":23,"compteGeneralId":16686,
					company.put("mapId", company.getLong("id"));
					company.put("ownerId", customer.getLong("id"));
					company.put("dbState", AEPersistentUtil.DB_ACTION_INSERT);
					company.put("compteGeneralId", accountsMap.get(company.optLong("compteGeneralId")));
					company.put("properties", 0);

					orgDAO.insertOrganization(company);
					partiesMap.put(company.getLong("mapId"), company.getLong("id"));

					// process contacts
					if(contacts != null && !contacts.isEmpty()) {
						contactService.manage(
								contacts, 
								Organization.lazyDescriptor(company.getLong("id")), 
								localConnection);
					}

					// process address
					if(addressesList != null && !addressesList.isEmpty()) {
						addressService.manage(
								addressesList, 
								Organization.lazyDescriptor(company.getLong("id")), 
								localConnection);
					}
				}

				/**
				 * clients
				 */
				JSONArray clientsArray = orgDAO.loadCompanies(arguments.getLong("ownerId"), 10);
				for (int j = 0; j < clientsArray.length(); j++) {
					JSONObject company = clientsArray.getJSONObject(j);

					// load contact
					ContactsList contacts = contactService.load(
							Organization.lazyDescriptor(company.getLong("id")), 
							localConnection);
					for (Contact contact : contacts) {
						contact.setPersistentState(AEPersistent.State.NEW);
					}

					// load address
					AddressesList addressesList = addressService.load(
							Organization.lazyDescriptor(company.getLong("id")), 
							localConnection);
					for (Address address : addressesList) {
						address.setPersistentState(AEPersistent.State.NEW);
					}

					// {"ownerId":23,"compteGeneralId":16686,
					company.put("mapId", company.getLong("id"));
					company.put("ownerId", customer.getLong("id"));
					company.put("dbState", AEPersistentUtil.DB_ACTION_INSERT);
					company.put("compteGeneralId", accountsMap.get(company.optLong("compteGeneralId")));
					company.put("properties", 0);

					orgDAO.insertOrganization(company);
					partiesMap.put(company.getLong("mapId"), company.getLong("id"));

					// process contacts
					if(contacts != null && !contacts.isEmpty()) {
						contactService.manage(
								contacts, 
								Organization.lazyDescriptor(company.getLong("id")), 
								localConnection);
					}

					// process address
					if(addressesList != null && !addressesList.isEmpty()) {
						addressService.manage(
								addressesList, 
								Organization.lazyDescriptor(company.getLong("id")), 
								localConnection);
					}
				}

				/**
				 * doc templates
				 */
				// load purchase templates
				AETradeDocumentDAO docDAO = daoFactory.getAETradeDocumentDAO(localConnection);
				AEDocumentsList docTemplates = docDAO.loadSupplyTemplates(ownerId);
				docTemplates.addAll(docDAO.loadSaleTemplates(ownerId));
				for (AEDocument aeDocument : docTemplates) {
					AEDocumentDescriptor docDescr = (AEDocumentDescriptor) aeDocument.getDescriptor();

					// load document
					AETradeDocument tDocument = (AETradeDocument) docDAO.load(aeDocument.getDescriptor());

					tDocument.setCompany(customerDescr);

					AEDescriptive accDescrNew = null;
					AEDescriptive accDescr = tDocument.getIssuerAcc();
					if(accDescr != null) {
						Long accIdNew = accountsMap.get(accDescr.getDescriptor().getID());
						if(accIdNew != null) {
							accDescrNew = AccAccount.lazyDescriptor(accIdNew);
						}
					}
					tDocument.setIssuerAcc(accDescrNew);

					accDescrNew = null;
					accDescr = tDocument.getRecipientAcc();
					if(accDescr != null) {
						Long accIdNew = accountsMap.get(accDescr.getDescriptor().getID());
						if(accIdNew != null) {
							accDescrNew = AccAccount.lazyDescriptor(accIdNew);
						}
					}
					tDocument.setRecipientAcc(accDescrNew);

					AEDescriptive partyDescrNew = null;
					AEDescriptive partyDescr = tDocument.getIssuer();
					if(partyDescr != null) {
						Long partyIdNew = partiesMap.get(partyDescr.getDescriptor().getID());
						if(partyIdNew != null) {
							partyDescrNew = Organization.lazyDescriptor(partyIdNew);
						}
					}
					tDocument.setIssuer(partyDescrNew);

					partyDescrNew = null;
					partyDescr = tDocument.getRecipient();
					if(partyDescr != null) {
						Long partyIdNew = partiesMap.get(partyDescr.getDescriptor().getID());
						if(partyIdNew != null) {
							partyDescrNew = Organization.lazyDescriptor(partyIdNew);
						}
					}
					tDocument.setRecipient(partyDescrNew);

					// concrete vat account
					AEDescriptive vatAccount = tDocument.getVatAccount();
					if(vatAccount != null) {
						Long vatAccountNewId = accountsMap.get(vatAccount.getDescriptor().getID());
						if(vatAccountNewId != null) {
							AEDescriptor vatAccountNew = AccAccount.lazyDescriptor(vatAccountNewId);
							tDocument.setVatAccount(vatAccountNew);
						} else {
							tDocument.setVatAccount(null);
						}
					}

					// load items
					AEDocumentItemDAO itemDAO = daoFactory.getAEDocumentItemDAO(localConnection);
					AEDocumentItemsList docItems = itemDAO.load(docDescr);
					for (AEDocumentItem aeDocumentItem : docItems) {						
						aeDocumentItem.setPersistentState(AEPersistent.State.NEW);

						accDescrNew = null;
						accDescr = aeDocumentItem.getAccount();
						if(accDescr != null) {
							Long accIdNew = accountsMap.get(aeDocumentItem.getAccount().getDescriptor().getID());
							if(accIdNew != null) {
								accDescrNew = AccAccount.lazyDescriptor(accIdNew);
							}
						}
						aeDocumentItem.setAccount(accDescrNew);

						partyDescrNew = null;
						partyDescr = aeDocumentItem.getParty();
						if(partyDescr != null) {
							Long partyIdNew = partiesMap.get(partyDescr.getDescriptor().getID());
							if(partyIdNew != null) {
								partyDescrNew = AccAccount.lazyDescriptor(partyIdNew);
							}
						}
						aeDocumentItem.setParty(partyDescrNew);

						aeDocumentItem.setPaid(false);

					}
					tDocument.setItems(docItems);

					// save
					insert(tDocument, invContext, localConnection);
					processItems(tDocument, invContext, localConnection);
				}

				/**
				 * SOT templates
				 */
				AEDocumentItemDAO itemDAO = daoFactory.getAEDocumentItemDAO(localConnection);
				AEDocumentFilter sotTemplatesFilter = new AEDocumentFilter();
				// all SOT descriptors
				AEDescriptorsList sotTemplatesDescrList = new AEDescriptorsList();

				// PERIMES
				sotTemplatesFilter.setTemplate(Boolean.TRUE);
				sotTemplatesFilter.setDocType(AEDocumentType.valueOf(AEDocumentType.System.PERIMES));
				sotTemplatesFilter.setOwner(Organization.lazyDescriptor(ownerId));
				sotTemplatesDescrList.addAll(docDAO.loadDescriptors(sotTemplatesFilter));

				// STOCKS
				sotTemplatesFilter.setDocType(AEDocumentType.valueOf(AEDocumentType.System.STOCKS));
				sotTemplatesDescrList.addAll(docDAO.loadDescriptors(sotTemplatesFilter));

				// SUIVI_GPL
				sotTemplatesFilter.setDocType(AEDocumentType.valueOf(AEDocumentType.System.SUIVI_GPL));
				sotTemplatesDescrList.addAll(docDAO.loadDescriptors(sotTemplatesFilter));

				// DONNEES
				sotTemplatesFilter.setDocType(AEDocumentType.valueOf(AEDocumentType.System.DONNEES));
				sotTemplatesDescrList.addAll(docDAO.loadDescriptors(sotTemplatesFilter));

				// process SOT templates
				for (AEDescriptor sotTemplateDescr : sotTemplatesDescrList) {
					// load template
					AEDocument sotTemplate = docDAO.load(sotTemplateDescr);
					if(sotTemplate == null) {
						continue;
					}

					// reset and init template
					sotTemplate.setID(AEPersistentUtil.NEW_ID);
					sotTemplate.setPersistentState(State.NEW);
					sotTemplate.setCompany(customerDescr);

					// load, reset and init items
					sotTemplate.setItems(itemDAO.load(sotTemplateDescr));
					for (AEDocumentItem sotTemplateItem : sotTemplate.getItems()) {	
						// reset item
						sotTemplateItem.setID(AEPersistentUtil.NEW_ID);
						sotTemplateItem.setPersistentState(AEPersistent.State.NEW);

						// redirect account
						AEDescriptor accDescrNew = null;
						if(sotTemplateItem.getAccount() != null) {
							AEDescriptor accDescr = sotTemplateItem.getAccount().getDescriptor();
							if(accDescr != null) {
								Long accIdNew = accountsMap.get(accDescr.getID());
								if(accIdNew != null) {
									accDescrNew = AccAccount.lazyDescriptor(accIdNew);
								}
							}
						}
						sotTemplateItem.setAccount(accDescrNew);

						// redirect account secondary
						AEDescriptor accSecondaryDescrNew = null;
						if(sotTemplateItem.getAccountSecondary() != null) {
							AEDescriptor accSecondaryDescr = sotTemplateItem.getAccountSecondary().getDescriptor();
							if(accSecondaryDescr != null) {
								Long accSecondaryIdNew = accountsMap.get(accSecondaryDescr.getID());
								if(accSecondaryIdNew != null) {
									accSecondaryDescrNew = AccAccount.lazyDescriptor(accSecondaryIdNew);
								}
							}
						}
						sotTemplateItem.setAccountSecondary(accSecondaryDescrNew);
					}

					// save SOT template
					insert(sotTemplate, invContext, localConnection);
					processItems(sotTemplate, invContext, localConnection);
				}

				// AppConfig
				invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
				AEResponse aeAppConfigResponse = authLocal.loadAppConfig(request, invContext);

				JSONArray aeAppConfigJSONArray =  aeAppConfigResponse.getPayload().optJSONArray("forbiddenModules");
				if(aeAppConfigJSONArray != null) {
					AEAppConfigList aeAppConfigList = new AEAppConfigList();
					aeAppConfigList.create(aeAppConfigJSONArray);
				}
				JSONObject aeAppConfigArg = new JSONObject();
				aeAppConfigArg.put("ownerId", customerDescr.getDescriptor().getID());
				aeAppConfigArg.put("forbiddenModules", aeAppConfigJSONArray);
				AERequest aeAppConfigRequest = new AERequest(aeAppConfigArg);

				invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
				authLocal.saveAppConfig(aeAppConfigRequest, invContext);

				/**
				 * PAM
				 */
				PAMDAO pamDAO = daoFactory.getPAMDAO(localConnection);
				CFC pam = pamDAO.loadPAM(Organization.lazyDescriptor(ownerId));
				if(pam != null) {
					pam.setCfcModel(pamDAO.loadPAMModel(pam.getDescriptor()));

					pam.setMapId(pam.getID());
					pam.setCompany(customerDescr);
					pam.setPersistentState(AEPersistent.State.NEW);
					pamDAO.insertPAM(pam);

					if(pam.getCfcModel() != null) {
						CFCModel pamModel = pam.getCfcModel();
						for (Iterator<CFCColumn> iterator = pamModel.iterator(); iterator.hasNext();) {
							CFCColumn pamColumn = (CFCColumn) iterator.next();
							pamColumn.setToCFC(pam.getDescriptor());
							if(pamColumn.getAccBank() != null) {
								Long newAccBankId = accountsMap.get(pamColumn.getAccBank().getDescriptor().getID());
								if(newAccBankId != null) {
									AEDescriptive newAccBank = AccAccount.lazyDescriptor(newAccBankId);
									pamColumn.setAccBank(newAccBank);
								} else {
									pamColumn.setAccBank(null);
								}
							}
							if(!AEValue.isNull(pamColumn.getValue()) && CFCColumn.NType.ACCOUNT.equals(pamColumn.getNType())) {
								Long newAccGOAId = accountsGOAMap.get(pamColumn.getValue().optLong());
								AEValue nv = new AEValue();
								if(newAccGOAId != null) {
									nv.set(Long.toString(newAccGOAId));
								}
								pamColumn.setValue(nv);
							} else if(!AEValue.isNull(pamColumn.getValue()) && CFCColumn.NType.ATTRIBUTE.equals(pamColumn.getNType())) {
								Long newAttrId = attributesGOAMap.get(pamColumn.getValue().optLong());
								AEValue nv = new AEValue();
								if(newAttrId != null) {
									nv.set(Long.toString(newAttrId));
								}
								pamColumn.setValue(nv);
							}
							if(pamColumn.getAccAccount() != null) {
								Long newAccAccountId = accountsMap.get(pamColumn.getAccAccount().getDescriptor().getID());
								if(newAccAccountId != null) {
									AEDescriptive newAccAccount = AccAccount.lazyDescriptor(newAccAccountId);
									pamColumn.setAccAccount(newAccAccount);
								} else {
									pamColumn.setAccAccount(null);
								}
							} 
							pamDAO.insertPAMColumn(pamColumn);
						}
					}
				}

				/**
				 * Social
				 */
				SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);
				SocialInfo socialInfo = socialDAO.loadSocialInfo(Organization.lazyDescriptor(ownerId));
				if(socialInfo != null) {
					socialInfo.resetAsNew();
					socialInfo.setCompany(customerDescr);

					socialDAO.insert(socialInfo);
				}

				/**
				 * Jcr
				 */
				invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
				jcrLocal.copyStructure(ownerId, customerDescr.getID(), invContext);
			}

			// commit transaction
			localConnection.commit();

			// create and return response
			JSONObject payload = new JSONObject();
			payload.put("customer", customer);
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			if(localConnection != null) {
				localConnection.rollback();
			}
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private void insert(AEDocument aeDocument, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConn = null;
		try {
			// get connection wrapper to execute this method
			localConn = daoFactory.getConnection(aeConnection);
			localConn.beginTransaction();

			// prepare concrete document type
			if(aeDocument instanceof ContractDeTravail) {
				prepareInsert((ContractDeTravail) aeDocument, invContext, localConn);
			} else {
				prepareInsert(aeDocument, invContext, aeConnection);
			}

			// insert
			AEDocumentDAO docDAO = daoFactory.getDocumentDAO(aeDocument.getType(), localConn);
			docDAO.insert(aeDocument);

			// commit
			localConn.commit();
		} catch (Throwable t) {
			t.printStackTrace();
			localConn.rollback();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConn);
		}
	}

	private void processItems(AEDocument aeDocument, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();

			// save in transaction
			AEDocumentItemDAO docItemDAO = daoFactory.getAEDocumentItemDAO(localConnection);
			AEDocumentItemsList items = aeDocument.getItems();
			for (Iterator<AEDocumentItem> iterator = items.iterator(); iterator.hasNext();) {
				AEDocumentItem aeDocumentItem = (AEDocumentItem) iterator.next();
				aeDocumentItem.setDocument(aeDocument.getDescriptor());
				switch(aeDocumentItem.getPersistentState()) {
				case NEW:
					docItemDAO.insert(aeDocumentItem);
					break;
				case UPDATED:
					docItemDAO.update(aeDocumentItem);
					break;
				case DELETED:
					docItemDAO.delete(aeDocumentItem.getID());
					iterator.remove();
					break;
				default:
					// internal error
					assert(false);
					break;
				}
			}
			localConnection.commit();
		} catch (AEException e) {
			localConnection.rollback();
			e.printStackTrace();
			throw e;
		} catch (Throwable t) {
			localConnection.rollback();
			t.printStackTrace();
			throw new AEException((int) AEError.System.NA.getSystemID(), t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/**
	 * private where no need for different  
	 */
	private void prepareInsert(AEDocument aeDocument, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		/**
		 * validate insert
		 */


		/**
		 * prepare
		 */
		Date dateNow = new Date();
		aeDocument.setCreator(invContext.getAuthPrincipal().getName());
		aeDocument.setTimeCreated(dateNow);
		aeDocument.setModifier(invContext.getAuthPrincipal().getName());
		aeDocument.setTimeModified(dateNow);
		if(aeDocument.getDate() == null) {
			aeDocument.setDate(dateNow);
		}
		if(aeDocument.getCompany() == null) {
			aeDocument.setCompany(invContext.getAuthPrincipal().getCompany());
		}

		aeDocument.setRegDate(dateNow);
	}

	@Override
	public AEResponse importSuppliers(File fileSuppliers, AEInvocationContext invContext) throws AEException {
		FileInputStream fis = null; 
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	

			/**
			 * prepare dao's
			 */

			/**
			 * local attributes
			 */
			// prepare source in stream
			fis = new FileInputStream(fileSuppliers); 
			BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "Windows-1252"), 2048);
			JSONArray jsonArray = new JSONArray();
			String line = null;
			while((line = reader.readLine()) != null) {
				line = AEStringUtil.trim(line);
				JSONObject json = createSupplier(line);
				if(json != null) {
					jsonArray.put(json);
				}
			}

			// create and rerturn response
			JSONObject payload = new JSONObject();
			payload.put("items", jsonArray);
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			if(fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private JSONObject createSupplier(String line) {
		if(AEStringUtil.isEmpty(line)) {
			return null;
		}

		JSONObject json = new JSONObject();

		LightStringTokenizer tokenizer = new LightStringTokenizer(line, "\t");
		String token = null;

		//name	
		if(tokenizer.hasMoreTokens()) {
			token = AEStringUtil.trim(tokenizer.nextToken());
			try {
				json.put("name", token);
			} catch (JSONException e) {};
		}

		//code	
		if(tokenizer.hasMoreTokens()) {
			token = AEStringUtil.trim(tokenizer.nextToken());
			try {
				json.put("code", token);
			} catch (JSONException e) {};
		}

		//		//nature	
		//		if(tokenizer.hasMoreTokens()) {
		//			token = AEStringUtil.trim(tokenizer.nextToken());
		//			try {
		//				json.put("nature", token);
		//			} catch (JSONException e) {};
		//		}

		//address	
		if(tokenizer.hasMoreTokens()) {
			token = AEStringUtil.trim(tokenizer.nextToken());
			try {
				json.put("address", token);
			} catch (JSONException e) {};
		}

		//secondaryAddress	
		if(tokenizer.hasMoreTokens()) {
			token = AEStringUtil.trim(tokenizer.nextToken());
			try {
				json.put("secondaryAddress", token);
			} catch (JSONException e) {};
		}

		//postCode
		if(tokenizer.hasMoreTokens()) {
			token = AEStringUtil.trim(tokenizer.nextToken());
			try {
				json.put("postCode", token);
			} catch (JSONException e) {};
		}

		//town	
		if(tokenizer.hasMoreTokens()) {
			token = AEStringUtil.trim(tokenizer.nextToken());
			try {
				json.put("town", token);
			} catch (JSONException e) {};
		}

		//phone
		if(tokenizer.hasMoreTokens()) {
			token = AEStringUtil.trim(tokenizer.nextToken());
			try {
				json.put("phone", token);
			} catch (JSONException e) {};
		}

		//fax	
		if(tokenizer.hasMoreTokens()) {
			token = AEStringUtil.trim(tokenizer.nextToken());
			try {
				json.put("fax", token);
			} catch (JSONException e) {};
		}

		//email	
		if(tokenizer.hasMoreTokens()) {
			token = AEStringUtil.trim(tokenizer.nextToken());
			try {
				json.put("email", token);
			} catch (JSONException e) {};
		}

		//siren	
		if(tokenizer.hasMoreTokens()) {
			token = AEStringUtil.trim(tokenizer.nextToken());
			try {
				json.put("siren", token);
			} catch (JSONException e) {};
		}

		//nic
		if(tokenizer.hasMoreTokens()) {
			token = AEStringUtil.trim(tokenizer.nextToken());
			try {
				json.put("nic", token);
			} catch (JSONException e) {};
		}

		//tva_intracon	
		if(tokenizer.hasMoreTokens()) {
			token = AEStringUtil.trim(tokenizer.nextToken());
			try {
				json.put("tva_intracon", token);
			} catch (JSONException e) {};
		}

		//compteGeneralId
		if(tokenizer.hasMoreTokens()) {
			token = AEStringUtil.trim(tokenizer.nextToken());
			try {
				json.put("compteGeneralCode", token);
			} catch (JSONException e) {};
		}

		//compteAuxiliare
		if(tokenizer.hasMoreTokens()) {
			token = AEStringUtil.trim(tokenizer.nextToken());
			try {
				json.put("compteAuxiliare", token);
			} catch (JSONException e) {};
		}

		//payTypeId	
		if(tokenizer.hasMoreTokens()) {
			token = AEStringUtil.trim(tokenizer.nextToken());
			try {
				json.put("payTypeId", token);
			} catch (JSONException e) {};
		}

		//payDelayDuration
		if(tokenizer.hasMoreTokens()) {
			token = AEStringUtil.trim(tokenizer.nextToken());
			try {
				json.put("payDelayDuration", token);
			} catch (JSONException e) {};
		}

		//payDelayUOMId	
		if(tokenizer.hasMoreTokens()) {
			token = AEStringUtil.trim(tokenizer.nextToken());
			try {
				json.put("payDelayUOMId", token);
			} catch (JSONException e) {};
		}

		//note
		if(tokenizer.hasMoreTokens()) {
			token = AEStringUtil.trim(tokenizer.nextToken());
			try {
				json.put("note", token);
			} catch (JSONException e) {};
		}

		return json;
	}

	@Override
	public AEResponse saveCompanies(AERequest request) throws AEException {
		AEConnection localConnection = null;
		try {
			// get caler
			AEInvocationContext invContext = new AEInvocationContextImp(request.getAuthPrincipal());

			// validate caler
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// get connection and begin transaction
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			JSONObject arguments = request.getArguments();
			JSONArray jsonCompanies = arguments.getJSONArray("items");
			long ownerId = arguments.optLong("ownerId");

			// prepare accounts
			AccountDAO accDAO = DAOFactory.getInstance().getAccountDAO(localConnection);
			JSONArray accounts = accDAO.loadSubAccountsByOwner(ownerId);
			Map<String, JSONObject> map = new HashMap<String, JSONObject>();
			for (int i = 0; i < accounts.length(); i++) {
				JSONObject acc = accounts.getJSONObject(i);
				map.put(acc.getString("code"), acc);
			}

			// save companies
			localConnection.beginTransaction();
			for (int i = 0; i < jsonCompanies.length(); i++) {
				JSONObject company = jsonCompanies.getJSONObject(i);

				// get account id
				JSONObject acc = map.get(company.optString("compteGeneralCode"));
				if(acc != null) {
					company.put("compteGeneralId", acc.optLong("id"));
				}

				// get arguments
				Contact contact = new Contact(Contact.Type.BUSINESS);
				Address address = new Address(Address.Type.BUSINESS);

				// process organization
				OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);
				int dbState = -1;
				if(company.has("dbState")) {
					dbState = company.getInt("dbState");
					switch(dbState) {
					case AEPersistentUtil.DB_ACTION_NONE: {
						contact.setView();
						address.setView();
						break;
					}
					case AEPersistentUtil.DB_ACTION_INSERT: {
						orgDAO.insertOrganization(company);
						break;
					}
					case AEPersistentUtil.DB_ACTION_UPDATE: {
						contact.setView();
						contact.setUpdated();
						address.setView();
						address.setUpdated();
						orgDAO.update(company);
						break;
					}
					case AEPersistentUtil.DB_ACTION_DELETE: {
						orgDAO.delete(company);
						break;
					}
					}
				}

				// should be after customer processing
				AEDescriptor customerDescr = new AEDescriptorImp(company.getLong("id"), DomainClass.ORGANIZATION);

				// process contacts
				contact.create(company);
				contactService.manage(new ContactsList(contact) , customerDescr, localConnection);
				company.put(Contact.key_contactId, contact.getID());

				// process address
				address.create(company);
				addressService.manage(new AddressesList(address) , customerDescr, localConnection);
				company.put(Address.key_addressId, address.getID());
			}

			// commit transaction
			localConnection.commit();

			// create and return response
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			if(localConnection != null) {
				localConnection.rollback();
			}
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	//	private AccPeriod getFirstOpenPeriod(long ownerId, long moduleId, AEConnection aeConnection) throws AEException {
	//		AEConnection localConnection = null;
	//		try {
	//			AccPeriod accPeriod = null;
	//			
	//			DAOFactory daoFactory = DAOFactory.getInstance();
	//			localConnection = daoFactory.getConnection(aeConnection);
	//			
	//			// detect the first open period
	//			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
	//			accPeriod = accPeriodDAO.getFirstOpenPeriod(ownerId, moduleId);
	//			if(accPeriod == null) {
	//				// there is no open period, so we need to create one
	//				Date startDate = null;
	//				accPeriod = accPeriodDAO.getLastClosedPeriod(ownerId, moduleId);
	//				if(accPeriod == null) {	
	//					// there is no closed period, so start from absolute start date
	//					startDate = getAbsoluteStartDate(ownerId, localConnection);
	//				} else {
	//					// there is closed period, start the period from next day
	//					startDate = AEDateUtil.addDaysToDate(accPeriod.getEndDate(), 1);
	//				}
	//				
	//				accPeriod = new AccPeriod();
	//				accPeriod.setCompany(Organization.lazyDescriptor(ownerId));
	//				accPeriod.setModuleId(moduleId);
	//				accPeriod.setStartDate(startDate);
	//				accPeriod.setEndDate(getPeriodEndDate(accPeriod.getStartDate()));
	//				accPeriod.setClosed(false);
	//				accPeriodDAO.insert(accPeriod);
	//			}
	//			
	//			assert(accPeriod.isPersistent());
	//			return accPeriod;
	//		} catch (Throwable t) {
	//			throw new AEException(t);
	//		} finally {
	//			AEConnection.close(localConnection);
	//		}
	//	}
	//	
	//	private Date getAbsoluteStartDate(long ownerId, AEConnection aeConnection) throws AEException {
	//		AEConnection localConnection = null;
	//		try {
	//			DAOFactory daoFactory = DAOFactory.getInstance();
	//			localConnection = daoFactory.getConnection(aeConnection);
	//			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(aeConnection);
	//			Date startDate = orgDAO.getStartDate(ownerId);
	//			if(startDate == null) {
	//				// there is no start date
	//				// so start from the first date of current month
	//				startDate = AEDateUtil.dayOfMonth(AEDateUtil.getClearDate(new Date()), -1, 1);
	//			}
	//			return startDate;
	//		} catch (Throwable t) {
	//			throw new AEException(t);
	//		} finally {
	//			AEConnection.close(localConnection);
	//		}
	//	}
	//	
	//	/**
	//	 * The period is one month.
	//	 * 
	//	 * @param startDate
	//	 * @return
	//	 * @throws AEException
	//	 */
	//	private Date getPeriodEndDate(Date startDate) throws AEException {
	//		Date endDate = AEDateUtil.getLastDate(
	//				AEDateUtil.getMonthInYear(startDate) - 1, AEDateUtil.getYear(startDate));
	//		return endDate;
	//	}

	@Override
	public void deleteEmployee(long id, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			localConnection = daoFactory.getConnection(aeConnection);

			// get Person
			EmployeeDAO emplDAO = daoFactory.getEmployeeDAO(localConnection);
			Long personId = emplDAO.loadPersonId(id);

			localConnection.beginTransaction();

			// do the real work here
			AEDescriptor emplDescriptor = Employee.lazyDescriptor(id);

			// delete employee first!!!
			emplDAO.delete(id);

			// delete addresses
			AddressDAO adrDAO = daoFactory.getAddressDAO(localConnection);
			adrDAO.delete(emplDescriptor);

			// delete contacts
			ContactDAO contactDAO = daoFactory.getContactDAO(localConnection);
			contactDAO.delete(emplDescriptor);

			localConnection.commit();	

			// try to delete person
			// out of transaction !!!
			if(personId != null) {
				// try to delete person
				try {
					deletePerson(personId, invContext, localConnection);
				} catch(Exception e) {

				}
			}

		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally { 
			AEConnection.close(localConnection);
		}
	}

	@Override
	public void deletePerson(long id, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();

			// do the real work here
			AEDescriptor personDescriptor = Person.lazyDescriptor(id);

			// delete person first!!!
			PersonDAO personDAO = daoFactory.getPersonDAO(localConnection);
			personDAO.delete(personDescriptor);

			// delete addresses
			AddressDAO adrDAO = daoFactory.getAddressDAO(localConnection);
			adrDAO.delete(personDescriptor);

			// delete contacts
			ContactDAO contactDAO = daoFactory.getContactDAO(localConnection);
			contactDAO.delete(personDescriptor);

			localConnection.commit();	
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally { 
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AETimePeriod getFiancialPeriod(AEDescriptor compDescr, Date endDate, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			OrganizationDAO companyDAO = daoFactory.getOrganizationDAO(localConnection);

			// load configured financial period
			AETimePeriod confFinancialPeriod = companyDAO.loadFinancialPeriod(compDescr);

			// calculate current financial period
			Date confFinYearStartDate = confFinancialPeriod.getStartDate();
			if(confFinYearStartDate == null) {
				confFinYearStartDate = AEDateUtil.beginOfTheYear(endDate);
			}

			// duration is in months
			long finYearDuration = confFinancialPeriod.getDuration();
			if(finYearDuration <= 0) {
				finYearDuration = 12;
			}

			// the first financial year can be up to 18 months
			// the second, third etc. are 12 months

			// detect the first financial year
			Date startDate = confFinYearStartDate;
			Date secondFinancialYearStartDate = AEDateUtil.addMonthsToDate(confFinYearStartDate, (int) finYearDuration);
			if(endDate.after(secondFinancialYearStartDate) || endDate.equals(secondFinancialYearStartDate)) {
				// after first financial year
				finYearDuration = 12;
				long wholePeriods = 
						AEDateUtil.getMonthsBetween(secondFinancialYearStartDate, endDate) / finYearDuration;
				startDate = AEDateUtil.addMonthsToDate(secondFinancialYearStartDate, (int) (wholePeriods * finYearDuration));
			}

			// create current financial period
			AETimePeriod finacialPeriod = new AETimePeriod();
			finacialPeriod.setStartDate(startDate);
			finacialPeriod.setEndDate(endDate);
			finacialPeriod.setDuration(finYearDuration);
			finacialPeriod.setUnit(Calendar.MONTH);

			return finacialPeriod;
		} catch (Exception e) {
			throw new AEException(e);
		} finally { 
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadOrganizationTemplates(AERequest request, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// authorize
			authorize(new AuthPermission("System/Party/OrganizationTemplate", AuthPermission.READ), invContext);

			// get connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			// load organization templates
			OrganizationTemplateDAO orgTemplateDAO = daoFactory.getOrganizationTemplateDAO(localConnection);
			OrganizationTemplatesList orgTemplatesSet = orgTemplateDAO.load();

			// create and return response
			JSONObject payload = new JSONObject()
			.put("organizationTemplates", orgTemplatesSet.toJSONArray());
			AEResponse aeResponse = new AEResponse(payload);
			return aeResponse;
		} catch (Throwable t) {
			logger.errorv("{0} in {1}#{2}: {3}", t.getClass().getSimpleName(), this.getClass().getSimpleName(), "loadOrganizationTemplates", t.getMessage());
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}	
	}

	/**
	 * Arguments example:
	 *   loadCustomerArguments.put(Organization.JSONKey.customerId, 27253L);
	 *   loadCustomerArguments.put(Organization.JSONKey.doNotLoadContacts, true);
	 *   loadCustomerArguments.put(Organization.JSONKey.doNotLoadAddresses, true);
	 *	 loadCustomerArguments.put(Organization.JSONKey.doNotLoadCAO, true);
	 *	 loadCustomerArguments.put(Organization.JSONKey.doNotLoadSocialInfo, true);
	 */
	@Override
	public AEResponse loadCustomer(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			long customerId = arguments.getLong(Organization.JSONKey.customerId);
			AEDescriptor customerDescr = Organization.lazyDescriptor(arguments.getLong(Organization.JSONKey.customerId));

			// authorize whether specified principal can operate with specified customer
			ap.ownershipValidator(customerId);
			
			// authorize 
			authorize(new AuthPermission("System/Party/Customer", AuthPermission.READ), invContext, Organization.lazyDescriptor(customerId));

			// get connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));

			// load customer
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);
			JSONObject customer = orgDAO.loadCustomerOne(customerDescr);

			// load in depth
			if(customer != null) {
				loadCustomerInDepth(customer, aeRequest, invContext, localConnection);
			}

			// create and return response
			JSONObject payload = new JSONObject();
			if(customer != null) {
				payload.put(Organization.JSONKey.customer, customer);
			} else {
				payload.put(Organization.JSONKey.customer, AEStringUtil.EMPTY_STRING);
			}
			return new AEResponse(payload);
		} catch (Throwable t) {
			logger.errorv("{0} in {1}#{2}: {3}", t.getClass().getSimpleName(), this.getClass().getSimpleName(), "loadCustomer", t.getMessage());
			throw new AEException(t); // keep source message
		} finally {
			AEConnection.close(localConnection);
		}	
	}

	@Override
	public void loadCustomerInDepth(JSONObject customer, AERequest aeRequest, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			// extract arguments
			JSONObject arguments = aeRequest.getArguments();
			AEDescriptor customerDescr = Organization.lazyDescriptor(customer.getLong(AEDomainObject.JSONKey.id.name()));
			boolean doNotLoadContacts = arguments.optBoolean(Organization.JSONKey.doNotLoadContacts);
			boolean doNotLoadAddresses = arguments.optBoolean(Organization.JSONKey.doNotLoadAddresses);
			boolean doNotLoadCAO = arguments.optBoolean(Organization.JSONKey.doNotLoadCAO);
			boolean onlyCashAccounts = arguments.optBoolean(Organization.JSONKey.onlyCashAccounts);
			boolean doNotLoadSocialInfo = arguments.optBoolean(Organization.JSONKey.doNotLoadSocialInfo);

			// dao and connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			// contacts
			if(!doNotLoadContacts) {
				ContactsList contacts = contactService.load(customerDescr, localConnection);
				if(contacts != null && !contacts.isEmpty()) {
					Contact contact = contacts.get(0);
					JSONObject contactJSON = contact.toJSONObject();
					JSONUtil.apply(customer, contactJSON);
				}
			}

			// load address
			if(!doNotLoadAddresses) {
				AddressesList addressesList = addressService.load(customerDescr, localConnection);
				if(addressesList != null && !addressesList.isEmpty()) {
					Address address = addressesList.get(0);
					JSONObject addressJSON = address.toJSONObject();
					JSONUtil.apply(customer, addressJSON);
				}
			}

			// coa
			if(!doNotLoadCAO) {
				JSONObject coa = null;
				if(onlyCashAccounts) {
					JSONArray accounts = accLocal.loadCashAccountsByOwner(customer.getLong(AEDomainObject.JSONKey.id.name()), localConnection);
					coa = new JSONObject();
					coa.put("accounts", accounts);
				} else {
					coa = accLocal.loadCOA(customer.getLong("id"), localConnection);

				}
				customer.put("chartOfAccounts", coa);
			}

			// social info
			if(!doNotLoadSocialInfo) {
				SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);
				SocialInfo socialInfo = socialDAO.loadSocialInfo(Organization.lazyDescriptor(customer.getLong(AEDomainObject.JSONKey.id.name())));
				if(socialInfo != null) {
					customer.put(SocialInfo.JSONKey.socialInfo.toString(), socialInfo.toJSONObject());
				}
			}
		} catch (Throwable t) {
			logger.errorv("{0} in {1}#{2}: {3}", t.getClass().getSimpleName(), this.getClass().getSimpleName(), "loadCustomerInDepth", t.getMessage());
			throw new AEException(t); // keep source message
		} finally {
			AEConnection.close(localConnection);
		}

	}
}
