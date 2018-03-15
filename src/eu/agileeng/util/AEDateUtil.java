package eu.agileeng.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Months;
import org.joda.time.Years;

/**
 * <p>
 * </p>
 * 
 * @author Nabla team
 * @version 1.0 Created on 2005-1-3
 */

public class AEDateUtil {
	public static Map<Integer, String> frenchMonths = new HashMap<Integer, String>();
	
    public final static long MILIS_IN_HOUR = 60 * 60 * 1000;
    
    public final static long MILIS_IN_DAY = MILIS_IN_HOUR * 24;
    
    public final static long MILIS_IN_WEEK = MILIS_IN_DAY * 7 ;

    public final static String SYSTEM_DATE_FORMAT = "dd-MM-yyyy";
    
    public final static String SYSTEM_TIME_FORMAT = "HH:mm";
    
    public final static String FRENCH_DATE_FORMAT = "dd/MM/yyyy";
    
    public final static String SYSTEM_DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm";
    
    public final static String DB_DATE_FORMAT = "yyyy-MM-dd";
    
    public final static String EXPORT_FILE_DATE_FORMAT = "yyyyMMdd";
    
    public final static String MONTH_YEAR_FORMAT = "MM/yyyy";
    
    public final static String FRENCH_DATE_FORMAT_WITHOUT_YEAR = "dd/MM";

    private static final String formats[] = {
        "dd*MM*yyyy",
        "yyyy*MM*dd HH:mm:ss",
        "dd*MM*yyyy hh:mm:ss a",
        "dd*MM*yyyy HH:mm:ss", 
        "yyyy*MM*dd HH:mm",
        "dd*MM*yyyy hh:mm a",
        "dd*MM*yyyy HH:mm", 
        "dd*MM*yy", 
        "yyyy*MM*dd",
        "yyyyMMdd_HHmmss"};

    private static final String separators[] = {"-", ".", "/", "\\"};

    public static final String DATE_NULL = AEDateUtil.formatToSystem(new Date(0));
    
    public static Date DATE_MAX = null;
    
    public static Date DATE_MIN = null;
    
    private static DateMidnightComparator dateMidnightComparator = null;
    
    static {
        Calendar c = Calendar.getInstance();
        
        // 31/12/9999
        c.set(9999, 11, 31, 0, 0, 0);
        DATE_MAX = c.getTime();
        
        // 01/01/1900
        c.set(1900, 0, 1, 0, 0, 0);
        DATE_MIN = c.getTime();
        
        frenchMonths.put(1, "Janv");
        frenchMonths.put(2, "Fév");
        frenchMonths.put(3, "Mars");
        frenchMonths.put(4, "Avr");
        frenchMonths.put(5, "Mai");
        frenchMonths.put(6, "Juin");
        frenchMonths.put(7, "Juil");
        frenchMonths.put(8, "Aout");
        frenchMonths.put(9, "Sept");
        frenchMonths.put(10, "Oct");
        frenchMonths.put(11, "Nov");
        frenchMonths.put(12, "Déc");
    }
    
    public static Calendar getEuropeanCalendar() {
        Calendar c = Calendar.getInstance();
        c.setMinimalDaysInFirstWeek(4);
        c.setFirstDayOfWeek(Calendar.MONDAY);
        return c;
    }

    public static Calendar getEuropeanCalendar(Date time) {
        Calendar c = getEuropeanCalendar();
        c.setTime(time);
        return c;
    }

    /**
     * Calculation of week numbers in given year under ISO 8601 standart.
     * (International Standard Date and Time Notation - In commercial and
     * industrial applications (delivery times, production plans, etc.),
     * especially in Europe, it is often required to refer to a week of a year.
     * Week 01 of a year is per definition the first week that has the Thursday
     * in this year, which is equivalent to the week that contains the fourth
     * day of January. In other words, the first week of a new year is the week
     * that has the majority of its days in the new year. Week 01 might also
     * contain days from the previous year and the week before week 01 of a year
     * is the last week (52 or 53) of the previous year even if it contains days
     * from the new year. A week starts with Monday (day 1) and ends with Sunday
     * (day 7).
     * 
     * @param year
     * @return week numbers in given year under ISO 8601 standart.
     */
    public static int getWeeksInYear(int year) {
        Calendar c = getEuropeanCalendar();
        c.set(Calendar.MONTH, Calendar.DECEMBER);
        c.set(Calendar.YEAR, year);
        c.set(Calendar.DAY_OF_MONTH, 28);
        return c.get(Calendar.WEEK_OF_YEAR);
    }

    public static int getWeek(Date date) {
        Calendar c = getEuropeanCalendar(date);
        return c.get(Calendar.WEEK_OF_YEAR);
    }

    public static int getDayInYear(Date date) {
        Calendar c = getEuropeanCalendar();
        c.setTime(date);
        return c.get(Calendar.DAY_OF_YEAR);

    }
    
    public static int getDayOfWeek(Date date) {
        return getEuropeanCalendar(date).get(Calendar.DAY_OF_WEEK);
    }

    public static int getDayOfWeekJoda(Date date) {
    	DateTime dt = new DateTime(date);
        return dt.dayOfWeek().get();
    }
    
    public static int getDayOfMonth(Date date) {
        return getEuropeanCalendar(date).get(Calendar.DAY_OF_MONTH);
    }
    
    public static int getMonthInYear(Date date) {
        Calendar c = getEuropeanCalendar();
        c.setTime(date);
        return c.get(Calendar.MONTH) + 1;

    }

    public static int get3MonthInYear(Date date) {
        int _monthInYear = getMonthInYear(date);

        return ((_monthInYear - 1) / 3) + 1;
    }

    public static int getYear(Date date) {
        Calendar c = getEuropeanCalendar();
        c.setTime(date);
        return c.get(Calendar.YEAR);
    }

    public static long getBeginningOfDay(Date day) {
        if (null == day) {
            return -1;
        }
        Calendar dayCalendar = getEuropeanCalendar();
        dayCalendar.setTime(day);
        dayCalendar.set(Calendar.HOUR_OF_DAY, 0);
        dayCalendar.set(Calendar.MINUTE, 0);
        dayCalendar.set(Calendar.SECOND, 0);
        dayCalendar.set(Calendar.MILLISECOND, 1);
        dayCalendar.get(Calendar.HOUR_OF_DAY);
        Date beginning = dayCalendar.getTime();
        return beginning.getTime();
    }

    public static long getEndOfDay(Date day) {
        if (null == day) {
            return -1;
        }
        Calendar dayCalendar = getEuropeanCalendar();
        dayCalendar.setTime(day);
        dayCalendar.add(Calendar.DAY_OF_YEAR, 1);
        dayCalendar.set(Calendar.HOUR_OF_DAY, 0);
        dayCalendar.set(Calendar.MINUTE, 0);
        dayCalendar.set(Calendar.SECOND, 0);
        dayCalendar.set(Calendar.MILLISECOND, 1);
        dayCalendar.get(Calendar.HOUR_OF_DAY);
        Date beginning = dayCalendar.getTime();
        return beginning.getTime();
    }

    public static long getEndOfCurrDay(Date day) {
        if (null == day) {
            return -1;
        }
        Calendar dayCalendar = getEuropeanCalendar();
        dayCalendar.setTime(day);
        // dayCalendar.add(Calendar.DAY_OF_YEAR, 1);
        dayCalendar.set(Calendar.HOUR_OF_DAY, 23);
        dayCalendar.set(Calendar.MINUTE, 59);
        dayCalendar.set(Calendar.SECOND, 59);
        dayCalendar.set(Calendar.MILLISECOND, 999);
        dayCalendar.get(Calendar.HOUR_OF_DAY);
        Date beginning = dayCalendar.getTime();
        return beginning.getTime();
    }

    public static java.util.Date getCurrDate() {
        int year = 0;
        int month = 0;
        int day = 0;
        Calendar calend = getEuropeanCalendar();
        calend.setTime(new java.util.Date());
        year = calend.get(Calendar.YEAR);
        month = calend.get(Calendar.MONTH);
        day = calend.get(Calendar.DATE);
        Calendar res = getEuropeanCalendar();
        res.set(year, month, day, 12, 0, 0);
        return res.getTime();
    }

    public static java.util.Date getClearDate(java.util.Date date) {
    	return getClearDateTime(date);
    }

    public static java.util.Date getClearDateTime(java.util.Date date) {
        if (date == null) {
            return null;
        }
        
        int year = 0;
        int month = 0;
        int day = 0;
        
        Calendar calend = getEuropeanCalendar();
        calend.setTime(date);
        year = calend.get(Calendar.YEAR);
        month = calend.get(Calendar.MONTH);
        day = calend.get(Calendar.DATE);
        
        Calendar res = getEuropeanCalendar();
        res.set(year, month, day, 0, 0, 0);
        long timeTmp = res.getTimeInMillis();
        timeTmp = (timeTmp / 1000);
        timeTmp = timeTmp * 1000;
        
        return new Date(timeTmp);
    }

    public static int getDaysDiffSign(Date date1, Date date2) {
        Date d1 = getClearDate(date1);
        Date d2 = getClearDate(date2);
        int days1 = (int) (d1.getTime() / MILIS_IN_DAY);
        int days2 = (int) (d2.getTime() / MILIS_IN_DAY);
        return days2 - days1;
    }
    
    public static int getDaysBetween(Date start, Date end) {
    	return Days.daysBetween(new DateTime(start), new DateTime(end)).getDays();
    }
    
    public static long getWeeksDiffSign(Date from, Date to) {
    	Calendar start = getEuropeanCalendar(getClearDate(from));
    	Calendar end = getEuropeanCalendar(getClearDate(to));
    	
        long startL = start.getTimeInMillis() + start.getTimeZone().getOffset(start.getTimeInMillis());
        long endL   = end.getTimeInMillis() +  end.getTimeZone().getOffset(end.getTimeInMillis());
        
        return (endL - startL) / MILIS_IN_WEEK;
    }

    public static double getHoursDiffSign(Date date1, Date date2) {
        Date _d1 = clearDateSeconds(date1);
        Date _d2 = clearDateSeconds(date2);
        double hours1 = (_d1.getTime());
        double hours2 = (_d2.getTime());
        return (hours1 - hours2) / MILIS_IN_HOUR;
    }
    
    public static int getMonthsBetween(Date start, Date end) {
    	return Months.monthsBetween(new DateTime(start), new DateTime(end)).getMonths();
    }

    public static int getYearsBetween(Date start, Date end) {
    	return Years.yearsBetween(new DateTime(start), new DateTime(end)).getYears();
    }
    
    public static Date clearDateSeconds(Date date) {
        Calendar calend = getEuropeanCalendar(date);
        calend.set(Calendar.SECOND, 0);
        return new Date((calend.getTimeInMillis() / 1000) * 1000);
    }

    public static long getDateDiffSing(Date date1, Date date2) {
        Date d1 = getClearDate(date1);
        Date d2 = getClearDate(date2);
        return d1.getTime() - d2.getTime();
    }

    /**
     * Compares two dates as compares days (whiout time of days).
     */
    public static int compareDays(Date day1, Date day2) {
        Calendar c1 = getEuropeanCalendar();
        c1.setTime(day1);
        Calendar c2 = getEuropeanCalendar();
        c2.setTime(day2);
        if ((c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR))
                && (c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH))
                && (c1.get(Calendar.DATE) == c2.get(Calendar.DATE))) {
            return 0;
        }
        return day1.compareTo(day2);
    }

    /**
     * Sets the calendar's date with one day back.
     */
    public static void setPreviousDate(Calendar cal) {
        cal.set(Calendar.DAY_OF_MONTH, (cal.get(Calendar.DAY_OF_MONTH) - 1));
    }

    public static void setNextDate(Calendar cal) {
        cal.set(Calendar.DAY_OF_MONTH, (cal.get(Calendar.DAY_OF_MONTH) + 1));
    }
    
    public static Date oneMonthBefore(Date date) {
    	Calendar cal = getEuropeanCalendar(date);
    	cal.add(Calendar.MONTH, -1);
    	return getClearDateTime(cal.getTime());
    }

    /**
     * Parse specified string as java.util.Date.
     *  
     * @param toParse
     * @return java.util.Date in case of success ot <code>null</code> in case of failure
     */
    public static Date parseDate(String toParse) {
        boolean success = false;
        int ctr = 0;
        Date result = null;
        SimpleDateFormat dateFormat = null;
        while ((!success) && (ctr < formats.length)) {
            int ctrInner = 0;
            String nextFormat = formats[ctr++];
            while ((!success) && (ctrInner < separators.length)) {
                String nextSeparator = separators[ctrInner++];
                String nextFS = nextFormat.replace("*".charAt(0), nextSeparator.charAt(0));
                dateFormat = new SimpleDateFormat(nextFS);
                try {
                     result = dateFormat.parse(toParse);
                     success = true;
                } catch (Exception ex) {
                }
            }// ~:while
        }// ~:while
        return result;
    }

    public static Date parseDateStrict(String toParse) {
        boolean success = false;
        int ctr = 0;
        Date result = null;
        SimpleDateFormat dateFormat = null;
        while ((!success) && (ctr < formats.length)) {
            int ctrInner = 0;
            String nextFormat = formats[ctr++];
            while ((!success) && (ctrInner < separators.length)) {
                String nextSeparator = separators[ctrInner++];
                String nextFS = nextFormat.replace("*".charAt(0),
                        nextSeparator.charAt(0));
                dateFormat = new SimpleDateFormat(nextFS);
                // set strict parsing mode where inputs must match this object's format.
                dateFormat.setLenient(false);
                try {
                     result = dateFormat.parse(toParse);
                     // dateformat.parse will accept any date as long as it's in the format
                     // you defined, it simply rolls dates over, for example, december 32
                     // becomes jan 1 and december 0 becomes november 30
                     // This statement will make sure that once the string
                     // has been checked for proper formatting that the date is still the
                     // date that was entered, if it's n
                     if(dateFormat.format(result).equals(toParse)) {
                         success = true;
                     } else {
                         result = null;
                     }
                } catch (Exception ex) {
                }
            }// ~:while
        }// ~:while
        
        if(result != null && (DATE_MIN.after(result) || DATE_MAX.before(result))) {
        	result = null;
        }
        
        return result;
    }
    
    /**
     * 
     * @param toParse the time in format 23:50
     * @return
     */
    public static Date parseTimeStrict(String toParse) {
        boolean success = false;
        int ctr = 0;
        Date result = null;
        SimpleDateFormat dateFormat = null;
        while ((!success) && (ctr < formats.length)) {
            int ctrInner = 0;
            String nextFormat = formats[ctr++];
            while ((!success) && (ctrInner < separators.length)) {
                String nextSeparator = separators[ctrInner++];
                String nextFS = nextFormat.replace("*".charAt(0),
                        nextSeparator.charAt(0));
                dateFormat = new SimpleDateFormat(nextFS);
                // set strict parsing mode where inputs must match this object's format.
                dateFormat.setLenient(false);
                try {
                     result = dateFormat.parse(toParse);
                     // dateformat.parse will accept any date as long as it's in the format
                     // you defined, it simply rolls dates over, for example, december 32
                     // becomes jan 1 and december 0 becomes november 30
                     // This statement will make sure that once the string
                     // has been checked for proper formatting that the date is still the
                     // date that was entered, if it's n
                     if(dateFormat.format(result).equals(toParse)) {
                         success = true;
                     } else {
                         result = null;
                     }
                } catch (Exception ex) {
                }
            }// ~:while
        }// ~:while
        return result;
    }
    
    public static Date parseTimeOnly(String hhmm) {
    	Date time = AEDateUtil.parseDateStrict(DATE_NULL + " " + hhmm);
    	if(time != null) {
    		time = AEDateUtil.clearDateSeconds(time);
    	}
        return time;
    }
    
    public static Date addDaysToDate(Date time, int days) {
        if (time != null) {
            Calendar c = getEuropeanCalendar(time);
            c.add(Calendar.DAY_OF_YEAR, days);
            return c.getTime();
        }
        return null;
    }
    
    public static Date addMinutesToDate(Date time, int minutes) {
        if (time != null) {
            Calendar c = getEuropeanCalendar(time);
            c.add(Calendar.MINUTE, minutes);
            return c.getTime();
        }
        return null;
    }
    
    public static Date addWeeksToDate(Date time, int weeks) {
        if (time != null) {
            Calendar c = getEuropeanCalendar(time);
            c.add(Calendar.DAY_OF_YEAR, weeks * 7);
            return c.getTime();
        }
        return null;
    }
    
    public static Date addMonthsToDate(Date time, int months) {
        if (time != null) {
            Calendar c = getEuropeanCalendar(time);
            c.add(Calendar.MONTH, months);
            return c.getTime();
        }
        return null;
    }

    public static Date plusMonths(Date time, int months) {
        if (time != null) {
        	DateTime jTime = new DateTime(time);
        	return jTime.plusMonths(months).toDate();
        }
        return null;
    }
    
    public static Date addYearsToDate(Date time, int years) {
        if (time != null) {
            Calendar c = getEuropeanCalendar(time);
            c.add(Calendar.YEAR, years);
            return c.getTime();
        }
        return null;
    }
    
    /**
     * Check where a date falls within a period (including end conditions) when
     * comparing as days
     * 
     * @param searchDate
     *            Te date to check. If <code>null</code> then false will be
     *            returned
     * @param startDate
     *            The start date of period. searchDate must be >= than it. If
     *            <code>null</code> then an open-start period is assumed
     * @param endDate
     *            The end date of period. searchDate must be <= than it. If
     *            <code>null</code> then an open-end period is assumed
     * @return Whether searchDate falls between the two other dates
     */
    public static boolean isInPeriod(Date searchDate, Date startDate,
            Date endDate) {
        if (null == searchDate) {
            return false;
        }
        if ((null == startDate) && (null == endDate)) {
            return true;
        }
        if (null != startDate) {
            if (getDaysDiffSign(startDate, searchDate) < 0) {
                return false;
            }
        }
        if (null != endDate) {
            if (getDaysDiffSign(searchDate, endDate) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return ends of month based on
     */
    public static Date getEndsOfTheMonth(Date initDate) {
        Calendar _c = getEuropeanCalendar(initDate);
        int lastDay = _c.getActualMaximum(Calendar.DAY_OF_MONTH);
        _c.set(Calendar.DAY_OF_MONTH, lastDay);
        return _c.getTime();
    }
    
    public static int getNumberOfDaysInTheMonth(Date initDate) {
        Calendar _c = getEuropeanCalendar(initDate);
        return _c.getActualMaximum(Calendar.DAY_OF_MONTH);
    }
    
    public static Date endOfTheYear(Date date) {
        Calendar _c = getEuropeanCalendar(date);
        _c.set(Calendar.DAY_OF_MONTH, 31);
        _c.set(Calendar.MONTH, 11);
        return _c.getTime();
    }
    
    public static Date beginOfTheYear(Date date) {
        Calendar _c = getEuropeanCalendar(date);
        _c.set(Calendar.DAY_OF_MONTH, 1);
        _c.set(Calendar.MONTH, 0);
        return _c.getTime();
    }
    
    public static AETimePeriod getTimePeriodMonth(Date startDate, int durationInMonth, Date forDate) {
    	if(startDate == null || forDate == null || durationInMonth < 1) {
    		return null;
    	}
    	Date periodStartDate = getClearDate(startDate);
    	Date clearForDate = getClearDate(forDate);

    	Calendar c = getEuropeanCalendar(periodStartDate);
    	c.add(Calendar.MONTH, durationInMonth);
    	Date periodEndDate = getClearDate(c.getTime());
    	while(periodEndDate.before(clearForDate)) {
    		periodStartDate = periodEndDate;
    	   	c.setTime(periodStartDate);
        	c.add(Calendar.MONTH, durationInMonth);
        	periodEndDate = getClearDate(c.getTime());
    	}
    	
    	// one day backward
    	c.add(Calendar.DAY_OF_MONTH, -1);
    	periodEndDate = c.getTime();
    	
    	// create period
    	AETimePeriod period = new AETimePeriod();
    	period.setStartDate(periodStartDate);
    	period.setEndDate(periodEndDate);
    	return period;
    }

    /**
     * Return new date with day <code>dayOfMonth</code> and month base on
     * <code>monthCountOffset </code> and <code>initDate</code> Examples:
     * initDate = 27.12.2008 monthCountOffset = 0 dayOfMonth = 10 Result :
     * 10.12.2008 monthCountOffset = 1 dayOfMonth = 10 Result : 10.01.2009
     * monthCountOffset = -1 dayOfMonth = 10 Result : 10.11.2008
     */
    public static Date dayOfMonth(Date initDate, int monthCountOffset, int dayOfMonth) {
        Calendar _c = getEuropeanCalendar(initDate);
        int _currMonth = _c.get(Calendar.MONTH);
        if (_currMonth + monthCountOffset > 11) {
            _c.roll(Calendar.YEAR, (_currMonth + monthCountOffset) / 11);
        }
        if (_currMonth + monthCountOffset < 0) {
            _c.roll(Calendar.YEAR,
                    -((-((_currMonth + 1) + monthCountOffset - 12)) / 12));
        }
        _c.roll(Calendar.MONTH, monthCountOffset);
        if (dayOfMonth > _c.getActualMaximum(Calendar.DAY_OF_MONTH)) {
            dayOfMonth = _c.getActualMaximum(Calendar.DAY_OF_MONTH);
        }
        _c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        return _c.getTime();
    }

    public static String convertToString(Date date, String pattern) {
        SimpleDateFormat _df;
        if (pattern != null && !pattern.isEmpty()) {
            _df = new SimpleDateFormat(pattern);
        } else {
            _df = new SimpleDateFormat(SYSTEM_DATE_FORMAT);
        }
        return _df.format(date);
    }

    public static Date toWorkDateBefore(Date date) {
        Date _d = date;
        if (_d == null) {
            _d = new Date();
        }
        while (!isWorkingDay(_d)) {
            _d = addDaysToDate(_d, -1);
        }
        return _d;
    }

    public static Date toWorkDateAfter(Date date) {
        Date _d = date;
        if (_d == null) {
            _d = new Date();
        }
        while (!isWorkingDay(_d)) {
            _d = addDaysToDate(_d, 1);
        }
        return _d;
    }

    public static boolean isWorkingDay(Date date) {
        Date _d = date;
        if (_d == null) {
            _d = new Date();
        }
        Calendar _c = getEuropeanCalendar(_d);
        int _day = _c.get(Calendar.DAY_OF_WEEK);

        if (_day == Calendar.SUNDAY || _day == Calendar.SATURDAY) {
            return false;
        }
        return true;
    }

    public static Date now() {
        return new Date();
    }
    
    public static String formatToSystem(Date date) {
    	SimpleDateFormat dateFormat = new SimpleDateFormat(SYSTEM_DATE_FORMAT);
    	return dateFormat.format(date);
    }
    
    public static String formatToFrench(Date date) {
    	SimpleDateFormat dateFormat = new SimpleDateFormat(FRENCH_DATE_FORMAT);
    	return dateFormat.format(date);
    }
    
    public static String formatToFrenchWithoutYear(Date date) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(FRENCH_DATE_FORMAT_WITHOUT_YEAR);
		return dateFormat.format(date);
    }
    
    public static String formatTimeToSystem(Date date) {
    	SimpleDateFormat dateFormat = new SimpleDateFormat(SYSTEM_TIME_FORMAT);
    	return dateFormat.format(date);
    }
    
    public static String format(Date date, String pattern) {
    	SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
    	return dateFormat.format(date);
    }
    
    public static String formatDateTimeToSystem(Date date) {
    	SimpleDateFormat dateFormat = new SimpleDateFormat(SYSTEM_DATE_TIME_FORMAT);
    	return dateFormat.format(date);
    }
    
    public static boolean areDatesEqual(Date d1, Date d2) {
    	if(d1 == null || d2 == null) {
    		return false;
    	}
    	
    	// both dates are not null
    	Calendar cal1 = getEuropeanCalendar(d1);
    	Calendar cal2 = getEuropeanCalendar(d2);
    	
    	return cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    	       && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    	       && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR);
    }
    
    public static Date getFirstDate(int month, int year) {
    	Calendar calendar = getEuropeanCalendar();
    	calendar.set(year, month, 1);
    	return getClearDate(calendar.getTime());
    }
    
    public static Date getLastDate(int month, int year) {
    	// Get a calendar instance
    	Calendar calendar = getEuropeanCalendar();

    	// Get the last date of the current month. To get the last date for a
    	// specific month you can set the calendar to the first 
    	// date for desired month.
    	calendar.set(year, month, 1);
    	int lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

    	// Set the calendar date to the last date of the month so then we can
    	// get the last day of the month
    	calendar.set(Calendar.DAY_OF_MONTH, lastDayOfMonth);	
    	
    	return getClearDate(calendar.getTime());
    }
    
    public static Date firstDayOfWeek(Date date) {
    	Calendar cal = getEuropeanCalendar(date);
    	while(cal.getFirstDayOfWeek() != cal.get(Calendar.DAY_OF_WEEK)) {
            cal.add(Calendar.DAY_OF_YEAR, -1);
    	}
    	return getClearDateTime(cal.getTime());
    }
    
    public static Date firstDayOfMonth(Date date) {
        Calendar cal = getEuropeanCalendar(date);
        cal.set(Calendar.DAY_OF_MONTH, 1);
    	return getClearDateTime(cal.getTime());
    }
    
    public static Date lastDayOfWeek(Date date) {
    	Calendar cal = getEuropeanCalendar(date);
    	while(Calendar.SUNDAY != cal.get(Calendar.DAY_OF_WEEK)) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
    	}
    	return getClearDateTime(cal.getTime());
    }
    
    public static Date lastDayOfMonth(Date date) {
        Calendar cal = getEuropeanCalendar(date);
        int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        cal.set(Calendar.DAY_OF_MONTH, lastDay);
        return getClearDateTime(cal.getTime());
    }
    
    public static java.util.Date createDateTime(java.util.Date date, java.util.Date time) {
        if (date == null || time == null) {
            return null;
        }
        int year = 0;
        int month = 0;
        int day = 0;
        Calendar calend = getEuropeanCalendar();
        calend.setTime(date);
        year = calend.get(Calendar.YEAR);
        month = calend.get(Calendar.MONTH);
        day = calend.get(Calendar.DATE);
        Calendar res = getEuropeanCalendar();
        
        Calendar timeCalend = getEuropeanCalendar();
        timeCalend.setTime(time);
        
        res.set(year, month, day, timeCalend.get(Calendar.HOUR_OF_DAY), timeCalend.get(Calendar.MINUTE), 0);
        
        long timeTmp = res.getTimeInMillis();
        timeTmp = (timeTmp / 1000);
        timeTmp = timeTmp * 1000;
        return new Date(timeTmp);
    }
    
    /**
     * The first financial year can be up to 18 months,
     * the second, third etc. is always 12 months
     * The end of fiscal year is always end of month.
     * 
     * @param configuredStartDate
     * @param configuredDuration
     * @param forDate
     * @return
     */
	public static AETimePeriod getFinancialFrenchYearForDate(Date configuredStartDate, int configuredDuration, Date forDate) {
		if(configuredStartDate == null || configuredDuration < 1 || forDate == null || forDate.before(configuredStartDate)) {
			return null;
		}
		// detect the first financial year
		int duration = configuredDuration;
		Date startDate = configuredStartDate;
		Date secondFinancialYearStartDate = addDaysToDate(getEndFinancialYear(startDate, duration), 1);
		if(forDate.after(secondFinancialYearStartDate) || forDate.equals(secondFinancialYearStartDate)) {
			// after first financial year
			duration = 12;
			long wholePeriods = 
				AEDateUtil.getMonthsBetween(secondFinancialYearStartDate, forDate) / duration;
			startDate = AEDateUtil.addMonthsToDate(secondFinancialYearStartDate, (int) (wholePeriods * duration));
		}
		Date endDate = getEndFinancialYear(startDate, duration);
		
		// create current financial period
		AETimePeriod finacialPeriod = new AETimePeriod();
		finacialPeriod.setStartDate(startDate);
		finacialPeriod.setEndDate(endDate);
		finacialPeriod.setDuration(duration);
		finacialPeriod.setUnit(Calendar.MONTH);
		
		return finacialPeriod;
	}
	
	/**
	 * The end of fiscal year is always end of month.
	 * 
	 * @param startDate
	 * @param duration
	 * @return
	 */
	private static Date getEndFinancialYear(Date startDate, int duration) {
		return lastDayOfMonth(addDaysToDate(addMonthsToDate(startDate, duration), -1));
	}
	
	public static String formatToFrenchLong(Date date) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMMM yyyy", Locale.FRANCE);
		return dateFormat.format(date);
	}
	
	public static String formatToFrenchShort(Date date) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.FRANCE);
		return dateFormat.format(date);
	}
	
	/**
	 * Comparator class to be used for sorting by DateMidnight
	 */
	public static class DateMidnightComparator implements Comparator<DateMidnight>{
		@Override
		public int compare(DateMidnight d1, DateMidnight d2){
			if(d1.isBefore(d2)){
				return -1;
			} else if(d1.isAfter(d2)){
				return 1;
			} else {
				return 0;
			}
		}
	};
	
	public static DateMidnightComparator getDateMidnightComparator(){
		if(dateMidnightComparator == null){
			dateMidnightComparator = new DateMidnightComparator();
		}
		return dateMidnightComparator;
	}
}
