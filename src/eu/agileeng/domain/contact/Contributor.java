package eu.agileeng.domain.contact;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEPersistent;
import eu.agileeng.util.AECollectionUtil;
import eu.agileeng.util.AEStringUtil;

public class Contributor extends AEDomainObject {

	static public enum JSONKey {
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 2348975334321447317L;

	private Employee employee;

	private boolean checked;

	public Contributor() {
		super(DomainClass.Contributor);
	}

	protected Contributor(DomainClass dc) {
		super(dc);
	}

	public Employee getEmployee() {
		return employee;
	}

	public void setEmployee(Employee employee) {
		this.employee = employee;
	}

	@Override
	public void create(JSONObject json) throws JSONException {
		super.create(json);

		//employee
		Employee emp = new Employee();
		emp.create(json.getJSONObject(Employee.JSONKey.employee));
		this.setEmployee(emp);
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();

		if(this.getEmployee() != null) {
			json.put(Employee.JSONKey.employee, this.getEmployee().toJSONObject());
		}

		if(this.checked) {
			json.put("checked", this.checked);
		}

		return json;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEObject#setCompany(eu.agileeng.domain.AEDescriptive)
	 */
	@Override
	public void setCompany(AEDescriptive company) {
		super.setCompany(company);
		if(this.employee != null) {
			this.employee.setCompany(company);
		}
	}

	@Override
	public void setUpdated() {
		super.setUpdated();

		// propagate to employee
		Employee empl = this.getEmployee();
		if(empl != null) {
			// employee
			if(AEPersistent.ID.isPersistent(empl.getID())) {
				empl.setPersistentState(State.UPDATED);
			}

			// contacts
			ContactsList contacts = empl.getContactsList();
			if(!AECollectionUtil.isEmpty(contacts)) {
				for (Contact contact : contacts) {
					if(AEPersistent.ID.isPersistent(contact.getID())) {
						contact.setPersistentState(State.UPDATED);
					}
				}
			}

			// addresses
			AddressesList addresses = empl.getAddressesList();
			if(!AECollectionUtil.isEmpty(addresses)) {
				for (Address address : addresses) {
					if(AEPersistent.ID.isPersistent(address.getID())) {
						address.setPersistentState(State.UPDATED);
					}
				}
			}
		}
	}

	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.Contributor);
	}

	/**
	 * @return the checked
	 */
	public boolean isChecked() {
		return checked;
	}

	/**
	 * @param checked the checked to set
	 */
	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	/**
	 * Unrecognized Contributor must be also invalid one
	 * so perstistent operations with an unrecognized contributor are not allowed.
	 * 
	 * @return <code>true</code> if firstName, lastName, street, city and postalCode are all (null or empty) strings
	 * 
	 * @throws AEException in case of invalid internal state
	 */
	public boolean isUnrecognized() throws AEException {
		boolean ret = false;

		Employee empl = getEmployee();
		if(empl == null) {
			throw AEError.System.INVALID_REQUEST.toException();
		}

		String fName = empl.getFirstName();
		String lName = empl.getLastName();

		if(AEStringUtil.isEmpty(fName) && AEStringUtil.isEmpty(lName)) {
			// unrecognized contributor
			ret = true;
		}

		return ret;
	}

	/**
	 * Persistent operations with a Contributor must be allowed only if he is a valid one.
	 * 
	 * @return <code>true</code> 
	 * If and only if firstName, lastName, street, city and postalCode are not null and not empty strings
	 * 
	 * @throws AEException in case of invalid internal state
	 */
	public boolean isValid() throws AEException {
		boolean ret = true;

		Employee empl = getEmployee();
		if(empl == null) {
			throw AEError.System.INVALID_REQUEST.toException();
		}

		String fName = empl.getFirstName();
		String lName = empl.getLastName();

		Address addr =  empl.getAddress();
		if(addr == null) {
			throw AEError.System.INVALID_REQUEST.toException();
		}
		String street = addr.getStreet();
		String postalCode = addr.getPostalCode() ;
		String city = addr.getCity();

		if(AEStringUtil.isEmptyTrimed(fName)
				|| AEStringUtil.isEmptyTrimed(lName)
				|| AEStringUtil.isEmptyTrimed(street)
				|| AEStringUtil.isEmptyTrimed(postalCode)
				|| AEStringUtil.isEmptyTrimed(city)) {

			// not valid
			ret = false;
		}
		return ret;
	}
	
	protected void createDescriptor(AEDescriptor aeDescr) {
		aeDescr.setCode(getCode());
		if(getEmployee() != null) {
			aeDescr.setName(getEmployee().getName());
		} else {
			aeDescr.setName(getName());
		}
		
		aeDescr.setDescription(getDescription());
	}
}
