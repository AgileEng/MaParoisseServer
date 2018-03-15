/**
 * 
 */
package eu.agileeng.domain.acc.export.cegid;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.acc.export.ExportFactory;
import eu.agileeng.domain.acc.export.ExportJob;
import eu.agileeng.domain.acc.export.ExportRequest;
import eu.agileeng.services.AEInvocationContext;

/**
 * @author vvatov
 *
 */
public class CEGIDExportFactory extends ExportFactory {

	/**
	 * 
	 */
	public CEGIDExportFactory() {
	}

	@Override
	public ExportJob getExportJob(AEInvocationContext context, ExportRequest request) throws AEException {
		return new CEGIDExportJob(context, request);
	}

	@Override
	public String getDestinationFileExtension() {
		return "tra";
	}
}
