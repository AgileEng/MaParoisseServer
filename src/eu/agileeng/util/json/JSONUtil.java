/**
 * 
 */
package eu.agileeng.util.json;

import java.util.Iterator;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEException;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;

/**
 * @author vvatov
 *
 */
public class JSONUtil {

	/**
	 * 
	 */
	private JSONUtil() {
	}

	public final static void apply(JSONObject to, JSONObject from) throws JSONException {
		if(to != null && from != null) {
			for (Iterator<?> iterator = from.keys(); iterator.hasNext();) {
				String key = (String) iterator.next();
				to.put(key, from.get(key));
			}
		}
	}
	
	public final static JSONObject findBy(JSONArray arr, String key, Object value) throws JSONException {
		JSONObject found = null;
		for (int i = 0; i < arr.length(); i++) {
			JSONObject _next = arr.optJSONObject(i);
			if(_next != null) {
				Object _nextValue = _next.opt(key);
				if(_nextValue != null && _nextValue.equals(value)) {
					found = _next;
					break;
				}
			}
		}
		return found;
	}
	
	public final static double normDouble(JSONObject json, String key) throws JSONException {
		double ret = 0.0;
		if(json.has(key)) {
			ret = json.optDouble(key);
			if(Double.isNaN(ret) || Double.isInfinite(ret)) {
				ret = 0.0;
			}
		}
		return ret;
	}
	
	public final static Double parseDouble(JSONObject json, String key) throws AEException {
		Double ret = null;
		if(json.has(key)) {
			ret = AEMath.parseDouble(json.optString(key), true);
		}
		return ret;
	}
	
	public final static Double parseDoubleStrict(JSONObject json, String key) throws AEException {
		Double ret = null;
		if(json.has(key)) {
			String rawValue = json.optString(key);
			if(!AEStringUtil.isEmpty(rawValue)) {
				ret = AEMath.parseDouble(rawValue, true);
			}
		}
		return ret;
	}
	
	public final static Integer optInteger(JSONObject json, String key) {
		Integer ret = null;
		if(json.has(key)) {
			String rawValue = json.optString(key);
			if(!AEStringUtil.isEmpty(rawValue)) {
				try {
					ret = Integer.parseInt(rawValue);
				} catch (NumberFormatException e) {
					// not contain a parsable integer
				}
			}
		}
		return ret;
	}
	
	public final static Long optLong(JSONObject json, String key) {
		Long ret = null;
		if(json.has(key)) {
			String rawValue = json.optString(key);
			if(!AEStringUtil.isEmpty(rawValue)) {
				try {
					ret = Long.parseLong(rawValue);
				} catch (NumberFormatException e) {
					// not contain a parsable integer
				}
			}
		}
		return ret;
	}
	
	public final static void addAll(JSONArray to, JSONArray from) throws JSONException {
		if(to != null && from != null) {
			for (int i = 0; i < from.length(); i++) {
				to.put(JSONObject.wrap(from.get(i)));
			}
		}
	}
}
