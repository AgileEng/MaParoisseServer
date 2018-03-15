package eu.agileeng.domain.messaging;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel;
import eu.agileeng.domain.DomainModel.DomainClass;

public class DiscussionBoardPost extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5659121646604230706L;
	
	
	
	private String messageBody;
	
	private long topicId;
	
	private Date dateCreated = new Date(0);
	
	private long authorId;
	
	private String authorName;
	
	/**
	 * Possible values:
	 * 10 - Opening
	 * 20 - Regular
	 * 30 - Closing
	 */
	private long type;
	
	

	public DiscussionBoardPost(DomainClass clazz) {
		super(clazz);
		// TODO Auto-generated constructor stub
	}
	
	public DiscussionBoardPost() {
		super(DomainModel.DomainClass.DiscussionBoardPost);
		
	}
	
	public static enum Type {
		OPENING(10L),
		REOPENING(11L),
		REGULAR(20L),
		CLOSING(30L);
		
		private long id;
		
		private Type(long id) {
			this.id = id;
		}
		
		public final long getID() {
			return this.id;
		}
		
		public static final Type valueOf(long id) {
			Type foundType = null;
			for (Type type : Type.values()) {
				if (type.getID() == id) {
					foundType = type;
					break;
				}
			}
			return foundType;
		}
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		
		json.put("body", getMessageBody());
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		json.put("date", dateFormat.format(getDateCreated()));
		
		json.put("authorId", getAuthorId());
		json.put("authorName", getAuthorName());
		json.put("topicId", getTopicId());
		json.put("type", this.getID());
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		setAuthorId(jsonObject.optLong("authorId"));
		//setDateCreatred(Date.parse(jsonObject.optString("date")));
		setTopicId(jsonObject.optLong("topicId"));
		setMessageBody(jsonObject.optString("body"));
		this.setType(jsonObject.optLong("type"));
	}

	public String getMessageBody() {
		return messageBody;
	}



	public void setMessageBody(String messageBody) {
		this.messageBody = messageBody;
	}



	public Date getDateCreated() {
		return dateCreated;
	}



	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}



	public long getAuthorId() {
		return authorId;
	}



	public void setAuthorId(long authorId) {
		this.authorId = authorId;
	}

	public long getTopicId() {
		return topicId;
	}

	public void setTopicId(long topicId) {
		this.topicId = topicId;
	}

	public String getAuthorName() {
		return authorName;
	}

	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}

	public long getType() {
		return type;
	}

	public void setType(long type) {
		this.type = type;
	}

}
