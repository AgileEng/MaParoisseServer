/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 14.06.2010 15:04:45
 */
package eu.agileeng.domain.file;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEList;

/**
 *
 */
@SuppressWarnings("serial")
public class FileAttachmentsList extends ArrayList<FileAttachment> implements AEList {

	/**
	 * 
	 */
	public FileAttachmentsList() {
	}

	/**
	 * @param initialCapacity
	 */
	public FileAttachmentsList(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * @param c
	 */
	public FileAttachmentsList(Collection<? extends FileAttachment> c) {
		super(c);
	}

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (FileAttachment item : this) {
				jsonArray.put(item.toJSONObject());
			}
		}
		
		return jsonArray;
	}
	
	public JSONArray toJSONArrayExt() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (FileAttachment item : this) {
				jsonArray.put(item.toJSONObjectExt());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);
			
			FileAttachment fa = new FileAttachment();
			fa.create(jsonItem);
			
			add(fa);
		}
	}
}
