package eu.agileeng.domain.acc.cashbasis;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEPersistent;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.util.AEStringUtil;

/**
 * Cash basis accounting is the practice of recording revenues when cash is received 
 * and recording expenses when the expense is paid.
 * 
 * @author vvatov
 */
public class AccJournalEntryTemplate extends AEDomainObject {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -27749457634474426L;

	static public enum JSONKey {
		accJournalEntryTemplates,
		accountIdentificationRuleId,
		accAccountId,
		journalIdentificationRuleId,
		journalCode,
		journalingRuleId,
		amountRuleId, 
		amountParameter,
		dt,
		ct,
		codeQuete;
	}
	
	static public enum AccountIdentificationRule {
		NA(0L),
		COMMON_OPERATION(1L), // common operation
		BALANCE_OPERATION(2L), // balance operation
		EXPENSE(6L),       // get the account from the Individual COA
		REVENUE(7L),       // get the account from the Individual COA
		BANK_ACCOUNT(51L), // get the account from configured in bank account instance  
		CASH_ACCOUNT(53L), // get the account from the Individual COA
		/**
		 * Accounts 58 "internal transfers" are transit accounts used for accounting practice operations after which they are on sale. 
		 * These accounts are especially designed to enable centralization without risk of duplication:
		 *   -  transfers of funds from a cash account or bank account to another bank or credit union;
		 *   -  and, more generally, any transaction which is the subject of a record in several special journals. 
		 */
		INTERNAL_TRANSFERS(58L), // get the account from the Individual COA
		THIRD_ACCOUNT(40L),// get the account from the Individual COA
		OTHER_RECEIVABLE_PAYABLE(46L), // get the account from the Individual COA
		DF(50L),           // Droit de la Fabrique field from financial transaction
		CELEBRANT(60L),    // Célébrant field from financial transaction
		QUETE(70);         // Quete field from financial transaction

		private long id;

		private AccountIdentificationRule(long id) {
			this.id = id;
		}

		public final long getId() {
			return this.id;
		}

		public static AccountIdentificationRule valueOf(long id) {
			AccountIdentificationRule ret = null;
			for (AccountIdentificationRule inst : AccountIdentificationRule.values()) {
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
	
	static public enum JournalIdentificationRule {
		NA(0L),
		AA(10L),     	   // A NOUVEAU (Balances)
		CA(20L), 		   // CAISSE
		BQ(30L),           // BQ 1 à 9	BANQUE
		OD(40L),           // OPERATIONS DIVERSES
		SAL(50L),          // SALAIRES
		AC(60L),           // ACHATS
		PR(70L);           // PRESTATIONS FOURNIES
		
		private long id;

		private JournalIdentificationRule(long id) {
			this.id = id;
		}

		public final long getId() {
			return this.id;
		}

		public static JournalIdentificationRule valueOf(long id) {
			JournalIdentificationRule ret = null;
			for (JournalIdentificationRule inst : JournalIdentificationRule.values()) {
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
	
	static public enum JournalingRule {
		NA(0L),
		FINANCIAL_TRANSACTION(10L),  // journaling by financial transaction   
		PAYMENT_TRANSACTION(20L);    // journaling by payment transaction 

		private long id;

		private JournalingRule(long id) {
			this.id = id;
		}

		public final long getId() {
			return this.id;
		}

		public static JournalingRule valueOf(long id) {
			JournalingRule ret = null;
			for (JournalingRule inst : JournalingRule.values()) {
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
	
	static public enum AmountRule {
		NA(0L),
		AMOUNT(10L), 
		LINKED_TABLE(20L);   

		private long id;

		private AmountRule(long id) {
			this.id = id;
		}

		public final long getId() {
			return this.id;
		}

		public static AmountRule valueOf(long id) {
			AmountRule ret = null;
			for (AmountRule inst : AmountRule.values()) {
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
	
	public AccJournalEntryTemplate() {
		super(DomainClass.AccJournalEntryTemplate);
	}
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.AccJournalEntryTemplate);
	}
	
	private AEDescriptor financialTransactionTemplate;
	
	private AccountIdentificationRule accountIdentificationRule = AccountIdentificationRule.NA;
	
	private AEDescriptive accAccount;
	
	private boolean debit;
	
	private boolean credit;
	
	private JournalIdentificationRule journalIdentificationRule = JournalIdentificationRule.NA;
	
	private AEDescriptive accJournal;
	
	private JournalingRule journalingRule = JournalingRule.NA;
	
	private AmountRule amountRule = AmountRule.NA;
	
	private AEDescriptive amountParameter;
	
	private AEDescriptor quete;

	/**
	 * @return the financialTransactionTemplate
	 */
	public AEDescriptor getFinancialTransactionTemplate() {
		return financialTransactionTemplate;
	}

	/**
	 * @param financialTransactionTemplate the financialTransactionTemplate to set
	 */
	public void setFinancialTransactionTemplate(AEDescriptor financialTransactionTemplate) {
		this.financialTransactionTemplate = financialTransactionTemplate;
	}

	/**
	 * @return the accountIdentificationRule
	 */
	public AccountIdentificationRule getAccountIdentificationRule() {
		return accountIdentificationRule;
	}

	/**
	 * @param accountIdentificationRule the accountIdentificationRule to set
	 */
	public void setAccountIdentificationRule(
			AccountIdentificationRule accountIdentificationRule) {
		this.accountIdentificationRule = accountIdentificationRule;
	}

	/**
	 * @return the accAccount
	 */
	public AEDescriptive getAccAccount() {
		return accAccount;
	}

	/**
	 * @param accAccount the accAccount to set
	 */
	public void setAccAccount(AEDescriptive accAccount) {
		this.accAccount = accAccount;
	}

	/**
	 * @return the journalIdentificationRule
	 */
	public JournalIdentificationRule getJournalIdentificationRule() {
		return journalIdentificationRule;
	}

	/**
	 * @param journalIdentificationRule the journalIdentificationRule to set
	 */
	public void setJournalIdentificationRule(
			JournalIdentificationRule journalIdentificationRule) {
		this.journalIdentificationRule = journalIdentificationRule;
	}

	/**
	 * @return the accJournal
	 */
	public AEDescriptive getAccJournal() {
		return accJournal;
	}

	/**
	 * @param accJournal the accJournal to set
	 */
	public void setAccJournal(AEDescriptive accJournal) {
		this.accJournal = accJournal;
	}

	/**
	 * @return the journalingRule
	 */
	public JournalingRule getJournalingRule() {
		return journalingRule;
	}

	/**
	 * @param journalingRule the journalingRule to set
	 */
	public void setJournalingRule(JournalingRule journalingRule) {
		this.journalingRule = journalingRule;
	}

	/**
	 * @return the amountRule
	 */
	public AmountRule getAmountRule() {
		return amountRule;
	}

	/**
	 * @param amountRule the amountRule to set
	 */
	public void setAmountRule(AmountRule amountRule) {
		this.amountRule = amountRule;
	}

	/**
	 * @return the amountParameter
	 */
	public AEDescriptive getAmountParameter() {
		return amountParameter;
	}

	/**
	 * @param amountParameter the amountParameter to set
	 */
	public void setAmountParameter(AEDescriptive amountParameter) {
		this.amountParameter = amountParameter;
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject jsonObject = super.toJSONObject();

		// accountIdentificationRuleId
		if(accountIdentificationRule != null) {
			jsonObject.put(JSONKey.accountIdentificationRuleId.name(), accountIdentificationRule.getId());
		}
		
		// accAccountId
		if(accAccount != null) {
			jsonObject.put(JSONKey.accAccountId.name(), accAccount.getDescriptor().getID());
		}
		
		// journalIdentificationRuleId
		if(journalIdentificationRule != null) {
			jsonObject.put(JSONKey.journalIdentificationRuleId.name(), journalIdentificationRule.getId());
		}
		
		// journalCode
		if(accJournal != null) {
			jsonObject.put(JSONKey.journalCode.name(), accJournal.getDescriptor().getCode());
		}
		
		// journalingRuleId
		if(journalingRule != null) {
			jsonObject.put(JSONKey.journalingRuleId.name(), journalingRule.getId());
		}
		
		// amountRuleId
		if(amountRule != null) {
			jsonObject.put(JSONKey.amountRuleId.name(), amountRule.getId());
		}
		
		// amountParameter
		if(amountParameter != null) {
			jsonObject.put(JSONKey.amountParameter.name(), amountParameter.getDescriptor().getCode());
		}
		
		// debit
		jsonObject.put(JSONKey.dt.name(), this.debit);
		
		// credit
		jsonObject.put(JSONKey.ct.name(), this.credit);
		
		// quete
		if(getQuete() != null) {
			jsonObject.put(JSONKey.codeQuete.name(), getQuete().getCode());
		}
		
		return jsonObject;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// accountIdentificationRuleId
		setAccountIdentificationRule(AccountIdentificationRule.valueOf(jsonObject.optLong(JSONKey.accountIdentificationRuleId.name())));
		
		// accAccountId
		long accAccountId = jsonObject.optLong(JSONKey.accAccountId.name());
		if(AEPersistent.ID.isPersistent(accAccountId)) {
			setAccAccount(AccAccount.lazyDescriptor(accAccountId));
		}
		
		// journalIdentificationRuleId
		setJournalIdentificationRule(JournalIdentificationRule.valueOf(jsonObject.optLong(JSONKey.journalIdentificationRuleId.name())));
		
		// journalCode
		if(jsonObject.has(JSONKey.journalCode.name())) {
			AEDescriptive journal = new AEDescriptorImp(AEPersistentUtil.NEW_ID, DomainClass.AccJournal);
			journal.getDescriptor().setCode(jsonObject.optString(JSONKey.journalCode.name()));
			setAccJournal(journal);
		}
		
		// journalingRuleId
		setJournalingRule(JournalingRule.valueOf(jsonObject.optLong(JSONKey.journalingRuleId.name())));
		
		// amountRuleId
		setAmountRule(AmountRule.valueOf(jsonObject.optLong(JSONKey.amountRuleId.name())));
		
		// amountParameter
		if(jsonObject.has(JSONKey.amountParameter.name())) {
			AEDescriptive amountParameter = new AEDescriptorImp(AEPersistentUtil.NEW_ID, DomainClass.TRANSIENT);
			amountParameter.getDescriptor().setCode(jsonObject.optString(JSONKey.amountParameter.name()));
			setAmountParameter(amountParameter);
		}
		
		// debit
		setDebit(jsonObject.getBoolean(JSONKey.dt.name()));
		
		// credit
		setCredit(jsonObject.getBoolean(JSONKey.ct.name()));
		
		// quete
		String codeQuete = jsonObject.optString(JSONKey.codeQuete.name());
		if(!AEStringUtil.isEmpty(codeQuete)) {
			setQuete(new AEDescriptorImp().withCode(codeQuete));
		}
	}

	/**
	 * @return the debit
	 */
	public boolean isDebit() {
		return debit;
	}

	/**
	 * @param debit the debit to set
	 */
	public void setDebit(boolean debit) {
		this.debit = debit;
	}

	/**
	 * @return the credit
	 */
	public boolean isCredit() {
		return credit;
	}

	/**
	 * @param credit the credit to set
	 */
	public void setCredit(boolean credit) {
		this.credit = credit;
	}

	/**
	 * @return the quete
	 */
	public AEDescriptor getQuete() {
		return quete;
	}

	/**
	 * @param quete the quete to set
	 */
	public void setQuete(AEDescriptor quete) {
		this.quete = quete;
	}
}
