/**
 * 
 */
package eu.agileeng.domain;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.util.json.JSONSerializable;

/**
 * @author vvatov
 *
 */
@SuppressWarnings("serial")
public class AEExceptionsList extends ArrayList<AEException> implements JSONSerializable, AEList {

	/**
	 * 
	 */
	public AEExceptionsList() {
	}

	/**
	 * @param arg0
	 */
	public AEExceptionsList(int arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public AEExceptionsList(Collection<? extends AEException> arg0) {
		super(arg0);
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		return null;
	}

	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		
	}

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (AEException e : this) {
				jsonArray.put(e.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
	}
}
