package eu.agileeng.domain.social;

import eu.agileeng.domain.AEException;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AEValidator;

public class SalaryGridValidator implements AEValidator {
	private static SalaryGridValidator inst = new SalaryGridValidator(); 
	
	public static SalaryGridValidator getInstance() {
		return inst;
	}
	
	@Override
	public void validate(Object o) throws AEException {
		if(o instanceof SalaryGrid) {
			SalaryGrid sg = (SalaryGrid) o;
			if(AEStringUtil.isEmpty(sg.getName())) {
				throw new AEException("La validation a échoué!");
			}
		}
	}
}
