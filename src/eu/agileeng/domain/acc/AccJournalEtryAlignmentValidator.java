/**
 * 
 */
package eu.agileeng.domain.acc;

import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.util.AEValidator;

/**
 * @author vvatov
 *
 */
public class AccJournalEtryAlignmentValidator implements AEValidator {

	private static final AccJournalEtryAlignmentValidator inst = new AccJournalEtryAlignmentValidator();
	
	/**
	 * 
	 */
	private AccJournalEtryAlignmentValidator() {
	}

	public static AccJournalEtryAlignmentValidator getInst() {
		return inst;
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.util.AEValidator#validate(java.lang.Object)
	 */
	@Override
	public void validate(Object o) throws AEException {
		if(o instanceof AccJournalEntry) {
			AccJournalEntry jEntry = (AccJournalEntry) o;
			if(!jEntry.isBalanced() || !jEntry.isItemsTheSameDate()) {
				throw new AEException(
						AEError.System.ACC_JOURAL_ETRY_NOT_ALIGED.getSystemID(),
						"Votre écriture n’est pas équilibrée.");
			}
		}
	}
}
