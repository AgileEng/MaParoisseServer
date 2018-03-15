/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 24.05.2010 21:15:50
 */
package eu.agileeng.domain;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
@SuppressWarnings("serial")
public class AEError extends EnumeratedType {
	
	private static Map<AEError.System, AEError> errors = 
		new HashMap<AEError.System, AEError>(System.values().length); 
	static {
		AEError.System[] arrSysErrors = AEError.System.values();
		for (int i = 0; i < arrSysErrors.length; i++) {
			AEError.System sysError = arrSysErrors[i];
			errors.put(sysError, new AEError(sysError));
		}
	}
	
	static public enum System {
		NA(0L),
		INVALID_SESSION(
				1L,
				"Temps de connexion dépassé, veuillez vous identifier. "),
		OWNER_ID_NOT_SPECIFIED(2L),
		SECURITY_VIOLATION(
				3L,
				"Violation des règles de sécurité. "),
		INVALID_PARAMETER(
				4L,
				"Argument inapproprié. "),
		INVALID_CREDENTIALS(
				5L,
				"Connexion impossible. Nom d’utilisateur ou mot de passe erroné."),
		INVALID_REQUEST(
				6L,
				"Demande inadéquate contraire aux règles de sécurité. "),
		SYSTEM_ERROR(
				7L,
				"Erreur système 7"),
		VAT_CALCULATION_ERROR(100L),
		UNSUFFICIENT_RIGHTS(
			101L,
			"Vous n'avez pas les droits suffisants pour effectuer cette opération"),
		PRINCIPAL_NAME_NOT_UNIQUE(
			102L,
			"Ce nom de connexion existe déjà, utilisez un autre nom."),
		TENANT_NOT_ACTIVE(
			103L,
			"This operation is forbidden! The tenant is not active."),
		CANNOT_CLOSE_UNFINISHED(105L),
		OVERLAPPING_PERIODS(
				106L,
				"Périodes de chevauchement. "),
		UNKNOWN_EXPORT_IDENTIFICATOR(110L),
		INCORRECT_ACC_PERIOD(
				114L,
				"La date de saisie ne correspond pas à un exercice ouvert. "),
		CANNOT_EXPORT_NOT_CLOSED_PERIOD(115L),
		CANNOT_DELETE_FROM_CLOSED_PERIOD(
				116L,
				"La saisie sur une période clôturée est impossible. "),
		INVOICE_THERE_IS_NO_PARTY(117L),
		INVOICE_THERE_IS_NO_PARTY_ACCOUNT(118L),
		CANNOT_INSERT_UPDATE_CLOSED_PERIOD(
				119L,
				"La saisie sur une période clôturée est impossible. "),
		DOC_DATE_IS_MANDATORY(120L),
		PARTY_OR_ACCOUNT_MANDATORY(121L),
		PARTY_AND_ACCOUNT_BOTH(122L),
		ACC_JOURAL_ETRY_NOT_ALIGED(123L),
		ACC_PERIOD_WAS_NOT_FOUD(124L),
		DOCUMENT_IS_LOCKED(
				125L, 
				"Le document est verrouillé et ne peut pas être modifier"),
		
		// Jcr [200, 300)
		JCR_MODULE_ID_NOT_SPECIFIED(200L),
		JCR_PATH_NOT_FOUND(201L),
		JCR_PATH_IS_NOT_NODE(202L),
		JCR_NODE_TYPE_MISSING(202L),
		JCR_CONTENT_FILE_MISSING(203L),
		JCR_NODE_DUPLICATION(204L),
		JCR_NODE_PATH_MISSING(205L),
		JCR_NODE_FULL_TEXT_SEARCH_EXPRESSION_MISSING(206L),
		
		// Social [300, 400)
		SOCIAL_SALARY_GRID_NOT_DEFINED(
				300L, 
				"Attention, aucune grille de salaire n’est définie, rapprochez vous de votre comptable"),
		SOCIAL_SALARY_GRID_ITEM_NOT_UNIQ(
				301L, 
				"L'ordre du jour grille des salaires ne peut pas être identifié de façon unique"),
		SOCIAL_SALARY_HOURS_PER_WEEK_MISSING(
				302L,
				"Vous devez renseigner le champ 'Nombre d'heures (par semaine)'"),
		SOCIAL_INVALID_UIN(
				303L,
				"Invalide Numéro de sécurité sociale"),
		SOCIAL_THE_ACTUAL_NOT_FOUND(
				304L, 
				"The last contract/anex cannot be found"),
		SOCIAL_SETTINGS_MISSING(
				305L, 
				"Le module social n'est pas configuré."),
		SOCIAL_INVALID_SCHEDULE_PERIOD(
				306L, 
				"Période invalide."),
		SOCIAL_INVALID_TEMPLATE_START_DATE(
				307L, 
				"Invalid template start date."),
		SOCIAL_INVALID_PERIOD(
				308L, 
				"Période incohérente."),
				
		SOCIAL_WARNING_350(
				350L, 
				"Rappel à la loi non exhaustif maximum journalier autorisé dépassé."),
		SOCIAL_WARNING_351(
				351L, 
				"Rappel à la loi non exhaustif maximum hebdomadaire autorisé dépassé."),
		SOCIAL_WARNING_352(
				352L, 
				"Rappel à la loi non exhaustif temps de repos quotidien non respecté."),	
		SOCIAL_WARNING_ACTIVE_CONTRACT_EXIST(
				353L, 
				"Attention Il ya un autre contrat actif pour cet employé"),	
		SOCIAL_WARNING_CDD_CONTRACT_DELAY(
				354L, 
				"Attention veillez au respect des délais de carence entre deux CDD consécutifs"),		
		SOCIAL_WARNING_355(
				355L, 
				"Les heures de travail sont differentes de celles qui etaient prevues."),
				
		FACTURATION_CLIENT_MANDATORY(
				401L, 
				"Le client est obligatoire!"),	
		FACTURATION_PAYMENT_TERMS_MANDATORY(
				402L, 
				"Mode de règlement est obligatoire!"),			
		FACTURATION_TRANSITION_ERROR(
				403L, 
				"La transformation n'est pas possible!"),	
		FACTURATION_PAYMENT_PAY_ERROR(
				404L, 
				"Versement n'est pas possible!"),	
		FACTURATION_VALIDATED_PAID_FINAL_FACTURE_CANNOT_BE_CHANGED(
				405L, 
				"La facture d'acompte associée à cet acompte n'est pas la dernière facture générée, imposible de supprimer"),
		FACTURATION_INVOICED_DEVIS_CANNOT_BE_DELETED (
				406L, 
				"Le devis est déjà facturé et ne peut pas être supprimé"),
		FACTURATION_CANOT_BE_DUPLICATED (
				407L, 
				"Advance invoice cannot be duplicated"),
		FACTURATION_CANOT_CREATE_CREDIT_NOTE_OVER_DEVIS (
				408L, 
				"Cannot create credit note over devis"),
		BANK_ETEBAC_BUNDLE_OPENING_BALANCE_DOESNT_MATCH (
				500L, 
				"Attention: [%s] [%s] [%s] Le solde initial du fichier que l'on tente de remonter (%f) ne correspond pas au solde final de la dernière remontée (%f). "
				+ "Il a des fichiers manquants. Vérifiez sur le site Jedeclare.com."),
		BANK_ETEBAC_BUNDLE_IN_CLOSED_PERIOD (
				510L, 
				"ERROR [%s] [%s] [%s] Consider a period that is closed"),
				
		// Contributor [600, 700)
		CONTRIBUTOR_NOT_UNIQUE(
				600L, 
				"Un donateur de même nom prénom et adresse existe déjà !"),
		CONTRIBUTOR_IN_USE(
				601L, 
				"Le donateur est en cours d'utilisation et ne peut pas être supprimé !"),
				
		FT_REASON_MISSING(100000L);
				
		private long systemID;
		
		private String msg;
		
		private System(long systemID) {
			this.systemID = systemID;
		}
		
		private System(long systemID, String msg) {
			this.systemID = systemID;
			this.msg = msg;
		}
		
		public final long getSystemID() {
			return this.systemID;
		}
		
		public final String getMessage() {
			return this.msg;
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
		
		public AEException toException() {
			return new AEException(getSystemID(), getMessage());
		}
		
		public AEWarning toWarning() {
			return new AEWarning(getSystemID(), getMessage());
		}
	}
	
	private AEError(AEError.System error) {
		super(DomainModel.DomainClass.ERROR, error.getSystemID());
	}
	
	public static AEError valueOf(AEError.System error) {
		AEError err = errors.get(error);
		if(err == null) {
			err = valueOf(System.NA);
		}
		return err;
	}
	
	public static AEError valueOf(long errorID) {
		return valueOf(System.valueOf(errorID));
	}
}
