package eu.agileeng.domain.messaging.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.messaging.DiscussionBoardPost;
import eu.agileeng.domain.messaging.DiscussionBoardPostsList;
import eu.agileeng.domain.messaging.DiscussionBoardSubject;
import eu.agileeng.domain.messaging.DiscussionBoardSubjectList;
import eu.agileeng.domain.messaging.DiscussionBoardTask;
import eu.agileeng.domain.messaging.DiscussionBoardTaskDecision;
import eu.agileeng.domain.messaging.DiscussionBoardTaskDecisionsList;
import eu.agileeng.domain.messaging.DiscussionBoardTasksList;
import eu.agileeng.domain.messaging.TaskFilter;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEStringUtil;

public class MessagingDAO extends AbstractDAO {
	
	private static final String insertSQLDiscussionSubject = "insert into DiscussionBoardSubject (PARENT, CODE, NAME, DESCRIPTION, OWNER_ID, TYPE, DATE, COMPANY_ID, STATE)"
			+" values (?, ?, ?, ?, ?, ?, GETDATE(), ?, ?)";
	private static final String insertSQLDiscussionPost = "insert into DiscussionBoardPost (BODY, TOPIC, AUTHOR_ID, NAME, DESCRIPTION, CODE, DATE, TYPE)"
			+" values (?, ?, ?, ?, ?, ?, GETDATE(), ?)";
	
	private static final String insertSQLTask = "insert into DiscussionBoardTask (NAME, DESCRIPTION, CODE, TYPE, PRIORITY, ASSIGNED_TO, DEV_DUE_DATE,"
			+" VALID_DUE_DATE, STATE, RELEASED_DATE, TOPIC_ID, LAST_POST_REF, HASHTAG, PROGRESS)"
			+" values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String insertSQLTaskDecision = "insert into DiscussionBoardTaskDecision (TASK_ID, AUTH_PR_ID) VALUES (?, ?)";
	
	private static final String selectSQLSubjectByParent = "select * from DiscussionBoardSubject where PARENT = ?";
	
	private static final String selectSQLRootSubjects = "select * from DiscussionBoardSubject where PARENT is NULL AND COMPANY_ID = ?";
	
	private static final String selectSQLPostsByTopic = "select post.*, principal.NAME as AUTHOR_NAME from DiscussionBoardPost post"
			+" inner join AuthPrincipal principal on post.AUTHOR_ID = principal.ID"
			+" where post.TOPIC = ? ORDER BY DATE DESC";
	
	private static final String selectSQLTasksByTopicId = "select * from DiscussionBoardTask where TOPIC_ID = ?";
	
	private static final String selectSQLTasksByTenantId = "select * from DiscussionBoardTask task"
			+" inner join DiscussionBoardSubject sub on task.TOPIC_ID = sub.ID"
			+" where sub.COMPANY_ID = ?";
	
	private static final String selectSQLDecisionsByTaskId = "select dbtd.*,ap.NAME as AUTH_NAME, ap.FIRST_NAME, ap.LAST_NAME from DiscussionBoardTaskDecision dbtd"
			+" inner join AuthPrincipal ap on dbtd.AUTH_PR_ID = ap.ID"
			+" where dbtd.TASK_ID = ?";

	private static final String updateSQLSubjectState = "update DiscussionBoardSubject set STATE = ? WHERE ID = ?";
	
	private static final String updateSQLTask = "update DiscussionBoardTask set NAME = ?, DESCRIPTION = ?, CODE = ?, TYPE = ?, PRIORITY = ?, ASSIGNED_TO = ?, DEV_DUE_DATE = ?,"
			+" VALID_DUE_DATE = ?, STATE = ?, RELEASED_DATE = ?, TOPIC_ID = ?, LAST_POST_REF = ?, HASHTAG = ?, PROGRESS = ?"
			+" where ID = ?";
	
	private static final String updateSQLTaskDecisionByPrincipalId = "update DiscussionBoardTaskDecision set DECISION = ? where AUTH_PR_ID = ? and TASK_ID = ?";
	
	public MessagingDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
		// TODO Auto-generated constructor stub
	}
	
	//############################ Build PreparedStatement ##################################################
	
	public int build(DiscussionBoardSubject subject, PreparedStatement ps, int i) throws SQLException, AEException {
		
		//PARENT
		if(subject.getParentId() <= 0) {
			ps.setNull(i++, Types.BIGINT);
		} else {
			ps.setLong(i++, subject.getParentId());
		}
		
		//CODE
		ps.setString(i++, subject.getCode());
		//NAME
		ps.setString(i++, subject.getName());
		//DESCRIPTION
		ps.setString(i++, subject.getDescription());
		//OWNER_ID
		ps.setLong(i++, subject.getOwnerId());
		//TYPE
		ps.setLong(i++, subject.getType());
		//COMPANY_ID
		ps.setLong(i++, subject.getCompany().getDescriptor().getID());
		//STATE
		ps.setLong(i++, subject.getState());

		
		return i;
	}
	
	public int build(DiscussionBoardPost post, PreparedStatement ps, int i) throws SQLException, AEException {
		//BODY
		ps.setString(i++, post.getMessageBody());
		//TOPIC
		ps.setLong(i++, post.getTopicId());
		//AUTHOR_ID
		ps.setLong(i++, post.getAuthorId());
		//NAME
		ps.setString(i++, post.getName());
		//DESCRIPTION
		ps.setString(i++, post.getDescription());
		//CODE
		ps.setString(i++, post.getCode());
		//TYPE
		ps.setLong(i++, post.getType());

		
		return i;
	}
	
	public int build(DiscussionBoardTask t, PreparedStatement ps, int i) throws SQLException, AEException {
		//NAME
		ps.setString(i++, t.getName());
		//DESCRIPTION
		ps.setString(i++, t.getDescription());
		//CODE
		ps.setString(i++, t.getCode());
		//TYPE
		ps.setLong(i++, t.getType());
		//PRIORITY
		ps.setLong(i++, t.getPriority());
		//ASSIGNED_TO
		ps.setLong(i++, t.getAssignedTo());
		//DEV_DUE_DATE
		if (t.getDevDueDate() != null) {
			ps.setDate(i++, new java.sql.Date(t.getDevDueDate().getTime()));
		} else {
			ps.setNull(i++, java.sql.Types.DATE);
		}
		//VALID_DUE_DATE
		if (t.getValidDueDate() != null) {
			ps.setDate(i++, new java.sql.Date(t.getValidDueDate().getTime()));
		} else {
			ps.setNull(i++, java.sql.Types.DATE);
		}
		//STATE
		ps.setLong(i++, t.getState());
		//RELEASED_DATE
		if (t.getReleasedDate() != null) {
			ps.setDate(i++, new java.sql.Date(t.getReleasedDate().getTime()));
		} else {
			ps.setNull(i++, java.sql.Types.DATE);
		}
		//TOPIC_ID
		if (t.getTopicId() > 0) {
			ps.setLong(i++, t.getTopicId());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		//LAST_POST_REF
		if (t.getLastPostRef() > 0) {
			ps.setLong(i++, t.getLastPostRef());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		//HASHTAG
		ps.setString(i++, t.getHashtag());
		//PROGRESS
		ps.setDouble(i++, t.getProgress());
		
		return i;
	}
	
	public int build(DiscussionBoardTaskDecision d, PreparedStatement ps, int i) throws SQLException, AEException {
		//TASK_ID
		ps.setLong(i++, d.getTaskId());
		//AUTH_PR_ID
		ps.setLong(i++, d.getAuthPrincipalId());
		
		return i;
	}
	
	//############################ INSERT METHODS ###########################################################

	public DiscussionBoardSubject insertSubject(DiscussionBoardSubject subject) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
	    
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLDiscussionSubject);
			
			int i = 1;
			i = build(subject, ps, i);
			
						
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				
				//required
				subject.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			

			
			return subject;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public DiscussionBoardSubject insertTopic(DiscussionBoardSubject subject, DiscussionBoardPost post) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
	    
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLDiscussionSubject);
			
			int i = 1;
			i = build(subject, ps, i);
			
						
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				
				//required
				subject.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			post.setTopicId(subject.getID());
			
			insertPost(post);

			
			return subject;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public DiscussionBoardPost insertPost(DiscussionBoardPost post) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
	    
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLDiscussionPost);
			
			int i = 1;
			build(post, ps, i);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				
				//required
				post.setID(id);
				
				
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			

			
			return post;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public DiscussionBoardTask insertTask(DiscussionBoardTask task) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
	    
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLTask);
			
			int i = 1;
			build(task, ps, i);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				
				//required
				task.setID(id);
				
				
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// insert decisions
			for(DiscussionBoardTaskDecision decision : task.getDecisions()) {
				decision.setTaskId(task.getID());
				insertTaskDecision(decision);
			}

			
			return task;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public DiscussionBoardTaskDecision insertTaskDecision(DiscussionBoardTaskDecision decision) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
	    
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLTaskDecision);
			
			int i = 1;
			build(decision, ps, i);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				
				//required
				decision.setID(id);
				
				
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			

			
			return decision;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	//############################ UPDATE METHODS ##########################################################
	
	public DiscussionBoardTask updateTask(DiscussionBoardTask task) throws AEException {
		PreparedStatement ps = null;
		try {
			ps = getAEConnection().prepareStatement(updateSQLTask);
			
			int i = 1;
			
			i = build(task, ps, i);
			ps.setLong(i++, task.getID());
			
			ps.executeUpdate();
			
			// only one decision can be updated at once
			if (task.getDecisions() != null) updateTaskDecision(task.getDecisions().get(0));
			
			return task;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(ps);
			close();
		}
		
	}
	
	public DiscussionBoardTaskDecision updateTaskDecision(DiscussionBoardTaskDecision d) throws AEException {
		  PreparedStatement ps = null;
		  try {
		   ps = getAEConnection().prepareStatement(updateSQLTaskDecisionByPrincipalId);
		   
		   int i = 1;
		   
		   ps.setInt(i++, d.getDecision());
		   ps.setLong(i++, d.getAuthPrincipalId());
		   ps.setLong(i++, d.getTaskId());
		   
		   ps.executeUpdate();
		   
		   return d;
		  } catch (Throwable t) {
		   throw new AEException(t);
		  } finally {
		   AEConnection.close(ps);
		   close();
		  }
		  
		 }
	
	//############################ Select methods ##########################################################
	
	public DiscussionBoardSubjectList selectSubjectsByParent(long parentId, long companyId) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
	    
		try {
			DiscussionBoardSubjectList sList = new DiscussionBoardSubjectList();
			// prepare statement and insert
			
			
			if(parentId <= 0) {
				//selectSQLRootSubjects
				ps = getAEConnection().prepareStatement(selectSQLRootSubjects);
				ps.setLong(1, companyId);
			} else {
				ps = getAEConnection().prepareStatement(selectSQLSubjectByParent);
				ps.setLong(1, parentId);
			}
			
			
			
			rs = ps.executeQuery();
			while (rs.next()) {
				DiscussionBoardSubject subject = new DiscussionBoardSubject();
				
				buildSubject(subject, rs);
				
				subject.setView();
				sList.add(subject);
			}
			

			
			return sList;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public DiscussionBoardPostsList selectPostsByTopic(long topicId) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
	    
		try {
			DiscussionBoardPostsList pList = new DiscussionBoardPostsList();
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(selectSQLPostsByTopic);
			
			ps.setLong(1, topicId);
			
			
			rs = ps.executeQuery();
			while (rs.next()) {
				DiscussionBoardPost post = new DiscussionBoardPost();
				
				buildPost(post, rs);
				
				post.setView();
				pList.add(post);
			}
			
			
			return pList;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public DiscussionBoardTasksList selectTasksByOwnerId(long tenantId) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
	    
		try {
			DiscussionBoardTasksList tList = new DiscussionBoardTasksList();
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(selectSQLTasksByTenantId);
			
			ps.setLong(1, tenantId);
			
			
			rs = ps.executeQuery();
			while (rs.next()) {
				DiscussionBoardTask task = new DiscussionBoardTask();
				
				buildTask(task, rs);
				
				task.setView();
				tList.add(task);
			}
			
			
			return tList;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public DiscussionBoardTasksList applyFilter(long tenantId, TaskFilter filter) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
	    
		try {
			DiscussionBoardTasksList tList = new DiscussionBoardTasksList();
			
			StringBuilder str = new StringBuilder(selectSQLTasksByTenantId);
			
			//filter by date
			if (filter.getFromDevDueDate() != null && filter.getToDevDueDate() != null) {
				str.append(" and task.DEV_DUE_DATE between '").append(AEDateUtil.convertToString(filter.getFromDevDueDate(), AEDateUtil.EXPORT_FILE_DATE_FORMAT)).
				append("' and '").append(AEDateUtil.convertToString(filter.getToDevDueDate(), AEDateUtil.EXPORT_FILE_DATE_FORMAT)).append("'");
			} else if (filter.getFromDevDueDate() != null) {
				str.append(" and task.DEV_DUE_DATE > '").append(AEDateUtil.convertToString(filter.getFromDevDueDate(), AEDateUtil.EXPORT_FILE_DATE_FORMAT)).append("'");
			} else if (filter.getToDevDueDate() != null) {
				str.append(" and task.DEV_DUE_DATE < '").append(AEDateUtil.convertToString(filter.getToDevDueDate(), AEDateUtil.EXPORT_FILE_DATE_FORMAT)).append("'");
			}
			
			//filter by code
			if (!AEStringUtil.isEmpty(filter.getCode())) {
				str.append(" and upper(task.CODE) LIKE '%").append(filter.getCode().toUpperCase()).append("%'");
			}
			
			//filter by priority
			if (filter.getPriority() > 0) {
				str.append(" and task.PRIORITY = ").append(filter.getPriority());
			}
			
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(str.toString());
			ps.setLong(1, tenantId);
			
			
			rs = ps.executeQuery();
			while (rs.next()) {
				DiscussionBoardTask task = new DiscussionBoardTask();
				
				buildTask(task, rs);
				
				task.setView();
				tList.add(task);
			}
			
			
			return tList;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public DiscussionBoardTasksList selectTasksByTopicId(long topicId) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
	    
		try {
			DiscussionBoardTasksList tList = new DiscussionBoardTasksList();
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(selectSQLTasksByTopicId);
			
			ps.setLong(1, topicId);
			
			
			
			rs = ps.executeQuery();
			while (rs.next()) {
				DiscussionBoardTask task = new DiscussionBoardTask();
				
				buildTask(task, rs);
				
				task.setView();
				//Load decisions
				task.setDecisions(selectDecisionsByTaskId(task.getID()));
				
				tList.add(task);
			}
			
			
			return tList;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public DiscussionBoardTaskDecisionsList selectDecisionsByTaskId(long taskId) throws AEException {
		PreparedStatement ps = null;
	    ResultSet rs = null;
	    
		try {
			DiscussionBoardTaskDecisionsList dList = new DiscussionBoardTaskDecisionsList();
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(selectSQLDecisionsByTaskId);
			
			ps.setLong(1, taskId);
			
			
			
			rs = ps.executeQuery();
			while (rs.next()) {
				DiscussionBoardTaskDecision decision = new DiscussionBoardTaskDecision();
				
				buildDecision(decision, rs);
				
				decision.setView();
				dList.add(decision);
			}
			
			
			return dList;
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public long updateSubjectState(long state, long topicId) throws AEException {
		PreparedStatement ps = null;
		try {
			ps = this.getAEConnection().prepareStatement(updateSQLSubjectState);
			ps.setLong(1, state);
			ps.setLong(2, topicId);
			
			int rowCount = ps.executeUpdate();
		} catch (Throwable t) {
			throw new AEException(t);
		} finally {
			AEConnection.close(ps);
			this.close();
		}
		return state;
	}
	
	//############################ Build Objects ###########################################################
	
	public void buildSubject(DiscussionBoardSubject subject, ResultSet rs) throws SQLException {

		//ID
		subject.setID(rs.getLong("ID"));
		//PARENT
		subject.setParentId(rs.getLong("PARENT"));
		//CODE
		subject.setCode(rs.getString("CODE"));
		//NAME
		subject.setName(rs.getString("NAME"));
		//DESCRIPTION
		subject.setDescription(rs.getString("DESCRIPTION"));
		//OWNER_ID
		subject.setOwnerId(rs.getLong("OWNER_ID"));
		//DATE
		subject.setDateCreatred(rs.getDate("DATE"));
		//TYPE
		subject.setType(rs.getLong("TYPE"));
		//COMPANY_ID
		subject.setCompany(Organization.lazyDescriptor(rs.getLong("COMPANY_ID")));
		//STATE
		subject.setState(rs.getLong("STATE"));
	}
	
	public void buildPost(DiscussionBoardPost post, ResultSet rs) throws SQLException {

		//ID
		post.setID(rs.getLong("ID"));
		//BODY
		post.setMessageBody(rs.getString("BODY"));
		//TOPIC
		post.setTopicId(rs.getLong("TOPIC"));
		//AUTHOR_ID
		post.setAuthorId(rs.getLong("AUTHOR_ID"));
		//NAME
		post.setName(rs.getString("NAME"));
		//DESCRIPTION
		post.setDescription(rs.getString("DESCRIPTION"));
		//CODE
		post.setCode(rs.getString("CODE"));
		//DATE
		post.setDateCreated(rs.getTimestamp("DATE"));
		//AUTHOR_NAME
		post.setAuthorName(rs.getString("AUTHOR_NAME"));
		//TYPE
		post.setType(rs.getType());
	}
	
	public void buildTask(DiscussionBoardTask task, ResultSet rs) throws SQLException {
		//ID
		task.setID(rs.getLong("ID"));
		//NAME
		task.setName(rs.getString("NAME"));
		//DESCRIPTION
		task.setDescription(rs.getString("DESCRIPTION"));
		//CODE
		task.setCode(rs.getString("CODE"));
		//TYPE
		task.setType(rs.getLong("TYPE"));
		//PRIORITY
		task.setPriority(rs.getLong("PRIORITY"));
		//ASSIGNED_TO
		task.setAssignedTo(rs.getLong("ASSIGNED_TO"));
		//DEV_DUE_DATE
		if (rs.getDate("DEV_DUE_DATE") != null) {
			task.setDevDueDate(new Date(rs.getDate("DEV_DUE_DATE").getTime()));
		}
		//VALID_DUE_DATE
		if (rs.getDate("VALID_DUE_DATE") != null) {
			task.setValidDueDate(new Date(rs.getDate("VALID_DUE_DATE").getTime()));
		}
		//STATE
		task.setState(rs.getLong("STATE"));
		//RELEASED_DATE
		task.setReleasedDate(rs.getDate("RELEASED_DATE"));
		//TOPIC_ID
		task.setTopicId(rs.getLong("TOPIC_ID"));
		//LAST_POST_REF
		task.setLastPostRef(rs.getLong("LAST_POST_REF"));
		//HASHTAG
		task.setHashtag(rs.getString("HASHTAG"));
		//PROGRESS
		task.setProgress(rs.getDouble("PROGRESS"));

	}
	
	public void buildDecision(DiscussionBoardTaskDecision d, ResultSet rs) throws SQLException {

		//ID
		d.setID(rs.getLong("ID"));
		//TASK_ID
		d.setTaskId(rs.getLong("TASK_ID"));
		//AUTH_PR_ID
		d.setAuthPrincipalId(rs.getLong("AUTH_PR_ID"));
		//DECISION
		d.setDecision(rs.getInt("DECISION"));
		//DECISION_MADE_DATE
		if (rs.getDate("DECISION_MADE_DATE") != null) d.setDecisionMadeDate(new Date(rs.getDate("DECISION_MADE_DATE").getTime()));
		//auth column name display
		d.setAuthName(rs.getString("FIRST_NAME")+" "+rs.getString("LAST_NAME")+" ("+rs.getString("AUTH_NAME")+")");
		
	}

}
