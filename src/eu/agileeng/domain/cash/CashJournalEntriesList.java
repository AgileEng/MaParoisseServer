/**
 * 
 */
package eu.agileeng.domain.cash;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.AEList;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;

/**
 * @author vvatov
 *
 */
public class CashJournalEntriesList extends ArrayList<CashJournalEntry> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6837065980188741811L;

	/**
	 * 
	 */
	public CashJournalEntriesList() {
	}

	/**
	 * @param arg0
	 */
	public CashJournalEntriesList(int arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public CashJournalEntriesList(Collection<? extends CashJournalEntry> arg0) {
		super(arg0);
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEList#toJSONArray()
	 */
	@Override
	public JSONArray toJSONArray() throws JSONException {
		return null;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.AEList#create(org.apache.tomcat.util.json.JSONArray)
	 */
	@Override
	public void create(JSONArray jsonArray) throws JSONException {
	}
	
	public CashJournalEntry get(Date entryDate, String accCode) {
		CashJournalEntry entry = null;
		for (CashJournalEntry cjEntry : this) {
			if(cjEntry.getAccCode() != null && cjEntry.getAccCode().equalsIgnoreCase(accCode)
					&& AEDateUtil.areDatesEqual(cjEntry.getEntryDate(), entryDate)) {
				entry = cjEntry;
				break;
			}
		}
		return entry;
	}
	
	public CashJournalEntry get(Date entryDate, long goaId) {
		CashJournalEntry entry = null;
		for (CashJournalEntry cjEntry : this) {
			if(cjEntry.getGoaId() == goaId && AEDateUtil.areDatesEqual(cjEntry.getEntryDate(), entryDate)) {
				entry = cjEntry;
				break;
			}
		}
		return entry;
	}
	
	public Double getAttributeSum(Date entryDate, long attrId) throws AEException {
		Double sum = null;
		for (CashJournalEntry cjEntry : this) {
			if(cjEntry.getJeTransactions() != null
					&& !cjEntry.getJeTransactions().isEmpty()
					&& AEDateUtil.areDatesEqual(cjEntry.getEntryDate(), entryDate)) {
				
				List<CashJETransacion> jeTransactions = cjEntry.getJeTransactions();
				for (CashJETransacion cashJETransacion : jeTransactions) {
					List<CashJETProp> propsList = cashJETransacion.getPropsList();
					if(propsList != null && !propsList.isEmpty()) {
						for (CashJETProp cashJETProp : propsList) {
							if(cashJETProp.getPropDefId() == attrId) {
								String val = cashJETProp.getValue();
								try {
									double dVal = AEMath.parseDouble(val, true);
									if(sum == null) {
										sum = new Double(dVal);
									} else {
										sum += dVal;
									}
								} catch (Exception e) {
									throw new AEException("Caisse, " + cjEntry.getDate() + ": " + e.getMessage());
								}
							}
						}
					}
				}
			}
		}
		return sum;
	}
	
	public Double getAttributeSum(long attrId) throws AEException{
		Double sum = null;
		for (CashJournalEntry cjEntry : this) {
			if(cjEntry.getJeTransactions() != null && !cjEntry.getJeTransactions().isEmpty()) {
				List<CashJETransacion> jeTransactions = cjEntry.getJeTransactions();
				for (CashJETransacion cashJETransacion : jeTransactions) {
					List<CashJETProp> propsList = cashJETransacion.getPropsList();
					if(propsList != null && !propsList.isEmpty()) {
						for (CashJETProp cashJETProp : propsList) {
							if(cashJETProp.getPropDefId() == attrId) {
								try {
									String val = cashJETProp.getValue();
									double dVal = AEMath.parseDouble(val, true);
									if(sum == null) {
										sum = new Double(dVal);
									} else {
										sum += dVal;
									}
								} catch (Exception e) {
									throw new AEException("Caisse, " + cjEntry.getDate() + ": " + e.getMessage());
								}
							}
						}
					}
				}
			}
		}
		return sum;
	}
	
	public final Double getGOASum(long goaId) {
		Double sum = null;
		for (CashJournalEntry cjEntry : this) {
			if(cjEntry.getGoaId() == goaId) {
				if(sum == null) {
					sum = new Double(AEMath.doubleValue(cjEntry.getAmount()));
				} else {
					sum += (cjEntry.getAmount() != null ? cjEntry.getAmount().doubleValue() : 0.0) ;
				}
				break;
			}
		}
		return sum;
	}
	
	public static Map<Date, CashJournalEntriesList> groupByDate(CashJournalEntriesList l) {
		Map<Date, CashJournalEntriesList> m = new HashMap<Date, CashJournalEntriesList>();
		if(l != null) {
			for (CashJournalEntry cashJournalEntry : l) {
				Date d = cashJournalEntry.getEntryDate();
				CashJournalEntriesList entries = m.get(d);
				if(entries == null) {
					entries = new CashJournalEntriesList();
					m.put(d, entries);
				}
				entries.add(cashJournalEntry);
			}
		}
		return m;
	}
}
