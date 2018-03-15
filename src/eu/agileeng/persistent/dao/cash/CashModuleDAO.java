package eu.agileeng.persistent.dao.cash;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.tomcat.util.json.JSONException;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.acc.AccPeriod;
import eu.agileeng.domain.cash.CashJETProp;
import eu.agileeng.domain.cash.CashJETransacion;
import eu.agileeng.domain.cash.CashJournalEntriesList;
import eu.agileeng.domain.cash.CashJournalEntry;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.util.AECollectionUtil;
import eu.agileeng.util.AEMath;

public class CashModuleDAO extends AbstractDAO {
	private static String insertSQL = "insert into CashJournal "
			+ "(GOA_ID, ACC_ID, JOURNAL_ID, ENTRY_DATE, ENTRY_TYPE, "
			+ " ENTRY_AMOUNT, DT_AMOUNT, CT_AMOUNT, OWNER_ID, ACCOUNTED, ACCOUNTED_AMOUNT) "
			+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static String updateSQL = "update CashJournal set "
			+ "GOA_ID = ?, ACC_ID = ?, JOURNAL_ID = ?, ENTRY_DATE = ?, ENTRY_TYPE = ?, "
			+ " ENTRY_AMOUNT = ?, DT_AMOUNT = ?, CT_AMOUNT = ?, OWNER_ID = ?, ACCOUNTED = ?, ACCOUNTED_AMOUNT = ? "
			+ " where ID = ?";
	
	private static String updateSQLToNotAccounted = "update CashJournal set "
		+ "ACCOUNTED = 0, ACCOUNTED_AMOUNT = 0.0 "
		+ " where OWNER_ID = ? and entry_date >= ? and entry_date <= ?";
	
	private static String deleteSQL = "delete from CashJournal where ID = ?";

	private static String selectSQLByOwner = 
		"select * from CashJournal where OWNER_ID = ?";
	
	private static String selectSQLByOwnerAndPeriod = 
		"select * from CashJournal where OWNER_ID = ? and entry_date >= ? and entry_date <= ?";
	
	private static String selectSQLByOwnerAndPeriodCode = 
		"select cj.*, acc.CODE as accCode from CashJournal cj " 
		+ "inner join Account acc on cj.ACC_ID = acc.ID " 
		+ "where cj.OWNER_ID = ? and entry_date >= ? and entry_date <= ?";
	
	private static String selectSQLNotAccountedExpensesByOwnerAndPeriod = 
		"select * from CashJournal where "
		+ "owner_id = ? and entry_date >= ? and entry_date <= ? and entry_type = 20 and accounted = 0";
	
	private static String insertSQLJETransacions = "insert into CashJETransactions "
		+ "(JOURNAL_ENTRY_ID, AMOUNT) values (?, ?)";

	private static String deleteSQLJETransacionsByJournalEntry = 
		"delete from CashJournal where JOURNAL_ENTRY_ID = ?";
	
	private static String deleteSQLBy_GOA_Date = 
		"delete from CashJournal where GOA_ID = ? and ENTRY_DATE = ?";
	
	private static String deleteSQLById = 
		"delete from CashJournal where ID = ?";
	
	private static String selectSQLBy_GOA_Date_Owner = 
		"select * from CashJournal where GOA_ID = ? and ENTRY_DATE = ? and owner_id = ?";

	private static String selectSQLJETransacionsByJournalEntry = 
		"select * from CashJETransactions where JOURNAL_ENTRY_ID = ?";

	private static String insertSQLJETProps = "insert into CashJETProperties "
		+ "(JE_ID, JET_ID, ATTRIBUTE_ID, VALUE) values (?, ?, ?, ?)";

	private static String selectSQLJETPropsByTrans = 
		"select * from CashJETProperties where JET_ID = ?";
	
	private static String selectSQLAssetDiversTransactions = 
		"select * from CashJournal " 
		+ " where OWNER_ID = ? and ENTRY_DATE between ? and ? "
		+ " and ACC_ID = ? and ENTRY_TYPE = 10 "
		+ " order by ENTRY_DATE asc";
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public CashModuleDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	public List<CashJournalEntry> loadJournalEntries(long ownerId) throws AEException, JSONException {
		List<CashJournalEntry> entriesList = new LinkedList<CashJournalEntry>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLByOwner);
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			while (rs.next()) {
				CashJournalEntry entry = new CashJournalEntry();
				build(entry, rs);
				
				// load transactions and their properties
				List<CashJETransacion> transList =  loadCJETransactions(entry.getDescriptor());
				if(!AECollectionUtil.isEmpty(transList)) {
					entry.setJeTransactions(transList);
					for (CashJETransacion cashJETransacion : transList) {
						List<CashJETProp> propsList = loadCJETProps(cashJETransacion.getDescriptor());
						cashJETransacion.setPropsList(propsList);
					}
				}
				
				entry.setView();
				entriesList.add(entry);
			}
			return entriesList;
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public CashJournalEntriesList loadJournalEntries(long ownerId, Date startDate, Date endDate) throws AEException, JSONException {
		CashJournalEntriesList entriesList = new CashJournalEntriesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLByOwnerAndPeriod);
			ps.setLong(1, ownerId);
			ps.setDate(2, AEPersistentUtil.getSQLDate(startDate));
			ps.setDate(3, AEPersistentUtil.getSQLDate(endDate));
			rs = ps.executeQuery();
			while (rs.next()) {
				CashJournalEntry entry = new CashJournalEntry();
				build(entry, rs);
				
				// load transactions and their properties
				List<CashJETransacion> transList =  loadCJETransactions(entry.getDescriptor());
				if(!AECollectionUtil.isEmpty(transList)) {
					entry.setJeTransactions(transList);
					for (CashJETransacion cashJETransacion : transList) {
						List<CashJETProp> propsList = loadCJETProps(cashJETransacion.getDescriptor());
						cashJETransacion.setPropsList(propsList);
					}
				}
				
				entry.setView();
				entriesList.add(entry);
			}
			return entriesList;
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public CashJournalEntriesList loadJournalEntriesLazzy(long ownerId, Date startDate, Date endDate) throws AEException, JSONException {
		CashJournalEntriesList entriesList = new CashJournalEntriesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLByOwnerAndPeriodCode);
			ps.setLong(1, ownerId);
			ps.setDate(2, AEPersistentUtil.getSQLDate(startDate));
			ps.setDate(3, AEPersistentUtil.getSQLDate(endDate));
			rs = ps.executeQuery();
			while (rs.next()) {
				CashJournalEntry entry = new CashJournalEntry();
				build(entry, rs);
				entry.setAccCode(rs.getString("accCode"));
				entry.setView();
				entriesList.add(entry);
			}
			return entriesList;
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public CashJournalEntriesList loadNotAccountedExpensesJournalEntriesLazzy(long ownerId, Date startDate, Date endDate) throws AEException {
		CashJournalEntriesList entriesList = new CashJournalEntriesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLNotAccountedExpensesByOwnerAndPeriod);
			ps.setLong(1, ownerId);
			ps.setDate(2, AEPersistentUtil.getSQLDate(startDate));
			ps.setDate(3, AEPersistentUtil.getSQLDate(endDate));
			rs = ps.executeQuery();
			while (rs.next()) {
				CashJournalEntry entry = new CashJournalEntry();
				build(entry, rs);
				entry.setView();
				entriesList.add(entry);
			}
			return entriesList;
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public CashJournalEntry loadJournalEntryForUpdate(long goaID, Date entryDate, long ownerId) throws AEException, JSONException {
		CashJournalEntry entry = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLBy_GOA_Date_Owner);
			ps.setLong(1, goaID);
			ps.setDate(2, AEPersistentUtil.getSQLDate(entryDate));
			ps.setLong(3, ownerId);
			rs = ps.executeQuery();
			if (rs.next()) {
				entry = new CashJournalEntry();
				build(entry, rs);
				entry.setView();
			}
			return entry;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void insert(CashJournalEntry entry) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);
			build(entry, ps, 0);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				entry.setID(id);
			} else {
				throw new AEException(getClass().getName()
						+ "::insert: No keys were generated");
			}
			
			// process transactions
			AEDescriptor entryDescr = entry.getDescriptor();
			List<CashJETransacion> transList = entry.getJeTransactions();
			if(!AECollectionUtil.isEmpty(transList)) {
				insertCJETransacions(transList, entryDescr);
				for (CashJETransacion cashJETransacion : transList) {
					insertCJETProps(cashJETransacion.getPropsList(), entryDescr, cashJETransacion.getDescriptor());
				}
			}

			// set view state
			entry.setView();
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}

	public void delete(CashJournalEntry entry) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQL);

			// build statement
			int i = 1;
			ps.setLong(i++, entry.getID());

			// execute
			@SuppressWarnings("unused")
			int count = ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void deleteByGOAAndDate(CashJournalEntry entry) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQLBy_GOA_Date);

			// build statement
			ps.setLong(1, entry.getGoaId());
			ps.setDate(2, AEPersistentUtil.getSQLDate(entry.getDate()));

			// execute
			@SuppressWarnings("unused")
			int count = ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void deleteCashJournalEntryByID(long cashJournalEntryId) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQLById);

			// build statement
			ps.setLong(1, cashJournalEntryId);

			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	public int build(CashJournalEntry trans, PreparedStatement ps, int i) throws SQLException {
		// GOA_ID
		ps.setLong(++i, trans.getGoaId());
		// ACC_ID
		ps.setLong(++i, trans.getAccId());
		// JOURNAL_ID
		ps.setLong(++i, trans.getJournalId());
		// ENTRY_DATE
		ps.setDate(++i, AEPersistentUtil.getSQLDate(trans.getEntryDate()));
		// ENTRY_TYPE
		ps.setInt(++i, trans.getEntryType());
		// ENTRY_AMOUNT
		if(trans.getAmount() != null && !Double.isNaN(trans.getAmount())) {
			ps.setDouble(++i, trans.getAmount().doubleValue());
		} else {
			ps.setNull(++i, java.sql.Types.NUMERIC);
		}
		// DT_AMOUNT
		if(trans.getDtAmount() != null) {
			ps.setDouble(++i, trans.getDtAmount().doubleValue());
		} else {
			ps.setNull(++i, java.sql.Types.NUMERIC);
		}
		// CT_AMOUNT
		if(trans.getCtAmount() != null) {
			ps.setDouble(++i, trans.getCtAmount().doubleValue());
		} else {
			ps.setNull(++i, java.sql.Types.NUMERIC);
		}
		// OWNER_ID
		ps.setLong(++i, trans.getOwnerId());
		
		//ACCOUNTED
		ps.setBoolean(++i, trans.isAccounted());
		
		//ACCOUNTED_AMOUNT
		if(trans.getAccountedAmount() != null) {
			ps.setDouble(++i, trans.getAccountedAmount());
		} else {
			ps.setNull(++i, java.sql.Types.NUMERIC);
		}
		
		// return the current ps position
		return i;
	}

	public void build(CashJournalEntry entry, ResultSet rs) throws SQLException {
		// ID
		entry.setID(rs.getLong("ID"));
		// GOA_ID
		entry.setGoaId(rs.getLong("GOA_ID"));
		// ACC_ID
		entry.setAccId(rs.getLong("ACC_ID"));
		// JOURNAL_ID
		entry.setJournalId(rs.getLong("JOURNAL_ID"));
		// ENTRY_DATE
		entry.setEntryDate(rs.getDate("ENTRY_DATE"));
		// ENTRY_TYPE
		entry.setEntryType(rs.getInt("ENTRY_TYPE"));
		// ENTRY_AMOUNT
		double amount = rs.getDouble("ENTRY_AMOUNT");
		if(!rs.wasNull()) {
			entry.setAmount(amount);
		} else {
			entry.setAmount(null);
		}
		// DT_AMOUNT
		amount = rs.getDouble("DT_AMOUNT");
		if(!rs.wasNull()) {
			entry.setDtAmount(amount);
		} else {
			entry.setDtAmount(null);
		}
		// CT_AMOUNT
		amount = rs.getDouble("CT_AMOUNT");
		if(!rs.wasNull()) {
			entry.setCtAmount(amount);
		} else {
			entry.setCtAmount(null);
		}
		// OWNER_ID
		entry.setOwnerId(rs.getLong("OWNER_ID"));
		
		//ACCOUNTED
		entry.setAccounted(rs.getBoolean("ACCOUNTED"));
		
		//ACCOUNTED_AMOUNT
		double accAmount = rs.getDouble("ACCOUNTED_AMOUNT");
		if(!rs.wasNull()) {
			entry.setAccountedAmount(new Double(accAmount));
		}
	}

	public void update(CashJournalEntry entry) throws AEException, JSONException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			int i = build(entry, ps, 0);
			ps.setLong(++i, entry.getID());

			// execute
			ps.executeUpdate();

			// set view state
			entry.setView();
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void updateToNotAccounted(AEDescriptor ownerDescr, AccPeriod accPeriod) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQLToNotAccounted);

			// build statement
			ps.setLong(1, ownerDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(accPeriod.getStartDate()));
			ps.setDate(3, AEPersistentUtil.getSQLDate(accPeriod.getEndDate()));			

			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void update(CashJournalEntriesList entriesList) throws AEException, JSONException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			for (CashJournalEntry cashJournalEntry : entriesList) {
				if(!cashJournalEntry.isUpdated()) {
					continue;
				}
				
				int i = build(cashJournalEntry, ps, 0);
				ps.setLong(++i, cashJournalEntry.getID());

				// execute
				ps.executeUpdate();

				// set view state
				cashJournalEntry.setView();
			}
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public int build(CashJETransacion trans, PreparedStatement ps, int i) throws SQLException {
		// JOURNAL_ENTRY_ID
		ps.setLong(++i, trans.getToCashJournalEntry().getID());
		
		//AMOUNT
		ps.setDouble(++i, AEMath.doubleValue(trans.getAmount()));
		
		// return the current ps position
		return i;
	}

	public void build(CashJETransacion trans, ResultSet rs) throws SQLException {
		// ID
		trans.setID(rs.getLong("ID"));
		
		// JOURNAL_ENTRY_ID
		trans.setToCashJournalEntry(CashJournalEntry.createAEDescriptor(rs.getLong("JOURNAL_ENTRY_ID")));
		
		//AMOUNT
		trans.setAmount(rs.getDouble("AMOUNT"));
	}
	
	public void insertCJETransacions(List<CashJETransacion> transList, AEDescriptor toCJE) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLJETransacions);
			for (CashJETransacion cashJETransacion : transList) {
				cashJETransacion.setToCashJournalEntry(toCJE);
				
				build(cashJETransacion, ps, 0);

				// execute
				ps.executeUpdate();

				// set generated key
				rs = ps.getGeneratedKeys();
				if (rs.next()) {
					// propagate generated key
					long id = rs.getLong(1);
					cashJETransacion.setID(id);
				} else {
					throw new AEException(getClass().getName()
							+ "::insert: No keys were generated");
				}

				// set view state
				cashJETransacion.setView();
			}
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void deleteCJETransacions(AEDescriptor toCJEdescr) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQLJETransacionsByJournalEntry);

			// build statement
			int i = 1;
			ps.setLong(i++, toCJEdescr.getID());

			// execute
			@SuppressWarnings("unused")
			int count = ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public List<CashJETransacion> loadCJETransactions(AEDescriptor toCJEdescr) throws AEException {
		List<CashJETransacion> transList = new LinkedList<CashJETransacion>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLJETransacionsByJournalEntry);
			ps.setLong(1, toCJEdescr.getID());
			rs = ps.executeQuery();
			while (rs.next()) {
				CashJETransacion trans = new CashJETransacion();
				build(trans, rs);
				trans.setView();
				transList.add(trans);
			}
			return transList;
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public int build(CashJETProp jetProp, PreparedStatement ps, int i) throws SQLException {
		//JE_ID
		ps.setLong(++i, jetProp.getToCJEntryDescr().getID());
		
		//JET_ID
		ps.setLong(++i, jetProp.getToCJETransactionDescr().getID());
		
		//PROP_ID
		ps.setLong(++i, jetProp.getPropDefId());
		
		//AMOUNT
		ps.setString(++i, jetProp.getValue());
		
		// return the current ps position
		return i;
	}

	public void build(CashJETProp prop, ResultSet rs) throws SQLException {
		//ID
		prop.setID(rs.getInt("ID"));
		
		//JE_ID
		prop.setToCJEntryDescr(new AEDescriptorImp(
				rs.getLong("JE_ID"),  
				DomainClass.CashJournalEntry));
		
		//JET_ID
		prop.setToCJETransactionDescr(new AEDescriptorImp(
				rs.getLong("JET_ID"),  
				DomainClass.CashJETransaction));
		
		//PROP_ID
		prop.setPropDefId(rs.getLong("ATTRIBUTE_ID"));
		
		//AMOUNT
		prop.setValue(rs.getString("VALUE"));
	}
	
	public void insertCJETProps(List<CashJETProp> transList, AEDescriptor toCJE, AEDescriptor toCJET) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLJETProps);
			for (CashJETProp cashJETProp : transList) {
				cashJETProp.setToCJEntryDescr(toCJE);
				cashJETProp.setToCJETransactionDescr(toCJET);
				
				build(cashJETProp, ps, 0);

				// execute
				ps.executeUpdate();

				// set generated key
				rs = ps.getGeneratedKeys();
				if (rs.next()) {
					// propagate generated key
					long id = rs.getLong(1);
					cashJETProp.setID(id);
				} else {
					throw new AEException(getClass().getName()
							+ "::insert: No keys were generated");
				}

				// set view state
				cashJETProp.setView();
			}
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void deleteCJETProps(AEDescriptor toCJEdescr) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQLJETransacionsByJournalEntry);

			// build statement
			int i = 1;
			ps.setLong(i++, toCJEdescr.getID());

			// execute
			@SuppressWarnings("unused")
			int count = ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public List<CashJETProp> loadCJETProps(AEDescriptor toCJETdescr) throws AEException {
		List<CashJETProp> transList = new LinkedList<CashJETProp>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLJETPropsByTrans);
			ps.setLong(1, toCJETdescr.getID());
			rs = ps.executeQuery();
			while (rs.next()) {
				CashJETProp trans = new CashJETProp();
				build(trans, rs);
				trans.setView();
				transList.add(trans);
			}
			return transList;
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public CashJournalEntriesList loadAssetDiversTransactions(long ownerId, long accId, Date startDate, Date endDate) throws AEException, JSONException {
		CashJournalEntriesList entriesList = new CashJournalEntriesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAssetDiversTransactions);
			ps.setLong(1, ownerId);
			ps.setDate(2, AEPersistentUtil.getSQLDate(startDate));
			ps.setDate(3, AEPersistentUtil.getSQLDate(endDate));
			ps.setLong(4, accId);
			rs = ps.executeQuery();
			while (rs.next()) {
				CashJournalEntry entry = new CashJournalEntry();
				build(entry, rs);
				
				// load transactions and their properties
				List<CashJETransacion> transList =  loadCJETransactions(entry.getDescriptor());
				if(!AECollectionUtil.isEmpty(transList)) {
					entry.setJeTransactions(transList);
//					for (CashJETransacion cashJETransacion : transList) {
//						List<CashJETProp> propsList = loadCJETProps(cashJETransacion.getDescriptor());
//						cashJETransacion.setPropsList(propsList);
//					}
				}
				
				entry.setView();
				entriesList.add(entry);
			}
			return entriesList;
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

}
