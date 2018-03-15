/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 22.05.2010 11:26:45
 */
package eu.agileeng.domain.document.social.findutravail;

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
public class FinDuTravailFactory extends AEDocumentFactory {

	private static FinDuTravailFactory inst = new FinDuTravailFactory();
	
	/**
	 * 
	 */
	private FinDuTravailFactory() {
	}

	public static final FinDuTravailFactory getInstance() {
		return inst;
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.domain.document.AEDocumentFactory#createDocument()
	 */
	@Override
	public AEDocument createDocument() {
		return new FinDuTravail();
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.document.AEDocumentFactory#createdocumentDescriptor()
	 */
	@Override
	public AEDocumentDescriptor createDocumentDescriptor(AEDocumentType docType) {
		return new AEDocumentDescriptorImp(
					AEPersistentUtil.NEW_ID, 
					AEDocumentType.valueOf(AEDocumentType.System.FinDuTravail));
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.document.AEDocumentFactory#getSaveValidator()
	 */
	@Override
	public AEValidator getSaveValidator() {
		return FinDuTravailSaveValidator.getInstance();
	}

	@Override
	public AEDocument createDocument(AEDocumentType docType) {
		return createDocument();
	}
	
	@Override
	public String getPrintViewURL(AEDocumentType docType) {
		String printViewURL = super.getPrintViewURL(docType);
		AEDocumentType.System inst = AEDocumentType.System.valueOf(docType.getSystemID());
		switch(inst) {
			case FinDuTravail:
				printViewURL = "/server/jsp/FinDuTravailPrint.jsp";
				break;
			case CertificatDeTravail:
				printViewURL = "/server/jsp/CertificatDeTravailPrint.jsp";
				break;
			default:
				break;
		}
		return printViewURL;
	}
}
