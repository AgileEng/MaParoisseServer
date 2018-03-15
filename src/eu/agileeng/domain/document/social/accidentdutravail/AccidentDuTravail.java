/**
 * 
 */
package eu.agileeng.domain.document.social.accidentdutravail;

import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.social.AESocialDocument;
import eu.agileeng.util.AEDateUtil;

/**
 * @author vvatov
 *
 */
public class AccidentDuTravail extends AESocialDocument {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1482570569286941612L;

	static public enum JSONKey {
		typeDeDeclaration,
		typeArret,
		dateDeAccident,
		dateDuDernier,
		travailNonRepris,
		dateDeFin,
		dateDeReprise,
		miTemps;
	}
	
	static public enum TypeDeDeclaration {
		NA(0),
		Initial(10),
		Final(20),
		DeRecute(30),
		DeProlongation(40);
				
		private long typeID;
		
		private TypeDeDeclaration(long typeID) {
			this.typeID = typeID;
		}
		
		public final long getTypeID() {
			return this.typeID;
		}
		
		public static TypeDeDeclaration valueOf(long typeID) {
			TypeDeDeclaration ret = NA;
			for (TypeDeDeclaration inst : TypeDeDeclaration.values()) {
				if(inst.getTypeID() == typeID) {
					ret = inst;
					break;
				}
			}
			return ret;
		}
	}
	
	static public enum TypeArret {
		NA(0),
		Accident(10),
		Maladie(20),
		Maternite(30),
		Paternite(40);
				
		private long typeID;
		
		private TypeArret(long typeID) {
			this.typeID = typeID;
		}
		
		public final long getTypeID() {
			return this.typeID;
		}
		
		public static TypeArret valueOf(long typeID) {
			TypeArret ret = NA;
			for (TypeArret inst : TypeArret.values()) {
				if(inst.getTypeID() == typeID) {
					ret = inst;
					break;
				}
			}
			return ret;
		}
	}
	
	private TypeDeDeclaration typeDeDeclaration;
	
	private TypeArret typeArret;
	
	private Date dateDeAccident;
	
	private Date dateDuDernier;
	
	private boolean travailNonRepris = true;
	
	private Date dateDeFin;
	
	private Date dateDeReprise;
	
	private boolean miTemps;
	
	/**
	 * No arg constructor.
	 * Try to use it only during DB Fetching
	 */
	public AccidentDuTravail() {
		this(AEDocumentType.valueOf(AEDocumentType.System.NA));
	}
	
	public AccidentDuTravail(AEDocumentType docType) {
		super(docType);
	}

	public TypeDeDeclaration getTypeDeDeclaration() {
		return typeDeDeclaration;
	}

	public void setTypeDeDeclaration(TypeDeDeclaration typeDeDeclaration) {
		this.typeDeDeclaration = typeDeDeclaration;
	}

	public TypeArret getTypeArret() {
		return typeArret;
	}

	public void setTypeArret(TypeArret typeArret) {
		this.typeArret = typeArret;
	}

	public Date getDateDeAccident() {
		return dateDeAccident;
	}

	public void setDateDeAccident(Date dateDeAccident) {
		this.dateDeAccident = dateDeAccident;
	}

	public Date getDateDuDernier() {
		return dateDuDernier;
	}

	public void setDateDuDernier(Date dateDuDernier) {
		this.dateDuDernier = dateDuDernier;
	}

	public boolean isTravailNonRepris() {
		return travailNonRepris;
	}

	public void setTravailNonRepris(boolean travailNonRepris) {
		this.travailNonRepris = travailNonRepris;
	}

	public Date getDateDeFin() {
		return dateDeFin;
	}

	public void setDateDeFin(Date dateDeFin) {
		this.dateDeFin = dateDeFin;
	}

	public Date getDateDeReprise() {
		return dateDeReprise;
	}

	public void setDateDeReprise(Date dateDeReprise) {
		this.dateDeReprise = dateDeReprise;
	}

	public boolean isMiTemps() {
		return miTemps;
	}

	public void setMiTemps(boolean miTemps) {
		this.miTemps = miTemps;
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
//		private TypeDeDeclaration typeDeDeclaration;
		if(getTypeDeDeclaration() != null) {
			json.put(JSONKey.typeDeDeclaration.toString(), getTypeDeDeclaration().getTypeID());
		}
		
//		private TypeArret typeArret;
		if(getTypeArret() != null) {
			json.put(JSONKey.typeArret.toString(), getTypeArret().getTypeID());
		}
		
//		private Date dateDeAccident;
		if(getDateDeAccident() != null) {
			json.put(JSONKey.dateDeAccident.toString(), AEDateUtil.formatToSystem(getDateDeAccident()));
		}
		
//		private Date dateDuDernier;
		if(getDateDuDernier() != null) {
			json.put(JSONKey.dateDuDernier.toString(), AEDateUtil.formatToSystem(getDateDuDernier()));
		}
		
//		private boolean travailNonRepris = true;
		json.put(JSONKey.travailNonRepris.toString(), isTravailNonRepris());
		
//		private Date dateDeFin;
		if(getDateDeFin() != null) {
			json.put(JSONKey.dateDeFin.toString(), AEDateUtil.formatToSystem(getDateDeFin()));
		}
		
//		private Date dateDeReprise;
		if(getDateDeReprise() != null) {
			json.put(JSONKey.dateDeReprise.toString(), AEDateUtil.formatToSystem(getDateDeReprise()));
		}
		
//		private boolean miTemps;
		json.put(JSONKey.miTemps.toString(), isMiTemps());
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		if(jsonObject.has(JSONKey.typeDeDeclaration.toString())) {
			setTypeDeDeclaration(TypeDeDeclaration.valueOf(jsonObject.optLong(JSONKey.typeDeDeclaration.toString())));
		}
		
		if(jsonObject.has(JSONKey.typeArret.toString())) {
			setTypeArret(TypeArret.valueOf(jsonObject.optLong(JSONKey.typeArret.toString())));
		}
		
		if(jsonObject.has(JSONKey.dateDeAccident.toString())) {
			setDateDeAccident(AEDateUtil.parseDateStrict(jsonObject.optString(JSONKey.dateDeAccident.toString())));
		}
		
		if(jsonObject.has(JSONKey.dateDuDernier.toString())) {
			setDateDuDernier(AEDateUtil.parseDateStrict(jsonObject.optString(JSONKey.dateDuDernier.toString())));
		}
		
		if(jsonObject.has(JSONKey.travailNonRepris.toString())) {
			setTravailNonRepris(jsonObject.optBoolean(JSONKey.travailNonRepris.toString()));
		}
		
		if(jsonObject.has(JSONKey.dateDeFin.toString())) {
			setDateDeFin(AEDateUtil.parseDateStrict(jsonObject.optString(JSONKey.dateDeFin.toString())));
		}
		
		if(jsonObject.has(JSONKey.dateDeReprise.toString())) {
			setDateDeReprise(AEDateUtil.parseDateStrict(jsonObject.optString(JSONKey.dateDeReprise.toString())));
		}
		
		if(jsonObject.has(JSONKey.miTemps.toString())) {
			setMiTemps(jsonObject.optBoolean(JSONKey.miTemps.toString()));
		}
	}
}
