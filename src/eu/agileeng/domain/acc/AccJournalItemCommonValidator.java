package eu.agileeng.domain.acc;

import eu.agileeng.domain.AEException;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AEValidator;

public class AccJournalItemCommonValidator implements AEValidator {

	private static AccJournalItemCommonValidator inst = new AccJournalItemCommonValidator();
	
	/**
	 * 
	 */
	private AccJournalItemCommonValidator() {
	}

	public static AccJournalItemCommonValidator getInst() {
		return inst;
	}
	
	@Override
	public void validate(Object o) throws AEException {
		if(o instanceof AccJournalItem) {
			AccJournalItem accJournalItem = (AccJournalItem) o;
			if(accJournalItem.getAccount() == null) {
				throw new AEException("Eléments du journal comptable: Compte comptable absent. ");
			}
			if(accJournalItem.getJournal() == null || AEStringUtil.isEmpty(accJournalItem.getJournal().getCode())) {
				throw new AEException("Eléments du journal comptable: Journal comptable absent. ");
			}
			if(!(accJournalItem.isCredit() ^ accJournalItem.isDebit())) {
				throw new AEException("Vous ne pouvez pas saisir à la fois en débit et en crédit.<br>Saisissez le solde ou utilisez plusieurs lignes. ");
			}

			double dtAmount = AEMath.doubleValue(accJournalItem.getDtAmount());
			double ctAmount = AEMath.doubleValue(accJournalItem.getCtAmount());
			if(!AEMath.isZeroAmount(dtAmount) && !AEMath.isZeroAmount(ctAmount)) {
				throw new AEException("Vous ne pouvez pas sélectionner débit et crédit en même temps.<br>Faites un choix ou utilisez deux lignes si l'écriture doit se faire en débit et crédit. ");
			}
			
			if(accJournalItem.getDate() == null) {
				throw new AEException("Eléments du journal comptable: Date comptable absent. ");
			}
		}
	}

}
