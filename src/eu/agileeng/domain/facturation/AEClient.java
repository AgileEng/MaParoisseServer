package eu.agileeng.domain.facturation;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.business.AEPaymentTerms;
import eu.agileeng.domain.contact.Address;
import eu.agileeng.domain.contact.Contact;
import eu.agileeng.domain.contact.Person;
import eu.agileeng.domain.contact.PersonDescr;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.util.json.JSONUtil;

public class AEClient extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5695147641413483118L;
	
	static public class JSONKey {
		public static final String client = "client";
		public static final String clients = "clients";
		
		public static final String familleId = "familleId";
		public static final String familleName = "familleName";
		
		public static final String vatNumber = "vatNumber";
		public static final String paymentTermsId = "paymentTermsId";
		public static final String paymentTermsDescr = "paymentTermsDescr";
		public static final String discountRate = "discountRate";
		
		public static final String accountId = "accountId";
		public static final String accountAuxiliary = "accountAuxiliary";
	}
	
	private long codeNumber;
	
	private AEDescriptive group; // Famille
	
	private Address address;
	
	private Contact contact;
	
	private String vatNumber;
	
	private AEDescriptive account; // Compte Comptable
	
	private String accountAuxiliary; // Auxiliary account
	
	private AEDescriptive paymentTerms; // Mode de reglement
	
	private double discountRate; // Taux de remise
	
	private PersonDescr responsiblePerson;
	
	public AEClient() {
		super(DomainClass.AEClient);
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// famille
		AEDescriptorImp group = new AEDescriptorImp();
		group.setID(jsonObject.optLong(JSONKey.familleId));
		group.setName(jsonObject.optString(JSONKey.familleName));
		setGroup(group);
		
		// contact
		if(jsonObject.has(Contact.key_contact)) {
			JSONObject contactJSON = jsonObject.optJSONObject(Contact.key_contact);
			if(contactJSON != null) {
				this.grantContact().create(contactJSON);
			}
		}
		
		// address
		if(jsonObject.has(Address.key_address)) {
			JSONObject addressJSON = jsonObject.optJSONObject(Address.key_address);
			if(addressJSON != null) {
				this.grantAddress().create(addressJSON);
			}
		}

	   	// vatNumber
		setVatNumber(jsonObject.optString(JSONKey.vatNumber));
		
		// paymentTermsId
		long paymentTermsId = jsonObject.optLong(JSONKey.paymentTermsId);
		if(paymentTermsId > 0) {
			setPaymentTerms(AEPaymentTerms.lazyDescriptor(paymentTermsId));
		}
		
		// discount rate
		try {
			setDiscountRate(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.discountRate));
		} catch(Exception e) {
		}
	   	
		// accountId
		long accountId = jsonObject.optLong(JSONKey.accountId);
		if(accountId > 0) {
			setAccount(AccAccount.lazyDescriptor(accountId));
		}
		
		// accountAuxiliary
		setAccountAuxiliary(jsonObject.optString(JSONKey.accountAuxiliary));
		
		// person
		if(jsonObject.has(Person.JSONKey.person)) {
			JSONObject personJSON = jsonObject.optJSONObject(Person.JSONKey.person);
			if(personJSON != null) {
				this.grantResponsiblePerson().create(personJSON);
			}
		}
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		// famille
		if(getGroup() != null) {
			json.put(JSONKey.familleId, getGroup().getDescriptor().getID());
			json.put(JSONKey.familleName, getGroup().getDescriptor().getName());
		}
		
		// contact
		json.put(Contact.key_contact, grantContact().toJSONObject());
		
		// address
		json.put(Address.key_address, grantAddress().toJSONObject());

	   	// vatNumber
		json.put(JSONKey.vatNumber, getVatNumber());
		
		// paymentTermsId
		if(getPaymentTerms() != null) {
			json.put(JSONKey.paymentTermsId, getPaymentTerms().getDescriptor().getID());
		}
		
		// paymentTermsDescr
		if(getPaymentTerms() != null) {
			json.put(JSONKey.paymentTermsDescr, getPaymentTerms().getDescriptor().getDescription());
		}
		
		// discount rate
		json.put(JSONKey.discountRate, getDiscountRate());
	   	
		// accountId
		if(getAccount() != null) {
			json.put(JSONKey.accountId, getAccount().getDescriptor().getID());
		}
		
		// accountAuxiliary
		json.put(JSONKey.accountAuxiliary, getAccountAuxiliary());
		
		// person
		json.put(Person.JSONKey.person, grantResponsiblePerson().toJSONObject());
		
		return json;
	}
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.AEClient);
	}

	public AEDescriptive getGroup() {
		return group;
	}

	public void setGroup(AEDescriptive group) {
		this.group = group;
	}

	public Address getAddress() {
		return address;
	}

	public Address grantAddress() {
		if(address == null) {
			address = new Address(Address.Type.BUSINESS);
		}
		return address;
	}
	
	public void setAddress(Address address) {
		this.address = address;
	}

	public Contact getContact() {
		return contact;
	}

	public Contact grantContact() {
		if(contact == null) {
			contact = new Contact(Contact.Type.BUSINESS);
		}
		return contact;
	}
	
	public void setContact(Contact contact) {
		this.contact = contact;
	}

	public String getVatNumber() {
		return vatNumber;
	}

	public void setVatNumber(String vatNumber) {
		this.vatNumber = vatNumber;
	}

	public AEDescriptive getAccount() {
		return account;
	}

	public void setAccount(AEDescriptive account) {
		this.account = account;
	}

	public AEDescriptive getPaymentTerms() {
		return paymentTerms;
	}

	public void setPaymentTerms(AEDescriptive paymentTerms) {
		this.paymentTerms = paymentTerms;
	}

	public PersonDescr getResponsiblePerson() {
		return responsiblePerson;
	}
	
	public PersonDescr grantResponsiblePerson() {
		if(responsiblePerson == null) {
			responsiblePerson = new PersonDescr();
		}
		return responsiblePerson;
	}

	public void setResponsiblePerson(PersonDescr responsiblePerson) {
		this.responsiblePerson = responsiblePerson;
	}

	public double getDiscountRate() {
		return discountRate;
	}

	public void setDiscountRate(double discountRate) {
		this.discountRate = discountRate;
	}

	public String getAccountAuxiliary() {
		return accountAuxiliary;
	}

	public void setAccountAuxiliary(String accountAuxiliary) {
		this.accountAuxiliary = accountAuxiliary;
	}

	public long getCodeNumber() {
		return codeNumber;
	}

	public void setCodeNumber(long codeNumber) {
		this.codeNumber = codeNumber;
	}
}
