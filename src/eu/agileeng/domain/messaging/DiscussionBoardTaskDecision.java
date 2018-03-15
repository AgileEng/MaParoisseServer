package eu.agileeng.domain.messaging;

import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;


public class DiscussionBoardTaskDecision extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1512823946914513822L;
	
	private long taskId;
	private long authPrincipalId;
	private String authName;
	private int decision;
	private Date decisionMadeDate;

	public DiscussionBoardTaskDecision() {
		super(DomainClass.Decision);
	}
	
	public static enum Decision {
		UNDEFINED(-1),
		APPROVE(1),
		DISAPPROVE(0);
		
		private int id;

		private Decision(int id) {
			this.id = id;
		}

		public final int getID() {
			return this.id;
		}

		public final static Decision valueOf(int id) {
			Decision foundDecision = null;
			for (Decision decision : Decision.values()) {
				if(decision.getID() == id) {
					foundDecision = decision;
					break;
				}
			}
			return foundDecision;
		}
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		json.put("taskId", this.getTaskId());
		json.put("authPrincipalId", this.getAuthPrincipalId());
		json.put("decision", this.getDecision());
		
		return json;
	}
	
	@Override
	public void create(JSONObject json) throws JSONException {
		super.create(json);
		
		this.setTaskId(json.optLong("taskId"));
		this.setAuthPrincipalId(json.optLong("authPrincipalId"));
		this.setDecision(json.optInt("decision"));
		
	}
	
	public JSONObject appendToJSON(JSONObject json) throws JSONException {
		
		
		json.put(this.genDataIndex(), this.getDecision());
		
		return json;
	}
	
	public String genDataIndex() {
		return "auth_"+this.getAuthPrincipalId();
	}

	public long getTaskId() {
		return taskId;
	}

	public void setTaskId(long taskId) {
		this.taskId = taskId;
	}

	public long getAuthPrincipalId() {
		return authPrincipalId;
	}

	public void setAuthPrincipalId(long authPrincipalId) {
		this.authPrincipalId = authPrincipalId;
	}

	public int getDecision() {
		return decision;
	}

	public void setDecision(int decision) {
		this.decision = decision;
	}

	public Date getDecisionMadeDate() {
		return decisionMadeDate;
	}

	public void setDecisionMadeDate(Date decisionMadeDate) {
		this.decisionMadeDate = decisionMadeDate;
	}

	public String getAuthName() {
		return authName;
	}

	public void setAuthName(String authName) {
		this.authName = authName;
	}

}
