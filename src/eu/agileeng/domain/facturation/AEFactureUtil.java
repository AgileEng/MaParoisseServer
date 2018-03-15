package eu.agileeng.domain.facturation;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.business.AEPaymentTerms;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.facturation.AEFacturationDAO;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.StringAlign;


public class AEFactureUtil {

	/**
	 * Percent or Amount mode
	 * 
	 * @author vvatov
	 */
	static public enum PercentAmountMode {
		NA(0L),
		PERCENT(10L),
		AMOUNT(20L);
				
		private long modeId;
		
		private PercentAmountMode(long modeId) {
			this.modeId = modeId;
		}
		
		public final long getModeId() {
			return this.modeId;
		}
		
		public static PercentAmountMode valueOf(long modeId) {
			PercentAmountMode ret = null;
			for (PercentAmountMode inst : PercentAmountMode.values()) {
				if(inst.getModeId() == modeId) {
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
	 * Percent or Amount mode
	 * 
	 * @author vvatov
	 */
	static public enum FactureItemType {
		NA(0L),
		ARTICLE(10L),
		CATEGORY(20L),
		NOTE(30L),
		ADVANCE(40L),
		ADVANCE_DEDUCTION(50L);
				
		private long typeId;
		
		private FactureItemType(long typeId) {
			this.typeId = typeId;
		}
		
		public final long getTypeId() {
			return this.typeId;
		}
		
		public static FactureItemType valueOf(long typeId) {
			FactureItemType ret = null;
			for (FactureItemType inst : FactureItemType.values()) {
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
	
	static public enum FactureState {
		NA(0L),
		IN_PROGRESS(10L),
		ACCEPTED(20L),
		INVOICED(30L),
		REFUSED(40L),
		DRAFT(110L),
		VALIDATED(115L),
		PAID(120L),
		CANCELLED(125L);
				
		private long stateId;
		
		private FactureState(long stateId) {
			this.stateId = stateId;
		}
		
		public final long getStateId() {
			return this.stateId;
		}
		
		public static FactureState valueOf(long stateId) {
			FactureState found = NA;
			for (FactureState state : FactureState.values()) {
				if(state.getStateId() == stateId) {
					found = state;
					break;
				}
			}
			return found;
		}
	}
	
	static public enum FactureSubType {
		NA(0),
		REGULAR(100L),
		ADVANCE(110L),
		CREDIT_NOTE(120L),
		DEBIT_NOTE(130L);
				
		private long subTypeId;
		
		private FactureSubType(long subTypeId) {
			this.subTypeId = subTypeId;
		}
		
		public final long getSubTypeId() {
			return this.subTypeId;
		}
		
		public static FactureSubType valueOf(long subTypeId) {
			FactureSubType found = NA;
			for (FactureSubType subType : FactureSubType.values()) {
				if(subType.getSubTypeId() == subTypeId) {
					found = subType;
					break;
				}
			}
			return found;
		}
	}
	
	static public enum PayableType {
		NA(0),
		SCHEDULE(10),
	    REQUISITION(20), //will be used when we have monetary obligation 
	    JUNK(30); // paid facture's payment over not invoiced devise, must be transformed to Requisition when
		          // devise is invoiced
				
		private long typeId;
		
		private PayableType(long typeId) {
			this.typeId = typeId;
		}
		
		public final long getTypeId() {
			return this.typeId;
		}
		
		public static PayableType valueOf(long typeId) {
			PayableType found = NA;
			for (PayableType type : PayableType.values()) {
				if(type.getTypeId() == typeId) {
					found = type;
					break;
				}
			}
			return found;
		}
	}
	
	static public enum CreditNoteReason {
		NA(0),
		CANCEL(10),
	    GOODS_RETURNED(20),  
	    PRICE_CORRECTION(30); 
				
		private long reasonId;
		
		private CreditNoteReason(long reasonId) {
			this.reasonId = reasonId;
		}
		
		public final long getReasonId() {
			return this.reasonId;
		}
		
		public static CreditNoteReason valueOf(long reasonId) {
			CreditNoteReason found = NA;
			for (CreditNoteReason reason : CreditNoteReason.values()) {
				if(reason.getReasonId() == reasonId) {
					found = reason;
					break;
				}
			}
			return found;
		}
	}
	
	static public enum PaymentType {
		NA(0),
	    ADVANCE(10), //will be used when we have monetary obligation
		REGULAR(20),
	    BALANCE(30); //  
				
		private long typeId;
		
		private PaymentType(long typeId) {
			this.typeId = typeId;
		}
		
		public final long getTypeId() {
			return this.typeId;
		}
		
		public static PaymentType valueOf(long typeId) {
			PaymentType found = NA;
			for (PaymentType type : PaymentType.values()) {
				if(type.getTypeId() == typeId) {
					found = type;
					break;
				}
			}
			return found;
		}
	}
	
	public static AEPaymentTerms getPaymentTerms(long payTermsId) throws AEException {
		AEFacturationDAO facturationDAO = DAOFactory.getInstance().getFacturationDAO(null);
		return facturationDAO.loadPaymentTerms(payTermsId);
	}
	
//	public static AEPaymentTermsList getPaymentTerms(AEDescriptor ownerDescr) throws AEException {
//		AEPaymentTermsList ptList = new AEPaymentTermsList();
//		try {
//			// reserve the ID = 1 for not available
////			AEPaymentTerms pt = new AEPaymentTerms();
////			pt.setID(1);
////			pt.setCode("N/A");
////			pt.setName("N/A");
////			pt.setDelay(30);
////			// account ???
////			pt.setDefault(false);
////			pt.setSequenceNumber(1);
////			pt.setEndOfMonth(false);
////			ptList.add(pt);
//			
//			AEPaymentTerms pt = new AEPaymentTerms();
//			pt.setID(2);
//			pt.setCode("CHQ");
//			pt.setName("Chèque");
//			pt.setDelay(30);
//			// account ???
//			pt.setDefault(false);
//			pt.setSequenceNumber(2);
//			pt.setEndOfMonth(true);
//			pt.setDescription("CHQ / 30 jours / FM");
//			ptList.add(pt);
//			
//			pt = new AEPaymentTerms();
//			pt.setID(3);
//			pt.setCode("VIR");
//			pt.setName("Virement");
//			pt.setDelay(30);
//			// account ???
//			pt.setDefault(true);
//			pt.setSequenceNumber(3);
//			pt.setEndOfMonth(false);
//			pt.setDescription("VIR / 30 jours");
//			ptList.add(pt);
//			
//			pt = new AEPaymentTerms();
//			pt.setID(4);
//			pt.setCode("LCR1");
//			pt.setName("Lettre de change releve");
//			pt.setDelay(30);
//			// account ???
//			pt.setDefault(false);
//			pt.setSequenceNumber(4);
//			pt.setEndOfMonth(false);
//			pt.setDescription("LCR1 / 30 jours");
//			ptList.add(pt);
//			
//			pt = new AEPaymentTerms();
//			pt.setID(5);
//			pt.setCode("ESP");
//			pt.setName("Espèces");
//			pt.setDelay(0);
//			// account ???
//			pt.setDefault(false);
//			pt.setSequenceNumber(5);
//			pt.setEndOfMonth(false);
//			pt.setDescription("ESP / 0 jours");
//			ptList.add(pt);
//
//			return ptList;
//		} catch (Exception e) {
//			throw new AEException(e);
//		}
//	}
	
	static public enum ReceiptBook {
		NA(0L, AEStringUtil.EMPTY_STRING, 1, "0"),
		DEVIS(1L, "D", 5, "0"),
		FACTURE(2L, "F", 5, "0"),
		AVOIR(3L, "AV", 5, "0"),
		BROUILLON(4L, "BR", 5, "0"),
		CLIENT(5L, "C", 5, "0"),
		ARTICLE(6L, "A", 5, "0");
				
		private long receiptBookId;
		
		private String prefix = AEStringUtil.EMPTY_STRING;
		
		private int length = 5;
		
		private String symbol;
		
		private ReceiptBook(long receiptBookId, String prefix, int length, String symbol) {
			this.receiptBookId = receiptBookId;
			this.prefix = prefix;
			this.length = length;
			this.symbol = symbol;
		}
		
		public final long getReceiptBookId() {
			return this.receiptBookId;
		}
		
		public static ReceiptBook valueOf(long receiptBookId) {
			ReceiptBook ret = null;
			for (ReceiptBook inst : ReceiptBook.values()) {
				if(inst.getReceiptBookId() == receiptBookId) {
					ret = inst;
					break;
				}
			}
			return ret;
		}

		public String getPrefix() {
			return prefix;
		}

		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}

		public int getLength() {
			return length;
		}

		public void setLength(int length) {
			this.length = length;
		}
		
		public final String toString(long number) {
			StringAlign sa = new StringAlign(getLength(), StringAlign.Align.RIGHT);
			return (getPrefix() + sa.format(Long.toString(number)).replaceAll(" ", getSymbol()));
		}

		public String getSymbol() {
			return symbol;
		}

		public void setSymbol(String symbol) {
			this.symbol = symbol;
		}
	}
}
