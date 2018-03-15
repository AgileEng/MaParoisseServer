/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 13.06.2010 10:36:10
 */
package eu.agileeng.domain;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.imp.AEDescriptorImp;

/**
 *
 */
@SuppressWarnings("serial")
public class AEDescriptorsList extends ArrayList<AEDescriptor> implements AEList {
	/**
	 * 
	 */
	public AEDescriptorsList() {
	}

	/**
	 * @param arg0
	 */
	public AEDescriptorsList(int arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public AEDescriptorsList(Collection<? extends AEDescriptor> arg0) {
		super(arg0);
	}

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();
		for (AEDescriptor descr : this) {
			jsonArray.put(descr.toJSONObject());
		}
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		this.clear();
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObj = (JSONObject) jsonArray.get(i);
			AEDescriptorImp descr = new AEDescriptorImp();
			descr.create(jsonObj);
			add(descr);
		}
	}
}
