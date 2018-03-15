package eu.agileeng.services.export;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEException;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AEService;

public interface DocumentExportService extends AEService {

	JSONObject exportGridAsCsv(JSONArray columns, JSONArray data, AEInvocationContext invContext) throws AEException;

	public JSONObject handleXls(String base64Data, AEInvocationContext invContext) throws AEException;

}
