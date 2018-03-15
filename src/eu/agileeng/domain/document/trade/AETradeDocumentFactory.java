package eu.agileeng.domain.document.trade;

import eu.agileeng.domain.document.AEDocument;
import eu.agileeng.domain.document.AEDocumentDescriptor;
import eu.agileeng.domain.document.AEDocumentFactory;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.imp.AEDocumentDescriptorImp;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.util.AEValidator;

public class AETradeDocumentFactory extends AEDocumentFactory {

	private static AETradeDocumentFactory inst = new AETradeDocumentFactory();
	
	/**
	 * 
	 */
	private AETradeDocumentFactory() {
	}

	public static final AETradeDocumentFactory getInstance() {
		return inst;
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.domain.document.AEDocumentFactory#createDocument()
	 */
	@Override
	public AEDocument createDocument() {
		return new AETradeDocument();
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
		return AETradeDocSaveValidator.getInstance();
	}

	@Override
	public AEDocument createDocument(AEDocumentType docType) {
		return new AETradeDocument(docType);
	}
}
