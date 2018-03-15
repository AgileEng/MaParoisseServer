/**
 * 
 */
package eu.agileeng.services;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEException;
import eu.agileeng.util.AEDynamicProperties;
import eu.agileeng.util.json.JSONSerializable;

/**
 * @author vvatov
 *
 */
public class AEResponse extends AEMessage implements JSONSerializable {

	private boolean success = true;
	
	private AEException error;
	
	private JSONObject payload;
	
	private AEDynamicProperties properties;
	
	/**
	 * 
	 */
	public AEResponse() {
	}

	/**
	 * 
	 */
	public AEResponse(JSONObject payload) {
		this.payload = payload;
	}
	
	public AEResponse(AEException e) {
		this();
		setError(e);
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.util.json.JSONSerializable#toJSONObject()
	 */
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject rawResponse = new JSONObject();
		JSONObject jsonHead = new JSONObject();
		
		// HEAD
		if(isSuccess()) {
			jsonHead.put(RESULT, 1);
		} else {
			jsonHead.put(RESULT, 0);
			jsonHead.put("errors", this.error.getLocalizedMessage());
			jsonHead.put("errCode", this.error.getCode());
		}
		
		rawResponse.put(HEAD, jsonHead);
		if(payload == null) {
			payload = new JSONObject();
		}
		rawResponse.put(BODY, payload);
		return rawResponse;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public AEException getError() {
		return error;
	}

	public void setError(AEException error) {
		this.error = error;
		this.success = false;
	}

	public JSONObject getPayload() {
		return payload;
	}

	public void setPayload(JSONObject payload) {
		this.payload = payload;
	}

	public AEDynamicProperties getProperties() {
		return properties;
	}

	public void setProperties(AEDynamicProperties properties) {
		this.properties = properties;
	}

	@Override
	@Deprecated
	public void create(JSONObject jsonObject) throws JSONException {
		// TODO Auto-generated method stub
		
	}
}
