/**
 * 
 */
package eu.agileeng.domain.acc.export;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.acc.AccJournalItem;
import eu.agileeng.domain.acc.AccJournalItemsList;
import eu.agileeng.services.AEInvocationContext;

/**
 * @author vvatov
 *
 */
public abstract class ExportJob {

	protected AEInvocationContext context;
	
	protected ExportRequest request;
	
	protected BufferedWriter writer;
	
	protected SimpleDateFormat dateFormat = new SimpleDateFormat(); 

	protected DecimalFormat amountFormat = new DecimalFormat();
	
	/**
	 * 
	 */
	protected ExportJob(AEInvocationContext context, ExportRequest request) {
		this.context = context;
		this.request = request;
		
		// initialize amount format by default
		DecimalFormatSymbols ds = new DecimalFormatSymbols();
		ds.setDecimalSeparator(',');
		amountFormat.setDecimalFormatSymbols(ds);
		amountFormat.applyPattern("#0.00");
	}

	public AEInvocationContext getContext() {
		return context;
	}

	public ExportRequest getRequest() {
		return request;
	}

	/**
	 * Template method
	 * 
	 * @throws AEException
	 */
	public void run() throws AEException {
		try { 
			// prepare destination out stream
			FileOutputStream fos = new FileOutputStream(getRequest().getDestinationFile()); 
			writer = new BufferedWriter(new OutputStreamWriter(fos, getRequest().getCharset()), 2048);
			
			// let every job write its own header
			writeHeader();
			
			// let every job create its own line and write it
			AccJournalItemsList accJournalItems = getRequest().getAccJournalItemsList();
			for (AccJournalItem accJournalItem : accJournalItems) {
				writer.write(createLine(accJournalItem, getRequest()));
				writeLineSeparator();
			}
		} catch (Exception e) {
			throw new AEException();
		} finally {
			try {
				if(writer != null) {
					writer.flush();
					writer.close();
					writer = null;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public abstract String createLine(AccJournalItem accJournalItem, ExportRequest request);
	
	public BufferedWriter getWriter() {
		return writer;
	}

	public SimpleDateFormat getDateFormat() {
		return dateFormat;
	}

	public DecimalFormat getAmountFormat() {
		return amountFormat;
	}

	/**
	 * Writes <code>System.getProperty("line.separator")</code> as default
	 * line separator.
	 * <br>Can be overriden.
	 * 
	 * @throws IOException
	 */
	protected void writeLineSeparator() throws IOException {
		if(writer != null) {
			writer.write(System.getProperty("line.separator"));
		}
	}
	
	protected void writeHeader() throws IOException {
		// empty by default
	}
	
	abstract public void onJobFinished();
}
