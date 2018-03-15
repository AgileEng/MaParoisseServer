/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 18.11.2009 16:12:12
 */
package eu.agileeng.domain.contact;

import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEStringUtil;

/**
 *
 */
@SuppressWarnings("serial")
public class Person extends Party {

	static public class JSONKey {
		public static final String person = "person";
		public static final String identityNo = "identityNo";
		public static final String hasIdentityNo = "hasIdentityNo";
		public static final String salutationID = "salutationID";
		public static final String firstName = "firstName";
		public static final String middleName = "middleName";
		public static final String lastName = "lastName";
		public static final String girlName = "girlName";
		public static final String name = "name";
		public static final String position = "position";
		public static final String dateOfBirth = "dateOfBirth";
		public static final String placeOfBirth = "placeOfBirth";
		public static final String nationalityTypeID = "nationalityTypeID";
		public static final String nationalityStr = "nationalityStr";
		public static final String docTypeID = "docTypeID";
		public static final String docNumber = "docNumber";
		public static final String docDateOfExpiry = "docDateOfExpiry";
		public static final String docIssuedByStr = "docIssuedByStr";
		public static final String address = "address";
		public static final String contact = "contact";
	}
	
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
	
	private String position;
	
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
	
	static public enum NationalityType {
		NA(0),
		French(5),
		Other(10),
		Europian(15);
				
		private long typeID;
		
		private NationalityType(long typeID) {
			this.typeID = typeID;
		}
		
		public final long getTypeID() {
			return this.typeID;
		}
		
		public static NationalityType valueOf(long typeID) {
			NationalityType ret = null;
			for (NationalityType inst : NationalityType.values()) {
				if(inst.getTypeID() == typeID) {
					ret = inst;
					break;
				}
			}
			if(ret == null) {
				ret = NA;
			}
			return ret;
		}
	}
	
//	{
//		name : 'Monsieur',
//		id : 10
//	}, {
//		name : 'Madame',
//		id : 20
//	}, {
//		name : 'Mademoiselle',
//		id : 30
//	}
	static public enum SalutationType {
		NA(0),
		Mr(10),
		Mrs(20),
		Ms(30);
				
		private long typeID;
		
		private SalutationType(long typeID) {
			this.typeID = typeID;
		}
		
		public final long getTypeID() {
			return this.typeID;
		}
		
		public static SalutationType valueOf(long typeID) {
			SalutationType ret = null;
			for (SalutationType inst : SalutationType.values()) {
				if(inst.getTypeID() == typeID) {
					ret = inst;
					break;
				}
			}
			if(ret == null) {
				ret = NA;
			}
			return ret;
		}
	}
	
	/**
	 * @param clazz
	 */
	public Person() {
		super(DomainClass.PERSON);
	}

	/**
	 * @return the formOfAddress
	 */
	public String getFormOfAddress() {
		return formOfAddress;
	}

	/**
	 * @param formOfAddress the formOfAddress to set
	 */
	public void setFormOfAddress(String formOfAddress) {
		this.formOfAddress = formOfAddress;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return the firstName
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * @param firstName the firstName to set
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/**
	 * @return the middleName
	 */
	public String getMiddleName() {
		return middleName;
	}

	/**
	 * @param middleName the middleName to set
	 */
	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	/**
	 * @return the lastName
	 */
	public String getLastName() {
		return lastName;
	}

	/**
	 * @param lastName the lastName to set
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	/**
	 * @return the girlName
	 */
	public String getGirlName() {
		return girlName;
	}

	/**
	 * @param girlName the girlName to set
	 */
	public void setGirlName(String girlName) {
		this.girlName = girlName;
	}

	/**
	 * @return the dateOfBirth
	 */
	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	/**
	 * @param dateOfBirth the dateOfBirth to set
	 */
	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	/**
	 * @return the placeOfBirth
	 */
	public String getPlaceOfBirth() {
		return placeOfBirth;
	}

	/**
	 * @param placeOfBirth the placeOfBirth to set
	 */
	public void setPlaceOfBirth(String placeOfBirth) {
		this.placeOfBirth = placeOfBirth;
	}

	/**
	 * @return the nationalityType
	 */
	public NationalityType getNationalityType() {
		return nationalityType;
	}

	/**
	 * @param nationalityType the nationalityType to set
	 */
	public void setNationalityType(NationalityType nationalityType) {
		this.nationalityType = nationalityType;
	}

	/**
	 * @return the nationality
	 */
	public String getNationality() {
		return nationality;
	}

	/**
	 * @param nationality the nationality to set
	 */
	public void setNationality(String nationality) {
		this.nationality = nationality;
	}

	/**
	 * @return the docNumber
	 */
	public String getDocNumber() {
		return docNumber;
	}

	/**
	 * @param docNumber the docNumber to set
	 */
	public void setDocNumber(String docNumber) {
		this.docNumber = docNumber;
	}

	/**
	 * @return the docDateOfExpiry
	 */
	public Date getDocDateOfExpiry() {
		return docDateOfExpiry;
	}

	/**
	 * @param docDateOfExpiry the docDateOfExpiry to set
	 */
	public void setDocDateOfExpiry(Date docDateOfExpiry) {
		this.docDateOfExpiry = docDateOfExpiry;
	}

	/**
	 * @return the docIssuedByString
	 */
	public String getDocIssuedByString() {
		return docIssuedByString;
	}

	/**
	 * @param docIssuedByString the docIssuedByString to set
	 */
	public void setDocIssuedByString(String docIssuedByString) {
		this.docIssuedByString = docIssuedByString;
	}

	/**
	 * @return the docType
	 */
	public AEDocumentType getDocType() {
		return docType;
	}

	/**
	 * @param docType the docType to set
	 */
	public void setDocType(AEDocumentType docType) {
		this.docType = docType;
	}

	/**
	 * @return the salutation
	 */
	public SalutationType getSalutation() {
		return salutation;
	}

	/**
	 * @param salutation the salutation to set
	 */
	public void setSalutation(SalutationType salutation) {
		this.salutation = salutation;
	}
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.PERSON);
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
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
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
			address.createExt(jsonObject.getJSONObject(Person.JSONKey.address));
			this.grantAddressesList().add(address);
		}
//		{ame  : 'contact'}, // type : object, see AccBureau.DomainRecord Contact
		if(jsonObject.has(Person.JSONKey.contact)) {
			Contact contact = new Contact(Contact.Type.BUSINESS);
			contact.createExt(jsonObject.getJSONObject(Person.JSONKey.contact));
			this.grantContactsList().add(contact);
		}
		
		this.createName();
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
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
			json.put(Person.JSONKey.docTypeID, getDocType().getID());
		}
		json.put(Person.JSONKey.docNumber, getDocNumber());
		if(getDocDateOfExpiry() != null) {
			json.put(Person.JSONKey.docDateOfExpiry, AEDateUtil.formatToSystem(getDocDateOfExpiry()));
		}
		json.put(Person.JSONKey.docIssuedByStr, getDocIssuedByString());
		if(getAddressesList() != null) {
			Address address = getAddressesList().getAddress(Address.Type.BUSINESS);
			if(address != null) {
				json.put(Person.JSONKey.address, address.toJSONObjectExt());
			}
		}
		if(getContactsList() != null) {
			Contact contact = getContactsList().getContact(Contact.Type.BUSINESS);
			if(contact != null) {
				json.put(Person.JSONKey.contact, contact.toJSONObjectExt());
			}
		}
		
		return json;
	}
	
	@Override
	public AEDescriptor getDescriptor() {
		PersonDescr pDescr = new PersonDescr(getID());
		pDescr.setCode(getCode());
		pDescr.setName(getName());
		pDescr.setDescription(getDescription());
		pDescr.setSalutation(getSalutation());
		pDescr.setPosition(getPosition());
		return pDescr;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}
}
