/**
 * 
 */
package eu.agileeng.domain.document.social;

import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.contact.Employee;
import eu.agileeng.domain.document.AEDocumentFilter;
import eu.agileeng.domain.document.social.findutravail.FinDuTravail;
import eu.agileeng.util.AEDateUtil;

/**
 * @author vvatov
 *
 */
public class AESocialDocumentFilter extends AEDocumentFilter {
	
	static public class JSONKey {
		public static final String modificationDate = "modificationDate";
		public static final String effectiveDate = "effectiveDate";
		public static final String reasonRelease = "reasonRelease";	
		public static final String dateDuDernier = "dateDuDernier";
		public static final String dateDeFin = "dateDeFin";
		public static final String dateDeReprise = "dateDeReprise";
	}
	
	private Employee employee; 
	
	private Date modificationDate;
	
	private Date effectiveDate;
	
	private FinDuTravail.Reason reasonRelease;
	
	private Date dateDuDernier;
	
	private Date dateDeFin;
	
	private Date dateDeReprise;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8516339111268762874L;

	/**
	 * @param clazz
	 */
	public AESocialDocumentFilter() {
	}

	public Employee getEmployee() {
		return employee;
	}

	public void setEmployee(Employee employee) {
		this.employee = employee;
	}

	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// employee
		if(jsonObject.has(AESocialDocument.JSONKey.employee)) {
			this.employee = new Employee();
			this.employee.create(jsonObject.getJSONObject(AESocialDocument.JSONKey.employee));
		}
		
		// public static final String modificationDate = "modificationDate";
		if(jsonObject.has(JSONKey.modificationDate)) {
			this.setModificationDate(AEDateUtil.parseDateStrict(jsonObject.optString(JSONKey.modificationDate)));
		}
		
		// public static final String effectiveDate = "effectiveDate";
		if(jsonObject.has(JSONKey.effectiveDate)) {
			this.setEffectiveDate(AEDateUtil.parseDateStrict(jsonObject.optString(JSONKey.effectiveDate)));
		}
		
		if(jsonObject.has(JSONKey.reasonRelease)) {
			long reasonReleaseId = jsonObject.optLong(JSONKey.reasonRelease);
			if(reasonReleaseId > 0) {
				this.setReasonRelease(FinDuTravail.Reason.valueOf(reasonReleaseId));
			}
		}
		
		if(jsonObject.has(JSONKey.dateDuDernier)) {
			this.setDateDuDernier(AEDateUtil.parseDateStrict(jsonObject.optString(JSONKey.dateDuDernier)));
		}
		
		if(jsonObject.has(JSONKey.dateDeFin)) {
			this.setDateDeFin(AEDateUtil.parseDateStrict(jsonObject.optString(JSONKey.dateDeFin)));
		}
		
		if(jsonObject.has(JSONKey.dateDeReprise)) {
			this.setDateDeReprise(AEDateUtil.parseDateStrict(jsonObject.optString(JSONKey.dateDeReprise)));
		}
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		return json;
	}

	public Date getModificationDate() {
		return modificationDate;
	}

	public void setModificationDate(Date modificationDate) {
		this.modificationDate = modificationDate;
	}

	public Date getEffectiveDate() {
		return effectiveDate;
	}

	public void setEffectiveDate(Date effectiveDate) {
		this.effectiveDate = effectiveDate;
	}

	public FinDuTravail.Reason getReasonRelease() {
		return reasonRelease;
	}

	public void setReasonRelease(FinDuTravail.Reason reasonRelease) {
		this.reasonRelease = reasonRelease;
	}

	public Date getDateDuDernier() {
		return dateDuDernier;
	}

	public void setDateDuDernier(Date dateDuDernier) {
		this.dateDuDernier = dateDuDernier;
	}

	public Date getDateDeFin() {
		return dateDeFin;
	}

	public void setDateDeFin(Date dateDeFin) {
		this.dateDeFin = dateDeFin;
	}

	public Date getDateDeReprise() {
		return dateDeReprise;
	}

	public void setDateDeReprise(Date dateDeReprise) {
		this.dateDeReprise = dateDeReprise;
	}
}
