package eu.agileeng.services.facturation;

import eu.agileeng.domain.AEException;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.AEService;

public interface AEFacturationService extends AEService {
	public AEResponse saveArticle(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse loadArticles(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse deleteArticle(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse saveClient(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse loadClients(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse deleteClient(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse loadFacturesInfo(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse saveFacture(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse loadFactures(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse loadFacture(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse deleteFacture(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse duplicateFacture(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse calculateFacture(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse loadByFilter(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse loadPaymentsByFilter(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse devisTransitionToAccepted(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse devisTransitionToInvoiced(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse factureTransitionToValidated(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse payPayment(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse invoicePayment(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse unpaidPayment(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse loadPrintInfo(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse createCreditNote(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse savePrintTemplate(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse loadPrintTemplates(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse deletePrintTemplate(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse loadRelevantPrintTemplates(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse loadTableauDeBord(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse savePaymentTerm(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse loadPymentTermTemplates(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse loadPaymentTermTemplate(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse deletePaymentTermTemplate(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
}
