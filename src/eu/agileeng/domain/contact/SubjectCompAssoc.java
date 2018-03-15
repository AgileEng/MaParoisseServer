/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 09.06.2010 19:29:38
 */
package eu.agileeng.domain.contact;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel;

/**
 *
 */
@SuppressWarnings("serial")
public class SubjectCompAssoc extends AEDomainObject {

	private AEDescriptive 	authSubject;
	
	/**
	 * @param clazz
	 */
	public SubjectCompAssoc() {
		super(DomainModel.DomainClass.SubbjectCompAssoc);
	}
	
	/**
	 * @param clazz
	 */
	public SubjectCompAssoc(AEDescriptive authSubject, AEDescriptive company) {
		this();
		setCompany(company);
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
}
