package eu.agileeng.persistent.dao.cash;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.acc.AccPeriod;
import eu.agileeng.domain.business.bank.BankAccount;
import eu.agileeng.domain.business.bank.BankAccountBalance;
import eu.agileeng.domain.business.bank.BankAccountBalancesList;
import eu.agileeng.domain.business.bank.BankAccountsList;
import eu.agileeng.domain.contact.Address;
import eu.agileeng.domain.contact.Contact;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEStringUtil;

public class BankAccountDAO extends AbstractDAO {

	private static final String insertSQL = 
		"insert into BankAccount (OWNER_ID, CODE, NAME, DESCRIPTION, BANKNAME, "
		+ " ACC_ID, IBAN, RIB, JOURNAL_ID, JOURNAL_CODE, ADDRESS, CONTACT_NAME, "
		+ "CONTACT_PHONE, CONTACT_EMAIL, WEB_SITE, ETEBAC, ENTRY_TYPE) values "
		+ "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String updateSQL = 
		"update BankAccount set OWNER_ID = ?, CODE = ?, NAME = ?, DESCRIPTION = ?, BANKNAME = ?, "
		+ " ACC_ID = ?, IBAN = ?, RIB = ?, JOURNAL_ID = ?, JOURNAL_CODE = ?, ADDRESS = ?, CONTACT_NAME = ?, "
		+ "CONTACT_PHONE = ?, CONTACT_EMAIL = ?, WEB_SITE = ?, ETEBAC = ?, ENTRY_TYPE = ? "
		+ " where id = ?";
	
	private static final String selectSQLToOwner = 
		"select ba.*, acc.code as acc_code from BankAccount ba left join Account acc on "
		+ "ba.ACC_ID = acc.ID where ba.OWNER_ID = ?";
	
	private static final String selectSQLBankAccountById = 
		"select ba.*, acc.code as acc_code, acc.name as acc_name from BankAccount ba left join Account acc on "
		+ "ba.ACC_ID = acc.ID where ba.ID = ?";
	
	private static final String selectSQLToOwnerAndEntryType = 
			"select ba.*, acc.code as acc_code from BankAccount ba left join Account acc on "
			+ "ba.ACC_ID = acc.ID where ba.OWNER_ID = ? and ba.ENTRY_TYPE = ?";
	
	private static final String selectSQLByRibWithoutCle = 
		"select ba.*, acc.code as acc_code from BankAccount ba left join Account acc on " 
		+ " ba.ACC_ID = acc.ID where SUBSTRING (RIB, 1, 21) = ?";
	
	/////////////////////////////////////////////////////
	private static final String insertSQLBalance = 
		"insert into BankAccountBalance (BANK_ACCOUNT_ID, ACC_PERIOD_ID, "
		+ "OPENING_BALANCE, DEBIT_TURNOVER, CREDIT_TURNOVER, FINAL_BALANCE, "
		+ "BANK_OPENING_BALANCE, BANK_OPENING_BALANCE_DATE, BANK_FINAL_BALANCE, BANK_FINAL_BALANCE_DATE) "
		+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String updateSQLBalance = 
		"update BankAccountBalance set BANK_ACCOUNT_ID = ?, ACC_PERIOD_ID = ?, "
		+ "OPENING_BALANCE = ?, DEBIT_TURNOVER = ?, CREDIT_TURNOVER = ?, FINAL_BALANCE = ?, "
		+ "BANK_OPENING_BALANCE = ?, BANK_OPENING_BALANCE_DATE = ?, BANK_FINAL_BALANCE = ?, BANK_FINAL_BALANCE_DATE = ? "
		+ " where id = ?";
	
	private static final String selectSQLBalance = 
		"select * from BankAccountBalance where BANK_ACCOUNT_ID = ? and ACC_PERIOD_ID = ?";
	
	private static final String selectSQLBankAccountName = 
			"select name from BankAccount where id = ?";
	
	/////////////////////////////////////////////////////
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public BankAccountDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	public void insert(BankAccount bankAccount) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);
			build(bankAccount, ps, 1);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				bankAccount.setID(rs.getLong(1));
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set the View state
			bankAccount.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	private static final String validateSQLAccAccount = 
			"select count(id) as cnt from BankAccount where OWNER_ID = ? and ACC_ID = ?";
	private static final String validateSQLIban = 
			"select count(id) as cnt from BankAccount where OWNER_ID = ? and IBAN = ?";
	private static final String validateSQLRib = 
			"select count(id) as cnt from BankAccount where OWNER_ID = ? and RIB != '' and RIB is not null and RIB = ?";
	public void validate(BankAccount bankAccount) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// validate acc account
			ps = getAEConnection().prepareStatement(validateSQLAccAccount);
			ps.setLong(1, bankAccount.getCompany().getDescriptor().getID());
			ps.setLong(2, bankAccount.getAccount().getDescriptor().getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				int count = rs.getInt("cnt");
				if(count > 1) {
					throw new AEException("Un journal avec le même compte existent déjà!");
				}
			}
			AEConnection.close(rs);
			AEConnection.close(ps);
			
			// validate iban
			ps = getAEConnection().prepareStatement(validateSQLIban);
			ps.setLong(1, bankAccount.getCompany().getDescriptor().getID());
			ps.setString(2, AEStringUtil.trim(bankAccount.getIban()));
			rs = ps.executeQuery();
			if(rs.next()) {
				int count = rs.getInt("cnt");
				if(count > 1) {
					throw new AEException("Un journal avec le même IBAN existe déjà!");
				}
			}
			AEConnection.close(rs);
			AEConnection.close(ps);
			
			// validate rib
			ps = getAEConnection().prepareStatement(validateSQLRib);
			ps.setLong(1, bankAccount.getCompany().getDescriptor().getID());
			ps.setString(2, AEStringUtil.trim(bankAccount.getRib()));
			rs = ps.executeQuery();
			if(rs.next()) {
				int count = rs.getInt("cnt");
				if(count > 1) {
					throw new AEException("Un journal avec le même RIB existe déjà!");
				}
			}
			AEConnection.close(rs);
			AEConnection.close(ps);
			
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void update(BankAccount bankAccount) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(updateSQL);
			int i = build(bankAccount, ps, 1);
			ps.setLong(i++, bankAccount.getID());
			
			// execute
			ps.executeUpdate();
			
			// set the View state
			bankAccount.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	protected int build(BankAccount ba, PreparedStatement ps, int i) throws SQLException, AEException {
		// OWNER_ID
		if(ba.getCompany() != null) {
			ps.setLong(i++, ba.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		// CODE
		ps.setString(i++, ba.getCode());
		// NAME
		ps.setString(i++, ba.getName());
		// DESCRIPTION
		ps.setString(i++, ba.getDescription());
		// BANKNAME
		if(ba.getBank() != null) {
			ps.setString(i++, ba.getBank().getDescriptor().getName());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		// ACC_ID
		if(ba.getAccount() != null) {
			ps.setLong(i++, ba.getAccount().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		// IBAN
		ps.setString(i++, ba.getIban());
		// RIB
		ps.setString(i++, ba.getRib());
		// JOURNAL_ID, JOURNAL_CODE
		if(ba.getAccJournal() != null) {
			ps.setLong(i++, ba.getAccJournal().getDescriptor().getID());
			ps.setString(i++, ba.getAccJournal().getDescriptor().getCode());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		// ADDRESS
		if(ba.getAddress() != null) {
			ps.setString(i++, ba.getAddress().getDescription());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		// CONTACT_NAME
		if(ba.getContact() != null) {
			ps.setString(i++, ba.getContact().getName());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		// CONTACT_PHONE
		if(ba.getContact() != null) {
			ps.setString(i++, ba.getContact().getPhone());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		// CONTACT_EMAIL
		if(ba.getContact() != null) {
			ps.setString(i++, ba.getContact().geteMail());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		// WEB_SITE
		if(ba.getContact() != null) {
			ps.setString(i++, ba.getContact().getHomepage());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		// ETEBAC
		ps.setString(i++, ba.getEtebac());
		// ENTRY_TYPE
		ps.setInt(i++, ba.getEntryType());
		
		// return the current ps position 
		return i;
	}
	
	protected void build(BankAccount ba, ResultSet rs) throws SQLException, AEException {
		// ID
		ba.setID(rs.getLong("ID"));
		
		// OWNER_ID
		ba.setCompany(Organization.lazyDescriptor(rs.getLong("OWNER_ID")));

		// CODE
		ba.setCode(rs.getString("CODE"));

		// NAME
		ba.setName(rs.getString("NAME"));

		// DESCRIPTION
		ba.setDescription(rs.getString("DESCRIPTION"));

		// BANKNAME
		AEDescriptor bankDescr = new AEDescriptorImp();
		bankDescr.setName(rs.getString("BANKNAME"));
		ba.setBank(bankDescr);

		// ACC_ID, ACC_CODE
		AEDescriptor accDescr = new AEDescriptorImp();
		accDescr.setID(rs.getLong("ACC_ID"));
		accDescr.setCode(rs.getString("ACC_CODE"));
		try {
			accDescr.setName(rs.getString("ACC_NAME"));
		} catch(Exception e){};
		ba.setAccount(accDescr);

		// IBAN
		ba.setIban(rs.getString("IBAN"));

		// RIB
		ba.setRib(rs.getString("RIB"));

		// JOURNAL_ID, JOURNAL_CODE
		AEDescriptor journalDescr = new AEDescriptorImp();
		journalDescr.setID(rs.getLong("JOURNAL_ID"));
		journalDescr.setCode(rs.getString("JOURNAL_CODE"));
		ba.setAccJournal(journalDescr);

		// ADDRESS
		Address address = new Address();
		address.setDescription(rs.getString("ADDRESS"));
		ba.setAddress(address);

		// CONTACT_NAME, CONTACT_PHONE, CONTACT_EMAIL, WEB_SITE
		Contact contact = new Contact();
		contact.setName(rs.getString("CONTACT_NAME"));
		contact.setPhone(rs.getString("CONTACT_PHONE"));
		contact.seteMail(rs.getString("CONTACT_EMAIL"));
		contact.setHomepage(rs.getString("WEB_SITE"));
		ba.setContact(contact);
		
		// ENTRY_TYPE
		ba.setEntryType(rs.getInt("ENTRY_TYPE"));
		
		// ETEBAC
		ba.setEtebac(rs.getString("ETEBAC"));
	}
	
	public BankAccountsList load(AEDescriptor toDescr) throws AEException {
		BankAccountsList bankAccountsList = new BankAccountsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLToOwner);
			ps.setLong(1, toDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				BankAccount ba = new BankAccount();
				build(ba, rs);
				ba.setView();
				bankAccountsList.add(ba);
			}
		    return bankAccountsList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public BankAccount load(long bankAccId) throws AEException {
		BankAccount bankAccount = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLBankAccountById);
			ps.setLong(1, bankAccId);
			rs = ps.executeQuery();
			if(rs.next()) {
				bankAccount = new BankAccount();
				build(bankAccount, rs);
				bankAccount.setView();
			}
		    return bankAccount;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public String loadBankAccountName(long bankAccId) throws AEException {
		String name = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLBankAccountName);
			ps.setLong(1, bankAccId);
			rs = ps.executeQuery();
			if(rs.next()) {
				name = rs.getString("name");
			}
		    return name;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public BankAccountsList load(AEDescriptor toDescr, int entryType) throws AEException {
		BankAccountsList bankAccountsList = new BankAccountsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLToOwnerAndEntryType);
			ps.setLong(1, toDescr.getID());
			ps.setLong(2, entryType);
			rs = ps.executeQuery();
			while(rs.next()) {
				BankAccount ba = new BankAccount();
				build(ba, rs);
				ba.setView();
				bankAccountsList.add(ba);
			}
		    return bankAccountsList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public BankAccountsList loadByRibWithoutCle(String rib) throws AEException {
		BankAccountsList bankAccountsList = new BankAccountsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLByRibWithoutCle);
			ps.setString(1, rib);
			rs = ps.executeQuery();
			while(rs.next()) {
				BankAccount ba = new BankAccount();
				build(ba, rs);
				ba.setView();
				bankAccountsList.add(ba);
			}
		    return bankAccountsList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	/////////////////////////////////////////////
	protected int build(BankAccountBalance bab, PreparedStatement ps, int i) throws SQLException, AEException {
		// BANK_ACCOUNT_ID
		if(bab.getBankAccount() != null) {
			ps.setLong(i++, bab.getBankAccount().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// ACC_PERIOD_ID
		if(bab.getAccPeriod() != null) {
			ps.setLong(i++, bab.getAccPeriod().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// OPENING_BALANCE
		ps.setDouble(i++, bab.getOpeningBalance());
		
		// DEBIT_TURNOVER
		ps.setDouble(i++, bab.getDebitTurnover());
		
		// CREDIT_TURNOVER
		ps.setDouble(i++, bab.getCreditTurnover());
		
		// FINAL_BALANCE
		ps.setDouble(i++, bab.getFinalBalance());
		
		// BANK_OPENING_BALANCE
		ps.setDouble(i++, bab.getBankOpeningBalance());
		
		// BANK_OPENING_BALANCE_DATE
		if(bab.getBankOpeningBalanceDate() != null) {
			ps.setDate(i++, AEPersistentUtil.getSQLDate(bab.getBankOpeningBalanceDate()));
		} else {
			ps.setNull(i++,  java.sql.Types.DATE);
		}
		
		// BANK_CLOSING_BALANCE
		ps.setDouble(i++, bab.getBankFinalBalance());
		
		// BANK_FINAL_BALANCE_DATE
		if(bab.getBankFinalBalanceDate() != null) {
			ps.setDate(i++, AEPersistentUtil.getSQLDate(bab.getBankFinalBalanceDate()));
		} else {
			ps.setNull(i++,  java.sql.Types.DATE);
		}
		
		// return the current ps position 
		return i;
	}
	
	protected void build(BankAccountBalance bab, ResultSet rs) throws SQLException, AEException {
		// ID
		bab.setID(rs.getLong("ID"));
		
		// BANK_ACCOUNT_ID
		long bankAccId = rs.getLong("BANK_ACCOUNT_ID");
		if(!rs.wasNull()) {
			bab.setBankAccount(BankAccount.lazyDescriptor(bankAccId));
		}
		
		// ACC_PERIOD_ID
		long accPeriodId = rs.getLong("ACC_PERIOD_ID");
		if(!rs.wasNull()) {
			bab.setAccPeriod(AccPeriod.lazyDescriptor(accPeriodId));
		}
		
		// OPENING_BALANCE
		bab.setOpeningBalance(rs.getDouble("OPENING_BALANCE"));
		
		// DEBIT_TURNOVER
		bab.setDebitTurnover(rs.getDouble("DEBIT_TURNOVER"));
		
		// CREDIT_TURNOVER
		bab.setCreditTurnover(rs.getDouble("CREDIT_TURNOVER"));
		
		// CLOSING_BALANCE
		bab.setFinalBalance(rs.getDouble("FINAL_BALANCE"));
		
		// BANK_OPENING_BALANCE
		bab.setBankOpeningBalance(rs.getDouble("BANK_OPENING_BALANCE"));
		
		// BANK_OPENING_BALANCE_DATE
		bab.setBankOpeningBalanceDate(rs.getDate("BANK_OPENING_BALANCE_DATE"));
	
		// BANK_CLOSING_BALANCE
		bab.setBankFinalBalance(rs.getDouble("BANK_FINAL_BALANCE"));
		
		// BANK_FINAL_BALANCE_DATE
		bab.setBankFinalBalanceDate(rs.getDate("BANK_FINAL_BALANCE_DATE"));
	}
	
	public void insert(BankAccountBalance bankAccountBalance) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLBalance);
			build(bankAccountBalance, ps, 1);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				bankAccountBalance.setID(rs.getLong(1));
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set the View state
			bankAccountBalance.setView();
		} catch (SQLException e) {
			throw new AEException("Duplication! ");
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void update(BankAccountBalance bankAccountBalancce) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(updateSQLBalance);
			int i = build(bankAccountBalancce, ps, 1);
			ps.setLong(i++, bankAccountBalancce.getID());
			
			// execute
			ps.executeUpdate();
			
			// set the View state
			bankAccountBalancce.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	
	private static final String deleteSQLBalance = "delete from BankAccountBalance where id = ?";
	public void delete(AEDescriptor bab) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and execute
			ps = getAEConnection().prepareStatement(deleteSQLBalance);
			ps.setLong(1, bab.getID());
			
			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public BankAccountBalance load(AEDescriptor bankAccDescr, AEDescriptor accPeriodDescr) throws AEException {
		BankAccountBalance bab = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLBalance);
			ps.setLong(1, bankAccDescr.getID());
			ps.setLong(2, accPeriodDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				bab = new BankAccountBalance();
				
				// build the instance
				build(bab, rs);
				
				// set in view state
				bab.setView();
			}
		    return bab;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static final String selectSQLBalancesForYear = 
			"select * from BankAccountBalance bab where bab.BANK_ACCOUNT_ID = ? and bab.BANK_FINAL_BALANCE_DATE between ? and ? order by bab.BANK_FINAL_BALANCE_DATE ";
	public BankAccountBalancesList loadBankAccountBalances(AEDescriptor bankAccDescr, int year) throws AEException {
		BankAccountBalancesList baBalancesList = new BankAccountBalancesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			Date startDate = AEDateUtil.getFirstDate(0, year);
			Date endDate = AEDateUtil.endOfTheYear(startDate);
			ps = getAEConnection().prepareStatement(selectSQLBalancesForYear);
			ps.setLong(1, bankAccDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDate(startDate)));
			ps.setDate(3, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDate(endDate)));
			rs = ps.executeQuery();
			while(rs.next()) {
				BankAccountBalance bab = new BankAccountBalance();
				
				// build the instance
				build(bab, rs);
				
				// set in view state
				bab.setView();
				
				// add to the list
				baBalancesList.add(bab);
			}
		    return baBalancesList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static final String selectSQLLastBalancesForYear = 
			"select top 1 * from BankAccountBalance bab where bab.BANK_ACCOUNT_ID = ? and bab.BANK_FINAL_BALANCE_DATE between ? and ? "
			+ " order by bab.BANK_FINAL_BALANCE_DATE desc";
	/**
	 * 
	 * 
	 * @param ownerDescr
	 * @param bankAccDescr
	 * @param year
	 * @return MAY RETURN NULL
	 */
	public BankAccountBalance loadLastBalance(AEDescriptor bankAccDescr, int year) throws AEException {
		BankAccountBalance bab = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			Date startDate = AEDateUtil.getFirstDate(0, year);
			Date endDate = AEDateUtil.endOfTheYear(startDate);
			ps = getAEConnection().prepareStatement(selectSQLLastBalancesForYear);
			ps.setLong(1, bankAccDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDate(startDate)));
			ps.setDate(3, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDate(endDate)));
			rs = ps.executeQuery();
			if(rs.next()) {
				bab = new BankAccountBalance();

				// build the instance
				build(bab, rs);

				// set in view state
				bab.setView();
			}
			return bab;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static final String selectSQLByIdAndOwner = 
			"select id from BankAccount where id = ? and OWNER_ID = ?";
	public boolean checkTenant(AEDescriptor bankAccDescr, AEDescriptor tenantDescr) throws AEException {
		boolean bRes = false;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLByIdAndOwner);
			ps.setLong(1, bankAccDescr.getID());
			ps.setLong(2, tenantDescr.getID());
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
	
	private static final String selectSQLBankBalancesExist = 
			"select ba.* from BankAccount ba where ba.OWNER_ID = ? "
			+ " and NOT EXISTS (select * from BankAccountBalance bab where bab.BANK_ACCOUNT_ID = ba.id and YEAR(bab.BANK_FINAL_BALANCE_DATE) = ?) ";
	public boolean checkBankBalancesExists(AEDescriptor tenantDescr, int year) throws AEException {
		boolean bRes = true;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLBankBalancesExist);
			ps.setLong(1, tenantDescr.getID());
			ps.setLong(2, year);
			rs = ps.executeQuery();
			if(rs.next()) {
				bRes = false;
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
}
