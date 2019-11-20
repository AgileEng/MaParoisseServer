/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 15.11.2009 12:00:43
 */
package eu.agileeng.services.cash.ejb;

import static eu.agileeng.accbureau.AEApp.logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;
import org.jboss.logging.Logger;
import org.nfunk.jep.JEP;
import org.nfunk.jep.SymbolTable;

import eu.agileeng.accbureau.AEApp;
import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDescriptorsList;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.acc.AccJournalEntriesList;
import eu.agileeng.domain.acc.AccJournalEntry;
import eu.agileeng.domain.acc.AccJournalEtryAlignmentValidator;
import eu.agileeng.domain.acc.AccJournalItem;
import eu.agileeng.domain.acc.AccJournalItemsList;
import eu.agileeng.domain.acc.AccPeriod;
import eu.agileeng.domain.business.bank.BankAccount;
import eu.agileeng.domain.business.bank.BankAccountBalance;
import eu.agileeng.domain.business.bank.BankAccountBalancesList;
import eu.agileeng.domain.business.bank.BankAccountsList;
import eu.agileeng.domain.business.bank.BankRecognitionRule;
import eu.agileeng.domain.business.bank.BankRecognitionRulesList;
import eu.agileeng.domain.business.bank.BankTransaction;
import eu.agileeng.domain.business.bank.BankTransactionsList;
import eu.agileeng.domain.cash.CFC;
import eu.agileeng.domain.cash.CFCCell;
import eu.agileeng.domain.cash.CFCColumn;
import eu.agileeng.domain.cash.CFCData;
import eu.agileeng.domain.cash.CFCModel;
import eu.agileeng.domain.cash.CFCRow;
import eu.agileeng.domain.cash.CashJETProp;
import eu.agileeng.domain.cash.CashJETransacion;
import eu.agileeng.domain.cash.CashJournalEntriesList;
import eu.agileeng.domain.cash.CashJournalEntry;
import eu.agileeng.domain.cash.CashPeriod;
import eu.agileeng.domain.cash.FinancesUtil;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.document.AEDocument;
import eu.agileeng.domain.document.AEDocumentDescriptor;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.imp.AEDocumentDescriptorImp;
import eu.agileeng.domain.document.trade.AEDocumentItem;
import eu.agileeng.domain.document.trade.AEDocumentItemsList;
import eu.agileeng.domain.document.trade.AETradeDocument;
import eu.agileeng.domain.document.trade.AETradeDocumentResult;
import eu.agileeng.domain.document.trade.AETradeDocumentResultsList;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.domain.inventory.InventoryCalculator;
import eu.agileeng.domain.inventory.InventoryColumn;
import eu.agileeng.domain.inventory.InventoryRow;
import eu.agileeng.domain.inventory.InventoryStatus;
import eu.agileeng.domain.mandat.Mandat;
import eu.agileeng.domain.mandat.MandatExt;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistent;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.acc.AccJournalDAO;
import eu.agileeng.persistent.dao.acc.AccPeriodDAO;
import eu.agileeng.persistent.dao.acc.AccountDAO;
import eu.agileeng.persistent.dao.acc.ChartOfAccountsDAO;
import eu.agileeng.persistent.dao.cash.BankAccountDAO;
import eu.agileeng.persistent.dao.cash.BankJournalDAO;
import eu.agileeng.persistent.dao.cash.CFCDAO;
import eu.agileeng.persistent.dao.cash.CashModuleDAO;
import eu.agileeng.persistent.dao.document.AEDocumentDAO;
import eu.agileeng.persistent.dao.document.trade.AEDocumentItemDAO;
import eu.agileeng.persistent.dao.document.trade.AETradeDocumentDAO;
import eu.agileeng.persistent.dao.file.FileAttachmentDAO;
import eu.agileeng.persistent.dao.ide.IDEDAO;
import eu.agileeng.persistent.dao.inventory.InventoryDAO;
import eu.agileeng.persistent.dao.mandat.MandatDAO;
import eu.agileeng.persistent.dao.oracle.OrganizationDAO;
import eu.agileeng.persistent.dao.pam.PAMDAO;
import eu.agileeng.security.AuthPermission;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.security.AuthRole;
import eu.agileeng.services.AEBean;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AEInvocationContextValidator;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.acc.ejb.AccLocal;
import eu.agileeng.services.bank.ejb.BankLocal;
import eu.agileeng.services.document.ejb.AEDocumentLocal;
import eu.agileeng.services.imp.AEInvocationContextImp;
import eu.agileeng.util.AECollectionUtil;
import eu.agileeng.util.AEDateIterator;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AETimePeriod;
import eu.agileeng.util.AEValue;
import eu.agileeng.util.json.JSONUtil;
import eu.agileeng.util.mail.Emailer;

/**
 *
 */
@Stateless
public class CashBean extends AEBean implements CashLocal {

	@EJB private AccLocal accLocal;
	@EJB private AEDocumentLocal docService;
	@EJB private BankLocal bankService;
	
	private static final Logger logger = Logger.getLogger(CashBean.class);

	private static final long serialVersionUID = 9184424076718418234L;

	@Override
	public AEResponse loadCashDeskTurnOver(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			boolean editable = !(aeRequest.getArguments().optBoolean("history"));

			/**
			 * determine the period
			 */
			Date startDate = null;
			Date endDate = null;
			AccPeriod accPeriod = null;
			if(editable) {
				// not history
				accPeriod = getFirstOpenPeriod(
						aeRequest.getArguments().getLong("ownerId"), 
						AEApp.CASH_MODULE_ID, 
						localConnection);

				startDate = AEDateUtil.getClearDate(accPeriod.getStartDate());	

				// calculate end operational date
				Date nowDate = AEDateUtil.getClearDate(new Date());
				if(nowDate.before(AEDateUtil.getClearDate(accPeriod.getEndDate()))) {
					endDate = nowDate;
				} else {
					endDate = AEDateUtil.getClearDate(accPeriod.getEndDate());
				}
			} else {
				// history
				int month = aeRequest.getArguments().getInt("month");
				int year = aeRequest.getArguments().getInt("year");

				startDate = AEDateUtil.getFirstDate(month - 1, year);		
				endDate = AEDateUtil.getLastDate(month - 1, year);

				accPeriod = daoFactory.getAccPeriodDAO(localConnection).loadAccPeriod(
						startDate, 
						endDate, 
						AEApp.CASH_MODULE_ID,
						aeRequest.getArguments().getLong("ownerId"));
			}

			/**
			 * load entries
			 */
			AEResponse aeResponse = loadCashJournal(aeRequest, startDate, endDate, localConnection);

			/**
			 * load attachment
			 */
			if(accPeriod != null) {
				CashPeriod cashPeriod = 
					daoFactory.getAccPeriodDAO(localConnection).loadCashPeriod(accPeriod.getID());
				if(cashPeriod != null) {
					aeResponse.getPayload().put("cashPeriod", cashPeriod.toJSONObject());
				}
			}
			return aeResponse;
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private JSONObject createAttributesMetaData(JSONArray attributesArray) throws JSONException {
		JSONObject retObj = new JSONObject();

		// metaData
		JSONObject metaData = new JSONObject();
		retObj.put("metaData", metaData);
		metaData.put("totalProperty", "total");
		metaData.put("root", "transactions");
		metaData.put("idProperty", "id");
		JSONArray fieldsArray = new JSONArray();
		metaData.put("fields", fieldsArray);

		// columns static definition
		JSONArray columns = new JSONArray();
		retObj.put("columns", columns);

		// amount is the first column
		JSONObject columnAmount = new JSONObject();
		columnAmount.put("xtype", "numbercolumn");
		columnAmount.put("align", "right");
		columnAmount.put("header", "Montant TTC");
		columnAmount.put("width", 100);
		columnAmount.put("dataIndex", "_value");
		columnAmount.put("_editor", "NumberField");
		columns.put(columnAmount);

		JSONObject columnDate = new JSONObject();
		columnDate.put("header", "Date");
		columnDate.put("hidden", true);
		columnDate.put("width", 30);
		columnDate.put("hideable", false);
		columnDate.put("dataIndex", "date");
		columns.put(columnDate);

		for (int i = 0; i < attributesArray.length(); i++) {
			JSONObject attr = attributesArray.optJSONObject(i);

			JSONObject field = new JSONObject();
			field.put("name", attr.optLong("id"));
			field.put("type", "long");
			fieldsArray.put(field);

			// column
			JSONObject column = new JSONObject();
			column.put("align", "left");
			column.put("header", attr.opt("name"));
			column.put("width", 150);
			column.put("dataIndex", attr.optLong("id"));
			if("date".equalsIgnoreCase(attr.optString("xType"))) {
				column.put("xtype", "datecolumn");
				column.put("format", "d/m/Y");
				column.put("_editor", "DateField");
			} else if("number".equalsIgnoreCase(attr.optString("xType"))) {
				column.put("xtype", "numbercolumn");
				column.put("align", "right");
				column.put("_editor", "NumberField");
			} else if("cashAccount".equalsIgnoreCase(attr.optString("xType"))) {
				column.put("_editor", "CashAccountCombo");
			} else if("client".equalsIgnoreCase(attr.optString("xType"))) {
				column.put("_editor", "ClientsCombo");
			} else if("supplier".equalsIgnoreCase(attr.optString("xType"))) {
				column.put("_editor", "SuppliersCombo");
			} else {
				column.put("_editor", "TextField");
			}

			columns.put(column);
		}

		//// At the end add static attributes
		JSONObject field = new JSONObject();
		field.put("name", "_value");
		field.put("type", "float");
		fieldsArray.put(field);

		JSONObject fieldId = new JSONObject();
		fieldId.put("name", "id");
		fieldId.put("type", "int");
		fieldsArray.put(fieldId);

		JSONObject fieldDBState = new JSONObject();
		fieldDBState.put("name", "dbState");
		fieldDBState.put("type", "int");
		fieldsArray.put(fieldDBState);

		JSONObject fieldDate = new JSONObject();
		fieldDate.put("name", "date");
		fieldDate.put("type", "string");
		fieldsArray.put(fieldDate);

		retObj.put("transactions", new JSONArray());

		return retObj;
	}

	@Override
	public AEResponse saveCashJournalEntry(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			AEResponse response = new AEResponse();
			JSONObject arguments = aeRequest.getArguments();
			DAOFactory daoFactory = DAOFactory.getInstance();

			JSONObject entry = arguments.optJSONObject("entry");
			CashJournalEntry cjEntry = new CashJournalEntry();
			cjEntry.create(entry);

			// get connection and begin transaction
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();
			CashModuleDAO cashDAO = daoFactory.getCashModuleDAO(localConnection);
			CashJournalEntry cjEntryDB = 
				cashDAO.loadJournalEntryForUpdate(
						cjEntry.getGoaId(), 
						cjEntry.getEntryDate(),
						cjEntry.getOwnerId());
			if(cjEntryDB != null) {
				// keep accounted amount
				cjEntry.setAccountedAmount(cjEntryDB.getAccountedAmount());

				// set to be accounted
				cjEntry.setAccounted(false);

				// delete entry (all related will be cascade deleted)
				cashDAO.deleteCashJournalEntryByID(cjEntryDB.getID());
			}

			// insert the entry
			cashDAO.insert(cjEntry);

			// commit
			localConnection.commit();

			// return
			JSONObject payload = new JSONObject();
			payload.put("entry", "{}");
			response.setPayload(payload);
			return response;
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
	public AEResponse closingPeriod(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * prepare dao's
			 */
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);

			/**
			 * determine period
			 */
			AccPeriod accPeriod = getFirstOpenPeriod(
					aeRequest.getArguments().getLong("ownerId"), 
					AEApp.CASH_MODULE_ID, 
					localConnection);

			Date startDate = AEDateUtil.getClearDate(accPeriod.getStartDate());	
			Date endDate = AEDateUtil.getClearDate(accPeriod.getEndDate());

			// we can close only finished periods
			//			Date nowDate = AEDateUtil.getClearDate(new Date());
			//			if(nowDate.before(endDate) || nowDate.equals(endDate)) {
			//				throw new AEException(
			//						(int) AEError.System.CANNOT_CLOSE_UNFINISHED.getSystemID(), 
			//				"La période n'est pas terminée.");
			//			}

			// owner
			OrganizationDAO orgDAO = DAOFactory.getInstance().getOrganizationDAO(localConnection);
			AEDescriptor customerDescr = Organization.lazyDescriptor(aeRequest.getArguments().getLong("ownerId"));
			JSONArray customersArray = orgDAO.loadCustomer(customerDescr);
			JSONObject customer = customersArray.getJSONObject(0);
			customerDescr.setName(customer.optString("name"));

			// default accounts
			AccountDAO accDAO = daoFactory.getAccountDAO(localConnection);
			JSONArray accJSONArray = accDAO.loadSubAccountsByOwner(aeRequest.getArguments().getLong("ownerId"));

			JSONObject cashAccount = null;

			JSONObject vatAccount = null;

			JSONObject vatAccount1 = null;

			JSONObject vatAccount2 = null;

			JSONObject vatAccount3 = null;

			JSONObject diversAccount = null;
			double diversAccountVatRate = 0.0;

			JSONObject adjPositiveAccount = null;

			JSONObject adjNegativeAccount = null;

			for (int i = 0; i < accJSONArray.length(); i++) {
				JSONObject accJSON = accJSONArray.getJSONObject(i);

				// don't process account if the code property is empty
				String accCodeRaw = accJSON.optString("code");
				if(AEStringUtil.isEmpty(accCodeRaw)) {
					continue;
				}
				String accCode = accCodeRaw.trim();
				if(AEStringUtil.isEmpty(accCode)) {
					continue;
				}

				// don't use else if because one acc can be multiple
				// choosen as default account
				if(accCode.equalsIgnoreCase(customer.optString("defaultCashAccCode").trim())) {

					// this is cash account
					cashAccount = accJSON;
				} 

				if(accCode.equalsIgnoreCase(customer.optString("defaultTVAOutAccCode").trim())) {

					// this is TVA account
					vatAccount = accJSON;
				} 

				if(accCode.equalsIgnoreCase(customer.optString("tvaOut1AccCode").trim())) {

					// this is TVA account
					vatAccount1 = accJSON;
				} 

				if(accCode.equalsIgnoreCase(customer.optString("tvaOut2AccCode").trim())) {

					// this is TVA account
					vatAccount2 = accJSON;
				} 

				if(accCode.equalsIgnoreCase(customer.optString("tvaOut3AccCode").trim())) {

					// this is TVA account
					vatAccount3 = accJSON;
				} 

				if(accCode.equalsIgnoreCase(customer.optString("defaultCashAdjAccCode").trim())) {

					// this is diff positive account
					adjPositiveAccount = accJSON;
				} 

				if(accCode.equalsIgnoreCase(customer.optString("defaultCashAdjNegAccCode").trim())) {

					// this is diff positive account
					adjNegativeAccount = accJSON;
				}

				if(accCode.equalsIgnoreCase(customer.optString(Organization.JSONKey.defaultDiversAccCode).trim())) {

					// this is divers account
					diversAccount = accJSON;

					if(diversAccount.has("vat")) {
						JSONObject vatJSON = diversAccount.optJSONObject("vat");
						if(vatJSON != null && vatJSON.has("rate")) { 
							diversAccountVatRate = AEMath.doubleValue(JSONUtil.parseDoubleStrict(vatJSON, "rate"));
						}

					}		
				}
			}

			if(cashAccount == null || adjPositiveAccount == null || adjNegativeAccount == null) {
				throw new AEException("S'il vous plaît vérifier l'installation par défaut du compte.");
			}

			/**
			 * Create Journal Entries
			 */
			// DAOs
			CashModuleDAO cashModuleDAO = daoFactory.getCashModuleDAO(localConnection);

			// entries list to be created
			AccJournalEntriesList assetsJournalEntries = new AccJournalEntriesList();
			AccJournalEntriesList expensesJournalEntries = new AccJournalEntriesList();

			// individual entry, must be added to entries list before processing
			AccJournalEntry assetsJournalEntry = new AccJournalEntry();
			AccJournalEntry expensesJournalEntry = new AccJournalEntry();

			// updated cash items
			CashJournalEntriesList forAccountingList = null;

			AEDescriptive ownerDescr = 
				Organization.lazyDescriptor(aeRequest.getArguments().optLong("ownerId"));
			aeRequest.getArguments().put("startDate", startDate);
			aeRequest.getArguments().put("endDate", endDate);
			AEResponse cashDeskTurnOver = loadCashJournal(
					aeRequest, 
					startDate, 
					endDate, 
					localConnection);
			JSONObject cashJournalEntries = cashDeskTurnOver.getPayload();

			AEDescriptorImp journalDescr = new AEDescriptorImp();
			journalDescr.setCode("CAI");
			JSONArray accountsArray = cashJournalEntries.getJSONArray("records");
			int cashAccMode = customer.optInt("cashAccMode");
			switch (cashAccMode) {
			case FinancesUtil.CASH_ACC_MODE_DAILY: {
				// create difference item
				// create assets monthly items 
				// create expenses daily not accounted entries (one entry for day) these entries are balanced
				// balance individual entries
				for (int i = 0; i < accountsArray.length(); i++) {
					JSONObject accJSON = accountsArray.getJSONObject(i);
					JSONObject vatAccCurr = null;
					if(accJSON.optInt("sysId") == 90) {
						// difference item
						double accumulatedDifference = accJSON.getDouble("TTC");
						if(accumulatedDifference > 0) {
							// positive difference
							AccJournalItem diffItem = new AccJournalItem(accPeriod);
							diffItem.setCompany(ownerDescr);
							diffItem.setJournal(journalDescr);
							diffItem.setDate(endDate);
							diffItem.setAccount(new AEDescriptorImp(adjPositiveAccount.getLong("id")));
							diffItem.setDescription(accJSON.optString("name"));
							diffItem.setCtAmount(new Double(accumulatedDifference));

							assetsJournalEntry.addItem(diffItem);
						} else if(accumulatedDifference < 0) {
							// negative difference
							AccJournalItem diffItem = new AccJournalItem(accPeriod);
							diffItem.setCompany(ownerDescr);
							diffItem.setJournal(journalDescr);
							diffItem.setDate(endDate);
							diffItem.setAccount(new AEDescriptorImp(adjNegativeAccount.getLong("id")));
							diffItem.setDescription(accJSON.optString("name"));
							diffItem.setDtAmount(new Double(Math.abs(accumulatedDifference)));

							expensesJournalEntry.addItem(diffItem);
						}
					} else if(accJSON.optInt("sysId") == 10) {
						// asset

						vatAccCurr = getVATAccount(
								customer, 
								vatAccount, 
								vatAccount1, 
								vatAccount2,
								vatAccount3,
								accJSON);

						AccJournalItemsList accJournalItemsList = createCashAssetsAccJournalItems(
								diversAccount, 
								ownerDescr.getDescriptor(), 
								startDate, 
								endDate, 
								diversAccountVatRate, 
								accPeriod, 
								accJSON, 
								journalDescr, 
								vatAccCurr, 
								cashModuleDAO);

						assetsJournalEntry.addItems(accJournalItemsList);
					}
				}

				// create expenses daily not accounted items
				aeRequest.getArguments().put("assetsJournalEntries", (Object) assetsJournalEntries);
				aeRequest.getArguments().put("expensesJournalEntries", (Object) expensesJournalEntries);
				aeRequest.getArguments().put("journalDescr", journalDescr);
				aeRequest.getArguments().put("accJSONArray", accJSONArray);
				aeRequest.getArguments().put("accPeriod", accPeriod);
				aeRequest.getArguments().put("setExported", "true");
				aeRequest.getArguments().put("customerJSON", customer);

				AEResponse forAccountingResponse = createCashJournalItems(aeRequest, localConnection);
				if(forAccountingResponse.getPayload().has("forAccountingList")) {
					forAccountingList = (CashJournalEntriesList) forAccountingResponse.getPayload().opt("forAccountingList");
					for (CashJournalEntry cashJournalEntry : forAccountingList) {
						cashJournalEntry.updateToAccounted();
					}
				}

				// create balance cash account items for individual entries
				Double cashCtAmount = expensesJournalEntry.calcDtAmount();
				if(Math.abs(AEMath.doubleValue(cashCtAmount)) >= 0.005) {
					AccJournalItem expensesCashItem = new AccJournalItem(accPeriod);
					expensesCashItem.setCompany(customerDescr);
					expensesCashItem.setJournal(journalDescr);
					expensesCashItem.setDate(endDate);
					expensesCashItem.setAccount(new AEDescriptorImp(cashAccount.getLong("id")));
					expensesCashItem.setDescription(cashAccount.optString("name"));
					expensesCashItem.setCtAmount(cashCtAmount);
					expensesJournalEntry.addItem(expensesCashItem);
				}

				Double cashDtAmount = assetsJournalEntry.calcCtAmount();
				if(Math.abs(AEMath.doubleValue(cashDtAmount)) >= 0.005) {
					AccJournalItem assetsCashItem = new AccJournalItem(accPeriod);
					assetsCashItem.setCompany(customerDescr);
					assetsCashItem.setJournal(journalDescr);
					assetsCashItem.setDate(endDate);
					assetsCashItem.setAccount(new AEDescriptorImp(cashAccount.getLong("id")));
					assetsCashItem.setDescription(cashAccount.optString("name"));
					assetsCashItem.setDtAmount(cashDtAmount);
					assetsJournalEntry.addItem(assetsCashItem);
				}
			};
			break;
			case FinancesUtil.CASH_ACC_MODE_MONTHLY:
			default: {
				for (int i = 0; i < accountsArray.length(); i++) {
					JSONObject accJSON = accountsArray.getJSONObject(i);
					JSONObject vatAccCurr = null;
					switch(accJSON.optInt("sysId")) {
					case 10: {
						// asset
						vatAccCurr = getVATAccount(
								customer, 
								vatAccount, 
								vatAccount1, 
								vatAccount2,
								vatAccount3,
								accJSON);

						AccJournalItemsList accJournalItemsList = createCashAssetsAccJournalItems(
								diversAccount, 
								ownerDescr.getDescriptor(), 
								startDate, 
								endDate, 
								diversAccountVatRate, 
								accPeriod, 
								accJSON, 
								journalDescr, 
								vatAccCurr, 
								cashModuleDAO);

						assetsJournalEntry.addItems(accJournalItemsList);

						break;
					} case 20: {
						// expenses
						// create item for this expense
						AccJournalItem accItem = new AccJournalItem(accPeriod);

						accItem.setCompany(ownerDescr);
						accItem.setJournal(journalDescr);
						accItem.setDate(endDate);
						accItem.setAccount(new AEDescriptorImp(accJSON.getLong("accId")));
						accItem.setDescription(accJSON.optString("name"));
						accItem.setDtAmount(new Double(accJSON.getDouble("TTC")));

						expensesJournalEntry.addItem(accItem);
						break;
					} case 90 : {
						AccJournalItem diffItem = new AccJournalItem(accPeriod);
						double accumulatedDifference = accJSON.getDouble("TTC");
						if(accumulatedDifference > 0) {
							// positive difference

							diffItem.setCompany(ownerDescr);
							diffItem.setJournal(journalDescr);
							diffItem.setDate(endDate);
							diffItem.setAccount(new AEDescriptorImp(adjPositiveAccount.getLong("id")));
							diffItem.setDescription(accJSON.optString("name"));
							diffItem.setCtAmount(new Double(accumulatedDifference));

							assetsJournalEntry.addItem(diffItem);
						} else if(accumulatedDifference < 0) {
							// negative difference

							diffItem.setCompany(ownerDescr);
							diffItem.setJournal(journalDescr);
							diffItem.setDate(endDate);
							diffItem.setAccount(new AEDescriptorImp(adjNegativeAccount.getLong("id")));
							diffItem.setDescription(accJSON.optString("name"));
							diffItem.setDtAmount(new Double(Math.abs(accumulatedDifference)));

							expensesJournalEntry.addItem(diffItem);
						} else {
							// zero difference
							// nothing to do
						}
						break;
					}
					}
				}

				// create cash account items
				AccJournalItem cashDtItem = new AccJournalItem(accPeriod);
				Double dtAmount = expensesJournalEntry.calcDtAmount();
				cashDtItem.setCompany(ownerDescr);
				cashDtItem.setJournal(journalDescr);
				cashDtItem.setDate(endDate);
				cashDtItem.setAccount(new AEDescriptorImp(cashAccount.getLong("id")));
				cashDtItem.setDescription(cashAccount.optString("name"));
				cashDtItem.setDtAmount(dtAmount);
				assetsJournalEntry.addItem(cashDtItem);

				AccJournalItem cashCtItem = new AccJournalItem(accPeriod);
				Double ctAmount = assetsJournalEntry.calcCtAmount();
				cashCtItem.setCompany(ownerDescr);
				cashCtItem.setJournal(journalDescr);
				cashCtItem.setDate(endDate);
				cashCtItem.setAccount(new AEDescriptorImp(cashAccount.getLong("id")));
				cashCtItem.setDescription(cashAccount.optString("name"));
				cashCtItem.setCtAmount(ctAmount);
				expensesJournalEntry.addItem(cashCtItem);
			};
			break;
			}

			// IMPORTANT: add individual entries to the global entries list
			if(!assetsJournalEntry.isEmpty()) {
				assetsJournalEntries.add(assetsJournalEntry);
			}
			if(!expensesJournalEntry.isEmpty()) {
				expensesJournalEntries.add(expensesJournalEntry);
			}

			/**
			 * Update DB
			 */
			localConnection.beginTransaction();

			// insert Cash Journal Entries
			AccJournalDAO journalDAO = daoFactory.getAccJournalDAO(localConnection);
			for (AccJournalEntry entry : assetsJournalEntries) {
				journalDAO.insertEntry(entry);
			}
			for (AccJournalEntry entry : expensesJournalEntries) {
				journalDAO.insertEntry(entry);
			}

			// update accounted
			if(forAccountingList != null) {
				cashModuleDAO.update(forAccountingList);
			}

			// close cash period
			ChartOfAccountsDAO goaDAO = daoFactory.getChartOfAccountsDAO(localConnection);
			// assets goa
			goaDAO.closeGOA(ownerDescr.getDescriptor().getID(), 2, endDate);
			// expenses goa
			goaDAO.closeGOA(ownerDescr.getDescriptor().getID(), 3, endDate);
			// close acc period
			accPeriodDAO.closePeriod(accPeriod.getID());

			localConnection.commit();

			/**
			 * Process after close.
			 * Must be after transaction close.
			 * If after close fails, this should not affect the whole close task.
			 */

			// send e-mail
			try {
				Emailer emailer = new Emailer();
				emailer.onPeriodClosed(customerDescr, invContext, accPeriod, AEApp.CASH_MODULE_ID);
			} catch (Throwable t) {
				t.printStackTrace();
			}

			// auto export
			try {
				JSONObject exportRequestArguments = new JSONObject();

				// where to export
				exportRequestArguments.put("accPeriodId", accPeriod.getID());
				exportRequestArguments.put("setExported", true);
				if("secal.png".equalsIgnoreCase(invContext.getAuthPrincipal().getDescription())) {
					exportRequestArguments.put("exportTo", "CEGID");
				} else if("gianati.png".equalsIgnoreCase(invContext.getAuthPrincipal().getDescription())) {
					exportRequestArguments.put("exportTo", "SAGE");
				}

				exportRequestArguments.put("ownerId", aeRequest.getArguments().getLong("ownerId"));

				// what to export
				AEDescriptorsList journalEntriesList = new AEDescriptorsList();
				for (AccJournalEntry entry : assetsJournalEntries) {
					journalEntriesList.add(entry.getDescriptor());
				}
				for (AccJournalEntry entry : expensesJournalEntries) {
					journalEntriesList.add(entry.getDescriptor());
				}
				exportRequestArguments.put("journalEntriesList", (Object) journalEntriesList);


				AERequest exportRequest = new AERequest(exportRequestArguments);
				if(cashAccMode == FinancesUtil.CASH_ACC_MODE_MONTHLY) {
					accLocal.export(exportRequest, invContext);
				} else if(cashAccMode == FinancesUtil.CASH_ACC_MODE_DAILY) {
					accLocal.exportDaily(exportRequest, invContext);
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}

			/**
			 * return response
			 */
			//			AccJournalItemsList journalItems = accLocal.loadGeneralJournal(
			//					ownerDescr.getDescriptor().getID(),
			//					"CAI",
			//					endDate,
			//					localConnection);
			JSONObject payload = new JSONObject();
			//			payload.put("gjEntries", journalItems.toJSONArray());
			return new AEResponse(payload);
		} catch (Throwable t) {
			if(localConnection != null) {
				localConnection.rollback();
			}
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private AccJournalItemsList createCashExpensesAccJournalItems(
			JSONObject diversAccount,
			AEDescriptor customerDescr,
			CashJournalEntry cashJournalEntry,
			Date accDate,
			AccPeriod accPeriod,
			JSONObject accJSON,
			AEDescriptor journalDescr,
			CashModuleDAO cashModuleDAO
	) throws JSONException, AEException {

		AccJournalItemsList accJournalItemsList = new AccJournalItemsList();

		// if specified cashJournalEntry is a divers initial (first time) accounting (not correction)
		// then account transaction details
		boolean diversAccCahJournalEntryFirstTimeAccounting = false;
		List<CashJETransacion> cashJTTransactions = null;
		if(Math.abs(AEMath.doubleValue(cashJournalEntry.getAccountedAmount())) >= 0.005  
				&& diversAccount != null && diversAccount.optLong("id") == cashJournalEntry.getAccId()) {
			// first time accounting for divers account entry
			cashJTTransactions = 
				cashModuleDAO.loadCJETransactions(cashJournalEntry.getDescriptor());
			if(cashJTTransactions != null && !cashJTTransactions.isEmpty()) {
				diversAccCahJournalEntryFirstTimeAccounting = true;
			}
		}
		if(diversAccCahJournalEntryFirstTimeAccounting) {
			// create detail items (item for every transaction)
			for (CashJETransacion cashJETransacion : cashJTTransactions) {
				// create item for every transaction expense
				AccJournalItem accItem = new AccJournalItem(accPeriod);

				accItem.setCompany(customerDescr);
				accItem.setJournal(journalDescr);
				accItem.setDate(accDate);
				accItem.setAccount(AccAccount.lazyDescriptor(cashJournalEntry.getAccId()));
				accItem.setDescription(accJSON.optString("name"));
				accItem.setDtAmount(cashJETransacion.getAmount());

				accJournalItemsList.add(accItem);
			}
		} else {
			// create item for this expense
			AccJournalItem accItem = new AccJournalItem(accPeriod);

			accItem.setCompany(customerDescr);
			accItem.setJournal(journalDescr);
			accItem.setDate(accDate);
			accItem.setAccount(AccAccount.lazyDescriptor(cashJournalEntry.getAccId()));
			accItem.setDescription(accJSON.optString("name"));
			accItem.setDtAmount(cashJournalEntry.getAmountAccounting());

			accJournalItemsList.add(accItem);
		}

		return accJournalItemsList;
	}

	private AccJournalItemsList createCashAssetsAccJournalItems(
			JSONObject diversAccount,
			AEDescriptor ownerDescr,
			Date startDate,
			Date endDate,
			double diversAccountVatRate,
			AccPeriod accPeriod,
			JSONObject accJSON,
			AEDescriptor journalDescr,
			JSONObject vatAcc,
			CashModuleDAO cashModuleDAO
	) throws JSONException, AEException {

		AccJournalItemsList accJournalItemsList = new AccJournalItemsList(); 

		// check for divers account
		if(diversAccount != null && diversAccount.getLong("id") == accJSON.getLong("accId")) {
			// create detailed items for this asset
			CashJournalEntriesList assetDiversEntriesList = 
				cashModuleDAO.loadAssetDiversTransactions(
						ownerDescr.getDescriptor().getID(), 
						diversAccount.getLong("id"), 
						startDate, 
						endDate); 

			for (CashJournalEntry cashJournalEntry : assetDiversEntriesList) {
				List<CashJETransacion> transList =  cashJournalEntry.getJeTransactions();
				if(transList != null && !transList.isEmpty()) {
					// create item for every transaction
					for (CashJETransacion cashJETransacion : transList) {
						AccJournalItem accItem = new AccJournalItem(accPeriod);		

						double amount = cashJETransacion.getAmount();
						double vatAmount = 0.0;
						double taxableAmount = amount;

						if(Math.abs(diversAccountVatRate) >= 0.0001) {
							taxableAmount = amount * 100 / (100 + diversAccountVatRate);
							taxableAmount = AEMath.round(taxableAmount, 2);
							vatAmount = AEMath.round(amount - taxableAmount, 2);
						}

						if(Math.abs(taxableAmount) >= 0.0001) {
							accItem.setCompany(ownerDescr);
							accItem.setJournal(journalDescr);
							accItem.setDate(cashJournalEntry.getDate());
							accItem.setAccount(new AEDescriptorImp(accJSON.getLong("accId")));
							accItem.setDescription(accJSON.optString("name"));
							accItem.setCtAmount(taxableAmount);

							accJournalItemsList.add(accItem);
						}

						// create item for VAT
						if(Math.abs(vatAmount) >= 0.0001) {
							if(vatAcc == null) {
								throw new AEException(
								"S'il vous plaît vérifier l'installation par défaut du compte (TVA).");
							}

							AccJournalItem vatItem = new AccJournalItem(accPeriod);

							vatItem.setCompany(ownerDescr);
							vatItem.setJournal(journalDescr);
							vatItem.setDate(cashJournalEntry.getDate());
							vatItem.setAccount(new AEDescriptorImp(vatAcc.getLong("id")));
							vatItem.setDescription(accJSON.optString("name"));
							vatItem.setCtAmount(vatAmount);

							accJournalItemsList.add(vatItem);
						}
					}
				} else {
					// create item for this entry
					AccJournalItem accItem = new AccJournalItem(accPeriod);		

					double amount = AEMath.doubleValue(cashJournalEntry.getAmount());
					double vatAmount = 0.0;
					double taxableAmount = amount;

					if(Math.abs(diversAccountVatRate) >= 0.0001) {
						taxableAmount = amount * 100 / (100 + diversAccountVatRate);
						taxableAmount = AEMath.round(taxableAmount, 2);
						vatAmount = AEMath.round(amount - taxableAmount, 2);
					}

					if(Math.abs(taxableAmount) >= 0.0001) {
						accItem.setCompany(ownerDescr);
						accItem.setJournal(journalDescr);
						accItem.setDate(cashJournalEntry.getDate());
						accItem.setAccount(new AEDescriptorImp(accJSON.getLong("accId")));
						accItem.setDescription(accJSON.optString("name"));
						accItem.setCtAmount(taxableAmount);

						accJournalItemsList.add(accItem);
					}

					// create item for VAT
					if(Math.abs(vatAmount) >= 0.0001) {
						if(vatAcc == null) {
							throw new AEException("S'il vous plaît vérifier l'installation par défaut du compte (TVA).");
						}

						AccJournalItem vatItem = new AccJournalItem(accPeriod);

						vatItem.setCompany(ownerDescr);
						vatItem.setJournal(journalDescr);
						vatItem.setDate(cashJournalEntry.getDate());
						vatItem.setAccount(new AEDescriptorImp(vatAcc.getLong("id")));
						vatItem.setDescription(accJSON.optString("name"));
						vatItem.setCtAmount(vatAmount);

						accJournalItemsList.add(vatItem);
					}
				}
			}
		} else {
			// create summary item for this asset
			AccJournalItem accItem = new AccJournalItem(accPeriod);
			double htAmount = accJSON.getDouble("HT");
			if(Math.abs(htAmount) >= 0.0001) {
				accItem.setCompany(ownerDescr);
				accItem.setJournal(journalDescr);
				accItem.setDate(endDate);
				accItem.setAccount(new AEDescriptorImp(accJSON.getLong("accId")));
				accItem.setDescription(accJSON.optString("name"));
				accItem.setCtAmount(new Double(accJSON.getDouble("HT")));

				accJournalItemsList.add(accItem);
			}

			// create item for VAT
			double vatAmount = accJSON.getDouble("TVA");
			if(Math.abs(vatAmount) >= 0.0001) {
				if(vatAcc == null) {
					throw new AEException("S'il vous plaît vérifier l'installation par défaut du compte (TVA).");
				}

				AccJournalItem vatItem = new AccJournalItem(accPeriod);

				vatItem.setCompany(ownerDescr);
				vatItem.setJournal(journalDescr);
				vatItem.setDate(endDate);
				vatItem.setAccount(new AEDescriptorImp(vatAcc.getLong("id")));
				vatItem.setDescription(accJSON.optString("name"));
				vatItem.setCtAmount(accJSON.getDouble("TVA"));

				accJournalItemsList.add(vatItem);
			}
		}

		return accJournalItemsList;
	}

	private AEResponse loadCashJournal(AERequest aeRequest, Date startDate, Date endDate, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();

			long ownerId = aeRequest.getArguments().optLong("ownerId");
			boolean editable = !(aeRequest.getArguments().optBoolean("history"));

			// create and return response
			JSONObject payload = new JSONObject();

			// metaData static definition
			JSONObject metaData = new JSONObject();
			payload.put("metaData", metaData);
			metaData.put("totalProperty", "total");
			metaData.put("root", "records");
			metaData.put("idProperty", "id");
			JSONArray fieldsArray = new JSONArray();
			metaData.put("fields", fieldsArray);
			JSONObject field1 = new JSONObject();
			field1.put("name", "groupId");
			field1.put("type", "int");
			fieldsArray.put(field1);

			JSONObject field2 = new JSONObject();
			field2.put("name", "group");
			field2.put("type", "string");
			fieldsArray.put(field2);

			JSONObject field3 = new JSONObject();
			field3.put("name", "id");
			field3.put("type", "int");
			fieldsArray.put(field3);

			JSONObject field4 = new JSONObject();
			field4.put("name", "sysId");
			field4.put("type", "long");
			fieldsArray.put(field4);

			JSONObject field5 = new JSONObject();
			field5.put("name", "code");
			field5.put("type", "string");
			fieldsArray.put(field5);

			JSONObject fieldSIndex = new JSONObject();
			fieldSIndex.put("name", "sIndex");
			fieldSIndex.put("type", "int");
			fieldsArray.put(fieldSIndex);

			JSONObject field6 = new JSONObject();
			field6.put("name", "description");
			field6.put("type", "string");
			fieldsArray.put(field6);

			JSONObject field7 = new JSONObject();
			field7.put("name", "accumulation");
			field7.put("type", "float");
			fieldsArray.put(field7);

			JSONObject field8 = new JSONObject();
			field8.put("name", "attributes");
			fieldsArray.put(field8);

			JSONObject field9 = new JSONObject();
			field9.put("name", "attrMetaData");
			fieldsArray.put(field9);

			JSONObject field10 = new JSONObject();
			field10.put("name", "coaId");
			field10.put("type", "long");
			fieldsArray.put(field10);		

			JSONObject field11 = new JSONObject();
			field11.put("name", "accId");
			field11.put("type", "long");
			fieldsArray.put(field11);		

			JSONObject field12 = new JSONObject();
			field12.put("name", "ownerId");
			field12.put("type", "long");
			fieldsArray.put(field12);		

			JSONObject field13 = new JSONObject();
			field13.put("name", "HT");
			field13.put("type", "float");
			fieldsArray.put(field13);		

			JSONObject field14 = new JSONObject();
			field14.put("name", "TVA");
			field14.put("type", "float");
			fieldsArray.put(field14);

			JSONObject field15 = new JSONObject();
			field15.put("name", "TTC");
			field15.put("type", "float");
			fieldsArray.put(field15);		

			JSONObject field16 = new JSONObject();
			field16.put("name", "vat");
			fieldsArray.put(field16);	

			// columns static definition
			JSONArray columns = new JSONArray();
			payload.put("columns", columns);		
			JSONObject column1 = new JSONObject();
			column1.put("header", "Compte");
			column1.put("width", 100);
			column1.put("sortable", true);
			column1.put("dataIndex", "code");
			column1.put("hideable", true);
			column1.put("hidden", true);
			column1.put("locked", true);
			columns.put(column1);

			JSONObject column2 = new JSONObject();
			column2.put("header", "group");
			column2.put("hidden", true);
			column2.put("width", 30);
			column1.put("hideable", false);
			column2.put("dataIndex", "group");
			column2.put("locked", true);
			columns.put(column2);

			JSONObject column3 = new JSONObject();
			column3.put("header", "Nom");
			column3.put("width", 180);
			column1.put("hideable", false);
			column3.put("dataIndex", "description");
			column3.put("locked", true);
			columns.put(column3);

			// records static definition
			JSONArray records = new JSONArray();
			payload.put("records", records);

			/**
			 * Setup system cash accounts
			 */
			JSONObject accountTotalEntrees = new JSONObject();
			accountTotalEntrees.put("groupId", 400);
			accountTotalEntrees.put("group", "(3) Soldes");
			accountTotalEntrees.put("id", -50);
			accountTotalEntrees.put("code", "");
			accountTotalEntrees.put("name", "TOTAL ENTREES");
			accountTotalEntrees.put("description", "TOTAL ENTREES");
			accountTotalEntrees.put("HT", 0.0);
			accountTotalEntrees.put("TVA", 0.0);
			accountTotalEntrees.put("TTC", 0.0);
			accountTotalEntrees.put("sysId", 50);
			accountTotalEntrees.put("ownerId", ownerId);
			accountTotalEntrees.put("accumulation", 0.0);
			records.put(accountTotalEntrees);

			JSONObject accountTotalSorties = new JSONObject();
			accountTotalSorties.put("groupId", 400);
			accountTotalSorties.put("group", "(3) Soldes");
			accountTotalSorties.put("id", -60);
			accountTotalSorties.put("code", "");
			accountTotalSorties.put("name", "TOTAL SORTIES");
			accountTotalSorties.put("description", "TOTAL SORTIES");
			accountTotalSorties.put("HT", 0.0);
			accountTotalSorties.put("TVA", 0.0);
			accountTotalSorties.put("TTC", 0.0);
			accountTotalSorties.put("sysId", 60);
			accountTotalSorties.put("ownerId", ownerId);
			accountTotalSorties.put("accumulation", 0.0);
			records.put(accountTotalSorties);

			JSONObject accountSoldePrecedent = new JSONObject();
			accountSoldePrecedent.put("groupId", 400);
			accountSoldePrecedent.put("group", "(3) Soldes");
			accountSoldePrecedent.put("id", -40);
			accountSoldePrecedent.put("code", "");
			accountSoldePrecedent.put("name", "SOLDE PRECEDENT");
			accountSoldePrecedent.put("description", "SOLDE PRECEDENT");
			accountSoldePrecedent.put("HT", 0.0);
			accountSoldePrecedent.put("TVA", 0.0);
			accountSoldePrecedent.put("TTC", 0.0);
			accountSoldePrecedent.put("sysId", 40);
			accountSoldePrecedent.put("ownerId", ownerId);
			accountSoldePrecedent.put("accumulation", 0.0);
			records.put(accountSoldePrecedent);

			JSONObject accountSoldeTheorique = new JSONObject();
			accountSoldeTheorique.put("groupId", 400);
			accountSoldeTheorique.put("group", "(3) Soldes");
			accountSoldeTheorique.put("id", -70);
			accountSoldeTheorique.put("code", "");
			accountSoldeTheorique.put("name", "SOLDE THEORIQUE");
			accountSoldeTheorique.put("description", "SOLDE THEORIQUE");
			accountSoldeTheorique.put("HT", 0.0);
			accountSoldeTheorique.put("TVA", 0.0);
			accountSoldeTheorique.put("TTC", 0.0);
			accountSoldeTheorique.put("sysId", 70);
			accountSoldeTheorique.put("ownerId", ownerId);
			accountSoldeTheorique.put("accumulation", 0.0);
			records.put(accountSoldeTheorique);

			JSONObject accountReel = new JSONObject();
			accountReel.put("groupId", 400);
			accountReel.put("group", "(3) Soldes");
			accountReel.put("id", -30);
			accountReel.put("code", "");
			accountReel.put("name", "SOLDE REEL");
			accountReel.put("description", "SOLDE REEL");
			accountReel.put("HT", 0.0);
			accountReel.put("TVA", 0.0);
			accountReel.put("TTC", 0.0);
			accountReel.put("sysId", 30);
			accountReel.put("ownerId", ownerId);
			accountReel.put("accumulation", 0.0);
			records.put(accountReel);

			JSONObject accountDifference = new JSONObject();
			accountDifference.put("groupId", 400);
			accountDifference.put("group", "(3) Soldes");
			accountDifference.put("id", -90);
			accountDifference.put("code", "");
			accountDifference.put("name", "Ecart à justifier".toUpperCase());
			accountDifference.put("description", "Ecart à justifier".toUpperCase());
			accountDifference.put("HT", 0.0);
			accountDifference.put("TVA", 0.0);
			accountDifference.put("TTC", 0.0);
			accountDifference.put("sysId", 90);
			accountDifference.put("ownerId", ownerId);
			accountDifference.put("accumulation", 0.0);
			records.put(accountDifference);

			/**
			 * Load user defined cash accounts
			 */
			// get connection and begin transaction
			localConnection = daoFactory.getConnection(aeConnection);

			// read active CashGOA after startDate and wrap them
			aeRequest.getArguments().put("activeAfterDate", AEDateUtil.formatToSystem(startDate));
			AEResponse aeResponseGOA = accLocal.loadGOA(aeRequest);

			// assets accounts in the GOA
			JSONArray assetsArray = aeResponseGOA.getPayload().optJSONObject("assetsGOA").optJSONArray("accounts");
			if(assetsArray != null) {
				for (int i = 0; i < assetsArray.length(); i++) {
					JSONObject account = assetsArray.getJSONObject(i);

					account.put("groupId", 100);
					account.put("group", "(1) Entrees");
					account.put("sysId", 10);
					account.put("ownerId", ownerId);

					// attributes metaData
					if(account.optJSONArray("attributes") != null) {
						account.put(
								"attrMetaData", 
								createAttributesMetaData(account.optJSONArray("attributes")));
					}

					records.put(account);
				}
			}

			// expenses accounts in the GOA
			JSONArray expensesArray = aeResponseGOA.getPayload().optJSONObject("expensesGOA").optJSONArray("accounts");
			if(expensesArray != null) {
				for (int i = 0; i < expensesArray.length(); i++) {
					JSONObject account = expensesArray.getJSONObject(i);

					account.put("groupId", 200);
					account.put("group", "(2) Sorties");
					account.put("sysId", 20);
					account.put("ownerId", ownerId);

					// attributes metaData
					if(account.optJSONArray("attributes") != null) {
						account.put(
								"attrMetaData", 
								createAttributesMetaData(account.optJSONArray("attributes")));
					}

					records.put(account);
				}
			}

			/**
			 * read cash journal entries for specified owner and period to be processed
			 * returned collection cannot be null, but can be empty
			 */
			CashModuleDAO cashDAO = daoFactory.getCashModuleDAO(localConnection);
			CashJournalEntriesList entriesCollection = 
				cashDAO.loadJournalEntries(ownerId, startDate, endDate);

			/**
			 * process date range for accounts accumulated in records
			 */
			double dayPrevBalance = 0.0;
			AEDateIterator dateIterator = new AEDateIterator(startDate, endDate);
			for (Date _date : dateIterator) {
				// the dataIndex where all information about this date is stored in _account
				String day = AEDateUtil.formatToSystem(_date);

				/**
				 * init system accounts for this day
				 */
				accountTotalEntrees.put(day, 0.0);
				accountTotalSorties.put(day, 0.0);
				accountSoldePrecedent.put(day, dayPrevBalance);
				accountSoldeTheorique.put(day, dayPrevBalance);
				accountReel.put(day, 0.0); // init here
				accountDifference.put(day, 0.0 - dayPrevBalance);

				for (int i = 0; i < records.length(); i++) {
					JSONObject _account = records.getJSONObject(i);

					if(!_account.has("accumulation")) {
						_account.put("accumulation", 0.0);
					}

					// process the pair(_account, _date)
					CashJournalEntry _entry =  CashJournalEntry.findByAccAndDate(
							entriesCollection, _account.optLong("id"), _date);
					if(_entry != null) {
						// an entry has ben found

						// update fields definition (fieldsArray)
						// add field to fields: amount and transsactions
						JSONObject field = new JSONObject();
						field.put("name", day);
						field.put("type", "float");
						fieldsArray.put(field);

						field = new JSONObject();
						field.put("name", day + "_transactions");
						fieldsArray.put(field);

						// update data for current account
						_account.put(day, _entry.getAmount() != null ? _entry.getAmount().doubleValue() : 0.0);
						_account.put(
								"accumulation", _account.optDouble("accumulation") + _account.optDouble(day));

						// transactions are loaded
						List<CashJETransacion> transList = _entry.getJeTransactions();
						if(!AECollectionUtil.isEmpty(transList)) {
							JSONObject jsonTransWrapper = new JSONObject();
							_account.put(day + "_transactions", jsonTransWrapper);

							JSONArray jsonTransArray = new JSONArray();
							jsonTransWrapper.put("transactions", jsonTransArray);

							for (CashJETransacion cashJETransacion : transList) {
								JSONObject jsonProp = new JSONObject();
								jsonProp.put("_value", cashJETransacion.getAmount() != null ? cashJETransacion.getAmount().doubleValue() : null);
								List<CashJETProp> propsList = cashJETransacion.getPropsList();
								for (CashJETProp cashJETProp : propsList) {
									jsonProp.put(Long.toString(cashJETProp.getPropDefId()), cashJETProp.getValue());
								}
								jsonTransArray.put(jsonProp);
							}
						}

						/**
						 * Columns
						 * We need to add column definition for the day
						 */
						JSONObject column = JSONUtil.findBy(columns, "header", day);
						if(column == null) {
							column = new JSONObject();
							column.put("xtype", "numbercolumn");
							column.put("align", "center");
							column.put("header", day);
							column.put("width", 80);
							column.put("dataIndex", day);
							column.put("_editor", "NumberField");
							column.put("editable", editable);
							//							// hidden management
							//							if(_entry.getAmount() != null) {
							//								// initialy set to hidden
							//								column.put("hidden", true);
							//							}
							columns.put(column);
						}

						/**
						 * column hidden management
						 */
						if(_entry.getAmount() == null) {
							// must be not hidden
							column.put("hidden", false);
						}

						// calculation
						switch(_account.optInt("sysId")) {
						case 10: 
							accountTotalEntrees.put(
									day, accountTotalEntrees.optDouble(day) + _account.optDouble(day));
							accountTotalEntrees.put(
									"accumulation", 
									accountTotalEntrees.optDouble("accumulation") + _account.optDouble(day));
							accountSoldeTheorique.put(
									day, accountSoldeTheorique.optDouble(day) + _account.optDouble(day));
							accountDifference.put(
									day, accountDifference.optDouble(day) - _account.optDouble(day));
							accountDifference.put(
									"accumulation", 
									accountDifference.optDouble("accumulation") - _account.optDouble(day));
							break;
						case 20: 
							accountTotalSorties.put(
									day, accountTotalSorties.optDouble(day) + _account.optDouble(day));
							accountTotalSorties.put(
									"accumulation", 
									accountTotalSorties.optDouble("accumulation") + _account.optDouble(day));
							accountSoldeTheorique.put(
									day, accountSoldeTheorique.optDouble(day) - _account.optDouble(day));
							accountDifference.put(
									day, accountDifference.optDouble(day) + _account.optDouble(day));
							accountDifference.put(
									"accumulation", accountDifference.optDouble("accumulation") + _account.optDouble(day));
							break;
						case 30 : 
							accountDifference.put(
									day, accountDifference.optDouble(day) + _account.optDouble(day));
							accountDifference.put(
									"accumulation", accountDifference.optDouble("accumulation") + _account.optDouble(day));
							break;
						}
					} else {
						// entry does'n exist

						// update fields definition (fieldsArray)
						// add field to fields: amount and transsactions
						JSONObject field = new JSONObject();
						field.put("name", day);
						field.put("type", "float");
						fieldsArray.put(field);

						field = new JSONObject();
						field.put("name", day + "_transactions");
						fieldsArray.put(field);

						// update data for current account
						if(_account.optLong("sysId") < 40) {
							// not system (enterable) account
							_account.put(day, 0.0);
						}

						// there are no transactions to process

						/**
						 * Columns
						 * We need to add column definition for the day
						 */
						// add column definition for the day if still not exist
						JSONObject column = JSONUtil.findBy(columns, "header", day);
						if(column == null) {
							column = new JSONObject();
							column.put("xtype", "numbercolumn");
							column.put("align", "right");
							column.put("header", day);
							column.put("width", 80);
							column.put("dataIndex", day);
							column.put("_editor", "NumberField");
							column.put("editable", editable);
							//							column.put("hidden", true);
							columns.put(column);
						}
						// must be not hidden
						if(_account.optLong("sysId") < 30) {
							column.put("hidden", false);
						}
					}

					/**
					 * keep dayPrevBalance for the next day
					 */
					if(accountReel.has(day)) {
						dayPrevBalance = accountReel.optDouble(day);
					} else {
						dayPrevBalance = 0.0;
					}

					/**
					 * VAT
					 */
					if(_account.has("accumulation")) {
						double taxedAmount = _account.getDouble("accumulation");
						double vatRate = 0.0;
						double amount = taxedAmount;
						double vatAmount = 0.0;
						if(_account.opt("vat") != null) {
							JSONObject vatJSON = _account.getJSONObject("vat");
							if(vatJSON.has("rate")) {
								vatRate = vatJSON.getDouble("rate");
								amount = AEMath.round(taxedAmount / ((100.0 + vatRate) / 100.0), 2);
								vatAmount = taxedAmount - amount;
							}
						}
						_account.put("TTC", taxedAmount);
						if(!_account.equals(accountDifference)
								&& !_account.equals(accountTotalSorties)
								&& !_account.equals(accountTotalEntrees)) {
							_account.put("HT", amount);
						}
						_account.put("TVA", vatAmount);
					}

				} // eo day
			}

			accountTotalEntrees.put("TTC", accountTotalEntrees.optDouble("accumulation"));
			accountTotalSorties.put("TTC", accountTotalSorties.optDouble("accumulation"));
			accountDifference.put("TTC", accountDifference.optDouble("accumulation"));

			/**
			 * Add these columns in the end
			 */
			JSONObject column6 = new JSONObject();
			column6.put("xtype", "numbercolumn");
			column6.put("align", "right");
			column6.put("header", "TTC");
			column6.put("width", 80);
			column6.put("hideable", false);
			column6.put("dataIndex", "TTC");
			column6.put("css", "font-weight: bold;");
			columns.put(column6);

			JSONObject column5 = new JSONObject();
			column5.put("xtype", "numbercolumn");
			column5.put("align", "right");
			column5.put("header", "TVA");
			column5.put("width", 80);
			column5.put("hideable", false);
			column5.put("dataIndex", "TVA");
			columns.put(column5);

			JSONObject column4 = new JSONObject();
			column4.put("xtype", "numbercolumn");
			column4.put("align", "right");
			column4.put("header", "HT");
			column4.put("width", 80);
			column1.put("hideable", false);
			column4.put("dataIndex", "HT");
			columns.put(column4);

			// load Accounts taged as <code>isCash</code> to load client store cash
			payload.put("cashAccounts", accLocal.loadCashAccountsByOwner(ownerId, localConnection));

			// load clients and suppliers
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);
			payload.put("clients", orgDAO.loadClients(ownerId));
			payload.put("suppliers", orgDAO.loadSuppliers(ownerId));

			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

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
//			throw new AEException(t.getMessage(), t);
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
//			t.printStackTrace();
//			throw new AEException(t.getMessage(), t);
//		} finally {
//			AEConnection.close(localConnection);
//		}
//	}

	@Override
	public AEResponse openPeriod(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * determine period
			 */
			AccPeriod accPeriod = getLastClosedPeriod(
					aeRequest.getArguments().getLong("ownerId"), 
					AEApp.CASH_MODULE_ID, 
					localConnection);

			if(accPeriod != null) {
				localConnection.beginTransaction();

				/**
				 * Delete general journal
				 */
				AccJournalDAO journalDAO = daoFactory.getAccJournalDAO(localConnection);
				journalDAO.deleteByAccPeriodId(accPeriod.getID());

				/**
				 * Update cash entries to NotAccounted
				 */
				CashModuleDAO cashModuleDAO = daoFactory.getCashModuleDAO(localConnection);
				cashModuleDAO.updateToNotAccounted(Organization.lazyDescriptor(
						aeRequest.getArguments().getLong("ownerId")), 
						accPeriod);

				/**
				 * Open the period
				 */
				AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
				accPeriodDAO.openPeriod(accPeriod.getID());
				accPeriodDAO.notExportedPeriod(accPeriod.getID());

				/**
				 * Delete attachments
				 */
				FileAttachmentDAO fileAttachmentDAO = daoFactory.getFileAttachmentDAO(localConnection);
				fileAttachmentDAO.deleteTo(accPeriod.getDescriptor());
			}

			/**
			 * commit transaction and return response
			 */
			localConnection.commit();

			/**
			 * return response
			 */
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Throwable t) {
			if(localConnection != null) {
				localConnection.rollback();
			}
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

//	private AccPeriod getLastClosedPeriod(long ownerId, long moduleId, AEConnection aeConnection) throws AEException {
//		AEConnection localConnection = null;
//		try {
//			AccPeriod accPeriod = null;
//
//			DAOFactory daoFactory = DAOFactory.getInstance();
//			localConnection = daoFactory.getConnection(aeConnection);
//
//			// detect the last closed period
//			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
//			accPeriod = accPeriodDAO.getLastClosedPeriod(ownerId, moduleId);
//
//			return accPeriod;
//		} catch (Throwable t) {
//			t.printStackTrace();
//			throw new AEException(t.getMessage(), t);
//		} finally {
//			AEConnection.close(localConnection);
//		}
//	}

	private JSONObject getVATAccount(
			JSONObject company, 
			JSONObject vatAccount, 
			JSONObject vatAccount1, 
			JSONObject vatAccount2, 
			JSONObject vatAccount3, 
			JSONObject jsonAccount) {

		JSONObject retAccount = null;
		if(jsonAccount.has("vat")) {
			JSONObject jsonVAT = jsonAccount.optJSONObject("vat");
			if(jsonVAT != null && jsonVAT.has("id")) {
				long vatId = jsonVAT.optLong("id");
				if(company.has("tvaOutVatId") && vatId == company.optLong("tvaOutVatId")) {
					retAccount = vatAccount;
				} else if(company.has("tvaOut1VatId") && vatId == company.optLong("tvaOut1VatId")) {
					retAccount = vatAccount1;
				} else if(company.has("tvaOut2VatId") && vatId == company.optLong("tvaOut2VatId")) {
					retAccount = vatAccount2;
				}  else if(company.has("tvaOut3VatId") && vatId == company.optLong("tvaOut3VatId")) {
					retAccount = vatAccount3;
				} 
			}
		}
		return retAccount;
	}

	@Override
	public AEResponse loadCFC(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * prepare dao's
			 */
			CFCDAO cfcDAO = daoFactory.getCFCDAO(localConnection);

			/**
			 * local attributes
			 */
			long ownerId = aeRequest.getArguments().optLong("ownerId");
			boolean loadData = aeRequest.getArguments().optBoolean("loadData");
			CFC cfc = null;

			/**
			 * determine AccPeriod
			 */
			boolean editable = !(aeRequest.getArguments().optBoolean("history"));
			AccPeriod accPeriod = null;
			if(editable) {
				// not history
				accPeriod = getFirstOpenPeriod(ownerId, AEApp.CFC_MODULE_ID, localConnection);
			} else {
				// history
				int month = aeRequest.getArguments().getInt("month");
				int year = aeRequest.getArguments().getInt("year");

				Date startDate = AEDateUtil.getFirstDate(month - 1, year);		
				Date endDate = AEDateUtil.getLastDate(month - 1, year);
				accPeriod = daoFactory.getAccPeriodDAO(localConnection).loadAccPeriod(
						startDate, 
						endDate, 
						AEApp.CFC_MODULE_ID,
						ownerId);
			}
			if(accPeriod == null) {
				throw new AEException(
						AEError.System.ACC_PERIOD_WAS_NOT_FOUD.getSystemID(), 
				"La période n'a pas été trouvé.");
			}

			// load CFC
			cfc = cfcDAO.loadCFC(Organization.lazyDescriptor(ownerId));

			// load CFCModel
			if(cfc != null) {
				cfc.setCfcModel(cfcDAO.loadCFCModel(cfc.getDescriptor()));
			}

			if(cfc != null && loadData) {
				CFCData cfcData = loadCFCData(cfc, accPeriod, invContext, localConnection);
				cfc.setCFCData(cfcData);
			}

			// load accounts
			JSONArray allCashAcounts = new JSONArray();
			AEResponse aeResponseGOA = accLocal.loadGOA(aeRequest);
			JSONArray assetsArray = aeResponseGOA.getPayload().optJSONObject("assetsGOA").optJSONArray("accounts");
			if(assetsArray != null) {
				for(int i = 0; i < assetsArray.length(); i++) {
					allCashAcounts.put(assetsArray.getJSONObject(i));
				}
			}
			JSONArray expensesArray = aeResponseGOA.getPayload().optJSONObject("expensesGOA").optJSONArray("accounts");
			if(expensesArray != null) {
				for(int i = 0; i < expensesArray.length(); i++) {
					allCashAcounts.put(expensesArray.getJSONObject(i));
				}
			}
			AccountDAO accDAO = daoFactory.getAccountDAO(localConnection);
			JSONArray accountsBank = accDAO.loadBankAccounts(ownerId);

			// create and rerturn response
			JSONObject payload = new JSONObject();
			if(cfc != null) {
				payload.put("cfc", cfc.toJSONObject());
			}
			payload.put("accounts", allCashAcounts);
			payload.put("accountsBank", accountsBank);
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse saveCFC(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			/**
			 * prepare dao's
			 */
			CFCDAO cfcDAO = daoFactory.getCFCDAO(localConnection);

			/**
			 * local attributes
			 */
			CFC cfc = new CFC();
			cfc.create(aeRequest.getArguments().getJSONObject("cfc"));

			// save CFC
			if(cfc.isNew()) {
				cfcDAO.insertCFC(cfc);
			} else if(cfc.isUpdated()) {
				cfcDAO.updateCFC(cfc);
			}

			// save CFCModel
			if(cfc.getCfcModel() != null) {
				CFCModel cfcModel = cfc.getCfcModel();
				for (Iterator<CFCColumn> iterator = cfcModel.iterator(); iterator.hasNext();) {
					CFCColumn cfcColumn = (CFCColumn) iterator.next();
					cfcColumn.setToCFC(cfc.getDescriptor());
					switch(cfcColumn.getPersistentState()) {
					case NEW:
						cfcDAO.insertCFCColumn(cfcColumn);
						break;
					case UPDATED:
						cfcDAO.updateCFCColumn(cfcColumn);
						break;
					case DELETED:
						cfcDAO.deleteCFCColumn(cfcColumn.getDescriptor());
						iterator.remove();
						break;
					}
				}
			}

			// commit
			localConnection.commit();

			// create and rerturn response
			JSONObject payload = new JSONObject();
			if(cfc != null) {
				payload.put("cfc", cfc.toJSONObject());
			}
			return new AEResponse(payload);
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public CFCData loadCFCData(CFC cfc, AccPeriod accPeriod,
			AEInvocationContext invContext, AEConnection aeConnection)
	throws AEException {

		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			/**
			 * prepare dao's
			 */
			CFCDAO cfcDAO = daoFactory.getCFCDAO(localConnection);
			CashModuleDAO cashModuleDAO = daoFactory.getCashModuleDAO(localConnection);

			/**
			 * local attributes
			 */
			CFCData cfcData = new CFCData();;

			/**
			 * create spread sheet
			 */
			// load cash journal
			CashJournalEntriesList dbCashEntriesList =
				cashModuleDAO.loadJournalEntriesLazzy(
						cfc.getCompany().getDescriptor().getID(), 
						accPeriod.getStartDate(), 
						accPeriod.getEndDate());

			// load saved cells
			ArrayList<CFCCell> cellsDBList = cfcDAO.loadCFCCells(
					cfc.getDescriptor(),
					accPeriod.getStartDate(),
					accPeriod.getEndDate());

			// create all spreadsheet and populate values regards model definitions
			AEDateIterator dateIterator = new AEDateIterator(accPeriod.getStartDate(), accPeriod.getEndDate());
			for (Date date : dateIterator) {
				// for every row
				CFCModel cfcModel = cfc.getCfcModel();
				for (CFCColumn column : cfcModel) {
					// for every column

					// process the cell (date, rowIndex, column)
					CFCCell cell = new CFCCell();
					cell.setEntryDate(date);
					cell.setColumn(column);
					cell.setToCFC(cfc.getDescriptor());
					cell.setRowIndex(0);
					cell.setColumnId(column.getID());

					// ensure the row where the cell should be inserted
					CFCRow row = cfcData.getRow(date, 0);
					if(row == null) {
						row = new CFCRow(date, 0);
						cfcData.add(row);
					}
					row.add(cell);

					// process meta data
					if(!AEValue.isNull(column.getValue()) && CFCColumn.NType.VALUE.equals(column.getNType())) {
						// try to set already saved value
						CFCCell savedCell = getSavedCFCCell(
								cell.getEntryDate(), 
								cell.getRowIndex(), 
								cell.getColumnId(), 
								cellsDBList);
						if(savedCell != null) {
							cell.setValue(savedCell.getValue());
						} else {
							if(!AEValue.isNull(column.getValue()) && AEValue.XType.DOUBLE.equals(column.getValue().getXType())) {
								cell.setValue(new AEValue("0.0", AEValue.XType.DOUBLE));
							} else if(!AEValue.isNull(column.getValue()) && AEValue.XType.STRING.equals(column.getValue().getXType())) {
								cell.setValue(new AEValue(AEStringUtil.EMPTY_STRING, AEValue.XType.STRING));
							}
						}
					} else if(!AEValue.isNull(column.getValue()) && CFCColumn.NType.ACCOUNT.equals(column.getNType())) {
						CashJournalEntry cjEntry = dbCashEntriesList.get(
								cell.getEntryDate(), 
								column.getValue().optLong());
						AEValue value = new AEValue();
						if(cjEntry != null) {
							Double amount = cjEntry.getAmount();
							if(amount != null) {
								value.set(cjEntry.getAmount());
							} else {
								value.set(0.0);
							}
						} else {
							value.set(0.0);
						}
						cell.setValue(value);
					}
				}
			}

			/**
			 * Value and Formula are set.
			 * Evaluate formulas.
			 */
			for (CFCRow cfcRow : cfcData) {
				for (CFCCell cfcCell : cfcRow) {
					CFCColumn colModel = cfcCell.getColumn();
					if(colModel != null 
							&& CFCColumn.NType.EXPRESSION.equals(colModel.getNType())) {

						// extract the expression
						String expression = colModel.getValue().getString();

						// configure parser to evaluate expression
						JEP parser = new JEP();
						parser.setAllowUndeclared(true);
						parser.parseExpression(expression);

						// iterate, detect and set parameters
						SymbolTable paramsTable = parser.getSymbolTable();
						@SuppressWarnings("unchecked")
						Set<String> params = paramsTable.keySet();
						for (String paramName : params) {
							double paramValue = 0.0;
							// param is regexpr C1, C2, ....
							if(paramName.startsWith("C")) {
								try {
									int paramColIndex = Integer.parseInt(paramName.substring(1)) - 1;
									for (CFCCell paramCell: cfcRow) {
										if(paramCell.getColumn().getColIndex() == paramColIndex) {
											paramValue = paramCell.getValue().getDouble();
											paramValue = AEMath.round(paramValue, 2);
											break;
										}
									}
									parser.addVariable(paramName, paramValue);
								} catch (Exception e) {
									// error in the expression, put error as value
									AEValue resultValue = new AEValue();
									resultValue.set("#ERR");
									cfcCell.setValue(resultValue);
									continue;
								}
							}

						}
						// vriables are set, so evaluate
						double result = parser.getValue();
						AEValue resultValue = new AEValue();
						resultValue.set(result);
						cfcCell.setValue(resultValue);
					}
				}
			}


			/** 
			 * Process sum
			 */
			CFCRow sumRow = new CFCRow(CFCRow.SUM);
			for (CFCRow row : cfcData) {
				for (CFCCell cell : row) {
					CFCCell sumCell = sumRow.getCell(cell.getColumnId());
					if(sumCell == null) {
						sumCell = new CFCCell(sumRow.getSysId());
						sumCell.setEntryDate(null);
						sumCell.setColumn(cell.getColumn());
						sumCell.setToCFC(cfc.getDescriptor());
						sumCell.setRowIndex(0);
						sumCell.setColumnId(cell.getColumnId());
						sumCell.setValue(new AEValue("0.0", AEValue.XType.DOUBLE));

						sumRow.add(sumCell);
					}
					sumCell.addDouble(cell.getValue());
				}
			}
			cfcData.add(sumRow);

			// rerturn response
			return cfcData;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}

	}

	private CFCCell getSavedCFCCell(Date date, int rowIndex, long columnID, ArrayList<CFCCell> dbCellsList) {
		CFCCell cell = null;
		for (CFCCell cfcCell : dbCellsList) {
			if(cfcCell.getRowIndex() == rowIndex
					&& cfcCell.getColumnId() == columnID
					&& AEDateUtil.areDatesEqual(cfcCell.getEntryDate(), date)) {
				cell = cfcCell;
				break;
			}
		}
		return cell;
	}

	@Override
	public AEResponse saveCFCCell(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * prepare dao's
			 */
			CFCDAO cfcDAO = daoFactory.getCFCDAO(localConnection);

			/**
			 * local attributes
			 */
			long ownerId = aeRequest.getArguments().optLong("ownerId");
			String field = aeRequest.getArguments().optString("field");
			Date date = AEDateUtil.parseDateStrict(aeRequest.getArguments().optString("date"));
			int rowIndex = aeRequest.getArguments().optInt("rowIndex");
			CFCColumn cfcColumn = cfcDAO.loadColumnByOwnerAndIndex(
					ownerId, 
					Integer.parseInt(field.substring(6)));

			if(cfcColumn != null) {
				CFCCell cfcCell = new CFCCell(
						cfcColumn, 
						date, 
						rowIndex, 
						new AEValue(aeRequest.getArguments().optString("value"), cfcColumn.getValue().getXType()));

				localConnection.beginTransaction();
				cfcDAO.updateCFCCellValue(cfcCell);
				localConnection.commit();

				// create and rerturn response
				JSONObject payload = new JSONObject();
				payload.put("cfcCell", cfcCell.toJSONObject());
				return new AEResponse(payload);
			} else {
				throw new AEException("Internal Error: The CFC Column cannot be found.");
			}
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse closingCFCPeriod(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			/**
			 * prepare dao's
			 */
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);

			/**
			 * owner
			 */
			OrganizationDAO orgDAO = DAOFactory.getInstance().getOrganizationDAO(localConnection);
			AEDescriptor customerDescr = Organization.lazyDescriptor(aeRequest.getArguments().getLong("ownerId"));
			JSONArray customersArray = orgDAO.loadCustomer(customerDescr);
			JSONObject customer = customersArray.getJSONObject(0);
			customerDescr.setName(customer.optString("name"));

			/**
			 * determine period
			 */
			AccPeriod accPeriod = getFirstOpenPeriod(
					customerDescr.getID(), 
					AEApp.CFC_MODULE_ID, 
					localConnection);

			// we can close only finished periods
			Date nowDate = AEDateUtil.getClearDate(new Date());
			if(nowDate.before(AEDateUtil.getClearDate(accPeriod.getEndDate()))) {
				throw new AEException(
						(int) AEError.System.CANNOT_CLOSE_UNFINISHED.getSystemID(), 
				"La période n'est pas terminée.");
			}

			/**
			 * close the period
			 */
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
			try {
				// send e-mail
				Emailer emailer = new Emailer();
				emailer.onPeriodClosed(customerDescr, invContext, accPeriod, AEApp.CFC_MODULE_ID);

				// auto export
				JSONObject exportRequestArguments = new JSONObject();
				exportRequestArguments.put("accPeriodId", accPeriod.getID());
				if("secal.png".equalsIgnoreCase(invContext.getAuthPrincipal().getDescription())) {
					exportRequestArguments.put("exportTo", "CEGID");
				} else if("gianati.png".equalsIgnoreCase(invContext.getAuthPrincipal().getDescription())) {
					exportRequestArguments.put("exportTo", "SAGE");
				}
				exportRequestArguments.put("ownerId", aeRequest.getArguments().getLong("ownerId"));
				AERequest exportRequest = new AERequest(exportRequestArguments);
				accLocal.export(exportRequest, invContext);
			} catch (Throwable t) {
				t.printStackTrace();
			}

			/**
			 * return response
			 */
			JSONObject payload = new JSONObject();
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
	public AEResponse openCFCPeriod(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * determine period
			 */
			AccPeriod accPeriod = getLastClosedPeriod(
					aeRequest.getArguments().getLong("ownerId"), 
					AEApp.CFC_MODULE_ID, 
					localConnection);

			if(accPeriod != null) {
				localConnection.beginTransaction();

				/**
				 * delete general journal
				 */
				AccJournalDAO journalDAO = daoFactory.getAccJournalDAO(localConnection);
				journalDAO.deleteByAccPeriodId(accPeriod.getID());

				/**
				 * open the period
				 */
				AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
				accPeriodDAO.openPeriod(accPeriod.getID());
				accPeriodDAO.notExportedPeriod(accPeriod.getID());

				/**
				 * Delete attachments
				 */
				FileAttachmentDAO fileAttachmentDAO = daoFactory.getFileAttachmentDAO(localConnection);
				fileAttachmentDAO.deleteTo(accPeriod.getDescriptor());
			}

			/**
			 * commit transaction and return response
			 */
			localConnection.commit();

			/**
			 * return response
			 */
			JSONObject payload = new JSONObject();
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
	public AEResponse saveBankAccount(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			long ownerId = arguments.getLong("ownerId");
			long sOwnerId = arguments.getLong("sOwnerId");
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			// authorize 
			authorize(new AuthPermission("System/Configuration/Customer/BankAccount", AuthPermission.SAVE_AND_DELETE), invContext, Organization.lazyDescriptor(ownerId));
			
			// whether this article is ownered by this customer
			if(ownerId != sOwnerId) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
			
			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			/**
			 * prepare dao's
			 */
			BankAccountDAO bankAccountDAO = daoFactory.getBankAccountDAO(localConnection);

			/**
			 * local attributes
			 */
			BankAccount bankAccount = new BankAccount();
			bankAccount.create(aeRequest.getArguments().getJSONObject("bankAccount"));
			
			// save bank account
			if(bankAccount.isNew()) {
				bankAccountDAO.insert(bankAccount);
			} else if(bankAccount.isUpdated()) {
				bankAccountDAO.update(bankAccount);
			}
			
			// validate uniqueness
			bankAccountDAO.validate(bankAccount);
			
			//parse recognition rules to java objects
			BankRecognitionRulesList rules = new BankRecognitionRulesList();
			rules.create(aeRequest.getArguments().getJSONArray("recognitionRules"));
			
			// prepare ownerId and BankAccountId for every recognition rule
			for (BankRecognitionRule rule: rules) {
				rule.setCompany(Organization.lazyDescriptor(ownerId));
				AEDescriptor bankAccDescr = BankAccount.lazyDescriptor(bankAccount.getID());
				rule.setBankAccount(bankAccDescr);
			}
			
			//save Bank Recognition Rules
			BankRecognitionRulesList brrList = bankService.saveRecognitionRules(rules, localConnection);

			// commit
			localConnection.commit();

			// create and rerturn response
			JSONObject payload = new JSONObject();
			payload.put("bankAccount", bankAccount.toJSONObject());
			payload.put("recognitionRules", brrList.toJSONArray());
			return new AEResponse(payload);
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			logger.error("Save bank account failed: ", t);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadBankAccounts(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = aeRequest.getAuthPrincipal();

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			long ownerId = arguments.getLong("ownerId");
			long sOwnerId = arguments.getLong("sOwnerId");
			
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			// authorize 
			authorize(new AuthPermission("System/Configuration/Customer/BankAccount", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));
			
			// whether this article is ownered by this customer
			if(ownerId != sOwnerId) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
			
			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			/**
			 * prepare dao's
			 */
			BankAccountDAO bankAccountDAO = daoFactory.getBankAccountDAO(localConnection);

			/**
			 * local attributes
			 */
			boolean loadAccounts = arguments.optBoolean("loadAccounts");
			boolean loadParties = arguments.optBoolean("loadParties");

			// load bank accounts
			BankAccountsList banksList = bankAccountDAO.load(Organization.lazyDescriptor(ownerId));

			// load parties
			JSONArray abstractAccounts = new JSONArray();

			if(loadParties) {
				// prepare
				OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);

				JSONArray clientsArray = orgDAO.loadThirdPartyAccounts(ownerId, 30);
				for(int i = 0; i < clientsArray.length(); i++) {
					JSONObject jsonComp = clientsArray.getJSONObject(i);
					jsonComp.put("groupId", "30");
					jsonComp.put("group", "--Fournisseur--");
					jsonComp.put("accId", jsonComp.optLong("compteGeneralId", 0));
					jsonComp.put("accCode", jsonComp.optString("compteGeneralCode", AEStringUtil.EMPTY_STRING));
					jsonComp.put("accDescription", jsonComp.optString("compteGeneralDescription", AEStringUtil.EMPTY_STRING));
					if(jsonComp.has("compteAuxiliare")) {
						jsonComp.put("auxiliary", jsonComp.optString("compteAuxiliare"));
					}
					
					String display = AEStringUtil.EMPTY_STRING;
					if(ap.getMaxRole() == AuthRole.System.operative_accountant.getSystemID()) {
						display = jsonComp.optString("name", AEStringUtil.EMPTY_STRING);
					} else {
						display = jsonComp.optString("accCode", AEStringUtil.EMPTY_STRING)
								+ " - " + jsonComp.optString("accDescription", AEStringUtil.EMPTY_STRING)
								+ " {" + jsonComp.optString("auxiliary", AEStringUtil.EMPTY_STRING) + "}";
					}
					jsonComp.put("display", display);
					
					abstractAccounts.put(jsonComp);
				}

				// load suppliers
				JSONArray suppliersArray = orgDAO.loadThirdPartyAccounts(ownerId, 10);
				for(int i = 0; i < suppliersArray.length(); i++) {
					JSONObject jsonComp = suppliersArray.getJSONObject(i);
					jsonComp.put("groupId", "10");
					jsonComp.put("group", "--Client--");
					jsonComp.put("accId", jsonComp.optLong("compteGeneralId", 0));
					jsonComp.put("accCode", jsonComp.optString("compteGeneralCode", AEStringUtil.EMPTY_STRING));
					jsonComp.put("accDescription", jsonComp.optString("compteGeneralDescription", AEStringUtil.EMPTY_STRING));
					if(jsonComp.has("compteAuxiliare")) {
						jsonComp.put("auxiliary", jsonComp.optString("compteAuxiliare"));
					}
					
					// what to display at the end
					String display = AEStringUtil.EMPTY_STRING;
					if(ap.getMaxRole() == AuthRole.System.operative_accountant.getSystemID()) {
						display = jsonComp.optString("name", AEStringUtil.EMPTY_STRING);
					} else {
						display = jsonComp.optString("accCode", AEStringUtil.EMPTY_STRING)
								+ " - " + jsonComp.optString("accDescription", AEStringUtil.EMPTY_STRING)
								+ " {" + jsonComp.optString("auxiliary", AEStringUtil.EMPTY_STRING) + "}";
					}
					jsonComp.put("display", display);
					
					abstractAccounts.put(jsonComp);
				}
			}

			// load accounts
			JSONArray accArray = null;
			if(loadAccounts) {
				accArray = accLocal.loadBankAccountsByOwner(ownerId, localConnection);
				if(loadParties) {
					for(int i = 0; i < accArray.length(); i++) {
						JSONObject jsonComp = accArray.getJSONObject(i);
						jsonComp.put("groupId", "1000");
						jsonComp.put("group", "--Compte--");
						jsonComp.put("accId", jsonComp.getLong("id"));
						jsonComp.put("id", -1 * jsonComp.getLong("id"));
						
						// what to display at the end
						String display = AEStringUtil.EMPTY_STRING;
						if(ap.getMaxRole() == AuthRole.System.operative_accountant.getSystemID()) {
							display = jsonComp.optString("description", AEStringUtil.EMPTY_STRING);
						} else {
							display = jsonComp.optString("code", AEStringUtil.EMPTY_STRING)
									+ " - " + jsonComp.optString("description", AEStringUtil.EMPTY_STRING);
						}
						jsonComp.put("display", display);
						
						abstractAccounts.put(jsonComp);
					}
				}
			}

			// create and rerturn response
			JSONObject payload = new JSONObject();
			payload.put("bankAccounts", banksList.toJSONArray());
			if(accArray != null) {
				payload.put("accounts", accArray);
			}
			if(loadParties) {
				payload.put("abstractAccounts", abstractAccounts);
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
	public AEResponse saveBankTransactions(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			/**
			 * Arguments
			 */
			long ownerId = aeRequest.getArguments().optLong("ownerId");
			long sOwnerId = aeRequest.getArguments().getLong("sOwnerId");
			long bankAccountId = aeRequest.getArguments().optLong("bankAccountId");
			JSONObject babJSON = aeRequest.getArguments().optJSONObject("balance");
			JSONArray btJSONArray = aeRequest.getArguments().getJSONArray("items");

			// Validate
			ap.ownershipValidator(ownerId);
			
			if(ownerId != sOwnerId) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
			
			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			
			BankJournalDAO bankJournalDAO = daoFactory.getBankJournalDAO(localConnection);
			AEDocumentItemDAO itemDAO = daoFactory.getAEDocumentItemDAO(localConnection);
			AETradeDocumentDAO tDocDAO = daoFactory.getAETradeDocumentDAO(localConnection);
			CFCDAO cfcDAO  = daoFactory.getCFCDAO(localConnection);
			MandatDAO mandatDAO  = daoFactory.getMandatDAO(localConnection);

			// Create instances			
			BankAccountBalance bab = new BankAccountBalance();
			bab.create(babJSON);
			
			// Validate
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
			AccPeriod accPeriod = accPeriodDAO.loadAccPeriod(bab.getAccPeriod().getDescriptor().getID());
			if(accPeriod == null || accPeriod.isClosed() || accPeriod.getCompany() == null || accPeriod.getCompany().getDescriptor().getID() != ownerId) {
				throw AEError.System.SECURITY_VIOLATION.toException();
			}
			
			BankTransactionsList bankTransactionsList = new BankTransactionsList();
			bankTransactionsList.create(btJSONArray);
			for (Iterator<BankTransaction> iterator = bankTransactionsList.iterator(); iterator.hasNext();) {
				BankTransaction bankTransaction = (BankTransaction) iterator.next();
				
				// the same bank account
				if(bab.getBankAccount() == null || bankTransaction.getBankAccount() == null 
						|| bab.getBankAccount().getDescriptor().getID() != bankTransaction.getBankAccount().getID()) {
					
					throw AEError.System.SECURITY_VIOLATION.toException();
				}
				
				// entry date in period
				if(bankTransaction.getDate() == null
						|| bankTransaction.getDate().before(accPeriod.getStartDate()) || bankTransaction.getDate().after(accPeriod.getEndDate())) {
					throw new AEException("Transaction invalide (Date de valeur).");
				}
			}
			
			// Save in transaction
			localConnection.beginTransaction();
			
			for (Iterator<BankTransaction> iterator = bankTransactionsList.iterator(); iterator.hasNext();) {
				BankTransaction bankTransaction = (BankTransaction) iterator.next();
				Boolean paid = null;
				switch(bankTransaction.getPersistentState()) {
				case NEW:
					bankJournalDAO.insert(bankTransaction);
					paid = Boolean.TRUE;
					break;
				case UPDATED:
					bankJournalDAO.update(bankTransaction);
					paid = Boolean.TRUE;
					break;
				case DELETED:
					bankJournalDAO.delete(bankTransaction);
					paid = Boolean.FALSE;
					iterator.remove();
					break;
				default:
					break;
				}
				if(paid != null && bankTransaction.getReference() != null) {
					if(DomainClass.AEInvoiceItem.equals(bankTransaction.getReference().getClazz())) {
						itemDAO.setPaid(bankTransaction.getReference().getID(), paid.booleanValue());
					} else if(DomainClass.CFCCell.equals(bankTransaction.getReference().getClazz())) {
						cfcDAO.setPaid(bankTransaction.getReference().getID(), paid.booleanValue());
					} else if(DomainClass.MandatCell.equals(bankTransaction.getReference().getClazz())) {
						mandatDAO.setPaid(bankTransaction.getReference().getID(), paid.booleanValue());
					} if(DomainClass.AeDocument.equals(bankTransaction.getReference().getClazz())) {
						tDocDAO.setPaid(bankTransaction.getReference().getID(), paid.booleanValue());
					}
				}
			}

			// update balance
			BankAccountDAO babDAO = daoFactory.getBankAccountDAO(localConnection);
			babDAO.update(bab);

			// commit
			localConnection.commit();

			// payableItems
			JSONArray payableItems = new JSONArray();

			// load unpaid items
			AETradeDocumentResultsList bankDocuments = tDocDAO.loadUnpaidBankItems(ownerId, bankAccountId);
			for (AETradeDocumentResult aeTradeDocumentResult : bankDocuments) {
				JSONObject j = aeTradeDocumentResult.toJSONObject();
				j.put("group", "Saisie Chèques - VRT - LCR");
				payableItems.put(j);
			}

			// load sale invoices items
			AETradeDocumentResultsList saleInvDocuments = tDocDAO.loadSaleInvoicesUnpaid(ownerId);
			for (AETradeDocumentResult aeTradeDocumentResult : saleInvDocuments) {
				JSONObject j = aeTradeDocumentResult.toJSONObject();
				j.put("group", "Ventes");		
				payableItems.put(j);
			}

			// load unpaid cfc
			ArrayList<CFCCell> cfcCells = cfcDAO.loadUnpaidCFCCells(ownerId);
			for (CFCCell cfcCell : cfcCells) {
				if(!AEValue.isNull(cfcCell.getValue()) && cfcCell.getValue().optDouble() != 0.0) {
					JSONObject j = cfcCell.toJSONObject();
					j.put("docType", cfcCell.getSysId());
					j.put("number", cfcCell.getName());
					j.put("group", "Suivi virements de fonds (CFC)");
					payableItems.put(j);
				}
			}

			// load unpaid mandat
			ArrayList<CFCCell> mandatCells = mandatDAO.loadUnpaidMandatCells(ownerId);
			for (CFCCell mandatCell : mandatCells) {
				if(!AEValue.isNull(mandatCell.getValue()) && mandatCell.getValue().optDouble() != 0.0) {
					JSONObject j = mandatCell.toJSONObject();
					j.put("docType", mandatCell.getSysId());
					j.put("number", mandatCell.getName());
					j.put("group", "Mandat");
					payableItems.put(j);
				}
			}

			// create and rerturn response
			JSONObject payload = new JSONObject();
			payload.put("items", bankTransactionsList.toJSONArray());
			payload.put("payableItems", payableItems);
			return new AEResponse(payload);
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public AEResponse recognizeAndSaveBankTransactions(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			//localConnection.beginTransaction();
			
			/**
			 * load recognition rules
			 */
			
			BankJournalDAO bankDAO = daoFactory.getBankJournalDAO(localConnection);
			
			/**
			 * init some variables
			 */
			
			long bankAccountId = aeRequest.getArguments().optLong("bankAccountId");
			BankRecognitionRulesList rulesList = bankDAO.load(bankAccountId);
			//long ownerId = aeRequest.getArguments().optLong("ownerId");
			BankTransactionsList unrecognisedTransactions = new BankTransactionsList();
			
			JSONArray trs = aeRequest.getArguments().optJSONArray("items");
			BankTransactionsList allTransactions = new BankTransactionsList();
			allTransactions.create(trs);
			
			
			for (BankTransaction t : allTransactions) {
				if (!t.isRecognised()) {
					unrecognisedTransactions.add(t);
				}
			}
			
			/**
			 * recongize transactions
			 */
			bankService.recognizeBankTransactionsList(unrecognisedTransactions, rulesList);
			
			aeRequest.getArguments().put("items", allTransactions.toJSONArray());
			
			saveBankTransactions(aeRequest);
			
			
			
			return saveBankTransactions(aeRequest);
		} catch (JSONException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
		
	}

	@Override
	public AEResponse loadBankTransactions(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * prepare dao's
			 */
			BankJournalDAO bankJournalDAO = daoFactory.getBankJournalDAO(localConnection);
			BankAccountDAO bankAccDAO = daoFactory.getBankAccountDAO(localConnection); 

			/**
			 * local attributes
			 */
			long ownerId = aeRequest.getArguments().optLong("ownerId");
			long bankAccountId = aeRequest.getArguments().optLong("bankAccountId");
			AEDescriptor bankAccDescr = BankAccount.lazyDescriptor(bankAccountId);

			/**
			 * determine AccPeriod
			 */
			boolean editable = !(aeRequest.getArguments().optBoolean("history"));
			AccPeriod accPeriod = null;
			if(editable) {
				// not history
				accPeriod = getFirstOpenPeriod(ownerId, AEApp.BANK_MODULE_ID, localConnection);
			} else {
				// history
				int month = aeRequest.getArguments().getInt("month");
				int year = aeRequest.getArguments().getInt("year");

				Date startDate = AEDateUtil.getFirstDate(month - 1, year);		
				Date endDate = AEDateUtil.getLastDate(month - 1, year);
				accPeriod = daoFactory.getAccPeriodDAO(localConnection).loadAccPeriod(
						startDate, 
						endDate, 
						AEApp.BANK_MODULE_ID,
						ownerId);
			}
			if(accPeriod == null) {
				throw new AEException(
						AEError.System.ACC_PERIOD_WAS_NOT_FOUD.getSystemID(), 
				"La période n'a pas été trouvé.");
			}

			// load transactions
			BankTransactionsList bankTransactions = bankJournalDAO.load(
					Organization.lazyDescriptor(ownerId), 
					BankAccount.lazyDescriptor(bankAccountId), 
					accPeriod.getStartDate(), 
					accPeriod.getEndDate());

			// load balance
			BankAccountBalance bab = bankAccDAO.load(
					bankAccDescr, 
					accPeriod.getDescriptor());
			if(bab == null) {
				bab = new BankAccountBalance();
				bab.setBankAccount(bankAccDescr);
				bab.setAccPeriod(accPeriod);

				// try to find last closed balance
				AccPeriod lastClosedPeriod = getLastClosedPeriod(ownerId, AEApp.BANK_MODULE_ID, localConnection);
				if(lastClosedPeriod != null) {
					BankAccountBalance closedBB = bankAccDAO.load(
							bankAccDescr, 
							lastClosedPeriod.getDescriptor());
					if(closedBB != null) {
						bab.setOpeningBalance(closedBB.getFinalBalance());
					}
				}

				bab.calculate(bankTransactions);
				bankAccDAO.insert(bab);
			} else {
				// FIX ME
				bab.calculate(bankTransactions);
			}

			// payableItems
			JSONArray payableItems = new JSONArray();

			if(editable) {
				// load unpaid bank document items
				AETradeDocumentDAO tDocDAO = daoFactory.getAETradeDocumentDAO(localConnection);
				AETradeDocumentResultsList bankDocuments = tDocDAO.loadUnpaidBankItems(ownerId, bankAccountId);
				for (AETradeDocumentResult aeTradeDocumentResult : bankDocuments) {
					JSONObject j = aeTradeDocumentResult.toJSONObject();
					j.put("group", "Saisie Chèques - VRT - LCR");		
					payableItems.put(j);
				}

				// load sale invoices items
				AETradeDocumentResultsList saleInvDocuments = tDocDAO.loadSaleInvoicesUnpaid(ownerId);
				for (AETradeDocumentResult aeTradeDocumentResult : saleInvDocuments) {
					JSONObject j = aeTradeDocumentResult.toJSONObject();
					j.put("group", "Ventes");		
					payableItems.put(j);
				}

				// load unpaid cfc
				CFCDAO cfcDAO = daoFactory.getCFCDAO(localConnection);
				ArrayList<CFCCell> cfcCells = cfcDAO.loadUnpaidCFCCells(ownerId);
				for (CFCCell cfcCell : cfcCells) {
					if(!AEValue.isNull(cfcCell.getValue()) && cfcCell.getValue().optDouble() != 0.0) {
						JSONObject j = cfcCell.toJSONObject();
						j.put("docType", cfcCell.getSysId());
						j.put("number", cfcCell.getName());
						j.put("group", "Suivi virements de fonds (CFC)");
						payableItems.put(j);
					}
				}

				// load unpaid mandat
				MandatDAO mandatDAO = daoFactory.getMandatDAO(localConnection);
				ArrayList<CFCCell> mandatCells = mandatDAO.loadUnpaidMandatCells(ownerId);
				for (CFCCell mandatCell : mandatCells) {
					if(!AEValue.isNull(mandatCell.getValue()) && mandatCell.getValue().optDouble() != 0.0) {
						JSONObject j = mandatCell.toJSONObject();
						j.put("docType", mandatCell.getSysId());
						j.put("number", mandatCell.getName());
						j.put("group", "Mandat");
						payableItems.put(j);
					}
				}
			}

			// create and rerturn response
			JSONObject payload = new JSONObject();
			payload.put("items", bankTransactions.toJSONArray());
			payload.put("payableItems", payableItems);
			payload.put("period", AEDateUtil.convertToString(accPeriod.getStartDate(), "MM/yyyy"));
			payload.put("startDate", AEDateUtil.convertToString(accPeriod.getStartDate(), AEDateUtil.SYSTEM_DATE_FORMAT));
			payload.put("endDate", AEDateUtil.convertToString(accPeriod.getEndDate(), AEDateUtil.SYSTEM_DATE_FORMAT));
			payload.put("balance", bab.toJSONObject());
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse importETEBAC(File fileETEBAC, AEInvocationContext invContext) throws AEException {
		return bankService.importETEBAC(fileETEBAC, invContext);
	}

	@Override
	public AEResponse closingBankPeriod(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			/**
			 * prepare
			 */
			long ownerId = aeRequest.getArguments().optLong("ownerId");
			BankAccountDAO bankAccountDAO = daoFactory.getBankAccountDAO(localConnection);
			BankJournalDAO bankJournalDAO = daoFactory.getBankJournalDAO(localConnection);
			AccJournalDAO journalDAO = daoFactory.getAccJournalDAO(localConnection);

			/**
			 * owner
			 */
			OrganizationDAO orgDAO = DAOFactory.getInstance().getOrganizationDAO(localConnection);
			AEDescriptor customerDescr = Organization.lazyDescriptor(aeRequest.getArguments().getLong("ownerId"));
			JSONArray customersArray = orgDAO.loadCustomer(customerDescr);
			JSONObject customer = customersArray.getJSONObject(0);
			customerDescr.setName(customer.optString("name"));

			/**
			 * determine period
			 */
			AccPeriod accPeriod = getFirstOpenPeriod(
					customerDescr.getID(), 
					AEApp.BANK_MODULE_ID, 
					localConnection);

			Date startDate = AEDateUtil.getClearDate(accPeriod.getStartDate());	
			Date endDate = AEDateUtil.getClearDate(accPeriod.getEndDate());

			// we can close only finished periods
			Date nowDate = AEDateUtil.getClearDate(new Date());
			if(nowDate.before(endDate)) {
//				throw new AEException(
//						(int) AEError.System.CANNOT_CLOSE_UNFINISHED.getSystemID(), 
//				"La période n'est pas terminée.");
			}

			/**
			 * get all bank accounts
			 */
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);
			BankAccountsList banksList = bankAccountDAO.load(ownerDescr);

			/**
			 * validate
			 */
			for (Iterator<BankAccount> iterator = banksList.iterator(); iterator.hasNext();) {
				BankAccount bankAccount = (BankAccount) iterator.next();
				if(bankAccount.getEntryType() == BankAccount.ENTRY_TYPE_ETEBAC) {
					AccPeriod firstOpenPeriod = getFirstOpenPeriod(ownerId, AEApp.BANK_MODULE_ID, localConnection);
					if(firstOpenPeriod != null) {
						BankAccountBalance balance = bankAccountDAO.load(
								bankAccount.getDescriptor(), 
								firstOpenPeriod.getDescriptor());
						if(balance.getBankOpeningBalanceDate() != null && Math.abs(balance.getOpeningBalance() - balance.getBankOpeningBalance()) > 0.001) {
							throw new AEException(
									String.format(
											"The period cannot be closed:<p><b>Calculated opening balance for '%s' is different than imported.", 
											bankAccount.getName()));
						}
						if(balance.getBankFinalBalanceDate() != null && Math.abs(balance.getFinalBalance() - balance.getBankFinalBalance()) > 0.001) {
							throw new AEException(
									String.format(
											"The period cannot be closed:<p><b>Calculated final balance for '%s' is different than imported.", 
											bankAccount.getName()));
						}
					}
				}
			}

			// detect the bank accounting policy
			int bankAccMode = customer.optInt("bankAccMode");
			boolean groupingAccMode = (bankAccMode == FinancesUtil.BANK_ACC_MODE_GROUPING);

			/**
			 * process
			 */
			for (Iterator<BankAccount> iterator = banksList.iterator(); iterator.hasNext();) {
				BankAccount bankAccount = (BankAccount) iterator.next();

				// load all transactions for current bank account
				BankTransactionsList bankTransactions = bankJournalDAO.load(
						ownerDescr, 
						bankAccount.getDescriptor(), 
						startDate, 
						endDate);

				// prepare acc journal for multiple use
				AEDescriptor journalDescr = bankAccount.getAccJournal().getDescriptor();

				// prepare debit and credit journal entries in case of compound accounting
				// they will not be used in other case!!!
				AccJournalEntry compoundJournalEntry = null;
				if(groupingAccMode) {
					compoundJournalEntry = new AccJournalEntry();
				}

				// post to general journal ever bank transaction
				for (Iterator<BankTransaction> iterator2 = bankTransactions.iterator(); iterator2.hasNext();) {
					BankTransaction bankTransaction = (BankTransaction) iterator2.next();
					
					// skip balances
					if(!BankTransaction.CODE_MOVEMENT.equals(bankTransaction.getCode())) {
						continue;
					}
					
					// check for account
					if(bankTransaction.getAccount() == null || !bankTransaction.getAccount().isPersistent()) {
						throw new AEException(
								"Impossible de clôturer la période, au moins un compte non reconnu pour la banque \'"
								+ bankAccount.getName() + "\'");
					}

					if(groupingAccMode) {
						// compound accounting

						AccJournalItem transactionItem = new AccJournalItem(accPeriod);
						transactionItem.setCompany(ownerDescr);
						transactionItem.setJournal(journalDescr);
						transactionItem.setDate(bankTransaction.getDate());
						transactionItem.setReference(bankTransaction.getReference());
						transactionItem.setDescription(bankTransaction.getDescription());

						if(bankTransaction.getDtAmount() != null 
								&& Math.abs(AEMath.round(bankTransaction.getDtAmount(), 2)) >= 0.005) {
							transactionItem.setAccount(bankTransaction.getAccount());
							transactionItem.setAuxiliary(bankTransaction.getAuxiliary());
							transactionItem.setCtAmount(bankTransaction.getDtAmount());
						} else if(bankTransaction.getCtAmount() != null
								&& Math.abs(AEMath.round(bankTransaction.getCtAmount(), 2)) >= 0.005) {
							transactionItem.setAccount(bankTransaction.getAccount());
							transactionItem.setAuxiliary(bankTransaction.getAuxiliary());
							transactionItem.setDtAmount(bankTransaction.getCtAmount());
						}

						// add item
						compoundJournalEntry.addItem(transactionItem);
					} else {
						// detail accounting
						AccJournalEntry journalEntry = new AccJournalEntry();

						AccJournalItem debitItem = new AccJournalItem(accPeriod);
						debitItem.setCompany(ownerDescr);
						debitItem.setJournal(journalDescr);
						debitItem.setDate(bankTransaction.getDate());
						debitItem.setReference(bankTransaction.getReference());
						debitItem.setDescription(bankTransaction.getDescription());

						AccJournalItem creditItem = new AccJournalItem(accPeriod);
						creditItem.setCompany(ownerDescr);
						creditItem.setJournal(journalDescr);
						creditItem.setDate(bankTransaction.getDate());
						creditItem.setReference(bankTransaction.getReference());
						creditItem.setDescription(bankTransaction.getDescription());

						if(bankTransaction.getDtAmount() != null 
								&& Math.abs(AEMath.round(bankTransaction.getDtAmount(), 2)) > 0.001) {

							debitItem.setAccount(bankAccount.getAccount().getDescriptor());
							debitItem.setDtAmount(bankTransaction.getDtAmount());

							creditItem.setAccount(bankTransaction.getAccount());
							creditItem.setAuxiliary(bankTransaction.getAuxiliary());
							creditItem.setCtAmount(bankTransaction.getDtAmount());
						} else if(bankTransaction.getCtAmount() != null
								&& Math.abs(AEMath.round(bankTransaction.getCtAmount(), 2)) > 0.001) {

							debitItem.setAccount(bankTransaction.getAccount());
							debitItem.setAuxiliary(bankTransaction.getAuxiliary());
							debitItem.setDtAmount(bankTransaction.getCtAmount());

							creditItem.setAccount(bankAccount.getAccount().getDescriptor());
							creditItem.setCtAmount(bankTransaction.getCtAmount());
						}

						// add items
						journalEntry.addItem(debitItem);
						journalEntry.addItem(creditItem);

						// insert entry
						journalDAO.insertEntry(journalEntry);
					}
				}

				// the bankAccount finished
				if(groupingAccMode && compoundJournalEntry != null) {
					// create debit balance item
					AccJournalItem debitBalanceItem = new AccJournalItem(accPeriod);
					debitBalanceItem.setCompany(ownerDescr);
					debitBalanceItem.setJournal(journalDescr);
					debitBalanceItem.setDate(accPeriod.getEndDate());
					debitBalanceItem.setDescription(bankAccount.getName());
					debitBalanceItem.setAccount(bankAccount.getAccount().getDescriptor());
					debitBalanceItem.setDtAmount(compoundJournalEntry.calcCtAmount());

					// create credit balance item
					AccJournalItem creditBalanceItem = new AccJournalItem(accPeriod);
					creditBalanceItem.setCompany(ownerDescr);
					creditBalanceItem.setJournal(journalDescr);
					creditBalanceItem.setDate(accPeriod.getEndDate());
					creditBalanceItem.setDescription(bankAccount.getName());
					creditBalanceItem.setAccount(bankAccount.getAccount().getDescriptor());
					creditBalanceItem.setCtAmount(compoundJournalEntry.calcDtAmount());

					// add items after calculation (these items must be not included in calculation)
					if(debitBalanceItem != null && Math.abs(AEMath.doubleValue(debitBalanceItem.getDtAmount())) >= 0.005) {
						compoundJournalEntry.addItem(debitBalanceItem);
					}
					if(creditBalanceItem != null && Math.abs(AEMath.doubleValue(creditBalanceItem.getCtAmount())) >= 0.005) {
						compoundJournalEntry.addItem(creditBalanceItem);
					}

					// insert compound entry
					journalDAO.insertEntry(compoundJournalEntry);
				}
			}

			// close acc period
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
			accPeriodDAO.closePeriod(accPeriod.getID());

			// set final balances as next opening balances
			for (Iterator<BankAccount> iterator = banksList.iterator(); iterator.hasNext();) {
				BankAccount bankAccount = (BankAccount) iterator.next();
				AccPeriod lastClosedPeriod = getLastClosedPeriod(ownerId, AEApp.BANK_MODULE_ID, localConnection);
				AccPeriod firstOpenPeriod = getFirstOpenPeriod(ownerId, AEApp.BANK_MODULE_ID, localConnection);
				if(lastClosedPeriod != null && firstOpenPeriod != null) {
					BankAccountBalance lastClosedBB = bankAccountDAO.load(
							bankAccount.getDescriptor(), 
							lastClosedPeriod.getDescriptor());
					BankAccountBalance firstOpenBB = bankAccountDAO.load(
							bankAccount.getDescriptor(), 
							firstOpenPeriod.getDescriptor());
					
					// firstOpenBB: create if not exists 
					if(firstOpenBB == null) {
						firstOpenBB = new BankAccountBalance();
						firstOpenBB.setBankAccount(bankAccount);
						firstOpenBB.setAccPeriod(firstOpenPeriod);
						bankAccountDAO.insert(firstOpenBB);
					}
					
					// update firstOpenBB
					if(lastClosedBB != null && firstOpenBB != null) {
						// set opening balance
						firstOpenBB.setOpeningBalance(lastClosedBB.getFinalBalance());
						
						// load bank entries in the period
						// and calculate balance
						BankTransactionsList bankTransactions = bankJournalDAO.load(
								Organization.lazyDescriptor(ownerId), 
								BankAccount.lazyDescriptor(bankAccount.getDescriptor().getID()), 
								firstOpenPeriod.getStartDate(), 
								firstOpenPeriod.getEndDate());
						firstOpenBB.calculate(bankTransactions);

						// update in the DB
						bankAccountDAO.update(firstOpenBB);
					}
				}
			}

			/**
			 * commit transaction
			 */
			localConnection.commit();

			/**
			 * Process after close.
			 * Must be after transaction close.
			 * If after close fails, this should not affect the whole close task.
			 */
			try {
				// send e-mail
				Emailer emailer = new Emailer();
				emailer.onPeriodClosed(customerDescr, invContext, accPeriod, AEApp.BANK_MODULE_ID);

				// auto export
				JSONObject exportRequestArguments = new JSONObject();
				exportRequestArguments.put("accPeriodId", accPeriod.getID());
				if("secal.png".equalsIgnoreCase(invContext.getAuthPrincipal().getDescription())) {
					exportRequestArguments.put("exportTo", "CEGID");
				} else if("gianati.png".equalsIgnoreCase(invContext.getAuthPrincipal().getDescription())) {
					exportRequestArguments.put("exportTo", "SAGE");
				}
				exportRequestArguments.put("ownerId", aeRequest.getArguments().getLong("ownerId"));
				AERequest exportRequest = new AERequest(exportRequestArguments);
				accLocal.export(exportRequest, invContext);
			} catch (Throwable t) {
				t.printStackTrace();
			}

			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Throwable t) {
			if(localConnection != null) {
				localConnection.rollback();
			}
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse openBankPeriod(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * determine period
			 */
			AccPeriod accPeriod = getLastClosedPeriod(
					aeRequest.getArguments().getLong("ownerId"), 
					AEApp.BANK_MODULE_ID, 
					localConnection);

			if(accPeriod != null) {
				localConnection.beginTransaction();

				/**
				 * delete general journal
				 */
				AccJournalDAO journalDAO = daoFactory.getAccJournalDAO(localConnection);
				journalDAO.deleteByAccPeriodId(accPeriod.getID());

				/**
				 * open the period
				 */
				AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
				accPeriodDAO.openPeriod(accPeriod.getID());
				accPeriodDAO.notExportedPeriod(accPeriod.getID());

				/**
				 * Delete attachments
				 */
				FileAttachmentDAO fileAttachmentDAO = daoFactory.getFileAttachmentDAO(localConnection);
				fileAttachmentDAO.deleteTo(accPeriod.getDescriptor());
			}

			/**
			 * commit transaction and return response
			 */
			localConnection.commit();

			/**
			 * return response
			 */
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Throwable t) {
			if(localConnection != null) {
				localConnection.rollback();
			}
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadMandat(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * prepare dao's
			 */
			MandatDAO mandatDAO = daoFactory.getMandatDAO(localConnection);

			/**
			 * local attributes
			 */
			long ownerId = aeRequest.getArguments().optLong("ownerId");
			boolean loadData = aeRequest.getArguments().optBoolean("loadData");

			/**
			 * determine AccPeriod
			 */
			boolean editable = !(aeRequest.getArguments().optBoolean("history"));
			AccPeriod accPeriod = null;
			if(editable) {
				// not history
				accPeriod = getFirstOpenPeriod(ownerId, AEApp.MANDAT_MODULE_ID, localConnection);
			} else {
				// history
				int month = aeRequest.getArguments().getInt("month");
				int year = aeRequest.getArguments().getInt("year");

				Date startDate = AEDateUtil.getFirstDate(month - 1, year);		
				Date endDate = AEDateUtil.getLastDate(month - 1, year);
				accPeriod = daoFactory.getAccPeriodDAO(localConnection).loadAccPeriod(
						startDate, 
						endDate, 
						AEApp.MANDAT_MODULE_ID,
						ownerId);
			}
			if(accPeriod == null) {
				throw new AEException(
						AEError.System.ACC_PERIOD_WAS_NOT_FOUD.getSystemID(), 
				"La période n'a pas été trouvé.");
			}

			// load Mandat
			Mandat mandat = mandatDAO.loadMandat(Organization.lazyDescriptor(ownerId));

			// load CFCModel
			if(mandat != null) {
				mandat.setCfcModel(mandatDAO.loadMandatModel(mandat.getDescriptor()));
			}

			if(mandat != null && loadData) {
				CFCData mandatData = loadMandatData(mandat, accPeriod, invContext, localConnection);
				mandat.setCFCData(mandatData);
			}

			// load accounts
			JSONArray allCashAcounts = new JSONArray();
			AEResponse aeResponseGOA = accLocal.loadGOA(aeRequest);
			JSONArray assetsArray = aeResponseGOA.getPayload().optJSONObject("assetsGOA").optJSONArray("accounts");
			if(assetsArray != null) {
				for(int i = 0; i < assetsArray.length(); i++) {
					allCashAcounts.put(assetsArray.getJSONObject(i));
				}
			}
			JSONArray expensesArray = aeResponseGOA.getPayload().optJSONObject("expensesGOA").optJSONArray("accounts");
			if(expensesArray != null) {
				for(int i = 0; i < expensesArray.length(); i++) {
					allCashAcounts.put(expensesArray.getJSONObject(i));
				}
			}
			AccountDAO accDAO = daoFactory.getAccountDAO(localConnection);
			JSONArray accountsBank = accDAO.loadBankAccounts(ownerId);

			// create and rerturn response
			JSONObject payload = new JSONObject();
			if(mandat != null) {
				payload.put("Mandat", mandat.toJSONObject());

			}
			payload.put("accounts", allCashAcounts);
			payload.put("accountsBank", accountsBank);
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse saveMandat(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			/**
			 * prepare dao's
			 */
			MandatDAO mandatDAO = daoFactory.getMandatDAO(localConnection);

			/**
			 * local attributes
			 */
			CFC mandat = new CFC();
			mandat.create(aeRequest.getArguments().getJSONObject("Mandat"));

			// save CFC
			if(mandat.isNew()) {
				mandatDAO.insertMandat(mandat);
			} else if(mandat.isUpdated()) {
				mandatDAO.updateMandat(mandat);
			}

			// save CFCModel
			if(mandat.getCfcModel() != null) {
				CFCModel mandatModel = mandat.getCfcModel();
				for (Iterator<CFCColumn> iterator = mandatModel.iterator(); iterator.hasNext();) {
					CFCColumn mandatColumn = (CFCColumn) iterator.next();
					mandatColumn.setToCFC(mandat.getDescriptor());
					switch(mandatColumn.getPersistentState()) {
					case NEW:
						mandatDAO.insertMandatColumn(mandatColumn);
						break;
					case UPDATED:
						mandatDAO.updateMandatColumn(mandatColumn);
						break;
					case DELETED:
						mandatDAO.deleteMandatColumn(mandatColumn.getDescriptor());
						iterator.remove();
						break;
					}
				}
			}

			// commit
			localConnection.commit();

			// create and rerturn response
			JSONObject payload = new JSONObject();
			if(mandat != null) {
				payload.put("Mandat", mandat.toJSONObject());
			}
			return new AEResponse(payload);
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse saveMandatCell(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * prepare dao's
			 */
			MandatDAO mandatDAO = daoFactory.getMandatDAO(localConnection);

			/**
			 * local attributes
			 */
			long ownerId = aeRequest.getArguments().optLong("ownerId");
			String field = aeRequest.getArguments().optString("field");
			Date date = AEDateUtil.parseDateStrict(aeRequest.getArguments().optString("date"));
			int rowIndex = aeRequest.getArguments().optInt("rowIndex");
			
			
//			CFCColumn mandatColumn = mandatDAO.loadColumnByOwnerAndIndex(
//					ownerId, 
//					Integer.parseInt(field.substring(6)));

			CFCColumn mandatColumn = mandatDAO.loadColumnByOwnerAndId(
					ownerId, 
					Long.parseLong(field.substring(6)));
			
			if(mandatColumn != null) {
				CFCCell mandatCell = new CFCCell(
						mandatColumn, 
						date, 
						rowIndex, 
						new AEValue(aeRequest.getArguments().optString("value"), mandatColumn.getValue().getXType()));

				localConnection.beginTransaction();
				mandatDAO.updateMandatCellValue(mandatCell);
				localConnection.commit();

				// create and rerturn response
				JSONObject payload = new JSONObject();
				payload.put("MandatCell", mandatCell.toJSONObject());
				return new AEResponse(payload);
			} else {
				throw new AEException("Internal Error: The CFC Column cannot be found.");
			}
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse closingMandatPeriod(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			/**
			 * prepare dao's
			 */
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);

			/**
			 * owner
			 */
			OrganizationDAO orgDAO = DAOFactory.getInstance().getOrganizationDAO(localConnection);
			AEDescriptor customerDescr = Organization.lazyDescriptor(aeRequest.getArguments().getLong("ownerId"));
			JSONArray customersArray = orgDAO.loadCustomer(customerDescr);
			JSONObject customer = customersArray.getJSONObject(0);
			customerDescr.setName(customer.optString("name"));

			/**
			 * determine period
			 */
			AccPeriod accPeriod = getFirstOpenPeriod(
					customerDescr.getID(), 
					AEApp.MANDAT_MODULE_ID, 
					localConnection);

			// we can close only finished periods
			Date nowDate = AEDateUtil.getClearDate(new Date());
			if(nowDate.before(AEDateUtil.getClearDate(accPeriod.getEndDate()))) {
				throw new AEException(
						(int) AEError.System.CANNOT_CLOSE_UNFINISHED.getSystemID(), 
				"La période n'est pas terminée.");
			}

			/**
			 * close the period
			 */
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
			try {
				// send e-mail
				Emailer emailer = new Emailer();
				emailer.onPeriodClosed(customerDescr, invContext, accPeriod, AEApp.MANDAT_MODULE_ID);

				// auto export
				JSONObject exportRequestArguments = new JSONObject();
				exportRequestArguments.put("accPeriodId", accPeriod.getID());
				if("secal.png".equalsIgnoreCase(invContext.getAuthPrincipal().getDescription())) {
					exportRequestArguments.put("exportTo", "CEGID");
				} else if("gianati.png".equalsIgnoreCase(invContext.getAuthPrincipal().getDescription())) {
					exportRequestArguments.put("exportTo", "SAGE");
				}
				exportRequestArguments.put("ownerId", aeRequest.getArguments().getLong("ownerId"));
				AERequest exportRequest = new AERequest(exportRequestArguments);
				accLocal.export(exportRequest, invContext);
			} catch (Throwable t) {
				t.printStackTrace();
			}

			/**
			 * return response
			 */
			JSONObject payload = new JSONObject();
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
	public AEResponse openMandatPeriod(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * determine period
			 */
			AccPeriod accPeriod = getLastClosedPeriod(
					aeRequest.getArguments().getLong("ownerId"), 
					AEApp.MANDAT_MODULE_ID, 
					localConnection);

			if(accPeriod != null) {
				localConnection.beginTransaction();

				/**
				 * delete general journal
				 */
				AccJournalDAO journalDAO = daoFactory.getAccJournalDAO(localConnection);
				journalDAO.deleteByAccPeriodId(accPeriod.getID());

				/**
				 * open the period
				 */
				AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
				accPeriodDAO.openPeriod(accPeriod.getID());
				accPeriodDAO.notExportedPeriod(accPeriod.getID());

				/**
				 * Delete attachments
				 */
				FileAttachmentDAO fileAttachmentDAO = daoFactory.getFileAttachmentDAO(localConnection);
				fileAttachmentDAO.deleteTo(accPeriod.getDescriptor());
			}

			/**
			 * commit transaction and return response
			 */
			localConnection.commit();

			/**
			 * return response
			 */
			JSONObject payload = new JSONObject();
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
	public CFCData loadMandatData(CFC mandat, AccPeriod accPeriod, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			/**
			 * prepare dao's
			 */
			MandatDAO mandatDAO = daoFactory.getMandatDAO(localConnection);
			CashModuleDAO cashModuleDAO = daoFactory.getCashModuleDAO(localConnection);

			/**
			 * local attributes
			 */
			CFCData mandatData = new CFCData();

			/**
			 * create spread sheet
			 */
			// load cash journal
			CashJournalEntriesList dbCashEntriesList =
				cashModuleDAO.loadJournalEntries(
						mandat.getCompany().getDescriptor().getID(), 
						accPeriod.getStartDate(), 
						accPeriod.getEndDate());

			// load saved cells
			ArrayList<CFCCell> cellsDBList = mandatDAO.loadMandatCells(
					mandat.getDescriptor(),
					AEDateUtil.addDaysToDate(accPeriod.getStartDate(), -1),
					accPeriod.getEndDate(),
					0);

			// create all spreadsheet and populate values regards model definitions
			AEDateIterator dateIterator = new AEDateIterator(accPeriod.getStartDate(), accPeriod.getEndDate());
			for (Date date : dateIterator) {
				// for every row
				CFCModel mandatModel = mandat.getCfcModel();
				for (CFCColumn column : mandatModel) {
					// for every column

					// process the cell (date, rowIndex, column)
					CFCCell cell = new CFCCell();
					cell.setEntryDate(AEDateUtil.getClearDateTime(date));
					cell.setColumn(column);
					cell.setToCFC(mandat.getDescriptor());
					cell.setRowIndex(0);
					cell.setColumnId(column.getID());

					// ensure the row where the cell should be inserted
					CFCRow row = mandatData.getRow(date, 0);
					if(row == null) {
						row = new CFCRow(date, 0);
						mandatData.add(row);
					}
					row.add(cell);

					// process meta data
					if(AEValue.isNull(column.getValue())) {
						continue;
					}
					if(CFCColumn.NType.VALUE.equals(column.getNType())) {
						// try to set already saved value
						CFCCell savedCell = getSavedCFCCell(
								cell.getEntryDate(), 
								cell.getRowIndex(), 
								cell.getColumnId(), 
								cellsDBList);
						if(savedCell != null) {
							cell.setValue(savedCell.getValue());
						} else {
							if(AEValue.XType.DOUBLE.equals(column.getValue().getXType())) {
								cell.setValue(new AEValue("0.0", AEValue.XType.DOUBLE));
							} else if(AEValue.XType.STRING.equals(column.getValue().getXType())) {
								cell.setValue(new AEValue(AEStringUtil.EMPTY_STRING, AEValue.XType.STRING));
							}
						}
					} else if(CFCColumn.NType.ACCOUNT.equals(column.getNType())) {
						// get the value from cashJournal for specified in the mdoel account 
						CashJournalEntry cjEntry = dbCashEntriesList.get(
								cell.getEntryDate(), 
								column.getValue().optLong());
						AEValue value = new AEValue();
						if(cjEntry != null) {
							Double amount = cjEntry.getAmount();
							if(amount != null) {
								value.set(cjEntry.getAmount());
							} else {
								value.set(0.0);
							}
						} else {
							value.set(0.0);
						}
						cell.setValue(value);
					} else if(CFCColumn.NType.ATTRIBUTE.equals(column.getNType())) {
						// get the value from cashJournal for specified in the mdoel account 
						Double attrVal = dbCashEntriesList.getAttributeSum(
								cell.getEntryDate(), 
								column.getValue().optLong());
						AEValue value = new AEValue();
						if(attrVal != null) {
							value.set(attrVal);
						} else {
							value.set(0.0);
						}
						cell.setValue(value);
					}
				}
			}

			/**
			 * Value and Formula are set.
			 * Evaluate formulas.
			 */
			for (CFCRow mandatRow : mandatData) {
				for (CFCCell mandatCell : mandatRow) {
					try {
						CFCColumn colModel = mandatCell.getColumn();
						if(colModel != null 
								&& (CFCColumn.NType.EXPRESSION.equals(colModel.getNType())
										|| CFCColumn.NType.EXPRESSION_COB.equals(colModel.getNType()))) {

							if(CFCColumn.NType.EXPRESSION_COB.equals(colModel.getNType())) {
								// the first date can be overriden
								if(AEDateUtil.areDatesEqual(accPeriod.getStartDate(), mandatRow.getDate())) {
									// check for saved cell
									CFCCell savedCell = getSavedCFCCell(
											mandatRow.getDate(), 
											0, 
											colModel.getID(), 
											cellsDBList);
									if(savedCell != null) {
										mandatCell.setValue(savedCell.getValue());
										continue;
									}
								}
							}

							// extract the expression
							String expression = colModel.getValue().getString();

							// configure parser to evaluate expression
							JEP parser = new JEP();
							parser.setAllowUndeclared(true);
							parser.parseExpression(expression);

							// iterate, detect and set parameters
							SymbolTable paramsTable = parser.getSymbolTable();
							@SuppressWarnings("unchecked")
							Set<String> params = paramsTable.keySet();
							for (String paramName : params) {
								double paramValue = 0.0;
								// param is regexpr C1, C2, ...., R1, R2, ...
								if(paramName.startsWith("C")) {
									int paramColIndex = Integer.parseInt(paramName.substring(1)) - 1;
									for (CFCCell paramCell: mandatRow) {
										if(paramCell.getColumn().getColIndex() == paramColIndex) {
											paramValue = paramCell.getValue().getDouble();
											paramValue = AEMath.round(paramValue, 2);
											break;
										}
									}
									parser.addVariable(paramName, paramValue);
								} else if(paramName.startsWith("R")) {
									int cIndex = paramName.indexOf('C');
									String rParam = paramName.substring(0, cIndex);
									String cParam = paramName.substring(cIndex);

									int rowOffset = -(Integer.parseInt(rParam.substring(1)));
									Date rowDay = AEDateUtil.addDaysToDate(mandatRow.getDate(), rowOffset);
									CFCRow calcRow = mandatData.getRow(rowDay, 0);
									if(calcRow != null) {
										int paramColIndex = Integer.parseInt(cParam.substring(1)) - 1;
										for (CFCCell paramCell: calcRow) {
											if(paramCell.getColumn().getColIndex() == paramColIndex) {
												paramValue = paramCell.getValue().getDouble();
												paramValue = AEMath.round(paramValue, 2);
												break;
											}
										}
									} else {
										// not in current period, find in saved values
										int paramColIndex = Integer.parseInt(cParam.substring(1)) - 1;
										for (CFCColumn col: mandat.getCfcModel()) {
											if(col.getColIndex() == paramColIndex) {
												CFCCell savedCell = getSavedCFCCell(
														rowDay, 
														0, 
														col.getID(), 
														cellsDBList);
												if(savedCell != null) {
													paramValue = savedCell.getValue().getDouble();
													paramValue = AEMath.round(paramValue, 2);
												}
												break;
											}
										}
									}
									parser.addVariable(paramName, paramValue);
								}
							}
							// vriables are set, so evaluate
							double result = parser.getValue();
							AEValue resultValue = new AEValue();
							resultValue.set(result);
							mandatCell.setValue(resultValue);
						}
					} catch (Throwable t) {
						mandatCell.setValue(new AEValue("#ERR"));
					}
				}
			}

			/** 
			 * Process sum
			 */
			CFCRow sumRow = new CFCRow(CFCRow.SUM);
			for (CFCRow row : mandatData) {
				for (CFCCell cell : row) {
					CFCCell sumCell = sumRow.getCell(cell.getColumnId());
					if(sumCell == null) {
						sumCell = new CFCCell(sumRow.getSysId());
						sumCell.setEntryDate(null);
						sumCell.setColumn(cell.getColumn());
						sumCell.setToCFC(mandat.getDescriptor());
						sumCell.setRowIndex(0);
						sumCell.setColumnId(cell.getColumnId());
						sumCell.setValue(new AEValue("0.0", AEValue.XType.DOUBLE));

						sumRow.add(sumCell);
					}
					sumCell.addDouble(cell.getValue());
				}
			}
			mandatData.add(sumRow);

			// rerturn response
			return mandatData;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadBankDocumentData(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			// validate caler
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// prepare
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);
			JSONObject arguments = aeRequest.getArguments();

			// companies
			JSONArray abstractAccArray = new JSONArray();

			// load suppliers
			JSONArray clientsArray = orgDAO.loadCompanies(arguments.getLong("ownerId"), 30);
			for(int i = 0; i < clientsArray.length(); i++) {
				JSONObject jsonComp = clientsArray.getJSONObject(i);
				jsonComp.put("groupId", "30");
				jsonComp.put("group", "--Fournisseur--");
				if(jsonComp.has("name")) {
					jsonComp.put("display", jsonComp.get("name"));
				}
				abstractAccArray.put(jsonComp);
			}

			// load suppliers
			JSONArray suppliersArray = orgDAO.loadCompanies(arguments.getLong("ownerId"), 10);
			for(int i = 0; i < suppliersArray.length(); i++) {
				JSONObject jsonComp = suppliersArray.getJSONObject(i);
				jsonComp.put("groupId", "10");
				jsonComp.put("group", "--Client--");
				if(jsonComp.has("name")) {
					jsonComp.put("display", jsonComp.get("name"));
				}
				abstractAccArray.put(jsonComp);
			}

			// load accounts
			JSONArray itemAccounts = accLocal.loadBankAccountsByOwner(
					arguments.getLong("ownerId"), 
					localConnection);
			for(int i = 0; i < itemAccounts.length(); i++) {
				JSONObject jsonComp = itemAccounts.getJSONObject(i);
				jsonComp.put("groupId", "1000");
				jsonComp.put("group", "--Compte--");
				if(jsonComp.has("description")) {
					jsonComp.put("display", jsonComp.get("description"));
				}
				jsonComp.put("id", -1 * jsonComp.getLong("id"));
				abstractAccArray.put(jsonComp);
			}

			// load bankAccounts
			BankAccountDAO bankAccDAO = daoFactory.getBankAccountDAO(localConnection);
			BankAccountsList banksList = bankAccDAO.load(Organization.lazyDescriptor(arguments.getLong("ownerId")), BankAccount.ENTRY_TYPE_MANUAL);

			// first open period
			AccPeriod accPeriod = getFirstOpenPeriod(
					aeRequest.getArguments().getLong("ownerId"), 
					AEApp.PAYMENT_MODULE_ID, 
					localConnection);

			// load bankDocuments
			AETradeDocumentDAO tDocDAO = daoFactory.getAETradeDocumentDAO(localConnection);
			AETradeDocumentResultsList bankDocuments = tDocDAO.loadBankDescriptors(
					arguments.getLong("ownerId"), 
					accPeriod.getStartDate(), 
					accPeriod.getEndDate());

			JSONObject payload = new JSONObject();
			payload.put("abstractAccounts", abstractAccArray);
			//			payload.put("accounts", itemAccounts);
			payload.put("bankAccounts", banksList.toJSONArray());
			payload.put("bankDocuments", bankDocuments.toJSONArray());
			payload.put("period", AEDateUtil.convertToString(accPeriod.getStartDate(), "MM/yyyy"));
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse closingBankDocumentPeriod(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			/**
			 * prepare dao's
			 */
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);

			/**
			 * owner
			 */
			OrganizationDAO orgDAO = DAOFactory.getInstance().getOrganizationDAO(localConnection);
			AEDescriptor customerDescr = Organization.lazyDescriptor(aeRequest.getArguments().getLong("ownerId"));
			JSONArray customersArray = orgDAO.loadCustomer(customerDescr);
			JSONObject customer = customersArray.getJSONObject(0);
			customerDescr.setName(customer.optString("name"));

			/**
			 * determine period
			 */
			AccPeriod accPeriod = getFirstOpenPeriod(
					customerDescr.getID(), 
					AEApp.PAYMENT_MODULE_ID, 
					localConnection);

			// we can close only finished periods
			Date nowDate = AEDateUtil.getClearDate(new Date());
			if(nowDate.before(AEDateUtil.getClearDate(accPeriod.getEndDate()))) {
				throw new AEException(
						(int) AEError.System.CANNOT_CLOSE_UNFINISHED.getSystemID(), 
				"La période n'est pas terminée.");
			}

			/**
			 * close the period
			 */
			accPeriodDAO.closePeriod(accPeriod.getID());

			/**
			 * commit transaction and return response 
			 */
			localConnection.commit();

			// send e-mail
			Emailer emailer = new Emailer();
			emailer.onPeriodClosed(customerDescr, invContext, accPeriod, AEApp.PAYMENT_MODULE_ID);

			/**
			 * return response
			 */
			JSONObject payload = new JSONObject();
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
	public AEResponse openBankDocumentPeriod(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * determine period
			 */
			AccPeriod accPeriod = getLastClosedPeriod(
					aeRequest.getArguments().getLong("ownerId"), 
					AEApp.PAYMENT_MODULE_ID, 
					localConnection);

			if(accPeriod != null) {
				localConnection.beginTransaction();

				/**
				 * delete general journal
				 */
				AccJournalDAO journalDAO = daoFactory.getAccJournalDAO(localConnection);
				journalDAO.deleteByAccPeriodId(accPeriod.getID());

				/**
				 * open the period
				 */
				AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
				accPeriodDAO.openPeriod(accPeriod.getID());
				accPeriodDAO.notExportedPeriod(accPeriod.getID());

				/**
				 * Delete attachments
				 */
				FileAttachmentDAO fileAttachmentDAO = daoFactory.getFileAttachmentDAO(localConnection);
				fileAttachmentDAO.deleteTo(accPeriod.getDescriptor());

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
			if(localConnection != null) {
				localConnection.rollback();
			}
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadBankDocument(AERequest aeRequest) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// get connection
			localConnection = daoFactory.getConnection();
			long docId = aeRequest.getArguments().getLong("id");
			AEDescriptor docDescr = AETradeDocument.lazyDescriptor(docId);

			// load document
			AEDocumentDAO docDAO = daoFactory.getAETradeDocumentDAO(localConnection);
			AEDocument aeDocument = docDAO.load(docDescr);

			// load items
			AEDocumentItemDAO itemDAO = daoFactory.getAEDocumentItemDAO(localConnection);
			aeDocument.setItems(itemDAO.load(docDescr));

			// return response
			JSONObject payload = new JSONObject();
			payload.put("document", aeDocument.toJSONObject());
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadUnpaidBankDocument(AERequest aeRequest,
			AEInvocationContext invContext, AEConnection aeConnection) throws AEException {

		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();

			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			// get connection
			localConnection = daoFactory.getConnection(aeConnection);
			long ownerId = aeRequest.getArguments().getLong("ownerId");
			long bankAccId = aeRequest.getArguments().getLong("bankAccId");

			// load bankDocuments
			AETradeDocumentDAO tDocDAO = daoFactory.getAETradeDocumentDAO(localConnection);
			AETradeDocumentResultsList bankDocuments = tDocDAO.loadUnpaidBankItems(ownerId, bankAccId);

			// return response
			JSONObject payload = new JSONObject();
			payload.put("bankDocuments", bankDocuments.toJSONArray());
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse payBankDocuments(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);
			AccountDAO accDAO = daoFactory.getAccountDAO(localConnection);

			/**
			 * local attributes
			 */
			long ownerId = aeRequest.getArguments().optLong("ownerId");
			long bankAccountId = aeRequest.getArguments().optLong("bankAccountId");
			String journalCode = aeRequest.getArguments().optString("journalCode");
			JSONArray payableItemsJSON = aeRequest.getArguments().optJSONArray("bankDocs");

			// load transactions
			BankTransactionsList bankTransactions = new BankTransactionsList();

			// load items
			AEDocumentItemDAO itemDAO = daoFactory.getAEDocumentItemDAO(localConnection);
			CFCDAO cfcDAO = daoFactory.getCFCDAO(localConnection);
			MandatDAO mandatDAO = daoFactory.getMandatDAO(localConnection);
			for(int i = 0; i < payableItemsJSON.length(); i++) {
				JSONObject payableItemJSON = payableItemsJSON.getJSONObject(i);

				long pItemType = payableItemJSON.optInt("docType", 0);
				if(pItemType == AEDocumentType.System.AEPrelevement.getSystemID()
						|| pItemType == AEDocumentType.System.AEVirement.getSystemID()
						|| pItemType == AEDocumentType.System.AECheque.getSystemID()
						|| pItemType == AEDocumentType.System.AELCR.getSystemID()) {

					// create document
					AETradeDocument bankDoc = new AETradeDocument();
					bankDoc.create(payableItemJSON);

					// load items
					AEDocumentItemsList itemsList = itemDAO.load(bankDoc.getDescriptor());
					for (AEDocumentItem aeDocumentItem : itemsList) {
						// check for paid
						if(aeDocumentItem.isPaid()) {
							continue;
						}

						BankTransaction bt = new BankTransaction();
						bt.setRecognised(true);

						bt.setCompany(Organization.lazyDescriptor(ownerId));

						bt.setBankAccount(BankAccount.lazyDescriptor(bankAccountId));

						AEDescriptor journal = new AEDescriptorImp();
						journal.setCode(journalCode);
						bt.setJournal(journal);

						// private AEDescriptor accPeriod;

						// private AEDescriptor statementRef;

						//						bt.setDate(new Date());

						if(aeDocumentItem.getAccount() != null) {
							bt.setAccount(aeDocumentItem.getAccount().getDescriptor());
						}

						if(aeDocumentItem.getParty() != null) {
							bt.setAuxiliary(aeDocumentItem.getParty().getDescriptor());
						}

						bt.setDescription(aeDocumentItem.getDescription());

						// reference
						AEDocumentDescriptor refDescr = new AEDocumentDescriptorImp();
						refDescr.setID(aeDocumentItem.getID());
						refDescr.setNumber(bankDoc.getNumberString());
						refDescr.setDate(bankDoc.getDate());
						refDescr.setDocumentType(bankDoc.getType());
						refDescr.setClazz(DomainClass.AEInvoiceItem);
						String docType = AEStringUtil.EMPTY_STRING;
						if(bankDoc.getType() != null) {
							if(bankDoc.getType().getSystemID() == AEDocumentType.System.AEVirement.getSystemID()) {
								docType = "Virement";
							} else if(bankDoc.getType().getSystemID() == AEDocumentType.System.AEPrelevement.getSystemID()) {
								docType = "Prélèvement";
							} else if(bankDoc.getType().getSystemID() == AEDocumentType.System.AECheque.getSystemID()) {
								docType = "Chèque";
							} else if(bankDoc.getType().getSystemID() == AEDocumentType.System.AELCR.getSystemID()) {
								docType = "LCR";
							}
						}
						refDescr.setDescription(
								docType + " "
								+ bankDoc.getNumberString() 
								+ ", " 
								+ AEDateUtil.convertToString(bankDoc.getDate(), AEDateUtil.FRENCH_DATE_FORMAT));
						bt.setReference(refDescr);

						// private AEDescriptor currency;

						// amount
						Double amount = new Double(aeDocumentItem.getAmount());
						if(AEDocumentItem.ACC_TYPE_DEBIT.equalsIgnoreCase(aeDocumentItem.getAccType())) {
							bt.setDtAmount(null);
							bt.setCtAmount(amount);
						} else if(AEDocumentItem.ACC_TYPE_CREDIT.equalsIgnoreCase(aeDocumentItem.getAccType())) {
							bt.setDtAmount(amount);
							bt.setCtAmount(null);
						} else {
							bt.setDtAmount(null);
							bt.setCtAmount(null);
						}

						bankTransactions.add(bt);
					}
				} else if(pItemType == AEDocumentType.System.CFC.getSystemID()) {
					CFCCell cfcCell = cfcDAO.loadPayableItem(CFCCell.lazyDescriptorCFC(payableItemJSON.optLong("id")));
					if(cfcCell == null || cfcCell.isPaid()) {
						continue;
					}
					BankTransaction bt = new BankTransaction();
					bt.setRecognised(true);

					bt.setCompany(Organization.lazyDescriptor(ownerId));

					bt.setBankAccount(BankAccount.lazyDescriptor(bankAccountId));

					AEDescriptor journal = new AEDescriptorImp();
					journal.setCode(journalCode);
					bt.setJournal(journal);

					// private AEDescriptor accPeriod;

					// private AEDescriptor statementRef;

					//					bt.setDate(new Date());

					bt.setAccount(cfcCell.getAccBank());

					bt.setDescription(cfcCell.getName());

					// reference
					AEDocumentDescriptor refDescr = new AEDocumentDescriptorImp();
					refDescr.setID(cfcCell.getID());
					refDescr.setClazz(DomainClass.CFCCell);
					refDescr.setDocumentType(AEDocumentType.valueOf(AEDocumentType.System.CFC));
					refDescr.setDescription(
							AEDateUtil.convertToString(cfcCell.getEntryDate(), AEDateUtil.FRENCH_DATE_FORMAT)
							+ ", CFC, " + cfcCell.getName());
					bt.setReference(refDescr);

					// private AEDescriptor currency;

					// amount
					Double amount = 0.0;
					if(!AEValue.isNull(cfcCell.getValue())) {
						amount = cfcCell.getValue().optDouble();
					}
					if(amount > 0.0) {
						bt.setDtAmount(Math.abs(amount));
						bt.setCtAmount(null);
					} else if (amount < 0.0) {
						bt.setDtAmount(null);
						bt.setCtAmount(Math.abs(amount));
					} else {
						bt.setDtAmount(null);
						bt.setCtAmount(null);
					}

					bankTransactions.add(bt);
				} else if(pItemType == AEDocumentType.System.MANDAT.getSystemID()) {
					CFCCell mandatCell = mandatDAO.loadPayableItem(CFCCell.lazyDescriptorMandat(payableItemJSON.optLong("id")));
					if(mandatCell == null || mandatCell.isPaid()) {
						continue;
					}
					BankTransaction bt = new BankTransaction();
					bt.setRecognised(true);

					bt.setCompany(Organization.lazyDescriptor(ownerId));

					bt.setBankAccount(BankAccount.lazyDescriptor(bankAccountId));

					AEDescriptor journal = new AEDescriptorImp();
					journal.setCode(journalCode);
					bt.setJournal(journal);

					// private AEDescriptor accPeriod;

					// private AEDescriptor statementRef;

					//					bt.setDate(new Date());

					bt.setAccount(mandatCell.getAccBank());

					bt.setDescription(mandatCell.getName());

					// reference
					AEDocumentDescriptor refDescr = new AEDocumentDescriptorImp();
					refDescr.setID(mandatCell.getID());
					refDescr.setClazz(DomainClass.MandatCell);
					refDescr.setDocumentType(AEDocumentType.valueOf(AEDocumentType.System.MANDAT));
					refDescr.setDescription(
							AEDateUtil.convertToString(mandatCell.getEntryDate(), AEDateUtil.FRENCH_DATE_FORMAT)
							+ ", Mandat, " + mandatCell.getName());
					bt.setReference(refDescr);

					// private AEDescriptor currency;

					// amount
					Double amount = 0.0;
					if(!AEValue.isNull(mandatCell.getValue())) {
						amount = mandatCell.getValue().optDouble();
					}
					if(amount < 0.0) {
						bt.setDtAmount(Math.abs(amount));
						bt.setCtAmount(null);
					} else if (amount > 0.0) {
						bt.setDtAmount(null);
						bt.setCtAmount(Math.abs(amount));
					} else {
						bt.setDtAmount(null);
						bt.setCtAmount(null);
					}

					bankTransactions.add(bt);
				} else if(pItemType == AEDocumentType.System.AESaleInvoice.getSystemID()) {
					AETradeDocument saleDoc = (AETradeDocument) docService.loadHeader(
							new AEDocumentDescriptorImp(
									payableItemJSON.optLong("id"), 
									AEDocumentType.valueOf(AEDocumentType.System.AESaleInvoice)),
									invContext,
									localConnection);
					if(saleDoc == null || saleDoc.isPaid()) {
						continue;
					}
					BankTransaction bt = new BankTransaction();
					bt.setRecognised(true);

					bt.setCompany(Organization.lazyDescriptor(ownerId));

					bt.setBankAccount(BankAccount.lazyDescriptor(bankAccountId));

					AEDescriptor journal = new AEDescriptorImp();
					journal.setCode(journalCode);
					bt.setJournal(journal);

					// private AEDescriptor accPeriod;

					// private AEDescriptor statementRef;

					//					bt.setDate(new Date());

					AccAccount partyAcc = null;
					AEDescriptive partyDescr = null;
					if(saleDoc.getRecipient() != null) {
						partyDescr = orgDAO.loadDescriptive(saleDoc.getRecipient().getDescriptor().getID());
						if(partyDescr != null) {
							partyAcc = orgDAO.loadAccAccount(partyDescr.getDescriptor());
						}
					} else if(saleDoc.getRecipientAcc()  != null) {
						partyAcc = accDAO.loadById(saleDoc.getRecipientAcc().getDescriptor());
					}
					if(partyAcc == null) {
						throw new AEException(
								(int) AEError.System.INVOICE_THERE_IS_NO_PARTY_ACCOUNT.getSystemID(),
								saleDoc.getDescriptor().getDescription() 
								+ " - Code d'erreur: " 
								+ AEError.System.INVOICE_THERE_IS_NO_PARTY_ACCOUNT.getSystemID());
					}
					bt.setAccount(partyAcc.getDescriptor());

					bt.setDescription(saleDoc.getDescription());

					// reference
					bt.setReference((AEDocumentDescriptor)saleDoc.getDescriptor());

					// private AEDescriptor currency;

					// amount
					Double amount = saleDoc.getAmount();
					if(amount > 0.0) {
						bt.setDtAmount(Math.abs(amount));
						bt.setCtAmount(null);
					} else if (amount < 0.0) {
						bt.setDtAmount(null);
						bt.setCtAmount(Math.abs(amount));
					} else {
						bt.setDtAmount(null);
						bt.setCtAmount(null);
					}

					bankTransactions.add(bt);
				}
			}

			// create and rerturn response
			JSONObject payload = new JSONObject();
			payload.put("items", bankTransactions.toJSONArray());
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse recognizeBankTransactions(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);

			/**
			 * Parameters
			 */
			long ownerId = aeRequest.getArguments().getLong("ownerId");
			long bankAccountId = aeRequest.getArguments().getLong("bankAccountId");
			String journalCode = aeRequest.getArguments().optString("journalCode");
			JSONArray items = aeRequest.getArguments().optJSONArray("items");
			if(items == null) {
				items = new JSONArray();
			}

			// tiers
			/**
			 * suppliers
			 */
			JSONArray suppliersArray = orgDAO.loadCompanies(ownerId, 30);


			/**
			 * clients
			 */
			JSONArray clientsArray = orgDAO.loadCompanies(ownerId, 10);

			/**
			 * unpaid documents
			 */
			AETradeDocumentDAO tDocDAO = daoFactory.getAETradeDocumentDAO(localConnection);
			AEDocumentItemDAO itemDAO = daoFactory.getAEDocumentItemDAO(localConnection);
			AETradeDocumentResultsList bankDocuments = tDocDAO.loadUnpaidBankItems(ownerId, bankAccountId);

			BankTransactionsList btList = new BankTransactionsList();
			for (int i = 0; i < items.length(); i++) {
				JSONObject bankTranJSON = items.getJSONObject(i);

				BankTransaction bt = new BankTransaction();
				bt.create(bankTranJSON);

				bt.setCompany(Organization.lazyDescriptor(ownerId));
				bt.setBankAccount(BankAccount.lazyDescriptor(bankAccountId));
				AEDescriptor journal = new AEDescriptorImp();
				journal.setCode(journalCode);
				bt.setJournal(journal);


				// recognition
				if(bt.getDtAmount() != null && bt.getDtAmount() != 0.0) {
					// client
					for (int j = 0; j < clientsArray.length(); j++) {
						JSONObject client = clientsArray.getJSONObject(j);

						// recognition
						String regex = getRegex(client);
						if(!AEStringUtil.isEmpty(regex)) {
							Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
							Matcher matcher = pattern.matcher(bt.getDescription());
							if(matcher.find()) {
								setAccounts(bt, client);
							}
						}
					}
				} else if(bt.getCtAmount() != null && bt.getCtAmount() != 0.0) {
					// supplier
					if("01".equals(bt.getCodeOperation())) {
						// find cheque
						String chequeNumber = null;
						if(bt.getReference() != null
								&& !AEStringUtil.isEmpty(bt.getReference().getDescription())) {
							chequeNumber = bt.getReference().getDescription().trim();
							for (AETradeDocumentResult aeTradeDocumentResult : bankDocuments) {
								if(aeTradeDocumentResult.getTradeDocument() != null
										&& !AEStringUtil.isEmpty(aeTradeDocumentResult.getTradeDocument().getNumberString())
										&& aeTradeDocumentResult.getTradeDocument().getType() != null 
										&& aeTradeDocumentResult.getTradeDocument().getType().getSystemID() == AEDocumentType.System.AECheque.getSystemID()) {

									if(chequeNumber.equals(aeTradeDocumentResult.getTradeDocument().getNumberString().trim())) {
										AETradeDocument bankDoc = aeTradeDocumentResult.getTradeDocument();
										AEDocumentItemsList itemsList = itemDAO.load(bankDoc.getDescriptor());
										for (AEDocumentItem aeDocumentItem : itemsList) {
											// check for paid
											if(aeDocumentItem.isPaid()) {
												continue;
											}

											if(aeDocumentItem.getAccount() != null) {
												bt.setAccount(aeDocumentItem.getAccount().getDescriptor());
											}

											if(aeDocumentItem.getParty() != null) {
												bt.setAuxiliary(aeDocumentItem.getParty().getDescriptor());
											}

											// reference
											AEDocumentDescriptor refDescr = new AEDocumentDescriptorImp();
											refDescr.setID(aeDocumentItem.getID());
											refDescr.setNumber(bankDoc.getNumberString());
											refDescr.setDate(bankDoc.getDate());
											refDescr.setDocumentType(bankDoc.getType());
											refDescr.setClazz(DomainClass.AEInvoiceItem);
											String docType = AEStringUtil.EMPTY_STRING;
											if(bankDoc.getType() != null) {
												if(bankDoc.getType().getSystemID() == AEDocumentType.System.AEVirement.getSystemID()) {
													docType = "Virement";
												} else if(bankDoc.getType().getSystemID() == AEDocumentType.System.AEPrelevement.getSystemID()) {
													docType = "Prélèvement";
												} else if(bankDoc.getType().getSystemID() == AEDocumentType.System.AECheque.getSystemID()) {
													docType = "Chèque";
												} else if(bankDoc.getType().getSystemID() == AEDocumentType.System.AELCR.getSystemID()) {
													docType = "LCR";
												}
											}
											refDescr.setDescription(
													docType + " "
													+ bankDoc.getNumberString() 
													+ ", " 
													+ AEDateUtil.convertToString(bankDoc.getDate(), AEDateUtil.FRENCH_DATE_FORMAT));
											bt.setReference(refDescr);
										}
									}
								}
							}
						}
					} else {
						for (int j = 0; j < suppliersArray.length(); j++) {
							JSONObject supplier = suppliersArray.getJSONObject(j);

							// recognition
							String regex = getRegex(supplier);
							if(!AEStringUtil.isEmpty(regex)) {
								Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
								Matcher matcher = pattern.matcher(bt.getDescription());
								if(matcher.find()) {
									setAccounts(bt, supplier);
								}
							}
						}
					}
				}

				btList.add(bt);
			}

			/**
			 * return response
			 */
			JSONObject payload = new JSONObject();
			payload.put("items", btList.toJSONArray());
			return new AEResponse(payload);
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private String getRegex(JSONObject jsonObject) {
		String regex = null;
		if(jsonObject.has("note") && !AEStringUtil.isEmpty(jsonObject.optString("note"))) {
			regex = jsonObject.optString("note");
		} else if(jsonObject.has("code") && !AEStringUtil.isEmpty(jsonObject.optString("code"))) {
			regex = "\\b" + jsonObject.optString("code") + "\\b";
		}
		return regex;
	}

	private void setAccounts(BankTransaction bt, JSONObject party) {
		if(party.has("compteGeneralId")) {
			bt.setAccount(AccAccount.lazyDescriptor(party.optLong("compteGeneralId")));
		}

		AEDescriptor auxDescr = Organization.lazyDescriptor(party.optLong("id"));
		auxDescr.setCode(party.optString("compteAuxiliare"));
		bt.setAuxiliary(auxDescr);
	}

	@Override
	public AEResponse reconcileBankDocuments(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);
			AccountDAO accDAO = daoFactory.getAccountDAO(localConnection);

			/**
			 * local attributes
			 */
			long ownerId = aeRequest.getArguments().optLong("ownerId");
			long bankAccountId = aeRequest.getArguments().optLong("bankAccountId");
			String journalCode = aeRequest.getArguments().optString("journalCode");
			JSONObject btJSON = aeRequest.getArguments().optJSONObject("bankTransaction");
			JSONObject payableItemJSON = aeRequest.getArguments().optJSONObject("payableDoc");

			BankTransaction bt = new BankTransaction();
			bt.create(btJSON);

			// load items
			AEDocumentItemDAO itemDAO = daoFactory.getAEDocumentItemDAO(localConnection);
			CFCDAO cfcDAO = daoFactory.getCFCDAO(localConnection);
			MandatDAO mandatDAO = daoFactory.getMandatDAO(localConnection);

			long pItemType = payableItemJSON.optInt("docType", 0);
			if(pItemType == AEDocumentType.System.AEPrelevement.getSystemID()
					|| pItemType == AEDocumentType.System.AEVirement.getSystemID()
					|| pItemType == AEDocumentType.System.AECheque.getSystemID()
					|| pItemType == AEDocumentType.System.AELCR.getSystemID()) {

				// create document
				AETradeDocument bankDoc = new AETradeDocument();
				bankDoc.create(payableItemJSON);

				// load items
				AEDocumentItemsList itemsList = itemDAO.load(bankDoc.getDescriptor());
				for (AEDocumentItem aeDocumentItem : itemsList) {
					// check for paid
					if(aeDocumentItem.isPaid()) {
						continue;
					}

					bt.setCompany(Organization.lazyDescriptor(ownerId));

					bt.setBankAccount(BankAccount.lazyDescriptor(bankAccountId));

					AEDescriptor journal = new AEDescriptorImp();
					journal.setCode(journalCode);
					bt.setJournal(journal);

					// private AEDescriptor accPeriod;

					// private AEDescriptor statementRef;

					//						bt.setDate(new Date());

					if(aeDocumentItem.getAccount() != null) {
						bt.setAccount(aeDocumentItem.getAccount().getDescriptor());
					}

					if(aeDocumentItem.getParty() != null) {
						bt.setAuxiliary(aeDocumentItem.getParty().getDescriptor());
					}

					bt.setDescription(aeDocumentItem.getDescription());

					// reference
					AEDocumentDescriptor refDescr = new AEDocumentDescriptorImp();
					refDescr.setID(aeDocumentItem.getID());
					refDescr.setNumber(bankDoc.getNumberString());
					refDescr.setDate(bankDoc.getDate());
					refDescr.setDocumentType(bankDoc.getType());
					refDescr.setClazz(DomainClass.AEInvoiceItem);
					String docType = AEStringUtil.EMPTY_STRING;
					if(bankDoc.getType() != null) {
						if(bankDoc.getType().getSystemID() == AEDocumentType.System.AEVirement.getSystemID()) {
							docType = "Virement";
						} else if(bankDoc.getType().getSystemID() == AEDocumentType.System.AEPrelevement.getSystemID()) {
							docType = "Prélèvement";
						} else if(bankDoc.getType().getSystemID() == AEDocumentType.System.AECheque.getSystemID()) {
							docType = "Chèque";
						} else if(bankDoc.getType().getSystemID() == AEDocumentType.System.AELCR.getSystemID()) {
							docType = "LCR";
						}
					}
					refDescr.setDescription(
							docType + " "
							+ bankDoc.getNumberString() 
							+ ", " 
							+ AEDateUtil.convertToString(bankDoc.getDate(), AEDateUtil.FRENCH_DATE_FORMAT));
					bt.setReference(refDescr);

					// private AEDescriptor currency;

					// amount
					Double amount = new Double(aeDocumentItem.getAmount());
					if(AEDocumentItem.ACC_TYPE_DEBIT.equalsIgnoreCase(aeDocumentItem.getAccType())) {
						bt.setDtAmount(null);
						bt.setCtAmount(amount);
					} else if(AEDocumentItem.ACC_TYPE_CREDIT.equalsIgnoreCase(aeDocumentItem.getAccType())) {
						bt.setDtAmount(amount);
						bt.setCtAmount(null);
					} else {
						bt.setDtAmount(null);
						bt.setCtAmount(null);
					}

				}
			} else if(pItemType == AEDocumentType.System.CFC.getSystemID()) {
				CFCCell cfcCell = cfcDAO.loadPayableItem(CFCCell.lazyDescriptorCFC(payableItemJSON.optLong("id")));
				//				if(cfcCell == null || cfcCell.isPaid()) {
				//					continue;
				//				}

				bt.setCompany(Organization.lazyDescriptor(ownerId));

				bt.setBankAccount(BankAccount.lazyDescriptor(bankAccountId));

				AEDescriptor journal = new AEDescriptorImp();
				journal.setCode(journalCode);
				bt.setJournal(journal);

				// private AEDescriptor accPeriod;

				// private AEDescriptor statementRef;

				//					bt.setDate(new Date());

				bt.setAccount(cfcCell.getAccBank());

				bt.setDescription(cfcCell.getName());

				// reference
				AEDocumentDescriptor refDescr = new AEDocumentDescriptorImp();
				refDescr.setID(cfcCell.getID());
				refDescr.setClazz(DomainClass.CFCCell);
				refDescr.setDocumentType(AEDocumentType.valueOf(AEDocumentType.System.CFC));
				refDescr.setDescription(
						AEDateUtil.convertToString(cfcCell.getEntryDate(), AEDateUtil.FRENCH_DATE_FORMAT)
						+ ", CFC, " + cfcCell.getName());
				bt.setReference(refDescr);

				// private AEDescriptor currency;

				// amount
				Double amount = 0.0;
				if(!AEValue.isNull(cfcCell.getValue())) {
					amount = cfcCell.getValue().optDouble();
				}
				if(amount > 0.0) {
					bt.setDtAmount(amount);
					bt.setCtAmount(null);
				} else if (amount < 0.0) {
					bt.setDtAmount(null);
					bt.setCtAmount(Math.abs(amount));
				} else {
					bt.setDtAmount(null);
					bt.setCtAmount(null);
				}

			} else if(pItemType == AEDocumentType.System.MANDAT.getSystemID()) {
				CFCCell mandatCell = mandatDAO.loadPayableItem(CFCCell.lazyDescriptorMandat(payableItemJSON.optLong("id")));
				//				if(mandatCell == null || mandatCell.isPaid()) {
				//					continue;
				//				}

				bt.setCompany(Organization.lazyDescriptor(ownerId));

				bt.setBankAccount(BankAccount.lazyDescriptor(bankAccountId));

				AEDescriptor journal = new AEDescriptorImp();
				journal.setCode(journalCode);
				bt.setJournal(journal);

				// private AEDescriptor accPeriod;

				// private AEDescriptor statementRef;

				//					bt.setDate(new Date());

				bt.setAccount(mandatCell.getAccBank());

				bt.setDescription(mandatCell.getName());

				// reference
				AEDocumentDescriptor refDescr = new AEDocumentDescriptorImp();
				refDescr.setID(mandatCell.getID());
				refDescr.setClazz(DomainClass.MandatCell);
				refDescr.setDocumentType(AEDocumentType.valueOf(AEDocumentType.System.MANDAT));
				refDescr.setDescription(
						AEDateUtil.convertToString(mandatCell.getEntryDate(), AEDateUtil.FRENCH_DATE_FORMAT)
						+ ", Mandat, " + mandatCell.getName());
				bt.setReference(refDescr);

				// private AEDescriptor currency;

				// amount
				Double amount = 0.0;
				if(!AEValue.isNull(mandatCell.getValue())) {
					amount = mandatCell.getValue().optDouble();
				}
				if(amount > 0.0) {
					bt.setDtAmount(amount);
					bt.setCtAmount(null);
				} else if (amount < 0.0) {
					bt.setDtAmount(null);
					bt.setCtAmount(Math.abs(amount));
				} else {
					bt.setDtAmount(null);
					bt.setCtAmount(null);
				}
			} else if(pItemType == AEDocumentType.System.AESaleInvoice.getSystemID()) {
				AETradeDocument saleDoc = (AETradeDocument) docService.loadHeader(
						new AEDocumentDescriptorImp(
								payableItemJSON.optLong("id"), 
								AEDocumentType.valueOf(AEDocumentType.System.AESaleInvoice)),
								invContext,
								localConnection);
				//				if(saleDoc == null || saleDoc.isPaid()) {
				//					continue;
				//				}

				bt.setCompany(Organization.lazyDescriptor(ownerId));

				bt.setBankAccount(BankAccount.lazyDescriptor(bankAccountId));

				AEDescriptor journal = new AEDescriptorImp();
				journal.setCode(journalCode);
				bt.setJournal(journal);

				// private AEDescriptor accPeriod;

				// private AEDescriptor statementRef;

				//				bt.setDate(new Date());

				AccAccount partyAcc = null;
				AEDescriptive partyDescr = null;
				if(saleDoc.getRecipient() != null) {
					partyDescr = orgDAO.loadDescriptive(saleDoc.getRecipient().getDescriptor().getID());
					if(partyDescr != null) {
						partyAcc = orgDAO.loadAccAccount(partyDescr.getDescriptor());
					}
				} else if(saleDoc.getRecipientAcc()  != null) {
					partyAcc = accDAO.loadById(saleDoc.getRecipientAcc().getDescriptor());
				}
				if(partyAcc == null) {
					throw new AEException(
							(int) AEError.System.INVOICE_THERE_IS_NO_PARTY_ACCOUNT.getSystemID(),
							saleDoc.getDescriptor().getDescription() 
							+ " - Code d'erreur: " 
							+ AEError.System.INVOICE_THERE_IS_NO_PARTY_ACCOUNT.getSystemID());
				}
				bt.setAccount(partyAcc.getDescriptor());

				bt.setDescription(saleDoc.getDescription());

				// reference
				bt.setReference((AEDocumentDescriptor)saleDoc.getDescriptor());

				// private AEDescriptor currency;

				// amount
				Double amount = saleDoc.getAmount();
				if(amount > 0.0) {
					bt.setDtAmount(Math.abs(amount));
					bt.setCtAmount(null);
				} else if (amount < 0.0) {
					bt.setDtAmount(null);
					bt.setCtAmount(Math.abs(amount));
				} else {
					bt.setDtAmount(null);
					bt.setCtAmount(null);
				}
			}

			// create and rerturn response
			JSONObject payload = new JSONObject();
			payload.put("item", bt.toJSONObject());
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/**
	 * Creates general journal entries:
	 * - for not accounted expenses cash items;
	 * - balance items
	 * One entry per day
	 */
	@Override
	public AEResponse createCashJournalItems(AERequest aeRequest, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Connection to DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			/**
			 * Dates
			 */
			AccPeriod accPeriod = (AccPeriod) aeRequest.getArguments().get("accPeriod");
			Date startDate = AEDateUtil.getClearDate(accPeriod.getStartDate());	
			Date endDate = AEDateUtil.getClearDate(accPeriod.getEndDate());

			/**
			 * Customer
			 */
			JSONObject customer = aeRequest.getArguments().getJSONObject("customerJSON");
			AEDescriptor customerDescr = Organization.lazyDescriptor(AEPersistentUtil.NEW_ID);
			customerDescr.create(customer);

			/**
			 * Accounts
			 */
			JSONArray accJSONArray = aeRequest.getArguments().optJSONArray("accJSONArray");
			Map<String, JSONObject> mapByCode = AccAccount.indexByCode(accJSONArray);

			JSONObject cashAccount = 
				(JSONObject) mapByCode.get(customer.optString("defaultCashAccCode").trim());

			JSONObject adjPositiveAccount = 
				(JSONObject) mapByCode.get(customer.optString("defaultCashAdjAccCode").trim());

			JSONObject adjNegativeAccount = 
				(JSONObject) mapByCode.get(customer.optString("defaultCashAdjNegAccCode").trim());

			JSONObject diversAccount = 
				(JSONObject) mapByCode.get(customer.optString(Organization.JSONKey.defaultDiversAccCode).trim());

			if(cashAccount == null || adjPositiveAccount == null || adjNegativeAccount == null) {
				throw new AEException("S'il vous plaît vérifier l'installation par défaut du compte.");
			}

			/**
			 * Journal
			 */
			AEDescriptor journalDescr = (AEDescriptor) aeRequest.getArguments().get("journalDescr");

			/**
			 * Journal entries, one entry per date
			 */
			AccJournalEntriesList assetsJournalEntries = 
				(AccJournalEntriesList) aeRequest.getArguments().get("assetsJournalEntries");
			AccJournalEntriesList expensesJournalEntries = 
				(AccJournalEntriesList) aeRequest.getArguments().get("expensesJournalEntries");

			/**
			 * load not accounted expenses cash journal entries for the period without transaction
			 */	
			CashModuleDAO cashModuleDAO = daoFactory.getCashModuleDAO(localConnection);
			CashJournalEntriesList forAccountingListAll = 
				cashModuleDAO.loadNotAccountedExpensesJournalEntriesLazzy(customer.getLong("id"), startDate, endDate);
			if(!AECollectionUtil.isEmpty(forAccountingListAll)) {
				/**
				 * Create journal entries
				 */
				Map<Long, JSONObject> mapById = AccAccount.indexById(accJSONArray);

				// group by entry date
				Map<Date, CashJournalEntriesList> mapForAccountingList = 
					CashJournalEntriesList.groupByDate(forAccountingListAll);
				Set<Date> setForAccountingDates = mapForAccountingList.keySet();
				List<Date> listForAccountingDates = new ArrayList<Date>(setForAccountingDates);
				Collections.sort(listForAccountingDates);
				for (Date date : listForAccountingDates) {
					CashJournalEntriesList forAccountingList = mapForAccountingList.get(date);
					AccJournalEntry assetsJournalEntry = new AccJournalEntry();
					AccJournalEntry expensesJournalEntry = new AccJournalEntry();
					Date accDate = date;

					// process forAccountingList, not accounted for accDate
					for (Iterator<CashJournalEntry> iterator = forAccountingList.iterator(); iterator.hasNext();) {
						CashJournalEntry cashJournalEntry = (CashJournalEntry) iterator.next();
						JSONObject accJSON = mapById.get(cashJournalEntry.getAccId());
						if(accJSON == null || Math.abs(cashJournalEntry.getAmountAccounting()) < 0.005) {
							// this entry cannot be accounted
							continue;
						}

						AEDescriptor accDescr = new AEDescriptorImp();
						accDescr.create(accJSON);
						switch(cashJournalEntry.getEntryType()) {
						case 20: {
							// create items for this expense
							AccJournalItemsList entriesList = createCashExpensesAccJournalItems(
									diversAccount, 
									customerDescr, 
									cashJournalEntry, 
									accDate, 
									accPeriod, 
									accJSON, 
									journalDescr, 
									cashModuleDAO);
							expensesJournalEntry.addItems(entriesList);
							break;
						}
						default: {
							// the item is not created so remove from the list
							iterator.remove();
							forAccountingListAll.remove(expensesJournalEntry);
							break;
						}
						}
					}

					/**
					 * Create balance items for expensesJournalEntry and assetsJournalEntry
					 */
					Double cashCtAmount = expensesJournalEntry.calcDtAmount();
					if(Math.abs(AEMath.doubleValue(cashCtAmount)) >= 0.005) {
						AccJournalItem expensesCashItem = new AccJournalItem(accPeriod);
						expensesCashItem.setCompany(customerDescr);
						expensesCashItem.setJournal(journalDescr);
						expensesCashItem.setDate(accDate);
						expensesCashItem.setAccount(new AEDescriptorImp(cashAccount.getLong("id")));
						expensesCashItem.setDescription(cashAccount.optString("name"));
						expensesCashItem.setCtAmount(cashCtAmount);
						expensesJournalEntry.addItem(expensesCashItem);
					}

					Double cashDtAmount = assetsJournalEntry.calcCtAmount();
					if(Math.abs(AEMath.doubleValue(cashDtAmount)) >= 0.005) {
						AccJournalItem assetsCashItem = new AccJournalItem(accPeriod);
						assetsCashItem.setCompany(customerDescr);
						assetsCashItem.setJournal(journalDescr);
						assetsCashItem.setDate(accDate);
						assetsCashItem.setAccount(new AEDescriptorImp(cashAccount.getLong("id")));
						assetsCashItem.setDescription(cashAccount.optString("name"));
						assetsCashItem.setDtAmount(cashDtAmount);
						assetsJournalEntry.addItem(assetsCashItem);
					}

					// add entries to the lists
					assetsJournalEntries.add(assetsJournalEntry);
					expensesJournalEntries.add(expensesJournalEntry);
				}
			}

			JSONObject payload = new JSONObject();
			payload.put("forAccountingList", (Object) forAccountingListAll);
			return new AEResponse(payload);

		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse accountCashSubPeriod(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		logger().debug("Enter ...");
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * 
			 */
			long ownerId = aeRequest.getArguments().getLong("ownerId");

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));

			/**
			 * Customer and customer description
			 */
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);
			AEDescriptor customerDescr = Organization.lazyDescriptor(ownerId);
			JSONArray customersArray = orgDAO.loadCustomer(customerDescr);
			JSONObject customer = customersArray.getJSONObject(0);
			customerDescr.create(customer);
			logger().debug("Accounting " + customerDescr.getName() + " ...");

			/**
			 * Cash accounting period
			 */
			AccPeriod accPeriod = getFirstOpenPeriod(
					customer.getLong("id"), 
					AEApp.CASH_MODULE_ID, 
					localConnection);

			/**
			 * Customer's accounts
			 */
			AccountDAO accDAO = daoFactory.getAccountDAO(localConnection);
			JSONArray accJSONArray = accDAO.loadSubAccountsByOwner(customer.getLong("id"));

			/**
			 * Journal Entries
			 */
			AccJournalEntriesList assetsJournalEntries = new AccJournalEntriesList();
			AccJournalEntriesList expensesJournalEntries = new AccJournalEntriesList();

			/**
			 * Journal description
			 */
			AEDescriptorImp journalDescr = new AEDescriptorImp();
			journalDescr.setCode("CAI");

			/**
			 * Create accounting entries, one entry per days
			 */
			aeRequest.getArguments().put("assetsJournalEntries", (Object) assetsJournalEntries);
			aeRequest.getArguments().put("expensesJournalEntries", (Object) expensesJournalEntries);
			aeRequest.getArguments().put("journalDescr", journalDescr);
			aeRequest.getArguments().put("accJSONArray", accJSONArray);
			aeRequest.getArguments().put("accPeriod", accPeriod);
			aeRequest.getArguments().put("customerJSON", customer);

			AEResponse forAccountingResponse = createCashJournalItems(aeRequest, localConnection);
			CashJournalEntriesList forAccountingList = null;
			if(forAccountingResponse.getPayload().has("forAccountingList")) {
				forAccountingList = (CashJournalEntriesList) forAccountingResponse.getPayload().opt("forAccountingList");
				for (CashJournalEntry cashJournalEntry : forAccountingList) {
					cashJournalEntry.updateToAccounted();
				}
			}

			if(AECollectionUtil.isEmpty(forAccountingList)) {
				logger().debug("Accounting " + customerDescr.getName() + " completed with empty list");

				JSONObject payload = new JSONObject();
				return new AEResponse(payload);
			}

			/**
			 * Update DB
			 */
			localConnection.beginTransaction();

			// insert Cash Journal Entries, one entry per day
			AccJournalDAO journalDAO = daoFactory.getAccJournalDAO(localConnection);
			for (AccJournalEntry entry : expensesJournalEntries) {
				journalDAO.insertEntry(entry);
			}

			// update cash entries to accounted
			if(forAccountingList != null) {
				CashModuleDAO cashModuleDAO = daoFactory.getCashModuleDAO(localConnection);
				cashModuleDAO.update(forAccountingList);
			}

			localConnection.commit();

			/**
			 * Process after close.
			 * Must be after transaction close.
			 * If after close fails, this should not affect the whole close task.
			 */

			// auto export
			try {
				JSONObject exportRequestArguments = new JSONObject();

				// where to export
				if("secal.png".equalsIgnoreCase(invContext.getAuthPrincipal().getDescription())) {
					exportRequestArguments.put("exportTo", "CEGID");
				} else if("gianati.png".equalsIgnoreCase(invContext.getAuthPrincipal().getDescription())) {
					exportRequestArguments.put("exportTo", "SAGE");
				}

				// what to export
				AEDescriptorsList journalEntriesList = new AEDescriptorsList();
				for (AccJournalEntry entry : expensesJournalEntries) {
					journalEntriesList.add(entry.getDescriptor());	
				}
				exportRequestArguments.put("journalEntriesList", (Object) journalEntriesList);

				// customer
				exportRequestArguments.put("ownerId", ownerId);

				// acc period
				exportRequestArguments.put("accPeriodId", accPeriod.getID());

				// export
				AERequest exportRequest = new AERequest(exportRequestArguments);
				accLocal.exportDaily(exportRequest, invContext);
			} catch (Throwable t) {
				t.printStackTrace();
			}

			logger().debug("Accounting " + customerDescr.getName() + " completed");

			/**
			 * return response
			 */
			logger().debug("Exit");
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
	public AEResponse loadPAM(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));

			/**
			 * prepare dao's
			 */
			PAMDAO pamDAO = daoFactory.getPAMDAO(localConnection);

			/**
			 * local attributes
			 */
			long ownerId = aeRequest.getArguments().optLong("ownerId");
			boolean loadData = aeRequest.getArguments().optBoolean("loadData");
			CFC pam = null;

			/**
			 * determine AccPeriod
			 */
			boolean editable = !(aeRequest.getArguments().optBoolean("history"));
			AccPeriod accPeriod = null;
			if(editable) {
				// not history
				accPeriod = getFirstOpenPeriod(ownerId, AEApp.PAM_MODULE_ID, localConnection);
			} else {
				// history
				int month = aeRequest.getArguments().getInt("month");
				int year = aeRequest.getArguments().getInt("year");

				Date startDate = AEDateUtil.getFirstDate(month - 1, year);		
				Date endDate = AEDateUtil.getLastDate(month - 1, year);
				accPeriod = daoFactory.getAccPeriodDAO(localConnection).loadAccPeriod(
						startDate, 
						endDate, 
						AEApp.PAM_MODULE_ID,
						ownerId);
			}
			if(accPeriod == null) {
				throw new AEException(
						AEError.System.ACC_PERIOD_WAS_NOT_FOUD.getSystemID(), 
				"La période n'a pas été trouvé.");
			}

			// load CFC
			pam = pamDAO.loadPAM(Organization.lazyDescriptor(ownerId));

			// load CFCModel
			if(pam != null) {
				pam.setCfcModel(pamDAO.loadPAMModel(pam.getDescriptor()));
			}

			if(pam != null && loadData) {
				CFCData pamData = loadPAMData(pam, accPeriod, invContext, localConnection);
				pam.setCFCData(pamData);
			}

			// load accounts
			JSONArray allCashAcounts = new JSONArray();
			AEResponse aeResponseGOA = accLocal.loadGOA(aeRequest);
			JSONArray assetsArray = aeResponseGOA.getPayload().optJSONObject("assetsGOA").optJSONArray("accounts");
			if(assetsArray != null) {
				for(int i = 0; i < assetsArray.length(); i++) {
					allCashAcounts.put(assetsArray.getJSONObject(i));
				}
			}
			JSONArray expensesArray = aeResponseGOA.getPayload().optJSONObject("expensesGOA").optJSONArray("accounts");
			if(expensesArray != null) {
				for(int i = 0; i < expensesArray.length(); i++) {
					allCashAcounts.put(expensesArray.getJSONObject(i));
				}
			}

			// accountsJournal
			AccountDAO accDAO = daoFactory.getAccountDAO(localConnection);
			JSONArray accountsJournal = accDAO.loadCashAccounts(ownerId);

			// create and rerturn response
			JSONObject payload = new JSONObject();
			if(pam != null) {
				payload.put("PAM", pam.toJSONObject());
			}
			payload.put("accounts", allCashAcounts);
			payload.put("accountsJournal", accountsJournal);
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse savePAM(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			localConnection.beginTransaction();

			/**
			 * prepare dao's
			 */
			PAMDAO pamDAO = daoFactory.getPAMDAO(localConnection);

			/**
			 * local attributes
			 */
			CFC pam = new CFC();
			pam.create(aeRequest.getArguments().getJSONObject("PAM"));

			// save CFC
			if(pam.isNew()) {
				pamDAO.insertPAM(pam);
			} else if(pam.isUpdated()) {
				pamDAO.updatePAM(pam);
			}

			// save CFCModel
			if(pam.getCfcModel() != null) {
				CFCModel mandatModel = pam.getCfcModel();
				for (Iterator<CFCColumn> iterator = mandatModel.iterator(); iterator.hasNext();) {
					CFCColumn pamColumn = (CFCColumn) iterator.next();
					pamColumn.setToCFC(pam.getDescriptor());
					switch(pamColumn.getPersistentState()) {
					case NEW:
						pamDAO.insertPAMColumn(pamColumn);
						break;
					case UPDATED:
						pamDAO.updatePAMColumn(pamColumn);
						break;
					case DELETED:
						pamDAO.deletePAMColumn(pamColumn.getDescriptor());
						iterator.remove();
						break;
					}
				}
			}

			// commit
			localConnection.commit();

			// create and rerturn response
			JSONObject payload = new JSONObject();
			if(pam != null) {
				payload.put("PAM", pam.toJSONObject());
			}
			return new AEResponse(payload);
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse savePAMCell(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));

			/**
			 * prepare dao's
			 */
			PAMDAO pamDAO = daoFactory.getPAMDAO(localConnection);

			/**
			 * local attributes
			 */
			long ownerId = aeRequest.getArguments().optLong("ownerId");
			String field = aeRequest.getArguments().optString("field");
			Date date = AEDateUtil.parseDateStrict(aeRequest.getArguments().optString("date"));
			int rowIndex = aeRequest.getArguments().optInt("rowIndex");
			CFCColumn pamColumn = pamDAO.loadColumnByOwnerAndIndex(
					ownerId, 
					Integer.parseInt(field.substring(6)));

			if(pamColumn != null) {
				CFCCell pamCell = new CFCCell(
						pamColumn, 
						date, 
						rowIndex, 
						new AEValue(aeRequest.getArguments().optString("value"), pamColumn.getValue().getXType()));

				localConnection.beginTransaction();
				pamDAO.updatePAMCellValue(pamCell);
				localConnection.commit();

				// create and rerturn response
				JSONObject payload = new JSONObject();
				payload.put("PAMCell", pamCell.toJSONObject());
				return new AEResponse(payload);
			} else {
				throw new AEException("Internal Error: The CFC Column cannot be found.");
			}
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse closingPAMPeriod(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));

			/**
			 * prepare dao's
			 */
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);

			/**
			 * owner
			 */
			OrganizationDAO orgDAO = DAOFactory.getInstance().getOrganizationDAO(localConnection);
			AEDescriptor customerDescr = Organization.lazyDescriptor(aeRequest.getArguments().getLong("ownerId"));
			JSONArray customersArray = orgDAO.loadCustomer(customerDescr);
			JSONObject customer = customersArray.getJSONObject(0);
			customerDescr.setName(customer.optString("name"));

			/**
			 * determine period
			 */
			AccPeriod accPeriod = getFirstOpenPeriod(
					customerDescr.getID(), 
					AEApp.PAM_MODULE_ID, 
					localConnection);

			// we can close only finished periods
			Date nowDate = AEDateUtil.getClearDate(new Date());
			if(nowDate.before(AEDateUtil.getClearDate(accPeriod.getEndDate()))) {
				throw new AEException(
						(int) AEError.System.CANNOT_CLOSE_UNFINISHED.getSystemID(), 
				"La période n'est pas terminée.");
			}

			/**
			 * Create journal entries
			 */
			// load PAM
			PAMDAO pamDAO = daoFactory.getPAMDAO(localConnection);
			CFC pam = pamDAO.loadPAM(Organization.lazyDescriptor(customerDescr.getID()));
			if(pam == null) {
				throw new AEException("The PAM is not defined, so cannot be closed!");
			}

			// load model
			pam.setCfcModel(pamDAO.loadPAMModel(pam.getDescriptor()));

			// load data
			CFCData pamData = loadPAMData(pam, accPeriod, invContext, localConnection);
			pam.setCFCData(pamData);

			// create Joural Etries
			AEDescriptorImp journalDescr = new AEDescriptorImp();
			journalDescr.setCode("PAM");
			AccJournalEntriesList journalEntriesList = new AccJournalEntriesList();
			for (CFCRow pamRow : pamData) {
				// check whether pamRow is accountable
				if(CFCRow.DATE_ENTRY != pamRow.getSysId()) {
					continue;
				}

				// prepare data for pamRow accountig
				Date entryDate = pamRow.getDate();
				AccJournalEntry journalEntry = new AccJournalEntry();
				journalEntry.setName(journalDescr.getCode());
				journalEntry.setDescription(AEDateUtil.convertToString(entryDate, AEDateUtil.DB_DATE_FORMAT));

				// for every column create JouralItem
				for (CFCCell pamCell : pamRow) {
					CFCColumn pamColumn = pamCell.getColumn();
					if(pamColumn.getAccAccount() != null && pamColumn.getAccEntryType() != null) {
						AccJournalItem journalItem = new AccJournalItem(accPeriod);

						// create joural item only if amount is not zero						
						AEValue cellValue = pamCell.getValue();
						double amount = AEMath.round(cellValue.optDouble(), 2);
						if(Math.abs(amount) > 0.004) {
							// date
							journalItem.setDate(entryDate);

							// amout
							if(CFCColumn.AType.Dt.equals(pamColumn.getAccEntryType())) {
								journalItem.setDtAmount(amount);
							} else if(CFCColumn.AType.Ct.equals(pamColumn.getAccEntryType())) {
								journalItem.setCtAmount(amount);
							}

							journalItem.setCompany(customerDescr);
							journalItem.setJournal(journalDescr);
							journalItem.setAccount(pamColumn.getAccAccount().getDescriptor());
							journalItem.setDescription(pamColumn.getName());
							journalItem.setReference(pamColumn.getDescriptor());

							// add to journal entry
							journalEntry.addItem(journalItem);
						}
					}
				}

				// add to joural entries list
				if(!journalEntry.isEmpty()) {
					// validate alignment
					journalEntry.validateWith(AccJournalEtryAlignmentValidator.getInst());

					journalEntriesList.add(journalEntry);
				}
			}

			/**
			 * Begin transaction
			 */
			localConnection.beginTransaction();

			/**
			 * Insert journal entries
			 */
			AccJournalDAO accJournalDAO = daoFactory.getAccJournalDAO(localConnection);
			for (AccJournalEntry accJournalEntry : journalEntriesList) {
				accJournalDAO.insertEntry(accJournalEntry);
			}

			/**
			 * close the period
			 */
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
			try {
				// send e-mail
				Emailer emailer = new Emailer();
				emailer.onPeriodClosed(customerDescr, invContext, accPeriod, AEApp.PAM_MODULE_ID);

				// auto export
				JSONObject exportRequestArguments = new JSONObject();
				exportRequestArguments.put("accPeriodId", accPeriod.getID());
				if("secal.png".equalsIgnoreCase(invContext.getAuthPrincipal().getDescription())) {
					exportRequestArguments.put("exportTo", "CEGID");
				} else if("gianati.png".equalsIgnoreCase(invContext.getAuthPrincipal().getDescription())) {
					exportRequestArguments.put("exportTo", "SAGE");
				}
				exportRequestArguments.put("ownerId", aeRequest.getArguments().getLong("ownerId"));
				AERequest exportRequest = new AERequest(exportRequestArguments);
				accLocal.export(exportRequest, invContext);
			} catch (Throwable t) {
				t.printStackTrace();
			}

			/**
			 * return response
			 */
			JSONObject payload = new JSONObject();
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
	public AEResponse openPAMPeriod(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * determine period
			 */
			AccPeriod accPeriod = getLastClosedPeriod(
					aeRequest.getArguments().getLong("ownerId"), 
					AEApp.PAM_MODULE_ID, 
					localConnection);

			if(accPeriod != null) {
				localConnection.beginTransaction();

				/**
				 * delete general journal
				 */
				AccJournalDAO journalDAO = daoFactory.getAccJournalDAO(localConnection);
				journalDAO.deleteByAccPeriodId(accPeriod.getID());

				/**
				 * open the period
				 */
				AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
				accPeriodDAO.openPeriod(accPeriod.getID());
				accPeriodDAO.notExportedPeriod(accPeriod.getID());

				/**
				 * Delete attachments
				 */
				FileAttachmentDAO fileAttachmentDAO = daoFactory.getFileAttachmentDAO(localConnection);
				fileAttachmentDAO.deleteTo(accPeriod.getDescriptor());
			}

			/**
			 * commit transaction and return response
			 */
			localConnection.commit();

			/**
			 * return response
			 */
			JSONObject payload = new JSONObject();
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
	public CFCData loadPAMData(CFC pam, AccPeriod accPeriod, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			/**
			 * prepare dao's
			 */
			PAMDAO pamDAO = daoFactory.getPAMDAO(localConnection);
			CashModuleDAO cashModuleDAO = daoFactory.getCashModuleDAO(localConnection);

			/**
			 * local attributes
			 */
			CFCData pamData = new CFCData();

			/**
			 * create spread sheet
			 */
			// load cash journal
			CashJournalEntriesList dbCashEntriesList =
				cashModuleDAO.loadJournalEntries(
						pam.getCompany().getDescriptor().getID(), 
						accPeriod.getStartDate(), 
						accPeriod.getEndDate());

			// load saved cells
			ArrayList<CFCCell> cellsDBList = pamDAO.loadPAMCells(
					pam.getDescriptor(),
					accPeriod.getStartDate(),
					accPeriod.getEndDate());

			// create all spreadsheet and populate values regards model definitions
			AEDateIterator dateIterator = new AEDateIterator(accPeriod.getStartDate(), accPeriod.getEndDate());
			for (Date date : dateIterator) {
				// for every row
				CFCModel pamModel = pam.getCfcModel();
				for (CFCColumn column : pamModel) {
					// for every column

					// process the cell (date, rowIndex, column)
					CFCCell cell = new CFCCell();
					cell.setEntryDate(date);
					cell.setColumn(column);
					cell.setToCFC(pam.getDescriptor());
					cell.setRowIndex(0);
					cell.setColumnId(column.getID());

					// ensure the row where the cell should be inserted
					CFCRow row = pamData.getRow(date, 0);
					if(row == null) {
						row = new CFCRow(date, 0);
						pamData.add(row);
					}
					row.add(cell);

					// process meta data
					if(AEValue.isNull(column.getValue())) {
						continue;
					}
					if(CFCColumn.NType.VALUE.equals(column.getNType())) {
						// try to set already saved value
						CFCCell savedCell = getSavedCFCCell(
								cell.getEntryDate(), 
								cell.getRowIndex(), 
								cell.getColumnId(), 
								cellsDBList);
						if(savedCell != null) {
							cell.setValue(savedCell.getValue());
						} else {
							if(AEValue.XType.DOUBLE.equals(column.getValue().getXType())) {
								cell.setValue(new AEValue("0.0", AEValue.XType.DOUBLE));
							} else if(AEValue.XType.STRING.equals(column.getValue().getXType())) {
								cell.setValue(new AEValue(AEStringUtil.EMPTY_STRING, AEValue.XType.STRING));
							}
						}
					} else if(CFCColumn.NType.ACCOUNT.equals(column.getNType())) {
						// get the value from cashJournal for specified in the mdoel account 
						CashJournalEntry cjEntry = dbCashEntriesList.get(
								cell.getEntryDate(), 
								column.getValue().optLong());
						AEValue value = new AEValue();
						if(cjEntry != null) {
							Double amount = cjEntry.getAmount();
							if(amount != null) {
								value.set(cjEntry.getAmount());
							} else {
								value.set(0.0);
							}
						} else {
							value.set(0.0);
						}
						cell.setValue(value);
					} else if(CFCColumn.NType.ATTRIBUTE.equals(column.getNType())) {
						// get the value from cashJournal for specified in the mdoel account 
						Double attrVal = dbCashEntriesList.getAttributeSum(
								cell.getEntryDate(), 
								column.getValue().optLong());
						AEValue value = new AEValue();
						if(attrVal != null) {
							value.set(attrVal);
						} else {
							value.set(0.0);
						}
						cell.setValue(value);
					}
				}
			}

			/**
			 * Value and Formula are set.
			 * Evaluate formulas.
			 */
			for (CFCRow pamRow : pamData) {
				for (CFCCell pamCell : pamRow) {
					CFCColumn colModel = pamCell.getColumn();
					if(colModel != null 
							&& CFCColumn.NType.EXPRESSION.equals(colModel.getNType())) {

						// extract the expression
						String expression = colModel.getValue().getString();

						// configure parser to evaluate expression
						JEP parser = new JEP();
						parser.setAllowUndeclared(true);
						parser.parseExpression(expression);

						// iterate, detect and set parameters
						SymbolTable paramsTable = parser.getSymbolTable();
						@SuppressWarnings("unchecked")
						Set<String> params = paramsTable.keySet();
						for (String paramName : params) {
							double paramValue = 0.0;
							// param is regexpr C1, C2, ....
							if(paramName.startsWith("C")) {
								try {
									int paramColIndex = Integer.parseInt(paramName.substring(1)) - 1;
									for (CFCCell paramCell: pamRow) {
										if(paramCell.getColumn().getColIndex() == paramColIndex) {
											paramValue = paramCell.getValue().getDouble();
											paramValue = AEMath.round(paramValue, 2);
											break;
										}
									}
									parser.addVariable(paramName, paramValue);
								} catch (Exception e) {
									// error in the expression, put error as value
									AEValue resultValue = new AEValue();
									resultValue.set("#ERR");
									pamCell.setValue(resultValue);
									continue;
								}
							}

						}
						// vriables are set, so evaluate
						double result = parser.getValue();
						AEValue resultValue = new AEValue();
						resultValue.set(result);
						pamCell.setValue(resultValue);
					}
				}
			}

			/** 
			 * Process sum
			 */
			CFCRow sumRow = new CFCRow(CFCRow.SUM);
			for (CFCRow row : pamData) {
				for (CFCCell cell : row) {
					CFCCell sumCell = sumRow.getCell(cell.getColumnId());
					if(sumCell == null) {
						sumCell = new CFCCell(sumRow.getSysId());
						sumCell.setEntryDate(null);
						sumCell.setColumn(cell.getColumn());
						sumCell.setToCFC(pam.getDescriptor());
						sumCell.setRowIndex(0);
						sumCell.setColumnId(cell.getColumnId());
						sumCell.setValue(new AEValue("0.0", AEValue.XType.DOUBLE));

						sumRow.add(sumCell);
					}
					sumCell.addDouble(cell.getValue());
				}
			}
			pamData.add(sumRow);

			// rerturn response
			return pamData;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadIDE(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * prepare dao's
			 */
			IDEDAO ideDAO = daoFactory.getIDEDAO(localConnection);

			/**
			 * local attributes
			 */
			long ownerId = aeRequest.getArguments().optLong("ownerId");
			boolean loadData = aeRequest.getArguments().optBoolean("loadData");
			CFC ide = null;

			/**
			 * determine AccPeriod
			 */
			boolean editable = !(aeRequest.getArguments().optBoolean("history"));
			AccPeriod accPeriod = null;
			if(editable) {
				// not history
				accPeriod = getFirstOpenPeriod(ownerId, AEApp.IDE_MODULE_ID, localConnection);
			} else {
				// history
				int month = aeRequest.getArguments().getInt("month");
				int year = aeRequest.getArguments().getInt("year");

				Date startDate = AEDateUtil.getFirstDate(month - 1, year);		
				Date endDate = AEDateUtil.getLastDate(month - 1, year);
				accPeriod = daoFactory.getAccPeriodDAO(localConnection).loadAccPeriod(
						startDate, 
						endDate, 
						AEApp.IDE_MODULE_ID,
						ownerId);
			}
			if(accPeriod == null) {
				throw new AEException(
						AEError.System.ACC_PERIOD_WAS_NOT_FOUD.getSystemID(), 
				"La période n'a pas été trouvé.");
			}

			// load CFC
			ide = ideDAO.loadIDE(Organization.lazyDescriptor(ownerId));

			// load CFCModel
			if(ide != null) {
				ide.setCfcModel(ideDAO.loadIDEModel(ide.getDescriptor()));
			}

			if(ide != null && loadData) {
				CFCData ideData = loadIDEData(ide, accPeriod, invContext, localConnection);
				ide.setCFCData(ideData);
			}

			// load accounts
			JSONArray allCashAcounts = new JSONArray();
			AEResponse aeResponseGOA = accLocal.loadGOA(aeRequest);
			JSONArray assetsArray = aeResponseGOA.getPayload().optJSONObject("assetsGOA").optJSONArray("accounts");
			if(assetsArray != null) {
				for(int i = 0; i < assetsArray.length(); i++) {
					allCashAcounts.put(assetsArray.getJSONObject(i));
				}
			}
			JSONArray expensesArray = aeResponseGOA.getPayload().optJSONObject("expensesGOA").optJSONArray("accounts");
			if(expensesArray != null) {
				for(int i = 0; i < expensesArray.length(); i++) {
					allCashAcounts.put(expensesArray.getJSONObject(i));
				}
			}
			AccountDAO accDAO = daoFactory.getAccountDAO(localConnection);
			JSONArray accountsBank = accDAO.loadBankAccounts(ownerId);

			// create and rerturn response
			JSONObject payload = new JSONObject();
			if(ide != null) {
				payload.put("ide", ide.toJSONObject());
			}
			payload.put("accounts", allCashAcounts);
			payload.put("accountsBank", accountsBank);
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse saveIDE(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			/**
			 * prepare dao's
			 */
			IDEDAO ideDAO = daoFactory.getIDEDAO(localConnection);

			/**
			 * local attributes
			 */
			CFC ide = new CFC();
			ide.create(aeRequest.getArguments().getJSONObject("ide"));

			// save CFC
			if(ide.isNew()) {
				ideDAO.insertIDE(ide);
			} else if(ide.isUpdated()) {
				ideDAO.updateIDE(ide);
			}

			// save CFCModel
			if(ide.getCfcModel() != null) {
				CFCModel ideModel = ide.getCfcModel();
				for (Iterator<CFCColumn> iterator = ideModel.iterator(); iterator.hasNext();) {
					CFCColumn ideColumn = (CFCColumn) iterator.next();
					ideColumn.setToCFC(ide.getDescriptor());
					switch(ideColumn.getPersistentState()) {
					case NEW:
						ideDAO.insertIDEColumn(ideColumn);
						break;
					case UPDATED:
						ideDAO.updateIDEColumn(ideColumn);
						break;
					case DELETED:
						ideDAO.deleteIDEColumn(ideColumn.getDescriptor());
						iterator.remove();
						break;
					}
				}
			}

			// commit
			localConnection.commit();

			// create and rerturn response
			JSONObject payload = new JSONObject();
			if(ide != null) {
				payload.put("ide", ide.toJSONObject());
			}
			return new AEResponse(payload);
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse saveIDECell(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * prepare dao's
			 */
			IDEDAO ideDAO = daoFactory.getIDEDAO(localConnection);

			/**
			 * local attributes
			 */
			long ownerId = aeRequest.getArguments().optLong("ownerId");
			String field = aeRequest.getArguments().optString("field");
			Date date = AEDateUtil.parseDateStrict(aeRequest.getArguments().optString("date"));
			int rowIndex = aeRequest.getArguments().optInt("rowIndex");
			CFCColumn ideColumn = ideDAO.loadColumnByOwnerAndIndex(
					ownerId, 
					Integer.parseInt(field.substring(6)));

			if(ideColumn != null) {
				CFCCell ideCell = new CFCCell(
						ideColumn, 
						date, 
						rowIndex, 
						new AEValue(aeRequest.getArguments().optString("value"), ideColumn.getValue().getXType()));

				localConnection.beginTransaction();
				ideDAO.updateIDECellValue(ideCell);
				localConnection.commit();

				// create and rerturn response
				JSONObject payload = new JSONObject();
				payload.put("ideCell", ideCell.toJSONObject());
				return new AEResponse(payload);
			} else {
				throw new AEException("Internal Error: The IDE Column cannot be found.");
			}
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse closingIDEPeriod(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			/**
			 * prepare dao's
			 */
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);

			/**
			 * owner
			 */
			OrganizationDAO orgDAO = DAOFactory.getInstance().getOrganizationDAO(localConnection);
			AEDescriptor customerDescr = Organization.lazyDescriptor(aeRequest.getArguments().getLong("ownerId"));
			JSONArray customersArray = orgDAO.loadCustomer(customerDescr);
			JSONObject customer = customersArray.getJSONObject(0);
			customerDescr.setName(customer.optString("name"));

			/**
			 * determine period
			 */
			AccPeriod accPeriod = getFirstOpenPeriod(
					customerDescr.getID(), 
					AEApp.IDE_MODULE_ID, 
					localConnection);

			// we can close only finished periods
			Date nowDate = AEDateUtil.getClearDate(new Date());
			if(nowDate.before(AEDateUtil.getClearDate(accPeriod.getEndDate()))) {
				throw new AEException(
						(int) AEError.System.CANNOT_CLOSE_UNFINISHED.getSystemID(), 
				"La période n'est pas terminée.");
			}

			/**
			 * close the period
			 */
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
			try {
				// send e-mail
				Emailer emailer = new Emailer();
				emailer.onPeriodClosed(customerDescr, invContext, accPeriod, AEApp.IDE_MODULE_ID);

				//				// auto export
				//				JSONObject exportRequestArguments = new JSONObject();
				//				exportRequestArguments.put("accPeriodId", accPeriod.getID());
				//				if("secal.png".equalsIgnoreCase(invContext.getAuthSubject().getDescription())) {
				//					exportRequestArguments.put("exportTo", "CEGID");
				//				} else if("gianati.png".equalsIgnoreCase(invContext.getAuthSubject().getDescription())) {
				//					exportRequestArguments.put("exportTo", "SAGE");
				//				}
				//				exportRequestArguments.put("ownerId", aeRequest.getArguments().getLong("ownerId"));
				//				AERequest exportRequest = new AERequest(exportRequestArguments);
				//				accLocal.export(exportRequest, invContext);
			} catch (Throwable t) {
				t.printStackTrace();
			}

			/**
			 * return response
			 */
			JSONObject payload = new JSONObject();
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
	public AEResponse openIDEPeriod(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * determine period
			 */
			AccPeriod accPeriod = getLastClosedPeriod(
					aeRequest.getArguments().getLong("ownerId"), 
					AEApp.IDE_MODULE_ID, 
					localConnection);

			if(accPeriod != null) {
				localConnection.beginTransaction();

				/**
				 * delete general journal
				 */
				AccJournalDAO journalDAO = daoFactory.getAccJournalDAO(localConnection);
				journalDAO.deleteByAccPeriodId(accPeriod.getID());

				/**
				 * open the period
				 */
				AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
				accPeriodDAO.openPeriod(accPeriod.getID());
				accPeriodDAO.notExportedPeriod(accPeriod.getID());

				/**
				 * Delete attachments
				 */
				FileAttachmentDAO fileAttachmentDAO = daoFactory.getFileAttachmentDAO(localConnection);
				fileAttachmentDAO.deleteTo(accPeriod.getDescriptor());
			}

			/**
			 * commit transaction and return response
			 */
			localConnection.commit();

			/**
			 * return response
			 */
			JSONObject payload = new JSONObject();
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
	public CFCData loadIDEData(CFC ide, AccPeriod accPeriod, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			/**
			 * prepare dao's
			 */
			IDEDAO ideDAO = daoFactory.getIDEDAO(localConnection);
			CashModuleDAO cashModuleDAO = daoFactory.getCashModuleDAO(localConnection);

			/**
			 * local attributes
			 */
			CFCData ideData = new CFCData();;

			/**
			 * create spread sheet
			 */
			// load cash journal
			CashJournalEntriesList dbCashEntriesList =
				cashModuleDAO.loadJournalEntries(
						ide.getCompany().getDescriptor().getID(), 
						accPeriod.getStartDate(), 
						accPeriod.getEndDate());

			// load saved cells
			ArrayList<CFCCell> cellsDBList = ideDAO.loadIDECells(
					ide.getDescriptor(),
					accPeriod.getStartDate(),
					accPeriod.getEndDate());

			// create all spreadsheet and populate values regards model definitions
			AEDateIterator dateIterator = new AEDateIterator(accPeriod.getStartDate(), accPeriod.getEndDate());
			for (Date date : dateIterator) {
				// for every row
				CFCModel ideModel = ide.getCfcModel();
				for (CFCColumn column : ideModel) {
					// for every column

					// process the cell (date, rowIndex, column)
					CFCCell cell = new CFCCell();
					cell.setEntryDate(date);
					cell.setColumn(column);
					cell.setToCFC(ide.getDescriptor());
					cell.setRowIndex(0);
					cell.setColumnId(column.getID());

					// ensure the row where the cell should be inserted
					CFCRow row = ideData.getRow(date, 0);
					if(row == null) {
						row = new CFCRow(date, 0);
						ideData.add(row);
					}
					row.add(cell);

					// process meta data
					if(!AEValue.isNull(column.getValue()) && CFCColumn.NType.VALUE.equals(column.getNType())) {
						// try to set already saved value
						CFCCell savedCell = getSavedCFCCell(
								cell.getEntryDate(), 
								cell.getRowIndex(), 
								cell.getColumnId(), 
								cellsDBList);
						if(savedCell != null) {
							cell.setValue(savedCell.getValue());
						} else {
							if(!AEValue.isNull(column.getValue()) && AEValue.XType.DOUBLE.equals(column.getValue().getXType())) {
								cell.setValue(new AEValue("0.0", AEValue.XType.DOUBLE));
							} else if(!AEValue.isNull(column.getValue()) && AEValue.XType.STRING.equals(column.getValue().getXType())) {
								cell.setValue(new AEValue(AEStringUtil.EMPTY_STRING, AEValue.XType.STRING));
							}
						}
					} else if(!AEValue.isNull(column.getValue()) && CFCColumn.NType.ACCOUNT.equals(column.getNType())) {
						CashJournalEntry cjEntry = dbCashEntriesList.get(
								cell.getEntryDate(), 
								column.getValue().optLong());
						AEValue value = new AEValue();
						if(cjEntry != null) {
							Double amount = cjEntry.getAmount();
							if(amount != null) {
								value.set(cjEntry.getAmount());
							} else {
								value.set(0.0);
							}
						} else {
							value.set(0.0);
						}
						cell.setValue(value);
					} else if(CFCColumn.NType.ATTRIBUTE.equals(column.getNType())) {
						// get the value from cashJournal for specified in the mdoel account 
						Double attrVal = dbCashEntriesList.getAttributeSum(
								cell.getEntryDate(), 
								column.getValue().optLong());
						AEValue value = new AEValue();
						if(attrVal != null) {
							value.set(attrVal);
						} else {
							value.set(0.0);
						}
						cell.setValue(value);
					}
				}
			}

			/**
			 * Value and Formula are set.
			 * Evaluate formulas.
			 */
			for (CFCRow ideRow : ideData) {
				for (CFCCell ideCell : ideRow) {
					CFCColumn colModel = ideCell.getColumn();
					if(colModel != null 
							&& CFCColumn.NType.EXPRESSION.equals(colModel.getNType())) {

						// extract the expression
						String expression = colModel.getValue().getString();

						// configure parser to evaluate expression
						JEP parser = new JEP();
						parser.setAllowUndeclared(true);
						parser.parseExpression(expression);

						// iterate, detect and set parameters
						SymbolTable paramsTable = parser.getSymbolTable();
						@SuppressWarnings("unchecked")
						Set<String> params = paramsTable.keySet();
						for (String paramName : params) {
							double paramValue = 0.0;
							// param is regexpr C1, C2, ....
							if(paramName.startsWith("C")) {
								try {
									int paramColIndex = Integer.parseInt(paramName.substring(1)) - 1;
									for (CFCCell paramCell: ideRow) {
										if(paramCell.getColumn().getColIndex() == paramColIndex) {
											paramValue = paramCell.getValue().getDouble();
											paramValue = AEMath.round(paramValue, 2);
											break;
										}
									}
									parser.addVariable(paramName, paramValue);
								} catch (Exception e) {
									// error in the expression, put error as value
									AEValue resultValue = new AEValue();
									resultValue.set("#ERR");
									ideCell.setValue(resultValue);
									continue;
								}
							}

						}
						// vriables are set, so evaluate
						double result = parser.getValue();
						AEValue resultValue = new AEValue();
						resultValue.set(result);
						ideCell.setValue(resultValue);
					}
				}
			}


			/** 
			 * Process sum
			 */
			CFCRow sumRow = new CFCRow(CFCRow.SUM);
			for (CFCRow row : ideData) {
				for (CFCCell cell : row) {
					CFCCell sumCell = sumRow.getCell(cell.getColumnId());
					if(sumCell == null) {
						sumCell = new CFCCell(sumRow.getSysId());
						sumCell.setEntryDate(null);
						sumCell.setColumn(cell.getColumn());
						sumCell.setToCFC(ide.getDescriptor());
						sumCell.setRowIndex(0);
						sumCell.setColumnId(cell.getColumnId());
						sumCell.setValue(new AEValue("0.0", AEValue.XType.DOUBLE));

						sumRow.add(sumCell);
					}
					sumCell.addDouble(cell.getValue());
				}
			}
			ideData.add(sumRow);

			// rerturn response
			return ideData;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadInventoryStatus(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validation
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

			// whether this user is ownered by this customer
			ap.ownershipValidator(sOwnerId);

			// whether this article is ownered by this customer
			if(sOwnerId != ownerId) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * prepare dao's
			 */
			InventoryDAO inventoryDAO = daoFactory.getInventoryDAO(localConnection);

			/**
			 * local attributes
			 */
			boolean loadData = aeRequest.getArguments().optBoolean("loadData");

			/**
			 * determine AccPeriod
			 */
			boolean editable = !(aeRequest.getArguments().optBoolean("history"));
			AccPeriod accPeriod = null;
			if(editable) {
				// not history
				accPeriod = getFirstOpenPeriod(ownerId, AEApp.INVENTORY_MODULE_ID, localConnection);
			} else {
				// history
				int month = aeRequest.getArguments().getInt("month");
				int year = aeRequest.getArguments().getInt("year");

				Date startDate = AEDateUtil.getFirstDate(month - 1, year);		
				Date endDate = AEDateUtil.getLastDate(month - 1, year);
				accPeriod = daoFactory.getAccPeriodDAO(localConnection).loadAccPeriod(
						startDate, 
						endDate, 
						AEApp.INVENTORY_MODULE_ID,
						ownerId);
			}
			if(accPeriod == null) {
				throw new AEException(
						AEError.System.ACC_PERIOD_WAS_NOT_FOUD.getSystemID(), 
				"La période n'a pas été trouvé.");
			}

			/**
			 * load Inventory Supply Table
			 */
			CFC invSupply = inventoryDAO.loadInventory(Organization.lazyDescriptor(ownerId));

			if(invSupply != null) {
				invSupply.setCfcModel(inventoryDAO.loadInventoryModel(invSupply.getDescriptor()));
			}

			if(invSupply != null && loadData) {
				CFCData invSupplyData = loadInventorySupplyData(invSupply, accPeriod, invContext, localConnection);
				invSupply.setCFCData(invSupplyData);
			}

			/**
			 * load Inventory Status Table
			 */
			InventoryStatus invStatus = inventoryDAO.loadInventoryStatus(Organization.lazyDescriptor(ownerId));

			if(invStatus != null) {
				invStatus.setCfcModel(inventoryDAO.loadInventoryModel(invStatus.getDescriptor()));
			}

			if(invStatus != null && loadData) {
				CFCData invStatusData = loadInventoryStatusData(invStatus, accPeriod, invContext, localConnection);
				invStatus.setCFCData(invStatusData);
			}

			/**
			 * load Inventory Status Annually Table
			 */
			InventoryStatus invStatusAnnually = inventoryDAO.loadInventoryStatus(Organization.lazyDescriptor(ownerId));

			if(invStatusAnnually != null) {
				invStatusAnnually.setCfcModel(inventoryDAO.loadInventoryModel(invStatus.getDescriptor()));
			}

			if(invStatusAnnually != null && loadData) {
				CFCData invStatusDataAnnually = loadInventoryStatusDataAnnually(invStatusAnnually, accPeriod, invContext, localConnection);
				invStatusAnnually.setCFCData(invStatusDataAnnually);
			}

			// create and rerturn response
			JSONObject payload = new JSONObject();
			if(invSupply != null) {
				payload.put("differences", invStatus.toJSONObject());
				payload.put("deliveries", invSupply.toJSONObject());
				payload.put("annuallyDifferences", invStatusAnnually.toJSONObject());
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
	public CFCData loadInventorySupplyData(CFC invSupply, AccPeriod accPeriod, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {

		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			/**
			 * prepare dao's
			 */
			InventoryDAO inventoryDAO = daoFactory.getInventoryDAO(localConnection);

			/**
			 * local attributes
			 */
			CFCData invSupplyData = new CFCData();
			CFCModel cfcModel = invSupply.getCfcModel();

			/**
			 * create spread sheet
			 */

			// load supply for configured stocks
			List<Long> ids = new ArrayList<Long>();
			for (CFCColumn column : cfcModel) {
				if(CFCColumn.NType.ATTRIBUTE.equals(column.getNType())) {
					if(column instanceof InventoryColumn) {
						ids.add(((InventoryColumn) column).getSupplyAttrId());
					} else {
						ids.add(column.getValue().optLong());
					}
				}
			}
			Map<Date, Map<Long, Double>> supply = 
				inventoryDAO.loadSupply(
						invSupply.getCompany().getDescriptor().getID(), 
						ids, 
						accPeriod);

			// create all spreadsheet and populate values regards model definitions
			AEDateIterator dateIterator = new AEDateIterator(accPeriod.getStartDate(), accPeriod.getEndDate());
			for (Date date : dateIterator) {
				// for every row

				for (CFCColumn column : cfcModel) {
					// for every column

					// process the cell (date, rowIndex, column)
					CFCCell cell = new CFCCell();
					cell.setEntryDate(date);
					cell.setColumn(column);
					cell.setToCFC(invSupply.getDescriptor());
					cell.setRowIndex(0);
					cell.setColumnId(column.getID());

					// ensure the row where the cell should be inserted
					CFCRow row = invSupplyData.getRow(date, 0);
					if(row == null) {
						row = new CFCRow(date, 0);
						invSupplyData.add(row);
					}
					row.add(cell);

					// process meta data
					if(CFCColumn.NType.ATTRIBUTE.equals(column.getNType())) {
						// load supply for date and attribute
						Map<Long, Double> attrMap = supply.get(AEDateUtil.getClearDateTime(date));
						if(attrMap != null && column instanceof InventoryColumn) {
							Double qty = attrMap.get(((InventoryColumn)column).getSupplyAttrId());
							cell.setValue(new AEValue(AEMath.doubleValue(qty)));
						}
					}
				}
			}

			/**
			 * Value and Formula are set.
			 * Evaluate formulas.
			 */
			for (CFCRow cfcRow : invSupplyData) {
				for (CFCCell cfcCell : cfcRow) {
					CFCColumn colModel = cfcCell.getColumn();
					if(colModel != null 
							&& CFCColumn.NType.EXPRESSION.equals(colModel.getNType())) {

						// extract the expression
						String expression = colModel.getValue().getString();

						// configure parser to evaluate expression
						JEP parser = new JEP();
						parser.setAllowUndeclared(true);
						parser.parseExpression(expression);

						// iterate, detect and set parameters
						SymbolTable paramsTable = parser.getSymbolTable();
						@SuppressWarnings("unchecked")
						Set<String> params = paramsTable.keySet();
						for (String paramName : params) {
							double paramValue = 0.0;
							// param is regexpr C1, C2, ....
							if(paramName.startsWith("C")) {
								try {
									int paramColIndex = Integer.parseInt(paramName.substring(1)) - 1;
									for (CFCCell paramCell: cfcRow) {
										if(paramCell.getColumn().getColIndex() == paramColIndex) {
											paramValue = paramCell.getValue().getDouble();
											paramValue = AEMath.round(paramValue, 2);
											break;
										}
									}
									parser.addVariable(paramName, paramValue);
								} catch (Exception e) {
									// error in the expression, put error as value
									AEValue resultValue = new AEValue();
									resultValue.set("#ERR");
									cfcCell.setValue(resultValue);
									continue;
								}
							}

						}
						// vriables are set, so evaluate
						double result = parser.getValue();
						AEValue resultValue = new AEValue();
						resultValue.set(result);
						cfcCell.setValue(resultValue);
					}
				}
			}


			/** 
			 * Process sum
			 */
			CFCRow sumRow = new CFCRow(CFCRow.SUM);
			for (CFCRow row : invSupplyData) {
				for (CFCCell cell : row) {
					CFCCell sumCell = sumRow.getCell(cell.getColumnId());
					if(sumCell == null) {
						sumCell = new CFCCell(sumRow.getSysId());
						sumCell.setEntryDate(null);
						sumCell.setColumn(cell.getColumn());
						sumCell.setToCFC(invSupply.getDescriptor());
						sumCell.setRowIndex(0);
						sumCell.setColumnId(cell.getColumnId());
						sumCell.setValue(new AEValue("0.0", AEValue.XType.DOUBLE));

						sumRow.add(sumCell);
					}
					if(cell.getValue() != null) {
						sumCell.addDouble(cell.getValue());
					}
				}
			}
			invSupplyData.add(sumRow);

			// rerturn response
			return invSupplyData;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public CFCData loadInventoryStatusData(InventoryStatus invStatus, AccPeriod accPeriod, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {

		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			/**
			 * prepare dao's
			 */
			InventoryDAO inventoryDAO = daoFactory.getInventoryDAO(localConnection);

			/**
			 * local attributes
			 */
			CFCData invStatusData = new CFCData();
			CFCModel cfcModel = invStatus.getCfcModel();

			/**
			 * period
			 */
			invStatus.setMonth(AEDateUtil.getMonthInYear(accPeriod.getEndDate()));
			invStatus.setYear(AEDateUtil.getYear(accPeriod.getEndDate()));

			/**
			 * create spread sheet
			 */

			// load previous period
			Date prevEndDate = AEDateUtil.addDaysToDate(accPeriod.getStartDate(), -1);
			Date prevStartDate = AEDateUtil.firstDayOfMonth(prevEndDate);
			ArrayList<CFCCell> prevSavedCells = inventoryDAO.loadInvCells(
					invStatus.getDescriptor(), prevStartDate, prevEndDate);

			// load opening balance for configured stocks
			ArrayList<CFCCell> savedCells = inventoryDAO.loadInvCells(invStatus.getDescriptor(), accPeriod.getStartDate(), accPeriod.getEndDate());

			// load supply for configured stocks
			List<Long> supplyIds = new ArrayList<Long>();
			for (CFCColumn column : cfcModel) {
				if(CFCColumn.NType.ATTRIBUTE.equals(column.getNType())) {
					if(column instanceof InventoryColumn) {
						supplyIds.add(((InventoryColumn) column).getSupplyAttrId());
					} else {
						supplyIds.add(column.getValue().optLong());
					}
				}
			}
			Map<Long, Double> supply = 
				inventoryDAO.loadStocksSummarized(
						invStatus.getCompany().getDescriptor().getID(), 
						supplyIds, 
						accPeriod);

			// load sale for configured stocks
			List<Long> saleIds = new ArrayList<Long>();
			for (CFCColumn column : cfcModel) {
				if(CFCColumn.NType.ATTRIBUTE.equals(column.getNType())) {
					if(column instanceof InventoryColumn) {
						saleIds.add(((InventoryColumn) column).getSaleAttrId());
					} else {
						saleIds.add(column.getValue().optLong());
					}
				}
			}
			Map<Long, Double> sale = 
				inventoryDAO.loadStocksSummarized(
						invStatus.getCompany().getDescriptor().getID(), 
						saleIds, 
						accPeriod);

			/** 
			 * Process rows
			 */
			for (InventoryRow.Type rowType : InventoryRow.Type.values()) {
				if(InventoryRow.Type.NA.equals(rowType) || rowType.getSysId() > InventoryRow.Type.FREE_SUM.getSysId()) {
					continue;
				}
				InventoryRow row = new InventoryRow(rowType);
				for (CFCColumn column : cfcModel) {
					// for every column

					InventoryColumn invColumn = null;
					if(column instanceof InventoryColumn) {
						invColumn = (InventoryColumn) column;
					} else {
						continue;
					}

					// process the cell (date, rowIndex, column)
					CFCCell cell = new CFCCell();
					cell.setEntryDate(accPeriod.getEndDate());
					cell.setColumn(column);
					cell.setToCFC(invStatus.getDescriptor());
					cell.setRowIndex((int)rowType.getSysId());
					cell.setColumnId(column.getID());

					// value
					switch(rowType) {
					case A: {
						// "Stocks jaugés début mois";
						CFCCell savedCell = InventoryCalculator.getCFCCell(savedCells, InventoryRow.Type.A.getSysId(), invColumn);
						if(savedCell != null) {
							cell.setValue(savedCell.getValue());
						} else {
							savedCell = InventoryCalculator.getCFCCell(prevSavedCells, InventoryRow.Type.E.getSysId(), invColumn);
							if(savedCell != null) {
								cell.setValue(savedCell.getValue());
							}
						}
						break;
					}
					case B: {
						// supply
						if(supply.containsKey(invColumn.getSupplyAttrId())) {
							cell.setValue(new AEValue(AEMath.doubleValue(supply.get(invColumn.getSupplyAttrId()))));
						}
						break;
					}
					case C: {
						// sale;
						if(sale.containsKey(invColumn.getSaleAttrId())) {
							cell.setValue(new AEValue(AEMath.doubleValue(sale.get(invColumn.getSaleAttrId()))));
						}
						break;
					}
					case D: {
						// "Stock final comptable du mois";
						double d = InventoryCalculator.calculateRowD(invStatusData, invColumn);
						cell.setValue(new AEValue(d));
						break;
					}
					case E: {
						// "Stocks jaugés fin de mois";
						CFCCell savedCell = InventoryCalculator.getCFCCell(savedCells, InventoryRow.Type.E.getSysId(), invColumn);
						if(savedCell != null) {
							cell.setValue(savedCell.getValue());
						}
						break;
					}
					case F: {
						// "Ecart du mois";
						double d = InventoryCalculator.calculateRowF(invStatusData, invColumn);
						cell.setValue(new AEValue(d));
						break;
					}
					case G: {
						// "Ecart cumulés mois précédent";
						CFCCell savedCell = InventoryCalculator.getCFCCell(savedCells, InventoryRow.Type.G.getSysId(), invColumn);
						if(savedCell != null) {
							cell.setValue(savedCell.getValue());
						} else {
							savedCell = InventoryCalculator.getCFCCell(prevSavedCells, InventoryRow.Type.H.getSysId(), invColumn);
							if(savedCell != null) {
								cell.setValue(savedCell.getValue());
							}
						}
						break;
					}
					case H: {
						// "Nouveaux écarts cumulés";
						double d = InventoryCalculator.calculateRowH(invStatusData, invColumn);
						cell.setValue(new AEValue(d));
						break;
					}
					case I: {
						// "Ecarts cumulés Etat de Stock";
						CFCCell savedCell = InventoryCalculator.getCFCCell(savedCells, InventoryRow.Type.I.getSysId(), invColumn);
						if(savedCell != null) {
							cell.setValue(savedCell.getValue());
						}
						break;
					}
					case J: {
						// "Différence";
						double d = InventoryCalculator.calculateRowJ(invStatusData, invColumn);
						cell.setValue(new AEValue(d));
						break;
					}
					case K: {
						// "Ecart en 0/00";
						double d = InventoryCalculator.calculateRowK(invStatusData, invColumn);
						cell.setValue(new AEValue(d));
						break;
					}
					case FREE_1: {
						CFCCell savedCell = InventoryCalculator.getCFCCell(savedCells, InventoryRow.Type.FREE_1.getSysId(), invColumn);
						if(savedCell != null) {
							cell.setValue(savedCell.getValue());
						}
						break;
					}
					case FREE_2: {
						CFCCell savedCell = InventoryCalculator.getCFCCell(savedCells, InventoryRow.Type.FREE_2.getSysId(), invColumn);
						if(savedCell != null) {
							cell.setValue(savedCell.getValue());
						}
						break;
					}
					case FREE_3: {
						CFCCell savedCell = InventoryCalculator.getCFCCell(savedCells, InventoryRow.Type.FREE_3.getSysId(), invColumn);
						if(savedCell != null) {
							cell.setValue(savedCell.getValue());
						}
						break;
					}
					case FREE_4: {
						CFCCell savedCell = InventoryCalculator.getCFCCell(savedCells, InventoryRow.Type.FREE_4.getSysId(), invColumn);
						if(savedCell != null) {
							cell.setValue(savedCell.getValue());
						}
						break;
					}
					case FREE_5: {
						CFCCell savedCell = InventoryCalculator.getCFCCell(savedCells, InventoryRow.Type.FREE_5.getSysId(), invColumn);
						if(savedCell != null) {
							cell.setValue(savedCell.getValue());
						}
						break;
					}
					case FREE_6: {
						CFCCell savedCell = InventoryCalculator.getCFCCell(savedCells, InventoryRow.Type.FREE_6.getSysId(), invColumn);
						if(savedCell != null) {
							cell.setValue(savedCell.getValue());
						}
						break;
					}
					case FREE_SUM: {
						double d = InventoryCalculator.calculateFreeSum(invStatusData, invColumn);
						cell.setValue(new AEValue(d));
						break;
					}
					}

					// add the cell
					row.add(cell);
				}
				invStatusData.add(row);
			}

			// rerturn response
			return invStatusData;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse saveInventoryStatusCell(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			JSONObject arguments = aeRequest.getArguments();
			String field = arguments.optString("field");
			int sysId = arguments.getInt("sysId");
			long sOwnerId = arguments.getLong("sOwnerId");
			long ownerId = arguments.getLong("ownerId");

			// whether this user is ownered by this customer
			ap.ownershipValidator(sOwnerId);

			// whether this article is ownered by this customer
			if(sOwnerId != ownerId) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * prepare dao's
			 */
			InventoryDAO inventoryDAO = daoFactory.getInventoryDAO(localConnection);

			// date
			AccPeriod accPeriod = getFirstOpenPeriod(ownerId, AEApp.INVENTORY_MODULE_ID, localConnection);
			Date date = accPeriod.getEndDate();

			// column definition
			CFCColumn cfcColumn = inventoryDAO.loadColumnByOwnerAndIndex(
					ownerId, 
					Integer.parseInt(field.substring(6)));

			if(cfcColumn != null) {
				CFCCell cfcCell = new CFCCell(
						cfcColumn, 
						date, 
						sysId, 
						new AEValue(aeRequest.getArguments().optString("value"), cfcColumn.getValue().getXType()));

				localConnection.beginTransaction();
				inventoryDAO.updateInvCellValue(cfcCell);
				localConnection.commit();

				// create and rerturn response
				JSONObject payload = new JSONObject();
				payload.put("cfcCell", cfcCell.toJSONObject());
				return new AEResponse(payload);
			} else {
				throw new AEException("Internal Error: The CFC Column cannot be found.");
			}
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse closeInventoryStatus(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			/**
			 * prepare dao's
			 */
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);

			/**
			 * owner
			 */
			OrganizationDAO orgDAO = DAOFactory.getInstance().getOrganizationDAO(localConnection);
			AEDescriptor customerDescr = Organization.lazyDescriptor(aeRequest.getArguments().getLong("ownerId"));
			JSONArray customersArray = orgDAO.loadCustomer(customerDescr);
			JSONObject customer = customersArray.getJSONObject(0);
			customerDescr.setName(customer.optString("name"));

			/**
			 * determine period
			 */
			AccPeriod accPeriod = getFirstOpenPeriod(
					customerDescr.getID(), 
					AEApp.INVENTORY_MODULE_ID, 
					localConnection);

			// we can close only finished periods
			Date nowDate = AEDateUtil.getClearDate(new Date());
			if(nowDate.before(AEDateUtil.getClearDate(accPeriod.getEndDate()))) {
				throw new AEException(
						(int) AEError.System.CANNOT_CLOSE_UNFINISHED.getSystemID(), 
				"La période n'est pas terminée.");
			}

			/**
			 * Load Inventory Status Table
			 */
			InventoryDAO inventoryDAO = daoFactory.getInventoryDAO(localConnection);
			InventoryStatus invStatus = inventoryDAO.loadInventoryStatus(Organization.lazyDescriptor(ownerId));

			if(invStatus != null) {
				invStatus.setCfcModel(inventoryDAO.loadInventoryModel(invStatus.getDescriptor()));
			}

			if(invStatus != null) {
				CFCData invStatusData = loadInventoryStatusData(invStatus, accPeriod, invContext, localConnection);
				invStatus.setCFCData(invStatusData);
			}

			CFCModel invStatusModel = invStatus.getCfcModel();
			CFCData invStatusData = invStatus.getCfcData();

			/**
			 * Keep row E.
			 * Will be used in the next period.
			 */
			CFCRow rowE = invStatusData.getRow(InventoryRow.Type.E.getSysId());
			for (CFCColumn cfcColumn : invStatusModel) {
				CFCCell cell = rowE.getCell(cfcColumn.getID());
				if(cell != null && !inventoryDAO.existInvCellValue(cell)) {
					inventoryDAO.insertInvCell(cell);
				}
			}

			/**
			 * Keep row H.
			 * Will be used in the next period.
			 */
			CFCRow rowH = invStatusData.getRow(InventoryRow.Type.H.getSysId());
			for (CFCColumn cfcColumn : invStatusModel) {
				CFCCell cell = rowH.getCell(cfcColumn.getID());
				if(cell != null && !inventoryDAO.existInvCellValue(cell)) {
					inventoryDAO.insertInvCell(cell);
				}
			}

			/**
			 * close the period
			 */
			accPeriodDAO.closePeriod(accPeriod.getID());

			/**
			 * Detect and process the end of financial year
			 */
			AETimePeriod financialYearInfo = orgDAO.loadFinancialPeriod(customerDescr);
			AETimePeriod financialYear = 
				AEDateUtil.getFinancialFrenchYearForDate(
						financialYearInfo.getStartDate(), 
						(int) financialYearInfo.getDuration(), 
						accPeriod.getEndDate());
			if(AEDateUtil.getClearDateTime(accPeriod.getEndDate()).equals(AEDateUtil.getClearDateTime(financialYear.getEndDate()))) {
				// close the financial year
				InventoryStatus invStatusAnnualy = inventoryDAO.loadInventoryStatus(customerDescr);

				if(invStatusAnnualy != null) {
					invStatusAnnualy.setCfcModel(inventoryDAO.loadInventoryModel(invStatus.getDescriptor()));
				}

				if(invStatusAnnualy != null) {
					CFCData invStatusDataAnnualy = loadInventoryStatusDataAnnually(
							invStatusAnnualy, 
							accPeriod, 
							invContext, 
							localConnection);
					invStatusAnnualy.setCFCData(invStatusDataAnnualy);
				}

				CFCModel invStatusModelAnnualy = invStatusAnnualy.getCfcModel();
				CFCData invStatusDataAnnualy = invStatusAnnualy.getCfcData();

				/**
				 * Keep row E.
				 * Will be used in the next period.
				 */
				CFCRow rowEA = invStatusDataAnnualy.getRow(InventoryRow.Type.E_ANNUALLY.getSysId());
				for (CFCColumn cfcColumn : invStatusModelAnnualy) {
					CFCCell cell = rowEA.getCell(cfcColumn.getID());
					if(cell != null && !inventoryDAO.existInvCellValue(cell)) {
						inventoryDAO.insertInvCell(cell);
					}
				}

				/**
				 * Keep row H.
				 * Will be used in the next period.
				 */
				CFCRow rowHA = invStatusDataAnnualy.getRow(InventoryRow.Type.H_ANNUALLY.getSysId());
				for (CFCColumn cfcColumn : invStatusModelAnnualy) {
					CFCCell cell = rowHA.getCell(cfcColumn.getID());
					if(cell != null && !inventoryDAO.existInvCellValue(cell)) {
						inventoryDAO.insertInvCell(cell);
					}
				}
			}

			/**
			 * commit transaction and return response 
			 */
			localConnection.commit();

			/**
			 * Process after close.
			 * Must be after transaction close.
			 * If after close fails, this should not affect the whole close task.
			 */
			try {
				// send e-mail
				Emailer emailer = new Emailer();
				emailer.onPeriodClosed(customerDescr, invContext, accPeriod, AEApp.INVENTORY_MODULE_ID);

				// auto export
				//				JSONObject exportRequestArguments = new JSONObject();
				//				exportRequestArguments.put("accPeriodId", accPeriod.getID());
				//				if("secal.png".equalsIgnoreCase(invContext.getAuthSubject().getDescription())) {
				//					exportRequestArguments.put("exportTo", "CEGID");
				//				} else if("gianati.png".equalsIgnoreCase(invContext.getAuthSubject().getDescription())) {
				//					exportRequestArguments.put("exportTo", "SAGE");
				//				}
				//				exportRequestArguments.put("ownerId", aeRequest.getArguments().getLong("ownerId"));
				//				AERequest exportRequest = new AERequest(exportRequestArguments);
				//				accLocal.export(exportRequest, invContext);
			} catch (Throwable t) {
				t.printStackTrace();
			}

			/**
			 * return response
			 */
			JSONObject payload = new JSONObject();
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
	public AEResponse openInventoryStatus(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
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
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * determine period
			 */
			AccPeriod accPeriod = getLastClosedPeriod(
					aeRequest.getArguments().getLong("ownerId"), 
					AEApp.INVENTORY_MODULE_ID, 
					localConnection);

			if(accPeriod != null) {
				localConnection.beginTransaction();

				/**
				 * delete general journal
				 */
				AccJournalDAO journalDAO = daoFactory.getAccJournalDAO(localConnection);
				journalDAO.deleteByAccPeriodId(accPeriod.getID());

				/**
				 * open the period
				 */
				AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
				accPeriodDAO.openPeriod(accPeriod.getID());
				accPeriodDAO.notExportedPeriod(accPeriod.getID());
			}

			/**
			 * commit transaction and return response
			 */
			localConnection.commit();

			/**
			 * return response
			 */
			JSONObject payload = new JSONObject();
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
	public CFCData loadInventoryStatusDataAnnually(InventoryStatus invStatus, AccPeriod accPeriod, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {

		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			/**
			 * prepare dao's
			 */
			InventoryDAO inventoryDAO = daoFactory.getInventoryDAO(localConnection);
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);

			/**
			 * local attributes
			 */
			CFCData invStatusData = new CFCData();
			CFCModel cfcModel = invStatus.getCfcModel();

			/**
			 * create spread sheet
			 */

			AETimePeriod financialYearInfo = orgDAO.loadFinancialPeriod(invStatus.getCompany().getDescriptor());
			AETimePeriod financialYear = 
				AEDateUtil.getFinancialFrenchYearForDate(
						financialYearInfo.getStartDate(), 
						(int) financialYearInfo.getDuration(), 
						accPeriod.getEndDate());
			AccPeriod annuallyPeriod = new AccPeriod();
			annuallyPeriod.setStartDate(financialYear.getStartDate());
			annuallyPeriod.setEndDate(accPeriod.getEndDate());

			/**
			 * period
			 */
			invStatus.setYear(AEDateUtil.getYear(financialYear.getEndDate()));

			// load previous month
			Date prevMonthEndDate = AEDateUtil.addDaysToDate(financialYear.getStartDate(), -1);
			Date prevMonthStartDate = AEDateUtil.firstDayOfMonth(prevMonthEndDate);
			ArrayList<CFCCell> prevSavedCellsMonth = inventoryDAO.loadInvCells(
					invStatus.getDescriptor(), prevMonthStartDate, prevMonthEndDate);

			// load saved cells for current period
			ArrayList<CFCCell> savedCells = inventoryDAO.loadInvCells(
					invStatus.getDescriptor(), 
					financialYear.getStartDate(), 
					financialYear.getEndDate());

			// load supply for configured stocks
			List<Long> supplyIds = new ArrayList<Long>();
			for (CFCColumn column : cfcModel) {
				if(CFCColumn.NType.ATTRIBUTE.equals(column.getNType())) {
					if(column instanceof InventoryColumn) {
						supplyIds.add(((InventoryColumn) column).getSupplyAttrId());
					} else {
						supplyIds.add(column.getValue().optLong());
					}
				}
			}
			Map<Long, Double> supply = 
				inventoryDAO.loadStocksSummarized(
						invStatus.getCompany().getDescriptor().getID(), 
						supplyIds, 
						annuallyPeriod);

			// load sale for configured stocks
			List<Long> saleIds = new ArrayList<Long>();
			for (CFCColumn column : cfcModel) {
				if(CFCColumn.NType.ATTRIBUTE.equals(column.getNType())) {
					if(column instanceof InventoryColumn) {
						saleIds.add(((InventoryColumn) column).getSaleAttrId());
					} else {
						saleIds.add(column.getValue().optLong());
					}
				}
			}
			Map<Long, Double> sale = 
				inventoryDAO.loadStocksSummarized(
						invStatus.getCompany().getDescriptor().getID(), 
						saleIds, 
						annuallyPeriod);

			/** 
			 * Process rows
			 */
			for (InventoryRow.Type rowType : InventoryRow.Type.values()) {
				// skip 
				if(rowType.getSysId() <= InventoryRow.Type.FREE_SUM.getSysId()) {
					continue;
				}

				// create row for specified rowType
				InventoryRow row = new InventoryRow(rowType);

				// iterate columns and add to the row
				for (CFCColumn column : cfcModel) {
					// we should deal with InventoryColumn
					InventoryColumn invColumn = null;
					if(column instanceof InventoryColumn) {
						invColumn = (InventoryColumn) column;
					} else {
						continue;
					}

					// process the cell (date, rowIndex, column)
					CFCCell cell = new CFCCell();
					cell.setEntryDate(financialYear.getEndDate());
					cell.setColumn(column);
					cell.setToCFC(invStatus.getDescriptor());
					cell.setRowIndex((int)rowType.getSysId());
					cell.setColumnId(column.getID());

					// value
					switch(rowType) {
					case A_ANNUALLY: {
						// "Stocks jaugés début mois";
						// Value by default is E at the end from previous month 
						// User may modify the value.
						CFCCell savedCell = InventoryCalculator.getCFCCell(savedCells, InventoryRow.Type.A_ANNUALLY.getSysId(), invColumn);
						if(savedCell != null) {
							cell.setValue(savedCell.getValue());
						} else {
							savedCell = InventoryCalculator.getCFCCell(prevSavedCellsMonth, InventoryRow.Type.E.getSysId(), invColumn);
							if(savedCell != null) {
								cell.setValue(savedCell.getValue());
							}
						}
						break;
					}
					case B_ANNUALLY: {
						// supply
						if(supply.containsKey(invColumn.getSupplyAttrId())) {
							cell.setValue(new AEValue(AEMath.doubleValue(supply.get(invColumn.getSupplyAttrId()))));
						}
						break;
					}
					case C_ANNUALLY: {
						// sale;
						if(sale.containsKey(invColumn.getSaleAttrId())) {
							cell.setValue(new AEValue(AEMath.doubleValue(sale.get(invColumn.getSaleAttrId()))));
						}
						break;
					}
					case D_ANNUALLY: {
						// "Stock final comptable du mois";
						double d = InventoryCalculator.calculateRowDAnnually(invStatusData, invColumn);
						cell.setValue(new AEValue(d));
						break;
					}
					case E_ANNUALLY: {
						// "Stocks jaugés fin de mois";
						CFCCell savedCell = InventoryCalculator.getCFCCell(savedCells, InventoryRow.Type.E_ANNUALLY.getSysId(), invColumn);
						if(savedCell != null) {
							cell.setValue(savedCell.getValue());
						}
						break;
					}
					case F_ANNUALLY: {
						// "Ecart du mois";
						double d = InventoryCalculator.calculateRowFAnnually(invStatusData, invColumn);
						cell.setValue(new AEValue(d));
						break;
					}
					case G_ANNUALLY: {
						// "Ecart cumulés mois précédent";
						// Valu by default is H at the end from previous period.
						// User may modify them.
						CFCCell savedCell = InventoryCalculator.getCFCCell(savedCells, InventoryRow.Type.G_ANNUALLY.getSysId(), invColumn);
						if(savedCell != null) {
							cell.setValue(savedCell.getValue());
						} else {
							savedCell = InventoryCalculator.getCFCCell(prevSavedCellsMonth, InventoryRow.Type.H.getSysId(), invColumn);
							if(savedCell != null) {
								cell.setValue(savedCell.getValue());
							}
						}
						break;
					}
					case H_ANNUALLY: {
						// "Nouveaux écarts cumulés";
						double d = InventoryCalculator.calculateRowHAnnually(invStatusData, invColumn);
						cell.setValue(new AEValue(d));
						break;
					}
					case I_ANNUALLY: {
						// "Ecarts cumulés Etat de Stock";
						CFCCell savedCell = InventoryCalculator.getCFCCell(savedCells, InventoryRow.Type.I_ANNUALLY.getSysId(), invColumn);
						if(savedCell != null) {
							cell.setValue(savedCell.getValue());
						}
						break;
					}
					case J_ANNUALLY: {
						// "Différence";
						double d = InventoryCalculator.calculateRowJAnnually(invStatusData, invColumn);
						cell.setValue(new AEValue(d));
						break;
					}
					case K_ANNUALLY: {
						// "Ecart en 0/00";
						double d = InventoryCalculator.calculateRowKAnnually(invStatusData, invColumn);
						cell.setValue(new AEValue(d));
						break;
					}
					}

					// add the cell
					row.add(cell);
				}
				invStatusData.add(row);
			}

			// rerturn response
			return invStatusData;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse saveInventoryStatusCellAnnually(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			JSONObject arguments = aeRequest.getArguments();
			String field = arguments.optString("field");
			int sysId = arguments.getInt("sysId");
			long sOwnerId = arguments.getLong("sOwnerId");
			long ownerId = arguments.getLong("ownerId");

			// whether this user is ownered by this customer
			ap.ownershipValidator(sOwnerId);

			// whether this article is ownered by this customer
			if(sOwnerId != ownerId) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * prepare dao's
			 */
			InventoryDAO inventoryDAO = daoFactory.getInventoryDAO(localConnection);
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);

			// date
			AccPeriod accPeriod = getFirstOpenPeriod(ownerId, AEApp.INVENTORY_MODULE_ID, localConnection);
			AETimePeriod financialYearInfo = orgDAO.loadFinancialPeriod(Organization.lazyDescriptor(ownerId));
			AETimePeriod financialYear = 
				AEDateUtil.getFinancialFrenchYearForDate(
						financialYearInfo.getStartDate(), 
						(int) financialYearInfo.getDuration(), 
						accPeriod.getEndDate());
			Date date = financialYear.getEndDate();

			// column definition
			CFCColumn cfcColumn = inventoryDAO.loadColumnByOwnerAndIndex(
					ownerId, 
					Integer.parseInt(field.substring(6)));

			if(cfcColumn != null) {
				CFCCell cfcCell = new CFCCell(
						cfcColumn, 
						date, 
						sysId, 
						new AEValue(aeRequest.getArguments().optString("value"), cfcColumn.getValue().getXType()));

				localConnection.beginTransaction();
				inventoryDAO.updateInvCellValue(cfcCell);
				localConnection.commit();

				// create and rerturn response
				JSONObject payload = new JSONObject();
				payload.put("cfcCell", cfcCell.toJSONObject());
				return new AEResponse(payload);
			} else {
				throw new AEException("Internal Error: The CFC Column cannot be found.");
			}
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadMandatExt(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * prepare dao's
			 */
			MandatDAO mandatDAO = daoFactory.getMandatDAO(localConnection);

			/**
			 * local attributes
			 */
			long ownerId = aeRequest.getArguments().optLong("ownerId");
			boolean loadData = aeRequest.getArguments().optBoolean("loadData");

			/**
			 * determine AccPeriod
			 */
			boolean editable = !(aeRequest.getArguments().optBoolean("history"));
			AccPeriod accPeriod = null;
			if(editable) {
				// not history
				accPeriod = getFirstOpenPeriod(ownerId, AEApp.MANDAT_MODULE_ID, localConnection);
			} else {
				// history
				int month = aeRequest.getArguments().getInt("month");
				int year = aeRequest.getArguments().getInt("year");

				Date startDate = AEDateUtil.getFirstDate(month - 1, year);		
				Date endDate = AEDateUtil.getLastDate(month - 1, year);
				accPeriod = daoFactory.getAccPeriodDAO(localConnection).loadAccPeriod(
						startDate, 
						endDate, 
						AEApp.MANDAT_MODULE_ID,
						ownerId);
			}
			if(accPeriod == null) {
				throw new AEException(
						AEError.System.ACC_PERIOD_WAS_NOT_FOUD.getSystemID(), 
				"La période n'a pas été trouvé.");
			}

			/**
			 * load Mandat
			 */
			Mandat mandat = mandatDAO.loadMandat(Organization.lazyDescriptor(ownerId));

			if(mandat != null) {
				mandat.setCfcModel(mandatDAO.loadMandatModel(mandat.getDescriptor()));
			}

			if(mandat != null && loadData) {
				CFCData mandatData = loadMandatData(mandat, accPeriod, invContext, localConnection);
				mandat.setCFCData(mandatData);
			}

			/**
			 * load MandatExt
			 */
			MandatExt mandatExt = mandatDAO.loadMandatExt(Organization.lazyDescriptor(ownerId));

			if(mandatExt != null) {
				mandatExt.setCfcModel(mandatDAO.loadMandatModelExt(mandatExt.getDescriptor()));
			}

			if(mandatExt != null && loadData) {
				CFCData mandatDataExt = loadMandatDataExt(mandatExt, mandat, accPeriod, invContext, localConnection);
				mandatExt.setCFCData(mandatDataExt);
			}

			/**
			 * Load accounts
			 */
			JSONArray allCashAcounts = new JSONArray();
			AEResponse aeResponseGOA = accLocal.loadGOA(aeRequest);
			JSONArray assetsArray = aeResponseGOA.getPayload().optJSONObject("assetsGOA").optJSONArray("accounts");
			if(assetsArray != null) {
				for(int i = 0; i < assetsArray.length(); i++) {
					allCashAcounts.put(assetsArray.getJSONObject(i));
				}
			}
			JSONArray expensesArray = aeResponseGOA.getPayload().optJSONObject("expensesGOA").optJSONArray("accounts");
			if(expensesArray != null) {
				for(int i = 0; i < expensesArray.length(); i++) {
					allCashAcounts.put(expensesArray.getJSONObject(i));
				}
			}
			AccountDAO accDAO = daoFactory.getAccountDAO(localConnection);
			JSONArray accountsBank = accDAO.loadBankAccounts(ownerId);

			// create and rerturn response
			JSONObject payload = new JSONObject();
			if(mandat != null) {
				payload.put("Mandat", mandat.toJSONObject());
			}
			if(mandatExt != null) {
				payload.put("MandatExt", mandatExt.toJSONObject());
			}
			payload.put("accounts", allCashAcounts);
			payload.put("accountsBank", accountsBank);
			return new AEResponse(payload);
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse saveMandatExt(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			/**
			 * prepare dao's
			 */
			MandatDAO mandatDAO = daoFactory.getMandatDAO(localConnection);

			/**
			 * local attributes
			 */
			CFC mandat = new CFC();
			mandat.create(aeRequest.getArguments().getJSONObject("Mandat"));

			// save CFC
			if(mandat.isNew()) {
				mandatDAO.insertMandat(mandat);
			} else if(mandat.isUpdated()) {
				mandatDAO.updateMandat(mandat);
			}

			// save CFCModel
			if(mandat.getCfcModel() != null) {
				CFCModel mandatModel = mandat.getCfcModel();
				for (Iterator<CFCColumn> iterator = mandatModel.iterator(); iterator.hasNext();) {
					CFCColumn mandatColumn = (CFCColumn) iterator.next();
					mandatColumn.setToCFC(mandat.getDescriptor());
					switch(mandatColumn.getPersistentState()) {
					case NEW:
						mandatDAO.insertMandatColumn(mandatColumn);
						break;
					case UPDATED:
						mandatDAO.updateMandatColumn(mandatColumn);
						break;
					case DELETED:
						mandatDAO.deleteMandatColumn(mandatColumn.getDescriptor());
						iterator.remove();
						break;
					}
				}
			}

			// commit
			localConnection.commit();

			// create and rerturn response
			JSONObject payload = new JSONObject();
			if(mandat != null) {
				payload.put("Mandat", mandat.toJSONObject());
			}
			return new AEResponse(payload);
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse saveMandatCellExt(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * prepare dao's
			 */
			MandatDAO mandatDAO = daoFactory.getMandatDAO(localConnection);

			/**
			 * local attributes
			 */
			long ownerId = aeRequest.getArguments().optLong("ownerId");
			String field = aeRequest.getArguments().optString("field");
			Date date = AEDateUtil.parseDateStrict(aeRequest.getArguments().optString("date"));
			int rowIndex = aeRequest.getArguments().optInt("rowIndex");
			long gridId = aeRequest.getArguments().getLong("gridId");

			// date and column
			CFCColumn mandatColumn = null;
			if(date == null && gridId == 10) {
				AccPeriod accPeriod = getFirstOpenPeriod(ownerId, AEApp.MANDAT_MODULE_ID, localConnection);
				date = accPeriod.getEndDate();
				mandatColumn = mandatDAO.loadColumnByOwnerAndIndexExt(ownerId, Integer.parseInt(field.substring(6)));
			} else {
				mandatColumn = mandatDAO.loadColumnByOwnerAndId(ownerId, Long.parseLong(field.substring(6)));
			}

			// update cell value
			if(mandatColumn != null) {
				String strValue = aeRequest.getArguments().optString("value");
				AEValue aeValue = null;
				if(!AEStringUtil.isEmpty(strValue)) {
					aeValue = new AEValue(aeRequest.getArguments().optString("value"), mandatColumn.getValue().getXType());
				}

				CFCCell mandatCell = new CFCCell(
						mandatColumn, 
						date, 
						rowIndex, 
						aeValue,
						gridId);

				localConnection.beginTransaction();
				mandatDAO.updateMandatCellValue(mandatCell);
				localConnection.commit();

				// create and rerturn response
				JSONObject payload = new JSONObject();
				payload.put("MandatCell", mandatCell.toJSONObject());
				return new AEResponse(payload);
			} else {
				throw new AEException("Internal Error: The CFC Column cannot be found.");
			}
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse closingMandatPeriodExt(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();

			/**
			 * prepare dao's
			 */
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);

			/**
			 * owner
			 */
			OrganizationDAO orgDAO = DAOFactory.getInstance().getOrganizationDAO(localConnection);
			AEDescriptor customerDescr = Organization.lazyDescriptor(aeRequest.getArguments().getLong("ownerId"));
			JSONArray customersArray = orgDAO.loadCustomer(customerDescr);
			JSONObject customer = customersArray.getJSONObject(0);
			customerDescr.setName(customer.optString("name"));

			/**
			 * determine period
			 */
			AccPeriod accPeriod = getFirstOpenPeriod(
					customerDescr.getID(), 
					AEApp.MANDAT_MODULE_ID, 
					localConnection);

			// we can close only finished periods
			Date nowDate = AEDateUtil.getClearDate(new Date());
			if(nowDate.before(AEDateUtil.getClearDate(accPeriod.getEndDate()))) {
				throw new AEException(
						(int) AEError.System.CANNOT_CLOSE_UNFINISHED.getSystemID(), 
				"La période n'est pas terminée.");
			}

			/**
			 * Load Mandat 
			 */
			MandatDAO mandatDAO = daoFactory.getMandatDAO(localConnection);
			Mandat mandat = mandatDAO.loadMandat(Organization.lazyDescriptor(customerDescr.getID()));

			if(mandat != null) {
				mandat.setCfcModel(mandatDAO.loadMandatModel(mandat.getDescriptor()));
			}

			if(mandat != null) {
				CFCData mandatData = loadMandatData(mandat, accPeriod, invContext, localConnection);
				mandat.setCFCData(mandatData);
			}

			// keep the last values
			Date lastDay = accPeriod.getEndDate();
			CFCModel mandatModel = mandat.getCfcModel();
			CFCData  mandatData = mandat.getCfcData();
			CFCRow lastRow = mandatData.getRow(lastDay, 0);
			if(lastRow != null) {
				for (CFCColumn cfcColumn : mandatModel) {
					CFCCell mandatCell = lastRow.getCell(cfcColumn.getID()); 
					if(mandatCell != null) {
						mandatDAO.updateMandatCellValue(mandatCell);
					}
				}
			}

			/**
			 * Load Mandat Ext
			 */
			MandatExt mandatExt = mandatDAO.loadMandatExt(Organization.lazyDescriptor(customerDescr.getID()));

			if(mandatExt != null) {
				mandatExt.setCfcModel(mandatDAO.loadMandatModelExt(mandatExt.getDescriptor()));
			}

			if(mandatExt != null) {
				CFCData mandatDataExt = loadMandatDataExt(mandatExt, mandat, accPeriod, invContext, localConnection);
				mandatExt.setCFCData(mandatDataExt);
			}

			// keep the values
			CFCModel mandatModelExt = mandatExt.getCfcModel();
			CFCData  mandatDataExt = mandatExt.getCfcData();
			for (CFCRow cfcRow : mandatDataExt) {
				for (CFCColumn cfcColumn : mandatModelExt) {
					if(!CFCColumn.NType.LABEL.equals(cfcColumn.getNType())) {
						CFCCell mandatCell = cfcRow.getCell(cfcColumn.getID()); 
						if(mandatCell != null && !AEValue.isNull(mandatCell.getValue())) {
							mandatDAO.updateMandatCellValue(mandatCell);
						}
					}
				}
			}

			/**
			 * close the period
			 */
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
			try {
				// send e-mail
				Emailer emailer = new Emailer();
				emailer.onPeriodClosed(customerDescr, invContext, accPeriod, AEApp.MANDAT_MODULE_ID);

				// auto export
				JSONObject exportRequestArguments = new JSONObject();
				exportRequestArguments.put("accPeriodId", accPeriod.getID());
				if("secal.png".equalsIgnoreCase(invContext.getAuthPrincipal().getDescription())) {
					exportRequestArguments.put("exportTo", "CEGID");
				} else if("gianati.png".equalsIgnoreCase(invContext.getAuthPrincipal().getDescription())) {
					exportRequestArguments.put("exportTo", "SAGE");
				}
				exportRequestArguments.put("ownerId", aeRequest.getArguments().getLong("ownerId"));
				AERequest exportRequest = new AERequest(exportRequestArguments);
				accLocal.export(exportRequest, invContext);
			} catch (Throwable t) {
				t.printStackTrace();
			}

			/**
			 * return response
			 */
			JSONObject payload = new JSONObject();
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
	public AEResponse openMandatPeriodExt(AERequest aeRequest) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContext invContext = new AEInvocationContextImp(aeRequest.getAuthPrincipal());
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();

			/**
			 * determine period
			 */
			AccPeriod accPeriod = getLastClosedPeriod(
					aeRequest.getArguments().getLong("ownerId"), 
					AEApp.MANDAT_MODULE_ID, 
					localConnection);

			if(accPeriod != null) {
				localConnection.beginTransaction();

				/**
				 * delete general journal
				 */
				AccJournalDAO journalDAO = daoFactory.getAccJournalDAO(localConnection);
				journalDAO.deleteByAccPeriodId(accPeriod.getID());

				/**
				 * open the period
				 */
				AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
				accPeriodDAO.openPeriod(accPeriod.getID());
				accPeriodDAO.notExportedPeriod(accPeriod.getID());

				/**
				 * Delete attachments
				 */
				FileAttachmentDAO fileAttachmentDAO = daoFactory.getFileAttachmentDAO(localConnection);
				fileAttachmentDAO.deleteTo(accPeriod.getDescriptor());
			}

			/**
			 * commit transaction and return response
			 */
			localConnection.commit();

			/**
			 * return response
			 */
			JSONObject payload = new JSONObject();
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
	public CFCData loadMandatDataExt(CFC mandatExt, CFC mandat, AccPeriod accPeriod, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {

		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			/**
			 * prepare dao's
			 */
			MandatDAO mandatDAO = daoFactory.getMandatDAO(localConnection);
			//			CashModuleDAO cashModuleDAO = daoFactory.getCashModuleDAO(localConnection);

			/**
			 * local attributes
			 */

			/**
			 * create spread sheet
			 */
			//			// load cash journal
			//			CashJournalEntriesList dbCashEntriesList =
			//				cashModuleDAO.loadJournalEntries(
			//						mandat.getCompany().getDescriptor().getID(), 
			//						accPeriod.getStartDate(), 
			//						accPeriod.getEndDate());

			// load saved cells
			ArrayList<CFCCell> cellsDBList = mandatDAO.loadMandatCells(
					mandatExt.getDescriptor(),
					AEDateUtil.addDaysToDate(accPeriod.getStartDate(), -1),
					accPeriod.getEndDate(),
					10);

			/** 
			 * Process rows
			 */
			CFCData mandatData = loadMandatExtDataModel((MandatExt) mandatExt, accPeriod);
			for (CFCRow cfcRow : mandatData) {
				for (CFCCell cfcCell : cfcRow) {
					try {
						CFCCell savedCell = getSavedCFCCell(
								accPeriod.getEndDate(), 
								cfcRow.getIndex(), 
								cfcCell.getColumn().getID(), 
								cellsDBList);
						if(savedCell != null) {
							cfcCell.setValue(savedCell.getValue());
						}
						switch(cfcRow.getIndex()) {
						// row 1
						case 0: {
							switch(cfcCell.getColumn().getColIndex()) {
							case 0: {
								// (1, A)
								cfcCell.setValue(new AEValue("Solde initial:", AEValue.XType.STRING));
								break;
							}
							case 1: {
								// (1, B)
								if(savedCell == null) {
									// Equal to "Solde final" from the previous month, 
									// but user may enter another value.
									// so there is no entered value, get the "Solde final"
									// from previous month
									CFCCell closedBalPrevMonth = getSavedCFCCell(
											AEDateUtil.addDaysToDate(accPeriod.getStartDate(), -1), // previous month
											cfcRow.getIndex(), // the same row
											8, // the column id
											cellsDBList);
									if(closedBalPrevMonth != null) {
										cfcCell.setValue(closedBalPrevMonth.getValue());
									}
								}
								break;
							}
							case 2: {
								// (1, C)
								cfcCell.setValue(new AEValue("Loyer fixe:", AEValue.XType.STRING));
								break;
							}
							case 3: {
								// (1, D)
								break;
							}
							case 4: {
								// (1, E)
								cfcCell.setValue(new AEValue("Loyer var:", AEValue.XType.STRING));
								break;
							}
							case 5: {
								// (1, F)
								break;
							}
							case 6: {
								// (1, G)
								cfcCell.setValue(new AEValue("Solde Final:", AEValue.XType.STRING));
								break;
							}
							case 7: {
								// (1, H)
								break;
							}
							case 8: {
								// (1, I)
								cfcCell.setValue(new AEValue("", AEValue.XType.STRING));
								break;
							}
							case 9: {
								// (1, J)
								break;
							}
							}
							break;
						}
						// row 2
						case 1: {
							switch(cfcCell.getColumn().getColIndex()) {
							case 0: {
								// (1, A)
								cfcCell.setValue(new AEValue("Com fixe:", AEValue.XType.STRING));
								break;
							}
							case 1: {
								break;
							}
							case 2: {
								// (1, C)
								cfcCell.setValue(new AEValue("Com var:", AEValue.XType.STRING));
								break;
							}
							case 3: {
								// (1, D)
								break;
							}
							case 4: {
								// (1, E)
								cfcCell.setValue(new AEValue("Pertes carb.", AEValue.XType.STRING));
								break;
							}
							case 5: {
								// (1, F)
								break;
							}
							case 6: {
								// (1, G)
								cfcCell.setValue(new AEValue("Boni carb.", AEValue.XType.STRING));
								break;
							}
							case 7: {
								// (1, H)
								// Solde Final
								// =+B2+C33+D2+F2-J33-M33-N33-B3-D3+F3-H3-J3
								break;
							}
							case 8: {
								// (1, I)
								cfcCell.setValue(new AEValue("Contrib. RDC", AEValue.XType.STRING));
								break;
							}
							case 9: {
								// (1, J)
								break;
							}
							}
							break;
						}
						}
					} catch(Exception e) {
						cfcCell.setValue(new AEValue("#ERR"));
					}
				}
			}

			// calculates Solde Final
			if(mandatData != null && mandat != null && mandat.getCfcData() != null) {
				CFCCell soldeFinal = mandatData.getCell(0, 7);
				if(soldeFinal != null) {
					// =+C4+E4+G4+G5+D41+E41-F41-C5-E5-I5-K5-K41-M41
					double value = 0.0;

					CFCCell tmpCell = null;

					// +C4
					tmpCell = mandatData.getCell(0, 1);
					if(tmpCell != null && !AEValue.isNull(tmpCell.getValue())) {
						value += tmpCell.getValue().optDouble();
					}

					// +E4
					tmpCell = mandatData.getCell(0, 3);
					if(tmpCell != null && !AEValue.isNull(tmpCell.getValue())) {
						value += tmpCell.getValue().optDouble();
					}

					// +G4
					tmpCell = mandatData.getCell(0, 5);
					if(tmpCell != null && !AEValue.isNull(tmpCell.getValue())) {
						value += tmpCell.getValue().optDouble();
					}

					// +G5
					tmpCell = mandatData.getCell(1, 5);
					if(tmpCell != null && !AEValue.isNull(tmpCell.getValue())) {
						value += tmpCell.getValue().optDouble();
					}

					// +D41
					tmpCell = mandat.getCfcData().getCellBySysId(CFCRow.SUM, 5);
					if(tmpCell != null && !AEValue.isNull(tmpCell.getValue())) {
						value += tmpCell.getValue().optDouble();
					}

					// +E41
					tmpCell = mandat.getCfcData().getCellBySysId(CFCRow.SUM, 8);
					if(tmpCell != null && !AEValue.isNull(tmpCell.getValue())) {
						value += tmpCell.getValue().optDouble();
					}
					
					// -F41
					tmpCell = mandat.getCfcData().getCellBySysId(CFCRow.SUM, 10);
					if(tmpCell != null && !AEValue.isNull(tmpCell.getValue())) {
						value -= Math.abs(tmpCell.getValue().optDouble());
					}

					// -C5
					tmpCell = mandatData.getCell(1, 1);
					if(tmpCell != null && !AEValue.isNull(tmpCell.getValue())) {
						value -= tmpCell.getValue().optDouble();
					}

					// -E5
					tmpCell = mandatData.getCell(1, 3);
					if(tmpCell != null && !AEValue.isNull(tmpCell.getValue())) {
						value -= tmpCell.getValue().optDouble();
					}

					// -I5
					tmpCell = mandatData.getCell(1, 7);
					if(tmpCell != null && !AEValue.isNull(tmpCell.getValue())) {
						value -= tmpCell.getValue().optDouble();
					}

					// -K5
					tmpCell = mandatData.getCell(1, 9);
					if(tmpCell != null && !AEValue.isNull(tmpCell.getValue())) {
						value -= tmpCell.getValue().optDouble();
					}

					// -K41
					tmpCell = mandat.getCfcData().getCellBySysId(CFCRow.SUM, 19);
					if(tmpCell != null && !AEValue.isNull(tmpCell.getValue())) {
						value -= tmpCell.getValue().optDouble();
					}

					// -M41
					tmpCell = mandat.getCfcData().getCellBySysId(CFCRow.SUM, 21);
					if(tmpCell != null && !AEValue.isNull(tmpCell.getValue())) {
						value -= tmpCell.getValue().optDouble();
					}

//					// -N41
//					tmpCell = mandat.getCfcData().getCellBySysId(CFCRow.SUM, 16);
//					if(tmpCell != null && !AEValue.isNull(tmpCell.getValue())) {
//						value -= tmpCell.getValue().optDouble();
//					}

					soldeFinal.setValue(new AEValue(value));
				}
			}

			// rerturn response
			return mandatData;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	private CFCData loadMandatExtDataModel(MandatExt mandatExt, AccPeriod accPeriod) {
		CFCData mandatDataExtDataModel = new CFCData();

		// reused variables
		CFCRow row = null;

		/**
		 * Row 1
		 */
		row = new CFCRow(accPeriod.getEndDate(), 0);
		mandatDataExtDataModel.add(row);

		// process columns
		for (CFCColumn column : mandatExt.getCfcModel()) {
			// for every column

			// create the cell at (row.getIndex(), column.getColIndex())
			CFCCell cell = new CFCCell();
			cell.setEntryDate(accPeriod.getEndDate());
			cell.setColumn(column);
			cell.setToCFC(mandatExt.getDescriptor());
			cell.setRowIndex(row.getIndex());
			cell.setColumnId(column.getID());
			cell.setGridId(10);

			// add the cell
			row.add(cell);
		}

		/**
		 * Row 2
		 */
		row = new CFCRow(accPeriod.getEndDate(), 1);
		mandatDataExtDataModel.add(row);

		// process columns
		for (CFCColumn column : mandatExt.getCfcModel()) {
			// for every column

			// create the cell at (row.getIndex(), column.getColIndex())
			CFCCell cell = new CFCCell();
			cell.setEntryDate(accPeriod.getEndDate());
			cell.setColumn(column);
			cell.setToCFC(mandatExt.getDescriptor());
			cell.setRowIndex(row.getIndex());
			cell.setColumnId(column.getID());
			cell.setGridId(10);

			// add the cell
			row.add(cell);
		}

		return mandatDataExtDataModel;
	}
	
	@Timeout
	public void timeout(Timer timer) {
	    System.out.println("TimerBean: timeout occurred");
	}

	@Override
	public AEResponse saveBankBalances(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			/**
			 * Arguments
			 */
			long ownerId = aeRequest.getArguments().optLong("ownerId");
			long sOwnerId = aeRequest.getArguments().getLong("sOwnerId");
			long bankAccountId = aeRequest.getArguments().optLong("bankAccountId");
			JSONArray bbJSONArray = aeRequest.getArguments().getJSONArray("items");

			// Validate
			ap.ownershipValidator(ownerId);
			
			// authorize 
			authorize(new AuthPermission("System/Configuration/Customer/BankAccount", AuthPermission.SAVE_AND_DELETE), invContext, Organization.lazyDescriptor(ownerId));
			
			if(ownerId != sOwnerId) {
				throw AEError.System.SECURITY_VIOLATION.toException();
			}
			
			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
						
			// dao's
			BankAccountDAO bankAccDAO = daoFactory.getBankAccountDAO(localConnection);
			
			// check tenant
			if(!bankAccDAO.checkTenant(BankAccount.lazyDescriptor(bankAccountId), Organization.lazyDescriptor(ownerId))) {
				throw AEError.System.INVALID_PARAMETER.toException();
			}

			// create BankAccountBalancesList and remove void items
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);
			BankAccountBalancesList baBalancesList = new BankAccountBalancesList();
			
			baBalancesList.create(bbJSONArray);
			
			for (Iterator<BankAccountBalance> iterator = baBalancesList.iterator(); iterator.hasNext();) {
				BankAccountBalance bab = (BankAccountBalance) iterator.next();
				
				// date management
				if(bab.getBankFinalBalanceDate() == null && !bab.isPersistent()) {
					iterator.remove();
				}
			}
			
			// Save in transaction
			localConnection.beginTransaction();
			BankAccountDAO babDAO = daoFactory.getBankAccountDAO(localConnection);
			for (Iterator<BankAccountBalance> iterator = baBalancesList.iterator(); iterator.hasNext();) {
				BankAccountBalance bab = (BankAccountBalance) iterator.next();
				
				/**
				 * AccPeriod
				 */
				AccPeriod accPeriod = getAccPeriod(
						ownerId, 
						AEApp.ACCOUNTING_MODULE_ID, 
						bab.getBankFinalBalanceDate(), 
						localConnection);
				
				// validate in open period
				if(accPeriod != null && accPeriod.isClosed()) {
					throw AEError.System.CANNOT_INSERT_UPDATE_CLOSED_PERIOD.toException();
				}
				
				switch(bab.getPersistentState()) {
					case NEW:
						ensureChange(bab, bankAccountId, ownerDescr, localConnection);
						babDAO.insert(bab);
						break;
					case UPDATED:
						ensureChange(bab, bankAccountId, ownerDescr, localConnection);
						babDAO.update(bab);
						break;
					case DELETED:
						ensureChange(bab, bankAccountId, ownerDescr, localConnection);
						babDAO.delete(bab.getDescriptor());
						iterator.remove();
						break;
					default:
						break;
				}
			}

			// commit
			localConnection.commit();

			// create and rerturn response
			JSONObject payload = new JSONObject();
			return new AEResponse(payload);
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			logger.error(t);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	/**
	 * Provides safeguard changes to specified <code>bab</code>.
	 * 
	 * @param bab
	 * @param bankAccountId
	 * @param ownerDescr
	 * @param localConnection
	 * @throws AEException
	 */
	private void ensureChange(BankAccountBalance bab, long bankAccountId, AEDescriptor ownerDescr, AEConnection localConnection) throws AEException {
		// bank account is defined
		if(bab.getBankAccount() == null || bab.getBankAccount().getDescriptor().getID() != bankAccountId) {
			throw AEError.System.INVALID_PARAMETER.toException();
		}
		
		// date management
		if(bab.getBankFinalBalanceDate() == null) {
			throw new AEException("Transaction invalide (Date de valeur).");
		}
		
		// check period
		AccPeriod accPeriod = getAccPeriod(
				ownerDescr.getID(), 
				AEApp.ACCOUNTING_MODULE_ID, 
				AEDateUtil.getClearDate(bab.getBankFinalBalanceDate()), 
				localConnection);
		
		// process null period
		if(accPeriod == null) {
			accPeriod = createAccountingPeriod(ownerDescr, bab.getBankFinalBalanceDate(), localConnection);
		}
		
		// changes in closed period are forbidden
		if(bab.getPersistentState() != null && !AEPersistent.State.VIEW.equals(bab.getPersistentState())) {
			isInPermittedAccountingPeriods(ownerDescr, bab.getBankFinalBalanceDate(), localConnection);
		}
		
		bab.setAccPeriod(accPeriod.getDescriptor());
	}
	
	@Override
	public AEResponse loadBankBalances(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			// arguments
			long ownerId = aeRequest.getArguments().getLong("ownerId");
			long sOwnerId = aeRequest.getArguments().getLong("sOwnerId");
			long bankAccountId = aeRequest.getArguments().getLong("bankAccountId");
			int year = aeRequest.getArguments().optInt("year", AEDateUtil.getYear(new Date()));

			// validate
			ap.ownershipValidator(ownerId);
			
			// authorize 
			authorize(new AuthPermission("System/Configuration/Customer/BankAccount", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));
			
			if(ownerId != sOwnerId) {
				throw AEError.System.SECURITY_VIOLATION.toException();
			}
			
			/**
			 * get connection to the DB
			 */	
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			// dao's
			BankAccountDAO bankAccDAO = daoFactory.getBankAccountDAO(localConnection);
			
			// check tenant
			if(!bankAccDAO.checkTenant(BankAccount.lazyDescriptor(bankAccountId), Organization.lazyDescriptor(ownerId))) {
				throw AEError.System.SECURITY_VIOLATION.toException();
			}

			// load items
			BankAccountBalancesList baBalancesList = bankAccDAO.loadBankAccountBalances(
					BankAccount.lazyDescriptor(bankAccountId), 
					year);

			// create and rerturn response
			JSONObject payload = new JSONObject()
				.put("bankAccountId", bankAccountId)
				.put("year", year)
				.put("items", baBalancesList.toJSONArray());
			return new AEResponse(payload);
		} catch (Throwable t) {
			logger.error(t);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
}