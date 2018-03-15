/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 17.11.2009 14:15:24
 */
package eu.agileeng.persistent.dao.oracle;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.messaging.dao.MessagingDAO;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.DAOFactory;
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
import eu.agileeng.persistent.dao.pam.PAMDAO;
import eu.agileeng.persistent.dao.social.SocialDAO;
import eu.agileeng.persistent.dao.tableauDeBord.TableauDeBordDAO;
import eu.agileeng.persistent.dao.utility.AEBookmarkDAO;
import eu.agileeng.persistent.quete.QueteDAO;
import eu.agileeng.security.ejb.dao.AuthRoleDAO;
import eu.agileeng.security.ejb.dao.AuthSubjectRoleAssocDAO;
import eu.agileeng.security.ejb.dao.AuthPrincipalDAO;


/**
 *
 */
public class OracleDAOFactory extends DAOFactory {
	private static String lookupName = "java:OracleDS";
	private static OracleDAOFactory inst = new OracleDAOFactory();

	private DataSource ds;
	
	/**
	 * 
	 */
	private OracleDAOFactory() {
	}

	public static OracleDAOFactory getInst() {
		return inst;
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.DAOFactory#getDataSource()
	 */
	@Override
	public synchronized DataSource getDataSource() throws AEException {
		if(this.ds == null) {
			try {
				InitialContext ic = new InitialContext();
				this.ds = (DataSource) ic.lookup(lookupName);
				ic.close();
			} catch (NamingException e) {
				throw new AEException(e.getMessage(), e);
			}
		}
		return this.ds;
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.dao.DAOFactory#getAviDAO(eu.agileeng.persistent.AEConnection)
	 */
	@Override
	public CashDAO getCashDAO(AEConnection aeConnection) throws AEException {
		return new CashDAO(getConnection(aeConnection));
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.dao.DAOFactory#getSequenceDAO(eu.agileeng.persistent.AEConnection)
	 */
	@Override
	public SequenceDAO getSequenceDAO(AEConnection aeConnection) throws AEException {
		return new SequenceDAO(getConnection(aeConnection));
	}

//	/* (non-Javadoc)
//	 * @see eu.agileeng.persistent.dao.DAOFactory#getEnumeratedTypeDAO(eu.agileeng.persistent.AEConnection)
//	 */
//	@Override
//	public EnumeratedTypeDAO getEnumeratedTypeDAO(AEConnection aeConnection) throws AEException {
//		return new EnumeratedTypeDAO(getConnection(aeConnection));
//	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.dao.DAOFactory#getContactDAO(eu.agileeng.persistent.AEConnection)
	 */
	@Override
	public ContactDAO getContactDAO(AEConnection aeConnection) throws AEException {
		return new ContactDAO(getConnection(aeConnection));
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.dao.DAOFactory#getPersonDAO(eu.agileeng.persistent.AEConnection)
	 */
	@Override
	public PersonDAO getPersonDAO(AEConnection aeConnection) throws AEException {
		return new PersonDAO(getConnection(aeConnection));
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.dao.DAOFactory#getOrganizationDAO(eu.agileeng.persistent.AEConnection)
	 */
	@Override
	public OrganizationDAO getOrganizationDAO(AEConnection aeConnection) throws AEException {
		return new OrganizationDAO(getConnection(aeConnection));
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.dao.DAOFactory#getOpportunityDAO(eu.agileeng.persistent.AEConnection)
	 */
	@Override
	public OpportunityDAO getOpportunityDAO(AEConnection aeConnection) throws AEException {
		return new OpportunityDAO(getConnection(aeConnection));
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.dao.DAOFactory#getAddressDAO(eu.agileeng.persistent.AEConnection)
	 */
	@Override
	public AddressDAO getAddressDAO(AEConnection aeConnection) throws AEException {
		return new AddressDAO(aeConnection);
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.dao.DAOFactory#getDocumentDAO(eu.agileeng.domain.document.AEDocumentType, eu.agileeng.persistent.AEConnection)
	 */
	@Override
	public AEDocumentDAO getDocumentDAO(AEDocumentType documentType, AEConnection aeConnection) throws AEException {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.dao.DAOFactory#getAuthRoleDAO(eu.agileeng.persistent.AEConnection)
	 */
	@Override
	public AuthRoleDAO getAuthRoleDAO(AEConnection aeConnection) throws AEException {
		return new AuthRoleDAO(getConnection(aeConnection));
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.dao.DAOFactory#getAuthSubjectDAO(eu.agileeng.persistent.AEConnection)
	 */
	@Override
	public AuthPrincipalDAO getAuthPrincipalDAO(AEConnection aeConnection) throws AEException {
		return new AuthPrincipalDAO(getConnection(aeConnection));
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.dao.DAOFactory#getAuthRoleSubAssocDAO(eu.agileeng.persistent.AEConnection)
	 */
	@Override
	public AuthSubjectRoleAssocDAO getAuthSubjectRoleAssocDAO(AEConnection aeConnection) throws AEException {
		return new AuthSubjectRoleAssocDAO(getConnection(aeConnection));
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.dao.DAOFactory#getEmployeeDAO(eu.agileeng.persistent.AEConnection)
	 */
	@Override
	public EmployeeDAO getEmployeeDAO(AEConnection aeConnection) throws AEException {
		return new EmployeeDAO(getConnection(aeConnection));
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.dao.DAOFactory#getSubjectCompAssocDAO(eu.agileeng.persistent.AEConnection)
	 */
	@Override
	public SubjectCompAssocDAO getSubjectCompAssocDAO(AEConnection aeConnection) throws AEException {
		return new SubjectCompAssocDAO(getConnection(aeConnection));
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.dao.DAOFactory#getFileAttachmentDAO(eu.agileeng.persistent.AEConnection)
	 */
	@Override
	public FileAttachmentDAO getFileAttachmentDAO(AEConnection aeConnection) throws AEException {
		return new FileAttachmentDAO(getConnection(aeConnection));
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.dao.DAOFactory#getAECommentDAO(eu.agileeng.persistent.AEConnection)
	 */
	@Override
	public AECommentDAO getAECommentDAO(AEConnection aeConnection) throws AEException {
		return new AECommentDAO(getConnection(aeConnection));
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.persistent.dao.DAOFactory#getCotractDeTravailDAO(eu.agileeng.persistent.AEConnection)
	 */
	@Override
	public CotractDeTravailDAO getCotractDeTravailDAO(AEConnection aeConnection) throws AEException {
		return new CotractDeTravailDAO(getConnection(aeConnection));
	}

	@Override
	public VATItemDAO getVATItemDAO(AEConnection aeConnection) throws AEException {
		return new VATItemDAO(getConnection(aeConnection));
	}
	
	@Override
	public ChartOfAccountsDAO getChartOfAccountsDAO(AEConnection aeConnection) throws AEException {
		return new ChartOfAccountsDAO(getConnection(aeConnection));
	}

	@Override
	public AccountDAO getAccountDAO(AEConnection aeConnection) throws AEException {
		return new AccountDAO(getConnection(aeConnection));
	}

	@Override
	public CashModuleDAO getCashModuleDAO(AEConnection aeConnection) throws AEException {
		return new CashModuleDAO(getConnection(aeConnection));
	}

	@Override
	public AccJournalDAO getAccJournalDAO(AEConnection aeConnection) throws AEException {
		return new AccJournalDAO(getConnection(aeConnection));
	}

	@Override
	public AETradeDocumentDAO getAETradeDocumentDAO(AEConnection aeConnection) throws AEException {
		return new AETradeDocumentDAO(getConnection(aeConnection));
	}

	@Override
	public AEDocumentItemDAO getAEDocumentItemDAO(AEConnection aeConnection) throws AEException {
		return new AEDocumentItemDAO(getConnection(aeConnection));
	}

	@Override
	public AccPeriodDAO getAccPeriodDAO(AEConnection aeConnection) throws AEException {
		return new AccPeriodDAO(getConnection(aeConnection));
	}

	@Override
	public CFCDAO getCFCDAO(AEConnection aeConnection) throws AEException {
		return new CFCDAO(getConnection(aeConnection));
	}

	@Override
	public BankAccountDAO getBankAccountDAO(AEConnection aeConnection) throws AEException {
		return new BankAccountDAO(getConnection(aeConnection));
	}

	@Override
	public BankJournalDAO getBankJournalDAO(AEConnection aeConnection) throws AEException {
		return new BankJournalDAO(getConnection(aeConnection));
	}
	
	@Override
	public MandatDAO getMandatDAO(AEConnection aeConnection) throws AEException {
		return new MandatDAO(getConnection(aeConnection));
	}
	
	@Override
	public AppDAO getAppDAO(AEConnection aeConnection) throws AEException {
		return new AppDAO(getConnection(aeConnection));
	}

	@Override
	public PAMDAO getPAMDAO(AEConnection aeConnection) throws AEException {
		return new PAMDAO(getConnection(aeConnection));
	}
	
	@Override
	public AccidentDuTravailDAO getAccidentDuTravailDAO(AEConnection aeConnection) throws AEException {
		return new AccidentDuTravailDAO(getConnection(aeConnection));
	}

	@Override
	public IDEDAO getIDEDAO(AEConnection aeConnection) throws AEException {
		return new IDEDAO(getConnection(aeConnection));
	}

	@Override
	public RibDAO getRibDAO(AEConnection aeConnection) throws AEException {
		return new RibDAO(getConnection(aeConnection));
	}

	@Override
	public SocialDAO getSocialDAO(AEConnection aeConnection) throws AEException {
		return new SocialDAO(getConnection(aeConnection));
	}
	
	@Override
	public AEFacturationDAO getFacturationDAO(AEConnection aeConnection) throws AEException {
		return new AEFacturationDAO(getConnection(aeConnection));
	}
	
	@Override
	public AEFactureDAO getFactureDAO(AEConnection aeConnection) throws AEException {
		return new AEFactureDAO(getConnection(aeConnection));
	}
	
	@Override
	public AEFactureItemDAO getFactureItemDAO(AEConnection aeConnection) throws AEException {
		return new AEFactureItemDAO(getConnection(aeConnection));
	}
	
	@Override
	public AEPaymentDAO getPaymentDAO(AEConnection aeConnection) throws AEException {
		return new AEPaymentDAO(getConnection(aeConnection));
	}
	
	@Override
	public TableauDeBordDAO getTableauDeBordDAO(AEConnection aeConnection) throws AEException {
		return new TableauDeBordDAO(getConnection(aeConnection));
	}
	
	@Override
	public AEBookmarkDAO getAEBookmarkDAO(AEConnection aeConnection) throws AEException {
		return new AEBookmarkDAO(getConnection(aeConnection));
	}
	
	@Override
	public InventoryDAO getInventoryDAO(AEConnection aeConnection) throws AEException {
		return new InventoryDAO(getConnection(aeConnection));
	}

	@Override
	public MessagingDAO getMessagingDAO(AEConnection aeConnection) throws AEException {
		return new MessagingDAO(getConnection(aeConnection));
	}
	
	@Override
	public DmtboxDAO getDmtboxDAO(AEConnection aeConnection) throws AEException {
		return new DmtboxDAO(getConnection(aeConnection));
	}
	
	@Override
	public OrganizationTemplateDAO getOrganizationTemplateDAO(AEConnection aeConnection) throws AEException {
		return new OrganizationTemplateDAO(getConnection(aeConnection));
	}

	@Override
	public CouncilDAO getCouncilDAO(AEConnection aeConnection) throws AEException {
		return new CouncilDAO(getConnection(aeConnection));
	}
	
	@Override
	public FinancialTransactionDAO getFinancialTransactionDAO(AEConnection aeConnection) throws AEException {
		return new FinancialTransactionDAO(getConnection(aeConnection));
	}
	
	@Override
	public ContributorDAO getContributorDAO(AEConnection aeConnection) throws AEException {
		return new ContributorDAO(getConnection(aeConnection));
	}
	
	@Override
	public SimplePartyDAO getSimplePartyDAO(AEConnection aeConnection) throws AEException {
		return new SimplePartyDAO(getConnection(aeConnection));
	}
	
	@Override
	public AEStatementDAO getAEStatementDAO(AEConnection aeConnection) throws AEException {
		return new AEStatementDAO(getConnection(aeConnection));
	}
	
	@Override
	public BordereauParoisseDAO getBordereauParoisseDAO(AEConnection aeConnection) throws AEException {
		return new BordereauParoisseDAO(getConnection(aeConnection));
	}
	
	@Override
	public QueteDAO getQueteDAO(AEConnection aeConnection) throws AEException {
		return new QueteDAO(getConnection(aeConnection));
	}
	
	@Override
	public DocNumSequenceDAO getDocNumSequenceDAO(AEConnection aeConnection) throws AEException {
		return new DocNumSequenceDAO(getConnection(aeConnection));
	}
}
