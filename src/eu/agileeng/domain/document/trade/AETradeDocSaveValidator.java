package eu.agileeng.domain.document.trade;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.util.AEValidator;

public class AETradeDocSaveValidator implements AEValidator {
	private static AETradeDocSaveValidator inst = new AETradeDocSaveValidator();
	
	/**
	 * 
	 */
	private AETradeDocSaveValidator() {
	}

	public static AETradeDocSaveValidator getInstance() {
		return inst;
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.util.AEValidator#validate(java.lang.Object)
	 */
	@Override
	public void validate(Object o) throws AEException {
		if(!(o instanceof AETradeDocument)) {
			return;
		}
		
		AETradeDocument tDoc = (AETradeDocument) o;

		// validate VAT
		if(Math.abs(tDoc.getAmount() - tDoc.getTaxableAmount() - tDoc.getVatAmount()) > 0.001) {
			if(tDoc.getType().equals(AEDocumentType.valueOf(AEDocumentType.System.AECheque))) {
				throw new AEException(
						(int)AEError.System.VAT_CALCULATION_ERROR.getSystemID(), 
						"Le montant global de votre \"Chèque\" n\'est pas égal à la somme des détails.");
			} else if (tDoc.getType().equals(AEDocumentType.valueOf(AEDocumentType.System.AEPrelevement))) {
				throw new AEException(
						(int)AEError.System.VAT_CALCULATION_ERROR.getSystemID(), 
						"Le montant global de votre \"Prélèvement\" n\'est pas égal à la somme des détails.");
			} else if (tDoc.getType().equals(AEDocumentType.valueOf(AEDocumentType.System.AEVirement))) {
				throw new AEException(
						(int)AEError.System.VAT_CALCULATION_ERROR.getSystemID(), 
						"Le montant global de votre \"Virement\" n\'est pas égal à la somme des détails.");
			} else if (tDoc.getType().equals(AEDocumentType.valueOf(AEDocumentType.System.AELCR))) {
				throw new AEException(
						(int)AEError.System.VAT_CALCULATION_ERROR.getSystemID(), 
						"Le montant global de votre \"LCR\" n\'est pas égal à la somme des détails.");
			} else {
				throw new AEException(
						(int)AEError.System.VAT_CALCULATION_ERROR.getSystemID(), 
						"Attention enregistrement impossible, le total HT + TVA n’est pas égal au TTC!");
			}
		}

		// validate date
		if(tDoc.getDate() == null && !tDoc.isTemplate()) {
			throw new AEException(
					(int) AEError.System.DOC_DATE_IS_MANDATORY.getSystemID(), 
					"La date de référence est obligatoire!");
		}

		// validate the pair (party, account)
		if(tDoc.isTemplate() 
				&& (AEDocumentType.valueOf(AEDocumentType.System.AEPurchaseInvoice).equals(tDoc.getType())
						|| AEDocumentType.valueOf(AEDocumentType.System.AEPurchaseInvoiceFNP).equals(tDoc.getType()))) {

			AEDescriptive party = tDoc.getIssuer();
			AEDescriptive account = tDoc.getIssuerAcc();
			if(party == null && account == null) {
				throw new AEException(
						(int) AEError.System.PARTY_OR_ACCOUNT_MANDATORY.getSystemID(), 
						"Sélectionnez un compte general, ou un compte auxilliaire!");
			}
		} else if(tDoc.isTemplate() 
				&& AEDocumentType.valueOf(AEDocumentType.System.AESaleInvoice).equals(tDoc.getType())) {
			
			AEDescriptive party = tDoc.getRecipient();
			AEDescriptive account = tDoc.getRecipientAcc();
			if(party == null && account == null) {
				throw new AEException(
						(int) AEError.System.PARTY_OR_ACCOUNT_MANDATORY.getSystemID(), 
						"Sélectionnez un compte general, ou un compte auxilliaire!");
			}
		}
	}
}
