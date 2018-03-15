package eu.agileeng.services;

import eu.agileeng.domain.AEException;
import eu.agileeng.util.AEValidator;

public class AERequestValidator implements AEValidator {

	private static AERequestValidator inst = null;
	
	/**
	 * 
	 */
	private AERequestValidator() {
	}

	public static AERequestValidator getInstance() {
        if (inst == null) {
            synchronized (AERequestValidator.class) {
                if (inst == null) {
                	inst = new AERequestValidator();
                }
            }
        }
        return inst;
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.util.AEPredicate#evaluate(java.lang.Object)
	 */
	@Override
	public void validate(Object o) throws AEException {
		
	}
}
