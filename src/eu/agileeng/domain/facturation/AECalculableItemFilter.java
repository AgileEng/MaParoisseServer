package eu.agileeng.domain.facturation;

import eu.agileeng.persistent.AEPersistent;
import eu.agileeng.util.AEFilter;

public class AECalculableItemFilter implements AEFilter {

	private static AECalculableItemFilter inst;
	
	public static AECalculableItemFilter getInstance() {
		if(inst == null) {
			inst = new AECalculableItemFilter();
		}
		return inst;
	}
	
	@Override
	public boolean evaluate(Object o) {
		boolean bRet = false;
		if(o instanceof AEFactureItem) {
			AEFactureItem item = (AEFactureItem) o;
			if(item.getPersistentState() != AEPersistent.State.DELETED) {
				if(AEFactureUtil.FactureItemType.ARTICLE.equals(item.getTypeItem())
						|| AEFactureUtil.FactureItemType.CATEGORY.equals(item.getTypeItem())
						|| AEFactureUtil.FactureItemType.ADVANCE.equals(item.getTypeItem())
						|| AEFactureUtil.FactureItemType.ADVANCE_DEDUCTION.equals(item.getTypeItem())) {

					bRet = true;
				}
			}
		}
		return bRet;
	}
}
