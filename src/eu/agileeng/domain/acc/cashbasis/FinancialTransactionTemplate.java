package eu.agileeng.domain.acc.cashbasis;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.acc.cashbasis.AccJournalEntryTemplate.JournalIdentificationRule;
import eu.agileeng.domain.imp.AEDescriptorImp;

public class FinancialTransactionTemplate extends AEDomainObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = -514113957613995030L;

	static public enum JSONKey {
		financialTransactionTemplates,
		financialTransactionTemplate,
		paymentMethodId;
	}
	
	/**
	 * The system internally uses this class as AccJournals repository
	 * 
	 * @author vvatov
	 */
	static public enum PaymentMethod {
		NA(0L, "NA", "NA"),
		CASH(10L, "CA", "CAISSE"),
		BANK(20L, "BQ", "BANQUE"),
		VARIOUS(30l, "OD", "DIVERS"),
		NOUVEAU(40l, "AA", "A NOUVEAU"); // a journal for balances

		private long id;
		
		private String code;
		
		private String name;

		private PaymentMethod(long id, String code, String name) {
			this.id = id;
			this.code = code;
			this.name = name;
		}

		public final long getId() {
			return this.id;
		}

		public static PaymentMethod valueOf(long id) {
			PaymentMethod ret = null;
			for (PaymentMethod inst : PaymentMethod.values()) {
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

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @param name the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * @return the code
		 */
		public String getCode() {
			return code;
		}
		
		public JournalIdentificationRule getJournalIdentificationRule() {
			JournalIdentificationRule ret = JournalIdentificationRule.NA;
			switch(this) {
			case CASH:
				ret = JournalIdentificationRule.CA;
				break;
			case BANK:
				ret = JournalIdentificationRule.BQ;
				break;
			case VARIOUS:
				ret = JournalIdentificationRule.OD;
				break;
			case NOUVEAU:
				ret = JournalIdentificationRule.AA;
				break;
			default:
				ret = JournalIdentificationRule.NA;
				break;
			}
			return ret;
		}
	}
	
	private FinancialTransactionTemplate.PaymentMethod paymentMethod = FinancialTransactionTemplate.PaymentMethod.NA;
	
	private AccJournalEntryTeplatesList accJournalEntryTemplates; 
	
	private AEDescriptive appModuleTemplate;
	
	/**
	 * @param clazz
	 */
	public FinancialTransactionTemplate() {
		super(DomainClass.FinancialTransactionTemplate);
	}
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.FinancialTransactionTemplate);
	}

	/**
	 * @return the paymentMethod
	 */
	public FinancialTransactionTemplate.PaymentMethod getPaymentMethod() {
		return paymentMethod;
	}

	/**
	 * @param paymentMethod the paymentMethod to set
	 */
	public void setPaymentMethod(FinancialTransactionTemplate.PaymentMethod paymentMethod) {
		this.paymentMethod = paymentMethod;
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject jsonObject = super.toJSONObject();

		if(this.paymentMethod != null) {
			jsonObject.put(JSONKey.paymentMethodId.name(), this.paymentMethod.getId());
		}
		
		if(this.accJournalEntryTemplates != null) {
			jsonObject.put(AccJournalEntryTemplate.JSONKey.accJournalEntryTemplates.name(), this.accJournalEntryTemplates.toJSONArray());
		}
		
		return jsonObject;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// payment method (required)
		setPaymentMethod(PaymentMethod.valueOf(jsonObject.getLong(JSONKey.paymentMethodId.name())));
		
		// 
		AccJournalEntryTeplatesList accJournalEntryTeplatesList = new AccJournalEntryTeplatesList();
		JSONArray accTransactionsJson = jsonObject.optJSONArray(AccJournalEntryTemplate.JSONKey.accJournalEntryTemplates.name());
		if(accTransactionsJson != null) {
			accJournalEntryTeplatesList.create(accTransactionsJson);
			accJournalEntryTeplatesList.assignTo(this);
		}
		setAccJournalEntryTemplates(accJournalEntryTeplatesList);
	}

	/**
	 * @return the appModuleTemplate
	 */
	public AEDescriptive getAppModuleTemplate() {
		return appModuleTemplate;
	}

	/**
	 * @param appModuleTemplate the appModuleTemplate to set
	 */
	public void setAppModuleTemplate(AEDescriptive appModuleTemplate) {
		this.appModuleTemplate = appModuleTemplate;
	}

	/**
	 * @return the accJournalEntryTemplates
	 */
	public AccJournalEntryTeplatesList getAccJournalEntryTemplates() {
		return accJournalEntryTemplates;
	}

	/**
	 * @param accJournalEntryTemplates the accJournalEntryTemplates to set
	 */
	public void setAccJournalEntryTemplates(AccJournalEntryTeplatesList accJournalEntryTemplates) {
		this.accJournalEntryTemplates = accJournalEntryTemplates;
	}
}
