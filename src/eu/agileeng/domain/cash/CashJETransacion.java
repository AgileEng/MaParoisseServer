package eu.agileeng.domain.cash;

import java.util.ArrayList;
import java.util.List;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.util.json.JSONSerializable;

@SuppressWarnings("serial")
public class CashJETransacion extends AEDomainObject implements JSONSerializable {

	private AEDescriptor toCashJournalEntry;
	
	private List<CashJETProp> propsList = new ArrayList<CashJETProp>();
	
	private Double amount;
	
	public CashJETransacion() {
		super(DomainClass.CashJETransaction);
	}

	public AEDescriptor getToCashJournalEntry() {
		return toCashJournalEntry;
	}

	public void setToCashJournalEntry(AEDescriptor toCashJournalEntry) {
		this.toCashJournalEntry = toCashJournalEntry;
	}

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	@Override
	public JSONObject toJSONObject() throws JSONException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		// TODO Auto-generated method stub
		
	}

	public static CashJETransacion create(JSONObject jsonTransaction, List<CashJETProp> propsListDef) throws AEException, JSONException {
		CashJETransacion tran = new CashJETransacion();
		tran.setID(jsonTransaction.optLong("id"));
		if(jsonTransaction.has("_value")) {
			tran.setAmount(new Double(jsonTransaction.optDouble("_value")));
		}
		// properties array
		for (CashJETProp propDef : propsListDef) {
			CashJETProp prop = CashJETProp.create(propDef, jsonTransaction.optString(Long.toString(propDef.getID())));
			tran.addCJETProperty(prop);
		}
		return tran;
	}
	
	public boolean addCJETProperty(CashJETProp prop) {
		return this.propsList.add(prop);
	}

	public List<CashJETProp> getPropsList() {
		return propsList;
	}

	public void setPropsList(List<CashJETProp> propsList) {
		this.propsList = propsList;
	}
}
