/**
 * 
 */
package eu.agileeng.domain.business.bank;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEList;

/**
 * Represents a multi purpose list of bank transactions. 
 * 
 * @author vvatov
 *
 */
public class BankTransactionsList extends ArrayList<BankTransaction> implements AEList {

	/**
	 * The opening balance for this list.
	 * Can be undefined
	 */
	private double openingBalance;
	
	/**
	 * The opening balance date for this list
	 * Can be null
	 */
	private Date openingBalanceDate;
	
	/**
	 * The date of the first transaction in this list
	 * Can be not set 
	 */
	private Date firstTransactionDate;

	/**
	 * The final balance for this list
	 * Can be undefined
	 */
	private double finalBalance;
	
	/**
	 * The final balance date for this list	
	 * Can be null
	 */
	private Date finalBalanceDate;
	
	/**
	 * The company (customer), owner of these bank transactions
	 * Can be null
	 */
	private AEDescriptive company;
	
	/**
	 * The bank account where these transactions are done
	 * Can be null
	 */
	private AEDescriptive bankAccount;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3715674112070154502L;

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEList#toJSONArray()
	 */
	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (BankTransaction item : this) {
				jsonArray.put(item.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEList#create(org.apache.tomcat.util.json.JSONArray)
	 */
	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);
			
			BankTransaction bt = new BankTransaction();
			bt.create(jsonItem);
			
			add(bt);
		}
	}
	
	/**
	 * Use only transactions of type "Movement" when calculate turnover
	 * 
	 * @return
	 */
//	public double calculateDtTurnover() {
//		double dtTurnover = 0.0;
//		for (Iterator<BankTransaction> iterator = this.iterator(); iterator.hasNext();) {
//			BankTransaction bt = (BankTransaction) iterator.next();
//			if(bt.getDtAmount() != null) {
//				dtTurnover =+ bt.getDtAmount();
//			}
//		}
//		return dtTurnover;
//	}
	
	/**
	 * Use only transactions of type "Movement" when calculate turnover
	 * 
	 * @return
	 */
//	public double calculateCtTurnover() {
//		double ctTurnover = 0.0;
//		for (Iterator<BankTransaction> iterator = this.iterator(); iterator.hasNext();) {
//			BankTransaction bt = (BankTransaction) iterator.next();
//			if(bt.getCtAmount() != null) {
//				ctTurnover =+ bt.getCtAmount();
//			}
//		}
//		return ctTurnover;
//	}

	public double getOpeningBalance() {
		return openingBalance;
	}

	public void setOpeningBalance(double openingBalance) {
		this.openingBalance = openingBalance;
	}

	public double getFinalBalance() {
		return finalBalance;
	}

	public void setFinalBalance(double finalBalance) {
		this.finalBalance = finalBalance;
	}

	public Date getOpeningBalanceDate() {
		return openingBalanceDate;
	}

	public void setOpeningBalanceDate(Date openingBalanceDate) {
		this.openingBalanceDate = openingBalanceDate;
	}

	public Date getFinalBalanceDate() {
		return finalBalanceDate;
	}

	public void setFinalBalanceDate(Date finalBalanceDate) {
		this.finalBalanceDate = finalBalanceDate;
	}
	
	public boolean isClosed() {
		return this.getFinalBalanceDate() != null;
	}
	
	public boolean isOpened() {
		return this.getOpeningBalanceDate() != null;
	}
	
	public AEDescriptive getCompany() {
		return company;
	}

	public void setCompany(AEDescriptive company) {
		this.company = company;
	}

	public AEDescriptive getBankAccount() {
		return bankAccount;
	}

	public void setBankAccount(AEDescriptive bankAccount) {
		this.bankAccount = bankAccount;
	}
	
	public Date getFirstTransactionDate() {
		return firstTransactionDate;
	}

	public void setFirstTransactionDate(Date firstTransactionDate) {
		this.firstTransactionDate = firstTransactionDate;
	}
	
	/**
	 * Calculates balance date from existing bank transactions.
	 * 
	 * <br><b>Important:</b> Use this method only and only if this list is asc sorted by date
	 */
	public void extractDates() {
		if(!isEmpty()) {
			openingBalanceDate = get(0).getDate();
			firstTransactionDate = get(0).getDate();
			finalBalanceDate = get(size() - 1).getDate();
		} else {
			openingBalanceDate = null;
			firstTransactionDate = null;
			finalBalanceDate = null;
		}
	}
	
	/**
	 * Extracts dates from existing bank transactions.
	 * 
	 * <br><b>Important:</b> Use this method only and only if this list is parsed from Ebics file
	 */
	public void extractEbicsDates() {
		if(!isEmpty()) {
			openingBalanceDate = null;
			if("01".equals(get(0).getCode())) {
				openingBalanceDate = get(0).getDate();
			}
			
			firstTransactionDate = null;
			for (Iterator<BankTransaction> iterator = this.iterator(); iterator.hasNext();) {
				BankTransaction bt = iterator.next();
				if("04".equals(bt.getCode())) {
					firstTransactionDate = bt.getDate();
					break;
				}
			}
			
			finalBalanceDate = null;
			if("07".equals(get(size() - 1).getCode())) {
				finalBalanceDate = get(size() - 1).getDate();
			}
		} else {
			openingBalanceDate = null;
			firstTransactionDate = null;
			finalBalanceDate = null;
		}
	}
	
	/**
	 * <br><b>Important:</b> Use this method only and only if this list is parsed from Ebics file
	 * @return Whether there are movements (record with code = "04") in this list or not
	 */
	public final boolean haveEbicsMovements() {
		boolean bRet = false;
		for (Iterator<BankTransaction> iterator = this.iterator(); iterator.hasNext();) {
			BankTransaction bt = iterator.next();
			if("04".equals(bt.getCode())) {
				bRet = true;
				break;
			}
		}
		return bRet;
	}
}
