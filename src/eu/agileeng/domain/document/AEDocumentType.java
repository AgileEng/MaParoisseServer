/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 22.05.2010 10:28:41
 */
package eu.agileeng.domain.document;

import java.util.HashMap;
import java.util.Map;

import eu.agileeng.domain.DomainModel;
import eu.agileeng.domain.EnumeratedType;

/**
 *
 */
@SuppressWarnings("serial")
public class AEDocumentType extends EnumeratedType {
	
	private static Map<System, AEDocumentType> docTypes = 
		new HashMap<System, AEDocumentType>(System.values().length); 
	static {
		docTypes.put(System.NA, new AEDocumentType(System.NA));
		docTypes.put(System.IdentityCard, new AEDocumentType(System.IdentityCard));
		docTypes.put(System.ResidenceCardPerm, new AEDocumentType(System.ResidenceCardPerm));
		docTypes.put(System.ResidenceCardTmp, new AEDocumentType(System.ResidenceCardTmp));
		docTypes.put(System.ContractDeTravail, new AEDocumentType(System.ContractDeTravail));
		docTypes.put(System.ContractDeTravailAnex, new AEDocumentType(System.ContractDeTravailAnex));
		docTypes.put(System.AEPurchaseInvoice, new AEDocumentType(System.AEPurchaseInvoice));
		docTypes.put(System.AEPurchaseInvoiceFNP, new AEDocumentType(System.AEPurchaseInvoiceFNP));
		docTypes.put(System.AESaleInvoice, new AEDocumentType(System.AESaleInvoice));
		docTypes.put(System.AEVirement, new AEDocumentType(System.AEVirement));
		docTypes.put(System.AEPrelevement, new AEDocumentType(System.AEPrelevement));
		docTypes.put(System.AECheque, new AEDocumentType(System.AECheque));
		docTypes.put(System.AELCR, new AEDocumentType(System.AELCR));
		docTypes.put(System.CFC, new AEDocumentType(System.CFC));
		docTypes.put(System.MANDAT, new AEDocumentType(System.MANDAT));
		docTypes.put(System.PERIMES, new AEDocumentType(System.PERIMES));
		docTypes.put(System.STOCKS, new AEDocumentType(System.STOCKS));
		docTypes.put(System.SUIVI_GPL, new AEDocumentType(System.SUIVI_GPL));
		docTypes.put(System.DONNEES, new AEDocumentType(System.DONNEES));
		docTypes.put(System.IDE, new AEDocumentType(System.IDE));
		docTypes.put(System.AccidentDuTravail, new AEDocumentType(System.AccidentDuTravail));
		docTypes.put(System.ArretDeTravail, new AEDocumentType(System.ArretDeTravail));
		docTypes.put(System.FinDuTravail, new AEDocumentType(System.FinDuTravail));
		docTypes.put(System.CertificatDeTravail, new AEDocumentType(System.CertificatDeTravail));
		docTypes.put(System.Rib, new AEDocumentType(System.Rib));
		
		docTypes.put(System.AEDevisSale, new AEDocumentType(System.AEDevisSale));
		docTypes.put(System.AEFactureSale, new AEDocumentType(System.AEFactureSale));
		
		docTypes.put(System.FinancialTransaction, new AEDocumentType(System.FinancialTransaction));
		docTypes.put(System.BordereauParoisse, new AEDocumentType(System.BordereauParoisse));
		docTypes.put(System.Cerfa_11580_03, new AEDocumentType(System.Cerfa_11580_03));
		docTypes.put(System.ETAT_ANNUEL_DES_COMPTES, new AEDocumentType(System.ETAT_ANNUEL_DES_COMPTES));
		docTypes.put(System.ETAT_ANNUEL_DES_COMPTES_DRAFT, new AEDocumentType(System.ETAT_ANNUEL_DES_COMPTES_DRAFT));
	}
	
	static public enum System {
		NA(0L),
		IdentityCard(2L),
		ResidenceCardPerm(3L),
		ResidenceCardTmp(4L),
		AEPurchaseInvoice(100L),
		AEPurchaseInvoiceFNP(110L),
		AESaleInvoice(200L),
		AEVirement(510L),
		AEPrelevement(520L),
		AECheque(530L),
		AELCR(540),
		CFC(600),
		MANDAT(700),
		PERIMES(800),
		STOCKS(810),
		SUIVI_GPL(820),
		DONNEES(830),
		IDE(950),
		SocialDocumet(1000), // (1000, 1100), step 10
		ContractDeTravail(1010),
		ContractDeTravailAnex(1015),
		AccidentDuTravail(1020),
		ArretDeTravail(1030),
		CertificatDeTravail(1040),
		FinDuTravail(1050),
		Rib(1060),
		
		// Sale 1100 - 1199
		AEDevisSale(1100L),   // Quote
		AEFactureSale(1105L), // Invoice
		
		FinancialTransaction(1200), 
		BordereauParoisse(12010),
		Cerfa_11580_03(12015),
		ETAT_ANNUEL_DES_COMPTES(12020),
		ETAT_ANNUEL_DES_COMPTES_DRAFT(12030);
				
		private long systemID;
		
		private System(long systemID) {
			this.systemID = systemID;
		}
		
		public final long getSystemID() {
			return this.systemID;
		}
		
		public static System valueOf(long systemID) {
			System ret = null;
			for (System inst : System.values()) {
				if(inst.getSystemID() == systemID) {
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
	
	private AEDocumentType(AEDocumentType.System inst) {
		super(DomainModel.DomainClass.DocumentType, inst.getSystemID());
	}
	
	public static AEDocumentType valueOf(AEDocumentType.System inst) {
		AEDocumentType docType = docTypes.get(inst);
		if(docType == null) {
			docType = valueOf(System.NA);
		}
		return docType;
	}
	
	public static AEDocumentType valueOf(long systemID) {
		return valueOf(System.valueOf(systemID));
	}
	
	public static boolean isBankdocument(AEDocumentType docType) {
		boolean bRet = false;
		if(docType != null) {
			if(docType.getSystemID() >= System.AEVirement.getSystemID() 
					&&docType.getSystemID() <= System.AELCR.getSystemID()) {
				
				bRet = true;
			}
		}
		return bRet;
	}
	
	public static boolean isPurchaseInvoice(AEDocumentType docType) {
		boolean bRet = false;
		if(docType != null) {
			if(docType.getSystemID() == System.AEPurchaseInvoice.getSystemID()) {
				bRet = true;
			}
		}
		return bRet;
	}
	
	public static boolean isSaleInvoice(AEDocumentType docType) {
		boolean bRet = false;
		if(docType != null) {
			if(docType.getSystemID() == System.AESaleInvoice.getSystemID()) {
				bRet = true;
			}
		}
		return bRet;
	}
	
	public boolean isOfType(AEDocumentType.System system) {
		boolean bRet = false;
		if(system != null) {
			if(getSystemID() == system.getSystemID()) {
				bRet = true;
			}
		}
		return bRet;
	}
}
