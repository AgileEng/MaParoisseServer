/**
 * 
 */
package eu.agileeng.domain.document.social;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.contact.Employee;
import eu.agileeng.domain.document.AEDocument;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.util.AEDateUtil;

/**
 * @author vvatov
 *
 */
public class AESocialDocument extends AEDocument {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1429177756066422447L;

	static public class JSONKey {
		public static final String employee = "employee";
		public static final String regDate = "regDate";
		public static final String modifyingId = "modifyingId";
		public static final String unconditionalInsert = "unconditionalInsert";
	}
	
	private Employee employee; // Salarie
	
	private AEDescriptive modifyingDoc;
	
	private boolean unconditionalInsert;
	
	/**
	 * no arg constructor.
	 * Try to use it only during DB Fetching
	 */
	public AESocialDocument() {
		super(AEDocumentType.valueOf(AEDocumentType.System.SocialDocumet));
	}
	
	public AESocialDocument(AEDocumentType docType) {
		super(docType);
	}
	
	/**
	 * @return the employee
	 */
	public Employee getEmployee() {
		return employee;
	}

	/**
	 * @param employee the employee to set
	 */
	public void setEmployee(Employee employee) {
		this.employee = employee;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);

		//{name : 'employee'}, // type : object
		if(jsonObject.has(AESocialDocument.JSONKey.employee)) {
			this.employee = new Employee();
			this.employee.create(jsonObject.getJSONObject(AESocialDocument.JSONKey.employee));
		}
		
		if(jsonObject.has(AESocialDocument.JSONKey.regDate)) {
			setRegDate(AEDateUtil.parseDateStrict(jsonObject.optString(AESocialDocument.JSONKey.regDate)));
		}
		
		if(jsonObject.has(AESocialDocument.JSONKey.modifyingId)) {
			long _modifyingId = jsonObject.optLong(AESocialDocument.JSONKey.modifyingId);
			if(_modifyingId > 0) {
				setModifyingDoc(new AEDescriptorImp(_modifyingId, DomainClass.AeDocument));
			}
		}
		
		if(jsonObject.has(AESocialDocument.JSONKey.unconditionalInsert)) {
			setUnconditionalInsert(jsonObject.optBoolean(AESocialDocument.JSONKey.unconditionalInsert));
		}
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		if(this.employee != null) {
			json.put(AESocialDocument.JSONKey.employee, this.employee.toJSONObject());
		}
		
		if(getRegDate() != null) {
			json.put(AESocialDocument.JSONKey.regDate, AEDateUtil.formatToSystem(getRegDate()));
		}
		
		if(getModifyingDoc() != null) {
			json.put(AESocialDocument.JSONKey.modifyingId, getModifyingDoc().getDescriptor().getID());
		}
		
		return json;
	}

	public AEDescriptive getModifyingDoc() {
		return modifyingDoc;
	}

	public void setModifyingDoc(AEDescriptive modifyingDoc) {
		this.modifyingDoc = modifyingDoc;
	}

	public boolean isUnconditionalInsert() {
		return unconditionalInsert;
	}

	public void setUnconditionalInsert(boolean unconditionalInsert) {
		this.unconditionalInsert = unconditionalInsert;
	}
}
