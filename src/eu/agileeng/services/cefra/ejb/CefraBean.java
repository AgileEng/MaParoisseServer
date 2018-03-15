package eu.agileeng.services.cefra.ejb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONObject;
import org.jboss.logging.Logger;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.AccAccountBalancesList;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.acc.AccAccountBalance;
import eu.agileeng.domain.acc.AccAccountBalanceExt;
import eu.agileeng.domain.acc.AccJournalResultsList;
import eu.agileeng.domain.acc.cashbasis.AccJournalEntryTemplate.JournalIdentificationRule;
import eu.agileeng.domain.acc.cashbasis.FinancialTransaction;
import eu.agileeng.domain.acc.cashbasis.Quete;
import eu.agileeng.domain.acc.cashbasis.QuetesList;
import eu.agileeng.domain.business.bank.BankAccount;
import eu.agileeng.domain.business.bank.BankAccountBalance;
import eu.agileeng.domain.cefra.n11580_03.BalanceDataSource;
import eu.agileeng.domain.cefra.n11580_03.BalanceRequest;
import eu.agileeng.domain.cefra.n11580_03.BilanDataSource;
import eu.agileeng.domain.cefra.n11580_03.BilanRequest;
import eu.agileeng.domain.cefra.n11580_03.BordereauParoisseDataSource;
import eu.agileeng.domain.cefra.n11580_03.BordereauParoisseRequest;
import eu.agileeng.domain.cefra.n11580_03.BudgetRealizationDataSource;
import eu.agileeng.domain.cefra.n11580_03.BudgetRealizationRequest;
import eu.agileeng.domain.cefra.n11580_03.Cefra11580_03DataSource;
import eu.agileeng.domain.cefra.n11580_03.Cefra11580_03Request;
import eu.agileeng.domain.cefra.n11580_03.CompteDeResultatDataSource;
import eu.agileeng.domain.cefra.n11580_03.CompteDeResultatRequest;
import eu.agileeng.domain.cefra.n11580_03.CouncilDataSource;
import eu.agileeng.domain.cefra.n11580_03.CouncilRequest;
import eu.agileeng.domain.cefra.n11580_03.DonorsDataSource;
import eu.agileeng.domain.cefra.n11580_03.DonorsRequest;
import eu.agileeng.domain.cefra.n11580_03.FinancesDataSource;
import eu.agileeng.domain.cefra.n11580_03.FinancesRequest;
import eu.agileeng.domain.cefra.n11580_03.GrandLivreDataSource;
import eu.agileeng.domain.cefra.n11580_03.GrandLivreRequest;
import eu.agileeng.domain.cefra.n11580_03.JournauxDataSource;
import eu.agileeng.domain.cefra.n11580_03.JournauxRequest;
import eu.agileeng.domain.contact.Contributor;
import eu.agileeng.domain.contact.Employee;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.contact.Person;
import eu.agileeng.domain.council.Council;
import eu.agileeng.domain.document.AEDocument;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.AEDocumentsList;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.acc.AccJournalDAO;
import eu.agileeng.persistent.dao.acc.AccountDAO;
import eu.agileeng.persistent.dao.acc.BordereauParoisseDAO;
import eu.agileeng.persistent.dao.cash.BankAccountDAO;
import eu.agileeng.persistent.dao.common.ContributorDAO;
import eu.agileeng.persistent.dao.document.statement.AEStatementDAO;
import eu.agileeng.security.AuthPermission;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.services.AEBean;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AEInvocationContextValidator;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.acc.ejb.AccLocal;
import eu.agileeng.services.cash.ejb.CashLocal;
import eu.agileeng.services.council.ejb.CouncilLocal;
import eu.agileeng.services.party.ejb.PartyLocal;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AETimePeriod;
import eu.agileeng.util.json.JSONUtil;

@Stateless
public class CefraBean extends AEBean implements CefraLocal {

	private static final long serialVersionUID = 6060394363157351985L;

	protected static Logger logger = Logger.getLogger(CefraBean.class);
	
	@EJB
	private PartyLocal partyLocal;
	
	@EJB
	private AccLocal accLocal;
	
	@EJB
	private CouncilLocal councilLocal;
	
	@EJB 
	private CashLocal cashLocal;
	
	@Override
	public Cefra11580_03DataSource generateDataSource(Cefra11580_03Request request, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			// whether this principal is authorized for specified tenant
			long ownerId = request.getCompany().getDescriptor().getID();
			ap.ownershipValidator(ownerId);
			
			/**
			 * Authorize 
			 */
			authorize(new AuthPermission("System/Cefra/11580_03", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * DAO Factory and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * 
			 */
			Cefra11580_03DataSource dataSource = new Cefra11580_03DataSource();
			
			// user entered data
//			dataSource.setNature(request.getNature());
//			dataSource.setPaymentMethod(request.getPaymentMethod());
			
			// customer
			AERequest customerRequest = new AERequest(
					new JSONObject().put(Organization.JSONKey.customerId, request.getCompany().getDescriptor().getID())
					.put(Organization.JSONKey.doNotLoadCAO, true)
					.put(Organization.JSONKey.doNotLoadSocialInfo, true));
			AEResponse customerResponse = partyLocal.loadCustomer(customerRequest, invContext);
			dataSource.setCustomer(customerResponse.getPayload().getJSONObject(Organization.JSONKey.customer));
			
			// contributor
			ContributorDAO contrDAO = daoFactory.getContributorDAO(localConnection);
			Contributor contributor = contrDAO.loadContributor(request.getContributor());
			dataSource.setContributor(contributor);
			
			// docNumber
			dataSource.setDocNumber(request.getDocNumber());
			
			// period
			AETimePeriod period = new AETimePeriod(AEDateUtil.beginOfTheYear(request.getDateTo()), (request.getDateTo()));
			dataSource.setPeriod(period);
			
			// year
			dataSource.setYear(request.getYear());
			
			// donations
			dataSource.setDonations(contrDAO.loadContributorDonations(
					request.getCompany().getDescriptor(), 
					contributor.getEmployee().getPerson().getDescriptor(), 
					request.getYear()));
			
			// payment methods
			Set<JournalIdentificationRule> rules = 
					contrDAO.loadJournalIdentRules(
							request.getCompany().getDescriptor(), 
							contributor.getEmployee().getPerson().getDescriptor(), 
							request.getYear());
			for (JournalIdentificationRule rule : rules) {
				if(JournalIdentificationRule.CA.equals(rule)) {
					dataSource.setPayCashMethod(true);
				} else {
					dataSource.setPayOtherMethod(true);
				}
			}
			if(dataSource.isPayCashMethod() == false && dataSource.isPayOtherMethod() == false) {
				dataSource.setPayOtherMethod(true);
			}
			
			// update receipted (not very good implementation)
			contrDAO.updateReceipted(dataSource.getDonations());

			// build response
			return dataSource;
		} catch (Exception e) {
			logger.error(e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public BordereauParoisseDataSource generateDataSource(BordereauParoisseRequest request, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			// whether this principal is authorized for specified tenant
			long ownerId = request.getCompany().getDescriptor().getID();
			ap.ownershipValidator(ownerId);
			
			/**
			 * Authorize 
			 */
			authorize(new AuthPermission("System/Cefra/BordereauParoisse", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * DAO Factory and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * 
			 */
			BordereauParoisseDataSource dataSource = new BordereauParoisseDataSource();
			
			// customer
			AERequest customerRequest = new AERequest(
					new JSONObject().put(Organization.JSONKey.customerId, request.getCompany().getDescriptor().getID())
					.put(Organization.JSONKey.doNotLoadCAO, true)
					.put(Organization.JSONKey.doNotLoadSocialInfo, true));
			AEResponse customerResponse = partyLocal.loadCustomer(customerRequest, invContext);
			dataSource.setCustomer(customerResponse.getPayload().getJSONObject(Organization.JSONKey.customer));
			
			// year
			dataSource.setYear(request.getYear());
			
			// quetesList
			BordereauParoisseDAO bordereauParoisseDAO = daoFactory.getBordereauParoisseDAO(localConnection);
			QuetesList quetesList = bordereauParoisseDAO.loadQuetes(request.getCompany().getDescriptor(), request.getYear());
			dataSource.setQuetesList(quetesList);
			
			// group by amounts
			AccAccountBalancesList accAccountBalances = new AccAccountBalancesList();

			AccAccountBalance accAccBalance4671 = new AccAccountBalance();
			accAccBalance4671.setAccAccount(AccAccount.lazyDescriptor(-4671).withCode("4671").withName("Archevêché"));
			
			AccAccountBalance accAccBalance4020 = new AccAccountBalance();
			accAccBalance4020.setAccAccount(AccAccount.lazyDescriptor(-4020).withCode("4020").withName("Quêtes à reverser"));
			
			// Bug 201803 
			// The amount on Pax CHRISTI (with code 04) should go into line “Don’t Quêtes à reverser (Compte 4020)”
			for (Quete quete : quetesList) {
				switch (quete.getCode()) {
					case "07":
					case "11":
					case "01":
					case "09":
					case "08":
					case "13":
					case "03":
					case "06":
					case "10":
					case "12":
					case "24":
					case "04":
						accAccBalance4020.setCreditTurnover(accAccBalance4020.getCreditTurnover() + quete.getAmount());
					break;
					case "14":
					case "15":
					case "16":
					case "18":
					case "19":
					case "20":
					case "22":
					case "17":
//					case "04":
						accAccBalance4671.setCreditTurnover(accAccBalance4671.getCreditTurnover() + quete.getAmount());
					break;
					default:
						break;
				}
			}
			
			if(!AEMath.isZeroAmount(accAccBalance4020.getCreditTurnover())) {
				accAccBalance4020.calculateFinalBalance();
				accAccountBalances.add(accAccBalance4020);
			}
			
			if(!AEMath.isZeroAmount(accAccBalance4671.getCreditTurnover())) {
				accAccBalance4671.calculateFinalBalance();
				accAccountBalances.add(accAccBalance4671);
			}
			
			dataSource.setAccAccountBalances(accAccountBalances);

			// build response
			return dataSource;
		} catch (Exception e) {
			logger.error("generateDataSource failed: ", e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public CompteDeResultatDataSource generateDataSource(CompteDeResultatRequest request, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			// whether this principal is authorized for specified tenant
			long ownerId = request.getCompany().getDescriptor().getID();
			ap.ownershipValidator(ownerId);
			
			/**
			 * Authorize 
			 */
			authorize(new AuthPermission("System/Cefra/CompteDeResultat", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * DAO Factory and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * 
			 */
			CompteDeResultatDataSource dataSource = new CompteDeResultatDataSource();
			
			// customer
			AERequest customerRequest = new AERequest(
					new JSONObject().put(Organization.JSONKey.customerId, request.getCompany().getDescriptor().getID())
					.put(Organization.JSONKey.doNotLoadCAO, true)
					.put(Organization.JSONKey.doNotLoadSocialInfo, true));
			AEResponse customerResponse = partyLocal.loadCustomer(customerRequest, invContext);
			dataSource.setCustomer(customerResponse.getPayload().getJSONObject(Organization.JSONKey.customer));
			
			// period
			dataSource.setPeriod(request.getPeriod());
			
			// accName
			final StringBuilder nameBuilder = AEStringUtil.getStringBuilder(null); 
			Map<String, String> accNamesMap = dataSource.getAccNamesMap();
			
			// current year
			AccJournalDAO accJournalDAO = daoFactory.getAccJournalDAO(localConnection);
			AccAccountBalancesList currentYear = accJournalDAO.loadTurnover(request.getCompany().getDescriptor(), request.getPeriod());

			Map<String, Double> currentYearMap = new HashMap<String, Double>();
			for (AccAccountBalance accAccountBalance : currentYear) {
				String accCode = accAccountBalance.getAccAccount().getDescriptor().getCode();
				String accName = accAccountBalance.getAccAccount().getDescriptor().getName();
				
				if(AEStringUtil.isEmpty(accName)) {
					if(accCode.length() < 4) {
						accName = AEStringUtil.getStringBuilder(nameBuilder).append(accCode).append(" - ").append("Total").toString();
					} else {
						accName = accCode;
					}
				} else {
					accName = AEStringUtil.getStringBuilder(nameBuilder).append(accCode).append(" - ").append(AEStringUtil.trim(accName)).toString();
				}
				
				if(accCode.startsWith("6") || accCode.startsWith("7")) {
					currentYearMap.put(accCode, accAccountBalance.getFinalBalance());
					if (!accNamesMap.containsKey(accCode)) {
						accNamesMap.put(accCode, accName);
					}
					// accNamesMap.putIfAbsent(accCode, accName);
				}
			}
			dataSource.setCurrentYear(currentYearMap);
			
			// previous year
			Date fromDatePrevYear = AEDateUtil.addYearsToDate(request.getPeriod().getStartDate(), -1);
			Date toDatePrevYear = AEDateUtil.addYearsToDate(request.getPeriod().getEndDate(), -1);
			AETimePeriod previousPeriod = new AETimePeriod(fromDatePrevYear, toDatePrevYear);
			dataSource.setPreviousPeriod(previousPeriod);
			AccAccountBalancesList previousYear = accJournalDAO.loadTurnover(request.getCompany().getDescriptor(), previousPeriod);

			Map<String, Double> previousYearMap = new HashMap<String, Double>();
			for (AccAccountBalance accAccountBalance : previousYear) {
				String accCode = accAccountBalance.getAccAccount().getDescriptor().getCode();
				String accName = accAccountBalance.getAccAccount().getDescriptor().getName();
				
				if(AEStringUtil.isEmpty(accName)) {
					if(accCode.length() < 4) {
						accName = AEStringUtil.getStringBuilder(nameBuilder).append(accCode).append(" - ").append("Total").toString();
					} else {
						accName = accCode;
					}
				} else {
					accName = AEStringUtil.getStringBuilder(nameBuilder).append(accCode).append(" - ").append(AEStringUtil.trim(accName)).toString();
				}
				
				if(accCode.startsWith("6") || accCode.startsWith("7")) {
					previousYearMap.put(accCode, accAccountBalance.getFinalBalance());
					if (!accNamesMap.containsKey(accCode)) {
						accNamesMap.put(accCode, accName);
					}
//					accNamesMap.putIfAbsent(accCode, accName);
				}
			}
			dataSource.setPreviousYear(previousYearMap);
			
			// load loss and profit accounts and calculate profit		
			// calculate accounts loss and profit
			AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);
			JSONArray accountsLoss = accountDAO.loadAccounts(request.getCompany().getDescriptor().getID(), " and (acc.CODE = '1290')");
//			if(accountsLoss != null && accountsLoss.length() == 1) {
//				String accCode = AEStringUtil.trim(accountsLoss.getJSONObject(0).optString("code"));
//				String accName = AEStringUtil.trim(accountsLoss.getJSONObject(0).optString("name"));
//				dataSource.setLossAccount(AEStringUtil.getStringBuilder(nameBuilder).append(accCode).append(" - ").append(accName).toString());
//			}
			dataSource.setLossAccount("Déficit");
			JSONArray accountsProfit = accountDAO.loadAccounts(request.getCompany().getDescriptor().getID(), " and (acc.CODE = '1200')");
//			if(accountsProfit != null && accountsProfit.length() == 1) {
//				String accCode = AEStringUtil.trim(accountsProfit.getJSONObject(0).optString("code"));
//				String accName = AEStringUtil.trim(accountsProfit.getJSONObject(0).optString("name"));
//				dataSource.setProfitAccount(AEStringUtil.getStringBuilder(nameBuilder).append(accCode).append(" - ").append(accName).toString());
//			}
			dataSource.setProfitAccount("Excédent");
			
			// current
			double sumExpenses = dataSource.sumExpensesCurrent();
			double sumIncomes = dataSource.sumIncomeCurrent();
			double profit = sumIncomes - sumExpenses;
			if(AEMath.isPositiveAmount(profit) && accountsProfit.length() > 0) {
				// profit
				dataSource.setProfitCurrent(Math.abs(profit));
			} else if(AEMath.isNegativeAmount(profit) && accountsLoss.length() > 0) {
				// loss (1290)
				dataSource.setLossCurrent(Math.abs(profit));
			}
			
			// previous
			sumExpenses = dataSource.sumExpensesPrevious();
			sumIncomes = dataSource.sumIncomePrevious();
			profit = sumIncomes - sumExpenses;
			if(AEMath.isPositiveAmount(profit) && accountsProfit.length() > 0) {
				// profit
				dataSource.setProfitPrevious(Math.abs(profit));
			} else if(AEMath.isNegativeAmount(profit) && accountsLoss.length() > 0) {
				// loss (1290)
				dataSource.setLossPrevious(Math.abs(profit));
			}
			
			// build response
			return dataSource;
		} catch (Exception e) {
			logger.error("generateDataSource failed: ", e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public BalanceDataSource generateDataSource(BalanceRequest request, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			// whether this principal is authorized for specified tenant
			long ownerId = request.getCompany().getDescriptor().getID();
			ap.ownershipValidator(ownerId);
			
			/**
			 * Authorize 
			 */
			authorize(new AuthPermission("System/Cefra/Balance", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * DAO Factory and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * 
			 */
			BalanceDataSource dataSource = new BalanceDataSource();
			
			// customer
			AERequest customerRequest = new AERequest(
					new JSONObject().put(Organization.JSONKey.customerId, request.getCompany().getDescriptor().getID())
					.put(Organization.JSONKey.doNotLoadCAO, true)
					.put(Organization.JSONKey.doNotLoadSocialInfo, true));
			AEResponse customerResponse = partyLocal.loadCustomer(customerRequest, invContext);
			dataSource.setCustomer(customerResponse.getPayload().getJSONObject(Organization.JSONKey.customer));
			
			// period
			dataSource.setPeriod(request.getPeriod());
			
			// year
			dataSource.setYear(AEDateUtil.getYear(dataSource.getPeriod().getStartDate()));
			
			// accounts from - to
			Integer fromAccCodeInt = null;
			String fromAccCode = request.getFromAccountCode();
			if(!AEStringUtil.isEmpty(fromAccCode)) {
				switch(fromAccCode.length()) {
					case 1:
						fromAccCode += "000";
						break;
					case 2:
						fromAccCode += "00";
						break;
					case 3:
						fromAccCode += "0";
						break;
					default:
						break;
				}
				fromAccCodeInt = Integer.parseInt(fromAccCode);
			}
			dataSource.setFromAccountCode(fromAccCode);
			
			Integer toAccCodeInt = null;
			String toAccCode = request.getToAccountCode();
			if(!AEStringUtil.isEmpty(toAccCode)) {
				switch(toAccCode.length()) {
					case 1:
						toAccCode += "000";
						break;
					case 2:
						toAccCode += "00";
						break;
					case 3:
						toAccCode += "0";
						break;
					default:
						break;
				}
				toAccCodeInt = Integer.parseInt(toAccCode);
			}
			dataSource.setToAccountCode(toAccCode);
			
			// balance
			AccJournalDAO accJournalDAO = daoFactory.getAccJournalDAO(localConnection);
			Map<String, AccAccountBalanceExt> balance = accJournalDAO.loadBalanceSheet(
					request.getCompany().getDescriptor(), 
					request.getPeriod(),
					fromAccCodeInt,
					toAccCodeInt);
			dataSource.setBalanceSheet(balance);
			
			// setup dataSource
			dataSource.setup();
			
			// build response
			return dataSource;
		} catch (Exception e) {
			logger.error("generateDataSource failed: ", e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public BilanDataSource generateDataSource(BilanRequest request, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			// whether this principal is authorized for specified tenant
			long ownerId = request.getCompany().getDescriptor().getID();
			ap.ownershipValidator(ownerId);

			/**
			 * Authorize 
			 */
			authorize(new AuthPermission("System/Cefra/Bilan", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));
			
			/**
			 * DAO Factory and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * 
			 */
			BilanDataSource dataSource = new BilanDataSource();
			
			// customer
			AERequest customerRequest = new AERequest(
					new JSONObject().put(Organization.JSONKey.customerId, request.getCompany().getDescriptor().getID())
					.put(Organization.JSONKey.doNotLoadCAO, true)
					.put(Organization.JSONKey.doNotLoadSocialInfo, true));
			AEResponse customerResponse = partyLocal.loadCustomer(customerRequest, invContext);
			dataSource.setCustomer(customerResponse.getPayload().getJSONObject(Organization.JSONKey.customer));
			
			// period and validation
			dataSource.setPeriod(request.getPeriod());
			
			// load bilan sheet for current year (excluding accounts 120* and 129*)
			AccJournalDAO accJournalDAO = daoFactory.getAccJournalDAO(localConnection);
			AccAccountBalancesList currentYear = accJournalDAO.loadBilanSheet(request.getCompany().getDescriptor(), request.getPeriod());
			
			// 120 and 129 accounts custom calculation to be added to the bilan sheet
			JSONObject budgetRealArguments = new JSONObject()
				.put(AEDomainObject.JSONKey.ownerId.name(), request.getCompany().getDescriptor().getID())
				.put(AEDomainObject.JSONKey.sOwnerId.name(), request.getCompany().getDescriptor().getID())
//				.put("year", AEDateUtil.getYear(request.getPeriod().getStartDate()))
				.put("startDate", AEDateUtil.formatToSystem(request.getPeriod().getStartDate()))
				.put("endDate", AEDateUtil.formatToSystem(request.getPeriod().getEndDate()));
			AEResponse budgetRealResponse = accLocal.loadBudgetReal(new AERequest(budgetRealArguments), invContext);
			JSONArray accountsIncome  = budgetRealResponse.getPayload().optJSONArray("income");
			JSONArray accountsExpense = budgetRealResponse.getPayload().optJSONArray("expense");
			double sumExpenses = BudgetRealizationDataSource.getRealAmount(accountsExpense);
			double sumIncomes = BudgetRealizationDataSource.getRealAmount(accountsIncome);
			double profit = sumIncomes - sumExpenses;
			if(AEMath.isPositiveAmount(profit)) {
				// profit: deal with account 120 
				
				// create zero balance for 120 account and add to the balance sheet
				AccAccountBalance b = new AccAccountBalance();
				b.setAccAccount(new AEDescriptorImp().withCode("120"));
				currentYear.add(b);
				
				// increase credit part with the profit
				List<String> accCodes = new ArrayList<String>();
				accCodes.add("1");
				accCodes.add("12");
				accCodes.add("120");
				AccAccountBalancesList balances = currentYear.getBalances(accCodes);
				for (AccAccountBalance rb : balances) {
					rb.setCreditTurnover(rb.getCreditTurnover() + Math.abs(profit));
					rb.calculateFinalBalance();
				}
			} else if(AEMath.isNegativeAmount(profit)) {
				// loss: deal with account 129
				
				// create zero balance for 129 account and add to the balance sheet
				AccAccountBalance b = new AccAccountBalance();
				b.setAccAccount(new AEDescriptorImp().withCode("129"));
				currentYear.add(b);
				
				// increase debit part with the profit
				List<String> accCodes = new ArrayList<String>();
				accCodes.add("1");
				accCodes.add("12");
				accCodes.add("129");
				AccAccountBalancesList balances = currentYear.getBalances(accCodes);
				for (AccAccountBalance rb : balances) {
					rb.setDebitTurnover(rb.getDebitTurnover() + Math.abs(profit));
					rb.calculateFinalBalance();
				}
			}
			
			// transform to map
			Map<String, AccAccountBalance> currentYearMap = new HashMap<String, AccAccountBalance>();
			for (AccAccountBalance accAccountBalance : currentYear) {
				String accCode = accAccountBalance.getAccAccount().getDescriptor().getCode();
				currentYearMap.put(accCode, accAccountBalance);
			}
			dataSource.setCurrentYear(currentYearMap);
			
			// return dataSource
			return dataSource;
		} catch (Exception e) {
			logger.error("generateDataSource failed: ", e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public JournauxDataSource generateDataSource(JournauxRequest request, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			// whether this principal is authorized for specified tenant
			long ownerId = request.getCompany().getDescriptor().getID();
			ap.ownershipValidator(ownerId);

			/**
			 * Authorize 
			 */
			authorize(new AuthPermission("System/Cefra/Journaux", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));
			
			/**
			 * DAO Factory and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * 
			 */
			JournauxDataSource dataSource = new JournauxDataSource();
			
			// customer
			AERequest customerRequest = new AERequest(
					new JSONObject().put(Organization.JSONKey.customerId, request.getCompany().getDescriptor().getID())
					.put(Organization.JSONKey.doNotLoadCAO, true)
					.put(Organization.JSONKey.doNotLoadSocialInfo, true));
			AEResponse customerResponse = partyLocal.loadCustomer(customerRequest, invContext);
			dataSource.setCustomer(customerResponse.getPayload().getJSONObject(Organization.JSONKey.customer));
			
			// period
			dataSource.setPeriod(request.getPeriod());
			
			// journaux
			dataSource.setAccJournal(request.getAccJournal());
			
			// journal items
			AccJournalDAO accJournalDAO = daoFactory.getAccJournalDAO(localConnection);
			AccJournalResultsList accJournalResultsList = accJournalDAO.load(
					request.getCompany().getDescriptor(), 
					request.getPeriod(), 
					request.getAccJournal());
			dataSource.setAccJournalResultsList(accJournalResultsList);
			dataSource.groupAccJournalResultsInMap();
			
			// build response
			return dataSource;
		} catch (Exception e) {
			logger.error("generateDataSource failed: ", e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public GrandLivreDataSource generateDataSource(GrandLivreRequest request, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			// whether this principal is authorized for specified tenant
			long ownerId = request.getCompany().getDescriptor().getID();
			ap.ownershipValidator(ownerId);

			/**
			 * Authorize 
			 */
			authorize(new AuthPermission("System/Cefra/Journaux", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));
			
			/**
			 * DAO Factory and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * 
			 */
			GrandLivreDataSource dataSource = new GrandLivreDataSource();
			
			// customer
			AERequest customerRequest = new AERequest(
					new JSONObject().put(Organization.JSONKey.customerId, request.getCompany().getDescriptor().getID())
					.put(Organization.JSONKey.doNotLoadCAO, true)
					.put(Organization.JSONKey.doNotLoadSocialInfo, true));
			AEResponse customerResponse = partyLocal.loadCustomer(customerRequest, invContext);
			dataSource.setCustomer(customerResponse.getPayload().getJSONObject(Organization.JSONKey.customer));
			
			// period
			dataSource.setYear(AEDateUtil.getYear(request.getPeriod().getStartDate()));
			dataSource.setPeriod(request.getPeriod());
			
			// account range			
			Long fromAccCodeLong = null;
			String fromAccCode = request.getFromAccountCode();
			if(!AEStringUtil.isEmpty(fromAccCode)) {
				switch(fromAccCode.length()) {
					case 1:
						fromAccCode += "000";
						break;
					case 2:
						fromAccCode += "00";
						break;
					case 3:
						fromAccCode += "0";
						break;
					default:
						break;
				}
				fromAccCodeLong = Long.parseLong(fromAccCode);
			}
			dataSource.setFromAccountCode(fromAccCode);
			
			Long toAccCodeLong = null;
			String toAccCode = request.getToAccountCode();
			if(!AEStringUtil.isEmpty(toAccCode)) {
				switch(toAccCode.length()) {
					case 1:
						toAccCode += "000";
						break;
					case 2:
						toAccCode += "00";
						break;
					case 3:
						toAccCode += "0";
						break;
					default:
						break;
				}
				toAccCodeLong = Long.parseLong(toAccCode);
			}
			dataSource.setToAccountCode(toAccCode);
			
			// accounts
			AccountDAO accDAO = daoFactory.getAccountDAO(localConnection);
			JSONArray accountsJsonArray = accDAO.loadAccountsByOwner(request.getCompany().getDescriptor().getID());
			List<String> accounts = new ArrayList<String>();
			for (int i = 0; i < accountsJsonArray.length(); i++) {
				JSONObject accJson = accountsJsonArray.getJSONObject(i);
				boolean validAccount = true;
				if(fromAccCodeLong != null || toAccCodeLong != null) { 
					String code = accJson.getString(AEDomainObject.JSONKey.code.name());
					long codeLong = Long.parseLong(code);
					
					// fromAccCodeLong rule
					if(fromAccCodeLong != null && codeLong < fromAccCodeLong) {
						validAccount = false;
					}
					
					// toAccCodeLong rule
					if(validAccount && toAccCodeLong != null && codeLong > toAccCodeLong) {
						validAccount = false;
					}
				}
				if(validAccount) {
					accounts.add(accJson.getString(AEDomainObject.JSONKey.code.name()) + " - " + accJson.getString(AEDomainObject.JSONKey.name.name()));
				}
			}
			Collections.sort(accounts);
			dataSource.setAccounts(accounts);
			
			// grand livre
			AccJournalDAO accJournalDAO = daoFactory.getAccJournalDAO(localConnection);
			accJournalDAO.loadGrandLivre(
					dataSource,
					request.getCompany().getDescriptor(), 
					request.getPeriod(), 
					fromAccCodeLong,
					toAccCodeLong);
			
			// calculate data source
			dataSource.calculate();
			
			// build response
			return dataSource;
		} catch (Exception e) {
			logger.error("generateDataSource failed: ", e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public BudgetRealizationDataSource generateDataSource(BudgetRealizationRequest request, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			// whether this principal is authorized for specified tenant
			long ownerId = request.getCompany().getDescriptor().getID();
			ap.ownershipValidator(ownerId);

			/**
			 * Authorize 
			 */
			authorize(new AuthPermission("System/Configuration/Customer/Budget", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));
			
			
			/**
			 * DAO Factory and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * 
			 */
			BudgetRealizationDataSource dataSource = new BudgetRealizationDataSource();
			
			// customer
			AERequest customerRequest = new AERequest(
					new JSONObject().put(Organization.JSONKey.customerId, request.getCompany().getDescriptor().getID())
					.put(Organization.JSONKey.doNotLoadCAO, true)
					.put(Organization.JSONKey.doNotLoadSocialInfo, true));
			AEResponse customerResponse = partyLocal.loadCustomer(customerRequest, invContext);
			dataSource.setCustomer(customerResponse.getPayload().getJSONObject(Organization.JSONKey.customer));
			
			// year
			dataSource.setYear(request.getYear());
			
			// account class
			String accountClass = request.getAccountClass();
			
			/**
			 * Generate and Build Data Source
			 */
			AEDescriptor tenantDescr = Organization.lazyDescriptor(request.getCompany().getDescriptor().getID());
			
			/**
			 * Get budget and estimated and merge them
			 */
			
			// load budget estimated (for the next year)
			AccountDAO accountDAO = daoFactory.getAccountDAO(localConnection);
			JSONArray budgetEstimated = accountDAO.loadBudget(request.getCompany().getDescriptor().getID(), request.getYear() + 1);
			
			// load budget (current year) and real execution (income and expense)
			JSONObject budgetRealArguments = new JSONObject()
				.put(AEDomainObject.JSONKey.ownerId.name(), request.getCompany().getDescriptor().getID())
				.put(AEDomainObject.JSONKey.sOwnerId.name(), request.getCompany().getDescriptor().getID())
				.put("year", request.getYear());
			AEResponse budgetRealResponse = accLocal.loadBudgetReal(new AERequest(budgetRealArguments), invContext);
			JSONArray accountsIncome  = budgetRealResponse.getPayload().optJSONArray("income");
			JSONArray accountsExpense = budgetRealResponse.getPayload().optJSONArray("expense");
			
			// load loss and profit accounts and calculate profit		
			// calculate accounts loss and profit
			JSONArray accountsLoss = accountDAO.loadAccounts(tenantDescr.getID(), " and (acc.CODE = '1290')");
			JSONArray accountsProfit = accountDAO.loadAccounts(tenantDescr.getID(), " and (acc.CODE = '1200')");
			double sumExpenses = BudgetRealizationDataSource.getRealAmount(accountsExpense);
			double sumBudgetExpenses = BudgetRealizationDataSource.getBudgetAmount(accountsExpense);
			double sumIncomes = BudgetRealizationDataSource.getRealAmount(accountsIncome);
			double sumBudgetIncomes = BudgetRealizationDataSource.getBudgetAmount(accountsIncome);
			double profit = sumIncomes - sumExpenses;
			if(AEMath.isPositiveAmount(profit) && accountsProfit.length() > 0) {
				// profit
				JSONObject b = accountsProfit.getJSONObject(0);
				b.put("amount", Math.abs(profit));
			} else if(AEMath.isNegativeAmount(profit) && accountsLoss.length() > 0) {
				// loss (1290)
				JSONObject b = accountsLoss.getJSONObject(0);
				b.put("amount", Math.abs(profit));
			}
			
			// prepare and merge
			String idKey = AEDomainObject.JSONKey.id.name();
			
			// map accountsIncome with budgetEstimated
			if(accountsIncome != null && budgetEstimated != null) {
				for (int i = 0; i < accountsIncome.length(); i++) {
					JSONObject accountIncome = accountsIncome.getJSONObject(i);
					for (int j = 0; j < budgetEstimated.length(); j++) {
						JSONObject accountBudgetEstimated = budgetEstimated.getJSONObject(j);
						if(accountIncome.getLong(idKey) == accountBudgetEstimated.getLong(idKey)) {
							accountIncome.put("estimatedAmount", accountBudgetEstimated.getDouble("amount"));
						}
					}
				}
			}
			double sumEstimatedIncomes = BudgetRealizationDataSource.getEstimatedAmount(accountsIncome);
			
			// map accountsExpense with budgetEstimated
			if(accountsExpense != null && budgetEstimated != null) {
				for (int i = 0; i < accountsExpense.length(); i++) {
					JSONObject accountExpense = accountsExpense.getJSONObject(i);
					for (int j = 0; j < budgetEstimated.length(); j++) {
						JSONObject accountBudgetEstimated = budgetEstimated.getJSONObject(j);
						if(accountExpense.getLong(idKey) == accountBudgetEstimated.getLong(idKey)) {
							accountExpense.put("estimatedAmount", accountBudgetEstimated.getDouble("amount"));
						}
					}
				}
			}
			double sumEstimatedExpenses = BudgetRealizationDataSource.getEstimatedAmount(accountsExpense);
			
			// map accountsLoss
			for (int i = 0; i < accountsLoss.length(); i++) {
				JSONObject accountLoss = accountsLoss.getJSONObject(i);
				double amnt = accountLoss.optDouble("amount", 0.0);
				accountLoss.put("realAmount", amnt);
				if(AEMath.isPositiveAmount(sumBudgetExpenses - sumBudgetIncomes)) {
					accountLoss.put("budgetAmount", sumBudgetExpenses - sumBudgetIncomes);
				}
				if(AEMath.isPositiveAmount(sumEstimatedExpenses - sumEstimatedIncomes)) {
					accountLoss.put("estimatedAmount", sumEstimatedExpenses - sumEstimatedIncomes);
				}
			}
			JSONUtil.addAll(accountsIncome, accountsLoss);
			
			// map accountsProfit
			for (int i = 0; i < accountsProfit.length(); i++) {
				JSONObject accountProfit = accountsProfit.getJSONObject(i);
				double amnt = accountProfit.optDouble("amount", 0.0);
				accountProfit.put("realAmount", amnt);
				if(AEMath.isPositiveAmount(sumBudgetIncomes - sumBudgetExpenses)) {
					accountProfit.put("budgetAmount", sumBudgetIncomes - sumBudgetExpenses);
				}
				if(AEMath.isPositiveAmount(sumEstimatedIncomes - sumEstimatedExpenses)) {
					accountProfit.put("estimatedAmount", sumEstimatedIncomes - sumEstimatedExpenses);
				}
			}
			JSONUtil.addAll(accountsExpense, accountsProfit);
			
			if("6".equals(accountClass)) {
				dataSource.setAccounts(accountsExpense);
			} else if("7".equals(accountClass)) {
				dataSource.setAccounts(accountsIncome);
			} else {
				dataSource.setAccounts(new JSONArray());
			}
			
			// build response
			return dataSource;
		} catch (Exception e) {
			logger.error("generateDataSource failed: ", e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public CouncilDataSource generateDataSource(CouncilRequest request, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			// whether this principal is authorized for specified tenant
			long ownerId = request.getCompany().getDescriptor().getID();
			ap.ownershipValidator(ownerId);

			// authorize 
			authorize(new AuthPermission("System/Configuration/Customer/Councill", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));
			
			/**
			 * DAO Factory and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * Data Source
			 */
			CouncilDataSource dataSource = new CouncilDataSource();
			
			// tenant
			AEDescriptor tenantDescr = Organization.lazyDescriptor(request.getCompany().getDescriptor().getID());
			
			// customer
			AERequest customerRequest = new AERequest(
					new JSONObject().put(Organization.JSONKey.customerId, request.getCompany().getDescriptor().getID())
					.put(Organization.JSONKey.doNotLoadCAO, true)
					.put(Organization.JSONKey.doNotLoadSocialInfo, true));
			AEResponse customerResponse = partyLocal.loadCustomer(customerRequest, invContext);
			dataSource.setCustomer(customerResponse.getPayload().getJSONObject(Organization.JSONKey.customer));
			
			// year
			dataSource.setYear(request.getYear());
			
			// date
			dataSource.setDate(request.getDate());
			
			// council
			Date endDate = AEDateUtil.getLastDate(11, dataSource.getYear());
			AERequest councilRequest = new AERequest( 
					new JSONObject()
						.put(AEDomainObject.JSONKey.ownerId.name(), tenantDescr.getID())
						.put("date", AEDateUtil.convertToString(endDate, AEDateUtil.SYSTEM_DATE_FORMAT)));
			Council council = councilLocal.loadByDate(councilRequest, invContext);
			dataSource.setCouncil(council);
			
			// build response
			return dataSource;
		} catch (Exception e) {
			logger.error("generateDataSource failed: ", e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public FinancesDataSource generateDataSource(FinancesRequest request, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			// whether this principal is authorized for specified tenant
			long ownerId = request.getCompany().getDescriptor().getID();
			ap.ownershipValidator(ownerId);
			
			// authorize 
			authorize(new AuthPermission("System/Configuration/Customer/EngTitres", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * DAO Factory and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * Data Source
			 */
			FinancesDataSource dataSource = new FinancesDataSource();
			
			// tenant
			AEDescriptor tenantDescr = Organization.lazyDescriptor(request.getCompany().getDescriptor().getID());
			
			// customer
			AERequest customerRequest = new AERequest(
					new JSONObject().put(Organization.JSONKey.customerId, request.getCompany().getDescriptor().getID())
					.put(Organization.JSONKey.doNotLoadCAO, true)
					.put(Organization.JSONKey.doNotLoadSocialInfo, true));
			AEResponse customerResponse = partyLocal.loadCustomer(customerRequest, invContext);
			dataSource.setCustomer(customerResponse.getPayload().getJSONObject(Organization.JSONKey.customer));
			
			// year
			dataSource.setYear(request.getYear());
			
			// date
			dataSource.setDate(request.getDate());
			
			// period
			AETimePeriod period = new AETimePeriod(AEDateUtil.getClearDate(AEDateUtil.beginOfTheYear(request.getDate())), request.getDate());
			
			// Engagements & Titres
			AERequest engTitresRequest = new AERequest( 
					new JSONObject()
						.put(AEDomainObject.JSONKey.ownerId.name(), tenantDescr.getID())
						.put(AEDomainObject.JSONKey.sOwnerId.name(), tenantDescr.getID())
						.put("year", request.getYear()));
			AEResponse engTitresResponse = councilLocal.loadEngTitres(engTitresRequest, invContext);
			dataSource.setEngagements(engTitresResponse.getPayload().getJSONArray("engagements"));
			dataSource.setTitres(engTitresResponse.getPayload().getJSONArray("titres"));
			
			// Banks and balances
			AERequest bankRequest = new AERequest(
				new JSONObject()
					.put(AEDomainObject.JSONKey.ownerId.name(), tenantDescr.getID())
					.put(AEDomainObject.JSONKey.sOwnerId.name(), tenantDescr.getID()))
				.withAuthPrincipal(ap);
			AEResponse bankAccountsAeResponse = cashLocal.loadBankAccounts(bankRequest);
			JSONArray bankAccountsAll = bankAccountsAeResponse.getPayload().optJSONArray(FinancialTransaction.JSONKey.bankAccounts.name());
			if(bankAccountsAll == null) {
				bankAccountsAll = new JSONArray();
			}
			BankAccountDAO bankAccountDAO = daoFactory.getBankAccountDAO(localConnection);
			for (int j = 0; j < bankAccountsAll.length(); j++) {
				JSONObject bankAccount = bankAccountsAll.getJSONObject(j);
				
				// acc account descriptor
				AEDescriptor accAccountDescr = AccAccount.lazyDescriptor(bankAccount.optLong("accId", AEPersistentUtil.NEW_ID));

				// calc accountancy balance
				AccAccountBalance accAccountBalance = new AccAccountBalance();
				accAccountBalance.setAccAccount(accAccountDescr);
				accAccountBalance.setPeriod(period);
				accLocal.loadAccFinalBalance(accAccountBalance, invContext, localConnection);
				
				// set balance and date
				bankAccount.put("accBalance", accAccountBalance.getFinalBalance());
				bankAccount.put("accBalanceDate", AEDateUtil.formatToFrench(period.getEndDate()));
				
				// last bank statement extraction
				BankAccountBalance lastBalance = bankAccountDAO.loadLastBalance(
						BankAccount.lazyDescriptor(bankAccount.getLong(AEDomainObject.JSONKey.id.name())), 
						dataSource.getYear());
				bankAccount.put("lastStatementBalance", lastBalance != null ? lastBalance.getBankFinalBalance() : 0.0);
				bankAccount.put("lastStatementDate", lastBalance != null ? AEDateUtil.formatToFrench(lastBalance.getBankFinalBalanceDate()) : AEStringUtil.EMPTY_STRING);
			}
			dataSource.setBanks(bankAccountsAll);

			
			// CashDesk
			JSONObject cashDesk = new JSONObject();
			AccountDAO accDAO = daoFactory.getAccountDAO(localConnection);
			JSONArray accsJson = accDAO.loadAccounts(tenantDescr.getID(), " and (acc.CODE = '5300')");
			if(accsJson == null) {
				accsJson = new JSONArray();
			}
			if(accsJson != null && accsJson.length() == 1) {
				cashDesk = accsJson.getJSONObject(0);

				// acc account descriptor
				AEDescriptor accAccountDescr = AccAccount.lazyDescriptor(cashDesk.optLong(AEDomainObject.JSONKey.id.name(), AEPersistentUtil.NEW_ID));
				
				// accBalance
				AccAccountBalance accAccountBalance = new AccAccountBalance();
				accAccountBalance.setAccAccount(accAccountDescr);
				accAccountBalance.setPeriod(period);
				accLocal.loadAccFinalBalance(accAccountBalance, invContext, localConnection);

				// set balance
				cashDesk.put("accBalance", accAccountBalance.getFinalBalance());
				cashDesk.put("accBalanceDate", AEDateUtil.formatToFrench(period.getEndDate()));
			}
			dataSource.setCashDesk(cashDesk);
			
			// build response
			return dataSource;
		} catch (Exception e) {
			logger.error("generateDataSource failed: ", e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public DonorsDataSource generateDataSource(DonorsRequest request, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			// whether this principal is authorized for specified tenant
			long ownerId = request.getCompany().getDescriptor().getID();
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);
			ap.ownershipValidator(ownerId);
			
			/**
			 * Authorize 
			 */
			authorize(new AuthPermission("System/Cefra/DonorsReport", AuthPermission.READ), invContext, ownerDescr);

			/**
			 * DAO Factory and connection
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * 
			 */
			DonorsDataSource dataSource = new DonorsDataSource();
			
			// year
			int year = (int) request.getYear();
			dataSource.setYear(year);
			
			// customer
			AERequest customerRequest = new AERequest(
					new JSONObject().put(Organization.JSONKey.customerId, ownerId)
					.put(Organization.JSONKey.doNotLoadCAO, true)
					.put(Organization.JSONKey.doNotLoadSocialInfo, true));
			AEResponse customerResponse = partyLocal.loadCustomer(customerRequest, invContext);
			dataSource.setCustomer(customerResponse.getPayload().getJSONObject(Organization.JSONKey.customer));
			
			// donations
			AERequest donationsRequest = new AERequest(new JSONObject()
					.put(AEDomainObject.JSONKey.ownerId.name(), request.getCompany().getDescriptor().getID())
					.put(AEDomainObject.JSONKey.sOwnerId.name(), request.getCompany().getDescriptor().getID())
					.put("year", year));
			AEResponse donationsResponse = accLocal.loadDonations(donationsRequest, invContext);
			dataSource.setDonations(donationsResponse.getPayload().getJSONArray("donations"));
			dataSource.setAccountancy(donationsResponse.getPayload().getJSONArray("accountancy"));
			
			// update donations with info from generated receipts
			AEStatementDAO statementDAO = (AEStatementDAO) daoFactory.getDocumentDAO(
					AEDocumentType.valueOf(AEDocumentType.System.Cerfa_11580_03), 
					localConnection); 
			Map<Long, AEDocumentsList> docs = statementDAO.loadReceiptsGroupedByPersonId(ownerDescr, year);
			JSONArray donations = donationsResponse.getPayload().getJSONArray("donations");
			for (int i = 0; i < donations.length(); i++) {
				JSONObject don = donations.getJSONObject(i);
				try {
					long personId = don.
							getJSONObject(Employee.JSONKey.employee).
							getJSONObject(Person.JSONKey.person).
							getLong(AEDomainObject.JSONKey.id.name());
					AEDocumentsList personDocs = docs.get(personId);
					if(personDocs != null) {
						if(personDocs.size() == 1) {
							AEDocument doc = personDocs.get(0);
							don.put("docNo", doc.getNumberString());
							if(doc.getDate() != null) {
								don.put("docDate", AEDateUtil.formatToFrench(doc.getDate()));
							} else {
								don.put("docDate", "ANNEE");
							}
							don.put("docNature", doc.getNote());
						} else {
							don.put("docNo", AEStringUtil.EMPTY_STRING);
							don.put("docDate", "ANNEE");
							don.put("docNature", AEStringUtil.EMPTY_STRING);
						} 
					}
				} catch (Exception e) {
				}
			}
			
			// build response
			return dataSource;
		} catch (Exception e) {
			logger.error(e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
}
