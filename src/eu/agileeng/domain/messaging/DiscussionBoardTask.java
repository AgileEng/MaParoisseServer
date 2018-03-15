package eu.agileeng.domain.messaging;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;


public class DiscussionBoardTask extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2453999451842135915L;
	
	// params
	private long type;
	private long priority;
	private long assignedTo;
	private Date devDueDate;
	private Date validDueDate;
	private long state;
	private Date releasedDate;
	private long topicId;
	private long lastPostRef;
	private String hashtag;
	private double progress;
	private DiscussionBoardTaskDecisionsList decisions;

	public DiscussionBoardTask() {
		super(DomainClass.DiscussionBoardTask);
	}
	
	public static enum Type {
		FEATURE(20L),
		ENHANCEMENT(30L),
		BUG(10L);
		
		private long id;

		private Type(long id) {
			this.id = id;
		}

		public final long getID() {
			return this.id;
		}

		public final static Type valueOf(long id) {
			Type foundType = null;
			for (Type type : Type.values()) {
				if(type.getID() == id) {
					foundType = type;
					break;
				}
			}
			return foundType;
		}
	}
	
	public static enum State {
		DISCUSSION(10L),
		PLANNING(20L),
		APPROVEMENT(30L),
		DEVELOPMENT(40L),
		TESTING(50L),
		PRODUCTION(60L),
		EVALUATION(70L);
		
		private long id;
		
		private State(long id) {
			this.id = id;
		}
		
		public final long getID() {
			return this.id;
		}
		
		public static final State valueOf(long id) {
			State foundState = null;
			for (State value : State.values()) {
				if (value.getID() == id) {
					foundState = value;
					break;
				}
			}
			return foundState;
		}
	}
	
	public static enum Priority {
		LOW(4L),
		MEDIUM(3L),
		HIGH(2L),
		URGENT(1L);

		
		private long id;
		
		private Priority(long id) {
			this.id = id;
		}
		
		public final long getID() {
			return this.id;
		}
		
		public static final Priority valueOf(long id) {
			Priority foundState = null;
			for (Priority value : Priority.values()) {
				if (value.getID() == id) {
					foundState = value;
					break;
				}
			}
			return foundState;
		}
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		json.put("type", this.getType());
		json.put("priority", this.getPriority());
		json.put("assignedTo", this.getAssignedTo());
		json.put("devDueDate", this.getDevDueDate());
		json.put("valDueDate", this.getValidDueDate());
		json.put("relDate", this.getReleasedDate());
		json.put("state", this.getState());
		json.put("topicId", this.getTopicId());
		json.put("lastPostRef", this.getLastPostRef());
		json.put("hashtag", this.getHashtag());
		json.put("progress", this.getProgress());
		
		//append dynamic columns to json
		if (this.getDecisions() == null) this.setDecisions(new DiscussionBoardTaskDecisionsList());
		
		this.getDecisions().appendToJSON(json);
		
		return json;
	}
	
	@Override
	public void create(JSONObject json) throws JSONException {
		super.create(json);
		
		this.setType(json.optLong("type"));
		this.setPriority(json.optLong("priority"));
		this.setAssignedTo(json.optLong("assignedTo"));
		this.setState(json.optLong("state"));
		this.setTopicId(json.optLong("topicId"));
		this.setLastPostRef(json.optLong("lastPostRef"));
		this.setHashtag(json.optString("hashtag"));
		this.setProgress(json.optDouble("progress"));
		try {
			if(json.optString("devDueDate").length() > 0){
				this.setDevDueDate(parseDateString(json.optString("devDueDate")));
			}
			//this.setReleasedDate(parseDateString(json.optString("relDate")));
			if(json.optString("valDueDate").length() > 0){
				this.setValidDueDate(parseDateString(json.optString("valDueDate")));
			}
			
		} catch (AEException e) {
			e.printStackTrace();
		}
		//this.setDevDueDate();
		
		//TODO read of dynamic planners/approvers
	}
	
	public JSONArray genDynamicColumnsModel() throws JSONException {
		JSONArray jsonArray = new JSONArray();
		
		if (this.getDecisions() == null) this.setDecisions(new DiscussionBoardTaskDecisionsList());
		
		for (DiscussionBoardTaskDecision d : this.getDecisions()) {
			JSONObject json = new JSONObject();
			
			json.put("header", d.getAuthName());
			json.put("dataIndex", d.genDataIndex());
			
			jsonArray.put(json);
		}
		
		return jsonArray;
	}
	
	public JSONArray genDynamicFieldsModel() throws JSONException {
		JSONArray jsonArray = new JSONArray();
		
		for (DiscussionBoardTaskDecision d : this.getDecisions()) {
			JSONObject json = new JSONObject();
			
			json.put("name", d.genDataIndex());
			json.put("type", "int");
			
			jsonArray.put(json);
		}
		
		return jsonArray;
	}
	
	public Date parseDateString(String dateStr) throws AEException {
		DateFormat df = new SimpleDateFormat("dd/MM/yy");//"EEE MMM dd HH:mm:ss z yyyy");
		
	    try {
			Date result =  df.parse(dateStr);
			
			return result;
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			throw new AEException(0, "Cannot parse date", e);
		}
	}

	public long getType() {
		return type;
	}

	public void setType(long type) {
		this.type = type;
	}

	public long getPriority() {
		return priority;
	}

	public void setPriority(long priority) {
		this.priority = priority;
	}

	public long getAssignedTo() {
		return assignedTo;
	}

	public void setAssignedTo(long assignedTo) {
		this.assignedTo = assignedTo;
	}

	public Date getDevDueDate() {
		return devDueDate;
	}

	public void setDevDueDate(Date devDueDate) {
		this.devDueDate = devDueDate;
	}

	public Date getValidDueDate() {
		return validDueDate;
	}

	public void setValidDueDate(Date validDueDate) {
		this.validDueDate = validDueDate;
	}

	public long getState() {
		return state;
	}

	public void setState(long state) {
		this.state = state;
	}

	public Date getReleasedDate() {
		return releasedDate;
	}

	public void setReleasedDate(Date releasedDate) {
		this.releasedDate = releasedDate;
	}

	public long getTopicId() {
		return topicId;
	}

	public void setTopicId(long topicId) {
		this.topicId = topicId;
	}

	public long getLastPostRef() {
		return lastPostRef;
	}

	public void setLastPostRef(long lastPostRef) {
		this.lastPostRef = lastPostRef;
	}

	public String getHashtag() {
		return hashtag;
	}

	public void setHashtag(String hashtag) {
		this.hashtag = hashtag;
	}

	public double getProgress() {
		return progress;
	}

	public void setProgress(double progress) {
		this.progress = progress;
	}

	public DiscussionBoardTaskDecisionsList getDecisions() {
		return decisions;
	}

	public void setDecisions(DiscussionBoardTaskDecisionsList decisions) {
		this.decisions = decisions;
	}
	
	

}
