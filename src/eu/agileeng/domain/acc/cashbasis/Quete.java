package eu.agileeng.domain.acc.cashbasis;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.json.JSONUtil;

public class Quete extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static public enum Type {
		NA(0L),
		DF(10L),
		CELEBRANT(20L),
		QUETE(30l);

		private long id;

		private Type(long id) {
			this.id = id;
		}

		public final long getId() {
			return this.id;
		}

		public static Type valueOf(long id) {
			Type ret = null;
			for (Type inst : Type.values()) {
				if(inst.getId() == id) {
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
	
	static public enum JSONKey {
		quete,
		quetes,
		type,
		amount;
	}
	
	private Quete.Type type = Quete.Type.NA;
	
	/**
	 * The amount of this Quete
	 */
	private double amount;
	
	private AEDescriptor financialTransaction;
	
	/**
	 * Construcor
	 */
	public Quete() {
		super(DomainClass.Quete);
	}
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.Quete);
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject jsonObject = super.toJSONObject();
		
		// type
		if(getType() != null) {
			jsonObject.put(JSONKey.type.name(), getType().getId());
		}
		
		// amount
		jsonObject.put(JSONKey.amount.name(), getAmount());
		
		return jsonObject;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// type
		if(jsonObject.has(JSONKey.type.name())) {
			setType(Type.valueOf(jsonObject.getLong(JSONKey.type.name())));
		}
		
		// amount
		try {
			setAmount(AEMath.doubleValue(JSONUtil.parseDoubleStrict(jsonObject, JSONKey.amount.name())));
		} catch (AEException e) {
		}
	}

	/**
	 * @return the type
	 */
	public Quete.Type getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(Quete.Type type) {
		this.type = type;
	}

	/**
	 * @return the amount
	 */
	public double getAmount() {
		return amount;
	}

	/**
	 * @param amount the amount to set
	 */
	public void setAmount(double amount) {
		this.amount = amount;
	}

	/**
	 * @return the financialTransaction
	 */
	public AEDescriptor getFinancialTransaction() {
		return financialTransaction;
	}

	/**
	 * @param financialTransaction the financialTransaction to set
	 */
	public void setFinancialTransaction(AEDescriptor financialTransaction) {
		this.financialTransaction = financialTransaction;
	}
}
