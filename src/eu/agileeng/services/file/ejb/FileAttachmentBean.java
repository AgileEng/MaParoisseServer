/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 13.06.2010 16:23:12
 */
package eu.agileeng.services.file.ejb;

import java.util.Date;

import javax.ejb.Stateless;

import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.common.AEComment;
import eu.agileeng.domain.common.AECommentsList;
import eu.agileeng.domain.file.FileAttachment;
import eu.agileeng.domain.file.FileAttachmentFilter;
import eu.agileeng.domain.file.FileAttachmentsList;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistent;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.common.AECommentDAO;
import eu.agileeng.persistent.dao.file.FileAttachmentDAO;
import eu.agileeng.security.AuthPermission;
import eu.agileeng.services.AEBean;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AEInvocationContextValidator;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;

/**
 *
 */
@SuppressWarnings("serial")
@Stateless
public class FileAttachmentBean extends AEBean implements FileAttachmentRemote, FileAttachmentLocal {

	/**
	 * 
	 */
	public FileAttachmentBean() {
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.services.file.FileAttachmentService#manage(eu.agileeng.domain.file.FileAttachment, eu.agileeng.domain.AEDescriptor)
	 */
	@Override
	public FileAttachment manage(FileAttachment fileAttachment, AEInvocationContext invContext) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// validate invocation context
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// get connection and begin transaction
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();
			
			// do the real work here
			manage(fileAttachment, invContext, localConnection);
			
			localConnection.commit();
			return fileAttachment;	
		} catch (Exception e) {
			e.printStackTrace();
			if(localConnection != null) {
				localConnection.rollback();
			}
			throw new AEException(e);
		} finally { 
			AEConnection.close(localConnection);
		}
	}
	
	/**
 	 * private where no need for different  
	 */
	private void prepareInsert(AEDomainObject domObj, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		/**
		 * validate insert
		 */
		
		/**
		 * prepare
		 */
		Date dateNow = new Date();
		domObj.setCreator(invContext.getAuthPrincipal().getName());
		domObj.setTimeCreated(dateNow);
		domObj.setModifier(invContext.getAuthPrincipal().getName());
		domObj.setTimeModified(dateNow);
//		domObj.setCompany(invContext.getAuthSubject().getCompany());
	}
	
	/**
 	 * private where no need for different  
	 */
	private void prepareUpdate(AEDomainObject aeObject, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		/**
		 * validate update
		 */
		
		/**
		 * prepare
		 */
		Date dateNow = new Date();
		aeObject.setModifier(invContext.getAuthPrincipal().getName());
		aeObject.setTimeModified(dateNow);
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.services.file.FileAttachmentService#load(eu.agileeng.domain.AEDescriptor, eu.agileeng.services.AEInvocationContext)
	 */
	@Override
	public FileAttachment load(AEDescriptor fileAttachmentDescr, AEInvocationContext invContext) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// TODO authorize
			
			// get connection
			localConnection = daoFactory.getConnection();
			FileAttachmentDAO fileAttachDAO = daoFactory.getFileAttachmentDAO(localConnection);
			FileAttachment fileAttachment = fileAttachDAO.load(fileAttachmentDescr);
			
			AECommentDAO commentsDAO = daoFactory.getAECommentDAO(localConnection);
			fileAttachment.setComments(commentsDAO.loadToObj(fileAttachmentDescr));

			return fileAttachment;
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}	
	}
	
	private void manage(AECommentsList comments, AEDescriptive toObject, AEInvocationContext invContext, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// TODO authorize
			
			// get connection
			localConnection = daoFactory.getConnection(aeConnection);
			AECommentDAO commentsDAO = daoFactory.getAECommentDAO(localConnection);
			for (AEComment aeComment : comments) {
				AEPersistent.State state = aeComment.getPersistentState();
				switch(state) {
					case NEW:
						prepareInsert(aeComment, invContext, localConnection);
						aeComment.setToObject(toObject);
						commentsDAO.insert(aeComment);
						break;
					case UPDATED:
						prepareUpdate(aeComment, invContext, localConnection);
						commentsDAO.update(aeComment);
						break;
					case DELETED:
						break;
					case VIEW:
						// manage in depth
						break;
					default:
						// must be never hapen
						assert(false);
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}	
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.services.file.FileAttachmentService#load(eu.agileeng.domain.file.FileAttachmentFilter, eu.agileeng.services.AEInvocationContext)
	 */
	@Override
	public FileAttachmentsList load(FileAttachmentFilter filter, AEInvocationContext invContext) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// TODO authorize
			
			// get connection
			localConnection = daoFactory.getConnection();
			FileAttachmentDAO fileAttachDAO = daoFactory.getFileAttachmentDAO(localConnection);
			filter.setAuthSubject(invContext.getAuthPrincipal());
			FileAttachmentsList fileAttachments = fileAttachDAO.load(filter);
			
			return fileAttachments;
		} catch (Throwable t) {
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}	
	}

	@Override
	public AEResponse manage(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * validate execution
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			DAOFactory daoFactory = DAOFactory.getInstance();
			JSONObject jsonFileAttachment = aeRequest.getArguments().getJSONObject("fileAttachment");
			FileAttachment fileAttachment = new FileAttachment();
			fileAttachment.create(jsonFileAttachment);
			
			// get connection and begin transaction
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			localConnection.beginTransaction();

			// do the real work here
			FileAttachmentDAO fileAttachmentDAO = daoFactory.getFileAttachmentDAO(localConnection);
			AEPersistent.State state = fileAttachment.getPersistentState();
			switch(state) {
				case NEW:
					authorize(new AuthPermission("System/FileAttachment", AuthPermission.CREATE), invContext);
					prepareInsert(fileAttachment, invContext, localConnection);
					fileAttachmentDAO.insert(fileAttachment);
					break;
				case UPDATED:
					authorize(new AuthPermission("System/FileAttachment", AuthPermission.UPDATE), invContext);
					prepareUpdate(fileAttachment, invContext, localConnection);
					fileAttachmentDAO.update(fileAttachment);
					break;
				case DELETED:
					authorize(new AuthPermission("System/FileAttachment", AuthPermission.DELETE), invContext);
					fileAttachmentDAO.delete(fileAttachment.getDescriptor(), fileAttachment.getCompany().getDescriptor());
					break;
				case VIEW:
					// manage in depth
					break;
				default:
					// must be never hapen
					assert(false);
			}


			// commit
			localConnection.commit();
			
			JSONObject payload = new JSONObject();
			payload.put("fileAttachment", fileAttachment.toJSONObject());
			return new AEResponse(payload);
		} catch (AEException e) {
			localConnection.rollback();
			e.printStackTrace();
			throw e;
		} catch (Throwable t) {
			localConnection.rollback();
			t.printStackTrace();
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public FileAttachment manage(FileAttachment fileAttachment,
			AEInvocationContext invContext, AEConnection aeConnection) throws AEException {

		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// validate invocation context
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// TODO file attachment validation
			
			// TODO authorize
			
			// get connection and begin transaction
			localConnection = daoFactory.getConnection(aeConnection);
			localConnection.beginTransaction();
			
			// do the real work here
			FileAttachmentDAO fileAttachmentDAO = daoFactory.getFileAttachmentDAO(localConnection);
			AEPersistent.State state = fileAttachment.getPersistentState();
			switch(state) {
				case NEW:
					prepareInsert(fileAttachment, invContext, localConnection);
					fileAttachmentDAO.insert(fileAttachment);
					break;
				case UPDATED:
					prepareUpdate(fileAttachment, invContext, localConnection);
					fileAttachmentDAO.update(fileAttachment);
					break;
				case DELETED:
					break;
				case VIEW:
					// manage in depth
					break;
				default:
					// must be never hapen
					assert(false);
			}
			if(fileAttachment.getComments() != null) {
				manage(fileAttachment.getComments(), fileAttachment, invContext, localConnection);
			}
			
			localConnection.commit();
			return fileAttachment;	
		} catch (Exception e) {
			e.printStackTrace();
			if(localConnection != null) {
				localConnection.rollback();
			}
			throw new AEException(e);
		} finally { 
			AEConnection.close(localConnection);
		}
	}
}
