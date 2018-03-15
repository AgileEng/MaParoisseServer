package eu.agileeng.domain.contact;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

public class ContributorDonationsList extends ArrayList<ContributorDonation> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = 796420957755653464L;

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (ContributorDonation cd : this) {
				jsonArray.put(cd.toJSONObject());
			}
		}

		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);

			ContributorDonation cd = new ContributorDonation();
			cd.create(jsonItem);

			add(cd);
		}
	}
	
	public ContributorDonation getByPerson(long personId) {
		ContributorDonation ret = null;
		for (Iterator<ContributorDonation> iterator = this.iterator(); iterator.hasNext();) {
			ContributorDonation cd = (ContributorDonation) iterator.next();
			if(cd.getEmployee().getPerson().getDescriptor().getID() == personId) {
				ret = cd;
				break;
			}
		}
		return ret;
				
	}
}
