package eu.agileeng.domain.facturation;

import java.util.Iterator;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.document.trade.AEDocumentItem;
import eu.agileeng.domain.document.trade.AEDocumentItemsList;
import eu.agileeng.util.AEFilter;

public class AEFactureItemsList extends AEDocumentItemsList {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4060803880784212939L;

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);
			
			AEFactureItem item = new AEFactureItem();
			item.create(jsonItem);
			
			add(item);
		}
	}
	
	public AEFactureItemsList filter(AEFilter filter) {
		AEFactureItemsList filtered = new AEFactureItemsList();
		for (Iterator<AEDocumentItem> iterator = this.iterator(); iterator.hasNext();) {
			AEFactureItem item = (AEFactureItem) iterator.next();
			if(filter.evaluate(item)) {
				filtered.add(item);
			}
		}
		return filtered;
	}
}
