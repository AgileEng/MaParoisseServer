/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 13.06.2010 16:18:20
 */
package eu.agileeng.services.file;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.file.FileAttachment;
import eu.agileeng.domain.file.FileAttachmentFilter;
import eu.agileeng.domain.file.FileAttachmentsList;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.AEService;

/**
 *
 */
public interface FileAttachmentService extends AEService {
	public FileAttachment manage(FileAttachment fileAttachment, AEInvocationContext invService) throws AEException;
	public FileAttachment load(AEDescriptor fileAttachmentDescr, AEInvocationContext invContext) throws AEException;
	public FileAttachmentsList load(FileAttachmentFilter filter, AEInvocationContext invContext) throws AEException;
	public AEResponse manage(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
}
