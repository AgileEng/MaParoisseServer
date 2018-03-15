/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 23.11.2009 14:18:24
 */
package eu.agileeng.services.opportunity.ejb;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.avi.OpportunitiesList;
import eu.agileeng.domain.avi.Opportunity;
import eu.agileeng.domain.document.AEDocumentFilter;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.oracle.OpportunityDAO;
import eu.agileeng.services.AEBean;
import eu.agileeng.services.party.ejb.PartyLocal;


/**
 *
 */
@SuppressWarnings("serial")
@Stateless
public class OpportunityBean extends AEBean implements OpportunityLocal, OpportunityRemote {

	@EJB private PartyLocal partyService;
	
	/**
	 * 
	 */
	public OpportunityBean() {
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.services.opportunity.OpportunityService#load()
	 */
	@Override
	public OpportunitiesList load() throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection aeConn = null;
		try {
			aeConn = daoFactory.getConnection();
			OpportunityDAO opportDAO = daoFactory.getOpportunityDAO(aeConn);
			return opportDAO.load();
		} finally {
			AEConnection.close(aeConn);
		}
	}

	/* (non-Javadoc)
	 * @see eu.agileeng.services.opportunity.OpportunityService#save(eu.agileeng.domain.avi.OpportunitiesList)
	 */
	/**
	 * Inserts or Updates or Nothing toward PersistentState of every 
	 * <code>opportList</code> member.
	 */
	@Override
	public OpportunitiesList manage(OpportunitiesList opportList) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConnection = null;
		try {
			// get connection and begin transaction
			localConnection = daoFactory.getConnection();
			localConnection.beginTransaction();
			
			// save in transaction
			for (Opportunity opportunity : opportList) {
				switch(opportunity.getPersistentState()) {
					case NEW:
						insert(opportunity, localConnection);
						break;
					case UPDATED:
						update(opportunity, localConnection);
						break;
					case VIEW:
						continue;
					default:
						// internal error
						assert(false);
						break;
				}
			}
			
			localConnection.commit();
			return opportList;
		} catch (Throwable t) {
			localConnection.rollback();
			t.printStackTrace();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	/**
 	 * private where no need for different  
	 */
	private void insert(Opportunity opport, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConn = null;
		try {
			// get connection wrapper to execute this method
			localConn = daoFactory.getConnection(aeConnection);
			localConn.beginTransaction();
			
			// manage customer
			if(opport.getCustomer() != null) {
				partyService.manage(opport.getCustomer(), null, localConn);
			}
			
			// manage person
			if(opport.getResponsiblePerson() != null) {
				partyService.manage(opport.getResponsiblePerson(), null, localConn);
			}
			
			// insert opportunity
			OpportunityDAO opportDAO = daoFactory.getOpportunityDAO(aeConnection);
			opportDAO.insert(new OpportunitiesList(opport));
			
			// commit
			localConn.commit();
		} catch (Throwable t) {
			t.printStackTrace();
			localConn.rollback();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConn);
		}
	}
	
	/**
 	 * Updates specified <code>opport</code> in the boundary of specified
 	 * <code>aeConnection</code>.  
	 */
	private void update(Opportunity opport, AEConnection aeConnection) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection localConn = null;
		try {
			// get connection wrapper to execute this method
			localConn = daoFactory.getConnection(aeConnection);
			localConn.beginTransaction();
			
			// update customer
			if(opport.getCustomer() != null) {
				partyService.manage(opport.getCustomer(), null, localConn);
			}
			
			// update responsible person
			if(opport.getResponsiblePerson() != null) {
				partyService.manage(opport.getResponsiblePerson(), null, localConn);
			}
			
			// update opportunity
			OpportunityDAO opportDAO = daoFactory.getOpportunityDAO(aeConnection);
			opportDAO.update(new OpportunitiesList(opport));
			
			// commit
			localConn.commit();
		} catch (Throwable t) {
			t.printStackTrace();
			localConn.rollback();
			throw new AEException(t.getMessage(), t);
		} finally {
			AEConnection.close(localConn);
		}
	}
	
	/* (non-Javadoc)
	 * @see eu.agileeng.services.opportunity.OpportunityService#load(eu.agileeng.domain.contact.OpportunityFilter)
	 */
	@Override
	public OpportunitiesList load(AEDocumentFilter filter) throws AEException {
		DAOFactory daoFactory = DAOFactory.getInstance();
		AEConnection aeConn = null;
		try {
			aeConn = daoFactory.getConnection();
			OpportunityDAO opportDAO = daoFactory.getOpportunityDAO(aeConn);
			return opportDAO.load(filter);
		} finally {
			AEConnection.close(aeConn);
		}
	}
}
