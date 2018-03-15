package eu.agileeng.domain.facturation;

import java.util.Comparator;
import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEPersistent;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.json.JSONUtil;

public class AEPayment extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3261360351785544299L;

	static public class JSONKey {
		public static final String payment = "payment";
		public static final String payments = "payments";
		public static final String facture = "facture"; // toFacture
		public static final String client = "client"; // payer
		public static final String paymentTermsId = "paymentTermsId";
		public static final String paymentTermsDescr = "paymentTermsDescr";
		public static final String payableType = "payableType";
		public static final String paymentType = "paymentType";
		public static final String date = "date";
		public static final String dueDate = "dueDate";
		public static final String amount = "amount";
		public static final String paid = "paid";
		public static final String paidDate = "paidDate";
	}
	
	/**
	 * The reference to the facture, the owner of this payment
	 */
	private AEDescriptive toFacture;
	
	/**
	 * The payer of this payment.
	 * Usually it is the facture's client.
	 */
	private AEDescriptive payer;
	
	/**
	 * The payment terms for this payment
	 */
	private AEDescriptive paymentTerms; // Mode de reglement
	
	/**
	 * The payable type.
	 * Schedule, Requisition etc.
	 */
	private AEFactureUtil.PayableType payableType;
	
	/**
	 * The payment type.
	 * Regular, Adnance etc.
	 * Advance payments should be processed specific.
	 */
	private AEFactureUtil.PaymentType paymentType;
	
	/**
	 * Registration date.
	 * The date when this payment should be accounted.
	 */
	private Date date;
	
	/**
	 * The due date when this payment should be paid.
	 */
	private Date dueDate;
	
	/**
	 * The amount of this payment
	 */
    private double amount;
    
    /**
     * Whether this payment is paid or not
     */
    private boolean paid;
    
    /**
     * When this payment has been paid
     */
    private Date paidDate;
	
	public AEPayment() {
		this(DomainClass.AEPayment);
	}
	
	protected AEPayment(DomainClass clazz) {
		super(clazz);
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();

		// public static final String facture = "facture"; // toFacture
		if(getToFacture() != null) {
			JSONObject factureJSON = getToFacture().toJSONObject();
			json.put(JSONKey.facture, factureJSON);
		}
		
		// public static final String client = "client"; // payer
		if(getPayer() != null) {
			JSONObject clientJSON = getPayer().toJSONObject();
			json.put(JSONKey.client, clientJSON);
		}
		
		// paymentTermsId
		if(getPaymentTerms() != null) {
			json.put(JSONKey.paymentTermsId, getPaymentTerms().getDescriptor().getID());
			json.put(JSONKey.paymentTermsDescr, getPaymentTerms().getDescriptor().getDescription());
		}
		
		// public static final String payableType = "payableType";
		if(getPayableType() != null) {
			json.put(JSONKey.payableType, getPayableType().getTypeId());
		}
		
		// public static final String paymentType = "paymentType";
		if(getPaymentType() != null) {
			json.put(JSONKey.paymentType, getPaymentType().getTypeId());
		}
		
		// public static final String date = "date";
		if(getDate() != null) {
			json.put(JSONKey.date, AEDateUtil.convertToString(getDate(), AEDateUtil.SYSTEM_DATE_FORMAT));
		}
		
		// public static final String dueDate = "dueDate";
		if(getDueDate() != null) {
			json.put(JSONKey.dueDate, AEDateUtil.convertToString(getDueDate(), AEDateUtil.SYSTEM_DATE_FORMAT));
		}
		
		// public static final String amount = "amount";
		json.put(JSONKey.amount, AEMath.toAmountString(getAmount()));
		
		// public static final String paid = "paid";
		json.put(JSONKey.paid, isPaid());
		
		// public static final String paidDate = "paidDate";
		if(getPaidDate() != null) {
			json.put(JSONKey.paidDate, AEDateUtil.convertToString(getPaidDate(), AEDateUtil.SYSTEM_DATE_FORMAT));
		}
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);

		// public static final String facture = "facture"; // toFacture
		if(jsonObject.has(JSONKey.facture)) {
			JSONObject factureJson = jsonObject.optJSONObject(JSONKey.facture);
			if(factureJson != null) {
				AEFacture facture = new AEFacture();
				facture.create(factureJson);
				setToFacture(facture);
			}
		}
		
		// public static final String client = "client"; // payer
		if(jsonObject.has(JSONKey.client)) {
			JSONObject clientJson = jsonObject.optJSONObject(JSONKey.client);
			if(clientJson != null) {
				AEClient client = new AEClient();
				client.create(clientJson);
				setPayer(client);
			}
		}
		
		// paymentTermsId
		long paymentTermsId = jsonObject.optLong(JSONKey.paymentTermsId);
		if(paymentTermsId > 0) {
			try {
				setPaymentTerms(AEFactureUtil.getPaymentTerms(paymentTermsId));
			} catch (Exception e) {
			}
		}
		
		// public static final String payableType = "payableType";
		setPayableType(AEFactureUtil.PayableType.valueOf(jsonObject.optLong(JSONKey.payableType)));
		
		// public static final String paymentType = "paymentType";
		setPaymentType(AEFactureUtil.PaymentType.valueOf(jsonObject.optLong(JSONKey.paymentType)));
		
		// public static final String date = "date";
		setDate(AEDateUtil.parseDateStrict(jsonObject.optString(JSONKey.date)));
		
		// public static final String dueDate = "dueDate";
		setDueDate(AEDateUtil.parseDateStrict(jsonObject.optString(JSONKey.dueDate)));
		
		// public static final String amount = "amount";
		try {
			setAmount(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.amount));
		} catch (Exception e) {
		}
		
		// public static final String paid = "paid";
		setPaid(jsonObject.optBoolean(JSONKey.paid));
		
		// public static final String paidDate = "paidDate";
		setPaidDate(AEDateUtil.parseDateStrict(jsonObject.optString(JSONKey.paidDate)));
	}
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.AEPayment);
	}

	public AEDescriptive getToFacture() {
		return toFacture;
	}

	public void setToFacture(AEDescriptive toFacture) {
		this.toFacture = toFacture;
	}

	public AEDescriptive getPayer() {
		return payer;
	}

	public void setPayer(AEDescriptive payer) {
		this.payer = payer;
	}

	public AEDescriptive getPaymentTerms() {
		return paymentTerms;
	}

	public void setPaymentTerms(AEDescriptive paymentTerms) {
		this.paymentTerms = paymentTerms;
	}

	public AEFactureUtil.PayableType getPayableType() {
		return payableType;
	}

	public void setPayableType(AEFactureUtil.PayableType payableType) {
		this.payableType = payableType;
	}

	public AEFactureUtil.PaymentType getPaymentType() {
		return paymentType;
	}

	public void setPaymentType(AEFactureUtil.PaymentType paymentType) {
		this.paymentType = paymentType;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Date getDueDate() {
		return dueDate;
	}

	public void setDueDate(Date dueDate) {
		this.dueDate = dueDate;
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	public boolean isPaid() {
		return paid;
	}

	public void setPaid(boolean paid) {
		this.paid = paid;
	}

	public Date getPaidDate() {
		return paidDate;
	}

	public void setPaidDate(Date paidDate) {
		this.paidDate = paidDate;
	}
	
	/**
	 * 
	 * @author vvatov
	 */
    public static class NPComparator implements Comparator<AEPayment> {
        public int compare(AEPayment p1, AEPayment p2) {
            int nRes = 0;
            // first comparision level - deleted (deleted at the end as unsorted)
            if(p1.isDeleted() && !p2.isDeleted()) {
                nRes = 1;
            } else if(!p1.isDeleted() && p2.isDeleted()) {
                nRes = -1;
            } else {
            	return p1.getPaymentType().compareTo(p2.paymentType);
            }
            return nRes;
        }
    }
    
    public boolean isDeleted() {
    	return getPersistentState() == AEPersistent.State.DELETED;
    }
}
