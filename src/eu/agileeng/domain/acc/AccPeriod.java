package eu.agileeng.domain.acc;

import java.util.Date;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.file.FileAttachment;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEStringUtil;

public class AccPeriod extends AEDomainObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8461764434566331776L;

	private long moduleId;
	
	private String moduleName;
	
	private Date startDate;
	
	private Date endDate;
	
	private boolean closed;
	
	private boolean exported;
	
	private FileAttachment fileAttachment;
	
	/**
	 * @param clazz
	 */
	protected AccPeriod(DomainClass clazz) {
		super(clazz);
	}

	public AccPeriod(Date startDate, Date endDate) {
		this();
		this.startDate = startDate;
		this.endDate = endDate;
	}
	
	public AccPeriod() {
		this(DomainClass.AccPeriod);
	}

	public long getModuleId() {
		return moduleId;
	}

	public void setModuleId(long moduleId) {
		this.moduleId = moduleId;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public boolean isClosed() {
		return closed;
	}

	public void setClosed(boolean closed) {
		this.closed = closed;
	}
	
	public static AEDescriptor lazyDescriptor(long id) {
		return new AEDescriptorImp(id, DomainClass.AccPeriod);
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject json = super.toJSONObject();

		json.put("moduleName", getModuleName());
		
		if(getStartDate() != null) {
			json.put("startDate", AEDateUtil.convertToString(getStartDate(), AEDateUtil.SYSTEM_DATE_FORMAT));
		} else {
			json.put("startDate", AEStringUtil.EMPTY_STRING);
		}
		
		if(getEndDate() != null) {
			json.put("endDate", AEDateUtil.convertToString(getEndDate(), AEDateUtil.SYSTEM_DATE_FORMAT));
		} else {
			json.put("endDate", AEStringUtil.EMPTY_STRING);
		}
		
		json.put("closed", isClosed() ? true : false);
		
		json.put("exported", isExported() ? true : false);
		
		if(getFileAttachment() != null && !AEStringUtil.isEmpty(getFileAttachment().getName())) {
			// attachment
			json.put("fileLength", this.getFileAttachment().getFileLength());
			json.put("fileRemotePath", this.getFileAttachment().getRemoteRoot());
			json.put("fileName", this.getFileAttachment().getName());
			StringBuffer sb = new StringBuffer("<a href=\"../../FileDownloadServlet?file=");
			sb
				.append(this.getFileAttachment().getRemoteRoot()).append("\\").append(this.getFileAttachment().getRemotePath())
				.append("&fileName=").append(this.getFileAttachment().getName()).append("\">")
				.append(this.getFileAttachment().getName()).append("</a>");
			json.put("attachmentLink", sb.toString());

			json.put("fileAttachment", getFileAttachment().toJSONObject());
		}
		
		return json;
	}
	
	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		super.create(jsonObject);
		
		// TODO
	}

	public boolean isExported() {
		return exported;
	}

	public void setExported(boolean exported) {
		this.exported = exported;
	}

	public FileAttachment getFileAttachment() {
		return fileAttachment;
	}
	
	public FileAttachment grantFileAttachment() {
		if(this.fileAttachment == null) {
			this.fileAttachment = new FileAttachment();
		}
		return this.fileAttachment;
	}

	public void setFileAttachment(FileAttachment fileAttachment) {
		this.fileAttachment = fileAttachment;
	}

	public String getModuleName() {
		return moduleName;
	}

	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}
}
