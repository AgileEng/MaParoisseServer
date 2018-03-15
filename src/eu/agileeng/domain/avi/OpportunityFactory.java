/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 26.11.2009 17:26:45
 */
package eu.agileeng.domain.avi;

import eu.agileeng.domain.contact.Address;
import eu.agileeng.domain.contact.Contact;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.contact.Person;

/**
 *
 */
public class OpportunityFactory {

	private static OpportunityFactory inst = new OpportunityFactory();
	
	/**
	 * 
	 */
	private OpportunityFactory() {
	}

	public static OpportunityFactory getInstance() {
		return inst;
	}
	
	public Opportunity createContact() {
		Opportunity opportunity = new Opportunity();
		
		// customer
		Organization customer = new Organization();
		customer.grantAddressesList().add(new Address(Address.Type.BUSINESS));
		customer.grantContactsList().add(new Contact(Contact.Type.BUSINESS));
		opportunity.setCustomer(customer);
		
		// person
		Person respPerson = new Person();
		respPerson.grantAddressesList().add(new Address(Address.Type.BUSINESS));
		respPerson.grantContactsList().add(new Contact(Contact.Type.BUSINESS));
		opportunity.setResponsiblePerson(respPerson);
		
		// TODO
		
		return opportunity;
	}
}
