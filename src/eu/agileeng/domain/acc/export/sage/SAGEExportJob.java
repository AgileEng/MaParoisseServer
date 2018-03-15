package eu.agileeng.domain.acc.export.sage;

import java.util.Date;

import eu.agileeng.domain.acc.AccJournalItem;
import eu.agileeng.domain.acc.export.ExportJob;
import eu.agileeng.domain.acc.export.ExportRequest;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.util.AEStringUtil;

public class SAGEExportJob extends ExportJob {
	
	/**
	 * @param context
	 * @param request
	 */
	public SAGEExportJob(AEInvocationContext context, ExportRequest request) {
		super(context, request);
		
		// initialize date format by default
		dateFormat.applyPattern("dd/MM/yyyy");
	}

	@Override
	public String createLine(AccJournalItem accJournalItem, ExportRequest request) {
		StringBuilder lineBuilder = new StringBuilder(256);
		
		// date de mouvement
		append(lineBuilder, accJournalItem.getDate());
		
		// code journal
		append(lineBuilder, accJournalItem.grantJournal().getCode());
		
		// compte general or compte auxiliare
		if(accJournalItem.getAuxiliary() != null && !AEStringUtil.isEmpty(accJournalItem.getAuxiliary().getCode())) {
			append(lineBuilder, accJournalItem.getAuxiliary().getCode());
		} else {
			append(lineBuilder, accJournalItem.grantAccount().getCode());
		}
		
		// reference du movement
		if(accJournalItem.getReference() != null) {
			append(lineBuilder, accJournalItem.getReference().getDescription());
		} else {
			append(lineBuilder, AEStringUtil.EMPTY_STRING);
		}
		
		// libelle du movement
		append(lineBuilder, accJournalItem.getDescription());
		
		// Debit amount
		append(lineBuilder, accJournalItem.getDtAmount());
		
		// Credit amount
		append(lineBuilder, accJournalItem.getCtAmount());
		
		// currency code
		append(lineBuilder, "E");
		
		return lineBuilder.toString();
	}
	
	/**
	 * Append specified <code>str</code> to the end of specified <code>traLineBuilder</code>
	 * left ajustment into field with specified <code>lenght</code>.
	 * 
	 * @param lineBuilder
	 * @param str
	 * @param lenght
	 */
	private void append(StringBuilder lineBuilder, String str) {
		String field = str != null ? str : AEStringUtil.EMPTY_STRING;
		lineBuilder.append(field);
		lineBuilder.append("\t");
	}
	
	private void append(StringBuilder lineBuilder, Date date) {
		if(date == null) {
			lineBuilder.append(AEStringUtil.EMPTY_STRING);
		} else {
			lineBuilder.append(dateFormat.format(date));
		}
		lineBuilder.append("\t");
	}
	
	private void append(StringBuilder lineBuilder, Double value) {
		String field = null;
		if(value != null) {
			field = amountFormat.format(value);
		} else {
			field = amountFormat.format(0.0);
		}
		lineBuilder.append(field);
		lineBuilder.append("\t");
	}

	@Override
	public void onJobFinished() {
		// nothing to do
	}
}
