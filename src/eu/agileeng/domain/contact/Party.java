/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 18.11.2009 15:54:49
 */
package eu.agileeng.domain.contact;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;

/**
 * Take a look through your address book, and what do you see? If it's anything like
 * mine, you will see a lot of addresses, telephone numbers, the odd e-mail address...
 * all linked to something. Often that something is a person, but once in awhile a
 * company shows up. Instinctively I look for a generalization of person and company. 
 * This type is a classic case of an unnamed concept—one that everybody knows and uses but
 * nobody has a name for. I have seen it on countless data models on various names:
 * person/organization, player, legal entity, and so on.
 * This model shows the similar responsibilities of person and organization.
 * The term I prefer is party. So I define a party as the supertype of a
 * person or organization. This allows me to have addresses and phone numbers for
 * departments within companies, or even informal teams.
 * Party should be used in many situations where person or organization is used.
 * It is surprising how many things relate to party rather than to person or
 * organization. You receive and send letters to both people and organizational
 * units; I make payments to people and organizations; both organizations and
 * people carry out actions, have bank accounts, file taxes. These examples are
 * enough, I think, to make the abstraction worthwhile.
 */
@SuppressWarnings("serial")
public abstract class Party extends AEDomainObject {

	private boolean hasUIN = true;
	
	/**
	 * Unique identification number.
	 * EIN in case of organization,
	 * EGN in case of person
	 */
	private String UIN;
	
	/**
	 * initialize here to be independent of multiple constructors
	 */
	private AddressesList addressesList = new AddressesList();
	
	/**
	 * initialize here to be independent of multiple constructors
	 */
	private ContactsList contactsList = new ContactsList();
	
	private AEDescriptive parent;
	
	private boolean template;
	
	private boolean active;
	
	/**
	 * @param clazz
	 */
	protected Party(DomainClass clazz) {
		super(clazz);
	}

	/**
	 * @return the addressesList
	 */
	public AddressesList getAddressesList() {
		return addressesList;
	}
	
	/**
	 * @return the addressesList
	 */
	public AddressesList grantAddressesList() {
		if(addressesList == null) {
			addressesList = new AddressesList();
		}
		return addressesList;
	}

	/**
	 * @param addressesList the addressesList to set
	 */
	public void setAddressesList(AddressesList addressesList) {
		this.addressesList = addressesList;
	}

	/**
	 * @return the contactsList
	 */
	public ContactsList getContactsList() {
		return contactsList;
	}
	
	/**
	 * @return the contactsList
	 */
	public ContactsList grantContactsList() {
		if(contactsList == null) {
			contactsList = new ContactsList();
		}
		return contactsList;
	}

	/**
	 * @param contactsList the contactsList to set
	 */
	public void setContactsList(ContactsList contactsList) {
		this.contactsList = contactsList;
	}

	/**
	 * @return the uIN
	 */
	public String getUIN() {
		return UIN;
	}

	/**
	 * @param uIN the uIN to set
	 */
	public void setUIN(String uIN) {
		UIN = uIN;
	}
	
	public Contact getDefaultContact() {
		return contactsList != null ? contactsList.getContact(Contact.Type.BUSINESS) : null;
	}
	
	public Address getDefaultAddress() {
		return addressesList != null ? addressesList.getAddress(Address.Type.BUSINESS) : null;
	}

	/**
	 * @return the parent
	 */
	public AEDescriptive getParent() {
		return parent;
	}

	/**
	 * @param parent the parent to set
	 */
	public void setParent(AEDescriptive parent) {
		this.parent = parent;
	}
	
	public Party getParentFull() {
		Party party = null;
		if(getParent() instanceof Party) {
			party = (Party) getParent();
		}
		return party;
	}

	public boolean isTemplate() {
		return template;
	}

	public void setTemplate(boolean template) {
		this.template = template;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isHasUIN() {
		return hasUIN;
	}

	public void setHasUIN(boolean hasUIN) {
		this.hasUIN = hasUIN;
	}
}
