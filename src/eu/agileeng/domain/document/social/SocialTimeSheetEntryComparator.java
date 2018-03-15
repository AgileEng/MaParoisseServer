package eu.agileeng.domain.document.social;

import java.util.Comparator;

import eu.agileeng.util.AEDateUtil;

public class SocialTimeSheetEntryComparator implements Comparator<SocialTimeSheetEntry> {

	private static SocialTimeSheetEntryComparator inst = new SocialTimeSheetEntryComparator();
	
	private SocialTimeSheetEntryComparator() {
	}
	
	public static SocialTimeSheetEntryComparator getInst() {
		return inst;
	}
	
	@Override
	public int compare(SocialTimeSheetEntry o1, SocialTimeSheetEntry o2) {
		if (SocialPeriodType.TEMPLATE.getId() == o1.getPeriodType().getId()) {
			return AEDateUtil.compareDays(o1.getDate(), o2.getDate());
		} else {
			if(o1.getWeekNumber() < o2.getWeekNumber()) {
				return -1;
			} else if(o1.getWeekNumber() == o2.getWeekNumber()) {
				if(o1.getDayOfWeek() < o2.getDayOfWeek()) {
					return -1;
				} else if(o1.getDayOfWeek() == o2.getDayOfWeek()) {
					return 0;
				} else {
					return 1;
				}
			} else {
				return 1;
			}
		}
	}
}
