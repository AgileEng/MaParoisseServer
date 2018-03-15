package eu.agileeng.services.auth.ejb;

import javax.ejb.Stateless;

import eu.agileeng.accbureau.AEAppModule;
import eu.agileeng.accbureau.AEAppModulesList;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDescriptorsSet;
import eu.agileeng.domain.AEException;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.app.AppDAO;
import eu.agileeng.services.AEBean;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.auth.AuthAccessValidator;

@Stateless
public class AuthAccessBean extends AEBean implements AuthAccessLocal {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -21568667436446676L;

	@Override
	public AuthAccessValidator getPath(String code, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		
		try {
			
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			AppDAO appDAO = daoFactory.getAppDAO(localConnection);
			
			AEAppModulesList modulesPath = appDAO.loadAppModulePath(code);
			
			StringBuilder path = new StringBuilder();
			
			for (AEAppModule m: modulesPath) {
				path.append(m.getCode()).append("/");
			}
			
			return new AuthAccessValidator(path.toString());
		} catch (Throwable t) {
			
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public AEAppModulesList getAvailableModules(AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection();
			AppDAO appDAO = daoFactory.getAppDAO(localConnection);
			
			AEAppModulesList modules = null;
			
			AEDescriptorsSet companies = invContext.getAuthPrincipal().getCompaniesSet();
			
			if (companies.size() == 1) {
				for(AEDescriptor c: companies) {
					modules = appDAO.loadAvailableAppModules(c);
					
				}
			}
			
			return modules;
		} catch (Throwable t) {
			
			throw new AEException(t);
		} finally {
			AEConnection.close(localConnection);
		}
	}
	
	@Override
	public AuthAccessValidator getValidator(String code, AEInvocationContext invContext) throws AEException {
		AuthAccessValidator validator = getPath(code, invContext);
		validator.setModules(getAvailableModules(invContext));
		
		return validator;
	}
	
}
