package eu.agileeng.domain.facturation;

import java.util.Iterator;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.business.AEPaymentTerms;
import eu.agileeng.domain.document.AEDocumentDescriptor;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.imp.AEDocumentDescriptorImp;
import eu.agileeng.domain.document.trade.AEDocumentItem;
import eu.agileeng.domain.document.trade.AETradeDocument;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEObjectUtil;
import eu.agileeng.util.json.JSONUtil;

public class AEFacture extends AETradeDocument {

	static public class JSONKey {
		public static final String printTemplate = "printTemplate";
		public static final String printTemplateId = "printTemplateId";
		
		public static final String vatItemsView = "vatItemsView";
		
		public static final String facture = "facture";
		public static final String factures = "factures";
		public static final String createdFacture = "createdFacture";

		public static final String client = "client";
		public static final String subType = "subType";

		public static final String factureItems = "factureItems";

		public static final String srcDoc = "srcDoc";
		public static final String dateOfExpiry = "dateOfExpiry";
		public static final String paymentTermsId = "paymentTermsId";
		public static final String payments = "payments";
		public static final String state = "state";

		public static final String advanceMode = "advanceMode";
		public static final String advancePercent = "advancePercent";
		public static final String advanceAmount = "advanceAmount";

		public static final String discountMode = "discountMode";
		public static final String discountPercent = "discountPercent";
		public static final String discountAmount = "discountAmount";

		public static final String shipping = "shipping";

		public static final String itemsDiscountAmount = "itemsDiscountAmount";

		public static final String brutoAmount = "brutoAmount";
		public static final String paidAmount = "paidAmount";
		public static final String locked = "locked";
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 6882246027433759892L;

	/**
	 * The client of this facture.
	 */
	private AEClient client;

	/**
	 * The document from which this document is created.
	 * If this document is Facture (Invoice), here should be its Device (Quote)
	 */
	private AEDescriptive srcDoc;

	private AEFactureUtil.FactureSubType subType = AEFactureUtil.FactureSubType.REGULAR;

	private AEFactureItemsList factureItems = new AEFactureItemsList();

	private AEPaymentsList payments;

	private AEFactureUtil.FactureState state = AEFactureUtil.FactureState.NA;

	private AEPaymentTerms paymentTerms;

	/************** advance ********************/
	private AEFactureUtil.PercentAmountMode advanceMode = AEFactureUtil.PercentAmountMode.NA;

	private double advancePercent;

	private double advanceAmount;

	/************** document level discount ********************/
	private AEFactureUtil.PercentAmountMode discountMode = AEFactureUtil.PercentAmountMode.NA;

	private double discountPercent;

	private double discountAmount;

	/************** shipping ********************/
	private AEVatItem shipping;

	/**
	 * The sum of items discount amount
	 */
	private double itemsDiscountAmount;

	/**
	 * The sum of items bruto amount 
	 * item's brutoAmount = qty * unitPrice - itemDiscountAmount
	 */
	private double brutoAmount;

	/**
	 * taxableAmount = brutoAmount - discountAmount + shippingTaxableAmount 
	 */

	private double paidAmount;
	
	private boolean locked;

	/**
	 * vatAmount
	 * 
	 * 1. itemsQtySum = Sum of item's qty;
	 * 2. for every item: itemDocDiscountAmount = (discountAmount / itemsQtySum) * itemQty;
	 * 3. for every item: itemTaxableAmount = (itemBrutoAmount - itemDocDiscountAmount);
	 * 4. add the shipping item (shipping is like an item, where doc discount is not applied)    
	 * 
	 * The sum of items vatAmounts
	 */

	public AEFacture(AEDocumentType docType) {
		super(docType);
	}

	public AEFacture() {
		this(AEDocumentType.valueOf(AEDocumentType.System.NA));
	}

	public AEDescriptive getSrcDoc() {
		return srcDoc;
	}

	public void setSrcDoc(AEDescriptive srcDoc) {
		this.srcDoc = srcDoc;
	}

	public AEPaymentsList getPayments() {
		return payments;
	}

	public AEPaymentsList grantPayments() {
		if(payments == null) {
			payments = new AEPaymentsList();
		}
		return payments;
	}

	public void setPayments(AEPaymentsList payments) {
		this.payments = payments;
	}

	public AEFactureUtil.FactureState getState() {
		return state;
	}

	public void setState(AEFactureUtil.FactureState state) {
		this.state = state;
	}

	public AEFactureUtil.PercentAmountMode getAdvanceMode() {
		return advanceMode;
	}

	public void setAdvanceMode(AEFactureUtil.PercentAmountMode advanceMode) {
		this.advanceMode = advanceMode;
	}

	public double getAdvancePercent() {
		return advancePercent;
	}

	public void setAdvancePercent(double advancePercent) {
		this.advancePercent = advancePercent;
	}

	public double getAdvanceAmount() {
		return advanceAmount;
	}

	public void setAdvanceAmount(double advanceAmount) {
		this.advanceAmount = advanceAmount;
	}

	public AEFactureUtil.PercentAmountMode getDiscountMode() {
		return discountMode;
	}

	public void setDiscountMode(AEFactureUtil.PercentAmountMode discountMode) {
		this.discountMode = discountMode;
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

	public double getItemsDiscountAmount() {
		return itemsDiscountAmount;
	}

	public void setItemsDiscountAmount(double itemsDiscountAmount) {
		this.itemsDiscountAmount = itemsDiscountAmount;
	}

	public double getBrutoAmount() {
		return brutoAmount;
	}

	public void setBrutoAmount(double brutoAmount) {
		this.brutoAmount = brutoAmount;
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();

		// public static final String client = "client";
		if(getClient() != null) {
			JSONObject clientJSON = getClient().toJSONObject();
			json.put(JSONKey.client, clientJSON);
		}

		// subType
		json.put(JSONKey.subType, getSubType().getSubTypeId());

		if(getDateOfExpiry() != null) {
			json.put(JSONKey.dateOfExpiry, AEDateUtil.convertToString(getDateOfExpiry(), AEDateUtil.SYSTEM_DATE_FORMAT));
		}

		// facture items
		if(getFactureItems() != null) {
			json.put(JSONKey.factureItems, getFactureItems().toJSONArray());
		}

		// public static final String srcDoc = "srcDoc";
		if(getSrcDoc() != null) {
			JSONObject srcDocJSON = getSrcDoc().toJSONObject();
			json.put(JSONKey.srcDoc, srcDocJSON);
		}

		// paymentTermsId
		if(getPaymentTerms() != null) {
			json.put(JSONKey.paymentTermsId, getPaymentTerms().getID());
		}

		// public static final String payments = "payments";
		if(getPayments() != null) {
			JSONArray pJSONArray = getPayments().toJSONArray();
			json.put(JSONKey.payments, pJSONArray);
		}

		// public static final String state = "state";
		if(getState() != null) {
			json.put(JSONKey.state, getState().getStateId());
		}

		// public static final String advanceMode = "advanceMode";
		if(getAdvanceMode() != null) {
			json.put(JSONKey.advanceMode, getAdvanceMode().getModeId());
		}

		// public static final String advancePercent = "advancePercent";
		json.put(JSONKey.advancePercent, AEMath.toAmountString(getAdvancePercent()));

		// public static final String advanceAmount = "advanceAmount";
		json.put(JSONKey.advanceAmount, AEMath.toAmountString(getAdvanceAmount()));

		// public static final String discountMode = "discountMode";
		if(getDiscountMode() != null) {
			json.put(JSONKey.discountMode, getDiscountMode().getModeId());
		}

		// public static final String discountPercent = "discountPercent";
		json.put(JSONKey.discountPercent, AEMath.toAmountString(getDiscountPercent()));

		// public static final String discountAmount = "discountAmount";
		json.put(JSONKey.discountAmount, AEMath.toAmountString(getDiscountAmount()));

		// public static final String shipping = "shipping";
		if(getShipping() != null) {
			json.put(JSONKey.shipping, getShipping().toJSONObject());
		}

		// public static final String itemsDiscountAmount = "itemsDiscountAmount";
		json.put(JSONKey.itemsDiscountAmount, AEMath.toAmountString(getItemsDiscountAmount()));

		// public static final String brutoAmount = "brutoAmount";
		json.put(JSONKey.brutoAmount, AEMath.toAmountString(getBrutoAmount()));

		// paidAdvance
		json.put(JSONKey.paidAmount, AEMath.toAmountString(getPaidAmount()));
		
		// locked
		json.put(JSONKey.locked, isLocked());

		return json;
	}

	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);

		// public static final String client = "client";
		if(jsonObject.has(JSONKey.client)) {
			JSONObject clientJson = jsonObject.optJSONObject(JSONKey.client);
			if(clientJson != null) {
				AEClient client = new AEClient();
				client.create(clientJson);
				setClient(client);
			}
		}

		// subType
		setSubType(AEFactureUtil.FactureSubType.valueOf(jsonObject.optLong(JSONKey.subType)));

		// dateOfExpiry
		setDateOfExpiry(AEDateUtil.parseDateStrict(jsonObject.optString(JSONKey.dateOfExpiry)));

		// facture items
		if(jsonObject.has(JSONKey.factureItems)) {
			JSONArray itemsJSON = jsonObject.getJSONArray(JSONKey.factureItems);
			if(itemsJSON != null) {
				AEFactureItemsList factureItems = new AEFactureItemsList();
				factureItems.create(itemsJSON);
				setFactureItems(factureItems);
			}
		}

		// public static final String srcDoc = "srcDoc";
		if(jsonObject.has(JSONKey.srcDoc)) {
			JSONObject srcDocJson = jsonObject.optJSONObject(JSONKey.srcDoc);
			if(srcDocJson != null) {
				AEDocumentDescriptorImp srcDoc = new AEDocumentDescriptorImp();
				srcDoc.create(srcDocJson);
				setSrcDoc(srcDoc);
			}
		}

		// paymentTermsId
		long paymentTermsId = jsonObject.optLong(JSONKey.paymentTermsId);
		if(paymentTermsId > 0) {
			try {
				setPaymentTerms(AEFactureUtil.getPaymentTerms(paymentTermsId));
			} catch (Exception e) {
			}
		}

		// public static final String payments = "payments";
		if(jsonObject.has(JSONKey.payments)) {
			JSONArray paymentsJsonArray = jsonObject.optJSONArray(JSONKey.payments);
			if(paymentsJsonArray != null) {
				AEPaymentsList pList = new AEPaymentsList();
				pList.create(paymentsJsonArray);
				setPayments(pList);
			}
		}

		// public static final String state = "state";
		if(jsonObject.has(JSONKey.state)) {
			int stateId = jsonObject.optInt(JSONKey.state);
			AEFactureUtil.FactureState state = AEFactureUtil.FactureState.valueOf(stateId);
			setState(state);
		}

		// public static final String advanceMode = "advanceMode";
		if(jsonObject.has(JSONKey.advanceMode)) {
			int advanceModeId = jsonObject.optInt(JSONKey.advanceMode);
			AEFactureUtil.PercentAmountMode mode = AEFactureUtil.PercentAmountMode.valueOf(advanceModeId);
			setAdvanceMode(mode);
		}

		// public static final String advancePercent = "advancePercent";
		try {
			setAdvanceAmount(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.advanceAmount));
		} catch (Exception e) {
		}

		// public static final String advanceAmount = "advanceAmount";
		try {
			setAdvancePercent(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.advancePercent));
		} catch (Exception e) {
		}

		// public static final String discountMode = "discountMode";
		if(jsonObject.has(JSONKey.discountMode)) {
			int advanceModeId = jsonObject.optInt(JSONKey.discountMode);
			AEFactureUtil.PercentAmountMode mode = AEFactureUtil.PercentAmountMode.valueOf(advanceModeId);
			setDiscountMode(mode);
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

		// public static final String shipping = "shipping";
		if(jsonObject.has(JSONKey.shipping)) {
			JSONObject shippingJSON = jsonObject.optJSONObject(JSONKey.shipping);
			if(shippingJSON != null) {
				grantShipping().create(shippingJSON);
			}
		}

		// public static final String itemsDiscountAmount = "itemsDiscountAmount";
		try {
			setItemsDiscountAmount(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.itemsDiscountAmount));
		} catch (Exception e) {
		}

		// public static final String brutoAmount = "brutoAmount";
		try {
			setBrutoAmount(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.brutoAmount));
		} catch (Exception e) {
		}

		// paidAdvance
		try {
			setPaidAmount(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.paidAmount));
		} catch (Exception e) {
		}
	}

	public AEPaymentTerms getPaymentTerms() {
		return paymentTerms;
	}

	public void setPaymentTerms(AEPaymentTerms paymentTerms) {
		this.paymentTerms = paymentTerms;
	}

	public AEVatItem getShipping() {
		return shipping;
	}

	public AEVatItem grantShipping() {
		if(shipping == null) {
			shipping = new AEVatItem();
		}
		return shipping;
	}

	public void setShipping(AEVatItem shipping) {
		this.shipping = shipping;
	}

	public AEClient getClient() {
		return client;
	}

	public AEClient grantClient() {
		if(client == null) {
			client = new AEClient();
		}
		return client;
	}

	public void setClient(AEClient client) {
		this.client = client;
	}

	public AEFactureItemsList getFactureItems() {
		return factureItems;
	}

	public AEFactureItemsList grantFactureItems() {
		if(factureItems == null) {
			factureItems = new AEFactureItemsList();
		}
		return factureItems;
	}

	public void setFactureItems(AEFactureItemsList factureItems) {
		this.factureItems = factureItems;
	}

	public AEFactureUtil.FactureSubType getSubType() {
		return subType;
	}

	public void setSubType(AEFactureUtil.FactureSubType subType) {
		this.subType = subType;
	}

	/**
	 * Calculates the whole document
	 */
	@Override
	public void calculate() throws AEException {
		// recalculate items
		reCalculateItems();

		// calculate the itemsDiscount
		calculateItemsDiscount();

		// calculate bruto amount
		calculateBrutoAmount();

		// calculate shipment
		calculateShipping();

		// calculate taxable amount
		calculateTaxableAmount();

		// tax
		tax();
	}

	/**
	 * Recalculate items
	 */
	public void reCalculateItems() {
		// recalculate items
		AEFactureItemsList fItems = getFactureItems().filter(AECalculableItemFilter.getInstance());
		for (Iterator<AEDocumentItem> iterator = fItems.iterator(); iterator.hasNext();) {
			AEFactureItem item = (AEFactureItem) iterator.next();
			item.calculate();
		}
	}

	public void calculateItemsDiscount() {
		double itemsDiscount = 0.0;
		AEFactureItemsList fItems = getFactureItems().filter(AECalculableItemFilter.getInstance());
		for (Iterator<AEDocumentItem> iterator = fItems.iterator(); iterator.hasNext();) {
			AEFactureItem item = (AEFactureItem) iterator.next();
			itemsDiscount += item.getDiscountAmount();
		}
		setItemsDiscountAmount(AEMath.round(itemsDiscount, 2));
	}

	public void calculateBrutoAmount() {
		double brutoAmount = 0.0;
		AEFactureItemsList fItems = getFactureItems().filter(AECalculableItemFilter.getInstance());
		for (Iterator<AEDocumentItem> iterator = fItems.iterator(); iterator.hasNext();) {
			AEFactureItem item = (AEFactureItem) iterator.next();
			brutoAmount += item.getBrutoAmount();
		}
		setBrutoAmount(AEMath.round(brutoAmount, 2));
	}

	public void calculateShipping() {
		AEVatItem shipping = grantShipping();

		double vatAmount = shipping.getTaxableAmount() * shipping.getRate() / 100;
		shipping.setVatAmount(AEMath.round(vatAmount, 2));

		double amount = shipping.getTaxableAmount() + shipping.getVatAmount();
		shipping.setAmount(AEMath.round(amount, 2));
	}

	public void calculateTaxableAmount() {
		double taxableAmount = 
			getBrutoAmount() - getDiscountAmount() + grantShipping().getTaxableAmount();
		setTaxableAmount(AEMath.round(taxableAmount, 2));
	}

	public void tax() throws AEException {
		// calculate VatAmount
		AEVatItemsList vatDetails = getVatDetails();
		double vatAmount = vatDetails.calculateVatAmount();
		setVatAmount(AEMath.round(vatAmount, 2));

		// calculate amount
		double amount = getTaxableAmount() + getVatAmount();
		setAmount(AEMath.round(amount, 2)); 
	}

	public AEVatItemsList getVatDetails() throws AEException {
		AEVatItemsList vatDetails = new AEVatItemsList();

		// group items by VAT
		AEFactureItemsList fItems = getFactureItems().filter(AECalculableItemFilter.getInstance());
		for (Iterator<AEDocumentItem> iterator = fItems.iterator(); iterator.hasNext();) {
			AEFactureItem item = (AEFactureItem) iterator.next();
			if(item.isVatDefined()) {
				AEVatItem itemVat = (AEVatItem) AEObjectUtil.deepCopy(item.getVatItem());
				AEVatItem registeredVat = vatDetails.getById(itemVat.getID());
				if(registeredVat != null) {
					// add taxable amount
					double sumTaxableAmount = registeredVat.getTaxableAmount() + item.getTaxableAmount();
					registeredVat.setTaxableAmount(AEMath.round(sumTaxableAmount, 2));
				} else {
					// add new vat item
					itemVat.setTaxableAmount(item.getTaxableAmount());
					vatDetails.add(itemVat);
				}
			}
		}

		// shipping: there is shipping if sgipping taxable amount is different than zero
		if(getShipping() != null && !AEMath.isZeroAmount(getShipping().getTaxableAmount())) {
			AEVatItem sgippingVat = (AEVatItem) AEObjectUtil.deepCopy(getShipping());
			AEVatItem registeredVat = vatDetails.getById(sgippingVat.getID());
			if(registeredVat != null) {
				// add taxable amount
				double sumTaxableAmount = registeredVat.getTaxableAmount() + sgippingVat.getTaxableAmount();
				registeredVat.setTaxableAmount(AEMath.round(sumTaxableAmount, 2));
			} else {
				// add new vat item
				vatDetails.add(sgippingVat);
			}
		}

		// tax all vat items
		for (AEVatItem vatItem : vatDetails) {
			// vatAmount
			double vatAmount = vatItem.getTaxableAmount() * vatItem.getRate() / 100;
			vatItem.setVatAmount(AEMath.round(vatAmount, 2));

			// amount
			double amount = vatItem.getTaxableAmount() + vatItem.getVatAmount();
			vatItem.setAmount(AEMath.round(amount, 2));
		}

		return vatDetails;
	}

	public final JSONArray getVatItemsView() throws AEException {
		AEVatItemsList vatItems = getVatDetails();
		JSONArray vatItemsView = new JSONArray();
		try {
			for (AEVatItem vatItem : vatItems) {
				vatItemsView.put(vatItem.toJSONObject());
			}
		} catch (JSONException e) {
			throw new AEException(e);
		}
		return vatItemsView;
	}
	
	/**
	 * 
	 * @throws AEException
	 */
	public void calculateAdvance() throws AEException {
		if(AEFactureUtil.PercentAmountMode.PERCENT.equals(getAdvanceMode())) {
			double advPercent = getAdvancePercent();
			double advAmount = getAmount() * advPercent / 100;
			setAdvanceAmount(AEMath.round(advAmount, 2));
		} else if(AEFactureUtil.PercentAmountMode.AMOUNT.equals(getAdvanceMode())) {
			double advAmount = getAdvanceAmount();
			double advPercent = 0.0;
			if(getAmount() != 0.0) {
				advPercent = advAmount * 100 / getAmount();
			}
			setAdvancePercent(AEMath.round(advPercent, 2));
		} else {
			setAdvancePercent(0.0);
			setAdvanceAmount(0.0);
		}
	}

	public static AEFactureDescr lazyDescriptor(long id) {
		AEFactureDescr docDescr = new AEFactureDescr();
		docDescr.setID(id);
		return docDescr;
	}

	public AEFactureUtil.PayableType getInitialPayableType() {
		AEFactureUtil.PayableType pt = AEFactureUtil.PayableType.NA;
		if(isFacture()) {
			pt = AEFactureUtil.PayableType.REQUISITION;
		} else {
			pt = AEFactureUtil.PayableType.SCHEDULE;
		}
		return pt;
	}

	public AEFactureUtil.PayableType getInitialAdvancePayableType() {
		AEFactureUtil.PayableType pt = isDevis() ?
				AEFactureUtil.PayableType.SCHEDULE : // will be changed to requisition when devis is accepted
					AEFactureUtil.PayableType.REQUISITION;
		return pt;
	}

	public boolean isDevis() {
		return AEDocumentType.System.AEDevisSale.getSystemID() == getType().getSystemID();
	}

	public boolean isFacture() {
		return AEDocumentType.System.AEFactureSale.getSystemID() == getType().getSystemID();
	}

	public boolean isAdvance() {
		return 	AEFactureUtil.FactureSubType.ADVANCE.equals(getSubType());
	}
	
	public boolean isRegular() {
		return 	AEFactureUtil.FactureSubType.REGULAR.equals(getSubType());
	}
	
	public boolean isCreditNote() {
		return 	AEFactureUtil.FactureSubType.CREDIT_NOTE.equals(getSubType());
	}
	
	public boolean isFormalFacture() {
		return 
			isFacture() 
			&& (AEFactureUtil.FactureState.VALIDATED.equals(getState())
					|| AEFactureUtil.FactureState.PAID.equals(getState()));
	}
	
	public boolean hasAdvance() {
		boolean bRet = false;
		if(getAdvanceMode() != null 
				&& getAdvanceMode() != AEFactureUtil.PercentAmountMode.NA 
				&& !AEMath.isZeroAmount(getAdvanceAmount())) {

			bRet = true;
		}
		return bRet;
	}

	public double getPaidAmount() {
		return paidAmount;
	}

	public void setPaidAmount(double paidAmount) {
		this.paidAmount = paidAmount;
	}

	@Override
	public AEDescriptor getDescriptor() {
		AEFactureDescr fDescr = new AEFactureDescr((AEDocumentDescriptor) super.getDescriptor());

		fDescr.setState(this.getState());
		fDescr.setSubType(this.getSubType());

		return fDescr;
	}

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}
	
	/**
	 * Rules:
	 * - devis in progress is not locked,
	 * - devis accepted is not locked
	 * - device accepted with paid advance, advance is locked
	 * - devis invoiced is not locked, after editing becomes in state accepted
	 * - facture draft is not locked
	 * - facture validated or paid is locked
	 * - advance invoice is locked
	 */
	public void calcLocked() {
		boolean locked = false;
		if(isFormalFacture()) {
			locked = true;
		} else if(AEFactureUtil.FactureSubType.ADVANCE.equals(getSubType())) {
			locked = true;
		}
		setLocked(locked);
	}
}
