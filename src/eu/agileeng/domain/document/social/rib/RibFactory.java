package eu.agileeng.domain.document.social.rib;

import eu.agileeng.domain.document.AEDocument;
import eu.agileeng.domain.document.AEDocumentDescriptor;
import eu.agileeng.domain.document.AEDocumentFactory;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.imp.AEDocumentDescriptorImp;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.util.AEValidator;

public class RibFactory extends AEDocumentFactory {

	private static RibFactory inst = new RibFactory();
	
	/**
	 * 
	 */
	private RibFactory() {
	}

	public static final RibFactory getInstance() {
		return inst;
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.domain.document.AEDocumentFactory#createDocument()
	 */
	@Override
	public AEDocument createDocument() {
		return new Rib();
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.document.AEDocumentFactory#createdocumentDescriptor()
	 */
	@Override
	public AEDocumentDescriptor createDocumentDescriptor(AEDocumentType docType) {
		return new AEDocumentDescriptorImp(
					AEPersistentUtil.NEW_ID, 
					AEDocumentType.valueOf(AEDocumentType.System.Rib));
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.domain.document.AEDocumentFactory#getSaveValidator()
	 */
	@Override
	public AEValidator getSaveValidator() {
		return RibSaveValidator.getInstance();
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
			case Rib:
				printViewURL = "/server/jsp/RibPrint.jsp";
				break;
			default:
				break;
		}
		return printViewURL;
	}
}
