/**
 * 
 */
package eu.agileeng.domain.acc;

import java.util.HashMap;
import java.util.Map;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.imp.AEDescriptorImp;

/**
 * @author vvatov
 *
 */
public class AccAccount extends AEDomainObject {
	
	public static final String codeAccountRegEx = "[1-9]\\d{2}\\d+";
	
	public static final String codePatternRegEx = "[1-9]\\d{2}([\\dX])+";
	
	public static final int accLenght = 4;
	
	static public enum JSONKey {
		chartOfAccounts,
		accounts,
		patterns,
		accType,
		modelId,
		parent,
		dontSeparateByAccType,
		coaModels,
		parentId,
		coaId,
		modifiable;
	}
	
	private String auxiliare;
	
	/**
	 * Defines enumeration of Account Types
	 * 
	 * @author vvatov
	 */
	static public enum AccountType {
		NA(0L),
		CLASS(10L),
		GROUP(20L),
		MAIN_ACCOUNT(30L),
		PATTERN(40L),
		ACCOUNT(50L);
				
		private long typeId;
		
		private AccountType(long typeId) {
			this.typeId = typeId;
		}
		
		public final long getTypeId() {
			return this.typeId;
		}
		
		public static AccountType valueOf(long typeId) {
			AccountType ret = null;
			for (AccountType inst : AccountType.values()) {
				if(inst.getTypeId() == typeId) {
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
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5691984248097363779L;

	/**
	 * @param clazz
	 */
	protected AccAccount(DomainClass clazz) {
		super(clazz);
	}
	
	public AccAccount() {
		this(DomainClass.AccAccount);
	}

	public String getAuxiliare() {
		return auxiliare;
	}

	public void setAuxiliare(String auxiliare) {
		this.auxiliare = auxiliare;
	}
	
	public AEDescriptor getAuxiliareDescr() {
		AEDescriptor auxDescr = new AEDescriptorImp();
		auxDescr.setCode(getAuxiliare());
		return auxDescr;
	}
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.AccAccount);
	}
	
	public static Map<String, JSONObject> indexByCode(JSONArray accounts) {
		Map<String, JSONObject> map = new HashMap<String, JSONObject>();
		if(accounts != null) {
			for (int i = 0; i < accounts.length(); i++) {
				try {
					JSONObject acc = (JSONObject) accounts.get(i);
					map.put(acc.getString("code").trim(), acc);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		return map;
	}
	
	public static Map<Long, JSONObject> indexById(JSONArray accounts) {
		Map<Long, JSONObject> map = new HashMap<Long, JSONObject>();
		if(accounts != null) {
			for (int i = 0; i < accounts.length(); i++) {
				try {
					JSONObject acc = (JSONObject) accounts.get(i);
					map.put(acc.getLong("id"), acc);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		return map;
	}
}
