/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 13.06.2010 16:21:15
 */
package eu.agileeng.services.file.ejb;

import javax.ejb.Local;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.file.FileAttachment;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.file.FileAttachmentService;

/**
 *
 */
@Local
public interface FileAttachmentLocal extends FileAttachmentService {
	public FileAttachment manage(FileAttachment fileAttachment, AEInvocationContext invContext, AEConnection aeConnection) throws AEException;
}
