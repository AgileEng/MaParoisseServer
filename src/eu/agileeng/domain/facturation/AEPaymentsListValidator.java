package eu.agileeng.domain.facturation;

import java.util.Iterator;

import eu.agileeng.domain.AEException;
import eu.agileeng.util.AEValidator;

public class AEPaymentsListValidator implements AEValidator {
	
	private static AEPaymentsListValidator inst = new AEPaymentsListValidator();
	
	/**
	 * 
	 */
	private AEPaymentsListValidator() {
	}

	public static AEPaymentsListValidator getInstance() {
		return inst;
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.util.AEValidator#validate(java.lang.Object)
	 */
	@Override
	public void validate(Object o) throws AEException {
		if(!(o instanceof AEPaymentsList)) {
			return;
		}
		
		AEPaymentsList pList = (AEPaymentsList) o;
		
		// must have the one Balance
		AEPayment balancePayment = null;
		for (Iterator<AEPayment> iterator = pList.iterator(); iterator.hasNext();) {
			AEPayment aePayment = (AEPayment) iterator.next();
			if(aePayment.getPaymentType() == AEFactureUtil.PaymentType.BALANCE) {
				if(balancePayment == null) {
					balancePayment = aePayment;
				} else {
					throw new AEException("System error: The one balance item predicate failed");
				}
			}
		}
	}

}
