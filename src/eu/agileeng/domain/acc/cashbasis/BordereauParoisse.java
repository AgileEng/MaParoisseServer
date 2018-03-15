package eu.agileeng.domain.acc.cashbasis;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.json.JSONUtil;

public class BordereauParoisse extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 765967418239400446L;
	
	static public enum JSONKey {
		accAmount,
		paidAmount,
		toPayAmount,
		currAmount,
		quetes;
	}
	
	public BordereauParoisse() {
		super(DomainClass.BordereauParoisse);
	}
	
	// amount by accountancy
	private double accAmount;
	
	// amount paid
	private double paidAmount;
	
	// amount to pay
	private double toPayAmount;
	
	// cuurently amount
	private double currAmount;
	
	// for year
	private int year;

	/**
	 * @return the accAmount
	 */
	public double getAccAmount() {
		return accAmount;
	}

	/**
	 * @param accAmount the accAmount to set
	 */
	public void setAccAmount(double accAmount) {
		this.accAmount = accAmount;
	}

	/**
	 * @return the paidAmount
	 */
	public double getPaidAmount() {
		return paidAmount;
	}

	/**
	 * @param paidAmount the paidAmount to set
	 */
	public void setPaidAmount(double paidAmount) {
		this.paidAmount = paidAmount;
	}

	/**
	 * @return the toPayAmount
	 */
	public double getToPayAmount() {
		return toPayAmount;
	}

	/**
	 * @param toPayAmount the toPayAmount to set
	 */
	public void setToPayAmount(double toPayAmount) {
		this.toPayAmount = toPayAmount;
	}

	/**
	 * @return the currAmount
	 */
	public double getCurrAmount() {
		return currAmount;
	}

	/**
	 * @param currAmount the currAmount to set
	 */
	public void setCurrAmount(double currAmount) {
		this.currAmount = currAmount;
	}

	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject jsonObject = super.toJSONObject();
		
		// accAmount
		jsonObject.put(JSONKey.accAmount.name(), getAccAmount());
		
		// paidAmount
		jsonObject.put(JSONKey.paidAmount.name(), getPaidAmount());
		
		// toPayAmount
		jsonObject.put(JSONKey.toPayAmount.name(), getToPayAmount());
		
		// currAmount
		jsonObject.put(JSONKey.currAmount.name(), getCurrAmount());
		
		return jsonObject;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// accAmount
		try {
			setAccAmount(AEMath.doubleValue(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.accAmount.name())));
		} catch(AEException e) {
			
		}
		
		// paidAmount
		try {
			setPaidAmount(AEMath.doubleValue(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.paidAmount.name())));
		} catch(AEException e) {
			
		}
		
		// toPayAmount
		try {
			setToPayAmount(AEMath.doubleValue(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.toPayAmount.name())));
		} catch(AEException e) {
			
		}
		
		// currAmount
		try {
			setCurrAmount(AEMath.doubleValue(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.currAmount.name())));
		} catch(AEException e) {
			
		}
	}
	
	public void calculateToPayAmount() {
		this.toPayAmount = this.accAmount - this.paidAmount;
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
