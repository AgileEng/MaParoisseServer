/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 21.11.2009 10:16:14
 */
package eu.agileeng.domain;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;

import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.util.AEPredicate;


/**
 *
 */
@SuppressWarnings("serial")
public class EnumeratedTypesList extends ArrayList<EnumeratedType> implements AEList {

	/**
	 * 
	 */
	public EnumeratedTypesList() {
	}

	/**
	 * @param initialCapacity
	 */
	public EnumeratedTypesList(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * @param c
	 */
	public EnumeratedTypesList(Collection<? extends EnumeratedType> c) {
		super(c);
	}

	public EnumeratedTypesList filter(AEPredicate predicate) {
		EnumeratedTypesList enumList = new EnumeratedTypesList();
		for (EnumeratedType et : this) {
			if(predicate.evaluate(et)) {
				enumList.add(et);
			}		
		}
		return enumList;
	}
	
	public EnumeratedTypesList filter(DomainClass clazz) {
		return filter(new EnumTypeClassPredicate(clazz));
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
