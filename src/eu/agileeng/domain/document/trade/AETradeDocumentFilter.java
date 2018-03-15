package eu.agileeng.domain.document.trade;

import java.util.Date;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.accbureau.AEApp;
import eu.agileeng.domain.AEDescriptorsList;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.json.JSONUtil;

public class AETradeDocumentFilter extends AEDomainObject {

	static public class JSONKey {
		public static final String montantHTFrom = "montantHTFrom";
		public static final String montantHTTo = "montantHTTo";
		public static final String montantTTCFrom = "montantTTCFrom";
		public static final String montantTTCTo = "montantTTCTo";
		public static final String itemAccIds = "itemAccIds";
		public static final String viewId = "viewId";
	}
	
	private Long ownerId;
	
	private Long docType;
	
	private String number;
	
	private Date docDateFrom;
	
	private Date docDateTo;
	
	private Long companyId;
	
	private String companyName;
	
	private String description;
	
	private Double montantHTFrom;

	private Double montantHTTo;
	
	private Double montantTTCFrom;
	
	private Double montantTTCTo;
	
	private AEDescriptorsList itemAccDescrList;
	
	private View view = View.COARSE;
	
	/**
	 * Percent or Amount mode
	 * 
	 * @author vvatov
	 */
	static public enum View {
		NA(0L),
		COARSE(10L),
		DETAILED(20L);
				
		private long viewId;
		
		private View(long viewId) {
			this.viewId = viewId;
		}
		
		public final long getViewId() {
			return this.viewId;
		}
		
		public static View valueOf(long viewId) {
			View ret = null;
			for (View inst : View.values()) {
				if(inst.getViewId() == viewId) {
					ret = inst;
					break;
				}
			}
			if(ret == null) {
				ret = NA;
			}
			return ret;
		}
	}
	
	public AETradeDocumentFilter() {
		super(DomainClass.AETradeDocumentFilter);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -1796242984014021124L;

	public Long getDocType() {
		return docType;
	}

	public void setDocType(Long docType) {
		this.docType = docType;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public Date getDocDateFrom() {
		return docDateFrom;
	}

	public void setDocDateFrom(Date docDateFrom) {
		this.docDateFrom = docDateFrom;
	}

	public Date getDocDateTo() {
		return docDateTo;
	}

	public void setDocDateTo(Date docDateTo) {
		this.docDateTo = docDateTo;
	}

	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// docType
		long docType = jsonObject.optLong("docType");
		if(docType > 0) {
			setDocType(docType);
		}
		
		// docDateFrom
		if(jsonObject.has("docDateFrom")) {
			Date dateFrom = AEDateUtil.parseDateStrict(jsonObject.optString("docDateFrom"));
			if(dateFrom != null) {
				setDocDateFrom(dateFrom);
			}
		}
		
		// docDateTo
		if(jsonObject.has("docDateTo")) {
			Date dateTo = AEDateUtil.parseDateStrict(jsonObject.optString("docDateTo"));
			if(dateTo != null) {
				setDocDateTo(dateTo);
			}
		}
		
		// ownerId
		long ownerId = jsonObject.optLong("ownerId");
		if(ownerId > 0) {
			setOwnerId(ownerId);
		}
		
		// number
		String number = jsonObject.optString("number");
		if(!AEStringUtil.isEmpty(number)) {
			setNumber(number);
		}
		
		// companyId
		long companyId = jsonObject.optLong("companyId");
		if(companyId > 0) {
			setCompanyId(companyId);
		}
		
		// companyName
		String companyName = jsonObject.optString("companyName");
		if(!AEStringUtil.isEmpty(companyName)) {
			setCompanyName(companyName);
		}
		
		// description
		String description = jsonObject.optString("description");
		if(!AEStringUtil.isEmpty(description)) {
			setDescription(description);
		}
		
		//private Double montantHTFrom;
		if(jsonObject.has(JSONKey.montantHTFrom)) {
			try {
				setMontantHTFrom(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.montantHTFrom));
			} catch (AEException e) {
				AEApp.logger().error(e.getMessage(), e);
			}
		}

		//private Double montantHTTo;
		if(jsonObject.has(JSONKey.montantHTTo)) {
			try {
				setMontantHTTo(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.montantHTTo));
			} catch (AEException e) {
				AEApp.logger().error(e.getMessage(), e);
			}
		}
		
		//private Double montantTTCFrom;
		if(jsonObject.has(JSONKey.montantTTCFrom)) {
			try {
				setMontantTTCFrom(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.montantTTCFrom));
			} catch (AEException e) {
				AEApp.logger().error(e.getMessage(), e);
			}
		}
		
		//private Double montantTTCTo;
		if(jsonObject.has(JSONKey.montantTTCTo)) {
			try {
				setMontantTTCTo(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.montantTTCTo));
			} catch (AEException e) {
				AEApp.logger().error(e.getMessage(), e);
			}
		}
		
		//private AEDescriptorsList itemAccDescrList;
		if(jsonObject.has(JSONKey.itemAccIds)) {
			JSONArray itemAccIdsArray = jsonObject.optJSONArray(JSONKey.itemAccIds);
			if(itemAccIdsArray != null && itemAccIdsArray.length() > 0) {
				itemAccDescrList = new AEDescriptorsList();
				for (int i = 0; i < itemAccIdsArray.length(); i++) {
					try {
						long accId = itemAccIdsArray.getLong(i);
						itemAccDescrList.add(AccAccount.lazyDescriptor(accId));
					} catch(JSONException e) {}
				}
			}
		}
		
		//private View view = View.COARSE;
		if(jsonObject.has(JSONKey.viewId)) {
			view = View.valueOf(jsonObject.optLong(JSONKey.viewId));
		}
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();

		// docType
		if(getDocType() != null) {
			json.put("docType", getDocType().longValue());
		}
		
		// docDateFrom
		if(getDocDateFrom() != null) {
			json.put("docDateFrom", AEDateUtil.convertToString(getDocDateFrom(), AEDateUtil.SYSTEM_DATE_FORMAT));
		}
		
		// docDateTo
		if(getDocDateTo() != null) {
			json.put("docDateTo", AEDateUtil.convertToString(getDocDateTo(), AEDateUtil.SYSTEM_DATE_FORMAT));
		}
		
		// ownerId
		if(getOwnerId() != null) {
			json.put("ownerId", getOwnerId().longValue());
		}
		
		// number
		if(!AEStringUtil.isEmpty(getNumber())) {
			json.put("number", getNumber());
		}
		
		// companyId
		if(getCompanyId() != null) {
			json.put("companyId", getCompanyId());
		}
		
		// companyName
		if(!AEStringUtil.isEmpty(getCompanyName())) {
			json.put("companyName", getCompanyName());
		}
		
		// description
		if(!AEStringUtil.isEmpty(getDescription())) {
			json.put("description", getDescription());
		}
		
		//private Double montantHTFrom;
		if(getMontantHTFrom() != null) {
			json.put(JSONKey.montantHTFrom, AEMath.toAmountString(getMontantHTFrom()));
		}

		//private Double montantHTTo;
		if(getMontantHTTo() != null) {
			json.put(JSONKey.montantHTTo, AEMath.toAmountString(getMontantHTTo()));
		}
		
		//private Double montantTTCFrom;
		if(getMontantTTCFrom() != null) {
			json.put(JSONKey.montantTTCFrom, AEMath.toAmountString(getMontantTTCFrom()));
		}
		
		//private Double montantTTCTo;
		if(getMontantTTCTo() != null) {
			json.put(JSONKey.montantTTCTo, AEMath.toAmountString(getMontantTTCTo()));
		}
		
		//private AEDescriptorsList itemAccDescrList;
		if(getItemAccDescrList() != null) {
			json.put(JSONKey.itemAccIds, getItemAccDescrList().toJSONArray());
		}
		
		//private View view = View.COARSE;
		json.put(JSONKey.viewId, view.getViewId());
		
		return json;
	}

	public Long getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}

	public Long getCompanyId() {
		return companyId;
	}

	public void setCompanyId(Long companyId) {
		this.companyId = companyId;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public Double getMontantHTFrom() {
		return montantHTFrom;
	}

	public void setMontantHTFrom(Double montantHTFrom) {
		this.montantHTFrom = montantHTFrom;
	}

	public Double getMontantHTTo() {
		return montantHTTo;
	}

	public void setMontantHTTo(Double montantHTTo) {
		this.montantHTTo = montantHTTo;
	}

	public Double getMontantTTCFrom() {
		return montantTTCFrom;
	}

	public void setMontantTTCFrom(Double montantTTCFrom) {
		this.montantTTCFrom = montantTTCFrom;
	}

	public Double getMontantTTCTo() {
		return montantTTCTo;
	}

	public void setMontantTTCTo(Double montantTTCTo) {
		this.montantTTCTo = montantTTCTo;
	}

	public AEDescriptorsList getItemAccDescrList() {
		return itemAccDescrList;
	}

	public void setItemAccDescrList(AEDescriptorsList itemAccDescrList) {
		this.itemAccDescrList = itemAccDescrList;
	}

	public View getView() {
		return view;
	}

	public void setView(View view) {
		this.view = view;
	}
}
