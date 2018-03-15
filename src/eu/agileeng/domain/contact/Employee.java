/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 20.11.2009 16:20:56
 */
package eu.agileeng.domain.contact;

import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;
import org.jboss.logging.Logger;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.contact.Person.NationalityType;
import eu.agileeng.domain.contact.Person.SalutationType;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.social.contractdetravail.ContractDeTravailType;
import eu.agileeng.domain.document.social.findutravail.FinDuTravail;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.util.AECollectionUtil;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEObjectUtil;
import eu.agileeng.util.AEStringUtil;

/**
 *
 */
public class Employee extends AEDomainObject {

	private static final long serialVersionUID = 8284563080597793256L;

	private static Logger LOG = Logger.getLogger(Employee.class);
	
	static public class JSONKey {
		public static final String employee = "employee";
		public static final String employees = "employees";
		private static final String leTexteBool = "leTexteBool";
		private static final String dateEntry = "dateEntry";
		private static final String dateRelease = "dateRelease";
		private static final String modificationDate = "modificationDate";
		private static final String numberOfWeeks = "numberOfWeeks";
		private static final String contractType = "contractType";
		private static final String reasonRelease = "reasonRelease";
		public static final String ftpId = "ftpId";
	}
	
	private transient boolean buildLazzy;
	
	/**
	 * The last known person's data for this employee
	 */
	private AEDescriptive person;
	
	private boolean hasUIN = true;
	
	/**
	 * Unique identification number.
	 * EIN in case of organization,
	 * EGN in case of person
	 */
	private String UIN;
	
	private boolean validateUIN = true;
	
	private String formOfAddress;
	
	// doctor ...
	private String title;
	
	private String firstName;
	
	private String middleName;
	
	private String lastName;
	
	/**
	 * The field name must me assembled from these names
	 */
	
	private String girlName;
	
	// mister, missis ...
	private SalutationType salutation;
	
	private Date dateOfBirth;
	
	private String placeOfBirth;
	
	private NationalityType nationalityType;
	
	private String nationality;
	
	private AEDocumentType docType; //residenceCard too in France
	
	/**
	 * Document's number
	 */
	private String docNumber;
	
	/**
	 * Document's Date Of Expiry
	 */
	private Date docDateOfExpiry;
	
	/**
	 * Document's Issued By as free form text
	 */
	private String docIssuedByString;
	
	private String position;
	
	private boolean leTexte;
	
	private Address address;
	
	private Contact contact;
	
	private Date dateEntry;
	
	private Date dateRelease;
	
	private Date modificationDate;
	
	private Integer numberOfWeeks;
	
	private ContractDeTravailType contractType;
	
	private FinDuTravail.Reason reasonRelease;
	
	private Long ftpId;
	
	/**
	 * @param clazz
	 */
	public Employee() {
		super(DomainClass.EMPLOYEE);
	}
	
	/**
	 * @param clazz
	 */
	public Employee(AEDescriptive company, AEDescriptive person, String position) {
		this();
		setCompany(company);
		this.person = person;
		this.position = position;
	}

	/**
	 * @return the person
	 */
	public AEDescriptive getPerson() {
		return person;
	}

	/**
	 * @param person the person to set
	 */
	public void setPerson(AEDescriptive person) {
		this.person = person;
	}

	/**
	 * @return the position
	 */
	public String getPosition() {
		return position;
	}

	/**
	 * @param position the position to set
	 */
	public void setPosition(String position) {
		this.position = position;
	}
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.EMPLOYEE);
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
//		{name : 'person'}, // type : object or proxy, 'person.id is always filled' 
		if(jsonObject.has(Person.JSONKey.person)) {
			Person person = new Person();
			person.create(jsonObject.getJSONObject(Person.JSONKey.person));
			this.setPerson(person);
		}
		if(jsonObject.has(Person.JSONKey.hasIdentityNo)) {
			setHasUIN(jsonObject.optBoolean(Person.JSONKey.hasIdentityNo)); 
		}
//		{name : 'identityNo', type : 'string'},
		if(jsonObject.has(Person.JSONKey.identityNo)) {
			setUIN(jsonObject.optString(Person.JSONKey.identityNo)); 
		}
//	    {name : 'salutationID', type : 'long'}, // title: {10, Mr.} {20, Mrs.} {30, Miss}
		if(jsonObject.has(Person.JSONKey.salutationID)) {
			this.setSalutation(Person.SalutationType.valueOf(jsonObject.optLong(Person.JSONKey.salutationID)));
		}
//		{name : 'firstName', type : 'string'},
		if(jsonObject.has(Person.JSONKey.firstName)) {
			this.setFirstName(jsonObject.optString(Person.JSONKey.firstName));
		}
//		{name : 'middleName', type : 'string'},
		if(jsonObject.has(Person.JSONKey.middleName)) {
			this.setMiddleName(jsonObject.optString(Person.JSONKey.middleName));
		}
//		{name : 'lastName', type : 'string'},
		if(jsonObject.has(Person.JSONKey.lastName)) {
			this.setLastName(jsonObject.optString(Person.JSONKey.lastName));
		}
//		{name : 'girlName', type : 'string'},
		if(jsonObject.has(Person.JSONKey.girlName)) {
			this.setGirlName(jsonObject.optString(Person.JSONKey.girlName));
		}
//		{name : 'dateOfBirth', type : 'date', dateFormat : 'd-m-Y'},
		if(jsonObject.has(Person.JSONKey.dateOfBirth)) {
			this.setDateOfBirth(AEDateUtil.parseDateStrict(jsonObject.optString(Person.JSONKey.dateOfBirth)));
		}
//		{name : 'placeOfBirth', type : 'string'},
		if(jsonObject.has(Person.JSONKey.placeOfBirth)) {
			this.setPlaceOfBirth(jsonObject.optString(Person.JSONKey.placeOfBirth));
		}
//		{name : 'nationalityTypeID', type : 'long'}, // {} {}
		if(jsonObject.has(Person.JSONKey.nationalityTypeID)) {
			this.setNationalityType(Person.NationalityType.valueOf(
					jsonObject.optLong(Person.JSONKey.nationalityTypeID)));
		}
//		{name : 'nationalityStr', type : 'long'},
		if(jsonObject.has(Person.JSONKey.nationalityStr)) {
			this.setNationality(jsonObject.optString(Person.JSONKey.nationalityStr));
		}
//		{name : 'docTypeID', type : 'long'},
		if(jsonObject.has(Person.JSONKey.docTypeID)) {
			this.setDocType(AEDocumentType.valueOf(jsonObject.optLong(Person.JSONKey.docTypeID)));
		}
//		{name : 'docNumber', type : 'string'},
		if(jsonObject.has(Person.JSONKey.docNumber)) {
			this.setDocNumber(jsonObject.optString(Person.JSONKey.docNumber));
		}
//		{name : 'docDateOfExpiry', type : 'date', dateFormat : 'd-m-Y'},
		if(jsonObject.has(Person.JSONKey.docDateOfExpiry)) {
			this.setDocDateOfExpiry(AEDateUtil.parseDateStrict(jsonObject.optString(Person.JSONKey.docDateOfExpiry)));
		}
//		{name : 'docIssuedByStr',	type : 'string'},
		if(jsonObject.has(Person.JSONKey.docIssuedByStr)) {
			this.setDocIssuedByString(jsonObject.optString(Person.JSONKey.docIssuedByStr));
		}
//		{name : 'address'}, // type : object, see AccBureau.DomainRecord Address
		if(jsonObject.has(Person.JSONKey.address)) {
			Address address = new Address(Address.Type.BUSINESS);
			try {
				address.createExt(jsonObject.getJSONObject(Person.JSONKey.address));
				this.setAddress(address);
			} catch(JSONException e) {
			}
		}
//		{ame  : 'contact'}, // type : object, see AccBureau.DomainRecord Contact
		if(jsonObject.has(Person.JSONKey.contact)) {
			Contact contact = new Contact(Contact.Type.BUSINESS);
			try {
				contact.createExt(jsonObject.getJSONObject(Person.JSONKey.contact));
				this.setContact(contact);
			} catch(JSONException e) {
			}
		}
//		{name : 'leTexteBool', type : 'boolean', defaultValue : false}] 
		if(jsonObject.has(Employee.JSONKey.leTexteBool)) {
			this.setLeTexte(jsonObject.optBoolean(Employee.JSONKey.leTexteBool));
		}
		
		if(jsonObject.has(Employee.JSONKey.dateEntry)) {
			this.setDateEntry(AEDateUtil.parseDateStrict(jsonObject.optString(Employee.JSONKey.dateEntry)));
		}
		
		if(jsonObject.has(Employee.JSONKey.dateRelease)) {
			this.setDateRelease(AEDateUtil.parseDateStrict(jsonObject.optString(Employee.JSONKey.dateRelease)));
		}
		
		if(jsonObject.has(Employee.JSONKey.modificationDate)) {
			this.setModificationDate(AEDateUtil.parseDateStrict(jsonObject.optString(Employee.JSONKey.modificationDate)));
		}
		
		if(jsonObject.has(Employee.JSONKey.numberOfWeeks)) {
			int numberOfWeeks = jsonObject.optInt(Employee.JSONKey.numberOfWeeks);
			if(numberOfWeeks > 0) {
				this.setNumberOfWeeks(numberOfWeeks);
			}
		}
		
		if(jsonObject.has(Employee.JSONKey.contractType)) {
			long contractType = jsonObject.optLong(Employee.JSONKey.contractType);
			if(contractType > 0) {
				this.setContractType(new ContractDeTravailType(ContractDeTravailType.System.valueOf(contractType)));
			}
		}
		
		if(jsonObject.has(Employee.JSONKey.reasonRelease)) {
			long reasonRelease = jsonObject.optLong(Employee.JSONKey.reasonRelease);
			if(reasonRelease > 0) {
				this.setReasonRelease(FinDuTravail.Reason.valueOf(reasonRelease));
			}
		}
		
		if(jsonObject.has(Employee.JSONKey.ftpId)) {
			long ftpId = jsonObject.optLong(Employee.JSONKey.ftpId);
			if(ftpId > 0) {
				this.setFtpId(ftpId);
			}
		}
		
		this.createName();
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
//		{name : 'person'}, // type : object or proxy, 'person.id is always filled' 
		if(getPerson() != null) {
			json.put(Person.JSONKey.person, getPerson().toJSONObject());
		}
		json.put(Person.JSONKey.hasIdentityNo, isHasUIN());
		json.put(Person.JSONKey.identityNo, getUIN());
		if(getSalutation() != null) {
			json.put(Person.JSONKey.salutationID, getSalutation().getTypeID());
		}
		json.put(Person.JSONKey.firstName, getFirstName());
		json.put(Person.JSONKey.middleName, getMiddleName());
		json.put(Person.JSONKey.lastName, getLastName());
		json.put(Person.JSONKey.girlName, getGirlName());
		if(getDateOfBirth() != null) {
			json.put(Person.JSONKey.dateOfBirth, AEDateUtil.formatToSystem(getDateOfBirth()));
		}
		json.put(Person.JSONKey.placeOfBirth, getPlaceOfBirth());
		if(getNationalityType() != null) {
			json.put(Person.JSONKey.nationalityTypeID, getNationalityType().getTypeID());
		}
		json.put(Person.JSONKey.nationalityStr, getNationality());
		if(getDocType() != null) {
			json.put(Person.JSONKey.docTypeID, getDocType().getSystemID());
		}
		json.put(Person.JSONKey.docNumber, getDocNumber());
		if(getDocDateOfExpiry() != null) {
			json.put(Person.JSONKey.docDateOfExpiry, AEDateUtil.formatToSystem(getDocDateOfExpiry()));
		}
		json.put(Person.JSONKey.docIssuedByStr, getDocIssuedByString());
		if(getAddress() != null) {
			json.put(Person.JSONKey.address, getAddress().toJSONObjectExt());
		}
		if(getContact() != null) {
			json.put(Person.JSONKey.contact, getContact().toJSONObjectExt());
		}
		
		json.put(Employee.JSONKey.leTexteBool, isLeTexte());
		
		if(getDateEntry() != null) {
			json.put(Employee.JSONKey.dateEntry, AEDateUtil.formatToSystem(getDateEntry()));
		}
		
		if(getDateRelease() != null) {
			json.put(Employee.JSONKey.dateRelease, AEDateUtil.formatToSystem(getDateRelease()));
		}
		
		if(getModificationDate() != null) {
			json.put(Employee.JSONKey.modificationDate, AEDateUtil.formatToSystem(getModificationDate()));
		}
		
		if(getNumberOfWeeks() != null) {
			json.put(Employee.JSONKey.numberOfWeeks, getNumberOfWeeks());
		}
		
		if(getContractType() != null) {
			json.put(Employee.JSONKey.contractType, getContractType().getSystemID());
		}
		
		if(getReasonRelease() != null) {
			json.put(Employee.JSONKey.reasonRelease, getReasonRelease().getTypeID());
		}
		
		if(getFtpId() != null) {
			json.put(Employee.JSONKey.ftpId, getFtpId());
		}
		
		return json;
	}
	
	/**
	 * @return the leTexte
	 */
	public boolean isLeTexte() {
		return leTexte;
	}

	/**
	 * @param leTexte the leTexte to set
	 */
	public void setLeTexte(boolean leTexte) {
		this.leTexte = leTexte;
	}

	public String getFormOfAddress() {
		return formOfAddress;
	}

	public void setFormOfAddress(String formOfAddress) {
		this.formOfAddress = formOfAddress;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getMiddleName() {
		return middleName;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getGirlName() {
		return girlName;
	}

	public void setGirlName(String girlName) {
		this.girlName = girlName;
	}

	public SalutationType getSalutation() {
		return salutation;
	}

	public void setSalutation(SalutationType salutation) {
		this.salutation = salutation;
	}

	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public String getPlaceOfBirth() {
		return placeOfBirth;
	}

	public void setPlaceOfBirth(String placeOfBirth) {
		this.placeOfBirth = placeOfBirth;
	}

	public NationalityType getNationalityType() {
		return nationalityType;
	}

	public void setNationalityType(NationalityType nationalityType) {
		this.nationalityType = nationalityType;
	}

	public String getNationality() {
		return nationality;
	}

	public void setNationality(String nationality) {
		this.nationality = nationality;
	}

	public AEDocumentType getDocType() {
		return docType;
	}

	public void setDocType(AEDocumentType docType) {
		this.docType = docType;
	}

	public String getDocNumber() {
		return docNumber;
	}

	public void setDocNumber(String docNumber) {
		this.docNumber = docNumber;
	}

	public Date getDocDateOfExpiry() {
		return docDateOfExpiry;
	}

	public void setDocDateOfExpiry(Date docDateOfExpiry) {
		this.docDateOfExpiry = docDateOfExpiry;
	}

	public String getDocIssuedByString() {
		return docIssuedByString;
	}

	public void setDocIssuedByString(String docIssuedByString) {
		this.docIssuedByString = docIssuedByString;
	}
	
	public void createName() {
		StringBuffer name = new StringBuffer();
		if(!AEStringUtil.isEmpty(getLastName())) {
			name.append(getLastName());
		}
		if(!AEStringUtil.isEmpty(getFirstName())) {
			if(!AEStringUtil.isEmpty(name)) {
				name.append(" ");
			}
			name.append(getFirstName());
		}
		setName(name.toString());
	}

	public Address getAddress() {
		return address;
	}

	public AddressesList getAddressesList() {
		AddressesList al = new AddressesList();
		if(getAddress() != null) {
			al.add(getAddress());
		}
		return al;
	}
	
	public void setAddress(Address address) {
		this.address = address;
	}

	public Contact getContact() {
		return contact;
	}

	public ContactsList getContactsList() {
		ContactsList cl = new ContactsList();
		if(getContact() != null) {
			cl.add(getContact());
		}
		return cl;
	}
	
	public void setContact(Contact contact) {
		this.contact = contact;
	}

	private void setPersonData(Person person) {
		// build person
		person.setHasUIN(isHasUIN());
		person.setUIN(getUIN());
		person.setTitle(getTitle());
		person.setFirstName(getFirstName());
		person.setMiddleName(getMiddleName());
		person.setLastName(getLastName());
		person.setName(getName());
		person.setGirlName(getGirlName());
		person.setSalutation(getSalutation());
		person.setDateOfBirth(getDateOfBirth());
		person.setPlaceOfBirth(getPlaceOfBirth());
		person.setNationalityType(getNationalityType());
		person.setNationality(getNationality());
		person.setDocType(getDocType());
		person.setDocNumber(getDocNumber());
		person.setDocDateOfExpiry(getDocDateOfExpiry());
		person.setDocIssuedByString(getDocIssuedByString());
		
		// address
		if(getAddress() != null) {
			try {
				Address address = (Address) AEObjectUtil.deepCopy(getAddress());
				Address personAddress = person.grantAddressesList().getAddress(Address.Type.BUSINESS);
				if(personAddress != null) {
					personAddress.setStreet(address.getStreet());
					personAddress.setSecondaryStreet(address.getSecondaryStreet());
					personAddress.setPostalCode(address.getPostalCode());
					personAddress.setCity(address.getCity());
					personAddress.setDistrict(address.getDistrict());
					personAddress.setState(address.getState());
					personAddress.setCountryID(address.getCountryID());
					personAddress.setpOBox(address.getpOBox());
					personAddress.setAbbreviation(address.getAbbreviation());
					
					personAddress.setUpdated();
				} else {
					address.resetAsNew();
					address.setType(Address.Type.BUSINESS);
					person.grantAddressesList().add(address);
				}
			} catch (AEException e) {
				LOG.error(e);
			}
		}
		
		// contact
		if(getContact() != null) {
			try {
				Contact contact = (Contact) AEObjectUtil.deepCopy(getContact());
				Contact personContact = person.grantContactsList().getContact(Contact.Type.BUSINESS);
				if(personContact != null) {
					// copy contact data to personContact
					personContact.setPhone(contact.getPhone());
					personContact.setMobile(contact.getMobile());
					personContact.seteMail(contact.geteMail());
					personContact.setHomepage(contact.getHomepage());
					personContact.setFax(contact.getFax());
					
					personContact.setUpdated();
				} else {
					// add the contact
					contact.resetAsNew();
					contact.setType(Contact.Type.BUSINESS);
					person.grantContactsList().add(contact);
				}
			} catch (AEException e) {
				LOG.error(e);
			}
		}
		
		person.setUpdated();
	}
	
	public Person createPerson() {
		Person person = new Person();
		setPersonData(person);
		this.setPerson(person);
		return person;
	}
	
	public void create(Person person) {
		setID(AEPersistentUtil.getTmpID());
		
		// build person
		this.setHasUIN(person.isHasUIN());
		this.setUIN(person.getUIN());
		this.setTitle(person.getTitle());
		this.setFirstName(person.getFirstName());
		this.setMiddleName(person.getMiddleName());
		this.setLastName(person.getLastName());
		this.setName(person.getName());
		this.setGirlName(person.getGirlName());
		this.setSalutation(person.getSalutation());
		this.setDateOfBirth(person.getDateOfBirth());
		this.setPlaceOfBirth(person.getPlaceOfBirth());
		this.setNationalityType(person.getNationalityType());
		this.setNationality(person.getNationality());
		this.setDocType(person.getDocType());
		this.setDocNumber(person.getDocNumber());
		this.setDocDateOfExpiry(person.getDocDateOfExpiry());
		this.setDocIssuedByString(person.getDocIssuedByString());
		
		// address
		if(!AECollectionUtil.isEmpty(person.getAddressesList())) {
			Address address = person.getAddressesList().getAddress(Address.Type.BUSINESS);
			address.resetAsNew();
			setAddress(address);
		}
		
		// contact
		if(!AECollectionUtil.isEmpty(person.getContactsList())) {
			Contact contact = person.getContactsList().getContact(Contact.Type.BUSINESS);
			contact.resetAsNew();
			setContact(contact);
		}
		
		this.setPerson(person.getDescriptor());
	}
	
	public Person updatePerson() {
		return updatePerson(null);
	}
	
	public Person updatePerson(Person person) {
		if(person == null) {
			person = new Person();
			if(getPerson() != null) {
				person.setID(getPerson().getDescriptor().getID());
			}
		}
		setPersonData(person);
		this.setPerson(person);
		return person;
	}

	public String getUIN() {
		return UIN;
	}

	public void setUIN(String uIN) {
		UIN = uIN;
	}

	public Date getDateEntry() {
		return dateEntry;
	}

	public void setDateEntry(Date dateEntry) {
		this.dateEntry = dateEntry;
	}

	public Date getDateRelease() {
		return dateRelease;
	}

	public void setDateRelease(Date dateRelease) {
		this.dateRelease = dateRelease;
	}

	public Date getModificationDate() {
		return modificationDate;
	}

	public void setModificationDate(Date modificationDate) {
		this.modificationDate = modificationDate;
	}

	public Integer getNumberOfWeeks() {
		return numberOfWeeks;
	}

	public void setNumberOfWeeks(Integer numberOfWeeks) {
		this.numberOfWeeks = numberOfWeeks;
	}

	public ContractDeTravailType getContractType() {
		return contractType;
	}

	public void setContractType(ContractDeTravailType cotractType) {
		this.contractType = cotractType;
	}

	public FinDuTravail.Reason getReasonRelease() {
		return reasonRelease;
	}

	public void setReasonRelease(FinDuTravail.Reason reasonRelease) {
		this.reasonRelease = reasonRelease;
	}

	public boolean isHasUIN() {
		return hasUIN;
	}

	public void setHasUIN(boolean hasUIN) {
		this.hasUIN = hasUIN;
	}

	public boolean isValidateUIN() {
		return validateUIN;
	}

	public void setValidateUIN(boolean validateUIN) {
		this.validateUIN = validateUIN;
	}

	public boolean isBuildLazzy() {
		return buildLazzy;
	}

	public void setBuildLazzy(boolean buildLazzy) {
		this.buildLazzy = buildLazzy;
	}

	public Long getFtpId() {
		return ftpId;
	}

	public void setFtpId(Long ftpId) {
		this.ftpId = ftpId;
	}
	
	@Override
	public void resetAsNew() {
		super.resetAsNew();
		
		// address
		if(getAddress() != null) {
			getAddress().resetAsNew();
		}
		
		// contact
		if(getContact() != null) {
			getContact().resetAsNew();
		}
	}
}
