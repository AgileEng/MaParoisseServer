package eu.agileeng.domain.council;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.util.AEDateUtil;

public class Council extends AEDomainObject {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5046428881899868727L;
	
	private long ownerId;
	private Date startDate;
	private Date endDate;
	private boolean closed;
	private CouncilMembersList members;
	

	public Council() {
		super(DomainClass.Council);
		// TODO Auto-generated constructor stub
	}

	public long getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(long ownerId) {
		this.ownerId = ownerId;
	}

	public Date getStartDate() {
		return startDate;
	}


	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}


	public Date getEndDate() {
		return endDate;
	}


	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}


	public boolean isClosed() {
		return closed;
	}


	public void setClosed(boolean closed) {
		this.closed = closed;
	}


	public CouncilMembersList getMembers() {
		return members;
	}


	public void setMembers(CouncilMembersList members) {
		this.members = members;
	}
	
	@Override
	public void create(JSONObject json) throws JSONException {
		super.create(json);
		
		this.setOwnerId(json.getLong("ownerId"));
		
		//start date
		if (json.has("startDate")) {
			this.setStartDate(AEDateUtil.parseDateStrict(json.optString("startDate")));
		}
		//end date
		if (json.has("endDate")) {
			this.setEndDate(AEDateUtil.parseDateStrict(json.optString("endDate")));
		}
		
		//closed
		this.setClosed(json.getBoolean("closed"));
		
		//members
		CouncilMembersList cml = new CouncilMembersList();
		cml.create(json.optJSONArray("members"));
		if (this.getID() > 0) cml.setCouncilId(this.getID());
		this.setMembers(cml);
		
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		json.put("ownerId", this.getOwnerId());
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		
		if (this.getStartDate() != null) json.put("startDate", sdf.format(this.getStartDate()));
		if (this.getEndDate() != null) json.put("endDate", sdf.format(this.getEndDate()));
		
		json.put("closed", this.isClosed());
		json.put("members", this.getMembers().toJSONArray());
		
		return json;
	}

}
