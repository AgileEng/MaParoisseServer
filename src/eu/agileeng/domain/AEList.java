/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 23.11.2009 18:37:36
 */
package eu.agileeng.domain;

import java.io.Serializable;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;

/**
 *
 */
public interface AEList extends Serializable {
	public JSONArray toJSONArray() throws JSONException;
	
	public void create(JSONArray jsonArray) throws JSONException;
}
