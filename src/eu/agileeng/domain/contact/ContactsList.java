/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 18.11.2009 16:25:42
 */
package eu.agileeng.domain.contact;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;

import eu.agileeng.domain.AEList;


/**
 *
 */
@SuppressWarnings("serial")
public class ContactsList extends ArrayList<Contact> implements AEList {

	/**
	 * 
	 */
	public ContactsList() {
	}

	/**
	 * 
	 */
	public ContactsList(Contact contact) {
		this(1);
		add(contact);
	}
	
	/**
	 * @param initialCapacity
	 */
	public ContactsList(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * @param c
	 */
	public ContactsList(Collection<? extends Contact> c) {
		super(c);
	}
	
	public Contact getContact(Contact.Type contactType) {
		Contact contact = null;
		for (Contact nextContact: this) {
			if(nextContact.getType() != null && nextContact.getType().equals(contactType)) {
				contact = nextContact;
				break;
			}
		}
		return contact;
	}

	@Override
	public JSONArray toJSONArray() throws JSONException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		// TODO Auto-generated method stub
		
	}
}
