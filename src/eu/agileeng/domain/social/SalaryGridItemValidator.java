package eu.agileeng.domain.social;

import eu.agileeng.domain.AEException;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.AEValidator;

public class SalaryGridItemValidator implements AEValidator {

	private static SalaryGridItemValidator inst = new SalaryGridItemValidator(); 
	
	public static SalaryGridItemValidator getInstance() {
		return inst;
	}
	
	@Override
	public void validate(Object o) throws AEException {
		if(o instanceof SalaryGridItem) {
			SalaryGridItem sgItem = (SalaryGridItem) o;
			if((AEStringUtil.isEmpty(sgItem.getEchelon()) && sgItem.getCoefficient() == null) 
					|| sgItem.getSalary() == null) {
				throw new AEException("La validation a échoué!");
			}
		}
	}
}
