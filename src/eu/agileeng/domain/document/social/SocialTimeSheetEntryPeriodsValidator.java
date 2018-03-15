package eu.agileeng.domain.document.social;

import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.util.AETimePeriod;
import eu.agileeng.util.AEValidator;

public class SocialTimeSheetEntryPeriodsValidator implements AEValidator {

	private static SocialTimeSheetEntryPeriodsValidator inst = new SocialTimeSheetEntryPeriodsValidator();
	
	/**
	 * 
	 */
	private SocialTimeSheetEntryPeriodsValidator() {
	}

	public static SocialTimeSheetEntryPeriodsValidator getInstance() {
		return inst;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.util.AEValidator#validate(java.lang.Object)
	 */
	@Override
	public void validate(Object o) throws AEException { 
		if(!(o instanceof SocialTimeSheetEntry)) {
			return;
		}
		SocialTimeSheetEntry entry = (SocialTimeSheetEntry) o;
		
		/**
		 * Planning 
		 */
		AETimePeriod period1 = new AETimePeriod(entry.getFromTime_1(), entry.getToTime_1());
		if(!period1.isValid()) {
			throw AEError.System.SOCIAL_INVALID_PERIOD.toException();
		}
		
		AETimePeriod period2 = new AETimePeriod(entry.getFromTime_2(), entry.getToTime_2());
		if(!period2.isValid()) {
			throw AEError.System.SOCIAL_INVALID_PERIOD.toException();
		}
		
		if(!period1.isNull() && !period2.isNull()
				&& !period1.before(period2) && !period2.before(period1)) {
			throw AEError.System.SOCIAL_INVALID_PERIOD.toException();
		}
		
		/**
		 * Actual working
		 */
		AETimePeriod periodActual1 = new AETimePeriod(entry.getFromTimeActual_1(), entry.getToTimeActual_1());
		if(!periodActual1.isValid()) {
			throw AEError.System.SOCIAL_INVALID_PERIOD.toException();
		}
		
		AETimePeriod periodActual2 = new AETimePeriod(entry.getFromTimeActual_2(), entry.getToTimeActual_2());
		if(!periodActual2.isValid()) {
			throw AEError.System.SOCIAL_INVALID_PERIOD.toException();
		}
		
		AETimePeriod periodActual3 = new AETimePeriod(entry.getFromTimeActual_3(), entry.getToTimeActual_3());
		if(!periodActual3.isValid()) {
			throw AEError.System.SOCIAL_INVALID_PERIOD.toException();
		}
		
		if(!periodActual1.isNull() && !periodActual2.isNull()
				&& !periodActual1.before(periodActual2) && !periodActual2.before(periodActual1)) {
			throw AEError.System.SOCIAL_INVALID_PERIOD.toException();
		}
		
		if(!periodActual1.isNull() && !periodActual3.isNull()
				&& !periodActual1.before(periodActual3) && !periodActual3.before(periodActual1)) {
			throw AEError.System.SOCIAL_INVALID_PERIOD.toException();
		}
		
		if(!periodActual2.isNull() && !periodActual3.isNull()
				&& !periodActual2.before(periodActual3) && !periodActual3.before(periodActual2)) {
			throw AEError.System.SOCIAL_INVALID_PERIOD.toException();
		}
	}
}
