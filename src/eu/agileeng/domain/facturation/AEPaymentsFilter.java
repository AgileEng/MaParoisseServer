package eu.agileeng.domain.facturation;

import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.util.AEDateUtil;

public class AEPaymentsFilter extends AEPayment {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4647733371589934281L;

	private Date dateFrom;
	
	private Date dateTo;
	
	public AEPaymentsFilter() {
		super(DomainClass.AEPaymentsFilter);
	}

	public Date getDateFrom() {
		return dateFrom;
	}

	public void setDateFrom(Date dateFrom) {
		this.dateFrom = dateFrom;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// dateFrom
		if(jsonObject.has("dateFrom")) {
			this.setDateFrom(AEDateUtil.parseDateStrict(jsonObject.optString("dateFrom")));
		}
		
		// dateFrom
		if(jsonObject.has("dateTo")) {
			this.setDateTo(AEDateUtil.parseDateStrict(jsonObject.optString("dateTo")));
		}
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		return json;
	}

	public Date getDateTo() {
		return dateTo;
	}

	public void setDateTo(Date dateTo) {
		this.dateTo = dateTo;
	}
}
