/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 15.11.2009 16:58:18
 */
package eu.agileeng.services;

import javax.naming.InitialContext;

import eu.agileeng.accbureau.AEApp;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.messaging.ejb.MessagingBean;
import eu.agileeng.domain.messaging.ejb.MessagingLocal;
import eu.agileeng.security.ejb.AuthBean;
import eu.agileeng.security.ejb.AuthLocal;
import eu.agileeng.services.acc.ejb.AccBean;
import eu.agileeng.services.acc.ejb.AccLocal;
import eu.agileeng.services.acc.ejb.FinancialTransactionBean;
import eu.agileeng.services.acc.ejb.FinancialTransactionLocal;
import eu.agileeng.services.address.ejb.AddressBean;
import eu.agileeng.services.address.ejb.AddressLocal;
import eu.agileeng.services.auth.ejb.AuthAccessBean;
import eu.agileeng.services.auth.ejb.AuthAccessLocal;
import eu.agileeng.services.bank.ejb.BankBean;
import eu.agileeng.services.bank.ejb.BankLocal;
import eu.agileeng.services.cash.ejb.CashBean;
import eu.agileeng.services.cash.ejb.CashLocal;
import eu.agileeng.services.contact.ejb.ContactBean;
import eu.agileeng.services.contact.ejb.ContactLocal;
import eu.agileeng.services.council.ejb.CouncilBean;
import eu.agileeng.services.council.ejb.CouncilLocal;
import eu.agileeng.services.dmtbox.ejb.DmtboxBean;
import eu.agileeng.services.dmtbox.ejb.DmtboxLocal;
import eu.agileeng.services.document.ejb.AEDocumentBean;
import eu.agileeng.services.document.ejb.AEDocumentLocal;
import eu.agileeng.services.export.ejb.DocumentExportBean;
import eu.agileeng.services.export.ejb.DocumentExportLocal;
import eu.agileeng.services.facturation.ejb.AEFacturationBean;
import eu.agileeng.services.facturation.ejb.AEFacturationLocal;
import eu.agileeng.services.file.ejb.FileAttachmentBean;
import eu.agileeng.services.file.ejb.FileAttachmentLocal;
import eu.agileeng.services.jcr.ejb.JcrBean;
import eu.agileeng.services.jcr.ejb.JcrLocal;
import eu.agileeng.services.opportunity.ejb.OpportunityBean;
import eu.agileeng.services.opportunity.ejb.OpportunityLocal;
import eu.agileeng.services.party.ejb.PartyBean;
import eu.agileeng.services.party.ejb.PartyLocal;
import eu.agileeng.services.social.ejb.AESocialBean;
import eu.agileeng.services.social.ejb.AESocialLocal;
import eu.agileeng.services.tableauDeBord.ejb.TableauDeBordBean;
import eu.agileeng.services.tableauDeBord.ejb.TableauDeBordLocal;
import eu.agileeng.services.utility.ejb.AEUtilityBean;
import eu.agileeng.services.utility.ejb.AEUtilityLocal;

/**
 *
 */
public class ServiceLocator {
	// singleton's private instance 
	private static ServiceLocator me = new ServiceLocator();

	private InitialContext context = null;
	
	private final String moduleName = "MaParoisseServer";
	
	private ServiceLocator() {
		try {
			context = new InitialContext();            
		} catch (Exception e) {
			AEApp.logger().error("Cannot instantiate InitialContext. ", e);
		} 
	}

	// returns the Service Locator instance 
	static public ServiceLocator getInstance() { 
		return me;
	}
	
	// Services Constants Inner Class - service objects
	public static enum Services {
		CASH(CashBean.class.getSimpleName(), CashLocal.class.getName()),
		OPPORTUNITY(OpportunityBean.class.getSimpleName(), OpportunityLocal.class.getName()),
		PARTY(PartyBean.class.getSimpleName(), PartyLocal.class.getName()),
		ADDRESS(AddressBean.class.getSimpleName(), AddressLocal.class.getName()),
		CONTACT(ContactBean.class.getSimpleName(), ContactLocal.class.getName()),
		AUTH(AuthBean.class.getSimpleName(), AuthLocal.class.getName()),
		DOCUMENT(AEDocumentBean.class.getSimpleName(), AEDocumentLocal.class.getName()),
		FILE_ATTACHMENT(FileAttachmentBean.class.getSimpleName(), FileAttachmentLocal.class.getName()),
		ACC_SERVICE(AccBean.class.getSimpleName(), AccLocal.class.getName()),
		SOCIAl(AESocialBean.class.getSimpleName(), AESocialLocal.class.getName()),
		JCR(JcrBean.class.getSimpleName(), JcrLocal.class.getName()),
		FACTURATION(AEFacturationBean.class.getSimpleName(), AEFacturationLocal.class.getName()),
		TABLEAU_DE_BORD(TableauDeBordBean.class.getSimpleName(), TableauDeBordLocal.class.getName()),
		UTILITY(AEUtilityBean.class.getSimpleName(), AEUtilityLocal.class.getName()),
		MESSAGING(MessagingBean.class.getSimpleName(), MessagingLocal.class.getName()),
		DMTBOX(DmtboxBean.class.getSimpleName(), DmtboxLocal.class.getName()),
		DOCUMENT_EXPORT(DocumentExportBean.class.getSimpleName(), DocumentExportLocal.class.getName()),
		BANK(BankBean.class.getSimpleName(), BankLocal.class.getName()),
		COUNCIL(CouncilBean.class.getSimpleName(), CouncilLocal.class.getName()),
		AUTH_ACCESS(AuthAccessBean.class.getSimpleName(), AuthAccessLocal.class.getName()),
		FINANCIAL_TRANSACTION(FinancialTransactionBean.class.getSimpleName(), FinancialTransactionLocal.class.getName());
		
		private String beanName;
		
		private String localInterface;
		
		public String getBeanName() {
			return beanName;
		}

		public final String getLocalInterface() {
			return localInterface;
		}
		
		private Services(String beanName, String localInterface) {
			this.beanName = beanName;
			this.localInterface = localInterface;
		}
	}    

	public final AEService getService(Services service) throws AEException {
		try {
			StringBuilder ln = new StringBuilder("java:app");
			ln.append("/").append(moduleName).append("/").append(service.getBeanName()).append("!").append(service.getLocalInterface());
			return (AEService) context.lookup(ln.toString());
		} catch (Exception e) {
			throw new AEException(e.getMessage(), e);
		} 
	}
}
