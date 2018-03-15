/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 10.11.2009 16:06:47
 */
package eu.agileeng.domain;

import java.io.Serializable;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.json.JSONSerializable;

/**
 *
 */
@SuppressWarnings("serial")
public class AEException extends Exception implements Serializable, JSONSerializable {

	private int code;
	
	static private enum JsonKey {
		code,
		message
	}
	
	/**
	 * 
	 */
	public AEException() {
		super();
	}

	/**
	 * @param message
	 */
	public AEException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public AEException(Throwable cause) {
		this(cause.getMessage(), cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public AEException(String message, Throwable cause) {
		super(message, cause);
		if(cause instanceof AEException) {
			this.code = ((AEException) cause).getCode();
		}
	}
	
	/**
	 * 
	 * @param code
	 * @param message
	 */
	public AEException(int code, String message) {
		super(message);
		this.code = code;
	}
	
	/**
	 * 
	 * @param code
	 * @param message
	 */
	public AEException(AEError.System aeError) {
		this(aeError.getSystemID(), aeError.getMessage());
	}

	/**
	 * 
	 * @param code
	 * @param message
	 */
	public AEException(long code, String message) {
		super(message);
		this.code = (int) code;
	}
	
	/**
	 * 
	 * @param code
	 * @param message
	 * @param cause
	 */
	public AEException(int code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	/**
	 * @return the code
	 */
	public int getCode() {
		return code;
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = new JSONObject();
		
		json.put(AEException.JsonKey.code.toString(), getCode());
		
		if(!AEStringUtil.isEmpty(getMessage())) {
			json.put(AEException.JsonKey.message.toString(), getMessage());
		}
		
		return json;
	}

	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		
	}
}
