package eu.agileeng.domain.cefra.n11580_03;

import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;

public class BudgetRealizationDataSource extends ReportDataSource {

	/**
	 * 
	 */
	private static final long serialVersionUID = -74573246596030992L;
	
	private JSONArray accounts;
	
	private TreeMap<String, ArrayList<String>> accountMap;
	
	public BudgetRealizationDataSource() {
		
	}
	
	public BudgetRealizationDataSource(JSONArray accounts) {
		this.accounts = accounts;
	}

	/**
	 * @return the accounts
	 */
	public JSONArray getAccounts() {
		return accounts;
	}

	/**
	 * @param accounts the accounts to set
	 */
	public void setAccounts(JSONArray accounts) {
		this.accounts = accounts;
	}

	/**
	 * Returns <b>accumulated budget amount</b> for specified <code>accCode</code>.
	 * 
	 * @param accCode
	 * @return
	 */
	public double getBudgetAmount(String accCode) {
		double amount = 0.0;
		if(accounts != null) {
			for (int i = 0; i < accounts.length(); i++) {
				JSONObject acc = accounts.getJSONObject(i);
				String _accCode = acc.optString(AEDomainObject.JSONKey.code.name(), AEStringUtil.EMPTY_STRING);
				if(!AEStringUtil.isEmpty(_accCode) && _accCode.startsWith(accCode)) {
					amount += acc.optDouble("budgetAmount", 0.0);
				}
			}
		}
		return amount;
	}
	
	/**
	 * Returns <b>accumulated budget amount</b>.
	 * 
	 * @param accCode
	 * @return
	 */
	public double getBudgetAmount() {
		double amount = 0.0;
		if(accounts != null) {
			for (int i = 0; i < accounts.length(); i++) {
				JSONObject acc = accounts.getJSONObject(i);
				amount += acc.optDouble("budgetAmount", 0.0);
			}
		}
		return amount;
	}
	
	/**
	 * Returns <b>accumulated real amount</b> for specified <code>accCode</code>.
	 * 
	 * 
	 * @param accCode
	 * @return
	 */
	public double getRealAmount(String accCode) {
		double amount = 0.0;
		if(accounts != null) {
			for (int i = 0; i < accounts.length(); i++) {
				JSONObject acc = accounts.getJSONObject(i);
				String _accCode = acc.optString(AEDomainObject.JSONKey.code.name(), AEStringUtil.EMPTY_STRING);
				if(!AEStringUtil.isEmpty(_accCode) && _accCode.startsWith(accCode)) {
					amount += acc.optDouble("realAmount", 0.0);
				}
			}
		}
		return amount;
	}
	
	/**
	 * Returns <b>accumulated real amount</b>.
	 * 
	 * 
	 * @param accCode
	 * @return
	 */
	public double getRealAmount() {
		return getRealAmount(this.accounts);
	}
	
	/**
	 * Returns <b>accumulated real amount</b> for specified <code>JSONArray</code>.
	 * 
	 * 
	 * @param accCode
	 * @return
	 */
	public static double getRealAmount(JSONArray accounts) {
		double amount = 0.0;
		if(accounts != null) {
			for (int i = 0; i < accounts.length(); i++) {
				JSONObject acc = accounts.getJSONObject(i);
				amount += acc.optDouble("realAmount", 0.0);
			}
		}
		return amount;
	}
	
	/**
	 * Returns <b>accumulated budget amount</b> for specified <code>JSONArray</code>.
	 * 
	 * 
	 * @param accCode
	 * @return
	 */
	public static double getBudgetAmount(JSONArray accounts) {
		double amount = 0.0;
		if(accounts != null) {
			for (int i = 0; i < accounts.length(); i++) {
				JSONObject acc = accounts.getJSONObject(i);
				amount += acc.optDouble("budgetAmount", 0.0);
			}
		}
		return amount;
	}
	
	/**
	 * Returns <b>accumulated estimated amount</b> for specified <code>JSONArray</code>.
	 * 
	 * 
	 * @param accCode
	 * @return
	 */
	public static double getEstimatedAmount(JSONArray accounts) {
		double amount = 0.0;
		if(accounts != null) {
			for (int i = 0; i < accounts.length(); i++) {
				JSONObject acc = accounts.getJSONObject(i);
				amount += acc.optDouble("estimatedAmount", 0.0);
			}
		}
		return amount;
	}
	
	/**
	 * Returns <b>accumulated estimated amount</b> for specified <code>accCode</code>.
	 * 
	 * 
	 * @param accCode
	 * @return
	 */
	public double getEstimatedAmount(String accCode) {
		double amount = 0.0;
		if(accounts != null) {
			for (int i = 0; i < accounts.length(); i++) {
				JSONObject acc = accounts.getJSONObject(i);
				String _accCode = acc.optString(AEDomainObject.JSONKey.code.name(), AEStringUtil.EMPTY_STRING);
				if(!AEStringUtil.isEmpty(_accCode) && _accCode.startsWith(accCode)) {
					amount += acc.optDouble("estimatedAmount", 0.0);
				}
			}
		}
		return amount;
	}
	
	/**
	 * Returns <b>accumulated estimated amount</b>.
	 * 
	 * 
	 * @param accCode
	 * @return
	 */
	public double getEstimatedAmount() {
		double amount = 0.0;
		if(accounts != null) {
			for (int i = 0; i < accounts.length(); i++) {
				JSONObject acc = accounts.getJSONObject(i);
				amount += acc.optDouble("estimatedAmount", 0.0);
			}
		}
		return amount;
	}
	
	/**
	 * Detects whether has data for specified <code>accCode</code>.
	 * 
	 * @param accCode
	 * @return
	 */
	public boolean hasData(String accCode) {
		boolean hasData = true;
		
		double budgetAmount = getBudgetAmount(accCode);
		double realAmount = getRealAmount(accCode);
		double estimatedAmount = getEstimatedAmount(accCode);
		if(AEMath.isZeroAmount(budgetAmount) && AEMath.isZeroAmount(realAmount) && AEMath.isZeroAmount(estimatedAmount)) {
			hasData = false;
		}
		
		return hasData;
	}
	
	public boolean accExists(String accCode) {
		boolean accExists = true;
		if(AEStringUtil.isEmpty(getName(accCode))) {
			accExists = false;
		}
		
		return accExists;
	}
	
	/**
	 * Returns the name for specified <code>accCode</code>.
	 * 
	 * @param accCode
	 * @return
	 */
	public String getName(String accCode) {
		String name = null;
		
		if(accounts != null && !AEStringUtil.isEmpty(accCode)) {
			String code = null;
			if(accCode.length() == 3) {
				code = accCode + "0";
			} else {
				code = accCode;
			}
			for (int i = 0; i < accounts.length(); i++) {
				JSONObject acc = accounts.getJSONObject(i);
				String _accCode = acc.optString(AEDomainObject.JSONKey.code.name(), AEStringUtil.EMPTY_STRING);
				if(code.equalsIgnoreCase(_accCode)) {
					name = acc.optString(AEDomainObject.JSONKey.name.name(), AEStringUtil.EMPTY_STRING);
				}
			}
		}
		
		return AEStringUtil.trim(name);
	}
	
	public double getDifference(String accCode) {
		double diff = 0.0;
		diff = (getRealAmount(accCode) - getBudgetAmount(accCode));
		return diff;
	}
	
	public double getPercentage(String accCode) {
		double percent = 0.0;
		double realAmount = getRealAmount(accCode);
		double budgetAmount = getBudgetAmount(accCode);
		if (realAmount != 0.0 && budgetAmount != 0.0) {
			percent = ((realAmount/budgetAmount) * 100.0);
		}
		return percent;
	}
	
	public void buildAccountMap() {
		if(accounts != null) {
			accountMap = new TreeMap<String, ArrayList<String>>();
			for (int i = 0; i < accounts.length(); i++) {
				JSONObject acc = accounts.getJSONObject(i);
				String _accCode = acc.optString(AEDomainObject.JSONKey.code.name(), AEStringUtil.EMPTY_STRING);
				String key = _accCode.substring(0, 3);
				if(!AEStringUtil.isEmpty(_accCode)) {
					if (accountMap.containsKey(key)) {
						accountMap.get(key).add(_accCode);
					} else {
						ArrayList<String> newList = new ArrayList<String>();
						newList.add(_accCode);
						accountMap.put(key, newList);
					}
				}
			}
		}
	}
	
	public TreeMap<String, ArrayList<String>> getAccountMap() {
		return accountMap;
	}
}
