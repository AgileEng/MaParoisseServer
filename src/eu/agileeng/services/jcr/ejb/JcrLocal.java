/**
 * 
 */
package eu.agileeng.services.jcr.ejb;

import java.nio.file.Path;

import javax.ejb.Local;

import org.apache.tomcat.util.json.JSONArray;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.jcr.JcrFile;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.jcr.JcrService;

/**
 * @author vvatov
 *
 */
@Local
public interface JcrLocal extends JcrService {
	public void copyStructure(long fromId, long toId, AEInvocationContext invContext) throws AEException;
	public JcrFile getJcrFileWithContent(String path, long ownerId, AEInvocationContext invContext) throws AEException;
	
	public Path getModuleRepositoryPath(AERequest aeRequest,  AEInvocationContext invContext) throws AEException;
	
	public AEResponse getDownloadPath(AERequest aeRequest,  AEInvocationContext invContext) throws AEException;
	
	public AEResponse getAttachmentPath(AERequest aeRequest,  AEInvocationContext invContext) throws AEException;
	
	public AEResponse upload(AERequest aeRequest,  AEInvocationContext invContext) throws AEException;
	
	public AEResponse delete(AERequest aeRequest,  AEInvocationContext invContext) throws AEException;
	
	public JSONArray listAttachedFiles(AERequest aeRequest,  AEInvocationContext invContext) throws AEException;
}
