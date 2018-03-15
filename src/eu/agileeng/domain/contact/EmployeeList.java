/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 20.11.2009 16:26:47
 */
package eu.agileeng.domain.contact;

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
public class EmployeeList extends ArrayList<Employee> implements AEList {

	/**
	 * 
	 */
	public EmployeeList() {
	}

	/**
	 * @param arg0
	 */
	public EmployeeList(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * @param arg0
	 */
	public EmployeeList(Collection<? extends Employee> c) {
		super(c);
	}
	
	public Employee getEmployeeByPersonID(long id) {
		Employee empl = null;
		for (Employee nextEmployee : this) {
			if(nextEmployee.getPerson() != null && nextEmployee.getPerson().getDescriptor().getID() == id) {
				empl = nextEmployee;
				break;
			}
		}
		return empl;
	}

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (Employee employee : this) {
				jsonArray.put(employee.toJSONObject());
			}
		}

		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);

			Employee employee = new Employee();
			employee.create(jsonItem);

			add(employee);
		}
	}
}
