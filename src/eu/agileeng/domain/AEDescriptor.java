/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 21.11.2009 17:03:07
 */
package eu.agileeng.domain;

import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.util.json.JSONSerializable;


/**
 *
 */
public interface AEDescriptor extends AEDescriptive, JSONSerializable {

	public void setID(long id);
	
	public long getID();
	
	public AEDescriptor withId(long id);
	
	public void setClazz(DomainClass clazz);
	
	public DomainClass getClazz();
	
	public void setCode(String code);
	
	public AEDescriptor withCode(String code);
	
	public String getCode();
	
	public void setName(String name);
	
	public AEDescriptor withName(String name);
	
	public String getDescription();
	
	public void setDescription(String description);
	
	public AEDescriptor withDescription(String description);
	
	public String getName();
	
	public boolean isPersistent();
	
	public void setProperties(long properties);
	
	public long getProperties();
	
	public void setSysId(long sysId);
	
	public long getSysId();
}
