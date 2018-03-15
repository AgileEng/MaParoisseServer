package eu.agileeng.domain.document.social.rib;

import eu.agileeng.domain.AEException;
import eu.agileeng.util.AEValidator;

public class RibSaveValidator implements AEValidator {

	private static RibSaveValidator inst = new RibSaveValidator();
	
	/**
	 * 
	 */
	private RibSaveValidator() {
	}

	public static RibSaveValidator getInstance() {
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
