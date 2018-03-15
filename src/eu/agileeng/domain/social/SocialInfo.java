package eu.agileeng.domain.social;

import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.document.social.SocialTemplatesSet;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEStringUtil;

public class SocialInfo extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1999668493429400470L;

	private AEDescriptive salaryGrid;
	
	private double fullTimeWeeklyHours = 35.0;
	
	private Date nightWorkBegin;
	
	private Date nightWorkEnd;
	
	private Date startDate;
	
	private Date scheduleStartDate;
	
	private String logoUrl;
	
	private boolean generateContract;
	
	private String societe;
	
	private String societeTerm;
	
	private String password;
	
	private SocialTemplatesSet templatesSet;
	
	private String urssafNumber;
	
	private String urssafPretext;
	
	private String urssafText;
	
	private String activitePrincipale;
	
	private String activitePrincipaleText;
	
	private boolean cctv;
	
	private Long emplIdSeed;
	
	static public enum JSONKey {
		socialInfo,
		salaryGridId,
		fullTimeWeeklyHours,
		nightWorkBegin,
		nightWorkEnd,
		startDate,
		scheduleStartDate,
		logoUrl,
		generateContract,
		societe,
		societeTerm,
		password,
		templatesSetId,
		urssafNumber,
		urssafPretext,
		activitePrincipale,
		cctv,
		urssafText,
		activitePrincipaleText,
		emplIdSeed
	}
	
	public SocialInfo() {
		super(DomainClass.SocialInfo);
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		// SalaryGrid
		if(getSalaryGrid() != null) {
			json.put(JSONKey.salaryGridId.toString(), getSalaryGrid().getDescriptor().getID());
		}
		
		// fullTimeWeeklyHours
		json.put(JSONKey.fullTimeWeeklyHours.toString(), getFullTimeWeeklyHours());
		
		// nightWorkBegin
		if(this.getNightWorkBegin() != null) {
			json.put(JSONKey.nightWorkBegin.toString(), AEDateUtil.formatTimeToSystem(getNightWorkBegin()));
		}
		
		// nightWorkEnd
		if(this.getNightWorkEnd() != null) {
			json.put(JSONKey.nightWorkEnd.toString(), AEDateUtil.formatTimeToSystem(getNightWorkEnd()));
		}
		
		// startDate
		if(this.getStartDate() != null) {
			json.put(JSONKey.startDate.toString(), AEDateUtil.formatToSystem(this.getStartDate()));
		}
		
		// startDate
		if(this.getScheduleStartDate() != null) {
			json.put(JSONKey.scheduleStartDate.toString(), AEDateUtil.formatToSystem(this.getScheduleStartDate()));
		}
		
		// logoUrl
		if(!AEStringUtil.isEmpty(getLogoUrl())) {
			json.put(JSONKey.logoUrl.toString(), getLogoUrl());
		}
		
		// generateContract
		json.put(JSONKey.generateContract.toString(), isGenerateContract());
		
		// societe
		if(!AEStringUtil.isEmpty(getSociete())) {
			json.put(JSONKey.societe.toString(), getSociete());
		}
		
		// soceteTerm
		if(!AEStringUtil.isEmpty(getSocieteTerm())) {
			json.put(JSONKey.societeTerm.toString(), getSocieteTerm());
		}
		
		// password
		if(!AEStringUtil.isEmpty(getPassword())) {
			json.put(JSONKey.password.toString(), getPassword());
		}
		
		// templatesSetId
		if(SocialTemplatesSet.isDefined(getTemplatesSet())) {
			json.put(JSONKey.templatesSetId.toString(), getTemplatesSet().getId());
		}
		
		// urssafNumber
		if(!AEStringUtil.isEmpty(getUrssafNumber())) {
			json.put(JSONKey.urssafNumber.toString(), getUrssafNumber());
		}
		
		// urssafPretext
		if(!AEStringUtil.isEmpty(getUrssafPretext())) {
			json.put(JSONKey.urssafPretext.toString(), getUrssafPretext());
		}
		
		// activitePrincipale
		if(!AEStringUtil.isEmpty(getActivitePrincipale())) {
			json.put(JSONKey.activitePrincipale.toString(), getActivitePrincipale());
		}
		
		json.put(JSONKey.cctv.toString(), isCctv());
		
		// urssafText
		if(!AEStringUtil.isEmpty(getUrssafText())) {
			json.put(JSONKey.urssafText.toString(), getUrssafText());
		}
		
		// activitePrincipaleText
		if(!AEStringUtil.isEmpty(getActivitePrincipaleText())) {
			json.put(JSONKey.activitePrincipaleText.toString(), getActivitePrincipaleText());
		}
		
		// emplIdSeed
		if(getEmplIdSeed() != null) {
			json.put(JSONKey.emplIdSeed.name(), getEmplIdSeed());
		}
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// SalaryGrid
		if(jsonObject.has(JSONKey.salaryGridId.toString())) {
			try {
				setSalaryGrid(SalaryGrid.lazyDescriptor(jsonObject.getLong(JSONKey.salaryGridId.toString())));
			} catch (Exception e) {
				// nothing to do
			}
		}
		
		// nightWorkBegin
		if(jsonObject.has(JSONKey.nightWorkBegin.toString())) {
			String hhmm = jsonObject.optString(JSONKey.nightWorkBegin.toString());
			if(!AEStringUtil.isEmpty(hhmm)) {
				setNightWorkBegin(AEDateUtil.parseTimeOnly(hhmm));
			}
		}
		
		// nightWorkEnd
		if(jsonObject.has(JSONKey.nightWorkEnd.toString())) {
			String hhmm = jsonObject.optString(JSONKey.nightWorkEnd.toString());
			if(!AEStringUtil.isEmpty(hhmm)) {
				setNightWorkEnd(AEDateUtil.parseTimeOnly(hhmm));
			}
		}
		
		// startDate
		if(jsonObject.has(JSONKey.startDate.toString())) {
			String date = jsonObject.optString(JSONKey.startDate.toString());
			if(!AEStringUtil.isEmpty(date)) {
				setStartDate(AEDateUtil.parseDateStrict(date));
			}
		}
		
		// scheduleStartDate
		if(jsonObject.has(JSONKey.scheduleStartDate.toString())) {
			String date = jsonObject.optString(JSONKey.scheduleStartDate.toString());
			if(!AEStringUtil.isEmpty(date)) {
				setScheduleStartDate(AEDateUtil.parseDateStrict(date));
			}
		}
		
		// logoUrl
		if(jsonObject.has(JSONKey.logoUrl.toString())) {
			String logoUrl = jsonObject.optString(JSONKey.logoUrl.toString());
			if(!AEStringUtil.isEmpty(logoUrl)) {
				setLogoUrl(logoUrl);
			}
		}
		
		// generateContract
		setGenerateContract(jsonObject.optBoolean(JSONKey.generateContract.toString()));
		
		// societe
		if(jsonObject.has(JSONKey.societe.toString())) {
			String societe = jsonObject.optString(JSONKey.societe.toString());
			if(!AEStringUtil.isEmpty(societe)) {
				setSociete(societe);
			}
		}
		
		// societeTerm
		if(jsonObject.has(JSONKey.societeTerm.toString())) {
			String societeTerm = jsonObject.optString(JSONKey.societeTerm.toString());
			if(!AEStringUtil.isEmpty(societeTerm)) {
				setSocieteTerm(societeTerm);
			}
		}
		
		// password
		if(jsonObject.has(JSONKey.password.toString())) {
			String password = jsonObject.optString(JSONKey.password.toString());
			if(!AEStringUtil.isEmpty(password)) {
				setPassword(password);
			}
		}
		
		// templatesSetId
		if(jsonObject.has(JSONKey.templatesSetId.toString())) {
			long templatesSetId = jsonObject.optLong(JSONKey.templatesSetId.toString());
			SocialTemplatesSet tSet = SocialTemplatesSet.valueOf((int) templatesSetId);
			if(SocialTemplatesSet.isDefined(tSet)) {
				setTemplatesSet(tSet);
			}
		}
		
		// urssafNumber
		if(jsonObject.has(JSONKey.urssafNumber.toString())) {
			String urssafNumber = jsonObject.optString(JSONKey.urssafNumber.toString());
			if(!AEStringUtil.isEmpty(urssafNumber)) {
				setUrssafNumber(urssafNumber);
			}
		}
		
		// urssafPretext
		if(jsonObject.has(JSONKey.urssafPretext.toString())) {
			String urssafPretext = jsonObject.optString(JSONKey.urssafPretext.toString());
			if(!AEStringUtil.isEmpty(urssafPretext)) {
				setUrssafPretext(urssafPretext);
			}
		}
		
		// activitePrincipale
		if(jsonObject.has(JSONKey.activitePrincipale.toString())) {
			String activitePrincipale = jsonObject.optString(JSONKey.activitePrincipale.toString());
			if(!AEStringUtil.isEmpty(activitePrincipale)) {
				setActivitePrincipale(activitePrincipale);
			}
		}
		
		// cctv
		setCctv(jsonObject.optBoolean(JSONKey.cctv.toString()));
		
		// urssafText,
		if(jsonObject.has(JSONKey.urssafText.toString())) {
			setUrssafText(jsonObject.optString(JSONKey.urssafText.toString()));
		}
		
		// activitePrincipaleText
		if(jsonObject.has(JSONKey.activitePrincipaleText.toString())) {
			setActivitePrincipaleText(jsonObject.optString(JSONKey.activitePrincipaleText.toString()));
		}
		
		// emplIdSeed
		if(jsonObject.has(JSONKey.emplIdSeed.name())) {
			long l = jsonObject.optLong(JSONKey.emplIdSeed.name());
			if(l > 0) {
				setEmplIdSeed(l);
			}
		}
	}

	public AEDescriptive getSalaryGrid() {
		return salaryGrid;
	}

	public void setSalaryGrid(AEDescriptive salaryGrid) {
		this.salaryGrid = salaryGrid;
	}

	public double getFullTimeWeeklyHours() {
		return fullTimeWeeklyHours;
	}

	public void setFullTimeWeeklyHours(double fullTimeWeeklyHours) {
		this.fullTimeWeeklyHours = fullTimeWeeklyHours;
	}

	public Date getNightWorkBegin() {
		return nightWorkBegin;
	}

	public void setNightWorkBegin(Date nightWorkBegin) {
		this.nightWorkBegin = nightWorkBegin;
	}

	public Date getNightWorkEnd() {
		return nightWorkEnd;
	}

	public void setNightWorkEnd(Date nightWorkEnd) {
		this.nightWorkEnd = nightWorkEnd;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getScheduleStartDate() {
		return scheduleStartDate;
	}

	public void setScheduleStartDate(Date scheduleStartDate) {
		this.scheduleStartDate = scheduleStartDate;
	}

	public String getLogoUrl() {
		return logoUrl;
	}

	public void setLogoUrl(String logoUrl) {
		this.logoUrl = logoUrl;
	}

	public boolean isGenerateContract() {
		return generateContract;
	}

	public void setGenerateContract(boolean generateContract) {
		this.generateContract = generateContract;
	}

	public String getSociete() {
		return societe;
	}

	public void setSociete(String societe) {
		this.societe = societe;
	}

	public String getSocieteTerm() {
		return societeTerm;
	}

	public void setSocieteTerm(String societeTerm) {
		this.societeTerm = societeTerm;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public SocialTemplatesSet getTemplatesSet() {
		return templatesSet;
	}

	public void setTemplatesSet(SocialTemplatesSet templatesSet) {
		this.templatesSet = templatesSet;
	}

	public String getUrssafNumber() {
		return urssafNumber;
	}

	public void setUrssafNumber(String urssafNumber) {
		this.urssafNumber = urssafNumber;
	}

	public String getUrssafPretext() {
		return urssafPretext;
	}

	public void setUrssafPretext(String urssafPretext) {
		this.urssafPretext = urssafPretext;
	}

	public String getActivitePrincipale() {
		return activitePrincipale;
	}

	public void setActivitePrincipale(String activitePrincipale) {
		this.activitePrincipale = activitePrincipale;
	}

	public boolean isCctv() {
		return cctv;
	}

	public void setCctv(boolean cctv) {
		this.cctv = cctv;
	}

	public String getUrssafText() {
		return urssafText;
	}

	public void setUrssafText(String urssafText) {
		this.urssafText = urssafText;
	}

	public String getActivitePrincipaleText() {
		return activitePrincipaleText;
	}

	public void setActivitePrincipaleText(String activitePrincipaleText) {
		this.activitePrincipaleText = activitePrincipaleText;
	}

	public Long getEmplIdSeed() {
		return emplIdSeed;
	}

	public void setEmplIdSeed(Long emplIdSeed) {
		this.emplIdSeed = emplIdSeed;
	}
}
