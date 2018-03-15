/**
 * 
 */
package eu.agileeng.domain.document.social.accidentdutravail;

import eu.agileeng.domain.document.AEDocument;
import eu.agileeng.domain.document.AEDocumentDescriptor;
import eu.agileeng.domain.document.AEDocumentFactory;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.imp.AEDocumentDescriptorImp;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.util.AEValidator;


/**
 * @author vvatov
 *
 */
public class AccidentDuTravailFactory extends AEDocumentFactory {
	private static AccidentDuTravailFactory inst = new AccidentDuTravailFactory();
	
	/**
	 * 
	 */
	private AccidentDuTravailFactory() {
	}

	public static final AccidentDuTravailFactory getInstance() {
		return inst;
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.domain.document.AEDocumentFactory#createDocument()
	 */
	@Override
	public AEDocument createDocument() {
		return new AccidentDuTravail();
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
		return AccidentDuTravailSaveValidator.getInstance();
	}

	@Override
	public AEDocument createDocument(AEDocumentType docType) {
		return new AccidentDuTravail(docType);
	}
	
	@Override
	public String getPrintViewURL(AEDocumentType docType) {
		String printViewURL = super.getPrintViewURL(docType);
		AEDocumentType.System inst = AEDocumentType.System.valueOf(docType.getSystemID());
		switch(inst) {
			case AccidentDuTravail:
				printViewURL = "/server/jsp/AccidentDuTravailPrint.jsp";
				break;
			case ArretDeTravail:
				printViewURL = "/server/jsp/ArretDeTravailPrint.jsp";
				break;
			default:
				break;
		}
		return printViewURL;
	}
}
