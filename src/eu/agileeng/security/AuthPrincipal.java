/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 20.05.2010 17:04:46
 */
package eu.agileeng.security;

import java.security.PermissionCollection;
import java.security.Principal;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.Subject;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.accbureau.AEAppConfigList;
import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDescriptorsList;
import eu.agileeng.domain.AEDescriptorsSet;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.contact.Person;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.util.AECollectionUtil;
import eu.agileeng.util.AEStringUtil;

/**
 * This class implements the principal interface.
 */
@SuppressWarnings("serial")
public class AuthPrincipal extends AEDomainObject implements Principal {
	
	static public enum JSONKey {
		personId,
		person,
		active,
		system,
		password,
		firstName,
		middleName,
		lastName,
		fullName,
		eMail,
		phone,
		companies,
		roles,
		appConfig,
		hiddenModules,
		mustChangePassword,
		locked;
	}

	private static final Pattern [] windowsPasswordPatternArray = {
        Pattern.compile("[a-z]"),
        Pattern.compile("[A-Z]"),
        Pattern.compile("[0-9]"),
        Pattern.compile("\\W")
    };
	
	private AEDescriptive person;
	
	private boolean authenticated;
	
	/** 
	 * The reference to the principal from an external system, 
	 * the source of this principal. 
	 * There is use id and name fields
	 *
	 **/
	private AEDescriptor externalPrincipal;
	
	private AEDescriptorsSet companiesSet = new AEDescriptorsSet();
	
	private AEDescriptorsList rolesList = new AEDescriptorsList();
	
	private AEAppConfigList appConfigList;
	
	private AEDescriptorsList hiddenModules;
	
	private String password;
	
	private boolean active = true;
	
	private boolean system = false;
	
	private String firstName;
	
	private String middleName;
	
	private String lastName;
	
	private String eMail;
	
	private String phone;
	
	//////////////////////////////////////////////////////////

	/** Whether this user's account is locked. */
	private boolean locked = false;

	/** Whether this user is logged in. */
	private boolean loggedIn = true;

    /** The last host address used by this user. */
    private String lastHostAddress;

	/** The last password change time for this user. */
	private Date lastPasswordChangeTime = new Date(0);

	/** The last login time for this user. */
	private Date lastLoginTime = new Date(0);

	/** The last failed login time for this user. */
	private Date lastFailedLoginTime = new Date(0);
	
	/** The expiration date/time for this user's account. */
	private Date expirationTime = new Date(Long.MAX_VALUE);
	
	/** The failed login count for this user's account. */
	private int failedLoginCount = 0;

	
	//////////////////////////////////////////////////////////
	
	/**
	 * @param clazz
	 */
	public AuthPrincipal() {
		super(DomainModel.DomainClass.AuthSubject);
	}

	/**
	 * @param clazz
	 */
	public AuthPrincipal(String name) {
		this();
		setName(name);
	}
	
	/**
	 * @return the person
	 */
	public AEDescriptive getPerson() {
		return person;
	}

	/**
	 * @param person the person to set
	 */
	public void setPerson(AEDescriptive person) {
		this.person = person;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.AuthSubject);
	}

	public boolean addRole(AEDescriptive role) {
		boolean bRet = false;
		if(role != null && !rolesList.contains(role.getDescriptor())) {
			rolesList.add(role.getDescriptor());
		}
		return bRet;
	}
	
	public boolean addCompany(AEDescriptive company) {
		boolean bRet = false;
		if(company != null) {
			companiesSet.add(company.getDescriptor());
			bRet = true;
		}
		return bRet;
	}

	/**
	 * @return the companiesList
	 */
	public AEDescriptorsSet getCompaniesSet() {
		return companiesSet;
	}

	/**
	 * @param companiesSet the companiesList to set
	 */
	public void setCompaniesSet(AEDescriptorsSet companiesSet) {
		this.companiesSet = companiesSet;
	}

	/**
	 * @return the rolesList
	 */
	public AEDescriptorsList getRolesList() {
		return rolesList;
	}

	/**
	 * @param rolesList the rolesList to set
	 */
	public void setRolesList(AEDescriptorsList rolesList) {
		this.rolesList = rolesList;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isSystem() {
		return system;
	}

	public void setSystem(boolean system) {
		this.system = system;
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		// override id
		// json.put(AEDomainObject.JSONKey.id.name(), 0L);
		
		// person
		if(getPerson() != null) {
			json.put(AuthPrincipal.JSONKey.personId.name(), getPerson().getDescriptor().getID());
		}
		
		// 
		json.put(AuthPrincipal.JSONKey.active.name(), isActive());
		json.put(AuthPrincipal.JSONKey.system.name(), isSystem());
		
		if(getFirstName() != null) {
			json.put(AuthPrincipal.JSONKey.firstName.name(), getFirstName());
		}
		
		if(getMiddleName() != null) {
			json.put(AuthPrincipal.JSONKey.middleName.name(), getMiddleName());
		}
		
		if(getLastName() != null) {
			json.put(AuthPrincipal.JSONKey.lastName.name(), getLastName());
		}
		
		json.put(AuthPrincipal.JSONKey.fullName.name(), getFullName());
		
		if(getEMail() != null) {
			json.put(AuthPrincipal.JSONKey.eMail.name(), getEMail());
		}
		
		if(getPhone() != null) {
			json.put(AuthPrincipal.JSONKey.phone.name(), getPhone());
		}
		
		// mustChangePassword
		Date expirationTime = getExpirationTime();
		Date now = new Date();
		if(expirationTime != null && now.after(expirationTime)) {
			json.put(AuthPrincipal.JSONKey.mustChangePassword.name(), true);
		} else {
			json.put(AuthPrincipal.JSONKey.mustChangePassword.name(), false);
		}
		
		// locked
		json.put(AuthPrincipal.JSONKey.locked.name(), isLocked());
		
		// companies
		AEDescriptorsSet compsList = getCompaniesSet();
		if(compsList != null) {
			JSONArray jsonCompsArray = new JSONArray();
			for (AEDescriptor compDescr : compsList) {
				jsonCompsArray.put(compDescr.toJSONObject());
			}
			json.put(AuthPrincipal.JSONKey.companies.name(), jsonCompsArray);
		}
		
		// roles
		AEDescriptorsList rolesList = getRolesList();
		if(rolesList != null) {
			JSONArray jsonRolesArray = new JSONArray();
			for (AEDescriptor roleDescr : rolesList) {
				jsonRolesArray.put(roleDescr.toJSONObject());
			}
			json.put(AuthPrincipal.JSONKey.roles.name(), jsonRolesArray);
		}
		
		// appConfig
		AEAppConfigList appConfList = getAppConfigList();
		if(appConfList != null) {
			json.put(AuthPrincipal.JSONKey.appConfig.name(), appConfList.toJSONArray());
		}
		
		// hiddenModules
		AEDescriptorsList hiddenModules = getHiddenModules();
		if(hiddenModules != null) {
			json.put(AuthPrincipal.JSONKey.hiddenModules.name(), hiddenModules.toJSONArray());
		}
		
		return json;
	}

	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
	
		if(jsonObject.has(AuthPrincipal.JSONKey.person.name()) && !jsonObject.isNull(AuthPrincipal.JSONKey.person.name())) {
			Person person = new Person();
			try {
				person.create(jsonObject.getJSONObject(AuthPrincipal.JSONKey.person.name()));
			} catch (JSONException e) {};
		}
		
		// roles
		if(jsonObject.has(AuthPrincipal.JSONKey.roles.name()) && !jsonObject.isNull(AuthPrincipal.JSONKey.roles.name())) {
			try {
				JSONArray jsonRoles = jsonObject.getJSONArray(AuthPrincipal.JSONKey.roles.name());
				this.rolesList.create(jsonRoles);
			} catch (JSONException e) {}
		}
		
		// companies
		if(jsonObject.has(AuthPrincipal.JSONKey.companies.name()) && !jsonObject.isNull(AuthPrincipal.JSONKey.companies.name())) {
			try {
				JSONArray jsonCompanies = jsonObject.getJSONArray(AuthPrincipal.JSONKey.companies.name());
				this.companiesSet.create(jsonCompanies);
				for (Iterator<AEDescriptor> iterator = this.companiesSet.iterator(); iterator.hasNext();) {
					AEDescriptor compDescr = (AEDescriptor) iterator.next();
					compDescr.setClazz(DomainClass.ORGANIZATION);
				}
			} catch (JSONException e) {}
		}
		
		setPassword(jsonObject.getString(AuthPrincipal.JSONKey.password.name()));
		setFirstName(jsonObject.optString(AuthPrincipal.JSONKey.firstName.name()));
		setMiddleName(jsonObject.optString(AuthPrincipal.JSONKey.middleName.name()));
		setLastName(jsonObject.optString(AuthPrincipal.JSONKey.lastName.name()));
		setEMail(jsonObject.optString(AuthPrincipal.JSONKey.eMail.name()));
		setPhone(jsonObject.optString(AuthPrincipal.JSONKey.phone.name()));
		setActive(jsonObject.optBoolean(AuthPrincipal.JSONKey.active.name()));
	}
	
	public static JSONArray toJSONArray(List<AuthPrincipal> authSubjectList) throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!AECollectionUtil.isEmpty(authSubjectList)) {
			for (AuthPrincipal authSubject : authSubjectList) {
				jsonArray.put(authSubject.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getMiddleName() {
		return middleName;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEMail() {
		return eMail;
	}

	public void setEMail(String eMail) {
		this.eMail = eMail;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}
	
	@SuppressWarnings("rawtypes")
	@Deprecated
	public long getMaxRole() {
		long roleSysId = AuthRole.System.maxId.getSystemID();
		if(rolesList != null && !rolesList.isEmpty()) {
			for (Iterator iterator = rolesList.iterator(); iterator.hasNext();) {
				AEDescriptor roleDescr = (AEDescriptor) iterator.next();
				if(roleDescr.getSysId() < roleSysId) {
					roleSysId = roleDescr.getSysId();
				}
			}
		}
		return roleSysId;
	}

	public AEAppConfigList getAppConfigList() {
		return appConfigList;
	}

	public void setAppConfigList(AEAppConfigList appConfigList) {
		this.appConfigList = appConfigList;
	}

	public AEDescriptorsList getHiddenModules() {
		return hiddenModules;
	}

	public void setHiddenModules(AEDescriptorsList hiddenModules) {
		this.hiddenModules = hiddenModules;
	}
	
	public String getFullName() {
		StringBuilder fullName = new StringBuilder();
		if(!AEStringUtil.isEmpty(getLastName())) {
			fullName.append(getLastName());
		}
		if(!AEStringUtil.isEmpty(getFirstName())) {
			if(!AEStringUtil.isEmpty(fullName)) {
				fullName.append(" ");
			}
			fullName.append(getFirstName());
		}
		return fullName.toString();
	}
	
	@Deprecated
	public final boolean hasAdministratorRights() {
		return isMemberOf(AuthRole.System.administrator);
	}
	
	@Deprecated
	public final boolean hasPowerUserRights() {
		return isMemberOf(AuthRole.System.power_user);
	}
	
	public final boolean isMemberOf(AuthRole.System role) {
		boolean isMemberOf = false;
		if(rolesList != null && !rolesList.isEmpty()) {
			for (Iterator<AEDescriptor> iterator = rolesList.iterator(); iterator.hasNext();) {
				AEDescriptor roleDescr = (AEDescriptor) iterator.next();
				if(roleDescr.getSysId() == role.getSystemID()) {
					isMemberOf = true;
					break;
				}
			}
		}
		return isMemberOf;
	}

	public String geteMail() {
		return eMail;
	}

	public void seteMail(String eMail) {
		this.eMail = eMail;
	}

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	public boolean isLoggedIn() {
		return loggedIn;
	}

	public void setLoggedIn(boolean loggedIn) {
		this.loggedIn = loggedIn;
	}

	public String getLastHostAddress() {
		return lastHostAddress;
	}

	public void setLastHostAddress(String lastHostAddress) {
		this.lastHostAddress = lastHostAddress;
	}

	public Date getLastPasswordChangeTime() {
		return lastPasswordChangeTime;
	}

	public void setLastPasswordChangeTime(Date lastPasswordChangeTime) {
		this.lastPasswordChangeTime = lastPasswordChangeTime;
	}

	public Date getLastLoginTime() {
		return lastLoginTime;
	}

	public void setLastLoginTime(Date lastLoginTime) {
		this.lastLoginTime = lastLoginTime;
	}

	public Date getLastFailedLoginTime() {
		return lastFailedLoginTime;
	}

	public void setLastFailedLoginTime(Date lastFailedLoginTime) {
		this.lastFailedLoginTime = lastFailedLoginTime;
	}

	public Date getExpirationTime() {
		return expirationTime;
	}

	public void setExpirationTime(Date expirationTime) {
		this.expirationTime = expirationTime;
	}

	public int getFailedLoginCount() {
		return failedLoginCount;
	}

	public void setFailedLoginCount(int failedLoginCount) {
		this.failedLoginCount = failedLoginCount;
	}
	
	public void ownershipValidator(long compId) throws AEException {
		if(this.companiesSet == null || this.companiesSet.isEmpty()) {
			logger.errorv("this.companiesSet is null or empty in {0}#{1}: {2}", this.getClass().getSimpleName(), "ownershipValidator", AEError.System.UNSUFFICIENT_RIGHTS.getMessage());
			throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
		}
		
		boolean ownership = this.companiesSet.contains(Organization.lazyDescriptor(compId));
		
		if(!ownership) {
			logger.errorv("this.companiesSet does not contain specified compId in {0}#{1}: {2}", this.getClass().getSimpleName(), "ownershipValidator", AEError.System.UNSUFFICIENT_RIGHTS.getMessage());
			throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
		}
	}

	public AEDescriptor getExternalPrincipal() {
		return externalPrincipal;
	}

	public void setExternalPrincipal(AEDescriptor externalPrincipal) {
		this.externalPrincipal = externalPrincipal;
	}
	
	/**
	 * Package visible method
	 * 
	 * @param perm
	 * @throws AuthException
	 */
	void checkPermission(AuthPermission perm) throws AuthException {
		if (perm == null) {
			throw new NullPointerException("Security Violation: Permission can't be null");
		}

		// common validation
		try {
			validateWith(AuthPrincipalCommonValidator.getInstance());
		} catch (AEException e) {
			throw (AuthException) e;
		}

		// check permission
		boolean permitted = true;
		
		// principal level
		// the principal level overrides the role level 

		// denies first
		for (Iterator<AEDescriptor> iterator = rolesList.iterator(); iterator.hasNext();) {
			AEDescriptor roleDescr = (AEDescriptor) iterator.next();
			PermissionCollection pc = AuthAccessController.getPermissionCollectionDeny(roleDescr);
			if(pc instanceof AuthPermissionCollection && ((AuthPermissionCollection) pc).denies(perm)) {
				permitted = false;
				break;
			}
		}
		if(!permitted) {
			throw new AuthException();
		}
		
		// implies second
		permitted = false;
		for (Iterator<AEDescriptor> iterator = rolesList.iterator(); iterator.hasNext();) {
			AEDescriptor roleDescr = (AEDescriptor) iterator.next();
			PermissionCollection pc = AuthAccessController.getPermissionCollection(roleDescr);
			if(pc != null && pc.implies(perm)) {
				permitted = true;
				break;
			}
		}
		
		if(!permitted) {
			throw new AuthException();
		}
	}
	
	public boolean isAuthenticated() {
		return authenticated;
	}

	public void setAuthenticated(boolean authenticated) {
		this.authenticated = authenticated;
	}

	public boolean implies(Subject subject) {
		return false;
	}
	
	public static boolean isValidWindowsPassword(String pw) {
		if(pw.length() < 6) {
			return false;
		}
		
		// iterate over the patterns looking for matches
		int matchCount = 0;
		for (Pattern thisPattern : windowsPasswordPatternArray) {
			Matcher theMatcher = thisPattern.matcher(pw);        
			if (theMatcher.find()) {
				matchCount ++;
			}
		}
		return matchCount >= 3;
	}
}
