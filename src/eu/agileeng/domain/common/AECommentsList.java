/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 13.06.2010 22:17:14
 */
package eu.agileeng.domain.common;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;

import eu.agileeng.domain.AEList;

/**
 *
 */
@SuppressWarnings("serial")
public class AECommentsList extends ArrayList<AEComment> implements AEList {

	/**
	 * 
	 */
	public AECommentsList() {
	}

	/**
	 * @param arg0
	 */
	public AECommentsList(int arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public AECommentsList(Collection<? extends AEComment> arg0) {
		super(arg0);
	}

	@Override
	public JSONArray toJSONArray() throws JSONException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		// TODO Auto-generated method stub
		
	}

}
