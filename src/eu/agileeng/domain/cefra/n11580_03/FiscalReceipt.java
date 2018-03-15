package eu.agileeng.domain.cefra.n11580_03;

import java.util.Date;
import java.util.List;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.contact.ContributorDonation;

public class FiscalReceipt {
	private AEDescriptor personDescr;
	
	private double amount;
	
	private String number;
	
	private List<ContributorDonation> donations;
	
	private Date dateTo;
	
	private String nature;

	/**
	 * @return the personDescr
	 */
	public AEDescriptor getPersonDescr() {
		return personDescr;
	}

	/**
	 * @param personDescr the personDescr to set
	 */
	public void setPersonDescr(AEDescriptor personDescr) {
		this.personDescr = personDescr;
	}

	/**
	 * @return the amount
	 */
	public double getAmount() {
		return amount;
	}

	/**
	 * @param amount the amount to set
	 */
	public void setAmount(double amount) {
		this.amount = amount;
	}

	/**
	 * @return the number
	 */
	public String getNumber() {
		return number;
	}

	/**
	 * @param number the number to set
	 */
	public void setNumber(String number) {
		this.number = number;
	}

	/**
	 * @return the donations
	 */
	public List<ContributorDonation> getDonations() {
		return donations;
	}

	/**
	 * @param donations the donations to set
	 */
	public void setDonations(List<ContributorDonation> donations) {
		this.donations = donations;
	}

    public ContributorDonation getContributorDonationOne() {
    	ContributorDonation res = null;
    	if(this.donations != null && this.donations.size() == 1) {
    		res = this.donations.get(0);
    	}
    	return res;
    }

	/**
	 * @return the dateTo
	 */
	public Date getDateTo() {
		return dateTo;
	}

	/**
	 * @param dateTo the dateTo to set
	 */
	public void setDateTo(Date dateTo) {
		this.dateTo = dateTo;
	}

	/**
	 * @return the nature
	 */
	public String getNature() {
		return nature;
	}

	/**
	 * @param nature the nature to set
	 */
	public void setNature(String nature) {
		this.nature = nature;
	}
}
