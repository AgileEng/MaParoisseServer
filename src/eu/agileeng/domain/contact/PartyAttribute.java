/**
 * 
 */
package eu.agileeng.domain.contact;

import java.io.Serializable;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.imp.AEDescriptorImp;

/**
 * @author vvatov
 *
 */
public class PartyAttribute extends AEDomainObject {

	static public enum Nature implements Serializable {
		VAT_ACCOUNT
	}
	
	static public enum XType implements Serializable {
		INTEGER,
		LONG,
		DOUBLE,
		STRING,
		DATE,
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1384267175080991931L;

	private AEDescriptive party;
	
	private Nature nature;
	
	private AEDescriptive referTo;
	
	private PartyAttribute.XType xType;
	
	private Object value;
	
	/**
	 * @param clazz
	 */
	public PartyAttribute() {
		super(DomainClass.PartyAttribute);
	}

	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.PartyAttribute);
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();

		// TODO
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// TODO
	}

	public AEDescriptive getParty() {
		return party;
	}

	public void setParty(AEDescriptive party) {
		this.party = party;
	}

	public Nature getNature() {
		return nature;
	}

	public void setNature(Nature nature) {
		this.nature = nature;
	}

	public AEDescriptive getReferTo() {
		return referTo;
	}

	public void setReferTo(AEDescriptive referTo) {
		this.referTo = referTo;
	}

	public PartyAttribute.XType getxType() {
		return xType;
	}

	public void setxType(PartyAttribute.XType xType) {
		this.xType = xType;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}
}
