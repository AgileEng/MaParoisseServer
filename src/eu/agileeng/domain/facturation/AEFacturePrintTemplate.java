package eu.agileeng.domain.facturation;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.imp.AEDescriptorImp;

public class AEFacturePrintTemplate extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8756672755705925276L;
	
	static public class JSONKey {
		public static final String printTemplate = "printTemplate";
		public static final String printTemplates = "printTemplates";
		public static final String client = "client";
		public static final String body = "body";
		public static final String issuer = "issuer";
		public static final String byDefault = "byDefault";
		public static final String forReminder = "forReminder";
		public static final String forDevis = "forDevis";
		public static final String forFacture = "forFacture";
	}
	
	/**
	 * Mask for body's fields
	 */
	public static final long bodyQty = 0x01;
	
	public static final long bodyDiscount = 0x01 << 1;
	
	public static final long bodyCode = 0x01 << 2;
	
	public static final long bodyUnitPrice = 0x01 << 3;
	
	public static final long bodyName = 0x01 << 4;
	
	public static final long bodyDescription = 0x01 << 5;
	
	public long body = 	Long.MAX_VALUE;
	
	/**
	 * Mask for issuer's fields
	 */
	private long issuer = Long.MAX_VALUE;
	
	/**
	 * Mask for client's field
	 */
	private long client = Long.MAX_VALUE;
	
	/**
	 * Whether this template is the default one or not.
	 */
	private boolean byDefault;
	
	/**
	 * Whether this template will be used for reminders or not
	 */
	private boolean forReminder;
	
	/**
	 * Whether this template will be used for devises or not
	 */
	private boolean forDevis;
	
	/**
	 * Whether this template will be used for factures or not
	 */
	private boolean forFacture;
	
	public AEFacturePrintTemplate() {
		super(DomainClass.AEFacturePrintTemplate);
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		// body
		json.put(JSONKey.body, getBody());
		
		// issuer
		json.put(JSONKey.issuer, getIssuer());
		
		// client
		json.put(JSONKey.client, getClient());
		
		// byDefault
		json.put(JSONKey.byDefault, isByDefault());
		
		// forReminder
		json.put(JSONKey.forReminder, isForReminder());
		
		// forDevis
		json.put(JSONKey.forDevis, isForDevis());
		
		// forFacture
		json.put(JSONKey.forFacture, isForFacture());
		
		return json;
	}
	
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// body
		setBody(jsonObject.optLong(JSONKey.body));
		
		// issuer
		setIssuer(jsonObject.optLong(JSONKey.issuer));
		
		// client
		setClient(jsonObject.optLong(JSONKey.client));
		
		// byDefault
		setByDefault(jsonObject.optBoolean(JSONKey.byDefault));
		
		// forReminder
		setForReminder(jsonObject.optBoolean(JSONKey.forReminder));
		
		// forDevis
		setForDevis(jsonObject.optBoolean(JSONKey.forDevis));
		
		// forFacture
		setForFacture(jsonObject.optBoolean(JSONKey.forFacture));
	}

	public long getBody() {
		return body;
	}

	public void setBody(long body) {
		this.body = body;
	}

	public long getIssuer() {
		return issuer;
	}

	public void setIssuer(long issuer) {
		this.issuer = issuer;
	}

	public long getClient() {
		return client;
	}

	public void setClient(long client) {
		this.client = client;
	}
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.AEFacturePrintTemplate);
	}

	public boolean isByDefault() {
		return byDefault;
	}

	public void setByDefault(boolean byDefault) {
		this.byDefault = byDefault;
	}

	public boolean isForReminder() {
		return forReminder;
	}

	public void setForReminder(boolean forReminder) {
		this.forReminder = forReminder;
	}

	public boolean isForDevis() {
		return forDevis;
	}

	public void setForDevis(boolean forDevis) {
		this.forDevis = forDevis;
	}

	public boolean isForFacture() {
		return forFacture;
	}

	public void setForFacture(boolean forFacture) {
		this.forFacture = forFacture;
	}
}
