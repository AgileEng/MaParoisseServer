package eu.agileeng.domain.facturation;

import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;

public class AEPaymentView {
	private AEPayment p = null;
	public AEPaymentView(AEPayment p) {
		this.p = p;
	}
	
	public String getDate() {
		String date = AEStringUtil.EMPTY_STRING;
		if (p.getDate() != null) {
			date = AEDateUtil.convertToString(p.getDate(), AEDateUtil.FRENCH_DATE_FORMAT);
		}
		return date;
	}
	
	public String getFactureNumber() {
		String number = AEStringUtil.EMPTY_STRING;
		if (p.getToFacture() instanceof AEFactureDescr) {
			AEFactureDescr fd = (AEFactureDescr) p.getToFacture();
			number = fd.getNumber();
		}
		return number;
	}
	
	public String getAmount() {
		String amount = AEStringUtil.EMPTY_STRING;
		if (this.p != null) {
			amount = AEMath.toAmountString(p.getAmount());
		}
		return amount;
	}
	
	public String getPayerName() {
		String pn = AEStringUtil.EMPTY_STRING;
		if(this.p != null && this.p.getPayer() != null) {
			pn = this.p.getPayer().getDescriptor().getName();
		}
		return pn;
	}
	
	public String getDueDate() {
		String dueDate = AEStringUtil.EMPTY_STRING;
		if (p.getDueDate() != null) {
			dueDate = AEDateUtil.convertToString(p.getDueDate(), AEDateUtil.FRENCH_DATE_FORMAT);
		}
		return dueDate;
	}
}
