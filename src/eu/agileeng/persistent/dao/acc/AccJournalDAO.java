package eu.agileeng.persistent.dao.acc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDescriptorsList;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.AccAccountBalancesList;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.acc.AccAccountBalance;
import eu.agileeng.domain.acc.AccAccountBalanceExt;
import eu.agileeng.domain.acc.AccJournalEntry;
import eu.agileeng.domain.acc.AccJournalFilter;
import eu.agileeng.domain.acc.AccJournalItem;
import eu.agileeng.domain.acc.AccJournalItemsList;
import eu.agileeng.domain.acc.AccJournalResult;
import eu.agileeng.domain.acc.AccJournalResultsList;
import eu.agileeng.domain.acc.AccPeriod;
import eu.agileeng.domain.acc.cashbasis.AccJournalEntryTemplate.AccountIdentificationRule;
import eu.agileeng.domain.acc.cashbasis.AccJournalEntryTemplate.JournalIdentificationRule;
import eu.agileeng.domain.acc.cashbasis.FinancialTransactionTemplate;
import eu.agileeng.domain.acc.cashbasis.FinancialTransactionTemplate.PaymentMethod;
import eu.agileeng.domain.acc.cashbasis.Quete;
import eu.agileeng.domain.acc.cashbasis.QuetesList;
import eu.agileeng.domain.acc.export.ExportRequest;
import eu.agileeng.domain.business.bank.BankAccount;
import eu.agileeng.domain.cefra.n11580_03.GrandLivreDataSource;
import eu.agileeng.domain.contact.Address;
import eu.agileeng.domain.contact.Contributor;
import eu.agileeng.domain.contact.Employee;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.contact.SimpleParty;
import eu.agileeng.domain.document.AEDocumentDescriptor;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.imp.AEDocumentDescriptorImp;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AETimePeriod;

public class AccJournalDAO extends AbstractDAO {

	private static String insertSQL = 
		"insert into GeneralJournal "
		+ " (OWNER_ID, JOURNAL_ID, JOURNAL_CODE, BATCH_ID, ENTRY_ID, ENTRY_DATE, ACCOUNT_ID, AUXILIARY, "
		+ " REFERENCE, DESCRIPTION, CURRENCY, DEBIT_AMOUNT, CREDIT_AMOUNT, ACC_PERIOD_ID, REFERENCE_ID, REFERENCE_TYPE, DEBIT, CREDIT, "
		+ " ACC_IDENT_RULE_ID, JOURNAL_IDENT_RULE_ID, QUETE_CODE, CREATION_DATE, CONTRIBUTOR_ID, SUPPLIER_ID, BANK_ACCOUNT_ID, YEAR_END_CLOSING) "
		+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
//	private static String updateSQL = 
//			"update GeneralJournal "
//			+ " set OWNER_ID = ?, JOURNAL_ID = ?, JOURNAL_CODE= ?, BATCH_ID= ?, ENTRY_ID= ?, ENTRY_DATE= ?, ACCOUNT_ID= ?, AUXILIARY= ?, "
//			+ " REFERENCE= ?, DESCRIPTION= ?, CURRENCY= ?, DEBIT_AMOUNT= ?, CREDIT_AMOUNT= ?, ACC_PERIOD_ID= ?, REFERENCE_ID= ?, REFERENCE_TYPE= ?, DEBIT= ?, CREDIT= ?, "
//			+ " ACC_IDENT_RULE_ID= ?, JOURNAL_IDENT_RULE_ID= ?, QUETE_CODE= ?, CREATION_DATE = ?, CONTRIBUTOR_ID = ?, SUPPLIER_ID = ?, BANK_ACCOUNT_ID = ? "
//			+ " where ID = ?";
	
	private static String updateSQLUserData = 
			"update GeneralJournal "
			+ " set ENTRY_DATE= ?, ACCOUNT_ID= ?, AUXILIARY= ?, "
			+ " REFERENCE= ?, DESCRIPTION= ?, CURRENCY= ?, DEBIT_AMOUNT= ?, CREDIT_AMOUNT= ?, ACC_PERIOD_ID= ?, REFERENCE_ID= ?, REFERENCE_TYPE= ?, DEBIT= ?, CREDIT= ?, "
			+ " QUETE_CODE= ?, CREATION_DATE = ?, CONTRIBUTOR_ID = ?, SUPPLIER_ID = ? "
			+ " where ID = ? and OWNER_ID = ?";
	
	private static String insertSQLEntry = 
		"insert into GeneralJournalEntry (CODE, NAME, DESCRIPTION) values (?, ?, ?)";

	private static String updateSQLEntry = 
		"update GeneralJournalEntry set CODE = ?, NAME = ?, DESCRIPTION = ? where ID = ?";
//
//	private static String deleteSQL = "";
	
	private static String selectSQL = 
		"select gj.*, acc.CODE as ACCOUNT_CODE from GeneralJournal gj inner join Account acc "
		+ " on gj.ACCOUNT_ID = acc.ID where gj.OWNER_ID = ?";
	
	private static String selectSQLItemsByEntryId = 
			"select gj.*, acc.CODE as ACCOUNT_CODE from GeneralJournal gj inner join Account acc "
			+ " on gj.ACCOUNT_ID = acc.ID where gj.entry_id = ?";
	
	private static String deleteSQLByPeriod = 
		"delete from GeneralJournal where ACC_PERIOD_ID = ?";
	
	private static String deleteSQLByPeriodAndAccount = 
		"delete from GeneralJournal where ACC_PERIOD_ID = ? AND ACCOUNT_ID = ?";
	
//	SELECT 
//	  gJournal.*, 
//	  acc.CODE as acc_code, 
//	  doc.TYPE_ID as doc_type, doc.DOC_DATE as doc_date, doc.NIMBER_STRING as doc_number, 
//	  tDoc.PAYMENT_DUE_DATE doc_payment_due_date, 
//	  accPeriod.START_DATE as start_date, accPeriod.END_DATE as end_date, 
//	  appModule.NAME as module_name,
//	  issuer.payType_Id as issuer_pay_type,
//	  recipient.payType_Id as recipient_pay_type 
//	  FROM GeneralJournal gJournal inner join Account acc on gJournal.ACCOUNT_ID = acc.ID 
//	  inner join AccPeriod accPeriod on gJournal.ACC_PERIOD_ID = accPeriod.ID 
//	  inner join AppModule appModule on accPeriod.MODULE_ID = appModule.ID 
//	  left join Document doc on gJournal.REFERENCE_ID = doc.ID and gJournal.REFERENCE_TYPE = doc.CLASS_ID
//	  left join TradeDocument tDoc on doc.ID = tDoc.DOC_ID
//	  left join Company issuer on tDoc.ISSUER_ID = issuer.PartyID
//	  left join Company recipient on tDoc.RECIPIENT_ID = recipient.PartyID
	private static String selectSQLExport = 
		"SELECT gJournal.*, acc.CODE as account_code, " +
		"doc.TYPE_ID as doc_type, doc.DOC_DATE as doc_date, doc.NIMBER_STRING as doc_number, " +
		"tDoc.PAYMENT_DUE_DATE doc_payment_due_date, " +
		"accPeriod.START_DATE as start_date, accPeriod.END_DATE as end_date, " +
		"appModule.NAME as module_name, appModule.CODE as module_code, " +
		"issuer.payType_Id as issuer_pay_type, " +
		"recipient.payType_Id as recipient_pay_type " + 
		"FROM GeneralJournal gJournal inner join Account acc on gJournal.ACCOUNT_ID = acc.ID " +
		"inner join AccPeriod accPeriod on gJournal.ACC_PERIOD_ID = accPeriod.ID " +
		"left join Document doc on gJournal.REFERENCE_ID = doc.ID and gJournal.REFERENCE_TYPE = doc.CLASS_ID " +
		"left join TradeDocument tDoc on doc.ID = tDoc.DOC_ID " +
		"left join Company issuer on tDoc.ISSUER_ID = issuer.PartyID " +
		"left join Company recipient on tDoc.RECIPIENT_ID = recipient.PartyID " +
		"inner join AppModule appModule on accPeriod.MODULE_ID = appModule.ID " +
		
		"where gJournal.OWNER_ID = ? and gJournal.ACC_PERIOD_ID = ? " + 
		"order by gJournal.ENTRY_DATE ASC";
	
	private static String selectSQLExportByID = 
		"SELECT gJournal.*, acc.CODE as account_code, " +
		"doc.TYPE_ID as doc_type, doc.DOC_DATE as doc_date, doc.NIMBER_STRING as doc_number, " +
		"tDoc.PAYMENT_DUE_DATE doc_payment_due_date, " +
		"accPeriod.START_DATE as start_date, accPeriod.END_DATE as end_date, " +
		"appModule.NAME as module_name, appModule.CODE as module_code, " +
		"issuer.payType_Id as issuer_pay_type, " +
		"recipient.payType_Id as recipient_pay_type " + 
		"FROM GeneralJournal gJournal inner join Account acc on gJournal.ACCOUNT_ID = acc.ID " +
		"inner join AccPeriod accPeriod on gJournal.ACC_PERIOD_ID = accPeriod.ID " +
		"left join Document doc on gJournal.REFERENCE_ID = doc.ID and gJournal.REFERENCE_TYPE = doc.CLASS_ID " +
		"left join TradeDocument tDoc on doc.ID = tDoc.DOC_ID " +
		"left join Company issuer on tDoc.ISSUER_ID = issuer.PartyID " +
		"left join Company recipient on tDoc.RECIPIENT_ID = recipient.PartyID " +
		"inner join AppModule appModule on accPeriod.MODULE_ID = appModule.ID " +
		
		"where gJournal.entry_id = ?";
	
	private static String deleteSQLGeneralJournalEntryByAccPeriod = 
		"delete from GeneralJournalEntry where id in "
		+ "(SELECT distinct(entry_id) from GeneralJournal where acc_period_id = ?)";
	
//	private static String selectSQLJournalEntryByFilter = 
//		"SELECT gjItem.*, "
//		+ "	acc.CODE as ACCOUNT_CODE, acc.NAME as ACCOUNT_NAME, "
//		+ "	ft.ID as FT_ID, "   
//		+ "	contr.ID as CONTR_ID, contr.EMPLOYEE_ID as CONTR_EMPL_ID, "  
//		+ " suppl.id as suppl_id, suppl.name as suppl_name, "
//		+ "	empl.FirstName as empl_firstname, empl.LastName as empl_lastname, empl.NAME as empl_name, "  
//		+ "	addr.STREET as addr_street, addr.POSTAL_CODE as addr_postal_code, addr.CITY as addr_city "  
//		+ "from "
//		+ "	GeneralJournal gjItem " 
//		+ "	inner join Account acc on gjItem.ACCOUNT_ID = acc.ID "  
//		+ "	inner join GeneralJournalEntry gjEntry on gjItem.ENTRY_ID = gjEntry.ID "
//		+ "	left outer join FinancialTransaction ft on ft.ACC_JOURNAL_ENTRY_ID = gjEntry.ID " 
//		+ "	left outer join Contributor contr on ft.CONTRIBUTOR_ID = contr.ID "  
//		+ "	left outer join SimpleParty suppl on ft.SUPPLIER_ID = suppl.ID "  
//		+ "	left outer join Employee empl on contr.EMPLOYEE_ID = empl.ID "  
//		+ "	left outer join Address addr on empl.ID = addr.TO_OBJ_ID and addr.TO_CLASS_ID = " + DomainClass.EMPLOYEE.getID() + "  and addr.TYPE_ID = " + Address.Type.BUSINESS.getID() + " "
//		+ "where ft.owner_id = ? and ft.PAYMENT_METHOD_ID = ? ";
	

//			"SELECT ft.ID as FT_ID, gjItem.*, "
//			+ "acc.CODE as ACCOUNT_CODE, acc.NAME as ACCOUNT_NAME,  "
//			+ "contr.ID as CONTR_ID, contr.EMPLOYEE_ID as CONTR_EMPL_ID,  " 
//			+ "empl.FirstName as empl_firstname, empl.LastName as empl_lastname, empl.NAME as empl_name,  "
//			+ "addr.STREET as addr_street, addr.POSTAL_CODE as addr_postal_code, addr.CITY as addr_city  "
//			+ "from FinancialTransaction ft  "
//			+ "inner join GeneralJournalEntry gjEntry on ft.ACC_JOURNAL_ENTRY_ID = gjEntry.ID " 
//			+ "inner join GeneralJournal gjItem on gjEntry.ID = gjItem.ENTRY_ID  "
//			+ "inner join Account acc on gjItem.ACCOUNT_ID = acc.ID  "
//			+ "left outer join Contributor contr on ft.CONTRIBUTOR_ID = contr.ID  "
//			+ "left outer join Employee empl on contr.EMPLOYEE_ID = empl.ID  "
//			+ "left outer join Address addr on empl.ID = addr.TO_OBJ_ID and addr.TO_CLASS_ID = " + DomainClass.EMPLOYEE + " and addr.TYPE_ID = " + Address.Type.BUSINESS.getID() + " "
//		    + "where ft.owner_id = ? and ft.PAYMENT_METHOD_ID = ? ";
	
	private static String selectSQLAccountTurnover = 
			"select ACCOUNT_ID, SUM(DEBIT_AMOUNT) as debitT, SUM(CREDIT_AMOUNT) as creditT " 
			+ " from GeneralJournal gj " 
			+ " where ACCOUNT_ID = ? and ENTRY_DATE between ? AND ? " 
			+ " GROUP BY ACCOUNT_ID";
	
	private static String selectSQLJournalEntry = 
			"SELECT * FROM GeneralJournalEntry where id = ?";
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public AccJournalDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	public void insertEntry(AccJournalEntry entry) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			/**
			 * Insert entry
			 */
			ps = getAEConnection().prepareGenKeyStatement(insertSQLEntry);
			buildEntry(entry, ps, 0);
			ps.executeUpdate();
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// entry_id
				long id = rs.getLong(1);
				entry.setID(id);
				entry.propEntryId();
				
				// owner_id
				entry.propCompany();
				
				// batch_id
				long batchId = nextValue(entry.getCompany().getDescriptor(), 0, AEStringUtil.EMPTY_STRING);
				entry.propBatchId(batchId);
				
				entry.setView();
				
				AEConnection.close(ps);
				AEConnection.close(rs);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			/**
			 * Insert items
			 */
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);
			AccJournalItemsList itemsList = entry.getAccJournalItems();
			for (AccJournalItem accJournalItem : itemsList) { 
				assert(accJournalItem.getAccPeriod().isPersistent());
//				String journalCode = accJournalItem.getJournal() != null ? accJournalItem.getJournal().getCode() : AEStringUtil.EMPTY_STRING;
//				long batchId = nextValue(accJournalItem.getCompany().getDescriptor(), AEDateUtil.getYear(accJournalItem.getDate()), journalCode);
//				accJournalItem.setBatchId(batchId);
				buildItem(accJournalItem, ps, 0);

				// execute
				ps.executeUpdate();

				// set generated key
				rs = ps.getGeneratedKeys();
				if (rs.next()) {
					// propagate generated key
					long id = rs.getLong(1);
					accJournalItem.setID(id);
				} else {
					throw new AEException(getClass().getName() + "::insert: No keys were generated");
				}

				// set view state
				accJournalItem.setView();
			}

			// set view state
			entry.setView();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void updateEntry(AccJournalEntry entry) throws AEException {
		PreparedStatement ps = null;
		try {
			/**
			 * Update entry
			 */
			ps = getAEConnection().prepareStatement(updateSQLEntry);
			int i = buildEntry(entry, ps, 0);
			ps.setLong(++i, entry.getID());
			ps.executeUpdate();
			AEConnection.close(ps);
			
			/**
			 * Update items
			 */
			entry.propCompany();
			AccJournalItemsList itemsList = entry.getAccJournalItems();
			for (AccJournalItem accJournalItem : itemsList) { 
				updateItemUserData(accJournalItem);
			}

			// set view state
			entry.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
//	public void updateItem(AccJournalItem accJournalItem) throws AEException {
//		PreparedStatement ps = null;
//		try {
//			ps = getAEConnection().prepareStatement(updateSQL);
//			int	i = buildItem(accJournalItem, ps, 0);
//			ps.setLong(++i, accJournalItem.getID());
//			ps.executeUpdate();
//
//			// set view state
//			accJournalItem.setView();
//
//			// set view state
//			accJournalItem.setView();
//		} catch (SQLException e) {
//			throw new AEException(e);
//		} finally {
//			AEConnection.close(ps);
//			close();
//		}
//	}
	
	public void updateItemUserData(AccJournalItem accJournalItem) throws AEException {
		PreparedStatement ps = null;
		try {
			ps = getAEConnection().prepareStatement(updateSQLUserData);
			int	i = buildItemUserData(accJournalItem, ps, 0);
			ps.setLong(++i, accJournalItem.getID());
			ps.setLong(++i, accJournalItem.getCompany().getDescriptor().getID());
			ps.executeUpdate();

			// set view state
			accJournalItem.setView();

			// set view state
			accJournalItem.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	private static String deleteSQLEntry = "delete from GeneralJournalEntry where id = ?";
	private static String deleteSQLItemsByEntryId = "delete from GeneralJournal where entry_id = ?";
	public void deleteEntry(AEDescriptor entry) throws AEException {
		PreparedStatement ps = null;
		try {
			// delete items
			ps = getAEConnection().prepareStatement(deleteSQLItemsByEntryId);
			ps.setLong(1, entry.getID());
			ps.executeUpdate();
			AEConnection.close(ps);
			
			// delete from journal items
			ps = getAEConnection().prepareStatement(deleteSQLEntry);
			ps.setLong(1, entry.getID());
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public int buildItem(AccJournalItem item, PreparedStatement ps, int i) throws SQLException {
		// OWNER_ID
		ps.setLong(++i, item.getCompany().getDescriptor().getID());
		// JOURNAL_ID, JOURNAL_CODE
		if(item.getJournal() != null) {
			ps.setLong(++i, item.getJournal().getID());
			ps.setString(++i, item.getJournal().getCode());
		} else {
			ps.setNull(++i, java.sql.Types.INTEGER);
			ps.setNull(++i, java.sql.Types.NVARCHAR);
		}
		// BATCH_ID
		ps.setLong(++i, item.getBatchId());
		// ENTRY_ID
		ps.setLong(++i, item.getEntryId());
		// ENTRY_DATE
		ps.setDate(++i, AEPersistentUtil.getSQLDate(item.getDate()));
		// ACCOUNT_ID
		ps.setLong(++i, item.getAccount().getID());
		// AUXILIARY
		if(item.getAuxiliary() != null) {
			ps.setString(++i, item.getAuxiliary().getCode());
		} else {
			ps.setNull(++i, java.sql.Types.NVARCHAR);
		}
		// REFERENCE
		if(item.getReference() != null) {
			ps.setString(++i, item.getReference().getDescription());
		} else {
			ps.setNull(++i, java.sql.Types.NVARCHAR);
		}
		// DESCRIPTION
		ps.setString(++i, item.getDescription());
		// CURRENCY
		ps.setString(++i, "EUR");
		// DEBIT_AMOUNT
		if(item.getDtAmount() != null) {
			ps.setDouble(++i, item.getDtAmount());
		} else {
			ps.setNull(++i, java.sql.Types.NUMERIC);
		}
		// CREDIT_AMOUNT
		if(item.getCtAmount() != null) {
			ps.setDouble(++i, item.getCtAmount());
		} else {
			ps.setNull(++i, java.sql.Types.NUMERIC);
		}
		// ACC_PERIOD_ID
		if(item.getAccPeriod() != null) {
			ps.setLong(++i, item.getAccPeriod().getID());
		} else {
			ps.setNull(++i, java.sql.Types.INTEGER);
		}
		// REFERENCE_ID, REFERENCE_TYPE
		if(item.getReference() != null) {
			ps.setLong(++i, item.getReference().getID());
			ps.setLong(++i, item.getReference().getClazz().getID());
		} else {
			ps.setNull(++i, java.sql.Types.INTEGER);
			ps.setNull(++i, java.sql.Types.INTEGER);
		}
		// DEBIT
		ps.setBoolean(++i, item.isDebit());
		
		// CREDIT
		ps.setBoolean(++i, item.isCredit());
		
		// ACC_IDENT_RULE_ID
		if(item.getAccountIdentificationRule() != null) {
			ps.setLong(++i, item.getAccountIdentificationRule().getId());
		} else {
			ps.setNull(++i, java.sql.Types.INTEGER);
		}
		
		// JOURNAL_IDENT_RULE_ID
		if(item.getJournalIdentificationRule() != null) {
			ps.setLong(++i, item.getJournalIdentificationRule().getId());
		} else {
			ps.setNull(++i, java.sql.Types.INTEGER);
		}
		
		// QUETE_CODE
		if(item.getQuete() != null && !AEStringUtil.isEmpty(item.getQuete().getCode())) {
			ps.setString(++i, item.getQuete().getCode());
		} else {
			ps.setNull(++i, java.sql.Types.NCHAR);
		}
		
		// CREATION_DATE
		ps.setDate(++i, AEPersistentUtil.getSQLDate(item.getDateCreation()));
		
		// CONTRIBUTOR_ID
		if(item.getContributor() != null) {
			ps.setLong(++i, item.getContributor().getID());
		} else {
			ps.setNull(++i, java.sql.Types.NUMERIC);
		}
		
		// SUPPLIER_ID
		if(item.getSupplier() != null) {
			ps.setLong(++i, item.getSupplier().getID());
		} else {
			ps.setNull(++i, java.sql.Types.NUMERIC);
		}
		
		// BANK_ACCOUNT_ID
		if(item.getBankAccount() != null) {
			ps.setLong(++i, item.getBankAccount().getID());
		} else {
			ps.setNull(++i, java.sql.Types.NUMERIC);
		}
		
		// YEAR_END_CLOSING
		ps.setBoolean(++i, item.isYearEndClosing());
		
		// return the current ps position 
		return i;
	}

	public int buildItemUserData(AccJournalItem item, PreparedStatement ps, int i) throws SQLException {
		// ENTRY_DATE
		ps.setDate(++i, AEPersistentUtil.getSQLDate(item.getDate()));

		// ACCOUNT_ID
		ps.setLong(++i, item.getAccount().getID());

		// AUXILIARY
		if(item.getAuxiliary() != null) {
			ps.setString(++i, item.getAuxiliary().getCode());
		} else {
			ps.setNull(++i, java.sql.Types.NVARCHAR);
		}

		// REFERENCE
		if(item.getReference() != null) {
			ps.setString(++i, item.getReference().getDescription());
		} else {
			ps.setNull(++i, java.sql.Types.NVARCHAR);
		}

		// DESCRIPTION
		ps.setString(++i, item.getDescription());

		// CURRENCY
		ps.setString(++i, "EUR");

		// DEBIT_AMOUNT
		if(item.getDtAmount() != null) {
			ps.setDouble(++i, item.getDtAmount());
		} else {
			ps.setNull(++i, java.sql.Types.NUMERIC);
		}

		// CREDIT_AMOUNT
		if(item.getCtAmount() != null) {
			ps.setDouble(++i, item.getCtAmount());
		} else {
			ps.setNull(++i, java.sql.Types.NUMERIC);
		}

		// ACC_PERIOD_ID
		if(item.getAccPeriod() != null) {
			ps.setLong(++i, item.getAccPeriod().getID());
		} else {
			ps.setNull(++i, java.sql.Types.INTEGER);
		}

		// REFERENCE_ID, REFERENCE_TYPE
		if(item.getReference() != null) {
			ps.setLong(++i, item.getReference().getID());
			ps.setLong(++i, item.getReference().getClazz().getID());
		} else {
			ps.setNull(++i, java.sql.Types.INTEGER);
			ps.setNull(++i, java.sql.Types.INTEGER);
		}

		// DEBIT
		ps.setBoolean(++i, item.isDebit());
		
		// CREDIT
		ps.setBoolean(++i, item.isCredit());
		
		// QUETE_CODE
		if(item.getQuete() != null && !AEStringUtil.isEmpty(item.getQuete().getCode())) {
			ps.setString(++i, item.getQuete().getCode());
		} else {
			ps.setNull(++i, java.sql.Types.NCHAR);
		}
		
		// CREATION_DATE
		ps.setDate(++i, AEPersistentUtil.getSQLDate(item.getDateCreation()));
		
		// CONTRIBUTOR_ID
		if(item.getContributor() != null) {
			ps.setLong(++i, item.getContributor().getID());
		} else {
			ps.setNull(++i, java.sql.Types.NUMERIC);
		}
		
		// SUPPLIER_ID
		if(item.getSupplier() != null) {
			ps.setLong(++i, item.getSupplier().getID());
		} else {
			ps.setNull(++i, java.sql.Types.NUMERIC);
		}
		
		// return the current ps position 
		return i;
	}
	
	public int buildEntry(AccJournalEntry entry, PreparedStatement ps, int i) throws SQLException {
		// CODE
		ps.setString(++i, entry.getCode());
		
		// NAME
		ps.setString(++i, entry.getName());
		
		// NAME
		ps.setString(++i, entry.getDescription());
		
		// return the current ps position 
		return i;
	}
	
	public void buildItem(AccJournalItem item, ResultSet rs) throws SQLException {
		// ID
		item.setID(rs.getLong("ID"));

		// OWNER_ID
		item.setCompany(Organization.lazyDescriptor(rs.getLong("OWNER_ID")));

		// JOURNAL_ID, JOURNAL_CODE
		AEDescriptorImp journalDescr = new AEDescriptorImp();
		journalDescr.setID(rs.getLong("JOURNAL_ID"));
		journalDescr.setCode(rs.getString("JOURNAL_CODE"));
		item.setJournal(journalDescr);
		
		// BATCH_ID
		item.setBatchId(rs.getLong("BATCH_ID"));
		
		// ENTRY_ID
		item.setEntryId(rs.getLong("ENTRY_ID"));
		
		// ENTRY_DATE
		item.setDate(rs.getDate("ENTRY_DATE"));
		
		// ACCOUNT_ID, ACCOUNT_CODE, ACCOUNT_NAME
		AEDescriptorImp accDescr = new AEDescriptorImp(rs.getLong("ACCOUNT_ID"));
		accDescr.setCode(rs.getString("ACCOUNT_CODE"));
		try {
			accDescr.setName(rs.getString("ACCOUNT_NAME"));
		} catch (Exception e){};
		item.setAccount(accDescr);
		
		// AUXILIARY
		AEDescriptorImp auxDescr = new AEDescriptorImp();
		auxDescr.setCode(rs.getString("AUXILIARY"));
		item.setAuxiliary(auxDescr);
		
		// DESCRIPTION
		item.setDescription(rs.getString("DESCRIPTION"));

		// CURRENCY
		AEDescriptorImp currDescr = new AEDescriptorImp();
		currDescr.setCode(rs.getString("CURRENCY"));
		item.setCurrency(currDescr);
		
		// DEBIT_AMOUNT
		double dtAmount = rs.getDouble("DEBIT_AMOUNT");
		if(!rs.wasNull()) {
			item.setDtAmount(new Double(dtAmount));
		}
		
		// CREDIT_AMOUNT
		double ctAmount = rs.getDouble("CREDIT_AMOUNT");
		if(!rs.wasNull()) {
			item.setCtAmount(new Double(ctAmount));
		}
		
		// ACC_PERIOD_ID
		long accPeriodID = rs.getLong("ACC_PERIOD_ID");
		if(!rs.wasNull()) {
			item.setAccPeriod(AccPeriod.lazyDescriptor(accPeriodID));
		}
		
		// REFERENCE_ID, REFERENCE_TYPE, REFERENCE
		AEDocumentDescriptor reference = new AEDocumentDescriptorImp();
		reference.setID(rs.getLong("REFERENCE_ID"));
		reference.setClazz(DomainClass.valueOf(rs.getLong("REFERENCE_TYPE")));
		reference.setDescription(rs.getString("REFERENCE"));
		item.setReference(reference);
		
		// DEBIT
		item.setDebit(rs.getBoolean("DEBIT"));
		
		// CREDIT
		item.setCredit(rs.getBoolean("CREDIT"));
		
		// ACC_IDENT_RULE_ID
		long tmpId = rs.getLong("ACC_IDENT_RULE_ID");
		if(!rs.wasNull()) {
			item.setAccountIdentificationRule(AccountIdentificationRule.valueOf(tmpId));
		}
		
		// JOURNAL_IDENT_RULE_ID
		tmpId = rs.getLong("JOURNAL_IDENT_RULE_ID");
		if(!rs.wasNull()) {
			item.setJournalIdentificationRule(JournalIdentificationRule.valueOf(tmpId));
		}
		
		// QUETE_CODE
		String tmpString = rs.getString("QUETE_CODE");
		if(!rs.wasNull()) {
			AEDescriptor queteDescr = Quete.lazyDescriptor(AEPersistentUtil.NEW_ID).withCode(tmpString);
			item.setQuete(queteDescr);
		}
		
		// CREATION_DATE
		item.setDateCreation(rs.getDate("CREATION_DATE"));
		
		// CONTRIBUTOR_ID
		tmpId = rs.getLong("CONTRIBUTOR_ID");
		if(!rs.wasNull()) {
			item.setContributor(Contributor.lazyDescriptor(tmpId));
		}
		
		// SUPPLIER_ID
		tmpId = rs.getLong("SUPPLIER_ID");
		if(!rs.wasNull()) {
			item.setSupplier(SimpleParty.lazyDescriptor(tmpId));
		}
		
		// BANK_ACCOUNT_ID
		tmpId = rs.getLong("BANK_ACCOUNT_ID");
		if(!rs.wasNull()) {
			item.setBankAccount(BankAccount.lazyDescriptor(tmpId));
		}
		
		// TALLY
		try {
			boolean tally = rs.getBoolean("TALLY");
			item.setTally(tally);
		} catch (Exception e) {
		}
	}
	
	public void buildItemExport(AccJournalItem item, ResultSet rs) throws SQLException {
		// doc_type, doc_date, doc_number
		AEDocumentDescriptor refDescriptor = (AEDocumentDescriptor) item.grantReference();
		refDescriptor.setDocumentType(AEDocumentType.valueOf(rs.getLong("doc_type")));
		refDescriptor.setDate(rs.getDate("doc_date"));
		refDescriptor.setNumber(rs.getString("doc_number"));
		
		// doc_payment_due_date
		item.setPaymentDueDate(rs.getDate("doc_payment_due_date"));
		// issuer_pay_type
		AEDescriptor issuerPaymentType = new AEDescriptorImp();
		issuerPaymentType.setSysId(rs.getLong("issuer_pay_type"));
		item.setIssuerPaymentType(issuerPaymentType);
		// recipient_pay_type
		AEDescriptor recipientPaymentType = new AEDescriptorImp();
		recipientPaymentType.setSysId(rs.getLong("recipient_pay_type"));
		item.setRecipientPaymentType(recipientPaymentType);
	}
	
	public AccJournalItemsList load(long ownerId, String journalCode, Date entryDate) throws AEException {
		AccJournalItemsList itemsList = new AccJournalItemsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			// owner_id = ? and journal_code = ? and entry_date = ?
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			while(rs.next()) {
				AccJournalItem item = new AccJournalItem(null);
				buildItem(item, rs);
				item.setView();
				itemsList.add(item);
			}
			return itemsList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AccJournalItemsList loadItemsToEntry(long accEntryId) throws AEException {
		AccJournalItemsList itemsList = new AccJournalItemsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLItemsByEntryId);
			ps.setLong(1, accEntryId);
			rs = ps.executeQuery();
			while(rs.next()) {
				AccJournalItem item = new AccJournalItem(null);
				buildItem(item, rs);
				item.setView();
				itemsList.add(item);
			}
			return itemsList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AccJournalEntry loadEntry(long accJournalEntryId) throws AEException {
		AccJournalEntry accJournalEntry = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLJournalEntry);
			ps.setLong(1, accJournalEntryId);
			rs = ps.executeQuery();
			if(rs.next()) {
				accJournalEntry = new AccJournalEntry();
				build(accJournalEntry, rs);
				accJournalEntry.setView();
			}
			return accJournalEntry;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void deleteByAccPeriodId(long id) throws AEException {
		PreparedStatement ps = null;
		try {
			// delete from joural entries
			ps = getAEConnection().prepareStatement(deleteSQLGeneralJournalEntryByAccPeriod);
			ps.setLong(1, id);
			ps.executeUpdate();
			AEConnection.close(ps);
			
			// delete from journal items
			ps = getAEConnection().prepareStatement(deleteSQLByPeriod);
			ps.setLong(1, id);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void deleteByPeriodAndAccount(long periodID, long accID) throws AEException {
		PreparedStatement ps = null;
		try {
			// delete from joural entries
			ps = getAEConnection().prepareStatement(deleteSQLByPeriodAndAccount);
			ps.setLong(1, periodID);
			ps.setLong(2, accID);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public ExportRequest loadForExport(long ownerId, long accPeriodId) throws AEException {
		AccJournalItemsList itemsList = new AccJournalItemsList();
		ExportRequest expRequest = new ExportRequest();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLExport);
			// where gJournal.OWNER_ID = ? and gJournal.ACC_PERIOD_ID = ?
			ps.setLong(1, ownerId);
			ps.setLong(2, accPeriodId);
			rs = ps.executeQuery();
			while(rs.next()) {
				AccJournalItem item = new AccJournalItem(null);
				buildItem(item, rs);
				buildItemExport(item, rs);
				// start_date
				expRequest.setStartDate(rs.getDate("start_date"));
				// end_date
				expRequest.setEndDate(rs.getDate("end_date"));
				// module_name
				expRequest.grantAppModule().setName(rs.getString("module_name"));
				// module_code
				expRequest.grantAppModule().setCode(rs.getString("module_code"));

				item.setView();
				itemsList.add(item);
			}
			expRequest.setAccJournalItemsList(itemsList);
			return expRequest;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public ExportRequest loadForExport(AEDescriptorsList entriesList) throws AEException {
		AccJournalItemsList itemsList = new AccJournalItemsList();
		ExportRequest expRequest = new ExportRequest();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLExportByID);
			if(entriesList != null) {
				for (AEDescriptor accEntryDescr : entriesList) {
					ps.setLong(1, accEntryDescr.getID());
					rs = ps.executeQuery();
					while(rs.next()) {
						AccJournalItem item = new AccJournalItem(null);
						buildItem(item, rs);
						buildItemExport(item, rs);
						// start_date
						expRequest.setStartDate(item.getDate());
						// end_date
						expRequest.setEndDate(item.getDate());
						// module_name
						expRequest.grantAppModule().setName(rs.getString("module_name"));
						// module_code
						expRequest.grantAppModule().setCode(rs.getString("module_code"));

						item.setView();
						itemsList.add(item);
					}
				}
			}
			expRequest.setAccJournalItemsList(itemsList);
			return expRequest;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String selectSQLJournalEntryByFilter = 
			"SELECT gjItem.*, "
			+ "	acc.CODE as ACCOUNT_CODE, acc.NAME as ACCOUNT_NAME, "
			+ "	contr.ID as CONTR_ID, contr.EMPLOYEE_ID as CONTR_EMPL_ID, "  
			+ " suppl.id as suppl_id, suppl.name as suppl_name, "
			+ "	empl.FirstName as empl_firstname, empl.LastName as empl_lastname, empl.NAME as empl_name, "  
			+ "	addr.STREET as addr_street, addr.POSTAL_CODE as addr_postal_code, addr.CITY as addr_city "  
			+ "from "
			+ "	GeneralJournal gjItem " 
			+ "	inner join Account acc on gjItem.ACCOUNT_ID = acc.ID "  
			+ "	left outer join Contributor contr on gjItem.CONTRIBUTOR_ID = contr.ID "  
			+ "	left outer join SimpleParty suppl on gjItem.SUPPLIER_ID = suppl.ID "  
			+ "	left outer join Employee empl on contr.EMPLOYEE_ID = empl.ID "  
			+ "	left outer join Address addr on empl.ID = addr.TO_OBJ_ID and addr.TO_CLASS_ID = " + DomainClass.EMPLOYEE.getID() + "  and addr.TYPE_ID = " + Address.Type.BUSINESS.getID() + " "
			+ "where gjItem.owner_id = ? and acc.ACC_TYPE = " + AccAccount.AccountType.ACCOUNT.getTypeId() + " ";
	public AccJournalResultsList load(AccJournalFilter filter) throws AEException {
		AccJournalResultsList results = new AccJournalResultsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = selectSQLJournalEntryByFilter;
			StringBuffer sb = new StringBuffer(sql);
			
			// paymentMethod
			if(filter.getPaymentMethod() != null) {
				sb
				.append(" and gjItem.JOURNAL_IDENT_RULE_ID = ")
				.append(filter.getPaymentMethod().getJournalIdentificationRule().getId());
			}
			
			// bankAcc
			if(filter.getBankAccount() != null) {
				sb
				.append(" and gjItem.BANK_ACCOUNT_ID = ")
				.append(filter.getBankAccount().getID());
			}
			
			// period
			if(filter.getDateFrom() != null) {
				sb
					.append(" and gjItem.entry_date >= ")
					.append(AEPersistentUtil.escapeToDate(filter.getDateFrom()));
			};
			if(filter.getDateTo() != null) {
				sb
					.append(" and gjItem.entry_date <= ")
					.append(AEPersistentUtil.escapeToDate(filter.getDateTo()));
			};
			
			// description
			if(!AEStringUtil.isEmpty(filter.getDescription())) {
				sb
					.append(" and UPPER(gjItem.DESCRIPTION) like '%")
				    .append(filter.getDescription().toUpperCase()).append("%' ");
			}
			
			// amountFrom
			if(filter.getAmountFrom() != null) {
				sb
					.append(" and (CASE WHEN ISNULL(gjItem.CREDIT_AMOUNT, 0.0) >= ISNULL(gjItem.DEBIT_AMOUNT, 0.0) THEN ISNULL(gjItem.CREDIT_AMOUNT, 0.0) ELSE ISNULL(gjItem.DEBIT_AMOUNT, 0.0) END) >= ")
					.append(filter.getAmountFrom());
			}

			// amountTo
			if(filter.getAmountTo() != null) {
				sb
				.append(" and (CASE WHEN ISNULL(gjItem.CREDIT_AMOUNT, 0.0) >= ISNULL(gjItem.DEBIT_AMOUNT, 0.0) THEN ISNULL(gjItem.CREDIT_AMOUNT, 0.0) ELSE ISNULL(gjItem.DEBIT_AMOUNT, 0.0) END) <= ")
					.append(filter.getAmountTo());
			}
			
			// accJournal
			if(filter.getAccJournal() != null && !AEStringUtil.isEmpty(filter.getAccJournal().getCode())) {
				sb.append(" and JOURNAL_CODE like '").append(filter.getAccJournal().getCode()).append("'");
			}
			
			// accCodeFrom
			if(!AEStringUtil.isEmpty(filter.getAccCodeFrom())) {
				sb.append(" and CONVERT(int, acc.CODE) >= ").append(Integer.parseInt(filter.getAccCodeFrom()));
			}
			
			// accCodeTo
			if(!AEStringUtil.isEmpty(filter.getAccCodeTo())) {
				sb.append(" and CONVERT(int, acc.CODE) <= ").append(Integer.parseInt(filter.getAccCodeTo()));
			}
			
			// tally
			if(AccJournalFilter.TallyState.checked.equals(filter.getTallyState()) 
					|| AccJournalFilter.TallyState.unchecked.equals(filter.getTallyState())) {
				
				if(AccJournalFilter.TallyState.checked.equals(filter.getTallyState())) {
					sb.append(" and gjItem.tally = 1 ");
				} else {
					sb.append(" and (gjItem.tally is null or gjItem.tally != 1) ");
				}
			}
			
			/**
			 * Append order
			 */
			sb.append(" order by ENTRY_DATE asc, ID asc ");
			
			/**
			 * Create statement
			 */
			ps = getAEConnection().prepareStatement(sb.toString());

			/**
			 * Append system filter
			 */
			ps.setLong(1, filter.getCompany().getDescriptor().getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				AccJournalResult res = new AccJournalResult();
				build(res, rs);
				
				res.setView();
				if(res.getAccJournalItem() != null) {
					res.getAccJournalItem().setView();
				}
				
				results.add(res);
			}
			return results;
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	/**
	 * Zero amount balance items are not included in the result
	 */
	private static String selectSQLAccountBalanceByFilter = 
		"select balance.*, acc.CODE as ACCOUNT_CODE, acc.NAME as ACCOUNT_NAME from AccountBalance balance "
		+ " inner join Account acc on balance.ACCOUNT_ID = acc.ID "
		+ " where balance.OWNER_ID = ? and TYPE = 0 and acc.ACC_TYPE = " + AccAccount.AccountType.ACCOUNT.getTypeId() + " ";
	public AccJournalResultsList loadOpeningBalance(AccJournalFilter filter) throws AEException {
		AccJournalResultsList results = new AccJournalResultsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = selectSQLAccountBalanceByFilter;
			StringBuffer sb = new StringBuffer(sql);
			
			/**
			 * Filter fields
			 */
			
			// period
			if(filter.getDateFrom() != null) {
				sb
					.append(" and DATEFROMPARTS(year, 1, 1) >= ")
					.append(AEPersistentUtil.escapeToDate(filter.getDateFrom()));
			};
			
			if(filter.getDateTo() != null) {
				sb
					.append(" and DATEFROMPARTS(year, 1, 1) <= ")
					.append(AEPersistentUtil.escapeToDate(filter.getDateTo()));
			};
			
			// amountFrom
			if(filter.getAmountFrom() != null) {
				sb
					.append(" and (CASE WHEN ISNULL(CREDIT_AMOUNT, 0.0) >= ISNULL(DEBIT_AMOUNT, 0.0) THEN ISNULL(CREDIT_AMOUNT, 0.0) ELSE ISNULL(DEBIT_AMOUNT, 0.0) END) >= ")
					.append(filter.getAmountFrom());
			}

			// amountTo
			if(filter.getAmountTo() != null) {
				sb
				.append(" and (CASE WHEN ISNULL(CREDIT_AMOUNT, 0.0) >= ISNULL(DEBIT_AMOUNT, 0.0) THEN ISNULL(CREDIT_AMOUNT, 0.0) ELSE ISNULL(DEBIT_AMOUNT, 0.0) END) <= ")
					.append(filter.getAmountTo());
			}
			
			// accCodeFrom
			if(!AEStringUtil.isEmpty(filter.getAccCodeFrom())) {
				sb.append(" and CONVERT(int, acc.CODE) >= ").append(Integer.parseInt(filter.getAccCodeFrom()));
			}
			
			// accCodeTo
			if(!AEStringUtil.isEmpty(filter.getAccCodeTo())) {
				sb.append(" and CONVERT(int, acc.CODE) <= ").append(Integer.parseInt(filter.getAccCodeTo()));
			}
			
			/**
			 * Append order
			 */
			sb.append(" order by acc.CODE asc ");
			
			ps = getAEConnection().prepareStatement(sb.toString());
			ps.setLong(1, filter.getCompany().getDescriptor().getID());
//			ps.setInt(2, AEDateUtil.getYear(filter.getDateFrom()));
			rs = ps.executeQuery();
			while(rs.next()) {
				AccJournalResult item = new AccJournalResult();
				buildOpeningBalance(item, rs);
				
				// check dt and ct amounts
				if(!AEMath.isZeroAmount(item.getAccJournalItem().getDtAmount()) || !AEMath.isZeroAmount(item.getAccJournalItem().getCtAmount())) {
					item.setView();
					item.getAccJournalItem().setView();
					
					// add to the result collection
					results.add(item);
				}
			}
			return results;
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void build(AccJournalResult item, ResultSet rs) throws SQLException {
		// id
		item.setID(rs.getLong("ID"));
		
		// AccJournalItem
		AccJournalItem accJournalItem = new AccJournalItem();
		buildItem(accJournalItem, rs);
		item.setAccJournalItem(accJournalItem);
		
		// Contributor
		long contrId = rs.getLong("CONTR_ID");
		if(!rs.wasNull()) {
			Address a = new Address();
			if(!item.isLoadLazzy()) {
				a.setStreet(rs.getString("addr_STREET"));
				a.setPostalCode(rs.getString("addr_POSTAL_CODE"));
				a.setCity(rs.getString("addr_CITY"));
			}
			
			Employee e = new Employee();
			e.setFirstName(rs.getString("empl_FirstName"));
			e.setLastName(rs.getString("empl_LastName"));
			e.setName(rs.getString("empl_NAME"));
			e.setAddress(a);
			
			Contributor contr = new Contributor();
			contr.setID(contrId);
			contr.setEmployee(e);
			
			item.setContributor(contr);
		}	
		
		// supplier
		long supplId = rs.getLong("suppl_id");
		if(!rs.wasNull()) {
			String name = rs.getString("suppl_name");
			item.setSupplier(new AEDescriptorImp(supplId).withName(name));
		}
		
		// openingBalance
		
		// closingBalance
	}
	
	/**
	 * IMPORTANT: IDs are negative to be different from accJournalEntries IDs
	 * 
	 * @param item
	 * @param rs
	 * @throws SQLException
	 */
	public void buildOpeningBalance(AccJournalResult item, ResultSet rs) throws SQLException {
		// AccJournalItem
		AccJournalItem accJournalItem = new AccJournalItem();
		
		/**
		 * build AccJournalItem
		 */

		// ID
		accJournalItem.setID(rs.getLong("ID") * -1L);
		item.setID(rs.getLong("ID") * -1L);

		// OWNER_ID
		accJournalItem.setCompany(Organization.lazyDescriptor(rs.getLong("OWNER_ID")));

		// JOURNAL_ID, JOURNAL_CODE
		AEDescriptor journalDescr = new AEDescriptorImp()
			.withId(FinancialTransactionTemplate.PaymentMethod.NOUVEAU.getId())
			.withCode(FinancialTransactionTemplate.PaymentMethod.NOUVEAU.getCode());
		accJournalItem.setJournal(journalDescr);
		
		// BATCH_ID
		
		// ENTRY_ID
		// IMPORTANT: ENTRY_ID = 1L
		accJournalItem.setEntryId(1L);
		
		// ENTRY_DATE
		accJournalItem.setDate(AEDateUtil.getFirstDate(0, rs.getInt("YEAR")));
		
		// ACCOUNT_ID, ACCOUNT_CODE, ACCOUNT_NAME
		AEDescriptorImp accDescr = new AEDescriptorImp(rs.getLong("ACCOUNT_ID"));
		accDescr.setCode(rs.getString("ACCOUNT_CODE"));
		accDescr.setName(rs.getString("ACCOUNT_NAME"));
		accJournalItem.setAccount(accDescr);
		
		// AUXILIARY
		
		// DESCRIPTION

		// CURRENCY
		accJournalItem.setCurrency(new AEDescriptorImp().withCode("EUR"));
		
		// DEBIT_AMOUNT
		double dtAmount = rs.getDouble("DEBIT_AMOUNT");
		if(!rs.wasNull()) {
			accJournalItem.setDtAmount(new Double(dtAmount));
		}
		
		// CREDIT_AMOUNT
		double ctAmount = rs.getDouble("CREDIT_AMOUNT");
		if(!rs.wasNull()) {
			accJournalItem.setCtAmount(new Double(ctAmount));
		}
		
		// ACC_PERIOD_ID
		
		// REFERENCE_ID, REFERENCE_TYPE, REFERENCE
		
		// DEBIT
		accJournalItem.setDebit(!AEMath.isZeroAmount(dtAmount));
		
		// CREDIT
		accJournalItem.setCredit(!AEMath.isZeroAmount(ctAmount));
		
		// ACC_IDENT_RULE_ID
		accJournalItem.setAccountIdentificationRule(AccountIdentificationRule.BALANCE_OPERATION);
		
		// JOURNAL_IDENT_RULE_ID
		accJournalItem.setJournalIdentificationRule(JournalIdentificationRule.AA);
		
		// QUETE_CODE
		
		// CREATION_DATE
		accJournalItem.setDateCreation(rs.getDate("DATE_CREATED"));
		
		// CONTRIBUTOR_ID
		
		// SUPPLIER_ID
		
		// BANK_ACCOUNT_ID

		/**
		 * end build
		 */
		
		item.setAccJournalItem(accJournalItem);
	}
	
	public void calculateTurnover(AccAccountBalance accBalance) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAccountTurnover);
			ps.setLong(1, accBalance.getAccAccount().getDescriptor().getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(accBalance.getPeriod().getStartDate()));
			ps.setDate(3, AEPersistentUtil.getSQLDate(accBalance.getPeriod().getEndDate()));
			rs = ps.executeQuery();
			if(rs.next()) {
				double debitT = rs.getDouble("debitT");
				accBalance.setDebitTurnover(debitT);
				
				double creditT = rs.getDouble("creditT");
				accBalance.setCreditTurnover(creditT);
			}
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String selectSQLAccountTurnoverTallyChecked = 
			"select ACCOUNT_ID, SUM(DEBIT_AMOUNT) as debitT, SUM(CREDIT_AMOUNT) as creditT " 
			+ " from GeneralJournal gj " 
			+ " where ACCOUNT_ID = ? and ENTRY_DATE between ? AND ? and tally = 1" 
			+ " GROUP BY ACCOUNT_ID";
	private static String selectSQLAccountTurnoverTallyUnchecked = 
			"select ACCOUNT_ID, SUM(DEBIT_AMOUNT) as debitT, SUM(CREDIT_AMOUNT) as creditT " 
			+ " from GeneralJournal gj " 
			+ " where ACCOUNT_ID = ? and ENTRY_DATE between ? AND ? and (tally is null or tally != 1)" 
			+ " GROUP BY ACCOUNT_ID";
	public void calculateTurnoverByTally(AccAccountBalance accBalance, boolean checked) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			if(checked) {
				ps = getAEConnection().prepareStatement(selectSQLAccountTurnoverTallyChecked);
			} else {
				ps = getAEConnection().prepareStatement(selectSQLAccountTurnoverTallyUnchecked);
			}

			ps.setLong(1, accBalance.getAccAccount().getDescriptor().getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(accBalance.getPeriod().getStartDate()));
			ps.setDate(3, AEPersistentUtil.getSQLDate(accBalance.getPeriod().getEndDate()));
			rs = ps.executeQuery();
			if(rs.next()) {
				double debitT = rs.getDouble("debitT");
				accBalance.setDebitTurnover(debitT);
				
				double creditT = rs.getDouble("creditT");
				accBalance.setCreditTurnover(creditT);
			}
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String selectSQLAccountTurnoverByJournal = 
			"select ACCOUNT_ID, SUM(DEBIT_AMOUNT) as debitT, SUM(CREDIT_AMOUNT) as creditT " 
			+ " from GeneralJournal gj " 
			+ " where ACCOUNT_ID = ? and ENTRY_DATE between ? AND ? and JOURNAL_CODE = ?" 
			+ " GROUP BY ACCOUNT_ID";
	public void calculateTurnover(AccAccountBalance accBalance, AEDescriptive journal) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAccountTurnoverByJournal);
			ps.setLong(1, accBalance.getAccAccount().getDescriptor().getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(accBalance.getPeriod().getStartDate()));
			ps.setDate(3, AEPersistentUtil.getSQLDate(accBalance.getPeriod().getEndDate()));
			ps.setString(4, journal.getDescriptor().getCode());
			rs = ps.executeQuery();
			if(rs.next()) {
				double debitT = rs.getDouble("debitT");
				accBalance.setDebitTurnover(debitT);
				
				double creditT = rs.getDouble("creditT");
				accBalance.setCreditTurnover(creditT);
			}
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String selectSQLBudgetReal = 
			"select account_id, sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount " 
			+ "from GeneralJournal gj "
			+ "inner join Account acc on gj.ACCOUNT_ID = acc.ID "  
			+ "where gj.OWNER_ID = ? and gj.ENTRY_DATE between ? and ? and acc.code like ? "
			+ "group by ACCOUNT_ID ";
	public JSONArray loadBudgetRealIncome(AEDescriptor tenantDescr, AETimePeriod period) throws AEException {
		JSONArray bArr = new JSONArray();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLBudgetReal);
			
			// select 7* accounts
			ps.setLong(1, tenantDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(period.getStartDate()));
			ps.setDate(3, AEPersistentUtil.getSQLDate(period.getEndDate()));
			ps.setString(4, "7%");
			rs = ps.executeQuery();
			while(rs.next()) {
				JSONObject b = new JSONObject();
				
				// OWNER_ID
				// b.put(AEDomainObject.JSONKey.ownerId.name(), tenantDescr.getID());

				// ACCOUNT_ID
				b.put(AEDomainObject.JSONKey.id.name(), rs.getLong("account_id"));
				
				// YEAR
				// b.put("year", AEDateUtil.getYear(period.getStartDate()));
				
				// AMOUNT
				double dtAmount = rs.getDouble("debit_amount");
				// b.put("dtAmount", dtAmount);
				
				double ctAmount = rs.getDouble("credit_amount");
				// b.put("ctAmount", ctAmount);
				
				b.put("amount", ctAmount - dtAmount);
				
				bArr.put(b);
			}
			
			return bArr;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public JSONArray loadBudgetRealExpense(AEDescriptor tenantDescr, AETimePeriod period) throws AEException {
		JSONArray bArr = new JSONArray();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLBudgetReal);
			
			// select 6* accounts
			ps.setLong(1, tenantDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(period.getStartDate()));
			ps.setDate(3, AEPersistentUtil.getSQLDate(period.getEndDate()));
			ps.setString(4, "6%");
			rs = ps.executeQuery();
			while(rs.next()) {
				JSONObject b = new JSONObject();
				
				// OWNER_ID
				// b.put(AEDomainObject.JSONKey.ownerId.name(), tenantDescr.getID());

				// ACCOUNT_ID
				b.put(AEDomainObject.JSONKey.id.name(), rs.getLong("account_id"));
				
				// YEAR
				// b.put("year", AEDateUtil.getYear(period.getStartDate()));
				
				// AMOUNT
				double dtAmount = rs.getDouble("debit_amount");
				// b.put("dtAmount", dtAmount);
				
				double ctAmount = rs.getDouble("credit_amount");
				// b.put("ctAmount", ctAmount);
				
				b.put("amount", dtAmount - ctAmount);
				
				bArr.put(b);
			}
			
			return bArr;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	/**
	 * 129 (Loss)
	 * The debtor account balance 12 represents a loss, expenses being greater than the incomes.
	 * 
	 * @param tenantDescr
	 * @param period
	 * @return
	 * @throws AEException
	 */
	private static String selectSQLProfit_Loss = 
//			"select acc.id as account_id, acc.code as account_code, acc.name as account_name, sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount "
//			+ " from GeneralJournal gj inner join Account acc on gj.ACCOUNT_ID = acc.ID "   
//			+ " where gj.OWNER_ID = ? and gj.ENTRY_DATE between ? and ? and acc.code like ? " 
//			+ " group by acc.ID, acc.CODE, acc.NAME ";
			"select balance.account_id, balance.account_code, balance.account_name, sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount  from ( " 
			+ "		select acc.id as account_id, acc.code as account_code, acc.name as account_name, sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount " 
			+ "		from GeneralJournal gj inner join Account acc on gj.ACCOUNT_ID = acc.ID " 
			+ "		where gj.OWNER_ID = ? and gj.ENTRY_DATE between ? and ? and acc.code like ? " 
			+ "		group by acc.ID, acc.CODE, acc.NAME "
			+ " "
			+ "		union all "
			+ " "
			+ "		select acc.id as account_id, acc.code as account_code, acc.name as account_name, sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount " 
			+ "		from AccountBalance ab inner join Account acc on ab.ACCOUNT_ID = acc.ID " 
			+ "		where ab.OWNER_ID = ? and ab.YEAR = ? and ab.TYPE = 0 and acc.code like ? " 
			+ "		group by acc.ID, acc.CODE, acc.NAME) as balance "
			+ "	group by balance.account_id, balance.account_code, balance.account_name ";


	public JSONArray loadLoss(AEDescriptor tenantDescr, AETimePeriod period) throws AEException {
		JSONArray bArr = new JSONArray();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLProfit_Loss);
			
			// select 129* accounts
			ps.setLong(1, tenantDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(period.getStartDate()));
			ps.setDate(3, AEPersistentUtil.getSQLDate(period.getEndDate()));
			ps.setString(4, "129%");
			
			ps.setLong(5, tenantDescr.getID());
			ps.setInt(6, AEDateUtil.getYear(period.getStartDate()));
			ps.setString(7, "129%");
			
			rs = ps.executeQuery();
			while(rs.next()) {
				JSONObject b = new JSONObject();

				// ACCOUNT_ID
				b.put(AEDomainObject.JSONKey.id.name(), rs.getLong("account_id"));
				
				// ACCOUNT_CODE
				b.put(AEDomainObject.JSONKey.code.name(), rs.getString("account_code"));
				
				// ACCOUNT_NAME
				b.put(AEDomainObject.JSONKey.name.name(), rs.getString("account_name"));
				
				// DEBIT
				double dtAmount = rs.getDouble("debit_amount");
				
				// CREDIT
				double ctAmount = rs.getDouble("credit_amount");
				
				// amount
				b.put("amount", dtAmount - ctAmount);
				
				bArr.put(b);
			}
			
			return bArr;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	/**
	 * 120 (Profit)
	 * 
	 * The credit balance of 12 represents a profit, incomes were higher than the expenses.
	 * 
	 * @param tenantDescr
	 * @param period
	 * @return
	 * @throws AEException
	 */
	public JSONArray loadProfit(AEDescriptor tenantDescr, AETimePeriod period) throws AEException {
		JSONArray bArr = new JSONArray();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLProfit_Loss);
			
			// select 120* accounts
			ps.setLong(1, tenantDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(period.getStartDate()));
			ps.setDate(3, AEPersistentUtil.getSQLDate(period.getEndDate()));
			ps.setString(4, "120%");
			
			ps.setLong(5, tenantDescr.getID());
			ps.setInt(6, AEDateUtil.getYear(period.getStartDate()));
			ps.setString(7, "120%");
			rs = ps.executeQuery();
			while(rs.next()) {
				JSONObject b = new JSONObject();
				
				// ACCOUNT_ID
				b.put(AEDomainObject.JSONKey.id.name(), rs.getLong("account_id"));
				
				// ACCOUNT_CODE
				b.put(AEDomainObject.JSONKey.code.name(), rs.getString("account_code"));
				
				// ACCOUNT_NAME
				b.put(AEDomainObject.JSONKey.name.name(), rs.getString("account_name"));
				
				// DEBIT
				double dtAmount = rs.getDouble("debit_amount");
				
				// CREDIT
				double ctAmount = rs.getDouble("credit_amount");
				
				b.put("amount", ctAmount - dtAmount);
				
				bArr.put(b);
			}
			
			return bArr;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String selectOpeningBalance = "select * from AccountBalance where ACCOUNT_ID = ? and YEAR = ? and TYPE = 0";
	public void loadOpeningBalance(AccAccountBalance accBalance) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectOpeningBalance);
			ps.setLong(1, accBalance.getAccAccount().getDescriptor().getID());
			ps.setInt(2, AEDateUtil.getYear(accBalance.getPeriod().getStartDate()));
			rs = ps.executeQuery();
			if(rs.next()) {
				double debitBalance = rs.getDouble("debit_amount");
				double creditBalance = rs.getDouble("credit_amount");
				accBalance.setOpeningBalance(debitBalance - creditBalance);
			}
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String selectSQLLastOpeningBalanceYear = 
			"select max(YEAR) as year from AccountBalance where ACCOUNT_ID = ? and year <= ? and TYPE = 0";
	public Integer loadLastOpeningBalanceYear(AEDescriptor accountDescr, Date date) throws AEException {
		Integer year = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLLastOpeningBalanceYear);
			ps.setLong(1, accountDescr.getID());
			ps.setInt(2, AEDateUtil.getYear(date));
			rs = ps.executeQuery();
			if(rs.next()) {
				year = rs.getInt("year");
			}
			return year;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void build(AccJournalEntry entry, ResultSet rs) throws SQLException {
		super.build(entry, rs);
	}
	
	private static String selectSQLQuetesSum = 
			"select quete_code, sum(DEBIT_AMOUNT) as dt,  sum(CREDIT_AMOUNT) as ct from GeneralJournal "
			+ " where OWNER_ID = ? and ENTRY_DATE between ? and ? and quete_code is not null "
			+ " group by QUETE_CODE"; 
	public QuetesList loadQuetesSum(AEDescriptor tenantDescr, AETimePeriod period) throws AEException {
		QuetesList qList = new QuetesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLQuetesSum);
			ps.setLong(1, tenantDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(period.getStartDate()));
			ps.setDate(3, AEPersistentUtil.getSQLDate(period.getEndDate()));
			rs = ps.executeQuery();
			while(rs.next()) {
				Quete q = new Quete();
								
				q.setCode(rs.getString("quete_code"));
				q.setAmount(rs.getDouble("ct"));
				
				qList.add(q);
			}
			return qList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String selectSQLQuetesGroupByAcc = 
			"select acc.code as code, acc.name as name , sum(DEBIT_AMOUNT) as dt, sum(CREDIT_AMOUNT) as ct from GeneralJournal gj "
			+ " inner join Account acc on gj.ACCOUNT_ID = acc.ID "
			+ " where gj.OWNER_ID = ? and ENTRY_DATE between ? and ? and quete_code is not null " 
			+ " group by code, name"; 
	public AccAccountBalancesList loadQuetesGroupByAcc(AEDescriptor tenantDescr, AETimePeriod period) throws AEException {
		AccAccountBalancesList accBList = new AccAccountBalancesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLQuetesGroupByAcc);
			ps.setLong(1, tenantDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(period.getStartDate()));
			ps.setDate(3, AEPersistentUtil.getSQLDate(period.getEndDate()));
			rs = ps.executeQuery();
			while(rs.next()) {
				AccAccountBalance netChange = new AccAccountBalance();
				netChange.setAccAccount(AccAccount.lazyDescriptor(AEPersistentUtil.NEW_ID)
						.withCode(rs.getString("code"))
						.withName(rs.getString("name")));
				netChange.setCreditTurnover(rs.getDouble("ct"));
				netChange.calculateFinalBalance();
				accBList.add(netChange);
			}
			return accBList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String calculateSQLTurnover = "" 
			+ "with turn_over (acc_code, debit_amount, credit_amount) "
			+ "as ( "
			+ "select acc.CODE as acc_code, sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount from GeneralJournal gj " 
			+ "  inner join Account acc on gj.ACCOUNT_ID = acc.ID " 
			+ "  where gj.OWNER_ID = ? and gj.ENTRY_DATE between ? and ? "
			+ "  group by acc.CODE "
			+ ") "
			+ " "	
			+ "select turn_over_calculated.*, account_codes.name as acc_name from ( "
			+ "	select turn_over.acc_code, 40 as acc_type, turn_over.debit_amount, turn_over.credit_amount from turn_over "
			+ "	union all "
			+ "	select left(turn_over.acc_code, 3) as acc_code, 30 as acc_type, sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount from turn_over "
			+ "	  group by left(turn_over.acc_code, 3) "
			+ "	union all "
			+ "	select left(turn_over.acc_code, 2) as acc_code, 20 as acc_type, sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount from turn_over "
			+ "	  group by left(turn_over.acc_code, 2) "
			+ "	union all "
			+ "	select left(turn_over.acc_code, 1) as acc_code, 10 as acc_type, sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount from turn_over "
			+ "	  group by left(turn_over.acc_code, 1) "
			+ ") as turn_over_calculated left outer join ( "
			+ "	select acc.CODE, acc.name from Account acc " 
			+ "	inner join ChartOfAccounts coa on acc.COA_ID = coa.ID and coa.OWNER_ID = ? "
			+ ") as account_codes on turn_over_calculated.acc_code = account_codes.code "
			+ "order by acc_code ";
	public AccAccountBalancesList loadTurnover(AEDescriptor tenantDescr, AETimePeriod period) throws AEException {
		AccAccountBalancesList abList = new AccAccountBalancesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(calculateSQLTurnover);
			ps.setLong(1, tenantDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(period.getStartDate())));
			ps.setDate(3, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(period.getEndDate())));
			ps.setLong(4, tenantDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				AccAccountBalance ab = new AccAccountBalance();
				
				// create from rs
				ab.setAccAccount(new AEDescriptorImp()
					.withCode(rs.getString("acc_code"))
					.withName(rs.getString("acc_name")));
				ab.setAccType(rs.getInt("acc_type"));
				ab.setDebitTurnover(rs.getDouble("debit_amount"));
				ab.setCreditTurnover(rs.getDouble("credit_amount"));

				// calculate
				ab.calculateFinalBalance();
				
				// set view
				ab.setView();
				
				// add to the list
				abList.add(ab);
			}
			return abList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String loadSQLBalanceSheetWithoutOpeningBalance =  
		" select balance.*, account_codes.name as acc_name, account_codes.code as acc_code from "
		+ "(select b_acc_code, SUM(DEBIT_BALANCE) as debit_balance, SUM(CREDIT_BALANCE) as credit_balance, SUM(DEBIT_AMOUNT) as debit_amount, SUM(CREDIT_AMOUNT) as credit_amount " 
		+ "	from ( " 
		+ "		select acc.CODE as b_acc_code, 0.0 as debit_balance, 0.0 as credit_balance, sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount " 
		+ "		from GeneralJournal gj inner join Account acc on gj.ACCOUNT_ID = acc.ID "  
		+ "		where gj.OWNER_ID = ? and gj.ENTRY_DATE between ? and ? " 
		+ "		group by acc.CODE) as raw_data " 
		+ "	group by b_acc_code " 
		+ ") as balance right outer join " 
		+ "(select acc.CODE, acc.name from Account acc " 
		+ "	inner join ChartOfAccounts coa on acc.COA_ID = coa.ID and coa.OWNER_ID = ? "
		+ ") as account_codes on balance.b_acc_code = account_codes.code ";
	
	private static String loadSQLBalanceSheetWithOpeningBalance =  
		" select balance.*, account_codes.name as acc_name, account_codes.code as acc_code from "
		+ "(select b_acc_code, SUM(DEBIT_BALANCE) as debit_balance, SUM(CREDIT_BALANCE) as credit_balance, SUM(DEBIT_AMOUNT) as debit_amount, SUM(CREDIT_AMOUNT) as credit_amount " 
		+ " 	from ( " 
		+ " 	  select acc.CODE as b_acc_code, debit_amount as debit_balance, credit_amount as credit_balance, 0.0 as debit_amount, 0.0 as credit_amount " 
		+ " 	    from AccountBalance ab inner join Account acc on ab.ACCOUNT_ID = acc.ID " 
		+ " 	    where ab.OWNER_ID = ? and ab.YEAR = ? and ab.TYPE = 0 " 
		+ " 	union all " 
		+ " 	  select acc.CODE as b_acc_code, 0.0 as debit_balance, 0.0 as credit_balance, sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount " 
		+ " 	    from GeneralJournal gj inner join Account acc on gj.ACCOUNT_ID = acc.ID "  
		+ " 	    where gj.OWNER_ID = ? and gj.ENTRY_DATE between ? and ? " 
		+ " 	    group by acc.CODE) as raw_data " 
		+ " 	group by b_acc_code " 
		+ " ) as balance right outer join " 
		+ " (select acc.CODE, acc.name from Account acc " 
		+ " 	inner join ChartOfAccounts coa on acc.COA_ID = coa.ID and coa.OWNER_ID = ? "
		+ " ) as account_codes on balance.b_acc_code = account_codes.code ";

	public Map<String, AccAccountBalanceExt> loadBalanceSheet(AEDescriptor tenantDescr, AETimePeriod period, Integer fromAccCodeInt, Integer toAccCodeInt) throws AEException {
		Map<String, AccAccountBalanceExt> balance = new HashMap<String, AccAccountBalanceExt>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			Date fromDate = period.getStartDate();
			Date firstDayOfYear = AEDateUtil.getFirstDate(0, AEDateUtil.getYear(fromDate));
			boolean withOpeningBalance = fromDate.equals(firstDayOfYear);
			String sql = withOpeningBalance ?
					loadSQLBalanceSheetWithOpeningBalance :
					loadSQLBalanceSheetWithoutOpeningBalance;
			if(fromAccCodeInt != null || toAccCodeInt != null) {
				sql += " where ";
			}
			if(fromAccCodeInt != null) {
				sql += " CONVERT(int, balance.b_acc_code)  >=  " + fromAccCodeInt + " ";
			}
			if(toAccCodeInt != null) {
				if(fromAccCodeInt != null) {
					sql += " and ";
				}
				sql += " CONVERT(int, balance.b_acc_code)  <=  " + toAccCodeInt + " ";
			}
			if(withOpeningBalance) {
				ps = getAEConnection().prepareStatement(sql);
				ps.setLong(1, tenantDescr.getID());
				ps.setInt(2, AEDateUtil.getYear(period.getStartDate()));
				ps.setLong(3, tenantDescr.getID());
				ps.setDate(4, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(period.getStartDate())));
				ps.setDate(5, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(period.getEndDate())));
				ps.setLong(6, tenantDescr.getID());
				rs = ps.executeQuery();
			} else {
				ps = getAEConnection().prepareStatement(sql);
				ps.setLong(1, tenantDescr.getID());
				ps.setDate(2, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(period.getStartDate())));
				ps.setDate(3, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(period.getEndDate())));
				ps.setLong(4, tenantDescr.getID());
				rs = ps.executeQuery();
			}
			while(rs.next()) {
				AccAccountBalanceExt ab = new AccAccountBalanceExt();
				
				// create from rs
				ab.setAccAccount(new AEDescriptorImp()
					.withCode(rs.getString("acc_code"))
					.withName(rs.getString("acc_name")));
				ab.setDebitTurnover(rs.getDouble("debit_amount"));
				ab.setCreditTurnover(rs.getDouble("credit_amount"));
				ab.setDebitOpeningBalance(rs.getDouble("debit_balance"));
				ab.setCreditOpeningBalance(rs.getDouble("credit_balance"));

				// calculate
				ab.calculateFinalBalance();
				
				// set view
				ab.setView();
				
				// add to the list
				balance.put(ab.getAccAccount().getDescriptor().getCode(), ab);
			}
			return balance;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String loadSQLBalanceSheetExtWithoutOpeningBalance = "" 
			+ "with turn_over (acc_code, debit_amount, credit_amount) "
			+ "as ( "
			+ "select acc.CODE as acc_code, sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount from GeneralJournal gj " 
			+ "  inner join Account acc on gj.ACCOUNT_ID = acc.ID " 
			+ "  where gj.OWNER_ID = ? and gj.ENTRY_DATE between ? and ? "
			+ "  group by acc.CODE "
			+ ") "
			+ " "	
			+ "select turn_over_calculated.*, account_codes.name as acc_name from ( "
			+ "	select turn_over.acc_code, 40 as acc_type, turn_over.debit_amount, turn_over.credit_amount from turn_over "
			+ "	union all "
			+ "	select left(turn_over.acc_code, 3) as acc_code, 30 as acc_type, sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount from turn_over "
			+ "	  group by left(turn_over.acc_code, 3) "
			+ "	union all "
			+ "	select left(turn_over.acc_code, 2) as acc_code, 20 as acc_type, sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount from turn_over "
			+ "	  group by left(turn_over.acc_code, 2) "
			+ "	union all "
			+ "	select left(turn_over.acc_code, 1) as acc_code, 10 as acc_type, sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount from turn_over "
			+ "	  group by left(turn_over.acc_code, 1) "
			+ ") as turn_over_calculated left outer join ( "
			+ "	select acc.CODE, acc.name from Account acc " 
			+ "	inner join ChartOfAccounts coa on acc.COA_ID = coa.ID and coa.OWNER_ID = ? "
			+ ") as account_codes on turn_over_calculated.acc_code = account_codes.code "
			+ "order by acc_code ";
	
	private static String loadSQLBalanceSheetExtWithOpeningBalance = ""
			+ " with turn_over (acc_code, debit_amount, credit_amount) "
			+ " as (  "
			+ " select acc.CODE as acc_code, sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount from GeneralJournal gj " 
			+ "   inner join Account acc on gj.ACCOUNT_ID = acc.ID " 
			+ "   where not acc.code like '120%' and not acc.code like '129%' and gj.OWNER_ID = ? and gj.ENTRY_DATE between ? and ? " 
			+ "   group by acc.CODE " 
			+ " ), "
			+ " balance (acc_code, debit_amount, credit_amount) " 
			+ " as ( " 
			+ " select acc.CODE as acc_code, sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount "
			+ "  from AccountBalance ab inner join Account acc on ab.ACCOUNT_ID = acc.ID "
			+ "  where not acc.code like '120%' and not acc.code like '129%' and ab.OWNER_ID = ? and ab.YEAR = ? and ab.TYPE = 0 "
			+ "  group by acc.CODE " 
			+ " ) "
			+ " " 
			+ " select balance_sheet.*, account_codes.name as acc_name from ( " 
			+ "   select acc_code, acc_type, SUM(debit_amount) as debit_amount, SUM(credit_amount) as credit_amount from ( " 
			+ "		select turn_over.acc_code, 40 as acc_type, turn_over.debit_amount, turn_over.credit_amount from turn_over " 
			+ "		union all " 
			+ "		select left(turn_over.acc_code, 3) as acc_code, 30 as acc_type, sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount from turn_over " 
			+ "		  group by left(turn_over.acc_code, 3) " 
			+ "		union all " 
			+ "		select left(turn_over.acc_code, 2) as acc_code, 20 as acc_type, sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount from turn_over " 
			+ "		  group by left(turn_over.acc_code, 2) " 
			+ "		union all " 
			+ "		select left(turn_over.acc_code, 1) as acc_code, 10 as acc_type, sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount from turn_over " 
			+ "		  group by left(turn_over.acc_code, 1) " 
			+ " "	
			+ "		union all " 
			+ " "	
			+ "		select balance.acc_code, 40 as acc_type, balance.debit_amount, balance.credit_amount from balance " 
			+ "		union all " 
			+ "		select left(balance.acc_code, 3) as acc_code, 30 as acc_type, sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount from balance " 
			+ "		  group by left(balance.acc_code, 3) " 
			+ "		union all " 
			+ "		select left(balance.acc_code, 2) as acc_code, 20 as acc_type, sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount from balance " 
			+ "		  group by left(balance.acc_code, 2) " 
			+ "		union all " 
			+ "		select left(balance.acc_code, 1) as acc_code, 10 as acc_type, sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount from balance " 
			+ "		  group by left(balance.acc_code, 1) " 
			+ "	  ) as balance_sheet_raw group by acc_type, acc_code "
			+ " ) as balance_sheet left outer join ( "
			+ " select acc.CODE, acc.name from Account acc "
			+ " inner join ChartOfAccounts coa on acc.COA_ID = coa.ID and coa.OWNER_ID = ? "
			+ " ) as account_codes on balance_sheet.acc_code = account_codes.code "
			+ " order by acc_code ";
	public AccAccountBalancesList loadBilanSheet(AEDescriptor tenantDescr, AETimePeriod period) throws AEException {
		AccAccountBalancesList abList = new AccAccountBalancesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			Date fromDate = period.getStartDate();
			Date firstDayOfYear = AEDateUtil.getFirstDate(0, AEDateUtil.getYear(fromDate));
			boolean withOpeningBalance = fromDate.equals(firstDayOfYear);
			String sql = withOpeningBalance ?
					loadSQLBalanceSheetExtWithOpeningBalance :
					loadSQLBalanceSheetExtWithoutOpeningBalance;
			if(withOpeningBalance) {
				ps = getAEConnection().prepareStatement(sql);
				ps.setLong(1, tenantDescr.getID());
				ps.setDate(2, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(period.getStartDate())));
				ps.setDate(3, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(period.getEndDate())));
				ps.setLong(4, tenantDescr.getID());
				ps.setLong(5, AEDateUtil.getYear(period.getStartDate()));
				ps.setLong(6, tenantDescr.getID());
				rs = ps.executeQuery();
			} else {
				ps = getAEConnection().prepareStatement(sql);
				ps.setLong(1, tenantDescr.getID());
				ps.setDate(2, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(period.getStartDate())));
				ps.setDate(3, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(period.getEndDate())));
				ps.setLong(4, tenantDescr.getID());
				rs = ps.executeQuery();
			}
			while(rs.next()) {
				AccAccountBalance ab = new AccAccountBalance();
				
				// create from rs
				ab.setAccAccount(new AEDescriptorImp()
					.withCode(rs.getString("acc_code"))
					.withName(rs.getString("acc_name")));
				ab.setAccType(rs.getInt("acc_type"));
				ab.setDebitTurnover(rs.getDouble("debit_amount"));
				ab.setCreditTurnover(rs.getDouble("credit_amount"));

				// calculate
				ab.calculateFinalBalance();
				
				// set view
				ab.setView();
				
				// add to the list
				abList.add(ab);
			}
			return abList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String loadSQLClosingBalance = ""
			+ " with turn_over (acc_id, debit_amount, credit_amount) "
			+ " as (  "
			+ " select acc.id as acc_id, sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount from GeneralJournal gj " 
			+ "   inner join Account acc on gj.ACCOUNT_ID = acc.ID " 
			+ "   where gj.OWNER_ID = ? and gj.ENTRY_DATE between ? and ? " 
			+ "   group by acc.id " 
			+ " ), "
			+ " balance (acc_id, debit_amount, credit_amount) " 
			+ " as ( " 
			+ " select acc.id as acc_id, sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount "
			+ "  from AccountBalance ab inner join Account acc on ab.ACCOUNT_ID = acc.ID "
			+ "  where ab.OWNER_ID = ? and ab.YEAR = ? and ab.TYPE = 0 "
			+ "  group by acc.id " 
			+ " ) "
			+ " " 
			+ " select balance_sheet.*, account_codes.code as acc_code, account_codes.name as acc_name from ( " 
			+ "   select acc_id, acc_type, SUM(debit_amount) as debit_amount, SUM(credit_amount) as credit_amount from ( " 
			+ "		select turn_over.acc_id, 40 as acc_type, turn_over.debit_amount, turn_over.credit_amount from turn_over " 
			+ " "	
			+ "		union all " 
			+ " "	
			+ "		select balance.acc_id, 40 as acc_type, balance.debit_amount, balance.credit_amount from balance " 
			+ "	  ) as balance_sheet_raw group by acc_type, acc_id "
			+ " ) as balance_sheet left outer join ( "
			+ " select acc.id, acc.code, acc.name from Account acc "
			+ " inner join ChartOfAccounts coa on acc.COA_ID = coa.ID and coa.OWNER_ID = ? "
			+ " ) as account_codes on balance_sheet.acc_id = account_codes.id "
			+ " order by acc_id ";
	public AccAccountBalancesList calcClosingBalances(AEDescriptor tenantDescr, AETimePeriod period) throws AEException {
		AccAccountBalancesList abList = new AccAccountBalancesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(loadSQLClosingBalance);
			
			ps.setLong(1, tenantDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(period.getStartDate())));
			ps.setDate(3, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(period.getEndDate())));
			ps.setLong(4, tenantDescr.getID());
			ps.setLong(5, AEDateUtil.getYear(period.getStartDate()));
			ps.setLong(6, tenantDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				AccAccountBalance ab = new AccAccountBalance();

				// create from rs
				ab.setAccAccount(new AEDescriptorImp()
					.withId(rs.getLong("acc_id"))
					.withCode(rs.getString("acc_code"))
					.withName(rs.getString("acc_name")));
				ab.setAccType(rs.getInt("acc_type"));
				ab.setDebitTurnover(rs.getDouble("debit_amount"));
				ab.setCreditTurnover(rs.getDouble("credit_amount"));

				// calculate
				ab.calculateFinalBalance();

				// set view
				ab.setView();

				// add to the list balances from 1* to 5*
				if(ab.getAccAccount().getDescriptor() != null && ab.getAccAccount().getDescriptor().getCode() != null 
						&& (ab.getAccAccount().getDescriptor().getCode().startsWith("1")
								|| ab.getAccAccount().getDescriptor().getCode().startsWith("2")
								|| ab.getAccAccount().getDescriptor().getCode().startsWith("3")
								|| ab.getAccAccount().getDescriptor().getCode().startsWith("4")
								|| ab.getAccAccount().getDescriptor().getCode().startsWith("5"))) {
					
					
					ab.transformToOpeningBalance();
					abList.add(ab);
				}
			}
			return abList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	// skip address
	private static String selectSQLJournalEntriesReport = ""
			+ "SELECT gjItem.*, "
			+ "	acc.CODE as ACCOUNT_CODE, acc.NAME as ACCOUNT_NAME, "
			+ "	contr.ID as CONTR_ID, contr.EMPLOYEE_ID as CONTR_EMPL_ID, "  
			+ " suppl.id as suppl_id, suppl.name as suppl_name, "
			+ "	empl.FirstName as empl_firstname, empl.LastName as empl_lastname, empl.NAME as empl_name "  
//			+ "	addr.STREET as addr_street, addr.POSTAL_CODE as addr_postal_code, addr.CITY as addr_city "  
			+ "from "
			+ "	GeneralJournal gjItem " 
			+ "	inner join Account acc on gjItem.ACCOUNT_ID = acc.ID "  
			+ "	left outer join Contributor contr on gjItem.CONTRIBUTOR_ID = contr.ID "  
			+ "	left outer join SimpleParty suppl on gjItem.SUPPLIER_ID = suppl.ID "  
			+ "	left outer join Employee empl on contr.EMPLOYEE_ID = empl.ID "  
//			+ "	left outer join Address addr on empl.ID = addr.TO_OBJ_ID and addr.TO_CLASS_ID = " + DomainClass.EMPLOYEE.getID() + "  and addr.TYPE_ID = " + Address.Type.BUSINESS.getID() + " "
			+ "where gjItem.OWNER_ID = ? and gjItem.ENTRY_DATE between ? and ? and gjItem.JOURNAL_CODE = ? "
			+ " order by gjItem.ENTRY_DATE asc, gjItem.ID asc ";
	private static String selectSQLJournalEntriesReportOnlyPeriod = ""
			+ "SELECT gjItem.*, "
			+ "	acc.CODE as ACCOUNT_CODE, acc.NAME as ACCOUNT_NAME, "
			+ "	contr.ID as CONTR_ID, contr.EMPLOYEE_ID as CONTR_EMPL_ID, "  
			+ " suppl.id as suppl_id, suppl.name as suppl_name, "
			+ "	empl.FirstName as empl_firstname, empl.LastName as empl_lastname, empl.NAME as empl_name "  
//			+ "	addr.STREET as addr_street, addr.POSTAL_CODE as addr_postal_code, addr.CITY as addr_city "  
			+ "from "
			+ "	GeneralJournal gjItem " 
			+ "	inner join Account acc on gjItem.ACCOUNT_ID = acc.ID "  
			+ "	left outer join Contributor contr on gjItem.CONTRIBUTOR_ID = contr.ID "  
			+ "	left outer join SimpleParty suppl on gjItem.SUPPLIER_ID = suppl.ID "  
			+ "	left outer join Employee empl on contr.EMPLOYEE_ID = empl.ID "  
//			+ "	left outer join Address addr on empl.ID = addr.TO_OBJ_ID and addr.TO_CLASS_ID = " + DomainClass.EMPLOYEE.getID() + "  and addr.TYPE_ID = " + Address.Type.BUSINESS.getID() + " "
			+ "where gjItem.OWNER_ID = ? and gjItem.ENTRY_DATE between ? and ? "
			+ " order by gjItem.ENTRY_DATE asc, gjItem.ID asc ";
	private static String selectSQLJournalEntriesFromBalance = 
			"select balance.*, acc.CODE as ACCOUNT_CODE, acc.NAME as ACCOUNT_NAME from AccountBalance balance "
			+ " inner join Account acc on balance.ACCOUNT_ID = acc.ID "
			+ " where balance.OWNER_ID = ? and TYPE = 0 and DATEFROMPARTS(year, 1, 1) between ? and ? "
			+ " order by acc.CODE asc ";
	/**
	 * IMPORTANT: keep selectSQLJournalEntriesReport and selectSQLJournalEntriesReportOnlyPeriod synchronized
	 * 
	 * @param tenantDescr
	 * @param period
	 * @param accJournalDescr
	 * @return
	 * @throws AEException
	 */
	public AccJournalResultsList load(AEDescriptor tenantDescr, AETimePeriod period, AEDescriptor accJournalDescr) throws AEException {
		AccJournalResultsList results = new AccJournalResultsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			boolean onlyPeriod = AEStringUtil.isEmpty(accJournalDescr.getCode());
			
			/**
			 * Balances first
			 */
			if(onlyPeriod || PaymentMethod.NOUVEAU.getCode().equalsIgnoreCase(accJournalDescr.getCode())) {
				// load balances
				ps = getAEConnection().prepareStatement(selectSQLJournalEntriesFromBalance);
				ps.setLong(1, tenantDescr.getID());
				ps.setDate(2,AEPersistentUtil.getSQLDate(period.getStartDate()));
				ps.setDate(3,AEPersistentUtil.getSQLDate(period.getEndDate()));
				rs = ps.executeQuery();
				while(rs.next()) {
					AccJournalResult res = new AccJournalResult();
					buildOpeningBalance(res, rs);

					// check dt and ct amounts
					if(!AEMath.isZeroAmount(res.getAccJournalItem().getDtAmount()) || !AEMath.isZeroAmount(res.getAccJournalItem().getCtAmount())) {
						res.setView();
						res.getAccJournalItem().setView();
						
						// add to the result collection
						results.add(res);
					}
				}
				
				// close before next usage
				AEConnection.close(rs);
				AEConnection.close(ps);
			}
			
			/**
			 * Create statement 
			 */
			if(onlyPeriod) {
				ps = getAEConnection().prepareStatement(selectSQLJournalEntriesReportOnlyPeriod);
			} else {
				ps = getAEConnection().prepareStatement(selectSQLJournalEntriesReport);
			}

			/**
			 * Append system filter
			 */
			ps.setLong(1, tenantDescr.getID());
			ps.setDate(2,AEPersistentUtil.getSQLDate(period.getStartDate()));
			ps.setDate(3,AEPersistentUtil.getSQLDate(period.getEndDate()));
			if(!onlyPeriod) {
				ps.setString(4, accJournalDescr.getCode());
			}
			rs = ps.executeQuery();
			while(rs.next()) {
				AccJournalResult res = new AccJournalResult();
				res.setLoadLazzy(true);
				build(res, rs);
				
				res.setView();
				if(res.getAccJournalItem() != null) {
					res.getAccJournalItem().setView();
				}
				
				results.add(res);
			}
			return results;
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	// skip address
	private static String selectSQLGrandLivre = ""
			+ "SELECT gjItem.*, "
			+ "	acc.CODE as ACCOUNT_CODE, acc.NAME as ACCOUNT_NAME, "
			+ "	contr.ID as CONTR_ID, contr.EMPLOYEE_ID as CONTR_EMPL_ID, "  
			+ " suppl.id as suppl_id, suppl.name as suppl_name, "
			+ "	empl.FirstName as empl_firstname, empl.LastName as empl_lastname, empl.NAME as empl_name "    
			+ "from "
			+ "	GeneralJournal gjItem " 
			+ "	inner join Account acc on gjItem.ACCOUNT_ID = acc.ID "  
			+ "	left outer join Contributor contr on gjItem.CONTRIBUTOR_ID = contr.ID "  
			+ "	left outer join SimpleParty suppl on gjItem.SUPPLIER_ID = suppl.ID "  
			+ "	left outer join Employee empl on contr.EMPLOYEE_ID = empl.ID "  
			+ "where gjItem.OWNER_ID = ? and gjItem.ENTRY_DATE between ? and ? and acc.ACC_TYPE = " + AccAccount.AccountType.ACCOUNT.getTypeId() + " ";
	private static String selectSQLGrandLivreInitialBalance = 
			"select ab.*, acc.ID as ACCOUNT_ID, acc.CODE as ACCOUNT_CODE, acc.NAME as ACCOUNT_NAME "
			+ "  from AccountBalance ab  "
			+ "  inner join Account acc on ab.ACCOUNT_ID = acc.ID " 
			+ "  where ab.OWNER_ID = ? and YEAR = ? and TYPE = 0 and acc.ACC_TYPE = " + AccAccount.AccountType.ACCOUNT.getTypeId() + " ";
	private static String selectSQLGrandLivreCumulatedTurnover = ""
		+ "	with turn_over (acc_id, debit_amount, credit_amount) "
		+ "		 as (  "
		+ "		 select acc.id as acc_id, sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount from GeneralJournal gj " 
		+ "		  inner join Account acc on gj.ACCOUNT_ID = acc.ID " 
		+ "		   where gj.OWNER_ID = ? and gj.ENTRY_DATE between ? and ? and acc.ACC_TYPE = " + AccAccount.AccountType.ACCOUNT.getTypeId() + " " 
		+ "		   group by acc.id " 
		+ "		) "
		+ "	 "
		+ "	select turn_over.*, acc.code as acc_code, acc.name as acc_name " 
		+ "	  from turn_over inner join Account acc on turn_over.acc_id = acc.id "
		+ " where acc.ACC_TYPE = " + AccAccount.AccountType.ACCOUNT.getTypeId() + " ";
	public void loadGrandLivre(GrandLivreDataSource dataSource, AEDescriptor tenantDescr, AETimePeriod period, Long fromAccountCode, Long toAccountCode) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			/**
			 * Initial balance
			 */
			Map<String, AccJournalResult> initialBalance = dataSource.getInitialBalance();
			String sql = selectSQLGrandLivreInitialBalance;
			if(fromAccountCode != null || toAccountCode != null) {
				sql += " and ";
			}
			if(fromAccountCode != null) {
				sql += " CONVERT(bigint, acc.CODE)  >=  " + fromAccountCode + " ";
			}
			if(toAccountCode != null) {
				if(fromAccountCode != null) {
					sql += " and ";
				}
				sql += " CONVERT(bigint, acc.CODE)  <=  " + toAccountCode + " ";
			}
			ps = getAEConnection().prepareStatement(sql);
			ps.setLong(1, tenantDescr.getID());
			ps.setInt(2, AEDateUtil.getYear(period.getStartDate()));
			rs = ps.executeQuery();
			while(rs.next()) {
				AccJournalResult res = new AccJournalResult();

				// build
				res.setID(rs.getLong("ID"));
				AccJournalItem accJournalItem = new AccJournalItem();
				AEDescriptorImp accDescr = new AEDescriptorImp(rs.getLong("ACCOUNT_ID"));
				accDescr.setCode(rs.getString("ACCOUNT_CODE"));
				accDescr.setName(rs.getString("ACCOUNT_NAME"));
				accJournalItem.setAccount(accDescr);
				accJournalItem.setDtAmount(rs.getDouble("debit_amount"));
				accJournalItem.setCtAmount(rs.getDouble("credit_amount"));
				res.setAccJournalItem(accJournalItem);
				
				// key
				String key = res.getAccJournalItem().getAccount().getCode() + " - " + res.getAccJournalItem().getAccount().getName();
				initialBalance.put(key, res);
			}
			AEConnection.close(rs);
			AEConnection.close(ps);
			
			/**
			 * Cumulated turnover before period
			 */
			Map<String, AccJournalResult> turnoverBeforePeriod = dataSource.getAccumulatedTurnoverBefore();
			Date beginOfTheYear = AEDateUtil.beginOfTheYear(period.getStartDate());
			Date beforePeriod = AEDateUtil.addDaysToDate(period.getStartDate(), -1);
			
			sql = selectSQLGrandLivreCumulatedTurnover;
//			if(fromAccountCode != null || toAccountCode != null) {
//				sql += " and ";
//			}
			if(fromAccountCode != null) {
				sql += " and CONVERT(bigint, acc.CODE)  >=  " + fromAccountCode + " ";
			}
			if(toAccountCode != null) {
//				if(fromAccountCode != null) {
//					sql += " and ";
//				}
				sql += " and CONVERT(bigint, acc.CODE)  <=  " + toAccountCode + " ";
			}
			ps = getAEConnection().prepareStatement(sql);
			ps.setLong(1, tenantDescr.getID());
			ps.setDate(2,AEPersistentUtil.getSQLDate(beginOfTheYear));
			ps.setDate(3,AEPersistentUtil.getSQLDate(beforePeriod));
			rs = ps.executeQuery();
			while(rs.next()) {
				AccJournalResult res = new AccJournalResult();

				// build
				AccJournalItem accJournalItem = new AccJournalItem();
				AEDescriptorImp accDescr = new AEDescriptorImp(rs.getLong("acc_id"));
				accDescr.setCode(rs.getString("acc_code"));
				accDescr.setName(rs.getString("acc_name"));
				accJournalItem.setAccount(accDescr);
				accJournalItem.setDtAmount(rs.getDouble("debit_amount"));
				accJournalItem.setCtAmount(rs.getDouble("credit_amount"));
				res.setAccJournalItem(accJournalItem);
				
				// key
				String key = res.getAccJournalItem().getAccount().getCode() + " - " + res.getAccJournalItem().getAccount().getName();
				turnoverBeforePeriod.put(key, res);
			}
			AEConnection.close(rs);
			AEConnection.close(ps);
			
			/**
			 * Turnover in period
			 */
			Map<String, AccJournalResultsList> grandLivre = dataSource.getGrandLivre();
			sql = selectSQLGrandLivre;
			if(fromAccountCode != null || toAccountCode != null) {
				sql += " and ";
			}
			if(fromAccountCode != null) {
				sql += " CONVERT(bigint, acc.CODE)  >=  " + fromAccountCode + " ";
			}
			if(toAccountCode != null) {
				if(fromAccountCode != null) {
					sql += " and ";
				}
				sql += " CONVERT(bigint, acc.CODE)  <=  " + toAccountCode + " ";
			}
			sql += " order by gjItem.ENTRY_DATE asc, gjItem.ID asc ";
			ps = getAEConnection().prepareStatement(sql);
			ps.setLong(1, tenantDescr.getID());
			ps.setDate(2,AEPersistentUtil.getSQLDate(period.getStartDate()));
			ps.setDate(3,AEPersistentUtil.getSQLDate(period.getEndDate()));
			rs = ps.executeQuery();
			while(rs.next()) {
				AccJournalResult res = new AccJournalResult();
				res.setLoadLazzy(true);
				build(res, rs);
				
				res.setView();
				if(res.getAccJournalItem() != null) {
					res.getAccJournalItem().setView();
				}
				
				String key = res.getAccJournalItem().getAccount().getCode() + " - " + res.getAccJournalItem().getAccount().getName();
				AccJournalResultsList results = grandLivre.get(key);
				if(results == null) {
					results = new AccJournalResultsList();
					grandLivre.put(key, results);
				}
				results.add(res);
			}
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	
	private static String selectSQLLastDate = 
			"select max(ENTRY_DATE) as entry_date from GeneralJournal where OWNER_ID = ?";
	
	public Date loadLastDate(AEDescriptor tenantDescr) throws AEException {
		Date lastDate = new Date();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLLastDate);
			ps.setLong(1, tenantDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				lastDate = rs.getDate("entry_date");
			}
			return AEDateUtil.getClearDateTime(lastDate);
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String selectSQLValidateByTenant = 
			"select acc.id, acc.code from Account acc inner join ChartOfAccounts coa on acc.COA_ID = coa.ID where coa.OWNER_ID = ? and acc.ID = ?";
	/**
	 * Use this validation only from free journal entries (not from template)
	 * 
	 * @param accJournalItems
	 * @param tenantDescr
	 * @throws AEException
	 */
	@SuppressWarnings("resource")
	public void validateAccounts(AccJournalItemsList accJournalItems, AEDescriptor tenantDescr) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			for (Iterator<AccJournalItem> iterator = accJournalItems.iterator(); iterator.hasNext();) {
				AccJournalItem accJournalItem = (AccJournalItem) iterator.next();
				ps = getAEConnection().prepareStatement(selectSQLValidateByTenant);
				if(accJournalItem.getAccount() != null) {
					ps.setLong(1, tenantDescr.getID());
					ps.setLong(2, accJournalItem.getAccount().getID());
					rs = ps.executeQuery();
					if(!rs.next()) {
						accJournalItem.setAccount(null);
					} else {
						// In addition, the use from donor table (field) must be limited to 704X, 77XX and 7018 accounts.
						String accCode = AEStringUtil.trim(rs.getString("code"));
						if(accJournalItem.getContributor() != null && !(accCode.startsWith("704") || accCode.startsWith("77") || accCode.startsWith("7018"))) {
							throw new AEException("Vous ne pouvez pas saisir de cordonnées de donateurs, hors les comptes de classe 704, 77, ou du compte 7018. ");
						}
					}
					AEConnection.close(rs);
				}
			}
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	///////
	private static String selectSQLId = 
			"select id from GeneralJournalSequence where OWNER_ID = ? and YEAR = ?";
	private static String updateSQLCurrentValue = 
			"update GeneralJournalSequence set CURRENT_VALUE = CURRENT_VALUE + 1 where id = ?";
	private static String selectSQLCurrentValue = 
			"select CURRENT_VALUE from GeneralJournalSequence where id = ?";
	private static int YEAR = 2015;
	private long nextValue(AEDescriptor tenantDescr, int year, String journalCode) throws AEException {
		long nextValue = AEPersistentUtil.NEW_ID;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			long id = AEPersistentUtil.NEW_ID;
			getAEConnection().beginTransaction();

			// select or insert
			ps = getAEConnection().prepareStatement(selectSQLId);
			ps.setLong(1, tenantDescr.getID());
			ps.setInt(2, YEAR);
			rs = ps.executeQuery();
			if(rs.next()) {
				id = rs.getLong("id");
			} else {
				id = insert(tenantDescr, YEAR, AEStringUtil.EMPTY_STRING);
			}
			AEConnection.close(rs);
			AEConnection.close(ps);
			
			// update current value
			ps = getAEConnection().prepareStatement(updateSQLCurrentValue);
			ps.setLong(1, id);
			ps.executeUpdate();
			AEConnection.close(rs);
			AEConnection.close(ps);
			
			// select current value
			ps = getAEConnection().prepareStatement(selectSQLCurrentValue);
			ps.setLong(1, id);
			rs = ps.executeQuery();
			if(rs.next()) {
				nextValue = rs.getLong("CURRENT_VALUE");
			}
			AEConnection.close(rs);
			AEConnection.close(ps);
			
			getAEConnection().commit();
			return nextValue;
		} catch (SQLException e) {
			getAEConnection().rollback();
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}		
	}
	
//	private static String updateSQLReturnValue = 
//			"update GeneralJournalSequence set CURRENT_VALUE = CURRENT_VALUE - 1 where id = ?";
//	public long returnValue(AEDescriptor tenantDescr, int year) throws AEException {
//		long previousValue = AEPersistentUtil.NEW_ID;
//		PreparedStatement ps = null;
//		ResultSet rs = null;
//		try {
//			long id = AEPersistentUtil.NEW_ID;
//			getAEConnection().beginTransaction();
//
//			// select or insert
//			ps = getAEConnection().prepareStatement(selectSQLId);
//			ps.setLong(1, tenantDescr.getID());
//			ps.setInt(2, year);
//			rs = ps.executeQuery();
//			if(rs.next()) {
//				id = rs.getLong("id");
//				
//				// release
//				AEConnection.close(rs);
//				AEConnection.close(ps);
//				
//				// return current value
//				ps = getAEConnection().prepareStatement(updateSQLReturnValue);
//				ps.setLong(1, id);
//				ps.executeUpdate();
//				AEConnection.close(rs);
//				AEConnection.close(ps);
//				
//				// select current value
//				ps = getAEConnection().prepareStatement(selectSQLCurrentValue);
//				ps.setLong(1, id);
//				rs = ps.executeQuery();
//				if(rs.next()) {
//					previousValue = rs.getLong("CURRENT_VALUE");
//				}
//				AEConnection.close(rs);
//				AEConnection.close(ps);
//			}
//			
//			getAEConnection().commit();
//			return previousValue;
//		} catch (SQLException e) {
//			getAEConnection().rollback();
//			throw new AEException(e);
//		} finally {
//			AEConnection.close(rs);
//			AEConnection.close(ps);
//			close();
//		}		
//	}
	
	private static String insertSQLSequence = 
			"insert into GeneralJournalSequence (OWNER_ID, YEAR, JOURNAL, CURRENT_VALUE) values (?, ?, ?, ?)";
	private long insert(AEDescriptor tenantDescr, int year, String journalCode) throws AEException {
		long id = AEPersistentUtil.NEW_ID;
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			long initialValue = 0L; //selectInitialValue(tenantDescr, year, journalCode);
			
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLSequence);
			ps.setLong(1, tenantDescr.getID());
			ps.setInt(2, year);
			ps.setString(3, journalCode);
			ps.setLong(4, initialValue);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				id = rs.getLong(1);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			return id;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	
//	private static String updateSQLTally = 
//			"update GeneralJournal "
//			+ " set TALLY = ? "
//			+ " where ID = ? and OWNER_ID = ?";
	private static String selectSQLEntryIdByItemId = "select ENTRY_ID from GeneralJournal where id = ? and OWNER_ID = ?";
	private static String updateSQLTally = "update GeneralJournal set TALLY = ? where ENTRY_ID = ?";
	public void updateTally(boolean tally, long accJournalItemId, AEDescriptor ownerDescr) throws AEException {
		ResultSet rs = null;
		PreparedStatement ps = null;
		try {
			Long entryId = null;
			ps = getAEConnection().prepareStatement(selectSQLEntryIdByItemId);
			ps.setLong(1, accJournalItemId);
			ps.setLong(2, ownerDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				entryId = rs.getLong("ENTRY_ID");
			}
			AEConnection.close(rs);
			AEConnection.close(ps);

			if(entryId != null) {
				ps = getAEConnection().prepareStatement(updateSQLTally);
				ps.setBoolean(1, tally);
				ps.setLong(2, entryId);
				ps.executeUpdate();
			}
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String selectSQLEntryYearEndClosing = 
			"select distinct(jEntry.id) as entryId from GeneralJournalEntry jEntry "  
			+ " inner join GeneralJournal jItem on jEntry.id = jItem.ENTRY_ID "
			+ " where jItem.OWNER_ID = ? and jItem.acc_period_id = ? and jItem.YEAR_END_CLOSING = 1";
	public void deleteYearEndClosing(AEDescriptive ownerDescr, AEDescriptive accPeriodDescr) throws AEException {
		ResultSet rs = null;
		PreparedStatement ps = null;
		try {
			// collect entries for deleting
			ps = getAEConnection().prepareStatement(selectSQLEntryYearEndClosing);
			ps.setLong(1, ownerDescr.getDescriptor().getID());
			ps.setLong(2, accPeriodDescr.getDescriptor().getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				long entryId = rs.getLong("entryId");
				deleteEntry(AccJournalEntry.lazyDescriptor(entryId));
			}
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
}
