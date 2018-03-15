package eu.agileeng.domain.messaging.ejb;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.messaging.DiscussionBoardPost;
import eu.agileeng.domain.messaging.DiscussionBoardSubject;
import eu.agileeng.domain.messaging.DiscussionBoardSubjectList;
import eu.agileeng.domain.messaging.DiscussionBoardTask;
import eu.agileeng.domain.messaging.DiscussionBoardTasksList;
import eu.agileeng.domain.messaging.TaskFilter;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AEService;

public interface MessagingService extends AEService {
	/**
	 * Load Subjects by given parent id
	 * 
	 * @param parentId
	 * @return <code>DiscussionBoardSubjectList</code>
	 */
	public DiscussionBoardSubjectList loadSubjectsByParentId(long parentId, long companyId) throws AEException;
	
	/**
	 * Save Category/Topic
	 * 
	 * @param <code>DiscussionBoardSubject</code>
	 * @param <code>AEInvocationContext</code>
	 * @return <code>DiscussionBoardSubject</code>
	 */
	public DiscussionBoardSubject saveSubject(DiscussionBoardSubject subject, AEInvocationContext invContext) throws AEException;
	
	/**
	 * Save Topic with the first Post
	 * 
	 * @param <code>DiscussionBoardSubject</code> with posts property
	 * @param <code>AEInvocationContext</code>
	 * @return <code>DiscussionBoardSubject</code> with posts property
	 */
	public DiscussionBoardSubject saveTopicWithPost(DiscussionBoardSubject subject, AEInvocationContext invContext) throws AEException;
	
	/**
	 * Save a new post
	 * 
	 * @param <code>DiscussionBoardPost</code>
	 * @param <code>AEInvocationContext</code>
	 * @return <code>DiscussionBoardPost</code>
	 */
	public DiscussionBoardPost savePost(DiscussionBoardPost post, AEInvocationContext invContext) throws AEException;

	public DiscussionBoardSubject loadPosts(DiscussionBoardSubject subject) throws AEException;

	public DiscussionBoardTask saveTask(DiscussionBoardTask task) throws AEException;

	public DiscussionBoardTasksList loadTasksByTopicId(long topicId, AEInvocationContext invContext) throws AEException;

	public DiscussionBoardTasksList saveTopicTasks(DiscussionBoardTasksList tList, AEInvocationContext invContext) throws AEException;

	public DiscussionBoardTasksList filterTasks(long ownerId, TaskFilter filter, AEInvocationContext invContext) throws AEException;
}
