/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 18.11.2009 14:53:36
 */
package eu.agileeng.domain.avi;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.contact.OpportunityPhase;
import eu.agileeng.domain.contact.OpportunityStatus;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.contact.Person;

/**
 *
 */
@SuppressWarnings("serial")
public class Opportunity extends AEDomainObject {

	// keep in the name the name of this Opportunity
	
	// the customer of this Opportunity
	private Organization customer;
	
	private Person responsiblePerson;
	
	private OpportunityPhase phase;
	
	private OpportunityStatus status;
	
	private Task task = new Task();
	
	/**
	 * @param clazz
	 */
	public Opportunity() {
		super(DomainClass.OPPORTUNITY);
	}

	/**
	 * @return the customer
	 */
	public Organization getCustomer() {
		return customer;
	}

	/**
	 * @param customer the customer to set
	 */
	public void setCustomer(Organization customer) {
		this.customer = customer;
	}

	/**
	 * @return the phase
	 */
	public OpportunityPhase getPhase() {
		return phase;
	}

	/**
	 * @param phase the phase to set
	 */
	public void setPhase(OpportunityPhase phase) {
		this.phase = phase;
	}

	/**
	 * @return the responsiblePerson
	 */
	public Person getResponsiblePerson() {
		return responsiblePerson;
	}

	/**
	 * @param responsiblePerson the responsiblePerson to set
	 */
	public void setResponsiblePerson(Person responsiblePerson) {
		this.responsiblePerson = responsiblePerson;
	}

	/**
	 * @return the task
	 */
	public Task getTask() {
		return task;
	}

	/**
	 * @param task the task to set
	 */
	public void setTask(Task task) {
		this.task = task;
	}

	/**
	 * @return the status
	 */
	public OpportunityStatus getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(OpportunityStatus status) {
		this.status = status;
	}
}
