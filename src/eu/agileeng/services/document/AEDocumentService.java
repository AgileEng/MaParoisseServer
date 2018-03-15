/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 22.05.2010 12:48:40
 */
package eu.agileeng.services.document;

import java.io.File;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.document.AEDocument;
import eu.agileeng.domain.document.AEDocumentFilter;
import eu.agileeng.domain.document.AEDocumentsList;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.AEService;

/**
 *
 */
public interface AEDocumentService extends AEService {
	public AEDocument save(AEDocument aeDocument, AEInvocationContext invContext) throws AEException;
	public AEDocument load(AEDescriptor docDescr, AEInvocationContext invContext) throws AEException;
	public AEDocumentsList load(AEDocumentFilter docFilter, AEInvocationContext invContext) throws AEException;
	
	public AEResponse save(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse saveDocuments(AERequest aeRequest) throws AEException;
	public AEResponse loadSupplyTemplate(AERequest aeRequest) throws AEException;
	public AEResponse loadByFilter(AERequest aeRequest) throws AEException;
	
	public AEResponse closePurchasePeriod(AERequest aeRequest) throws AEException;
	public AEResponse openPurchasePeriod(AERequest aeRequest) throws AEException;
	
	public AEResponse closeSalePeriod(AERequest aeRequest) throws AEException;
	public AEResponse openSalePeriod(AERequest aeRequest) throws AEException;
	
	public AEResponse delete(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse importInvoiceGuides(File impFile, AEInvocationContext invContext) throws AEException;
	
	public AEResponse loadSOT(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEDocumentsList loadDocumentsFull(AEDocumentFilter docFilter, AEInvocationContext invContext) throws AEException;
	public AEResponse closeSOTPeriod(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse openSOTPeriod(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse recreateSOT(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse loadSOTExc(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse closeSOTExcPeriod(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse openSOTExcPeriod(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse recreateSOTExc(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse onValidated(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse onValidatedArray(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse updateToLocked(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	public AEResponse updateToNotLocked(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
}
