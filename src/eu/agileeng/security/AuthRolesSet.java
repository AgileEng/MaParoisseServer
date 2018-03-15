/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 03.06.2010 16:26:18
 */
package eu.agileeng.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;

import eu.agileeng.domain.AEList;

/**
 *
 */
@SuppressWarnings("serial")
public class AuthRolesSet extends HashSet<AuthRole> implements AEList {

	/**
	 * 
	 */
	public AuthRolesSet() {
	}

	/**
	 * @param arg0
	 */
	public AuthRolesSet(int arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public AuthRolesSet(Collection<? extends AuthRole> arg0) {
		super(arg0);
	}

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (AuthRole authRole : this) {
				jsonArray.put(authRole.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
	}
	
	public void addAllWithoutDupl(ArrayList<AuthRole> list) {
		for (Iterator<AuthRole> iterator = list.iterator(); iterator.hasNext();) {
			AuthRole authRole = (AuthRole) iterator.next();
			if(!this.contains(authRole)) {
				this.add(authRole);
			}
		}
	}
	
	public ArrayList<AuthRole> getAccountantRoles() {
		ArrayList<AuthRole> accRoles = new ArrayList<AuthRole>();
		for (Iterator<AuthRole> iterator = this.iterator(); iterator.hasNext();) {
			AuthRole authRole = (AuthRole) iterator.next();
			if(authRole.getSysId() == AuthRole.System.accountant.getSystemID()
					|| authRole.getSysId() == AuthRole.System.operative_accountant.getSystemID()) {
				
				accRoles.add(authRole);
			}
		}
		return accRoles;
	}
	
	public ArrayList<AuthRole> getSocialRoles() {
		ArrayList<AuthRole> socialRoles = new ArrayList<AuthRole>();
		for (Iterator<AuthRole> iterator = this.iterator(); iterator.hasNext();) {
			AuthRole authRole = (AuthRole) iterator.next();
			if(authRole.getSysId() == AuthRole.System.social.getSystemID()
					|| authRole.getSysId() == AuthRole.System.operative_social.getSystemID()) {
				
				socialRoles.add(authRole);
			}
		}
		return socialRoles;
	}
	
	public AuthRole getBySystemId(AuthRole.System sysRole) {
		AuthRole retRole = null;
		for (AuthRole role : this) {
			if(role.getSysId() == sysRole.getSystemID()) {
				retRole = role;
				break;
			}
		}
		return retRole;
	}
}
