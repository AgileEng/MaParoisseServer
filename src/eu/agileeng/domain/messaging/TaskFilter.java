package eu.agileeng.domain.messaging;

import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.messaging.DiscussionBoardTask.Priority;
import eu.agileeng.util.AEDateUtil;

public class TaskFilter extends AEDomainObject {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5323077918245241734L;
	
	//params
	private Date fromDevDueDate;
	private Date toDevDueDate;
	private long priority;
	
	
	public TaskFilter() {
		super(DomainClass.TaskFilter);
		
	}
	
	@Override
	public void create(JSONObject json) throws JSONException {
		super.create(json);
		
		this.setFromDevDueDate(AEDateUtil.parseDate(json.optString("fromDevDueDate")));
		this.setToDevDueDate(AEDateUtil.parseDate(json.optString("toDevDueDate")));
		this.setPriority(json.optLong("priority"));
		
	}

	public Date getFromDevDueDate() {
		return fromDevDueDate;
	}

	public void setFromDevDueDate(Date fromDevDueDate) {
		this.fromDevDueDate = fromDevDueDate;
	}

	public Date getToDevDueDate() {
		return toDevDueDate;
	}

	public void setToDevDueDate(Date toDevDueDate) {
		this.toDevDueDate = toDevDueDate;
	}

	public long getPriority() {
		return priority;
	}

	public void setPriority(long priority) {
		this.priority = priority;
	}


}
