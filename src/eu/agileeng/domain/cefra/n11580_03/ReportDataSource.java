package eu.agileeng.domain.cefra.n11580_03;

import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.contact.Address;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;

public class ReportDataSource extends AEDomainObject {

	public static final String PAROISSE = "PAROISSE";
	
	public static final String MENSE = "MENSE";
	
	private static int MIN_CODE_FOR_MENSE = 1500;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6116067480533712855L;
	
	private JSONObject customer;
	
	private int year;
	
	private String docNumber = AEStringUtil.EMPTY_STRING;
	
	public ReportDataSource() {
		super(DomainClass.TRANSIENT);
	}
	
	/**
	 * @return the customer
	 */
	public JSONObject getCustomer() {
		return customer;
	}

	/**
	 * @param customer the customer to set
	 */
	public void setCustomer(JSONObject customer) {
		this.customer = customer;
	}
	
	public String getCustomerName() {
		String ret = null;
		if(customer != null) {
			ret = customer.optString(AEDomainObject.JSONKey.name.name());
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getCustomerCode() {
		String ret = null;
		if(customer != null) {
			ret = customer.optString(AEDomainObject.JSONKey.code.name());
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getCustomerAddress() {
		String ret = null;
		if(customer != null) {
			ret = customer.optString(Address.key_address);
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getCustomerPostCode() {
		String ret = null;
		if(customer != null) {
			ret = customer.optString(Address.key_postCode);
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getCustomerCity() {
		String ret = null;
		if(customer != null) {
			ret = customer.optString(Address.key_town);
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getCustomerStatut() {
		String ret = null;
		if(customer != null) {
			ret = customer.optString(Organization.JSONKey.paroisseStatut);
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getCustomerNature() {
		String ret = null;
		if(customer != null) {
			ret = customer.optString(AEDomainObject.JSONKey.description.name());
		}
		return AEStringUtil.trim(ret);
	}
	
	/**
	 * The customer is either “PAROISSE” or “MENSE”,  based on the Parish's code
	 *   - if code < 1000 then “PAROISSE”; 
	 *   - if code >= 1000 then “MENSE”
	 * @return
	 */
	public boolean isCustomerParoisse() {
		boolean bRes = true;
		
		if(customer != null) {
			String code = customer.optString(AEDomainObject.JSONKey.code.name());
			try {
				int codeInt = Integer.parseInt(code);
				bRes = codeInt < MIN_CODE_FOR_MENSE ? true : false;
				if (codeInt >= MIN_CODE_FOR_MENSE) {
					bRes = false;
				}
			} catch (Exception e) {
				// nothing to do
			}
		}
		
		return bRes;
	}
	
	public String getCustomerDoyenne() {
		String ret = null;
		if(customer != null) {
			ret = customer.optString(Organization.JSONKey.paroisseDoyenne);
		}
		return AEStringUtil.trim(ret);
	}

	/**
	 * @return the year
	 */
	public int getYear() {
		return year;
	}

	/**
	 * @param year the year to set
	 */
	public void setYear(int year) {
		this.year = year;
	}
	
	public String getYearAsString() {
		return Integer.toString(this.year);
	}

	/**
	 * @return the docNumber
	 */
	public String getDocNumber() {
		return AEStringUtil.trim(docNumber);
	}

	/**
	 * @param docNumber the docNumber to set
	 */
	public void setDocNumber(String docNumber) {
		this.docNumber = docNumber;
	}
	
	public String toString(double d) {
		String ret = null;
		if(!AEMath.isZeroAmount(d)) {
			ret = AEMath.toAmountFrenchString(d);
		}
		return AEStringUtil.trim(ret);
	}
}
