package eu.agileeng.domain.cash;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.util.AECollectionUtil;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.json.JSONSerializable;

@SuppressWarnings("serial")
public class CashJournalEntry extends AEDomainObject implements JSONSerializable {

	private long goaId;
	
	private long accId;
	
	private String accCode;
	
	private long journalId;
	
	private Date entryDate;
	
	private Double amount;
	
	private int entryType;
	
	private Double dtAmount;
	
	private Double ctAmount;
	
	private long ownerId;
	
	private boolean accounted;
	
	private Double accountedAmount; 
	
	private List<CashJETransacion> jeTransactions = new ArrayList<CashJETransacion>();
	
	public CashJournalEntry() {
		super(DomainClass.CashJournalEntry);
	}
	
	public CashJournalEntry(long goaId, Date date, Double amount) {
		this();
		this.goaId = goaId;
		this.entryDate = date;
		this.amount = amount;
	}

	public Date getDate() {
		return entryDate;
	}

	public void setDate(Date date) {
		this.entryDate = date;
	}

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public long getGoaId() {
		return goaId;
	}

	public void setGoaId(long goaId) {
		this.goaId = goaId;
	}

	public long getJournalId() {
		return journalId;
	}

	public void setJournalId(long journalId) {
		this.journalId = journalId;
	}

	public Date getEntryDate() {
		return entryDate;
	}

	public void setEntryDate(Date entryDate) {
		this.entryDate = entryDate;
	}

	public int getEntryType() {
		return entryType;
	}

	public void setEntryType(int entryType) {
		this.entryType = entryType;
	}

	public Double getDtAmount() {
		return dtAmount;
	}

	public void setDtAmount(Double dtAmount) {
		this.dtAmount = dtAmount;
	}

	public Double getCtAmount() {
		return ctAmount;
	}

	public void setCtAmount(Double ctAmount) {
		this.ctAmount = ctAmount;
	}

	public long getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(long ownerId) {
		this.ownerId = ownerId;
	}

	public long getAccId() {
		return accId;
	}

	public void setAccId(long accId) {
		this.accId = accId;
	}

	public List<CashJETransacion> getJeTransactions() {
		return jeTransactions;
	}

	public void setJeTransactions(List<CashJETransacion> jeTransactions) {
		this.jeTransactions = jeTransactions;
	}
	
	public static final AEDescriptor createAEDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.CashJournalEntry);
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		String dateChanged = jsonObject.optString("dateChanged");

		this.entryDate = AEDateUtil.parseDateStrict(dateChanged);
		this.amount = jsonObject.optDouble(dateChanged);
		setGoaId(jsonObject.optLong("id"));
		setAccId(jsonObject.optLong("accId"));
		setOwnerId(jsonObject.optLong("ownerId"));
		setEntryType(jsonObject.optInt("sysId"));

		// transactions
		JSONObject jsonAttributesWrapper = jsonObject.optJSONObject(dateChanged + "_transactions");
		if(jsonAttributesWrapper != null) {
			JSONArray jsonTransArray = jsonAttributesWrapper.optJSONArray("transactions");
			JSONArray jsonAttrArray = jsonObject.optJSONArray("attributes");
			if(jsonTransArray!= null && jsonAttrArray != null) {
				List<CashJETProp> propDefsList = CashJETProp.create(jsonAttrArray);
				for(int i = 0; i < jsonTransArray.length(); i++) {
					JSONObject jsonTransaction = jsonTransArray.getJSONObject(i);
					try {
						CashJETransacion tran = CashJETransacion.create(jsonTransaction, propDefsList);
						jeTransactions.add(tran);
					} catch (AEException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	/**
	 * 
	 * @param entriesList
	 * @param accId
	 * @param date
	 * @return found entry or <code>null</code> of an entry does'n exist
	 */
	public static CashJournalEntry findByAccAndDate(List<CashJournalEntry> entriesList, long accId, Date date) {
		CashJournalEntry foundEntry = null;
		if(!AECollectionUtil.isEmpty(entriesList)) {
			for (CashJournalEntry cashJournalEntry : entriesList) {
				if(cashJournalEntry.getGoaId() == accId 
						&& AEDateUtil.areDatesEqual(cashJournalEntry.getEntryDate(), date)) {
					
					foundEntry = cashJournalEntry;
					break;
				}
			}
		}
		return foundEntry;
	}

	public String getAccCode() {
		return accCode;
	}

	public void setAccCode(String accCode) {
		this.accCode = accCode;
	}

	public boolean isAccounted() {
		return accounted;
	}

	public void setAccounted(boolean accounted) {
		this.accounted = accounted;
	}

	public Double getAccountedAmount() {
		return accountedAmount;
	}

	public void setAccountedAmount(Double accountedAmount) {
		this.accountedAmount = accountedAmount;
	}
	
	public void updateToAccounted() {
		setAccountedAmount(getAmount());
		setAccounted(true);
		setUpdated();
	}
	
	public double getAmountAccounting() {
		return AEMath.doubleValue(getAmount()) - AEMath.doubleValue(getAccountedAmount());
	}
}
