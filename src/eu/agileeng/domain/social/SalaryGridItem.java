package eu.agileeng.domain.social;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.util.AEStringUtil;

public class SalaryGridItem extends AEDomainObject {

	private AEDescriptive salaryGrid;
	
	private String niveau;
	
	/**
	 * 
	 */
	private String echelon;
	
	private Double coefficient;
	
	private Double pointValue;
	
	private Double salary;
	
	static public enum JSONKey {
		niveau,
		echelon,
		coefficient,
		pointValue,
		salary
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -117706397184492191L;

	public SalaryGridItem() {
		super(DomainClass.SalaryGridItem);
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		// niveau
		if(getNiveau() != null) {
			json.put(JSONKey.niveau.toString(), getNiveau());
		}
		
		// echelon
		if(!AEStringUtil.isEmpty(getEchelon())) {
			json.put(JSONKey.echelon.toString(), getEchelon());
		}
		
		// coefficient
		if(getCoefficient() != null) {
			json.put(JSONKey.coefficient.toString(), getCoefficient());
		}
		
		// pointValue
		if(getPointValue() != null) {
			json.put(JSONKey.pointValue.toString(), getPointValue());
		}
		
		// salary
		if(getSalary() != null) {
			json.put(JSONKey.salary.toString(), getSalary());
		}
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// niveau
		this.setNiveau(jsonObject.optString(JSONKey.niveau.toString()));
		
		// echelon
		if(jsonObject.has(JSONKey.echelon.toString())) {
			try {
				String _echelon = jsonObject.getString(JSONKey.echelon.toString());
				if(!AEStringUtil.isEmpty(_echelon)) {
					this.setEchelon(_echelon);
				} else {
					this.setEchelon(null);
				}
			} catch (Exception e) {
				this.setEchelon(null);
			}
		}
		
		// coefficient
		if(jsonObject.has(JSONKey.coefficient.toString())) {
			try {
				this.setCoefficient(jsonObject.getDouble(JSONKey.coefficient.toString()));
			} catch (Exception e) {
				this.setCoefficient(null);
			}
		}
		
		// pointValue
		if(jsonObject.has(JSONKey.pointValue.toString())) {
			try {
				this.setPointValue(jsonObject.getDouble(JSONKey.pointValue.toString()));
			} catch (Exception e) {
				this.setPointValue(null);
			}
		}
		
		// salary
		if(jsonObject.has(JSONKey.salary.toString())) {
			try {
				this.setSalary(jsonObject.getDouble(JSONKey.salary.toString()));
			} catch (Exception e) {
				this.setSalary(null);
			}
		}
	}

	public AEDescriptive getSalaryGrid() {
		return salaryGrid;
	}

	public void setSalaryGrid(AEDescriptive salaryGrid) {
		this.salaryGrid = salaryGrid;
	}

	public String getNiveau() {
		return niveau;
	}

	public void setNiveau(String niveau) {
		this.niveau = niveau;
	}

	public String getEchelon() {
		return echelon;
	}

	public void setEchelon(String echelon) {
		this.echelon = echelon;
	}

	public Double getCoefficient() {
		return coefficient;
	}

	public void setCoefficient(Double coefficient) {
		this.coefficient = coefficient;
	}

	public Double getPointValue() {
		return pointValue;
	}

	public void setPointValue(Double pointValue) {
		this.pointValue = pointValue;
	}

	public Double getSalary() {
		return salary;
	}

	public void setSalary(Double salary) {
		this.salary = salary;
	}
}
