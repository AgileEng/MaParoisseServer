package eu.agileeng.domain.document.social;

public class SocialTimeSheetTemplateKey {

	private int weekNumber;
	
	private int dayOfWeek;
	
	protected SocialTimeSheetTemplateKey() {
	}
	
	public SocialTimeSheetTemplateKey(int weekNumber, int dayOfWeek) {
		this.weekNumber = weekNumber;
		this.dayOfWeek = dayOfWeek;
	}

	public int getWeekNumber() {
		return weekNumber;
	}

	public void setWeekNumber(int weekNumber) {
		this.weekNumber = weekNumber;
	}

	public int getDayOfWeek() {
		return dayOfWeek;
	}

	public void setDayOfWeek(int dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + dayOfWeek;
		result = prime * result + weekNumber;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SocialTimeSheetTemplateKey))
			return false;
		SocialTimeSheetTemplateKey other = (SocialTimeSheetTemplateKey) obj;
		if (dayOfWeek != other.dayOfWeek)
			return false;
		if (weekNumber != other.weekNumber)
			return false;
		return true;
	}

}
