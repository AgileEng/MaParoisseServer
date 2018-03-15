/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 21.05.2010 15:29:33
 */
package eu.agileeng.domain.document;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel;
import eu.agileeng.domain.business.bank.BankAccount;
import eu.agileeng.domain.document.imp.AEDocumentDescriptorImp;
import eu.agileeng.domain.document.trade.AEDocumentItem;
import eu.agileeng.domain.document.trade.AEDocumentItemsList;
import eu.agileeng.domain.file.FileAttachment;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.util.AECollectionUtil;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEStringUtil;

/**
 *
 */
@SuppressWarnings("serial")
public class AEDocument extends AEDomainObject {

	static public enum JSONKey {
		validated,
		validatedBy,
		validatedTime,
		ids,
		ftpTransered;
	}
	
	private AEDocumentType type;
	
	private transient AEDocumentController documentController;
	
	/*
	 * Document number
	 */
	private long number;
	
	private String numberString;
	
	/**
	 * Document Date
	 */
	private Date date;
	
	/**
	 * Document registration number in the system.
	 */
	private long regNumber;
	
	private String regNumberString;
	
	/**
	 * Document registration date in the system. 
	 */
	private Date regDate;
	
	/**
	 * Document's Date Of Expiry
	 */
	private Date dateOfExpiry;
	
	/**
	 * Document's Issued By as free form text
	 */
	private String issuedByString;
	
	/**
	 * The journal where this document should be accounted
	 */
	private AEDescriptive journal;
	
	/**
	 * whether this document is template or not
	 */
	private boolean isTemplate;
	
	/**
	 * The template where this document is created upon.
	 */
	private AEDescriptive template;
	
	private AEDescriptive bankAcc;
	
	private AEDocumentItemsList items = new AEDocumentItemsList();
	
	private FileAttachment fileAttachment;
	
	private boolean validated;
	
	private String validatedBy;
	
	private Date validatedTime;
	
	private boolean ftpTransered;
	
	private AEDescriptor contributorDonationDescr;
	
	/**
	 * no arg constructor
	 */
	public AEDocument() {
		super(DomainModel.DomainClass.AeDocument);
	}

	/**
	 *  AEDocumentType arg constructor
	 */
	public AEDocument(AEDocumentType documentType) {
		this();
		this.type = documentType;
	}
	
	/**
	 * @return the type
	 */
	public AEDocumentType getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(AEDocumentType type) {
		this.type = type;
	}

	/**
	 * @return the number
	 */
	public long getNumber() {
		return number;
	}

	/**
	 * @param number the number to set
	 */
	public void setNumber(long number) {
		this.number = number;
	}

	/**
	 * @return the date
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * @param date the date to set
	 */
	public void setDate(Date date) {
		this.date = date;
	}

	/**
	 * @return the regNumber
	 */
	public long getRegNumber() {
		return regNumber;
	}

	/**
	 * @param regNumber the regNumber to set
	 */
	public void setRegNumber(long regNumber) {
		this.regNumber = regNumber;
	}

	/**
	 * @return the regDate
	 */
	public Date getRegDate() {
		return regDate;
	}

	/**
	 * @param regDate the regDate to set
	 */
	public void setRegDate(Date regDate) {
		this.regDate = regDate;
	}

	/**
	 * @return the numberString
	 */
	public String getNumberString() {
		return numberString;
	}

	/**
	 * @param numberString the numberString to set
	 */
	public void setNumberString(String numberString) {
		this.numberString = numberString;
	}

	/**
	 * @return the regNumberString
	 */
	public String getRegNumberString() {
		return regNumberString;
	}

	/**
	 * @param regNumberString the regNumberString to set
	 */
	public void setRegNumberString(String regNumberString) {
		this.regNumberString = regNumberString;
	}
	
	@Override
	public AEDescriptor getDescriptor() {
		AEDocumentDescriptor docDescr = new AEDocumentDescriptorImp(getID(), getType().getSystemID());
		docDescr.setCode(getCode());
		docDescr.setName(getName());
		
		StringBuffer description = new StringBuffer();
		description.append(getNumberString());
		if(!AEStringUtil.isEmpty(description)) {
			description.append(" / ");
		}
		try {
			description.append(AEDateUtil.formatToSystem(getDate()));
		} catch(Exception e) {}
		docDescr.setDescription(description.toString());
		
		docDescr.setDocumentType(getType());
		docDescr.setNumber(getNumberString());
		docDescr.setDate(getDate());
		return docDescr;
	}

	public AEDescriptor getDescriptorShort() {
		AEDocumentDescriptor docDescr = new AEDocumentDescriptorImp(getID(), getType().getSystemID());
		docDescr.setCode(getCode());
		docDescr.setName(getName());
		
		StringBuffer description = new StringBuffer();
		description.append(getNumberString());
		docDescr.setDescription(description.toString());
		
		docDescr.setDocumentType(getType());
		docDescr.setNumber(getNumberString());
		docDescr.setDate(getDate());
		return docDescr;
	}
	
	/**
	 * @return the documentController
	 */
	public AEDocumentController getDocumentController() {
		return this.documentController;
	}
	
	/**
	 * @return the documentController
	 */
	protected final void setDocumentController(AEDocumentController documentController) {
		assert(documentController != null);
		this.documentController = documentController;
	}

	/**
	 * @return the dateOfExpiry
	 */
	public Date getDateOfExpiry() {
		return dateOfExpiry;
	}

	/**
	 * @param dateOfExpiry the dateOfExpiry to set
	 */
	public void setDateOfExpiry(Date dateOfExpiry) {
		this.dateOfExpiry = dateOfExpiry;
	}

	/**
	 * @return the issuedByString
	 */
	public String getIssuedByString() {
		return issuedByString;
	}

	/**
	 * @param issuedByString the issuedByString to set
	 */
	public void setIssuedByString(String issuedByString) {
		this.issuedByString = issuedByString;
	}

	public AEDescriptive getJournal() {
		return journal;
	}

	public void setJournal(AEDescriptive journal) {
		this.journal = journal;
	}

	public AEDescriptive getTemplate() {
		return template;
	}

	public void setTemplate(AEDescriptive template) {
		this.template = template;
	}

	public boolean isTemplate() {
		return isTemplate;
	}

	public void setIsTemplate(boolean isTemplate) {
		this.isTemplate = isTemplate;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		setType(AEDocumentType.valueOf(jsonObject.getLong("docType")));
		setNumberString(jsonObject.optString("number"));
		setDate(AEDateUtil.parseDateStrict(jsonObject.optString("date")));
		setIsTemplate(jsonObject.optBoolean("isTemplate"));
		if(jsonObject.has("templateId") || jsonObject.has("templateCode")) {
			AEDescriptorImp template = new AEDescriptorImp();
			template.setID(jsonObject.optLong("templateId"));
			template.setCode(jsonObject.optString("templateCode"));
			setTemplate(template);
		}
		if(jsonObject.has("journalId") || jsonObject.has("journalCode")) {
			AEDescriptorImp journal = new AEDescriptorImp();
			journal.setID(jsonObject.optLong("journalId"));
			journal.setCode(jsonObject.optString("journalCode"));
			setJournal(journal);
		}
		
		// document items
		if(jsonObject.has("items")) {
			try {
				JSONArray itemsJSON = jsonObject.getJSONArray("items");
				AEDocumentItemsList items = new AEDocumentItemsList();
				items.create(itemsJSON);
				setItems(items);
			} catch(Exception e) {}
		}
		
		// fileAttachment
		if(jsonObject.has("fileAttachment")) {
			FileAttachment fileAttachment = new FileAttachment();
			fileAttachment.create(jsonObject.optJSONObject("fileAttachment"));
			setFileAttachment(fileAttachment);
		}
		
		// bankAccId
		if(jsonObject.has("bankAccId")) {
			long bankAccId = jsonObject.optLong("bankAccId");
			if(bankAccId > 0) {
				setBankAcc(BankAccount.lazyDescriptor(bankAccId));
			}
		}
		
		// ftpTransered
		if(jsonObject.has(JSONKey.ftpTransered.name())) {
			setFtpTransered(jsonObject.optBoolean(JSONKey.ftpTransered.name()));
		}
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();

		json.put("docType", getType().getSystemID());
		json.put("xType", getType().getSystemID());
		if(getDate() != null) {
			json.put("date", AEDateUtil.convertToString(getDate(), AEDateUtil.SYSTEM_DATE_FORMAT));
		}
		json.put("number", getNumberString());
		json.put("isTemplate", isTemplate());
		if(getTemplate() != null) {
			json.put("templateId", getTemplate().getDescriptor().getID());
			json.put("templateCode", getTemplate().getDescriptor().getCode());
		}
		if(getJournal() != null) {
			json.put("journalId", getJournal().getDescriptor().getID());
			json.put("journalCode", getJournal().getDescriptor().getCode());
		}
		
		json.put("items", getItems().toJSONArray());
		
		if(getBankAcc() != null) {
			json.put("bankAccId", getBankAcc().getDescriptor().getID());
			json.put("bankAccName", getBankAcc().getDescriptor().getName());
		}
		
		if(getFileAttachment() != null) {
			json.put("fileAttachment", getFileAttachment().toJSONObject());
		}
		
		// validation
		json.put(JSONKey.validated.toString(), isValidated());
		if(!AEStringUtil.isEmpty(getValidatedBy())) {
			json.put(JSONKey.validatedBy.toString(), getValidatedBy());
		}
		if(getValidatedTime() != null) {
			json.put(JSONKey.validatedTime.toString(), AEDateUtil.convertToString(getValidatedTime(), AEDateUtil.SYSTEM_DATE_FORMAT));
		}
		
		// ftpTransered
		json.put(JSONKey.ftpTransered.name(), isFtpTransered());
		
		return json;
	}
	
	public AEDocumentItemsList getItems() {
		return items;
	}

	public boolean addItem(AEDocumentItem item) {
		if(items == null) {
			items = new AEDocumentItemsList();
		}
		return items.add(item);
	}
	
	public void setItems(AEDocumentItemsList items) {
		this.items = items;
	}

	public FileAttachment getFileAttachment() {
		return fileAttachment;
	}

	public void setFileAttachment(FileAttachment fileAttachment) {
		this.fileAttachment = fileAttachment;
	}

	public AEDescriptive getBankAcc() {
		return bankAcc;
	}

	public void setBankAcc(AEDescriptive bankAcc) {
		this.bankAcc = bankAcc;
	}
	
	public final void sortItems(Comparator<AEDocumentItem> c) {
		if(!AECollectionUtil.isEmpty(this.items)) {
			Collections.sort(this.items, c);
		}
	}

	public boolean isValidated() {
		return validated;
	}

	public void setValidated(boolean validated) {
		this.validated = validated;
	}

	public String getValidatedBy() {
		return validatedBy;
	}

	public void setValidatedBy(String validatedBy) {
		this.validatedBy = validatedBy;
	}

	public Date getValidatedTime() {
		return validatedTime;
	}

	public void setValidatedTime(Date validatedTime) {
		this.validatedTime = validatedTime;
	}
	
	public boolean isOfType(AEDocumentType.System system) {
		return getType() != null ? getType().isOfType(system) : false;
	}

	public boolean isFtpTransered() {
		return ftpTransered;
	}

	public void setFtpTransered(boolean ftpTransered) {
		this.ftpTransered = ftpTransered;
	}

	/**
	 * @return the contributorDonationDescr
	 */
	public AEDescriptor getContributorDonationDescr() {
		return contributorDonationDescr;
	}

	/**
	 * @param contributorDonationDescr the contributorDonationDescr to set
	 */
	public void setContributorDonationDescr(AEDescriptor contributorDonationDescr) {
		this.contributorDonationDescr = contributorDonationDescr;
	}
}
