/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 09.11.2009 15:13:00
 */
package eu.agileeng.domain;

import java.io.Serializable;

import eu.agileeng.domain.DomainModel.DomainClass;


/**
 * 
 */
public interface AEObject extends Serializable {
	
	public static class Property {
		public static final long SYSTEM = 1L;
		public static final long ACTIVE = 1L << 1;
		public static final long LOCKED = 1L << 2;
		public static final long MONBUREAU = 1L << 3;
	}
	
	public void setClazz(DomainClass clazz);
	
	public DomainClass getClazz();
	
	public void setSequenceNumber(long number);
	
	public long getSequenceNumber();
	
	public void setCode(String code);
	
	public String getCode();
	
	public void setOption(String option);
	
	public String getOption();
	
	public void setName(String name);
	
	public String getName();
	
	public void setAlias(String alias);
	
	public String getAlias();
	
	public void setDescription(String description);
	
	public String getDescription();
	
	public void setAbbreviation(String abbreviation);
	
	public String getAbbreviation();
	
	public void setNote(String note);
	
	public String getNote();
	
	public void setProperties(long properties);
	
	public long getProperties();
	
	public void setProperty(long property);
	
	public boolean hasProperty(long property);
	
	public void removeProperty(long property);
	
	public void setCompany(AEDescriptive company);
	
	public AEDescriptive getCompany();
}
