package eu.agileeng.persistent.dao.acc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import eu.agileeng.accbureau.AppModuleTemplate;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.acc.AccJournalEntry;
import eu.agileeng.domain.acc.cashbasis.AccJournalEntryTemplate;
import eu.agileeng.domain.acc.cashbasis.AccJournalEntryTemplate.AccountIdentificationRule;
import eu.agileeng.domain.acc.cashbasis.AccJournalEntryTemplate.AmountRule;
import eu.agileeng.domain.acc.cashbasis.AccJournalEntryTemplate.JournalIdentificationRule;
import eu.agileeng.domain.acc.cashbasis.AccJournalEntryTemplate.JournalingRule;
import eu.agileeng.domain.acc.cashbasis.AccJournalEntryTeplatesList;
import eu.agileeng.domain.acc.cashbasis.FinancialTransaction;
import eu.agileeng.domain.acc.cashbasis.FinancialTransactionTemplate;
import eu.agileeng.domain.acc.cashbasis.FinancialTransactionTemplate.PaymentMethod;
import eu.agileeng.domain.acc.cashbasis.FinancialTransactionTemplatesList;
import eu.agileeng.domain.acc.cashbasis.Quete;
import eu.agileeng.domain.acc.cashbasis.Quete.Type;
import eu.agileeng.domain.acc.cashbasis.QuetesList;
import eu.agileeng.domain.business.bank.BankAccount;
import eu.agileeng.domain.contact.Contributor;
import eu.agileeng.domain.contact.SimpleParty;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEStringUtil;

public class FinancialTransactionDAO extends AbstractDAO {

	private static String selectSQLTemplates = "select * from FinancialTransactionTemplate where app_module_template_id = ?";
	
	private static String selectSQLAppModuleIdByFTTId = 
			"select amt.APP_MODULE_ID from FinancialTransactionTemplate ftt inner join AppModuleTemplate amt "
			+ " on ftt.APP_MODULE_TEMPLATE_ID = amt.ID where ftt.ID = ?";
	
	private static String insertSQLFinancialTransactionTemplate = 
			"insert into FinancialTransactionTemplate "
			+ "(APP_MODULE_TEMPLATE_ID, PAYMENT_METHOD_ID) "
			+ " values (?, ?)";
	
	private static String insertSQLAccJournalEntryTemplate = 
			"insert into AccJournalEntryTemplate "
			+ "(FTT_ID, ACCOUNT_IDENTIFICATION_RULE_ID, ACCOUNT_PATTERN_ID, JOURNAL_IDENTIFICATION_RULE_ID, "
			+ "JOURNAL_CODE, JOURNALING_RULE_ID, AMOUNT_RULE_ID, AMOUNT_PARAMETER_ID, DEBIT, CREDIT, QUETE_CODE) "
			+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static String updateSQLAccJournalEntryTemplate = 
			"update AccJournalEntryTemplate "
			+ "set FTT_ID = ?, ACCOUNT_IDENTIFICATION_RULE_ID = ?, ACCOUNT_PATTERN_ID = ?, JOURNAL_IDENTIFICATION_RULE_ID = ?, "
			+ "JOURNAL_CODE = ?, JOURNALING_RULE_ID = ?, AMOUNT_RULE_ID = ?, AMOUNT_PARAMETER_ID = ?, DEBIT = ?, CREDIT = ?, QUETE_CODE = ? "
			+ " where id = ?";
	
	private static String deleteSQLAccJournalEntryTemplate = 
			"delete from AccJournalEntryTemplate where id = ?";
	
	private static String selectSQLAccJournalEntryTemplates = "select * from AccJournalEntryTemplate where ftt_id = ?";
	
	// FinancialTransaction
	private static String insertSQLFinancialTransaction = 
			"insert into FinancialTransaction "
			+ "(CODE, NAME, DESCRIPTION, OWNER_ID, PAYMENT_METHOD_ID, ACC_JOURNAL_ENTRY_ID, FT_TEMPLATE_ID, DATE_TRANSACTION, "
			+ "DATE_ACCOUNTING, AMOUNT, CONTRIBUTOR_ID, ACC_EXPINC_ID, BANK_ACC_ID, SUPPLIER_ID) "
			+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static String updateSQLFinancialTransaction = 
			"update FinancialTransaction "
			+ " set CODE = ?, NAME = ?, DESCRIPTION = ?, OWNER_ID = ?, PAYMENT_METHOD_ID = ?, ACC_JOURNAL_ENTRY_ID = ?, "
			+ "	FT_TEMPLATE_ID  = ?, DATE_TRANSACTION  = ?, DATE_ACCOUNTING = ?, AMOUNT = ?, CONTRIBUTOR_ID = ?, ACC_EXPINC_ID = ?, BANK_ACC_ID = ?, "
			+ " SUPPLIER_ID = ? "
			+ " where ID = ?";
	
	private static String selectSQLFinancialTransactionByItemId = 
			" select ft.id from FinancialTransaction ft "
			+ " inner join GeneralJournalEntry gje on ft.ACC_JOURNAL_ENTRY_ID = gje.ID  "
			+ " inner join GeneralJournal gj on gje.ID = gj.ENTRY_ID "
			+ " where gj.ID = ? ";
		
		private static String selectSQLFinancialTransaction = 
				" select * from FinancialTransaction where id = ? ";
	
	// FinancialTransactionQuete
	private static String insertSQLFinancialTransactionDetail = 
			"insert into FinancialTransactionDetail "
			+ "(TYPE_ID, CODE, DESCRIPTION, NAME, AMOUNT, FT_ID) "
			+ " values (?, ?, ?, ?, ?, ?)";
	
	private static String updateSQLFinancialTransactionDetail = 
			"update FinancialTransactionDetail "
			+ " set TYPE_ID = ?, CODE = ?, DESCRIPTION = ?, NAME = ?, AMOUNT = ?, FT_ID = ? "
			+ " where id = ?";
	
	private static String selectSQLFTDetailsByFtId = 
			" SELECT * from FinancialTransactionDetail where ft_id = ?";
	
	public FinancialTransactionDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	public FinancialTransactionTemplatesList loadFinancialTransactionTemplates(AEDescriptor appModuleTemplateDescr) throws AEException {
		FinancialTransactionTemplatesList ftTemplatesList = new FinancialTransactionTemplatesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLTemplates);
			ps.setLong(1, appModuleTemplateDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				FinancialTransactionTemplate ftt = new FinancialTransactionTemplate();
				build(ftt, rs);
				
				// set view state
				ftt.setView();
				
				ftTemplatesList.add(ftt);
			}
			return ftTemplatesList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEDescriptor loadAppModuleTemplateDescrByFTT(AEDescriptor fttDescr) throws AEException {
		AEDescriptor appModuleTemplateDescr = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAppModuleIdByFTTId);
			ps.setLong(1, fttDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				appModuleTemplateDescr = new AEDescriptorImp(rs.getLong("app_module_id"));
			}
			return appModuleTemplateDescr;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	protected void build(FinancialTransactionTemplate c, ResultSet rs) throws SQLException {
		// ID
		c.setID(rs.getLong("ID"));
		
		// APP_MODULE_TEMPLATE_ID
		long tmpId = rs.getLong("APP_MODULE_TEMPLATE_ID");
		if(!rs.wasNull()) {
			c.setAppModuleTemplate(AppModuleTemplate.lazyDescriptor(tmpId));
		}
		
		// PAYMENT_METHOD_ID
		c.setPaymentMethod(PaymentMethod.valueOf(rs.getLong("PAYMENT_METHOD_ID")));
	}
	
	protected int build(FinancialTransactionTemplate ftTemplate, PreparedStatement ps, int i) throws SQLException, AEException {
		
		// APP_MODULE_TEMPLATE_ID
		if(ftTemplate.getAppModuleTemplate() != null) {
			ps.setLong(i++, ftTemplate.getAppModuleTemplate().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// PAYMENT_METHOD_ID
		if(ftTemplate.getPaymentMethod() != null) {
			ps.setLong(i++, ftTemplate.getPaymentMethod().getId());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		return i;
	}
	
	public AccJournalEntryTeplatesList loadAccJournalEntryTeplates(AEDescriptor financeTransactionTemplateDescr) throws AEException {
		AccJournalEntryTeplatesList jeTemplates = new AccJournalEntryTeplatesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAccJournalEntryTemplates);
			ps.setLong(1, financeTransactionTemplateDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				AccJournalEntryTemplate jet = new AccJournalEntryTemplate();
				build(jet, rs);
				
				// set view state
				jet.setView();
				
				jeTemplates.add(jet);
			}
			return jeTemplates;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	protected void build(AccJournalEntryTemplate c, ResultSet rs) throws SQLException {
		// ID
		c.setID(rs.getLong("ID"));
		
		// FTT_ID
		long tmpId = rs.getLong("FTT_ID");
		if(!rs.wasNull()) {
			c.setFinancialTransactionTemplate(FinancialTransactionTemplate.lazyDescriptor(tmpId));
		}
		
		// ACCOUNT_IDENTIFICATION_RULE_ID
		c.setAccountIdentificationRule(AccountIdentificationRule.valueOf(rs.getLong("ACCOUNT_IDENTIFICATION_RULE_ID")));
		
		// ACCOUNT_PATTERN_ID
		c.setAccAccount(AccAccount.lazyDescriptor(rs.getLong("ACCOUNT_PATTERN_ID")));
		
		// JOURNAL_IDENTIFICATION_RULE_ID
		c.setJournalIdentificationRule(JournalIdentificationRule.valueOf(rs.getLong("JOURNAL_IDENTIFICATION_RULE_ID")));
		
		// JOURNAL_CODE
		String journalCode = rs.getString("JOURNAL_CODE");
		if(!rs.wasNull()) {
			AEDescriptor journalDescr = new AEDescriptorImp();
			journalDescr.setCode(journalCode);
			c.setAccJournal(journalDescr);
		}
		
		// JOURNALING_RULE_ID
		c.setJournalingRule(JournalingRule.valueOf(rs.getLong("JOURNALING_RULE_ID")));
		
		// AMOUNT_RULE_ID
		c.setAmountRule(AmountRule.valueOf(rs.getLong("AMOUNT_RULE_ID")));
		
		// AMOUNT_PARAMETER_ID
		tmpId = rs.getLong("AMOUNT_PARAMETER_ID");
		if(!rs.wasNull()) {
			AEDescriptor amountParameter = new AEDescriptorImp(tmpId);
			c.setAmountParameter(amountParameter);
		}
		
		// DEBIT
		c.setDebit(rs.getInt("DEBIT") != 0 ? true : false);
		
		// CREDIT
		c.setCredit(rs.getInt("CREDIT") != 0 ? true : false);
		
		// QUETE_CODE
		String queteCode = rs.getString("QUETE_CODE");
		if(!rs.wasNull()) {
			c.setQuete(new AEDescriptorImp().withCode(queteCode));
		}
	}
	
	public void insert(FinancialTransactionTemplate ftTemplate) throws AEException {
		assert(!ftTemplate.isPersistent());
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLFinancialTransactionTemplate);
			build(ftTemplate, ps, 1);
			ps.executeUpdate();
			
			// process generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				
				//required
				ftTemplate.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set view state
			ftTemplate.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void insert(FinancialTransaction ft) throws AEException {
		assert(!ft.isPersistent());
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLFinancialTransaction);
			build(ft, ps, 1);
			ps.executeUpdate();
			
			// process generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				
				//required
				ft.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set view state
			ft.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void update(FinancialTransaction ft) throws AEException {
		PreparedStatement ps = null;
		try {
			// prepare statement and update
			ps = getAEConnection().prepareStatement(updateSQLFinancialTransaction);
			int i = build(ft, ps, 1);
			ps.setLong(i, ft.getID());
			ps.executeUpdate();
			
			// set view state
			ft.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	@SuppressWarnings("resource")
	public void insert(QuetesList ql) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLFinancialTransactionDetail);
			for (Quete q : ql) { 
				build(q, ps, 1);

				// execute
				ps.executeUpdate();

				// set generated key
				rs = ps.getGeneratedKeys();
				if (rs.next()) {
					// propagate generated key
					long id = rs.getLong(1);
					q.setID(id);
				} else {
					throw new AEException(getClass().getName() + "::insert: No keys were generated");
				}

				// set view state
				q.setView();
			}
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void update(QuetesList ql) throws AEException {
		PreparedStatement ps = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(updateSQLFinancialTransactionDetail);
			for (Quete q : ql) { 
				int i = build(q, ps, 1);
				ps.setLong(i, q.getID());

				// execute
				ps.executeUpdate();

				// set view state
				q.setView();
			}
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void update(FinancialTransactionTemplate ftTemplate) throws AEException {
		assert(ftTemplate.isPersistent());
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
//			ps = getAEConnection().prepareGenKeyStatement(updateSQLFinancialTransactionTemplate);
//			int i = build(ftTemplate, ps, 1);
//			ps.setLong(i, ftTemplate.getID());
//			ps.executeUpdate();
			
			// set view state
			ftTemplate.setView();
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void insert(AccJournalEntryTemplate journalEntryTemplate) throws AEException {
		assert(!journalEntryTemplate.isPersistent());
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLAccJournalEntryTemplate);
			build(journalEntryTemplate, ps, 1);
			ps.executeUpdate();
			
			// process generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				
				//required
				journalEntryTemplate.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set view state
			journalEntryTemplate.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void update(AccJournalEntryTemplate journalEntryTemplate) throws AEException {
		assert(!journalEntryTemplate.isPersistent());
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(updateSQLAccJournalEntryTemplate);
			int i = build(journalEntryTemplate, ps, 1);
			ps.setLong(i, journalEntryTemplate.getID());
			ps.executeUpdate();
			
			// set view state
			journalEntryTemplate.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void delete(AccJournalEntryTemplate journalEntryTemplate) throws AEException {
		assert(!journalEntryTemplate.isPersistent());
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(deleteSQLAccJournalEntryTemplate);
			ps.setLong(1, journalEntryTemplate.getID());
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	protected int build(AccJournalEntryTemplate accJournalTemplate, PreparedStatement ps, int i) throws SQLException, AEException {
		//FTT_ID
		if(accJournalTemplate.getFinancialTransactionTemplate() != null) {
			ps.setLong(i++, accJournalTemplate.getFinancialTransactionTemplate().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		//ACCOUNT_IDENTIFICATION_RULE_ID
		if(accJournalTemplate.getAccountIdentificationRule() != null) {
			ps.setLong(i++, accJournalTemplate.getAccountIdentificationRule().getId());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		//ACCOUNT_PATTERN_ID
		if(accJournalTemplate.getAccAccount() != null) {
			ps.setLong(i++, accJournalTemplate.getAccAccount().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		//JOURNAL_IDENTIFICATION_RULE_ID
		if(accJournalTemplate.getJournalIdentificationRule() != null) {
			ps.setLong(i++, accJournalTemplate.getJournalIdentificationRule().getId());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		//JOURNAL_CODE
		if(accJournalTemplate.getAccJournal() != null) {
			ps.setString(i++, accJournalTemplate.getAccJournal().getDescriptor().getCode());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		//JOURNALING_RULE_ID
		if(accJournalTemplate.getJournalingRule() != null) {
			ps.setLong(i++, accJournalTemplate.getJournalingRule().getId());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		//AMOUNT_RULE_ID
		if(accJournalTemplate.getAmountRule() != null) {
			ps.setLong(i++, accJournalTemplate.getAmountRule().getId());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		//AMOUNT_PARAMETER_ID
		if(accJournalTemplate.getAmountParameter() != null) {
			ps.setLong(i++, accJournalTemplate.getAmountParameter().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// DEBIT
		ps.setInt(i++, accJournalTemplate.isDebit() ? 1 : 0);
		
		// CREDIT
		ps.setInt(i++, accJournalTemplate.isCredit() ? 1 : 0);
		
		// QUETE_CODE
		if(accJournalTemplate.getQuete() != null) {
			ps.setString(i++, accJournalTemplate.getQuete().getCode());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		return i;
	}
	
	protected int build(FinancialTransaction ft, PreparedStatement ps, int i) throws SQLException, AEException {
		// CODE
		ps.setString(i++, ft.getCode());
		
		// NAME
		ps.setString(i++, ft.getName());
		
		// DESCRIPTION
		ps.setString(i++, ft.getDescription());
		
		// OWNER_ID
		if(ft.getCompany() != null) {
			ps.setLong(i++, ft.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// PAYMENT_METHOD_ID
		if(ft.getPaymentMethod() != null) {
			ps.setLong(i++, ft.getPaymentMethod().getId());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// ACC_JOURNAL_ENTRY_ID
		if(ft.getAccJournalEntry() != null) {
			ps.setLong(i++, ft.getAccJournalEntry().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// FT_TEMPLATE_ID
		if(ft.getFtTemplate() != null) {
			ps.setLong(i++, ft.getFtTemplate().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// DATE_TRANSACTION
		if(ft.getDateTransaction() != null) {
			ps.setDate(i++, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(ft.getDateTransaction())));
		} else {
			ps.setNull(i++, java.sql.Types.DATE);
		}
		
		// DATE_ACCOUNTING
		if(ft.getDateAccounting() != null) {
			ps.setDate(i++, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(ft.getDateAccounting())));
		} else {
			ps.setNull(i++, java.sql.Types.DATE);
		}
		
		// AMOUNT
		ps.setDouble(i++, ft.getAmount());
		
		// CONTRIBUTOR_ID
		if(ft.getContributor() != null && !ft.getContributor().isUnrecognized()) {
			ps.setLong(i++, ft.getContributor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// ACC_EXPINC_ID
		if(ft.getAccAccountExpInc() != null) {
			ps.setLong(i++, ft.getAccAccountExpInc().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// BANK_ACC_ID
		if(ft.getBankAccount() != null) {
			ps.setLong(i++, ft.getBankAccount().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// SUPPLIER_ID
		if(ft.getSupplier() != null) {
			ps.setLong(i++, ft.getSupplier().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		return i;
	}
	
	protected int build(Quete q, PreparedStatement ps, int i) throws SQLException, AEException {
		// TYPE_ID
		if(q.getType() != null) {
			ps.setLong(i++, q.getType().getId());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// CODE
		if(!AEStringUtil.isEmpty(q.getCode())) {
			ps.setString(i++, q.getCode());
		} else {
			ps.setNull(i++, java.sql.Types.NCHAR);
		}
		
		// DESCRIPTION
		ps.setString(i++, q.getDescription());
		
		// NAME
		ps.setString(i++, q.getName());
		
		// AMOUNT
		ps.setDouble(i++, q.getAmount());
		
		// FT_ID
		if(q.getFinancialTransaction() != null) {
			ps.setLong(i++, q.getFinancialTransaction().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		return i;
	}
	
	public FinancialTransaction loadSQLFinancialTransactionIdByItemId(long accJournalItemId) throws AEException {
		FinancialTransaction ft = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLFinancialTransactionByItemId);
			ps.setLong(1, accJournalItemId);
			rs = ps.executeQuery();
			if(rs.next()) {
				long ftId = rs.getLong("id");
				AEDescriptor ftDescr = FinancialTransaction.lazyDescriptor(ftId);
				ft = loadFinancialTransaction(ftDescr);
			}
			return ft;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public FinancialTransaction loadFinancialTransaction(AEDescriptor ftDescr) throws AEException {
		FinancialTransaction ft = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLFinancialTransaction);
			ps.setLong(1, ftDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				ft = new FinancialTransaction();
				build(ft, rs);
				
				// set view state
				ft.setView();
			}
			return ft;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	protected void build(FinancialTransaction ft, ResultSet rs) throws SQLException, AEException {
		// CODE, NAME, DESCRIPTION, OWNER_ID
		super.build(ft, rs);
		
		// PAYMENT_METHOD_ID
		long tmpId = rs.getLong("PAYMENT_METHOD_ID");
		if(!rs.wasNull()) {
			ft.setPaymentMethod(PaymentMethod.valueOf(tmpId));
		}
		
		// ACC_JOURNAL_ENTRY_ID
		tmpId = rs.getLong("ACC_JOURNAL_ENTRY_ID");
		if(!rs.wasNull()) {
			AccJournalEntry accJournalEntry = new AccJournalEntry();
			accJournalEntry.setID(tmpId);
			ft.setAccJournalEntry(accJournalEntry);
		}
		
		// FT_TEMPLATE_ID
		tmpId = rs.getLong("FT_TEMPLATE_ID");
		if(!rs.wasNull()) {
			ft.setFtTemplate(FinancialTransactionTemplate.lazyDescriptor(tmpId));
		}
		
		// DATE_TRANSACTION
		ft.setDateTransaction(rs.getDate("DATE_TRANSACTION"));
		
		// DATE_ACCOUNTING
		ft.setDateAccounting(rs.getDate("DATE_ACCOUNTING"));
		
		// AMOUNT
		ft.setAmount(rs.getDouble("AMOUNT"));
		
		// CONTRIBUTOR_ID
		tmpId = rs.getLong("CONTRIBUTOR_ID");
		if(!rs.wasNull()) {
			Contributor c = new Contributor();
			c.setID(tmpId);
			ft.setContributor(c);
		}
		
		// ACC_EXPINC_ID
		tmpId = rs.getLong("ACC_EXPINC_ID");
		if(!rs.wasNull()) {
			ft.setAccAccountExpInc(AccAccount.lazyDescriptor(tmpId));
		}
		
		// BANK_ACC_ID
		tmpId = rs.getLong("BANK_ACC_ID");
		if(!rs.wasNull()) {
			ft.setBankAccount(BankAccount.lazyDescriptor(tmpId));
		}
		
		// SUPPLIER_ID
		tmpId = rs.getLong("SUPPLIER_ID");
		if(!rs.wasNull()) {
			SimpleParty suppl = new SimpleParty();
			suppl.setID(tmpId);
			ft.setSupplier(suppl);
		}
	}
	
	public QuetesList loadFinancialTransactionDetails(AEDescriptor toFinancialTransaction) throws AEException {
		QuetesList quetesList = new QuetesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLFTDetailsByFtId);
			ps.setLong(1, toFinancialTransaction.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				Quete q = new Quete();
				build(q, rs);
				
				// set view state
				q.setView();
				
				quetesList.add(q);
			}
			return quetesList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	protected void build(Quete q, ResultSet rs) throws SQLException {
		// ID
		q.setID(rs.getLong("ID"));
		
	    // TYPE_ID
		q.setType(Type.valueOf(rs.getLong("TYPE_ID")));
		
		// CODE
		q.setCode(rs.getString("CODE"));
		
		// DESCRIPTION
		q.setDescription(rs.getString("DESCRIPTION"));
		
		// NAME
		q.setName(rs.getString("NAME"));
		
		// AMOUNT
		q.setAmount(rs.getDouble("AMOUNT"));
		
		// FT_ID
		long tmpId = rs.getLong("FT_ID");
		if(!rs.wasNull()) {
			q.setFinancialTransaction(FinancialTransaction.lazyDescriptor(tmpId));
		}
	}
	
	private static String updateSQLRemoveContributor = 
			"update FinancialTransaction set CONTRIBUTOR_ID = ? where ID = ?";
	public void removeContributor(AEDescriptor ftDescr) throws AEException {
		PreparedStatement ps = null;
		try {
			// prepare statement and update
			ps = getAEConnection().prepareStatement(updateSQLRemoveContributor);
			ps.setNull(1, java.sql.Types.BIGINT);
			ps.setLong(2, ftDescr.getID());
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
}
