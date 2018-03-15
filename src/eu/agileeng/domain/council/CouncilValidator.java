package eu.agileeng.domain.council;

import org.apache.commons.validator.routines.RegexValidator;
import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEException;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.util.AEValidator;

public class CouncilValidator implements AEValidator {

	private static CouncilValidator inst = new CouncilValidator();

	public static CouncilValidator getInstance() {
		return inst;
	}

	@Override
	public void validate(Object o) throws AEException {
		if (o instanceof JSONObject) {
			JSONObject councilJson = (JSONObject) o;

			JSONArray members = councilJson.getJSONArray("members");

			RegexValidator notEmptyValidator = new RegexValidator("^(.+)$");
//			RegexValidator postCodeValidator = new RegexValidator("^(\\d{5})$");

			for (int i = 0; i < members.length(); i++) {
				JSONObject member = members.getJSONObject(i);

				// don't validate member for delete
				if(member.has(AEDomainObject.JSONKey.dbState.name())) {
					int dbState = member.getInt(AEDomainObject.JSONKey.dbState.name());
					if(dbState == AEPersistentUtil.DB_ACTION_DELETE) {
						continue;
					}
				}

				if (!notEmptyValidator.isValid(member.getJSONObject("employee").getString("firstName"))
						|| !notEmptyValidator.isValid(member.getJSONObject("employee").getString("lastName"))
						|| (member.getLong("positionId") != CouncilMember.Position.none.getId() && member.getLong("positionId") != CouncilMember.Position.secretaire.getId()
						&& (   !notEmptyValidator.isValid(member.getJSONObject("employee").getJSONObject("address").getString("address"))
								|| !notEmptyValidator.isValid(member.getJSONObject("employee").getJSONObject("address").getString("town"))
								|| !notEmptyValidator.isValid(member.getJSONObject("employee").getJSONObject("address").getString("postCode"))
								|| !notEmptyValidator.isValid(member.getJSONObject("employee").getJSONObject("contact").getString("phone"))
								/*|| !notEmptyValidator.isValid(member.getJSONObject("employee").getJSONObject("contact").getString("email"))*/))) 
				{

					throw new AEException("There are empty required fields");
				}
			}

		}

	}

}
