package eu.agileeng.domain.facturation;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.util.AEValidator;

public class AEArticleValidator implements AEValidator {
	
	private static AEArticleValidator inst = new AEArticleValidator();

	/**
	 * 
	 */
	private AEArticleValidator() {
	}

	public static AEArticleValidator getInstance() {
		return inst;
	}

	@Override
	public void validate(Object o) throws AEException {
		if(o instanceof AEArticle) {
			AEArticle art = (AEArticle) o;
			
			// owner
			AEDescriptive ownerDescr = art.getCompany();
			if(ownerDescr == null || ownerDescr.getDescriptor().getID() <= 0) {
				throw AEError.System.OWNER_ID_NOT_SPECIFIED.toException();
			}
			
			// vat 
		} else {
			throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
		}
	}
}
