package eu.agileeng.domain;

import java.util.Collection;
import java.util.HashSet;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.imp.AEDescriptorImp;

public class AEDescriptorsSet extends HashSet<AEDescriptor> implements AEList {

	private static final long serialVersionUID = 5692902388481255132L;

	/**
	 * 
	 */
	public AEDescriptorsSet() {
	}

	/**
	 * @param arg0
	 */
	public AEDescriptorsSet(int arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public AEDescriptorsSet(Collection<? extends AEDescriptor> arg0) {
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
