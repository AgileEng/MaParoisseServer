package eu.agileeng.domain.facturation;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.util.AEValidator;

public class AEClientValidator implements AEValidator {
	
	private static AEClientValidator inst = new AEClientValidator();

	/**
	 * 
	 */
	private AEClientValidator() {
	}

	public static AEClientValidator getInstance() {
		return inst;
	}

	@Override
	public void validate(Object o) throws AEException {
		if(o instanceof AEClient) {
			AEClient c = (AEClient) o;
			
			// owner
			AEDescriptive ownerDescr = c.getCompany();
			if(ownerDescr == null || ownerDescr.getDescriptor().getID() <= 0) {
				throw AEError.System.OWNER_ID_NOT_SPECIFIED.toException();
			}
		} else {
			throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
		}
	}
}
