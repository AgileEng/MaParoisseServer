package eu.agileeng.domain.cefra.n11580_03;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.util.AETimePeriod;

public class GrandLivreRequest extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6579647722481351743L;

	private AETimePeriod period;
	
	private String fromAccountCode;
	
	private String toAccountCode;
	
	public GrandLivreRequest() {
		super(DomainClass.TRANSIENT);
	}
	
	
	/**
	 * @return the period
	 */
	public AETimePeriod getPeriod() {
		return period;
	}

	/**
	 * @param period the period to set
	 */
	public void setPeriod(AETimePeriod period) {
		this.period = period;
	}


	/**
	 * @return the fromAccountCode
	 */
	public String getFromAccountCode() {
		return fromAccountCode;
	}


	/**
	 * @param fromAccountCode the fromAccountCode to set
	 */
	public void setFromAccountCode(String fromAccountCode) {
		this.fromAccountCode = fromAccountCode;
	}


	/**
	 * @return the toAccountCode
	 */
	public String getToAccountCode() {
		return toAccountCode;
	}


	/**
	 * @param toAccountCode the toAccountCode to set
	 */
	public void setToAccountCode(String toAccountCode) {
		this.toAccountCode = toAccountCode;
	}
}
