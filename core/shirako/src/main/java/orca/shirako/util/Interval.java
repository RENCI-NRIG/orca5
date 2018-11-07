package orca.shirako.util;

/**
 * <code>Interval</code> represents a simple interval. The interval is closed on
 * both ends.
 * @author aydan
 */
public class Interval
{
    /**
     * Beginning of the interval.
     */
    protected long start;
    /**
     * End of the interval.
     */
    protected long end;

    /**
     * Copy constructor.
     * @param interval interval to copy
     */
    public Interval(Interval interval)
    {
        this(interval.start, interval.end);
    }

    /**
     * Creates a new interval.
     * @param start start of the interval
     * @param end end of the interval
     */
    public Interval(long start, long end)
    {
        if (end < start) {
            throw new IllegalArgumentException("end must not be greater than start");
        }
        this.start = start;
        this.end = end;
    }

    /**
     * Checks if this interval intersects the passed interval.
     * @param other interval to check
     * @return true if the intervals intersect, false otherwise
     */
    public boolean intersects(Interval other)
    {
        return ((this.end >= other.start) && (other.end >= this.start));
    }

    /**
     * Returns an interval representing the intersection of this interval and the passed interval.
     * @param other interval to check for intersection
     * @return intersection interval or null when the intervals do not intersect
     */
    public Interval getIntersection(Interval other)
    {
        if (this.start < other.start) {
            if (this.end < other.start) {
                return null;
            } else {
                return new Interval(other.start, Math.min(this.end, other.end));
            }
        } else {
            if (this.start > other.end) {
                return null;
            } else {
                return new Interval(this.start, Math.min(this.end, other.end));
            }
        }
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof Interval)) {
            return false;
        }

        Interval i = (Interval) other;
        return ((this.start == i.start) && (this.end == i.end));
    }

    @Override
    public String toString()
    {
        return "[" + start + "," + end + "]";
    }
}
