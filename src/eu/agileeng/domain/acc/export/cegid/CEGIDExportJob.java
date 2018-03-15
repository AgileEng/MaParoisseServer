/**
 * 
 */
package eu.agileeng.domain.acc.export.cegid;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.accbureau.AEApp;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.acc.AccJournalItem;
import eu.agileeng.domain.acc.export.ExportJob;
import eu.agileeng.domain.acc.export.ExportRequest;
import eu.agileeng.domain.document.AEDocumentDescriptor;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AETimePeriod;
import eu.agileeng.util.StringAlign;

/**
 * @author vvatov
 *
 */
public class CEGIDExportJob extends ExportJob {
	
	private static final ExecutorService executor = Executors.newFixedThreadPool(3);
	
//	private String traHeader = 
//		"***S5EXPJRLETE007010119002311200900840728231120091530AUVRAY                                                                -    40728   30121899001";

	private static final String S5 = 
		"***S5CLIJRLETE   0101190001011900006                                                                                           ";
	
	private static final String PS1 = 
		"***PS1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              ";
	
	private static final String 
		PS2 = "***PS2%s0%s0 60 50 50 50 50                                                                                                                                                                          ";
	
	private static final String PS3 = 
		"***PS3                                                                                                                                                                                   ";
	
	private static final String PS5 = 
		"***PS5EUR2X                                     1                           ";
	
	private static final String EXO = 
		"***EXO001%s%sOUV                                         ";
	
	private static final String ETB = 
		"***ETB%s%s";
	
//1	***S5CLIJRLETE   0101190001011900009
//2	***PS1
//3	***PS2100100 60 50 50 50 50
//4	***PS3
//5	***PS5EUR2X                                     1
//6	***EXO0010111201031102011OUV
//7	***ETB216Valcarb Avallon Avia
	
//	 - Keep line 1;2;4 and 5 as they are
//	 - Line 3 is ***PS2XX0YY0ZZ0 50 50 50 50 
//	   where XX is the length from the general account as defined in
//	     "parameters";"société";"plan comptable";Field "Généraux"
//	   YY  the length from the auxiliary account defined in the field "Auxilliaires"
//     ZZ length from "section analytique" I still need to check if we need that info from the user or not. In the
//        meantime we  may keep [space]6.
//  - Line 6: ***EXO remains the same. 
//	  Then 001 is the code for the period and depends from the date given afterward. 
//	  0111201031102011 are date of beginning and end from fiscal year 
//	  (that you have to ask for, or calculate ). 
//	  OUV means the fiscal year is opened.
//  - Line 7: ***ETB remains the 3 digits to give in the Establishments code 
//	  ( you will have to ask for) then the Establishments name.
//	
	public static enum Type {
		ACHATS,
		VENTES;
	}
	
	private class CopyFileTask implements Runnable {
		/**
		 * 
		 */
		private CopyFileTask() {
		}
		
		@Override
		public void run() {
			try {
				// let throw and log Exception if there is problem
				JSONObject customer = request.getCustomer();
				String importConnectionURL = customer.optString("importConnectionURL");
				// don't copy for auto import if connection URL is empty
				if(!AEStringUtil.isEmpty(importConnectionURL)) {
					String subFolder = "#" + importConnectionURL;
					org.apache.commons.io.FileUtils.copyFile(
							getRequest().getDestinationFile(),
							new File(
									"C:\\AccBureau\\cegid\\" + subFolder + "\\", 
									getRequest().getDestinationFile().getName()));
				}
			} catch (Throwable t) {
				AEApp.logger().error("Copy the file for auto import failed:", t);
			}
		}
	}
	
	/**
	 * @param context
	 * @param request
	 */
	public CEGIDExportJob(AEInvocationContext context, ExportRequest request) {
		super(context, request);
		
		// initialize date format by default
		dateFormat.applyPattern("ddMMyyyy");
	}

	/**
	 * Header at this time is hard coded
	 * @throws IOException
	 */
	protected void writeHeader() throws IOException {
		if(writer != null) {

			writer.write(S5);
			writer.write(System.getProperty("line.separator"));
			
			writer.write(PS1);
			writer.write(System.getProperty("line.separator"));
			
			int lengthG = 10;
			int lengthA = 10;
			JSONObject coa = request.getChartOfAccounts();
			if(coa != null) {
				lengthG = coa.optInt("lengthG");
				lengthA = coa.optInt("lengthA");
			}
			StringAlign strAlignPS2 = new StringAlign(2, StringAlign.Align.RIGHT);
			String ps2 = String.format(
					PS2, 
					strAlignPS2.format(Integer.toString(lengthG > 0 ? lengthG : 10)), 
					strAlignPS2.format(Integer.toString(lengthA > 0 ? lengthA : 10)));
			writer.write(ps2);
			writer.write(System.getProperty("line.separator"));
			
			writer.write(PS3);
			writer.write(System.getProperty("line.separator"));
			
			writer.write(PS5);
			writer.write(System.getProperty("line.separator"));
			
			AETimePeriod financialPeriod = null;
			JSONObject customer = request.getCustomer();
			if(customer != null) {
				try {
					Date startDate = AEDateUtil.parseDateStrict(customer.getString("finYearStartDate"));
					int  duration = customer.getInt("finYearDuration");
					financialPeriod = AEDateUtil.getFinancialFrenchYearForDate(
							startDate, 
							duration, 
							this.request.getStartDate());
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			if(financialPeriod == null) {
				financialPeriod = new AETimePeriod();
				financialPeriod.setStartDate(AEDateUtil.beginOfTheYear(this.request.getStartDate()));
				financialPeriod.setEndDate(AEDateUtil.endOfTheYear(this.request.getEndDate()));
			}
			String exo = String.format(
					EXO, 
					dateFormat.format(financialPeriod.getStartDate()), 
					dateFormat.format(financialPeriod.getEndDate()));
			writer.write(exo);
			writer.write(System.getProperty("line.separator"));
			
			StringAlign strAlign = new StringAlign(3, StringAlign.Align.LEFT);
			String etb = String.format(
					ETB, 
					strAlign.format(request.getCustomer().optString("code")),
					request.getCustomer().optString("name"));
			writer.write(etb);			
			writer.write(System.getProperty("line.separator"));
		}
	}
	
	@Override
	public String createLine(AccJournalItem accJournalItem, ExportRequest request) {
		StringBuilder traLineBuilder = new StringBuilder(512);
		
		// code journal
		append(traLineBuilder, accJournalItem.grantJournal().getCode(), 3);
		
		// date de mouvement
		append(traLineBuilder, accJournalItem.getDate());
		
		// nature de mouvement
		if(accJournalItem.grantReference() instanceof AEDocumentDescriptor) {
			AEDocumentDescriptor docDescr = (AEDocumentDescriptor) accJournalItem.grantReference();
			if(AEDocumentType.valueOf(AEDocumentType.System.AEPurchaseInvoice).equals(docDescr.getDocumentType())
					|| AEDocumentType.valueOf(AEDocumentType.System.AEPurchaseInvoiceFNP).equals(docDescr.getDocumentType())) {
				append(traLineBuilder, "FF", 2);
			} else if(AEDocumentType.valueOf(AEDocumentType.System.AESaleInvoice).equals(docDescr.getDocumentType())) {
				append(traLineBuilder, "FC", 2);
			} else {
				append(traLineBuilder, "OD", 2);
			}
		} else {
			append(traLineBuilder, "OD", 2);
		}
		
		// compte general
		append(traLineBuilder, accJournalItem.grantAccount().getCode(), 17);
		
		// Nature de ligne de movement
		// compte auxiliary
		if(accJournalItem.getAuxiliary() != null && !AEStringUtil.isEmpty(accJournalItem.getAuxiliary().getCode())) {
			append(traLineBuilder, "X", 1);
			append(traLineBuilder, accJournalItem.getAuxiliary().getCode(), 17);
		} else {
			append(traLineBuilder, AEStringUtil.EMPTY_STRING, 1);
			append(traLineBuilder, AEStringUtil.EMPTY_STRING, 17);
		}
			
		// reference du movement
		if(accJournalItem.getReference() != null) {
			append(traLineBuilder, accJournalItem.getReference().getDescription(), 35);
		} else {
			append(traLineBuilder, AEStringUtil.EMPTY_STRING, 35);
		}
		
		// libelle du movement
		append(traLineBuilder, accJournalItem.getDescription(), 35);
		
		// code payment
		AEDescriptor paymentType = null;
		String cegidPaymentTypeCode = AEStringUtil.EMPTY_STRING;
		if(accJournalItem.grantReference() instanceof AEDocumentDescriptor) {
			AEDocumentDescriptor docDescr = (AEDocumentDescriptor) accJournalItem.grantReference();
			if(AEDocumentType.valueOf(AEDocumentType.System.AEPurchaseInvoice).equals(docDescr.getDocumentType())) {
				paymentType = accJournalItem.getIssuerPaymentType();
			} else if(AEDocumentType.valueOf(AEDocumentType.System.AESaleInvoice).equals(docDescr.getDocumentType())) {
				paymentType = accJournalItem.getRecipientPaymentType();
			}
		};
		if(paymentType != null) {
			switch((int) paymentType.getSysId()) {
				case 10:
					cegidPaymentTypeCode = "VIR";
					break;
				case 20:
					cegidPaymentTypeCode = "PRE";
					break;
				case 30:
					cegidPaymentTypeCode = "CHQ";
					break;
				case 40:
					cegidPaymentTypeCode = "ESP";
					break;
				case 50:
					cegidPaymentTypeCode = " CB";
					break;
				case 60:
					cegidPaymentTypeCode = "LCR";
					break;
				case 70:
					cegidPaymentTypeCode = "TEP";
					break;
				case 80:
					cegidPaymentTypeCode = "TIP";
					break;
				case 90:
					cegidPaymentTypeCode = "TRI";
					break;
				case 100:
					cegidPaymentTypeCode = "VIC";
					break;
				case 110:
					cegidPaymentTypeCode = "VIT";
					break;
			}
		}
		append(traLineBuilder, cegidPaymentTypeCode, 3);
		
		// payment due date
		append(traLineBuilder, accJournalItem.getPaymentDueDate());
		
		// Debit or Credit
		if(accJournalItem.getDtAmount() != null && accJournalItem.getCtAmount() == null) {
			append(traLineBuilder, "D", 1);	
		} else if(accJournalItem.getDtAmount() == null && accJournalItem.getCtAmount() != null) {
			append(traLineBuilder, "C", 1);
		} else {
			append(traLineBuilder, AEStringUtil.EMPTY_STRING, 1);
		}
		
		// amount
		if(accJournalItem.getDtAmount() != null && accJournalItem.getCtAmount() == null) {
			append(traLineBuilder, accJournalItem.getDtAmount(), 20);
		} else if(accJournalItem.getDtAmount() == null && accJournalItem.getCtAmount() != null) {
			append(traLineBuilder, accJournalItem.getCtAmount(), 20);
		} else {
			append(traLineBuilder, 0.0, 20);
		}
		
		// movement type
		append(traLineBuilder, "N", 1);
		
		// qty
		append(traLineBuilder, AEStringUtil.EMPTY_STRING, 8);
		
		// currency code
		append(traLineBuilder, accJournalItem.grantCurrency().getCode(), 3);
		
		// currency rate
		append(traLineBuilder, 1.0, 10);
		
		// currency codification
		append(traLineBuilder, "E--", 3);
		
		append(traLineBuilder, AEStringUtil.EMPTY_STRING, 20);
		
		append(traLineBuilder, AEStringUtil.EMPTY_STRING, 20);
		
		append(traLineBuilder, request.getCustomer().optString("code"), 3);
		
		append(traLineBuilder, AEStringUtil.EMPTY_STRING, 2);
		
		append(traLineBuilder, AEStringUtil.EMPTY_STRING, 2);
		
		return traLineBuilder.toString();
	}
	
	/**
	 * Append specified <code>str</code> to the end of specified <code>traLineBuilder</code>
	 * left ajustment into field with specified <code>lenght</code>.
	 * 
	 * @param traLineBuilder
	 * @param str
	 * @param lenght
	 */
	private void append(StringBuilder traLineBuilder, String str, int lenght) {
		String field = str != null ? str : "";
		StringAlign strAlign = new StringAlign(lenght, StringAlign.Align.LEFT);
		traLineBuilder.append(strAlign.format(field));
	}
	
	private void append(StringBuilder traLineBuilder, Date date) {
		if(date == null) {
			GregorianCalendar cal = new GregorianCalendar(1900, 0, 1);
			date = cal.getTime();
		}
		traLineBuilder.append(dateFormat.format(date));
	}
	
	private void append(StringBuilder traLineBuilder, Double value, int lenght) {
		StringAlign strAlign = new StringAlign(lenght, StringAlign.Align.RIGHT);
		if(value != null) {
			String field = amountFormat.format(value);
			traLineBuilder.append(strAlign.format(field));
		} else {
			traLineBuilder.append(strAlign.format(""));
		}
	}

	@Override
	public void onJobFinished() {
		// copy file
		CopyFileTask task = new CopyFileTask();
		executor.execute(task);
	}
}
