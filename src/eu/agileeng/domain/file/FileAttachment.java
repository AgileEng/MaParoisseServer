/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 07.06.2010 21:46:40
 */
package eu.agileeng.domain.file;

import java.util.Date;

import org.apache.commons.io.FilenameUtils;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.common.AEComment;
import eu.agileeng.domain.common.AECommentsList;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.util.AEMimeTypes;
import eu.agileeng.util.AEStringUtil;

/**
 *
 */
@SuppressWarnings("serial")
public class FileAttachment extends AEDomainObject {
	
	static public enum JSONKey {
		fileAttachment,
		xType;
	}
	
	private AEDescriptive attachedTo; 	// Object to which the given file is attached
	private String localPath;           // 
	private long fileLength;            // The length attribute of the physical file in bytes
	private String fileType;            // The file type of the physical file: doc, pdf etc.
	private Date fileLastModified;      // The lastModified attribute of the physical file
	private int position;
	
	/**
	 * The path to the root directory of the remote storage.
	 * The root directory is the base directory to which 
	 * the remote storage is allowed access.
	 * Example: d:\MonBureau\FileStorage
	 */
	private String remoteRoot;
	
	/**
	 * The <code>relative</code> path to the physical file 
	 * toward <code>remoteRoot</code>.
	 * Example: Aossia\Invoices\2010\5
	 * 
	 * So the absolute path to the physical file on remote
	 * storage must be <code>remoteRoot + File.pathSeparator + remotepath</code>.
	 * 
	 */
	private String remotePath;
	
	/**
	 * The old relative path in case of file movement
	 */
	private String oldRemotePath;
	
	private AEDescriptive sender;     // the sender of the file: instance or AEDescriptor of AuthSubject
	
	private AEDescriptive recipient;  // the recipient of the file: instance or AEDescriptor of Person
	
	private AECommentsList comments;
	
	private boolean dirty;
	
	/**
	 * @param clazz
	 */
	public FileAttachment() {
		super(DomainClass.FileAttachment);
	}

	/**
	 * @return the attachedTo
	 */
	public AEDescriptive getAttachedTo() {
		return attachedTo;
	}

	/**
	 * @param attachedTo the attachedTo to set
	 */
	public void setAttachedTo(AEDescriptive attachedTo) {
		this.attachedTo = attachedTo;
	}

	/**
	 * @return the localPath
	 */
	public String getLocalPath() {
		return localPath;
	}

	/**
	 * @param localPath the localPath to set
	 */
	public void setLocalPath(String localPath) {
		this.localPath = localPath;
	}

	/**
	 * @return the fileLength
	 */
	public long getFileLength() {
		return fileLength;
	}

	/**
	 * @param fileLength the fileLength to set
	 */
	public void setFileLength(long fileLength) {
		this.fileLength = fileLength;
	}

	/**
	 * @return the fileType
	 */
	public String getFileType() {
		return fileType;
	}

	/**
	 * @param fileType the fileType to set
	 */
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	/**
	 * @return the fileLastModified
	 */
	public Date getFileLastModified() {
		return fileLastModified;
	}

	/**
	 * @param fileLastModified the fileLastModified to set
	 */
	public void setFileLastModified(Date fileLastModified) {
		this.fileLastModified = fileLastModified;
	}

	/**
	 * @return the position
	 */
	public int getPosition() {
		return position;
	}

	/**
	 * @param position the position to set
	 */
	public void setPosition(int position) {
		this.position = position;
	}

	/**
	 * @return the remoteRoot
	 */
	public String getRemoteRoot() {
		return remoteRoot;
	}

	/**
	 * @param remoteRoot the remoteRoot to set
	 */
	public void setRemoteRoot(String remoteRoot) {
		this.remoteRoot = remoteRoot;
	}

	/**
	 * @return the remotePath
	 */
	public String getRemotePath() {
		return remotePath;
	}

	/**
	 * @param remotePath the remotePath to set
	 */
	public void setRemotePath(String remotePath) {
		this.remotePath = remotePath;
	}

	/**
	 * @return the oldRemotePath
	 */
	public String getOldRemotePath() {
		return oldRemotePath;
	}

	/**
	 * @param oldRemotePath the oldRemotePath to set
	 */
	public void setOldRemotePath(String oldRemotePath) {
		this.oldRemotePath = oldRemotePath;
	}

	/**
	 * @return the sender
	 */
	public AEDescriptive getSender() {
		return sender;
	}

	/**
	 * @param sender the sender to set
	 */
	public void setSender(AEDescriptive sender) {
		this.sender = sender;
	}

	/**
	 * @return the recipient
	 */
	public AEDescriptive getRecipient() {
		return recipient;
	}

	/**
	 * @param recipient the recipient to set
	 */
	public void setRecipient(AEDescriptive recipient) {
		this.recipient = recipient;
	}

	/**
	 * @return the comments
	 */
	public AECommentsList getComments() {
		return comments;
	}

	/**
	 * @param comments the comments to set
	 */
	public void setComments(AECommentsList comments) {
		this.comments = comments;
	}
	
	public boolean addComment(AEComment comment) {
		boolean bRet = false;
		if(comment != null) {
			if(this.comments == null) {
				this.comments = new AECommentsList();
			}
			bRet = this.comments.add(comment);
		}
		return bRet;
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();

		json.put("length", getFileLength());
		json.put("remotePath", getRemoteRoot());
		json.put("remoteName", getRemotePath());
		if(getAttachedTo() != null) {
			json.put("toId", getAttachedTo().getDescriptor().getID());
			json.put("toType", getAttachedTo().getDescriptor().getClazz().getID());
		}
		if(!AEStringUtil.isEmpty(getRemoteRoot()) && !AEStringUtil.isEmpty(getRemotePath())) {
			StringBuffer sb = new StringBuffer("<a href=\"../../FileDownloadServlet?file=");
			sb
				.append(getRemoteRoot()).append("\\").append(getRemotePath())
				.append("&fileName=").append(getName()).append("\">")
				.append(getName()).append("</a>");
			json.put("attachmentLink", sb.toString());
		}
		json.put("isDirty", isDirty());
		
		return json;
	}
	
	public JSONObject toJSONObjectExt() throws JSONException {
		JSONObject json = super.toJSONObject();
		
		json.put(AEDomainObject.JSONKey.code.name(), AEMimeTypes.getMimeType(FilenameUtils.getExtension(getName())));

		json.put(JSONKey.xType.name(), JSONKey.fileAttachment.name());
		
		json.put(AEDomainObject.JSONKey.system.name(), false);
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		if(jsonObject.has("toId") && jsonObject.has("toType")) {
			AEDescriptorImp toDescr = new AEDescriptorImp(
					jsonObject.getLong("toId"),
					DomainClass.valueOf(jsonObject.getLong("toType")));
			setAttachedTo(toDescr);
		}
		
		if(jsonObject.has("length")) {
			setFileLength(jsonObject.optLong("length"));
		}

		if(jsonObject.has("remotePath")) {
			setRemoteRoot(jsonObject.optString("remotePath"));
		}
		
		if(jsonObject.has("remoteName")) {
			setRemotePath(jsonObject.optString("remoteName"));
		}
		
		if(jsonObject.has("isDirty")) {
			setDirty(jsonObject.getBoolean("isDirty"));
		}
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.FileAttachment);
	}
}
