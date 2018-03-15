package eu.agileeng.domain.social;

import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.util.AEStringUtil;

public class SalaryGridView {
	private JSONObject jsonDocument;
	public SalaryGridView(JSONObject jsonDocument) {
		this.jsonDocument = jsonDocument;
	}
	public double getRateMaxExtraHours() {
		double rateMaxExtraHours = 0.0;
		if (jsonDocument != null && jsonDocument.has(SalaryGrid.JSONKey.rateMaxExtraHours.toString())) {
			rateMaxExtraHours = jsonDocument.optDouble(SalaryGrid.JSONKey.rateMaxExtraHours.toString());
		}
		return rateMaxExtraHours;
	}
	
	public String getName() {
		String name = AEStringUtil.EMPTY_STRING;
		if (jsonDocument != null && jsonDocument.has("name")) {
			name = jsonDocument.optString("name");
		}
		return name;
	}
}
