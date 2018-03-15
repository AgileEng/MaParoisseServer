/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 30.11.2009 16:20:52
 */
package eu.agileeng.domain.document;

import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEStringUtil;

/**
 *
 */
@SuppressWarnings("serial")
public class AEDocumentFilter extends AEDomainObject {

	private AEDocumentType docType;
	private String number;
	private Boolean template;
	private Date dateFrom;
	private Date dateTo;
	
	private boolean loadAttachments;
	
	private Validated validated = AEDocumentFilter.Validated.ALL;
	
	/**
	 * Percent or Amount mode
	 * 
	 * @author vvatov
	 */
	static public enum Validated {
		ALL(0L),
		PROCESSED(10L),
		NOT_PROCESSED(20L);
				
		private long validatedId;
		
		private Validated(long validatedId) {
			this.validatedId = validatedId;
		}
		
		public final long getValidatedId() {
			return this.validatedId;
		}
		
		public static Validated valueOf(long validatedId) {
			Validated ret = ALL;
			for (Validated inst : Validated.values()) {
				if(inst.getValidatedId() == validatedId) {
					ret = inst;
					break;
				}
			}
			return ret;
		}
	}
	
	/**
	 * 
	 */
	public AEDocumentFilter() {
		super(DomainClass.AEDocumentFilter);
	}
	
	public AEDocumentFilter(Date from, Date to, AEDocumentType docType) {
		this();
		this.dateFrom = from;
		this.dateTo = to;
		this.docType = docType;
	}

	/**
	 * @return the dateFrom
	 */
	public Date getDateFrom() {
		return dateFrom;
	}

	/**
	 * @param dateFrom the dateFrom to set
	 */
	public void setDateFrom(Date dateFrom) {
		this.dateFrom = dateFrom;
	}

	/**
	 * @return the dateTo
	 */
	public Date getDateTo() {
		return dateTo;
	}

	/**
	 * @param dateTo the dateTo to set
	 */
	public void setDateTo(Date dateTo) {
		this.dateTo = dateTo;
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

	public AEDescriptive getOwner() {
		return getCompany();
	}

	public void setOwner(AEDescriptive owner) {
		setCompany(owner);
	}

	public Boolean getTemplate() {
		return template;
	}

	public void setTemplate(Boolean template) {
		this.template = template;
	}

	public boolean isLoadAttachments() {
		return loadAttachments;
	}

	public void setLoadAttachments(boolean loadAttachments) {
		this.loadAttachments = loadAttachments;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// validated
		long validatedId = jsonObject.optLong(AEDocument.JSONKey.validated.toString());
		setValidated(AEDocumentFilter.Validated.valueOf(validatedId));
		
		// docType
		long docType = jsonObject.optLong("docType");
		AEDocumentType aeDocType = AEDocumentType.valueOf(docType);
		if(aeDocType != null && !AEDocumentType.valueOf(AEDocumentType.System.NA).equals(aeDocType)) {
			setDocType(aeDocType);
		}
		
		// number
		String number = jsonObject.optString("number");
		if(!AEStringUtil.isEmpty(number)) {
			setNumber(number);
		}
		
		// dateFrom
		if(jsonObject.has("dateFrom")) {
			this.setDateFrom(AEDateUtil.parseDateStrict(jsonObject.optString("dateFrom")));
		}
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();

		// docType
		if(getDocType() != null) {
			json.put("docType", getDocType().getSystemID());
		}
		
		// number
		if(!AEStringUtil.isEmpty(getNumber())) {
			json.put("number", getNumber());
		}
		
		return json;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public Validated getValidated() {
		return validated;
	}

	public void setValidated(Validated validated) {
		this.validated = validated;
	}

}
