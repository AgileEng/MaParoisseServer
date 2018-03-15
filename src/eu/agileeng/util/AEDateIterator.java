package eu.agileeng.util;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

public class AEDateIterator implements Iterator<Date>, Iterable<Date> {

    private Calendar end = AEDateUtil.getEuropeanCalendar();

    private Calendar current = AEDateUtil.getEuropeanCalendar();

    public AEDateIterator(Date start, Date end) {
        this.end.setTime(AEDateUtil.getClearDate(end));
        this.end.add(Calendar.DATE, -1);
        this.current.setTime(AEDateUtil.getClearDate(start));
        this.current.add(Calendar.DATE, -1);
    }

    public boolean hasNext() {
        return !current.after(end);
    }

    public Date next() {
        current.add(Calendar.DATE, 1);
        return current.getTime();
    }

    public void remove() {
        throw new UnsupportedOperationException("Cannot remove");
    }

    public Iterator<Date> iterator() {
        return this;
    }
}