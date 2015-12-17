/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.time;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import orca.shirako.container.Globals;
import orca.shirako.container.api.IActorContainer;
import orca.util.PropList;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.Persistable;
import orca.util.persistence.Persistent;


/**
 * Represents the term for a ticket or lease (i.e., a "reservation"). A term
 * specifies the range of time units during which the term is valid.
 * <p>
 * A term consists of start and end time. The interval represented by a term is
 * closed on both ends. The term's <code>newStart</code> field represents the
 * start time of the latest term extension. For extended terms
 * <code>start</code> is constant, while <code>newStart</code> and
 * <code>end</code> change to reflect the extensions.
 * </p>
 * <p>
 * The length of a term is measured as the number of milliseconds in the
 * <bold>closed</bold> interval [<code>newStart</code>,<code>end</code>].
 * This number will be returned when calling {@link #getLength}. To obtain the
 * number of milliseconds in the closed interval [<code>start</code>,<code>end</code>],
 * use {@link #getFullLength()}.
 */
public class Term implements Cloneable, Persistable
{
    /**
     * Flag that controls, whether cycle numbers should be calculated.
     */
    public static boolean SetCycles = true;

    public static final String PropertyStartTime = "TermStartTime";
    public static final String PropertyEndTime = "TermEndTime";
    public static final String PropertyNewStartTime = "TermNewStartTime";

    /**
     * Date format string to use when displaying dates.
     */
    protected static SimpleDateFormat readable = new SimpleDateFormat(
        "EEEE, MMMM d, yyyy 'at' hh:mm:ss a zzz");

    /**
     * Cached reference to the actor clock.
     */
    // FIXME: wrong! get rid of this
    public static ActorClock clock;

    /**
     * Computes the difference in milliseconds between two dates.
     *
     * @param d1
     * @param d2
     *
     * @return difference in milliseconds
     */
    private static long delta(Date d1, Date d2)
    {
        long d1ms = d1.getTime();
        long d2ms = d2.getTime();

        return (d2ms - d1ms);
    }

    /**
     * Returns a readable date.
     *
     * @param date date
     *
     * @return a string of a date that is readable
     */
    public static String getReadableDate(final Date date)
    {
        if (date == null) {
            return "(null)";
        }

        return readable.format(date);
    }

    /**
     * Logs a comparison between two terms.
     *
     * @param t1 first term
     * @param t2 second term
     * @param logger logger
     */
    public static void logComparison(final Term t1, final Term t2)
    {
        StringBuffer sb = new StringBuffer();
        sb.append("");

        if (t1.startTime.equals(t2.startTime)) {
            sb.append("[same start time]");
        }

        if (t1.startTime.before(t2.startTime)) {
            sb.append("[t2 starts later by ");
            sb.append(Long.toString(delta(t1.startTime, t2.startTime)) + "]");
        }

        if (t1.startTime.after(t2.startTime)) {
            sb.append("[t2 starts earlier by ");
            sb.append(Long.toString(delta(t2.startTime, t1.startTime)) + "]");
        }

        if (t1.endTime.equals(t2.endTime)) {
            sb.append("[same end time]");
        }

        if (t1.endTime.before(t2.endTime)) {
            sb.append("[t2 ends later by ");
            sb.append(Long.toString(delta(t1.endTime, t2.endTime)) + "]");
        }

        if (t1.endTime.after(t2.endTime)) {
            sb.append("[t2 ends earlier by ");
            sb.append(Long.toString(delta(t2.endTime, t1.endTime)) + "]");
        }

        Globals.Log.info(sb.toString());
    }

    /**
     * Sets the clock to use when computing cycles.
     * @param clock clock instance
     */
    public static void setClock(final ActorClock clock)
    {
        Term.clock = clock;
    }

    /**
     * Start time: first valid millisecond.
     */
    @Persistent (key = PropertyStartTime)
    protected Date startTime;

    /**
     * End time: last valid millisecond.
     */
    @Persistent (key = PropertyEndTime)
    protected Date endTime;

    /**
     * Start time for this section of the lease
     */
    @Persistent (key = PropertyNewStartTime)
    protected Date newStartTime;

    /*
     * These are for debugging purposes. They should not be used unless it's for
     * debugging and may be removed without harming the system.
     */

    /**
     * Start cycle. Used only for debugging.
     */
    @NotPersistent
    private long cycleStart;

    /**
     * End cycle. Used only for debugging.
     */
    @NotPersistent
    private long cycleEnd;

    /**
     * New start cycle. Used only for debugging.
     */
    @NotPersistent
    private long cycleNewStart;

    /**
     * Default constructor: creates an empty term.
     */
    public Term()
    {
    }

    /**
     * Creates a "forever" term.
     * @param start start time
     */
    public Term(final Date start)
    {
        this(start, new Date(Long.MAX_VALUE), start);
    }

    /**
     * Creates a new term.
     * @param start start time
     * @param end end time
     */
    public Term(final Date start, final Date end)
    {
        this(start, end, start);
    }

    /**
     * Creates a new term.
     * @param start start time
     * @param end end time
     * @param newStart new start time
     */
    public Term(final Date start, final Date end, final Date newStart)
    {
        this.startTime = start;
        this.endTime = end;
        this.newStartTime = newStart;

        setCycles();
    }

    /**
     * Creates a new term starting at the specified time with the given
     * duration.
     * @param start start time
     * @param length duration in milliseconds
     */
    public Term(final Date start, final long length)
    {
        if ((length < 1) || (start == null)) {
            throw new IllegalArgumentException();
        }

        startTime = start;
        endTime = new Date((start.getTime() + length) - 1);
        newStartTime = start;

        setCycles();
    }

    /**
     * Copy constructor: makes a copy of the given term.
     * @param term term to copy
     */
    public Term(final Term term)
    {
        if (term == null) {
            throw new IllegalArgumentException();
        }

        if (term.startTime != null) {
            this.startTime = (Date) term.startTime.clone();
        }

        if (term.newStartTime != null) {
            this.newStartTime = (Date) term.newStartTime.clone();
        }

        if (term.endTime != null) {
            this.endTime = (Date) term.endTime.clone();
        }

        setCycles();
    }

    /**
     * Creates a new term from the term. The new term has the same start time
     * but different length.
     *
     * @param length new term length (milliseconds)
     *
     * @return term starting at the same time as this term but with the
     *         specified length
     */
    public Term changeLength(final long length)
    {
        return new Term(startTime, length);
    }

    /**
     * {@inheritDoc}
     */
    public Object clone()
    {
        return new Term(this);
    }

    /**
     * Checks if the term contains the given date.
     *
     * @param date the date to check
     *
     * @return true if the term contains the given date; false otherwise
     */
    public boolean contains(final Date date)
    {
        if (date == null) {
            throw new IllegalArgumentException();
        }

        if ((startTime == null) || (endTime == null)) {
            throw new IllegalStateException();
        }

        return !startTime.after(date) && !endTime.before(date);
    }

    /**
     * Checks if the current term contains the given term.
     *
     * @param term the <code>Term</code> to check
     *
     * @return true if the current term contains the given term; false otherwise
     */
    public boolean contains(Term term)
    {
        if (term == null) {
            throw new IllegalArgumentException();
        }

        if ((startTime == null) || (endTime == null)) {
            throw new IllegalStateException();
        }

        return !startTime.after(term.startTime) && !endTime.before(term.endTime);
    }

    /**
     * Returns a copy of the <code>Term</code>.
     *
     * @return the copied <code>Term</code>
     */
    public Term copy()
    {
        return new Term(this);
    }

    /**
     * Checks if the term ends after the given date.
     *
     * @param date date to check against
     *
     * @return true if the term ends before the given date
     */
    public boolean endsAfter(Date date)
    {
        if (date == null) {
            throw new IllegalArgumentException();
        }

        if (endTime == null) {
            throw new IllegalStateException();
        }

        return endTime.after(date);
    }

    /**
     * Checks if the term ends before the given date.
     * This method is equivalent to {@link #expired(Date)}.
     * @param date date to check against
     *
     * @return true if the term ends before the given date
     */
    public boolean endsBefore(Date date)
    {
        if (date == null) {
            throw new IllegalArgumentException();
        }

        if (endTime == null) {
            throw new IllegalStateException();
        }

        return endTime.before(date);
    }

    /**
     * Checks if this term extends the old one. In case this term does not
     * extend old term, logs the error and throws an exception.
     *
     * @param oldTerm old term
     *
     * @throws Exception if this term does not extend the old term
     */
    public void enforceExtendsTerm(Term oldTerm) throws Exception
    {
        boolean flag = extendsTerm(oldTerm);

        if (!flag) {
            Globals.Log.error("Updated term t2 does not extend current term t1");
            logComparison(oldTerm, this);
            throw new Exception("New term does not extend previous term");
        }
    }

    /**
     * Compares two terms.
     *
     * @param other other term to compare to
     *
     * @return true if both terms are equal
     */
    public boolean equals(final Object other)
    {
        if (other instanceof Term) {
            Term otherTerm = (Term) other;

            if (otherTerm == null) {
                return false;
            }

            if ((startTime != null) && !startTime.equals(otherTerm.startTime)) {
                return false;
            }

            if ((endTime != null) && !endTime.equals(otherTerm.endTime)) {
                return false;
            }

            if ((newStartTime != null) && !newStartTime.equals(otherTerm.newStartTime)) {
                return false;
            }

            return true;
        }

        return false;
    }

    /**
     * Checks if the term's expiration date is before the specified time.
     *
     * @param time the time to check against
     *
     * @return true if the term expires before the specified time
     */
    public boolean expired(Date time)
    {
        if (time == null) {
            throw new IllegalArgumentException();
        }

        if (endTime == null) {
            throw new IllegalStateException();
        }

        return endTime.before(time);
    }

    /**
     * Creates a new term as an extension of the specified term. The term is
     * extended with the current term length.
     *
     * @return term extended with the current term length
     */
    public Term extend()
    {
        if ((startTime == null) || (endTime == null)) {
            throw new IllegalStateException();
        }

        Date newStart = new Date(endTime.getTime() + 1);
        Date end = new Date((newStart.getTime() + getLength()) - 1);

        return new Term(startTime, end, newStart);
    }

    /**
     * Creates a new term as an extension of the specified term. The term is
     * extended with the specified duration.
     *
     * @param length new term length
     *
     * @return term extended with the specified length
     */
    public Term extend(final long length)
    {
        if ((startTime == null) || (endTime == null)) {
            throw new IllegalStateException();
        }

        Date newStart = new Date(endTime.getTime() + 1);
        Date end = new Date((newStart.getTime() + length) - 1);

        return new Term(startTime, end, newStart);
    }

    /**
     * Checks if this term extends the old term.
     *
     * @param oldTerm old term to check against
     *
     * @return true if this term extends the old one, false if not
     */
    public boolean extendsTerm(Term oldTerm)
    {
        if ((oldTerm == null) || (oldTerm.startTime == null) || (oldTerm.endTime == null)) {
            throw new IllegalArgumentException();
        }

        if ((startTime == null) || (newStartTime == null) || (endTime == null)) {
            throw new IllegalStateException();
        }

        // anirban@ 04/07/15
        // For extend to trigger as soon as request comes to the core, we are removing the constraint 
        // that the new start time for the reservation has to be beyond the old term's end time
        
        //return (startTime.equals(oldTerm.startTime) && newStartTime.after(oldTerm.endTime) &&
        //       endTime.after(newStartTime));
        
        return (startTime.equals(oldTerm.startTime) &&
                endTime.after(newStartTime));
        
    }

    /**
     * Returns the clock factory in use in this container.
     *
     * @return clock factory
     */
    private ActorClock getClock()
    {
        if (clock == null) {
            IActorContainer cm = Globals.getContainer();

            if (cm != null) {
                clock = cm.getActorClock();
            }

            if (clock == null) {
                throw new RuntimeException("Failed to obtain the actor clock from the container");
            }
        }

        return clock;
    }

    /**
     * Returns the expiration time for the term.
     *
     * @return expiration time
     */
    public Date getEndTime()
    {
        return endTime;
    }

    /**
     * Returns the full length of a term in milliseconds. The full length of a
     * term is the number of milliseconds in the closed interval [<code>start</code>,
     * <code>end</code>].
     *
     * @return term length
     * @see #getLength()
     */
    public long getFullLength()
    {
        if ((endTime == null) || (startTime == null)) {
            throw new IllegalStateException();
        }

        return endTime.getTime() - startTime.getTime() + 1;
    }

    /**
     * Returns the length of a term in milliseconds. The length of a term is the
     * number of milliseconds in the closed interval [<code>newStart</code>,
     * <code>end</code>].
     *
     * @return term length
     * @see #getFullLength()
     */
    public long getLength()
    {
        if ((endTime == null) || (newStartTime == null)) {
            throw new IllegalStateException();
        }

        return endTime.getTime() - newStartTime.getTime() + 1;
    }

    /**
     * Returns the new start time for the term.
     *
     * @return new start time
     */
    public Date getNewStartTime()
    {
        return newStartTime;
    }

    /**
     * Returns the start time for the term.
     *
     * @return start time
     */
    public Date getStartTime()
    {
        return startTime;
    }

    /**
     * Sets cycles.
     */
    private void setCycles()
    {
        if (SetCycles) {
            ActorClock clock = getClock();

            if (startTime != null) {
                cycleStart = clock.cycle(startTime);
            }

            if (endTime != null) {
                cycleEnd = clock.cycle(endTime);
            }

            if (newStartTime != null) {
                cycleNewStart = clock.cycle(newStartTime);
            }
        }
    }

    /**
     * Sets the end time.
     *
     * @param date end time
     */
    public void setEndTime(final Date date)
    {
        endTime = date;
        setCycles();
    }

    /**
     * Set the new start time.
     *
     * @param date new start time
     */
    public void setNewStartTime(final Date date)
    {
        newStartTime = date;
        setCycles();
    }

    /**
     * Set the start time.
     *
     * @param date start time
     */
    public void setStartTime(final Date date)
    {
        startTime = date;
		setCycles();
    }

    /**
     * Creates a new term from the term. The new term is shifted in time to
     * start at the specified start time.
     *
     * @param newStartDate start time
     *
     * @return term starting at the specified time, with the length of the
     *         current term.
     */
    public Term shift(final Date newStartDate)
    {
        if (newStartDate == null) {
            throw new IllegalArgumentException();
        }

        return new Term(newStartDate, getLength());
    }

    /**
     * Returns a readable string representation of this term.
     *
     * @return
     */
    public String toReadableString()
    {
        return new String(
            "Start: " + Term.getReadableDate(startTime) + "End: " + Term.getReadableDate(endTime));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        if (SetCycles) {
            return "term=[" + cycleStart + ":" + cycleNewStart + ":" + cycleEnd + "]";
        } else {
            return toReadableString();
        }
    }

    /**
     * Validates the term.
     * <p>
     * Note: does not check if the term has expired
     * </p>
     *
     * @throws Exception thrown if invalid start time for term thrown if invalid
     *             end time for term thrown if negative duration for term
     */
    public void validate() throws Exception
    {
        if (startTime == null) {
            throw new Exception("invalid start time for term");
        }

        if (endTime == null) {
            throw new Exception("invalid end time for term");
        }

        if (endTime.before(startTime)) {
            throw new Exception("negative duration for term");
        }

        if (endTime.equals(startTime)) {
            throw new Exception("zero duration for term");
        }

        if (startTime.getTime() > endTime.getTime()) {
            throw new Exception("Start after end");
        } else if (newStartTime.getTime() < startTime.getTime()) {
            throw new Exception("New start before start");
        } else if (newStartTime.getTime() > endTime.getTime()) {
            throw new Exception("New start after end");
        }
    }
}