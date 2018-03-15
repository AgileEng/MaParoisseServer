package eu.agileeng.domain.document.social.rib;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.social.AESocialDocument;

public class Rib extends AESocialDocument {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2263856668530759390L;

	static public enum JSONKey {
		bankName,
		address,
		contactName,
		contactPhone,
		contactEMail,
		webSite,
		iban,
		rib,
		customerNo,
		bic
	}
	
	private String bankName;
	private String address;
	private String contactName;
	private String contactPhone;
	private String contactEMail;
	private String webSite;
	private String iban;
	private String rib;
	private String customerNo;
	private String bic;
	
	/**
	 * no arg constructor.
	 * Try to use it only during DB Fetching
	 */
	public Rib() {
		super(AEDocumentType.valueOf(AEDocumentType.System.Rib));
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();

		if(getBankName() != null) {
			json.put(JSONKey.bankName.toString(), getBankName());
		}
		
		if(getAddress() != null) {
			json.put(JSONKey.address.toString(), getAddress());
		}
		
		if(getContactName() != null) {
			json.put(JSONKey.contactName.toString(), getContactName());
		}
		
		if(getContactPhone() != null) {
			json.put(JSONKey.contactPhone.toString(), getContactPhone());
		}
		
		if(getContactEMail() != null) {
			json.put(JSONKey.contactEMail.toString(), getContactEMail());
		}
		
		if(getWebSite() != null) {
			json.put(JSONKey.webSite.toString(), getWebSite());
		}
		
		if(getIban() != null) {
			json.put(JSONKey.iban.toString(), getIban());
		}
		
		if(getRib() != null) {
			json.put(JSONKey.rib.toString(), getRib());
		}
		
		if(getCustomerNo() != null) {
			json.put(JSONKey.customerNo.toString(), getCustomerNo());
		}
		
		if(getBic() != null) {
			json.put(JSONKey.bic.toString(), getBic());
		}
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		if(jsonObject.has(JSONKey.bankName.toString())) {
			setBankName(jsonObject.optString(JSONKey.bankName.toString()));
		}
		
		if(jsonObject.has(JSONKey.address.toString())) {
			setAddress(jsonObject.optString(JSONKey.address.toString()));
		}
		
		if(jsonObject.has(JSONKey.contactName.toString())) {
			setContactName(jsonObject.optString(JSONKey.contactName.toString()));
		}
		
		if(jsonObject.has(JSONKey.contactPhone.toString())) {
			setContactPhone(jsonObject.optString(JSONKey.contactPhone.toString()));
		}
		
		if(jsonObject.has(JSONKey.contactEMail.toString())) {
			setContactEMail(jsonObject.optString(JSONKey.contactEMail.toString()));
		}
		
		if(jsonObject.has(JSONKey.webSite.toString())) {
			setWebSite(jsonObject.optString(JSONKey.webSite.toString()));
		}
		
		if(jsonObject.has(JSONKey.iban.toString())) {
			setIban(jsonObject.optString(JSONKey.iban.toString()));
		}
		
		if(jsonObject.has(JSONKey.rib.toString())) {
			setRib(jsonObject.optString(JSONKey.rib.toString()));
		}
		
		if(jsonObject.has(JSONKey.customerNo.toString())) {
			setCustomerNo(jsonObject.optString(JSONKey.customerNo.toString()));
		}
		
		if(jsonObject.has(JSONKey.bic.toString())) {
			setBic(jsonObject.optString(JSONKey.bic.toString()));
		}
	}

	public String getBankName() {
		return bankName;
	}

	public void setBankName(String bankName) {
		this.bankName = bankName;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getContactName() {
		return contactName;
	}

	public void setContactName(String contactName) {
		this.contactName = contactName;
	}

	public String getContactPhone() {
		return contactPhone;
	}

	public void setContactPhone(String contactPhone) {
		this.contactPhone = contactPhone;
	}

	public String getContactEMail() {
		return contactEMail;
	}

	public void setContactEMail(String contactEMail) {
		this.contactEMail = contactEMail;
	}

	public String getWebSite() {
		return webSite;
	}

	public void setWebSite(String webSite) {
		this.webSite = webSite;
	}

	public String getIban() {
		return iban;
	}

	public void setIban(String iban) {
		this.iban = iban;
	}

	public String getRib() {
		return rib;
	}

	public void setRib(String rib) {
		this.rib = rib;
	}

	public String getCustomerNo() {
		return customerNo;
	}

	public void setCustomerNo(String customerNo) {
		this.customerNo = customerNo;
	}

	public String getBic() {
		return bic;
	}

	public void setBic(String bic) {
		this.bic = bic;
	}
}
