/**
 * 
 */
package eu.agileeng.domain.acc.export;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Date;

import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.acc.AccJournalItemsList;
import eu.agileeng.domain.imp.AEDescriptorImp;

/**
 * @author vvatov
 *
 */
public class ExportRequest implements Serializable {

	private Charset charset = Charset.forName("Windows-1252");
	
	private File destinationFile;
	
	private Date startDate;
	
	private Date endDate;
	
	private AEDescriptor appModule;
	
	private AccJournalItemsList accJournalItemsList; 
	
	private JSONObject customer;
	
	private JSONObject chartOfAccounts;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 66819857332746479L;

	/**
	 * 
	 */
	public ExportRequest(AccJournalItemsList accJournalItemsList, File destinationFile) {
		this.accJournalItemsList = accJournalItemsList;
		this.destinationFile = destinationFile;
	}
	
	public ExportRequest() {
	}

	public File getDestinationFile() {
		return destinationFile;
	}

	public void setDestinationFile(File destinationFile) {
		this.destinationFile = destinationFile;
	}

	public Charset getCharset() {
		return charset;
	}

	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	public AccJournalItemsList getAccJournalItemsList() {
		return accJournalItemsList;
	}

	public void setAccJournalItemsList(AccJournalItemsList accJournalItemsList) {
		this.accJournalItemsList = accJournalItemsList;
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

	public AEDescriptor getAppModule() {
		return appModule;
	}
	
	public AEDescriptor grantAppModule() {
		if(this.appModule == null) {
			this.appModule = new AEDescriptorImp();
		}
		return this.appModule;
	}

	public void setAppModule(AEDescriptor appModule) {
		this.appModule = appModule;
	}

	public JSONObject getCustomer() {
		return customer;
	}

	public void setCustomer(JSONObject customer) {
		this.customer = customer;
	}

	public JSONObject getChartOfAccounts() {
		return chartOfAccounts;
	}

	public void setChartOfAccounts(JSONObject chartOfAccounts) {
		this.chartOfAccounts = chartOfAccounts;
	}
}
