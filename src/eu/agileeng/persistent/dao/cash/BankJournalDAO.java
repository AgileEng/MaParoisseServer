package eu.agileeng.persistent.dao.cash;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.business.bank.BankAccount;
import eu.agileeng.domain.business.bank.BankAccountBalance;
import eu.agileeng.domain.business.bank.BankRecognitionRule;
import eu.agileeng.domain.business.bank.BankRecognitionRulesList;
import eu.agileeng.domain.business.bank.BankTransaction;
import eu.agileeng.domain.business.bank.BankTransactionsList;
import eu.agileeng.domain.business.bank.BankUtil;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.document.AEDocumentDescriptor;
import eu.agileeng.domain.document.imp.AEDocumentDescriptorImp;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.util.AEStringUtil;

/**
 * Manages both BankJournal and BankRecognitionRule tables
 * 
 * @author vvatov
 *
 */
public class BankJournalDAO extends AbstractDAO {
	
	private static final String insertSQL = 
		"insert into BankJournal (OWNER_ID, BANK_ACCOUNT_ID, JOURNAL_ID, JOURNAL_CODE, STATEMENT_REF, ENTRY_REF, "
		+ "ENTRY_DATE, ACCOUNT_ID, AUXILIARY_ID, AUXILIARY, REFERENCE_ID, REFERENCE_TYPE, REFERENCE, DESCRIPTION, CURRENCY, "
		+ "DEBIT_AMOUNT, CREDIT_AMOUNT, CODE, CODE_OPERATION, PERIOD_FINAL_DATE, IS_RECOGNISED) "
		+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String insertSQLRRule = "insert into BankRecognitionRule (OWNER_ID, BANK_ACCOUNT_ID, CODE, NAME,"
		+" DESCRIPTION, IS_SYSTEM, IS_ACTIVE, TEXT_CONDITION, RULE_CONDITION_ID, ACCOUNT_ID, AUXILIARY_ID, AUXILIARY_CODE, AFB_CODE)"
		+" values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String updateSQL = 
		"update BankJournal  set OWNER_ID = ?, BANK_ACCOUNT_ID = ?, JOURNAL_ID = ?, JOURNAL_CODE = ?, "
		+ "STATEMENT_REF = ?, ENTRY_REF = ?, ENTRY_DATE = ?, ACCOUNT_ID = ?, "
		+ "AUXILIARY_ID = ?, AUXILIARY = ?, REFERENCE_ID = ?, REFERENCE_TYPE = ?, REFERENCE = ?, DESCRIPTION = ?, CURRENCY = ?, "
		+ "DEBIT_AMOUNT = ?, CREDIT_AMOUNT = ?, CODE = ?, CODE_OPERATION = ?, PERIOD_FINAL_DATE = ?, IS_RECOGNISED = ? "
		+ "WHERE ID = ?";
	
	private static final String updateSQLRRule = "update BankRecognitionRule set OWNER_ID = ?, BANK_ACCOUNT_ID = ?, CODE = ?, NAME = ?,"
		+" DESCRIPTION = ?, IS_SYSTEM = ?, IS_ACTIVE = ?, TEXT_CONDITION = ?, RULE_CONDITION_ID = ?, ACCOUNT_ID = ?,"
		+" AUXILIARY_ID = ?, AUXILIARY_CODE = ?, AFB_CODE = ? where ID = ?";
	
	// don't remove order by
	private static final String selectSQL = 
		"select bj.*, acc.CODE as ACCOUNT_CODE, acc.DESCRIPTION as ACCOUNT_DESCRIPTION "
		+ "from BankJournal bj left join Account acc on bj.ACCOUNT_ID = acc.ID "
		+ "where bj.OWNER_ID = ? and bj.bank_account_id = ? and bj.entry_date >= ? and bj.entry_date <= ? "
		+ "order by bj.entry_date asc";
	
	private static final String selectSQLRRuleByBA = "select * from BankRecognitionRule where BANK_ACCOUNT_ID = ?";
	
	private static final String deleteSQL = 
		"delete from BankJournal where id = ?";
	
	private static final String deleteSQLRRule = "delete from BankRecognitionRule where ID = ?";
	
	private static final String selectSQLFinalBalance = 
			"select SUM(ISNULL(debit_amount, 0.0)) as debitTurnover, SUM(ISNULL(credit_amount, 0.0)) as creditTurnover "
			+ "from BankJournal " 
			+ "where BANK_ACCOUNT_ID = ? and CODE = ? and ENTRY_DATE > ?";
	
	private static final String selectSQLEbicsImportExists = 
			"select top 1 ID from BankJournal where OWNER_ID = ? and BANK_ACCOUNT_ID = ? and STATEMENT_REF = ?";
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public BankJournalDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}
	
	public void build(BankRecognitionRule brr, ResultSet rs) throws SQLException, AEException {
		// ID
		brr.setID(rs.getLong("ID"));
		//OWNER_ID
		long ownerId = rs.getLong("OWNER_ID");
		if(!rs.wasNull()) {
			brr.setCompany(Organization.lazyDescriptor(ownerId));
		}
		//BANK_ACCOUNT_ID
		AEDescriptor bankAccDescr = BankAccount.lazyDescriptor(rs.getLong("BANK_ACCOUNT_ID"));
		brr.setBankAccount(bankAccDescr);
		//CODE
		brr.setCode(rs.getString("CODE"));
		//NAME
		brr.setName(rs.getString("NAME"));
		//DESCRIPTION
		brr.setDescription(rs.getString("DESCRIPTION"));
		//IS_SYSTEM
		brr.setSystem(rs.getBoolean("IS_SYSTEM"));
		//IS_ACTIVE
		brr.setActive(rs.getBoolean("IS_ACTIVE"));
		//TEXT_CONDITION
		brr.setTextCondition(rs.getString("TEXT_CONDITION"));
		//RULE_CONDITION_ID
		brr.setRuleCondition(BankUtil.RuleCondition.valueOf(rs.getLong("RULE_CONDITION_ID")));
		//ACCOUNT_ID
		long accId = rs.getLong("ACCOUNT_ID");
		if(!rs.wasNull()) {
			brr.setAccount(AccAccount.lazyDescriptor(accId));
		}
		
		//AUXILIARY_ID
		AEDescriptor auxDescr = new AEDescriptorImp();
		auxDescr.setID(rs.getLong("AUXILIARY_ID"));
		
		//AUXILIARY_CODE
		auxDescr.setCode(rs.getString("AUXILIARY_CODE"));
		
		brr.setAuxiliary(auxDescr);
		//AFB_CODE
		brr.setCodeAFB(rs.getString("AFB_CODE"));
	}
	
	protected int build(BankRecognitionRule brr, PreparedStatement ps, int i) throws SQLException, AEException {
		// OWNER_ID
		if(brr.getCompany() != null) {
			ps.setLong(i++, brr.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// BANK_ACCOUNT_ID
		if(brr.getBankAccount() != null) {
			ps.setLong(i++, brr.getBankAccount().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		//CODE
		ps.setString(i++, brr.getCode());
		//NAME
		ps.setString(i++, brr.getName());
		//DESCRIPTION
		ps.setString(i++, brr.getDescription());
		//IS_SYSTEM
		ps.setBoolean(i++, brr.isSystem());
		//IS_ACTIVE
		ps.setBoolean(i++, brr.isActive());
		//TEXT_CONDITION
		ps.setString(i++, brr.getTextCondition());
		//RULE_CONDITION_ID
		ps.setLong(i++, brr.getRuleCondition().getConditionId());
		//ACCOUNT_ID
		if (brr.getAccount() != null) {
			ps.setLong(i++, brr.getAccount().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		if (brr.getAuxiliary() != null) {
		//AUXILIARY_ID
			ps.setLong(i++, brr.getAuxiliary().getID());
		
		//AUXILIARY_CODE
			ps.setString(i++, brr.getAuxiliary().getCode());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
			ps.setNull(i++, java.sql.Types.VARCHAR);
		}
		
		//AFB_CODE
		ps.setString(i++, brr.getCodeAFB());

		// return the current ps position 
		return i;
	}
	
	//INSERT BankTransaction
	public void insert(BankTransaction bankTransaction) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);
			build(bankTransaction, ps, 1);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				bankTransaction.setID(rs.getLong(1));
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set the View state
			bankTransaction.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	//INSERT BankRecognitionRule
	public void insert(BankRecognitionRule brr) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLRRule);
			build(brr, ps, 1);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				brr.setID(rs.getLong(1));
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set the View state
			brr.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	//UPDATE BankTransaction
	public void update(BankTransaction bankTransaction) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(updateSQL);
			int i = build(bankTransaction, ps, 1);
			ps.setLong(i++, bankTransaction.getID());
			
			// execute
			ps.executeUpdate();
			
			// set the View state
			bankTransaction.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	//UPDATE BankRecognitionRule
	public void update(BankRecognitionRule bankRR) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(updateSQLRRule);
			int i = build(bankRR, ps, 1);
			ps.setLong(i++, bankRR.getID());
			
			// execute
			ps.executeUpdate();
			
			// set the View state
			bankRR.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	protected int build(BankTransaction bt, PreparedStatement ps, int i) throws SQLException, AEException {
		// OWNER_ID
		if(bt.getCompany() != null) {
			ps.setLong(i++, bt.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// BANK_ACCOUNT_ID
		if(bt.getBankAccount() != null) {
			ps.setLong(i++, bt.getBankAccount().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// JOURNAL_ID, JOURNAL_CODE
		if(bt.getJournal() != null) {
			ps.setLong(i++, bt.getJournal().getID());
			ps.setString(i++, bt.getJournal().getCode());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// STATEMENT_REF, ENTRY_REF
		if(bt.getStatementRef() != null) {
			ps.setString(i++, bt.getStatementRef().getName());
			ps.setString(i++, bt.getStatementRef().getDescription());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// ENTRY_DATE
		ps.setDate(i++, AEPersistentUtil.getSQLDate(bt.getDate()));
		
		// ACCOUNT_ID
		if(bt.getAccount() != null) {
			ps.setLong(i++, bt.getAccount().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// AUXILIARY
		if(bt.getAuxiliary() != null) {
			// id
			if(bt.getAuxiliary().isPersistent()) {
				ps.setLong(i++, bt.getAuxiliary().getID());
			} else {
				ps.setNull(i++, java.sql.Types.BIGINT);
			}
			
			// code
			if(!AEStringUtil.isEmpty(bt.getAuxiliary().getCode())) {
				ps.setString(i++, bt.getAuxiliary().getCode());
			} else {
				ps.setNull(i++, java.sql.Types.NVARCHAR);
			}
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// REFERENCE_ID, REFERENCE_TYPE, REFERENCE
		if(bt.getReference() != null) {
			if(bt.getReference().getID() > 0) {
				ps.setLong(i++, bt.getReference().getID());
			} else {
				ps.setNull(i++, java.sql.Types.BIGINT);
			}
			if(bt.getReference().getClazz() != null) {
				ps.setLong(i++, bt.getReference().getClazz().getID());
			} else {
				ps.setNull(i++, java.sql.Types.BIGINT);
			}
			ps.setString(i++, bt.getReference().getDescription());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
			ps.setNull(i++, java.sql.Types.BIGINT);
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// DESCRIPTION
		ps.setString(i++, bt.getDescription());

		// CURRENCY
		if(bt.getCurrency() != null) {
			ps.setString(i++, bt.getCurrency().getCode());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// DEBIT_AMOUNT
		if(bt.getDtAmount() != null) {
			ps.setDouble(i++, bt.getDtAmount());
		} else {
			ps.setNull(i++, java.sql.Types.NUMERIC);
		}
		
		// CREDIT_AMOUNT
		if(bt.getCtAmount() != null) {
			ps.setDouble(i++, bt.getCtAmount());
		} else {
			ps.setNull(i++, java.sql.Types.NUMERIC);
		}
		
		// CODE
		ps.setString(i++, bt.getCode());
		
		// CODE_OPERATION
		ps.setString(i++, bt.getCodeOperation());
		
		// PERIOD_FINAL_DATE
		ps.setDate(i++, AEPersistentUtil.getSQLDate(bt.getPeriodFinalDate()));
		
		// IS_RECOGNISED
		ps.setBoolean(i++, bt.isRecognised());
		
		// return the current ps position 
		return i;
	}
	
	protected void build(BankTransaction bt, ResultSet rs) throws SQLException, AEException {
		// ID
		bt.setID(rs.getLong("ID"));

		// OWNER_ID
		bt.setCompany(Organization.lazyDescriptor(rs.getLong("OWNER_ID")));
		
		// BANK_ACCOUNT_ID
		bt.setBankAccount(BankAccount.lazyDescriptor(rs.getLong("BANK_ACCOUNT_ID")));
		
		// JOURNAL_ID, JOURNAL_CODE
		AEDescriptorImp journalDescr = new AEDescriptorImp();
		journalDescr.setID(rs.getLong("JOURNAL_ID"));
		journalDescr.setCode(rs.getString("JOURNAL_CODE"));
		bt.setJournal(journalDescr);
		
		// STATEMENT_REF, ENTRY_REF
		AEDescriptorImp statementRefDescr = new AEDescriptorImp();
		statementRefDescr.setName(rs.getString("STATEMENT_REF"));
		statementRefDescr.setDescription(rs.getString("ENTRY_REF"));
		bt.setStatementRef(statementRefDescr);
		
		// ENTRY_DATE
		bt.setDate(rs.getDate("ENTRY_DATE"));
		
		// ACCOUNT_ID, ACCOUNT_CODE
		long accId = rs.getLong("ACCOUNT_ID");
		if(!rs.wasNull()) {
			AEDescriptor accDescr = BankAccount.lazyDescriptor(accId);
			accDescr.setCode(rs.getString("ACCOUNT_CODE"));
			try {
				accDescr.setDescription(rs.getString("ACCOUNT_DESCRIPTION"));
			} catch(Throwable t) {};
			bt.setAccount(accDescr);
		}
		
		// AUXILIARY
		long auxiliaryId = rs.getLong("AUXILIARY_ID");
		if(!rs.wasNull()) {
			AEDescriptor auxDescr = Organization.lazyDescriptor(auxiliaryId);
			auxDescr.setCode(rs.getString("AUXILIARY"));
			bt.setAuxiliary(auxDescr);
		}
		
		// REFERENCE_ID, REFERENCE_TYPE, REFERENCE
		AEDocumentDescriptor docRefDescr = new AEDocumentDescriptorImp();
		docRefDescr.setID(rs.getLong("REFERENCE_ID"));
		docRefDescr.setClazz(DomainClass.valueOf(rs.getLong("REFERENCE_TYPE")));
		docRefDescr.setDescription(rs.getString("REFERENCE"));
		bt.setReference(docRefDescr);
		
		// DESCRIPTION
		bt.setDescription(rs.getString("DESCRIPTION"));

		// CURRENCY
		AEDescriptorImp currDescr = new AEDescriptorImp();
		currDescr.setCode(rs.getString("CURRENCY"));
		bt.setCurrency(currDescr);
		
		// DEBIT_AMOUNT
		double dtAmount = rs.getDouble("DEBIT_AMOUNT");
		if(!rs.wasNull()) {
			bt.setDtAmount(new Double(dtAmount));
		}
		
		// CREDIT_AMOUNT
		double ctAmount = rs.getDouble("CREDIT_AMOUNT");
		if(!rs.wasNull()) {
			bt.setCtAmount(new Double(ctAmount));
		}
		
		// CODE
		bt.setCode(rs.getString("CODE"));
		
		// PERIOD_FINAL_DATE
		bt.setPeriodFinalDate(rs.getDate("PERIOD_FINAL_DATE"));
		
		// CODE_OPERATION
		bt.setCodeOperation(rs.getString("CODE_OPERATION"));
		
		// IS_RECOGNISED
		bt.setRecognised(rs.getBoolean("IS_RECOGNISED"));
	}
	
	public BankTransactionsList load(AEDescriptor ownerDescr, AEDescriptor bankAccountDescr, Date startDate, Date endDate) throws AEException {
		BankTransactionsList bankTransactionsList = new BankTransactionsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, ownerDescr.getID());
			ps.setLong(2, bankAccountDescr.getID());
			ps.setDate(3, AEPersistentUtil.getSQLDate(startDate));
			ps.setDate(4, AEPersistentUtil.getSQLDate(endDate));
			rs = ps.executeQuery();
			while(rs.next()) {
				BankTransaction bt = new BankTransaction();
				build(bt, rs);
				bt.setView();
				bankTransactionsList.add(bt);
			}
		    return bankTransactionsList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	/**
	 * Calculates Final DB Balance.
	 * 
	 * @param bankAccDescr
	 * @param openingBalance
	 * @param openingBalanceDate
	 * @return
	 * @throws AEException
	 */
	public BankAccountBalance calculateFinalBalance(AEDescriptor bankAccDescr, double openingBalance, Date openingBalanceDate) throws AEException {
		BankAccountBalance finalBalance = new BankAccountBalance();
		finalBalance.setOpeningBalance(openingBalance);
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLFinalBalance);
			ps.setLong(1, bankAccDescr.getID());
			ps.setString(2, BankTransaction.CODE_MOVEMENT);
			ps.setDate(3, AEPersistentUtil.getSQLDate(openingBalanceDate));
			rs = ps.executeQuery();
			if(rs.next()) {
				finalBalance.setDebitTurnover(rs.getDouble("debitTurnover"));
				finalBalance.setCreditTurnover(rs.getDouble("creditTurnover"));
			}
			finalBalance.calculateFinalBalance();
		    return finalBalance;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	//SELECT BankRecognitionRulesList by BANK_ACCOUNT_ID
	public BankRecognitionRulesList load(long bankAccountId) throws AEException {
		BankRecognitionRulesList brrList = new BankRecognitionRulesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLRRuleByBA);
			ps.setLong(1, bankAccountId);

			rs = ps.executeQuery();
			while(rs.next()) {
				BankRecognitionRule brr = new BankRecognitionRule();
				build(brr, rs);
				brr.setView();
				brrList.add(brr);
			}
		    return brrList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void delete(BankTransaction bankTransaction) throws AEException {
		PreparedStatement ps = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(deleteSQL);
			ps.setLong(1, bankTransaction.getID());
			
			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	//DELETE BankRecognitionRule
	public void delete(BankRecognitionRule  brr) throws AEException {
		PreparedStatement ps = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(deleteSQLRRule);
			ps.setLong(1, brr.getID());
			
			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	

	/**
	 * Check whether specified bank statement is already imported.
	 * The check will be made by tenant, bank account and Ebics file name
	 * 
	 * @param ownerDescr
	 * @param bankAccountDescr
	 * @param statementRef getName() will return the file name, source of the import
	 * @return
	 * @throws AEException
	 */
	public boolean ebicsImportExists(AEDescriptor ownerDescr, AEDescriptor bankAccountDescr, AEDescriptor statementRef) throws AEException {
		boolean exists = false;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLEbicsImportExists);
			ps.setLong(1, ownerDescr.getID());
			ps.setLong(2, bankAccountDescr.getID());
			ps.setString(3, statementRef.getName());
			rs = ps.executeQuery();
			if(rs.next()) {
				exists = true;
			}
		    return exists;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
}
