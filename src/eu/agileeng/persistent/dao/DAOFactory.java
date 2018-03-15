/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 17.11.2009 14:12:12
 */
package eu.agileeng.persistent.dao;

import java.sql.SQLException;

import javax.sql.DataSource;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.messaging.dao.MessagingDAO;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.acc.AccJournalDAO;
import eu.agileeng.persistent.dao.acc.AccPeriodDAO;
import eu.agileeng.persistent.dao.acc.AccountDAO;
import eu.agileeng.persistent.dao.acc.BordereauParoisseDAO;
import eu.agileeng.persistent.dao.acc.ChartOfAccountsDAO;
import eu.agileeng.persistent.dao.acc.FinancialTransactionDAO;
import eu.agileeng.persistent.dao.acc.VATItemDAO;
import eu.agileeng.persistent.dao.app.AppDAO;
import eu.agileeng.persistent.dao.cash.BankAccountDAO;
import eu.agileeng.persistent.dao.cash.BankJournalDAO;
import eu.agileeng.persistent.dao.cash.CFCDAO;
import eu.agileeng.persistent.dao.cash.CashModuleDAO;
import eu.agileeng.persistent.dao.common.AECommentDAO;
import eu.agileeng.persistent.dao.common.ContributorDAO;
import eu.agileeng.persistent.dao.common.SimplePartyDAO;
import eu.agileeng.persistent.dao.council.CouncilDAO;
import eu.agileeng.persistent.dao.dmtbox.DmtboxDAO;
import eu.agileeng.persistent.dao.document.AEDocumentDAO;
import eu.agileeng.persistent.dao.document.DocNumSequenceDAO;
import eu.agileeng.persistent.dao.document.contractdeTravail.CotractDeTravailDAO;
import eu.agileeng.persistent.dao.document.rib.RibDAO;
import eu.agileeng.persistent.dao.document.social.AccidentDuTravailDAO;
import eu.agileeng.persistent.dao.document.statement.AEStatementDAO;
import eu.agileeng.persistent.dao.document.trade.AEDocumentItemDAO;
import eu.agileeng.persistent.dao.document.trade.AETradeDocumentDAO;
import eu.agileeng.persistent.dao.facturation.AEFacturationDAO;
import eu.agileeng.persistent.dao.facturation.AEFactureDAO;
import eu.agileeng.persistent.dao.facturation.AEFactureItemDAO;
import eu.agileeng.persistent.dao.facturation.AEPaymentDAO;
import eu.agileeng.persistent.dao.file.FileAttachmentDAO;
import eu.agileeng.persistent.dao.ide.IDEDAO;
import eu.agileeng.persistent.dao.inventory.InventoryDAO;
import eu.agileeng.persistent.dao.mandat.MandatDAO;
import eu.agileeng.persistent.dao.oracle.AddressDAO;
import eu.agileeng.persistent.dao.oracle.CashDAO;
import eu.agileeng.persistent.dao.oracle.ContactDAO;
import eu.agileeng.persistent.dao.oracle.EmployeeDAO;
import eu.agileeng.persistent.dao.oracle.MsSQLDAOFactory;
import eu.agileeng.persistent.dao.oracle.OpportunityDAO;
import eu.agileeng.persistent.dao.oracle.OracleDAOFactory;
import eu.agileeng.persistent.dao.oracle.OrganizationDAO;
import eu.agileeng.persistent.dao.oracle.OrganizationTemplateDAO;
import eu.agileeng.persistent.dao.oracle.PersonDAO;
import eu.agileeng.persistent.dao.oracle.SequenceDAO;
import eu.agileeng.persistent.dao.oracle.SubjectCompAssocDAO;
import eu.agileeng.persistent.dao.pam.PAMDAO;
import eu.agileeng.persistent.dao.social.SocialDAO;
import eu.agileeng.persistent.dao.tableauDeBord.TableauDeBordDAO;
import eu.agileeng.persistent.dao.utility.AEBookmarkDAO;
import eu.agileeng.persistent.quete.QueteDAO;
import eu.agileeng.security.ejb.dao.AuthPrincipalDAO;
import eu.agileeng.security.ejb.dao.AuthRoleDAO;
import eu.agileeng.security.ejb.dao.AuthSubjectRoleAssocDAO;


/**
 *
 */
public abstract class DAOFactory {

	public static enum AEDataSource {
		MSSQL,
		ORACLE;
	}
	
	public final static DAOFactory getInstance() {
		DAOFactory daoFactory = null;
		AEDataSource dsCode = AEDataSource.MSSQL;
		switch(dsCode) {
			case ORACLE:
				daoFactory = OracleDAOFactory.getInst();
				break;
			case MSSQL:
				daoFactory = MsSQLDAOFactory.getInst();
				break;
			default:
				assert(false) : "Undefined AEDataSource";
				break;
		}
		return daoFactory;
	}
	
	public abstract DataSource getDataSource() throws AEException;
	
	public final AEConnection getConnection() throws AEException {
		try {
			return new AEConnection(getDataSource().getConnection(), true);
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		}
	}
	
	public final AEConnection getConnection(AEConnection aeConnection) throws AEException {
		return aeConnection == null ? 
				getConnection() :
				new AEConnection(aeConnection.getConnection(), false);
	}
	
	public abstract CashDAO getCashDAO(AEConnection aeConnection) throws AEException;
	
	public abstract SequenceDAO getSequenceDAO(AEConnection aeConnection) throws AEException;
	
	public abstract ContactDAO getContactDAO(AEConnection aeConnection) throws AEException;
	
	public abstract PersonDAO getPersonDAO(AEConnection aeConnection) throws AEException;
	
	public abstract OrganizationDAO getOrganizationDAO(AEConnection aeConnection) throws AEException;
	
	public abstract OpportunityDAO getOpportunityDAO(AEConnection aeConnection) throws AEException;
	
	public abstract AddressDAO getAddressDAO(AEConnection aeConnection) throws AEException;
	
	public abstract AEDocumentDAO getDocumentDAO(AEDocumentType documentType, AEConnection aeConnection) throws AEException;
	
	public abstract AuthPrincipalDAO getAuthPrincipalDAO(AEConnection aeConnection) throws AEException;
	
	public abstract AuthRoleDAO getAuthRoleDAO(AEConnection aeConnection) throws AEException;
	
	public abstract AuthSubjectRoleAssocDAO getAuthSubjectRoleAssocDAO(AEConnection aeConnection) throws AEException;
	
	public abstract EmployeeDAO getEmployeeDAO(AEConnection aeConnection) throws AEException;
	
	public abstract SubjectCompAssocDAO getSubjectCompAssocDAO(AEConnection aeConnection) throws AEException;
	
	public abstract FileAttachmentDAO getFileAttachmentDAO(AEConnection aeConnection) throws AEException;
	
	public abstract AECommentDAO getAECommentDAO(AEConnection aeConnection) throws AEException;
	
	public abstract CotractDeTravailDAO getCotractDeTravailDAO(AEConnection aeConnection) throws AEException;
	
	public abstract AccidentDuTravailDAO getAccidentDuTravailDAO(AEConnection aeConnection) throws AEException;
	
	public abstract RibDAO getRibDAO(AEConnection aeConnection) throws AEException;
	
	public abstract VATItemDAO getVATItemDAO(AEConnection aeConnection) throws AEException;
	
	public abstract ChartOfAccountsDAO getChartOfAccountsDAO(AEConnection aeConnection) throws AEException;

	public abstract AccountDAO getAccountDAO(AEConnection aeConnection) throws AEException;
	
	public abstract CashModuleDAO getCashModuleDAO(AEConnection aeConnection) throws AEException;
	
	public abstract AccJournalDAO getAccJournalDAO(AEConnection aeConnection) throws AEException;
	
	public abstract AETradeDocumentDAO getAETradeDocumentDAO(AEConnection aeConnection) throws AEException;
	
	public abstract AEDocumentItemDAO getAEDocumentItemDAO(AEConnection aeConnection) throws AEException;
	
	public abstract AccPeriodDAO getAccPeriodDAO(AEConnection aeConnection) throws AEException;
	
	public abstract CFCDAO getCFCDAO(AEConnection aeConnection) throws AEException;
	
	public abstract IDEDAO getIDEDAO(AEConnection aeConnection) throws AEException;
	
	public abstract BankAccountDAO getBankAccountDAO(AEConnection aeConnection) throws AEException;
	
	public abstract BankJournalDAO getBankJournalDAO(AEConnection aeConnection) throws AEException;
	
	public abstract MandatDAO getMandatDAO(AEConnection aeConnection) throws AEException;
	
	public abstract AppDAO getAppDAO(AEConnection aeConnection) throws AEException;
	
	public abstract PAMDAO getPAMDAO(AEConnection aeConnection) throws AEException;
	
	public abstract SocialDAO getSocialDAO(AEConnection aeConnection) throws AEException;
	
	public abstract AEFacturationDAO getFacturationDAO(AEConnection aeConnection) throws AEException;
	
	public abstract AEFactureDAO getFactureDAO(AEConnection aeConnection) throws AEException;
	
	public abstract AEFactureItemDAO getFactureItemDAO(AEConnection aeConnection) throws AEException;
	
	public abstract AEPaymentDAO getPaymentDAO(AEConnection aeConnection) throws AEException;
	
	public abstract TableauDeBordDAO getTableauDeBordDAO(AEConnection aeConnection) throws AEException;
	
	public abstract AEBookmarkDAO getAEBookmarkDAO(AEConnection aeConnection) throws AEException;
	
	public abstract InventoryDAO getInventoryDAO(AEConnection aeConnection) throws AEException;

	public abstract MessagingDAO getMessagingDAO(AEConnection aeConnection) throws AEException;
	
	public abstract DmtboxDAO getDmtboxDAO(AEConnection aeConnection) throws AEException;
	
	public abstract OrganizationTemplateDAO getOrganizationTemplateDAO(AEConnection aeConnection) throws AEException;
	
	public abstract CouncilDAO getCouncilDAO(AEConnection aeConnection) throws AEException;
	
	public abstract FinancialTransactionDAO getFinancialTransactionDAO(AEConnection aeConnection) throws AEException;
	
	public abstract ContributorDAO getContributorDAO(AEConnection aeConnection) throws AEException;
	
	public abstract SimplePartyDAO getSimplePartyDAO(AEConnection aeConnection) throws AEException;
	
	public abstract AEStatementDAO getAEStatementDAO(AEConnection aeConnection) throws AEException;
	
	public abstract BordereauParoisseDAO getBordereauParoisseDAO(AEConnection aeConnection) throws AEException;
	
	public abstract QueteDAO getQueteDAO(AEConnection aeConnection) throws AEException;
	
	public abstract DocNumSequenceDAO getDocNumSequenceDAO(AEConnection aeConnection) throws AEException;
}
