package eu.agileeng.util;

import java.math.BigDecimal;

import eu.agileeng.domain.AEException;

public class RibValidator implements AEValidator {
	
	private static RibValidator inst = new RibValidator();

	/**
	 * 
	 */
	private RibValidator() {
	}

	public static RibValidator getInstance() {
		return inst;
	}

	@Override
	public void validate(Object o) throws AEException {
		if(o instanceof String) {
			String rib = (String) o;
			StringBuilder extendedRib = new StringBuilder(rib.length());
			for (char currentChar : rib.toCharArray()) {
				//Works on base 36
				int currentCharValue = Character.digit(currentChar, Character.MAX_RADIX);
				//Convert character to simple digit
				extendedRib.append(currentCharValue<10?currentCharValue:(currentCharValue + (int) StrictMath.pow(2,(currentCharValue-10)/9))%10);
			}
			boolean isValid = new BigDecimal(extendedRib.toString()).remainder(new BigDecimal(97)).intValue() == 0;
			if(!isValid) {
				throw new AEException("Vérifiez un RIB (" + rib + ") a échoué");
			}
		}
	}
}
