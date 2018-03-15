/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 31.05.2010 19:47:23
 */
package eu.agileeng.domain.contact;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;

import eu.agileeng.domain.AEList;

/**
 *
 */
@SuppressWarnings("serial")
public class PartiesList extends ArrayList<Party> implements AEList {

	/**
	 * 
	 */
	public PartiesList() {
	}

	/**
	 * @param arg0
	 */
	public PartiesList(int arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public PartiesList(Collection<? extends Party> arg0) {
		super(arg0);
	}
	
	public PartiesList getRoots() {
		PartiesList roots = new PartiesList();
		for (Party party : this) {
			if(party.getParent() == null) {
				roots.add(party);
			}
		}
		return roots;
	}
	
	public PartiesList getChildren(Party parent) {
		PartiesList children = new PartiesList();
		if(parent != null) {
			for (Party party : this) {
				if(parent.equals(party.getParent())) {
					children.add(party);
				};
			}
		}
		return children;
	}
	
	public boolean hasChildren(Party parent) {
		return !getChildren(parent).isEmpty();
	}

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (Party item : this) {
				jsonArray.put(item.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		// TODO
	}
}
