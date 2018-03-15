package eu.agileeng.domain.facturation;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.DomainModel;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.document.trade.AEDocumentItem;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.domain.measurement.UnitOfMeasurement;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.json.JSONUtil;

public class AEFactureItem extends AEDocumentItem {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3380887952129880584L;
	
	static public class JSONKey {
		public static final String article = "article";
		public static final String typeItem = "typeItem";
		public static final String familleId = "familleId";
		public static final String familleName = "familleName";
		public static final String group = "group";
		public static final String qty = "qty";		
		public static final String uomId = "uomId";
		public static final String uomCode = "uomCode";
		public static final String priceExVat = "priceExVat";
		public static final String grossAmount = "grossAmount";
		public static final String discountPercent = "discountPercent";
		public static final String discountAmount = "discountAmount";
		public static final String brutoAmount = "brutoAmount";
		public static final String vatId = "vatId";
		public static final String vatItem = "vatItem";
		public static final String priceInVat = "priceInVat";
		public static final String priceInVatPinned = "priceInVatPinned";
	}
	
	private AEDescriptive article;
	
	private AEFactureUtil.FactureItemType typeItem = AEFactureUtil.FactureItemType.NA;
	
	// use code, name, description from article
	
	private AEDescriptive group; // Famille
	
	private double qty;
	
	private AEDescriptive uom; // Unite
	
	/**
	 * Price without VAT
	 */
	private double priceExVat;
	
	/**
	 * qty * priceExVat
	 */
	private double grossAmount;
	
	/**
	 * Discount percent.
	 * Should be applied over grossAmount
	 */
	private double discountPercent;
	
	private double discountAmount;
	
	/**
	 * grossAmount - discountAmount
	 * this is NOT taxable amount
	 */
	private double brutoAmount;
	
	private long vatId;
	
	private AEVatItem vatItem; // Taux de TVA
	
	private double priceInVat;
	
	private boolean priceInVatPinned;
	
	/**
	 * The reference to item that is deducted
	 */
	private AEDescriptive itemDeducted;
	
	/**
	 * The reference to the payment that is invoiced
	 */
	private AEDescriptive paymentInvoiced;

	public AEFactureItem() {
		super(DomainModel.DomainClass.AEFactureItem);
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();

		// public static final String article = "article";
		if(getArticle() != null) {
			json.put(JSONKey.article, getArticle().toJSONObject());
		}
		
		// public static final String typeItem = "typeItem";
		if(getTypeItem() != null) {
			json.put(JSONKey.typeItem, getTypeItem().getTypeId());
		}
		
		// famille
		if(getGroup() != null) {
			json.put(JSONKey.familleId, getGroup().getDescriptor().getID());
			json.put(JSONKey.familleName, getGroup().getDescriptor().getName());
		}
	
		// public static final String qty = "qty";	
		json.put(JSONKey.qty, AEMath.toPrice3String(getQty()));
		
		// public static final String uom = "uom";
		if(getUom() != null) {
			json.put(JSONKey.uomId, getUom().getDescriptor().getID());
			json.put(JSONKey.uomCode, getUom().getDescriptor().getCode());
		}
		
		// public static final String priceExVat = "priceExVat";
		json.put(JSONKey.priceExVat, AEMath.toAmountString(getPriceExVat()));
		
		// public static final String grossAmount = "grossAmount";
		json.put(JSONKey.grossAmount, AEMath.toAmountString(getGrossAmount()));
		
		// public static final String discountPercent = "discountPercent";
		json.put(JSONKey.discountPercent, AEMath.toAmountString(getDiscountPercent()));
		
		// public static final String discountAmount = "discountAmount";
		json.put(JSONKey.discountAmount, AEMath.toAmountString(getDiscountAmount()));
		
		// public static final String brutoAmount = "brutoAmount";
		json.put(JSONKey.brutoAmount, AEMath.toAmountString(getBrutoAmount()));
		
		// public static final String vatId = "vatId";
		// json.put(JSONKey.vatId, getVatId());
		
		// public static final String vatItem = "vatItem";
		if(getVatItem() != null) {
			json.put(JSONKey.vatItem, getVatItem().toJSONObject());
			json.put(JSONKey.vatId, getVatItem().getID());
		}
		
		// public static final String priceInVat = "priceInVat";
		json.put(JSONKey.priceInVat, AEMath.toAmountString(getPriceInVat()));
		
		// public static final String priceInVatPinned = "priceInVatPinned";
		json.put(JSONKey.priceInVatPinned, isPriceInVatPinned());
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);

		// public static final String article = "article";
		if(jsonObject.has(JSONKey.article)) {
			JSONObject articleJSON = jsonObject.optJSONObject(JSONKey.article);
			if(articleJSON != null) {
				AEArticle article = new AEArticle();
				article.create(articleJSON);
				setArticle(article);
			}
		}
		
		// public static final String typeItem = "typeItem";
		setTypeItem(AEFactureUtil.FactureItemType.valueOf(jsonObject.optInt(JSONKey.typeItem)));
		
		// famille
		AEDescriptorImp group = new AEDescriptorImp();
		group.setID(jsonObject.optLong(JSONKey.group));
		group.setName(jsonObject.optString(JSONKey.familleName));
		setGroup(group);
		
		// public static final String qty = "qty";		
		try {
			setQty(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.qty));
		} catch (Exception e) {
		}
		
		// public static final String uom = "uom";
		if(jsonObject.has(JSONKey.uomId)) {
			long uomId = jsonObject.optLong(JSONKey.uomId);
			if(uomId > 0) {
				UnitOfMeasurement uom = UnitOfMeasurement.getByID(uomId);
				if(uom != null) {
					setUom(uom.getDescriptor());
				}
			}
		}
		
		// public static final String priceExVat = "priceExVat";
		try {
			setPriceExVat(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.priceExVat));
		} catch (Exception e) {
		}
		
		// public static final String grossAmount = "grossAmount";
		try {
			setGrossAmount(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.grossAmount));
		} catch (Exception e) {
		}
		
		// public static final String discountPercent = "discountPercent";
		try {
			setDiscountPercent(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.discountPercent));
		} catch (Exception e) {
		}
		
		// public static final String discountAmount = "discountAmount";
		try {
			setDiscountAmount(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.discountAmount));
		} catch (Exception e) {
		}
		
		// public static final String brutoAmount = "brutoAmount";
		try {
			setBrutoAmount(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.brutoAmount));
		} catch (Exception e) {
		}
		
		// public static final String vatId = "vatId";
		if(jsonObject.has(JSONKey.vatId)) {
			long vatId = jsonObject.optLong(JSONKey.vatId);
			if(vatId > 0) {
				setVatId(vatId);
			}
		}
		
		// public static final String vatItem = "vatItem";
		if(jsonObject.has(JSONKey.vatItem)) {
			JSONObject vatJSON = jsonObject.optJSONObject(JSONKey.vatItem);
			if(vatJSON != null) {
				grantVatItem().create(vatJSON);
			}
		}
		
		// public static final String priceInVat = "priceInVat";
		try {
			setPriceInVat(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.priceInVat));
		} catch (Exception e) {
		}
		
		// public static final String priceInVatPinned = "priceInVatPinned";
		setPriceInVatPinned(jsonObject.optBoolean(JSONKey.priceInVatPinned));
	}
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.AEFactureItem);
	}

	public AEDescriptive getArticle() {
		return article;
	}

	public void setArticle(AEDescriptive article) {
		this.article = article;
	}

	public AEFactureUtil.FactureItemType getTypeItem() {
		return typeItem;
	}

	public void setTypeItem(AEFactureUtil.FactureItemType typeItem) {
		this.typeItem = typeItem;
	}

	public AEDescriptive getGroup() {
		return group;
	}

	public void setGroup(AEDescriptive group) {
		this.group = group;
	}

	public double getQty() {
		return qty;
	}

	public void setQty(double qty) {
		this.qty = qty;
	}

	public AEDescriptive getUom() {
		return uom;
	}

	public void setUom(AEDescriptive uom) {
		this.uom = uom;
	}

	public double getPriceExVat() {
		return priceExVat;
	}

	public void setPriceExVat(double priceExVat) {
		this.priceExVat = priceExVat;
	}

	public double getGrossAmount() {
		return grossAmount;
	}

	public void setGrossAmount(double grossAmount) {
		this.grossAmount = grossAmount;
	}

	public double getDiscountPercent() {
		return discountPercent;
	}

	public void setDiscountPercent(double discountPercent) {
		this.discountPercent = discountPercent;
	}

	public double getDiscountAmount() {
		return discountAmount;
	}

	public void setDiscountAmount(double discountAmount) {
		this.discountAmount = discountAmount;
	}

	public double getBrutoAmount() {
		return brutoAmount;
	}

	public void setBrutoAmount(double brutoAmount) {
		this.brutoAmount = brutoAmount;
	}

	public AEVatItem getVatItem() {
		return vatItem;
	}
	
	public boolean isVatDefined() {
		return getVatItem() != null && getVatItem().getID() > 0;
	}
	
	public AEVatItem grantVatItem() {
		if(vatItem == null) {
			vatItem = new AEVatItem();
		}
		return vatItem;
	}

	public void setVatDescriptor(AEVatItem vatItem) {
		this.vatItem = vatItem;
	}

	public double getPriceInVat() {
		return priceInVat;
	}

	public void setPriceInVat(double priceInVat) {
		this.priceInVat = priceInVat;
	}

	public boolean isPriceInVatPinned() {
		return priceInVatPinned;
	}

	public void setPriceInVatPinned(boolean priceInVatPinned) {
		this.priceInVatPinned = priceInVatPinned;
	}

	public long getVatId() {
		return vatId;
	}

	public void setVatId(long vatId) {
		this.vatId = vatId;
	}
	
	/**
	 * Calculates the item's amounts
	 */
	public void calculate() {
		if(AEFactureUtil.FactureItemType.ADVANCE == getTypeItem()) {
			double priceExVat = 0.0;
			if(isVatDefined()) {
				double vatRate = getVatItem().getRate();
				priceExVat = getPriceInVat() / (vatRate / 100 + 1.0);
				setPriceExVat(AEMath.round(priceExVat, 3));
			}

			// calculate gross amount
			calculateGrossAmount();

			// calculate discount amount
			setDiscountPercent(0.0);
			setDiscountAmount(0.0);

			// calculate bruto amount
			calculateBrutoAmount();

			// tax
			tax();
		} else {
			// calculate gross amount
			calculateGrossAmount();

			// calculate discount amount
			calculateDiscountAmount();

			// calculate bruto amount
			calculateBrutoAmount();

			// tax
			tax();
		}
	}
	
	/**
	 * Calculates the gross amount
	 */
	public void calculateGrossAmount() {
		double ga = getQty() * getPriceExVat();
		setGrossAmount(AEMath.round(ga, 2));
	}
	
	/**
	 * Calculates the discount amount
	 */
	public void calculateDiscountAmount() {
		double da = 0.0;
		if(!AEMath.isZeroAmount(getDiscountPercent())) {
			da = getGrossAmount() * getDiscountPercent() / 100;
		}
		setDiscountAmount(AEMath.round(da, 2));
	}
	
	/**
	 * Calculates the bruto amount
	 */
	public void calculateBrutoAmount() {
		double ba = getGrossAmount() - getDiscountAmount();
		setBrutoAmount(AEMath.round(ba, 2));
	}
	
	/**
	 * Tax
	 */
	public void tax() {
		double taxableAmount = 0.0;
		double vatAmount = 0.0;
		double amount = 0.0;
		
		if(AEFactureUtil.FactureItemType.ADVANCE == getTypeItem()) {
			amount = qty * getPriceInVat();
			amount = AEMath.round(amount, 2);
			
			taxableAmount = getBrutoAmount();
			taxableAmount = AEMath.round(taxableAmount, 2);
			
			vatAmount = amount - taxableAmount;
			vatAmount = AEMath.round(vatAmount, 2);
		} else {
			// taxable amount
			taxableAmount = getBrutoAmount();

			// vat amount
			vatAmount = 0.0;
			if(isVatDefined()) {
				double vatRate = getVatItem().getRate();
				vatAmount = taxableAmount * vatRate / 100;
				vatAmount = AEMath.round(vatAmount, 2);
			}

			// amount
			amount = taxableAmount + vatAmount;
			amount = AEMath.round(amount, 2);
		}
		
		// set amounts
		setTaxableAmount(taxableAmount);
		setVatAmount(vatAmount);
		setAmount(amount);
	}

	public AEDescriptive getItemDeducted() {
		return itemDeducted;
	}

	public void setItemDeducted(AEDescriptive itemDeducted) {
		this.itemDeducted = itemDeducted;
	}

	public AEDescriptive getPaymentInvoiced() {
		return paymentInvoiced;
	}

	public void setPaymentInvoiced(AEDescriptive paymentInvoiced) {
		this.paymentInvoiced = paymentInvoiced;
	}
}
