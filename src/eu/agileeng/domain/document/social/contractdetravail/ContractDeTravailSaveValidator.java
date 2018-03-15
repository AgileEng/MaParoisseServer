/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 18.06.2010 10:38:43
 */
package eu.agileeng.domain.document.social.contractdetravail;

import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.AEObject;
import eu.agileeng.domain.document.AEDocument;
import eu.agileeng.util.AEValidator;

/**
 *
 */
public class ContractDeTravailSaveValidator implements AEValidator {
	
	private static ContractDeTravailSaveValidator inst = new ContractDeTravailSaveValidator();
	
	/**
	 * 
	 */
	private ContractDeTravailSaveValidator() {
	}

	public static ContractDeTravailSaveValidator getInstance() {
		return inst;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.util.AEValidator#validate(java.lang.Object)
	 */
	@Override
	public void validate(Object o) throws AEException { 
		if(o instanceof AEDocument) {
			AEDocument doc = (AEDocument) o;
			if(doc.hasProperty(AEObject.Property.LOCKED)) {
				throw AEError.System.DOCUMENT_IS_LOCKED.toException();
			}
		}
	}
}
