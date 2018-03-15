package eu.agileeng.domain.cefra.n11580_03;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.util.AETimePeriod;

public class BilanRequest extends AEDomainObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5687257560622982327L;
	
	private AETimePeriod period;
	
	public BilanRequest() {
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
}
