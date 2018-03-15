package eu.agileeng.domain.social;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.util.AEMath;

public class SalaryGrid extends AEDomainObject {

	private AEDescriptive accHouse;
	
	private int year;
	
	private SalaryGridItemsList sgItems;
	
	private boolean active;
	
	private Double rateMaxExtraHours;
	
	public SalaryGrid() {
		super(DomainClass.SalaryGrid);
	}

	static public enum JSONKey {
		salaryGrids,
		salaryGrid,
		sgItems,
		accHouse,
		year,
		active,
		rateMaxExtraHours
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3493516933267799460L;

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		// items
		if(getSgItems() != null) {
			json.put(JSONKey.sgItems.toString(), getSgItems().toJSONArray());
		}
		
		// accHouse
		if(getAccHouse() != null) {
			json.put(JSONKey.accHouse.toString(), getAccHouse().getDescriptor().getName());
		}
		
		// year
		json.put(JSONKey.year.toString(), getYear());
		
		// active
		json.put(JSONKey.active.toString(), isActive());
		
		// rateMaxExtraHours
		if(getRateMaxExtraHours() != null) {
			json.put(JSONKey.rateMaxExtraHours.toString(), getRateMaxExtraHours());
		}
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// sgItems
		if(jsonObject.has(JSONKey.sgItems.toString())) {
			this.sgItems = new SalaryGridItemsList();
			sgItems.create(jsonObject.getJSONArray(JSONKey.sgItems.toString()));
		}
		
		// accHouse
		if(jsonObject.has(JSONKey.accHouse.toString())) {
			setAccHouse(lazyAccHouseDescriptor(jsonObject.optString(JSONKey.accHouse.toString())));
		}
		
		// year
		if(jsonObject.has(JSONKey.year.toString())) {
			setYear(jsonObject.optInt(JSONKey.year.toString()));
		}
		
		// active
		if(jsonObject.has(JSONKey.active.toString())) {
			setActive(jsonObject.optBoolean(JSONKey.active.toString()));
		}
		
		// rateMaxExtraHours
		if(jsonObject.has(JSONKey.rateMaxExtraHours.toString())) {
			try {
				Number rate = AEMath.parseNumber(
						jsonObject.getString(JSONKey.rateMaxExtraHours.toString()), true);
				if(rate != null) {
					setRateMaxExtraHours(rate.doubleValue());
				}
			} catch (AEException e) {
			}
		}
	}

	public SalaryGridItemsList getSgItems() {
		return sgItems;
	}

	public void setSgItems(SalaryGridItemsList sgItems) {
		this.sgItems = sgItems;
	}

	public AEDescriptive getAccHouse() {
		return accHouse;
	}

	public void setAccHouse(AEDescriptive accHouse) {
		this.accHouse = accHouse;
	}
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.SalaryGrid);
	}
	
	public static AEDescriptor lazyAccHouseDescriptor(String name) {
		AEDescriptorImp descr = new AEDescriptorImp();
		descr.setName(name);
		return descr;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Double getRateMaxExtraHours() {
		return rateMaxExtraHours;
	}

	public void setRateMaxExtraHours(Double rateMaxExtraHours) {
		this.rateMaxExtraHours = rateMaxExtraHours;
	}
}
