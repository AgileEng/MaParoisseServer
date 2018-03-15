package eu.agileeng.domain.facturation;

import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AEValidator;

public class AEFactureSaveValidator implements AEValidator {
	
	private static AEFactureSaveValidator inst = new AEFactureSaveValidator();
	
	/**
	 * 
	 */
	private AEFactureSaveValidator() {
	}

	public static AEFactureSaveValidator getInstance() {
		return inst;
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.util.AEValidator#validate(java.lang.Object)
	 */
	@Override
	public void validate(Object o) throws AEException {
		if(!(o instanceof AEFacture)) {
			return;
		}
		
		AEFacture facture = (AEFacture) o;
		
		// client is mandatory
		if(facture.getClient() == null || AEStringUtil.isEmpty(facture.getClient().getName())) {
			throw AEError.System.FACTURATION_CLIENT_MANDATORY.toException();
		}
		
		// payment terms is mandatory
		if(facture.getPaymentTerms() == null) {
			throw AEError.System.FACTURATION_PAYMENT_TERMS_MANDATORY.toException();
		}
	}
}
