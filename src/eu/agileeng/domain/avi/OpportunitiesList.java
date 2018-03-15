/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 22.11.2009 15:28:14
 */
package eu.agileeng.domain.avi;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;

import eu.agileeng.domain.AEList;


/**
 *
 */
@SuppressWarnings("serial")
public class OpportunitiesList extends ArrayList<Opportunity> implements AEList {

	/**
	 * 
	 */
	public OpportunitiesList() {
	}

	public OpportunitiesList(Opportunity opp) {
		this.add(opp);
	}
	
	/**
	 * @param arg0
	 */
	public OpportunitiesList(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * @param arg0
	 */
	public OpportunitiesList(Collection<? extends Opportunity> c) {
		super(c);
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
