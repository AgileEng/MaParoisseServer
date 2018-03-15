package eu.agileeng.persistent.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.jcr.JcrNode;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.oracle.OrganizationDAO;
import eu.agileeng.security.AuthRole;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AEInvocationContextValidator;
import eu.agileeng.util.AEStringUtil;

public class JcrSession {
	/**
	 * Wrapped JCR Session
	 */
	private Session session;

	/**
	 * Whether this owns <code>session</code> or not
	 */
	private boolean sessionOwner;

	/**
	 * 
	 */
	public JcrSession(Session session, boolean sessionOwner) {
		this.session = session;
		this.sessionOwner = sessionOwner;
	}

	/**
	 * @return the connection
	 */
	public final Session getSession() {
		return this.session;
	}

	public boolean isSessionOwner() {
		return sessionOwner;
	}

	public static void close(JcrSession jcrSession) {
		if(jcrSession != null && jcrSession.session != null) {
			try {
				jcrSession.session.logout();
			} catch (Throwable t) {
			}
		}
	}

	public void save() throws AEException {
		if(session != null) {
			try {
				session.save();
			} catch (Exception e) {
				throw new AEException(e);
			}
		}
	}

	public String grantPath(Long ownerId, String moduleId, String path, AEInvocationContext invContext) throws AEException {
		AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
		invContextValidator.validate(invContext);

		try {
			String retPath = path;
			boolean haveUpdates = false;
			if(AEStringUtil.isEmpty(retPath)) {
				if(ownerId == null) {
					throw new AEException(
							AEError.System.OWNER_ID_NOT_SPECIFIED.getSystemID(), 
							"The compay ID missing");
				}

				retPath = "/" + ownerId.toString();
				if(!AEStringUtil.isEmpty(moduleId)) {
					retPath = retPath + "/" + moduleId;
				}
				if(!getSession().nodeExists(retPath)) {
					Node rootNode = getSession().getRootNode();
					String strOwnerId = ownerId.toString();
					String strOwnerName = null;

					// mandatory
					Node companyNode = null;
					if(rootNode.hasNode(strOwnerId)) {
						companyNode = rootNode.getNode(strOwnerId);
					} else {
						AEConnection dbConn = null;
						try {
							DAOFactory daoFactory = DAOFactory.getInstance();
							dbConn = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
							OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(dbConn);
							AEDescriptor orgDescr = orgDAO.loadDescriptor(ownerId);
							if(orgDescr != null) {
								strOwnerName = orgDescr.getName();
							}
						} catch(Exception e) {

						} finally {
							AEConnection.close(dbConn);
						}

						companyNode = rootNode.addNode(strOwnerId, JcrNode.JcrNodeType.folder);
						companyNode.addMixin(JcrNode.JcrNodeType.mixUnstructured);
						companyNode.setProperty(JcrNode.JcrProperty.ae_system, true);
						companyNode.setProperty(
								JcrNode.JcrProperty.ae_createdByRole, 
								AuthRole.System.administrator.getSystemID());
						companyNode.setProperty(JcrNode.JcrProperty.ae_code, "company_root");
						if(!AEStringUtil.isEmpty(strOwnerName)) {
							companyNode.setProperty(JcrNode.JcrProperty.ae_displayName, strOwnerName);
						}
						haveUpdates = true;
					}

					// optional
					if(!AEStringUtil.isEmpty(moduleId)) {
						if(!companyNode.hasNode(moduleId)) {
							Node moduleIdNode = companyNode.addNode(moduleId, JcrNode.JcrNodeType.folder);;
							moduleIdNode.addMixin(JcrNode.JcrNodeType.mixUnstructured);
							moduleIdNode.setProperty(JcrNode.JcrProperty.ae_system, true);
							moduleIdNode.setProperty(
									JcrNode.JcrProperty.ae_createdByRole, 
									AuthRole.System.administrator.getSystemID());
							moduleIdNode.setProperty(JcrNode.JcrProperty.ae_code, "module_root");
							haveUpdates = true;
						}
					}

					if(haveUpdates) {
						save();
					}

					assert(getSession().nodeExists(retPath));
				}
			}
			return retPath;
		} catch (RepositoryException e) {
			throw new AEException(e);
		}
	}

	public String grantDematboxPath(Long ownerId, String moduleName, String serviceName, AEInvocationContext invContext) throws AEException {
		AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
		invContextValidator.validate(invContext);

		try {
			String retPath = null;
			boolean haveUpdates = false;

			if(ownerId == null) {
				throw new AEException(
						AEError.System.OWNER_ID_NOT_SPECIFIED.getSystemID(), 
						"The compay ID missing");
			}

			// build retPath
			retPath = "/" + ownerId.toString();
			if(!AEStringUtil.isEmpty(moduleName)) {
				retPath = retPath + "/" + moduleName;
			}
			if(!AEStringUtil.isEmpty(serviceName)) {
				retPath = retPath + "/" + serviceName;
			}
			
			// grant path
			if(!getSession().nodeExists(retPath)) {
				Node rootNode = getSession().getRootNode();
				String strOwnerId = ownerId.toString();
				String strOwnerName = null;

				Node companyNode = null;
				Node moduleNode = null;
				Node serviceNode = null;
				
				// company node (First level)
				if(rootNode.hasNode(strOwnerId)) {
					companyNode = rootNode.getNode(strOwnerId);
				} else {
					AEConnection dbConn = null;
					try {
						DAOFactory daoFactory = DAOFactory.getInstance();
						dbConn = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
						OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(dbConn);
						AEDescriptor orgDescr = orgDAO.loadDescriptor(ownerId);
						if(orgDescr != null) {
							strOwnerName = orgDescr.getName();
						}
					} catch(Exception e) {

					} finally {
						AEConnection.close(dbConn);
					}

					companyNode = rootNode.addNode(strOwnerId, JcrNode.JcrNodeType.folder);
					companyNode.addMixin(JcrNode.JcrNodeType.mixUnstructured);
					companyNode.setProperty(JcrNode.JcrProperty.ae_system, true);
					companyNode.setProperty(
							JcrNode.JcrProperty.ae_createdByRole, 
							AuthRole.System.administrator.getSystemID());
					companyNode.setProperty(JcrNode.JcrProperty.ae_code, "company_root");
					if(!AEStringUtil.isEmpty(strOwnerName)) {
						companyNode.setProperty(JcrNode.JcrProperty.ae_displayName, strOwnerName);
					}
					haveUpdates = true;
				}

				// module node (Second level)
				if(companyNode.hasNode(moduleName)) {
					moduleNode = companyNode.getNode(moduleName);
				} else {
					moduleNode = companyNode.addNode(moduleName, JcrNode.JcrNodeType.folder);;
					moduleNode.addMixin(JcrNode.JcrNodeType.mixUnstructured);
					moduleNode.setProperty(JcrNode.JcrProperty.ae_system, true);
					moduleNode.setProperty(
							JcrNode.JcrProperty.ae_createdByRole, 
							AuthRole.System.administrator.getSystemID());
					moduleNode.setProperty(JcrNode.JcrProperty.ae_code, "module_root");
					haveUpdates = true;
				}

				// service node (Third level)
				if(moduleNode.hasNode(serviceName)) {
					serviceNode = moduleNode.getNode(serviceName);
				} else {
					serviceNode = moduleNode.addNode(serviceName, JcrNode.JcrNodeType.folder);;
					serviceNode.addMixin(JcrNode.JcrNodeType.mixUnstructured);
					serviceNode.setProperty(JcrNode.JcrProperty.ae_system, true);
					serviceNode.setProperty(
							JcrNode.JcrProperty.ae_createdByRole, 
							AuthRole.System.administrator.getSystemID());
					serviceNode.setProperty(JcrNode.JcrProperty.ae_code, "module_root");
					haveUpdates = true;
				}
				
				if(haveUpdates) {
					save();
				}

				assert(getSession().nodeExists(retPath));
			}
			return retPath;
		} catch (RepositoryException e) {
			throw new AEException(e);
		}
	}
}
