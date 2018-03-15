package eu.agileeng.domain.council;

import java.util.ArrayList;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;

import eu.agileeng.domain.AEList;

public class CouncilMembersList extends ArrayList<CouncilMember> implements AEList {

	@Override
	public JSONArray toJSONArray() throws JSONException {
		JSONArray jsonArr = new JSONArray();
		
		for (CouncilMember cm: this) {
			jsonArr.put(cm.toJSONObject());
		}
		
		return jsonArr;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
		for(int i = 0; i < jsonArray.length(); i++) {
			CouncilMember cm = new CouncilMember();
			cm.create(jsonArray.getJSONObject(i));
			this.add(cm);
		}
		
	}
	
	public void setCouncilId(long cid) {
		for(CouncilMember cm: this) {
			cm.setCouncilId(cid);
		}
	}

}
