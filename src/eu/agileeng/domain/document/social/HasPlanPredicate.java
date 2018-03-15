package eu.agileeng.domain.document.social;

import eu.agileeng.util.AEPredicate;

public class HasPlanPredicate implements AEPredicate {

	private static final long serialVersionUID = 4275927300786403348L;

	private static HasPlanPredicate inst = new HasPlanPredicate();
	
	private HasPlanPredicate() {
		
	}
	
	public static HasPlanPredicate getInstance() {
		return inst;
	}
	
	@Override
	public boolean evaluate(Object o) {
		boolean res = false;
		if(o instanceof SocialTimeSheetEntry) {
			SocialTimeSheetEntry entry = (SocialTimeSheetEntry) o;
			res = entry.hasPlan() && entry.isReal();
		}
		return res;
	}
}
