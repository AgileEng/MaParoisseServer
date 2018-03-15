/**
 * 
 */
package eu.agileeng.services.social.ejb;

import static eu.agileeng.accbureau.AEApp.logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONObject;
import org.jboss.logging.Logger;

import eu.agileeng.accbureau.AEApp;
import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.AEExceptionsList;
import eu.agileeng.domain.AEObject;
import eu.agileeng.domain.AEWarning;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.acc.AccPeriod;
import eu.agileeng.domain.business.EmploymentClassification;
import eu.agileeng.domain.contact.Address;
import eu.agileeng.domain.contact.AddressesList;
import eu.agileeng.domain.contact.Contact;
import eu.agileeng.domain.contact.ContactsList;
import eu.agileeng.domain.contact.Employee;
import eu.agileeng.domain.contact.EmployeeList;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.contact.Person;
import eu.agileeng.domain.document.AEDocument;
import eu.agileeng.domain.document.AEDocumentDescriptor;
import eu.agileeng.domain.document.AEDocumentFactory;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.AEDocumentsList;
import eu.agileeng.domain.document.AEPrintTemplatesList;
import eu.agileeng.domain.document.imp.AEDocumentDescriptorImp;
import eu.agileeng.domain.document.social.AESocialDocument;
import eu.agileeng.domain.document.social.AESocialDocumentFilter;
import eu.agileeng.domain.document.social.HasPlanPredicate;
import eu.agileeng.domain.document.social.SocialPeriodType;
import eu.agileeng.domain.document.social.SocialTemplatesSet;
import eu.agileeng.domain.document.social.SocialTimeSheet;
import eu.agileeng.domain.document.social.SocialTimeSheetEntry;
import eu.agileeng.domain.document.social.SocialTimeSheetEntryPeriodsValidator;
import eu.agileeng.domain.document.social.SocialTimeSheetTemplateKey;
import eu.agileeng.domain.document.social.TimeSheetPrintUtils;
import eu.agileeng.domain.document.social.accidentdutravail.AccidentDuTravail;
import eu.agileeng.domain.document.social.contractdetravail.ContractDeTravail;
import eu.agileeng.domain.document.social.contractdetravail.ContractDeTravailType;
import eu.agileeng.domain.document.social.findutravail.FinDuTravail;
import eu.agileeng.domain.document.social.rib.Rib;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.domain.social.SalaryGrid;
import eu.agileeng.domain.social.SalaryGridItem;
import eu.agileeng.domain.social.SalaryGridItemValidator;
import eu.agileeng.domain.social.SalaryGridItemsList;
import eu.agileeng.domain.social.SalaryGridValidator;
import eu.agileeng.domain.social.SalaryGridsList;
import eu.agileeng.domain.social.SocialInfo;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistent;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.acc.AccPeriodDAO;
import eu.agileeng.persistent.dao.document.AEDocumentDAO;
import eu.agileeng.persistent.dao.document.contractdeTravail.CotractDeTravailDAO;
import eu.agileeng.persistent.dao.document.contractdeTravail.MBContratDeTravailDAO;
import eu.agileeng.persistent.dao.document.ft.FinDuTravailDAO;
import eu.agileeng.persistent.dao.oracle.EmployeeDAO;
import eu.agileeng.persistent.dao.oracle.PartyDAO;
import eu.agileeng.persistent.dao.oracle.PersonDAO;
import eu.agileeng.persistent.dao.social.SocialDAO;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.security.AuthRole;
import eu.agileeng.services.AEBean;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AEInvocationContextValidator;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.address.ejb.AddressLocal;
import eu.agileeng.services.contact.ejb.ContactLocal;
import eu.agileeng.services.party.ejb.PartyLocal;
import eu.agileeng.util.AEDateIterator;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEObjectUtil;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AETimePeriod;
import eu.agileeng.util.FrenchIbanValidator;
import eu.agileeng.util.InseeValidator;
import eu.agileeng.util.RibValidator;
import eu.agileeng.util.StringAlign;
import eu.agileeng.util.json.JSONUtil;
import eu.agileeng.util.mail.Emailer;

/**
 * @author vvatov
 *
 */
public class AESocialBean extends AEBean implements AESocialLocal, AESocialRemote {

	private static Logger logger = Logger.getLogger(AESocialBean.class);
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6857474934795023223L;

	//	@EJB private AccLocal accLocal;
	@EJB private AddressLocal addressService;
	@EJB private ContactLocal contactService;
	@EJB private PartyLocal partyService;

	/* (non-Javadoc)
	 * @see eu.agileeng.services.Social.AESocialService#save(eu.agileeng.services.AERequest, eu.agileeng.services.AEInvocationContext)
	 */
	@Override
	public AEResponse save(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			JSONObject jsonDocument = arguments.getJSONObject("document");
			boolean unconditionalSave = arguments.optBoolean("unconditionalSave");

			/**
			 * Detect document type
			 */
			int docType = jsonDocument.optInt("docType");
			AEDocumentType aeDocType = AEDocumentType.valueOf(docType);
			if(aeDocType == null || AEDocumentType.valueOf(AEDocumentType.System.NA).equals(aeDocType)) {
				throw new AEException("System Error: Unknnown document type!");
			}

			/**
			 * Factories
			 */
			AEDocumentFactory docFactory = AEDocumentFactory.getInstance(aeDocType);
			DAOFactory daoFactory = DAOFactory.getInstance();

			/**
			 * Create document
			 */
			AESocialDocument socialDocument = 
					(AESocialDocument) docFactory.createDocument(arguments.getJSONObject("document"));

			/**
			 * PreSave
			 */

			/**
			 * doc save validation
			 */
			socialDocument.validateWith(docFactory.getSaveValidator());

			// get connection and begin transaction
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			localConnection.beginTransaction();
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			// save in transaction
			String action = null;
			switch(socialDocument.getPersistentState()) {
			case NEW:
				insert(socialDocument, unconditionalSave, invContext);
				action = "Nouveau";
				break;
			case UPDATED:
				update(socialDocument, unconditionalSave, invContext);
				action = "Sauver";
				break;
			case DELETED:
				//				delete(aeDocument, invContext);
				break;
			default:
				// internal error
				assert(false);
				break;
			}


			JSONObject savedJSONDocument = socialDocument.toJSONObject();

			/**
			 * AfterSave
			 */
			if(socialDocument.getCompany().getDescriptor().getID() != socialDocument.getEmployee().getCompany().getDescriptor().getID()) {
				throw new AEException("System error: The document's owner is different than employee's owner");
			}

			/**
			 * Commit and return response
			 */
			localConnection.commit();

			/**
			 * Send EMail after commit
			 */
			if(!(socialDocument instanceof ContractDeTravail)) {
				JSONObject sendEMailArguments = new JSONObject();
				sendEMailArguments.put("document", savedJSONDocument);
				sendEMailArguments.put("action", action);
				AERequest sendEMailRequest = new AERequest(sendEMailArguments);
				sendEMailRequest.setAuthPrincipal(invContext.getAuthPrincipal());
				invContext.setProperty(AEInvocationContext.AEConnection, null);

				sendEMail(sendEMailRequest, invContext);
			}

			JSONObject payload = new JSONObject();
			payload.put("document", savedJSONDocument);
			return new AEResponse(payload);
		} catch (AEWarning w) {
			AEConnection.rollback(localConnection);
			try {
				JSONObject payload = new JSONObject();
				payload.put("warning", w.getMessage());
				return new AEResponse(payload);
			} catch (Exception e) {
				throw new AEException(e);
			}
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.services.Social.AESocialService#load(eu.agileeng.services.AERequest, eu.agileeng.services.AEInvocationContext)
	 */
	@Override
	public AEResponse load(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			JSONObject jsonDocument = arguments.getJSONObject("document");			
			int docType = jsonDocument.getInt("docType");
			long docId = jsonDocument.getLong("id");

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();

			// get connection
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			// load document
			AEDocument doc = load(docType, docId, localConnection);

			// load customer
			JSONObject customer = null;
			if(arguments.optBoolean("loadCustomer")) {
				// request's arguments
				JSONObject customerArguments = new JSONObject();
				customerArguments.put("customerId", doc.getCompany().getDescriptor().getID());
				customerArguments.put("doNotLoadCAO", true);

				// request
				AERequest customerRequest = new AERequest(customerArguments);

				// load customer
				AEResponse customerResponse = partyService.loadCustomers(customerRequest, invContext);
				if(customerResponse.getPayload() != null) {
					JSONObject payload = customerResponse.getPayload();
					JSONArray customersArray = payload.optJSONArray("customers");
					if(customersArray.length() > 0) {
						customer = customersArray.getJSONObject(0);
					}
				}
			}

			// load social info
			SocialInfo socialInfo = null;
			if(arguments.optBoolean("loadSocialInfo")) {
				SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);
				socialInfo = socialDAO.loadSocialInfo(Organization.lazyDescriptor(doc.getCompany().getDescriptor().getID()));
			}

			SalaryGrid salaryGrid = null;
			if(socialInfo != null && socialInfo.getSalaryGrid() != null && arguments.optBoolean("loadSalaryGrid")) {
				SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);
				salaryGrid = socialDAO.loadSalaryGrid(socialInfo.getSalaryGrid().getDescriptor());
			}

			// return response
			JSONObject payload = new JSONObject();
			payload.put("document", doc.toJSONObject());
			if(customer != null) {
				payload.put("customer", customer);
			}
			if(socialInfo != null) {
				payload.put("socialInfo", socialInfo.toJSONObject());
			}
			if(salaryGrid != null) {
				payload.put("salaryGrid", salaryGrid.toJSONObject());
			}
			return new AEResponse(payload);
		} catch (Throwable t) {
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
			invContext.setProperty(AEInvocationContext.AEConnection, null);
		}
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.services.Social.AESocialService#loadByFilter(eu.agileeng.services.AERequest, eu.agileeng.services.AEInvocationContext)
	 */
	@Override
	public AEResponse loadByFilter(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			JSONObject jsonFilter = arguments.getJSONObject("filter");
			AESocialDocumentFilter filter = new AESocialDocumentFilter();
			filter.create(jsonFilter);

			// get connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			// load results
			AEDocumentDAO docDAO = daoFactory.getDocumentDAO(filter.getDocType(), localConnection);
			AEDocumentsList docsList = docDAO.loadContratDeTravail(filter);

			// all
			long countAll = docDAO.countAll(filter.getOwner().getDescriptor(), filter.getDocType());

			// not validated
			long countValidated = docDAO.countValidated(filter.getOwner().getDescriptor(), filter.getDocType());

			// return response
			JSONObject payload = new JSONObject();
			payload.put("documents", docsList.toJSONArray());
			payload.put("countAll", countAll);
			payload.put("countValidated", countValidated);
			return new AEResponse(payload);
		} catch (Throwable t) {
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.services.Social.AESocialService#delete(eu.agileeng.services.AERequest, eu.agileeng.services.AEInvocationContext)
	 */
	@Override
	public AEResponse delete(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			AEDescriptor ownerDescr = Organization.lazyDescriptor(arguments.optLong("sOwnerId"));
			JSONArray jsonDocumentsArray = arguments.getJSONArray("documents");
			boolean deleteEmployee = arguments.optBoolean("deleteEmployee");

			/**
			 * Delete documents
			 */
			JSONArray jsonDeleted = new JSONArray();
			if(jsonDocumentsArray != null && jsonDocumentsArray.length() > 0) {
				for (int i = 0; i < jsonDocumentsArray.length(); i++) {
					JSONObject jsonDocument = jsonDocumentsArray.getJSONObject(i);

					// create doc descriptor
					AEDocumentDescriptor aeDocDescr = 
							new AEDocumentDescriptorImp(
									jsonDocument.optLong("docId"), 
									AEDocumentType.valueOf(jsonDocument.optInt("docType")));

					try {
						delete(aeDocDescr, ownerDescr, deleteEmployee, invContext, localConnection);
						jsonDeleted.put(aeDocDescr.toJSONObject());
					} catch (Exception e) {

					}
				}
			}

			JSONObject payload = new JSONObject();
			payload.put("documents", jsonDeleted);
			return new AEResponse(payload);
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.services.Social.AESocialService#delete(eu.agileeng.services.AERequest, eu.agileeng.services.AEInvocationContext)
	 */
	@Override
	public void delete(AEDocumentDescriptor docDescr, AEDescriptor ownerDescr, boolean deleteEmployee, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			if(docDescr == null) {
				throw new AEException("System Error: docDescr cannot be null");
			}

			if(ownerDescr == null) {
				throw new AEException("System Error: ownerDescr cannot be null");
			}

			AEDocumentType aeDocType = docDescr.getDocumentType();
			if(aeDocType == null || AEDocumentType.valueOf(AEDocumentType.System.NA).equals(aeDocType)) {
				throw new AEException("System Error: Unknnown document type!");
			}
			long docId = docDescr.getID();


			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();

			// get connection and begin transaction
			localConnection = daoFactory.getConnection(aeConnection);

			/**
			 * doc delete validation
			 */
			// delete social document role based 
			if(!(ap.hasAdministratorRights() 
					|| ap.hasPowerUserRights() 
					|| ap.isMemberOf(AuthRole.System.social))) {

				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}

			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerDescr.getID());

			// whether this document is ownered by this customer
			AEDocumentDAO docDAO = daoFactory.getDocumentDAO(aeDocType, localConnection);
			docDAO.ownershipValidation(docId, ownerDescr.getID());

			// should not be locked
			if(docDAO.isLocked(docId)) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}

			// get Employee
			Long employeeId = null;
			if(aeDocType.isOfType(AEDocumentType.System.ContractDeTravail)) {
				CotractDeTravailDAO concreteDAO = (CotractDeTravailDAO) docDAO;
				employeeId = concreteDAO.loadEmployeeId(docId);
			} else if(aeDocType.isOfType(AEDocumentType.System.FinDuTravail)) {
				FinDuTravailDAO concreteDAO = (FinDuTravailDAO) docDAO;
				employeeId = concreteDAO.loadEmployeeId(docId);
			} 

			/**
			 * begin transaction and delete
			 */
			localConnection.beginTransaction();

			// delete document (social document will be auto deleted)
			docDAO.delete(docId);

			// deal with employee
			boolean updateEmployee = true;
			if(deleteEmployee && employeeId != null) {
				try {
					partyService.deleteEmployee(employeeId, invContext, localConnection);
					updateEmployee = false;
				} catch (Exception e) {}
			}
			if(updateEmployee && employeeId != null) {
				EmployeeDAO emplDAO = daoFactory.getEmployeeDAO(localConnection);
				if(docDAO instanceof CotractDeTravailDAO) {
					emplDAO.nullifyEntryDate(employeeId);
				} else if(docDAO instanceof FinDuTravailDAO) {
					emplDAO.nullifyReleaseDate(employeeId);
				}
			}

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

	/**
	 * Private where no need for different  
	 */
	private void insert(AESocialDocument socialDocument, boolean unconditionalSave, AEInvocationContext invContext) throws AEException, AEWarning {
		AEConnection localConnection = null;
		try {
			// get connection wrapper to execute this method
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			localConnection.beginTransaction();

			// innsert concrete document type
			if(socialDocument instanceof ContractDeTravail) {
				prepareInsert((ContractDeTravail) socialDocument, unconditionalSave, invContext, localConnection);
				insert((ContractDeTravail) socialDocument, invContext, localConnection);
			} else if(socialDocument instanceof AccidentDuTravail) {
				prepareInsert((AccidentDuTravail) socialDocument, invContext, localConnection);
				insert((AccidentDuTravail) socialDocument, invContext, localConnection);
			} else if(socialDocument instanceof FinDuTravail) {
				prepareInsert((FinDuTravail) socialDocument, invContext, localConnection);
				insert((FinDuTravail) socialDocument, invContext, localConnection);
			} else if(socialDocument instanceof Rib) {
				prepareInsert((Rib) socialDocument, invContext, localConnection);
				insert((Rib) socialDocument, invContext, localConnection);
			} else {
				prepareInsert(socialDocument, invContext);
			}

			// commit
			localConnection.commit();
		} catch (AEWarning w) {
			AEConnection.rollback(localConnection);
			throw w;
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/**
	 * private where no need for different  
	 */
	private void prepareInsert(ContractDeTravail contractDeTravail, boolean unconditionalSave, AEInvocationContext invContext, AEConnection aeConnection) throws AEException, AEWarning {
		/**
		 * common preparation
		 */
		prepareInsert((AESocialDocument) contractDeTravail, invContext);

		/**
		 * concrete preparation
		 */
		Employee empl = contractDeTravail.getEmployee();
		assert(empl != null);

		// validate INSEE
		if(empl.isHasUIN() && empl.isValidateUIN()) {
			InseeValidator.getInstance().validate(empl.getUIN());
		}

		// validate conditional
		if(!unconditionalSave) {
			boolean haveWarnings = false;
			StringBuffer sb = new StringBuffer();

			try {
				validateExistingContract(contractDeTravail, invContext, aeConnection);
			} catch (AEWarning w) {
				haveWarnings = true;
				sb.append("<li>" + w.getMessage() + "</li>");
			}

			// validate CDD Delay Contract
			try {
				validateCDDDelayContract(contractDeTravail, invContext, aeConnection);
			} catch (AEWarning w) {
				haveWarnings = true;
				sb.append("<li>" + w.getMessage() + "</li>");
			}

			if(haveWarnings) {
				sb.append("<br/>Êtes-vous sûr de vouloir enregistrer?");
				throw new AEWarning(sb.toString());
			}
		}

		Person person = null;
		if(empl.isPersistent()) {
			// choosen employee, so update the person
			assert(empl.getPerson() != null);
			assert(empl.getPerson().getDescriptor().getID() > 0);
			person = empl.updatePerson();
		} else {
			// deal with person
			AEDescriptive personDescr = empl.getPerson();
			if(personDescr == null || personDescr.getDescriptor().getID() < 1) {
				person = empl.createPerson();
			} else {
				// new Employee withh old person
				empl.updatePerson();
			}
		}
		assert(person != null);

		// set the employment start date
		assert(contractDeTravail.getDateOfEntry() != null);
		empl.setContractType(contractDeTravail.getSubType());
		if(AEDocumentType.System.ContractDeTravail.getSystemID() == contractDeTravail.getType().getSystemID()) {
			empl.setDateEntry(contractDeTravail.getDateOfEntry());
		} else if(AEDocumentType.System.ContractDeTravailAnex.getSystemID() == contractDeTravail.getType().getSystemID()) {
			empl.setModificationDate(contractDeTravail.getDate());
		}
		empl.setUpdated();
	}

	/**
	 * private where no need for different  
	 */
	private void prepareInsert(AccidentDuTravail accidentDuTravail, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		/**
		 * common preparation
		 */
		prepareInsert((AESocialDocument) accidentDuTravail, invContext);

		/**
		 * concrete preparation
		 */
		Employee empl = accidentDuTravail.getEmployee();
		assert(empl != null);
		assert(empl != null);
	}

	/**
	 * private where no need for different  
	 */
	private void prepareInsert(FinDuTravail finDuTravail, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		/**
		 * common preparation
		 */
		prepareInsert((AESocialDocument) finDuTravail, invContext);

		/**
		 * concrete preparation
		 */
		Employee empl = finDuTravail.getEmployee();
		assert(empl != null);
		assert(empl != null);
		if(finDuTravail.isOfType(AEDocumentType.System.FinDuTravail)) {
			empl.setDateRelease(finDuTravail.getDateRelease());
			if(finDuTravail.getReason() != null) {
				empl.setReasonRelease(FinDuTravail.Reason.valueOf(finDuTravail.getReason().getDescriptor().getID()));
			}
		}
	}

	/**
	 * private where no need for different  
	 */
	private void prepareInsert(Rib rib, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		/**
		 * common preparation
		 */
		prepareInsert((AESocialDocument) rib, invContext);

		/**
		 * concrete preparation
		 */
		Employee empl = rib.getEmployee();
		assert(empl != null);
		assert(empl != null);

		/**
		 * Validate RIB
		 */
		RibValidator.getInstance().validate(rib.getRib());
		FrenchIbanValidator.getInstance().validate(rib.getIban());
	}

	private void insert(ContractDeTravail contractDeTravail, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			// get connection wrapper to execute this method
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();

			/**
			 * Employee
			 */
			Employee empl = contractDeTravail.getEmployee();
			partyService.manage((Person) empl.getPerson(), invContext, localConnection);
			partyService.save(empl, invContext, localConnection);

			// insert
			AEDocumentDAO docDAO = daoFactory.getDocumentDAO(contractDeTravail.getType(), localConnection);
			docDAO.insert(contractDeTravail);

			// process modification
			if(contractDeTravail.getType().getSystemID() == AEDocumentType.System.ContractDeTravailAnex.getSystemID()) {
				CotractDeTravailDAO cDAO = (CotractDeTravailDAO) docDAO;
				cDAO.update(contractDeTravail.getModifyingDoc().getDescriptor(), (AEDocumentDescriptor) contractDeTravail.getDescriptor());
			}

			// commit
			localConnection.commit();
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private void insert(AccidentDuTravail accidentDuTravail, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			// get connection wrapper to execute this method
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();

			/**
			 * Employee
			 */
			Employee empl = accidentDuTravail.getEmployee();
			partyService.save(empl, invContext, localConnection);

			// insert
			AEDocumentDAO docDAO = daoFactory.getDocumentDAO(accidentDuTravail.getType(), localConnection);
			docDAO.insert(accidentDuTravail);

			// commit
			localConnection.commit();
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private void insert(FinDuTravail finDuTravail, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			// get connection wrapper to execute this method
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();

			/**
			 * Employee
			 */
			Employee empl = finDuTravail.getEmployee();
			partyService.save(empl, invContext, localConnection);

			// insert
			AEDocumentDAO docDAO = daoFactory.getDocumentDAO(finDuTravail.getType(), localConnection);
			docDAO.insert(finDuTravail);

			// commit
			localConnection.commit();
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private void insert(Rib rib, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			// get connection wrapper to execute this method
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();

			// insert
			AEDocumentDAO docDAO = daoFactory.getDocumentDAO(rib.getType(), localConnection);
			docDAO.insert(rib);

			// commit
			localConnection.commit();
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/**
	 * Private where no need for different  
	 */
	private void prepareInsert(AESocialDocument socialDocument, AEInvocationContext invContext) throws AEException {
		/**
		 * common preparation
		 */
		prepareInsert((AEDocument) socialDocument, invContext);

		/**
		 * concrete preparation
		 */
		Employee empl = socialDocument.getEmployee();
		if(empl != null) {
			empl.setCompany(socialDocument.getCompany());
		}
	}

	/**
	 * Private where no need for different  
	 */
	private void prepareInsert(AEDocument aeDocument, AEInvocationContext invContext) throws AEException {
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
		if(aeDocument.getRegDate() == null) {
			aeDocument.setRegDate(dateNow);
		}
	}

	@Override
	public AEResponse loadEmployees(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			AEDescriptor ownerDescr = Organization.lazyDescriptor(arguments.getLong("ownerId")); 
			//			boolean oldEmployees = arguments.optBoolean("oldEmployees");
			//			boolean releasedLastMonth = arguments.optBoolean("releasedLastMonth");

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();

			/**
			 * Connection
			 */
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * Load employees
			 */
			EmployeeDAO employeeDAO = daoFactory.getEmployeeDAO(localConnection);
			EmployeeList employeeList = employeeDAO.loadToCompany(ownerDescr);
			// load all employess in all cases
			//			if(oldEmployees) {
			//				employeeList = employeeDAO.loadOldEmployees(ownerDescr);
			//			} else {
			//				if(releasedLastMonth) {
			//					Date date = AEDateUtil.oneMonthBefore(new Date());
			//					employeeList = employeeDAO.loadCurrentEmployees(ownerDescr, date);
			//				} else {
			//					employeeList = employeeDAO.loadCurrentEmployees(ownerDescr);
			//				}
			//			}

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject();
			payload.put("employees", employeeList.toJSONArray());
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/**
	 * Private where no need for different  
	 */
	private void update(AESocialDocument socialDocument, boolean unconditionalSave, AEInvocationContext invContext) throws AEException, AEWarning {
		AEConnection localConnection = null;
		try {
			// get connection wrapper to execute this method
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			localConnection.beginTransaction();

			// innsert concrete document type
			if(socialDocument instanceof ContractDeTravail) {
				prepareUpdate((ContractDeTravail) socialDocument, unconditionalSave, invContext, localConnection);
				update((ContractDeTravail) socialDocument, invContext, localConnection);
			} else if(socialDocument instanceof AccidentDuTravail) {
				prepareUpdate((AccidentDuTravail) socialDocument, invContext, localConnection);
				update((AccidentDuTravail) socialDocument, invContext, localConnection);
			}  else if(socialDocument instanceof FinDuTravail) {
				prepareUpdate((FinDuTravail) socialDocument, invContext, localConnection);
				update((FinDuTravail) socialDocument, invContext, localConnection);
			} else if(socialDocument instanceof Rib) {
				prepareUpdate((Rib) socialDocument, invContext, localConnection);
				update((Rib) socialDocument, invContext, localConnection);
			} else {
				prepareUpdate(socialDocument, invContext);
			}

			// commit
			localConnection.commit();
		} catch (AEWarning w) {
			AEConnection.rollback(localConnection);
			throw w;
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/**
	 * private where no need for different  
	 */
	private void prepareUpdate(ContractDeTravail contractDeTravail, boolean unconditionalSave, AEInvocationContext invContext, AEConnection aeConnection) throws AEException, AEWarning {
		/**
		 * common preparation
		 */
		prepareUpdate((AESocialDocument) contractDeTravail, invContext);

		/**
		 * concrete preparation
		 */
		Employee empl = contractDeTravail.getEmployee();

		// validate employee
		assert(empl != null);
		if(empl.isHasUIN()) {
			InseeValidator.getInstance().validate(empl.getUIN());
		}

		// validate conditional
		if(!unconditionalSave) {
			boolean haveWarnings = false;
			StringBuffer sb = new StringBuffer();

			try {
				validateExistingContract(contractDeTravail, invContext, aeConnection);
			} catch (AEWarning w) {
				haveWarnings = true;
				sb.append("<li>" + w.getMessage() + "</li>");
			}

			// validate CDD Delay Contract
			try {
				validateCDDDelayContract(contractDeTravail, invContext, aeConnection);
			} catch (AEWarning w) {
				haveWarnings = true;
				sb.append("<li>" + w.getMessage() + "</li>");
			}

			if(haveWarnings) {
				sb.append("<br/>Êtes-vous sûr de vouloir enregistrer?");
				throw new AEWarning(sb.toString());
			}
		}

		if(AEDocumentType.System.ContractDeTravail.getSystemID() == contractDeTravail.getType().getSystemID()) {
			empl.setDateEntry(contractDeTravail.getDateOfEntry());
			empl.setContractType(contractDeTravail.getSubType());
		} else if(AEDocumentType.System.ContractDeTravailAnex.getSystemID() == contractDeTravail.getType().getSystemID()
				&& contractDeTravail.isActual()) {

			empl.setModificationDate(contractDeTravail.getDate());
			empl.setContractType(contractDeTravail.getSubType());
		}
		empl.setUpdated();
	}

	/**
	 * private where no need for different  
	 */
	private void prepareUpdate(AccidentDuTravail accidentDuTravail, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		/**
		 * common preparation
		 */
		prepareUpdate((AESocialDocument) accidentDuTravail, invContext);

		/**
		 * concrete preparation
		 */
	}

	/**
	 * private where no need for different  
	 */
	private void prepareUpdate(FinDuTravail finDuTravail, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		/**
		 * common preparation
		 */
		prepareUpdate((AESocialDocument) finDuTravail, invContext);

		/**
		 * concrete preparation
		 */
		Employee empl = finDuTravail.getEmployee();
		assert(empl != null);
		assert(empl != null);
		if(finDuTravail.isOfType(AEDocumentType.System.FinDuTravail)) {
			empl.setDateRelease(finDuTravail.getDateRelease());
			if(finDuTravail.getReason() != null) {
				empl.setReasonRelease(FinDuTravail.Reason.valueOf(finDuTravail.getReason().getDescriptor().getID()));
			}
			empl.setUpdated();
		}
	}

	/**
	 * private where no need for different  
	 */
	private void prepareUpdate(Rib rib, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		/**
		 * common preparation
		 */
		prepareUpdate((AESocialDocument) rib, invContext);

		/**
		 * concrete preparation
		 */

		/**
		 * Validate RIB
		 */
		RibValidator.getInstance().validate(rib.getRib());
		FrenchIbanValidator.getInstance().validate(rib.getIban());
	}

	/**
	 * Private where no need for different  
	 */
	private void prepareUpdate(AESocialDocument socialDocument, AEInvocationContext invContext) throws AEException {
		/**
		 * common preparation
		 */
		prepareUpdate((AEDocument) socialDocument, invContext);

		// TODO

		/**
		 * concrete preparation
		 */
		Employee empl = socialDocument.getEmployee();
		if(empl != null && empl.getCompany() == null) {
			empl.setCompany(socialDocument.getCompany());
		}
	}

	/**
	 * Private where no need for different  
	 */
	private void prepareUpdate(AEDocument aeDocument, AEInvocationContext invContext) throws AEException {
		/**
		 * validate update
		 */


		/**
		 * prepare
		 */
		Date dateNow = new Date();
		if(AEStringUtil.isEmpty(aeDocument.getCreator())) {
			aeDocument.setCreator(invContext.getAuthPrincipal().getName());
		}
		if(aeDocument.getTimeCreated() == null) {
			aeDocument.setTimeCreated(dateNow);
		}
		aeDocument.setModifier(invContext.getAuthPrincipal().getName());
		aeDocument.setTimeModified(dateNow);
	}

	private void update(ContractDeTravail contractDeTravail, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			// get connection wrapper to execute this method
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();

			/**
			 * Employee
			 */
			Employee empl = contractDeTravail.getEmployee();
			partyService.save(empl, invContext, localConnection);

			// update
			AEDocumentDAO docDAO = daoFactory.getDocumentDAO(contractDeTravail.getType(), localConnection);
			docDAO.update(contractDeTravail);

			// commit
			localConnection.commit();
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private void update(AccidentDuTravail accidentDuTravail, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			// get connection wrapper to execute this method
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();

			/**
			 * Employee
			 */
			Employee empl = accidentDuTravail.getEmployee();
			partyService.save(empl, invContext, localConnection);

			// update
			AEDocumentDAO docDAO = daoFactory.getDocumentDAO(accidentDuTravail.getType(), localConnection);
			docDAO.update(accidentDuTravail);

			// commit
			localConnection.commit();
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private void update(FinDuTravail finDuTravail, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			// get connection wrapper to execute this method
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();

			/**
			 * Employee
			 */
			Employee empl = finDuTravail.getEmployee();
			partyService.save(empl, invContext, localConnection);

			// update
			AEDocumentDAO docDAO = daoFactory.getDocumentDAO(finDuTravail.getType(), localConnection);
			docDAO.update(finDuTravail);

			// commit
			localConnection.commit();
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private void update(Rib rib, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			// get connection wrapper to execute this method
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();

			// update
			AEDocumentDAO docDAO = daoFactory.getDocumentDAO(rib.getType(), localConnection);
			docDAO.update(rib);

			// commit
			localConnection.commit();
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse print(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// get connection wrapper to execute this method
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * Business method
			 */

			// return response
			JSONObject payload = new JSONObject();
			payload.put("document", new AccidentDuTravail().toJSONObject());
			return new AEResponse(payload);
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse saveSalaryGrid(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			JSONObject jsonSalaryGrid = arguments.getJSONObject(SalaryGrid.JSONKey.salaryGrid.toString());

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();

			/**
			 * Create Salary Grid
			 */
			SalaryGrid sg = new SalaryGrid();
			sg.create(jsonSalaryGrid);

			/**
			 * PreSave
			 */

			/**
			 * Salary Grid save validation
			 */
			sg.validateWith(SalaryGridValidator.getInstance());

			// get connection and begin transaction
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			localConnection.beginTransaction();
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * Save in transaction
			 */
			switch(sg.getPersistentState()) {
			case NEW:
				insert(sg, invContext, localConnection);
				break;
			case UPDATED:
				update(sg, invContext, localConnection);
				break;
			case DELETED:
				assert(false);
				break;
			default:
				// internal error
				assert(false);
				break;
			}

			processItems(sg, invContext, localConnection);

			/**
			 * AfterSave
			 */

			/**
			 * Commit and return response
			 */
			localConnection.commit();

			JSONObject payload = new JSONObject();
			payload.put(SalaryGrid.JSONKey.salaryGrid.toString(), sg.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/**
	 * private where no need for different  
	 */
	private void insert(SalaryGrid sg, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConn = null;
		try {
			// get connection wrapper to execute this method
			localConn = daoFactory.getConnection(aeConnection);
			localConn.beginTransaction();

			// prepare concrete document type
			// prepareInsert(aeDocument, invContext, aeConnection);

			// insert
			SocialDAO socialDAO = daoFactory.getSocialDAO(localConn);
			socialDAO.insert(sg);

			// commit
			localConn.commit();
		} catch (Throwable t) {
			localConn.rollback();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConn);
		}
	}

	private void processItems(SalaryGrid sg, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			SalaryGridItemsList items = sg.getSgItems();

			// validate
			if(items != null) {
				items.validateWith(SalaryGridItemValidator.getInstance());
			}

			// get connection and begin transaction
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();

			// save in transaction
			SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);
			for (Iterator<SalaryGridItem> iterator = items.iterator(); iterator.hasNext();) {
				SalaryGridItem sgItem = iterator.next();
				sgItem.setSalaryGrid(sg.getDescriptor());
				switch(sgItem.getPersistentState()) {
				case NEW:
					socialDAO.insert(sgItem);
					break;
				case UPDATED:
					socialDAO.update(sgItem);
					break;
				case DELETED:
					socialDAO.delete(sgItem.getID());
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
			throw e;
		} catch (Throwable t) {
			localConnection.rollback();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/**
	 * private where no need for different  
	 */
	private void update(SalaryGrid sg, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConn = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// get connection wrapper to execute this method
			localConn = daoFactory.getConnection(aeConnection);
			localConn.beginTransaction();

			// prepare
			// prepareUpdate(aeDocument, invContext, aeConnection);

			// update
			SocialDAO socialDAO = daoFactory.getSocialDAO(localConn);
			socialDAO.update(sg);

			// commit
			localConn.commit();
		} catch (Throwable t) {
			localConn.rollback();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConn);
		}
	}

	@Override
	public AEResponse loadSalaryGridsAllLazy(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * local attributes
			 */
			String accHouse = aeRequest.getArguments().getString(SalaryGrid.JSONKey.accHouse.toString());

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * prepare dao's
			 */
			SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);

			// load bank accounts
			SalaryGridsList sgList = socialDAO.loadAllSalaryGridsLazy(SalaryGrid.lazyAccHouseDescriptor(accHouse));

			// create and rerturn response
			JSONObject payload = new JSONObject();
			payload.put(SalaryGrid.JSONKey.salaryGrids.toString(), sgList.toJSONArray());
			return new AEResponse(payload);
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadSalaryGrid(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * local attributes
			 */
			Long sgId = aeRequest.getArguments().getLong("id");
			AEDescriptor sgDescr = SalaryGrid.lazyDescriptor(sgId);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * prepare dao's
			 */
			SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);

			// load salary grid
			SalaryGrid sg = socialDAO.loadSalaryGrid(sgDescr);

			// load items
			sg.setSgItems(socialDAO.loadSGItems(sgDescr));

			// create and rerturn response
			JSONObject payload = new JSONObject();
			payload.put(SalaryGrid.JSONKey.salaryGrid.toString(), sg.toJSONObject());
			return new AEResponse(payload);
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse calculateSalary(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * local attributes
			 */
			Long ownerId = aeRequest.getArguments().getLong("ownerId");
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);

			String echelon = null;
			if(aeRequest.getArguments().has(ContractDeTravail.JSONKey.echelon.toString())) {
				echelon = aeRequest.getArguments().optString(ContractDeTravail.JSONKey.echelon.toString());
			}

			Double coefficient = null;
			if(aeRequest.getArguments().has(ContractDeTravail.JSONKey.coefficient.toString())) {
				try {
					coefficient = JSONUtil.parseDoubleStrict(aeRequest.getArguments(), ContractDeTravail.JSONKey.coefficient.toString());
				} catch (AEException e) {
				}
			}

			Double addSalaryPerc = null;
			if(aeRequest.getArguments().has(ContractDeTravail.JSONKey.addSalaryPerc.toString())) {
				try {
					addSalaryPerc = JSONUtil.parseDoubleStrict(aeRequest.getArguments(), ContractDeTravail.JSONKey.addSalaryPerc.toString());
				} catch (AEException e) {
				}
			}

			boolean fullTime = true;
			if(aeRequest.getArguments().has(ContractDeTravail.JSONKey.fullTime.toString())) {
				fullTime = aeRequest.getArguments().getBoolean(ContractDeTravail.JSONKey.fullTime.toString());
			}

			Double hoursPerWeek = null;
			if(aeRequest.getArguments().has(ContractDeTravail.JSONKey.hoursPerWeek.toString())) {
				hoursPerWeek = JSONUtil.parseDoubleStrict(aeRequest.getArguments(), ContractDeTravail.JSONKey.hoursPerWeek.toString());
			}

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * prepare dao's
			 */
			SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);

			/**
			 * load social info
			 */
			SocialInfo socialInfo = socialDAO.loadSocialInfo(ownerDescr);
			if(socialInfo == null) {
				throw AEError.System.SOCIAL_SALARY_GRID_NOT_DEFINED.toException();
			}

			// salary grid descriotion
			if(socialInfo.getSalaryGrid() == null) {
				throw AEError.System.SOCIAL_SALARY_GRID_NOT_DEFINED.toException();
			}
			AEDescriptor salaryGridDescr = socialInfo.getSalaryGrid().getDescriptor();

			// load salary grid item
			SalaryGridItem sgItem = socialDAO.findSgItem(salaryGridDescr.getID(), echelon, coefficient);
			if(sgItem == null) {
				throw AEError.System.SOCIAL_SALARY_GRID_ITEM_NOT_UNIQ.toException();
			}

			// get salary
			Double grossAmount = sgItem.getSalary();

			// apply addSalaryPerc
			if(addSalaryPerc != null) {
				grossAmount += (addSalaryPerc/100 * grossAmount);
			}

			// partial time
			if(!fullTime) {
				if(hoursPerWeek == null) {
					throw AEError.System.SOCIAL_SALARY_HOURS_PER_WEEK_MISSING.toException();
				}
				grossAmount = AEMath.round(grossAmount*hoursPerWeek/socialInfo.getFullTimeWeeklyHours(), 2);
			}

			// create and rerturn response
			JSONObject payload = new JSONObject();
			if(grossAmount != null) {
				payload.put("grossAmount", AEMath.toAmountString(grossAmount));
			}
			return new AEResponse(payload);
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadSocialInfo(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * local attributes
			 */
			Long ownerId = aeRequest.getArguments().getLong("ownerId");
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * prepare dao's
			 */
			SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);

			// load social info
			SocialInfo socInfo = socialDAO.loadSocialInfo(ownerDescr);

			// create and rerturn response
			JSONObject payload = new JSONObject();
			if(socInfo != null) {
				payload.put(SocialInfo.JSONKey.socialInfo.toString(), socInfo.toJSONObject());
			}
			return new AEResponse(payload);
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadActualAttestation(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			long employeeId = arguments.optLong("employeeId");

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();

			// get connection
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			// load results
			CotractDeTravailDAO docDAO = (CotractDeTravailDAO) daoFactory.getDocumentDAO(
					AEDocumentType.valueOf(AEDocumentType.System.ContractDeTravail), localConnection);
			AEDocument doc = docDAO.loadActualAttestation(employeeId);

			// load in depth
			if(doc instanceof AESocialDocument) {
				loadInDepth((AESocialDocument) doc, localConnection);
			} else {
				throw AEError.System.SOCIAL_THE_ACTUAL_NOT_FOUND.toException();
			}

			// return response
			JSONObject payload = new JSONObject();
			payload.put("document", doc.toJSONObject());
			return new AEResponse(payload);
		} catch (Throwable t) {
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	};

	private void loadInDepth(AESocialDocument socialDoc, AEConnection localConnection) throws AEException {
		Employee employee = socialDoc.getEmployee();
		if(employee != null) {
			// load contact
			ContactsList contacts = contactService.load(employee.getDescriptor(), localConnection);
			if(contacts != null && !contacts.isEmpty()) {
				employee.setContact(contacts.getContact(Contact.Type.BUSINESS));
			}

			// load address
			AddressesList addressesList = addressService.load(employee.getDescriptor(), localConnection);
			if(addressesList != null && !addressesList.isEmpty()) {
				employee.setAddress(addressesList.getAddress(Address.Type.BUSINESS));
			}
		}
	}

	@Override
	public AEResponse saveTimeSheet(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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
			
			long ownerId = arguments.optLong("sOwnerId");
			JSONObject jsonTimeSheet = arguments.getJSONObject(SocialTimeSheet.JsonKey.period.toString());

			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();

			/**
			 * Create SocialTimeSheet
			 */
			SocialTimeSheet timeSheet = new SocialTimeSheet();
			timeSheet.create(jsonTimeSheet);
			
			// get connection
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			// load and validate employee (in case of real timesheet there is no employee)
			if(timeSheet.getEmployee() != null) {
				EmployeeDAO employeeDAO = daoFactory.getEmployeeDAO(localConnection);
				Employee employee = employeeDAO.load(timeSheet.getEmployee().getDescriptor());
				if(employee != null) {
					if(employee.getCompany().getDescriptor().getID() != ownerId) {
						throw AEError.System.SECURITY_VIOLATION.toException();
					} else {
						timeSheet.setEmployee(employee);
					}
				} else {
					throw AEError.System.SECURITY_VIOLATION.toException();
				}
			}
			
			// begin transaction
			localConnection.beginTransaction();
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			// save
			saveTimeSheet(timeSheet, localConnection);

			// validate timesheet - warnings
			AEExceptionsList warnings = validateTimeSheet(timeSheet, invContext, localConnection);

			/**
			 * Commit and return response
			 */
			localConnection.commit();

			JSONObject payload = new JSONObject();
			JSONObject periodJson = timeSheet.toJSONObject();
			if(warnings != null && !warnings.isEmpty()) {
				periodJson.put("warnings", warnings.toJSONArray());
			}
			payload.put(SocialTimeSheet.JsonKey.period.toString(), periodJson);
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private void saveTimeSheet(SocialTimeSheet timeSheet, AEConnection connection) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();

			/**
			 * BeforeSave
			 */

			// synch hours
			timeSheet.synchHours();

			// SocialTimeSheet save validation
			SocialTimeSheetEntryPeriodsValidator periodValidator = SocialTimeSheetEntryPeriodsValidator.getInstance();
			for (Iterator<SocialTimeSheetEntry> iterator = timeSheet.iterator(); iterator.hasNext();) {
				SocialTimeSheetEntry entry = (SocialTimeSheetEntry) iterator.next();
				if(entry.getPersistentState() != AEPersistent.State.DELETED) {
					periodValidator.validate(entry);
				}
			}

			/**
			 * Save in transaction
			 */

			// get connection and begin transaction
			localConnection = daoFactory.getConnection(connection);
			localConnection.beginTransaction();

			SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);
			for (Iterator<SocialTimeSheetEntry> iterator = timeSheet.iterator(); iterator.hasNext();) {
				SocialTimeSheetEntry socialTimeSheetEntry = (SocialTimeSheetEntry) iterator.next();
				switch(socialTimeSheetEntry.getPersistentState()) {
				case NEW:
					socialDAO.insert(socialTimeSheetEntry);
					break;
				case UPDATED:
					socialDAO.update(socialTimeSheetEntry);
					break;
				case DELETED:
					socialDAO.deleteSocialTimeSheetEntry(socialTimeSheetEntry.getDescriptor());
					iterator.remove();
					break;
				default:
					// view
					break;
				}
			}

			if(timeSheet.getPeriodType().getId() == SocialPeriodType.TEMPLATE.getId()) {
				// recalculate number of weeks
				Map<Integer, List<SocialTimeSheetEntry>> weeks = timeSheet.toWeeks();
				Set<Integer> weeksSet = weeks.keySet();
				timeSheet.setNumberOfWeeks(weeksSet.size());

				// save in the employee
				EmployeeDAO employeeDAO = daoFactory.getEmployeeDAO(localConnection);
				employeeDAO.updateNumberOfWeeks(timeSheet.getEmployee(), timeSheet.getNumberOfWeeks());
			}

			/**
			 * AfterSave
			 */

			/**
			 * Commit and return response
			 */
			localConnection.commit();
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadTimeSheet(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();

			long ownerId = arguments.getLong(SocialTimeSheet.JsonKey.ownerId.toString());

			long employeeId = arguments.getLong(SocialTimeSheet.JsonKey.employeeId.toString());
			AEDescriptor emplDescr = Employee.lazyDescriptor(employeeId);

			int periodTypeId = arguments.getInt(SocialTimeSheet.JsonKey.periodType.toString());
			SocialPeriodType periodType = SocialPeriodType.valueOf(periodTypeId);
			if(SocialPeriodType.NA.equals(periodType)) {
				throw new AEException("System error: periodType is not valid");
			}

			// month
			Integer month = JSONUtil.optInteger(arguments, SocialTimeSheet.JsonKey.month.name());
			if(month != null && (month < 1 || month > 12)) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}

			// year
			Integer year = JSONUtil.optInteger(arguments, SocialTimeSheet.JsonKey.year.name());
			
			/**
			 * Factory and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();

			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			// load employee
			EmployeeDAO employeeDAO = daoFactory.getEmployeeDAO(localConnection);
			Employee employee = employeeDAO.load(emplDescr);
			if(employee.getCompany().getDescriptor().getID() != ownerId) {
				throw new AEException("System error: The employee's company is different than selected");
			}

			// determine period date
			Date startDate = null;
			Date endDate = null;
			if(SocialPeriodType.TEMPLATE.equals(periodType)) {
				Date dateEntry = employee.getDateEntry();
				startDate = AEDateUtil.firstDayOfWeek(dateEntry);
			} else if(SocialPeriodType.PLAN.equals(periodType)) {
				// init startDate and endDate
				if(month != null && year != null) {
					// use specified month and year to init startDate and endDate 
					startDate = AEDateUtil.firstDayOfWeek(AEDateUtil.getFirstDate(month - 1, year));		
					endDate = AEDateUtil.lastDayOfWeek(AEDateUtil.getLastDate(month - 1, year));
				} else {
					// use the first open period to init startDate and endDate 
					AccPeriod accPeriod = getFirstOpenPeriod(
							aeRequest.getArguments().getLong("ownerId"), 
							AEApp.SOCIAL_TIMESHEET_PLAN_MODULE_ID, 
							localConnection);
					startDate = AEDateUtil.firstDayOfWeek(accPeriod.getStartDate());
					endDate = AEDateUtil.lastDayOfWeek(accPeriod.getEndDate());
				}
			} else if(SocialPeriodType.ACTUAL.equals(periodType)) {
				// TODO
			}

			/**
			 * Load time sheet
			 */
			SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);
			SocialTimeSheet timeSheet = new SocialTimeSheet(employee, periodType, startDate);
			if(SocialPeriodType.TEMPLATE.equals(periodType)) {
				Integer numberOfWeeks = employee.getNumberOfWeeks();
				if(numberOfWeeks == null) {
					numberOfWeeks = 0;
				}
				timeSheet.setNumberOfWeeks(numberOfWeeks);
				socialDAO.loadTimeSheetTemplate(timeSheet);
			} else if(SocialPeriodType.PLAN.equals(periodType)) {
				timeSheet.setEndDate(endDate);
				socialDAO.loadTimeSheetPlanByPeriod(timeSheet);	
			} else if(SocialPeriodType.ACTUAL.equals(periodType)) {
				socialDAO.loadTimeSheetPlan(timeSheet);	
			}

			JSONObject payload = new JSONObject();
			payload.put(SocialTimeSheet.JsonKey.period.toString(), timeSheet.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse pushWeek(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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
			JSONObject jsonTimeSheet = arguments.getJSONObject(SocialTimeSheet.JsonKey.period.toString());
			long ownerId = arguments.optLong("sOwnerId");

			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);

			/**
			 * Create SocialTimeSheet
			 */
			SocialTimeSheet timeSheet = new SocialTimeSheet();
			timeSheet.create(jsonTimeSheet);
			
			/**
			 * Factories and connection
			 */ 
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			// load and validate employee (in case of real timesheet there is no employee)
			if(timeSheet.getEmployee() != null) {
				EmployeeDAO employeeDAO = daoFactory.getEmployeeDAO(localConnection);
				Employee employee = employeeDAO.load(timeSheet.getEmployee().getDescriptor());
				if(employee != null) {
					if(employee.getCompany().getDescriptor().getID() != ownerId) {
						throw AEError.System.SECURITY_VIOLATION.toException();
					} else {
						timeSheet.setEmployee(employee);
					}
				} else {
					throw AEError.System.SECURITY_VIOLATION.toException();
				}
			}

			AEExceptionsList warnings = null;
			if(SocialPeriodType.TEMPLATE.equals(timeSheet.getPeriodType())) {
				/**
				 * Push
				 */
				timeSheet.pushWeek();

				/**
				 * Begin transaction
				 */
				localConnection.beginTransaction();
				invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

				/**
				 * Save 
				 */
				saveTimeSheet(timeSheet, localConnection);

				// validate timesheet - warnings
				warnings = validateTimeSheet(timeSheet, invContext, localConnection);

				/**
				 * Commit and return response
				 */
				localConnection.commit();
			}

			JSONObject payload = new JSONObject();
			JSONObject periodJson = timeSheet.toJSONObject();
			if(warnings != null && !warnings.isEmpty()) {
				// periodJson.put("warnings", warnings.toJSONArray());
			}
			payload.put(SocialTimeSheet.JsonKey.period.toString(), periodJson);
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse popWeek(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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
			JSONObject jsonTimeSheet = arguments.getJSONObject(SocialTimeSheet.JsonKey.period.toString());
			long ownerId = arguments.optLong("sOwnerId");

			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);

			/**
			 * Factories and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			/**
			 * Create SocialTimeSheet
			 */
			SocialTimeSheet timeSheet = new SocialTimeSheet();
			timeSheet.create(jsonTimeSheet);

			// load and validate employee (in case of real timesheet there is no employee)
			if(timeSheet.getEmployee() != null) {
				EmployeeDAO employeeDAO = daoFactory.getEmployeeDAO(localConnection);
				Employee employee = employeeDAO.load(timeSheet.getEmployee().getDescriptor());
				if(employee != null) {
					if(employee.getCompany().getDescriptor().getID() != ownerId) {
						throw AEError.System.SECURITY_VIOLATION.toException();
					} else {
						timeSheet.setEmployee(employee);
					}
				} else {
					throw AEError.System.SECURITY_VIOLATION.toException();
				}
			}
			
			AEExceptionsList warnings = null;
			if(SocialPeriodType.TEMPLATE.equals(timeSheet.getPeriodType())) {
				/**
				 * Pop
				 */
				timeSheet.popWeek();

				/**
				 * Get connection and begin transaction
				 */
				localConnection.beginTransaction();
				invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

				/**
				 * Save
				 */
				saveTimeSheet(timeSheet, localConnection);

				// validate timesheet - warnings
				warnings = validateTimeSheet(timeSheet, invContext, localConnection);

				/**
				 * Commit and return response
				 */
				localConnection.commit();
			}

			JSONObject payload = new JSONObject();
			JSONObject periodJson = timeSheet.toJSONObject();
			if(warnings != null && !warnings.isEmpty()) {
				periodJson.put("warnings", warnings.toJSONArray());
			}
			payload.put(SocialTimeSheet.JsonKey.period.toString(), periodJson);
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public SocialTimeSheet createSchedule(AEDescriptor ownerDescr, Date startDateRaw, Date endDateRaw, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Factory and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			/**
			 * Determine and validate the period
			 */
			Date startDate = AEDateUtil.firstDayOfWeek(startDateRaw);
			Date endDate = AEDateUtil.lastDayOfWeek(endDateRaw);
			if(endDate.before(startDate)) {
				throw AEError.System.SOCIAL_INVALID_SCHEDULE_PERIOD.toException();
			}

			/**
			 * DAOs
			 */
			EmployeeDAO employeeDAO = daoFactory.getEmployeeDAO(localConnection);
			SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);

			/**
			 * Load current employees
			 */
			EmployeeList employeeList = employeeDAO.loadCurrentEmployeesLazzy(ownerDescr);

			/**
			 * For every employee create schedule
			 */
			SocialTimeSheet planEntries = new SocialTimeSheet();
			for (Iterator<Employee> iterator = employeeList.iterator(); iterator.hasNext();) {
				Employee employee = (Employee) iterator.next();

				// load template for the current employee
				Date templateStartDate = getTemplateStartDate(employee, localConnection);

				Integer numberOfWeeks = employee.getNumberOfWeeks();
				if(numberOfWeeks == null) {
					numberOfWeeks = 0;
				}

				SocialTimeSheet templateTimeSheet = 
						new SocialTimeSheet(employee, SocialPeriodType.TEMPLATE, templateStartDate, numberOfWeeks);			
				socialDAO.loadTimeSheetTemplate(templateTimeSheet);	

				if(templateTimeSheet.isEmpty()) {
					templateTimeSheet.createTemplate();
				}

				// create plan
				Date periodStartDate = startDate;
				while(templateStartDate.after(periodStartDate)) {
					periodStartDate = AEDateUtil.addDaysToDate(periodStartDate, 1);
				}
				if(AEDateUtil.getDayOfWeek(periodStartDate) != AEDateUtil.getEuropeanCalendar().getFirstDayOfWeek()) {
					throw new AEException("Internal error: periodStartDate is not Monday");
				}

				SocialTimeSheet planTimeSheet = 
						new SocialTimeSheet(employee, SocialPeriodType.PLAN, periodStartDate);

				long weeksDiff = AEDateUtil.getWeeksDiffSign(templateStartDate, periodStartDate);
				long startWeek = 1;
				if(numberOfWeeks > 1) {
					startWeek = (weeksDiff % numberOfWeeks) + 1;
				}

				// the periodStartDate should have startWeek template
				Date currentMonday = periodStartDate;
				long currentWeek = startWeek;
				while(currentMonday.before(endDate)) {
					// create week
					Map<Integer, SocialTimeSheetEntry> week = templateTimeSheet.getWeek(currentWeek);
					Set<Integer> days = week.keySet();
					for (Iterator<Integer> iterator2 = days.iterator(); iterator2.hasNext();) {
						Integer dayOfWeek = (Integer) iterator2.next();
						SocialTimeSheetEntry templateEntry = week.get(dayOfWeek);
						if(templateEntry != null) {
							SocialTimeSheetEntry entry = (SocialTimeSheetEntry) AEObjectUtil.deepCopy(templateEntry);

							entry.resetAsNew();

							// check the date

							Date currentDate = null;
							if(dayOfWeek == SocialTimeSheet.Weekday.SUM.ordinal()) {
								currentDate = AEDateUtil.addDaysToDate(currentMonday, dayOfWeek - 2);
							} else {
								currentDate = AEDateUtil.addDaysToDate(currentMonday, dayOfWeek - 1);
							}

							if(employee.getDateRelease() != null) {
								if(!currentDate.before(employee.getDateRelease())) {
									continue;
								}
							}
							entry.setDate(currentDate);
							planTimeSheet.create(entry);

							planTimeSheet.add(entry);
						}
					}

					// next week
					currentMonday = AEDateUtil.addWeeksToDate(currentMonday, 1);
					currentWeek++;
					if(currentWeek > numberOfWeeks) {
						currentWeek = 1;
					}
				}

				// save time sheet
				planEntries.addAll(planTimeSheet);
			}

			// validate the plan
			SocialTimeSheetEntryPeriodsValidator periodValidator = SocialTimeSheetEntryPeriodsValidator.getInstance();
			for (Iterator<SocialTimeSheetEntry> iterator = planEntries.iterator(); iterator.hasNext();) {
				SocialTimeSheetEntry entry = (SocialTimeSheetEntry) iterator.next();
				if(entry.getPersistentState() != AEPersistent.State.DELETED) {
					periodValidator.validate(entry);
				}
			}

			return planEntries;
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/**
	 * Generates/updates schedule for specified period and creates acc periods 
	 */
	@Override
	public AEResponse generateSchedule(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();

			int periodTypeId = arguments.getInt(SocialTimeSheet.JsonKey.periodType.toString());
			SocialPeriodType periodType = SocialPeriodType.valueOf(periodTypeId);
			if(!SocialPeriodType.PLAN.equals(periodType)) {
				JSONObject payload = new JSONObject();
				return new AEResponse(payload);
			}

			AEDescriptor ownerDescr = Organization.lazyDescriptor(arguments.getLong("ownerId"));
			Date startDateRaw = AEDateUtil.parseDateStrict(arguments.getString("startDate"));
			Date endDateRaw = AEDateUtil.parseDateStrict(arguments.getString("endDate"));

			/**
			 * Factory and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * Generate schedule
			 */

			SocialTimeSheet schedule = createSchedule(ownerDescr, startDateRaw, endDateRaw, localConnection);

			// validate schedule
			SocialTimeSheetEntryPeriodsValidator periodValidator = SocialTimeSheetEntryPeriodsValidator.getInstance();
			for (Iterator<SocialTimeSheetEntry> iterator = schedule.iterator(); iterator.hasNext();) {
				SocialTimeSheetEntry entry = (SocialTimeSheetEntry) iterator.next();
				if(entry.getPersistentState() != AEPersistent.State.DELETED) {
					periodValidator.validate(entry);
				}
			}

			// Begin transaction
			localConnection.beginTransaction();

			// DAOs
			SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);

			// insert schedule
			socialDAO.insertNotExisted(schedule);

			// update the schedule start date
			Date nextScheduleStartDate = getPlanStartDate(ownerDescr, localConnection);
			Date endDate = AEDateUtil.lastDayOfWeek(endDateRaw);
			if(nextScheduleStartDate == null || nextScheduleStartDate.before(endDate)) {
				nextScheduleStartDate = endDate;
				socialDAO.updateScheduleStartDate(ownerDescr, nextScheduleStartDate);
			};

			// insert missing acc periods
			int startMonth = AEDateUtil.getMonthInYear(startDateRaw);
			int endMonth = AEDateUtil.getMonthInYear(endDateRaw);
			for (int i = 0; (startMonth + i) <= endMonth; i++) {
				Date iPeriodStartDate = AEDateUtil.addMonthsToDate(startDateRaw, i);
				AccPeriod iPeriod = getAccPeriod(
						ownerDescr.getID(), 
						AEApp.SOCIAL_TIMESHEET_PLAN_MODULE_ID, 
						iPeriodStartDate, 
						localConnection);
				
				if(iPeriod == null) {
					iPeriod = new AccPeriod();
					iPeriod.setCompany(ownerDescr);
					iPeriod.setModuleId(AEApp.SOCIAL_TIMESHEET_PLAN_MODULE_ID);
					iPeriod.setStartDate(AEDateUtil.firstDayOfMonth(iPeriodStartDate));
					iPeriod.setEndDate(getPeriodEndDate(iPeriodStartDate));
					iPeriod.setClosed(false);
					accPeriodDAO.insert(iPeriod);
				}
			}
			
			// commit	
			localConnection.commit();

			/**
			 * Reload social info
			 */
			SocialInfo socInfo = socialDAO.loadSocialInfo(ownerDescr);

			JSONObject payload = new JSONObject();
			payload.put("socialInfo", socInfo.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse closePeriod(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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
			
			// ownerId and sOwnerId
			AEDescriptor customerDescr = Organization.lazyDescriptor(arguments.getLong("ownerId")); 
			long sOwnerId = arguments.getLong("sOwnerId");
			if(sOwnerId != customerDescr.getID()) {
				throw AEError.System.SECURITY_VIOLATION.toException();
			}
			ap.ownershipValidator(sOwnerId);
			
			// periodTypeId
			int periodTypeId = arguments.getInt(SocialTimeSheet.JsonKey.periodType.toString());
			SocialPeriodType periodType = SocialPeriodType.valueOf(periodTypeId);
			if(!SocialPeriodType.PLAN.equals(periodType) && !SocialPeriodType.ACTUAL.equals(periodType)) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}

			/**
			 * Get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * determine period
			 */
			AccPeriod accPeriod = null;
			if(SocialPeriodType.PLAN.equals(periodType)) {
				accPeriod = getFirstOpenPeriod(
						customerDescr.getID(), 
						AEApp.SOCIAL_TIMESHEET_PLAN_MODULE_ID, 
						localConnection);
			} else if(SocialPeriodType.ACTUAL.equals(periodType)) {
				accPeriod = getFirstOpenPeriod(
						customerDescr.getID(), 
						AEApp.SOCIAL_TIMESHEET_ACTUAL_MODULE_ID, 
						localConnection);
				
				// we can close only finished periods
				Date nowDate = AEDateUtil.getClearDate(new Date());
				if(nowDate.before(AEDateUtil.getClearDate(accPeriod.getEndDate()))) {
					throw new AEException(
							(int) AEError.System.CANNOT_CLOSE_UNFINISHED.getSystemID(), 
							"La période n'est pas terminée.");
				}
			}

			/**
			 * Begin transaction to close the period
			 */
			localConnection.beginTransaction();

			/**
			 * close the period
			 */
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
			accPeriodDAO.closePeriod(accPeriod.getID());

			/**
			 * commit transaction and return response 
			 */
			localConnection.commit();

			/**
			 * Process after close.
			 * Must be after transaction close.
			 * If after close fails, this should not affect the whole close task.
			 */
			if(SocialPeriodType.ACTUAL.equals(periodType)) {
				try {
					// send e-mail
					Emailer emailer = new Emailer();
					emailer.onPeriodClosed(customerDescr, invContext, accPeriod, AEApp.SOCIAL_TIMESHEET_ACTUAL_MODULE_ID);
				} catch (Throwable t) {
					AEApp.logger().error(t);
				}
			}

			/**
			 * return response
			 */
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
	public AEResponse openPeriod(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * determine period
			 */
			JSONObject arguments = aeRequest.getArguments();
			
			// ownerId and sOwnerId
			AEDescriptor customerDescr = Organization.lazyDescriptor(arguments.getLong("ownerId")); 
			long sOwnerId = arguments.getLong("sOwnerId");
			if(sOwnerId != customerDescr.getID()) {
				throw AEError.System.SECURITY_VIOLATION.toException();
			}
			ap.ownershipValidator(sOwnerId);
			
			// periodTypeId
			int periodTypeId = arguments.getInt(SocialTimeSheet.JsonKey.periodType.toString());
			SocialPeriodType periodType = SocialPeriodType.valueOf(periodTypeId);
			if(!SocialPeriodType.PLAN.equals(periodType) && !SocialPeriodType.ACTUAL.equals(periodType)) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}

			/**
			 * determine period
			 */
			AccPeriod accPeriod = null;
			if(SocialPeriodType.PLAN.equals(periodType)) {
				accPeriod = getLastClosedPeriod(
						customerDescr.getID(), 
						AEApp.SOCIAL_TIMESHEET_PLAN_MODULE_ID, 
						localConnection);
			} else if(SocialPeriodType.ACTUAL.equals(periodType)) {
				accPeriod = getLastClosedPeriod(
						customerDescr.getID(), 
						AEApp.SOCIAL_TIMESHEET_ACTUAL_MODULE_ID, 
						localConnection);
			}
			
			if(accPeriod != null) {
				localConnection.beginTransaction();

				/**
				 * Open the period
				 */
				AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
				accPeriodDAO.openPeriod(accPeriod.getID());
				accPeriodDAO.notExportedPeriod(accPeriod.getID());

				/**
				 * commit transaction and return response
				 */
				localConnection.commit();
			}

			/**
			 * return response
			 */
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/**
	 * Loads the DB state for requested time sheet.
	 * 
	 * Returns the time sheet info for:
	 * - requested ownerId;
	 * - requested periodType (Template, Plan, ActualWork);
	 * - applied rool over specified anchor period
	 * 
	 * @param ownerId The identity of the requested customer
	 * @param periodType The identity code of the module (sheet type) (Template (5), Plan ..., ActualWork ...)
	 * @param month The requested anchor month. Missed or empty property means no anchor month (anchor period will be the first open period)
	 * @param year The requested anchor year. Missed or empty property means no anchor year (anchor period will be the first open period)
	 * @param roll The requested rolling, integer value zero, negative or positive. Missed or empty property means no rolling will be applied to the anchor period
	 */
	@Override
	public AEResponse loadTimeSheetInfo(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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
			
			// ownerId and sOwnerId
			AEDescriptor ownerDescr = Organization.lazyDescriptor(arguments.getLong("ownerId")); 
			long sOwnerId = arguments.getLong("sOwnerId");
			if(sOwnerId != ownerDescr.getID()) {
				throw AEError.System.SECURITY_VIOLATION.toException();
			}
			ap.ownershipValidator(sOwnerId);
			
			// periodTypeId
			int periodTypeId = arguments.getInt(SocialTimeSheet.JsonKey.periodType.toString());
			SocialPeriodType periodType = SocialPeriodType.valueOf(periodTypeId);
			if(SocialPeriodType.NA.equals(periodType)) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}
			
			// month
			Integer month = JSONUtil.optInteger(arguments, SocialTimeSheet.JsonKey.month.name());
			if(month != null && (month < 1 || month > 12)) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}

			// year
			Integer year = JSONUtil.optInteger(arguments, SocialTimeSheet.JsonKey.year.name());
			
			// roll
			Integer roll = JSONUtil.optInteger(arguments, SocialTimeSheet.JsonKey.roll.name());
			
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();

			/**
			 * Connection
			 */
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * Calculate periods
			 */
			AETimePeriod anchorCalendarPeriod = null; // comming into request
			AETimePeriod calendarPeriod = null; // the calendar period to be returned: anchorCalendarPeriod with applied roll
			AccPeriod nextCalendarPeriod = null; // next to calendarPeriod acc period if exists: null if doesn't exist
			AccPeriod prevCalendarPeriod = null; // prev to calendarPeriod acc period if exists: null if doesn't exist
			AETimePeriod socialPeriod = null;
			AccPeriod accPeriod = null; // first open acc period (current working period)
			
			// calendar period
			if(month != null && year != null) {
				// use specified month and year to create anchorCalendarPeriod
				Date startDate = AEDateUtil.getFirstDate(month - 1, year);		
				Date endDate = AEDateUtil.getLastDate(month - 1, year);
				anchorCalendarPeriod = new AETimePeriod(startDate, endDate);
			} else {
				// use the first open period to create anchorCalendarPeriod
				if(SocialPeriodType.PLAN.equals(periodType)) {
					accPeriod = getFirstOpenPeriod(ownerDescr.getID(), AEApp.SOCIAL_TIMESHEET_PLAN_MODULE_ID, localConnection);
				} else if(SocialPeriodType.ACTUAL.equals(periodType)) {
					accPeriod = getFirstOpenPeriod(ownerDescr.getID(), AEApp.SOCIAL_TIMESHEET_ACTUAL_MODULE_ID, localConnection);
				}
				if(accPeriod != null) {
					anchorCalendarPeriod = new AETimePeriod(accPeriod.getStartDate(), accPeriod.getEndDate());
				}
			}
			
			// apply requested roll to anchorCalendarPeriod to define calendarPeriod
			if(roll != null) {
				Date startDate = AEDateUtil.addMonthsToDate(anchorCalendarPeriod.getStartDate(), roll);	
				Date endDate = AEDateUtil.lastDayOfMonth(startDate);
				calendarPeriod = new AETimePeriod(startDate, endDate);
			} else {
				calendarPeriod = anchorCalendarPeriod;
			}
			
			// other periods
			if(calendarPeriod != null) {
				socialPeriod = new AETimePeriod(
						AEDateUtil.firstDayOfWeek(calendarPeriod.getStartDate()), 
						AEDateUtil.lastDayOfWeek(calendarPeriod.getEndDate()));
				
				if(SocialPeriodType.PLAN.equals(periodType)) {
					prevCalendarPeriod = getAccPeriod(
							ownerDescr.getID(), 
							AEApp.SOCIAL_TIMESHEET_PLAN_MODULE_ID, 
							AEDateUtil.addMonthsToDate(calendarPeriod.getStartDate(), -1), 
							localConnection);
					
					if(accPeriod == null) {
						accPeriod = getAccPeriod(
								ownerDescr.getID(), 
								AEApp.SOCIAL_TIMESHEET_PLAN_MODULE_ID, 
								calendarPeriod.getStartDate(), 
								localConnection);
					}
					
					nextCalendarPeriod = getAccPeriod(
							ownerDescr.getID(), 
							AEApp.SOCIAL_TIMESHEET_PLAN_MODULE_ID, 
							AEDateUtil.addMonthsToDate(calendarPeriod.getStartDate(), 1), 
							localConnection);
				} else if(SocialPeriodType.ACTUAL.equals(periodType)) {
					prevCalendarPeriod = getAccPeriod(
							ownerDescr.getID(), 
							AEApp.SOCIAL_TIMESHEET_ACTUAL_MODULE_ID, 
							AEDateUtil.addMonthsToDate(calendarPeriod.getStartDate(), -1), 
							localConnection);
					
					if(accPeriod == null) {
						accPeriod = getAccPeriod(
								ownerDescr.getID(), 
								AEApp.SOCIAL_TIMESHEET_ACTUAL_MODULE_ID, 
								calendarPeriod.getStartDate(), 
								localConnection);
					}
					
					nextCalendarPeriod = getAccPeriod(
							ownerDescr.getID(), 
							AEApp.SOCIAL_TIMESHEET_ACTUAL_MODULE_ID, 
							AEDateUtil.addMonthsToDate(calendarPeriod.getStartDate(), 1), 
							localConnection);
				}
			}
			
			/**
			 * Load employees for calendarPeriod
			 * TODO
			 */
			EmployeeDAO employeeDAO = daoFactory.getEmployeeDAO(localConnection);
			EmployeeList employeeList = employeeDAO.loadCurrentEmployeesLazzy(ownerDescr);

			/**
			 * Load social info and init scheduleStartDate
			 */
			SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);
			SocialInfo socInfo = socialDAO.loadSocialInfo(ownerDescr);
			if(socInfo == null) {
				throw AEError.System.SOCIAL_SETTINGS_MISSING.toException();
			}
			if(socInfo.getScheduleStartDate() == null) {
				socInfo.setScheduleStartDate(getPlanStartDate(ownerDescr, localConnection));
			}

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject();
			
			// build config
			JSONObject configJson = new JSONObject();
			
			// social period
			if(socialPeriod != null) {
				configJson.put("startDate", AEDateUtil.formatToSystem(socialPeriod.getStartDate()));
				configJson.put("endDate", AEDateUtil.formatToSystem(socialPeriod.getEndDate()));
			}
			
			// calendar period
			if(calendarPeriod != null) {
				configJson.put(SocialTimeSheet.JsonKey.month.name(), AEDateUtil.getMonthInYear(calendarPeriod.getStartDate()));
				configJson.put(SocialTimeSheet.JsonKey.year.name(), AEDateUtil.getYear(calendarPeriod.getStartDate()));
			}
			
			configJson.put("editable", accPeriod != null && !accPeriod.isClosed());
			configJson.put("hasNext", nextCalendarPeriod != null);
			configJson.put("hasPrev", prevCalendarPeriod != null);
			
			// build payload
			payload.put("config", configJson);
			payload.put("employees", employeeList.toJSONArray());
			payload.put(SocialInfo.JSONKey.socialInfo.toString(), socInfo.toJSONObject());
			
			// return
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private Date getTemplateStartDate(Employee employee, AEConnection connection) throws AEException {
		Date date = null;

		date = employee.getDateEntry();

		if(date == null) {
			throw AEError.System.SOCIAL_INVALID_TEMPLATE_START_DATE.toException();
		}
		return AEDateUtil.firstDayOfWeek(date);
	}

	private Date getPlanStartDate(AEDescriptor ownerDescr, AEConnection connection) throws AEException {
		Date date = null;

		DAOFactory daoFactory = DAOFactory.getInstance();
		SocialDAO socialDAO = daoFactory.getSocialDAO(connection);
		SocialInfo socInfo = socialDAO.loadSocialInfo(ownerDescr);
		if(socInfo == null) {
			throw AEError.System.SOCIAL_SETTINGS_MISSING.toException();
		}
		if(socInfo.getScheduleStartDate() == null) {
			if(socInfo.getStartDate() == null) {
				throw AEError.System.SOCIAL_SETTINGS_MISSING.toException();
			}
			date = socInfo.getStartDate();
		} else {
			date = socInfo.getScheduleStartDate();
		}

		return date;
	}

	@Override
	public AEResponse loadTimeSheetActualInfo(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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
			AEDescriptor ownerDescr = Organization.lazyDescriptor(arguments.getLong("ownerId")); 
			long sOwnerId = arguments.getLong("sOwnerId");
			if(sOwnerId != ownerDescr.getID()) {
				throw AEError.System.SECURITY_VIOLATION.toException();
			}
			ap.ownershipValidator(sOwnerId);
			
			// periodTypeId
			int periodTypeId = arguments.getInt(SocialTimeSheet.JsonKey.periodType.toString());
			SocialPeriodType periodType = SocialPeriodType.valueOf(periodTypeId);
			if(!SocialPeriodType.ACTUAL.equals(periodType)) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}
			
			// month
			Integer month = JSONUtil.optInteger(arguments, SocialTimeSheet.JsonKey.month.name());
			if(month != null && (month < 1 || month > 12)) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}

			// year
			Integer year = JSONUtil.optInteger(arguments, SocialTimeSheet.JsonKey.year.name());
			
			// roll
			Integer roll = JSONUtil.optInteger(arguments, SocialTimeSheet.JsonKey.roll.name());

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();

			/**
			 * Connection
			 */
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * Calculate periods
			 */
			AETimePeriod anchorCalendarPeriod = null; // comming into request
			AETimePeriod calendarPeriod = null; // the calendar period to be returned: anchorCalendarPeriod with applied roll
			AccPeriod nextCalendarPeriod = null; // next to calendarPeriod acc period if exists: null if doesn't exist
			AccPeriod prevCalendarPeriod = null; // prev to calendarPeriod acc period if exists: null if doesn't exist
			AETimePeriod socialPeriod = null;
			AccPeriod accPeriod = null; // first open acc period (current working period)
			
			// calendar period
			if(month != null && year != null) {
				// use specified month and year to create anchorCalendarPeriod
				Date startDate = AEDateUtil.getFirstDate(month - 1, year);		
				Date endDate = AEDateUtil.getLastDate(month - 1, year);
				anchorCalendarPeriod = new AETimePeriod(startDate, endDate);
			} else {
				// use the first open period to create anchorCalendarPeriod
				accPeriod = getFirstOpenPeriod(ownerDescr.getID(), AEApp.SOCIAL_TIMESHEET_ACTUAL_MODULE_ID, localConnection);
				if(accPeriod != null) {
					anchorCalendarPeriod = new AETimePeriod(accPeriod.getStartDate(), accPeriod.getEndDate());
				}
			}
			
			// apply requested roll to anchorCalendarPeriod to define calendarPeriod
			if(roll != null) {
				Date startDate = AEDateUtil.addMonthsToDate(anchorCalendarPeriod.getStartDate(), roll);	
				Date endDate = AEDateUtil.lastDayOfMonth(startDate);
				calendarPeriod = new AETimePeriod(startDate, endDate);
			} else {
				calendarPeriod = anchorCalendarPeriod;
			}
			
			// other periods
			if(calendarPeriod != null) {
				socialPeriod = new AETimePeriod(
//						AEDateUtil.firstDayOfWeek(calendarPeriod.getStartDate()), 
//						AEDateUtil.lastDayOfWeek(calendarPeriod.getEndDate()));
						calendarPeriod.getStartDate(), 
						calendarPeriod.getEndDate());

				if(SocialPeriodType.ACTUAL.equals(periodType)) {
					prevCalendarPeriod = getAccPeriod(
							ownerDescr.getID(), 
							AEApp.SOCIAL_TIMESHEET_ACTUAL_MODULE_ID, 
							AEDateUtil.addMonthsToDate(calendarPeriod.getStartDate(), -1), 
							localConnection);

					if(accPeriod == null) {
						accPeriod = getAccPeriod(
								ownerDescr.getID(), 
								AEApp.SOCIAL_TIMESHEET_ACTUAL_MODULE_ID, 
								calendarPeriod.getStartDate(), 
								localConnection);
					}

					nextCalendarPeriod = getAccPeriod(
							ownerDescr.getID(), 
							AEApp.SOCIAL_TIMESHEET_ACTUAL_MODULE_ID, 
							AEDateUtil.addMonthsToDate(calendarPeriod.getStartDate(), 1), 
							localConnection);
				}
			}

			// calculate end operational date
			Date endOperationalDate = socialPeriod.getEndDate();
			Date nowDate = AEDateUtil.getClearDate(new Date());
			if(nowDate.before(AEDateUtil.getClearDate(accPeriod.getEndDate()))) {
				endOperationalDate = nowDate;
			}

			/**
			 * Load time sheet
			 */
			SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);
			SocialTimeSheet timeSheet = new SocialTimeSheet(periodType);
			socialDAO.loadTimeSheetActual(ownerDescr, timeSheet, socialPeriod.getStartDate(), endOperationalDate);	
			
			// if not validated, propose the planned periods
			for (SocialTimeSheetEntry entry: timeSheet) {
				if (!entry.isActualValidated()) {
					entry.setFromTimeActual_1(entry.getFromTime_1());
					entry.setToTimeActual_1(entry.getToTime_1());
					
					entry.setFromTimeActual_2(entry.getFromTime_2());
					entry.setToTimeActual_2(entry.getToTime_2());
				}
			}

			/**
			 * Load social info and init scheduleStartDate
			 */
			SocialInfo socInfo = socialDAO.loadSocialInfo(ownerDescr);
			if(socInfo == null) {
				throw AEError.System.SOCIAL_SETTINGS_MISSING.toException();
			}
			if(socInfo.getScheduleStartDate() == null) {
				socInfo.setScheduleStartDate(getPlanStartDate(ownerDescr, localConnection));
			}

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject();
			
			// build config
			JSONObject configJson = new JSONObject();
			
			// social period
			if(socialPeriod != null) {
				configJson.put("startDate", AEDateUtil.formatToSystem(socialPeriod.getStartDate()));
				configJson.put("endDate", AEDateUtil.formatToSystem(socialPeriod.getEndDate()));
			}
			
			// calendar period
			if(calendarPeriod != null) {
				configJson.put(SocialTimeSheet.JsonKey.month.name(), AEDateUtil.getMonthInYear(calendarPeriod.getStartDate()));
				configJson.put(SocialTimeSheet.JsonKey.year.name(), AEDateUtil.getYear(calendarPeriod.getStartDate()));
			}
			
			configJson.put("editable", accPeriod != null && !accPeriod.isClosed());
			configJson.put("hasNext", nextCalendarPeriod != null);
			configJson.put("hasPrev", prevCalendarPeriod != null);
			
			// build payload
			payload.put("config", configJson);
			payload.put(SocialTimeSheet.JsonKey.period.toString(), timeSheet.toJSONObject());
			payload.put(SocialInfo.JSONKey.socialInfo.toString(), socInfo.toJSONObject());

			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	protected Date getAbsoluteStartDate(long ownerId, AEConnection aeConnection) throws AEException {
		Date date = null;

		DAOFactory daoFactory = DAOFactory.getInstance();
		SocialDAO socialDAO = daoFactory.getSocialDAO(aeConnection);
		SocialInfo socInfo = socialDAO.loadSocialInfo(Organization.lazyDescriptor(ownerId));
		if(socInfo == null || socInfo.getStartDate() == null) {
			throw AEError.System.SOCIAL_SETTINGS_MISSING.toException();
		}
		date = socInfo.getStartDate();

		return date;
	}

	@Override
	public AEResponse loadPrintTemplates(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			AEDescriptor ownerDescr = Organization.lazyDescriptor(arguments.getLong("ownerId")); 
			int docType = arguments.getInt("docType");
			long docId = arguments.getInt("docId");

			/**
			 * Load the document
			 */

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();

			/**
			 * Connection
			 */
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * Load templates
			 */
			SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);
			AESocialDocument socDoc = (AESocialDocument) load(docType, docId, localConnection);
			SocialInfo socialInfo = socialDAO.loadSocialInfo(ownerDescr);
			SocialTemplatesSet tSet = socialInfo.getTemplatesSet();
			AEPrintTemplatesList printTemplates = socialDAO.loadPrintTemplates(
					ownerDescr, 
					socDoc, 
					tSet != null ? tSet.getId() : SocialTemplatesSet.NA.getId());

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject();
			payload.put("printTemplates", printTemplates.toJSONArray());
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private AEDocument load(int docType, long docId, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Detect document type
			 */
			AEDocumentType aeDocType = AEDocumentType.valueOf(docType);
			if(aeDocType == null || AEDocumentType.valueOf(AEDocumentType.System.NA).equals(aeDocType)) {
				throw new AEException("System Error: Unknnown document type!");
			}

			/**
			 * Factory and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			/**
			 * Load document
			 */
			AEDocumentDAO docDAO = daoFactory.getDocumentDAO(aeDocType, localConnection);
			AEDescriptor docDescr = new AEDescriptorImp(docId, DomainClass.AeDocument);
			AEDocument doc = docDAO.load(docDescr);

			// load in depth
			if(doc instanceof AESocialDocument) {
				loadInDepth((AESocialDocument) doc, localConnection);
			} else {
				throw AEError.System.SOCIAL_THE_ACTUAL_NOT_FOUND.toException();
			}

			// return response
			return doc;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private AEExceptionsList validateTimeSheet(SocialTimeSheet timeSheet, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			AEExceptionsList warnings = new AEExceptionsList();

			SocialPeriodType periodType = timeSheet.getPeriodType();
			if(SocialPeriodType.ACTUAL.equals(periodType)) {
				// actual period type is not under validation
				return warnings;
			}

			AEDescriptive empl = timeSheet.getEmployee();
			if(empl == null) {
				throw new AEException("System error: validateTimeSheet: The employee is null");
			}

			// start to validate
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			CotractDeTravailDAO docDAO = (CotractDeTravailDAO) daoFactory.getDocumentDAO(
					AEDocumentType.valueOf(AEDocumentType.System.ContractDeTravail), localConnection);

			// get the last contract of the employee
			AEDocument doc = docDAO.loadActualAttestation(empl.getDescriptor().getID());
			if(!(doc instanceof ContractDeTravail)) {
				throw new AEException("System error: validateTimeSheet: The last contract cannot be loaded");
			}
			ContractDeTravail contract = (ContractDeTravail) doc;
			Double hoursPerWeekInContact = contract.getHoursPerWeek();

			Map<Integer, List<SocialTimeSheetEntry>> weeksMap = timeSheet.toWeeks();
			if(contract.isFullTime()) {
				// day validations
				for (Iterator<SocialTimeSheetEntry> iterator = timeSheet.iterator(); iterator.hasNext();) {
					SocialTimeSheetEntry entry = iterator.next();

					if(!entry.isReal()) {
						continue;
					}

					// If the working period is over 10 hours in a day then send the message:
					// “Jour” DD/MM« Rappel à la loi non exhaustif maximum journalier autorisé dépassé ».
					double hours = AEMath.doubleValue(entry.getWorkingHours());
					if(hours > 10.0) {
						warnings.add(new AEException(
								AEError.System.SOCIAL_WARNING_350.getSystemID(),
								entry.getStringRepr() + " " + AEError.System.SOCIAL_WARNING_350.getMessage())); 
					}
				}

				// rest validation
				SocialTimeSheetEntry entryPrevious = null;
				List<SocialTimeSheetEntry> plannedEntries = timeSheet.filter(HasPlanPredicate.getInstance()); 
				for (Iterator<SocialTimeSheetEntry> iterator = plannedEntries.iterator(); iterator.hasNext();) {
					SocialTimeSheetEntry entry = iterator.next();

					// If there is no rest period of 11 hours between two days: 
					// “Rappel à la loi non exhaustif temps de repos quotidien non respecté”.
					if(entryPrevious != null) {
						AETimePeriod prevDayMax = entryPrevious.getMaxPlanPeriod();
						AETimePeriod dayMin = entry.getMinPlanPeriod();
						AETimePeriod rest = new AETimePeriod(prevDayMax.getEndDate(), dayMin.getStartDate());
						if(rest.isValidAndNotNull() && rest.calcDurationInHours() < 11) {
							warnings.add(new AEException(
									AEError.System.SOCIAL_WARNING_352.getSystemID(),
									entry.getStringRepr() + " " + AEError.System.SOCIAL_WARNING_352.getMessage())); 
						}
					}

					// init previous at the end
					entryPrevious = entry;
				}

				// week validations
				Set<Integer> keySet = weeksMap.keySet();
				List<Integer> weeksList = new ArrayList<Integer>(keySet);
				Collections.sort(weeksList);
				for (Iterator<Integer> iterator = weeksList.iterator(); iterator.hasNext();) {
					Integer weekNumber = iterator.next();
					List<SocialTimeSheetEntry> weekEntries = weeksMap.get(weekNumber);

					// If the working period is over 48 hours in a week then send the message: 
					// “Semaine” week number « Rappel à la loi non exhaustif maximum hebdomadaire autorisé dépassé ».
					double planHours = SocialTimeSheet.getPlanHours(weekEntries);
					if(planHours > 48.0) {
						warnings.add(new AEException(
								AEError.System.SOCIAL_WARNING_351.getSystemID(),
								"(S" + weekNumber + ") " + AEError.System.SOCIAL_WARNING_351.getMessage())); 
					}

					// If the working period is over 44 hours a week in a period of 12 weeks 
					// (slippery) then send the message: “Rappel à la loi non exhaustif maximum hebdomadaire autorisé dépassé sur la période de 12 semaine”

					// If there is no 35 hour rest period in a week: 
					// “Rappel à la loi non exhaustif temps de repos hebdomadaire non respecté”.
				}
			} else {

			}

			// If the amount of working time from one week is different 
			// from the time indicated in employees card: give a warning message; 
			Set<Integer> keySet = weeksMap.keySet();
			List<Integer> weeksList = new ArrayList<Integer>(keySet);
			Collections.sort(weeksList);

			for (Iterator<Integer> iterator = weeksList.iterator(); iterator.hasNext();) {
				Integer weekNumber = iterator.next();
				List<SocialTimeSheetEntry> weekEntries = weeksMap.get(weekNumber);

				double planHours = SocialTimeSheet.getPlanHours(weekEntries);
				if(!AEMath.isZeroAmount(Math.abs(planHours - AEMath.doubleValue(hoursPerWeekInContact)))) {
					warnings.add(new AEException(
							AEError.System.SOCIAL_WARNING_355.getSystemID(),
							"(S" + weekNumber + ") " + AEError.System.SOCIAL_WARNING_355.getMessage())); 
				}
			}

			// Notice it might happen that the working schedule gives a different working time from a week 
			// to the other therefore the controls should not lock the schedule; 
			// except if the total amount of working time divided by number of week differs 
			// from the time indicated in the employees card.
			double hoursInSchedule = 0.0;
			for (Iterator<Integer> iterator = weeksList.iterator(); iterator.hasNext();) {
				Integer weekNumber = iterator.next();
				List<SocialTimeSheetEntry> weekEntries = weeksMap.get(weekNumber);

				hoursInSchedule += SocialTimeSheet.getPlanHours(weekEntries);
			}
			if(timeSheet.getNumberOfWeeks() != 0 
					&& !AEMath.isZeroAmount(Math.abs(hoursInSchedule / timeSheet.getNumberOfWeeks() - AEMath.doubleValue(hoursPerWeekInContact)))) {
				warnings.add(new AEException(
						AEError.System.SOCIAL_WARNING_355.getSystemID(),
						AEError.System.SOCIAL_WARNING_355.getMessage())); 
			}

			return warnings;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse sendEMail(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			JSONObject jsonDocument = arguments.getJSONObject("document");
			JSONObject jsonCustomer = arguments.optJSONObject("customer");
			String action = arguments.optString("action");

			/**
			 * Detect document type
			 */
			int docType = jsonDocument.optInt("docType");
			AEDocumentType aeDocType = AEDocumentType.valueOf(docType);
			if(aeDocType == null || AEDocumentType.valueOf(AEDocumentType.System.NA).equals(aeDocType)) {
				throw new AEException("System Error: Unknnown document type!");
			}

			/**
			 * Factories
			 */
			AEDocumentFactory docFactory = AEDocumentFactory.getInstance(aeDocType);

			/**
			 * Create document
			 */
			AESocialDocument socialDocument = 
					(AESocialDocument) docFactory.createDocument(arguments.getJSONObject("document"));

			/**
			 * Create customer
			 */
			AEDescriptor customerDescr = null;
			if(jsonCustomer != null) {
				customerDescr = new AEDescriptorImp();
				customerDescr.create(jsonCustomer);
			} else {
				DAOFactory daoFactory = DAOFactory.getInstance();
				PartyDAO partyDAO = daoFactory.getOrganizationDAO(localConnection);
				customerDescr = partyDAO.loadDescriptor(socialDocument.getCompany().getDescriptor().getID());
			}

			/**
			 * Send EMail
			 */
			try {
				Emailer emailer = new Emailer();
				emailer.onSocialDocumentAction(customerDescr, invContext, socialDocument, action);
			} catch (Throwable t) {
				logger.error("Cannot send e-mail", t);
			}

			/**
			 * return empty response
			 */
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse transferFromMonBureau(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection connMonEntreprise = null;
		AEConnection connMonBureau = null;
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

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			long ownerId = arguments.getLong("ownerId");
			long ownerIdMonBureau = arguments.getLong("ownerIdMonBureau");
			int docType = arguments.getInt("docType");
			boolean merge = arguments.optBoolean("merge");

			/**
			 * Detect document type
			 */
			AEDocumentType aeDocType = AEDocumentType.valueOf(docType);
			if(aeDocType == null || AEDocumentType.valueOf(AEDocumentType.System.NA).equals(aeDocType)) {
				throw new AEException("System Error: Unknnown document type!");
			}

			/**
			 * Get connections 
			 */
			try {
				InitialContext ic = new InitialContext();
				DataSource dsMonBureau = (DataSource) ic.lookup("java:MonBureau");
				connMonBureau = new AEConnection(dsMonBureau.getConnection(), true);
				ic.close();
			} catch (NamingException e) {
				throw new AEException(e.getMessage(), e);
			}

			DAOFactory daoFactory = DAOFactory.getInstance();
			connMonEntreprise = daoFactory.getConnection();

			/**
			 * Begin MonEntreprise transaction
			 */
			connMonEntreprise.beginTransaction();

			/**
			 * Transfer
			 */
			if(aeDocType.getSystemID() == AEDocumentType.System.ContractDeTravail.getSystemID()) {
				transferContractDeTravail(
						aeDocType, 
						ownerId, 
						ownerIdMonBureau, 
						connMonEntreprise, 
						connMonBureau,
						invContext,
						merge);
			}

			/**
			 * Commit
			 */
			connMonEntreprise.commit();

			/**
			 * return empty response
			 */
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Throwable t) {
			AEConnection.rollback(connMonEntreprise);
			throw new AEException(t);
		} finally {
			AEConnection.close(connMonBureau);
			AEConnection.close(connMonEntreprise);
		}
	}

	private void transferContractDeTravail(
			AEDocumentType aeDocType, 
			long ownerId, 
			long ownerIdMonBureau, 
			AEConnection connMonEntreprise, 
			AEConnection connMonBureau,
			AEInvocationContext invContext,
			boolean merge) throws AEException, AEWarning {

		DAOFactory daoFactory = DAOFactory.getInstance();

		// validate for no second import
		if(!merge) {
			CotractDeTravailDAO contratDAO = daoFactory.getCotractDeTravailDAO(connMonEntreprise);
			if(contratDAO.alreadyImported(ownerId)) {
				throw new AEException("Already imported!");
			}
		}

		AEDocumentsList docList = null;
		MBContratDeTravailDAO mbCdtDAO = new MBContratDeTravailDAO(new AEConnection(connMonBureau.getConnection(), false));
		AESocialDocumentFilter filter = new AESocialDocumentFilter();
		filter.setCompany(Organization.lazyDescriptor(ownerIdMonBureau));

		// load documents from [Attestation2]
		EmployeeDAO emplDAO = daoFactory.getEmployeeDAO(connMonEntreprise);
		PersonDAO   personDAO = daoFactory.getPersonDAO(connMonEntreprise);
		docList = mbCdtDAO.loadAttestation2(filter);
		for (Iterator<AEDocument> iterator = docList.iterator(); iterator.hasNext();) {
			ContractDeTravail aeDocument = (ContractDeTravail) iterator.next();
			System.out.println("Attestation2: " + aeDocument.getID());

			// set docType
			aeDocument.setType(aeDocType);

			// set company
			aeDocument.setCompany(Organization.lazyDescriptor(ownerId));

			// set as imported
			aeDocument.setProperty(AEObject.Property.MONBUREAU);

			// employee
			aeDocument.getEmployee().setValidateUIN(false);
			aeDocument.getEmployee().createName();
			aeDocument.getEmployee().setProperty(AEObject.Property.MONBUREAU);

			Long personId = null;
			if(!AEStringUtil.isEmpty(aeDocument.getEmployee().getUIN())) {
				personId = emplDAO.loadPersonID(aeDocument.getEmployee().getUIN(), ownerId);
			}
			if(personId != null) {
				Person person = (Person) personDAO.load(Person.lazyDescriptor(personId));
				if(person != null) {
					aeDocument.getEmployee().setPerson(person);
				}
			}

			// reset as new
			aeDocument.resetAsNew();

			// save in MonEntreprise
			invContext.setProperty(AEInvocationContext.AEConnection, connMonEntreprise);
			insert(aeDocument, true, invContext);
		}

		// load documents from [ContratTravail]
		docList = mbCdtDAO.loadContratDeTravail(filter);
		for (Iterator<AEDocument> iterator = docList.iterator(); iterator.hasNext();) {
			ContractDeTravail aeDocument = (ContractDeTravail) iterator.next();
			System.out.println("Attestation2: " + aeDocument.getID());

			// set docType
			aeDocument.setType(aeDocType);

			// set company
			aeDocument.setCompany(Organization.lazyDescriptor(ownerId));

			// set as imported
			aeDocument.setProperty(AEObject.Property.MONBUREAU);

			// employee
			aeDocument.getEmployee().setValidateUIN(false);
			aeDocument.getEmployee().createName();
			aeDocument.getEmployee().setProperty(AEObject.Property.MONBUREAU);

			Long personId = null;
			if(!AEStringUtil.isEmpty(aeDocument.getEmployee().getUIN())) {
				personId = emplDAO.loadPersonID(aeDocument.getEmployee().getUIN(), ownerId);
			}
			if(personId != null) {
				Person person = (Person) personDAO.load(Person.lazyDescriptor(personId));
				if(person != null) {
					aeDocument.getEmployee().setPerson(person);
				}
			}

			// reset as new
			aeDocument.resetAsNew();

			// save in MonEntreprise
			invContext.setProperty(AEInvocationContext.AEConnection, connMonEntreprise);
			insert(aeDocument, true, invContext);
		}

		int i = docList.size();
		System.out.print(i);
	}

	/**
	 * Validate whether the employee already has existing contract.
	 * Validation should be executed only when we have a new employee with existing person
	 * 
	 * @param contractDeTravail
	 * @param invContext
	 * @param aeConnection
	 * @throws AEException
	 * @throws AEWarning
	 */
	private void validateExistingContract(ContractDeTravail contractDeTravail, AEInvocationContext invContext, AEConnection aeConnection) throws AEException, AEWarning {
		try {
			Employee empl = contractDeTravail.getEmployee();
			if(empl != null && !empl.isPersistent() 
					&& empl.getPerson() != null && empl.getPerson().getDescriptor().isPersistent()) {

				// connection
				DAOFactory daoFactory = DAOFactory.getInstance();
				CotractDeTravailDAO contractDAO = daoFactory.getCotractDeTravailDAO(aeConnection);

				// validate
				contractDAO.validateExistingContract(empl);
			}
		} catch (AEWarning w) {
			throw w;
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(aeConnection);
		}
	}

	/**
	 * Validation should be executed only when we have a new employee with existing person
	 * 
	 * @param contractDeTravail
	 * @param invContext
	 * @param aeConnection
	 * @throws AEException
	 * @throws AEWarning
	 */
	private void validateCDDDelayContract(ContractDeTravail contractDeTravail, AEInvocationContext invContext, AEConnection aeConnection) throws AEWarning, AEException {
		try {
			Employee empl = contractDeTravail.getEmployee();
			if(empl != null && !empl.isPersistent() 
					&& empl.getPerson() != null && empl.getPerson().getDescriptor().isPersistent()) {

				// only for CDD Surcroit or Saisonnier
				if(contractDeTravail.getSubType().getSystem() == ContractDeTravailType.System.TEMPORARY_PEAK
						|| contractDeTravail.getSubType().getSystem() == ContractDeTravailType.System.TEMPORARY_SEASONAL) {

					// get last CDD Surcroit or Saisonnier contract
					DAOFactory daoFactory = DAOFactory.getInstance();
					CotractDeTravailDAO contractDAO = daoFactory.getCotractDeTravailDAO(aeConnection);
					AETimePeriod cddPeriod = contractDAO.getLastCDDPeriod(contractDeTravail);

					// validate
					if(cddPeriod != null) {
						long cddPeriodDuration =  AEDateUtil.getDaysDiffSign(cddPeriod.getStartDate(), cddPeriod.getEndDate()) + 1;
						Date entryPoint = contractDeTravail.getDateOfEntry();
						long delay = AEDateUtil.getDaysDiffSign(cddPeriod.getEndDate(), entryPoint) - 1;

						double limit = 0.0;
						if(cddPeriodDuration > 14) {
							limit = (double) cddPeriodDuration / 3;
						} else {
							limit = (double) cddPeriodDuration / 2;
						}
						if(delay < limit) {
							throw AEError.System.SOCIAL_WARNING_CDD_CONTRACT_DELAY.toWarning();
						}
					}
				}
			}
		} catch (AEWarning w) {
			throw w;
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(aeConnection);
		}
	}

	@Override
	public SocialTimeSheet loadTemplateTimeSheet(long employeeId, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Factory and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * Load template time sheet
			 */
			SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);
			SocialTimeSheet timeSheet = new SocialTimeSheet(Employee.lazyDescriptor(employeeId), SocialPeriodType.TEMPLATE);
			socialDAO.loadTimeSheetTemplate(timeSheet);

			/**
			 * Calculate number of weeks
			 */
			Map<Integer, List<SocialTimeSheetEntry>> weeks = timeSheet.toWeeks();
			if(weeks != null && !weeks.isEmpty()) {
				timeSheet.setNumberOfWeeks(weeks.keySet().size());
			}

			return timeSheet;
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
			invContext.setProperty(AEInvocationContext.AEConnection, null);
		}
	}

	@Override
	public void eliorFtpTransfer(AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		BufferedWriter writer = null;
		FTPClient ftpClient = null;
		try {
			logger().debug("eliorFtpTransfer start ...");

			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			// the documents that should be posted
			SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);
			AEDocumentsList socDocsList = socialDAO.loadDocsToTransfer();

			// documents that are posted 
			List<AEDescriptor> ftpPosted = new ArrayList<AEDescriptor>();

			// group by owner_id
			Map<AEDescriptor, AEDocumentsList> socDocsMap = new HashMap<AEDescriptor, AEDocumentsList>();
			for (AEDocument socDoc : socDocsList) {
				AEDescriptor ownerDescr = socDoc.getCompany().getDescriptor();
				AEDocumentsList concreteDocsList = socDocsMap.get(ownerDescr);
				if(concreteDocsList == null) {
					concreteDocsList = new AEDocumentsList();
					socDocsMap.put(ownerDescr, concreteDocsList);
				}
				concreteDocsList.add(socDoc);
			}

			// file and writer
			String fileName = "me_" + AEDateUtil.convertToString(new Date(), "yyyyMMdd_HHmmss") + ".csv";
			File file = new File(
					AEApp.getInstance().getProperties().getProperty(AEApp.ftpFolder), 
					fileName);
			FileOutputStream fos = new FileOutputStream(file); 
			writer = new BufferedWriter(new OutputStreamWriter(fos, Charset.forName("UTF-8")), 2048);

			// fill in file`
			Set<AEDescriptor> ownerDescSet = socDocsMap.keySet();
			for (AEDescriptor ownerDescr : ownerDescSet) {

				// customer
				JSONObject customer = null;

				JSONObject customerArguments = new JSONObject();
				customerArguments.put("customerId", ownerDescr.getID());
				customerArguments.put("doNotLoadCAO", true);
				AERequest customerRequest = new AERequest(customerArguments);
				AEResponse customerResponse = partyService.loadCustomers(customerRequest, invContext);
				if(customerResponse.getPayload() != null) {
					JSONObject payload = customerResponse.getPayload();
					JSONArray customersArray = payload.optJSONArray("customers");
					if(customersArray.length() > 0) {
						customer = customersArray.getJSONObject(0);
					}
				}

				// social info
				SocialInfo socialInfo = socialDAO.loadSocialInfo(ownerDescr);

				// documents
				AEDocumentsList concreteDocsList = socDocsMap.get(ownerDescr);
				for (AEDocument aeDocument : concreteDocsList) {
					AEDocument doc = null;
					try {
						doc = load((int) aeDocument.getType().getSystemID(), aeDocument.getID(), localConnection);
					} catch (AEException e) {
						AEApp.logger().error("The docId = " + Long.toString(aeDocument.getID()) + " cannot be loaded", e);
						continue;
					}
					AESocialDocument socDoc = (AESocialDocument) doc;

					// Employee
					Employee empl = socDoc.getEmployee();
					if(empl.getFtpId() == null) {
						continue;
					}

					ContractDeTravail contract = null;
					if(!(doc instanceof ContractDeTravail)) {
						CotractDeTravailDAO contractDAO = daoFactory.getCotractDeTravailDAO(localConnection);
						contract = (ContractDeTravail) contractDAO.loadByEmployee(empl.getDescriptor());
					} else {
						contract = (ContractDeTravail) doc;
					}

					// Matricule: To be developed: for each employee we need a register number. 
					// If one employee has several contracts the number remains the same. 
					// For the new employees number is equal to last given register number +1. 
					// We (social collaborators) also need to be able to modify those register numbers, 
					// for we have employees that have not such.  (Chain 100 characters).
					StringAlign strAlign = new StringAlign(100, StringAlign.Align.LEFT);
					if(empl.getFtpId() != null) {
						writer.write(strAlign.format(Long.toString(empl.getFtpId())));
					} else {
						writer.write(strAlign.format(AEStringUtil.EMPTY_STRING));
					}
					writer.write(";");

					// Relation contractuelle
					// strAlign = new StringAlign(100, StringAlign.Align.RIGHT);
					writer.write(strAlign.format(AEStringUtil.EMPTY_STRING));
					writer.write(";");

					// Name  (Chain 100 characters).
					// strAlign = new StringAlign(100, StringAlign.Align.RIGHT);
					writer.write(strAlign.format(empl.getLastName()));
					writer.write(";");

					// First name (Chain 100 characters).
					// strAlign = new StringAlign(100, StringAlign.Align.RIGHT);
					writer.write(strAlign.format(empl.getFirstName()));
					writer.write(";");

					// Salutation (Chain 100 characters).
					String salutation = null;
					switch (empl.getSalutation()) {
					case Mr: {
						salutation = "Monsieur";
						break;
					}
					case Mrs: {
						salutation = "Madame";
						break;
					}
					case Ms: {
						salutation = "Mademoiselle";
						break;
					} 
					default : {
						salutation = AEStringUtil.EMPTY_STRING;
						break;
					}
					}
					writer.write(strAlign.format(salutation));
					writer.write(";");

					// blocage
					Date date = null;
					boolean one = false;
					if(AEDocumentType.System.AccidentDuTravail.getSystemID() == doc.getType().getSystemID()) {
						one = true;
						AccidentDuTravail sDoc = (AccidentDuTravail) socDoc;
						date = sDoc.getDateDuDernier();
					} else if(AEDocumentType.System.ArretDeTravail.getSystemID() == doc.getType().getSystemID()) {
						one = true;
						AccidentDuTravail sDoc = (AccidentDuTravail) socDoc;
						date = sDoc.getDateDuDernier();
					} else if(AEDocumentType.System.FinDuTravail.getSystemID() == doc.getType().getSystemID()) {
						one = true;
						FinDuTravail sDoc = (FinDuTravail) socDoc;
						date = sDoc.getDateRelease();
					}
					if(one) {
						writer.write("1");
					} else {
						writer.write("0");
					}
					writer.write(";");

					// Date blocage
					strAlign = new StringAlign(10, StringAlign.Align.LEFT);
					if(date != null) {
						writer.write(AEDateUtil.convertToString(date, AEDateUtil.FRENCH_DATE_FORMAT));
					} else {
						writer.write(strAlign.format(AEStringUtil.EMPTY_STRING));
					}
					writer.write(";");

					// Statut
					strAlign = new StringAlign(15, StringAlign.Align.LEFT);
					String classification = null;
					if(contract != null) {
						if(EmploymentClassification.System.EMPLOYEE.equals(contract.getEmploymentClassification().getSystem())) {
							classification = "Employé";
						} else if(EmploymentClassification.System.WORKER.equals(contract.getEmploymentClassification().getSystem())) {
							classification = "Ouvrier";
						} else if(EmploymentClassification.System.FOREMAN.equals(contract.getEmploymentClassification().getSystem())) {
							classification = "Agent de Maîtrise";
						} else if(EmploymentClassification.System.CADRE.equals(contract.getEmploymentClassification().getSystem())) {
							classification = "Cadre";
						} else {
							classification = AEStringUtil.EMPTY_STRING;
						}
					} else {
						classification = AEStringUtil.EMPTY_STRING;
					}
					writer.write(strAlign.format(classification));
					writer.write(";");

					// Type contrat
					String docType = null;
					strAlign = new StringAlign(3, StringAlign.Align.LEFT);
					if(contract != null) {
						if(ContractDeTravailType.System.PERMANENT.equals(contract.getSubType())) {
							docType = "CDI";
						} else {
							docType = "CDD";
						}
					} else {
						docType = AEStringUtil.EMPTY_STRING;
					}
					writer.write(strAlign.format(docType));
					writer.write(";");

					// Age
					Date bDate = empl.getDateOfBirth();
					if(bDate != null) {
						writer.write(Integer.toString(AEDateUtil.getYearsBetween(bDate, new Date())));
					} else {
						writer.write(strAlign.format(AEStringUtil.EMPTY_STRING));
					}
					writer.write(";");

					// birthdate
					strAlign = new StringAlign(15, StringAlign.Align.LEFT);
					if(bDate != null) {
						writer.write(AEDateUtil.convertToString(bDate, AEDateUtil.FRENCH_DATE_FORMAT));
					} else {
						writer.write(strAlign.format(AEStringUtil.EMPTY_STRING));
					}
					writer.write(";");

					// Etablissement
					strAlign = new StringAlign(100, StringAlign.Align.LEFT);
					if(!AEStringUtil.isEmpty(customer.optString("name"))) {
						writer.write(strAlign.format(customer.optString("name")));
					} else {
						writer.write(strAlign.format(AEStringUtil.EMPTY_STRING));
					}
					writer.write(";");

					// societe
					// strAlign = new StringAlign(100, StringAlign.Align.LEFT);
					if(!AEStringUtil.isEmpty(socialInfo.getSociete())) {
						writer.write(strAlign.format(socialInfo.getSociete()));
					} else {
						writer.write(strAlign.format(AEStringUtil.EMPTY_STRING));
					}

					// line separator
					writer.write(System.getProperty("line.separator"));

					// add in posted list
					ftpPosted.add(doc.getDescriptor());
				}
			}

			// close the file
			try {
				if(writer != null) {
					writer.flush();
					writer.close();
					writer = null;
				}
			} catch (Exception e) {
				AEApp.logger().error("closing BufferedWriter failed: ", e);
			}

			// upload the file
			AEApp.logger().info("Start uploading file");

			String server = AEApp.getInstance().getProperties().getProperty(AEApp.ftpServer);
			int port = 21;
			try {
				port = Integer.parseInt(AEApp.getInstance().getProperties().getProperty(AEApp.ftpPort));
			} catch (Exception e) {
				AEApp.logger().error(e);
			}
			String remoteFolder = AEApp.getInstance().getProperties().getProperty(AEApp.ftpRemoteFolder);
			String user = AEApp.getInstance().getProperties().getProperty(AEApp.ftpUser);
			String pass = AEApp.getInstance().getProperties().getProperty(AEApp.ftpPass);

			ftpClient = new FTPClient();
			ftpClient.connect(server, port);
			ftpClient.login(user, pass);
			ftpClient.enterLocalPassiveMode();
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			if(!AEStringUtil.isEmpty(remoteFolder)) {
				ftpClient.changeWorkingDirectory(remoteFolder);
			}

			InputStream inputStream = new FileInputStream(file);
			boolean done = ftpClient.storeFile(fileName, inputStream);
			inputStream.close();
			try {
				ftpClient.disconnect();
				ftpClient = null;
			} catch (IOException e) {
				AEApp.logger().error("disconnecting ftpClient failed: ", e);
			}

			if (done) {
				AEApp.logger().info("The file is uploaded successfully.");

				// update documets as ftp posted 
				localConnection.beginTransaction();
				socialDAO.updateFtpPosted(ftpPosted, true, fileName);
				localConnection.commit();
			}
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException("eliorFtpTransfer failed", t);
		} finally {
			AEConnection.close(localConnection);

			// paranoic writer
			try {
				if(writer != null) {
					writer.flush();
					writer.close();
					writer = null;
				}
			} catch (Exception e) {
				AEApp.logger().error("closing BufferedWriter failed: ", e);
			}

			// paranoic ftp client
			if(ftpClient != null) {
				try {
					ftpClient.disconnect();
				} catch (IOException e) {
					AEApp.logger().error("disconnecting ftpClient failed: ", e);
				}
			}
		}
	}

	@Override
	public AEResponse loadUniqEmployees(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			JSONObject arguments = aeRequest.getArguments();
			long sOwnerId = arguments.getLong("sOwnerId");
			long ownerId = arguments.getLong("ownerId");
			AEDescriptor ownerDescr = Organization.lazyDescriptor(arguments.getLong("ownerId")); 

			// whether this user is ownered by this customer
			ap.ownershipValidator(sOwnerId);

			// whether this article is ownered by this customer
			if(sOwnerId != ownerId) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();

			/**
			 * Connection
			 */
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * Load uniq employees
			 */
			EmployeeDAO employeeDAO = daoFactory.getEmployeeDAO(localConnection);
			EmployeeList employeeList = employeeDAO.loadToCompanyUniq(ownerDescr);

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject();
			payload.put(Employee.JSONKey.employees, employeeList.toJSONArray());
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse saveEmployeeFtpId(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			JSONObject arguments = aeRequest.getArguments();
			long sOwnerId = arguments.getLong("sOwnerId");
			long ownerId = arguments.getLong("ownerId");

			// whether this user is ownered by this customer
			ap.ownershipValidator(sOwnerId);

			// whether this article is ownered by this customer
			if(sOwnerId != ownerId) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();

			/**
			 * Connection
			 */
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			JSONArray emplJsonArray = arguments.getJSONArray(Employee.JSONKey.employees);
			EmployeeList emplList = new EmployeeList();
			emplList.create(emplJsonArray);

			/**
			 * Start DB operation
			 */
			localConnection.beginTransaction();
			EmployeeDAO emplDAO = daoFactory.getEmployeeDAO(localConnection);
			for (Employee employee : emplList) {
				switch(employee.getPersistentState()) {
				case UPDATED:
					emplDAO.updateFtpId(employee);
					employee.setView();
					break;
				default:
					break;
				}	
			}

			// commit
			localConnection.commit();

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject();
			payload.put(Employee.JSONKey.employees, emplList.toJSONArray());
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			AEApp.logger().error("saveEmployeeFtpId failed with: ", e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse generateEmplFtpId(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			JSONObject arguments = aeRequest.getArguments();
			long sOwnerId = arguments.getLong("sOwnerId");
			long ownerId = arguments.getLong("ownerId");

			// whether this user is ownered by this customer
			ap.ownershipValidator(sOwnerId);

			// whether this article is ownered by this customer
			if(sOwnerId != ownerId) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();

			/**
			 * Connection
			 */
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			// generate FtpId
			Long ftpId = partyService.generateEmplFtpId(
					Organization.lazyDescriptor(ownerId), 
					invContext, 
					localConnection);

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject();
			if(ftpId != null) {
				payload.put(Employee.JSONKey.ftpId, ftpId);
			}
			return new AEResponse(payload);
		} catch (Exception e) {
			AEApp.logger().error("saveEmployeeFtpId failed with: ", e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	/**
	 * 
	 */
	public AEResponse loadDailyPlanning(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			long sOwnerId = aeRequest.getArguments().optLong("sOwnerId");
			ap.ownershipValidator(sOwnerId);

			/**
			 * Extract arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			JSONObject dayJson = arguments.getJSONObject(SocialTimeSheet.JsonKey.day.toString());

			long ownerId = arguments.getLong(SocialTimeSheetEntry.JsonKey.ownerId.toString());
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);

			long employeeId = dayJson.optLong(SocialTimeSheetEntry.JsonKey.employeeId.toString(), -1);
			AEDescriptor emplDescr = null;
			if(employeeId > 0) {
				emplDescr = Employee.lazyDescriptor(employeeId);
			}

			int periodTypeId = dayJson.getInt(SocialTimeSheetEntry.JsonKey.periodType.toString());
			SocialPeriodType periodType = SocialPeriodType.valueOf(periodTypeId);

			String forDateString = dayJson.getString(SocialTimeSheetEntry.JsonKey.date.name());
			Date forDate = AEDateUtil.parseDateStrict(forDateString);

			int weekNumber = dayJson.getInt(SocialTimeSheetEntry.JsonKey.weekNumber.toString());

			int dayOfWeek = dayJson.getInt(SocialTimeSheetEntry.JsonKey.dayOfWeek.toString());;

			/**
			 * Validate arguments
			 */
			if(ownerId != sOwnerId) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}

			if(!SocialPeriodType.TEMPLATE.equals(periodType) && !SocialPeriodType.PLAN.equals(periodType)) {
				throw new AEException("Invocation Exception: invalid periodType ");
			}

			/**
			 * Factory and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();

			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			// load employees
			EmployeeDAO employeeDAO = daoFactory.getEmployeeDAO(localConnection);
			EmployeeList emplList = employeeDAO.loadCurrentEmployeesLazzy(ownerDescr);
			if(emplDescr != null) {
				for (Iterator<Employee> iterator = emplList.iterator(); iterator.hasNext();) {
					Employee employee = (Employee) iterator.next();
					if(emplDescr.equals(employee.getDescriptor())) {
						iterator.remove();
					}
				}
			}

			/**
			 * Load multiemployee time sheet
			 */
			SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);
			SocialTimeSheet timeSheet = null;
			if(SocialPeriodType.TEMPLATE.equals(periodType)) {
				timeSheet = socialDAO.loadMultiEmployeesTimeSheet(periodType, weekNumber, dayOfWeek, emplList);
			} else if(SocialPeriodType.PLAN.equals(periodType)) {
				timeSheet = socialDAO.loadMultiEmployeesTimeSheet(periodType, forDate, emplList);
			}

			JSONObject payload = new JSONObject();
			payload.put(SocialTimeSheet.JsonKey.timeSheet.toString(), timeSheet.toJSONArray());
			return new AEResponse(payload);
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse reportPeriodPlanning(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			long sOwnerId = aeRequest.getArguments().optLong("sOwnerId");
			ap.ownershipValidator(sOwnerId);

			/**
			 * Extract arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();

			long ownerId = arguments.getLong(SocialTimeSheetEntry.JsonKey.ownerId.toString());

			int periodTypeId = arguments.getInt(SocialTimeSheetEntry.JsonKey.periodType.toString());
			SocialPeriodType periodType = SocialPeriodType.valueOf(periodTypeId);

			Date today = AEDateUtil.getClearDateTime(new Date());
			
			String fromDateString = arguments.optString(SocialTimeSheet.JsonKey.fromDate.name());
			Date fromDate = AEDateUtil.parseDateStrict(fromDateString);
			if(fromDate == null) {
				fromDate = today;
			}

			String toDateString = arguments.optString(SocialTimeSheet.JsonKey.toDate.name());
			Date toDate = AEDateUtil.parseDateStrict(toDateString);
			if(toDate == null) {
				toDate = today;
			}

			// groupBy = {10, 20}
			int groupBy = arguments.getInt("groupBy");

			//			int weekNumber = dayJson.getInt(SocialTimeSheetEntry.JsonKey.weekNumber.toString());
			//			
			//			int dayOfWeek = dayJson.getInt(SocialTimeSheetEntry.JsonKey.dayOfWeek.toString());

			/**
			 * Validate arguments
			 */
			if(ownerId != sOwnerId) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}

			if(!SocialPeriodType.TEMPLATE.equals(periodType) && !SocialPeriodType.PLAN.equals(periodType)) {
				throw new AEException("Invocation Exception: invalid periodType ");
			}

			/**
			 * Factory and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();

			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * Load ownerDescr
			 */
			PartyDAO partyDAO = DAOFactory.getInstance().getOrganizationDAO(localConnection);
			AEDescriptor ownerDescr = partyDAO.loadDescriptor(ownerId);
			
			// load employees
			EmployeeDAO employeeDAO = daoFactory.getEmployeeDAO(localConnection);
			EmployeeList emplList = employeeDAO.loadCurrentEmployeesLazzy(ownerDescr);

			/**
			 * Load multiemployee time sheet
			 */
			// calculate the closed period
			Date closedFromDate = AEDateUtil.firstDayOfWeek(fromDate);
			Date closedToDate = AEDateUtil.lastDayOfWeek(toDate);
			
			SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);
			SocialTimeSheet timeSheet = null;
			if(SocialPeriodType.TEMPLATE.equals(periodType)) {
				timeSheet = socialDAO.loadMultiEmployeesTimeSheet(periodType, emplList); // createSchedule(ownerDescr, fromDate, toDate, localConnection);
			} else if(SocialPeriodType.PLAN.equals(periodType)) {
				timeSheet = socialDAO.loadMultiEmployeesTimeSheet(periodType, closedFromDate, closedToDate, emplList);
			}
			
			/**
			 * Create response
			 */
			JSONObject payload = new JSONObject();
			JSONObject periodJson = new JSONObject();
			periodJson.put("workingHoursStart", "00:00");
			periodJson.put("workingHoursEnd", "23:00");
			periodJson.put("equalHourPartsCount", 4);
			periodJson.put("type", groupBy);
			periodJson.put("customer", ownerDescr.getName());
			periodJson.put("printedBy", ap.getName());
			periodJson.put("printedAt", AEDateUtil.formatToSystem(new Date()));
			payload.put("period", periodJson);

			JSONArray weeksJsonArray = new JSONArray();
			periodJson.put("weeks", weeksJsonArray);

			// prepare weeks 
			Map<Integer, List<SocialTimeSheetEntry>> entriesGroupedByWeek = timeSheet.toWeeks();
			Set<Integer> weekNumbersSet = entriesGroupedByWeek.keySet();
			List<Integer> sortedWeekNumbersList = new ArrayList<Integer>(weekNumbersSet);
			Collections.sort(sortedWeekNumbersList);
			
			// set title using weeks min and max value
			int minWeekNumber = sortedWeekNumbersList.isEmpty() ? 0 : sortedWeekNumbersList.get(0);
			int maxWeekNumber = sortedWeekNumbersList.isEmpty() ? 0 : sortedWeekNumbersList.get(sortedWeekNumbersList.size() - 1);
			periodJson.put("title", String.format("Planning prévisionnel semaine %d sur %d", minWeekNumber, maxWeekNumber));
			
			// under weeks
			if(groupBy == 10) {
				for (Integer weekNumber : sortedWeekNumbersList) {
					List<SocialTimeSheetEntry> weekEntries = entriesGroupedByWeek.get(weekNumber);
					double weekWorkingTime = 0.0;

					// create and add weekJson
					JSONObject weekJson = new JSONObject();
					weeksJsonArray.put(weekJson);

					// build weekJson
					weekJson.put("name", "Semaine");
					weekJson.put("number", AEMath.longValue(weekNumber));
					JSONArray daysJsonArray = new JSONArray();
					weekJson.put("days", daysJsonArray);
					
					int rowSpan = 0;

					for(int i = 1; i <= 7; i++) {
						double dayWorkingTime = 0.0;

						JSONObject dayJson = new JSONObject();
						daysJsonArray.put(dayJson);

						dayJson.put("name", TimeSheetPrintUtils.daysInFrench[i - 1]);
						Date date = null;

						JSONArray employeesJsonArray = new JSONArray();
						dayJson.put("employees", employeesJsonArray);

						List<SocialTimeSheetEntry> forThisDay = SocialTimeSheet.filterByDay(weekEntries, i); 
						
						//NIKI
						if (forThisDay.size() == 0) { //if there are no employees for that day
							//create a dummy/empty employee
							JSONObject dummy = new JSONObject();
							employeesJsonArray.put(dummy);
							
							dummy.put("isEmpty", true);
							
							rowSpan++;
						}
						
						for (SocialTimeSheetEntry tsEntry : forThisDay) {
							// create and add tsEntry
							JSONObject tsEntryJson = new JSONObject();
							employeesJsonArray.put(tsEntryJson);

							date = tsEntry.getDate();

							// name
							if(tsEntry.getEmployee() != null) {
								tsEntryJson.put("name", tsEntry.getEmployee().getDescriptor().getName());
							} else {
								tsEntryJson.put("name", AEStringUtil.EMPTY_STRING);
							}

							// workingTime
							double emplWorkingTime = AEMath.doubleValue(tsEntry.getWorkingHours());
							tsEntryJson.put("workingTime", AEMath.toPrice2String(emplWorkingTime));

							// process summary working time
							weekWorkingTime += emplWorkingTime;
							dayWorkingTime += emplWorkingTime;

							// timeWorked as Json array
							JSONArray workedPeriodsArray = new JSONArray();
							tsEntryJson.put("timeWorked", workedPeriodsArray);

							// period1
							AETimePeriod period1 = new AETimePeriod(tsEntry.getFromTime_1(), tsEntry.getToTime_1());
							if(period1.isValidAndNotNull()) {
								JSONObject period1Json = new JSONObject();

								period1Json.put("start", AEDateUtil.formatTimeToSystem(tsEntry.getFromTime_1()));
								period1Json.put("end", AEDateUtil.formatTimeToSystem(tsEntry.getToTime_1()));

								workedPeriodsArray.put(period1Json);
							}

							// period2
							AETimePeriod period2 = new AETimePeriod(tsEntry.getFromTime_2(), tsEntry.getToTime_2());
							if(period2.isValidAndNotNull()) {
								JSONObject period2Json = new JSONObject();

								period2Json.put("start", AEDateUtil.formatTimeToSystem(tsEntry.getFromTime_2()));
								period2Json.put("end", AEDateUtil.formatTimeToSystem(tsEntry.getToTime_2()));

								workedPeriodsArray.put(period2Json);
							}
							
							rowSpan++;
						}

//						if(date != null) {
							dayJson.put("date", AEDateUtil.format(date, AEDateUtil.FRENCH_DATE_FORMAT));
//						} else {
//						dayJson.put("date", AEStringUtil.EMPTY_STRING);
//						}
						dayJson.put("workingTime", AEMath.toPrice2String(dayWorkingTime));
					}

					weekJson.put("workingTime", AEMath.toPrice2String(weekWorkingTime));
					weekJson.put("rowspan", rowSpan);
				}
			} else if(groupBy == 20) {
				for (Integer weekNumber : sortedWeekNumbersList) {
					List<SocialTimeSheetEntry> weekEntries = entriesGroupedByWeek.get(weekNumber);
					double weekWorkingTime = 0.0;
					
					int rowSpan = 0;

					// create and add weekJson
					JSONObject weekJson = new JSONObject();
					weeksJsonArray.put(weekJson);

					// build weekJson
					weekJson.put("name", "Semaine");
					weekJson.put("number", AEMath.longValue(weekNumber));
					JSONArray employeesInWeekJsonArray = new JSONArray();
					weekJson.put("employees", employeesInWeekJsonArray);

					Map<AEDescriptor, List<SocialTimeSheetEntry>> employeesInWeekMap = SocialTimeSheet.toEmployees(weekEntries);
					Set<AEDescriptor> employeesInWeekSet = employeesInWeekMap.keySet();
					for (AEDescriptor emplInWeek : employeesInWeekSet) {
						List<SocialTimeSheetEntry> employeeInWeekEntries = employeesInWeekMap.get(emplInWeek);
						double emplInWekWorkingTime = 0.0;
						
						// create and add emplJson
						JSONObject emplJson = new JSONObject();
						employeesInWeekJsonArray.put(emplJson);

						// build emplJson
						emplJson.put("name", emplInWeek.getName());
						JSONArray daysJsonArray = new JSONArray();
						emplJson.put("days", daysJsonArray);
						
						for(int i = 1; i <= 7; i++) {
							JSONObject dayJson = new JSONObject();
							daysJsonArray.put(dayJson);

							dayJson.put("name", TimeSheetPrintUtils.daysInFrench[i - 1]);

							List<SocialTimeSheetEntry> forThisDay = SocialTimeSheet.filterByDay(employeeInWeekEntries, i); 
							
							rowSpan++;
							
							for (SocialTimeSheetEntry tsEntry : forThisDay) {

								Date date = tsEntry.getDate();
								if(date != null) {
									dayJson.put("date", AEDateUtil.format(date, AEDateUtil.FRENCH_DATE_FORMAT));
								} else {
									dayJson.put("date", AEStringUtil.EMPTY_STRING);
								}

								// workingTime
								double dayWorkingTime = AEMath.doubleValue(tsEntry.getWorkingHours());
								dayJson.put("workingTime", AEMath.toPrice2String(dayWorkingTime));

								// process summary working time
								weekWorkingTime += dayWorkingTime;
								emplInWekWorkingTime += dayWorkingTime;

								// timeWorked as Json array
								JSONArray workedPeriodsArray = new JSONArray();
								dayJson.put("timeWorked", workedPeriodsArray);

								// period1
								AETimePeriod period1 = new AETimePeriod(tsEntry.getFromTime_1(), tsEntry.getToTime_1());
								if(period1.isValidAndNotNull()) {
									JSONObject period1Json = new JSONObject();

									period1Json.put("start", AEDateUtil.formatTimeToSystem(tsEntry.getFromTime_1()));
									period1Json.put("end", AEDateUtil.formatTimeToSystem(tsEntry.getToTime_1()));

									workedPeriodsArray.put(period1Json);
								}

								// period2
								AETimePeriod period2 = new AETimePeriod(tsEntry.getFromTime_2(), tsEntry.getToTime_2());
								if(period2.isValidAndNotNull()) {
									JSONObject period2Json = new JSONObject();

									period2Json.put("start", AEDateUtil.formatTimeToSystem(tsEntry.getFromTime_2()));
									period2Json.put("end", AEDateUtil.formatTimeToSystem(tsEntry.getToTime_2()));

									workedPeriodsArray.put(period2Json);
								}
								
								//rowSpan++;
							}


							emplJson.put("workingTime", AEMath.toPrice2String(emplInWekWorkingTime));
						}
					}
					weekJson.put("workingTime", AEMath.toPrice2String(weekWorkingTime));
					
					weekJson.put("rowspan", rowSpan);
				}
			}

			AEApp.logger().debug(payload);
			return new AEResponse(payload);
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse importSchedule(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			super.commonValidation(aeRequest, invContext);
			
			/**
			 * Extract and validate arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();

			// ownerId and sOwnerId
			AEDescriptor ownerDescr = Organization.lazyDescriptor(arguments.getLong("ownerId")); 
			long sOwnerId = arguments.getLong("sOwnerId");
			if(sOwnerId != ownerDescr.getID()) {
				throw AEError.System.SECURITY_VIOLATION.toException();
			}
			
			// periodTypeId
			int periodTypeId = arguments.getInt(SocialTimeSheet.JsonKey.periodType.toString());
			SocialPeriodType periodType = SocialPeriodType.valueOf(periodTypeId);
			if(!SocialPeriodType.PLAN.equals(periodType)) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}
			
			// employeeId
			long employeeId = arguments.getLong(SocialTimeSheet.JsonKey.employeeId.toString());
			AEDescriptor employeeDescr = Employee.lazyDescriptor(employeeId);
			
			// month
			// by convention, month is ONE based
			int month = arguments.getInt(SocialTimeSheet.JsonKey.month.name()) - 1;
			if(month < 0 || month > 11) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}

			// year
			int year = arguments.getInt(SocialTimeSheet.JsonKey.year.name());
			
			/**
			 * Determine and validate the period
			 */
			
			/**
			 * Factory and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			// the period (month, year) cannot be closed
			AccPeriod accPeriod = getAccPeriod(
					ownerDescr.getID(), 
					AEApp.SOCIAL_TIMESHEET_PLAN_MODULE_ID, 
					AEDateUtil.getFirstDate(month, year), 
					localConnection);
			if(accPeriod.isClosed()) {
				throw AEError.System.SOCIAL_INVALID_SCHEDULE_PERIOD.toException();
			}
			
			// validate period
			Date startDate = AEDateUtil.firstDayOfWeek(AEDateUtil.getFirstDate(month, year));
			Date endDate = AEDateUtil.lastDayOfWeek(AEDateUtil.getLastDate(month, year));
			if(endDate.before(startDate)) {
				throw AEError.System.SOCIAL_INVALID_SCHEDULE_PERIOD.toException();
			}
			
			/**
			 * DAOs
			 */
			SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);
			EmployeeDAO employeeDAO = daoFactory.getEmployeeDAO(localConnection);
			
			// load and validate employee (in case of real timesheet there is no employee)
			Employee employee = employeeDAO.load(employeeDescr);
			if(employee != null) {
				if(employee.getCompany().getDescriptor().getID() != sOwnerId) {
					throw AEError.System.SECURITY_VIOLATION.toException();
				}
			} else {
				throw AEError.System.SECURITY_VIOLATION.toException();
			}
			
			/**
			 * Generate schedule for specified employee for specified period
			 */

			// load template time sheet for specified employee
			// the template usually should be already created during Contract de travail creation
			Date templateStartDate = getTemplateStartDate(employee, localConnection);

			Integer numberOfWeeks = employee.getNumberOfWeeks();
			if(numberOfWeeks == null) {
				numberOfWeeks = 0;
			}

			SocialTimeSheet templateTimeSheet = 
					new SocialTimeSheet(employee, SocialPeriodType.TEMPLATE, templateStartDate, numberOfWeeks);	
			socialDAO.loadTimeSheetTemplate(templateTimeSheet);	

			if(templateTimeSheet.isEmpty()) {
				templateTimeSheet.createTemplate();
			}

			// create plan
			Date periodStartDate = startDate;
			while(templateStartDate.after(periodStartDate)) {
				periodStartDate = AEDateUtil.addDaysToDate(periodStartDate, 1);
			}
			if(AEDateUtil.getDayOfWeek(periodStartDate) != AEDateUtil.getEuropeanCalendar().getFirstDayOfWeek()) {
				throw new AEException("Internal error: periodStartDate is not Monday");
			}

			SocialTimeSheet planTimeSheet = 
					new SocialTimeSheet(employee, SocialPeriodType.PLAN, periodStartDate);

			long weeksDiff = AEDateUtil.getWeeksDiffSign(templateStartDate, periodStartDate);
			long startWeek = 1;
			if(numberOfWeeks > 1) {
				startWeek = (weeksDiff % numberOfWeeks) + 1;
			}

			// the periodStartDate should have startWeek template
			Date currentMonday = periodStartDate;
			long currentWeek = startWeek;
			while(currentMonday.before(endDate)) {
				// create week
				Map<Integer, SocialTimeSheetEntry> week = templateTimeSheet.getWeek(currentWeek);
				Set<Integer> days = week.keySet();
				for (Iterator<Integer> iterator2 = days.iterator(); iterator2.hasNext();) {
					Integer dayOfWeek = (Integer) iterator2.next();
					SocialTimeSheetEntry templateEntry = week.get(dayOfWeek);
					if(templateEntry != null) {
						SocialTimeSheetEntry entry = (SocialTimeSheetEntry) AEObjectUtil.deepCopy(templateEntry);

						entry.resetAsNew();

						// check the date

						Date currentDate = null;
						if(dayOfWeek == SocialTimeSheet.Weekday.SUM.ordinal()) {
							currentDate = AEDateUtil.addDaysToDate(currentMonday, dayOfWeek - 2);
						} else {
							currentDate = AEDateUtil.addDaysToDate(currentMonday, dayOfWeek - 1);
						}

						if(employee.getDateRelease() != null) {
							if(!currentDate.before(employee.getDateRelease())) {
								continue;
							}
						}
						entry.setDate(currentDate);
						planTimeSheet.create(entry);

						planTimeSheet.add(entry);
					}
				}

				// next week
				currentMonday = AEDateUtil.addWeeksToDate(currentMonday, 1);
				currentWeek++;
				if(currentWeek > numberOfWeeks) {
					currentWeek = 1;
				}
			}
			
			// validate schedule
			SocialTimeSheetEntryPeriodsValidator periodValidator = SocialTimeSheetEntryPeriodsValidator.getInstance();
			for (Iterator<SocialTimeSheetEntry> iterator = planTimeSheet.iterator(); iterator.hasNext();) {
				SocialTimeSheetEntry entry = (SocialTimeSheetEntry) iterator.next();
				if(entry.getPersistentState() != AEPersistent.State.DELETED) {
					periodValidator.validate(entry);
				}
			}

			// Begin transaction
			localConnection.beginTransaction();
			
			// Insert or Update if not exists
			socialDAO.insertOrUpdatePlanIfExists(planTimeSheet);
			
			// Commit	
			localConnection.commit();

			/**
			 * Return response
			 */
			JSONObject payload = new JSONObject();
			JSONObject periodJson = planTimeSheet.toJSONObject();
			payload.put(SocialTimeSheet.JsonKey.period.toString(), periodJson);
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public AEResponse importScheduleExt(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// common validation
			super.commonValidation(aeRequest, invContext);
			
			/**
			 * Extract and validate arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();

			// ownerId and sOwnerId
			AEDescriptor ownerDescr = Organization.lazyDescriptor(arguments.getLong("ownerId")); 
			long sOwnerId = arguments.getLong("sOwnerId");
			if(sOwnerId != ownerDescr.getID()) {
				throw AEError.System.SECURITY_VIOLATION.toException();
			}
			
			// periodTypeId
			int periodTypeId = arguments.getInt(SocialTimeSheet.JsonKey.periodType.toString());
			SocialPeriodType periodType = SocialPeriodType.valueOf(periodTypeId);
			if(!SocialPeriodType.PLAN.equals(periodType)) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}
			
			// employeeId
			long employeeId = arguments.getLong(SocialTimeSheet.JsonKey.employeeId.toString());
			AEDescriptor employeeDescr = Employee.lazyDescriptor(employeeId);
			
			// fromDate
			Date fromDate = AEDateUtil.parseDateStrict(arguments.getString(SocialTimeSheet.JsonKey.fromDate.name()));
			if(fromDate == null) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}

			// toDate
			Date toDate = AEDateUtil.parseDateStrict(arguments.getString(SocialTimeSheet.JsonKey.toDate.name()));
			if(toDate == null) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}
			
			// timePeriod
			AETimePeriod period = new AETimePeriod(fromDate, toDate);
			boolean isValid = period.isValidAndNotNull();
			if(!isValid) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}
			
			// start week number and start day of week
			int startWeekNumber = arguments.getInt(SocialTimeSheet.JsonKey.startWeekIndex.name());
			if(startWeekNumber < 1 || startWeekNumber > 4) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}
			int startDayOfWeek = AEDateUtil.getDayOfWeekJoda(fromDate);
			
			// it is time for factory and connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			// check for open acc periods and insert missing
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
			Date nextDate = fromDate;
			int monthDiff = AEDateUtil.getMonthsBetween(fromDate, toDate);
			for(int i = 0; i <= monthDiff; i++) {
				// the period cannot be closed
				nextDate = AEDateUtil.addMonthsToDate(nextDate, i);
				AccPeriod accPeriod = getAccPeriod(
						ownerDescr.getID(), 
						AEApp.SOCIAL_TIMESHEET_PLAN_MODULE_ID, 
						nextDate, 
						localConnection);
				if(accPeriod == null) {
					// insert
					accPeriod = new AccPeriod();
					
					accPeriod.setCompany(ownerDescr);
					accPeriod.setModuleId(AEApp.SOCIAL_TIMESHEET_PLAN_MODULE_ID);
					accPeriod.setStartDate(AEDateUtil.firstDayOfMonth(nextDate));
					accPeriod.setEndDate(getPeriodEndDate(nextDate));
					accPeriod.setClosed(false);
					
					accPeriodDAO.insert(accPeriod);
				} else if(accPeriod.isClosed()) {
					throw AEError.System.SOCIAL_INVALID_SCHEDULE_PERIOD.toException();
				} 
			}
			
			// load and validate employee (in case of real timesheet there is no employee)
			EmployeeDAO employeeDAO = daoFactory.getEmployeeDAO(localConnection);
			Employee employee = employeeDAO.load(employeeDescr);
			if(employee != null) {
				if(employee.getCompany().getDescriptor().getID() != sOwnerId) {
					throw AEError.System.SECURITY_VIOLATION.toException();
				}
			} else {
				throw AEError.System.SECURITY_VIOLATION.toException();
			}
			
			// load and validate template time sheet
			SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);
			
			// load template time sheet for specified employee
			// the template usually should be already created during Contract de travail creation
			Date templateStartDate = getTemplateStartDate(employee, localConnection);

			Integer numberOfWeeks = employee.getNumberOfWeeks();
			if(numberOfWeeks == null) {
				// nothing to do
				return new AEResponse(new JSONObject());
			}

			SocialTimeSheet templateTimeSheet = 
					new SocialTimeSheet(employee, SocialPeriodType.TEMPLATE, templateStartDate, numberOfWeeks);	
			socialDAO.loadTimeSheetTemplate(templateTimeSheet);	
			if(templateTimeSheet.isEmpty()) {
				// nothing to do
				return new AEResponse(new JSONObject());
			}

			/**
			 * Generate schedule for specified employee for specified period
			 */
			SocialTimeSheet planTimeSheet = 
					new SocialTimeSheet(employee, SocialPeriodType.PLAN, fromDate);
			int weekNumber = startWeekNumber;
			int dayOfWeek = startDayOfWeek;
			Map<SocialTimeSheetTemplateKey, SocialTimeSheetEntry> days = templateTimeSheet.toDays();
			for (AEDateIterator iterator = new AEDateIterator(fromDate, toDate); iterator.hasNext();) {
				Date date = iterator.next();
				
				// check employee's release date
				if(employee.getDateRelease() != null) {
					if(!date.before(employee.getDateRelease())) {
						break;
					}
				}
				
				// dayEntry
				SocialTimeSheetEntry dayEntry = days.get(new SocialTimeSheetTemplateKey(weekNumber, dayOfWeek));
				if(dayEntry != null) {
					SocialTimeSheetEntry entry = (SocialTimeSheetEntry) AEObjectUtil.deepCopy(dayEntry);

					// prepare 
					entry.resetAsNew();
					entry.setDate(date);
					planTimeSheet.create(entry);

					// add
					planTimeSheet.add(entry);
				}
				
				// detect SUNDAY (end of week)
				if(dayOfWeek == SocialTimeSheet.Weekday.SUNDAY.ordinal()) {
					// create and add WEEK SUM ENTRY
					SocialTimeSheetEntry sumEntry = days.get(new SocialTimeSheetTemplateKey(weekNumber, SocialTimeSheet.Weekday.SUM.ordinal()));
					
					if(sumEntry != null) {
						SocialTimeSheetEntry entry = (SocialTimeSheetEntry) AEObjectUtil.deepCopy(sumEntry);

						entry.resetAsNew();
						entry.setDate(date);
						planTimeSheet.create(entry);

						planTimeSheet.add(entry);
					}
				}
				
				// prepare next iteration
				dayOfWeek++;
				if(dayOfWeek > SocialTimeSheet.Weekday.SUNDAY.ordinal()) {
					// prepare the first day of the next week
					
					// first day
					dayOfWeek = 1;
					
					// of the next week
					weekNumber++;
					if(weekNumber > numberOfWeeks) {
						weekNumber = 1;
					}
				}
			}
			
			// validate plan schedule
			SocialTimeSheetEntryPeriodsValidator periodValidator = SocialTimeSheetEntryPeriodsValidator.getInstance();
			for (Iterator<SocialTimeSheetEntry> iterator = planTimeSheet.iterator(); iterator.hasNext();) {
				SocialTimeSheetEntry entry = (SocialTimeSheetEntry) iterator.next();
				periodValidator.validate(entry);
			}

			// Begin transaction
			localConnection.beginTransaction();
			
			// Insert or Update if not exists
			socialDAO.insertOrUpdatePlanIfExists(planTimeSheet);
			
			// Commit	
			localConnection.commit();

			/**
			 * Return response
			 */
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
	public SocialTimeSheet reportPeriodActual(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			long sOwnerId = aeRequest.getArguments().optLong("sOwnerId");
			ap.ownershipValidator(sOwnerId);

			/**
			 * Extract arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();

			long ownerId = arguments.getLong(SocialTimeSheetEntry.JsonKey.ownerId.toString());

			int periodTypeId = arguments.getInt(SocialTimeSheetEntry.JsonKey.periodType.toString());
			SocialPeriodType periodType = SocialPeriodType.valueOf(periodTypeId);

			String fromDateString = arguments.getString(SocialTimeSheet.JsonKey.fromDate.name());
			Date fromDate = AEDateUtil.parseDateStrict(fromDateString);

			String toDateString = arguments.getString(SocialTimeSheet.JsonKey.toDate.name());
			Date toDate = AEDateUtil.parseDateStrict(toDateString);
			
			Long employeeId = null;
			if(arguments.has(SocialTimeSheet.JsonKey.employeeId.name())) {
				employeeId = arguments.getLong(SocialTimeSheet.JsonKey.employeeId.name());
			}

			/**
			 * Validate arguments
			 */
			if(ownerId != sOwnerId) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}

			if(!SocialPeriodType.ACTUAL.equals(periodType)) {
				throw new AEException("Invocation Exception: invalid periodType ");
			}

			/**
			 * Factory and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();

			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * Load ownerDescr
			 */
			PartyDAO partyDAO = DAOFactory.getInstance().getOrganizationDAO(localConnection);
			AEDescriptor ownerDescr = partyDAO.loadDescriptor(ownerId);
			
			// load employees
			EmployeeList emplList = null;
			EmployeeDAO employeeDAO = daoFactory.getEmployeeDAO(localConnection);
			if(employeeId != null) {
				Employee empl = employeeDAO.load(Employee.lazyDescriptor(employeeId));
				emplList = new EmployeeList();
				emplList.add(empl);
			} else {
				emplList = employeeDAO.loadCurrentEmployeesLazzy(ownerDescr);
			}

			/**
			 * Load multiemployee time sheet
			 */
			// calculate the closed period
			Date closedFromDate = fromDate; //AEDateUtil.firstDayOfWeek(fromDate);
			Date closedToDate = toDate; //AEDateUtil.lastDayOfWeek(toDate);
			
			// load
			SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);
			SocialTimeSheet timeSheet = socialDAO.loadMultiEmployeesTimeSheet(SocialPeriodType.PLAN, closedFromDate, closedToDate, emplList);
			
			// additional initiallization
			timeSheet.setOwner(ownerDescr);
			timeSheet.setStartDate(closedFromDate);
			timeSheet.setEndDate(closedToDate);
			
			return timeSheet;
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public AEResponse updateTimeSheetEntryActual(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			long sOwnerId = aeRequest.getArguments().optLong("sOwnerId");
			ap.ownershipValidator(sOwnerId);

			/**
			 * Extract arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();

			long ownerId = arguments.getLong(SocialTimeSheetEntry.JsonKey.ownerId.toString());

			SocialTimeSheetEntry entry = new SocialTimeSheetEntry();
			entry.create(arguments.getJSONObject("timeSheetEntry"));
			
			// SocialTimeSheet save validation
			SocialTimeSheetEntryPeriodsValidator periodValidator = SocialTimeSheetEntryPeriodsValidator.getInstance();
			periodValidator.validate(entry);
			
			// set validate to true
			entry.setActualValidated(true);

			/**
			 * Validate arguments
			 */
			if(ownerId != sOwnerId) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}

			/**
			 * Factory and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();

			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			// update
			SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);
			AccPeriodDAO periodDAO = daoFactory.getAccPeriodDAO(localConnection);
			
			if (periodDAO.isInClosedPeriod(entry.getDate(), ownerId, AEApp.SOCIAL_TIMESHEET_ACTUAL_MODULE_ID)) throw new AEException("Period is already closed!");
			socialDAO.updateSocialTimeSheetEntryActual(entry);
			
			JSONObject payload = new JSONObject();
			payload.put("timeSheetEntry", entry.toJSONObject());
			
			return new AEResponse(payload);
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public AEResponse updateDailyTimeSheetActual(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			long sOwnerId = aeRequest.getArguments().optLong("sOwnerId");
			ap.ownershipValidator(sOwnerId);

			/**
			 * Extract arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();

			long ownerId = arguments.getLong(SocialTimeSheetEntry.JsonKey.ownerId.toString());
			
			SocialTimeSheet timeSheet = new SocialTimeSheet();

			
			timeSheet.create(arguments.getJSONArray("timeSheetEntries"));
			
			// synch hours
			timeSheet.synchHours();

			/**
			 * Validate arguments
			 */
			if(ownerId != sOwnerId) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}

			/**
			 * Factory and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();

			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			// update
			SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);
			AccPeriodDAO periodDAO = daoFactory.getAccPeriodDAO(localConnection);
			
			localConnection.beginTransaction();
			
			// SocialTimeSheet save validation
			SocialTimeSheetEntryPeriodsValidator periodValidator = SocialTimeSheetEntryPeriodsValidator.getInstance();
			for (Iterator<SocialTimeSheetEntry> iterator = timeSheet.iterator(); iterator.hasNext();) {
				SocialTimeSheetEntry entry = (SocialTimeSheetEntry) iterator.next();
				//
				if (periodDAO.isInClosedPeriod(entry.getDate(), ownerId, AEApp.SOCIAL_TIMESHEET_ACTUAL_MODULE_ID)) throw new AEException("Period is already closed!");
				
				//if(entry.getPersistentState() != AEPersistent.State.DELETED) {
				periodValidator.validate(entry);
				//}
				// set validated to true
				entry.setActualValidated(true);
			
				// update
				socialDAO.updateSocialTimeSheetEntryActual(entry);
			}
			
			localConnection.commit();
			
			JSONObject payload = new JSONObject();
			payload.put("timeSheetEntries", timeSheet.toJSONArray());
			
			return new AEResponse(payload);
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
}
