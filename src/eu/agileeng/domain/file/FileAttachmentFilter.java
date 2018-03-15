/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 14.06.2010 15:00:02
 */
package eu.agileeng.domain.file;

import java.util.ArrayList;
import java.util.List;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.services.AEBean;
import eu.agileeng.services.AEService;

/**
 *
 */
@SuppressWarnings("serial")
public class FileAttachmentFilter extends AEBean implements AEService {

	static public enum Folder {
		all,
		inbox,
		sent,
	}
	
	private AuthPrincipal authSubject;
	private AEDescriptive tenant;
	private FileAttachmentFilter.Folder folder = Folder.all;
	private List<AEDocumentType> docTypes = new ArrayList<>();
	
	/**
	 * 
	 */
	public FileAttachmentFilter() {
	}
	
	/**
	 * 
	 */
	public FileAttachmentFilter(FileAttachmentFilter.Folder folder) {
		this.folder = folder;
	}

	/**
	 * @return the folder
	 */
	public FileAttachmentFilter.Folder getFolder() {
		return folder;
	}

	/**
	 * @param folder the folder to set
	 */
	public void setFolder(FileAttachmentFilter.Folder folder) {
		this.folder = folder;
	}

	/**
	 * @param folder the folder to set
	 */
	public FileAttachmentFilter withFolder(FileAttachmentFilter.Folder folder) {
		this.folder = folder;
		return this;
	}
	
	/**
	 * @return the authSubject
	 */
	public AuthPrincipal getAuthSubject() {
		return authSubject;
	}

	/**
	 * @param authSubject the authSubject to set
	 */
	public void setAuthSubject(AuthPrincipal authSubject) {
		this.authSubject = authSubject;
	}

	/**
	 * @return the tenant
	 */
	public AEDescriptive getTenant() {
		return tenant;
	}

	/**
	 * @param tenant the tenant to set
	 */
	public void setTenant(AEDescriptive tenant) {
		this.tenant = tenant;
	}
	
	/**
	 * @param tenant the tenant to set
	 */
	public FileAttachmentFilter withTenant(AEDescriptive tenant) {
		this.tenant = tenant;
		return this;
	}

	/**
	 * @return the docType
	 */
	public AEDocumentType getDocType() {
		AEDocumentType theDocType = null;
		if(docTypes != null && docTypes.size() == 1) {
			theDocType = docTypes.get(0);
		}
		return theDocType;
	}

	/**
	 * @param docType the docType to set
	 */
	public void setDocTypes(List<AEDocumentType> docTypes) {
		if(docTypes != null) {
			this.docTypes = docTypes;
		}
	}
	
	/**
	 * @param docType the docType to set
	 */
	public FileAttachmentFilter withDocType(AEDocumentType docType) {
		if(docType != null) {
			this.docTypes.add(docType);
		}
		return this;
	}

	/**
	 * @return the docTypes
	 */
	public List<AEDocumentType> getDocTypes() {
		return docTypes;
	}
}
