package eu.agileeng.domain.document.social;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.document.AEPrintTemplate;
import eu.agileeng.domain.document.social.contractdetravail.ContractDeTravailType;

public class SocialPrintTemplate extends AEPrintTemplate {

	private static final long serialVersionUID = 5506464140925237278L;

	static public enum JsonKey {
		fullTime,
		term,
		templateSetId
	}
	
	// TEMPORARY_SEASONAL(10), //subtype of temporary contract (CDD) KEEP the group range!!!
	// TEMPORARY_REPLACEMENT(11), //subtype of temporary contract (CDD) KEEP the group range!!!
	// PERMANENT(100); //CDI KEEP the group range!!!
	private ContractDeTravailType subType;
	
	// true - full time contract, false - partial time contract
	private boolean fullTime;
	
	// TODO
	private int term;
	
	private SocialTemplatesSet templateSet;
		
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		// fullTime
		json.put(SocialPrintTemplate.JsonKey.fullTime.toString(), isFullTime());
		
		// term
		json.put(SocialPrintTemplate.JsonKey.term.toString(), getTerm());
		
		// template set
		if(getTemplateSet() != null) {
			json.put(SocialPrintTemplate.JsonKey.templateSetId.toString(), getTemplateSet().getId());
		}
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// fullTime
		if(jsonObject.has(SocialPrintTemplate.JsonKey.fullTime.toString())) {
			setFullTime(jsonObject.optBoolean(SocialPrintTemplate.JsonKey.fullTime.toString()));
		}
		
		// term
		if(jsonObject.has(SocialPrintTemplate.JsonKey.term.toString())) {
			setTerm(jsonObject.optInt(SocialPrintTemplate.JsonKey.term.toString()));
		}
	}

	public ContractDeTravailType getSubType() {
		return subType;
	}

	public void setSubType(ContractDeTravailType subType) {
		this.subType = subType;
	}

	public boolean isFullTime() {
		return fullTime;
	}

	public void setFullTime(boolean fullTime) {
		this.fullTime = fullTime;
	}

	public int getTerm() {
		return term;
	}

	public void setTerm(int term) {
		this.term = term;
	}

	/**
	 * @return the templateSet
	 */
	public SocialTemplatesSet getTemplateSet() {
		return templateSet;
	}

	/**
	 * @param templateSet the templateSet to set
	 */
	public void setTemplateSet(SocialTemplatesSet templateSet) {
		this.templateSet = templateSet;
	}
}
