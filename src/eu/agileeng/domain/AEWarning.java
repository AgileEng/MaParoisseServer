package eu.agileeng.domain;

import java.io.Serializable;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.json.JSONSerializable;

public class AEWarning extends Exception implements Serializable, JSONSerializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4358713288157709810L;


	private int code;
	
	static private enum JsonKey {
		code,
		message
	}
	
	/**
	 * 
	 */
	public AEWarning() {
		super();
	}

	/**
	 * @param message
	 */
	public AEWarning(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public AEWarning(Throwable cause) {
		this(cause.getMessage(), cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public AEWarning(String message, Throwable cause) {
		super(message, cause);
		if(cause instanceof AEWarning) {
			this.code = ((AEWarning) cause).getCode();
		}
	}
	
	/**
	 * 
	 * @param code
	 * @param message
	 */
	public AEWarning(int code, String message) {
		super(message);
		this.code = code;
	}

	/**
	 * 
	 * @param code
	 * @param message
	 */
	public AEWarning(long code, String message) {
		super(message);
		this.code = (int) code;
	}
	
	/**
	 * 
	 * @param code
	 * @param message
	 * @param cause
	 */
	public AEWarning(int code, String message, Throwable cause) {
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
		
		json.put(AEWarning.JsonKey.code.toString(), getCode());
		
		if(!AEStringUtil.isEmpty(getMessage())) {
			json.put(AEWarning.JsonKey.message.toString(), getMessage());
		}
		
		return json;
	}

	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		
	}
}
