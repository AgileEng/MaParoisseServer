/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 03.06.2010 16:28:18
 */
package eu.agileeng.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;

import eu.agileeng.domain.AEList;

/**
 *
 */
@SuppressWarnings("serial")
public class AuthPrincipalsList extends ArrayList<AuthPrincipal> implements AEList {

	/**
	 * 
	 */
	public AuthPrincipalsList() {
	}

	/**
	 * @param initialCapacity
	 */
	public AuthPrincipalsList(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * @param c
	 */
	public AuthPrincipalsList(Collection<? extends AuthPrincipal> c) {
		super(c);
	}

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();
		if(!this.isEmpty()) {
			for (AuthPrincipal ap : this) {
				jsonArray.put(ap.toJSONObject());
			}
		}
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		// TODO Auto-generated method stub
		
	}
	
	public void addAllWithoutDupl(ArrayList<AuthPrincipal> list) {
		for (Iterator<AuthPrincipal> iterator = list.iterator(); iterator.hasNext();) {
			AuthPrincipal authPrincipal = (AuthPrincipal) iterator.next();
			if(!this.contains(authPrincipal)) {
				this.add(authPrincipal);
			}
		}
	}
	
	public boolean aeRemove(AuthPrincipal ap) {
		boolean ret = false;
		if (ap == null) {
			for (Iterator<AuthPrincipal> iterator = this.iterator(); iterator.hasNext();) {
				AuthPrincipal currAP = iterator.next();
				if(currAP == null) {
					iterator.remove();
					ret = true;
				}
			}
		} else {
			for (Iterator<AuthPrincipal> iterator = this.iterator(); iterator.hasNext();) {
				AuthPrincipal currAP = iterator.next();
				if(ap.equals(currAP)) {
					iterator.remove();
					ret = true;
				}
			}
		}
		return ret;
	}
}
