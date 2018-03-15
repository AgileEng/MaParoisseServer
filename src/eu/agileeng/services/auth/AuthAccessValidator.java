package eu.agileeng.services.auth;

import java.util.HashMap;
import java.util.Map;

import eu.agileeng.accbureau.AEAppModule;
import eu.agileeng.accbureau.AEAppModulesList;
import eu.agileeng.domain.AEException;
import eu.agileeng.security.AuthAccessController;
import eu.agileeng.security.AuthPermission;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.security.AuthRole;
import eu.agileeng.services.AEInvocationContext;

public class AuthAccessValidator {
	
	private String path;
	private int mask;
	//allowed modules
	private Map<String, AEAppModule> modules;
	
	public AuthAccessValidator(String path) {
		this.path = path;
	}
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public int getMask() {
		return mask;
	}

	public void setMask(int mask) {
		this.mask = mask;
	}
	
	public Map<String, AEAppModule> getModules() {
		return modules;
	}

	public void setModules(Map<String, AEAppModule> modules) {
		this.modules = modules;
	}
	
	public void setModules(AEAppModulesList modulesList) {
		if (modulesList != null) {
			Map<String, AEAppModule> am = new HashMap<String, AEAppModule>();
			for (AEAppModule m: modulesList) {
				am.put(m.getCode(), m);
			}
			
			this.setModules(am);
		}
	}
	
	/**
	 * Check permission for a given tile. 
	 * It is recommended to use it only when checking menu tiles and not modules
	 * 
	 * @param code
	 * @param invContext
	 * @return TRUE if the required operation is permitted and FALSE if not
	 * @throws AEException
	 */
	public boolean checkPermission(String code, AEInvocationContext invContext) throws AEException {
		return this.checkPermission(code, this.getMask(), invContext, false);
	}
	
	/**
	 * Check permission with the optional boolean for modules check. Can be used for both modules and menu tiles.
	 * 
	 * @param code
	 * @param invContext
	 * @param checkModules
	 * @return TRUE if the required operation is permitted and FALSE if not
	 * @throws AEException
	 */
	public boolean checkPermission(String code, AEInvocationContext invContext, boolean checkModules) throws AEException {
		return this.checkPermission(code, this.getMask(), invContext, checkModules);
	}

	public boolean checkPermission(String code, int mask, AEInvocationContext invContext, boolean checkModules) throws AEException {
		
		// builtin permissions
		AuthPrincipal ap = invContext.getAuthPrincipal();
		if(ap.isMemberOf(AuthRole.System.technician) 
				|| ap.isMemberOf(AuthRole.System.administrator) 
				|| ap.isMemberOf(AuthRole.System.power_user)) {
			
			return true;
		}
		
		//it modules is null => auth principal has no or more than 1 company allowed
		//if (checkModules && this.getModules() != null && this.getModules().get(code) == null) return false;
		
		String mp = path+code;
		boolean valid = false;
		
		try {
			AuthAccessController.checkPermission(new AuthPermission(mp, mask), invContext);
			valid = true;
		} catch (AEException e) {
//			e.printStackTrace();
//			System.out.println(mp);
		}
		
		return valid;
	}
}
