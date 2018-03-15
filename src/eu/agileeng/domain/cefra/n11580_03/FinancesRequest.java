package eu.agileeng.domain.cefra.n11580_03;

import java.util.Date;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;

public class FinancesRequest extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7429129722878878139L;
	
	private int year;
	
	private Date date;
	
	public FinancesRequest() {
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

}
