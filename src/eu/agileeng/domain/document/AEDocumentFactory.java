/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 22.05.2010 11:04:45
 */
package eu.agileeng.domain.document;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.document.social.accidentdutravail.AccidentDuTravailFactory;
import eu.agileeng.domain.document.social.contractdetravail.ContractDeTravailFactory;
import eu.agileeng.domain.document.social.findutravail.FinDuTravailFactory;
import eu.agileeng.domain.document.social.rib.RibFactory;
import eu.agileeng.domain.document.statement.AEStatementFactory;
import eu.agileeng.domain.document.trade.AETradeDocumentFactory;
import eu.agileeng.domain.facturation.AEFactureFactory;
import eu.agileeng.util.AEValidator;


/**
 *
 */
public abstract class AEDocumentFactory {

	/**
	 * 
	 */
	protected AEDocumentFactory() {
	}

	public static AEDocumentFactory getInstance(AEDocumentType documentType) throws AEException {
		AEDocumentFactory docFactory = null;
		AEDocumentType.System inst = AEDocumentType.System.valueOf(documentType.getSystemID());
		switch(inst) {
			case ContractDeTravail:
			case ContractDeTravailAnex:
				docFactory = ContractDeTravailFactory.getInstance();
				break;
			case AccidentDuTravail:
				docFactory = AccidentDuTravailFactory.getInstance();
				break;
			case ArretDeTravail:
				docFactory = AccidentDuTravailFactory.getInstance();
				break;
			case FinDuTravail:
				docFactory = FinDuTravailFactory.getInstance();
				break;
			case CertificatDeTravail:
				docFactory = FinDuTravailFactory.getInstance();
				break;
			case Rib:
				docFactory = RibFactory.getInstance();
				break;
			case AEPurchaseInvoice:
				docFactory = AETradeDocumentFactory.getInstance();
				break;
			case AEPurchaseInvoiceFNP:
				docFactory = AETradeDocumentFactory.getInstance();
				break;
			case AESaleInvoice:
				docFactory = AETradeDocumentFactory.getInstance();
				break;
			case AECheque:
				docFactory = AETradeDocumentFactory.getInstance();
				break;
			case AEVirement:
				docFactory = AETradeDocumentFactory.getInstance();
				break;
			case AEPrelevement:
				docFactory = AETradeDocumentFactory.getInstance();
				break;
			case AELCR:
				docFactory = AETradeDocumentFactory.getInstance();
				break;
			case PERIMES:
			case STOCKS:
				docFactory = AETradeDocumentFactory.getInstance();
				break;
			case SUIVI_GPL:
			case DONNEES:
				docFactory = AETradeDocumentFactory.getInstance();
				break;
			case AEDevisSale:
			case AEFactureSale:
				docFactory = AEFactureFactory.getInstance();
				break;
			case BordereauParoisse:
			case Cerfa_11580_03:
			case ETAT_ANNUEL_DES_COMPTES:
			case ETAT_ANNUEL_DES_COMPTES_DRAFT:
				docFactory = AEStatementFactory.getInstance();
				break;
			default:
				throw new AEException("InternalError: Undefined AEDocumentType");
		}
		return docFactory;
	}
	
	public abstract AEDocument createDocument();
	
	public abstract AEDocument createDocument(AEDocumentType docType);
	
	public AEDocument createDocument(JSONObject json) throws JSONException {
		AEDocument aeDoc = createDocument();
		aeDoc.create(json);
		return aeDoc;
	}
	
	public abstract AEDocumentDescriptor createDocumentDescriptor(AEDocumentType docType);
	
	public String getPrintViewURL(AEDocumentType docType) {
		return "/server/jsp/EmptyPrint.jsp";
	}
	
	public abstract AEValidator getSaveValidator();
}
