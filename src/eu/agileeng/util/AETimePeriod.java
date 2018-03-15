/**
 * 
 */
package eu.agileeng.util;

import java.io.Serializable;
import java.util.Date;

/**
 * @author vvatov
 *
 */
public class AETimePeriod implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6047473389958781792L;
	
	private long duration;
	
	private long unit;
	
	private Date startDate;
	
	private Date endDate;

	public AETimePeriod(Date startDate, Date endDate) {
		this.startDate = startDate;
		this.endDate = endDate;
	}
	
	public AETimePeriod() {
	}
	
	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public long getUnit() {
		return unit;
	}

	public void setUnit(long unit) {
		this.unit = unit;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public boolean isValid() {
		if(isNull()) {
			return true;
		}
		
		// both dates are not null 
		if(startDate == null || endDate == null) {
			return false;
		}
		
		// startDate <= endDate
		if(endDate.before(startDate)) {
			return false;
		}
		
		return true;
	}
	
	public boolean isValidAndNotNull() {
		return !isNull() && isValid();
	}
	
	public boolean isNull() {
		return startDate == null && endDate == null;
	}
	
	public boolean before(AETimePeriod b) {
		AETimePeriod a = this;
		// a2 <= b1
		return !b.startDate.before(a.endDate);
	}
	
	public double calcDurationInHours() {
		double hours = 0.0;
		if(isValidAndNotNull()) {
			hours = AEDateUtil.getHoursDiffSign(endDate, startDate);
		}
		return hours;
	}
}
