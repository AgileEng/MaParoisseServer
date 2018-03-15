/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 20.11.2009 15:48:17
 */
package eu.agileeng.domain.avi;

import java.util.Date;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.contact.TaskCategory;


/**
 *
 */
@SuppressWarnings("serial")
public class Task extends AEDomainObject {

	private TaskCategory taskCategory;
	
	private Date timeNextActivity;
	
	/**
	 * @param clazz
	 */
	public Task() {
		super(DomainClass.TASK);
	}

	/**
	 * @return the taskCategory
	 */
	public TaskCategory getTaskCategory() {
		return taskCategory;
	}

	/**
	 * @param taskCategory the taskCategory to set
	 */
	public void setTaskCategory(TaskCategory taskCategory) {
		this.taskCategory = taskCategory;
	}

	/**
	 * @return the timeNextActivity
	 */
	public Date getTimeNextActivity() {
		return timeNextActivity;
	}

	/**
	 * @param timeNextActivity the timeNextActivity to set
	 */
	public void setTimeNextActivity(Date timeNextActivity) {
		this.timeNextActivity = timeNextActivity;
	}
}
