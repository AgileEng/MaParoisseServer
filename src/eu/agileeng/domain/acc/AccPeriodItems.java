package eu.agileeng.domain.acc;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;

import eu.agileeng.domain.AEList;

public class AccPeriodItems extends ArrayList<AccPeriod> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5871628074646331218L;

	public AccPeriodItems() {
	}

	public AccPeriodItems(int arg0) {
		super(arg0);
	}

	public AccPeriodItems(Collection<? extends AccPeriod> arg0) {
		super(arg0);
	}

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();
		if(!this.isEmpty()) {
			for (AccPeriod period : this) {
				jsonArray.put(period.toJSONObject());
			}
		}
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {

	}
}
