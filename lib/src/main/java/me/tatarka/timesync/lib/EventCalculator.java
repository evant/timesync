package me.tatarka.timesync.lib;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static me.tatarka.timesync.lib.MathUtil.divCeil;

class EventCalculator {
    /**
     * Calculate when to fire the next event. This should happen at the next timestamp which is a
     * multiple of the given interval since midnight in the current timezone. Both the arguments and
     * the result are in unix time milliseconds (equivalent to {@link System#currentTimeMillis()}).
     *
     * @param currentTime the time to start from, the result will be the next event after this time
     * @param interval    the interval
     * @return the next event time.
     */
    public static long getNextEvent(long currentTime, long interval) {
        if (interval == 0) return currentTime;

        long startTime = getPreviousMidnight(currentTime);
        long span = currentTime - startTime;

        long result;
        if (span == 0) {
            // It's midnight!
            result = startTime + interval;
        } else {
            result = startTime + divCeil(span, interval) * interval;
        }
        return result;
    }

    private static long getPreviousMidnight(long currentTime) {
        Calendar date = new GregorianCalendar();
        date.setTimeInMillis(currentTime);
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        return date.getTimeInMillis();
    }
}
