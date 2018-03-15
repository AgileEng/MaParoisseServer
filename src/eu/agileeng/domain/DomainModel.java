/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 03.11.2009 20:16:13
 */
package eu.agileeng.domain;

import java.io.Serializable;



/**
 * Unique Identification (UID) repository for MonBureau application.
 * An item(component/class/instance) should be uniquely identified 
 * if the application behaviour dependents on this knowledge. 
 */
public final class DomainModel {
	/**
	 * The singleton instance
	 */
	private static DomainModel inst = new DomainModel();
	
	static public enum DomainClass implements Serializable {
		TRANSIENT(0L),
		ERROR(1L),
		COMMENT(2L),
		UnitOfMeasurement(3L),
		BankAccount(4L),
		VATItem(5L),
		AEAttribute(6L),
		AEAttributeValue(7),
		AEApp(50L),
		AEAppModule(60L),
		AppModuleTemplate(61L),
		AEAppConfig(70L),
		AEModuleAlert(71L),
		JcrNode(200),
		OPGANIZATION_INDYSTRY(10000L),
		OPGANIZATION_FINANCE_CAPITAL(10010L),
		OPGANIZATION_EMPL_NUMBER(10020L),
		OPPORTUNITY(10200L),
		CONTACT(10400L),
		ADDRESS(10405L),
		PARTY(10500L),
		PartyAttribute(10501L),
		SimpleParty(10502L),
		PERSON(10505),
		ORGANIZATION(10510L),
		ORGANIZATION_TEMPLATE(10511L),
		OPPORTUNITY_PHASE(10515),
		OPPORTUNITY_STATUS(10520),
		TASK(10525),
		TASK_CATEGORY(10530),
		EMPLOYEE(10535),
		Contributor(10536),
		ContributorDonation(10537),
		AuthRole(10540),
		AuthPermission(10545),
		AuthSubject(10550),
		AuthRoleSubAssoc(10555),
		SubbjectCompAssoc(10556),
		AuthPermissionAssignment(10560),
		AuthOwner(10561),
		AuthAclEntry(10562),
		AeDocument(20000),
		AEDocumentFilter(20001),
		AEPrintTemplate(20002),
		DocumentType(20005),
		DocumentState(20006),
		FileAttachment(20010),
		FileAttachmentType(20015), // for file attachment type enumeration
		TaxType(20020), // enumerated class
		DepartmentAGIRC(20025), // enumerated class
		DepartmentARRCO(20030), // enumerated class
		EmploymentClassification(20035), // enumerated class
		ContractDeTravailType(20040), // enumerated class
		ContractDeTravailReason(20045), // enumerated class
		ContractDeTravailAssistence(20050), // enumerated class
		FeeType(20055),
		CashJournalEntry(20060),
		CashPeriod(20061),
		CashJETransaction(20065),
		CashJETProp(20070),
		AccJournal(20071),
		AccJournalItem(20075),
		AccJournalEntry(20080),
		AccJournalEntryTemplate(20081),
		AccJournalFilter(20082),
		AccJournalResult(20083),
		AEInvoiceItem(20085),
		AETradeDocumentFilter(20090),
		AETradeDocumentResult(20095),
		AccPeriod(20100),
		AccAccount(20105),
		AccAttriute(20106),
		AccAccountBalance(20107),
		CFC(20110),
		CFCColumn(20115),
		CFCCell(20120),
		MandatCell(20121),
		BankTransaction(20125),
		BankRecognitionRule(20126),
		BankAccountBalance(20130),
		AESocialDocumentFilter(20135),
		SalaryGrid(20140),
		SalaryGridItem(20145),
		SocialInfo(20150),
		SocialTimeSheetEntry(20155),
		AEArticle(20300),
		AEClient(20305),
		AEPaymentTerms(20310),
		AEFactureItem(20315),
		AEPayment(20320),
		AEPaymentsFilter(20325),
		AEFacturePrintTemplate(20330),
		AEPortlet(20400),
		BudgetGroup(20401),
		AEBookmark(20402),
		AEBudgetItem(20403),
		AEBudgetItemEntry(20404),
		AEBudgetMonthVariation(20405),
		DiscussionBoardPost(20500),
		DiscussionBoardSubject(20501),
		DiscussionBoardTask(20502),
		Decision(20503),
		TaskFilter(20504),
		Council(20600),
		CouncilMember(20601),
		FinancialTransactionTemplate(20700),
		FinancialTransaction(20705),
		Quete(20710),
		BordereauParoisse(20715);
		
		private long id;
		
		private DomainClass(long id) {
			this.id = id;
		}
		
		public final long getID() {
			return this.id;
		}
		
		public final static DomainClass valueOf(long id) {
			DomainClass ret = null;
			for (DomainClass clazz : DomainClass.values()) {
				if(clazz.getID() == id) {
					ret = clazz;
					break;
				}
			}
			return ret;
		}
	}
	
	public final static DomainModel getInstance() {
		return DomainModel.inst;
	}
	
	public final EnumeratedTypeFactory getEnumeratedTypeFactory() {
		return EnumeratedTypeFactory.getInstance();
	}
}
