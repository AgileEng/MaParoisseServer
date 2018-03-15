package eu.agileeng.domain.facturation;

import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.util.AEStringUtil;

public class AEFactureItemView {

	private JSONObject jsonDocument;
	
	public AEFactureItemView(JSONObject jsonDocument) {
		this.jsonDocument = jsonDocument;
	}
	
	public final String getCode() {
		String code = AEStringUtil.EMPTY_STRING;
		if (jsonDocument != null && jsonDocument.has("code")) {
			code = jsonDocument.optString("code");
		}
		return code;
	}
	
	public final String getName() {
		String name = AEStringUtil.EMPTY_STRING;
		if (jsonDocument != null && jsonDocument.has("name")) {
			name = jsonDocument.optString("name");
		}
		return name;
	}
	
	public final double getQuantity() {
		double qty = 0;
		if (jsonDocument != null && jsonDocument.has(AEFactureItem.JSONKey.qty)) {
			qty = jsonDocument.optDouble(AEFactureItem.JSONKey.qty.toString());
		}
		return qty;
	}
	
	public final String getUOM() {
		String uom = AEStringUtil.EMPTY_STRING;
		if (jsonDocument != null && jsonDocument.has(AEFactureItem.JSONKey.uomCode)) {
			uom = jsonDocument.optString(AEFactureItem.JSONKey.uomCode.toString());
		}
		return uom;
	}
	
	public final double getPriceExclVAT() {
		double price = 0;
		if (jsonDocument != null && jsonDocument.has(AEFactureItem.JSONKey.priceExVat)) {
			price = jsonDocument.optDouble(AEFactureItem.JSONKey.priceExVat.toString());
		}
		return price;
	}
	
	public final double getDiscountPercent() {
		double discountPercent = 0;
		if (jsonDocument != null && jsonDocument.has(AEFactureItem.JSONKey.discountPercent)) {
			discountPercent = jsonDocument.optDouble(AEFactureItem.JSONKey.discountPercent.toString());
		}
		return discountPercent;
	}
	
	public final double getDiscountAmount() {
		double discountAmount = 0;
		if (jsonDocument != null && jsonDocument.has(AEFactureItem.JSONKey.discountAmount)) {
			discountAmount = jsonDocument.optDouble(AEFactureItem.JSONKey.discountAmount.toString());
		}
		return discountAmount;
	}
	
	public final double getPriceInclVAT() {
		double price = 0;
		if (jsonDocument != null && jsonDocument.has(AEFactureItem.JSONKey.priceInVat)) {
			price = jsonDocument.optDouble(AEFactureItem.JSONKey.priceInVat.toString());
		}
		return price;
	}
	
	public final double getVATPercent() {
		JSONObject jsonVAT = null;
		double vat = 0;
		if (jsonDocument != null && jsonDocument.has(AEFactureItem.JSONKey.vatItem)) {
			jsonVAT = jsonDocument.optJSONObject(AEFactureItem.JSONKey.vatItem.toString());
			if (jsonVAT != null && jsonVAT.has(AEVatItem.JSONKey.rate)) {
				vat = jsonVAT.optDouble(AEVatItem.JSONKey.rate);
			}
		}
		return vat;
	}
	
	public final double getBruttoAmount() {
		double bruttoAmount = 0;
		if (jsonDocument != null && jsonDocument.has(AEFactureItem.JSONKey.brutoAmount)) {
			bruttoAmount = jsonDocument.optDouble(AEFactureItem.JSONKey.brutoAmount.toString());
		}
		return bruttoAmount;
	}
}
