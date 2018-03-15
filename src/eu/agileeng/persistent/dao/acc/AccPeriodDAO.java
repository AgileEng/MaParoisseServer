package eu.agileeng.persistent.dao.acc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import eu.agileeng.accbureau.AEApp;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.acc.AccPeriod;
import eu.agileeng.domain.acc.AccPeriodItems;
import eu.agileeng.domain.cash.CashPeriod;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.util.AEDateUtil;

public class AccPeriodDAO extends AbstractDAO {

	private static String insertSQL = 
		"insert into AccPeriod "
		+ " (OWNER_ID, MODULE_ID, START_DATE, END_DATE, CLOSED, EXPORTED) "
		+ " values (?, ?, ?, ?, ?, ?)";

	private static String updateSQLOpenClosePeriod = 
		"update AccPeriod set CLOSED = ? where id = ?";
	
	private static String updateSQLExported = 
		"update AccPeriod set EXPORTED = ? where id = ?";
	
	private static String selectSQLLastOpenClosePeriod = 
		"SELECT TOP 1 * FROM AccPeriod "
		+ " where OWNER_ID = ? and MODULE_ID = ? and CLOSED = ? "
		+ " order by END_DATE desc";
	
	private static String selectSQLLastPeriod = 
			"SELECT TOP 1 * FROM AccPeriod "
			+ " where OWNER_ID = ? and MODULE_ID = ? "
			+ " order by END_DATE desc";
	
	private static String selectSQLByDate = 
			"SELECT * FROM AccPeriod "
			+ " where OWNER_ID = ? and MODULE_ID = ? and START_DATE <= ? and END_DATE >= ? ";
	
	private static String selectSQLById = 
			"SELECT * FROM AccPeriod "
			+ " where id = ?";
	
	private static String selectSQLFirstOpenClosePeriod = 
		"SELECT TOP 1 * FROM AccPeriod "
		+ " where OWNER_ID = ? and MODULE_ID = ? and CLOSED = ? "
		+ " order by START_DATE asc";
	
	private static String selectSQLByFilter = 
		"select accPeriod.*, " +
		"att.FILE_LENGTH, att.REMOTE_ROOT, att.REMOTE_PATH, att.NAME as att_name, " +
		"appModule.NAME as app_module_name " +
		"from AccPeriod accPeriod " +
		"inner join AppModule appModule on accPeriod.MODULE_ID = appModule.ID " +
		"left join FileAttachment att on att.ATTACHED_TO_CLASS_ID = 20100 and att.ATTACHED_TO_ID = accPeriod.ID " +
		"where OWNER_ID = ? " +
		"order by accPeriod.end_date desc, accPeriod.module_id desc";
	
	private static String selectSQLCashPeriod = 
		"select accPeriod.*, " +
		"att.id as att_id, att.FILE_LENGTH, att.REMOTE_ROOT, att.REMOTE_PATH, att.NAME as att_name " +
		"from AccPeriod accPeriod " +
		"left join FileAttachment att on att.ATTACHED_TO_CLASS_ID = 20061 and att.ATTACHED_TO_ID = accPeriod.ID " +
		"where accPeriod.id = ?";
	
	private static String selectSQLPeriod = 
		"select accPeriod.*, " +
		"att.id as att_id, att.FILE_LENGTH, att.REMOTE_ROOT, att.REMOTE_PATH, att.NAME as att_name " +
		"from AccPeriod accPeriod " +
		"left join FileAttachment att on att.ATTACHED_TO_CLASS_ID = 20061 and att.ATTACHED_TO_ID = accPeriod.ID " +
		"where accPeriod.start_date = ? and accPeriod.end_date = ? and module_id = ? and owner_id = ?";
	
	private static String isClosedSQL = 
		"select closed FROM AccPeriod where id = ?";
	
	private static String isClosedSQLDate = 
		"select closed from AccPeriod where ? >= start_date and ? <= end_date " +
		"and owner_id = ? and module_id = ?";

	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public AccPeriodDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	public void insert(AccPeriod accPeriod) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);
			build(accPeriod, ps, 0);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				accPeriod.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			accPeriod.setView();
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}

	public int build(AccPeriod period, PreparedStatement ps, int i) throws SQLException {
		// OWNER_ID
		ps.setLong(++i, period.getCompany().getDescriptor().getID());
		
		// MODULE_ID
		ps.setLong(++i, period.getModuleId());
		
		// START_DATE
		if(period.getStartDate() != null) {
			ps.setDate(++i, AEPersistentUtil.getSQLDate(period.getStartDate()));
		} else {
			ps.setNull(++i, java.sql.Types.DATE);
		}
		
		// END_DATE
		if(period.getEndDate() != null) {
			ps.setDate(++i, AEPersistentUtil.getSQLDate(period.getEndDate()));
		} else {
			ps.setNull(++i, java.sql.Types.DATE);
		}
		
		// CLOSED
		ps.setBoolean(++i, period.isClosed());
		
		// EXPORTED
		ps.setBoolean(++i, period.isExported());
		
		// return the current ps position 
		return i;
	}

	public void build(AccPeriod period, ResultSet rs) throws SQLException {
		// ID
		period.setID(rs.getLong("ID"));
		
		// OWNER_ID
		period.setCompany(Organization.lazyDescriptor(rs.getLong("OWNER_ID")));

		// MODULE_ID
		period.setModuleId(rs.getLong("MODULE_ID"));
		
		// START_DATE
		period.setStartDate(rs.getDate("START_DATE"));
		
		// END_DATE
		period.setEndDate(rs.getDate("END_DATE"));
		
		// CLOSED
		period.setClosed(rs.getBoolean("CLOSED"));
		
		// EXPORTED
		period.setExported(rs.getBoolean("EXPORTED"));
	}
	
	public void builExt(AccPeriod period, ResultSet rs) throws SQLException {
		// app_module_name
		try {
			period.setModuleName(rs.getString("app_module_name"));
		} catch(Exception e) {
			// nothing to do
		}
		
		try {
			period.grantFileAttachment().setID(rs.getLong("att_id"));
		} catch(Exception e) {
			// nothing to do
		}
		
	    //FILE_LENGTH
		period.grantFileAttachment().setFileLength(rs.getLong("FILE_LENGTH"));
		
		//REMOTE_ROOT
		period.grantFileAttachment().setRemoteRoot(rs.getString("REMOTE_ROOT"));
		
		//REMOTE_PATH
		period.grantFileAttachment().setRemotePath(rs.getString("REMOTE_PATH"));
		
		//ATT_NAME
		period.grantFileAttachment().setName(rs.getString("ATT_NAME"));
	}
	
	/**
	 * 
	 * @param ownerId
	 * @param moduleId
	 * @return <code>null</code> if there is no closed period 
	 * @throws AEException
	 */
	public AccPeriod getLastClosedPeriod(long ownerId, long moduleId) throws AEException {
		AccPeriod accPeriod = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLLastOpenClosePeriod);
			// owner_id = ? and module_id = ? and closed = ?
			ps.setLong(1, ownerId);
			ps.setLong(2, moduleId);
			ps.setLong(3, 1);
			rs = ps.executeQuery();
			if(rs.next()) {
				accPeriod = new AccPeriod();
				build(accPeriod, rs);
				accPeriod.setView();
			}
			return accPeriod;
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AccPeriod getLastPeriod(long ownerId, long moduleId) throws AEException {
		AccPeriod accPeriod = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLLastPeriod);
			// owner_id = ? and module_id = ?
			ps.setLong(1, ownerId);
			ps.setLong(2, moduleId);
			rs = ps.executeQuery();
			if(rs.next()) {
				accPeriod = new AccPeriod();
				build(accPeriod, rs);
				accPeriod.setView();
			}
			return accPeriod;
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AccPeriod getNextPeriod(long ownerId, long moduleId, Date forDate, AEConnection aeConnection) throws AEException {
		AccPeriod accPeriod = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLLastOpenClosePeriod);
			
			ps.setLong(1, ownerId);
			ps.setLong(2, moduleId);
			ps.setLong(3, 1);
			rs = ps.executeQuery();
			if(rs.next()) {
				accPeriod = new AccPeriod();
				build(accPeriod, rs);
				accPeriod.setView();
			}
			return accPeriod;
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	/**
	 * 
	 * @param ownerId
	 * @param moduleId
	 * @return <code>null</code> if there is no open period
	 * @throws AEException
	 */
	public AccPeriod getLastOpenPeriod(long ownerId, long moduleId) throws AEException {
		AccPeriod accPeriod = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLLastOpenClosePeriod);
			// owner_id = ? and module_id = ? and closed = ?
			ps.setLong(1, ownerId);
			ps.setLong(2, moduleId);
			ps.setLong(3, 0);
			rs = ps.executeQuery();
			if(rs.next()) {
				accPeriod = new AccPeriod();
				build(accPeriod, rs);
				accPeriod.setView();
			}
			return accPeriod;
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	/**
	 * Opens specified period
	 * @param periodId
	 * @throws AEException
	 */
	public void openPeriod(long periodId) throws AEException {
		PreparedStatement ps = null;
		try {
			// update
			ps = getAEConnection().prepareStatement(updateSQLOpenClosePeriod);
			ps.setBoolean(1, false);
			ps.setLong(2, periodId);
				
			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	/**
	 * Closes specified period
	 * @param periodId
	 * @throws AEException
	 */
	public void closePeriod(long periodId) throws AEException {
		PreparedStatement ps = null;
		try {
			// update
			ps = getAEConnection().prepareStatement(updateSQLOpenClosePeriod);
			ps.setBoolean(1, true);
			ps.setLong(2, periodId);
				
			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	/**
	 * 
	 * @param ownerId
	 * @param moduleId
	 * @return <code>null</code> if there is no closed period 
	 * @throws AEException
	 */
	public AccPeriod getFirstClosedPeriod(long ownerId, long moduleId) throws AEException {
		AccPeriod accPeriod = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLFirstOpenClosePeriod);
			// owner_id = ? and module_id = ? and closed = ?
			ps.setLong(1, ownerId);
			ps.setLong(2, moduleId);
			ps.setLong(3, 1);
			rs = ps.executeQuery();
			if(rs.next()) {
				accPeriod = new AccPeriod();
				build(accPeriod, rs);
				accPeriod.setView();
			}
			return accPeriod;
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	/**
	 * 
	 * @param ownerId
	 * @param moduleId
	 * @return <code>null</code> if there is no open period
	 * @throws AEException
	 */
	public AccPeriod getFirstOpenPeriod(long ownerId, long moduleId) throws AEException {
		AccPeriod accPeriod = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLFirstOpenClosePeriod);
			// owner_id = ? and module_id = ? and closed = ?
			ps.setLong(1, ownerId);
			ps.setLong(2, moduleId);
			ps.setLong(3, 0);
			rs = ps.executeQuery();
			if(rs.next()) {
				accPeriod = new AccPeriod();
				build(accPeriod, rs);
				accPeriod.setView();
			}
			return accPeriod;
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AccPeriodItems loadByFilter(long ownerId) throws AEException {
		AccPeriodItems periodsList = new AccPeriodItems();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLByFilter);
			// owner_id = ?
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			while(rs.next()) {
				AccPeriod period = new AccPeriod();
				build(period, rs);
				builExt(period, rs);
				
				period.setView();
				periodsList.add(period);
			}
			return periodsList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String selectSQLAllByCustomer = 
			"SELECT * FROM AccPeriod where OWNER_ID = ? and MODULE_ID = ? order by START_DATE desc";
	public AccPeriodItems loadAll(AEDescriptor ownerDescr, long moduleId) throws AEException {
		AccPeriodItems periodsList = new AccPeriodItems();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAllByCustomer);
			ps.setLong(1, ownerDescr.getID());
			ps.setLong(2, moduleId);
			rs = ps.executeQuery();
			while(rs.next()) {
				AccPeriod period = new AccPeriod();
				build(period, rs);
				
				period.setView();
				
				periodsList.add(period);
			}
			return periodsList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public CashPeriod loadCashPeriod(long accPeriodId) throws AEException {
		CashPeriod period = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCashPeriod);
			ps.setLong(1, accPeriodId);
			rs = ps.executeQuery();
			if(rs.next()) {
				period = new CashPeriod();
				build(period, rs);
				builExt(period, rs);
				
				period.grantFileAttachment().setView();
				period.setView();
			}
			return period;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AccPeriod loadAccPeriod(Date startDate, Date endDate, long module_id, long owner_id) throws AEException {
		AccPeriod period = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLPeriod);
			ps.setDate(1, AEPersistentUtil.getSQLDate(startDate));
			ps.setDate(2, AEPersistentUtil.getSQLDate(endDate));
			ps.setLong(3, module_id);
			ps.setLong(4, owner_id);
			rs = ps.executeQuery();
			if(rs.next()) {
				period = new AccPeriod();
				build(period, rs);
				builExt(period, rs);
				
				period.grantFileAttachment().setView();
				period.setView();
			}
			return period;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AccPeriod loadAccPeriod(Date date, long module_id, long owner_id) throws AEException {
		AccPeriod period = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLByDate);
			// OWNER_ID = ? and MODULE_ID = ? and START_DATE <= ? and END_DATE >= ?
			ps.setLong(1, owner_id);
			ps.setLong(2, module_id);
			ps.setDate(3, AEPersistentUtil.getSQLDate(date));
			ps.setDate(4, AEPersistentUtil.getSQLDate(date));
			rs = ps.executeQuery();
			if(rs.next()) {
				period = new AccPeriod();
				build(period, rs);
				period.setView();
			}
			return period;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AccPeriod loadAccPeriod(long accPeriodId) throws AEException {
		AccPeriod period = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLById);
			ps.setLong(1, accPeriodId);
			rs = ps.executeQuery();
			if(rs.next()) {
				period = new AccPeriod();
				build(period, rs);
				period.setView();
			}
			return period;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void exportedPeriod(long periodId) throws AEException {
		PreparedStatement ps = null;
		try {
			// update
			ps = getAEConnection().prepareStatement(updateSQLExported);
			ps.setBoolean(1, true);
			ps.setLong(2, periodId);
				
			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void notExportedPeriod(long periodId) throws AEException {
		PreparedStatement ps = null;
		try {
			// update
			ps = getAEConnection().prepareStatement(updateSQLExported);
			ps.setBoolean(1, false);
			ps.setLong(2, periodId);
				
			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public boolean isClosed(long periodId) throws AEException {
		boolean bRes = false;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(isClosedSQL);
			// periodId = ?
			ps.setLong(1, periodId);
			rs = ps.executeQuery();
			if(rs.next()) {
				bRes = rs.getBoolean("closed");
			}
			return bRes;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String isPreviousClosedSQL = 
			"select top 1 closed from AccPeriod where OWNER_ID = ? and MODULE_ID = ? and END_DATE < ? order by END_DATE desc";
	public boolean isPreviousClosed(AEDescriptor ownerDescr, long moduleId, Date start_date) throws AEException {
		boolean bRes = true;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(isPreviousClosedSQL);
			ps.setLong(1, ownerDescr.getID());
			ps.setLong(2, moduleId);
			ps.setDate(3, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDate(start_date)));
			rs = ps.executeQuery();
			if(rs.next()) {
				bRes = rs.getBoolean("closed");
			}
			return bRes;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String isNextOpenedSQL = 
			"select top 1 CLOSED from AccPeriod where OWNER_ID = ? and MODULE_ID = ? and START_DATE > ? order by START_DATE asc";
	public boolean isNextOpened(AEDescriptor ownerDescr, long moduleId, Date end_date) throws AEException {
		boolean bRes = true;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(isNextOpenedSQL);
			ps.setLong(1, ownerDescr.getID());
			ps.setLong(2, moduleId);
			ps.setDate(3, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDate(end_date)));
			rs = ps.executeQuery();
			if(rs.next()) {
				bRes = !rs.getBoolean("closed");
			}
			return bRes;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public boolean hasPrevious(AEDescriptor ownerDescr, long moduleId, Date start_date) throws AEException {
		boolean bRes = false;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(isPreviousClosedSQL);
			ps.setLong(1, ownerDescr.getID());
			ps.setLong(2, moduleId);
			ps.setDate(3, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDate(start_date)));
			rs = ps.executeQuery();
			if(rs.next()) {
				bRes = true;
			}
			return bRes;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public boolean isInClosedPeriod(Date date, long ownerId, long moduleId) throws AEException {
		boolean bRes = false;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(isClosedSQLDate);
			ps.setDate(1, AEPersistentUtil.getSQLDate(date));
			ps.setDate(2, AEPersistentUtil.getSQLDate(date));
			ps.setLong(3, ownerId);
			ps.setLong(4, moduleId);
			rs = ps.executeQuery();
			if(rs.next()) {
				bRes = rs.getBoolean("closed");
			}
			return bRes;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String selectSQLCheckForOverlap = 
		" select first.ID as fId, first.START_DATE as fStart, first.END_DATE as fEnd, second.ID as sId, second.START_DATE as sStart, second.END_DATE as sEnd "
		+ " from AccPeriod first inner join (select * from AccPeriod where OWNER_ID = ? and MODULE_ID = ?) second "
		+ " on first.OWNER_ID = ? and first.MODULE_ID = ? and first.ID < second.ID "
		+ "	and first.START_DATE <= second.END_DATE and second.START_DATE <= first.END_DATE";
	public void checkForOverlap(AccPeriod accPeriod) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCheckForOverlap);
			ps.setLong(1, accPeriod.getCompany().getDescriptor().getID());
			ps.setLong(2, accPeriod.getModuleId());
			ps.setLong(3, accPeriod.getCompany().getDescriptor().getID());
			ps.setLong(4, accPeriod.getModuleId());
			rs = ps.executeQuery();
			if(rs.next()) {
				throw AEError.System.OVERLAPPING_PERIODS.toException();
			}
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String selectSQLCheckAfterInitialSaldo = 
			"select top 1 ID from AccountBalance where owner_id = ? and YEAR > ? and is_initial = 1";
	public void checkBeforeInitialSaldo(AccPeriod accPeriod) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCheckAfterInitialSaldo);
			ps.setLong(1, accPeriod.getCompany().getDescriptor().getID());
			ps.setLong(2, AEDateUtil.getYear(accPeriod.getEndDate()));
			rs = ps.executeQuery();
			if(rs.next()) {
				throw AEError.System.INCORRECT_ACC_PERIOD.toException();
			}
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String selectSQLOpenPeriodCount = 
			" select count(id) as count from AccPeriod where OWNER_ID = ? and MODULE_ID = ? and CLOSED = 0";
	public int getOpenPeriodsCount(AEDescriptor ownerDescr, long moduleId) throws AEException {
		int count = 0;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLOpenPeriodCount);
			ps.setLong(1, ownerDescr.getID());
			ps.setLong(2, AEApp.ACCOUNTING_MODULE_ID);
			rs = ps.executeQuery();
			if(rs.next()) {
				count = rs.getInt("count");
			}
			return count;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	/**
	 * 
	 * @param ownerId
	 * @param moduleId
	 * @return <code>null</code> if there is no open period
	 * @throws AEException
	 */
	private static String selectSQLFirstTwoOpenPeriods = 
			"SELECT TOP 2 * FROM AccPeriod "
			+ " where OWNER_ID = ? and MODULE_ID = ? and CLOSED = 0 "
			+ " order by START_DATE asc";
	public AccPeriodItems getFirstTwoOpenPeriods(long ownerId, long moduleId) throws AEException {
		AccPeriodItems accPeriodItems = new AccPeriodItems();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLFirstTwoOpenPeriods);
			ps.setLong(1, ownerId);
			ps.setLong(2, moduleId);
			rs = ps.executeQuery();
			while(rs.next()) {
				AccPeriod accPeriod = new AccPeriod();
				build(accPeriod, rs);
				accPeriod.setView();
				
				accPeriodItems.add(accPeriod);
			}
			return accPeriodItems;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
}
