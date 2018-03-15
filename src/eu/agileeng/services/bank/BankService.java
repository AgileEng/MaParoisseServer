/**
 * 
 */
package eu.agileeng.services.bank;

import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.business.bank.BankRecognitionRulesList;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEService;

/**
 * @author vvatov
 *
 */
public interface BankService extends AEService {

	public JSONObject loadRecognitionRules(AERequest aeRequest) throws AEException;
	
	public void importEbicsFiles(AEInvocationContext invContext) throws AEException;

	public BankRecognitionRulesList saveRecognitionRules(AERequest aeRequest) throws AEException;

}
