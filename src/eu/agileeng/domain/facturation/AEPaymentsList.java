package eu.agileeng.domain.facturation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.AEList;
import eu.agileeng.util.AEValidatable;
import eu.agileeng.util.AEValidator;

public class AEPaymentsList extends ArrayList<AEPayment> implements AEList, AEValidatable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9060948390651934248L;
	
	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArray = new JSONArray();

		if(!this.isEmpty()) {
			for (AEPayment payment : this) {
				jsonArray.put(payment.toJSONObject());
			}
		}
		
		return jsonArray;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = (JSONObject) jsonArray.get(i);
			
			AEPayment payment = new AEPayment();
			payment.create(jsonItem);
			
			add(payment);
		}
	}
	
	public AEPayment getAdvancePayment() {
		AEPayment found = null;
		for (Iterator<AEPayment> iterator = this.iterator(); iterator.hasNext();) {
			AEPayment payment = (AEPayment) iterator.next();
			if(payment.getPaymentType() == AEFactureUtil.PaymentType.ADVANCE) {
				found = payment;
				break;
			}
		}
		return found;
	}
	
	public AEPayment getBalancePayment() {
		AEPayment found = null;
		for (Iterator<AEPayment> iterator = this.iterator(); iterator.hasNext();) {
			AEPayment payment = (AEPayment) iterator.next();
			if(payment.getPaymentType() == AEFactureUtil.PaymentType.BALANCE) {
				found = payment;
				break;
			}
		}
		return found;
	}
	
	public AEPaymentsList getRegularPayments() {
		AEPaymentsList found = new AEPaymentsList();
		for (Iterator<AEPayment> iterator = this.iterator(); iterator.hasNext();) {
			AEPayment payment = (AEPayment) iterator.next();
			if(AEFactureUtil.PaymentType.REGULAR.equals(payment.getPaymentType())) {
				found.add(payment);
			}
		}
		return found;
	}
	
	/**
	 * Distributes specified <code>amount</code> over this paymnent list.
	 * 
	 * @param amount The amount to be distributed
	 */
    public void distributeAmount(double amount) throws AEException {
        // sort
        Collections.sort(this, new AEPayment.NPComparator());
        
        // distribute
        double distributedAmount = 0.0;
        for (Iterator<AEPayment> iter = this.getDistributablePayments().iterator(); iter.hasNext();) {
            AEPayment payment = (AEPayment) iter.next();
            distributedAmount += payment.getAmount();
        }
        
        // balance
        AEPayment balancePayment = getBalancePayment();
        if(balancePayment == null) {
        	throw new AEException("System error: Balance payment is not defined");
        }
        balancePayment.setAmount(amount - distributedAmount);
        balancePayment.setUpdated();
    }
    
    private AEPaymentsList getDistributablePayments() {
    	AEPaymentsList pList = new AEPaymentsList();
    	for (AEPayment payment : this) {
			if(!payment.isDeleted() && payment.getPaymentType() != AEFactureUtil.PaymentType.BALANCE) {
				pList.add(payment);
			}
		}
    	return pList;
    }

	@Override
	public void validateWith(AEValidator validator) throws AEException {
		// TODO Auto-generated method stub
		
	}
}
