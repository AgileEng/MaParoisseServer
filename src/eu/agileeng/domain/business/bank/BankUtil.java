package eu.agileeng.domain.business.bank;


public class BankUtil {

	/**
	 * Available rule types for BankRecognitionRule
	 * 
	 */
	static public enum RuleCondition {
		NA(0L),
		EQUALS(10),
		BEGINS_WITH(20L),
		CONTAINS(30L),
		ENDS_WITH(40L),
		REG_EXP(50L);
				
		private long conditionId;
		
		private RuleCondition(long conditionId) {
			this.conditionId = conditionId;
		}
		
		public final long getConditionId() {
			return this.conditionId;
		}
		
		public static RuleCondition valueOf(long conditionId) {
			RuleCondition ret = null;
			for (RuleCondition inst : RuleCondition.values()) {
				if(inst.getConditionId() == conditionId) {
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
	
	public BankUtil() {
	}

}
