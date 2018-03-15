package eu.agileeng.domain.facturation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.accbureau.AEApp;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;

public class AEFactureView {

	private JSONObject jsonDocument;
	public static final long DEVIS = AEDocumentType.System.AEDevisSale.getSystemID();
	public static final long FACTURE = AEDocumentType.System.AEFactureSale.getSystemID();
	
	public AEFactureView(JSONObject jsonDocument) {
		this.jsonDocument = jsonDocument;
	}

	public JSONObject getJsonDocument() {
		return jsonDocument;
	}
	
	public final int getDocType() {
		int type = 0;
		
		if (jsonDocument != null && jsonDocument.has("docType")) {
			type = jsonDocument.optInt("docType".toString());
		}
		
		return type;
	}
	
	public final String getNumber() {
		String number = AEStringUtil.EMPTY_STRING;
		if (jsonDocument != null && jsonDocument.has("number")) {
			number = jsonDocument.optString("number");
		}
		return number;
	}
	
	public final String getDate() {
		String date = AEStringUtil.EMPTY_STRING;
		if (jsonDocument != null && jsonDocument.has("date")) {
			Date d = AEDateUtil.parseDateStrict(jsonDocument.optString("date"));
			if (d != null) {
				date = AEDateUtil.convertToString(d, AEDateUtil.FRENCH_DATE_FORMAT);
			}
		}
		
		return date;
	}
	
	public final String getImgSrc() {
		String imgSrc = AEStringUtil.EMPTY_STRING;
		
		return imgSrc;
	}
	
	private JSONObject getJsonClient() {
		JSONObject client = null;
		if (jsonDocument != null && jsonDocument.has(AEFacture.JSONKey.client)) {
			client = jsonDocument.optJSONObject(AEFacture.JSONKey.client);
		}
		
		return client;
	}
	
	public final String getClientName() {
		String clientName = AEStringUtil.EMPTY_STRING;
		JSONObject client = this.getJsonClient();
		
		if (client != null && client.has("name")) {
			clientName = client.optString("name");
		}
		
		return clientName;
	}
	
	public final String getClientCode() {
		String clientCode = AEStringUtil.EMPTY_STRING;
		JSONObject client = this.getJsonClient();
		
		if (client != null && client.has("code")) {
			clientCode = client.optString("code");
		}
		
		return clientCode;
	}
	
	public final String getClientVAT() {
		String clientVAT = AEStringUtil.EMPTY_STRING;
		JSONObject client = this.getJsonClient();
		
		if (client != null && client.has("vatNumber")) {
			clientVAT = client.optString("vatNumber");
		}
		
		return clientVAT;
	}
	
	public final String getClientAddress() {
		String clientAddress = AEStringUtil.EMPTY_STRING;
		JSONObject client = this.getJsonClient();
		JSONObject address = null;
		
		if (client != null && client.has("address")) {
			address = client.optJSONObject("address");
			if (address != null && address.has("address")) {
				clientAddress = address.optString("address");
			}
		}
		
		return clientAddress;
	}
	
	public final String getClientPostCodeAndTown() {
		String clientPostCodeAndTown = AEStringUtil.EMPTY_STRING;
		JSONObject client = this.getJsonClient();
		JSONObject address = null;
		
		if (client != null && client.has("address")) {
			address = client.optJSONObject("address");
			if (address != null && address.has("postCode")) {
				clientPostCodeAndTown = address.optString("postCode");
			}
			if (address != null && address.has("town")) {
				clientPostCodeAndTown += " "+address.optString("town");
			}
		}
		
		return clientPostCodeAndTown;
	}
	
	/**
	 * 
	 * @return
	 * @throws AEException In this case the JSP should return error.
	 * Because we have inconsistent internal state and such document cannot be printed!!!
	 */
	public final List<AEFactureItemView> getFactureItems() throws AEException {
		List<AEFactureItemView> factureItems = new ArrayList<AEFactureItemView>();
		if (jsonDocument != null && jsonDocument.has(AEFacture.JSONKey.factureItems)) {
			JSONArray jsonItems = jsonDocument.optJSONArray(AEFacture.JSONKey.factureItems.toString());
			if(jsonItems != null) {
				for (int i=0; i<jsonItems.length(); i++) {
					try {
						AEFactureItemView itemView = new AEFactureItemView(jsonItems.getJSONObject(i));
						factureItems.add(itemView);
					} catch (JSONException e) {
						throw new AEException(e);
					}
				}
			}
		}
		return factureItems;
	}
	
	public final String getTotalNet() {
		String totalNet = AEStringUtil.EMPTY_STRING;
		if (jsonDocument != null && jsonDocument.has("taxableAmount")) {
			totalNet = AEMath.toAmountString(jsonDocument.optDouble("taxableAmount"));
		}
		return totalNet;
	}
	
	public final String getTVAAmount() {
		String tvaAmount = AEStringUtil.EMPTY_STRING;
		if (jsonDocument != null && jsonDocument.has("vatAmount")) {
			tvaAmount = AEMath.toAmountString(jsonDocument.optDouble("vatAmount"));
		}
		return tvaAmount;
	}
	
	public final String getAmount() {
		String amount = AEStringUtil.EMPTY_STRING;
		if (jsonDocument != null && jsonDocument.has("amount")) {
			amount = AEMath.toAmountString(jsonDocument.optDouble("amount"));
		}
		return amount;
	}
	
	public final String getValidityDate() {
		String validityDate = AEStringUtil.EMPTY_STRING;
		if (jsonDocument != null && jsonDocument.has("dateOfExpiry")) {
			Date d = AEDateUtil.parseDateStrict(jsonDocument.optString("dateOfExpiry"));
			if (d != null) {
				validityDate = AEDateUtil.convertToString(d, AEDateUtil.FRENCH_DATE_FORMAT);
			}
		}
		return validityDate;
	}
	
	public final String getDescription() {
		String description = AEStringUtil.EMPTY_STRING;
		if (jsonDocument != null && jsonDocument.has("description")) {
			description = jsonDocument.optString("description");
		}
		return description;
	}
	
	private JSONObject getShippingObject() {
		JSONObject shipping = null;
		if(jsonDocument.has(AEFacture.JSONKey.shipping)) {
			shipping = jsonDocument.optJSONObject(AEFacture.JSONKey.shipping);
		}
		return shipping;
	}
	
	public double getShippingTaxableAmount() {
		double shippingTaxableAmount = 0;
		JSONObject shippingObject = this.getShippingObject();
		if (shippingObject != null && shippingObject.has("taxableAmount")) {
			shippingTaxableAmount = shippingObject.optDouble("taxableAmount");
		}
		return shippingTaxableAmount;
	}
	
	public double getBruttoAmount() {
		double bruttoAmount = 0;
		if (jsonDocument != null && jsonDocument.has(AEFacture.JSONKey.brutoAmount)) {
			bruttoAmount = jsonDocument.optDouble(AEFacture.JSONKey.brutoAmount.toString());
		}
		return bruttoAmount;
	}
	
	public boolean isAdvance() {
		boolean res = false;
		if (jsonDocument != null && jsonDocument.has(AEFacture.JSONKey.subType)) {
			res = 
				(jsonDocument.optLong(AEFacture.JSONKey.subType) == AEFactureUtil.FactureSubType.ADVANCE.getSubTypeId());
		}
		return res;
	}
	
	public boolean isRegular() {
		boolean res = false;
		if (jsonDocument != null && jsonDocument.has(AEFacture.JSONKey.subType)) {
			res = 
				(jsonDocument.optLong(AEFacture.JSONKey.subType) == AEFactureUtil.FactureSubType.REGULAR.getSubTypeId());
		}
		return res;
	}
	
	public String getSrcDocNumer() {
		String res = AEStringUtil.EMPTY_STRING;
		if (jsonDocument != null && jsonDocument.has(AEFacture.JSONKey.srcDoc)) {
			JSONObject srcDocJson= jsonDocument.optJSONObject(AEFacture.JSONKey.srcDoc);
			if(srcDocJson != null && srcDocJson.has("number")) {
				res = srcDocJson.optString("number");
				res = AEStringUtil.trim(res);
			}
		}
		return res;
	}
	
	public String getTheOneVatRate(JSONArray vatItemsView) {
		String res = AEStringUtil.EMPTY_STRING;
		if (vatItemsView != null && vatItemsView.length() == 1) {
			try {
				JSONObject vatItemJson = vatItemsView.getJSONObject(0);
				if(vatItemJson != null && vatItemJson.has(AEVatItem.JSONKey.rate)) {
					Number rateNumer = AEMath.parseNumber(vatItemJson.optString(AEVatItem.JSONKey.rate), true);
					if(rateNumer != null) {
						res = AEMath.toRateString(rateNumer.doubleValue());
					}
				}
			} catch (Exception e) {
				AEApp.logger().error("getTheOneVatRate failed: ", e);
			}
		}
		return res;
	}
}
