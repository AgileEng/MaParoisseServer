package eu.agileeng.domain.messaging.ejb;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.messaging.DiscussionBoardPost;
import eu.agileeng.domain.messaging.DiscussionBoardSubject;
import eu.agileeng.domain.messaging.DiscussionBoardSubjectList;
import eu.agileeng.domain.messaging.DiscussionBoardTask;
import eu.agileeng.domain.messaging.DiscussionBoardTaskDecision;
import eu.agileeng.domain.messaging.DiscussionBoardTaskDecisionsList;
import eu.agileeng.domain.messaging.DiscussionBoardTasksList;
import eu.agileeng.domain.messaging.TaskFilter;
import eu.agileeng.domain.messaging.dao.MessagingDAO;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.security.AuthRole;
import eu.agileeng.security.ejb.dao.AuthPrincipalDAO;
import eu.agileeng.services.AEBean;
import eu.agileeng.services.AEInvocationContext;


public class MessagingBean extends AEBean implements MessagingLocal, MessagingRemote {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3133488636106675861L;

	public MessagingBean() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public DiscussionBoardSubjectList loadSubjectsByParentId(long parentId, long companyId) throws AEException {
		AEConnection localConnection = null;
		try {
			// get DAO and connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			
			MessagingDAO msgDAO = daoFactory.getMessagingDAO(localConnection);

			DiscussionBoardSubjectList sList = msgDAO.selectSubjectsByParent(parentId, companyId);
			
			return sList;			
		} catch (AEException e) {
			throw new AEException(e.getCode(),"Loading Error", e);
		} catch (Throwable t) {
			throw new AEException(t.getMessage(),t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public DiscussionBoardSubject saveSubject(DiscussionBoardSubject subject, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// get DAO and connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			
			
			
			MessagingDAO msgDAO = daoFactory.getMessagingDAO(localConnection);
			
			subject.setOwnerId(invContext.getAuthPrincipal().getID());

			msgDAO.insertSubject(subject);
			
			
			
			return subject;			
		} catch (AEException e) {
			
			throw new AEException(e.getCode(),"Save Error", e);
		} catch (Throwable t) {
			
			throw new AEException(t.getMessage(),t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public DiscussionBoardSubject saveTopicWithPost(DiscussionBoardSubject subject, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// get DAO and connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			
			MessagingDAO msgDAO = daoFactory.getMessagingDAO(localConnection);
			
			subject.setOwnerId(invContext.getAuthPrincipal().getID());
			
			localConnection.beginTransaction();

			msgDAO.insertSubject(subject);
			
			if(subject.getPosts().size() > 0) {
				subject.getPosts().get(0).setTopicId(subject.getID());
				
				subject.getPosts().get(0).setAuthorId(invContext.getAuthPrincipal().getID());
				
				msgDAO.insertPost(subject.getPosts().get(0));
			}
			
			localConnection.commit();
			
			return subject;			
		} catch (AEException e) {
			AEConnection.rollback(localConnection);
			throw new AEException(e.getCode(),"Save Error", e);
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t.getMessage(),t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public DiscussionBoardPost savePost(DiscussionBoardPost post, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// get DAO and connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			MessagingDAO msgDAO = daoFactory.getMessagingDAO(localConnection);

			AuthPrincipal ap = invContext.getAuthPrincipal();
			post.setAuthorId(ap.getID());
			
			localConnection.beginTransaction();
			msgDAO.insertPost(post);
			
			
			post.setDateCreated(new Date(System.currentTimeMillis()));
			post.setAuthorName(invContext.getAuthPrincipal().getName());
			
			// Modify the parent topic State if the current post is not regular or opening.
			if (post.getType() != DiscussionBoardPost.Type.REGULAR.getID() && post.getType() != DiscussionBoardPost.Type.OPENING.getID()) {
				// Throw an error if the user does not have the required access rights.
				// FIXME in Monentreprise. Also it would be a better idea to validate this before the insert ;)
				/*if (!(ap.hasAdministratorRights() 
						|| ap.hasPowerUserRights() 
						|| ap.isMemberOf(AuthRole.System.accountant)
						|| ap.isMemberOf(AuthRole.System.social))) {
					throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
				}*/
				long newSubjectState = 0;
				if (post.getType() == DiscussionBoardPost.Type.REOPENING.getID()) {
					newSubjectState = DiscussionBoardSubject.State.OPENED.getID();
				} else if (post.getType() == DiscussionBoardPost.Type.CLOSING.getID()) {
					newSubjectState = DiscussionBoardSubject.State.CLOSED.getID();
				}
				msgDAO.updateSubjectState(newSubjectState, post.getTopicId());
			}
			localConnection.commit();
			
			return post;			
		} catch (Throwable t) {
			AEConnection.rollback(localConnection);
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public DiscussionBoardSubject loadPosts(DiscussionBoardSubject subject) throws AEException {
		AEConnection localConnection = null;
		try {
			// get DAO and connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			
			MessagingDAO msgDAO = daoFactory.getMessagingDAO(localConnection);

			
			
			subject.setPosts(msgDAO.selectPostsByTopic(subject.getID()));
			
			
			
			return subject;			
		} catch (AEException e) {
			throw new AEException(e.getCode(),"Save Error", e);
		} catch (Throwable t) {
			throw new AEException(t.getMessage(),t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public DiscussionBoardTask saveTask(DiscussionBoardTask task) throws AEException {
		AEConnection localConnection = null;
		try {
			// get DAO and connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			
			MessagingDAO msgDAO = daoFactory.getMessagingDAO(localConnection);

			if (task.isPersistent()) {
				if (task.isUpdated()) { // UPDATE
					task = msgDAO.updateTask(task);
					
					task.setDecisions(msgDAO.selectDecisionsByTaskId(task.getID()));
				}
			} else { // INSERT
				
				// load involved authprincipals
				// for now just loading all with planner or approver role
				AuthPrincipalDAO authDAO = daoFactory.getAuthPrincipalDAO(localConnection);
				Set<AuthPrincipal> pSet = new HashSet<AuthPrincipal>();
				pSet.addAll(authDAO.loadPrincipalsByRoleSysId(AuthRole.System.planner.getSystemID(), "secal.png"));
				pSet.addAll(authDAO.loadPrincipalsByRoleSysId(AuthRole.System.approver.getSystemID(), "secal.png"));
				
				DiscussionBoardTaskDecisionsList dList = new DiscussionBoardTaskDecisionsList();
				//TODO insert authprincipals to DBTD table
				for (Iterator<AuthPrincipal> iterator = pSet.iterator(); iterator.hasNext();) {
					AuthPrincipal authPrincipal = (AuthPrincipal) iterator.next();
					
					DiscussionBoardTaskDecision decision = new DiscussionBoardTaskDecision();
					
					decision.setTaskId(task.getID());
					decision.setAuthPrincipalId(authPrincipal.getID());
					decision.setDecision(DiscussionBoardTaskDecision.Decision.UNDEFINED.getID());
					
					//msgDAO.insertTaskDecision(decision);
					dList.add(decision);
					
				}
				
				task.setDecisions(dList);
				task = msgDAO.insertTask(task);
			}
			
			
			return task;			
		} catch (AEException e) {
			throw new AEException(e.getCode(),"Save Error", e);
		} catch (Throwable t) {
			throw new AEException(t.getMessage(),t);
		} finally {
			AEConnection.close(localConnection);
		}
		
	}
	
	@Override
	public DiscussionBoardTasksList saveTopicTasks(DiscussionBoardTasksList tList, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// get DAO and connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			
			MessagingDAO msgDAO = daoFactory.getMessagingDAO(localConnection);
			
			
			for (DiscussionBoardTask task : tList) {
				if (task.isPersistent()) {
					if (task.isUpdated()) { // UPDATE
						task = msgDAO.updateTask(task);
					}
				} else { // INSERT
					
					// load involved authprincipals
					// for now just loading all with planner or approver role
					AuthPrincipalDAO authDAO = daoFactory.getAuthPrincipalDAO(localConnection);
					Set<AuthPrincipal> pSet = new HashSet<AuthPrincipal>();
					pSet.addAll(authDAO.loadPrincipalsByRoleSysId(AuthRole.System.planner.getSystemID(), "secal.png"));
					pSet.addAll(authDAO.loadPrincipalsByRoleSysId(AuthRole.System.approver.getSystemID(), "secal.png"));
					
					DiscussionBoardTaskDecisionsList dList = new DiscussionBoardTaskDecisionsList();
					//TODO insert authprincipals to DBTD table
					for (Iterator<AuthPrincipal> iterator = pSet.iterator(); iterator.hasNext();) {
						AuthPrincipal authPrincipal = (AuthPrincipal) iterator.next();
						
						DiscussionBoardTaskDecision decision = new DiscussionBoardTaskDecision();
						
						decision.setTaskId(task.getID());
						decision.setAuthPrincipalId(authPrincipal.getID());
						decision.setDecision(DiscussionBoardTaskDecision.Decision.UNDEFINED.getID());
						
						//msgDAO.insertTaskDecision(decision);
						dList.add(decision);
						
					}
					
					task.setDecisions(dList);
					// insert task itself
					task = msgDAO.insertTask(task);
				}
			
			}
			
			return tList;			
		} catch (AEException e) {
			throw new AEException(e.getCode(),"Save Error", e);
		} catch (Throwable t) {
			throw new AEException(t.getMessage(),t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public DiscussionBoardTasksList loadTasksByTopicId(long topicId, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// get DAO and connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			
			MessagingDAO msgDAO = daoFactory.getMessagingDAO(localConnection);
			
			DiscussionBoardTasksList tList = msgDAO.selectTasksByTopicId(topicId);
			
			
			return tList;			
		} catch (AEException e) {
			throw new AEException(e.getCode(),"Load Error", e);
		} catch (Throwable t) {
			throw new AEException(t.getMessage(),t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public DiscussionBoardTasksList filterTasks(long ownerId, TaskFilter filter, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// get DAO and connection
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			
			MessagingDAO msgDAO = daoFactory.getMessagingDAO(localConnection);
			
			DiscussionBoardTasksList tList = msgDAO.applyFilter(ownerId, filter);
			
			
			return tList;			
		} catch (AEException e) {
			throw new AEException(e.getCode(),"Load Error", e);
		} catch (Throwable t) {
			throw new AEException(t.getMessage(),t);
		} finally {
			AEConnection.close(localConnection);
		}
	}

}
