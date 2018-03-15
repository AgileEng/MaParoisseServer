package eu.agileeng.domain.paroisse;

import org.apache.commons.validator.routines.RegexValidator;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.util.AEValidator;

/**
 * Validates JSONObject representing a Paroisse
 * 
 * @author vvatov
 *
 */
public class ParoisseValidator implements AEValidator {

	private static RegexValidator codeValidator = new RegexValidator("\\d{4}");
	
	private static ParoisseValidator inst = new ParoisseValidator();
	
	public static ParoisseValidator getInstance() {
		return inst;
	}
	
	@Override
	public void validate(Object o) throws AEException {
		boolean isValid = false;
		if(o instanceof JSONObject) {
			JSONObject paroisseJson = (JSONObject) o; 
			String code = paroisseJson.getString(AEDomainObject.JSONKey.code.name());
			isValid = codeValidator.isValid(code);
			if(isValid) {
				// TODO Go with next validations ...
				
				//validate name
				RegexValidator nameValidator = new RegexValidator("^([^a-z]{1,30})$");
				if (!nameValidator.isValid(paroisseJson.getString(AEDomainObject.JSONKey.name.name()))) {
					throw new AEException("Name must have capital letters and no more than 30 characters!");
				}
				nameValidator = null;
				
				//validate description/nature
				RegexValidator dValidator = new RegexValidator("^(.{1,32})$");
				if (!dValidator.isValid(paroisseJson.getString(AEDomainObject.JSONKey.description.name()))) {
					throw new AEException("\"Nature\" must be no more than 32 characters");
				}
				dValidator = null;
				
				//validate statut
				RegexValidator sValidator = new RegexValidator("^(.{1,20})$");
				if (!sValidator.isValid(paroisseJson.getString("paroisseStatut"))) {
					throw new AEException("Statut must be no more than 20 characters!");
				}
				sValidator = null;
				
				//validate postal code
				RegexValidator pcValidator = new RegexValidator("^(\\d{5})$");
				if (!pcValidator.isValid(paroisseJson.getString("postCode"))) {
					throw new AEException("Postal Code must contain only numbers and have exactly 5 digits!");
				}
				pcValidator = null;
				
				//validate town/ville
				RegexValidator tValidator = new RegexValidator("^([^a-z]{1,25})$");
				if (!tValidator.isValid(paroisseJson.getString("town"))) {
					throw new AEException("Ville must have capital letters and no more than 25 characters!");
				}
				tValidator = null;
				
			}
		}
		if(!isValid) {
			throw new AEException(AEError.System.INVALID_REQUEST);
		}
	}
}
