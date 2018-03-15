package eu.agileeng.domain.cefra.n11580_03;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;

public class BordereauParoisseRequest extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5164752520281049343L;
	
	private int year;
	
	private String docNumber;
	
	public BordereauParoisseRequest() {
		super(DomainClass.TRANSIENT);
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

}
