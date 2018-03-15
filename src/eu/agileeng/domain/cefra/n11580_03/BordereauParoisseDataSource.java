package eu.agileeng.domain.cefra.n11580_03;

import eu.agileeng.domain.AccAccountBalancesList;
import eu.agileeng.domain.acc.cashbasis.Quete;
import eu.agileeng.domain.acc.cashbasis.QuetesList;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;


public class BordereauParoisseDataSource extends ReportDataSource {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1574735963738666884L;
	
	private QuetesList quetesList;
	
	private AccAccountBalancesList accAccountBalances;
	
	public BordereauParoisseDataSource() {
	}
	
	public String getSumAsString(String queteCode) {
		String ret = null;
		if(quetesList != null && !quetesList.isEmpty() && !AEStringUtil.isEmpty(queteCode)) {
			for (Quete quete : quetesList) {
				if(queteCode.equals(quete.getCode())) {
					double amount = quete.getAmount();
					if(AEMath.isZeroAmount(amount)){
						ret = AEStringUtil.EMPTY_STRING;
					} else {
						ret = AEMath.toAmountFrenchString(AEMath.round(amount, 2));
					}
					break;
				}
			}
		}
		return AEStringUtil.trim(ret);
	}
	
	public String getSumTotalAsString() {
		String ret = null;
		double sum = 0.0;
		if(quetesList != null && !quetesList.isEmpty()) {
			for (Quete quete : quetesList) {
				sum += AEMath.round(quete.getAmount(), 2);
			}
		}
		if(AEMath.isZeroAmount(sum)) {
			ret = AEStringUtil.EMPTY_STRING;
		} else {
			ret = AEMath.toAmountFrenchString(AEMath.round(sum, 2));
		}
		return AEStringUtil.trim(ret);
	}

	/**
	 * @param quetesList the quetesList to set
	 */
	public void setQuetesList(QuetesList quetesList) {
		this.quetesList = quetesList;
	}

	/**
	 * @return the accAccountBalances
	 */
	public AccAccountBalancesList getAccAccountBalances() {
		return accAccountBalances;
	}

	/**
	 * @param accAccountBalances the accAccountBalances to set
	 */
	public void setAccAccountBalances(AccAccountBalancesList accAccountBalances) {
		this.accAccountBalances = accAccountBalances;
	}

}
