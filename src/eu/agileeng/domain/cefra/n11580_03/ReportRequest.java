package eu.agileeng.domain.cefra.n11580_03;

import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.DomainModel.DomainClass;

public abstract class ReportRequest extends AEDomainObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6200167160650249070L;

	public static enum ReportType {
		UNKNOWN,
	    DRAFT,
	    RELEASE;
	}
	
	protected ReportRequest(DomainClass clazz) {
		super(clazz);
	}
	
	private ReportRequest.ReportType reportType = ReportRequest.ReportType.UNKNOWN;

	/**
	 * @return the reportType
	 */
	public ReportRequest.ReportType getReportType() {
		return reportType;
	}

	/**
	 * @param reportType the reportType to set
	 */
	public void setReportType(ReportRequest.ReportType reportType) {
		this.reportType = reportType;
	}
	
	/**
	 * @param reportType the reportType to set
	 */
	public ReportRequest withReportType(ReportRequest.ReportType reportType) {
		setReportType(reportType);
		return this;
	}
}
