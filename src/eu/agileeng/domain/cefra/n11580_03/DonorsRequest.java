package eu.agileeng.domain.cefra.n11580_03;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;

public class DonorsRequest extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3787321001126686424L;
	
	private long year;

	public DonorsRequest() {
		super(DomainClass.TRANSIENT);
	}

	/**
	 * @return the year
	 */
	public long getYear() {
		return year;
	}

	/**
	 * @param year the year to set
	 */
	public void setYear(long year) {
		this.year = year;
	}
}
