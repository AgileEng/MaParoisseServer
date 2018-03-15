/**
 * 
 */
package eu.agileeng.domain.document.social.accidentdutravail;

import eu.agileeng.domain.AEException;
import eu.agileeng.util.AEValidator;

/**
 * @author vvatov
 *
 */
public class AccidentDuTravailSaveValidator implements AEValidator {

	private static AccidentDuTravailSaveValidator inst = new AccidentDuTravailSaveValidator();
	
	/**
	 * 
	 */
	private AccidentDuTravailSaveValidator() {
	}

	public static AccidentDuTravailSaveValidator getInstance() {
		return inst;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.util.AEValidator#validate(java.lang.Object)
	 */
	@Override
	public void validate(Object o) throws AEException { 
	}
}
