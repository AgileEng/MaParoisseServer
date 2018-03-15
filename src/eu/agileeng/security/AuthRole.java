/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 20.05.2010 16:54:15
 */
package eu.agileeng.security;

import java.security.Principal;
import java.security.acl.Group;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.util.AECollectionUtil;
import eu.agileeng.util.json.JSONSerializable;

/**
 * Persistable
 * Code ???
 * Name
 * Description
 * Note
 * Properties (isSystem)
 * 
 * This class implements a group of principals.
 */
@SuppressWarnings("serial")
public class AuthRole extends AEDomainObject implements Group, JSONSerializable {

	private Set<Principal> members = new HashSet<Principal>();

	private boolean system = false;

	private boolean active = true;

	private long sysId;

	static public enum System {
		na(0L),
		technician(1L),
		administrator(100L),
		accountant(200L),
		power_user(200L), // alias
		social(200L),     // alias
		operative(300L),
		operative_min(300L), // for internal use
		operative_accountant(300L), // alias
		operative_read_only(350L),  // read-only role
		operative_social(300L),     //alias
		planner(600),
		approver(700),
		maxId(1000L);

		private long systemID;

		private System(long systemID) {
			this.systemID = systemID;
		}

		public final long getSystemID() {
			return this.systemID;
		}

		public static System valueOf(long systemID) {
			System ret = null;
			for (System inst : System.values()) {
				if(inst.getSystemID() == systemID) {
					ret = inst;
					break;
				}
			}
			if(ret == null) {
				ret = na;
			}
			return ret;
		}
	}

	/**
	 * @param clazz
	 */
	public AuthRole() {
		super(DomainModel.DomainClass.AuthRole);
	}

	public AuthRole(String group) {
		this();
		setName(group);
	}

	public boolean isSystem() {
		return system;
	}

	public void setSystem(boolean system) {
		this.system = system;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public long getSysId() {
		return sysId;
	}

	public void setSysId(long sysId) {
		this.sysId = sysId;
	}

	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.AuthRole);
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();

		json.put("sysId", getSysId());
		json.put("active", isActive());
		json.put("system", isSystem());

		return json;
	}

	@Override
	public void create(JSONObject jsonObject) throws JSONException {

	}

	public static JSONArray toJSONArray(List<AuthRole> authRolesList) throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!AECollectionUtil.isEmpty(authRolesList)) {
			for (AuthRole authRole : authRolesList) {
				jsonArray.put(authRole.toJSONObject());
			}
		}

		return jsonArray;
	}

	@Override
	public AEDescriptor getDescriptor() {
		AEDescriptor descr = super.getDescriptor();
		descr.setSysId(getSysId());
		return descr;
	}

	/**
	 * adds the specified member to the group.
	 * @param user The principal to add to the group.
	 * @return true if the member was added - false if the
	 * member could not be added.
	 */
	@Override
	public boolean addMember(Principal user) {
		if (members.contains(user)) {
			return false;
		}

		// do not allow groups to be added to itself.
		if (this.equals(user)) {
			throw new IllegalArgumentException();
		}

		members.add(user);
		return true;
	}

	/**
	 * returns true if the passed principal is a member of the group.
	 * @param member The principal whose membership must be checked for.
	 * @return true if the principal is a member of this group,
	 * false otherwise
	 */
	@Override
	public boolean isMember(Principal member) {
		//
		// if the member is part of the group (common case), return true.
		// if not, recursively search depth first in the group looking for the
		// principal.
		//
		if (members.contains(member)) {
			return true;
		} else {
			Set<Group> alreadySeen = new HashSet<Group>(10);
			return isMemberRecurse(member, alreadySeen);
		}
	}

	/**
	 * returns the enumeration of the members in the group.
	 */
	@Override
	public Enumeration<? extends Principal> members() {
		return Collections.enumeration(members);
	}

	/**
	 * removes the specified member from the group.
	 * @param user The principal to remove from the group.
	 * @param true if the principal was removed false if
	 * the principal was not a member
	 */
	@Override
	public boolean removeMember(Principal user) {
		return members.remove(user);
	}

	//
	// This function is the recursive search of groups for this
	// implementation of the Group. The search proceeds building up
	// a vector of already seen groups. Only new groups are considered,
	// thereby avoiding loops.
	//
	boolean isMemberRecurse(Principal member, Set<Group> alreadySeen) {
		Enumeration<? extends Principal> e = members();
		while (e.hasMoreElements()) {
			boolean mem = false;
			Principal p = (Principal) e.nextElement();

			// if the member is in this collection, return true
			if (p.equals(member)) {
				return true;
			} else if (p instanceof AuthRole) {
				//
				// if not recurse if the group has not been checked already.
				// Can call method in this package only if the object is an
				// instance of this class. Otherwise call the method defined
				// in the interface. (This can lead to a loop if a mixture of
				// implementations form a loop, but we live with this improbable
				// case rather than clutter the interface by forcing the
				// implementation of this method.)
				//
				AuthRole g = (AuthRole) p;
				alreadySeen.add(this);
				if (!alreadySeen.contains(g))
					mem =  g.isMemberRecurse(member, alreadySeen);
			} else if (p instanceof Group) {
				Group g = (Group) p;
				if (!alreadySeen.contains(g))
					mem = g.isMember(member);
			}

			if (mem)
				return mem;
		}
		return false;
	}
	
	public static boolean isOperative(AuthPrincipal ap) {
		return ap.getMaxRole() >= AuthRole.System.operative_min.getSystemID();
	}

	public boolean implies(Subject subject) {
		// TODO Auto-generated method stub
		return false;
	}
}
