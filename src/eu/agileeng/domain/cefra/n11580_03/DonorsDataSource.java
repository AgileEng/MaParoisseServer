package eu.agileeng.domain.cefra.n11580_03;

import org.apache.tomcat.util.json.JSONArray;


public class DonorsDataSource extends ReportDataSource {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1722037237124625256L;
	
//	JSONObject payload = new JSONObject()
//	.put("donations", ((ContributorDonationsList) donationsPojo.get("donations")).toJSONArray())
//	.put("accountancy", ((ContributorDonationsList) donationsPojo.get("accountancy")).toJSONArray())
//	.put("year", year);
	
	private JSONArray donations;
	
	private JSONArray accountancy;
	
	public DonorsDataSource() {
	}

	/**
	 * @return the donations
	 */
	public JSONArray getDonations() {
		return donations;
	}

	/**
	 * @param donations the donations to set
	 */
	public void setDonations(JSONArray donations) {
		this.donations = donations;
	}

	/**
	 * @return the accountancy
	 */
	public JSONArray getAccountancy() {
		return accountancy;
	}

	/**
	 * @param accountancy the accountancy to set
	 */
	public void setAccountancy(JSONArray accountancy) {
		this.accountancy = accountancy;
	}
}
