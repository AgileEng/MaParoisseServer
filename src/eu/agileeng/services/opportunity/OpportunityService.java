/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 23.11.2009 14:14:49
 */
package eu.agileeng.services.opportunity;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.avi.OpportunitiesList;
import eu.agileeng.domain.document.AEDocumentFilter;
import eu.agileeng.services.AEService;

/**
 *
 */
public interface OpportunityService extends AEService {
	public OpportunitiesList load() throws AEException;
	public OpportunitiesList load(AEDocumentFilter filter) throws AEException;
	public OpportunitiesList manage(OpportunitiesList opportList) throws AEException;
}
