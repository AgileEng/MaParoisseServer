package eu.agileeng.domain.acc;

import eu.agileeng.domain.AEException;
import eu.agileeng.util.AEValidator;

public class AccJournalEntryCommonValidator implements AEValidator {
	
	private static AccJournalEntryCommonValidator inst = new AccJournalEntryCommonValidator();
	
	/**
	 * 
	 */
	private AccJournalEntryCommonValidator() {
	}

	public static AccJournalEntryCommonValidator getInst() {
		return inst;
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.util.AEValidator#validate(java.lang.Object)
	 */
	@Override
	public void validate(Object o) throws AEException {
		if(o instanceof AccJournalEntry) {
			AccJournalEntry jEntry = (AccJournalEntry) o;
			AccJournalItemsList accJournalItems = jEntry.getAccJournalItems();
			if(accJournalItems != null) {
				for (AccJournalItem accJournalItem : accJournalItems) {
					AccJournalItemCommonValidator.getInst().validate(accJournalItem);
				}
			}
		}
	}
}
