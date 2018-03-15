package eu.agileeng.domain.facturation;

import eu.agileeng.domain.document.AEDocument;
import eu.agileeng.domain.document.AEDocumentDescriptor;
import eu.agileeng.domain.document.AEDocumentFactory;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.imp.AEDocumentDescriptorImp;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.util.AEValidator;

public class AEFactureFactory extends AEDocumentFactory {

	private static AEFactureFactory inst = new AEFactureFactory();
	
	public static final AEFactureFactory getInstance() {
		return inst;
	}
	
	@Override
	public AEDocument createDocument() {
		return new AEFacture(AEDocumentType.valueOf(AEDocumentType.System.AEDevisSale));
	}

	@Override
	public AEDocument createDocument(AEDocumentType docType) {
		return new AEFacture(docType);
	}

	@Override
	public AEDocumentDescriptor createDocumentDescriptor(AEDocumentType docType) {
		return new AEDocumentDescriptorImp(AEPersistentUtil.NEW_ID, docType);
	}

	@Override
	public AEValidator getSaveValidator() {
		return AEFactureSaveValidator.getInstance();
	}
	
	@Override
	public String getPrintViewURL(AEDocumentType docType) {
		return "/server/Facturation/generalPrint.jsp";
	}
	
	public String getTableauDeBordURL() {
		return "/server/Facturation/tableau.jsp";
	}
}
