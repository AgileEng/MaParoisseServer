package eu.agileeng.domain.business.bank;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;

import eu.agileeng.domain.AEList;

public class EbicsBankAccountTransactions extends ArrayList<BankTransactionsList> implements AEList {

	/**
	 * 
	 */
	private static final long serialVersionUID = 218435316225946419L;

	public EbicsBankAccountTransactions() {

	}

	public EbicsBankAccountTransactions(int initialCapacity) {
		super(initialCapacity);
	}

	public EbicsBankAccountTransactions(Collection<? extends BankTransactionsList> c) {
		super(c);
	}

	@Override
	public JSONArray toJSONArray() throws JSONException {
		return null;
	}

	@Override
	public void create(JSONArray jsonArray) throws JSONException {
	}
}
