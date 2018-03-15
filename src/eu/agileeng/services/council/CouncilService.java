package eu.agileeng.services.council;

import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.council.Council;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.AEService;

public interface CouncilService extends AEService {

	Council loadInitialData(JSONObject arguments, AEInvocationContext invContext) throws AEException;
	
	Council loadByDate(AERequest aeRequest, AEInvocationContext invContext) throws AEException;

	Council save(JSONObject arguments, AEInvocationContext invContext) throws AEException;

	Council closeCouncil(JSONObject arguments, AEInvocationContext invContext) throws AEException;
	
	public AEResponse loadEngTitres(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse saveEngTitres(AERequest aeRequest, AEInvocationContext invContext) throws AEException;

}
