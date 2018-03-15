package eu.agileeng.domain.cefra.n11580_03;

import java.util.Date;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONObject;

public class FinancesDataSource extends ReportDataSource {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4060108950618411076L;
	
	private Date date;
	
	private JSONArray banks;
	
	private JSONObject cashDesk;
	
	private JSONArray engagements;
	
	private JSONArray titres;
	
	public FinancesDataSource() {

	}

	/**
	 * @return the date
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * @param date the date to set
	 */
	public void setDate(Date date) {
		this.date = date;
	}

	/**
	 * @return the banks
	 */
	public JSONArray getBanks() {
		return banks;
	}

	/**
	 * @param banks the banks to set
	 */
	public void setBanks(JSONArray banks) {
		this.banks = banks;
	}

	/**
	 * @return the cashDesk
	 */
	public JSONObject getCashDesk() {
		return cashDesk;
	}

	/**
	 * @param cashDesk the cashDesk to set
	 */
	public void setCashDesk(JSONObject cashDesk) {
		this.cashDesk = cashDesk;
	}

	/**
	 * @return the engagements
	 */
	public JSONArray getEngagements() {
		return engagements;
	}

	/**
	 * @param engagements the engagements to set
	 */
	public void setEngagements(JSONArray engagements) {
		this.engagements = engagements;
	}

	/**
	 * @return the titres
	 */
	public JSONArray getTitres() {
		return titres;
	}

	/**
	 * @param titres the titres to set
	 */
	public void setTitres(JSONArray titres) {
		this.titres = titres;
	}
	
	public double getTitresSum() {
		double sum = 0.0;
		if(titres != null && titres.length() > 0) {
			for (int i = 0; i < titres.length(); i++) {
				JSONObject titre = titres.getJSONObject(i);
				sum += titre.optDouble("price", 0.0);
			}
		}
		return sum;
	}
	
	public double getLastBankBalancesSum() {
		double sum = 0.0;
		if(banks != null && banks.length() > 0) {
			for (int i = 0; i < banks.length(); i++) {
				JSONObject bank = banks.getJSONObject(i);
				sum += bank.optDouble("lastStatementBalance", 0.0);
			}
		}
		return sum;
	}
	
	public double getLastCashBalance() {
		double sum = 0.0;
		
		sum += getLastBankBalancesSum();
		if(cashDesk != null) {
			sum += cashDesk.optDouble("accBalance", 0.0);
		}

		return sum;
	}

}
