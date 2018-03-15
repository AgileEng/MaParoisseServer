/**
 * 
 */
package eu.agileeng.services;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.security.AuthPrincipal;

/**
 * @author vvatov
 *
 */
public class AERequest extends AEMessage {
	
	private String sessionID;
	
	private String serviceName;
	
	private String method;
	
	private JSONObject arguments;
	
	private AuthPrincipal authPrincipal;
	
	/**
	 * 
	 */
	public AERequest(String jsonString) throws JSONException {
		JSONObject rawRequest = new JSONObject(jsonString);
		JSONObject jsonHead = rawRequest.getJSONObject(HEAD);
		JSONObject jsonBody = rawRequest.getJSONObject(BODY);
		
		// sid is optional
		if(jsonHead.has(SID)) {
			this.sessionID = jsonHead.getString(SID);
		}
		
		// service_type is required
		this.serviceName = jsonHead.getString(SERVICE_TYPE);
		
		// method is required
		this.method = jsonHead.getString(METHOD);
		
		// body is required, can be empty JSONObject
		if(jsonBody.has(ARGS)) {
			this.arguments = jsonBody.getJSONObject(ARGS);
		}
	}

	public AERequest(JSONObject arguments) {
		this.arguments = arguments;
	}
	
	public String getSessionID() {
		return sessionID;
	}

	public void setSessionID(String sessionID) {
		this.sessionID = sessionID;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public JSONObject getArguments() {
		return arguments;
	}

	public void setArguments(JSONObject arguments) {
		this.arguments = arguments;
	}

	public AuthPrincipal getAuthPrincipal() {
		return authPrincipal;
	}

	public void setAuthPrincipal(AuthPrincipal authPrincipal) {
		this.authPrincipal = authPrincipal;
	}

	public AERequest withAuthPrincipal(AuthPrincipal authPrincipal) {
		this.authPrincipal = authPrincipal;
		return this;
	}
}
