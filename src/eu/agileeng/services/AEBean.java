package eu.agileeng.services;

import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.jboss.logging.Logger;

import eu.agileeng.accbureau.AEApp;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDescriptorsSet;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.acc.AccPeriod;
import eu.agileeng.domain.acc.AccPeriodItems;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.contact.OrganizationDescriptor;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.acc.AccPeriodDAO;
import eu.agileeng.persistent.dao.oracle.OrganizationDAO;
import eu.agileeng.security.AuthAccessController;
import eu.agileeng.security.AuthException;
import eu.agileeng.security.AuthPermission;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.util.AEDateUtil;

public class AEBean implements AEService {

	private static Logger logger = Logger.getLogger(AEBean.class);
	
	private static final long serialVersionUID = -2470261826620604259L;
	
	/**
	 * Fetches the first open period for specified <code>ownerId</code>, <code>moduleId</code>.
	 * Creates a new one in case of first time access.
	 * 
	 * @param ownerId
	 * @param moduleId
	 * @param aeConnection
	 * @return
	 * @throws AEException
	 */
	protected AccPeriod getFirstOpenPeriod(long ownerId, long moduleId, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			AccPeriod accPeriod = null;

			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			// detect the first open period
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
			if(moduleId == AEApp.MANDAT_MODULE_EXT_ID) {
				moduleId = AEApp.MANDAT_MODULE_ID;
			}
			accPeriod = accPeriodDAO.getFirstOpenPeriod(ownerId, moduleId);
			if(accPeriod == null) {
				// there is no open period, so we need to create one
				Date startDate = null;
				accPeriod = accPeriodDAO.getLastClosedPeriod(ownerId, moduleId);
				if(accPeriod == null) {	
					// there is no closed period, so start from absolute start date
					startDate = getAbsoluteStartDate(ownerId, localConnection);
				} else {
					// there is closed period, start the period from next day
					startDate = AEDateUtil.addDaysToDate(accPeriod.getEndDate(), 1);
				}

				accPeriod = new AccPeriod();
				accPeriod.setCompany(Organization.lazyDescriptor(ownerId));
				accPeriod.setModuleId(moduleId);
				accPeriod.setStartDate(startDate);
				accPeriod.setEndDate(getPeriodEndDate(accPeriod.getStartDate()));
				accPeriod.setClosed(false);
				accPeriodDAO.insert(accPeriod);
			}

			assert(accPeriod.isPersistent());
			return accPeriod;
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	/**
	 * Fetches the first open period for specified <code>ownerId</code>, <code>moduleId</code>.
	 * Returns <code>null</code> if there is no open period.
	 * 
	 * @param ownerId
	 * @param moduleId
	 * @param aeConnection
	 * @return
	 * @throws AEException
	 */
	protected AccPeriod getFirstOpenPeriodExact(long ownerId, long moduleId, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			// fetch the first open period
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
			AccPeriod accPeriod = accPeriodDAO.getFirstOpenPeriod(ownerId, moduleId);

			return accPeriod;
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	protected Date getAbsoluteStartDate(long ownerId, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(aeConnection);
			Date startDate = orgDAO.getStartDate(ownerId);
			if(startDate == null) {
				// there is no start date
				// so start from the first date of current month
				startDate = AEDateUtil.dayOfMonth(AEDateUtil.getClearDate(new Date()), -1, 1);
			}
			return startDate;
		} catch (Throwable t) {
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	/**
	 * The period is one month.
	 * 
	 * @param startDate
	 * @return
	 * @throws AEException
	 */
	protected Date getPeriodEndDate(Date startDate) throws AEException {
		Date endDate = AEDateUtil.getLastDate(
				AEDateUtil.getMonthInYear(startDate) - 1, AEDateUtil.getYear(startDate));
		return endDate;
	}
	
	protected AccPeriod getLastClosedPeriod(long ownerId, long moduleId, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			AccPeriod accPeriod = null;

			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			// detect the last closed period
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
			accPeriod = accPeriodDAO.getLastClosedPeriod(ownerId, moduleId);

			return accPeriod;
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	protected AccPeriod getAccPeriod(long ownerId, long moduleId, Date date, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			AccPeriod accPeriod = null;

			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(aeConnection);

			// detect the period
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);
			accPeriod = accPeriodDAO.loadAccPeriod(date, moduleId, ownerId);

			return accPeriod;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	/**
	 * Fetches accounting (fiscal) period for specified <code>ownerId</code>, <code>moduleId</code> and <code>calendarYear</code> 
	 * One calendar year is mapping to a fiscal period using the last date of the calendarYear (31/12/calendarYear).
	 * 
	 * @param ownerId
	 * @param moduleId
	 * @param year
	 * @param aeConnection
	 * @return The <code>AccPeriod</code> instance found in Database or <code>null</code> if the period doesn't exist.
	 * @throws AEException
	 */
	protected AccPeriod getAccPeriod(long ownerId, long moduleId, int calendarYear, AEConnection aeConnection) throws AEException {
		return getAccPeriod(ownerId, moduleId, AEDateUtil.getLastDate(11, calendarYear), aeConnection);
	}
	
	protected void commonValidation(AERequest aeRequest, AEInvocationContext invContext) throws AEException, JSONException {
		// validate invocation context
		AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
		invContextValidator.validate(invContext);
		
		// validate access to specified tenant
		long sOwnerId = aeRequest.getArguments().getLong("sOwnerId");
		invContext.getAuthPrincipal().ownershipValidator(sOwnerId);
	}
	
	protected final void checkPermission(AuthPermission perm, AEInvocationContext context) throws AuthException {
		AuthAccessController.checkPermission(perm, context);
	}
	
	protected final void authorize(AuthPermission perm, AEInvocationContext context) throws AuthException {
		checkPermission(perm, context);
	}
	
	protected final void checkPermission(AuthPermission perm, AEInvocationContext context, AEDescriptor ownerDescr) throws AEException {
		// check permissions within specified context
		AuthAccessController.checkPermission(perm, context);

		// check permissions within specified context and owner description
		if(ownerDescr != null) {
			AuthPrincipal ap = context.getAuthPrincipal();

			// check ownership
			ap.ownershipValidator(ownerDescr.getID());

			// check specified owner description
			AEDescriptorsSet companiesSet = ap.getCompaniesSet();
			for (AEDescriptor compDescr : companiesSet) {
				if(compDescr.equals(ownerDescr) && compDescr instanceof OrganizationDescriptor) {
					// CREATE, UPDATE, DELETE operations are available only for active owner
					if((perm.getMask() & AuthPermission.CREATE) == AuthPermission.CREATE
							|| (perm.getMask() & AuthPermission.UPDATE) == AuthPermission.UPDATE
							|| (perm.getMask() & AuthPermission.DELETE) == AuthPermission.DELETE) {

						OrganizationDescriptor orgDescr = (OrganizationDescriptor) compDescr;
						if(!orgDescr.isActive()) {
							throw AEError.System.TENANT_NOT_ACTIVE.toException();
						}
					}
				}
			}
		}
	}
	
	protected final void authorize(AuthPermission perm, AEInvocationContext context, AEDescriptor ownerDescr) throws AEException {
		checkPermission(perm, context, ownerDescr);
	}
	
	/**
	 * Creates, validates and inserts AccPeriod for module AEApp.ACCOUNTING_MODULE_ID
	 * <p>Must be in transaction !!!
	 * <p>Rules:
	 * 	 <br>  - Don't overlap existing periods;
	 *   <br>  - Must be after or equal Initial balance's period;
	 *   <br>  - The next period must be opened;
	 * 
	 * @param ownerDescr
	 * @param date
	 * @param aeConnection
	 * @return
	 * @throws AEException
	 */
	protected AccPeriod createAccountingPeriod(AEDescriptor ownerDescr, Date date, AEConnection aeConnection) throws AEException {
		AEConnection localConnection = null;
		try {
			localConnection = DAOFactory.getInstance().getConnection(aeConnection);
			localConnection.beginTransaction();
			
			AccPeriod accPeriod = new AccPeriod();
			accPeriod.setCompany(Organization.lazyDescriptor(ownerDescr.getID()));
			accPeriod.setModuleId(AEApp.ACCOUNTING_MODULE_ID);
			accPeriod.setStartDate(AEDateUtil.getFirstDate(0, AEDateUtil.getYear(date)));
			accPeriod.setEndDate(AEDateUtil.getLastDate(11, AEDateUtil.getYear(date)));
			accPeriod.setClosed(false);

			// get DAO
			DAOFactory daoFactory = DAOFactory.getInstance();
			AccPeriodDAO accPeriodDAO = daoFactory.getAccPeriodDAO(localConnection);

			// IMPORTANT: first insert and then validate DB data
			accPeriodDAO.insert(accPeriod);

//			// validation: max two open periods
//			int openPeriodsCount = accPeriodDAO.getOpenPeriodsCount(ownerDescr, AEApp.ACCOUNTING_MODULE_ID);
//			if(openPeriodsCount > 2) {
//				throw AEError.System.INCORRECT_ACC_PERIOD.toException();
//			}
			
			// validation: should not be after the initial balance's period
			accPeriodDAO.checkBeforeInitialSaldo(accPeriod);
			
			// validation: must have not overlap
			accPeriodDAO.checkForOverlap(accPeriod);
			
			// validation: must be consecutive
			AccPeriodItems accPeriodItems = accPeriodDAO.loadAll(ownerDescr, AEApp.ACCOUNTING_MODULE_ID);
			AccPeriod nextAccPeriod = null;
			for (AccPeriod currentAccPeriod : accPeriodItems) {
				if(nextAccPeriod != null) {
					// current.end < next
					Date x = AEDateUtil.addDaysToDate(currentAccPeriod.getEndDate(), 1);
					if(x.before(nextAccPeriod.getStartDate()) || x.after(nextAccPeriod.getStartDate())) {
						throw AEError.System.INCORRECT_ACC_PERIOD.toException();
					}

				}
				nextAccPeriod = currentAccPeriod;
			}

			// next period must be opened
			boolean isNextOpened = accPeriodDAO.isNextOpened(ownerDescr, AEApp.ACCOUNTING_MODULE_ID, accPeriod.getEndDate());
			if(!isNextOpened) {
				throw AEError.System.INCORRECT_ACC_PERIOD.toException();
			}

			localConnection.commit();
			
			return accPeriod;
		}  catch (Exception e) {
			logger.error("createAccountingPeriod failed ", e);
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	protected void isInPermittedAccountingPeriods(AEDescriptor ownerDescr, Date date, AEConnection aeConnection) throws AEException {
		AccPeriod accPeriod = getAccPeriod(
				ownerDescr.getID(), 
				AEApp.ACCOUNTING_MODULE_ID, 
				AEDateUtil.getClearDate(date), 
				aeConnection);
		
		// process null period
		if(accPeriod == null) {
			accPeriod = createAccountingPeriod(ownerDescr, date, aeConnection);
		}
		
		// validate in open period
		if(accPeriod.isClosed()) {
			throw AEError.System.CANNOT_INSERT_UPDATE_CLOSED_PERIOD.toException();
		}

		// validate in first two open periods
		boolean inPermittedPeriod = false;
		AccPeriodDAO accPeriodDAO = DAOFactory.getInstance().getAccPeriodDAO(aeConnection);
		AccPeriodItems firstTwoOpenPeriods = accPeriodDAO.getFirstTwoOpenPeriods(ownerDescr.getID(), AEApp.ACCOUNTING_MODULE_ID);
		for (AccPeriod accOpenPeriod : firstTwoOpenPeriods) {
			if(!(date.before(accOpenPeriod.getStartDate()) || date.after(accOpenPeriod.getEndDate()))) {
				inPermittedPeriod = true;
				break;
			}
		}
		if(!inPermittedPeriod) {
			throw AEError.System.INCORRECT_ACC_PERIOD.toException();
		}
	}
}
