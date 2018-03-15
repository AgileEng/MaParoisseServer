/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 23.05.2010 19:00:36
 */
package eu.agileeng.domain.document;

import java.util.Date;

import eu.agileeng.domain.AEDescriptor;

/**
 *
 */
public interface AEDocumentDescriptor extends AEDescriptor {
	public void setDocumentType(AEDocumentType docType);
	public AEDocumentType getDocumentType();
	
	public Date getDate();
	public void setDate(Date date);
	public String getNumber();
	public void setNumber(String number);
	
}
