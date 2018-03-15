package eu.agileeng.domain.messaging;

import java.util.Date;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.util.AEStringUtil;

public class DiscussionBoardSubject extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7441568316846990420L;
	
	
	private long parentId;
	
	private Date dateCreatred = new Date(0);
	
	private long ownerId;
	
	private long type;
	//TODO: posts has to be a list or not exist at all
	private DiscussionBoardPostsList posts;
	
	/**
	 * Possible values:
	 * 10 - Opened
	 * 20 - Closed
	 */
	private long state;

	public DiscussionBoardSubject(DomainClass clazz) {
		super(clazz);
		// TODO Auto-generated constructor stub
	}
	
	public DiscussionBoardSubject() {
		super(DomainModel.DomainClass.DiscussionBoardSubject);
		
	}
	
	public static enum State {
		OPENED(10L),
		CLOSED(20L);
		
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
	
	public static enum Type {
		CATEGORY(10L),
		TOPIC(20L);
		
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
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		
		json.put("companyId", this.getCompany().getDescriptor().getID());
		json.put("parentId", getParentId());
		
		json.put("date", getDateCreatred().toString());
		
		json.put("ownerId", getOwnerId());
		json.put("type", getType());
		json.put("state", this.getState());
		if (getPosts() != null) {
			json.put("posts", getPosts().toJSONArray());
		} else {
			json.put("posts", new JSONArray());
		}
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		
		setParentId(jsonObject.optLong("parentId"));
		this.setCompany(Organization.lazyDescriptor(jsonObject.optLong("companyId")));
		
		//setDateCreatred(Date.parse(jsonObject.optString("date")));
		setOwnerId(jsonObject.optLong("ownerId"));
		setType(jsonObject.optLong("type"));
		this.setState(jsonObject.optLong("state"));
		
		DiscussionBoardPostsList pList = new DiscussionBoardPostsList();
		
		if(jsonObject.has("posts") && !AEStringUtil.isEmpty(jsonObject.optString("posts"))) {
			pList.create(jsonObject.optJSONArray("posts"));
		}
		
		setPosts(pList);
	}

	public long getParentId() {
		return parentId;
	}

	public void setParentId(long parentId) {
		this.parentId = parentId;
	}

	public Date getDateCreatred() {
		return dateCreatred;
	}

	public void setDateCreatred(Date dateCreatred) {
		this.dateCreatred = dateCreatred;
	}

	public long getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(long ownerId) {
		this.ownerId = ownerId;
	}

	public long getType() {
		return type;
	}

	public void setType(long type) {
		this.type = type;
	}

	public DiscussionBoardPostsList getPosts() {
		return posts;
	}

	public void setPosts(DiscussionBoardPostsList posts) {
		this.posts = posts;
	}

	public long getState() {
		return state;
	}

	public void setState(long state) {
		this.state = state;
	}

}
