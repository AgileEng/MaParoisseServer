package eu.agileeng.domain.bookmark;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.imp.AEDescriptorImp;

public class AEBookmark extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static public class JSONKey {
		public static final String bookmarks = "bookmarks";
		
		public static final String group = "group";
		public static final String url = "url";
		public static final String tags = "tags";
	}
	
	private String group;
	
	private String url;
	
	private String tags;
	
	public AEBookmark() {
		super(DomainClass.AEBookmark);
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// group
		setGroup(jsonObject.optString(AEBookmark.JSONKey.group));
		
		// url
		setUrl(jsonObject.optString(AEBookmark.JSONKey.url));
		
		// tags
		setTags(jsonObject.optString(AEBookmark.JSONKey.tags));
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		// group
		json.put(AEBookmark.JSONKey.group, getGroup());
		
		// url
		json.put(AEBookmark.JSONKey.url, getUrl());
		
		// tags
		json.put(AEBookmark.JSONKey.tags, getTags());
		
		return json;
	}
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.AEBookmark);
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}
}
