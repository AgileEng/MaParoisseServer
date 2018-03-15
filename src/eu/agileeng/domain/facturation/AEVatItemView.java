package eu.agileeng.domain.facturation;

import org.apache.tomcat.util.json.JSONObject;

public class AEVatItemView {
	private JSONObject jsonDocument;

	public AEVatItemView(JSONObject jsonDocument) {
		this.jsonDocument = jsonDocument;
		assert(this.jsonDocument != null);
	}
}
