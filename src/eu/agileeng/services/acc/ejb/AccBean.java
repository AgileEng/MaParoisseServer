/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 09.12.2009 21:03:10
 */
package eu.agileeng.services.acc.ejb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;
import org.jboss.logging.Logger;

import eu.agileeng.accbureau.AEApp;
import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDescriptorsList;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.AccAccountBalancesList;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.acc.AccAccountBalance;
import eu.agileeng.domain.acc.AccJournalEntry;
import eu.agileeng.domain.acc.AccJournalItem;
import eu.agileeng.domain.acc.AccJournalItemsList;
import eu.agileeng.domain.acc.AccPeriod;
import eu.agileeng.domain.acc.AccPeriodItems;
import eu.agileeng.domain.acc.cashbasis.BordereauParoisse;
import eu.agileeng.domain.acc.cashbasis.BordereauParoissesList;
import eu.agileeng.domain.acc.cashbasis.FinancialTransactionTemplate;
import eu.agileeng.domain.acc.cashbasis.Quete;
import eu.agileeng.domain.acc.cashbasis.QuetesList;
import eu.agileeng.domain.acc.export.ExportFactory;
import eu.agileeng.domain.acc.export.ExportJob;
import eu.agileeng.domain.acc.export.ExportRequest;
import eu.agileeng.domain.business.bank.BankAccount;
import eu.agileeng.domain.business.bank.BankAccountBalance;
import eu.agileeng.domain.business.bank.BankAccountBalancesList;
import eu.agileeng.domain.business.bank.BankAccountsList;
import eu.agileeng.domain.cefra.n11580_03.BudgetRealizationDataSource;
import eu.agileeng.domain.contact.ContributorDonation;
import eu.agileeng.domain.contact.ContributorDonationsList;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.file.FileAttachment;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.domain.social.SalaryGrid;
import eu.agileeng.domain.social.SalaryGridsList;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.acc.AccJournalDAO;
import eu.agileeng.persistent.dao.acc.AccPeriodDAO;
import eu.agileeng.persistent.dao.acc.AccountDAO;
import eu.agileeng.persistent.dao.acc.BordereauParoisseDAO;
import eu.agileeng.persistent.dao.acc.ChartOfAccountsDAO;
import eu.agileeng.persistent.dao.acc.VATItemDAO;
import eu.agileeng.persistent.dao.cash.BankAccountDAO;
import eu.agileeng.persistent.dao.common.ContributorDAO;
import eu.agileeng.persistent.dao.oracle.OrganizationDAO;
import eu.agileeng.persistent.dao.social.SocialDAO;
import eu.agileeng.security.AuthPermission;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.security.AuthRole;
import eu.agileeng.services.AEBean;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AEInvocationContextValidator;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.council.ejb.CouncilLocal;
import eu.agileeng.services.file.ejb.FileAttachmentLocal;
import eu.agileeng.services.imp.AEInvocationContextImp;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AETimePeriod;
import eu.agileeng.util.LightStringTokenizer;
import eu.agileeng.util.json.JSONUtil;


/**
 *
 */
@SuppressWarnings("serial")
@Stateless
public class AccBean extends AEBean implements AccLocal {

	private static Logger logger = Logger.getLogger(AccBean.class);

	@EJB private FileAttachmentLocal fileAttachmentService;
	
	@EJB private CouncilLocal councilService;

	/**
	 * 
	 */
	public AccBean() {
	}

	@Override
	public AEResponse loadVATItems(AERequest request) throws AEException {
		AEResponse response = new AEResponse();
		//		JSONObject arguments = request.getArguments();
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			localConnection = daoFactory.getConnection();

			// manage in transaction
			try {
				VATItemDAO vatItemDAO = daoFactory.getVATItemDAO(localConnection);
				JSONArray retItems = vatItemDAO.load();
				JSONObject payload = new JSONObject();
				payload.put("items", retItems);
				response.setPayload(payload);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
		return response;
	}

	@Override
	public AEResponse saveVATItems(AERequest request) throws AEException {
		AEResponse response = new AEResponse();
		JSONObject arguments = request.getArguments();
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			// manage in transaction
			try {
				// transform to collection
				JSONArray items = arguments.getJSONArray("items");
				List<JSONObject> jsonList = new ArrayList<JSONObject>();
				for (int i = 0; i < items.length(); i++) {
					JSONObject vatItem = items.getJSONObject(i);
					jsonList.add(vatItem);
				}

				// emulate DB process
				VATItemDAO vatItemDAO = daoFactory.getVATItemDAO(localConnection);
				JSONArray retItems = new JSONArray();
				for (Iterator<JSONObject> iterator = jsonList.iterator(); iterator.hasNext();) {
					JSONObject jsonObject = (JSONObject) iterator.next();
					if(jsonObject.has("dbState")) {
						int dbState = jsonObject.getInt("dbState");
						switch(dbState) {
						case 0: {
							retItems.put(jsonObject);
							break;
						}
						case 1: {
							vatItemDAO.insert(jsonObject);
							retItems.put(jsonObject);
							break;
						}
						case 2: {
							vatItemDAO.update(jsonObject);
							retItems.put(jsonObject);
							break;
						}
						case 3: {
							vatItemDAO.delete(jsonObject);
							iterator.remove();
							break;
						}
						}
					}
				}


				JSONObject payload = new JSONObject();
				payload.put("items", retItems);
				response.setPayload(payload);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			localConnection.commit();
		} catch (Throwable t) {
			t.printStackTrace();
			if(localConnection != null) {
				localConnection.rollback();
			}
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
		return response;
	}

	@Override
	public AEResponse loadCOAModels(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// authorize 
			authorize(new AuthPermission("System/Configuration/Customer/ChartOfAccountModel", AuthPermission.READ), invContext, null);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			boolean dontSeparateByAccType = arguments.optBoolean(AccAccount.JSONKey.dontSeparateByAccType.name());

			// DAO and Connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			// load COA Models
			ChartOfAccountsDAO coaModelDAO = daoFactory.getChartOfAccountsDAO(localConnection);
			AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);
			JSONArray coaModelsJson = coaModelDAO.loadModels();
			for (int i = 0; i < coaModelsJson.length(); i++) {
				JSONObject coaModelJson = coaModelsJson.getJSONObject(i);

				long coaModelId = coaModelJson.getLong(AEDomainObject.JSONKey.id.name());

				// load accounts
				JSONArray accounts = accountDAO.loadAccounts(coaModelId);
				coaModelJson.put(AccAccount.JSONKey.accounts.name(), accounts);

				// load patterns
				JSONArray patterns = accountDAO.loadPatterns(coaModelId);
				if(dontSeparateByAccType) {
					for (int j = 0; j < patterns.length(); j++) {
						accounts.put(patterns.get(j));
					}
				} else {
					coaModelJson.put(AccAccount.JSONKey.patterns.name(), patterns);
				}
			}

			// load SalaryGrids
			SalaryGridsList salaryGrids = null;
			String accHouse = arguments.optString(SalaryGrid.JSONKey.accHouse.toString());
			if(!AEStringUtil.isEmpty(accHouse)) {
				SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);
				salaryGrids = socialDAO.loadAllSalaryGridsLazy(SalaryGrid.lazyAccHouseDescriptor(accHouse));
			}

			// Create response
			JSONObject payload = new JSONObject();
			payload.put("coaModels", coaModelsJson);
			if(salaryGrids != null) {
				payload.put("salaryGrids", salaryGrids.toJSONArray());
			}
			AEResponse response = new AEResponse(payload);
			return response;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse saveCOAModels(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// authorize 
			authorize(new AuthPermission("System/Configuration/Customer/ChartOfAccountModel", AuthPermission.SAVE_AND_DELETE), invContext, null);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();

			// Connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			// DAOs
			ChartOfAccountsDAO coaModelDAO = daoFactory.getChartOfAccountsDAO(localConnection);
			AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);

			// manage in transaction
			JSONArray coaModels = arguments.getJSONArray(AccAccount.JSONKey.coaModels.name());
			List<JSONObject> coaModelsList = new ArrayList<JSONObject>();
			for (int i = 0; i < coaModels.length(); i++) {
				JSONObject coaModel = coaModels.getJSONObject(i);
				coaModelsList.add(coaModel);
			}

			// manage coa models
			JSONArray retCoaModelsJson = new JSONArray();
			for (Iterator<JSONObject> iterator = coaModelsList.iterator(); iterator.hasNext();) {
				JSONObject coaModelJson = (JSONObject) iterator.next();

				// set COA Model rules (in case of insert and update)
				coaModelJson.put(AEDomainObject.JSONKey.ownerId.name(), 0L);
				coaModelJson.put(AccAccount.JSONKey.modelId.name(), 0L);

				// manage coa record
				int dbState = coaModelJson.getInt("dbState");
				switch(dbState) {
				case AEPersistentUtil.DB_ACTION_NONE: {
					retCoaModelsJson.put(coaModelJson);
					break;
				}
				case AEPersistentUtil.DB_ACTION_INSERT: {
					coaModelDAO.insertCOA(coaModelJson);
					retCoaModelsJson.put(coaModelJson);
					break;
				}
				case AEPersistentUtil.DB_ACTION_UPDATE: {
					coaModelDAO.updateCOA(coaModelJson);
					retCoaModelsJson.put(coaModelJson);
					break;
				}
				case AEPersistentUtil.DB_ACTION_DELETE: {
					coaModelDAO.deleteCOA(coaModelJson.getLong("id"));
					iterator.remove();
					break;
				}
				}

				// load individual COAs made by coaModelJson 
				JSONArray coas = coaModelDAO.loadCOAByModelId(coaModelJson.getLong("id"));
				Map<Long, JSONArray> accounts = new HashMap<Long, JSONArray>();

				//for every coa made by this model
				for (int i = 0; i < coas.length(); i++) {
					JSONObject coa = coas.getJSONObject(i);

					//get accounts
					JSONArray accs = accountDAO.loadAccounts(coa.getLong("id"));

					//arrange all accounts in a map by their parent's id
					for (int j = 0; j < accs.length(); j++) {
						JSONObject acc = accs.getJSONObject(j);
						JSONArray accsByParent = accounts.get(acc.getJSONObject("parent").getLong("id"));

						if (accsByParent == null) {
							accsByParent = new JSONArray();
						}
						accsByParent.put(acc);

						accounts.put(acc.getJSONObject("parent").getLong("id"), accsByParent);
					}
				}


				// prepare accounts
				JSONArray accountsArray = coaModelJson.getJSONArray(AccAccount.JSONKey.accounts.name());
				for (int i = 0; i < accountsArray.length(); i++) {
					JSONObject account = accountsArray.getJSONObject(i);

					// determine and set accType
					String code = account.getString(AEDomainObject.JSONKey.code.name());
					Matcher patternMatcher = Pattern.compile(AccAccount.codePatternRegEx, Pattern.CASE_INSENSITIVE).matcher(code);
					Matcher accountMatcher = Pattern.compile(AccAccount.codeAccountRegEx, Pattern.CASE_INSENSITIVE).matcher(code);
					if(accountMatcher.matches()) { // IMPORTANT: accountMatcher must be the first check
						account.put(AccAccount.JSONKey.accType.name(), AccAccount.AccountType.ACCOUNT.getTypeId());
					} else if(patternMatcher.matches()) {
						account.put(AccAccount.JSONKey.accType.name(), AccAccount.AccountType.PATTERN.getTypeId());
					} else {
						throw new AEException("Internal error: Determination of COA model account's accType failed: " + code);
					}

					account.put(AccAccount.JSONKey.coaId.name(), coaModelJson.getLong("id"));
					account.remove("vat");
					account.put(AccAccount.JSONKey.modifiable.name(), false);
					account.put(AccAccount.JSONKey.parentId.name(), 0L);
				}

				// process accounts
				JSONArray savedAcccounts = processModelAccounts(accountsArray, localConnection, false, accounts, coas);
				coaModelJson.put("accounts", savedAcccounts);
			}

			localConnection.commit();

			// create and return response
			JSONObject payload = new JSONObject();
			payload.put(AccAccount.JSONKey.coaModels.name(), retCoaModelsJson);
			return new AEResponse(payload);
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private JSONArray processModelAccounts(JSONArray accounts, AEConnection aeConnection, boolean individualCoa, Map<Long, JSONArray> accs, JSONArray coas) throws AEException {
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();

			// DAOs
			AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);

			List<JSONObject> jsonList = new ArrayList<JSONObject>();
			for (int i = 0; i < accounts.length(); i++) {
				JSONObject account = accounts.getJSONObject(i);

				jsonList.add(account);
			}

			JSONArray accounts4Insert = new JSONArray();
			Map<JSONObject, JSONArray> accounts4Update = new HashMap<JSONObject, JSONArray>();
			JSONArray accounts4Delete = new JSONArray();

			JSONArray jsonArray = new JSONArray();
			for (Iterator<JSONObject> iterator = jsonList.iterator(); iterator.hasNext();) {
				JSONObject jsonObject = (JSONObject) iterator.next();

				int dbState = jsonObject.getInt("dbState");
				switch(dbState) {
				case AEPersistentUtil.DB_ACTION_NONE: {
					jsonArray.put(jsonObject);
					break;
				}
				case AEPersistentUtil.DB_ACTION_INSERT: {
					//is the model updated
					if (coas.length() < 1) {
						accountDAO.insertAccount(jsonObject);
					} else {
						accounts4Insert.put(jsonObject);
					}
					jsonArray.put(jsonObject);
					break;
				}
				case AEPersistentUtil.DB_ACTION_UPDATE: {
					if (individualCoa && !jsonObject.getBoolean("modifiable")) {
						throw new AEException("Account is not modifiable!");
					}

					// load the DB state
					JSONObject dbAccountJson = accountDAO.loadAccountByIdFull(jsonObject.getLong(AEDomainObject.JSONKey.id.name()));

					if(dbAccountJson != null) {
						// The code is considered safe for updating if 
						// the new code begins with the old one, followed by one or several letters X or x
						// 
						// If the code update is unsafe, rollback the update
						String accountCode = AEStringUtil.trim(jsonObject.optString(AEDomainObject.JSONKey.code.name()));
						String oldAccountCode = AEStringUtil.trim(dbAccountJson.optString(AEDomainObject.JSONKey.code.name()));
						StringBuilder regExSB = 
								new StringBuilder("^").append("(").append(oldAccountCode).append(")").append("X+");
						Matcher safeCodeMatcher = Pattern.compile(regExSB.toString(), Pattern.CASE_INSENSITIVE).matcher(accountCode);
						if(!safeCodeMatcher.matches()) {
							jsonObject.put(AEDomainObject.JSONKey.code.name(), oldAccountCode);
						}
					}

					//is the model updated
					if (coas.length() < 1) {
						accountDAO.updateAccount(jsonObject);
					} else {
						accounts4Update.put(jsonObject, accs.get(jsonObject.getLong("id")));
					}
					jsonArray.put(jsonObject);
					break;
				}
				case AEPersistentUtil.DB_ACTION_DELETE: {
					if (individualCoa && !jsonObject.getBoolean("modifiable")) {
						throw new AEException("Account is not modifiable!");
					}

					//is the model updated
					if (coas.length() < 1) {
						accountDAO.deleteAccount(jsonObject);
					} else {
						if (accs.get(jsonObject.getLong("id")) != null) {
							for (int j = 0; j < accs.get(jsonObject.getLong("id")).length(); j++) {
								accounts4Delete.put(accs.get(jsonObject.getLong("id")).getJSONObject(j));
							}
						}
						accounts4Delete.put(jsonObject);
					}

					// remove deleted account from the collection
					iterator.remove();

					break;
				}
				}
			}

			// propagate changes to the Individual COAs accounts
			accountDAO.insertAccounts(accounts4Insert, coas);
			accountDAO.updateAccounts(accounts4Update);
			try {
				accountDAO.deleteAccounts(accounts4Delete);
			} catch (AEException e) {
				throw new AEException("An error occured! Deactivate!", e);
			}

			// commit
			localConnection.commit();

			// return
			return jsonArray;
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private JSONArray processAccounts(JSONArray accounts, AEConnection aeConnection, boolean individualCoa) throws AEException {
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();

			// DAOs
			AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);

			List<JSONObject> jsonList = new ArrayList<JSONObject>();
			for (int i = 0; i < accounts.length(); i++) {
				JSONObject account = accounts.getJSONObject(i);

				jsonList.add(account);
			}

			JSONArray jsonArray = new JSONArray();
			for (Iterator<JSONObject> iterator = jsonList.iterator(); iterator.hasNext();) {
				JSONObject jsonObject = (JSONObject) iterator.next();

				int dbState = jsonObject.getInt("dbState");
				switch(dbState) {
				case AEPersistentUtil.DB_ACTION_NONE: {
					jsonArray.put(jsonObject);
					break;
				}
				case AEPersistentUtil.DB_ACTION_INSERT: {
					accountDAO.insertAccount(jsonObject);
					jsonArray.put(jsonObject);
					break;
				}
				case AEPersistentUtil.DB_ACTION_UPDATE: {
					if (individualCoa && !jsonObject.getBoolean("modifiable")) {
						throw new AEException("Account is not modifiable!");
					}
					accountDAO.updateAccount(jsonObject);
					jsonArray.put(jsonObject);
					break;
				}
				case AEPersistentUtil.DB_ACTION_DELETE: {
					if (individualCoa && !jsonObject.getBoolean("modifiable")) {
						throw new AEException("Account is not modifiable!");
					}
					accountDAO.deleteAccount(jsonObject);
					iterator.remove();

					break;
				}
				}
			}
			localConnection.commit();
			return jsonArray;
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public void saveCOA(JSONObject jsonCOA, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();

			//validate COA
			if (jsonCOA.getLong("modelId") < 1) throw new AEException("No model ID!");


			// process COA
			ChartOfAccountsDAO coaDAO = daoFactory.getChartOfAccountsDAO(localConnection);
			int dbState = jsonCOA.getInt(AEDomainObject.JSONKey.dbState.name());
			switch(dbState) {
			case AEPersistentUtil.DB_ACTION_INSERT: {
				coaDAO.insertCOA(jsonCOA);
				break;
			}
			case AEPersistentUtil.DB_ACTION_UPDATE: {
				coaDAO.updateCOA(jsonCOA);
				break;
			}
			case AEPersistentUtil.DB_ACTION_DELETE: {
				coaDAO.deleteCOA(jsonCOA.getLong("id"));
				break;
			}
			case AEPersistentUtil.DB_ACTION_NONE:
			default: {
				break;
			}
			}

			// load COA Models
			JSONArray models = coaDAO.loadModels();
			Map<Long, JSONObject> accountModels = new HashMap<Long, JSONObject>();
			AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);
			for (int i = 0; i < models.length(); i++) {
				JSONObject coaModel = models.getJSONObject(i);

				//load only the account models for this COA
				if (coaModel.getLong(AEDomainObject.JSONKey.id.name()) == jsonCOA.getLong("modelId")) {
					// prepare accounts
					JSONArray accounts = accountDAO.loadAccounts(coaModel.getLong("id"));
					for (int j = 0; j < accounts.length(); j++) {
						JSONObject account = accounts.getJSONObject(j);
						accountModels.put(account.getLong(AEDomainObject.JSONKey.id.name()), account);
					}

					// prepare patterns
					JSONArray patterns = accountDAO.loadPatterns(coaModel.getLong("id"));
					for (int j = 0; j < patterns.length(); j++) {
						JSONObject pattern = patterns.getJSONObject(j);
						accountModels.put(pattern.getLong(AEDomainObject.JSONKey.id.name()), pattern);
					}

					// that is all
					break;
				}

			}

			// process accounts
			if(jsonCOA.has("accounts")) {
				JSONArray accountsArray = jsonCOA.getJSONArray("accounts");
				if(accountsArray != null) {
					long coaId = jsonCOA.getLong("id");

					// prepare and validate accounts
					for (int i = 0; i < accountsArray.length(); i++) {
						JSONObject account = accountsArray.getJSONObject(i);

						// validate parentId
						if (account.getJSONObject("parent").getLong("id") < 1) throw new AEException("No parent ID!");

						// validate against parent's pattern/code
						JSONObject accModel = (JSONObject) accountModels.get(account.getJSONObject("parent").getLong("id"));
						if(accModel == null) {
							throw new AEException("Data integrity error: accModel missing");
						}

						// validate code pattern only if account is for insert or for update 
						int accountDbState = account.getInt(AEDomainObject.JSONKey.dbState.name());
						if(accountDbState == AEPersistentUtil.DB_ACTION_INSERT || accountDbState == AEPersistentUtil.DB_ACTION_UPDATE) {
							String code = accModel.getString(AEDomainObject.JSONKey.code.name());
							Pattern pattern = Pattern.compile("^(" + code.replaceAll("X", "\\\\d").replaceAll("x", "\\\\d") + ")$");
							Matcher matcher = pattern.matcher(account.getString(AEDomainObject.JSONKey.code.name()));
							if(!matcher.matches()) {
								throw new AEException("Account code does not match its pattern!");
							}
						}

						// fix modifiable tampering
						if (accModel.getLong("accType") == AccAccount.AccountType.PATTERN.getTypeId()) {
							account.put("modifiable", true);
						} else {
							account.put("modifiable", false);
						}

						account.remove("vat");
						account.put("coaId", coaId);
					}

					// save accounts
					JSONArray savedAcccounts = processAccounts(accountsArray, localConnection, true);
					jsonCOA.put("accounts", savedAcccounts);
				}
			}

			localConnection.commit();
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			logger.errorv("{0} in {1}#{2}: {3}", t.getClass().getSimpleName(), this.getClass().getSimpleName(), "saveCOA", t.getMessage());
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse importAccounts(AERequest request) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			JSONObject coa = request.getArguments().getJSONObject("coaModel");
			String fileName = "C:/AccBureau/import/accounts.txt";

			//read the file line by line
			JSONArray accountsArray = new JSONArray();
			BufferedReader in = null;
			try {
				InputStreamReader isr = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
				in = new BufferedReader(isr);
				String line = new String();
				LightStringTokenizer tokenizer = new LightStringTokenizer(AEStringUtil.EMPTY_STRING, "\t");
				while ((line = in.readLine()) != null) {
					JSONObject acc = parseLine(line, tokenizer);
					accountsArray.put(acc);
				}
			} catch (Exception ex) {

			} finally {
				if(in != null) {
					try {
						in.close();
					} catch (IOException ex1) {
					}
				}
			}

			// get connection and begin transaction
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			// process accounts
			for (int i = 0; i < accountsArray.length(); i++) {
				JSONObject account = accountsArray.getJSONObject(i);
				account.put("coaId", coa.getLong("id"));
			}
			JSONArray savedAcccounts = processAccounts(accountsArray, localConnection, false);
			coa.put("accounts", savedAcccounts);
			localConnection.commit();

			AEResponse aeResponse = new AEResponse(coa);
			return aeResponse;
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

	private JSONObject parseLine(String line, LightStringTokenizer tokenizer) throws JSONException {
		JSONObject acc = new JSONObject();
		acc.put("id", -1L);
		acc.put("account", AEStringUtil.EMPTY_STRING);
		acc.put("active", true);
		acc.put("system", false);
		acc.put("modifiable", true);
		acc.put("dbState", AEPersistentUtil.DB_ACTION_INSERT);

		// parse line
		tokenizer.setString(line);

		// code
		if (tokenizer.hasMoreTokens()) {
			acc.put("code", AEStringUtil.trim(tokenizer.nextToken()));
		}

		// name
		if (tokenizer.hasMoreTokens()) {
			String name = AEStringUtil.trim(tokenizer.nextToken());
			acc.put("name", name);
			acc.put("description", name);
		}

		return acc;
	}

	@Override
	public JSONObject loadCOA(long orgId, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			// load coa
			ChartOfAccountsDAO coaModelDAO = daoFactory.getChartOfAccountsDAO(localConnection);
			AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);
			JSONObject coa = coaModelDAO.loadByCustomer(orgId);

			// load accounts from COA
			JSONArray accounts = null;
			if(coa.has(AEDomainObject.JSONKey.id.name())) {
				accounts = accountDAO.loadAccounts(coa.getLong(AEDomainObject.JSONKey.id.name()));
			}
			if(accounts == null) {
				accounts = new JSONArray();
			}
			coa.put(AccAccount.JSONKey.accounts.name(), accounts);

			// load active patterns from COAModel
			JSONArray patterns = null;
			if(coa.has(AccAccount.JSONKey.modelId.name())) {
				patterns = accountDAO.loadActivePatterns(coa.getLong(AccAccount.JSONKey.modelId.name()));
			}
			if(patterns == null) {
				patterns = new JSONArray();
			}
			coa.put(AccAccount.JSONKey.patterns.name(), patterns);

			// return coa
			return coa;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public void deleteCOA(long coaId, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();

			// delete accounts
			AccountDAO accDAO = daoFactory.getAccountDAO(localConnection);
			accDAO.deleteAccountsByCOA(coaId);

			// delete COA
			ChartOfAccountsDAO coaDAO = daoFactory.getChartOfAccountsDAO(localConnection);
			coaDAO.deleteCOA(coaId);

			localConnection.commit();
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

	@Override
	public AEResponse deleteCOA(AERequest request) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			JSONObject coa = request.getArguments().getJSONObject("coa");
			deleteCOA(coa.getLong("id"), localConnection);

			localConnection.commit();

			return new AEResponse();
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

	@Override
	public AEResponse saveGOA(AERequest request) throws AEException {
		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();

			long ownerId = request.getArguments().getLong("sOwnerId");

			// get connection and begin transaction
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			JSONObject assetsGOA = request.getArguments().getJSONObject("assetsGOA");
			saveGOA(assetsGOA, ownerId, localConnection);

			JSONObject expensesGOA = request.getArguments().getJSONObject("expensesGOA");
			saveGOA(expensesGOA, ownerId, localConnection);

			localConnection.commit();

			JSONObject jsonResponse = new JSONObject();
			jsonResponse.put("assetsGOA", assetsGOA);
			jsonResponse.put("expensesGOA", expensesGOA);
			return new AEResponse(jsonResponse);
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

	@Override
	public void saveGOA(JSONObject jsonGOA, long ownerId, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();

			// process COA
			ChartOfAccountsDAO coaDAO = daoFactory.getChartOfAccountsDAO(localConnection);
			int dbState = jsonGOA.getInt("dbState");
			switch(dbState) {
			case AEPersistentUtil.DB_ACTION_NONE: {
				break;
			}
			case AEPersistentUtil.DB_ACTION_INSERT: {
				coaDAO.insertGOA(jsonGOA);
				break;
			}
			case AEPersistentUtil.DB_ACTION_UPDATE: {
				coaDAO.updateGOA(jsonGOA);
				break;
			}
			case AEPersistentUtil.DB_ACTION_DELETE: {
				coaDAO.deleteGOA(jsonGOA.getLong("id"));
				break;
			}
			}

			// process GOA accounts
			JSONArray accountsArray = jsonGOA.getJSONArray("accounts");
			for (int i = 0; i < accountsArray.length(); i++) {
				JSONObject account = accountsArray.getJSONObject(i);
				account.put("coaId", jsonGOA.getLong("id"));
			}
			JSONArray savedAcccounts = processGOAAccounts(accountsArray, ownerId, localConnection);
			jsonGOA.put("accounts", savedAcccounts);

			// commit 
			localConnection.commit();
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

	public JSONArray processGOAAccounts(JSONArray accounts, long ownerId, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();

			// get connection and begin transaction
			localConnection = daoFactory.getConnection(aeConnection);

			// manage in transaction
			AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);

			List<JSONObject> jsonList = new ArrayList<JSONObject>();
			for (int i = 0; i < accounts.length(); i++) {
				JSONObject account = accounts.getJSONObject(i);
				jsonList.add(account);
			}

			// process jsonList and add processed to retItems
			JSONArray retAccountsArray = new JSONArray();
			for (Iterator<JSONObject> iterator = jsonList.iterator(); iterator.hasNext();) {
				JSONObject jsonAccount = (JSONObject) iterator.next();
				int dbState = jsonAccount.getInt("dbState");
				switch(dbState) {
				case 0: {
					retAccountsArray.put(jsonAccount);
					// process attributes
					JSONArray attrJSONArray = jsonAccount.optJSONArray("attributes");
					if(attrJSONArray != null) {
						jsonAccount.put(
								"attributes", 
								accountDAO.processAttributes(attrJSONArray, jsonAccount.optLong("id")));
					}
					break;
				}
				case 1: {
					accountDAO.insertGOAAccount(jsonAccount);
					retAccountsArray.put(jsonAccount);
					break;
				}
				case 2: {
					accountDAO.updateGOAAccount(jsonAccount);
					retAccountsArray.put(jsonAccount);
					break;
				}
				case 3: {
					AccPeriod accPeriod = getFirstOpenPeriod(
							ownerId, 
							AEApp.CASH_MODULE_ID, 
							localConnection);

					Date startDate = AEDateUtil.getClearDate(accPeriod.getStartDate());	
					accountDAO.deactivateGOAAccount(jsonAccount, AEDateUtil.addDaysToDate(startDate, -1));

					// accountDAO.deleteGOAAccount(jsonAccount);
					// iterator.remove();
					break;
				}
				}
			}
			return retAccountsArray;
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadGOA(AERequest request) throws AEException {
		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();

			Date activeAfterDate = null;
			if(request.getArguments().has("activeAfterDate")) {
				activeAfterDate = AEDateUtil.parseDateStrict(request.getArguments().getString("activeAfterDate"));
			}

			// get connection and begin transaction
			localConnection = daoFactory.getConnection();

			ChartOfAccountsDAO goaDAO = daoFactory.getChartOfAccountsDAO(localConnection);
			AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);

			// load cash assets group
			JSONObject assetsGOAProxy = request.getArguments().getJSONObject("assetsGOA");
			JSONObject assetsGOA = goaDAO.loadGOA(
					assetsGOAProxy.getLong("ownerId"), assetsGOAProxy.getLong("sysId"));
			// it's a possibility to empty assetsGOA (no group in DB)
			if(assetsGOA.has("id") && assetsGOA.getLong("id") > 0) {
				JSONArray assetsAccounts = null;
				if(activeAfterDate != null) {
					assetsAccounts = accountDAO.loadGOAActiveAccounts(assetsGOA.getLong("id"), activeAfterDate);
				} else {
					assetsAccounts = accountDAO.loadGOAAccounts(assetsGOA.getLong("id"));
				}
				assetsGOA.put("accounts", assetsAccounts);
			}

			// load cash expenses group
			JSONObject expensesGOAProxy = request.getArguments().getJSONObject("expensesGOA");
			JSONObject expensesGOA = goaDAO.loadGOA(
					expensesGOAProxy.getLong("ownerId"), expensesGOAProxy.getLong("sysId"));
			// it's a possibility to empty expensesGOA (no group in DB)
			if(expensesGOA.has("id") && expensesGOA.getLong("id") > 0) {
				JSONArray expensesAccounts = accountDAO.loadGOAAccounts(expensesGOA.getLong("id"));
				if(activeAfterDate != null) {
					expensesAccounts = accountDAO.loadGOAActiveAccounts(expensesGOA.getLong("id"), activeAfterDate);
				} else {
					expensesAccounts = accountDAO.loadGOAAccounts(expensesGOA.getLong("id"));
				}
				expensesGOA.put("accounts", expensesAccounts);
			}

			JSONObject payload = new JSONObject();
			payload.put("assetsGOA", assetsGOA);
			payload.put("expensesGOA", expensesGOA);
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public JSONArray loadCashAccountsByOwner(long ownerId, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);
			return accountDAO.loadCashAccounts(ownerId);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadGeneralJournal(AERequest aeRequest) throws AEException { 
		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			JSONObject payload = new JSONObject();
			AccJournalItemsList journalItems = loadGeneralJournal(
					aeRequest.getArguments().getLong("ownerId"), 
					aeRequest.getArguments().getString("journalCode"), 
					AEDateUtil.parseDateStrict(aeRequest.getArguments().getString("entryDate")), 
					localConnection);
			payload.put("generalJournalItems", journalItems.toJSONArray());

			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AccJournalItemsList loadGeneralJournal(long ownerId, String journalCode, Date entryDate, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			// load entries
			AccJournalDAO accJournDAO = daoFactory.getAccJournalDAO(localConnection);
			return accJournDAO.load(ownerId, journalCode, entryDate);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public JSONArray loadSupplyAccountsByOwner(long ownerId, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);
			return accountDAO.loadSupplyAccounts(ownerId);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}	
	}

	@Override
	public AEResponse export(AERequest aeRequest,  AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Valiate period closed
			 */
			long accPeriodId = aeRequest.getArguments().optLong("accPeriodId");
			boolean isClosed = isAccPeriodClosed(accPeriodId, localConnection);
			if(!isClosed) {
				throw new AEException(
						AEError.System.CANNOT_EXPORT_NOT_CLOSED_PERIOD.getSystemID(), 
						"Impossible de lancer l’export, vous devez clôturer le journal avant.");
			}

			// get export factory, if a factory cannot be created, an exception will be thrown
			ExportFactory expFactory = 
					ExportFactory.getInstance(aeRequest.getArguments().optString("exportTo"));

			/**
			 * Export is possible, so go
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));

			// determine which journal entries have to be exported
			AccJournalDAO accJournalDAO = daoFactory.getAccJournalDAO(localConnection);
			ExportRequest expRequest = accJournalDAO.loadForExport(
					aeRequest.getArguments().optLong("ownerId"), 
					aeRequest.getArguments().optLong("accPeriodId"));

			// customer
			OrganizationDAO orgDAO = DAOFactory.getInstance().getOrganizationDAO(localConnection);
			JSONArray customersArray = orgDAO.loadCustomer(
					Organization.lazyDescriptor(aeRequest.getArguments().getLong("ownerId")));
			JSONObject customer = customersArray.getJSONObject(0);
			expRequest.setCustomer(customer);

			// COA
			ChartOfAccountsDAO coaDAO = daoFactory.getChartOfAccountsDAO(localConnection);
			JSONObject coa = coaDAO.loadByCustomer(aeRequest.getArguments().getLong("ownerId"));
			expRequest.setChartOfAccounts(coa);

			// create destination file
			StringBuffer fileName = new StringBuffer("EXP_");
			fileName
			.append("ME_")
			.append(aeRequest.getArguments().optString("exportTo")).append("_")
			.append(expRequest.grantAppModule().getCode()).append("_")
			.append(AEDateUtil.convertToString(expRequest.getStartDate(), AEDateUtil.EXPORT_FILE_DATE_FORMAT)).append("_")
			.append(AEDateUtil.convertToString(expRequest.getEndDate(), AEDateUtil.EXPORT_FILE_DATE_FORMAT)).append("_")
			.append(customer.opt("code")).append("_")
			.append(Long.toString(System.currentTimeMillis()))
			.append(".").append(expFactory.getDestinationFileExtension());

			File destFile = new File("C:\\AccBureau\\export", fileName.toString());
			expRequest.setDestinationFile(destFile);

			// create ExportJob
			ExportJob expJob = expFactory.getExportJob(invContext, expRequest);

			// run Export
			expJob.run();

			localConnection.beginTransaction();

			// attach file to the closing period
			FileAttachment fileAtt = new FileAttachment();
			AEDescriptive attachedTo = (new AEDescriptorImp(
					aeRequest.getArguments().optLong("accPeriodId"),
					DomainClass.AccPeriod)).getDescriptor();
			fileAtt.setRemoteRoot("C:\\AccBureau\\export");
			fileAtt.setRemotePath(fileName.toString());
			fileAtt.setName(fileName.toString());
			fileAtt.setFileLength(destFile.length());
			fileAtt.setAttachedTo(attachedTo);
			fileAtt.setCompany(Organization.lazyDescriptor(aeRequest.getArguments().optLong("ownerId")));
			fileAttachmentService.manage(fileAtt, invContext, localConnection);

			// update period as exported
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
			accPeriodDAO.exportedPeriod(aeRequest.getArguments().optLong("accPeriodId"));

			localConnection.commit();

			/** 
			 * Process After Export.
			 * The transaction must be commited
			 */
			// send notification
			expJob.onJobFinished();

			// prepare and return response
			JSONObject payload = new JSONObject();
			StringBuffer sb = new StringBuffer("../../FileDownloadServlet?file=");
			sb
			.append(destFile.getAbsolutePath())
			.append("&fileName=").append(destFile.getName());
			payload.put("destFile", sb.toString());
			return new AEResponse(payload);
		} catch (Throwable t) {
			if(localConnection != null) {
				localConnection.rollback();
			}
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}	
	}

	@Override
	public AEResponse exportDaily(AERequest aeRequest,  AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// get export factory, if a factory cannot be created, an exception will be thrown
			ExportFactory expFactory = 
					ExportFactory.getInstance(aeRequest.getArguments().optString("exportTo"));

			/**
			 * Export is possible, so go
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));

			// determine which journal entries have to be exported
			AccJournalDAO accJournalDAO = daoFactory.getAccJournalDAO(localConnection);
			ExportRequest expRequest = accJournalDAO.loadForExport((AEDescriptorsList) aeRequest.getArguments().get("journalEntriesList"));

			// customer
			OrganizationDAO orgDAO = DAOFactory.getInstance().getOrganizationDAO(localConnection);
			JSONArray customersArray = orgDAO.loadCustomer(
					Organization.lazyDescriptor(aeRequest.getArguments().getLong("ownerId")));
			JSONObject customer = customersArray.getJSONObject(0);
			expRequest.setCustomer(customer);

			// COA
			ChartOfAccountsDAO coaDAO = daoFactory.getChartOfAccountsDAO(localConnection);
			JSONObject coa = coaDAO.loadByCustomer(aeRequest.getArguments().getLong("ownerId"));
			expRequest.setChartOfAccounts(coa);

			// create destination file
			StringBuffer fileName = new StringBuffer("EXP_");
			fileName
			.append("ME_")
			.append(aeRequest.getArguments().optString("exportTo")).append("_DAILY_")
			.append(expRequest.grantAppModule().getCode()).append("_")
			.append(AEDateUtil.convertToString(expRequest.getEndDate(), AEDateUtil.EXPORT_FILE_DATE_FORMAT)).append("_")
			.append(Long.toString(System.currentTimeMillis()))
			.append(".").append(expFactory.getDestinationFileExtension());

			File destFile = new File("C:\\AccBureau\\export", fileName.toString());
			expRequest.setDestinationFile(destFile);

			// create ExportJob
			ExportJob expJob = expFactory.getExportJob(invContext, expRequest);

			// run Export
			expJob.run();

			localConnection.beginTransaction();

			// attach file to the closing period
			FileAttachment fileAtt = new FileAttachment();
			AEDescriptive attachedTo = (new AEDescriptorImp(
					aeRequest.getArguments().optLong("accPeriodId"),
					DomainClass.AccPeriod)).getDescriptor();
			fileAtt.setRemoteRoot("C:\\AccBureau\\export");
			fileAtt.setRemotePath(fileName.toString());
			fileAtt.setName(fileName.toString());
			fileAtt.setFileLength(destFile.length());
			fileAtt.setAttachedTo(attachedTo);
			fileAtt.setCompany(Organization.lazyDescriptor(aeRequest.getArguments().optLong("ownerId")));
			fileAttachmentService.manage(fileAtt, invContext, localConnection);

			/**
			 * Set to exported
			 */
			if(aeRequest.getArguments().optBoolean("setExported")) {
				long accPeriodId = aeRequest.getArguments().optLong("accPeriodId");
				if(accPeriodId > 0) {
					AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
					accPeriodDAO.exportedPeriod(accPeriodId);
				}
			}

			localConnection.commit();

			/** 
			 * Process After Export.
			 * The transaction must be commited
			 */
			// send notification
			expJob.onJobFinished();

			// prepare and return response
			JSONObject payload = new JSONObject();
			StringBuffer sb = new StringBuffer("../../FileDownloadServlet?file=");
			sb
			.append(destFile.getAbsolutePath())
			.append("&fileName=").append(destFile.getName());
			payload.put("destFile", sb.toString());
			return new AEResponse(payload);
		} catch (Throwable t) {
			if(localConnection != null) {
				localConnection.rollback();
			}
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}	
	}

	@Override
	public AEResponse loadAccPeriodByFilter(AERequest request) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(request.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * load acc periods
			 */
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
			AccPeriodItems periodItems = accPeriodDAO.loadByFilter(request.getArguments().optLong("ownerId"));

			/**
			 * return response
			 */
			JSONObject payload = new JSONObject();
			payload.put("accPeriods", periodItems.toJSONArray());
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}	

	private boolean isAccPeriodClosed(long periodId, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
			return accPeriodDAO.isClosed(periodId);
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public JSONArray loadSaleAccountsByOwner(long ownerId, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);
			return accountDAO.loadSaleAccounts(ownerId);
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}	
	}

	@Override
	public JSONArray loadBankAccountsByOwner(long ownerId, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);
			return accountDAO.loadBankAccounts(ownerId);
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}	
	}

	@Override
	public JSONArray loadAccounts(long orgId, AEConnection aeConnection, String whereSql) throws AEException {
		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);
			return accountDAO.loadAccounts(orgId, whereSql);
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadCOA(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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

			// authorize whether specified principal can operate with specified customer
			ap.ownershipValidator(customerId);
			
			// authorize 
			authorize(new AuthPermission("System/Configuration/Customer/ChartOfAccount", AuthPermission.READ), invContext, Organization.lazyDescriptor(customerId));

			// get connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			// load COA
			JSONObject coa = loadCOA(customerId, localConnection);

			// create and return response
			JSONObject payload = new JSONObject();
			if(coa != null) {
				payload.put(AccAccount.JSONKey.chartOfAccounts.name(), coa);
			} else {
				payload.put(AccAccount.JSONKey.chartOfAccounts.name(), AEStringUtil.EMPTY_STRING);
			}
			return new AEResponse(payload);
		} catch (Throwable t) {
			logger.errorv("{0} in {1}#{2}: {3}", t.getClass().getSimpleName(), this.getClass().getSimpleName(), "loadCOA", t.getMessage());
			throw new AEException(t); // keep source message
		} finally {
			AEConnection.close(localConnection);
		}	
	}

	@Override
	public AEResponse saveCOA(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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

			// authorize whether specified principal can operate with specified customer
			ap.ownershipValidator(customerId);
			
			// authorize 
			authorize(new AuthPermission("System/Configuration/Customer/ChartOfAccount", AuthPermission.SAVE), invContext, Organization.lazyDescriptor(customerId));

			// get connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			// save COA
			JSONObject coa = arguments.getJSONObject("chartOfAccounts");
			coa.put(AEDomainObject.JSONKey.ownerId.name(), customerId);
			saveCOA(coa, localConnection);

			// create and return response
			JSONObject payload = new JSONObject();
			payload.put(AccAccount.JSONKey.chartOfAccounts.name(), coa);
			return new AEResponse(payload);
		} catch (Throwable t) {
			logger.errorv("{0} in {1}#{2}: {3}", t.getClass().getSimpleName(), this.getClass().getSimpleName(), "saveCOA", t.getMessage());
			throw new AEException(t); // keep source message
		} finally {
			AEConnection.close(localConnection);
		}	
	}

	@Override
	public void loadAccFinalBalance(AccAccountBalance accBalance, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			AccJournalDAO journalDAO = daoFactory.getAccJournalDAO(localConnection);

			// detect the last balance
			Integer year = journalDAO.loadLastOpeningBalanceYear(accBalance.getAccAccount().getDescriptor(), accBalance.getPeriod().getEndDate());
			Date startDate = null;
			if(year != null) {
				startDate = AEDateUtil.getClearDateTime(AEDateUtil.getFirstDate(0, year));
			} else {
				startDate = AEDateUtil.getClearDateTime(new Date(0L));
			}
			accBalance.getPeriod().setStartDate(startDate);

			// load opening balance
			journalDAO.loadOpeningBalance(accBalance);

			// load turnover
			journalDAO.calculateTurnover(accBalance);

			// calculate balance
			accBalance.calculateFinalBalance();
		} catch (Throwable t) {
			logger.error("loadAccFinalBalance failed. ", t);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public void loadAccFinalBalance(AccAccountBalance accBalance, AEDescriptive journal, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			AccJournalDAO journalDAO = daoFactory.getAccJournalDAO(localConnection);

			// detect the last balance
			Integer year = journalDAO.loadLastOpeningBalanceYear(accBalance.getAccAccount().getDescriptor(), accBalance.getPeriod().getEndDate());
			Date startDate = null;
			if(year != null) {
				startDate = AEDateUtil.getClearDateTime(AEDateUtil.getFirstDate(0, year));
			} else {
				startDate = AEDateUtil.getClearDateTime(new Date(0L));
			}
			accBalance.getPeriod().setStartDate(startDate);

			// load opening balance
			journalDAO.loadOpeningBalance(accBalance);

			// load turnover
			journalDAO.calculateTurnover(accBalance, journal);

			// calculate balance
			accBalance.calculateFinalBalance();
		} catch (Throwable t) {
			logger.error("loadAccFinalBalance failed. ", t);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadInitialBalance(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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
			authorize(new AuthPermission("System/Configuration/Customer/InitialBalance", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * Date
			 * Will be detected or created later
			 */
			Date date = null;

			/**
			 * Get concrete COA, balances and merge them
			 */
			// load coa
			ChartOfAccountsDAO coaModelDAO = daoFactory.getChartOfAccountsDAO(localConnection);
			AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);
			JSONObject coa = coaModelDAO.loadByCustomer(ownerId);

			// load accounts from COA
			JSONArray accounts = null;
			if(coa.has(AEDomainObject.JSONKey.id.name())) {
				accounts = accountDAO.loadBalanceAccounts(coa.getLong(AEDomainObject.JSONKey.id.name()));
			}
			if(accounts == null) {
				accounts = new JSONArray();
			}

			// load balances
			JSONArray balances = accountDAO.loadInitialBalance(ownerId);

			// merge
			String idKey = AEDomainObject.JSONKey.id.name();
			if(balances != null && balances.length() > 0) {
				for (int i = 0; i < accounts.length(); i++) {
					for (int j = 0; j < balances.length(); j++) {
						JSONObject account = accounts.getJSONObject(i);
						JSONObject balance = balances.getJSONObject(j);

						if(account.getLong(idKey) == balance.getLong(idKey)) {
							account.put("dtAmount", balance.getDouble("dtAmount"));
							account.put("ctAmount", balance.getDouble("ctAmount"));
						}

						if(date == null) {
							date = AEDateUtil.parseDateStrict(balance.getString("date"));
						}
					}
				}
			} else {
				date = new Date();
			}

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject()
			.put(AccAccount.JSONKey.accounts.name(), accounts)
			.put("date", AEDateUtil.formatToSystem(date));
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
	public AEResponse saveInitialBalance(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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
			authorize(new AuthPermission("System/Configuration/Customer/InitialBalance", AuthPermission.SAVE), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * Date and owner
			 */
			Date date = AEDateUtil.parseDateStrict(arguments.getString("date"));
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);

			/**
			 * Accounts and validate
			 */
			JSONArray accounts = arguments.getJSONArray(AccAccount.JSONKey.accounts.name());
			if(accounts.length() > 0) {
				double dtSum = 0.0;
				double ctSum = 0.0;
				for (int i = 0; i < accounts.length(); i++) {
					JSONObject ab = accounts.getJSONObject(i);

					double dt = AEMath.doubleValue(JSONUtil.parseDoubleStrict(ab, "dtAmount"));
					dtSum += dt;

					double ct =	AEMath.doubleValue(JSONUtil.parseDoubleStrict(ab, "ctAmount"));
					ctSum += ct;
				}
				double diff = dtSum - ctSum;
				if(!AEMath.isZeroAmount(diff)) {
					throw new AEException("Votre balance n'est pas équilibrée. Ecart de " + AEMath.toAmountFrenchString(diff) + " .");
				}
			}
			
			// start transaction here
			localConnection.beginTransaction();
			
			/**
			 * AccPeriod
			 */
			AccPeriod accPeriod = getAccPeriod(
					ownerId, 
					AEApp.ACCOUNTING_MODULE_ID, 
					AEDateUtil.getClearDate(date), 
					localConnection);
			
			// validate in open period
			if(accPeriod != null && accPeriod.isClosed()) {
				throw AEError.System.CANNOT_INSERT_UPDATE_CLOSED_PERIOD.toException();
			}
			
			/**
			 * Insert or update in transaction
			 */
			AccountDAO accDAO = daoFactory.getAccountDAO(localConnection);
			accDAO.insertOrUpdateInitialBalance(accounts, AEDateUtil.getYear(date), date, ownerId);

			// process null period
			// MUST be:
			//   -  after insertOrUpdateInitialBalance
			//   -  in transaction
			if(accPeriod == null) {
				accPeriod = createAccountingPeriod(ownerDescr, date, localConnection);
			}
			
			// the initial balance's period should be the first period
			// i.e. there is no previous periods
			// IMPORTANT: check just before commit
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
			if(accPeriodDAO.hasPrevious(ownerDescr, AEApp.ACCOUNTING_MODULE_ID, AEDateUtil.getFirstDate(0, AEDateUtil.getYear(date)))) {
				throw AEError.System.INCORRECT_ACC_PERIOD.toException();
			}
			
			/**
			 * commit
			 */
			localConnection.commit();

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject()
			.put(AccAccount.JSONKey.accounts.name(), accounts)
			.put("date", AEDateUtil.formatToSystem(date));
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
	public AEResponse loadDonations(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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
//			ContributorDAO contrDAO = daoFactory.getContributorDAO(localConnection);
			AEDescriptor compDescr = Organization.lazyDescriptor(ownerId);

			/**
			 * Year
			 */
			int year = arguments.optInt("year", AEDateUtil.getYear(new Date()));
			
			/**
			 * Sync donations
			 */
			JSONObject payload = syncDonationsJson(compDescr, year, localConnection);
/*
			// donations
			ContributorDonationsList donations = contrDAO.loadDonationsToCompany(compDescr, year);

			// accountancy
			ContributorDonationsList accountancy = contrDAO.loadDonationsAccountancyToCompany(compDescr, year);

			// first reset accountancy data
			for (Iterator<ContributorDonation> iterator = donations.iterator(); iterator.hasNext();) {
				ContributorDonation donation = (ContributorDonation) iterator.next();
				donation.amountAcc = 0.0;
			}

			// then update with up to date information from accountancy
			for (Iterator<ContributorDonation> iterator = accountancy.iterator(); iterator.hasNext();) {
				ContributorDonation accountancyDonation = (ContributorDonation) iterator.next();
				if(accountancyDonation.isPersistent()) {
					// setup identified accountancy amount
					ContributorDonation recognizedDonation = donations.getByPerson(accountancyDonation.getEmployee().getPerson().getDescriptor().getID());
					if(recognizedDonation != null) {
						recognizedDonation.amountAcc += accountancyDonation.amount;
					}

					// remove recognized
					iterator.remove();
				}
			}

			// identify updates
			for (Iterator<ContributorDonation> iterator = donations.iterator(); iterator.hasNext();) {
				ContributorDonation donation = (ContributorDonation) iterator.next();
				if(!AEMath.isZeroAmount(donation.amount - (donation.amountAcc + donation.amountChange))) {
					donation.setUpdated();
				}
			}
*/

			/**
			 * Create and return response
			 */
			return new AEResponse(payload);
		} catch (Exception e) {
			logger.error(e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public JSONObject syncDonationsJava(AEDescriptor compDescr, int year, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			ContributorDAO contrDAO = daoFactory.getContributorDAO(localConnection);

			// begin transaction
			localConnection.beginTransaction();
			
			// donations
			ContributorDonationsList donations = contrDAO.loadDonationsToCompany(compDescr, year);

			// accountancy
			ContributorDonationsList accountancy = contrDAO.loadDonationsAccountancyToCompany(compDescr, year);

			/**
			 * Setup donations with accountancy data
			 */
			// first reset accountancy data
			for (Iterator<ContributorDonation> iterator = donations.iterator(); iterator.hasNext();) {
				ContributorDonation donation = (ContributorDonation) iterator.next();
				donation.amountAcc = 0.0;
			}

			// then update with up to date information from accountancy
			for (Iterator<ContributorDonation> iterator = accountancy.iterator(); iterator.hasNext();) {
				ContributorDonation accountancyDonation = (ContributorDonation) iterator.next();
				if(accountancyDonation.isPersistent()) {
					// setup identified accountancy amount
					ContributorDonation recognizedDonation = donations.getByPerson(accountancyDonation.getEmployee().getPerson().getDescriptor().getID());
					if(recognizedDonation != null) {
						recognizedDonation.amountAcc += accountancyDonation.amount;
					}

					// remove recognized
					iterator.remove();
				}
			}

			// identify changes
			for (Iterator<ContributorDonation> iterator = donations.iterator(); iterator.hasNext();) {
				ContributorDonation donation = (ContributorDonation) iterator.next();
				double amountCurrent = donation.amountAcc + donation.amountChange;
				double amountDiff = donation.amount - amountCurrent;
				if(!AEMath.isZeroAmount(amountDiff)) {
					donation.amount = amountCurrent;
					donation.setUpdated();
				}
			}

			/**
			 * Manage donations
			 */
			if(year > 999) {
				for (Iterator<ContributorDonation> iterator = donations.iterator(); iterator.hasNext();) {
					ContributorDonation contributorDonation = (ContributorDonation) iterator.next();
					if(contributorDonation.getYear() != year) {
						throw AEError.System.INVALID_PARAMETER.toException();
					}
					if(contributorDonation.amount < 0.0) {
//						// throw AEError.System.INVALID_PARAMETER.toException();
//						throw new AEException(
//								String.format("Solde négatif (%s %s, %s): %s"
//								, contributorDonation.getEmployee() != null ? contributorDonation.getEmployee().getLastName() : AEStringUtil.EMPTY_STRING
//								, contributorDonation.getEmployee() != null ? contributorDonation.getEmployee().getFirstName() : AEStringUtil.EMPTY_STRING
//								, Integer.toString(contributorDonation.getYear())
//								, AEMath.toAmountFrenchString(contributorDonation.amount)));
					}
					if(contributorDonation.isPersistent()) {
						if(contributorDonation.isUpdated()) {
							contrDAO.update(contributorDonation);
						}
					} else {
						contrDAO.insert(contributorDonation);
					}
				}
			}
			
			/**
			 * Commit
			 */
			localConnection.commit();
			
//			JSONObject payload = new JSONObject()
//				.put("donations", donations.toJSONArray())
//				.put("accountancy", accountancy.toJSONArray())
//				.put("year", year);
			JSONObject payload = new JSONObject()
				.put("donations", (Object) donations)
				.put("accountancy", (Object) accountancy)
				.put("year", year);
			return payload;
		} catch (Exception e) {
			AEConnection.rollback(localConnection);
			logger.error(e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public JSONObject syncDonationsJson(AEDescriptor compDescr, int year, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			JSONObject donationsPojo = syncDonationsJava(compDescr, year, localConnection);
			
			JSONObject payload = new JSONObject()
				.put("donations", ((ContributorDonationsList) donationsPojo.get("donations")).toJSONArray())
				.put("accountancy", ((ContributorDonationsList) donationsPojo.get("accountancy")).toJSONArray())
				.put("year", year);
			
			return payload;
		} catch (Exception e) {
			logger.error(e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public AEResponse saveDonations(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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

			/**
			 * Year
			 */
			int year = arguments.getInt("year");
			
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

			// donations
			JSONArray donationsJson = arguments.getJSONArray("donations");
			ContributorDonationsList donations = new ContributorDonationsList();
			donations.create(donationsJson);

			localConnection.beginTransaction();

			/**
			 * Manage donations
			 */
			ContributorDAO contrDAO = daoFactory.getContributorDAO(localConnection);

			for (Iterator<ContributorDonation> iterator = donations.iterator(); iterator.hasNext();) {
				ContributorDonation contributorDonation = (ContributorDonation) iterator.next();
				if(contributorDonation.getYear() != year) {
					throw AEError.System.INVALID_PARAMETER.toException();
				}
				if(contributorDonation.amount < 0.0) {
					throw AEError.System.INVALID_PARAMETER.toException();
				}
				if(contributorDonation.isPersistent()) {
					if(contributorDonation.isUpdated()) {
						contrDAO.update(contributorDonation);
					}
				} else {
					contrDAO.insert(contributorDonation);
				}
			}

			localConnection.commit();

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject()
			.put("donations", donations.toJSONArray())
			.put("year", year);
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
	public AEResponse loadBudget(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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
			authorize(new AuthPermission("System/Configuration/Customer/Budget", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			// invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * Year
			 */
			int year = arguments.getInt("year");

			/**
			 * Get concrete COA, budget and merge them
			 */
			// load coa
			ChartOfAccountsDAO coaModelDAO = daoFactory.getChartOfAccountsDAO(localConnection);
			AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);
			JSONObject coa = coaModelDAO.loadByCustomer(ownerId);

			// load accounts from COA
			JSONArray accounts = null;
			if(coa.has(AEDomainObject.JSONKey.id.name())) {
				accounts = accountDAO.loadBudgetAccounts(coa.getLong(AEDomainObject.JSONKey.id.name()));
			}
			if(accounts == null) {
				accounts = new JSONArray();
			}

			// load budget
			JSONArray budget = accountDAO.loadBudget(ownerId, year);

			// merge
			String idKey = AEDomainObject.JSONKey.id.name();
			if(budget != null && budget.length() > 0) {
				for (int i = 0; i < accounts.length(); i++) {
					for (int j = 0; j < budget.length(); j++) {
						JSONObject account = accounts.getJSONObject(i);
						JSONObject accBudget = budget.getJSONObject(j);

						if(account.getLong(idKey) == accBudget.getLong(idKey)) {
							account.put("amount", accBudget.getDouble("amount"));
						}
					}
				}
			}

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject()
			.put(AccAccount.JSONKey.accounts.name(), accounts)
			.put("year", year);
			return new AEResponse(payload);
		} catch (Exception e) {
			logger.error("loadBudget failed", e);
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse saveBudget(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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
			authorize(new AuthPermission("System/Configuration/Customer/Budget", AuthPermission.SAVE), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			// invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * Date
			 */
			int year = arguments.getInt("year");

			/**
			 * Accounts
			 */
			JSONArray accounts = arguments.getJSONArray(AccAccount.JSONKey.accounts.name());

			/**
			 * Insert or update in transaction
			 */
			localConnection.beginTransaction();
			AccountDAO accDAO = daoFactory.getAccountDAO(localConnection);
			accDAO.insertOrUpdateBudget(accounts, year, ownerId);

			/**
			 * commit
			 */
			localConnection.commit();

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject()
			.put(AccAccount.JSONKey.accounts.name(), accounts)
			.put("year", year);
			return new AEResponse(payload);
		} catch (Exception e) {
			logger.error("saveBudget failed", e);
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadBordereauParoisse(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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
			int year = arguments.optInt("year", -1);

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
			authorize(new AuthPermission("System/FinancialTransaction/BordereauParoisse", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);
			BordereauParoisseDAO bordereauParoisseDAO = daoFactory.getBordereauParoisseDAO(localConnection);
			AEDescriptor compDescr = Organization.lazyDescriptor(ownerId);

			/**
			 * Go
			 */
			AEDescriptor tenantDescr = Organization.lazyDescriptor(ownerId);
			if(year == -1) {
				year = AEDateUtil.getYear(new Date());
			}

			// load BordereauParoissesList from DB
			BordereauParoissesList bordereauParoisses = bordereauParoisseDAO.loadBordereauParoisses(compDescr, year);
			if(Organization.AppType.mense.equals(invContext.getAuthPrincipal().getAppType())) {
				Iterator<BordereauParoisse> iterator = bordereauParoisses.iterator();
				while(iterator.hasNext()) {
					BordereauParoisse bp = iterator.next();
					if(!"14".equals(bp.getCode()) && !"16".equals(bp.getCode()) && !"20".equals(bp.getCode()) && !"22".equals(bp.getCode())) {
						iterator.remove();
					}
				}
			}

			// load acc data (sum by code)
			AccJournalDAO accJournalDAO = daoFactory.getAccJournalDAO(localConnection);
			Date dateInYear = AEDateUtil.getFirstDate(0, year);
			AETimePeriod period = new AETimePeriod(AEDateUtil.beginOfTheYear(dateInYear), AEDateUtil.endOfTheYear(dateInYear));
			QuetesList quetesSum = accJournalDAO.loadQuetesSum(tenantDescr, period);

			// setup acc data to bordereauParoisses
			Map<String, BordereauParoisse> bpMap = bordereauParoisses.toMap();
			for (Quete queteSum : quetesSum) {
				BordereauParoisse bp = bpMap.get(queteSum.getCode());
				if(bp != null) {
					if(!AEMath.isZeroAmount(bp.getAccAmount() - queteSum.getAmount())) {
						bp.setAccAmount(queteSum.getAmount());
						bp.setUpdated();
					}
				}
			}

			// calculate toPayAmount
			for (BordereauParoisse bp : bordereauParoisses) {
				bp.calculateToPayAmount();
			}

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject()
			.put(BordereauParoisse.JSONKey.quetes.name(), bordereauParoisses.toJSONArray())
			.put("year", year)
			.put("modifiable", true);
			return new AEResponse(payload);
		} catch (Exception e) {
			logger.error(e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse saveBordereauParoisse(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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
			AEDescriptor tenantDescr = Organization.lazyDescriptor(ownerId);
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
			authorize(new AuthPermission("System/FinancialTransaction/BordereauParoisse", AuthPermission.SAVE), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			// quetes
			JSONArray bordereauParoissesJson = arguments.getJSONArray(BordereauParoisse.JSONKey.quetes.name());
			BordereauParoissesList bordereauParoisses = new BordereauParoissesList();
			bordereauParoisses.create(bordereauParoissesJson);

			localConnection.beginTransaction();

			/**
			 * Manage donations
			 */

			// prepare
			bordereauParoisses.setYear(year);
			bordereauParoisses.setCompany(tenantDescr);

			// manage
			BordereauParoisseDAO bordereauParoisseDAO = daoFactory.getBordereauParoisseDAO(localConnection);
			for (Iterator<BordereauParoisse> iterator = bordereauParoisses.iterator(); iterator.hasNext();) {
				BordereauParoisse bp = (BordereauParoisse) iterator.next();
				if(bp.isPersistent()) {
					if(bp.isUpdated()) {
						bordereauParoisseDAO.update(bp);
					}
				} else {
					bordereauParoisseDAO.insert(bp);
				}
			}

			localConnection.commit();

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject()
			.put(BordereauParoisse.JSONKey.quetes.name(), bordereauParoisses.toJSONArray())
			.put("year", year)
			.put("modifiable", true);
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
	public AEResponse loadBudgetReal(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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
			authorize(new AuthPermission("System/Configuration/Customer/Budget", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			// invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * Year or (startDate, endDate)
			 */
			Date startDate = null;
			Date endDate = null;
			int year = 0;
			if(arguments.has("year")) {
				year = arguments.getInt("year");
				
				// calculate toward year
				startDate = AEDateUtil.getFirstDate(0, year);
				endDate = AEDateUtil.endOfTheYear(startDate);
			} else if(arguments.has("startDate") && arguments.has("endDate")) {
				// startDate
				startDate = AEDateUtil.parseDateStrict(arguments.getString("startDate"));
				if(startDate == null) {
					throw AEError.System.INVALID_PARAMETER.toException();
				}
				
				// endDate
				endDate = AEDateUtil.parseDateStrict(arguments.getString("endDate"));
				if(endDate == null) {
					throw AEError.System.INVALID_PARAMETER.toException();
				}
				
				// calculate toward endDate
				year = AEDateUtil.getYear(endDate);
			} else {
			   throw AEError.System.INVALID_PARAMETER.toException();
			}
			AEDescriptor tenantDescr = Organization.lazyDescriptor(ownerId);

			/**
			 * Get concrete COA, budget and merge them
			 */
			// load coa
			ChartOfAccountsDAO coaModelDAO = daoFactory.getChartOfAccountsDAO(localConnection);
			AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);
			JSONObject coa = coaModelDAO.loadByCustomer(ownerId);

			// load accounts from COA
			JSONArray accounts = null;
			if(coa.has(AEDomainObject.JSONKey.id.name())) {
				accounts = accountDAO.loadBudgetAccounts(coa.getLong(AEDomainObject.JSONKey.id.name()));
			}
			if(accounts == null) {
				accounts = new JSONArray();
			}

			// load budget
			JSONArray budget = accountDAO.loadBudget(ownerId, year);

			// load budgetReal
			AETimePeriod period = new AETimePeriod(startDate, endDate);
			AccJournalDAO accJournalDAO = daoFactory.getAccJournalDAO(localConnection);
			JSONArray budgetRealIncome = accJournalDAO.loadBudgetRealIncome(tenantDescr, period);
			JSONArray budgetRealExpense = accJournalDAO.loadBudgetRealExpense(tenantDescr, period);

			// merge
			String idKey = AEDomainObject.JSONKey.id.name();
			if(budget != null && budget.length() > 0) {
				for (int i = 0; i < accounts.length(); i++) {
					for (int j = 0; j < budget.length(); j++) {
						JSONObject account = accounts.getJSONObject(i);
						JSONObject accBudget = budget.getJSONObject(j);

						if(account.getLong(idKey) == accBudget.getLong(idKey)) {
							account.put("budgetAmount", accBudget.getDouble("amount"));
						}
					}
				}
			}

			JSONArray accountsIncome = new JSONArray();
			JSONArray accountsExpense = new JSONArray();
			for (int i = 0; i < accounts.length(); i++) {
				JSONObject account = accounts.getJSONObject(i);
				double budgetAmount = account.optDouble("budgetAmount", 0.0);
				double realAmount = 0.0;

				// real income
				for (int j = 0; j < budgetRealIncome.length(); j++) {
					JSONObject realIncome = budgetRealIncome.getJSONObject(j);

					if(account.getLong(idKey) == realIncome.getLong(idKey)) {
						realAmount = realIncome.getDouble("amount");
					}
				}

				// real expense
				for (int j = 0; j < budgetRealExpense.length(); j++) {
					JSONObject realExpense = budgetRealExpense.getJSONObject(j);

					if(account.getLong(idKey) == realExpense.getLong(idKey)) {
						realAmount = realExpense.getDouble("amount");
					}
				}

				// calculate differences
				double diffAmount = realAmount - budgetAmount;
				double diffPercent = AEMath.round(budgetAmount != 0.0 ? realAmount/budgetAmount * 100.0 : 0.0, 1); 

				// set
				account.put("realAmount", realAmount);
				account.put("diffAmount", diffAmount);
				account.put("diffPercent", diffPercent);

				// put into income or expense array
				String code = AEStringUtil.trim(account.getString(AEDomainObject.JSONKey.code.name()));
				if(code.startsWith("7")) {
					accountsIncome.put(account);
				} else if(code.startsWith("6")) {
					accountsExpense.put(account);
				}
			}

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject()
				.put("income", accountsIncome)
				.put("expense", accountsExpense);
			if(year > 0) {
				payload.put("year", year);
			}
			if(startDate != null) {
				payload.put("startDate", AEDateUtil.formatToSystem(startDate));
			}
			if(endDate != null) {
				payload.put("endDate", AEDateUtil.formatToSystem(endDate));
			}
			return new AEResponse(payload);
		} catch (Exception e) {
			logger.error("loadBudget failed", e);
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadAccPeriods(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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
			authorize(new AuthPermission("System/Configuration/Customer/AccPeriod", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			// invContext.setProperty(AEInvocationContext.AEConnection, localConnection);


			AEDescriptor tenantDescr = Organization.lazyDescriptor(ownerId);

			/**
			 * Load Acc periods
			 */
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
			AccPeriodItems accPeriodItems = accPeriodDAO.loadAll(tenantDescr, AEApp.ACCOUNTING_MODULE_ID);

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject()
				.put("accPeriods", accPeriodItems.toJSONArray());
			return new AEResponse(payload);
		} catch (Exception e) {
			logger.error("loadAccPeriods failed ", e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse closeAccPeriod(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);
			long accPeriodId = arguments.getLong("accPeriodId");
			boolean suppressWarnings = arguments.optBoolean("suppressWarnings");

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
			authorize(new AuthPermission("System/Configuration/Customer/AccPeriod", AuthPermission.SAVE), invContext, ownerDescr);

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(invContext.getAEConnection());
			invContext.setAEConnection(localConnection);

			AEDescriptor tenantDescr = Organization.lazyDescriptor(ownerId);

			/**
			 * Load Acc period
			 */
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
			AccPeriod accPeriod = accPeriodDAO.loadAccPeriod(accPeriodId);
			
			/**
			 * Validate
			 */
			if(accPeriod == null) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}
			
			if(accPeriod.getCompany() == null || accPeriod.getCompany().getDescriptor().getID() != ownerId) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}
			
			if(accPeriod.isClosed()) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}
			
			// The current date is later than the end date from the acc period
			if(!(new Date()).after(accPeriod.getEndDate())) {
				throw new AEException("L’année en cours n’est pas terminée.");
			}

			// previous period must be closed
			boolean isPreviousClosed = accPeriodDAO.isPreviousClosed(tenantDescr, AEApp.ACCOUNTING_MODULE_ID, accPeriod.getStartDate());
			if(!isPreviousClosed) {
				throw new AEException("Vérifiez que les exercices précédents sont clos. ");
			}
			
			// Forecast budget from the next year is seized (Menu GERER/PREVISIONNEL/Saisir le budget prévisionnel”.
			AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);
			JSONArray budget = accountDAO.loadBudget(ownerId, AEDateUtil.getYear(accPeriod.getEndDate()) + 1);
			if(budget == null || budget.length() == 0) {
				throw new AEException(
						"Le budget prévisionnel de l’année " 
						+  Integer.toString(AEDateUtil.getYear(accPeriod.getEndDate()))
						+ " n’a pas été saisie <p>(voir le menu GERER/PREVISIONNEL/Saisir le budget prévisionnel).</p> ");
			}
			
			// Bank balances have been seized for the current year (Menu “PARAMETRAGES/IDENTITE/Relevés bancaires”).
			BankAccountDAO bankAccountDAO = daoFactory.getBankAccountDAO(localConnection);
			if(!bankAccountDAO.checkBankBalancesExists(tenantDescr, AEDateUtil.getYear(accPeriod.getEndDate()))) {
				throw new AEException("Les soldes des relevés bancaires n’ont pas été saisies <p>(voir le menu PARAMETRAGE/IDENTITE/Relevés bancaires).</p> ");
			}
			
			/**
			 * local variables
			 */
			Date firstDatePeriodNext = AEDateUtil.getClearDate(AEDateUtil.addDaysToDate(accPeriod.getEndDate(), 1));
			int year = AEDateUtil.getYear(accPeriod.getEndDate());
			AccountDAO accDAO = daoFactory.getAccountDAO(localConnection);
			
			/**
			 * Warnings
			 */
			if(!suppressWarnings) {
				// check bank balances
				JSONArray warnings = new JSONArray();
				BankAccountsList banksList = bankAccountDAO.load(Organization.lazyDescriptor(ownerId));
				for (BankAccount bankAccount : banksList) {
					// load balance
					BankAccountBalancesList bankAccountBalancesList = bankAccountDAO.loadBankAccountBalances(
							bankAccount.getDescriptor(), 
							year);
					if(bankAccountBalancesList.size() > 0) {
						BankAccountBalance bankAccountBalance = bankAccountBalancesList.get(bankAccountBalancesList.size() -1);

						// load acc balance
						AccAccountBalance accAccountBalance = new AccAccountBalance();
						accAccountBalance.setAccAccount(bankAccount.getAccount());
						accAccountBalance.setPeriod(new AETimePeriod(accPeriod.getStartDate(), accPeriod.getEndDate()));
						loadAccFinalBalance(accAccountBalance, invContext, localConnection);

						if(!AEMath.isZeroAmount(accAccountBalance.getFinalBalance() - bankAccountBalance.getBankFinalBalance())) {
							String _msg = 
									"Il y a au mois un des soldes bancaires saisis dans le menu PARAMETRAGE/IDENTITE/Relevés bancaires qui ne correspond pas au solde comptable.<br/>"
									+ "Pour poursuivre malgré tout la clôture cliquez oui pour modifier votre saisie cliquez non.";
							warnings.put(_msg);
							break;
						}
					}
				}
				if(warnings.length() > 0) {
					JSONObject payload = new JSONObject().put("accPeriod", accPeriod.toJSONObject());
					payload.put("warnings", warnings);
					return new AEResponse(payload);
				}
			}
			
			/**
			 * Close the period
			 */
			localConnection.beginTransaction();
				
			AccJournalDAO accJournalDAO = daoFactory.getAccJournalDAO(localConnection);
			
			/**
			 * create next period if doesn't exist
			 */
			AccPeriod accPeriodNext = getAccPeriod(
					ownerId, 
					AEApp.ACCOUNTING_MODULE_ID, 
					firstDatePeriodNext, 
					localConnection);
			if(accPeriodNext == null) {
				accPeriodNext = createAccountingPeriod(tenantDescr, firstDatePeriodNext, localConnection);
			}
			
			/**
			 * Create engagements/titres if not exist
			 */
			// load engagements/titres for closing period
			AERequest engTitresRequest = new AERequest(new JSONObject()
				.put(AEDomainObject.JSONKey.ownerId.name(), ownerId)
				.put(AEDomainObject.JSONKey.sOwnerId.name(), sOwnerId)
				.put("year", AEDateUtil.getYear(accPeriod.getEndDate())));
			AEResponse engTitresResponse = councilService.loadEngTitres(engTitresRequest, invContext);
			JSONArray engagements = engTitresResponse.getPayload().getJSONArray("engagements");
			JSONArray titres = engTitresResponse.getPayload().getJSONArray("titres");
			
			// prepare insert request for the next period
			AERequest insertRequest = new AERequest(new JSONObject()
				.put(AEDomainObject.JSONKey.ownerId.name(), ownerId)
				.put(AEDomainObject.JSONKey.sOwnerId.name(), sOwnerId)
				.put("year", AEDateUtil.getYear(firstDatePeriodNext)));

			// load engagements/titres for next period
			AERequest engTitresRequestPeriodNext = new AERequest(new JSONObject()
					.put(AEDomainObject.JSONKey.ownerId.name(), ownerId)
					.put(AEDomainObject.JSONKey.sOwnerId.name(), sOwnerId)
					.put("year", AEDateUtil.getYear(firstDatePeriodNext)));
			AEResponse engTitresResponsePeriodNext = councilService.loadEngTitres(engTitresRequestPeriodNext, invContext);

			// if engagements next missing, prepare for insert
			JSONArray engagementsPeriodNext = engTitresResponsePeriodNext.getPayload().getJSONArray("engagements");
			if(engagementsPeriodNext == null || engagementsPeriodNext.length() == 0) {
				// prepare closing engagements for insert
				for (int i = 0; i < engagements.length(); i++) {
					JSONObject e = engagements.getJSONObject(i);
					e.put(AEDomainObject.JSONKey.id.name(), AEPersistentUtil.NEW_ID);
					e.put(AEDomainObject.JSONKey.dbState.name(), AEPersistentUtil.DB_ACTION_INSERT);
				}
				insertRequest.getArguments().put("engagements", engagements);
			}
			
			// if titres next missing, prepare for insert
			JSONArray titresPeriodNext = engTitresResponsePeriodNext.getPayload().getJSONArray("titres");
			if(titresPeriodNext == null || titresPeriodNext.length() == 0) {
				// prepare closing titres for insert
				for (int i = 0; i < titres.length(); i++) {
					JSONObject t = titres.getJSONObject(i);
					t.put(AEDomainObject.JSONKey.id.name(), AEPersistentUtil.NEW_ID);
					t.put(AEDomainObject.JSONKey.dbState.name(), AEPersistentUtil.DB_ACTION_INSERT);
				}
				insertRequest.getArguments().put("titres", titres);
			}
			
			// insert engagements/titres
			councilService.saveEngTitres(insertRequest, invContext);
			
			/**
			 * process donateurs
			 */
			
			/**
			 * generate Archiverche report
			 */
			
			/**
			 * generate closing balances
			 */
			AETimePeriod timePeriod = new AETimePeriod(accPeriod.getStartDate(), accPeriod.getEndDate());
			AccAccountBalancesList closingBalances = accJournalDAO.calcClosingBalances(tenantDescr, timePeriod);
			
			// validate rule: accounts 120 and 129 are set to zero for the closing year
			AccAccountBalance b1200 = closingBalances.getBalance("1200");
			if(b1200 != null && !AEMath.isZeroAmount(b1200.getFinalBalance())) {
				throw new AEException("Le bilan est déséquilibré. ");
			}
			
			AccAccountBalance b1290 = closingBalances.getBalance("1290");
			if(b1290 != null && !AEMath.isZeroAmount(b1290.getFinalBalance())) {
				throw new AEException("Le bilan est déséquilibré. ");
			}
			
			// 1200 and 1290 accounts opening balances custom calculation 
			JSONObject budgetRealArguments = new JSONObject()
				.put(AEDomainObject.JSONKey.ownerId.name(), tenantDescr.getID())
				.put(AEDomainObject.JSONKey.sOwnerId.name(), tenantDescr.getID())
				.put("year", AEDateUtil.getYear(accPeriod.getEndDate()));
			AEResponse budgetRealResponse = loadBudgetReal(new AERequest(budgetRealArguments), invContext);
			JSONArray accountsIncome  = budgetRealResponse.getPayload().optJSONArray("income");
			JSONArray accountsExpense = budgetRealResponse.getPayload().optJSONArray("expense");
			double sumExpenses = BudgetRealizationDataSource.getRealAmount(accountsExpense);
			double sumIncomes = BudgetRealizationDataSource.getRealAmount(accountsIncome);
			double profit = sumIncomes - sumExpenses;
			if(AEMath.isPositiveAmount(profit)) {
				// profit (1200) - set credit part with the profit
				AccAccountBalance b = b1200;
				if(b == null) {
					// create balance for 1200 account
					JSONArray accsJson = accDAO.loadAccounts(tenantDescr.getID(), " and (acc.CODE = '1200')");
					if(accsJson.length() > 0) {
						JSONObject acc = accsJson.getJSONObject(0);
						b = new AccAccountBalance();
						b.setAccAccount(new AEDescriptorImp()
							.withCode(acc.optString(AEDomainObject.JSONKey.code.name()))
							.withId(acc.optLong(AEDomainObject.JSONKey.id.name()))
							.withName(acc.optString(AEDomainObject.JSONKey.name.name())));
						closingBalances.add(b);
					}
				}
				if (b != null) {
					b.setOpeningBalance(0.0);
					b.setDebitTurnover(0.0);
					b.setCreditTurnover(Math.abs(profit));
					b.calculateFinalBalance();
				}
			} else if(AEMath.isNegativeAmount(profit)) {
				// loss (1290) - set debit part with the profit
				AccAccountBalance b = b1290;
				if(b == null) {
					// create balance for 1290 account
					JSONArray accsJson = accDAO.loadAccounts(tenantDescr.getID(), " and (acc.CODE = '1290')");
					if(accsJson.length() > 0) {
						JSONObject acc = accsJson.getJSONObject(0);
						b = new AccAccountBalance();
						b.setAccAccount(new AEDescriptorImp()
							.withCode(acc.optString(AEDomainObject.JSONKey.code.name()))
							.withId(acc.optLong(AEDomainObject.JSONKey.id.name()))
							.withName(acc.optString(AEDomainObject.JSONKey.name.name())));
						closingBalances.add(b);
					}
				}
				if (b != null) {
					b.setOpeningBalance(0.0);
					b.setDebitTurnover(Math.abs(profit));
					b.setCreditTurnover(0.0);
					b.calculateFinalBalance();
				}
			}
			
			// validate opening balances for the next year
			double dtSum = 0.0;
			double ctSum = 0.0;
			for (AccAccountBalance ab : closingBalances) {
				double dt = AEMath.doubleValue(ab.getDebitTurnover());
				dtSum += dt;

				double ct =	AEMath.doubleValue(ab.getCreditTurnover());
				ctSum += ct;
			}
			double diff = dtSum - ctSum;
			if(!AEMath.isZeroAmount(diff)) {
				throw new AEException("Le bilan est déséquilibré. Ecart de " + AEMath.toAmountFrenchString(diff) + " .");
			}
			
			// delete opening and not initial balances for the next year
			accountDAO.deleteOpeningBalances(tenantDescr, AEDateUtil.getYear(firstDatePeriodNext));
			
			// save opening balances for the next year
			accountDAO.insertOpeningBalances(closingBalances, AEDateUtil.getYear(firstDatePeriodNext), firstDatePeriodNext, tenantDescr.getID());
			
			/**
			 *  Accounts 1200 and 1290 must be automaticaly zeroed for the next period, based on opening balance:
			 *    - into 1010 account;
			 *    - into OD journal;
			 *    - at the first date of the next period.
			 */
			// delete previous zeroed entries
			accJournalDAO.deleteYearEndClosing(ownerDescr, accPeriodNext.getDescriptor());
			
			// prepare drivers (OD) journal 
			AEDescriptive driversJournal = new AEDescriptorImp()
				.withId(0L)
				.withCode(FinancialTransactionTemplate.PaymentMethod.VARIOUS.getCode());
			
			// ensure 1010 account
			JSONArray accs1010Json = accDAO.loadAccounts(tenantDescr.getID(), " and (acc.CODE = '1010')");
			if(accs1010Json.length() != 1) {
				throw new AEException("Problem with COA: account 1010");
			}
			JSONObject acc1010 = accs1010Json.getJSONObject(0);

			// 
			// zeroing 1200 account
			// 
			JSONArray accs1200Json = accDAO.loadAccounts(tenantDescr.getID(), " and (acc.CODE = '1200')");
			if(accs1200Json.length() != 1) {
				throw new AEException("Problem with COA: account 1200");
			}
			JSONObject acc1200 = accs1200Json.getJSONObject(0);
			
			AccAccountBalance acc1200Balance = new AccAccountBalance();
			acc1200Balance.setAccAccount(AccAccount.lazyDescriptor(acc1200.getLong(AEDomainObject.JSONKey.id.name())));
			acc1200Balance.setPeriod(new AETimePeriod(accPeriodNext.getStartDate(), accPeriodNext.getStartDate()));
			loadAccFinalBalance(acc1200Balance, driversJournal, invContext, localConnection);
			
			if(!AEMath.isZeroAmount(acc1200Balance.getFinalBalance())) {
				AccJournalEntry closingEntry = new AccJournalEntry();
				closingEntry.setAccJournal(driversJournal.getDescriptor());
				closingEntry.setCompany(ownerDescr);
				double amount = Math.abs(acc1200Balance.getFinalBalance());
				
				if(AEMath.isPositiveAmount(acc1200Balance.getFinalBalance())) {
					// dt 1010
					AccJournalItem dtItem = new AccJournalItem();
					dtItem.setCompany(ownerDescr);
					dtItem.setYearEndClosing(true);
					dtItem.setJournal(driversJournal.getDescriptor());
					dtItem.setAccPeriod(accPeriodNext.getDescriptor());
					dtItem.setDate(accPeriodNext.getStartDate());
					dtItem.setAccount(AccAccount.lazyDescriptor(acc1010.getLong(AEDomainObject.JSONKey.id.name())));
					dtItem.setDtAmount(amount);
					dtItem.setDebit(true);
					dtItem.setDescription("Affectation du résultat " + AEDateUtil.getYear(accPeriod.getEndDate()));
					closingEntry.addItem(dtItem);

					// ct 1200
					AccJournalItem ctItem = new AccJournalItem();
					ctItem.setCompany(ownerDescr);
					ctItem.setYearEndClosing(true);
					ctItem.setJournal(driversJournal.getDescriptor());
					ctItem.setAccPeriod(accPeriodNext.getDescriptor());
					ctItem.setDate(accPeriodNext.getStartDate());
					ctItem.setAccount(AccAccount.lazyDescriptor(acc1200.getLong(AEDomainObject.JSONKey.id.name())));
					ctItem.setCtAmount(amount);
					ctItem.setCredit(true);
					ctItem.setDescription("Affectation du résultat " + AEDateUtil.getYear(accPeriod.getEndDate()));
					closingEntry.addItem(ctItem);
				} else {
					// dt 1200
					AccJournalItem dtItem = new AccJournalItem();
					dtItem.setCompany(ownerDescr);
					dtItem.setYearEndClosing(true);
					dtItem.setJournal(driversJournal.getDescriptor());
					dtItem.setAccPeriod(accPeriodNext.getDescriptor());
					dtItem.setDate(accPeriodNext.getStartDate());
					dtItem.setAccount(AccAccount.lazyDescriptor(acc1200.getLong(AEDomainObject.JSONKey.id.name())));
					dtItem.setDtAmount(amount);
					dtItem.setDebit(true);
					dtItem.setDescription("Affectation du résultat " + AEDateUtil.getYear(accPeriod.getEndDate()));
					closingEntry.addItem(dtItem);

					// ct 1010
					AccJournalItem ctItem = new AccJournalItem();
					ctItem.setCompany(ownerDescr);
					ctItem.setYearEndClosing(true);
					ctItem.setJournal(driversJournal.getDescriptor());
					ctItem.setAccPeriod(accPeriodNext.getDescriptor());
					ctItem.setDate(accPeriodNext.getStartDate());
					ctItem.setAccount(AccAccount.lazyDescriptor(acc1010.getLong(AEDomainObject.JSONKey.id.name())));
					ctItem.setCtAmount(amount);
					ctItem.setCredit(true);
					ctItem.setDescription("Affectation du résultat " + AEDateUtil.getYear(accPeriod.getEndDate()));
					closingEntry.addItem(ctItem);
				}
				
				accJournalDAO.insertEntry(closingEntry);
			}

			// 
			// zeroing 1290 account
			// 
			JSONArray accs1290Json = accDAO.loadAccounts(tenantDescr.getID(), " and (acc.CODE = '1290')");
			if(accs1290Json.length() != 1) {
				throw new AEException("Problem with COA: account 1290");
			}
			JSONObject acc1290 = accs1290Json.getJSONObject(0);
			
			AccAccountBalance acc1290Balance = new AccAccountBalance();
			acc1290Balance.setAccAccount(AccAccount.lazyDescriptor(acc1290.getLong(AEDomainObject.JSONKey.id.name())));
			acc1290Balance.setPeriod(new AETimePeriod(accPeriodNext.getStartDate(), accPeriodNext.getStartDate()));
			loadAccFinalBalance(acc1290Balance, driversJournal, invContext, localConnection);
			
			if(!AEMath.isZeroAmount(acc1290Balance.getFinalBalance())) {
				AccJournalEntry closingEntry = new AccJournalEntry();
				closingEntry.setAccJournal(driversJournal.getDescriptor());
				closingEntry.setCompany(ownerDescr);
				double amount = Math.abs(acc1290Balance.getFinalBalance());

				if(AEMath.isPositiveAmount(acc1290Balance.getFinalBalance())) {
					AccJournalItem dtItem = new AccJournalItem();
					dtItem.setCompany(ownerDescr);
					dtItem.setYearEndClosing(true);
					dtItem.setJournal(driversJournal.getDescriptor());
					dtItem.setAccPeriod(accPeriodNext.getDescriptor());
					dtItem.setDate(accPeriodNext.getStartDate());
					dtItem.setAccount(AccAccount.lazyDescriptor(acc1010.getLong(AEDomainObject.JSONKey.id.name())));
					dtItem.setDtAmount(amount);
					dtItem.setDebit(true);
					dtItem.setDescription("Affectation du résultat " + AEDateUtil.getYear(accPeriod.getEndDate()));
					closingEntry.addItem(dtItem);

					// ct 1290
					AccJournalItem ctItem = new AccJournalItem();
					ctItem.setCompany(ownerDescr);
					ctItem.setYearEndClosing(true);
					ctItem.setJournal(driversJournal.getDescriptor());
					ctItem.setAccPeriod(accPeriodNext.getDescriptor());
					ctItem.setDate(accPeriodNext.getStartDate());
					ctItem.setAccount(AccAccount.lazyDescriptor(acc1290.getLong(AEDomainObject.JSONKey.id.name())));
					ctItem.setCtAmount(amount);
					ctItem.setCredit(true);
					ctItem.setDescription("Affectation du résultat " + AEDateUtil.getYear(accPeriod.getEndDate()));
					closingEntry.addItem(ctItem);
				} else {
					// dt 1290
					AccJournalItem dtItem = new AccJournalItem();
					dtItem.setCompany(ownerDescr);
					dtItem.setYearEndClosing(true);
					dtItem.setJournal(driversJournal.getDescriptor());
					dtItem.setAccPeriod(accPeriodNext.getDescriptor());
					dtItem.setDate(accPeriodNext.getStartDate());
					dtItem.setAccount(AccAccount.lazyDescriptor(acc1290.getLong(AEDomainObject.JSONKey.id.name())));
					dtItem.setDtAmount(amount);
					dtItem.setDebit(true);
					dtItem.setDescription("Affectation du résultat " + AEDateUtil.getYear(accPeriod.getEndDate()));
					closingEntry.addItem(dtItem);

					// ct 1010
					AccJournalItem ctItem = new AccJournalItem();
					ctItem.setCompany(ownerDescr);
					ctItem.setYearEndClosing(true);
					ctItem.setJournal(driversJournal.getDescriptor());
					ctItem.setAccPeriod(accPeriodNext.getDescriptor());
					ctItem.setDate(accPeriodNext.getStartDate());
					ctItem.setAccount(AccAccount.lazyDescriptor(acc1010.getLong(AEDomainObject.JSONKey.id.name())));
					ctItem.setCtAmount(amount);
					ctItem.setCredit(true);
					ctItem.setDescription("Affectation du résultat " + AEDateUtil.getYear(accPeriod.getEndDate()));
					closingEntry.addItem(ctItem);
				}
				
				accJournalDAO.insertEntry(closingEntry);
			}
			
			// finally close the period
			accPeriodDAO.closePeriod(accPeriodId);
			accPeriod.setClosed(true);

			// and commit db connection
			localConnection.commit();
			
			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject().put("accPeriod", accPeriod.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			logger.error("closeAccPeriod failed ", e);
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse openAccPeriod(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);
			long accPeriodId = arguments.getLong("accPeriodId");

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
			authorize(new AuthPermission("System/Configuration/Customer/AccPeriod", AuthPermission.SAVE), invContext, ownerDescr);
			
			// It seems that users logged as ordinary ones, are able to unclose one fiscal year.
			// This should be restricted to the administrators.
			if(!ap.isMemberOf(AuthRole.System.administrator)) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));

			AEDescriptor tenantDescr = Organization.lazyDescriptor(ownerId);

			/**
			 * Load Acc period
			 */
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
			AccPeriod accPeriod = accPeriodDAO.loadAccPeriod(accPeriodId);
			
			/**
			 * Validate
			 */
			if(accPeriod == null) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}
			
			if(accPeriod.getCompany() == null || accPeriod.getCompany().getDescriptor().getID() != ownerId) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}
			
			if(!accPeriod.isClosed()) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}
			
			// next period must be opened
			boolean isNextOpened = accPeriodDAO.isNextOpened(tenantDescr, AEApp.ACCOUNTING_MODULE_ID, accPeriod.getEndDate());
			if(!isNextOpened) {
				throw new AEException("Vérifiez que les exercices suivants ne sont pas clos. ");
			}
			
			Date firstDatePeriodNext = AEDateUtil.getClearDate(AEDateUtil.addDaysToDate(accPeriod.getEndDate(), 1));
			AccPeriod accPeriodNext = getAccPeriod(
					ownerId, 
					AEApp.ACCOUNTING_MODULE_ID, 
					firstDatePeriodNext, 
					localConnection);
			
			/**
			 * Open the period
			 */
			localConnection.beginTransaction();
			
			// ensure next period
			if(accPeriodNext == null) {
				accPeriodNext = createAccountingPeriod(tenantDescr, firstDatePeriodNext, localConnection);
			}
			
			// delete opening balance for next period
			AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);
			accountDAO.deleteOpeningBalances(tenantDescr, AEDateUtil.getYear(accPeriodNext.getStartDate()));
			
			// delete year-end closing entries for next period
			AccJournalDAO accJournalDAO = daoFactory.getAccJournalDAO(localConnection);
			accJournalDAO.deleteYearEndClosing(ownerDescr, accPeriodNext.getDescriptor());
			
			// finally open the period
			accPeriodDAO.openPeriod(accPeriodId);
			accPeriod.setClosed(false);

			// and commit db connection
			localConnection.commit();
			
			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject()
				.put("accPeriod", accPeriod.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			logger.error("openAccPeriod failed ", e);
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
}
