/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 22.05.2010 11:17:16
 */
package eu.agileeng.domain.document.social.findutravail;

import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.social.AESocialDocument;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.util.AEDateUtil;

/**
 *
 */
public class FinDuTravail extends AESocialDocument {
	
	static public enum JSONKey {
		responsibleId,
		responsibleName,
		dateDeAccident,
		dateDuDernier,
		student,
		leavingReason,
		leavingReasonDescription,
		employedAs,
		employedAsId,
		stillPresent;
	}
	
	static public enum Reason {
		NA(0),
		FinDeCDD(10),
		Demission(20),
		RuptureCDDEmployeur(30),
		RuptureCDDSalarie(40),
		RuptureCDIEmployeur(50),
		RuptureCDISalarie(60),
		Licenciement(70),
		AutreCas(80);
				
		private long typeID;
		
		private Reason(long typeID) {
			this.typeID = typeID;
		}
		
		public final long getTypeID() {
			return this.typeID;
		}
		
		public static Reason valueOf(long typeID) {
			Reason ret = NA;
			for (Reason inst : Reason.values()) {
				if(inst.getTypeID() == typeID) {
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
	private static final long serialVersionUID = 5824615397800151427L;
	
	private AEDescriptive supervisor;
	
	private AEDescriptive employeePosition;
	
	private Date dateEntry;
	
	private Date dateRelease;
	
	private boolean student;
	
	private AEDescriptive reason;
	
	private boolean stillPresent;
	
	/**
	 * no arg constructor.
	 * Try to use it only during DB Fetching
	 */
	public FinDuTravail() {
		super(AEDocumentType.valueOf(AEDocumentType.System.FinDuTravail));
	}

	public AEDescriptive getSupervisor() {
		return supervisor;
	}
	
	public AEDescriptive grantSupervisor() {
		if(this.supervisor == null) {
			this.supervisor = new AEDescriptorImp();
		}
		return this.supervisor;
	}

	public void setSupervisor(AEDescriptive supervisor) {
		this.supervisor = supervisor;
	}

	public AEDescriptive getEmployeePosition() {
		return employeePosition;
	}

	public AEDescriptive grantEmployeePosition() {
		if(this.employeePosition == null) {
			this.employeePosition = new AEDescriptorImp();
		}
		return this.employeePosition;
	}
	
	public void setEmployeePosition(AEDescriptive employeePosition) {
		this.employeePosition = employeePosition;
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

	public boolean isStudent() {
		return student;
	}

	public void setStudent(boolean student) {
		this.student = student;
	}

	public AEDescriptive getReason() {
		return reason;
	}

	public AEDescriptive grantReason() {
		if(this.reason == null) {
			this.reason = new AEDescriptorImp();
		}
		return this.reason;
	}
	
	public void setReason(AEDescriptive reason) {
		this.reason = reason;
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		//private AEDescriptive supervisor;
		if(getSupervisor() != null) {
			json.put(JSONKey.responsibleId.toString(), getSupervisor().getDescriptor().getID());
			json.put(JSONKey.responsibleName.toString(), getSupervisor().getDescriptor().getName());
		}
		
		//private AEDescriptive employeePosition;
		if(getEmployeePosition() != null) {
			json.put(JSONKey.employedAsId.toString(), getEmployeePosition().getDescriptor().getID());
			json.put(JSONKey.employedAs.toString(), getEmployeePosition().getDescriptor().getName());
		}
		
		//private Date dateEntry;
		if(getDateEntry() != null) {
			json.put(JSONKey.dateDeAccident.toString(), AEDateUtil.formatToSystem(getDateEntry()));
		}
		
		//private Date dateRelease;
		if(getDateRelease() != null) {
			json.put(JSONKey.dateDuDernier.toString(), AEDateUtil.formatToSystem(getDateRelease()));
		}
		
		//private boolean student;
		json.put(JSONKey.student.toString(), isStudent());
		
		//private AEDescriptive reason;
		if(getReason() != null) {
			json.put(JSONKey.leavingReason.toString(), getReason().getDescriptor().getID());
			json.put(JSONKey.leavingReasonDescription.toString(), getReason().getDescriptor().getDescription());
		}
		
		//stillPresent
		json.put(JSONKey.stillPresent.toString(), isStillPresent());
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		//private AEDescriptive supervisor;
		if(jsonObject.has(JSONKey.responsibleId.toString()) || jsonObject.has(JSONKey.responsibleName.toString())) {
			AEDescriptor supervisor = new AEDescriptorImp();
			supervisor.setID(jsonObject.optLong(JSONKey.responsibleId.toString()));
			supervisor.setName(jsonObject.optString(JSONKey.responsibleName.toString()));
			setSupervisor(supervisor);
		}
		
		//private AEDescriptive employeePosition;
		if(jsonObject.has(JSONKey.employedAsId.toString()) || jsonObject.has(JSONKey.employedAs.toString())) {
			AEDescriptor emplPosition = new AEDescriptorImp();
			emplPosition.setID(jsonObject.optLong(JSONKey.employedAsId.toString()));
			emplPosition.setName(jsonObject.optString(JSONKey.employedAs.toString()));
			setEmployeePosition(emplPosition);
		}
		
		//private Date dateEntry;
		if(jsonObject.has(JSONKey.dateDeAccident.toString())) {
			setDateEntry(AEDateUtil.parseDateStrict(jsonObject.optString(JSONKey.dateDeAccident.toString())));
		}
		
		//private Date dateRelease;
		if(jsonObject.has(JSONKey.dateDuDernier.toString())) {
			setDateRelease(AEDateUtil.parseDateStrict(jsonObject.optString(JSONKey.dateDuDernier.toString())));
		}
		
		//private boolean student;
		setStudent(jsonObject.optBoolean(JSONKey.student.toString()));
		
		//private AEDescriptive reason;
		if(jsonObject.has(JSONKey.leavingReason.toString())) {
			AEDescriptor reason = new AEDescriptorImp();
			reason.setID(jsonObject.optLong(JSONKey.leavingReason.toString()));
			reason.setDescription(jsonObject.optString(JSONKey.leavingReasonDescription.toString()));
			setReason(reason);
		}
		
		//stillPresent
		setStillPresent(jsonObject.optBoolean(JSONKey.stillPresent.toString()));
	}

	public boolean isStillPresent() {
		return stillPresent;
	}

	public void setStillPresent(boolean stillPresent) {
		this.stillPresent = stillPresent;
	}
}
