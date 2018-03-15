package eu.agileeng.domain.contact;

import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.util.AEStringUtil;

public class OrganizationView {
	private JSONObject jsonOrganization;
	
	public OrganizationView(JSONObject jsonOrganization) {
		this.jsonOrganization = jsonOrganization;
	}
	
	public final String getName() {
		String str = AEStringUtil.EMPTY_STRING;
		
		if (jsonOrganization != null && jsonOrganization.has("name")) {
			str = AEStringUtil.trim(jsonOrganization.optString("name"));
		}
		
		return str;
	}
	
	public final String getAddress() {
		String str = AEStringUtil.EMPTY_STRING;
		
		if (jsonOrganization != null && jsonOrganization.has("address")) {
			str = AEStringUtil.trim(jsonOrganization.optString("address"));
		}
		
		return str;
	}
	
	public final String getSecondaryAddress() {
		String str = AEStringUtil.EMPTY_STRING;
		
		if (jsonOrganization != null && jsonOrganization.has("secondaryAddress")) {
			str = AEStringUtil.trim(jsonOrganization.optString("secondaryAddress"));
		}
		
		return str;
	}
	
	public final String getPostCode() {
		String str = AEStringUtil.EMPTY_STRING;
		
		if (jsonOrganization != null && jsonOrganization.has("postCode")) {
			str = AEStringUtil.trim(jsonOrganization.optString("postCode"));
		}
		
		return str;
	}
	
	public final String getTown() {
		String str = AEStringUtil.EMPTY_STRING;
		
		if (jsonOrganization != null && jsonOrganization.has("town")) {
			str = AEStringUtil.trim(jsonOrganization.optString("town"));
		}
		
		return str;
	}
	
	public final String getPostCodeAndTown() {
		StringBuffer str = new StringBuffer();
		
		str.append(getPostCode());
		if(!AEStringUtil.isEmpty(str)) {
			str.append(" ");
		}
		str.append(getTown());
		
		return str.toString();
	}
	
	public final String getTelephone() {
		String str = AEStringUtil.EMPTY_STRING;
		
		if (jsonOrganization != null && jsonOrganization.has("phone")) {
			str = AEStringUtil.trim(jsonOrganization.optString("phone"));
		}
		
		return str;
	}
	
	public final String getFax() {
		String str = AEStringUtil.EMPTY_STRING;
		
		if (jsonOrganization != null && jsonOrganization.has("fax")) {
			str = AEStringUtil.trim(jsonOrganization.optString("fax"));
		}
		
		return str;
	}
	
	public final String getEmail() {
		String mail = AEStringUtil.EMPTY_STRING;
		if (jsonOrganization != null && jsonOrganization.has("email")) {
			mail = AEStringUtil.trim(jsonOrganization.optString("email"));
		}
		return mail;
	}
	
	public final String getSiret() {
		String siret = AEStringUtil.EMPTY_STRING;
		if (jsonOrganization != null && jsonOrganization.has("siren")) {
			siret = AEStringUtil.trim(jsonOrganization.optString("siren"));
		}
		return siret;
	}
	
	public final String getSiretWithSpaces() {
		String siretOld = getSiret();
		siretOld = AEStringUtil.trim(siretOld);
		
		StringBuilder siret = new StringBuilder(siretOld.replaceAll("\\s", ""));
	
		
		int len = siret.length();
		len -= 5;
		
		while (len > 0) {
			siret.insert(len, " ");
			len -= 3;
		}
		
		return siret.toString();
	}
	
	public final String getAPE() {
		String ape = AEStringUtil.EMPTY_STRING;
		if (jsonOrganization != null && jsonOrganization.has("nic")) {
			ape = AEStringUtil.trim(jsonOrganization.optString("nic"));
		}
		return ape;
	}
	
	public final String getVATNumber() {
		String vatNumber = AEStringUtil.EMPTY_STRING;
		if (jsonOrganization != null && jsonOrganization.has("tva_intracon")) {
			vatNumber = AEStringUtil.trim(jsonOrganization.optString("tva_intracon"));
		}
		return vatNumber;
	}
}
