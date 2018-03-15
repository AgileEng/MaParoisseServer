package eu.agileeng.domain.document.trade;

import java.net.URLConnection;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.document.AEDocument;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.file.FileAttachment;
import eu.agileeng.util.AECollectionUtil;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEStringUtil;

public class AETradeDocumentResult extends AEDomainObject {

	private AETradeDocument tradeDocument = new AETradeDocument();
	
	private FileAttachment attachment = new FileAttachment();
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -715909510145713369L;
	
	public AETradeDocumentResult() {
		super(DomainClass.AETradeDocumentResult);
	}

	public AETradeDocument getTradeDocument() {
		return tradeDocument;
	}

	public void setTradeDocument(AETradeDocument tDoc) {
		this.tradeDocument = tDoc;
	}

	public FileAttachment getAttachment() {
		return attachment;
	}

	public void setAttachment(FileAttachment attachment) {
		this.attachment = attachment;
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();

		// document
		json.put("docType", this.getTradeDocument().getType().getSystemID());
		json.put("xType", this.getTradeDocument().getType().getSystemID());
		if(this.getTradeDocument().getDate() != null) {
			json.put("date", AEDateUtil.convertToString(
					this.getTradeDocument().getDate(), 
					AEDateUtil.SYSTEM_DATE_FORMAT));
		}
		json.put("number", this.getTradeDocument().getNumberString());
		if(this.getTradeDocument().getType() != null 
				&& (this.getTradeDocument().getType().getSystemID() == AEDocumentType.System.AEPurchaseInvoice.getSystemID()
						|| this.getTradeDocument().getType().getSystemID() == AEDocumentType.System.AEPurchaseInvoiceFNP.getSystemID())) {
			if(this.getTradeDocument().getIssuer() != null) {
				json.put("issuerId", this.getTradeDocument().getIssuer().getDescriptor().getID());
				json.put("companyName", this.getTradeDocument().getIssuer().getDescriptor().getName());
			}
		} else if(this.getTradeDocument().getType() != null 
				&& this.getTradeDocument().getType().getSystemID() == AEDocumentType.System.AESaleInvoice.getSystemID()) {
			if(this.getTradeDocument().getRecipient() != null) {
				json.put("recipientId", this.getTradeDocument().getRecipient().getDescriptor().getID());
				json.put("companyName", this.getTradeDocument().getRecipient().getDescriptor().getName());
			}
		}
		if(this.getTradeDocument().getPaymentDueDate() != null) {
			json.put(
					"payDueDate", 
					AEDateUtil.convertToString(
							this.getTradeDocument().getPaymentDueDate(), 
							AEDateUtil.SYSTEM_DATE_FORMAT));
		}
		json.put("description", this.getTradeDocument().getDescription());
		json.put("taxableAmount", this.getTradeDocument().getTaxableAmount());
		json.put("vatAmount", this.getTradeDocument().getVatAmount());
		json.put("amount", this.getTradeDocument().getAmount());
		json.put(AEDocument.JSONKey.validated.toString(), this.getTradeDocument().isValidated());
		if(!AEStringUtil.isEmpty(this.getTradeDocument().getValidatedBy())) {
			json.put(AEDocument.JSONKey.validatedBy.toString(), this.getTradeDocument().getValidatedBy());
		}
		if(this.getTradeDocument().getValidatedTime() != null) {
			json.put(AEDocument.JSONKey.validatedTime.toString(), 
					AEDateUtil.convertToString(
							this.getTradeDocument().getValidatedTime(), 
							AEDateUtil.SYSTEM_DATE_FORMAT));
		}
		
		if(this.getTradeDocument().getBankAcc() != null) {
			json.put("bankAccId", this.getTradeDocument().getBankAcc().getDescriptor().getID());
			json.put("bankAccName", this.getTradeDocument().getBankAcc().getDescriptor().getName());
		}
		
		// items
		if(!AECollectionUtil.isEmpty(this.getTradeDocument().getItems())) {
			json.put("items", this.getTradeDocument().getItems().toJSONArray());
		}
		
		// attachment
		if(this.getAttachment().getName() != null) {
			json.put("fileLength", this.getAttachment().getFileLength());
			json.put("fileRemotePath", this.getAttachment().getRemoteRoot());
			json.put("fileName", this.getAttachment().getName());
			json.put("fileMimeType", URLConnection.guessContentTypeFromName(this.getAttachment().getName())); //No JCR for this one!
			StringBuffer sb = new StringBuffer("<a href=\"../../FileDownloadServlet?file=");
			sb
				.append(this.getAttachment().getRemoteRoot()).append("\\").append(this.getAttachment().getRemotePath())
				.append("&fileName=").append(this.getAttachment().getName()).append("\">")
				.append(this.getAttachment().getName()).append("</a>");
			json.put("attachmentLink", sb.toString());
			
			sb = new StringBuffer("../../../FileDownloadServlet?file=");
			sb
				.append(this.getAttachment().getRemoteRoot()).append("\\").append(this.getAttachment().getRemotePath())
				.append("&fileName=").append(this.getAttachment().getName());
			json.put("fileDownloadPath", sb.toString());
		}
		
		return json;
	}
}
