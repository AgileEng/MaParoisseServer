package eu.agileeng.domain.cefra.n11580_03;

import java.util.Date;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;

public class CouncilRequest extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 844265889335910307L;
	
	private int year;
	
	private Date date;
	
	public CouncilRequest() {
		super(DomainClass.TRANSIENT);
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
}
