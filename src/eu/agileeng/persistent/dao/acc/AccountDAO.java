package eu.agileeng.persistent.dao.acc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.AccAccountBalancesList;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.acc.AccAccountBalance;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.json.JSONUtil;

public class AccountDAO extends AbstractDAO {

	public static enum AccountBalanceType {
		OPENING,
		CLOSING;
	}

	private static String insertSQL = "insert into Account "
			+ "(COA_ID, PARENT_ID, CODE, NAME, DESCRIPTION, OWNER_ID, IS_SYSTEM, IS_ACTIVE, IS_MODIFIABLE, "
			+ " ACC_TYPE, VATRATE_ID, IS_CASH, IS_BANK, IS_SUPPLY, IS_SALE) "
			+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static String insertSQLGOAAccount = "insert into GOAAccounts "
			+ "(GOA_ID, ACC_ID, ACC_CODE_IN_GROUP, ACC_NAME_IN_GROUP, ACC_DESCRIPTION_IN_GROUP, S_INDEX) "
			+ " values (?, ?, ?, ?, ?, ?)";

	private static String updateSQL = "update Account set "
			+ "COA_ID = ?, PARENT_ID = ?, CODE = ?, NAME = ?, DESCRIPTION = ?, OWNER_ID = ?, "
			+ "IS_SYSTEM = ?, IS_ACTIVE = ?, IS_MODIFIABLE = ?, ACC_TYPE = ?, VATRATE_ID = ?, "
			+ "IS_CASH = ?, IS_BANK = ?, IS_SUPPLY = ?, IS_SALE = ? "
			+ " where ID = ?";

	private static String updateSQLAccountStrict = 
			"update Account set CODE = ?, NAME = ?, DESCRIPTION = ?, IS_ACTIVE = ? where ID = ?";

	private static String updateSQLAccountIsActive = 
			"update Account set IS_ACTIVE = ? where ID = ?";

	private static String updateSQLGOAAccount = "update GOAAccounts set "
			+ " GOA_ID = ?, ACC_ID = ?, ACC_CODE_IN_GROUP = ?, ACC_NAME_IN_GROUP = ?, "
			+ " ACC_DESCRIPTION_IN_GROUP = ?, S_INDEX = ? "
			+ " where id = ?";

	private static String deleteSQLGOAAccount = "delete from GOAAccounts where ID = ?";

	private static String deleteSQL = "delete from Account where ID = ?";

	private static String deleteSQLByCOAId = "delete from Account where coa_id = ?";

	private static String selectSQLAccounts = 
			"select acc.*, vat.code as vatCode, modelAcc.code as modelAccCode from Account acc "
					+ " left join VATRate vat on acc.vatrate_id = vat.id "
					+ " left join Account modelAcc on acc.PARENT_ID = modelAcc.ID "
					+ " where acc.coa_id = ? and acc.acc_type = " + AccAccount.AccountType.ACCOUNT.getTypeId() + " order by acc.CODE";

	private static String selectSQLBalanceAccounts = 
			"select acc.*, vat.code as vatCode, modelAcc.code as modelAccCode from Account acc "
					+ " left join VATRate vat on acc.vatrate_id = vat.id "
					+ " left join Account modelAcc on acc.PARENT_ID = modelAcc.ID "
					+ " where acc.coa_id = ? and acc.acc_type = " + AccAccount.AccountType.ACCOUNT.getTypeId() + ""
					+ " and (acc.code like ? or acc.code like ? or acc.code like ? or acc.code like ? or acc.code like ?)";

	private static String selectSQLAccountsByOwner = 
			"select acc.* from Account acc "
					+ "inner join ChartOfAccounts coa on acc.COA_ID = coa.ID "
					+ "where coa.OWNER_ID = ?";

	private static String selectSQLPatternAccounts = 
			"select acc.*, vat.code as vatCode from Account acc "
					+ " left join VATRate vat on acc.vatrate_id = vat.id where acc.coa_id = ? and acc_type = " + AccAccount.AccountType.PATTERN.getTypeId();

	private static String selectSQLConcreteAccounts = 
			"select acc.* from Account acc "
					+ "inner join ChartOfAccounts coa on acc.COA_ID = coa.ID "
					+ "where acc.PARENT_ID = ? and coa.OWNER_ID = ?";

	private static String selectSQLConcreteAccountsByCode = 
			"select acc.* from Account acc "
					+ "inner join ChartOfAccounts coa on acc.COA_ID = coa.ID "
					+ "where coa.OWNER_ID = ? and acc.code like ?";

	private static String selectSQLPatternActiveAccounts = 
			"select acc.*, vat.code as vatCode from Account acc "
					+ " left join VATRate vat on acc.vatrate_id = vat.id where acc.coa_id = ? "
					+ " and acc_type = " + AccAccount.AccountType.PATTERN.getTypeId() 
					+ " and is_active != 0";

	private static String selectSQLDescrByOwnerAndCodeWhere = 
			"select acc.id, acc.code, acc.name from Account acc inner join ChartOfAccounts coa on acc.COA_ID = coa.ID "
					+ " where coa.OWNER_ID = ? ";

	private static String selectSQLDescrByOwnerAndDiversAccCode = 
			"select acc.id, acc.code, acc.name " 
					+ " from Account acc inner join ChartOfAccounts coa on acc.COA_ID = coa.ID "
					+ "	where coa.OWNER_ID = ? and LTRIM(RTRIM(acc.code)) = (select LTRIM(RTRIM(diversAccCode)) from Company where PartyID = ?)";

	private static String selectSQLById = 
			"select code, name from account where id = ?";

	private static String selectSQLCashAccounts = 
			"select * from Account acc inner join ChartOfAccounts as coa " 
					+ " on acc.COA_ID = coa.ID where coa.OWNER_ID = ? and acc.IS_CASH = 1";

	private static String selectSQLSubAccounts = 
			"select * from Account acc inner join ChartOfAccounts as coa " 
					+ " on acc.COA_ID = coa.ID where coa.OWNER_ID = ? and acc.code like ?";

	private static String selectSQLSupplyAccounts = 
			"select acc.*, vat.id as vatId, vat.code as vatCode, vat.rate as vatRate " 
					+ "from Account acc inner join ChartOfAccounts as coa on acc.COA_ID = coa.ID "
					+ "left join VATRate vat on acc.vatrate_id = vat.id "
					+ "where coa.OWNER_ID = ? and acc.IS_SUPPLY = 1";

	private static String selectSQLSaleAccounts = 
			"select acc.*, vat.id as vatId, vat.code as vatCode, vat.rate as vatRate " 
					+ "from Account acc inner join ChartOfAccounts as coa on acc.COA_ID = coa.ID "
					+ "left join VATRate vat on acc.vatrate_id = vat.id "
					+ "where coa.OWNER_ID = ? and acc.IS_SALE = 1";

	private static String selectSQLBankAccounts = 
			"select acc.*, vat.id as vatId, vat.code as vatCode, vat.rate as vatRate " 
					+ "from Account acc inner join ChartOfAccounts as coa on acc.COA_ID = coa.ID "
					+ "left join VATRate vat on acc.vatrate_id = vat.id "
					+ "where coa.OWNER_ID = ? and acc.code like '512%'";

	private static String selectSQLSubAccountsByOwner = 
			"select acc.*, vat.id as vatId, vat.code as vatCode, vat.rate as vatRate " 
					+ "from Account acc inner join ChartOfAccounts as coa on acc.COA_ID = coa.ID "
					+ "left join VATRate vat on acc.vatrate_id = vat.id "
					+ "where coa.OWNER_ID = ? and acc.ACC_TYPE = " + AccAccount.AccountType.ACCOUNT.getTypeId();

	private static String selectSQLPatternAccountsByOwner = 
			"select acc.*, vat.id as vatId, vat.code as vatCode, vat.rate as vatRate " 
					+ "from Account acc inner join ChartOfAccounts as coa on acc.COA_ID = coa.ID "
					+ "left join VATRate vat on acc.vatrate_id = vat.id "
					+ "where coa.OWNER_ID = ? and acc.ACC_TYPE = " + AccAccount.AccountType.PATTERN.getTypeId();

	private static String selectSQLByIdFull = 
			//		"select acc.*, vat.id as vatId, vat.code as vatCode, vat.rate as vatRate " 
			//		+ "from Account acc left join VATRate vat on acc.vatrate_id = vat.id "
			//		+ "where acc.id = ?";
			"select acc.* from Account acc where acc.id = ?";

	private static String selectSQLGOAAccount = 
			"select goa_acc.*, vat.id as vatId, vat.rate, vat.code as vatCode from " 
					+ "GOAAccounts goa_acc left join Account acc on goa_acc.ACC_ID = acc.ID "
					+ "left join VATRate vat on acc.VATRATE_ID = vat.ID "
					+ "where goa_acc.GOA_ID = ? order by goa_acc.s_index asc";

	private static String selectSQLGOAActiveAccount = 
			"select goa_acc.*, vat.id as vatId, vat.rate, vat.code as vatCode from " 
					+ "GOAAccounts goa_acc left join Account acc on goa_acc.ACC_ID = acc.ID "
					+ "left join VATRate vat on acc.VATRATE_ID = vat.ID "
					+ "where goa_acc.GOA_ID = ? and goa_acc.LAST_ACTIVE_DAY >= ? order by goa_acc.s_index asc";

	private static String deactivateSQLGOAAccount = 
			"update GOAAccounts set LAST_ACTIVE_DAY = ? where id = ?";

	private static String selectSQLAttributesByOwner = 
			"SELECT attr.*, goaAcc.ACC_CODE_IN_GROUP, goaAcc.ACC_DESCRIPTION_IN_GROUP from AccountAttributes attr " 
					+ " inner join GOAAccounts goaAcc on attr.GOA_ACC_ID = goaAcc.ID " 
					+ " inner join GroupOfAccounts goa on goaAcc.GOA_ID = goa.ID "
					+ " where goa.OWNER_ID = ? order by goaAcc.ACC_CODE_IN_GROUP, goaAcc.ACC_DESCRIPTION_IN_GROUP";

	private static String insertSQLAttribute = "insert into AccountAttributes "
			+ "(NAME, XTYPE, REQUIRED, GOA_ACC_ID) "
			+ " values (?, ?, ?, ?)";

	private static String updateSQLAttribute = "update AccountAttributes "
			+ " set NAME = ?, XTYPE = ?, REQUIRED = ?, GOA_ACC_ID = ? where id = ?";

	private static String deleteSQLAttribute = "delete from AccountAttributes where id = ?";

	private static String selectSQLAttributes = 
			"select * from AccountAttributes where GOA_ACC_ID = ?";

	// start balance management

//	private static String selectSQLAccountBalance = 
//			"select * from AccountBalance where ACCOUNT_ID = ? and YEAR = ? and TYPE = ?";
	
	private static String selectSQLInitialBalanceByAccount = 
			"select * from AccountBalance where account_id = ? and is_initial = 1";
	
	private static String selectSQLInitialBalanceByOwner = 
			"select * from AccountBalance where owner_id = ? and is_initial = 1";

	private static String insertSQLInitialBalance = 
			"insert into AccountBalance (ACCOUNT_ID, YEAR, TYPE, DEBIT_AMOUNT, CREDIT_AMOUNT, DATE_CREATED, OWNER_ID, IS_INITIAL) "
					+ " values (?, ?, ?, ?, ?, ?, ?, 1)";

	private static String updateSQLInitialBalance = 
			"update AccountBalance set DEBIT_AMOUNT = ?, CREDIT_AMOUNT = ?, DATE_CREATED = ?, YEAR = ? "
					+ " where ACCOUNT_ID = ? and IS_INITIAL = 1";

	// end balance management

	private static String selectTip = "SELECT Tip FROM Tip Where ID = 1 order by ID ASC";
	
	private static String updateTip = "Update Tip Set Tip = ? Where ID = 1";
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public AccountDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	/**
	 * Load accounts, but not patterns from specified <code>coaID</code>.
	 * 
	 * @param coaID
	 * @return
	 * @throws AEException
	 * @throws JSONException
	 */
	public JSONArray loadAccounts(long coaID) throws AEException, JSONException {
		JSONArray accountsArray = new JSONArray();
		JSONObject account = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAccounts);
			ps.setLong(1, coaID);
			rs = ps.executeQuery();
			while(rs.next()) {
				account = new JSONObject();
				buildAccount(account, rs);
				account.put("dbState", 0);
				accountsArray.put(account);
			}
			return accountsArray;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	/**
	 * Load accounts, but not patterns from specified <code>coaID</code>.
	 * 
	 * @param coaID
	 * @return
	 * @throws AEException
	 * @throws JSONException
	 */
	public JSONArray loadBalanceAccounts(long coaID) throws AEException, JSONException { 
		JSONArray accountsArray = new JSONArray();
		JSONObject account = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLBalanceAccounts);
			ps.setLong(1, coaID);
			ps.setString(2, "1%");
			ps.setString(3, "2%");
			ps.setString(4, "3%");
			ps.setString(5, "4%");
			ps.setString(6, "5%");
			rs = ps.executeQuery();
			while(rs.next()) {
				account = new JSONObject();
				buildAccount(account, rs);
				account.put("dbState", 0);
				accountsArray.put(account);
			}
			return accountsArray;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	/**
	 * Load all accounts for specified <code>ownerId</code>.
	 * 
	 * @param coaID
	 * @return
	 * @throws AEException
	 * @throws JSONException
	 */
	public JSONArray loadAccountsByOwner(long ownerId) throws AEException, JSONException {
		JSONArray accountsArray = new JSONArray();
		JSONObject account = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAccountsByOwner);
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			while(rs.next()) {
				account = new JSONObject();
				buildAccount(account, rs);
				account.put("dbState", 0);
				accountsArray.put(account);
			}
			return accountsArray;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public JSONArray loadPatterns(long coaID) throws AEException, JSONException {
		JSONArray accountsArray = new JSONArray();
		JSONObject account = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLPatternAccounts);
			ps.setLong(1, coaID);
			rs = ps.executeQuery();
			while(rs.next()) {
				account = new JSONObject();
				buildAccount(account, rs);
				account.put(AEDomainObject.JSONKey.dbState.name(), 0);
				accountsArray.put(account);
			}
			return accountsArray;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public JSONArray loadActivePatterns(long coaID) throws AEException, JSONException {
		JSONArray accountsArray = new JSONArray();
		JSONObject account = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLPatternActiveAccounts);
			ps.setLong(1, coaID);
			rs = ps.executeQuery();
			while(rs.next()) {
				account = new JSONObject();
				buildAccount(account, rs);
				account.put(AEDomainObject.JSONKey.dbState.name(), 0);
				accountsArray.put(account);
			}
			return accountsArray;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public JSONArray loadAccounts(long ownerId, String codeLike) throws AEException, JSONException {
		JSONArray accountsArray = new JSONArray();
		JSONObject account = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = selectSQLDescrByOwnerAndCodeWhere;
			sql += codeLike;
			ps = getAEConnection().prepareStatement(sql);
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			while(rs.next()) {
				account = new JSONObject();
				buildAccountDescr(account, rs);
				account.put("dbState", 0);
				accountsArray.put(account);
			}
			return accountsArray;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public AEDescriptor loadDescrByOwnerAndDiversAccCode(long ownerId) throws AEException, JSONException {
		AEDescriptor account = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = selectSQLDescrByOwnerAndDiversAccCode;
			ps = getAEConnection().prepareStatement(sql);
			ps.setLong(1, ownerId);
			ps.setLong(2, ownerId);
			rs = ps.executeQuery();
			while(rs.next()) {
				// acc.id, acc.code, acc.name
				account = AccAccount.lazyDescriptor(rs.getLong("id"));
				account.setCode(rs.getString("code"));
				account.setName(rs.getString("name"));
			}
			return account;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public JSONArray loadCashAccounts(long ownerId) throws AEException, JSONException {
		JSONArray accountsArray = new JSONArray();
		JSONObject account = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCashAccounts);
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			while(rs.next()) {
				account = new JSONObject();
				buildAccount(account, rs);
				account.put("dbState", 0);
				accountsArray.put(account);
			}
			return accountsArray;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public JSONArray loadSubAccounts(long ownerId, String accCode) throws AEException, JSONException {
		JSONArray accountsArray = new JSONArray();
		JSONObject account = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLSubAccounts);
			ps.setLong(1, ownerId);
			ps.setString(2, accCode + "%");
			rs = ps.executeQuery();
			while(rs.next()) {
				account = new JSONObject();
				buildAccount(account, rs);
				account.put("dbState", 0);
				accountsArray.put(account);
			}
			return accountsArray;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public JSONArray loadSupplyAccounts(long ownerId) throws AEException, JSONException {
		JSONArray accountsArray = new JSONArray();
		JSONObject account = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLSupplyAccounts);
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			while(rs.next()) {
				account = new JSONObject();
				buildAccount(account, rs);
				account.put("dbState", 0);
				accountsArray.put(account);
			}
			return accountsArray;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public JSONArray loadSaleAccounts(long ownerId) throws AEException, JSONException {
		JSONArray accountsArray = new JSONArray();
		JSONObject account = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLSaleAccounts);
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			while(rs.next()) {
				account = new JSONObject();
				buildAccount(account, rs);
				account.put("dbState", 0);
				accountsArray.put(account);
			}
			return accountsArray;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public JSONArray loadSubAccountsByOwner(long ownerId) throws AEException, JSONException {
		JSONArray accountsArray = new JSONArray();
		JSONObject account = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLSubAccountsByOwner);
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			while(rs.next()) {
				account = new JSONObject();
				buildAccount(account, rs);
				account.put("dbState", 0);
				accountsArray.put(account);
			}
			return accountsArray;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public JSONArray loadPatternAccountsByOwner(long ownerId) throws AEException, JSONException {
		JSONArray accountsArray = new JSONArray();
		JSONObject account = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLPatternAccountsByOwner);
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			while(rs.next()) {
				account = new JSONObject();
				buildAccount(account, rs);
				account.put("dbState", 0);
				accountsArray.put(account);
			}
			return accountsArray;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public JSONObject loadAccountByIdFull(long accId) throws AEException, JSONException {
		JSONObject account = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLByIdFull);
			ps.setLong(1, accId);
			rs = ps.executeQuery();
			if(rs.next()) {
				account = new JSONObject();
				buildAccount(account, rs);
				account.put("dbState", 0);
			}
			return account;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public void insertAccount(JSONObject account) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);
			buildAccount(account, ps, 0);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				account.put("id", id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			account.put("dbState", 0);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} catch (JSONException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}

	@SuppressWarnings("resource")
	public void insertAccounts(JSONArray accounts, JSONArray coas) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);

			for (int i = 0; i < accounts.length(); i++) {
				JSONObject account = accounts.getJSONObject(i);
				buildAccount(account, ps, 0);

				// execute
				ps.executeUpdate();

				// set generated key
				rs = ps.getGeneratedKeys();
				if (rs.next()) {
					// propagate generated key
					long id = rs.getLong(1);
					account.put("id", id);
				} else {
					throw new AEException(getClass().getName() + "::insert: No keys were generated");
				}

				// set view state
				account.put("dbState", 0);

				if (account.getLong(AccAccount.JSONKey.accType.name()) != AccAccount.AccountType.PATTERN.getTypeId()) {
					for (int j = 0; j < coas.length(); j++) {
						JSONObject acc = new JSONObject(account, JSONObject.getNames(account));
						JSONObject parent = new JSONObject();
						parent.put("id", account.getLong(AEDomainObject.JSONKey.id.name()));
						parent.put("code", account.getLong(AEDomainObject.JSONKey.code.name()));
						acc.put("parent", parent);
						acc.put(AccAccount.JSONKey.coaId.name(), coas.getJSONObject(j).getLong(AEDomainObject.JSONKey.id.name()));

						buildAccount(acc, ps, 0);

						// execute
						ps.executeUpdate();

						// set generated key
						rs = ps.getGeneratedKeys();
						if (rs.next()) {
							// propagate generated key
							long id = rs.getLong(1);
							acc.put("id", id);
						} else {
							throw new AEException(getClass().getName() + "::insert: No keys were generated");
						}

						// set view state
						acc.put("dbState", 0);
					}
				}
			}
		} catch (SQLException e) {
			throw new AEException(e);
		} catch (JSONException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}

	public void insertGOAAccount(JSONObject account) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLGOAAccount);
			buildGOAAccount(account, ps, 0);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				account.put("id", id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			account.put("dbState", 0);

			// process attributes
			JSONArray attrJSONArray = account.optJSONArray("attributes");
			if(attrJSONArray != null) {
				account.put(
						"attributes", 
						processAttributes(attrJSONArray, account.optLong("id")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} catch (JSONException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}

	public void deleteAccount(JSONObject account) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQL);

			// build statement
			int i = 1;
			ps.setLong(i++, account.getLong("id"));

			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} catch (JSONException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	public void deleteAccounts(JSONArray accounts) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQL);
			for (int j = 0; j < accounts.length(); j++) {
				JSONObject account = accounts.getJSONObject(j);
				// build statement
				int i = 1;
				ps.setLong(i++, account.getLong("id"));

				// execute
				ps.executeUpdate();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} catch (JSONException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	public void deleteGOAAccount(JSONObject account) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQLGOAAccount);

			// build statement
			ps.setLong(1, account.getLong("id"));

			// execute
			ps.executeUpdate();

			// process attributes
			JSONArray attrJSONArray = account.optJSONArray("attributes");
			if(attrJSONArray != null) {
				account.put(
						"attributes", 
						processAttributes(attrJSONArray, account.optLong("id")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} catch (JSONException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	public void deleteAccountsByCOA(long coaId) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQLByCOAId);

			// build statement
			int i = 1;
			ps.setLong(i++, coaId);

			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	public int buildAccount(JSONObject account, PreparedStatement ps, int i) throws SQLException, AEException, JSONException {

		//COA_ID
		if(account.has("coaId")) {
			ps.setLong(++i, account.getLong("coaId"));
		} else {
			ps.setNull(++i, Types.INTEGER);
		}

		//PARENT_ID
		if(account.has(AccAccount.JSONKey.parent.name())) {
			JSONObject parent = account.getJSONObject(AccAccount.JSONKey.parent.name());
			if(parent.has(AEDomainObject.JSONKey.id.name()) && parent.getLong(AEDomainObject.JSONKey.id.name()) > 0) {
				ps.setLong(++i, parent.getLong(AEDomainObject.JSONKey.id.name()));
			} else {
				ps.setNull(++i, Types.INTEGER);
			}
		} else {
			ps.setNull(++i, Types.INTEGER);
		}

		//CODE
		if(account.has("code")) {
			ps.setString(++i, account.getString("code"));
		} else {
			ps.setNull(++i, Types.NVARCHAR);
		}
		//NAME
		if(account.has("name")) {
			ps.setString(++i, account.getString("name"));
		} else {
			ps.setNull(++i, Types.NVARCHAR);
		}
		//DESCRIPTION
		if(account.has("description")) {
			ps.setString(++i, account.getString("description"));
		} else {
			ps.setNull(++i, Types.NVARCHAR);
		}
		//OWNER_ID
		if(account.has("ownerId") && account.getLong("ownerId") > 0) {
			ps.setLong(++i, account.getLong("ownerId"));
		} else {
			ps.setNull(++i, Types.INTEGER);
		}
		//IS_SYSTEM
		if(account.has("system")) {
			ps.setInt(++i, account.getBoolean("system") ? 1 : 0);
		} else {
			ps.setNull(++i, Types.INTEGER);
		}
		//IS_ACTIVE
		if(account.has("active")) {
			ps.setInt(++i, account.getBoolean("active") ? 1 : 0);
		} else {
			ps.setNull(++i, Types.INTEGER);
		}
		//IS_MODIFIABLE
		if(account.has("modifiable")) {
			ps.setInt(++i, account.getBoolean("modifiable") ? 1 : 0);
		} else {
			ps.setNull(++i, Types.INTEGER);
		}

		//ACC_TYPE
		ps.setLong(++i, AccAccount.AccountType.valueOf(account.optLong(AccAccount.JSONKey.accType.name())).getTypeId());

		//VATRATE_ID
		if(account.has("vat")) {
			try {
				JSONObject vatRate = account.getJSONObject("vat");
				ps.setLong(++i, vatRate.getLong("id"));
			} catch (JSONException e) {
				ps.setNull(++i, Types.INTEGER);
			}
		} else {
			ps.setNull(++i, Types.INTEGER);
		}

		//IS_CASH
		if(account.has("isCash")) {
			ps.setInt(++i, account.getBoolean("isCash") ? 1 : 0);
		} else {
			ps.setNull(++i, Types.INTEGER);
		}

		//IS_BANK
		if(account.has("isBank")) {
			ps.setInt(++i, account.getBoolean("isBank") ? 1 : 0);
		} else {
			ps.setNull(++i, Types.INTEGER);
		}

		//IS_SUPPLY
		if(account.has("isSupply")) {
			ps.setInt(++i, account.getBoolean("isSupply") ? 1 : 0);
		} else {
			ps.setNull(++i, Types.INTEGER);
		}

		//IS_SALE
		if(account.has("isSale")) {
			ps.setInt(++i, account.getBoolean("isSale") ? 1 : 0);
		} else {
			ps.setNull(++i, Types.INTEGER);
		}

		// return the current ps position 
		return i;
	}

	public int buildGOAAccount(JSONObject account, PreparedStatement ps, int i) throws SQLException, AEException, JSONException {
		// GOA_ID
		psSetLong(ps, ++i, account, "coaId");
		// ACC_ID
		psSetLong(ps, ++i, account, "accId");
		// ACC_CODE_IN_GROUP
		psSetString(ps, ++i, account, "code");
		// ACC_NAME_IN_GROUP
		psSetString(ps, ++i, account, "name");
		// ACC_DESCRIPTION_IN_GROUP
		psSetString(ps, ++i, account, "description");
		// S_INDEX
		psSetInt(ps, ++i, account, "sIndex");

		// return the current ps position 
		return i;
	}

	public void buildAccount(JSONObject account, ResultSet rs) throws SQLException, AEException, JSONException {

		// ID
		account.put("id", rs.getLong("ID"));

		// COA_ID
		account.put("coaId", rs.getLong("COA_ID"));

		// PARENT_ID
		long parentId = rs.getLong("PARENT_ID");
		if(!rs.wasNull()) {
			JSONObject parent = new JSONObject();
			account.put(AccAccount.JSONKey.parent.name(), parent);

			parent.put(AEDomainObject.JSONKey.id.name(), parentId);
			try {
				parent.put(AEDomainObject.JSONKey.code.name(), rs.getString("modelAccCode"));
			} catch (Exception e){}
		}

		// CODE
		account.put("code", rs.getString("CODE"));

		// NAME
		account.put("name", rs.getString("NAME"));

		// DESCRIPTION
		account.put("description", rs.getString("DESCRIPTION"));

		//OWNER_ID
		account.put("ownerId", rs.getString("OWNER_ID"));

		//ACC_TYPE
		account.put(AccAccount.JSONKey.accType.name(), rs.getString("ACC_TYPE"));

		//IS_SYSTEM
		account.put("system", rs.getString("IS_SYSTEM"));

		//IS_ACTIVE
		account.put("active", rs.getInt("IS_ACTIVE") != 0);

		//IS_MODIFIABLE
		account.put("modifiable", rs.getInt("IS_MODIFIABLE") != 0);

		//vat
		long vatID = rs.getLong("VATRATE_ID");
		if(!rs.wasNull()) {
			JSONObject vat = new JSONObject();
			vat.put("id", vatID);
			account.put("vat", vat);

			/**
			 * create vat
			 */
			try {
				vat.put("id", rs.getLong("vatId"));
			} catch(Exception e) {}

			try {
				vat.put("code", rs.getString("vatCode"));
			} catch(Exception e) {}

			try {
				vat.put("rate", rs.getString("vatRate"));
			} catch(Exception e) {}

			/**
			 * 
			 */
			try {
				account.put("vatId", rs.getLong("vatId"));
			} catch(Exception e) {}

			try {
				account.put("vatCode", rs.getString("vatCode")); 
			} catch(Exception e) {}

			try {
				account.put("vatRate", rs.getString("vatRate"));
			} catch(Exception e) {}
		}

		//IS_CASH
		account.put("isCash", rs.getInt("IS_CASH") != 0);

		//IS_BANK
		account.put("isBank", rs.getInt("IS_BANK") != 0);

		//IS_SUPPLY
		account.put("isSupply", rs.getInt("IS_SUPPLY") != 0);

		//IS_SALE
		account.put("isSale", rs.getInt("IS_SALE") != 0);
	}

	public void buildAccountDescr(JSONObject account, ResultSet rs) throws SQLException, AEException, JSONException {
		// ID
		account.put("id", rs.getLong("ID"));

		// CODE
		account.put("code", rs.getString("CODE"));

		// NAME
		account.put("name", rs.getString("NAME"));
	}

	public void updateAccount(JSONObject account) throws AEException, JSONException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			int i = buildAccount(account, ps, 0);
			ps.setLong(++i, account.getLong("id"));

			// execute
			ps.executeUpdate();

			// set view state
			account.put("dbState", 0);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	/**
	 * Updates specified <code>accounts</code> without validation
	 * 
	 * @param accounts
	 * @throws AEException
	 * @throws JSONException
	 */
	public void updateAccounts(Map<JSONObject, JSONArray> accounts) throws AEException, JSONException {
		PreparedStatement ps = null;
		PreparedStatement ps1 = null;
		try {
			// create statements
			ps = getAEConnection().prepareStatement(updateSQLAccountStrict);
			ps1 = getAEConnection().prepareStatement(updateSQLAccountIsActive);

			//itarate over every pair (model/accs)
			Iterator<Entry<JSONObject, JSONArray>> iterator = accounts.entrySet().iterator();
			while(iterator.hasNext()) {
				Entry<JSONObject, JSONArray> pair = iterator.next();
				JSONObject model = pair.getKey();

				// build statement
				int i = 0;
				ps.setString(++i, model.getString(AEDomainObject.JSONKey.code.name()));
				ps.setString(++i, model.getString(AEDomainObject.JSONKey.name.name()));
				ps.setString(++i, model.getString(AEDomainObject.JSONKey.description.name()));
				ps.setInt(++i, model.getBoolean(AEDomainObject.JSONKey.active.name()) ? 1 : 0);

				ps.setLong(++i, model.getLong("id"));

				// execute
				ps.executeUpdate();

				// set view state
				model.put("dbState", 0);

				// propagate updates
				if(AccAccount.AccountType.PATTERN.getTypeId() != model.getLong(AccAccount.JSONKey.accType.name())) {
					// propagate account updates
					JSONArray accs = pair.getValue(); 
					if(accs != null) {
						for (int j = 0; j < accs.length(); j++) {
							JSONObject account = accs.getJSONObject(j);

							ps.setLong(i, account.getLong("id"));

							// execute
							ps.executeUpdate();

							// set view state
							account.put("dbState", 0);
						}
					}
				} else {
					// propagate pattern updates (only active field)
					JSONArray accs = pair.getValue(); 
					if(accs != null) {
						for (int j = 0; j < accs.length(); j++) {
							JSONObject account = accs.getJSONObject(j);

							ps1.setInt(1, model.getBoolean(AEDomainObject.JSONKey.active.name()) ? 1 : 0);
							ps1.setLong(2, account.getLong("id"));

							// execute
							ps1.executeUpdate();

							// set view state
							account.put("dbState", 0);
						}
					}
				}
			}
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(ps1);
			close();
		}
	}

	public void updateGOAAccount(JSONObject account) throws AEException, JSONException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQLGOAAccount);

			// build statement
			int i = buildGOAAccount(account, ps, 0);
			ps.setLong(++i, account.getLong("id"));

			// execute
			ps.executeUpdate();

			// set view state
			account.put("dbState", 0);

			// process attributes
			JSONArray attrJSONArray = account.optJSONArray("attributes");
			if(attrJSONArray != null) {
				account.put(
						"attributes", 
						processAttributes(attrJSONArray, account.optLong("id")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	public JSONArray loadGOAAccounts(long goaId) throws AEException, JSONException {
		JSONArray accountsArray = new JSONArray();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLGOAAccount);
			ps.setLong(1, goaId);
			rs = ps.executeQuery();
			while(rs.next()) {
				JSONObject account = new JSONObject();
				buildGOAAccount(account, rs);
				JSONArray attributes = loadAttributes(account.optLong("id"));
				if(attributes != null && attributes.length() > 0) {
					account.put("attributes", attributes);
				}
				account.put("dbState", 0);
				accountsArray.put(account);
			}
			return accountsArray;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public void deactivateGOAAccount(JSONObject account, Date lastActiveDate) throws AEException, JSONException {
		PreparedStatement ps = null;
		try {
			ps = getAEConnection().prepareStatement(deactivateSQLGOAAccount);
			ps.setLong(1,  account.getLong("id"));
			ps.setDate(2, AEPersistentUtil.getSQLDate(lastActiveDate));
			ps.executeUpdate();

			// set view state
			account.put("dbState", 0);

			// set lastActiveDay
			account.put("lastActiveDay", AEDateUtil.formatToSystem(lastActiveDate));

		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	public JSONArray loadGOAActiveAccounts(long goaId, Date activeAfterDate) throws AEException, JSONException {
		JSONArray accountsArray = new JSONArray();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLGOAActiveAccount);
			ps.setLong(1, goaId);
			ps.setDate(2, AEPersistentUtil.getSQLDate(activeAfterDate));
			rs = ps.executeQuery();
			while(rs.next()) {
				JSONObject account = new JSONObject();
				buildGOAAccount(account, rs);
				JSONArray attributes = loadAttributes(account.optLong("id"));
				if(attributes != null && attributes.length() > 0) {
					account.put("attributes", attributes);
				}
				account.put("dbState", 0);
				accountsArray.put(account);
			}
			return accountsArray;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	/**
	 * Loads GOA Accounts for speciified <code>goaId</code> 
	 * having attriutes.
	 * 
	 * 
	 * @param goaId
	 * @return
	 * @throws AEException
	 * @throws JSONException
	 */
	public JSONArray loadAttributesByOwner(long ownerId) throws AEException, JSONException {
		JSONArray attributesArray = new JSONArray();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAttributesByOwner);
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			while(rs.next()) {
				JSONObject attribute = new JSONObject();
				buildAttributeExt(attribute, rs);
				attribute.put("dbState", 0);
				attributesArray.put(attribute);
			}
			return attributesArray;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public void buildGOAAccount(JSONObject account, ResultSet rs) throws SQLException, AEException, JSONException {
		// ID 
		account.put("id", rs.getLong("ID"));
		// GOA_ID
		account.put("coaId", rs.getLong("GOA_ID"));
		// ACC_ID
		account.put("accId", rs.getLong("ACC_ID"));
		// ACC_CODE_IN_GROUP
		account.put("code", rs.getString("ACC_CODE_IN_GROUP"));
		// ACC_NAME_IN_GROUP
		account.put("name", rs.getString("ACC_NAME_IN_GROUP"));
		// ACC_DESCRIPTION_IN_GROUP
		account.put("description", rs.getString("ACC_DESCRIPTION_IN_GROUP"));
		// S_INDEX
		account.put("sIndex", rs.getLong("S_INDEX"));
		// LAST_ACTIVE_DAY
		account.put("lastActiveDay", rs.getDate("LAST_ACTIVE_DAY"));

		// vat: put only RATE
		JSONObject vat =  new JSONObject();
		vat.put("id", rs.getLong("vatId"));
		vat.put("rate", rs.getDouble("rate"));
		vat.put("code", rs.getString("vatCode"));

		account.put("vat", vat);
	}

	public int buildAttribute(JSONObject attribute, PreparedStatement ps, int i) throws SQLException, AEException, JSONException {
		// NAME
		psSetString(ps, ++i, attribute, "name");
		// XTYPE
		psSetString(ps, ++i, attribute, "xType");
		// REQUIRED
		psSetBoolean(ps, ++i, attribute, "required");
		// GOA_ACC_ID
		psSetLong(ps, ++i, attribute, "accId");

		// return the current ps position 
		return i;
	}

	public void buildAttribute(JSONObject attribute, ResultSet rs) throws SQLException, AEException, JSONException {
		// ID
		attribute.put("id", rs.getLong("ID"));

		// NAME
		attribute.put("name", rs.getString("NAME"));

		// XTYPE
		attribute.put("xType", rs.getString("XTYPE"));

		// REQUIRED
		attribute.put("required", rs.getBoolean("REQUIRED"));

		// GOA_ACC_ID
		attribute.put("accId", rs.getLong("GOA_ACC_ID"));
	}

	public void buildAttributeExt(JSONObject attribute, ResultSet rs) throws SQLException, AEException, JSONException {
		buildAttribute(attribute, rs);

		// ACC_CODE_IN_GROUP
		String accCode = rs.getString("ACC_CODE_IN_GROUP");
		attribute.put("code", accCode);

		// ACC_DESCRIPTION_IN_GROUP
		String accDescr = rs.getString("ACC_DESCRIPTION_IN_GROUP");
		attribute.put("option", accDescr);

		String descr = new StringBuilder()
		.append(accCode)
		.append(" - ")
		.append(accDescr)
		.append(" - ")
		.append(attribute.opt("name")).toString();
		attribute.put("description", descr);
	}

	public void insertAttribute(JSONObject attribute) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLAttribute);
			buildAttribute(attribute, ps, 0);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				attribute.put("id", id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			attribute.put("dbState", 0);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} catch (JSONException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}

	public void updateAttribute(JSONObject attribute) throws AEException, JSONException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQLAttribute);

			// build statement
			int i = buildAttribute(attribute, ps, 0);
			ps.setLong(++i, attribute.getLong("id"));

			// execute
			ps.executeUpdate();

			// set view state
			attribute.put("dbState", 0);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	public void deleteAttribute(JSONObject attribute) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQLAttribute);

			// build statement
			int i = 1;
			ps.setLong(i++, attribute.getLong("id"));

			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} catch (JSONException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	public JSONArray processAttributes(JSONArray attributes, long goaAccId) throws AEException, JSONException {
		List<JSONObject> jsonList = new ArrayList<JSONObject>();
		for (int i = 0; i < attributes.length(); i++) {
			JSONObject attribute = attributes.getJSONObject(i);
			attribute.put("accId", goaAccId);
			jsonList.add(attribute);
		}

		JSONArray retAttributes = new JSONArray();
		for (Iterator<JSONObject> iterator = jsonList.iterator(); iterator.hasNext();) {
			JSONObject jsonObject = (JSONObject) iterator.next();
			if(jsonObject.has("dbState")) {
				int dbState = jsonObject.getInt("dbState");
				switch(dbState) {
				case 0: {
					retAttributes.put(jsonObject);
					break;
				}
				case 1: {
					insertAttribute(jsonObject);
					retAttributes.put(jsonObject);
					break;
				}
				case 2: {
					updateAttribute(jsonObject);
					retAttributes.put(jsonObject);
					break;
				}
				case 3: {
					deleteAttribute(jsonObject);
					iterator.remove();
					break;
				}
				}
			}
		}
		return retAttributes;
	}

	public JSONArray loadAttributes(long goaAccId) throws AEException, JSONException {
		JSONArray attributesArray = new JSONArray();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAttributes);
			ps.setLong(1, goaAccId);
			rs = ps.executeQuery();
			while(rs.next()) {
				JSONObject attribute = new JSONObject();
				buildAttribute(attribute, rs);
				attribute.put("dbState", 0);
				attributesArray.put(attribute);
			}
			return attributesArray;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public AccAccount loadById(AEDescriptor accDescr) throws AEException {
		AccAccount account = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLById);
			ps.setLong(1, accDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				account = new AccAccount();
				account.setID(accDescr.getID());
				account.setCode(rs.getString("code"));
				account.setName(rs.getString("name"));

				// remove dirty flag
				account.setView();
			}
			return account;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public JSONArray loadBankAccounts(long ownerId) throws AEException, JSONException {
		JSONArray accountsArray = new JSONArray();
		JSONObject account = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLBankAccounts);
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			while(rs.next()) {
				account = new JSONObject();
				buildAccount(account, rs);
				account.put("dbState", 0);
				accountsArray.put(account);
			}
			return accountsArray;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public JSONArray loadConcreteAccounts(long modelAccId, long ownerId) throws AEException, JSONException {
		JSONArray accountsArray = new JSONArray();
		JSONObject account = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLConcreteAccounts);
			ps.setLong(1, modelAccId);
			ps.setLong(2, ownerId);
			rs = ps.executeQuery();
			while(rs.next()) {
				account = new JSONObject();
				buildAccount(account, rs);
				account.put("dbState", 0);
				accountsArray.put(account);
			}
			return accountsArray;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public JSONArray loadConcreteSubAccountsByCode(String accCode, long ownerId) throws AEException, JSONException {
		JSONArray accountsArray = new JSONArray();
		JSONObject account = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLConcreteAccountsByCode);
			ps.setLong(1, ownerId);
			ps.setString(2, accCode + "%");
			rs = ps.executeQuery();
			while(rs.next()) {
				account = new JSONObject();
				buildAccount(account, rs);
				account.put("dbState", 0);
				accountsArray.put(account);
			}
			return accountsArray;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public void insertOrUpdateInitialBalance(JSONArray accBalances, int year, Date date, long tenantId) throws AEException {
		PreparedStatement pSelect = null;
		PreparedStatement pInsert = null;
		PreparedStatement pUpdate = null;
		ResultSet rs = null;
		try {
			if(accBalances != null && accBalances.length() > 0) {
				pSelect = getAEConnection().prepareStatement(selectSQLInitialBalanceByAccount);
				pInsert = getAEConnection().prepareStatement(insertSQLInitialBalance);
				pUpdate = getAEConnection().prepareStatement(updateSQLInitialBalance);
				for (int i = 0; i < accBalances.length(); i++) {
					JSONObject ab = accBalances.getJSONObject(i);

					// select
					// "select * from AccountBalance where id = ? and is_initial = 1";
					pSelect.setLong(1, ab.getLong("id"));
					rs = pSelect.executeQuery();
					boolean update = rs.next();
					AEConnection.close(rs);
					if(update) {
						pUpdate.setDouble(1, AEMath.doubleValue(JSONUtil.parseDoubleStrict(ab, "dtAmount")));
						pUpdate.setDouble(2, AEMath.doubleValue(JSONUtil.parseDoubleStrict(ab, "ctAmount")));
						pUpdate.setDate(3, AEPersistentUtil.getSQLDate(date));
						pUpdate.setInt(4, year);
						pUpdate.setLong(5, ab.getLong("id"));

						pUpdate.executeUpdate();
					} else {
						pInsert.setLong(1, ab.getLong("id"));
						pInsert.setInt(2, year);
						pInsert.setInt(3, AccountBalanceType.OPENING.ordinal());
						pInsert.setDouble(4, AEMath.doubleValue(JSONUtil.parseDoubleStrict(ab, "dtAmount")));
						pInsert.setDouble(5, AEMath.doubleValue(JSONUtil.parseDoubleStrict(ab, "ctAmount")));
						pInsert.setDate(6, AEPersistentUtil.getSQLDate(date));
						pInsert.setLong(7, tenantId);

						pInsert.executeUpdate();
					}
				}
			}
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(pSelect);
			AEConnection.close(pInsert);
			AEConnection.close(pUpdate);
			close();
		}
	}
	
	private static String insertSQLOpeningBalance = 
			"insert into AccountBalance (ACCOUNT_ID, YEAR, TYPE, DEBIT_AMOUNT, CREDIT_AMOUNT, DATE_CREATED, OWNER_ID, IS_INITIAL) "
					+ " values (?, ?, ?, ?, ?, ?, ?, 0)";
	public void insertOpeningBalances(AccAccountBalancesList accBalances, int year, Date date, long tenantId) throws AEException {
		PreparedStatement pInsert = null;
		ResultSet rs = null;
		try {
			pInsert = getAEConnection().prepareStatement(insertSQLOpeningBalance);
			for (AccAccountBalance ab : accBalances) {
				pInsert.setLong(1, ab.getAccAccount().getDescriptor().getID());
				pInsert.setInt(2, year);
				pInsert.setInt(3, AccountBalanceType.OPENING.ordinal());
				pInsert.setDouble(4, ab.getDebitTurnover());
				pInsert.setDouble(5, ab.getCreditTurnover());
				pInsert.setDate(6, AEPersistentUtil.getSQLDate(date));
				pInsert.setLong(7, tenantId);

				pInsert.executeUpdate();
			}
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(pInsert);
			close();
		}
	}
	
	private static String deleteSQLOpeningBalance = "delete from AccountBalance where owner_id = ? and year = ? and is_initial = 0";
	public void deleteOpeningBalances(AEDescriptor ownerDescr, int year) throws AEException {
		PreparedStatement ps = null;
		try {
			ps = getAEConnection().prepareStatement(deleteSQLOpeningBalance);
			ps.setLong(1, ownerDescr.getID());
			ps.setInt(2, year);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}

	public JSONArray loadInitialBalance(long tenantId) throws AEException {
		JSONArray bArr = new JSONArray();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLInitialBalanceByOwner);

			// select
			ps.setLong(1, tenantId);
			rs = ps.executeQuery();
			while(rs.next()) {
				JSONObject b = new JSONObject();
				// ACCOUNT_ID
				b.put(AEDomainObject.JSONKey.id.name(), rs.getLong("ACCOUNT_ID"));
				
				// YEAR
				b.put("year", rs.getInt("YEAR"));
				
				// TYPE
				b.put("type", rs.getInt("TYPE") == 0 ? AccountBalanceType.OPENING : AccountBalanceType.CLOSING);
				
				// DEBIT_AMOUNT
				b.put("dtAmount", rs.getDouble("DEBIT_AMOUNT"));
				
				// CREDIT_AMOUNT
				b.put("ctAmount", rs.getDouble("CREDIT_AMOUNT"));
				
				// DATE_CREATED
				b.put("date", AEDateUtil.formatToSystem(rs.getDate("DATE_CREATED")));
				
				// OWNER_ID
				b.put(AEDomainObject.JSONKey.ownerId.name(), rs.getLong("OWNER_ID"));
				
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
	
	private static String selectSQLAccountBudgetByOwnerAndYear = 
			"select * from AccountBudget where owner_id = ? and year = ?";
	
	public JSONArray loadBudget(long tenantId, int year) throws AEException, SQLException {
		JSONArray bArr = new JSONArray();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAccountBudgetByOwnerAndYear);

			// select
			ps.setLong(1, tenantId);
			ps.setInt(2, year);
			rs = ps.executeQuery();
			while(rs.next()) {
				JSONObject b = new JSONObject();
				
				// OWNER_ID
				b.put(AEDomainObject.JSONKey.ownerId.name(), rs.getLong("OWNER_ID"));

				// ACCOUNT_ID
				b.put(AEDomainObject.JSONKey.id.name(), rs.getLong("ACCOUNT_ID"));
				
				// YEAR
				b.put("year", rs.getInt("YEAR"));
				
				// AMOUNT
				b.put("amount", rs.getDouble("AMOUNT"));
				
				bArr.put(b);
			}
			return bArr;
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String selectSQLBudgetByAccountAndYear = 
			"select id from AccountBudget where account_id = ? and year = ?";

	private static String insertSQLBudget = 
			"insert into AccountBudget (OWNER_ID, ACCOUNT_ID, YEAR, AMOUNT) "
					+ " values (?, ?, ?, ?)";

	private static String updateSQLBudget = 
			"update AccountBudget set AMOUNT = ? where ACCOUNT_ID = ? and year = ?";
	
	public void insertOrUpdateBudget(JSONArray accBalances, int year, long tenantId) throws AEException, SQLException {
		PreparedStatement pSelect = null;
		PreparedStatement pInsert = null;
		PreparedStatement pUpdate = null;
		ResultSet rs = null;
		try {
			if(accBalances != null && accBalances.length() > 0) {
				pSelect = getAEConnection().prepareStatement(selectSQLBudgetByAccountAndYear);
				pInsert = getAEConnection().prepareStatement(insertSQLBudget);
				pUpdate = getAEConnection().prepareStatement(updateSQLBudget);
				for (int i = 0; i < accBalances.length(); i++) {
					JSONObject ab = accBalances.getJSONObject(i);

					long accId = ab.getLong("id");
					double amount = AEMath.doubleValue(JSONUtil.parseDoubleStrict(ab, "amount"));
					
					// select
					pSelect.setLong(1, accId);
					pSelect.setInt(2, year);
					rs = pSelect.executeQuery();
					boolean update = rs.next();
					AEConnection.close(rs);
					if(update) {
						pUpdate.setDouble(1, amount);
						pUpdate.setLong(2, accId);
						pUpdate.setInt(3, year);

						pUpdate.executeUpdate();
					} else {
						pInsert.setLong(1, tenantId);
						pInsert.setLong(2, accId);
						pInsert.setInt(3, year);
						pInsert.setDouble(4, amount);

						pInsert.executeUpdate();
					}
				}
			}
		} finally {
			AEConnection.close(rs);
			AEConnection.close(pSelect);
			AEConnection.close(pInsert);
			AEConnection.close(pUpdate);
			close();
		}
	}
	
	/**
	 * Load accounts, but not patterns from specified <code>coaID</code>.
	 * 
	 * @param coaID
	 * @return
	 * @throws AEException
	 * @throws JSONException
	 */
	private static String selectSQLBudgetAccounts = 
			"select acc.*, vat.code as vatCode, modelAcc.code as modelAccCode from Account acc "
					+ " left join VATRate vat on acc.vatrate_id = vat.id "
					+ " left join Account modelAcc on acc.PARENT_ID = modelAcc.ID "
					+ " where acc.coa_id = ? and acc.acc_type = " + AccAccount.AccountType.ACCOUNT.getTypeId() + ""
					+ " and (acc.code like ? or acc.code like ?) order by acc.code asc ";
	public JSONArray loadBudgetAccounts(long coaID) throws AEException, JSONException { 
		JSONArray accountsArray = new JSONArray();
		JSONObject account = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLBudgetAccounts);
			ps.setLong(1, coaID);
			ps.setString(2, "6%");
			ps.setString(3, "7%");
			rs = ps.executeQuery();
			while(rs.next()) {
				account = new JSONObject();
				buildAccount(account, rs);
				account.put("dbState", 0);
				accountsArray.put(account);
			}
			return accountsArray;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public String loadTip() throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		String tip = AEStringUtil.EMPTY_STRING;
		try {
			ps = getAEConnection().prepareStatement(selectTip);
			rs = ps.executeQuery();
			if(rs.next()) {
				tip = rs.getString("tip");
			}
			return tip;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void saveTip(String tip) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateTip);

			// build statement
			ps.setString(1, tip);

			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
}
