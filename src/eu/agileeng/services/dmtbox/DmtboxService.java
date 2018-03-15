package eu.agileeng.services.dmtbox;

import org.apache.tomcat.util.json.JSONArray;

import eu.agileeng.domain.AEException;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AEService;

public interface DmtboxService extends AEService {

	public int getOperatorId();
	
	public String getModuleName();

	public String generateVirtualBoxId(long ownerId, String virtualBoxName, AEInvocationContext invContext) throws AEException;
	
	public String getCustomerName(long ownerId, AEInvocationContext invContext) throws AEException;
	
	public Long getOwnerId(String virtualBoxId, AEInvocationContext invContext) throws AEException;

	public JSONArray loadVirtualBoxByOwnerId(long ownerId,
			AEInvocationContext invContext) throws AEException;

	public long validateOwnershipOfVirtualBoxId(long ownerId, String virtualBoxId,
			AEInvocationContext invContext) throws AEException;

	public void deleteVirtualBox(long ownerId, String virtualBoxId, AEInvocationContext invContext) throws AEException;

}
