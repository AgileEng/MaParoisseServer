package eu.agileeng.domain.acc.export;

import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.acc.export.cegid.CEGIDExportFactory;
import eu.agileeng.domain.acc.export.sage.SAGEExportFactory;
import eu.agileeng.services.AEInvocationContext;

/**
 * @author Vesko Vatov
 *
 */
public abstract class ExportFactory {
	
	/**
	 * 
	 */
	protected ExportFactory() {
	}
	
	public static ExportFactory getInstance(String export) throws AEException {
		ExportFactory expFactory = null;
		if("CEGID".equalsIgnoreCase(export)) {
			expFactory = new CEGIDExportFactory();
		} else if("SAGE".equalsIgnoreCase(export)) {
			expFactory = new SAGEExportFactory();
		} else {
			throw new  AEException(
					(int) AEError.System.UNKNOWN_EXPORT_IDENTIFICATOR.getSystemID(),
					"Unknown export identification");
		}
		return expFactory;
	}
	
	public abstract ExportJob getExportJob(AEInvocationContext context, ExportRequest request) throws AEException;
	
	public abstract String getDestinationFileExtension();
}
