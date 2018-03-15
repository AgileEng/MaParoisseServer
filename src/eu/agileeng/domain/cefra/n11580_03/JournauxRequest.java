package eu.agileeng.domain.cefra.n11580_03;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.util.AETimePeriod;

public class JournauxRequest extends AEDomainObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6242971594061922863L;
	/**
	 * 
	 */
	private AETimePeriod period;
	
	private AEDescriptor accJournal;
	
	public JournauxRequest() {
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
	 * @return the accJournal
	 */
	public AEDescriptor getAccJournal() {
		return accJournal;
	}

	/**
	 * @param accJournal the accJournal to set
	 */
	public void setAccJournal(AEDescriptor accJournal) {
		this.accJournal = accJournal;
	}
}
