package eu.agileeng.domain.cefra.n11580_03;

import java.util.List;

import eu.agileeng.domain.contact.Address;
import eu.agileeng.domain.contact.Contributor;
import eu.agileeng.domain.contact.ContributorDonation;
import eu.agileeng.domain.contact.Employee;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AETimePeriod;

public class Cefra11580_03DataSource extends ReportDataSource {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8799578614498177421L;

	private Contributor contributor;

//	private Nature nature;
//
//	private PaymentMethod paymentMethod;

//	private String docNumber;

	private List<ContributorDonation> donations;
	
	private AETimePeriod period;
	
	private boolean payCashMethod = false;
	
	private boolean payOtherMethod = false;

	public Cefra11580_03DataSource() {
	}

	public String optMock() {
		return "Recu au titre des dons";
	}

//	/**
//	 * @return the nature
//	 */
//	public Nature getNature() {
//		return nature;
//	}
//
//	/**
//	 * @param nature the nature to set
//	 */
//	public void setNature(Nature nature) {
//		this.nature = nature;
//	}
//
//	/**
//	 * @return the paymentMethod
//	 */
//	public PaymentMethod getPaymentMethod() {
//		return paymentMethod;
//	}
//
//	/**
//	 * @param paymentMethod the paymentMethod to set
//	 */
//	public void setPaymentMethod(PaymentMethod paymentMethod) {
//		this.paymentMethod = paymentMethod;
//	}

	/**
	 * @return the contributor
	 */
	public Contributor getContributor() {
		return contributor;
	}

	/**
	 * @param contributor the contributor to set
	 */
	public void setContributor(Contributor contributor) {
		this.contributor = contributor;
	}

	/**
	 * @return the amount
	 */
	public double getAmount() {
		double amount = 0.0;
		if(donations != null) {
			for (ContributorDonation donation : donations) {
				amount += (donation.amount - donation.getAmountReceipted());
			}
		}
		return amount;
	}

	public int getCount() {
		int count = 0;
		if(donations != null) {
			count = donations.size();
		}
		return count;
	}

	public String[] amountInWords() {
		return AEMath.spelloutMoney(AEMath.round(getAmount(), 2));
	}

	public String amountAsString() {
		return AEMath.toAmountFrenchString(AEMath.round(getAmount(), 2));
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
	
	public String dateToAsString() {
		String ret = null;
		if(period != null && period.getEndDate() != null) {
			ret = AEDateUtil.formatToFrench(period.getEndDate());
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getContributorName() {
		String ret = null;
		if(contributor != null && contributor.getEmployee() != null) {
			Employee empl = contributor.getEmployee();
			empl.createName();
			ret = empl.getName();
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getContributorAddress() {
		String ret = null;
		if(contributor != null && contributor.getEmployee() != null) {
			Employee empl = contributor.getEmployee();
			if(empl.getAddress() != null) {
				Address addr = empl.getAddress();
				ret = addr.getStreet();
			}
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getContributorPostCode() {
		String ret = null;
		if(contributor != null && contributor.getEmployee() != null) {
			Employee empl = contributor.getEmployee();
			if(empl.getAddress() != null) {
				Address addr = empl.getAddress();
				ret = addr.getPostalCode();
			}
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getContributorCity() {
		String ret = null;
		if(contributor != null && contributor.getEmployee() != null) {
			Employee empl = contributor.getEmployee();
			if(empl.getAddress() != null) {
				Address addr = empl.getAddress();
				ret = addr.getCity();
			}
		}
		return AEStringUtil.trim(ret);
	}
	
	/**
	 * 
	 * @return The date of the donation(s)
	 */
	public String getDonationDate() {
		String ret = null;
		if(donations != null && donations.size() == 1) {
			ContributorDonation donation = donations.get(0);
			if(donation != null && donation.date != null) {
				ret = AEDateUtil.formatToFrench(donation.date);
			}
		} 
		if(ret == null || (donations != null && donations.size() > 1)) {
			// BugFix 20160105
			// Get the donation date from year, not from period
//			if(period != null && period.getStartDate() != null) {
//				ret = Integer.toString(AEDateUtil.getYear(period.getStartDate()));
//			}
			ret = Integer.toString(getYear());
		}
		return AEStringUtil.trim(ret);
	}

	/**
	 * @return the payCashMethod
	 */
	public boolean isPayCashMethod() {
		return payCashMethod;
	}

	/**
	 * @param payCashMethod the payCashMethod to set
	 */
	public void setPayCashMethod(boolean payCashMethod) {
		this.payCashMethod = payCashMethod;
	}

	/**
	 * @return the payOtherMethod
	 */
	public boolean isPayOtherMethod() {
		return payOtherMethod;
	}

	/**
	 * @param payOtherMethod the payOtherMethod to set
	 */
	public void setPayOtherMethod(boolean payOtherMethod) {
		this.payOtherMethod = payOtherMethod;
	}
}
