package eu.agileeng.domain.acc;


public class AccAccountBalanceExt extends AccAccountBalance {
	/**
	 * 
	 */
	private static final long serialVersionUID = -566168537148460382L;

	private double debitOpeningBalance;
	
	private double creditOpeningBalance;
	
	private double debitFinalBalance;
	
	private double creditFinalBalance;
	
	/**
	 * @return the debitFinalBalance
	 */
	public double getDebitFinalBalance() {
		return debitFinalBalance;
	}

	/**
	 * @param debitFinalBalance the debitFinalBalance to set
	 */
	public void setDebitFinalBalance(double debitFinalBalance) {
		this.debitFinalBalance = debitFinalBalance;
	}

	/**
	 * @return the creditFinalBalance
	 */
	public double getCreditFinalBalance() {
		return creditFinalBalance;
	}

	/**
	 * @param creditFinalBalance the creditFinalBalance to set
	 */
	public void setCreditFinalBalance(double creditFinalBalance) {
		this.creditFinalBalance = creditFinalBalance;
	}

	/**
	 * @return the debitOpeningBalance
	 */
	public double getDebitOpeningBalance() {
		return debitOpeningBalance;
	}

	/**
	 * @param debitOpeningBalance the debitOpeningBalance to set
	 */
	public void setDebitOpeningBalance(double debitOpeningBalance) {
		this.debitOpeningBalance = debitOpeningBalance;
	}

	/**
	 * @return the creditOpeningBalance
	 */
	public double getCreditOpeningBalance() {
		return creditOpeningBalance;
	}

	/**
	 * @param creditOpeningBalance the creditOpeningBalance to set
	 */
	public void setCreditOpeningBalance(double creditOpeningBalance) {
		this.creditOpeningBalance = creditOpeningBalance;
	}
	
	@Override
	public void calculateFinalBalance() {
		setOpeningBalance(debitOpeningBalance - creditOpeningBalance);
		
		double debitFinalBalance = getDebitOpeningBalance() + getDebitTurnover();
		double creditFinalBalance = getCreditOpeningBalance() + getCreditTurnover();
		if(debitFinalBalance > creditFinalBalance) {
			setDebitFinalBalance(debitFinalBalance - creditFinalBalance);
			setCreditFinalBalance(0.0);
		} else if(debitFinalBalance == creditFinalBalance) {
			setDebitFinalBalance(0.0);
			setCreditFinalBalance(0.0);
		} else {
			setDebitFinalBalance(0.0);
			setCreditFinalBalance(creditFinalBalance - debitFinalBalance);
		}
		
		setFinalBalance(debitFinalBalance - creditFinalBalance);
	}
}
