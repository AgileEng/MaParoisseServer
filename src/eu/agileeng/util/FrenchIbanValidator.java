package eu.agileeng.util;

import java.math.BigDecimal;

import eu.agileeng.domain.AEException;

public class FrenchIbanValidator implements AEValidator {
	
	private static final BigDecimal ibanCheckingConstant =  new  BigDecimal(97) ; 
	
	private static FrenchIbanValidator inst = new FrenchIbanValidator();

	/**
	 * 
	 */
	private FrenchIbanValidator() {
	}

	public static FrenchIbanValidator getInstance() {
		return inst;
	}

	@Override
	public void validate(Object o) throws AEException {
		if(o instanceof String) {
			String iban = (String) o;
			try {
				// check IBAN
				if(iban.length() < 5) {
					throw new AEException();
				}
				String iso13616Prepared = ISO13616Prepare(iban);
				BigDecimal dIban = new BigDecimal(iso13616Prepared);
				int remaider = dIban.remainder(ibanCheckingConstant).intValue();
				if(remaider != 1) {
					throw new AEException();
				}
				
				// check the RIB
				RibValidator.getInstance().validate(iban.substring(4));
			} catch (Exception e) {
				throw new AEException("Vérifiez un IBAN (" + iban + ") a échoué");
			}
		}
	}
	
	public String ISO13616Prepare(String iban) {
		String isostr = iban.toUpperCase();
		isostr = isostr.substring(4) + isostr.substring(0,4);
		for (int i = 0; i <= 25; i++) {
			String letter = Character.toString((char)(i+65));
			while (isostr.indexOf(letter) != -1) {
				isostr = isostr.replace(letter, new Integer(i+10).toString());
			}
		}
		return isostr; 
	}
}
