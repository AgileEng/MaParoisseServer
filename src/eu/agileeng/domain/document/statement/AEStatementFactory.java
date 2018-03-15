package eu.agileeng.domain.document.statement;

import eu.agileeng.domain.document.AEDocument;
import eu.agileeng.domain.document.AEDocumentDescriptor;
import eu.agileeng.domain.document.AEDocumentFactory;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.imp.AEDocumentDescriptorImp;
import eu.agileeng.domain.document.trade.AETradeDocSaveValidator;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.util.AEValidator;

public class AEStatementFactory extends AEDocumentFactory {

	private static AEStatementFactory inst = new AEStatementFactory();
	
	/**
	 * 
	 */
	private AEStatementFactory() {
	}

	public static final AEStatementFactory getInstance() {
		return inst;
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.domain.document.AEDocumentFactory#createDocument()
	 */
	@Override
	@Deprecated
	public AEDocument createDocument() {
		throw new IllegalStateException();
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
		return new AEStatement(docType);
	}
}
