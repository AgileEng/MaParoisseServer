/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 21.11.2009 17:11:48
 */
package eu.agileeng.domain;

import java.io.Serializable;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

/**
 *
 */
public interface AEDescriptive extends Serializable {
	public AEDescriptor getDescriptor();
	public JSONObject toJSONObject() throws JSONException;
}
