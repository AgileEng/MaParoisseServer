/**
 * 
 */
package eu.agileeng.domain.acc;

import java.util.Date;
import java.util.Iterator;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.util.AECollectionUtil;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;

/**
 * @author vvatov
 * 
 * A journal entry is a logging of a financial transaction into accounting journal items. 
 * The journal entry can consist of several items, each of which is either a debit or a credit. 
 * The total of the debits must equal the total of the credits or the journal entry is said to be "unbalanced". 
 * As a result, journal entries directly change the account balances on the general ledger.
 * 
 */
public class AccJournalEntry extends AEDomainObject {

	private static final long serialVersionUID = 6224296685507239228L;

	private AccJournalItemsList accJournalItems = new AccJournalItemsList();

	private AEDescriptor accJournal;
	
	private long idGui;

	static public enum JSONKey {
		accJournals,
		accBlackList,
		accJournalEntry,
		accJournalItem,
		accJournalItems,
		accJournalItemsFilter,
		accJournalCode;
	}

	/**
	 * @param clazz
	 */
	private AccJournalEntry(DomainClass clazz) {
		super(clazz);
	}

	public AccJournalEntry() {
		this(DomainClass.AccJournalEntry);
	}

	public AccJournalItemsList getAccJournalItems() {
		return accJournalItems;
	}

	public void setAccJournalItems(AccJournalItemsList accJournalItems) {
		this.accJournalItems = accJournalItems;
	}

	public boolean addItem(AccJournalItem item) {
		return this.accJournalItems.add(item);
	}

	public boolean addItems(AccJournalItemsList items) {
		return this.accJournalItems.addAll(items);
	}

	public Double calcDtAmount() {
		Double res = null;
		for (AccJournalItem accJournalItem : this.accJournalItems) {
			if(accJournalItem.getDtAmount() != null) {
				if(res == null) {
					res = new Double(0.0);
				}
				res += accJournalItem.getDtAmount();
			}
		}
		return res;
	}

	public Double calcCtAmount() {
		Double res = null;
		for (AccJournalItem accJournalItem : this.accJournalItems) {
			if(accJournalItem.getCtAmount() != null) {
				if(res == null) {
					res = new Double(0.0);
				}
				res += accJournalItem.getCtAmount();
			}
		}
		return res;
	}

	public void propEntryId() {
		for (AccJournalItem accJournalItem : this.accJournalItems) {
			accJournalItem.setEntryId(getID());
		}
	}

	public void propCompany() {
		for (AccJournalItem accJournalItem : this.accJournalItems) {
			accJournalItem.setCompany(getCompany());
		}
	}
	
	public void propBatchId(long batchId) {
		for (AccJournalItem accJournalItem : this.accJournalItems) {
			accJournalItem.setBatchId(batchId);
		}
	}

	public boolean isEmpty() {
		return AECollectionUtil.isEmpty(this.accJournalItems);
	}

	/**
	 * @return the accJournal
	 */
	public AEDescriptor getAccJournal() {
		return accJournal;
	}

	/**
	 * @param accJournal the accJournal to set
	 */
	public void setAccJournal(AEDescriptor accJournal) {
		this.accJournal = accJournal;
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();

		// AccJournalItemsList
		if(getAccJournalItems() != null) {
			json.put(JSONKey.accJournalItems.name(), getAccJournalItems().toJSONArray());
		}

		return json;
	}

	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);

		// accJournalItems
		if(jsonObject.has(JSONKey.accJournalItems.name())) {
			JSONArray accJournalItemsJson = jsonObject.getJSONArray(JSONKey.accJournalItems.name());
			accJournalItems.create(accJournalItemsJson);
		}
	}

	public boolean isBalanced() {
		boolean ret = false;
		Double dtAmount = calcDtAmount();
		Double ctAmount = calcCtAmount();
		ret = AEMath.isZeroAmount(AEMath.doubleValue(dtAmount) - AEMath.doubleValue(ctAmount));
		return ret;
	}
	
	public boolean isItemsTheSameDate() {
		boolean res = true;
		Date date = null;
		for (AccJournalItem accJournalItem : this.accJournalItems) {
			Date _date = AEDateUtil.getClearDateTime(accJournalItem.getDate());
			if(date == null) {
				if(_date == null) {
					res = false;
					break;
				}
				date = _date;
			} else {
				if(!date.equals(_date)) {
					res = false;
					break;
				}
			}
		}
		return res;
	}

	public boolean reBalance() {
		boolean balanced = isBalanced();
		
		if(!balanced) {
			
			// auto detect balancing item
			AccJournalItem balancingItem = getBalancingItem();

			// try to balance using balancingItem
			if(balancingItem != null) {
				double dtAmount = calcDtAmount();
				double ctAmount = calcCtAmount();
				double diffAmount = dtAmount - ctAmount;
				if(!AEMath.isZeroAmount(diffAmount)) {
					boolean removed = removeAccJournalItemById(balancingItem.getID());
					if(removed) {
						dtAmount = calcDtAmount();
						ctAmount = calcCtAmount();
						Double amount = AEMath.round(AEMath.doubleValue(dtAmount) - AEMath.doubleValue(ctAmount), 2);

						// dt and ct
						if(amount > 0) {
							balancingItem.setCtAmount(amount);
							balancingItem.setCredit(true);

							balancingItem.setDtAmount(null);
							balancingItem.setDebit(false);
						} else {
							balancingItem.setDtAmount(amount * (-1.0));
							balancingItem.setDebit(true);

							balancingItem.setCtAmount(null);
							balancingItem.setCredit(false);
						}

						// add back
						balancingItem.setUpdated();
						addItem(balancingItem);

						// success
						balanced = true;
					}
				}
			}
		}
		return balanced;
	}

	private AccJournalItem getBalancingItem() {
		AccJournalItem balancingItem = null;
		
		// count
		AccJournalItemsList dtItems = new AccJournalItemsList();
		AccJournalItemsList ctItems = new AccJournalItemsList();
		if(this.accJournalItems != null && !this.accJournalItems.isEmpty()) {
			for (AccJournalItem item : this.accJournalItems) {
				if(!AEMath.isZeroAmount(AEMath.doubleValue(item.getDtAmount()))) {
					dtItems.add(item);
				}
				if(!AEMath.isZeroAmount(AEMath.doubleValue(item.getCtAmount()))) {
					ctItems.add(item);
				}
			}
		}

		// take a decision
		if(dtItems.size() == 1 && ctItems.size() > 1) {
			balancingItem = dtItems.get(0);
		} else if(dtItems.size() > 1 && ctItems.size() == 1) {
			balancingItem = ctItems.get(0);
		} else if(dtItems.size() == 1 && ctItems.size() == 1) {
			if(dtItems.get(0).isUpdated()) {
				balancingItem = ctItems.get(0);
			} else if(ctItems.get(0).isUpdated()) {
				balancingItem = dtItems.get(0);
			}
		} 
		
		return balancingItem;
	}
	
	public void propContributor(AEDescriptor contributor) {
		for (AccJournalItem accJournalItem : this.accJournalItems) {
			accJournalItem.setContributor(contributor);
		}
	}

	public void propSupplier(AEDescriptor supplier) {
		for (AccJournalItem accJournalItem : this.accJournalItems) {
			accJournalItem.setSupplier(supplier);
		}
	}

	public Date getMaxDate() {
		Date date = null;
		for (AccJournalItem accJournalItem : this.accJournalItems) {
			if(date == null) {
				date = accJournalItem.getDate();
			} else if(accJournalItem.getDate() != null && date.before(accJournalItem.getDate())) {
				date = accJournalItem.getDate();
			}
		}
		return date;
	}

	public boolean hasAccJournalItems() {
		boolean bRes = this.accJournalItems != null && !this.accJournalItems.isEmpty();
		return bRes;
	}

	public AccJournalItem getAccJournalItemById(long accJournalItemId) {
		AccJournalItem accJournalItem = null;
		if(accJournalItems != null) {
			for (AccJournalItem item : accJournalItems) {
				if(item.getID() == accJournalItemId) {
					accJournalItem = item;
					break;
				}
			}
		}
		return accJournalItem;
	}

	public AccJournalItem getAccJournalItemByAccId(long accId) {
		AccJournalItem accJournalItem = null;
		if(accJournalItems != null) {
			for (AccJournalItem item : accJournalItems) {
				if(item.getAccount() != null && item.getAccount().getID() == accId) {
					accJournalItem = item;
					break;
				}
			}
		}
		return accJournalItem;
	}

	public boolean removeAccJournalItemById(long accJournalItemId) {
		boolean bRes = false;
		if(accJournalItems != null) {
			for (Iterator<AccJournalItem> iterator = accJournalItems.iterator(); iterator.hasNext();) {
				AccJournalItem accJournalItem = (AccJournalItem) iterator.next();
				if(accJournalItem.getID() == accJournalItemId) {
					iterator.remove();
					bRes = true;
					break;
				}
			}
		}
		return bRes;
	}
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.AccJournalEntry);
	}

	/**
	 * @return the idGui
	 */
	public long getIdGui() {
		return idGui;
	}

	/**
	 * @param idGui the idGui to set
	 */
	public void setIdGui(long idGui) {
		this.idGui = idGui;
	}
}
