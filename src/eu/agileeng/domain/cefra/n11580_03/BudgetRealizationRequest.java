package eu.agileeng.domain.cefra.n11580_03;

import eu.agileeng.domain.DomainModel.DomainClass;

public class BudgetRealizationRequest extends ReportRequest {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3719354481110090525L;
	
	private int year;
	
	/**
	 * Expenxe (6 account class) or Income (7 account class)
	 */
	private String accountClass;
	
	public BudgetRealizationRequest() {
		super(DomainClass.TRANSIENT);
	}

	/**
	 * @return the accountClass
	 */
	public String getAccountClass() {
		return accountClass;
	}

	/**
	 * @param accountClass the accountClass to set
	 */
	public void setAccountClass(String accountClass) {
		this.accountClass = accountClass;
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
