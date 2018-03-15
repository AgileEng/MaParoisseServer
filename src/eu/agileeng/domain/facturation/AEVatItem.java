package eu.agileeng.domain.facturation;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.json.JSONUtil;

public class AEVatItem extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8484687457251529864L;

	static public class JSONKey {
		public static final String vatItem = "vatItem";
		
		public static final String taxableAmount = "taxableAmount";
		public static final String vatAmount = "vatAmount";
		public static final String rate = "rate";
		public static final String amount = "amount";
		public static final String category = "category";
	}
	
	private double taxableAmount;
	
	private double rate;
	
	private double vatAmount;
	
	private double amount;
	
	private String category;
	
	public AEVatItem() {
		super(DomainClass.VATItem);
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();

		// public static final String rate = "rate";
		json.put(JSONKey.rate, AEMath.toAmountString(getRate()));
		
		// public static final String taxableAmount = "taxableAmount";
		json.put(JSONKey.taxableAmount, AEMath.toAmountString(getTaxableAmount()));
		
		// public static final String vatAmount = "vatAmount";
		json.put(JSONKey.vatAmount, AEMath.toAmountString(getVatAmount()));

		// public static final String amount = "amount";
		json.put(JSONKey.amount, AEMath.toAmountString(getAmount()));
		
		// public static final String category = "category";
		json.put(JSONKey.category, getCategory());
		
		// option
		json.put("option", "(" + getCode() + ") " + getRate() + "%");
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);

		// public static final String rate = "rate";
		try {
			setRate(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.rate));
		} catch (Exception e) {
		}
		
		// public static final String taxableAmount = "taxableAmount";
		try {
			setTaxableAmount(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.taxableAmount));
		} catch (Exception e) {
		}
		
		// public static final String vatAmount = "vatAmount";
		try {
			setVatAmount(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.vatAmount));
		} catch (Exception e) {
		}

		// public static final String amount = "amount";
		try {
			setAmount(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.amount));
		} catch (Exception e) {
		}
		
		// public static final String category = "category";
		setCategory(jsonObject.optString(JSONKey.category));
	}

	public double getTaxableAmount() {
		return taxableAmount;
	}

	public void setTaxableAmount(double taxableAmount) {
		this.taxableAmount = taxableAmount;
	}

	public double getRate() {
		return rate;
	}

	public void setRate(double rate) {
		this.rate = rate;
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public double getVatAmount() {
		return vatAmount;
	}

	public void setVatAmount(double vatAmount) {
		this.vatAmount = vatAmount;
	}
}
