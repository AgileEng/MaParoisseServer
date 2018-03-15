/**
 * 
 */
package eu.agileeng.services.jcr.ejb;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jcr.Binary;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import javax.transaction.UserTransaction;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONObject;
import org.jboss.logging.Logger;

import eu.agileeng.accbureau.AEApp;
import eu.agileeng.accbureau.AEAppModule;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.file.FileAttachment;
import eu.agileeng.domain.file.FileAttachmentFilter;
import eu.agileeng.domain.file.FileAttachmentsList;
import eu.agileeng.domain.jcr.DeleteNodeValidator;
import eu.agileeng.domain.jcr.JcrFile;
import eu.agileeng.domain.jcr.JcrNode;
import eu.agileeng.domain.jcr.JcrNodeFactory;
import eu.agileeng.domain.jcr.JcrNodeList;
import eu.agileeng.domain.jcr.NodeFolderValidation;
import eu.agileeng.domain.jcr.NodeProtectedValidator;
import eu.agileeng.domain.jcr.NodeSameNameValidator;
import eu.agileeng.domain.jcr.StructureChangeValidator;
import eu.agileeng.domain.social.SocialInfo;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.app.AppDAO;
import eu.agileeng.persistent.dao.document.AEDocumentDAO;
import eu.agileeng.persistent.dao.file.FileAttachmentDAO;
import eu.agileeng.persistent.dao.oracle.OrganizationDAO;
import eu.agileeng.persistent.dao.social.SocialDAO;
import eu.agileeng.persistent.jcr.JCRFactory;
import eu.agileeng.persistent.jcr.JcrSession;
import eu.agileeng.security.AuthPermission;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.security.AuthRole;
import eu.agileeng.services.AEBean;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AEInvocationContextValidator;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEFileUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEMimeTypes;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.mail.Emailer;

/**
 * @author vvatov
 *
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class JcrBean extends AEBean implements JcrLocal {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7354487336408295534L;

	private static final Path TMP_FILE_REPOSITORY = AEApp.getInstance().getTmpRepository();

	private static final Logger logger = Logger.getLogger(JcrBean.class);

	@Resource
	UserTransaction utx;

	@Override
	public AEResponse addNode(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		JcrSession jcrSession = null;
		File jcrContentFile = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			Long ownerId = arguments.optLong(JcrNode.JSONKey.ownerId);
			String moduleId = arguments.optString(JcrNode.JSONKey.moduleId);
			String path = arguments.optString(JcrNode.JSONKey.path);
			String primaryNodeType = arguments.optString(JcrNode.JSONKey.primaryNodeType);
			String newNodeName = arguments.optString(JcrNode.JSONKey.newNodeName);
			String mimeType = arguments.optString(JcrNode.JSONKey.mimeType);
			String note = arguments.optString(JcrNode.JSONKey.note);
			String jcrContentFileName = arguments.optString(JcrNode.JSONKey.jcrContentFileName);
			jcrContentFile = new File(jcrContentFileName);
			boolean isFile = JcrNode.JcrNodeType.file.equalsIgnoreCase(primaryNodeType);
			boolean isFolder = JcrNode.JcrNodeType.folder.equalsIgnoreCase(primaryNodeType);

			// validate arguments
			if (!isFile && !isFolder) {
				throw new AEException(
						AEError.System.JCR_NODE_TYPE_MISSING.getSystemID(),
						"Node type is is not file or folder.");
			}

			if(isFile) {
				if(jcrContentFile == null || !jcrContentFile.exists()) {
					throw new AEException(
							AEError.System.JCR_CONTENT_FILE_MISSING.getSystemID(),
							"The uploaded file '" + jcrContentFile.getAbsolutePath() + "' is missing");
				}
				newNodeName = jcrContentFile.getName();
			} else if(isFolder) {
				AuthPrincipal ap = invContext.getAuthPrincipal();
				if(AuthRole.isOperative(ap)) {
					throw new AEException("Vous n'avez pas les droits suffisants pour effectuer cette opération.");
				}
			}

			/**
			 * Factory, Session and start transaction
			 */
			JCRFactory jcrFactory = JCRFactory.getDefaultInstance();
			jcrSession = jcrFactory.getSession();
			utx.begin();

			/**
			 * Business logic
			 */
			// grant the parent path
			String parentPath = jcrSession.grantPath(ownerId, moduleId, path, invContext);

			// search the parent by path
			Item item = jcrSession.getSession().getItem(parentPath);
			if (null == item && !(item instanceof Node)) {
				throw new AEException(
						AEError.System.JCR_PATH_IS_NOT_NODE.getSystemID(),
						"The path '" + path + "' is not instance of Node");
			}
			Node pathNode = (Node) item;

			// check for duplication
			NodeSameNameValidator sameNameValidator = new NodeSameNameValidator(pathNode);
			sameNameValidator.validate(newNodeName);

			// create the node
			Date timeNow = new Date();
			Node newNode = pathNode.addNode(newNodeName, primaryNodeType);
			newNode.addMixin(JcrNode.JcrNodeType.mixUnstructured);
			newNode.addMixin(JcrNode.JcrNodeType.mixTitle);

			// JcrNode.JcrNodeType.mixTitle
			newNode.setProperty(JcrNode.JcrProperty.title, newNodeName);
			if(!AEStringUtil.isEmpty(note)) {
				newNode.setProperty(JcrNode.JcrProperty.description, note);
			}

			// JcrNode.JcrProperty.ae_created
			newNode.setProperty(JcrNode.JcrProperty.ae_createdBy, invContext.getAuthPrincipal().getFullName());
			newNode.setProperty(JcrNode.JcrProperty.ae_created, AEDateUtil.formatDateTimeToSystem(timeNow));
			newNode.setProperty(
					JcrNode.JcrProperty.ae_createdByRole, 
					invContext.getAuthPrincipal().getMaxRole());

			// JcrNode.JcrProperty.ae_lastModified
			newNode.setProperty(JcrNode.JcrProperty.ae_lastModifiedBy, invContext.getAuthPrincipal().getFullName());
			newNode.setProperty(JcrNode.JcrProperty.ae_lastModified, AEDateUtil.formatDateTimeToSystem(timeNow));

			// check for file
			if (isFile) {
				// ae properties
				newNode.setProperty(JcrNode.JcrProperty.ae_size, AEMath.getSizeInKB(jcrContentFile.length()));
				newNode.setProperty(
						JcrNode.JcrProperty.ae_fileExtension, 
						FilenameUtils.getExtension(jcrContentFileName));
				newNode.setProperty(
						JcrNode.JcrProperty.ae_iconClass, 
						AEFileUtil.getIconClass(jcrContentFileName));

				// if the node is nt:file primary type create the mandatory child node - jcr:content
				Node resNode = newNode.addNode(JcrNode.JcrProperty.content, JcrNode.JcrNodeType.resource);
				ValueFactory valueFactory = jcrSession.getSession().getValueFactory();

				// jcr properties
				resNode.setProperty(JcrNode.JcrProperty.mimeType, mimeType);
				resNode.setProperty(JcrNode.JcrProperty.encoding, "");
				resNode.setProperty(
						JcrNode.JcrProperty.data, 
						valueFactory.createBinary(new FileInputStream(jcrContentFile)));
			}

			// save the work
			jcrSession.getSession().save();

			/**
			 * Create and return response
			 */
			JcrNodeFactory jcrNodeFactory = JcrNodeFactory.getInstance(primaryNodeType);
			JcrNode jcrNode = jcrNodeFactory.createNode(newNode);

			/**
			 * Commit, will close all sessions 
			 */
			utx.commit();

			/**
			 * Process after close.
			 * Must be after transaction close.
			 * If after close fails, this should not affect the whole close task.
			 */
			if(isFile) {
				AEConnection dbConn = null;
				try {
					// send e-mail
					DAOFactory daoFactory = DAOFactory.getInstance();
					dbConn = daoFactory.getConnection();
					OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(dbConn);
					AEDescriptor ownerDescr = orgDAO.loadDescriptor(ownerId);
					if(ownerDescr != null) {
						Emailer emailer = new Emailer();
						emailer.onJcrFileUploaded(ownerDescr, invContext, jcrNode);
					}
				} catch (Throwable t) {
					t.printStackTrace();
				} finally {
					AEConnection.close(dbConn);
				}
			}

			/**
			 * return 
			 */
			JSONObject payload = new JSONObject();
			payload.put(JcrNode.JSONKey.node, jcrNode.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			try {
				utx.rollback();
			} catch (Exception ex) {}
			throw new AEException(e);
		} finally {
			// close ALL sessions
			// this may throw exception like below:
			// 21:48:49,014 INFO  [TxConnectionManager] throwable from unregister connection
			// java.lang.IllegalStateException: Trying to return an unknown connection2! org.apache.jackrabbit.jca.JCASessionHandle@ddaf7c
			// but this is INFO message.
			//
			// This is better than end with session leak!!!
			JcrSession.close(jcrSession);

			// Delete temp file
			AEFileUtil.deleteParentQuietly(jcrContentFile);
		}
	}

	@Override
	public AEResponse getNode(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		JcrSession jcrSession = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			Long ownerId = arguments.optLong(JcrNode.JSONKey.ownerId);
			String moduleId = arguments.optString(JcrNode.JSONKey.moduleId);
			String path = arguments.optString(JcrNode.JSONKey.path);
			String password = arguments.optString(JcrNode.JSONKey.password);

			/**
			 * Password protected
			 */
			if(!AEStringUtil.isEmpty(path) && path.endsWith("Salaires")) {
				if(!ap.hasAdministratorRights() && !ap.hasPowerUserRights() && !ap.isMemberOf(AuthRole.System.social)) {
					// validate password
					AEConnection localConnection = null;
					try {
						DAOFactory daoFactory = DAOFactory.getInstance();
						localConnection = daoFactory.getConnection();
						SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);
						SocialInfo si = socialDAO.loadSocialInfo(Organization.lazyDescriptor(ownerId));
						if(si != null) {
							if(!AEStringUtil.isEmpty(password)) {
								if(!password.equalsIgnoreCase(si.getPassword())) {
									throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
								}
							} else {
								throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
							}
						} else {
							throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
						}
					} catch (Exception e) {
						throw new AEException(e);
					} finally {
						AEConnection.close(localConnection);
					}
				}
			}

			/**
			 * Factory, Session and start transaction
			 */
			JCRFactory jcrFactory = JCRFactory.getDefaultInstance();
			jcrSession = jcrFactory.getSession();

			/**
			 * Business logic
			 */

			// grant the parent node
			String parentPath = jcrSession.grantPath(ownerId, moduleId, path, invContext);
			Item item = jcrSession.getSession().getItem(parentPath);
			if (null == item || !(item instanceof Node)) {
				throw new AEException(parentPath + " is not a Node");
			}
			Node parentNode = (Node) item;
			JcrNodeFactory parentNodeFactory = JcrNodeFactory.getInstance(parentNode.getPrimaryNodeType().getName());
			JcrNode parentJcrNode = parentNodeFactory.createNode(parentNode);

			// iterate parentNode
			JcrNodeList jcrContentList = new JcrNodeList();
			JcrNodeList jcrFolderList = new JcrNodeList();
			NodeIterator nodeIterator = parentNode.getNodes();
			for (int i = 0; i < nodeIterator.getSize(); i++) {
				Node node = nodeIterator.nextNode();
				String nt = node.getPrimaryNodeType().getName();
				boolean isFile = JcrNode.JcrNodeType.file.equalsIgnoreCase(nt);
				boolean isFolder = JcrNode.JcrNodeType.folder.equalsIgnoreCase(nt);

				// we are collecting only files and folders
				if(!isFile && !isFolder) {
					continue;
				}

				// create JcrNode
				JcrNodeFactory nodeFactory = JcrNodeFactory.getInstance(node.getPrimaryNodeType().getName());
				JcrNode jcrNode = nodeFactory.createNode(node);

				// add to the lists
				jcrContentList.add(jcrNode);
				if(isFolder) {
					jcrFolderList.add(jcrNode);
				}
			}

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject();
			payload.put("node", parentJcrNode.toJSONObject().toString());
			payload.put("folders", jcrFolderList.toJSONArray());
			payload.put("content", jcrContentList.toJSONArray());
			return new AEResponse(payload);
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			JcrSession.close(jcrSession);
		}
	}

	@Override
	public AEResponse getNodeData(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		JcrSession jcrSession = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			String path = arguments.optString(JcrNode.JSONKey.path);

			/**
			 * Factory, Session and start transaction
			 */
			JCRFactory jcrFactory = JCRFactory.getDefaultInstance();
			jcrSession = jcrFactory.getSession();

			/**
			 * Business logic
			 */

			// search the File Node by path
			Item item = jcrSession.getSession().getItem(path);
			if (null == item || !(item instanceof Node)) {
				throw new AEException(path + " is not a Node");
			}

			Node fileNode = (Node) item;
			if(!JcrNode.JcrNodeType.file.equals(fileNode.getPrimaryNodeType().getName())) {
				throw new AEException(path + " is not a File");
			}

			// get the content subnode
			Node jcrContent = fileNode.getNode(JcrNode.JcrProperty.content);
			if(jcrContent == null || !JcrNode.JcrNodeType.resource.equals(jcrContent.getPrimaryNodeType().getName())) {
				throw new AEException(JcrNode.JcrProperty.content + " child cannot be found");
			}

			// extract the data to the tmp folder
			File tmpFileRepository = new File(
					TMP_FILE_REPOSITORY.toFile(), 
					(String) invContext.getProperty(AEInvocationContext.HttpSessionId));
			if(!tmpFileRepository.exists()) {
				tmpFileRepository.mkdir();
			}
			File tmpFile = new File(
					tmpFileRepository, 
					UUID.nameUUIDFromBytes(this.getClass().getSimpleName().getBytes()).toString());
			Binary bin = jcrContent.getProperty(JcrNode.JcrProperty.data).getBinary();
			FileUtils.copyInputStreamToFile(
					bin.getStream(), 
					tmpFile);

			// release all resources acquired by the bin 
			try {
				bin.dispose();
			} catch(Exception e) {}

			/**
			 * Create and return response
			 */
			StringBuilder downloadUrl = new StringBuilder();
			downloadUrl.append("../../../FileDownloadServlet?");
			downloadUrl.append("file=").append(tmpFile.getAbsolutePath());
			downloadUrl.append("&fileName=").append(fileNode.getName());
			downloadUrl.append("&deleteOnExit=").append(true);

			JSONObject payload = new JSONObject();
			payload.put("file", tmpFile.getAbsolutePath());
			payload.put("fileName", tmpFile.getName());
			payload.put("downloadUrl", downloadUrl);

			return new AEResponse(payload);
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			JcrSession.close(jcrSession);
		}
	}

	@Override
	public AEResponse deleteNode(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		JcrSession jcrSession = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			String path = arguments.optString(JcrNode.JSONKey.path);

			/**
			 * Factory, Session and start transaction
			 */
			JCRFactory jcrFactory = JCRFactory.getDefaultInstance();
			jcrSession = jcrFactory.getSession();
			utx.begin();

			// search the Item
			Item item = jcrSession.getSession().getItem(path);

			/**
			 * Delete validation
			 */
			DeleteNodeValidator deleteValidator = new DeleteNodeValidator(invContext);
			deleteValidator.validate(item);

			StructureChangeValidator structureValidator = new StructureChangeValidator(invContext);
			structureValidator.validate(item);

			NodeProtectedValidator nodeValidator = new NodeProtectedValidator(invContext);
			nodeValidator.validate(item);

			Node node = (Node) item;

			/**
			 * Business logic
			 */

			// Prepare response
			JcrNodeFactory jcrNodeFactory = JcrNodeFactory.getInstance(node.getPrimaryNodeType().getName());
			JcrNode jcrNode = jcrNodeFactory.createNode(node);

			// Remove node
			node.remove();

			// Save and commit
			jcrSession.save();	
			utx.commit();

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject();
			payload.put(JcrNode.JSONKey.node, jcrNode.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			try {
				utx.rollback();
			} catch (Exception ex) {}
			throw new AEException(e);
		} finally {
			JcrSession.close(jcrSession);
		}
	}

	@Override
	public AEResponse search(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		JcrSession jcrSession = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			String path = arguments.optString(JcrNode.JSONKey.path);
			String fullTextSearchExpression = arguments.optString(JcrNode.JSONKey.fullTextSearchExpression);
			JSONObject params = arguments.optJSONObject("params");

			// validate arguments
			if (AEStringUtil.isEmpty(path)) {
				throw new AEException(
						AEError.System.JCR_NODE_PATH_MISSING.getSystemID(),
						"The path cannot be empty.");
			}

			if (AEStringUtil.isEmpty(fullTextSearchExpression)) {
				throw new AEException(
						AEError.System.JCR_NODE_FULL_TEXT_SEARCH_EXPRESSION_MISSING.getSystemID(),
						"The Full text search expression cannot be empty.");
			}

			/**
			 * Factory, Session and start transaction
			 */
			JCRFactory jcrFactory = JCRFactory.getDefaultInstance();
			jcrSession = jcrFactory.getSession();

			/**
			 * Business logic
			 */
			JcrNodeList searchResult = new JcrNodeList();
			QueryManager qm = jcrSession.getSession().getWorkspace().getQueryManager();
			StringBuilder sb = new StringBuilder();
			sb
			.append("select * from [nt:base] AS node where ")
			.append(" ISDESCENDANTNODE(node, '").append(path).append("')")
			.append(" and contains(node.*, ").append(stringToJcrSearchExp(fullTextSearchExpression)).append(")");
			Query query = qm.createQuery(sb.toString(), Query.JCR_SQL2);
			if(params != null) {
				query.setLimit(params.getLong("limit"));
				query.setOffset(params.getLong("start"));
			}
			QueryResult queryResult = query.execute();
			NodeIterator resultIterator = queryResult.getNodes();
			while (resultIterator.hasNext()) {
				Node node = resultIterator.nextNode();
				if(JcrNode.JcrNodeType.resource.equalsIgnoreCase(node.getPrimaryNodeType().getName())) {
					JcrNodeFactory jcrNodeFactory = 
							JcrNodeFactory.getInstance(node.getParent().getPrimaryNodeType().getName());
					searchResult.add(jcrNodeFactory.createNode(node.getParent()));
				} else {
					JcrNodeFactory jcrNodeFactory = 
							JcrNodeFactory.getInstance(node.getPrimaryNodeType().getName());
					searchResult.add(jcrNodeFactory.createNode(node));
				}
			}

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject();
			payload.put(JcrNode.JSONKey.searchResult, searchResult.toJSONArray());
			if(params != null) {
				payload.put("params", params);
			}
			payload.put("total", Long.MAX_VALUE);
			return new AEResponse(payload);
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			JcrSession.close(jcrSession);
		}
	}

	@Override
	public AEResponse moveNode(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		JcrSession jcrSession = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			String path = arguments.optString(JcrNode.JSONKey.path);
			String destPath = arguments.optString(JcrNode.JSONKey.destPath);

			/**
			 * Factory, Session and start transaction
			 */
			JCRFactory jcrFactory = JCRFactory.getDefaultInstance();
			jcrSession = jcrFactory.getSession();
			utx.begin();

			/**
			 * Search and validate the path
			 */
			Item item = jcrSession.getSession().getItem(path);
			NodeProtectedValidator nodeValidator = new NodeProtectedValidator(invContext);
			nodeValidator.validate(item);

			StructureChangeValidator structureValidator = new StructureChangeValidator(invContext);
			structureValidator.validate(item);

			Node node = (Node) item;

			/**
			 * Search and validate the destPath
			 */
			Item destItem = jcrSession.getSession().getItem(destPath);
			NodeFolderValidation.getInstance().validate(destItem);

			/**
			 * Duplicate name validation
			 */
			NodeSameNameValidator sameNameValidator = new NodeSameNameValidator((Node)destItem);
			sameNameValidator.validate(node.getName());

			/**
			 * Business logic
			 */
			String _destPath = destItem.getPath() + "/" + node.getName();;
			jcrSession.getSession().move(path, _destPath);
			jcrSession.save();

			// Prepare response
			Item _destItem = jcrSession.getSession().getItem(_destPath);
			if (null == _destItem || !(_destItem instanceof Node)) {
				throw new AEException(_destPath + " is not a Node");
			}
			Node _destNode = (Node) _destItem;
			JcrNodeFactory jcrNodeFactory = JcrNodeFactory.getInstance(_destNode.getPrimaryNodeType().getName());
			JcrNode jcrNode = jcrNodeFactory.createNode(_destNode);

			// Save and commit
			utx.commit();

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject();
			payload.put(JcrNode.JSONKey.node, jcrNode.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			try {
				utx.rollback();
			} catch (Exception ex) {}
			throw new AEException(e);
		} finally {
			JcrSession.close(jcrSession);
		}
	}

	@Override
	public AEResponse copyNode(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		JcrSession jcrSession = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			String path = arguments.optString(JcrNode.JSONKey.path);
			String destPath = arguments.optString(JcrNode.JSONKey.destPath);
			boolean dontValidate = arguments.optBoolean(JcrNode.JSONKey.dontValidate);

			/**
			 * Factory, Session and start transaction
			 */
			JCRFactory jcrFactory = JCRFactory.getDefaultInstance();
			jcrSession = jcrFactory.getSession();
			utx.begin();

			/**
			 * Search and validate the path
			 */
			Item item = jcrSession.getSession().getItem(path);
			if(!dontValidate) {
				NodeProtectedValidator nodeValidator = new NodeProtectedValidator(invContext);
				nodeValidator.validate(item);

				StructureChangeValidator structureValidator = new StructureChangeValidator(invContext);
				structureValidator.validate(item);
			}
			Node node = (Node) item;

			/**
			 * Search and validate the destPath
			 */
			Item destItem = jcrSession.getSession().getItem(destPath);
			NodeFolderValidation.getInstance().validate(destItem);

			/**
			 * Duplicate name validation
			 */
			NodeSameNameValidator sameNameValidator = new NodeSameNameValidator((Node)destItem);
			sameNameValidator.validate(node.getName());

			/**
			 * Business logic
			 */
			String _destPath = destItem.getPath() + "/" + node.getName();
			jcrSession.getSession().getWorkspace().copy(path, _destPath);
			jcrSession.save();

			// Prepare response
			Item _destItem = jcrSession.getSession().getItem(_destPath);
			if (null == _destItem || !(_destItem instanceof Node)) {
				throw new AEException(_destPath + " is not a Node");
			}
			Node _destNode = (Node) _destItem;
			JcrNodeFactory jcrNodeFactory = JcrNodeFactory.getInstance(_destNode.getPrimaryNodeType().getName());
			JcrNode jcrNode = jcrNodeFactory.createNode(_destNode);

			// Save and commit
			utx.commit();

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject();
			payload.put(JcrNode.JSONKey.node, jcrNode.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			try {
				utx.rollback();
			} catch (Exception ex) {}
			throw new AEException(e);
		} finally {
			JcrSession.close(jcrSession);
		}
	}

	@Override
	public AEResponse renameNode(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		JcrSession jcrSession = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			String path = arguments.optString(JcrNode.JSONKey.path);
			String newName = arguments.optString(JcrNode.JSONKey.newName);

			/**
			 * Factory, Session and start transaction
			 */
			JCRFactory jcrFactory = JCRFactory.getDefaultInstance();
			jcrSession = jcrFactory.getSession();
			utx.begin();

			/**
			 * Search and validate the path
			 */
			Item item = jcrSession.getSession().getItem(path);
			NodeProtectedValidator nodeValidator = new NodeProtectedValidator(invContext);
			nodeValidator.validate(item);

			StructureChangeValidator structureValidator = new StructureChangeValidator(invContext);
			structureValidator.validate(item);

			Node node = (Node) item;

			/**
			 * Validate the destPath
			 */
			if(AEStringUtil.isEmpty(newName)) {
				throw new AEException("The name canot be empty.");
			}

			/**
			 * Duplicate name validation
			 */
			NodeSameNameValidator sameNameValidator = new NodeSameNameValidator(node.getParent());
			sameNameValidator.validate(newName);

			/**
			 * Business logic
			 */
			//			String _destPath = node.getParent().getPath() + "/" + newName;
			//			jcrSession.getSession().move(node.getPath(), _destPath);
			//			jcrSession.save();
			//			
			//			// Prepare response
			//			Item _destItem = jcrSession.getSession().getItem(_destPath);
			//			if (null == _destItem || !(_destItem instanceof Node)) {
			//				throw new AEException(_destPath + " is not a Node");
			//			}
			//			Node _destNode = (Node) _destItem;
			//			JcrNodeFactory jcrNodeFactory = JcrNodeFactory.getInstance(_destNode.getPrimaryNodeType().getName());
			//			JcrNode jcrNode = jcrNodeFactory.createNode(_destNode);

			node.setProperty(JcrNode.JcrProperty.ae_displayName, newName);

			// Save and commit
			jcrSession.save();
			utx.commit();

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject();
			//			payload.put(JcrNode.JSONKey.node, jcrNode.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			try {
				utx.rollback();
			} catch (Exception ex) {}
			throw new AEException(e);
		} finally {
			JcrSession.close(jcrSession);
		}
	}

	/**
	 * Convert a string to a JCR search expression literal, suitable for use in jcr:contains()
	 * (inside XPath queries). The characters - and " have special meaning, and may be escaped with
	 * a backslash to obtain their literal value. See JSR-170 spec v1.0, Sec. 6.6.5.2.
	 *
	 * @param str Any string.
	 * @return A valid XPath 2.0 string literal suitable for use in jcr:contains(), including
	 *         enclosing quotes.
	 */
	public static String stringToJcrSearchExp(String str) {
		//      String exp = "'%" + escapeQueryChars(str.toLowerCase()).replaceAll("'", "''") + "%'";
		String exp = "'" + str + "'";
		return exp;
	}

	public static String escapeQueryChars(String str) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':'
					|| c == '^' || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}' ||
					c == '~' || c == '*' || c == '?') {
				sb.append('\\');
			}
			sb.append(c);
		}
		return sb.toString();
	}

	@Override
	public AEResponse checkout(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		JcrSession jcrSession = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			String path = arguments.optString(JcrNode.JSONKey.path);

			/**
			 * Factory, Session and start transaction
			 */
			JCRFactory jcrFactory = JCRFactory.getDefaultInstance();
			jcrSession = jcrFactory.getSession();
			utx.begin();

			/**
			 * Search and validate the path
			 */
			Item item = jcrSession.getSession().getItem(path);
			NodeProtectedValidator nodeValidator = new NodeProtectedValidator(invContext);
			nodeValidator.validate(item);
			Node node = (Node) item;

			/**
			 * Business logic
			 */

			// prepare
			try {
				node.addMixin(NodeType.MIX_SIMPLE_VERSIONABLE);
				jcrSession.save();
			} catch (Exception e) {}
			VersionManager versionManager = jcrSession.getSession().getWorkspace().getVersionManager();

			// Check-out.
			versionManager.checkout(node.getPath());
			jcrSession.save();

			// Prepare response
			JcrNodeFactory jcrNodeFactory = JcrNodeFactory.getInstance(node.getPrimaryNodeType().getName());
			JcrNode jcrNode = jcrNodeFactory.createNode(node);

			// Save and commit
			utx.commit();

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject();
			payload.put(JcrNode.JSONKey.node, jcrNode.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			try {
				utx.rollback();
			} catch (Exception ex) {}
			throw new AEException(e);
		} finally {
			JcrSession.close(jcrSession);
		}
	}

	@Override
	public AEResponse cancelCheckout(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		JcrSession jcrSession = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			String path = arguments.optString(JcrNode.JSONKey.path);

			/**
			 * Factory, Session and start transaction
			 */
			JCRFactory jcrFactory = JCRFactory.getDefaultInstance();
			jcrSession = jcrFactory.getSession();
			//			utx.begin();

			/**
			 * Search and validate the path
			 */
			Item item = jcrSession.getSession().getItem(path);
			NodeProtectedValidator nodeValidator = new NodeProtectedValidator(invContext);
			nodeValidator.validate(item);
			Node node = (Node) item;

			// Checked out validation
			if(!JcrNode.isVersionControlled(node) || !node.isCheckedOut()) {
				throw new AEException("This operation had no effect. The entry is not editable.");
			}

			/**
			 * Business logic
			 */

			// prepare
			try {
				node.addMixin(NodeType.MIX_SIMPLE_VERSIONABLE);
				jcrSession.save();
			} catch (Exception e) {}
			VersionManager versionManager = jcrSession.getSession().getWorkspace().getVersionManager();

			// cancel check-out.
			javax.jcr.version.Version rootVersion = versionManager.getVersionHistory(node.getPath()).getRootVersion();
			javax.jcr.version.Version baseVersion = versionManager.getBaseVersion(node.getPath());
			if(rootVersion.getPath().equalsIgnoreCase(baseVersion.getPath())) {
				versionManager.checkpoint(node.getPath());
				jcrSession.save();
			}
			versionManager.restore(versionManager.getBaseVersion(node.getPath()), false);
			jcrSession.save();

			// Prepare response
			JcrNodeFactory jcrNodeFactory = JcrNodeFactory.getInstance(node.getPrimaryNodeType().getName());
			JcrNode jcrNode = jcrNodeFactory.createNode(node);

			// Save and commit
			//			utx.commit();

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject();
			payload.put(JcrNode.JSONKey.node, jcrNode.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			//			try {
			//				utx.rollback();
			//			} catch (Exception ex) {}
			throw new AEException(e);
		} finally {
			JcrSession.close(jcrSession);
		}
	}

	@Override
	public AEResponse checkin(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		JcrSession jcrSession = null;
		File jcrContentFile = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			String path = arguments.optString(JcrNode.JSONKey.path);
			String note = arguments.optString(JcrNode.JSONKey.note);
			String jcrContentFileName = arguments.optString(JcrNode.JSONKey.jcrContentFileName);
			jcrContentFile = new File(jcrContentFileName);

			/**
			 * Factory, Session and start transaction
			 */
			JCRFactory jcrFactory = JCRFactory.getDefaultInstance();
			jcrSession = jcrFactory.getSession();
			//			utx.begin();

			/**
			 * Business logic
			 */

			// Search and validate the path
			Item item = jcrSession.getSession().getItem(path);
			NodeProtectedValidator nodeValidator = new NodeProtectedValidator(invContext);
			nodeValidator.validate(item);
			Node node = (Node) item;

			// Checked out validation
			if(!JcrNode.isVersionControlled(node) || !node.isCheckedOut()) {
				throw new AEException("Ce fichier ne peut pas être modifié, enregistrez le à part, ou demandez à ce qu’il soit rendu modifiable.");
			}

			VersionManager versionManager = jcrSession.getSession().getWorkspace().getVersionManager();

			// checkpoint
			javax.jcr.version.Version rootVersion = versionManager.getVersionHistory(node.getPath()).getRootVersion();
			javax.jcr.version.Version baseVersion = versionManager.getBaseVersion(node.getPath());
			if(rootVersion.getPath().equalsIgnoreCase(baseVersion.getPath())) {
				versionManager.checkpoint(node.getPath());
				jcrSession.save();
			}

			// update
			Date timeNow = new Date();
			node.setProperty(JcrNode.JcrProperty.ae_lastModifiedBy, invContext.getAuthPrincipal().getFullName());
			node.setProperty(JcrNode.JcrProperty.ae_lastModified, AEDateUtil.formatDateTimeToSystem(timeNow));
			node.setProperty(JcrNode.JcrProperty.ae_size, AEMath.getSizeInKB(jcrContentFile.length()));
			if(!AEStringUtil.isEmpty(note)) {
				node.setProperty(JcrNode.JcrProperty.description, note);
			} else {
				node.setProperty(JcrNode.JcrProperty.description, AEStringUtil.EMPTY_STRING);
			}
			Node resNode = node.getNode(JcrNode.JcrProperty.content);
			ValueFactory valueFactory = jcrSession.getSession().getValueFactory();
			resNode.setProperty(
					JcrNode.JcrProperty.data, 
					valueFactory.createBinary(new FileInputStream(jcrContentFile)));

			// save the work
			jcrSession.getSession().save();

			// checkin
			versionManager.checkin(node.getPath());
			jcrSession.save();

			/**
			 * Create and return response
			 */
			JcrNodeFactory jcrNodeFactory = JcrNodeFactory.getInstance(node.getPrimaryNodeType().getName());
			JcrNode jcrNode = jcrNodeFactory.createNode(node);

			/**
			 * Commit, will close all sessions 
			 */
			//			utx.commit();

			/**
			 * return 
			 */
			JSONObject payload = new JSONObject();
			payload.put(JcrNode.JSONKey.node, jcrNode.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			//			try {
			//				utx.rollback();
			//			} catch (Exception ex) {}
			throw new AEException(e);
		} finally {
			// close ALL sessions
			// this may throw exception like below:
			// 21:48:49,014 INFO  [TxConnectionManager] throwable from unregister connection
			// java.lang.IllegalStateException: Trying to return an unknown connection2! org.apache.jackrabbit.jca.JCASessionHandle@ddaf7c
			// but this is INFO message.
			//
			// This is better than end with session leak!!!
			JcrSession.close(jcrSession);

			// Delete temp file
			AEFileUtil.deleteParentQuietly(jcrContentFile);
		}
	}

	@Override
	public AEResponse versionHistory(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		JcrSession jcrSession = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			String path = arguments.optString(JcrNode.JSONKey.path);

			/**
			 * Factory, Session and start transaction
			 */
			JCRFactory jcrFactory = JCRFactory.getDefaultInstance();
			jcrSession = jcrFactory.getSession();

			/**
			 * Search and validate the path
			 */
			Item item = jcrSession.getSession().getItem(path);
			NodeProtectedValidator nodeValidator = new NodeProtectedValidator(invContext);
			nodeValidator.validate(item);
			Node node = (Node) item;

			/**
			 * Business logic
			 */
			JcrNodeList versionHistory = new JcrNodeList();
			if(JcrNode.isVersionControlled(node)) {
				VersionManager versionManager = jcrSession.getSession().getWorkspace().getVersionManager();
				VersionHistory vh = versionManager.getVersionHistory(node.getPath());
				Version rootVersion = vh.getRootVersion();
				VersionIterator vi = vh.getAllVersions();
				while(vi.hasNext()) {
					javax.jcr.version.Version v = vi.nextVersion();
					if(!rootVersion.getPath().equals(v.getPath())) {
						JcrNodeFactory jcrNF = JcrNodeFactory.getInstance(v.getPrimaryNodeType().getName());
						versionHistory.add(jcrNF.createNode(v));
					}
				}
			}
			Collections.reverse(versionHistory);

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject();
			payload.put(JcrNode.JSONKey.versionHistory, versionHistory.toJSONArray());
			return new AEResponse(payload);
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			JcrSession.close(jcrSession);
		}
	}

	@Override
	public AEResponse restoreVersion(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		JcrSession jcrSession = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			String path = arguments.optString(JcrNode.JSONKey.path);
			String versionName = arguments.optString(JcrNode.JSONKey.versionName);
			String versionPath = arguments.optString(JcrNode.JSONKey.versionPath);

			/**
			 * Factory, Session and start transaction
			 */
			JCRFactory jcrFactory = JCRFactory.getDefaultInstance();
			jcrSession = jcrFactory.getSession();
			//			utx.begin();

			/**
			 * Search and validate the path
			 */
			Item item = jcrSession.getSession().getItem(path);
			NodeProtectedValidator nodeValidator = new NodeProtectedValidator(invContext);
			nodeValidator.validate(item);
			Node node = (Node) item;

			// Checked out validation
			if(!JcrNode.isVersionControlled(node)) {
				throw new AEException("This operation had no effect. The entry is not versionable.");
			}

			/**
			 * Business logic
			 */

			// get the version
			VersionManager versionManager = jcrSession.getSession().getWorkspace().getVersionManager();
			javax.jcr.version.Version version = versionManager.getVersionHistory(
					node.getPath()).getVersion(versionName);
			if(version == null) {
				throw new AEException("This operation had no effect. The version can not be found.");
			}
			if(!version.getPath().equals(versionPath)) {
				throw new AEException("This operation had no effect. The version idetity failed.");
			}

			// restore
			versionManager.restore(version, true);
			jcrSession.save();

			// Prepare response
			JcrNodeFactory jcrNodeFactory = JcrNodeFactory.getInstance(node.getPrimaryNodeType().getName());
			JcrNode jcrNode = jcrNodeFactory.createNode(node);

			// Save and commit
			//			utx.commit();

			/**
			 * Create and return response
			 */
			JSONObject payload = new JSONObject();
			payload.put(JcrNode.JSONKey.node, jcrNode.toJSONObject());
			return new AEResponse(payload);
		} catch (Exception e) {
			//			try {
			//				utx.rollback();
			//			} catch (Exception ex) {}
			throw new AEException(e);
		} finally {
			JcrSession.close(jcrSession);
		}
	}

	@Override
	public void copyStructure(long fromId, long toId, AEInvocationContext invContext) throws AEException {
		JcrSession jcrSession = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Factory, Session and start transaction
			 */
			JCRFactory jcrFactory = JCRFactory.getDefaultInstance();
			jcrSession = jcrFactory.getSession();
			utx.begin();

			String fromItemPath = "/" + Long.toString(fromId);
			if(jcrSession.getSession().itemExists(fromItemPath)) {
				Item fromItem = jcrSession.getSession().getItem(fromItemPath);
				if (fromItem instanceof Node) {
					Node fromNode = (Node) fromItem;
					String toIdRoot = jcrSession.grantPath(toId, AEStringUtil.EMPTY_STRING, AEStringUtil.EMPTY_STRING, invContext);
					for (NodeIterator iterator = fromNode.getNodes(); iterator.hasNext();) {
						Node node = (Node) iterator.next();	

						String _destPath = toIdRoot + "/" + node.getName();
						jcrSession.getSession().getWorkspace().copy(node.getPath(), _destPath);
						jcrSession.save();
					}
				}
			}

			// Save and commit
			utx.commit();
		} catch (Exception e) {
			try {
				utx.rollback();
			} catch (Exception ex) {}
			throw new AEException(e);
		} finally {
			JcrSession.close(jcrSession);
		}
	}

	@Override
	public JcrFile getJcrFileWithContent(String path, long ownerId, AEInvocationContext invContext) throws AEException {
		JcrSession jcrSession = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);

			/**
			 * Extract request arguments  
			 */

			/**
			 * Password protected
			 */
			//			if(!AEStringUtil.isEmpty(path) && path.endsWith("Salaires")) {
			//				if(!ap.hasAdministratorRights() && !ap.hasPowerUserRights() && !ap.isMemberOf(AuthRole.System.social)) {
			//					// validate password
			//					AEConnection localConnection = null;
			//					try {
			//						DAOFactory daoFactory = DAOFactory.getInstance();
			//						localConnection = daoFactory.getConnection();
			//						SocialDAO socialDAO = daoFactory.getSocialDAO(localConnection);
			//						SocialInfo si = socialDAO.loadSocialInfo(Organization.lazyDescriptor(ownerId));
			//						if(si != null) {
			//							if(!AEStringUtil.isEmpty(password)) {
			//								if(!password.equalsIgnoreCase(si.getPassword())) {
			//									throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			//								}
			//							} else {
			//								throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			//							}
			//						} else {
			//							throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			//						}
			//					} catch (Exception e) {
			//						throw new AEException(e);
			//					} finally {
			//						AEConnection.close(localConnection);
			//					}
			//				}
			//			}

			/**
			 * Factory, Session and start transaction
			 */
			JCRFactory jcrFactory = JCRFactory.getDefaultInstance();
			jcrSession = jcrFactory.getSession();

			/**
			 * Business logic
			 */

			/**
			 * create jcrNode
			 */
			Item item = jcrSession.getSession().getItem(path);
			if (null == item || !(item instanceof Node)) {
				throw new AEException(path + " is not a Node");
			}
			Node node = (Node) item;

			JcrNodeFactory nodeFactory = JcrNodeFactory.getInstance(node.getPrimaryNodeType().getName());
			JcrNode jcrNode = nodeFactory.createNode(node);
			if (null == jcrNode || !(jcrNode instanceof JcrFile)) {
				throw new AEException(path + " is not a File");
			}
			JcrFile jcrFile = (JcrFile) jcrNode;

			/**
			 * Get content
			 */
			JSONObject contentRequestArguments = new JSONObject();
			contentRequestArguments.put(JcrNode.JSONKey.path, path);
			AERequest conteRequest = new AERequest(contentRequestArguments);
			AEResponse contentResponse = getNodeData(conteRequest, invContext);
			JSONObject getNodeDataPayload = contentResponse.getPayload();
			String nodeDataAbsolutePath = getNodeDataPayload.getString("file");
			jcrFile.setContent(new File(nodeDataAbsolutePath));

			/**
			 * Return response
			 */
			return jcrFile;
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			JcrSession.close(jcrSession);
		}
	}

	@Override
	public Path getModuleRepositoryPath(AERequest aeRequest,  AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();

			// common attributes
			long ownerId = arguments.getLong(AEDomainObject.JSONKey.ownerId.name());
			long sOwnerId = arguments.getLong(AEDomainObject.JSONKey.sOwnerId.name());

			/**
			 * Authorize for concrete tenant
			 */
			// state validation
			if(ownerId != sOwnerId) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}

			// whether this principal is authorized for specified tenant
			ap.ownershipValidator(ownerId);
			
			/**
			 * Authorize 
			 */
			authorize(new AuthPermission("System/Bibliotheque/Repository", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * Get path
			 */
			String moduleCode = arguments.getString(AEAppModule.JSONKey.appModuleCode.name());
			AppDAO appDAO = daoFactory.getAppDAO(localConnection);
			Path path = appDAO.loadRepositoryPath(AEAppModule.lazyDescriptor(AEPersistentUtil.NEW_ID).withCode(moduleCode));

			/**
			 * Create and return response
			 */
			return path;
		} catch (Exception e) {
			logger.error(e);
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse getDownloadPath(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();

			// common attributes
			long ownerId = arguments.getLong(AEDomainObject.JSONKey.ownerId.name());
			long sOwnerId = arguments.getLong(AEDomainObject.JSONKey.sOwnerId.name());

			/**
			 * Authorize for concrete tenant
			 */
			// state validation
			if(ownerId != sOwnerId) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}

			// whether this principal is authorized for specified tenant
			ap.ownershipValidator(ownerId);
			
			/**
			 * Authorize 
			 */
			authorize(new AuthPermission("System/Bibliotheque/Repository", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * Go
			 */
			String moduleCode = arguments.getString(AEAppModule.JSONKey.appModuleCode.name());
			boolean embedded = arguments.optBoolean("embedded");
			JSONObject payload = null;
			Path tmpPath = null;
			Path fullPath = null;
			if("170b3".equalsIgnoreCase(moduleCode) || "195b3".equalsIgnoreCase(moduleCode) || "175b3".equalsIgnoreCase(moduleCode)) {
				// path
				long attId = arguments.getLong(AEDomainObject.JSONKey.id.name());
				FileAttachmentDAO attDao = daoFactory.getFileAttachmentDAO(localConnection);
				FileAttachment att = attDao.load(FileAttachment.lazyDescriptor(attId));
				if(att != null) {
					// path
					fullPath = Paths.get(att.getRemoteRoot(), att.getRemotePath());
					String fileName = att.getName();

					if(!Files.exists(fullPath)) {
						throw new AEException("Le fichier n’existe pas. ");
					}

					// extract the data to the tmp folder
					tmpPath = Paths.get(
							TMP_FILE_REPOSITORY.toString(), 
							AEFileUtil.createTempFileName("tmp_jcr_", FilenameUtils.getExtension(fileName), TMP_FILE_REPOSITORY.toFile()));
					Files.copy(fullPath, tmpPath, StandardCopyOption.REPLACE_EXISTING);

					// create response payload
					StringBuilder downloadUrl = new StringBuilder()
					.append("../FileDownloadServlet?")
					.append("file=").append(tmpPath.getFileName())
					.append("&fileName=").append(fileName)
					.append("&deleteOnExit=").append(true)
					.append("&embedded=").append(embedded);

					payload = new JSONObject()
					.put("file", tmpPath.getFileName())
					.put("fileName", fileName)
					.put("downloadUrl", downloadUrl);
				} else {
					throw new AEException("Le Att n’existe pas. ");
				}
			} else {
				// path
				Path modulePath = getModuleRepositoryPath(aeRequest, invContext);
				Path path = arguments.optBoolean(AEDomainObject.JSONKey.system.name()) ? modulePath : Paths.get(modulePath.toString(), Long.toString(ownerId));
				String fileName = arguments.getString("fileName");
				fullPath = Paths.get(path.toString(), fileName);

				if(!Files.exists(fullPath)) {
					throw new AEException("Le fichier n’existe pas. ");
				}

				// extract the data to the tmp folder
				tmpPath = Paths.get(
						TMP_FILE_REPOSITORY.toString(), 
						AEFileUtil.createTempFileName("tmp_jcr_", FilenameUtils.getExtension(fileName), TMP_FILE_REPOSITORY.toFile()));

				Files.copy(fullPath, tmpPath, StandardCopyOption.REPLACE_EXISTING);

				// create response payload
				StringBuilder downloadUrl = new StringBuilder()
				.append("../FileDownloadServlet?")
				.append("file=").append(tmpPath.getFileName())
				.append("&fileName=").append(fullPath.getFileName().toString())
				.append("&deleteOnExit=").append(true)
				.append("&embedded=").append(embedded);

				payload = new JSONObject()
				.put("file", tmpPath.getFileName())
				.put("fileName", fullPath.getFileName().toString())
				.put("downloadUrl", downloadUrl);
			}

			/**
			 * Create and return response
			 */
			return new AEResponse(payload);
		} catch (Exception e) {
			logger.error(e);
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse upload(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();
			
			long ownerId = arguments.getLong(AEDomainObject.JSONKey.ownerId.name());
			long sOwnerId = arguments.getLong(AEDomainObject.JSONKey.sOwnerId.name());
			String moduleCode = arguments.optString(AEAppModule.JSONKey.appModuleCode.name());
			
			/**
			 * Authorize
			 */
			// state validation
			if(ownerId != sOwnerId) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}

			// whether this principal is authorized for specified tenant
			ap.ownershipValidator(ownerId);
			
			// Access validation
			if("150b3".equalsIgnoreCase(moduleCode) || "160b3".equalsIgnoreCase(moduleCode) 
					|| "163b3".equalsIgnoreCase(moduleCode) || "166b3".equalsIgnoreCase(moduleCode)) {
				// Exclusive rights to upload are granted
			} else {
				authorize(new AuthPermission("System/Bibliotheque/Repository", AuthPermission.SAVE | AuthPermission.CREATE), invContext, Organization.lazyDescriptor(ownerId));
			}

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * Get path
			 */
			Path path = getModuleRepositoryPath(aeRequest, invContext);
			Path tmpRepositoryPath = AEApp.getInstance().getTmpRepository();
			String tmpFileName = arguments.getString("fileName");
			Path tmpPath = Paths.get(tmpFileName);

			// determine destination path: shared or private
			Path dPath = null;
			if(ap.isMemberOf(AuthRole.System.administrator)) {
				// the path is module repository path
				dPath = path;
			} else {
				// apply tenantId as subfolder
				dPath = Paths.get(path.toString(), Long.toString(ownerId));
				if(!Files.exists(dPath)) {
					Files.createDirectories(dPath);
				}
			}
			Path destinationPath = Paths.get(dPath.toString(), tmpPath.getFileName().toString());
			if(Files.exists(destinationPath)) {
				throw new AEException("The file already exist ...");
			}

			// copy the file to the folder
			Files.copy(tmpPath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
			if(tmpPath.startsWith(tmpRepositoryPath)) {
				AEFileUtil.deleteFileQuietly(tmpPath.toFile());
			}

			/**
			 * Create and return response
			 */
			JSONObject f = new JSONObject()
			.put(AEDomainObject.JSONKey.id.name(), AEPersistentUtil.getTmpID())
			.put(AEDomainObject.JSONKey.code.name(), AEMimeTypes.getMimeType(FilenameUtils.getExtension(destinationPath.getFileName().toString())))
			.put(AEDomainObject.JSONKey.name.name(), destinationPath.getFileName().toString())
			.put(AEDomainObject.JSONKey.description.name(), AEStringUtil.EMPTY_STRING)
			.put(AEDomainObject.JSONKey.system.name(), dPath.equals(path) ? true : false);

			return new AEResponse(new JSONObject().put("file", f));
		} catch (Exception e) {
			logger.error(e);
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse delete(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();

			long ownerId = arguments.getLong(AEDomainObject.JSONKey.ownerId.name());
			long sOwnerId = arguments.getLong(AEDomainObject.JSONKey.sOwnerId.name());
			String moduleCode = arguments.optString(AEAppModule.JSONKey.appModuleCode.name());
			boolean sharedFile = arguments.optBoolean(AEDomainObject.JSONKey.system.name(), true);

			/**
			 * Authorize 
			 */
			if("150b3".equalsIgnoreCase(moduleCode) || "160b3".equalsIgnoreCase(moduleCode)
					|| "163b3".equalsIgnoreCase(moduleCode) || "166b3".equalsIgnoreCase(moduleCode) 
					|| "170b3".equalsIgnoreCase(moduleCode)) {
				// Exclusive rights to delete are granted
			} else {
				authorize(new AuthPermission("System/Bibliotheque/Repository", AuthPermission.DELETE), invContext, Organization.lazyDescriptor(ownerId));
			}
			
			/**
			 * Authorize for concrete tenant
			 */
			// state validation
			if(ownerId != sOwnerId) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}

			// whether this principal is authorized for specified tenant
			ap.ownershipValidator(ownerId);

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * Get path
			 */
			Path deletedPath = null;
			long attId = arguments.optLong(AEDomainObject.JSONKey.id.name(), 0L);
			if("170b3".equalsIgnoreCase(moduleCode)) {
				FileAttachmentDAO attDao = daoFactory.getFileAttachmentDAO(localConnection);
				FileAttachment att = attDao.load(FileAttachment.lazyDescriptor(attId));
				if(att != null) {
					// check for deleteion
					boolean delete = false;
					if(att.getAttachedTo() != null && DomainClass.FinancialTransaction.equals(att.getAttachedTo().getDescriptor().getClazz())) {
						// every FinancialTransaction Attachment can be deleted 
						delete = true;
					} else if(att.getAttachedTo() != null && DomainClass.AeDocument.equals(att.getAttachedTo().getDescriptor().getClazz())) {
						// Document Attachment can be deleted if the docType is ETAT_ANNUEL_DES_COMPTES_DRAFT 
						AEDocumentType docType = AEDocumentType.valueOf(AEDocumentType.System.ETAT_ANNUEL_DES_COMPTES_DRAFT);
						AEDocumentDAO docDAO = daoFactory.getDocumentDAO(docType, localConnection);
						AEDocumentType docTypeExt = docDAO.loadTypeId(att.getAttachedTo().getDescriptor().getID());
						if(docType.equals(docTypeExt)) {
							delete = true;
						}
					} else {
						throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
					};
					
					if(delete) {
						// delete from attachments
						attDao.delete(att.getID());

						// delete file
						Path fullPath = Paths.get(att.getRemoteRoot(), att.getRemotePath());
						if(Files.exists(fullPath) && !Files.isDirectory(fullPath)) {
							Files.deleteIfExists(fullPath);
						}

						deletedPath = Paths.get(att.getName());
					} else {
						throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
					}
				} else {
					throw new AEException("Attachment doesn't exist ");
				}
			} else if("175b3".equalsIgnoreCase(moduleCode)) {
				if(ap.isMemberOf(AuthRole.System.administrator)) {
					// check for deleteion
					FileAttachmentDAO attDao = daoFactory.getFileAttachmentDAO(localConnection);
					FileAttachment att = attDao.load(FileAttachment.lazyDescriptor(attId));
					if(att != null) {
						// delete from attachments
						attDao.delete(att.getID());

						// delete file
						Path fullPath = Paths.get(att.getRemoteRoot(), att.getRemotePath());
						if(Files.exists(fullPath) && !Files.isDirectory(fullPath)) {
							Files.deleteIfExists(fullPath);
						}

						deletedPath = Paths.get(att.getName());
					} else {
						throw new AEException("Attachment doesn't exist ");
					}
				} else {
					throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
				}
			} else {
				Path sharedPath = getModuleRepositoryPath(aeRequest, invContext);
				String fileName = arguments.getString("fileName");

				Path absoluteSharedPath = Paths.get(sharedPath.toString(), fileName);
				if(sharedFile) {
					// shared file can be deleted only by administrator
					if(ap.isMemberOf(AuthRole.System.administrator)) {
						if(Files.exists(absoluteSharedPath) && !Files.isDirectory(absoluteSharedPath) 
								&& absoluteSharedPath.startsWith(sharedPath)) { // check for fileName injection: as wrong relative path

							Files.deleteIfExists(absoluteSharedPath);
							deletedPath = absoluteSharedPath;
						}
					} else {
						throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
					}
				} else {
					// private file can be deleted by everyone 
					Path absolutePrivatePath = Paths.get(sharedPath.toString(), Long.toString(sOwnerId), fileName);
					if(Files.exists(absolutePrivatePath) && !Files.isDirectory(absolutePrivatePath)
							&& absolutePrivatePath.startsWith(Paths.get(sharedPath.toString(), Long.toString(sOwnerId)))) { // check for fileName injection: as wrong relative path

						Files.deleteIfExists(absolutePrivatePath);
						deletedPath = absolutePrivatePath;
					}
				}
			}

			/**
			 * Create and return response
			 */
			JSONObject f = new JSONObject()
			.put(AEDomainObject.JSONKey.id.name(), attId)
			.put(AEDomainObject.JSONKey.code.name(), 
					deletedPath != null ? AEMimeTypes.getMimeType(FilenameUtils.getExtension(deletedPath.getFileName().toString())) : AEStringUtil.EMPTY_STRING)
			.put(AEDomainObject.JSONKey.name.name(), 
					deletedPath != null ? deletedPath.getFileName().toString() : AEStringUtil.EMPTY_STRING)
			.put(AEDomainObject.JSONKey.description.name(), AEStringUtil.EMPTY_STRING)
			.put(AEDomainObject.JSONKey.system.name(), true);

			return new AEResponse(new JSONObject().put("file", f));
		} catch (Exception e) {
			logger.error(e);
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public JSONArray listAttachedFiles(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();

			// common attributes
			long ownerId = arguments.getLong(AEDomainObject.JSONKey.ownerId.name());
			long sOwnerId = arguments.getLong(AEDomainObject.JSONKey.sOwnerId.name());
			String moduleCode = arguments.getString(AEAppModule.JSONKey.appModuleCode.name());

			/**
			 * Authorize for concrete tenant
			 */
			// state validation
			if(ownerId != sOwnerId) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}

			// whether this principal is authorized for specified tenant
			ap.ownershipValidator(ownerId);
			
			/**
			 * Authorize 
			 */
			authorize(new AuthPermission("System/FinancialTransaction/Attachment", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * Factories and local variables
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			AEDescriptor tenantDescr = Organization.lazyDescriptor(ownerId);

			FileAttachmentsList attList = new FileAttachmentsList();
			FileAttachmentDAO attDao = daoFactory.getFileAttachmentDAO(localConnection);
			FileAttachmentFilter attFilter = new FileAttachmentFilter()
				.withTenant(tenantDescr)
				.withFolder(FileAttachmentFilter.Folder.all);
			if("170b3".equalsIgnoreCase(moduleCode)) {
				// attached to FinancialTransaction
				attList.addAll(attDao.listFTAttachments(attFilter));
				
				// attached to Document with 
				attFilter.withDocType(AEDocumentType.valueOf(AEDocumentType.System.ETAT_ANNUEL_DES_COMPTES_DRAFT));
				attList.addAll(attDao.listDocAttachments(attFilter));
			} else if("175b3".equalsIgnoreCase(moduleCode)) {
				// attached to Document with set of docTypes
				attFilter.withDocType(AEDocumentType.valueOf(AEDocumentType.System.BordereauParoisse));
				attFilter.withDocType(AEDocumentType.valueOf(AEDocumentType.System.Cerfa_11580_03));
				attFilter.withDocType(AEDocumentType.valueOf(AEDocumentType.System.ETAT_ANNUEL_DES_COMPTES));
				attList = attDao.listDocAttachments(attFilter);
			} else if("195b3".equalsIgnoreCase(moduleCode)) {
				attFilter.withDocType(AEDocumentType.valueOf(AEDocumentType.System.BordereauParoisse));
				attList = attDao.listDocAttachments(attFilter);
			}

			/**
			 * Create and return response
			 */
			JSONArray files = attList.toJSONArrayExt();
			//				.put(AEDomainObject.JSONKey.code.name(), AEMimeTypes.getMimeType(FilenameUtils.getExtension(absolutePath.getFileName().toString())))
			//				.put(AEDomainObject.JSONKey.name.name(), absolutePath.getFileName().toString())
			//				.put(AEDomainObject.JSONKey.description.name(), AEStringUtil.EMPTY_STRING)
			//				.put(AEDomainObject.JSONKey.system.name(), true);

			return files;
		} catch (Exception e) {
			logger.error("listAttachedFiles failed ", e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse getAttachmentPath(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();

			// common attributes
			long ownerId = arguments.getLong(AEDomainObject.JSONKey.ownerId.name());
			long sOwnerId = arguments.getLong(AEDomainObject.JSONKey.sOwnerId.name());

			/**
			 * Authorize for concrete tenant
			 */
			// state validation
			if(ownerId != sOwnerId) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}

			// whether this principal is authorized for specified tenant
			ap.ownershipValidator(ownerId);
			
			/**
			 * Authorize 
			 */
			authorize(new AuthPermission("System/Bibliotheque/Repository", AuthPermission.READ), invContext, Organization.lazyDescriptor(ownerId));

			/**
			 * Factories
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			invContext.setProperty(AEInvocationContext.AEConnection, localConnection);

			/**
			 * Go
			 */
			boolean embedded = arguments.optBoolean("embedded");
			JSONObject payload = null;
			Path tmpPath = null;
			Path fullPath = null;
			// path
			long attId = arguments.getLong("attId");
			FileAttachmentDAO attDao = daoFactory.getFileAttachmentDAO(localConnection);
			FileAttachment att = attDao.load(FileAttachment.lazyDescriptor(attId));
			if(att != null) {
				// path
				fullPath = Paths.get(att.getRemoteRoot(), att.getRemotePath());
				String fileName = att.getName();

				if(!Files.exists(fullPath)) {
					throw new AEException("The file doesn't exist ");
				}

				// extract the data to the tmp folder
				tmpPath = Paths.get(
						TMP_FILE_REPOSITORY.toString(), 
						AEFileUtil.createTempFileName("tmp_jcr_", FilenameUtils.getExtension(fileName), TMP_FILE_REPOSITORY.toFile()));
				Files.copy(fullPath, tmpPath, StandardCopyOption.REPLACE_EXISTING);

				// create response payload
				StringBuilder downloadUrl = new StringBuilder()
					.append("../FileDownloadServlet?")
					.append("file=").append(tmpPath.getFileName())
					.append("&fileName=").append(fileName)
					.append("&deleteOnExit=").append(true)
					.append("&embedded=").append(embedded);

				payload = new JSONObject()
					.put("file", tmpPath.getFileName())
					.put("fileName", fileName)
					.put("downloadUrl", downloadUrl);
			} else {
				throw new AEException("Attachment doesn't exist ");
			}

			/**
			 * Create and return response
			 */
			if(payload == null) {
				payload = new JSONObject();
			}
			return new AEResponse(payload);
		} catch (Exception e) {
			logger.error(e);
			AEConnection.rollback(localConnection);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}

	}
}
