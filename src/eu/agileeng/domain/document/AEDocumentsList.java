/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 23.05.2010 18:41:48
 */
package eu.agileeng.domain.document;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

/**
 *
 */
@SuppressWarnings("serial")
public class AEDocumentsList extends ArrayList<AEDocument> implements AEList {

	/**
	 * 
	 */
	public AEDocumentsList() {
	}

	/**
	 * @param arg0
	 */
	public AEDocumentsList(int arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public AEDocumentsList(Collection<? extends AEDocument> arg0) {
		super(arg0);
	}

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (AEDocument doc : this) {
				jsonArray.put(doc.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonDoc = (JSONObject) jsonArray.get(i);
			
			AEDocument doc = new AEDocument();
			doc.create(jsonDoc);
			
			add(doc);
		}
	}
}
