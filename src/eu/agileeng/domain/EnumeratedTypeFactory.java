/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 10.11.2009 16:16:46
 */
package eu.agileeng.domain;

import eu.agileeng.domain.business.ContractDeTravailReason;
import eu.agileeng.domain.business.DepartmentAGIRC;
import eu.agileeng.domain.business.DepartmentARRCO;
import eu.agileeng.domain.business.EmploymentClassification;
import eu.agileeng.domain.business.TaxTypeEnum;
import eu.agileeng.domain.contact.OpportunityPhase;
import eu.agileeng.domain.contact.OpportunityStatus;
import eu.agileeng.domain.contact.OrganizationEmplNumber;
import eu.agileeng.domain.contact.OrganizationFinanceCapital;
import eu.agileeng.domain.contact.OrganizationIndustry;
import eu.agileeng.domain.contact.TaskCategory;
import eu.agileeng.domain.document.social.contractdetravail.ContractDeTravailAssistence;
import eu.agileeng.domain.document.social.contractdetravail.ContractDeTravailType;

/**
 *
 */
public class EnumeratedTypeFactory {

	private static EnumeratedTypeFactory inst = new EnumeratedTypeFactory();
	
	/**
	 * 
	 */
	EnumeratedTypeFactory() {
	}
	
	public static EnumeratedTypeFactory getInstance() {
		return EnumeratedTypeFactory.inst;
	}

	public final EnumeratedType createEnumeratedType(DomainModel.DomainClass clazz) {
		EnumeratedType enumType = null;
		switch(clazz) {
			case OPGANIZATION_EMPL_NUMBER:
				enumType = new OrganizationEmplNumber();
				break;
			case OPGANIZATION_FINANCE_CAPITAL:
				enumType = new OrganizationFinanceCapital();
				break;
			case OPGANIZATION_INDYSTRY:
				enumType = new OrganizationIndustry();
				break;
			case OPPORTUNITY_PHASE:
				enumType = new OpportunityPhase();
				break;
			case OPPORTUNITY_STATUS:
				enumType = new OpportunityStatus();
				break;
			case TASK_CATEGORY:
				enumType = new TaskCategory();
				break;
			case TaxType:
				enumType = new TaxTypeEnum();
				break;
			case DepartmentAGIRC:
				enumType = new DepartmentAGIRC();
				break;
			case DepartmentARRCO:
				enumType = new DepartmentARRCO();
				break;
			case EmploymentClassification:
				enumType = new EmploymentClassification();
				break;
			case ContractDeTravailType:
				enumType = new ContractDeTravailType();
				break;
			case ContractDeTravailReason:
				enumType = new ContractDeTravailReason();
				break;
			case ContractDeTravailAssistence:
				enumType = new ContractDeTravailAssistence();
				break;
			default:
				assert(false) : "unknown clazz";
				break;
		}
		return enumType;
	}
}
