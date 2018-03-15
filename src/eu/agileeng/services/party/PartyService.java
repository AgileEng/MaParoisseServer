/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 09.12.2009 16:38:19
 */
package eu.agileeng.services.party;

import java.io.File;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDescriptorsList;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel;
import eu.agileeng.domain.contact.Employee;
import eu.agileeng.domain.contact.EmployeeList;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.contact.PartiesList;
import eu.agileeng.domain.contact.Party;
import eu.agileeng.domain.contact.Person;
import eu.agileeng.domain.contact.SubjectCompAssoc;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.AEService;

/**
 *
 */
public interface PartyService extends AEService {
	public Organization manage(Organization organization, AEInvocationContext invContext) throws AEException;
	public Person manage(Person person, AEInvocationContext invContext) throws AEException;
	public Party load(AEDescriptor partyDescr, AEInvocationContext invContext) throws AEException;
	public PartiesList loadAll(DomainModel.DomainClass partyClazz, AEInvocationContext invContext) throws AEException;
	public AEDescriptorsList loadAllDescriptors(DomainModel.DomainClass partyClazz, AEInvocationContext invContext) throws AEException;
	
	
	public Employee save(Employee empl, AEInvocationContext invContext) throws AEException;
	public Employee loadEmployee(AEDescriptor emplDescr, AEInvocationContext invContext) throws AEException;
	public EmployeeList loadEmployeesToCompany(AEDescriptor compDescr, AEInvocationContext invContext) throws AEException;
	
	public SubjectCompAssoc save(SubjectCompAssoc assoc, AEInvocationContext invContext) throws AEException;
	public SubjectCompAssoc loadSubjectCompAssoc(AEDescriptor assocDescr, AEInvocationContext invContext) throws AEException;
	public PartiesList loadCompaniesToSubject(AEDescriptor subjDescr, AEInvocationContext invContext) throws AEException;

	public AEResponse loadCustomer(AERequest request, AEInvocationContext invContext) throws AEException;
	public AEResponse saveCustomer(AERequest request, AEInvocationContext invContext) throws AEException;
	public AEResponse loadCustomers(AERequest request, AEInvocationContext invContext) throws AEException;
	public AEResponse saveCompany(AERequest request) throws AEException;
	public AEResponse loadCompanies(AERequest request) throws AEException;
	public AEResponse loadSupplyData(AERequest request) throws AEException;
	public AEResponse deleteCompany(AERequest request) throws AEException;
	public AEResponse deactivateCompany(AERequest request, AEInvocationContext invContext) throws AEException;
	public AEResponse activateCompany(AERequest request, AEInvocationContext invContext) throws AEException;
	public AEResponse newCustomerByTemplate(AERequest request) throws AEException;
	public AEResponse importSuppliers(File fileSuppliers, AEInvocationContext invContext) throws AEException;
	public AEResponse saveCompanies(AERequest request) throws AEException;
	
	public AEResponse loadOrganizationTemplates(AERequest request, AEInvocationContext invContext) throws AEException;
}
