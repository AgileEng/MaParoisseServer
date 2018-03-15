package eu.agileeng.domain.facturation;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.domain.measurement.UnitOfMeasurement;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.json.JSONUtil;

public class AEArticle extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4938258347726984270L;
	
	static public class JSONKey {
		public static final String familleId = "familleId";
		public static final String familleName = "familleName";
		
		public static final String uomId = "uomId";
		public static final String uomCode = "uomCode";
		
		public static final String accountId = "accountId";
		public static final String accountCode = "accountCode";
		public static final String accountName = "accountName";
		
		public static final String prixHT = "prixHT";
		
		public static final String vatId = "vatId";
		public static final String vatCode = "vatCode";
		public static final String vatRate = "vatRate";
		
		public static final String prixTTC = "prixTTC";
	}
	
	private long codeNumber;
	
	private AEDescriptive group; // Famille
	
	private AEDescriptive uom; // Unite
	
	private AEDescriptive account; // Compte Comptable
	
	private double priceExVat; // Prix HT, the price without VAT
	
	private AEDescriptive vatDescriptor; // Taux de TVA
	
	private double vatRate; // Taux de TVA
	
	private double priceInVat; // Prix TTC, the price with VAT 
	
	public AEArticle() {
		super(DomainClass.AEArticle);
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// famille
		AEDescriptorImp group = new AEDescriptorImp();
		group.setID(jsonObject.optLong(JSONKey.familleId));
		group.setName(jsonObject.optString(JSONKey.familleName));
		setGroup(group);

		// UOM
		UnitOfMeasurement uom = UnitOfMeasurement.getByID(jsonObject.optLong(JSONKey.uomId));
		if(uom != null) {
			setUom(uom.getDescriptor());
		}
		
		// Account
		long accountId = jsonObject.optLong(JSONKey.accountId);
		if(accountId > 0) {
			AEDescriptor account = AccAccount.lazyDescriptor(accountId);
			setAccount(account);
		}
		
		// priceExVat
		try {
			setPriceExVat(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.prixHT));
		} catch (Exception e) {
		}
		
		// vat 
		long vatId = jsonObject.optLong(JSONKey.vatId);
		if(vatId > 0) {
			AEDescriptor vat = new AEDescriptorImp();
			vat.setID(vatId);
			vat.setCode(jsonObject.optString(JSONKey.vatCode));
			setVatDescriptor(vat);
		}
		
		// vat rate
		try {
			setVatRate(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.vatRate));
		} catch (Exception e) {
		}
		
		// priceExVat
		try {
			setPriceInVat(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.prixTTC));
		} catch (Exception e) {
		}
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		// famille
		if(getGroup() != null) {
			json.put(JSONKey.familleId, getGroup().getDescriptor().getID());
			json.put(JSONKey.familleName, getGroup().getDescriptor().getName());
		}

		// UOM
		if(getUom() != null) {
			json.put(JSONKey.uomId, getUom().getDescriptor().getID());
			json.put(JSONKey.uomCode, getUom().getDescriptor().getCode());
		}
		
		// Account
		if(getAccount() != null) {
			json.put(JSONKey.accountId, getAccount().getDescriptor().getID());
		}
		
		// priceExVat
		json.put(JSONKey.prixHT, AEMath.toPrice3String(getPriceExVat()));
		
		// vat 
		if(getVatDescriptor() != null) {
			json.put(JSONKey.vatId, getVatDescriptor().getDescriptor().getID());
		}
		
		// vat rate
		json.put(JSONKey.vatRate, AEMath.toPrice2String(getVatRate()));
		
		// priceInVat
		json.put(JSONKey.prixTTC, AEMath.toPrice3String(getPriceInVat()));
		
		return json;
	}
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.AEArticle);
	}

	public AEDescriptive getGroup() {
		return group;
	}

	public void setGroup(AEDescriptive group) {
		this.group = group;
	}

	public AEDescriptive getUom() {
		return uom;
	}

	public void setUom(AEDescriptive uom) {
		this.uom = uom;
	}

	public AEDescriptive getAccount() {
		return account;
	}

	public void setAccount(AEDescriptive account) {
		this.account = account;
	}

	public double getPriceExVat() {
		return priceExVat;
	}

	public void setPriceExVat(double priceExVat) {
		this.priceExVat = priceExVat;
	}

	public AEDescriptive getVatDescriptor() {
		return vatDescriptor;
	}

	public void setVatDescriptor(AEDescriptive vatDescriptor) {
		this.vatDescriptor = vatDescriptor;
	}

	public double getVatRate() {
		return vatRate;
	}

	public void setVatRate(double vatRate) {
		this.vatRate = vatRate;
	}

	public double getPriceInVat() {
		return priceInVat;
	}

	public void setPriceInVat(double priceInVat) {
		this.priceInVat = priceInVat;
	}

	public long getCodeNumber() {
		return codeNumber;
	}

	public void setCodeNumber(long codeNumber) {
		this.codeNumber = codeNumber;
	}
}
