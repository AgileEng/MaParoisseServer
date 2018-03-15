package eu.agileeng.domain.facturation;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.document.trade.AETradeDocumentFilter;

public class AEFactureFilter extends AETradeDocumentFilter {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6772215282517216526L;

	private AEFactureUtil.FactureState state;
	
	private AEFactureUtil.FactureSubType subType;
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// state
		if(jsonObject.has(AEFacture.JSONKey.state)) {
			int stateId = jsonObject.optInt(AEFacture.JSONKey.state);
			AEFactureUtil.FactureState state = AEFactureUtil.FactureState.valueOf(stateId);
			setState(state);
		}
		
		// subType
		if(jsonObject.has(AEFacture.JSONKey.subType)) {
			int subTypeId = jsonObject.optInt(AEFacture.JSONKey.subType);
			AEFactureUtil.FactureSubType subType = AEFactureUtil.FactureSubType.valueOf(subTypeId);
			setSubType(subType);
		}
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();

		// state
		if(getState() != null) {
			json.put(AEFacture.JSONKey.state, getState().getStateId());
		}
		
		return json;
	}

	public AEFactureUtil.FactureState getState() {
		return state;
	}

	public void setState(AEFactureUtil.FactureState state) {
		this.state = state;
	}

	public AEFactureUtil.FactureSubType getSubType() {
		return subType;
	}

	public void setSubType(AEFactureUtil.FactureSubType subType) {
		this.subType = subType;
	}
}
