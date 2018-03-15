package eu.agileeng.domain.contact;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.imp.AEDescriptorImp;

public class PersonDescr extends AEDescriptorImp {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4321182831889841359L;

	private Person.SalutationType salutation = Person.SalutationType.NA;
	
	private String position;

	public PersonDescr(long id) {
		super(id, DomainClass.PERSON);
	}
	
	public PersonDescr() {
		this(-1);
	}
	
	public Person.SalutationType getSalutation() {
		return salutation;
	}

	public void setSalutation(Person.SalutationType salutation) {
		this.salutation = salutation;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);

		// salutationID
		if(jsonObject.has(Person.JSONKey.salutationID)) {
			this.setSalutation(
					Person.SalutationType.valueOf(jsonObject.optLong(Person.JSONKey.salutationID)));
		}
		
		// position
		if(jsonObject.has(Person.JSONKey.position)) {
			this.setPosition(jsonObject.optString(Person.JSONKey.position));
		}

		// name
		if(jsonObject.has(Person.JSONKey.name)) {
			this.setName(jsonObject.optString(Person.JSONKey.name));
		}
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		if(getSalutation() != null) {
			json.put(Person.JSONKey.salutationID, getSalutation().getTypeID());
		}

		json.put(Person.JSONKey.name, getName());

		json.put(Person.JSONKey.position, getPosition());
		
		return json;
	}
}
