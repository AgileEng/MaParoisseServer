package eu.agileeng.util;

import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;

public class InseeValidator implements AEValidator {

    /// Constante pour le calcul de la cle
    private static final long CLE_VERIF = 97;
	
    private static final int INDICE_LETTRE_CORSE = 6;
    
    // Constante pour calcul Corse 2A
    private final static long  CORSEA = 1000000;
    
    // Constante pour calcul Corse 2B
    private final static long CORSEB = 2000000;
    
    private static final String regex = 
//    	"^[1278][0-9]{2}(0[1-9]|1[0-2]|20)([02][1-9]|2[AB]|[1345678][0-9]|9[012345789])[0-9]{3}(00[1-9]|0[1-9][0-9]|[1-9][0-9]{2})(0[1-9]|[1-8][1-9]|9[1-7])$";
        "^[1278]\\d{2}(0[1-9]|1[0-2]|20)(\\d{2}|2[AB])(\\d{3})(\\d{3})(\\d{2})$";
    
	private static final InseeValidator inst = new InseeValidator();

	/**
	 * 
	 */
	private InseeValidator() {
	}

	public static InseeValidator getInstance() {
		return inst;
	}
    
	@Override
	public void validate(Object o) throws AEException {
		try {
			boolean res = false;
			if(o instanceof String) {
				String insee = normalizeInsee((String) o);
				if(insee.matches(regex)) {
					// validate cle
					Long lCle = calculateCle(insee.substring(0, 13));
					String strCle = insee.substring(13, 15);
					Long longCle = Long.parseLong(strCle);
					res = longCle.equals(lCle);
				}
			}
			if(!res) {
				throw AEError.System.SOCIAL_INVALID_UIN.toException();
			}
		} catch (Exception e) {
			// catch in case of NumberFormatException (parse is not possible)
			throw AEError.System.SOCIAL_INVALID_UIN.toException();
		}
	}

    /**
     * Calculate the key.
     * <code>nir</code> must be already normalized and regex matched
     */
    private Long calculateCle(String nir) {
        return CLE_VERIF - (nirToLong(nir) % CLE_VERIF);
    }

    /**
     *  Removes characters that can not be part of the INSEE
     *  [A-Z0-9] only
     */
    private String normalizeInsee(String rawInsee) {
        return rawInsee.toUpperCase().replaceAll("[^A-Z0-9]", AEStringUtil.EMPTY_STRING);
    }

    /**
     * Converts the NIR to long.
     * <code>nir</code> must be already normalized and regex matched
     */
    private long nirToLong(String nir) {
        long lNumber = 0;
        if(nir.charAt(INDICE_LETTRE_CORSE) == 'A') {
        	lNumber = Long.parseLong(nir.replace('A', '0'));
        	lNumber -= CORSEA;
        } else if(nir.charAt(INDICE_LETTRE_CORSE) == 'B') {
           	lNumber = Long.parseLong(nir.replace('B', '0'));
        	lNumber -= CORSEB;
        } else {
        	lNumber = Long.parseLong(nir);
        }
        return lNumber;
    }
}
