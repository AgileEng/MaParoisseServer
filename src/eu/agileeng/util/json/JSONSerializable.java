/**
 * 
 */
package eu.agileeng.util.json;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

/**
 * @author vvatov
 *
 */
public interface JSONSerializable {
	/**
	 * The common behaviour:
	 * <code><br>
	 * <br>JSONObject jsonError = new JSONObject();
	 * <br>jsonError.put("name1", value);
	 * <br>...
	 * <br>jsonError.put("nameN", value);
     * <br>return jsonError;
	 * </code>
	 * @return The JSON representation of this object in the form {"name1" : value, ..., "nameN" : value}
	 * @throws JSONException
	 */
	public JSONObject toJSONObject() throws JSONException;
	
	public void create(JSONObject jsonObject) throws JSONException;
}
