/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 18.11.2009 16:23:16
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
public class AddressesList extends ArrayList<Address> implements AEList {

	/**
	 * 
	 */
	public AddressesList() {
	}

	public AddressesList(Address address) {
		this(1);
		add(address);
	}
	
	/**
	 * @param arg0
	 */
	public AddressesList(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * @param arg0
	 */
	public AddressesList(Collection<? extends Address> c) {
		super(c);
	}

	public Address getAddress(Address.Type addressType) {
		Address address = null;
		for (Address nextAddress: this) {
			if(nextAddress.getType() != null && nextAddress.getType().equals(addressType)) {
				address = nextAddress;
				break;
			}
		}
		return address;
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
