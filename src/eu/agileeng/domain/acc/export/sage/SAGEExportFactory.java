package eu.agileeng.domain.acc.export.sage;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.acc.export.ExportFactory;
import eu.agileeng.domain.acc.export.ExportJob;
import eu.agileeng.domain.acc.export.ExportRequest;
import eu.agileeng.services.AEInvocationContext;

public class SAGEExportFactory extends ExportFactory {

	/**
	 * 
	 */
	public SAGEExportFactory() {
	}

	@Override
	public ExportJob getExportJob(AEInvocationContext context, ExportRequest request) throws AEException {
		return new SAGEExportJob(context, request);
	}

	@Override
	public String getDestinationFileExtension() {
		return "xls";
	}

}
