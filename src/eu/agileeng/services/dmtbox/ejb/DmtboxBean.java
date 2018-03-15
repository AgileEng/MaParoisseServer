package eu.agileeng.services.dmtbox.ejb;

import org.apache.tomcat.util.json.JSONArray;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.dmtbox.DmtboxDAO;
import eu.agileeng.persistent.dao.oracle.OrganizationDAO;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.services.AEBean;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AEInvocationContextValidator;

public class DmtboxBean extends AEBean implements DmtboxLocal, DmtboxRemote {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7497659636069232118L;
	
	private static final int OPERATOR_ID = 203;//213;
	
	public static final String DEMATBOX = "Intuitiv'box";
	
	@Override
	public int getOperatorId() {
		return OPERATOR_ID;
	}
	
	@Override
	public String generateVirtualBoxId(long ownerId, String virtualBoxName, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			// Factories
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			// generate virtualBoxId
			DmtboxDAO dmtboxDAO = daoFactory.getDmtboxDAO(localConnection);
			Long virtualBoxId = dmtboxDAO.insertVirtualBox(Organization.lazyDescriptor(ownerId), virtualBoxName);
			
			// return
			return virtualBoxId.toString();
		} catch(Exception e) {
			throw new AEException("Error generating virtualBoxId: "+e.getMessage(), e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public String getCustomerName(long ownerId, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			// Factories
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			// get customer name
			OrganizationDAO orgDAO = daoFactory.getOrganizationDAO(localConnection);
			AEDescriptor orgDescr = orgDAO.loadDescriptor(ownerId);
			String customerName = null;
			if(orgDescr != null) {
				customerName = orgDescr.getName();
			} else {
				customerName = "Unknown";
			}
			
			// return
			return customerName;
		} catch(Exception e) {
			throw new AEException("Error loading customer name: "+e.getMessage(), e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public Long getOwnerId(String virtualBoxId, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			
			// Factories
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			// generate virtualBoxId
			DmtboxDAO dmtboxDAO = daoFactory.getDmtboxDAO(localConnection);
			Long ownerId = dmtboxDAO.loadOwnerIdByVirtualBoxId(Long.parseLong(virtualBoxId));
			
			// return
			return ownerId;
		} catch(Exception e) {
			throw new AEException("Error loading ownerId: " + e.getMessage(), e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public String getModuleName() {
		return DEMATBOX;
	}
	
	@Override
	public JSONArray loadVirtualBoxByOwnerId(long ownerId, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			// whether this user is ownered by this customer
			ap.ownershipValidator(ownerId);
			
			// Factories
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			// generate virtualBoxId
			DmtboxDAO dmtboxDAO = daoFactory.getDmtboxDAO(localConnection);
			JSONArray virtualBox = dmtboxDAO.loadVirtualBoxByOwnerId(ownerId);
			
			// return
			return virtualBox;
		} catch(Exception e) {
			throw new AEException("Error loading ownerId: " + e.getMessage(), e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public long validateOwnershipOfVirtualBoxId(long ownerId, String virtualBoxId, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			// whether this user is owned by this customer
			ap.ownershipValidator(ownerId);
			
			// Factories
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			// generate virtualBoxId
			DmtboxDAO dmtboxDAO = daoFactory.getDmtboxDAO(localConnection);
			long id = dmtboxDAO.loadByVirtualBoxIdAndOwnerId(ownerId, Long.parseLong(virtualBoxId));
			
			if (id != Long.parseLong(virtualBoxId)) { //the given virtualBoxId is not registered with this tenant
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
			
			return id;
		} catch(Exception e) {
			throw new AEException("Error loading ownerId: " + e.getMessage(), e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public void deleteVirtualBox(long ownerId, String virtualBoxId, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			// validate
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);
			AuthPrincipal ap = invContext.getAuthPrincipal();
			
			// whether this user is owned by this customer
			ap.ownershipValidator(ownerId);
			
			// Factories
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection(
					(AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			
			// generate virtualBoxId
			DmtboxDAO dmtboxDAO = daoFactory.getDmtboxDAO(localConnection);
			dmtboxDAO.deleteVirtualBox(ownerId, Long.parseLong(virtualBoxId));
			
			
		} catch(Exception e) {
			throw new AEException("Error loading ownerId: " + e.getMessage(), e);
		} finally {
			AEConnection.close(localConnection);
		}
	}
}
