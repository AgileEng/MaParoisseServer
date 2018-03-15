/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 20.05.2010 17:07:58
 */
package eu.agileeng.security;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel;

/**
 *
 */
@SuppressWarnings("serial")
public class AuthSubjectRoleAssoc extends AEDomainObject {

	private AEDescriptive 	authSubject;
	
	private AEDescriptive 	authRole;
	
	/**
	 * @param clazz
	 */
	public AuthSubjectRoleAssoc() {
		super(DomainModel.DomainClass.AuthRoleSubAssoc);
	}
	
	/**
	 * @param clazz
	 */
	public AuthSubjectRoleAssoc(AEDescriptive authSubject, AEDescriptive authRole) {
		this();
		this.authRole = authRole;
		this.authSubject = authSubject;
	}

	/**
	 * @return the authSubject
	 */
	public AEDescriptive getAuthSubject() {
		return authSubject;
	}

	/**
	 * @param authSubject the authSubject to set
	 */
	public void setAuthSubject(AEDescriptive authSubject) {
		this.authSubject = authSubject;
	}

	/**
	 * @return the authRole
	 */
	public AEDescriptive getAuthRole() {
		return authRole;
	}

	/**
	 * @param authRole the authRole to set
	 */
	public void setAuthRole(AEDescriptive authRole) {
		this.authRole = authRole;
	}
}
