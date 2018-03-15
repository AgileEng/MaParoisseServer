/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 09.11.2009 16:36:36
 */
package eu.agileeng.domain.imp;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEObject;
import eu.agileeng.domain.DomainModel.DomainClass;

/**
 *
 */
@SuppressWarnings("serial")
public class AEObjectImp implements AEObject {

	private DomainClass clazz;
	
	private long sequenceNumber;
	
	private String code;
	
	private String option;
	
	private String name;
	
	private String alias;
	
	private String description;
	
	private String abbreviation;
	
	private String note;
	
	private long properties;
	
	private AEDescriptive company;
	
	/**
	 * 
	 */
	public AEObjectImp(DomainClass clazz) {
		this.clazz = clazz;
	}

	public void setSequenceNumber(long number) {
		sequenceNumber = number;
	}
	
	public long getSequenceNumber() {
		return sequenceNumber;
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.domain.IAEObject#getAbbreviation()
	 */
	@Override
	public String getAbbreviation() {
		return this.abbreviation;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.IAEObject#getAlias()
	 */
	@Override
	public String getAlias() {
		return this.alias;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.IAEObject#getCode()
	 */
	@Override
	public String getCode() {
		return this.code;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.IAEObject#getDescription()
	 */
	@Override
	public String getDescription() {
		return this.description;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.IAEObject#getName()
	 */
	@Override
	public String getName() {
		return this.name;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.IAEObject#getOption()
	 */
	@Override
	public String getOption() {
		return this.option;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.IAEObject#setAbbreviation(java.lang.String)
	 */
	@Override
	public void setAbbreviation(String abbreviation) {
		this.abbreviation = abbreviation;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.IAEObject#setAlias(java.lang.String)
	 */
	@Override
	public void setAlias(String alias) {
		this.alias = alias;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.IAEObject#setCode(java.lang.String)
	 */
	@Override
	public void setCode(String code) {
		this.code = code;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.IAEObject#setDescription()
	 */
	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.IAEObject#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.IAEObject#setOption(java.lang.String)
	 */
	@Override
	public void setOption(String option) {
		this.option = option;
	}

	@Override
	public DomainClass getClazz() {
		return this.clazz;
	}

	@Override
	public void setClazz(DomainClass clazz) {
		this.clazz = clazz;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEObject#getNote()
	 */
	@Override
	public String getNote() {
		return note;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEObject#setNote(java.lang.String)
	 */
	@Override
	public void setNote(String note) {
		this.note = note;
	}
	
	public void setProperties(long properties) {
		this.properties = properties;
	}
	
	public long getProperties() {
		return this.properties;
	}
	
	public void setProperty(long property) {
		this.properties |= property;
	}
	
	public void removeProperty(long property) {
		this.properties &= (~property);
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEObject#getCompany()
	 */
	@Override
	public AEDescriptive getCompany() {
		return this.company;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEObject#setCompany(eu.agileeng.domain.AEDescriptive)
	 */
	@Override
	public void setCompany(AEDescriptive company) {
		this.company = company;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEObject#hasProperty(long)
	 */
	@Override
	public boolean hasProperty(long property) {
		return (this.properties & property) == property;
	}
}
