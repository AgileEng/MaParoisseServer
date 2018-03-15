package eu.agileeng.domain.social;

import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.util.AEStringUtil;

public class SocialInfoView {
private JSONObject jsonDocument;
	
	public SocialInfoView(JSONObject jsonDocument) {
		this.jsonDocument = jsonDocument;
	}

	public JSONObject getJsonDocument() {
		return jsonDocument;
	}
	
	public final String getSociete() {
		String societe = AEStringUtil.EMPTY_STRING;
		
		if(jsonDocument != null && jsonDocument.has(SocialInfo.JSONKey.societe.toString())) {
			societe = jsonDocument.optString(SocialInfo.JSONKey.societe.toString());
		}
		
		return societe;
	}
	
	public final String getSocieteTerm() {
		String societeTerm = AEStringUtil.EMPTY_STRING;
		
		if(jsonDocument != null && jsonDocument.has(SocialInfo.JSONKey.societeTerm.toString())) {
			societeTerm = jsonDocument.optString(SocialInfo.JSONKey.societeTerm.toString());
		}
		
		return societeTerm;
	}
	
	public final String getURSSAFText() {
		String urssaf = AEStringUtil.EMPTY_STRING;
		if (jsonDocument != null && jsonDocument.has(SocialInfo.JSONKey.urssafPretext.toString())) {
			urssaf = jsonDocument.optString(SocialInfo.JSONKey.urssafPretext.toString());
			if (!"’".equals(urssaf.substring(urssaf.length() - 1))) {
				urssaf += " ";
			}
		}
		return urssaf;
	}
	
	public final String getURSSAFText2() {
		String urssaf2 = AEStringUtil.EMPTY_STRING;
		if (jsonDocument != null && jsonDocument.has(SocialInfo.JSONKey.urssafText.toString())) {
			urssaf2 = jsonDocument.optString(SocialInfo.JSONKey.urssafText.toString());
		}
		return urssaf2;
	}
	
	public final String getURSSAFNumber() {
		String urssaf = AEStringUtil.EMPTY_STRING;
		if (jsonDocument != null && jsonDocument.has(SocialInfo.JSONKey.urssafNumber.toString())) {
			urssaf = jsonDocument.optString(SocialInfo.JSONKey.urssafNumber.toString());
		}
		return urssaf;
	}
	
	public final boolean isCctv() {
		boolean res = false;
		if (jsonDocument != null && jsonDocument.has(SocialInfo.JSONKey.cctv.toString())) {
			res = jsonDocument.optBoolean(SocialInfo.JSONKey.cctv.toString());
		}
		return res;
	}
	
	public final String getUrssafText() {
		String res = AEStringUtil.EMPTY_STRING;
		if (jsonDocument != null && jsonDocument.has(SocialInfo.JSONKey.urssafText.toString())) {
			res = jsonDocument.optString(SocialInfo.JSONKey.urssafText.toString());
		}
		return res;
	}
	
	public final String getActivitePrincipalePretext() {
		String activite = AEStringUtil.EMPTY_STRING;
		if (jsonDocument != null && jsonDocument.has(SocialInfo.JSONKey.activitePrincipale.toString())) {
			activite = jsonDocument.optString(SocialInfo.JSONKey.activitePrincipale.toString());
			if (!"’".equals(activite.substring(activite.length() - 1))) {
				activite += " ";
			}
		}
		return activite;
	}

	public String getActivitePrincipaleText() {
		String res = AEStringUtil.EMPTY_STRING;
		if (jsonDocument != null && jsonDocument.has(SocialInfo.JSONKey.activitePrincipaleText.toString())) {
			res = jsonDocument.optString(SocialInfo.JSONKey.activitePrincipaleText.toString());
		}
		return res;
	}
}
