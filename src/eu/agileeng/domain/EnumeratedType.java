/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 03.11.2009 20:36:52
 */
package eu.agileeng.domain;

import eu.agileeng.domain.DomainModel.DomainClass;


/**
 * An enumerated type is a type whose instances describe values, where the set of possible values is finite. 
 * Typically, an enumerated type is used when the most important information is the 
 * existence of the value. If the values have extensive data or nontrivial operations 
 * associated with them, then they are not usually considered enumerated types. 
 * From a purely analytical standpoint, however, there is little difference between an enumerated type 
 * and any other type which has a finite set of possible values. 
 * A dynamic enumerated type is an enumerated type that can gain or lose values at run time. 
 * 
 * Enumerated types may be ordered or unordered, and ordered types may be singly or multiply ordered. 
 * Unordered types have no logical order. For example, the standard boolean type may be considered 
 * an unordered enumerated type: there is no logical reason to list the value true before or after the value false. 
 * For an ordered type, the possible values can be assigned a logical order. 
 * For example, an enumerated type covering the stages of a frog's life (egg, tadpole, adult) has a logical order. 
 * A multiply ordered type has more than one natural order. 
 * For example, a library catalogue may be able to present the collection of books 
 * in various catalogue systems (Library of Congress, Dewey Decimal, etc.) 
 * as well as alphabetical by author or title 
 * (although it is a stretch to imagine the books as an enumerated type).
 */
@SuppressWarnings("serial")
public abstract class EnumeratedType extends AEDomainObject {
	
	private long systemID;
	
	protected EnumeratedType(DomainClass clazz) {
		super(clazz);
	}

	protected EnumeratedType(DomainClass clazz, long idInst) {
		super(clazz);
		this.systemID = idInst;
		if(DomainClass.TRANSIENT.equals(clazz)) {
			setID(idInst);
		}
	}
	
	/**
	 * @return the IDInst
	 */
	public long getSystemID() {
		return systemID;
	}

	/**
	 * @param systemID the IDInst to set
	 */
	public void setSystemID(long systemID) {
		this.systemID = systemID;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (int) (systemID ^ (systemID >>> 32));
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		EnumeratedType other = (EnumeratedType) obj;
		if (systemID != other.systemID)
			return false;
		return true;
	}
}
