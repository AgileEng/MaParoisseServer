/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 13.06.2010 22:09:57
 */
package eu.agileeng.domain.common;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.imp.AEDescriptorImp;

/**
 *
 */
@SuppressWarnings("serial")
public class AEComment extends AEDomainObject {

	private AEDescriptive toObject;
	
	/**
	 * @param clazz
	 */
	public AEComment() {
		super(DomainClass.COMMENT);
	}

	/**
	 * @return the toObject
	 */
	public AEDescriptive getToObject() {
		return toObject;
	}

	/**
	 * @param toObject the toObject to set
	 */
	public void setToObject(AEDescriptive toObject) {
		this.toObject = toObject;
	}

	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.COMMENT);
	}
}
