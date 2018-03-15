/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 24.05.2010 21:34:22
 */
package eu.agileeng.domain.document.social.findutravail;

import eu.agileeng.domain.AEException;
import eu.agileeng.util.AEValidator;

/**
 *
 */
public class FinDuTravailSaveValidator implements AEValidator {

	private static FinDuTravailSaveValidator inst = new FinDuTravailSaveValidator();
	
	/**
	 * 
	 */
	private FinDuTravailSaveValidator() {
	}

	public static FinDuTravailSaveValidator getInstance() {
		return inst;
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.util.AEValidator#validate(java.lang.Object)
	 */
	@Override
	public void validate(Object o) throws AEException {
		return;
	}
}
