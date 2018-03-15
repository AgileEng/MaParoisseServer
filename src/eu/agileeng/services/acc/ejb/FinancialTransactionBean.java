package eu.agileeng.services.acc.ejb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.io.FilenameUtils;
import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONObject;
import org.jboss.logging.Logger;

import eu.agileeng.accbureau.AEApp;
import eu.agileeng.accbureau.AEAppModule;
import eu.agileeng.accbureau.AEAppModulesList;
import eu.agileeng.accbureau.AppModuleTemplate;
import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDescriptorsList;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.acc.AccAccountBalance;
import eu.agileeng.domain.acc.AccJournal;
import eu.agileeng.domain.acc.AccJournalEntry;
import eu.agileeng.domain.acc.AccJournalEntryCommonValidator;
import eu.agileeng.domain.acc.AccJournalEtryAlignmentValidator;
import eu.agileeng.domain.acc.AccJournalFilter;
import eu.agileeng.domain.acc.AccJournalItem;
import eu.agileeng.domain.acc.AccJournalItemCommonValidator;
import eu.agileeng.domain.acc.AccJournalItemsList;
import eu.agileeng.domain.acc.AccJournalResult;
import eu.agileeng.domain.acc.AccJournalResultsList;
import eu.agileeng.domain.acc.AccPeriod;
import eu.agileeng.domain.acc.cashbasis.AccJournalEntryTemplate;
import eu.agileeng.domain.acc.cashbasis.AccJournalEntryTemplate.AccountIdentificationRule;
import eu.agileeng.domain.acc.cashbasis.AccJournalEntryTeplatesList;
import eu.agileeng.domain.acc.cashbasis.FinancialTransaction;
import eu.agileeng.domain.acc.cashbasis.FinancialTransactionList;
import eu.agileeng.domain.acc.cashbasis.FinancialTransactionTemplate;
import eu.agileeng.domain.acc.cashbasis.FinancialTransactionTemplate.PaymentMethod;
import eu.agileeng.domain.acc.cashbasis.FinancialTransactionTemplatesList;
import eu.agileeng.domain.acc.cashbasis.Quete;
import eu.agileeng.domain.acc.cashbasis.Quete.Type;
import eu.agileeng.domain.acc.cashbasis.QuetesList;
import eu.agileeng.domain.business.bank.BankAccount;
import eu.agileeng.domain.contact.Address;
import eu.agileeng.domain.contact.Contributor;
import eu.agileeng.domain.contact.ContributorDonation;
import eu.agileeng.domain.contact.ContributorDonationsList;
import eu.agileeng.domain.contact.ContributorsList;
import eu.agileeng.domain.contact.Employee;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.contact.Person;
import eu.agileeng.domain.contact.SimplePartiesList;
import eu.agileeng.domain.contact.SimpleParty;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistent;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.acc.AccJournalDAO;
import eu.agileeng.persistent.dao.acc.AccountDAO;
import eu.agileeng.persistent.dao.acc.ChartOfAccountsDAO;
import eu.agileeng.persistent.dao.acc.FinancialTransactionDAO;
import eu.agileeng.persistent.dao.app.AppDAO;
import eu.agileeng.persistent.dao.cash.BankAccountDAO;
import eu.agileeng.persistent.dao.common.ContributorDAO;
import eu.agileeng.persistent.dao.common.SimplePartyDAO;
import eu.agileeng.persistent.dao.file.FileAttachmentDAO;
import eu.agileeng.security.AuthPermission;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.services.AEBean;
import eu.agileeng.services.AEInternal;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AEInvocationContextValidator;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.cash.ejb.CashLocal;
import eu.agileeng.services.file.ejb.FileAttachmentLocal;
import eu.agileeng.services.party.ejb.PartyLocal;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEFileUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AETimePeriod;
import eu.agileeng.util.LightStringTokenizer;
import eu.agileeng.util.json.JSONUtil;

@Stateless
public class FinancialTransactionBean extends AEBean implements FinancialTransactionLocal {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2019650826934669864L;

	private static Logger logger = Logger.getLogger(FinancialTransactionBean.class);

	@EJB private CashLocal cashLocal;
	@EJB private PartyLocal partyLocal;
	@EJB private AccLocal accLocal;
	@EJB private FileAttachmentLocal fileAttachmentLocal;

	@Override
	public AEResponse loadFinancialTransactionTemplates(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();

			// module
			String appModuleCode = arguments.getString(AppModuleTemplate.JSONKey.appModuleCode.name());

			// valid on date
			Date validOnDate = null;
			if(arguments.has(AppModuleTemplate.JSONKey.validOn.name())) {
				validOnDate = AEDateUtil.parseDateStrict(arguments.getString(AppModuleTemplate.JSONKey.validOn.name()));
			}
			if(validOnDate == null) {
				validOnDate = AEDateUtil.getClearDateTime(new Date());
			}

			/**
			 * Authorize for concrete tenant
			 */
			// without authorization: this request is on application level
			authorize(new AuthPermission("System/Configuration/FinancialTransactionTemplate", AuthPermission.READ), invContext, null);

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * DAO
			 */
			AppDAO appDAO = daoFactory.getAppDAO(localConnection);

			// load app module by code
			AEAppModule appModule = appDAO.loadAppModuleByCode(appModuleCode);
			if(appModule == null) {
				throw AEError.System.INVALID_REQUEST.toException();
			}

			// load app module namePath and codePath
			AEAppModulesList modulePath = appDAO.loadAppModulePath(appModule.getCode());

			// load application module template for specified module valid on specified date
			AEDescriptor appModuleDescr = appModule.getDescriptor();
			AppModuleTemplate appModuleTemplate = appDAO.loadAppModuleTemplate(appModuleDescr, validOnDate);

			// init 
			StringBuilder appModuleNamePathBuilder = new StringBuilder();
			StringBuilder appModuleCodePathBuilder = new StringBuilder();
			for (AEAppModule module : modulePath) {
				if(!AEStringUtil.isEmpty(appModuleNamePathBuilder)) {
					appModuleNamePathBuilder.append(" - ");
				}
				appModuleNamePathBuilder.append(module.getName());

				if(!AEStringUtil.isEmpty(appModuleCodePathBuilder)) {
					appModuleCodePathBuilder.append("/");
				}
				appModuleCodePathBuilder.append(module.getCode());
			}

			// load financial transaction templates for application module template
			FinancialTransactionTemplatesList ftTemplatesList = null;			
			if(appModuleTemplate != null) {
				// load FinancialTransactionTemplates
				ftTemplatesList = loadFinancialTransactionTemplates(appModuleTemplate.getDescriptor(), localConnection);
			} else {
				appModuleTemplate = new AppModuleTemplate().withAppModule(appModuleDescr);
				ftTemplatesList = new FinancialTransactionTemplatesList();
			}
			appModuleTemplate.setAppModuleNamePath(appModuleNamePathBuilder.toString());
			appModuleTemplate.setAppModuleCodePath(appModuleCodePathBuilder.toString());

			/**
			 * Load accounts
			 */
			JSONArray accounts = loadAccounts(localConnection);

			// build response
			JSONObject appModuleTemplateJson = appModuleTemplate.toJSONObject();
			appModuleTemplateJson.put(
					FinancialTransactionTemplate.JSONKey.financialTransactionTemplates.name(), 
					ftTemplatesList.toJSONArray());

			JSONObject payload = new JSONObject();
			payload.put(AppModuleTemplate.JSONKey.appModuleTemplate.name(), appModuleTemplateJson);
			payload.put(AccAccount.JSONKey.accounts.name(), accounts);
			return new AEResponse(payload);
		} catch (Exception e) {
			logger.error(e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@AEInternal
	private FinancialTransactionTemplatesList loadFinancialTransactionTemplates(AEDescriptor appModuleTemplateDescr, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			/**
			 * DAO
			 */
			FinancialTransactionDAO finTransDAO = daoFactory.getFinancialTransactionDAO(localConnection);

			// load financial transaction templates for specified application module template
			FinancialTransactionTemplatesList ftTemplatesList = finTransDAO.loadFinancialTransactionTemplates(appModuleTemplateDescr);

			// load in depth
			for (FinancialTransactionTemplate financialTransactionTemplate : ftTemplatesList) {
				loadInDepth(financialTransactionTemplate, localConnection);
			}

			return ftTemplatesList;
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@AEInternal
	private void loadInDepth(FinancialTransactionTemplate ftt, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			/**
			 * DAO
			 */
			FinancialTransactionDAO finTransDAO = daoFactory.getFinancialTransactionDAO(localConnection);

			AccJournalEntryTeplatesList accJournalEntryTeplates = finTransDAO.loadAccJournalEntryTeplates(ftt.getDescriptor());
			ftt.setAccJournalEntryTemplates(accJournalEntryTeplates);

		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse saveFinancialTransactionTemplates(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			JSONObject appModuleTemplateJson = arguments.getJSONObject(AppModuleTemplate.JSONKey.appModuleTemplate.name());

			// financial transaction templates
			JSONArray  finTransTemplatesJson = appModuleTemplateJson.getJSONArray(FinancialTransactionTemplate.JSONKey.financialTransactionTemplates.name());

			// valid to date
			Date validFrom = null;
			Date validTo = null;
			if(appModuleTemplateJson.has(AppModuleTemplate.JSONKey.validTo.name())) {
				validTo = AEDateUtil.parseDateStrict(appModuleTemplateJson.getString(AppModuleTemplate.JSONKey.validTo.name()));
			}

			// 
			AppModuleTemplate appModuleTemplate = new AppModuleTemplate(appModuleTemplateJson);
			appModuleTemplate.setValidFrom(validFrom);
			appModuleTemplate.setValidTo(validTo);

			//
			FinancialTransactionTemplatesList finTransTemplates = 
					new FinancialTransactionTemplatesList().createFrom(finTransTemplatesJson).toAppModuleTemplate(appModuleTemplate);

			/**
			 * Authorize for concrete tenant
			 */
			// without authorization: this request is on application level
			authorize(new AuthPermission("System/Configuration/FinancialTransactionTemplate", AuthPermission.SAVE_AND_DELETE), invContext, null);

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			localConnection.beginTransaction();
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * DAO
			 */
			// AppDAO appDAO = daoFactory.getAppDAO(localConnection);

			// Save app module template
			save(appModuleTemplate, localConnection);

			// Save financial transactions
			save(finTransTemplates, localConnection);

			// commit
			localConnection.commit();

			// build response
			appModuleTemplateJson = appModuleTemplate.toJSONObject();
			appModuleTemplateJson.put(
					FinancialTransactionTemplate.JSONKey.financialTransactionTemplates.name(), 
					finTransTemplates.toJSONArray());
			JSONObject payload = new JSONObject();
			payload.put(AppModuleTemplate.JSONKey.appModuleTemplate.name(), appModuleTemplateJson);
			return new AEResponse(payload);
		} catch (Exception e) {
			logger.error(e);
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/**
	 * FIXME
	 * 
	 * @param localConnection
	 * @return
	 * @throws AEException
	 */
	private JSONArray loadAccounts(AEConnection localConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();

		// load COA Models
		ChartOfAccountsDAO coaModelDAO = daoFactory.getChartOfAccountsDAO(localConnection);
		AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);

		JSONArray accounts = null;
		JSONArray coaModelsJson = coaModelDAO.loadModels();
		if(coaModelsJson.length() > 0) {
			JSONObject coaModelJson = coaModelsJson.getJSONObject(0);

			long coaModelId = coaModelJson.getLong(AEDomainObject.JSONKey.id.name());

			// load accounts
			accounts = accountDAO.loadAccounts(coaModelId);
			coaModelJson.put(AccAccount.JSONKey.accounts.name(), accounts);

			// load patterns
			JSONArray patterns = accountDAO.loadPatterns(coaModelId);
			for (int j = 0; j < patterns.length(); j++) {
				accounts.put(patterns.get(j));
			}
		}

		if(accounts == null) {
			accounts = new JSONArray();
		}

		return accounts;
	}

	private void save(AppModuleTemplate appModuleTemplate, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();

			/**
			 * DAO
			 */
			AppDAO appDAO = daoFactory.getAppDAO(localConnection);

			if(AEPersistent.State.NEW.equals(appModuleTemplate.getPersistentState())) {
				appDAO.insert(appModuleTemplate);
			} else if(AEPersistent.State.UPDATED.equals(appModuleTemplate.getPersistentState())) {
				appDAO.update(appModuleTemplate);
			} else if(AEPersistent.State.DELETED.equals(appModuleTemplate.getPersistentState())) {
				// appDAO.delete(appModuleTemplate);
			}

			// commit
			localConnection.commit();
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private void save(FinancialTransactionTemplatesList ftTemplates, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();

			/**
			 * DAO
			 */
			FinancialTransactionDAO finTransDAO = daoFactory.getFinancialTransactionDAO(localConnection);

			for (Iterator<FinancialTransactionTemplate> iterator = ftTemplates.iterator(); iterator.hasNext();) {
				FinancialTransactionTemplate finTransTempl = (FinancialTransactionTemplate) iterator.next();

				// db process
				if(AEPersistent.State.NEW.equals(finTransTempl.getPersistentState())) {
					finTransDAO.insert(finTransTempl);
				} else if(AEPersistent.State.UPDATED.equals(finTransTempl.getPersistentState())) {
					finTransDAO.update(finTransTempl);
				} else if(AEPersistent.State.DELETED.equals(finTransTempl.getPersistentState())) {
					// finTransDAO.delete(appModuleTemplate);
				}

				// save in depth
				AccJournalEntryTeplatesList journalEntryTemplates = finTransTempl.getAccJournalEntryTemplates();
				for (Iterator<AccJournalEntryTemplate> iterator2 = journalEntryTemplates.iterator(); iterator2.hasNext();) {
					AccJournalEntryTemplate accJournalEntryTemplate = (AccJournalEntryTemplate) iterator2.next();

					// validate 
					if(!(accJournalEntryTemplate.isDebit() ^ accJournalEntryTemplate.isCredit())) {
						// false ^ false = false
						// false ^ true = true
						// true ^ false = true
						// true ^ true = false
						throw AEError.System.INVALID_REQUEST.toException();
					}

					// prepare
					accJournalEntryTemplate.setFinancialTransactionTemplate(finTransTempl.getDescriptor());

					// process
					if(AEPersistent.State.NEW.equals(accJournalEntryTemplate.getPersistentState())) {
						finTransDAO.insert(accJournalEntryTemplate);
					} else if(AEPersistent.State.UPDATED.equals(accJournalEntryTemplate.getPersistentState())) {
						finTransDAO.update(accJournalEntryTemplate);
					} else if(AEPersistent.State.DELETED.equals(accJournalEntryTemplate.getPersistentState())) {
						finTransDAO.delete(accJournalEntryTemplate);
						iterator2.remove();
					}
				}
			}

			// commit
			localConnection.commit();
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse createFinancialTransaction(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);
			long sOwnerId = arguments.getLong(AEDomainObject.JSONKey.sOwnerId.name());

			// module
			String appModuleCode = arguments.getString(AppModuleTemplate.JSONKey.appModuleCode.name());

			// date now
			Date dateNow = AEDateUtil.getClearDateTime(new Date());

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
			authorize(new AuthPermission("System/FinancialTransaction", AuthPermission.CREATE), invContext, Organization.lazyDescriptor(ownerId));
			
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * DAO
			 */
			AppDAO appDAO = daoFactory.getAppDAO(localConnection);

			/**
			 * Load app module by code
			 */
			AEAppModule appModule = appDAO.loadAppModuleByCode(appModuleCode);
			if(appModule == null) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}

			// load app module namePath and codePath
			AEAppModulesList modulePath = appDAO.loadAppModulePath(appModule.getCode());

			StringBuilder appModuleNamePathBuilder = new StringBuilder();
			StringBuilder appModuleCodePathBuilder = new StringBuilder();
			for (AEAppModule module : modulePath) {
				if(!AEStringUtil.isEmpty(appModuleNamePathBuilder)) {
					appModuleNamePathBuilder.append(" - ");
				}
				appModuleNamePathBuilder.append(module.getName());

				if(!AEStringUtil.isEmpty(appModuleCodePathBuilder)) {
					appModuleCodePathBuilder.append("/");
				}
				appModuleCodePathBuilder.append(module.getCode());
			}

			/**
			 * Load application module template for specified module valid on specified date
			 */
			AEAppModule appModuleT = appDAO.loadAppModuleByRelationTo(appModule.getCode(), AEAppModule.AppModuleRelation.FROM_IS_TEMPLATE_TO);
			AEDescriptor appModuleTemplateDescr = appModuleT.getDescriptor();
			AppModuleTemplate appModuleTemplate = appDAO.loadAppModuleTemplate(appModuleTemplateDescr, dateNow);

			if(appModuleTemplate == null) {
				throw new AEException("Il n’y a aucun guide associé à cette écriture. ");
			}

			// load financial transaction templates for application module template
			FinancialTransactionTemplatesList ftTemplatesList = loadFinancialTransactionTemplates(appModuleTemplate.getDescriptor(), localConnection);
			if(ftTemplatesList == null || ftTemplatesList.isEmpty()) {
				throw new AEException(AEError.System.INVALID_PARAMETER);
			}

			/**
			 * Create FinancialTransactionList from FinancialTransactionTemplatesList
			 */
			FinancialTransactionList ftList = new FinancialTransactionList();
			for (Iterator<FinancialTransactionTemplate> iterator = ftTemplatesList.iterator(); iterator.hasNext();) {
				FinancialTransactionTemplate ftTemplate = (FinancialTransactionTemplate) iterator.next();

				// create FinancialTransactionList for ftTemplate
				FinancialTransactionList ftl = null;
				FinancialTransactionTemplate.PaymentMethod pm = ftTemplate.getPaymentMethod();
				switch (pm) {
				case BANK:
					ftl = createBankFinancialTransactions(ftTemplate, ownerDescr, ap, localConnection, invContext);
					break;
				case CASH:
				case VARIOUS:
				default:
					ftl = createFinancialTransactions(ftTemplate, ownerDescr, ap, localConnection, invContext);
				}

				// add to the list
				ftList.addAll(ftl);
			}


			/**
			 * Load all acc accounts 
			 */
			AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);
			JSONArray accounts = accountDAO.loadAccountsByOwner(ownerId);

			/**
			 * Load suppliers
			 */
			SimplePartyDAO spDAO = daoFactory.getSimplePartyDAO(localConnection);
			SimplePartiesList spList = spDAO.loadToCompany(ownerDescr);

			// build response
			JSONObject payload = new JSONObject();
			payload.put(AEAppModule.JSONKey.appModuleNamePath.name(), appModuleNamePathBuilder.toString());
			payload.put(AccAccount.JSONKey.accounts.name(), accounts);
			payload.put(FinancialTransaction.JSONKey.financialTransactions.name(), ftList.toJSONArray());
			payload.put(FinancialTransaction.JSONKey.suppliers.name(), spList.toJSONArray());
			return new AEResponse(payload);
		} catch (Exception e) {
			logger.error(e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private FinancialTransactionList createFinancialTransactions(FinancialTransactionTemplate ftTemplate, AEDescriptor ownerDescr, AuthPrincipal ap, AEConnection localConnection, AEInvocationContext invContext) throws AEException {
		FinancialTransaction ft = new FinancialTransaction();

		DAOFactory daoFactory = DAOFactory.getInstance();
		AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);


		/**
		 * Name 
		 */
		FinancialTransactionTemplate.PaymentMethod pm = ftTemplate.getPaymentMethod();
		switch (pm) {
		case CASH:
			ft.setName("Caisse");
			break;
		case VARIOUS:
		default:
			ft.setName("Diverses");
		}

		/**
		 * Load concrete accounts from the acc journal templates
		 * and Create AccJournalEntry
		 */
		AccJournalEntry accJournalEntry = new AccJournalEntry();
		AccJournalEntryTeplatesList accJournalEntryTeplatesList = ftTemplate.getAccJournalEntryTemplates();
		JSONArray expIncAccounts = new JSONArray();
		JSONArray cashAccounts = new JSONArray();
		JSONArray otherReceivablePayableAccounts = new JSONArray();
		if(accJournalEntryTeplatesList != null) {
			for (Iterator<AccJournalEntryTemplate> iterator = accJournalEntryTeplatesList.iterator(); iterator.hasNext();) {
				AccJournalEntryTemplate accJournalEntryTemplate = (AccJournalEntryTemplate) iterator.next();

				// load template account (an account from COAModel)
				AEDescriptive modelAccDescriptive = accJournalEntryTemplate.getAccAccount();

				// load concrete accounts from individual COA
				JSONArray availableConcreteAccounts = null;
				if(modelAccDescriptive != null) {
					JSONObject modelAccJson = accountDAO.loadAccountByIdFull(modelAccDescriptive.getDescriptor().getID());
					availableConcreteAccounts = loadAvailableConcreteAccounts(modelAccJson, ownerDescr.getDescriptor().getID(), localConnection);
				}

				if(AccJournalEntryTemplate.AccountIdentificationRule.EXPENSE.equals(accJournalEntryTemplate.getAccountIdentificationRule())
						|| AccJournalEntryTemplate.AccountIdentificationRule.REVENUE.equals(accJournalEntryTemplate.getAccountIdentificationRule())
						|| AccJournalEntryTemplate.AccountIdentificationRule.INTERNAL_TRANSFERS.equals(accJournalEntryTemplate.getAccountIdentificationRule())
						|| AccJournalEntryTemplate.AccountIdentificationRule.THIRD_ACCOUNT.equals(accJournalEntryTemplate.getAccountIdentificationRule())) {

					if(availableConcreteAccounts != null) {
						JSONUtil.addAll(expIncAccounts, availableConcreteAccounts);
					}
				} else if(AccJournalEntryTemplate.AccountIdentificationRule.CASH_ACCOUNT.equals(accJournalEntryTemplate.getAccountIdentificationRule())) {
					if(availableConcreteAccounts != null) {
						JSONUtil.addAll(cashAccounts, availableConcreteAccounts);
					}
				} else if(AccJournalEntryTemplate.AccountIdentificationRule.OTHER_RECEIVABLE_PAYABLE.equals(accJournalEntryTemplate.getAccountIdentificationRule())) {
					if(availableConcreteAccounts != null) {
						JSONUtil.addAll(otherReceivablePayableAccounts, availableConcreteAccounts);
					}
				}

				// create from template
				AccJournalItem accJournalItem = createAccJournalItem(accJournalEntryTemplate, availableConcreteAccounts); 

				// add to the entry
				accJournalEntry.addItem(accJournalItem);
			}
		}
		ft.setAccJournalEntry(accJournalEntry);
		ft.setExpIncAccounts(expIncAccounts);
		ft.setCashAccounts(cashAccounts);
		ft.setOtherReceivablePayableAccounts(otherReceivablePayableAccounts);

		/**
		 * Load bank accounts
		 */
		JSONArray bankAccounts = null;
		if(FinancialTransactionTemplate.PaymentMethod.BANK.equals(ftTemplate.getPaymentMethod())) {
			JSONObject bankAeRequestArguments = new JSONObject();
			bankAeRequestArguments.put(AEDomainObject.JSONKey.ownerId.name(), ownerDescr.getID());
			bankAeRequestArguments.put(AEDomainObject.JSONKey.sOwnerId.name(), ownerDescr.getID());
			AERequest bankAeRequest = new AERequest(bankAeRequestArguments).withAuthPrincipal(ap);
			AEResponse bankAccountsAeResponse = cashLocal.loadBankAccounts(bankAeRequest);
			bankAccounts = bankAccountsAeResponse.getPayload().getJSONArray(FinancialTransaction.JSONKey.bankAccounts.name());

			ft.setBankAccounts(bankAccounts);
		}

		Date date = AEDateUtil.getClearDateTime(new Date());

		// payment method
		ft.setPaymentMethod(ftTemplate.getPaymentMethod());

		// accAccountExpIncId
		if(expIncAccounts != null && expIncAccounts.length() == 1) {
			AEDescriptorImp accAccountExpIncDescr = new AEDescriptorImp();
			accAccountExpIncDescr.create(expIncAccounts.getJSONObject(0));

			ft.setAccAccountExpInc(accAccountExpIncDescr);
		}

		// bankAccount
		if(bankAccounts != null && bankAccounts.length() == 1) {
			AEDescriptorImp bankAcountDescr = new AEDescriptorImp();
			bankAcountDescr.create(bankAccounts.getJSONObject(0));

			ft.setBankAccount(bankAcountDescr);
			
			// accBalance
			Date dateTo = new Date();
			Date dateFrom = AEDateUtil.beginOfTheYear(dateTo);
			AETimePeriod period = new AETimePeriod(dateFrom, dateTo);
			AccAccountBalance accAccountBalance = new AccAccountBalance();
			JSONObject accAccountJson = accountDAO.loadAccountByIdFull(bankAccounts.getJSONObject(0).getLong(BankAccount.JSONKey.accId.name()));
			if(accAccountJson != null) {
				accAccountBalance.setAccAccount(
						new AEDescriptorImp(accAccountJson.getLong(AEDomainObject.JSONKey.id.name()))
						.withCode(accAccountJson.optString(AEDomainObject.JSONKey.code.name()))
						.withName(accAccountJson.optString(AEDomainObject.JSONKey.name.name())));
				accAccountBalance.setPeriod(period);
				accLocal.loadAccFinalBalance(accAccountBalance, invContext, localConnection);
				ft.setAccAccountBalance(accAccountBalance);
			}
		}

		// accCashAccount
		if(cashAccounts != null && cashAccounts.length() == 1) {
			AEDescriptorImp accAccountCashDescr = new AEDescriptorImp();
			accAccountCashDescr.create(cashAccounts.getJSONObject(0));

			ft.setAccAccountCash(accAccountCashDescr);
			
			// accBalance
			Date dateTo = new Date();
			Date dateFrom = AEDateUtil.beginOfTheYear(dateTo);
			AETimePeriod period = new AETimePeriod(dateFrom, dateTo);
			AccAccountBalance accAccountBalance = new AccAccountBalance();
			JSONObject accAccountJson = accountDAO.loadAccountByIdFull(accAccountCashDescr.getID());
			if(accAccountJson != null) {
				accAccountBalance.setAccAccount(
						new AEDescriptorImp(accAccountJson.getLong(AEDomainObject.JSONKey.id.name()))
						.withCode(accAccountJson.optString(AEDomainObject.JSONKey.code.name()))
						.withName(accAccountJson.optString(AEDomainObject.JSONKey.name.name())));
				accAccountBalance.setPeriod(period);
				accLocal.loadAccFinalBalance(accAccountBalance, invContext, localConnection);
				ft.setAccAccountBalance(accAccountBalance);
			}
		}

		// accCashAccount
		if(otherReceivablePayableAccounts != null && otherReceivablePayableAccounts.length() == 1) {
			AEDescriptorImp accAccountDescr = new AEDescriptorImp();
			accAccountDescr.create(otherReceivablePayableAccounts.getJSONObject(0));

			ft.setAccAccountOtherReceivablePayable(accAccountDescr);
		}

		// ftTemplate
		ft.setFtTemplate(ftTemplate.getDescriptor());

		// dateTransaction
		ft.setDateTransaction(date);

		// dateAccounting
		ft.setDateAccounting(date);

		// bankAccounts
		ft.setBankAccounts(bankAccounts);

		// expIncAccounts
		ft.setExpIncAccounts(expIncAccounts);

		/**
		 * At the end: setup AccJournalEntry from just created ft
		 */
		initialSetupAccJournalEntry(ft, localConnection);

		FinancialTransactionList ftl = new FinancialTransactionList();
		ftl.add(ft);
		return ftl;
	}

	private FinancialTransactionList createBankFinancialTransactions(FinancialTransactionTemplate ftTemplate, AEDescriptor ownerDescr, AuthPrincipal ap, AEConnection localConnection, AEInvocationContext invContext) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);

		JSONObject bankAeRequestArguments = new JSONObject();
		bankAeRequestArguments.put(AEDomainObject.JSONKey.ownerId.name(), ownerDescr.getID());
		bankAeRequestArguments.put(AEDomainObject.JSONKey.sOwnerId.name(), ownerDescr.getID());
		AERequest bankAeRequest = new AERequest(bankAeRequestArguments).withAuthPrincipal(ap);
		AEResponse bankAccountsAeResponse = cashLocal.loadBankAccounts(bankAeRequest);
		JSONArray bankAccountsAll = bankAccountsAeResponse.getPayload().getJSONArray(FinancialTransaction.JSONKey.bankAccounts.name());

		FinancialTransactionList ftl = new FinancialTransactionList();
		if(bankAccountsAll != null) {
			for (int i = 0; i < bankAccountsAll.length(); i++) {
				JSONObject bankAccount = bankAccountsAll.getJSONObject(i);
				JSONArray bankAccounts = new JSONArray();
				bankAccounts.put(bankAccount);

				FinancialTransaction ft = new FinancialTransaction();

				ft.setBankAccounts(bankAccounts);

				ft.setName(bankAccount.getString(AEDomainObject.JSONKey.name.name()));

				/**
				 * Load concrete accounts from the acc journal templates
				 * and Create AccJournalEntry
				 */
				AccJournalEntry accJournalEntry = new AccJournalEntry();
				AccJournalEntryTeplatesList accJournalEntryTeplatesList = ftTemplate.getAccJournalEntryTemplates();
				JSONArray expIncAccounts = new JSONArray();
				JSONArray cashAccounts = new JSONArray();
				JSONArray otherReceivablePayableAccounts = new JSONArray();
				if(accJournalEntryTeplatesList != null) {
					for (Iterator<AccJournalEntryTemplate> iterator = accJournalEntryTeplatesList.iterator(); iterator.hasNext();) {
						AccJournalEntryTemplate accJournalEntryTemplate = (AccJournalEntryTemplate) iterator.next();

						// load concrete accounts from individual COA
						JSONArray availableConcreteAccounts = null;

						// load template account (an account from COAModel)
						AEDescriptive modelAccDescriptive = accJournalEntryTemplate.getAccAccount();

						if(modelAccDescriptive != null) {
							JSONObject modelAccJson = accountDAO.loadAccountByIdFull(modelAccDescriptive.getDescriptor().getID());
							availableConcreteAccounts = loadAvailableConcreteAccounts(modelAccJson, ownerDescr.getDescriptor().getID(), localConnection);
						}

						if(AccJournalEntryTemplate.AccountIdentificationRule.EXPENSE.equals(accJournalEntryTemplate.getAccountIdentificationRule())
								|| AccJournalEntryTemplate.AccountIdentificationRule.REVENUE.equals(accJournalEntryTemplate.getAccountIdentificationRule())
								|| AccJournalEntryTemplate.AccountIdentificationRule.INTERNAL_TRANSFERS.equals(accJournalEntryTemplate.getAccountIdentificationRule())
								|| AccJournalEntryTemplate.AccountIdentificationRule.THIRD_ACCOUNT.equals(accJournalEntryTemplate.getAccountIdentificationRule())) {

							if(availableConcreteAccounts != null) {
								JSONUtil.addAll(expIncAccounts, availableConcreteAccounts);
							}
						} else if(AccJournalEntryTemplate.AccountIdentificationRule.CASH_ACCOUNT.equals(accJournalEntryTemplate.getAccountIdentificationRule())) {
							if(availableConcreteAccounts != null) {
								JSONUtil.addAll(cashAccounts, availableConcreteAccounts);
							}
						} else if(AccJournalEntryTemplate.AccountIdentificationRule.OTHER_RECEIVABLE_PAYABLE.equals(accJournalEntryTemplate.getAccountIdentificationRule())) {
							if(availableConcreteAccounts != null) {
								JSONUtil.addAll(otherReceivablePayableAccounts, availableConcreteAccounts);
							}
						}

						// create from template
						AccJournalItem accJournalItem = createAccJournalItem(accJournalEntryTemplate, availableConcreteAccounts); 

						// add to the entry
						accJournalEntry.addItem(accJournalItem);
					}
				}
				ft.setAccJournalEntry(accJournalEntry);
				ft.setExpIncAccounts(expIncAccounts);
				ft.setCashAccounts(cashAccounts);
				ft.setOtherReceivablePayableAccounts(otherReceivablePayableAccounts);

				Date date = AEDateUtil.getClearDateTime(new Date());

				// payment method
				ft.setPaymentMethod(ftTemplate.getPaymentMethod());

				// accAccountExpIncId
				if(expIncAccounts != null && expIncAccounts.length() == 1) {
					AEDescriptorImp accAccountExpIncDescr = new AEDescriptorImp();
					accAccountExpIncDescr.create(expIncAccounts.getJSONObject(0));

					ft.setAccAccountExpInc(accAccountExpIncDescr);
				}

				// bankAccount
				if(bankAccounts != null && bankAccounts.length() == 1) {
					AEDescriptorImp bankAcountDescr = new AEDescriptorImp();
					bankAcountDescr.create(bankAccounts.getJSONObject(0));

					ft.setBankAccount(bankAcountDescr);
					
					// accBalance
					Date dateTo = new Date();
					Date dateFrom = AEDateUtil.beginOfTheYear(dateTo);
					AETimePeriod period = new AETimePeriod(dateFrom, dateTo);
					AccAccountBalance accAccountBalance = new AccAccountBalance();
					JSONObject accAccountJson = accountDAO.loadAccountByIdFull(bankAccounts.getJSONObject(0).getLong(BankAccount.JSONKey.accId.name()));
					if(accAccountJson != null) {
						accAccountBalance.setAccAccount(
								new AEDescriptorImp(accAccountJson.getLong(AEDomainObject.JSONKey.id.name()))
								.withCode(accAccountJson.optString(AEDomainObject.JSONKey.code.name()))
								.withName(accAccountJson.optString(AEDomainObject.JSONKey.name.name())));
						accAccountBalance.setPeriod(period);
						accLocal.loadAccFinalBalance(accAccountBalance, invContext, localConnection);
						ft.setAccAccountBalance(accAccountBalance);
					}
				}

				// accCashAccount
				if(cashAccounts != null && cashAccounts.length() == 1) {
					AEDescriptorImp accAccountCashDescr = new AEDescriptorImp();
					accAccountCashDescr.create(cashAccounts.getJSONObject(0));

					ft.setAccAccountCash(accAccountCashDescr);
					
					// accBalance
					Date dateTo = new Date();
					Date dateFrom = AEDateUtil.beginOfTheYear(dateTo);
					AETimePeriod period = new AETimePeriod(dateFrom, dateTo);
					AccAccountBalance accAccountBalance = new AccAccountBalance();
					JSONObject accAccountJson = accountDAO.loadAccountByIdFull(accAccountCashDescr.getID());
					if(accAccountJson != null) {
						accAccountBalance.setAccAccount(
								new AEDescriptorImp(accAccountJson.getLong(AEDomainObject.JSONKey.id.name()))
								.withCode(accAccountJson.optString(AEDomainObject.JSONKey.code.name()))
								.withName(accAccountJson.optString(AEDomainObject.JSONKey.name.name())));
						accAccountBalance.setPeriod(period);
						accLocal.loadAccFinalBalance(accAccountBalance, invContext, localConnection);
						ft.setAccAccountBalance(accAccountBalance);
					}
				}

				// accCashAccount
				if(otherReceivablePayableAccounts != null && otherReceivablePayableAccounts.length() == 1) {
					AEDescriptorImp accAccountDescr = new AEDescriptorImp();
					accAccountDescr.create(otherReceivablePayableAccounts.getJSONObject(0));

					ft.setAccAccountOtherReceivablePayable(accAccountDescr);
				}

				// ftTemplate
				ft.setFtTemplate(ftTemplate.getDescriptor());

				// dateTransaction
				ft.setDateTransaction(date);

				// dateAccounting
				ft.setDateAccounting(date);

				// bankAccounts
				ft.setBankAccounts(bankAccounts);

				// expIncAccounts
				ft.setExpIncAccounts(expIncAccounts);

				/**
				 * At the end: setup AccJournalEntry from just created ft
				 */
				initialSetupAccJournalEntry(ft, localConnection);

				ftl.add(ft);
			}
		}
		return ftl;
	}

	private AccJournalItem createAccJournalItem(AccJournalEntryTemplate accJournalEntryTemplate, JSONArray availableConcreteAccounts) {
		AccJournalItem accJournalItem = (AccJournalItem) new AccJournalItem().withTmpId();

		// metadata: how accJournalItem must be setup from FinanceTransaction 
		accJournalItem.setDebit(accJournalEntryTemplate.isDebit());
		accJournalItem.setCredit(accJournalEntryTemplate.isCredit());
		accJournalItem.setAccountIdentificationRule(accJournalEntryTemplate.getAccountIdentificationRule());
		accJournalItem.setJournalIdentificationRule(accJournalEntryTemplate.getJournalIdentificationRule());
		accJournalItem.setQuete(accJournalEntryTemplate.getQuete());

		// acc account
		if(availableConcreteAccounts != null && availableConcreteAccounts.length() == 1) {
			AEDescriptorImp accountDescr = new AEDescriptorImp();
			accountDescr.create(availableConcreteAccounts.getJSONObject(0));

			accJournalItem.setAccount(accountDescr);
		}

		return accJournalItem;
	}

	/**
	 * Loads available concrete accounts (for specified <code>ownerId</code>) created from specified model account id
	 * 
	 * @param modelAccId Model Account Id
	 * @param ownerId Tenant Id
	 * @param localConnection Currently used connection
	 * @return not null accounts array
	 * @throws AEException
	 */
	private JSONArray loadAvailableConcreteAccounts(JSONObject modelAccJson, long ownerId, AEConnection localConnection) throws AEException {
		JSONArray accJsonArray = new JSONArray();

		if(modelAccJson != null) {
			DAOFactory daoFactory = DAOFactory.getInstance();
			AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);
			long modelAccId = modelAccJson.getLong(AEDomainObject.JSONKey.id.name());

			if(AccAccount.AccountType.ACCOUNT.getTypeId() == modelAccJson.getLong(AccAccount.JSONKey.accType.name())) {
				JSONArray concreteAccountsJsonArray = accountDAO.loadConcreteAccounts(modelAccId, ownerId);
				JSONUtil.addAll(accJsonArray, concreteAccountsJsonArray);
			} else if(AccAccount.AccountType.PATTERN.getTypeId() == modelAccJson.getLong(AccAccount.JSONKey.accType.name())) {
				String modelAccCode = modelAccJson.getString(AEDomainObject.JSONKey.code.name());
				String modelAccSubCode = modelAccCode.substring(0, 3);
				JSONArray concreteAccountsJsonArray = accountDAO.loadConcreteSubAccountsByCode(modelAccSubCode, ownerId);
				JSONUtil.addAll(accJsonArray, concreteAccountsJsonArray);
			}
		}

		return accJsonArray;
	}

	private void initialSetupAccJournalEntry(FinancialTransaction ft, AEConnection aeConnection) throws AEException {
		if(ft == null) {
			return;
		}

		DAOFactory daoFactory = DAOFactory.getInstance();
		BankAccountDAO bankAccountDAO = daoFactory.getBankAccountDAO(aeConnection);
		BankAccount bankAccount = null;
		AEDescriptor bankAccountDescr = ft.getBankAccount();
		if(bankAccountDescr != null) {
			bankAccount = bankAccountDAO.load(bankAccountDescr.getID());
		}

		AccJournalEntry accJournalEntry = ft.getAccJournalEntry();
		if(accJournalEntry != null) {
			AccJournalItemsList accJournalItems = accJournalEntry.getAccJournalItems();
			if(accJournalItems != null) {
				for (Iterator<AccJournalItem> iterator = accJournalItems.iterator(); iterator.hasNext();) {
					AccJournalItem accJournalItem = (AccJournalItem) iterator.next();

					/**
					 * setup accJournalItem from specified FinancialTransaction
					 */
					// private AEDescriptor journal;
					if(!AccJournalEntryTemplate.JournalIdentificationRule.BQ.equals(accJournalItem.getJournalIdentificationRule())) {
						AEDescriptor journalDescr = new AEDescriptorImp().withCode(accJournalItem.getJournalIdentificationRule().name());
						accJournalItem.setJournal(journalDescr);
					} else if(bankAccount != null && bankAccount.getAccJournal() != null){
						AEDescriptor journalDescr = new AEDescriptorImp().withCode(bankAccount.getAccJournal().getDescriptor().getCode());
						accJournalItem.setJournal(journalDescr);
					}

					// private AEDescriptor accPeriod;

					// private long batchId = AEPersistentUtil.NEW_ID;

					// private long entryId = AEPersistentUtil.NEW_ID;
					accJournalItem.setEntryId(accJournalEntry.getID());

					// private Date date;
					accJournalItem.setDate(ft.getDateAccounting());

					// private AccountIdentificationRule accountIdentificationRule = AccountIdentificationRule.NA;

					// private AEDescriptor account;
					if(AccJournalEntryTemplate.AccountIdentificationRule.BANK_ACCOUNT.equals(accJournalItem.getAccountIdentificationRule())
							&& bankAccount != null && bankAccount.getAccount() != null) {

						accJournalItem.setAccount(bankAccount.getAccount().getDescriptor());
					} else if(AccJournalEntryTemplate.AccountIdentificationRule.CASH_ACCOUNT.equals(accJournalItem.getAccountIdentificationRule())
							&& ft.getAccAccountCash() != null) {

						accJournalItem.setAccount(ft.getAccAccountCash());
					} else if(AccJournalEntryTemplate.AccountIdentificationRule.EXPENSE.equals(accJournalItem.getAccountIdentificationRule())
							&& ft.getAccAccountExpInc() != null) {

						accJournalItem.setAccount(ft.getAccAccountExpInc());
					} else if(AccJournalEntryTemplate.AccountIdentificationRule.REVENUE.equals(accJournalItem.getAccountIdentificationRule())
							&& ft.getAccAccountExpInc() != null) {

						accJournalItem.setAccount(ft.getAccAccountExpInc());
					} else if(AccJournalEntryTemplate.AccountIdentificationRule.INTERNAL_TRANSFERS.equals(accJournalItem.getAccountIdentificationRule())
							&& ft.getAccAccountExpInc() != null) {

						accJournalItem.setAccount(ft.getAccAccountExpInc());
					} else if(AccJournalEntryTemplate.AccountIdentificationRule.THIRD_ACCOUNT.equals(accJournalItem.getAccountIdentificationRule())
							&& ft.getAccAccountExpInc() != null) {

						accJournalItem.setAccount(ft.getAccAccountExpInc());
					} else if(AccJournalEntryTemplate.AccountIdentificationRule.OTHER_RECEIVABLE_PAYABLE.equals(accJournalItem.getAccountIdentificationRule())
							&& ft.getAccAccountExpInc() != null) {

						accJournalItem.setAccount(ft.getAccAccountOtherReceivablePayable());
					}

					// private AEDescriptor auxiliary;

					// private AEDescriptor reference;
					accJournalItem.setDateCreation(ft.getDateTransaction());

					// private AEDescriptor currency;

					// private boolean debit;

					// private boolean credit;

					// private Double dtAmount;
					if(accJournalItem.isDebit()) {
						accJournalItem.setDtAmount(ft.getAmount());
					}

					// private Double ctAmount;
					if(accJournalItem.isCredit()) {
						accJournalItem.setCtAmount(ft.getAmount());
					}

					// private Date paymentDueDate;

					// private AEDescriptor issuerPaymentType;

					// private AEDescriptor recipientPaymentType;
				}
			}
		}
	}

	@Override
	public AEResponse saveFinancialTransaction(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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

			// FinancialTransaction
			JSONObject ftJson = arguments.getJSONObject(FinancialTransaction.JSONKey.financialTransaction.name());
			ftJson.put(AEDomainObject.JSONKey.ownerId.name(), ownerId);

			/**
			 * Authorize for concrete tenant and action
			 */
			// state validation
			if(ownerId != sOwnerId) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}

			// whether this principal is authorized for specified tenant
			ap.ownershipValidator(ownerId);
			
			// whether this principal is authorized for specified tenant for specified action
			authorize(new AuthPermission("System/FinancialTransaction", AuthPermission.SAVE), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * Create FinancialTransaction
			 */
			FinancialTransaction ft = new FinancialTransaction();
			ft.create(ftJson);

			// Setup Quetes
			// MUST BE before setupAccJournalEntry
			QuetesList quetesList = ft.getQuetesList();
			if(quetesList != null && quetesList.size() == 1) {
				quetesList.get(0).setAmount(ft.getAmount());
			}

			// Setup Contributor
			// MUST BE before setupAccJournalEntry
			// If the Contributor is null, this FinancialTransaction doesn't deal with Contributor
			Contributor c  = ft.getContributor();
			if(c != null) {
				// ft manages contributor
				if(c.isUnrecognized()) {
					if(ft.isPersistent()) {
						// unrecognized contributor within existing FT,
						// so do nothing: FinancialTransaction knows how to deal with unrecognized Contributor
					} else {
						// unrecognized contributor within a new FT
						ft.setContributor(null);
					}
				} else if(!c.isValid()) {
					// recognized, but invalid
					throw AEError.System.INVALID_REQUEST.toException();
				}
			}

			/**
			 * DB connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			// setup AccJournalEntry
			// MUST BE the last setup operation
			setupAccJournalEntry(ft, localConnection);

			/**
			 * Start transaction
			 */
			localConnection.beginTransaction();

			/**
			 * Save ft
			 */
			switch(ft.getPersistentState()) {
			case NEW:
				insert(ft, invContext, localConnection);
				break;
			case UPDATED:
				update(ft, invContext, localConnection);
				break;
			case DELETED:
				break;
			default:
				break;
			}

			/**
			 * Attachment
			 */
			JSONObject fileAttachmentJson = ftJson.optJSONObject(FinancialTransaction.JSONKey.fileAttachment.name());
			Path tmpFile = null;
			if(fileAttachmentJson != null) {

				// source
				tmpFile = Paths.get(
						AEApp.getInstance().getTmpRepository().toString(),
						fileAttachmentJson.getString("remoteName"));
				if(!Files.exists(tmpFile)) {
					throw AEError.System.INVALID_PARAMETER.toException();
				}

				// target
				String fileRepository = AEApp.getInstance().getProperties().getProperty(AEApp.fileRepository);
				Path target = Paths.get(
						fileRepository, 
						new StringBuilder(FilenameUtils.getBaseName(tmpFile.getFileName().toString()))
						.append("_").append(ft.getClazz().getID()).append("_").append(ft.getID())
						.append(".").append(FilenameUtils.getExtension(tmpFile.getFileName().toString())).toString());

				// move the file
				Files.move(tmpFile, target);

				// attach to journal entry
				fileAttachmentJson.put("ownerId", ft.getCompany().getDescriptor().getID());
				fileAttachmentJson.put("toId", ft.getAccJournalEntry().getID());
				fileAttachmentJson.put("toType", DomainClass.AccJournalEntry.getID());
				fileAttachmentJson.put("name", fileAttachmentJson.getString(AEDomainObject.JSONKey.name.name()));
				fileAttachmentJson.put("length", Files.size(target));
				fileAttachmentJson.put("remotePath", target.getParent().toString());
				fileAttachmentJson.put("remoteName", target.getFileName());
				fileAttachmentJson.put("isDirty", false);

				JSONObject attachArguments = new JSONObject();
				attachArguments.put("fileAttachment", fileAttachmentJson);
				AERequest attachRequest = new AERequest(attachArguments);
				fileAttachmentLocal.manage(attachRequest, invContext);
			}

			/**
			 * Commit
			 */
			localConnection.commit();

			/**
			 * delete tmp file if all is OK
			 */
			if(tmpFile != null && !Files.isDirectory(tmpFile)) {
				AEFileUtil.deleteFileQuietly(tmpFile.toFile());
			}

			/**
			 * Post save
			 */
			c  = ft.getContributor();
			if(c != null && c.isUnrecognized()) {
				// unrecognized contributor
				ft.setContributor(null);
			}

			/**
			 * Build response
			 */
			JSONObject payload = new JSONObject();
			payload.put(FinancialTransaction.JSONKey.financialTransaction.name(), ft.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			logger.error(e);
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private void insert(FinancialTransaction ft, AEInvocationContext invContext, AEConnection localConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		FinancialTransactionDAO ftDAO = daoFactory.getFinancialTransactionDAO(localConnection);

		/**
		 * Contributor
		 */
		Contributor c  = ft.getContributor();
		if(c != null) {
			if(c.isUnrecognized()) {
				// the case when a contributor is choosen and after that deleted from a new FT
				ftDAO.removeContributor(ft.getDescriptor());

				// don't manage Contributor, it is not changed
			} else {
				// Owner
				c.setCompany(ft.getCompany());

				// new financial transaction

				// with new contributor
				c.resetAsNew();

				// with new employee
				Employee empl = c.getEmployee();
				empl.resetAsNew();

				// update 
				AEDescriptive personDescr = empl.getPerson();
				Person person = null;
				if(personDescr == null) {
					// new Person: create Person from empl's data
					person = empl.createPerson();
				} else {
					// update empl's person
					person = (Person) partyLocal.load(personDescr.getDescriptor(), invContext);
					empl.updatePerson(person);
				}

				manage(c, invContext, localConnection);
			}
		}

		/**
		 * Supplier
		 */
		SimpleParty suppl  = ft.getSupplier();
		if(suppl != null) {
			save(suppl, invContext, localConnection);
		}

		/**
		 * AccJournalEntry
		 * AccJournalEntry has a references to Contributor and Supplier
		 */
		AccJournalEntry accJournalEntry = ft.getAccJournalEntry();
		if(accJournalEntry == null) {
			throw new AEException("System error: AccJournalEntry is not instantiated. ");
		}
		accJournalEntry.propContributor(c != null ? c.getDescriptor() : null);
		accJournalEntry.propSupplier(suppl != null ? suppl.getDescriptor() : null);
		accJournalEntry.propCompany();
		
		// validate
		validateAccJournalItems(accJournalEntry.getAccJournalItems(), localConnection);

		// insert
		AccJournalDAO accJournalDAO = daoFactory.getAccJournalDAO(localConnection);
		accJournalDAO.insertEntry(accJournalEntry);

		/**
		 * FinancialTransaction
		 * FinancialTransaction has a references to AccJournalEntry and Contributor and Supplier
		 */
		ftDAO.insert(ft);

		/**
		 * Quetes (after FinancialTransaction)
		 * Quetes has a reference to FinancialTransaction
		 */
		QuetesList quetesList = ft.getQuetesList();
		if(quetesList != null) {
			quetesList.propagateFinancialTransaction(ft);
			ftDAO.insert(quetesList);
		}
	}

	private void update(FinancialTransaction ft, AEInvocationContext invContext, AEConnection localConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		FinancialTransactionDAO ftDAO = daoFactory.getFinancialTransactionDAO(localConnection);

		// Contributor
		// Must be after accJournalEntry in case of deleted Contributor
		Contributor c  = ft.getContributor();
		if(c != null) {
			if(c.isUnrecognized()) {
				ftDAO.removeContributor(ft.getDescriptor());

				// don't manage Contributor, it is not changed
			} else {
				// Owner
				c.setCompany(ft.getCompany());

				// person's data should be changed if this is the up to now employee
				Employee empl = c.getEmployee();
				ContributorDAO contrDAO = daoFactory.getContributorDAO(localConnection);
				long employeeId = contrDAO.getLastEmployeeId(ft.getCompany().getDescriptor(), empl.getPerson().getDescriptor());
				if(c.getEmployee().getID() == employeeId) {
					// update person's data to up to date 
					AEDescriptive personDescr = empl.getPerson();
					Person person = (Person) partyLocal.load(personDescr.getDescriptor(), invContext);
					empl.updatePerson(person);
				}

				// propagate contr's persistent state to the employee
				// because the relation is one to one
				if(c.isUpdated()) {
					c.setUpdated();
				}

				// update DB
				manage(c, invContext, localConnection);
			}
		}

		// Supplier
		SimpleParty suppl  = ft.getSupplier();
		if(suppl != null) {
			save(suppl, invContext, localConnection);
		}

		/**
		 * AccJournalEntry
		 * AccJournalEntry has a references to Contributor and Supplier
		 */
		AccJournalEntry accJournalEntry = ft.getAccJournalEntry();
		if(accJournalEntry != null) {
			accJournalEntry.propContributor(c != null ? c.getDescriptor() : null);
			accJournalEntry.propSupplier(suppl != null ? suppl.getDescriptor() : null);
			accJournalEntry.propCompany();
			
			// validate
			validateAccJournalItems(accJournalEntry.getAccJournalItems(), localConnection);

			// update
			AccJournalDAO accJournalDAO = daoFactory.getAccJournalDAO(localConnection);
			accJournalDAO.updateEntry(accJournalEntry);
		}

		// FinancialTransaction
		ftDAO.update(ft);

		// Quetes (after FinancialTransaction)
		QuetesList quetesList = ft.getQuetesList();
		if(quetesList != null) {
			quetesList.propagateFinancialTransaction(ft);
			ftDAO.update(quetesList);
		}
	}

	private void setupAccJournalEntry(FinancialTransaction ft, AEConnection aeConnection) throws AEException {
		if(ft == null) {
			return;
		}

		DAOFactory daoFactory = DAOFactory.getInstance();
		BankAccountDAO bankAccountDAO = daoFactory.getBankAccountDAO(aeConnection);
		BankAccount bankAccount = null;
		if(ft.getBankAccount() != null) {
			long bankAccountId = ft.getBankAccount().getID();
			if(AEPersistent.ID.isPersistent(bankAccountId)) {
				bankAccount = bankAccountDAO.load(bankAccountId);
			}
		}

		// quetes
		QuetesList quetesList = ft.getQuetesList();

		/**
		 * Update AccJournalEntry from specified FinancialTransaction Data
		 */
		AccJournalEntry accJournalEntry = ft.getAccJournalEntry();

		/**
		 * Description
		 */
		accJournalEntry.setDescription(ft.getDescription());

		/**
		 * Journal Items
		 */
		AccJournalItemsList accJournalItems = accJournalEntry.getAccJournalItems();
		if(accJournalItems != null) {
			for (Iterator<AccJournalItem> iterator = accJournalItems.iterator(); iterator.hasNext();) {
				AccJournalItem accJournalItem = (AccJournalItem) iterator.next();

				/**
				 * setup accJournalItem from specified FinancialTransaction
				 */

				// journal;
				if(!AccJournalEntryTemplate.JournalIdentificationRule.BQ.equals(accJournalItem.getJournalIdentificationRule())) {
					AEDescriptor journalDescr = new AEDescriptorImp().withCode(accJournalItem.getJournalIdentificationRule().name());
					accJournalItem.setJournal(journalDescr);
				} else if(bankAccount != null && bankAccount.getAccJournal() != null){
					AEDescriptor journalDescr = new AEDescriptorImp().withCode(bankAccount.getAccJournal().getDescriptor().getCode());
					accJournalItem.setJournal(journalDescr);
				}

				// accPeriod;

				// batchId = AEPersistentUtil.NEW_ID;

				// entryId = AEPersistentUtil.NEW_ID;

				// with dateAccounting: when this FinancialTransaction is accounted
				accJournalItem.setDate(ft.getDateAccounting());

				// dateTransaction (dateCreation) are the same: When this FinancialTransaction occurs
				accJournalItem.setDateCreation(ft.getDateTransaction());

				// private AEDescriptor account;
				if(AccJournalEntryTemplate.AccountIdentificationRule.BANK_ACCOUNT.equals(accJournalItem.getAccountIdentificationRule())
						&& bankAccount != null && bankAccount.getAccount() != null) {

					accJournalItem.setAccount(bankAccount.getAccount().getDescriptor());

					// private Double dtAmount;
					if(accJournalItem.isDebit()) {
						accJournalItem.setDtAmount(ft.getAmount());
					}

					// private Double ctAmount;
					if(accJournalItem.isCredit()) {
						accJournalItem.setCtAmount(ft.getAmount());
					}
				} else if(AccJournalEntryTemplate.AccountIdentificationRule.CASH_ACCOUNT.equals(accJournalItem.getAccountIdentificationRule())) {
					// private Double dtAmount;
					if(accJournalItem.isDebit()) {
						accJournalItem.setDtAmount(ft.getAmount());
					}

					// private Double ctAmount;
					if(accJournalItem.isCredit()) {
						accJournalItem.setCtAmount(ft.getAmount());
					}
				} else if(AccJournalEntryTemplate.AccountIdentificationRule.EXPENSE.equals(accJournalItem.getAccountIdentificationRule())) {
					accJournalItem.setAccount(ft.getAccAccountExpInc());

					// private Double dtAmount;
					if(accJournalItem.isDebit()) {
						accJournalItem.setDtAmount(ft.getAmount());
					}

					// private Double ctAmount;
					if(accJournalItem.isCredit()) {
						accJournalItem.setCtAmount(ft.getAmount());
					}
				} else if(AccJournalEntryTemplate.AccountIdentificationRule.REVENUE.equals(accJournalItem.getAccountIdentificationRule())) {
					accJournalItem.setAccount(ft.getAccAccountExpInc());

					// private Double dtAmount;
					if(accJournalItem.isDebit()) {
						accJournalItem.setDtAmount(ft.getAmount());
					}

					// private Double ctAmount;
					if(accJournalItem.isCredit()) {
						accJournalItem.setCtAmount(ft.getAmount());
					}
				} else if(AccJournalEntryTemplate.AccountIdentificationRule.INTERNAL_TRANSFERS.equals(accJournalItem.getAccountIdentificationRule())) {
					accJournalItem.setAccount(ft.getAccAccountExpInc());

					// private Double dtAmount;
					if(accJournalItem.isDebit()) {
						accJournalItem.setDtAmount(ft.getAmount());
					}

					// private Double ctAmount;
					if(accJournalItem.isCredit()) {
						accJournalItem.setCtAmount(ft.getAmount());
					}
				} else if(AccJournalEntryTemplate.AccountIdentificationRule.THIRD_ACCOUNT.equals(accJournalItem.getAccountIdentificationRule())) {
					accJournalItem.setAccount(ft.getAccAccountExpInc());

					// private Double dtAmount;
					if(accJournalItem.isDebit()) {
						accJournalItem.setDtAmount(ft.getAmount());
					}

					// private Double ctAmount;
					if(accJournalItem.isCredit()) {
						accJournalItem.setCtAmount(ft.getAmount());
					}
				} else if(AccJournalEntryTemplate.AccountIdentificationRule.OTHER_RECEIVABLE_PAYABLE.equals(accJournalItem.getAccountIdentificationRule())) {
					// private Double dtAmount;
					if(accJournalItem.isDebit()) {
						accJournalItem.setDtAmount(ft.getAmount());
					}

					// private Double ctAmount;
					if(accJournalItem.isCredit()) {
						accJournalItem.setCtAmount(ft.getAmount());
					}
				} else if(AccJournalEntryTemplate.AccountIdentificationRule.QUETE.equals(accJournalItem.getAccountIdentificationRule())
						&& quetesList != null) {

					// find quete to match
					if(quetesList.size() == 0) {
						accJournalItem.setQuete(null);
						accJournalItem.setDtAmount(0.0);
						accJournalItem.setCtAmount(0.0);
					} else if(quetesList.size() == 1) {
						Quete q = quetesList.get(0);

						// quete extra account data
						accJournalItem.setQuete(q.getDescriptor());

						// dtAmount: get from FinancialTransaction amount
						if(accJournalItem.isDebit()) {
							accJournalItem.setDtAmount(ft.getAmount());
						}

						// ctAmount: get from FinancialTransaction amount
						if(accJournalItem.isCredit()) {
							accJournalItem.setCtAmount(ft.getAmount());
						}
					} else {
						Quete q = null;

						// Match accJournalItem to quete
						Quete.Type qType = Type.QUETE;
						String qCode = AEStringUtil.EMPTY_STRING;
						if(accJournalItem.getQuete() != null) {
							qCode = accJournalItem.getQuete().getCode();
						}
						QuetesList qList = quetesList.get(qType, qCode);
						if(qList.size() == 1) {
							q = qList.get(0);
						}

						if(q != null) {
							// quete extra account data
							accJournalItem.setQuete(q.getDescriptor());

							// dtAmount
							if(accJournalItem.isDebit()) {
								accJournalItem.setDtAmount(q.getAmount());
							}

							// ctAmount
							if(accJournalItem.isCredit()) {
								accJournalItem.setCtAmount(q.getAmount());
							}
						}
					}
				} else if(AccJournalEntryTemplate.AccountIdentificationRule.DF.equals(accJournalItem.getAccountIdentificationRule())
						&& quetesList != null) {

					// find quete to match
					Quete q = null;

					// Match accJournalItem to quete
					Quete.Type qType = Type.DF;
					String qCode = AEStringUtil.EMPTY_STRING;
					if(accJournalItem.getQuete() != null) {
						qCode = accJournalItem.getQuete().getCode();
					}
					QuetesList qList = quetesList.get(qType, qCode);
					if(qList.size() == 1) {
						q = qList.get(0);
					}

					if(q != null) {
						// quete extra account data
						accJournalItem.setQuete(q.getDescriptor());

						// dtAmount
						if(accJournalItem.isDebit()) {
							accJournalItem.setDtAmount(q.getAmount());
						}

						// ctAmount
						if(accJournalItem.isCredit()) {
							accJournalItem.setCtAmount(q.getAmount());
						}
					}
				} else if(AccJournalEntryTemplate.AccountIdentificationRule.CELEBRANT.equals(accJournalItem.getAccountIdentificationRule())
						&& quetesList != null) {

					// find quete to match
					Quete q = null;

					// Match accJournalItem to quete
					Quete.Type qType = Type.CELEBRANT;
					String qCode = AEStringUtil.EMPTY_STRING;
					if(accJournalItem.getQuete() != null) {
						qCode = accJournalItem.getQuete().getCode();
					}
					QuetesList qList = quetesList.get(qType, qCode);
					if(qList.size() == 1) {
						q = qList.get(0);
					}

					if(q != null) {
						// quete extra account data
						accJournalItem.setQuete(q.getDescriptor());

						// dtAmount
						if(accJournalItem.isDebit()) {
							accJournalItem.setDtAmount(q.getAmount());
						}

						// ctAmount
						if(accJournalItem.isCredit()) {
							accJournalItem.setCtAmount(q.getAmount());
						}
					}
				}

				// private AEDescriptor auxiliary;

				// private AEDescriptor reference;

				// private AEDescriptor currency;

				// private boolean debit;

				// private boolean credit;

				// private Date paymentDueDate;

				// private AEDescriptor issuerPaymentType;

				// private AEDescriptor recipientPaymentType;

				// description
				accJournalItem.setDescription(ft.getDescription());

				// contributor
				if(ft.getContributor() != null && !ft.getContributor().isUnrecognized()) {
					accJournalItem.setContributor(ft.getContributor().getDescriptor());
				} else {
					accJournalItem.setContributor(null);
				}

				// supplier
				if(ft.getSupplier() != null) {
					accJournalItem.setSupplier(ft.getSupplier().getDescriptor());
				} else {
					accJournalItem.setSupplier(null);
				}

				// bankAccount
				accJournalItem.setBankAccount(ft.getBankAccount());
			}
		}

		ft.setUpdated();

		/**
		 * Validate
		 */
		AccJournalEntryCommonValidator commonValidator = AccJournalEntryCommonValidator.getInst();
		commonValidator.validate(accJournalEntry);

		AccJournalEtryAlignmentValidator aValidator = AccJournalEtryAlignmentValidator.getInst();
		aValidator.validate(accJournalEntry);
	}

	/**
	 * Inserts, updates or deletes specified contributor regards its DBState.
	 * 
	 * @param c
	 * @param invContext
	 * @param localConnection
	 * @throws AEException
	 */
	private void manage(Contributor c, AEInvocationContext invContext, AEConnection localConnection) throws AEException {
		try {
			ContributorDAO contrDao = DAOFactory.getInstance().getContributorDAO(localConnection);

			switch(c.getPersistentState()) {
			case NEW: 
				// check uniqueness
				String firstName = c.getEmployee().getFirstName();
				String lastName = c.getEmployee().getLastName();
				String street = null;
				if(c.getEmployee().getAddress() != null) {
					street = c.getEmployee().getAddress().getStreet();
				}
				boolean uniqueness = contrDao.contributorUniqueness(
						c.getCompany().getDescriptor(), 
						c.getEmployee().getPerson().getDescriptor(),
						AEStringUtil.trim(firstName), 
						AEStringUtil.trim(lastName), 
						AEStringUtil.trim(street));
				if(!uniqueness) {
					throw AEError.System.CONTRIBUTOR_NOT_UNIQUE.toException();
				}

				partyLocal.manage((Person) c.getEmployee().getPerson(), invContext, localConnection);
				partyLocal.save(c.getEmployee(), invContext, localConnection);
				contrDao.insert(c);
				break;
			case UPDATED:
				if(c.getEmployee().getPerson() instanceof Person) {
					partyLocal.manage((Person) c.getEmployee().getPerson(), invContext, localConnection);
				}
				partyLocal.save(c.getEmployee(), invContext, localConnection);
				contrDao.update(c);
				break;
			case DELETED: 
				// currently Contributors are not deleted!!!
				break;
			default: 
				break;
			}
		} catch (Throwable t) {
			throw new AEException(t);
		} 
	}

	/**
	 * Inserts or updates specified supplier regards its id.
	 * Returns true on insert
	 * 
	 * @param c
	 * @param invContext
	 * @param localConnection
	 * @throws AEException
	 */
	private boolean save(SimpleParty sp, AEInvocationContext invContext, AEConnection localConnection) throws AEException {
		boolean insert = false;
		try {
			SimplePartyDAO spDao = DAOFactory.getInstance().getSimplePartyDAO(localConnection);

			if(!AEPersistent.ID.isPersistent(sp.getID())) {
				// insert
				spDao.insert(sp);
				insert = true;
			} else {
				// updated
				spDao.update(sp);
			}
			return insert;
		} catch (Throwable t) {
			throw new AEException(t);
		} 
	}
	
	private boolean saveSupplier(AEDescriptor suppl, AEDescriptor tenantDescriptor, AEInvocationContext invContext, AEConnection localConnection) throws AEException {
		SimpleParty sParty = new SimpleParty();
		sParty.setID(suppl.getID());
		sParty.setName(suppl.getName());
		sParty.setCompany(tenantDescriptor);
		
		boolean insert = save(sParty, invContext, localConnection);
		
		suppl.setID(sParty.getID());
		
		return insert;
	}

	@Override
	public AEResponse loadContributors(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);
			long sOwnerId = arguments.getLong(AEDomainObject.JSONKey.sOwnerId.name());

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
			authorize(new AuthPermission("System/FinancialTransaction/Contributor", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			// Load contributors
			ContributorDAO contributorDAO = daoFactory.getContributorDAO(localConnection);
			ContributorsList contributorsList = contributorDAO.loadContributorsToCompany(ownerDescr);

			// Sync donations
			int year = arguments.optInt("year", 0);
			if(year <= 0) {
				AccPeriod firstOpenPeriod = getFirstOpenPeriodExact(ownerId, AEApp.ACCOUNTING_MODULE_ID, localConnection);
				if(firstOpenPeriod == null) {
					// get year now
					year = AEDateUtil.getYear(new Date());
				} else {
					// there is open period
					// paranoic check end date
					if(firstOpenPeriod.getEndDate() != null) {
						year = AEDateUtil.getYear(firstOpenPeriod.getEndDate());
					} else {
						year = AEDateUtil.getYear(new Date());
					}
				}
			}
			JSONObject jsonObject = accLocal.syncDonationsJava(ownerDescr, year, localConnection);
			Object javaObject = jsonObject.opt("donations");
			if(javaObject instanceof ContributorDonationsList) {
				ContributorDonationsList donations = (ContributorDonationsList) javaObject;
				
				// check for notreceipted donations
				for (Iterator<Contributor> iterator = contributorsList.iterator(); iterator.hasNext();) {
					Contributor contr = (Contributor) iterator.next();
					ContributorDonation donation = donations.getByPerson(contr.getEmployee().getPerson().getDescriptor().getID());
					// if(donation != null && donation.amount > 0.0) {
					if(donation != null && AEMath.isPositiveAmount(donation.amount - donation.getAmountReceipted())) {
						contr.setChecked(true);
					}
				}
			}

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject();
			payload.put(FinancialTransaction.JSONKey.contributors.name(), contributorsList.toJSONArray());
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
	public AEResponse loadAccJournalItemsByFilter(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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

			// Filter
			JSONObject filterJson = arguments.getJSONObject(AccJournalEntry.JSONKey.accJournalItemsFilter.name());

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
			authorize(new AuthPermission("System/FinancialTransaction", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * Create Filter
			 */
			AccJournalFilter accJournalFilter = new AccJournalFilter();
			accJournalFilter.create(filterJson);

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * Execute filter
			 * 
			 */
			AccJournalDAO accJournalDAO = DAOFactory.getInstance().getAccJournalDAO(localConnection);
			AccJournalResultsList accJournalResults = null;
			PaymentMethod pm = accJournalFilter.getPaymentMethod();
			boolean tally = false;
			AccAccountBalance accBalanceTallyAll = null;
			AccAccountBalance accBalanceTallyChecked = null;
			AccAccountBalance accBalanceTallyUnchecked = null;
			
			if(pm == null) {
				// journal is not specified as payment method, 
				// but may be specified as accJournal field in accJournalFilter
				AEDescriptor journalDescr = accJournalFilter.getAccJournal();
				if(journalDescr != null && !AEStringUtil.isEmpty(journalDescr.getCode())) {
					// concrete journal
					if(PaymentMethod.NOUVEAU.getCode().equalsIgnoreCase(journalDescr.getCode())) {
						accJournalResults = accJournalDAO.loadOpeningBalance(accJournalFilter);
					} else {
						accJournalResults = accJournalDAO.load(accJournalFilter);
						
						// test for bank journal
						String journalCode = journalDescr.getCode();
						JSONObject bankAeRequestArguments = new JSONObject();
						bankAeRequestArguments.put(AEDomainObject.JSONKey.ownerId.name(), ownerId);
						bankAeRequestArguments.put(AEDomainObject.JSONKey.sOwnerId.name(), sOwnerId);
						AERequest bankAeRequest = new AERequest(bankAeRequestArguments).withAuthPrincipal(ap);
						AEResponse bankAccountsAeResponse = cashLocal.loadBankAccounts(bankAeRequest);
						JSONArray bankAccountsAll = bankAccountsAeResponse.getPayload().getJSONArray(FinancialTransaction.JSONKey.bankAccounts.name());
						if(bankAccountsAll != null) {
							for (int j = 0; j < bankAccountsAll.length(); j++) {
								JSONObject bankAccount = bankAccountsAll.getJSONObject(j);
								if(bankAccount != null && journalCode.equalsIgnoreCase(bankAccount.optString("journalCode"))) {
									// deal with tally
									tally = true;
									
									// to endDate
									Date endDate = AEDateUtil.getClearDateTime(AEDateUtil.now());
									
									// for account
									AEDescriptor accDescr = AccAccount.lazyDescriptor(bankAccount.getLong(BankAccount.JSONKey.accId.name()));
									
									// detect the last balance
									Integer year = accJournalDAO.loadLastOpeningBalanceYear(accDescr, endDate);
									Date startDate = null;
									if(year != null) {
										startDate = AEDateUtil.getClearDateTime(AEDateUtil.getFirstDate(0, year));
									} else {
										startDate = AEDateUtil.getClearDateTime(new Date(0L));
									}
									
									AETimePeriod period = new AETimePeriod(startDate, endDate);
									
									accBalanceTallyAll = new AccAccountBalance()
										.withAccAccount(accDescr)
										.withPeriod(period);
									accJournalDAO.loadOpeningBalance(accBalanceTallyAll);
									accJournalDAO.calculateTurnover(accBalanceTallyAll);
									accBalanceTallyAll.calculateFinalBalance();
									
									accBalanceTallyChecked = new AccAccountBalance()
										.withAccAccount(accDescr)
										.withPeriod(period);
									// treat opening balance as checked
									accJournalDAO.loadOpeningBalance(accBalanceTallyChecked);
									accJournalDAO.calculateTurnoverByTally(accBalanceTallyChecked, true);
									accBalanceTallyChecked.calculateFinalBalance();
									
									accBalanceTallyUnchecked = new AccAccountBalance()
										.withAccAccount(accDescr)
										.withPeriod(period);
									// do not load opening balance
									accJournalDAO.calculateTurnoverByTally(accBalanceTallyUnchecked, false);
									accBalanceTallyUnchecked.calculateFinalBalance();
									
									break;
								}
							}
						}
					}
				} else {
					// all journals
					accJournalResults = accJournalDAO.loadOpeningBalance(accJournalFilter);
					accJournalResults.addAll(accJournalDAO.load(accJournalFilter));
				}
			} else if(FinancialTransactionTemplate.PaymentMethod.NOUVEAU.equals(pm)) {
				// request for NOUVEAU journal (balances)
				accJournalResults = accJournalDAO.loadOpeningBalance(accJournalFilter);
			} else {
				// request for standart journal
				accJournalResults = accJournalDAO.load(accJournalFilter);
			}

			// paranoic
			if(accJournalResults == null) {
				accJournalResults = new AccJournalResultsList();
			}

			/**
			 * AccBalance
			 */
			Date dateTo = new Date();
			Date dateFrom = AEDateUtil.beginOfTheYear(dateTo);
			AETimePeriod period = new AETimePeriod(dateFrom, dateTo);

			AccAccountBalance accAccountBalance = null;
			AccountDAO accDAO = daoFactory.getAccountDAO(localConnection);
			AEDescriptor bankAccountDescr = accJournalFilter.getBankAccount();
			if(FinancialTransactionTemplate.PaymentMethod.BANK.equals(pm)) {
				// load bankAccount
				BankAccountDAO bankAccountDAO = daoFactory.getBankAccountDAO(localConnection);
				BankAccount bankAccount = bankAccountDAO.load(bankAccountDescr.getID());

				// accBalance
				accAccountBalance = new AccAccountBalance();
				JSONObject accJson = accDAO.loadAccountByIdFull(bankAccount.getAccount().getDescriptor().getID());
				if(accJson != null) {
					accAccountBalance.setAccAccount(
							new AEDescriptorImp(accJson.getLong(AEDomainObject.JSONKey.id.name()))
							.withCode(accJson.optString(AEDomainObject.JSONKey.code.name()))
							.withName(accJson.optString(AEDomainObject.JSONKey.name.name()))
							.withDescription(bankAccount.getName()));
					accAccountBalance.setPeriod(period);
					accLocal.loadAccFinalBalance(accAccountBalance, invContext, localConnection);
				}
			} else if(FinancialTransactionTemplate.PaymentMethod.CASH.equals(pm)) {
				// accBalance
				JSONArray accsJson = accDAO.loadAccounts(ownerId, " and (acc.CODE = '5300')");
				if(accsJson != null && accsJson.length() == 1) {
					JSONObject accJson = accsJson.getJSONObject(0);

					// accBalance
					accAccountBalance = new AccAccountBalance();
					accAccountBalance.setAccAccount(
							new AEDescriptorImp(accJson.getLong(AEDomainObject.JSONKey.id.name()))
							.withCode(accJson.optString(AEDomainObject.JSONKey.code.name()))
							.withName(accJson.optString(AEDomainObject.JSONKey.name.name()))
							.withDescription(pm.getName()));
					accAccountBalance.setPeriod(period);
					accLocal.loadAccFinalBalance(accAccountBalance, invContext, localConnection);

				}
			}


			/**
			 * Load attachments
			 */
			FileAttachmentDAO fileAttachmentDAO = DAOFactory.getInstance().getFileAttachmentDAO(localConnection);
			Set<AEDescriptor> accJournalEntryDescr = accJournalResults.getAccJournalEntryDescriptors();
			AEDescriptorsList accJournalEntryDescrList = new AEDescriptorsList();
			for (Iterator<AEDescriptor> iterator = accJournalEntryDescr.iterator(); iterator.hasNext();) {
				AEDescriptor aeDescriptor = (AEDescriptor) iterator.next();
				accJournalEntryDescrList.add(aeDescriptor);

			}
			JSONArray att = fileAttachmentDAO.loadToMultipleLazzy(accJournalEntryDescrList);

			/**
			 * Build response
			 */
			JSONObject payload = new JSONObject();
			payload.put(AccJournalResult.JSONKey.accJournalResults.name(), accJournalResults.toJSONArray());
			if(accAccountBalance != null) {
				payload.put(AccAccountBalance.JSONKey.accBalance.name(), accAccountBalance.toJSONObject());
			}
			payload.put("tally", tally);
			payload.put("attachments", att);
			if(tally) {
				JSONObject tallyAmounts = new JSONObject();
				if(accBalanceTallyAll != null) {
					tallyAmounts.put(AccJournalFilter.TallyState.all.name(), accBalanceTallyAll.getFinalBalance());
				}
				if(accBalanceTallyChecked != null) {
					tallyAmounts.put(AccJournalFilter.TallyState.checked.name(), accBalanceTallyChecked.getFinalBalance());
				}
				if(accBalanceTallyUnchecked != null) {
					tallyAmounts.put(AccJournalFilter.TallyState.unchecked.name(), accBalanceTallyUnchecked.getFinalBalance());
				}
				payload.put("tallyAmounts", tallyAmounts);
			}
			return new AEResponse(payload);
		} catch (Exception e) {
			logger.error(e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadAccJournals(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);
			long sOwnerId = arguments.getLong(AEDomainObject.JSONKey.sOwnerId.name());
			boolean loadAccAccounts = false;
			if(arguments.has(AccJournal.JSONKey.loadAccAccounts.name())) {
				loadAccAccounts = arguments.optBoolean(AccJournal.JSONKey.loadAccAccounts.name());
			}
			boolean loadSuppliers = false;
			if(arguments.has(AccJournal.JSONKey.loadSuppliers.name())) {
				loadSuppliers = arguments.optBoolean(AccJournal.JSONKey.loadSuppliers.name());
			}

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
			authorize(new AuthPermission("System/Configuration/Customer/Journal", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * Factory, daos and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			AccountDAO accDAO = daoFactory.getAccountDAO(localConnection);

			// Turnover period
			Date dateTo = new Date();
			Date dateFrom = AEDateUtil.beginOfTheYear(dateTo);
			AETimePeriod period = new AETimePeriod(dateFrom, dateTo);

			/**
			 * Load all money acc accounts
			 */
			JSONArray moneyAccountsAll = new JSONArray();

			// load 412XX accounts			
			JSONArray accounts512X = accDAO.loadAccounts(ownerId, " and (acc.CODE like '512%')");
			JSONUtil.addAll(moneyAccountsAll, accounts512X);

			// load 53XX accounts
			JSONArray accounts53XX = accDAO.loadAccounts(ownerId, " and (acc.CODE like '53%')");
			JSONUtil.addAll(moneyAccountsAll, accounts53XX);

			/**
			 * Load available journals for specified customers
			 */
			JSONArray accJournals = new JSONArray();
			FinancialTransactionTemplate.PaymentMethod[] paymentMethods = FinancialTransactionTemplate.PaymentMethod.values();
			for (int i = 0; i < paymentMethods.length; i++) {
				FinancialTransactionTemplate.PaymentMethod pm = paymentMethods[i];
				JSONObject accJournal = null;
				if(FinancialTransactionTemplate.PaymentMethod.BANK.equals(pm)) {
					JSONObject bankAeRequestArguments = new JSONObject();
					bankAeRequestArguments.put(AEDomainObject.JSONKey.ownerId.name(), ownerId);
					bankAeRequestArguments.put(AEDomainObject.JSONKey.sOwnerId.name(), sOwnerId);
					AERequest bankAeRequest = new AERequest(bankAeRequestArguments).withAuthPrincipal(ap);
					AEResponse bankAccountsAeResponse = cashLocal.loadBankAccounts(bankAeRequest);
					JSONArray bankAccountsAll = bankAccountsAeResponse.getPayload().getJSONArray(FinancialTransaction.JSONKey.bankAccounts.name());
					if(bankAccountsAll != null) {
						for (int j = 0; j < bankAccountsAll.length(); j++) {
							JSONObject bankAccount = bankAccountsAll.getJSONObject(j);

							accJournal = new JSONObject();

							// init
							accJournal.put(AEDomainObject.JSONKey.name.name(), bankAccount.getString(AEDomainObject.JSONKey.name.name()));
							accJournal.put(AEDomainObject.JSONKey.code.name(), bankAccount.getString(BankAccount.JSONKey.journalCode.name()));
							accJournal.put(FinancialTransaction.JSONKey.paymentMethodId.name(), pm.getId());
							accJournal.put(FinancialTransaction.JSONKey.bankAccountId.name(), bankAccount.getLong(AEDomainObject.JSONKey.id.name()));

							// accBalance
							AccAccountBalance accAccountBalance = new AccAccountBalance();
							JSONObject accAccountJson = accDAO.loadAccountByIdFull(bankAccount.getLong(BankAccount.JSONKey.accId.name()));
							if(accAccountJson != null) {
								accAccountBalance.setAccAccount(
										new AEDescriptorImp(accAccountJson.getLong(AEDomainObject.JSONKey.id.name()))
										.withCode(accAccountJson.optString(AEDomainObject.JSONKey.code.name()))
										.withName(accAccountJson.optString(AEDomainObject.JSONKey.name.name()))
										.withDescription(accJournal.optString(AEDomainObject.JSONKey.name.name())));
								accAccountBalance.setPeriod(period);
								accLocal.loadAccFinalBalance(accAccountBalance, invContext, localConnection);
								accJournal.put(AccAccountBalance.JSONKey.accBalance.name(), accAccountBalance.toJSONObject());
							}

							// accBlackList
							// - all accounts from moneyAccountsAll are forbidden except the counterpart account
							JSONArray accBlackList = new JSONArray();
							if(accAccountJson != null && moneyAccountsAll != null) {
								for (int k = 0; k < moneyAccountsAll.length(); k++) {
									JSONObject moneyAcc = moneyAccountsAll.getJSONObject(k);
									if(moneyAcc.getLong(AEDomainObject.JSONKey.id.name()) != accAccountJson.getLong(AEDomainObject.JSONKey.id.name())) {
										accBlackList.put(moneyAcc);
									}
								}
							}
							accJournal.put(AccJournalEntry.JSONKey.accBlackList.name(), accBlackList);

							accJournals.put(accJournal);
						}
					}
				} else if(FinancialTransactionTemplate.PaymentMethod.CASH.equals(pm)) {
					JSONArray cashAccAccounts = accDAO.loadAccounts(ownerId, " and (acc.CODE = '5300')");
					if(cashAccAccounts != null && cashAccAccounts.length() == 1) {
						// counterpart account
						JSONObject accAccountJson = cashAccAccounts.getJSONObject(0);

						// journal
						accJournal = new JSONObject();
						accJournal.put(AEDomainObject.JSONKey.name.name(), pm.getName());
						accJournal.put(AEDomainObject.JSONKey.code.name(), pm.getCode());
						accJournal.put(FinancialTransaction.JSONKey.paymentMethodId.name(), pm.getId());

						// balance
						AccAccountBalance accAccountBalance = new AccAccountBalance();
						accAccountBalance.setAccAccount(
								new AEDescriptorImp(accAccountJson.getLong(AEDomainObject.JSONKey.id.name()))
								.withCode(accAccountJson.optString(AEDomainObject.JSONKey.code.name()))
								.withName(accAccountJson.optString(AEDomainObject.JSONKey.name.name()))
								.withDescription(accJournal.optString(AEDomainObject.JSONKey.name.name())));
						accAccountBalance.setPeriod(period);
						accLocal.loadAccFinalBalance(accAccountBalance, invContext, localConnection);
						accJournal.put(AccAccountBalance.JSONKey.accBalance.name(), accAccountBalance.toJSONObject());

						// accBlackList:
						// - 512X accounts are forbidden for journal caisse
						JSONArray accBlackList = new JSONArray();
						if(accounts512X != null) {
							JSONUtil.addAll(accBlackList, accounts512X);
						}
						accJournal.put(AccJournalEntry.JSONKey.accBlackList.name(), accBlackList);

						accJournals.put(accJournal);
					}
				} else if(FinancialTransactionTemplate.PaymentMethod.VARIOUS.equals(pm)) {
					accJournal = new JSONObject();

					accJournal.put(AEDomainObject.JSONKey.name.name(), pm.getName());
					accJournal.put(AEDomainObject.JSONKey.code.name(), pm.getCode());
					accJournal.put(FinancialTransaction.JSONKey.paymentMethodId.name(), pm.getId());

					// accBlackList
					JSONArray accBlackList = new JSONArray();
					accJournal.put(AccJournalEntry.JSONKey.accBlackList.name(), accBlackList);

					accJournals.put(accJournal);
				} else if(FinancialTransactionTemplate.PaymentMethod.NOUVEAU.equals(pm)) {
					accJournal = new JSONObject();

					accJournal.put(AEDomainObject.JSONKey.name.name(), pm.getName());
					accJournal.put(AEDomainObject.JSONKey.code.name(), pm.getCode());
					accJournal.put(FinancialTransaction.JSONKey.paymentMethodId.name(), pm.getId());

					// accBlackList
					JSONArray accBlackList = new JSONArray();
					accJournal.put(AccJournalEntry.JSONKey.accBlackList.name(), accBlackList);

					// not modifiable
					accJournal.put("modifiable", false);

					accJournals.put(accJournal);
				}
			}

			/**
			 * Filter
			 * Get the last acc journal entry
			 */
			AccJournalDAO accJournalDAO = daoFactory.getAccJournalDAO(localConnection);
			Date lastEntryDate = accJournalDAO.loadLastDate(ownerDescr);
			if(lastEntryDate == null) {
				lastEntryDate = new Date();
			}
			Calendar time = AEDateUtil.getEuropeanCalendar(lastEntryDate);
			JSONObject accJournalFilter = new JSONObject();

			// month
			accJournalFilter.put(AccJournalFilter.JSONKey.month.name(), time.get(Calendar.MONTH) + 1);
			accJournalFilter.put(AccJournalFilter.JSONKey.year.name(), time.get(Calendar.YEAR));

			// period
			Date startFiscalYear = AEDateUtil.beginOfTheYear(time.getTime());
			Date endFiscalYear = AEDateUtil.endOfTheYear(time.getTime());
			accJournalFilter.put(AccJournalFilter.JSONKey.dateFrom.name(), AEDateUtil.formatToSystem(startFiscalYear));
			accJournalFilter.put(AccJournalFilter.JSONKey.dateTo.name(), AEDateUtil.formatToSystem(endFiscalYear));

			// month as timeControl
			accJournalFilter.put(AccJournalFilter.JSONKey.timeControl.name(), AccJournalFilter.JSONKey.monthFilter.name());

			/**
			 * AccAccounts
			 */
			JSONArray accounts = null;
			if(loadAccAccounts) {
				AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);
				accounts = accountDAO.loadAccountsByOwner(ownerId);
			}

			/**
			 * Suppliers
			 */
			SimplePartiesList spList = null;
			if(loadSuppliers) {
				SimplePartyDAO spDAO = daoFactory.getSimplePartyDAO(localConnection);
				spList = spDAO.loadToCompany(ownerDescr);
			}

			/**
			 * Build response
			 */
			JSONObject payload = new JSONObject()
			.put(AccJournalEntry.JSONKey.accJournals.name(), accJournals)
			.put(AccJournalFilter.JSONKey.accJournalFilter.name(), accJournalFilter);
			if(accounts != null) {
				payload.put(AccAccount.JSONKey.accounts.name(), accounts);
			}
			if(spList != null) {
				payload.put(FinancialTransaction.JSONKey.suppliers.name(), spList.toJSONArray());
			}
			return new AEResponse(payload);
		} catch (Exception e) {
			logger.error(e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadFinancialTransaction(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);
			long sOwnerId = arguments.getLong(AEDomainObject.JSONKey.sOwnerId.name());

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
			authorize(new AuthPermission("System/FinancialTransaction", AuthPermission.READ | AuthPermission.SAVE_AND_DELETE), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * Concrete arguments
			 */
			Long accJournalItemId = JSONUtil.optLong(arguments, "accJournalItemId");
			Long financialTransactionId = JSONUtil.optLong(arguments, "financialTransactionId");

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * Load FinancialTransactions List
			 */
			FinancialTransactionList ftList = new FinancialTransactionList();
			FinancialTransaction ft = null;
			if(financialTransactionId != null) {
				ft = loadFinancialTransactionById(financialTransactionId, invContext, localConnection);
			} else if (accJournalItemId != null) {
				ft = loadFinancialTransactionByAccJournaItemlId(accJournalItemId, invContext, localConnection);
			} else {
				throw AEError.System.INVALID_REQUEST.toException();
			}
			if(ft != null) {
				ftList.add(ft);
			} else {
				throw new AEException("Vous êtes en mode consultation, pour enregistrer ou modifier une écriture, utilisez soit le mode saisie guidée ou le mode saisie libre.");
			}

			/**
			 * Load FinancialTransaction's module
			 */
			AEDescriptor ftTemplateDescr = ft.getFtTemplate();
			if(ftTemplateDescr == null) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}
			FinancialTransactionDAO finTransDAO = daoFactory.getFinancialTransactionDAO(localConnection);
			AEDescriptor appModuleTemplateDescr = finTransDAO.loadAppModuleTemplateDescrByFTT(ftTemplateDescr);

			AppDAO appDAO = daoFactory.getAppDAO(localConnection);
			AEAppModule appModuleTemplate = appDAO.loadAppModule(appModuleTemplateDescr.getID());
			if(appModuleTemplate == null) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}
			AEAppModule appModule = appDAO.loadAppModuleByRelationFrom(appModuleTemplate.getCode(), AEAppModule.AppModuleRelation.FROM_IS_TEMPLATE_TO);
			if(appModule == null) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}

			AEAppModulesList appModulePath = appDAO.loadAppModulePath(appModule.getCode());
			StringBuilder appModuleNamePathBuilder = new StringBuilder();
			StringBuilder appModuleCodePathBuilder = new StringBuilder();
			for (AEAppModule module : appModulePath) {
				if(!AEStringUtil.isEmpty(appModuleNamePathBuilder)) {
					appModuleNamePathBuilder.append(" - ");
				}
				appModuleNamePathBuilder.append(module.getName());

				if(!AEStringUtil.isEmpty(appModuleCodePathBuilder)) {
					appModuleCodePathBuilder.append("/");
				}
				appModuleCodePathBuilder.append(module.getCode());
			}

			/**
			 * Load all acc accounts 
			 */
			AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);
			JSONArray accounts = accountDAO.loadAccountsByOwner(ownerId);

			/**
			 * Load suppliers
			 */
			SimplePartyDAO spDAO = daoFactory.getSimplePartyDAO(localConnection);
			SimplePartiesList spList = spDAO.loadToCompany(ownerDescr);

			// build response
			JSONObject payload = new JSONObject();
			payload.put(AEAppModule.JSONKey.appModuleNamePath.name(), appModuleNamePathBuilder.toString());
			payload.put(AEAppModule.JSONKey.appModuleCodePath.name(), appModuleCodePathBuilder.toString());
			payload.put(AEAppModule.JSONKey.appModuleId.name(), appModule.getID());
			payload.put(AEAppModule.JSONKey.appModuleCode.name(), appModule.getCode());
			payload.put(AccAccount.JSONKey.accounts.name(), accounts);
			payload.put(FinancialTransaction.JSONKey.financialTransactions.name(), ftList.toJSONArray());
			payload.put(FinancialTransaction.JSONKey.suppliers.name(), spList.toJSONArray());
			return new AEResponse(payload);
		} catch (Exception e) {
			logger.error(e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private FinancialTransaction loadFinancialTransactionByAccJournaItemlId(long accJournalItemId, AEInvocationContext invContext, AEConnection localConnection) throws AEException {
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();
			FinancialTransactionDAO ftDAO = daoFactory.getFinancialTransactionDAO(localConnection);

			// load FinancialTransactionId by accJournalItemId
			FinancialTransaction ft = ftDAO.loadSQLFinancialTransactionIdByItemId(accJournalItemId);
			if(ft != null) {
				loadInDepth(ft, invContext, localConnection);
			}

			return ft;
		} catch (Throwable t) {
			throw new AEException(t);
		} 
	}

	private FinancialTransaction loadFinancialTransactionById(long financialTransactionId, AEInvocationContext invContext, AEConnection localConnection) throws AEException {
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();
			FinancialTransactionDAO ftDAO = daoFactory.getFinancialTransactionDAO(localConnection);

			// load FinancialTransactionId by financialTransactionId
			AEDescriptor ftDescr = FinancialTransaction.lazyDescriptor(financialTransactionId);
			FinancialTransaction ft = ftDAO.loadFinancialTransaction(ftDescr);
			if(ft != null) {
				loadInDepth(ft, invContext, localConnection);
			}

			return ft;
		} catch (Throwable t) {
			throw new AEException(t);
		} 
	}

	private void loadInDepth(FinancialTransaction ft, AEInvocationContext invContext, AEConnection localConnection) throws AEException {
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();

			// ACC_JOURNAL_ENTRY_ID and lists of available acounts
			if(ft.getAccJournalEntry() != null) {
				// AccJournalEntry
				AccJournalDAO accJournalDAO = daoFactory.getAccJournalDAO(localConnection);

				AccJournalEntry accJournalEntry = accJournalDAO.loadEntry(ft.getAccJournalEntry().getID());
				AccJournalItemsList accJournalItems = accJournalDAO.loadItemsToEntry(accJournalEntry.getID());
				accJournalEntry.setAccJournalItems(accJournalItems);

				ft.setAccJournalEntry(accJournalEntry);

				// available acounts
				JSONArray expIncAccounts = new JSONArray();
				JSONArray cashAccounts = new JSONArray();
				JSONArray otherReceivablePayableAccounts = new JSONArray();
				if(accJournalItems != null) {
					for (AccJournalItem accJournalItem : accJournalItems) {
						AccJournalEntryTemplate.AccountIdentificationRule accIdentificationRule = accJournalItem.getAccountIdentificationRule();

						AEDescriptive accDescriptive = accJournalItem.getAccount();
						AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);
						JSONObject accAccount = accountDAO.loadAccountByIdFull(accDescriptive.getDescriptor().getID());
						AEDescriptive modelAccDescriptive = AccAccount.lazyDescriptor(
								accAccount.getJSONObject(AccAccount.JSONKey.parent.name()).getLong(AEDomainObject.JSONKey.id.name()));

						// load concrete accounts from individual COA
						JSONArray availableConcreteAccounts = null;
						if(modelAccDescriptive != null) {
							JSONObject modelAccJson = accountDAO.loadAccountByIdFull(modelAccDescriptive.getDescriptor().getID());
							availableConcreteAccounts = loadAvailableConcreteAccounts(modelAccJson, ft.getCompany().getDescriptor().getID(), localConnection);
						}

						if(AccJournalEntryTemplate.AccountIdentificationRule.EXPENSE.equals(accIdentificationRule)
								|| AccJournalEntryTemplate.AccountIdentificationRule.REVENUE.equals(accIdentificationRule)
								|| AccJournalEntryTemplate.AccountIdentificationRule.INTERNAL_TRANSFERS.equals(accIdentificationRule)
								|| AccJournalEntryTemplate.AccountIdentificationRule.THIRD_ACCOUNT.equals(accIdentificationRule)) {

							if(availableConcreteAccounts != null) {
								JSONUtil.addAll(expIncAccounts, availableConcreteAccounts);
							}
						} else if(AccJournalEntryTemplate.AccountIdentificationRule.CASH_ACCOUNT.equals(accIdentificationRule)) {
							if(availableConcreteAccounts != null) {
								JSONUtil.addAll(cashAccounts, availableConcreteAccounts);
							}
						} else if(AccJournalEntryTemplate.AccountIdentificationRule.OTHER_RECEIVABLE_PAYABLE.equals(accIdentificationRule)) {
							if(availableConcreteAccounts != null) {
								JSONUtil.addAll(otherReceivablePayableAccounts, availableConcreteAccounts);
							}
						}
					}
					ft.setExpIncAccounts(expIncAccounts);
					ft.setCashAccounts(cashAccounts);
					ft.setOtherReceivablePayableAccounts(otherReceivablePayableAccounts);
				}
			}

			// CONTRIBUTOR
			if(ft.getContributor() != null) {
				ContributorDAO contrDAO = daoFactory.getContributorDAO(localConnection);
				Contributor contr = contrDAO.loadContributor(ft.getContributor().getDescriptor());
				ft.setContributor(contr);
			}

			// SUPPLIER
			if(ft.getSupplier() != null) {
				SimplePartyDAO spDAO = daoFactory.getSimplePartyDAO(localConnection);
				SimpleParty sp = spDAO.load(ft.getSupplier().getDescriptor());
				ft.setSupplier(sp);
			}

			// QUETES
			FinancialTransactionDAO ftDAO = daoFactory.getFinancialTransactionDAO(localConnection);
			QuetesList qList = ftDAO.loadFinancialTransactionDetails(ft.getDescriptor());
			ft.setQuetesList(qList);
		} catch (Throwable t) {
			throw new AEException(t);
		} 
	}

	@Override
	public AEResponse saveAccJournalItems(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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

			/**
			 * Authorize for concrete tenant
			 */
			// common attributes
			long ownerId = arguments.getLong(AEDomainObject.JSONKey.ownerId.name());
			AEDescriptor tenantDescriptor = Organization.lazyDescriptor(ownerId);
			long sOwnerId = arguments.getLong(AEDomainObject.JSONKey.sOwnerId.name());

			// state validation
			if(ownerId != sOwnerId) {
				throw AEError.System.SECURITY_VIOLATION.toException();
			}

			// whether this principal is authorized for specified tenant
			ap.ownershipValidator(ownerId);
			
			/**
			 * Authorize 
			 */
			authorize(new AuthPermission("System/AccJournal", AuthPermission.SAVE), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * Start execution
			 */			
			// accJournal
			JSONObject accJournalJson = arguments.getJSONObject(AccJournal.JSONKey.accJournal.name());
			AccJournal accJournal = new AccJournal();
			accJournal.create(accJournalJson);
			PaymentMethod paymentMethod = accJournal.getPaymentMethod();

			// paymentMethod is required and different from balance journal
			if(paymentMethod == null || PaymentMethod.NA.equals(paymentMethod) || PaymentMethod.NOUVEAU.equals(paymentMethod)) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}

			// AccJournalItems
			JSONArray accJournalItemsJson = arguments.getJSONArray(AccJournalEntry.JSONKey.accJournalItems.name());
			AccJournalItemsList accJournalItems = new AccJournalItemsList();
			accJournalItems.create(accJournalItemsJson);

			// DB connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			BankAccountDAO bankAccountDAO = daoFactory.getBankAccountDAO(localConnection);
			AccJournalDAO accJournalDAO = daoFactory.getAccJournalDAO(localConnection);

			// if the type of the journal is Bank, bankAccount is required and check the tenant
			AEDescriptor bankAccountDescr = accJournal.getBankAccount();
			if(PaymentMethod.BANK.equals(paymentMethod)) {
				if(bankAccountDescr == null || !AEPersistent.ID.isPersistent(bankAccountDescr.getID())) {
					throw AEError.System.INVALID_PARAMETER.toException();
				}

				if(!bankAccountDAO.checkTenant(bankAccountDescr, tenantDescriptor)) {
					throw AEError.System.INVALID_PARAMETER.toException();
				}
			}

			/**
			 * Prepare for save
			 */

			// setup accJournalCode
			setupAccJournalCode(accJournal, localConnection);

			// distribute and setup accJournalItems for insert and update
			AccJournalItemsList accJournalItemsNew = new AccJournalItemsList();
			AccJournalItemsList accJournalItemsUpdated = new AccJournalItemsList();
			for (AccJournalItem accJournalItem : accJournalItems) {		
				if(accJournalItem.isNew() || accJournalItem.isUpdated()) {
					// common preparation
					accJournalItem.setDebit(!AEMath.isZeroAmount(accJournalItem.getDtAmount()));
					accJournalItem.setCredit(!AEMath.isZeroAmount(accJournalItem.getCtAmount()));
					if(!accJournalItem.isDebit() && !accJournalItem.isCredit()) {
						accJournalItem.setDebit(true);
					}
					accJournalItem.setJournal(accJournal.getDescriptor());
					accJournalItem.setBankAccount(bankAccountDescr);
					accJournalItem.setCompany(tenantDescriptor);

					// distribute new and updated items
					if(accJournalItem.isNew()) {
						accJournalItem.setAccountIdentificationRule(AccountIdentificationRule.COMMON_OPERATION);
						accJournalItem.setJournalIdentificationRule(paymentMethod.getJournalIdentificationRule());

						// 
						long entryId = accJournalItem.getEntryId();
						accJournalItem.setEntryIdGui(entryId); // keep the GUI entryId
						if(entryId == 0) {
							entryId = Long.MAX_VALUE;
						} else {
							entryId = Math.abs(entryId);
						}
						accJournalItem.setEntryId(entryId);

						// add to the new accJournalItems
						accJournalItemsNew.add(accJournalItem);
					} else if(accJournalItem.isUpdated()) {
						// add to the updated accJournalItems
						accJournalItemsUpdated.add(accJournalItem);
					}
				}
			}

			// validate accounts
			accJournalDAO.validateAccounts(accJournalItemsNew, tenantDescriptor);
			accJournalDAO.validateAccounts(accJournalItemsUpdated, tenantDescriptor);

			// distribute new AccJournalItems by journal entries
			SortedMap<Long, AccJournalEntry> accJournalEntriesNewMap = new TreeMap<Long, AccJournalEntry>();
			Map<Long, AccJournalEntry> accJournalEntriesNewMapIdGui = new HashMap<Long, AccJournalEntry>();
			for (AccJournalItem accJournalItemNew : accJournalItemsNew) {
				long entryId = accJournalItemNew.getEntryId();
				AccJournalEntry recognizedAccJournalEntry = accJournalEntriesNewMap.get(entryId);
				if(recognizedAccJournalEntry == null) {
					recognizedAccJournalEntry = new AccJournalEntry();
					recognizedAccJournalEntry.setCompany(tenantDescriptor);
					accJournalEntriesNewMap.put(entryId, recognizedAccJournalEntry);
					accJournalEntriesNewMapIdGui.put(accJournalItemNew.getEntryIdGui(), recognizedAccJournalEntry);
				}
				recognizedAccJournalEntry.addItem(accJournalItemNew);
			}

			SortedMap<Long, AccJournalEntry> accJournalEntriesUpdatedMap = new TreeMap<Long, AccJournalEntry>();
			for (AccJournalItem accJournalItemUpdated : accJournalItemsUpdated) {
				long entryId = accJournalItemUpdated.getEntryId();
				AccJournalEntry recognizedAccJournalEntry = accJournalEntriesUpdatedMap.get(entryId);
				if(recognizedAccJournalEntry == null) {
					recognizedAccJournalEntry = new AccJournalEntry();
					recognizedAccJournalEntry.setCompany(tenantDescriptor);
					recognizedAccJournalEntry.setID(entryId);
					accJournalEntriesUpdatedMap.put(entryId, recognizedAccJournalEntry);
				}
				recognizedAccJournalEntry.addItem(accJournalItemUpdated);
			}

			// detect counterpart account
			JSONObject accJson = null;
			AEDescriptor accDescr = null;
			if(PaymentMethod.BANK.equals(paymentMethod) || PaymentMethod.CASH.equals(paymentMethod)) {
				AccountDAO accDAO = daoFactory.getAccountDAO(localConnection);
				if(PaymentMethod.BANK.equals(paymentMethod)) {
					BankAccount bankAccount = bankAccountDAO.load(bankAccountDescr.getID());
					accJson = accDAO.loadAccountByIdFull(bankAccount.getAccount().getDescriptor().getID());
				} else {
					JSONArray accsJson = accDAO.loadAccounts(ownerId, " and (acc.CODE = '5300')");
					if(accsJson != null && accsJson.length() == 1) {
						accJson = accsJson.getJSONObject(0);
					}
				}
				if(accJson == null) {
					throw AEError.System.INVALID_PARAMETER.toException();
				}
				accDescr = AccAccount.lazyDescriptor(accJson.getLong(AEDomainObject.JSONKey.id.name()));
			}

			// auto balancing for CA and BQ journal
			if(PaymentMethod.BANK.equals(paymentMethod) || PaymentMethod.CASH.equals(paymentMethod)) {
				Collection<AccJournalEntry> accJournalEntriesNew = accJournalEntriesNewMap.values();
				for (AccJournalEntry accJournalEntry : accJournalEntriesNew) {
					Double dtAmount = accJournalEntry.calcDtAmount();
					Double ctAmount = accJournalEntry.calcCtAmount();
					Double amount = AEMath.round(AEMath.doubleValue(dtAmount) - AEMath.doubleValue(ctAmount), 2);
					if(!AEMath.isZeroAmount(amount)) {
						// need balancing
						AccJournalItem accJournalItem = accJournalEntry.getAccJournalItemByAccId(accDescr.getID());
						if(accJournalItem == null) {
							accJournalItem = new AccJournalItem();

							accJournalItem.setAccountIdentificationRule(AccountIdentificationRule.COMMON_OPERATION);
							accJournalItem.setJournalIdentificationRule(paymentMethod.getJournalIdentificationRule());

							accJournalItem.setJournal(accJournal.getDescriptor());
							accJournalItem.setBankAccount(bankAccountDescr);

							accJournalItem.setCompany(tenantDescriptor);

							accJournalItem.setDate(accJournalEntry.getMaxDate());
							accJournalItem.setDateCreation(new Date());

							// accAccount
							accJournalItem.setAccount(accDescr);

							// extra accounting data
							if(accJournalEntry.getAccJournalItems() != null && accJournalEntry.getAccJournalItems().size() == 1) {
								AccJournalItem srcItem = accJournalEntry.getAccJournalItems().get(0);

								accJournalItem.setDescription(srcItem.getDescription());
								//						accJournalItem.setContributor(srcItem.getContributor());
								//						accJournalItem.setSupplier(srcItem.getSupplier());
								//						accJournalItem.setQuete(srcItem.getQuete());
							}

							// add to the journal entry
							accJournalEntry.addItem(accJournalItem);
						}

						// dt and ct
						if(amount > 0) {
							accJournalItem.setCtAmount(amount);
							accJournalItem.setCredit(true);
						} else {
							accJournalItem.setDtAmount(amount * (-1.0));
							accJournalItem.setDebit(true);
						}
					}
				}
			}

			/**
			 * DB connection in transaction
			 */
			localConnection.beginTransaction();
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			
			/**
			 * Validate prepared
			 * Must be in transaction
			 */

			// update validation
			for (AccJournalItem accJournalItem : accJournalItemsUpdated) {
				AccJournalItemCommonValidator.getInst().validate(accJournalItem);
			}
			validateAccJournalItems(accJournalItemsUpdated, localConnection);

			// insert validation
			Collection<AccJournalEntry> accJournalsNew = accJournalEntriesNewMap.values();
			for (AccJournalEntry accJournalEntry : accJournalsNew) {
				AccJournalEntryCommonValidator.getInst().validate(accJournalEntry);
				AccJournalEtryAlignmentValidator.getInst().validate(accJournalEntry);
				
				validateAccJournalItems(accJournalEntry.getAccJournalItems(), localConnection);
			}

			/**
			 * Save
			 */

			boolean loadSuppliers = false;
			
			// update updated entries
			Collection<AccJournalEntry> accJournalsUpdated = accJournalEntriesUpdatedMap.values();
			for (AccJournalEntry accJournalEntry : accJournalsUpdated) {
				// balance
				AccJournalEntry accJournalEntryUpd = balanceAccJournalEntry(accJournalEntry, accDescr, localConnection);

				// check
				AccJournalEntryCommonValidator.getInst().validate(accJournalEntryUpd);
				AccJournalEtryAlignmentValidator.getInst().validate(accJournalEntryUpd);
				
				// update items
				if(accJournalEntryUpd.getAccJournalItems() != null) {
					for (AccJournalItem accJournalItem : accJournalEntryUpd.getAccJournalItems()) {
						if(accJournalItem.isUpdated()) {
							// update the item
							
							// process supplier
							AEDescriptor suppl  = accJournalItem.getSupplier();
							if(suppl != null) {
								boolean insert = saveSupplier(suppl, tenantDescriptor, invContext, localConnection);
								if(insert) {
									loadSuppliers = true;
								}
							}
							
							// update item
							accJournalDAO.updateItemUserData(accJournalItem);
						}
					}
				}
			}

			// insert new entries
			// already validated
			accJournalsNew = accJournalEntriesNewMap.values();
			for (AccJournalEntry accJournalEntry : accJournalsNew) {
				if(accJournalEntry != null && accJournalEntry.hasAccJournalItems()) {
					// process supplier
					AccJournalItemsList itemsList = accJournalEntry.getAccJournalItems();
					for (AccJournalItem accJournalItem : itemsList) { 
						AEDescriptor suppl  = accJournalItem.getSupplier();
						if(suppl != null) {
							boolean insert = saveSupplier(suppl, tenantDescriptor, invContext, localConnection);
							if(insert) {
								loadSuppliers = true;
							}
						}
					}
					
					// insert entry
					accJournalDAO.insertEntry(accJournalEntry);
				}
			}

			/**
			 * Process attachments
			 */
			JSONArray fileAttachmentsJsonArray = arguments.optJSONArray("attachments");
			if(fileAttachmentsJsonArray != null) {
				for (int i = 0; i < fileAttachmentsJsonArray.length(); i++) {
					JSONObject fileAttachmentJson = fileAttachmentsJsonArray.getJSONObject(i);

					// entryId
					long entryIdGui = fileAttachmentJson.getLong("entryId");
					long accEntryId = entryIdGui;
					AccJournalEntry accJournalEntryNew = accJournalEntriesNewMapIdGui.get(entryIdGui);
					if(accJournalEntryNew != null) {
						accEntryId = accJournalEntryNew.getID();
					}

					// delete old attachment
					boolean deleteOld = fileAttachmentJson.optBoolean("deleteOld");
					Long attId = JSONUtil.optLong(fileAttachmentJson, "attId");
					if(deleteOld && attId != null && AEPersistent.ID.isPersistent(attId)) {
						// setup
						JSONObject fileAttachmentForDelete = new JSONObject()
							.put(AEDomainObject.JSONKey.id.name(),  attId)
							.put("ownerId", ownerId)
							.put(AEDomainObject.JSONKey.dbState.name(),  AEPersistentUtil.DB_ACTION_DELETE);
						
						// send request for delete attachment
						AERequest deleteAttachmentRequest = new AERequest(new JSONObject().put("fileAttachment", fileAttachmentForDelete));
						fileAttachmentLocal.manage(deleteAttachmentRequest, invContext);
					}

					String remoteName = fileAttachmentJson.optString("remoteName");
					if(!AEStringUtil.isEmpty(remoteName)) {

						// save new attachment
						Path tmpFile = null;

						// source
						tmpFile = Paths.get(
								AEApp.getInstance().getTmpRepository().toString(),
								fileAttachmentJson.getString("remoteName"));
						if(!Files.exists(tmpFile)) {
							throw AEError.System.INVALID_PARAMETER.toException();
						}

						// target
						String fileRepository = AEApp.getInstance().getProperties().getProperty(AEApp.fileRepository);
						Path target = Paths.get(
								fileRepository, 
								new StringBuilder(FilenameUtils.getBaseName(tmpFile.getFileName().toString()))
								.append("_").append(Long.toString(ownerId)).append("_").append(DomainClass.AccJournalEntry.getID()).append("_").append(accEntryId)
								.append(".").append(FilenameUtils.getExtension(tmpFile.getFileName().toString())).toString());

						// move the file
						Files.move(tmpFile, target);

						// attach to entryId
						fileAttachmentJson.put("ownerId", ownerId);
						fileAttachmentJson.put("toId", accEntryId);
						fileAttachmentJson.put("toType", DomainClass.AccJournalEntry.getID());
						fileAttachmentJson.put("name", fileAttachmentJson.getString(AEDomainObject.JSONKey.name.name()));
						fileAttachmentJson.put("length", Files.size(target));
						fileAttachmentJson.put("remotePath", target.getParent().toString());
						fileAttachmentJson.put("remoteName", target.getFileName());
						fileAttachmentJson.put("isDirty", false);

						JSONObject attachArguments = new JSONObject();
						attachArguments.put("fileAttachment", fileAttachmentJson);
						AERequest attachRequest = new AERequest(attachArguments);
						fileAttachmentLocal.manage(attachRequest, invContext);
					}
				}
			}

			/**
			 * Commit
			 */
			localConnection.commit();

			//			/**
			//			 * Recalculate accBalance
			//			 */
			//			Date dateTo = new Date();
			//			Date dateFrom = AEDateUtil.beginOfTheYear(dateTo);
			//			AETimePeriod period = new AETimePeriod(dateFrom, dateTo);
			//
			//			AccAccountBalance accAccountBalance = null;
			//			AccountDAO accDAO = daoFactory.getAccountDAO(localConnection);
			//			if(FinancialTransactionTemplate.PaymentMethod.BANK.equals(paymentMethod)) {
			//				// load bankAccount
			//				BankAccount bankAccount = bankAccountDAO.load(bankAccountDescr.getID());
			//
			//				// accBalance
			//				accAccountBalance = new AccAccountBalance();
			//				accJson = accDAO.loadAccountByIdFull(bankAccount.getAccount().getDescriptor().getID());
			//				if(accJson != null) {
			//					accAccountBalance.setAccAccount(
			//							new AEDescriptorImp(accJson.getLong(AEDomainObject.JSONKey.id.name()))
			//							.withCode(accJson.optString(AEDomainObject.JSONKey.code.name()))
			//							.withName(accJson.optString(AEDomainObject.JSONKey.name.name()))
			//							.withDescription(bankAccount.getName()));
			//					accAccountBalance.setPeriod(period);
			//					accLocal.loadAccFinalBalance(accAccountBalance, invContext, localConnection);
			//				}
			//			} else if(FinancialTransactionTemplate.PaymentMethod.CASH.equals(paymentMethod)) {
			//				// accBalance
			//				JSONArray accsJson = accDAO.loadAccounts(ownerId, " and (acc.CODE = '5300')");
			//				if(accsJson != null && accsJson.length() == 1) {
			//					accJson = accsJson.getJSONObject(0);
			//
			//					// accBalance
			//					accAccountBalance = new AccAccountBalance();
			//					accAccountBalance.setAccAccount(
			//							new AEDescriptorImp(accJson.getLong(AEDomainObject.JSONKey.id.name()))
			//							.withCode(accJson.optString(AEDomainObject.JSONKey.code.name()))
			//							.withName(accJson.optString(AEDomainObject.JSONKey.name.name()))
			//							.withDescription(paymentMethod.getName()));
			//					accAccountBalance.setPeriod(period);
			//					accLocal.loadAccFinalBalance(accAccountBalance, invContext, localConnection);
			//				}
			//			}

			/**
			 * Suppliers
			 */
			SimplePartiesList spList = null;
			if(loadSuppliers) {
				SimplePartyDAO spDAO = daoFactory.getSimplePartyDAO(localConnection);
				spList = spDAO.loadToCompany(tenantDescriptor);
			};
			
			/**
			 * Build response
			 */
			JSONObject payload = new JSONObject();
			if(spList != null) {
				payload.put(FinancialTransaction.JSONKey.suppliers.name(), spList.toJSONArray());
			}
			return new AEResponse(payload);
		} catch (Exception e) {
			logger.error(e);
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/**
	 * Keep private when possible.
	 * 
	 * @param accJournalItem
	 * @param counterpartAccId
	 * @param localConnection
	 * @throws AEException
	 */
	private AccJournalEntry balanceAccJournalEntry(AccJournalEntry accJournalEntryUpd, AEDescriptor counterpartAccDescr, AEConnection localConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AccJournalDAO accJournalDAO = daoFactory.getAccJournalDAO(localConnection);

		// load accJournalEntry from DB
		long entryId = accJournalEntryUpd.getID();
		AccJournalEntry accJournalEntryDb = accJournalDAO.loadEntry(entryId);
		if(accJournalEntryDb == null) {
			throw AEError.System.INVALID_PARAMETER.toException();
		}
		AccJournalItemsList accJournalItemsDb = accJournalDAO.loadItemsToEntry(entryId);
		accJournalEntryDb.setAccJournalItems(accJournalItemsDb);

		// update accJournalEntryDb
		AccJournalItemsList accJournalItemsUpd = accJournalEntryUpd.getAccJournalItems();
		for (AccJournalItem accJournalItemUpd : accJournalItemsUpd) {
			AccJournalItem accJournalItemDb = accJournalEntryDb.getAccJournalItemById(accJournalItemUpd.getID());
			if(accJournalItemDb == null) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}

			// update DB item
			accJournalItemDb.setDate(accJournalItemUpd.getDate());
			accJournalItemDb.setAccount(accJournalItemUpd.getAccount());
			accJournalItemDb.setSupplier(accJournalItemUpd.getSupplier());
			accJournalItemDb.setDescription(accJournalItemUpd.getDescription());
			accJournalItemDb.setDebit(accJournalItemUpd.isDebit());
			accJournalItemDb.setDtAmount(accJournalItemUpd.getDtAmount());
			accJournalItemDb.setCredit(accJournalItemUpd.isCredit());
			accJournalItemDb.setCtAmount(accJournalItemUpd.getCtAmount());
			accJournalItemDb.setContributor(accJournalItemUpd.getContributor());
			accJournalItemDb.setQuete(accJournalItemUpd.getQuete());

			accJournalItemDb.setUpdated();
		}

		// need balancing?
		if(!accJournalEntryDb.isBalanced()) {
			accJournalEntryDb.reBalance();
		}

		return accJournalEntryDb;
	}

	private void setupAccJournalCode(AccJournal accJournal, AEConnection localConnection) throws AEException {
		String code = accJournal.getPaymentMethod().getCode();
		if(PaymentMethod.BANK.equals(accJournal.getPaymentMethod())) {
			DAOFactory daoFactory = DAOFactory.getInstance();
			BankAccountDAO bankAccountDAO = daoFactory.getBankAccountDAO(localConnection);
			BankAccount bankAccount = bankAccountDAO.load(accJournal.getBankAccount().getID());
			if(bankAccount == null || bankAccount.getAccJournal() == null) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}
			code = bankAccount.getAccJournal().getDescriptor().getCode();
		}
		accJournal.setCode(code);
	}

	@Override
	public AEResponse saveContributors(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);
			long sOwnerId = arguments.getLong(AEDomainObject.JSONKey.sOwnerId.name());

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
			authorize(new AuthPermission("System/FinancialTransaction/Contributor", AuthPermission.SAVE), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			localConnection.beginTransaction();

			/**
			 * Save Contributors 
			 */
			ContributorsList contributorsList = new ContributorsList();
			contributorsList.create(arguments.getJSONArray(FinancialTransaction.JSONKey.contributors.name()));
			for (Contributor contributor : contributorsList) {
				if(!contributor.isValid()) {
					throw AEError.System.INVALID_REQUEST.toException();
				}

				if(contributor.isNew()) {
					Employee empl = contributor.getEmployee();
					if(empl == null) {
						throw new AEException("System error: Employee is not instantiated. ");
					}

					// Owner
					contributor.setCompany(ownerDescr);

					// new contributor
					contributor.resetAsNew();

					// with new employee
					empl.resetAsNew();

					// update 
					AEDescriptive personDescr = empl.getPerson();
					Person person = null;
					if(personDescr == null) {
						// new Person: create Person from empl's data
						person = empl.createPerson();
					} else {
						// update empl's person
						person = (Person) partyLocal.load(personDescr.getDescriptor(), invContext);
						empl.updatePerson(person);
					}

					manage(contributor, invContext, localConnection);
				} else if(contributor.isUpdated()) {
					Employee empl = contributor.getEmployee();
					if(empl == null) {
						throw new AEException("System error: Employee is not instantiated. ");
					}

					// Owner
					contributor.setCompany(ownerDescr);

					// because this update is from the contributor's table, update person's data to up to date 
					AEDescriptive personDescr = empl.getPerson();
					Person person = null;
					if(personDescr == null) {
						// new Person: create Person from empl's data
						person = empl.createPerson();
					} else {
						// update empl's person
						person = (Person) partyLocal.load(personDescr.getDescriptor(), invContext);
						empl.updatePerson(person);
					}

					// propagate contr's persistent state to the employee
					// because the relation is one to one
					contributor.setUpdated();

					manage(contributor, invContext, localConnection);
				}
			}

			/**
			 * Commit
			 */
			localConnection.commit();

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject();
			payload.put(FinancialTransaction.JSONKey.contributors.name(), contributorsList.toJSONArray());
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
	public AEResponse deleteContributor(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);
			long sOwnerId = arguments.getLong(AEDomainObject.JSONKey.sOwnerId.name());
			long contributorId = arguments.getLong(AEDomainObject.JSONKey.id.name());

			/**
			 * Authorize for concrete tenant
			 */
			// state validation
			if(ownerId != sOwnerId) {
				throw AEError.System.INVALID_REQUEST.toException();
			}

			// whether this principal is authorized for specified tenant
			ap.ownershipValidator(ownerId);
			
			/**
			 * Authorize 
			 */
			authorize(new AuthPermission("System/FinancialTransaction/Contributor", AuthPermission.DELETE), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * Delete Contributor 
			 */
			ContributorDAO contrDao = DAOFactory.getInstance().getContributorDAO(localConnection);
			Contributor contributor = contrDao.loadContributor(Contributor.lazyDescriptor(contributorId));
			if(contributor != null && contributor.getEmployee() != null && contributor.getEmployee().getPerson() != null) {
				// check where specified contributorId matches apecified ownerId
				if(!ownerDescr.equals(contributor.getCompany().getDescriptor())) {
					throw AEError.System.INVALID_REQUEST.toException();
				}

				// get person descriptor
				AEDescriptor personDescr = contributor.getEmployee().getPerson().getDescriptor();

				localConnection.beginTransaction();

				// delete donations if not exists
				boolean existDonations = contrDao.existDonations(personDescr);
				if(existDonations) {
					throw AEError.System.CONTRIBUTOR_IN_USE.toException();
				} else {
					contrDao.deleteDonations(personDescr);
				}

				// load all employees which are contributors for specified person
				List<Long> listIDs = contrDao.loadEmployeeIDsByPersonId(ownerDescr, personDescr);

				// delete all contributors with specified personDescr if it is possible
				int count = contrDao.delete(ownerDescr, personDescr);
				if(count > 0) {
					// delete employees if it is possible
					for (Long emplId : listIDs) {
						partyLocal.deleteEmployee(emplId, invContext, localConnection);
					}
				}

				/**
				 * Commit
				 */
				localConnection.commit();

				/**
				 * Create and return response
				 */
				JSONObject payload = new JSONObject();
				payload.put(FinancialTransaction.JSONKey.contributor.name(), Contributor.lazyDescriptor(contributorId).toJSONObject());
				return new AEResponse(payload);
			} else {
				throw AEError.System.INVALID_REQUEST.toException();
			}
		} catch (Exception e) {
			logger.error(e);
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	private void validateAccJournalItems(AccJournalItemsList accJournalItemsList, AEConnection aeConnection) throws AEException {
		// there is nothing to validate if accJournalItemsList is null or empty
		if(accJournalItemsList == null || accJournalItemsList.isEmpty()) {
			return;
		};
		
		// extract and validate owner
		Set<AEDescriptor> ownersSet = new HashSet<AEDescriptor>();
		AEDescriptor ownerDescr = null;
		for (AccJournalItem accJournalItem : accJournalItemsList) {
			if(accJournalItem.getCompany() == null) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}
			ownerDescr = accJournalItem.getCompany().getDescriptor();
			ownersSet.add(ownerDescr);
		}
		if(ownersSet.size() != 1) {
			throw AEError.System.INVALID_PARAMETER.toException();
		}
		
		// extract dates
		Set<Date> datesSet = new HashSet<Date>();
		for (AccJournalItem accJournalItem : accJournalItemsList) {
			if(accJournalItem.getDate() == null) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}
			datesSet.add(accJournalItem.getDate());
		}
			
		for (Date date : datesSet) {
			isInPermittedAccountingPeriods(ownerDescr, date, aeConnection);
		}
	}

	@Override
	public AEResponse importContributors(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		BufferedReader reader = null;
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
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);
			long sOwnerId = arguments.getLong(AEDomainObject.JSONKey.sOwnerId.name());

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
			authorize(new AuthPermission("System/FinancialTransaction/Contributor", AuthPermission.SAVE), invContext, Organization.lazyDescriptor(ownerId));

			// read from file
			ContributorsList contributorsList = new ContributorsList();

			String fileName = arguments.optString("remoteName", AEStringUtil.EMPTY_STRING);
			if(!AEStringUtil.isEmpty(fileName)) {
				Path fileRepository = AEApp.getInstance().getTmpRepository();
				File file = new File(fileRepository.toFile(), fileName);
				if(file.exists() && file.isFile() && fileRepository.toFile().equals(file.getParentFile())) {
					String delimiter = "\t";
					FileInputStream fis = new FileInputStream(file); 
					reader = new BufferedReader(new InputStreamReader(fis, "Windows-1252"), 8192);
					boolean header = true;
					String line = null;
					while((line = reader.readLine()) != null) {
						line = AEStringUtil.trim(line);

						LightStringTokenizer tokenizer = new LightStringTokenizer(line, delimiter);

						//Nom	
						String name = null;
						if(tokenizer.hasMoreTokens()) {
							name = AEStringUtil.trim(tokenizer.nextToken());
						}

						//Prénom
						String prenom = null;
						if(tokenizer.hasMoreTokens()) {
							prenom = AEStringUtil.trim(tokenizer.nextToken());
						}

						//Adresse
						String adresse = null;
						if(tokenizer.hasMoreTokens()) {
							adresse = AEStringUtil.trim(tokenizer.nextToken());
						}

						//CP
						String cp = null;
						if(tokenizer.hasMoreTokens()) {
							cp = AEStringUtil.trim(tokenizer.nextToken());
						}

						//Commune
						String commune = null;
						if(tokenizer.hasMoreTokens()) {
							commune = AEStringUtil.trim(tokenizer.nextToken());
						}

						if(header) {
							if(!"Nom".equalsIgnoreCase(name)
									|| !"Prénom".equalsIgnoreCase(prenom)
									|| !"Adresse".equalsIgnoreCase(adresse)
									|| !"CP".equalsIgnoreCase(cp)
									|| !"Commune".equalsIgnoreCase(commune)) {
								
								break;
							}
							header = false;
						} else {
							// create Employee
							Employee empl = new Employee();
							empl.setLastName(name);
							empl.setFirstName(prenom);

							Address addr = new Address(Address.Type.BUSINESS);
							addr.setCity(commune);
							addr.setPostalCode(cp);
							addr.setStreet(adresse);
							empl.setAddress(addr);
							empl.createPerson();
							
							// create Contributor with just created employee
							Contributor contributor = new Contributor();
							contributor.setEmployee(empl);
							contributor.setCompany(ownerDescr);

							if(contributor.isValid()) {
								// add to the list
								contributorsList.add(contributor);
							}
						}
					}
					
					// delete tmp file
					if(reader != null) {
						try {
							reader.close();
							reader = null;
						} catch (IOException e) {
							logger.error(e);
						}
					}
					if(file.isFile() && fileRepository.toFile().equals(file.getParentFile())) {
						AEFileUtil.deleteFileQuietly(file);
					}
				}
			}
			
			/**
			 * Insert Contributors 
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));

			localConnection.beginTransaction();

			for (Contributor contributor : contributorsList) {
				try {
					manage(contributor, invContext, localConnection);
				} catch (Exception e) {
					logger.error(e);
				}
			}

			localConnection.commit();

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject();
//			payload.put(FinancialTransaction.JSONKey.contributors.name(), contributorsList.toJSONArray());
			return new AEResponse(payload);
		} catch (Exception e) {
			logger.error(e);
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
			if(reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					logger.error(e);
				}
			}
		}
	}

	@Override
	public AEResponse tallyAccJournalItem(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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

			/**
			 * Authorize for concrete tenant
			 */
			// common attributes
			long ownerId = arguments.getLong(AEDomainObject.JSONKey.ownerId.name());
			long sOwnerId = arguments.getLong(AEDomainObject.JSONKey.sOwnerId.name());
			long accJournalItemId = arguments.getLong(AEDomainObject.JSONKey.id.name());
			boolean tally = arguments.getBoolean("tally");

			// state validation
			if(ownerId != sOwnerId) {
				throw AEError.System.SECURITY_VIOLATION.toException();
			}

			// whether this principal is authorized for specified tenant
			ap.ownershipValidator(ownerId);
			
			authorize(new AuthPermission("System/AccJournal", AuthPermission.UPDATE), invContext, Organization.lazyDescriptor(ownerId));

			// connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			localConnection.beginTransaction();

			// update
			AccJournalDAO accJournalDAO = daoFactory.getAccJournalDAO(localConnection);
			accJournalDAO.updateTally(tally, accJournalItemId, Organization.lazyDescriptor(sOwnerId));
			
			// commit
			localConnection.commit();
			
			/**
			 * Build response
			 */
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Exception e) {
			logger.error(e);
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
}
