/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 20.06.2010 15:18:06
 */
package eu.agileeng.domain.business.bank;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.contact.Address;
import eu.agileeng.domain.contact.Contact;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.util.AEStringUtil;

/**
 * 
 */
@SuppressWarnings("serial")
public class BankAccount extends AEDomainObject {

	static public enum JSONKey {
		accId,
		journalCode;
	}
	
	public static final int ENTRY_TYPE_MANUAL = 10;
	
	public static final int ENTRY_TYPE_ETEBAC = 20;
	
	private AEDescriptive bank;
	
	private AEDescriptive account;
	
	private AEDescriptive accJournal;
	
	private String rib;
	
	private String iban;
	
	private Address address;
	
	private Contact contact;
	
	private int entryType = ENTRY_TYPE_MANUAL;
	
	private String etebac;
	
	/**
	 * @param clazz
	 */
	public BankAccount() {
		super(DomainClass.BankAccount);
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		if(!AEStringUtil.isEmpty(getIban())) {
			json.put("iban", getIban());
		}
		if(!AEStringUtil.isEmpty(getRib())) {
			json.put("rib", getRib());
		}
		if(getBank() != null) {
			json.put("bankName", getBank().getDescriptor().getName());
		}
		if(getAccount() != null) {
			json.put("accId", getAccount().getDescriptor().getID());
			json.put("accCode", getAccount().getDescriptor().getCode());
		}
		if(getAccJournal() != null) {
			json.put("journalId", getAccJournal().getDescriptor().getID());
			json.put("journalCode", getAccJournal().getDescriptor().getCode());
		}
		if(getAddress() != null) {
			json.put("address", getAddress().getDescription());
		}
		if(getContact() != null) {
			json.put("contactName", getContact().getName());
			json.put("contactPhone", getContact().getPhone());
			json.put("contactEMail", getContact().geteMail());
			json.put("webSite", getContact().getHomepage());
		}
		if(!AEStringUtil.isEmpty(getEtebac())) {
			json.put("etebac", getEtebac());
		}
		json.put("entryType", getEntryType());
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		setIban(jsonObject.optString("iban"));
		setRib(jsonObject.optString("rib"));
		if(jsonObject.has("bankName")) {
			AEDescriptorImp bankDescr = (AEDescriptorImp) Organization.lazyDescriptor(AEPersistentUtil.NEW_ID);
			bankDescr.setName(jsonObject.optString("bankName"));
			this.bank = bankDescr;
		}
		if(jsonObject.has("accId") && jsonObject.optLong("accId") > 0) {
			AEDescriptorImp accDescr = new AEDescriptorImp();
			accDescr.setID(jsonObject.optLong("accId"));
			setAccount(accDescr);
		}
		if(jsonObject.has("journalCode")) {
			AEDescriptorImp journalDescr = new AEDescriptorImp();
			journalDescr.setCode(jsonObject.optString("journalCode"));
			setAccJournal(journalDescr);
		}
		if(jsonObject.has("address")) {
			Address address = new Address();
			address.setDescription(jsonObject.optString("address"));
			setAddress(address);
		}
		Contact contact = new Contact();
		contact.setName(jsonObject.optString("contactName"));
		contact.setPhone(jsonObject.optString("contactPhone"));
		contact.seteMail(jsonObject.optString("contactEMail"));
		contact.setHomepage(jsonObject.optString("webSite"));
		setContact(contact);
		
		setEntryType(jsonObject.optInt("entryType"));
		setEtebac(jsonObject.optString("etebac"));
	}

	public AEDescriptive getBank() {
		return bank;
	}

	public void setBank(AEDescriptive bank) {
		this.bank = bank;
	}

	public AEDescriptive getAccount() {
		return account;
	}

	public void setAccount(AEDescriptive account) {
		this.account = account;
	}

	public AEDescriptive getAccJournal() {
		return accJournal;
	}

	public void setAccJournal(AEDescriptive accJournal) {
		this.accJournal = accJournal;
	}

	public String getRib() {
		return rib;
	}

	public void setRib(String rib) {
		this.rib = rib;
	}

	public String getIban() {
		return iban;
	}

	public void setIban(String iban) {
		this.iban = iban;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public Contact getContact() {
		return contact;
	}

	public void setContact(Contact contact) {
		this.contact = contact;
	}

	public String getEtebac() {
		return etebac;
	}

	public void setEtebac(String etebac) {
		this.etebac = etebac;
	}
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.BankAccount);
	}

	public int getEntryType() {
		return entryType;
	}

	public void setEntryType(int entryType) {
		this.entryType = entryType;
	}
}
