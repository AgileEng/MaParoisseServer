/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 18.06.2010 10:34:27
 */
package eu.agileeng.domain.document.social.contractdetravail;

import eu.agileeng.domain.document.AEDocument;
import eu.agileeng.domain.document.AEDocumentDescriptor;
import eu.agileeng.domain.document.AEDocumentFactory;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.imp.AEDocumentDescriptorImp;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.util.AEValidator;

/**
 *
 */
public class ContractDeTravailFactory extends AEDocumentFactory {
	
	private static ContractDeTravailFactory inst = new ContractDeTravailFactory();
	
	/**
	 * 
	 */
	private ContractDeTravailFactory() {
	}

	public static final ContractDeTravailFactory getInstance() {
		return inst;
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.domain.document.AEDocumentFactory#createDocument()
	 */
	@Override
	public AEDocument createDocument() {
		return new ContractDeTravail(AEDocumentType.valueOf(AEDocumentType.System.ContractDeTravail));
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.document.AEDocumentFactory#createdocumentDescriptor()
	 */
	@Override
	public AEDocumentDescriptor createDocumentDescriptor(AEDocumentType docType) {
		return new AEDocumentDescriptorImp(
					AEPersistentUtil.NEW_ID, 
					docType);
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.document.AEDocumentFactory#getSaveValidator()
	 */
	@Override
	public AEValidator getSaveValidator() {
		return ContractDeTravailSaveValidator.getInstance();
	}

	@Override
	public AEDocument createDocument(AEDocumentType docType) {
		return new ContractDeTravail(docType);
	}
	
	@Override
	public String getPrintViewURL(AEDocumentType docType) {
		String printViewURL = super.getPrintViewURL(docType);
		printViewURL = "/server/jsp/ContractDeTravailPrint.jsp";
		return printViewURL;
	}
}
