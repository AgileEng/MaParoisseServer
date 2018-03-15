package eu.agileeng.domain.acc;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.util.AEMath;

public class AccJournalResult extends AEDomainObject {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -715909510145713369L;
	
	static public enum JSONKey {
		accJournalResult,
		accJournalResults,
		accJournalItem,
		openingBalance,
		closingBalance,
		contributor,
		supplier;
	}
	
	private AccJournalItem accJournalItem;
	
	private Double openingBalance;
	
	private Double closingBalance;
	
	private AEDescriptive contributor;
	
	private AEDescriptive supplier;
	
	private transient boolean loadLazzy;
	
	public AccJournalResult() {
		super(DomainClass.AccJournalResult);
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		// private AccJournalItem accJournalItem;
		json.put(
				JSONKey.accJournalItem.name(), 
				getAccJournalItem() != null ? getAccJournalItem().toJSONObject() : new JSONObject());
		
		// private Double openingBalance;
		json.put(JSONKey.openingBalance.name(), AEMath.doubleValue(getOpeningBalance()));
		
		// private Double closingBalance;
		json.put(JSONKey.closingBalance.name(), AEMath.doubleValue(getClosingBalance()));
		
		// private AEDescriptor contributor;
		json.put(
				JSONKey.contributor.name(), 
				getContributor() != null ? getContributor().toJSONObject() : new JSONObject());
		
		// private AEDescriptor supplier;
		json.put(
				JSONKey.supplier.name(), 
				getSupplier() != null ? getSupplier().toJSONObject() : new JSONObject());
		
		return json;
	}

	/**
	 * @return the accJournalItem
	 */
	public AccJournalItem getAccJournalItem() {
		return accJournalItem;
	}

	/**
	 * @param accJournalItem the accJournalItem to set
	 */
	public void setAccJournalItem(AccJournalItem accJournalItem) {
		this.accJournalItem = accJournalItem;
	}
	
	/**
	 * @param accJournalItem the accJournalItem to set
	 */
	public AccJournalResult withAccJournalItem(AccJournalItem accJournalItem) {
		this.accJournalItem = accJournalItem;
		return this;
	}

	/**
	 * @return the openingBalance
	 */
	public Double getOpeningBalance() {
		return openingBalance;
	}

	/**
	 * @param openingBalance the openingBalance to set
	 */
	public void setOpeningBalance(Double openingBalance) {
		this.openingBalance = openingBalance;
	}

	/**
	 * @return the closingBalance
	 */
	public Double getClosingBalance() {
		return closingBalance;
	}

	/**
	 * @param closingBalance the closingBalance to set
	 */
	public void setClosingBalance(Double closingBalance) {
		this.closingBalance = closingBalance;
	}

	/**
	 * @return the contributor
	 */
	public AEDescriptive getContributor() {
		return contributor;
	}

	/**
	 * @param contributor the contributor to set
	 */
	public void setContributor(AEDescriptive contributor) {
		this.contributor = contributor;
	}

	/**
	 * @return the supplier
	 */
	public AEDescriptive getSupplier() {
		return supplier;
	}

	/**
	 * @param supplier the supplier to set
	 */
	public void setSupplier(AEDescriptive supplier) {
		this.supplier = supplier;
	}

	/**
	 * @return the loadLazzy
	 */
	public boolean isLoadLazzy() {
		return loadLazzy;
	}

	/**
	 * @param loadLazzy the loadLazzy to set
	 */
	public void setLoadLazzy(boolean loadLazzy) {
		this.loadLazzy = loadLazzy;
	}
	
	/**
	 * Calculates closing balance from current openingBalance, debitTurnover and creditTurnover
	 */
	public void calculateClosingBalance() {
		if(this.accJournalItem != null) {
			this.closingBalance = AEMath.round(
					AEMath.doubleValue(this.openingBalance) + AEMath.doubleValue(this.accJournalItem.getDtAmount()) - AEMath.doubleValue(this.accJournalItem.getCtAmount()), 2);
		} else {
			this.closingBalance = AEMath.round(this.openingBalance, 2);
		}
	}
	
	public double getDtAmount() {
		double amount = 0.0;
		if(this.accJournalItem != null) {
			amount = AEMath.doubleValue(this.accJournalItem.getDtAmount());
		}
		return amount;
	}
	
	public double getCtAmount() {
		double amount = 0.0;
		if(this.accJournalItem != null) {
			amount = AEMath.doubleValue(this.accJournalItem.getCtAmount());
		}
		return amount;
	}
}
