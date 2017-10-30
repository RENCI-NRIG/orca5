package orca.ndl.elements;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class OrcaReservationTermTest {

    private static final long TWO_WEEKS = TimeUnit.DAYS.toMillis(14);
    public static final long DEFAULT_MAX_DURATION = TWO_WEEKS;

    @Test
    public void getDurationInMinutes() throws Exception {
        OrcaReservationTerm term = new OrcaReservationTerm();
        term.setStart(new Date());
        term.setDuration(Math.toIntExact(TimeUnit.MILLISECONDS.toDays(DEFAULT_MAX_DURATION)), 0, 0, 0);

        final int durationInMinutes = term.getDurationInMinutes();

        assertEquals("Duration in Minutes did not match expected", TimeUnit.MILLISECONDS.toMinutes(DEFAULT_MAX_DURATION), durationInMinutes);
    }

    @Test
    public void getDurationInSeconds() throws Exception {
        OrcaReservationTerm term = new OrcaReservationTerm();
        term.setStart(new Date());
        term.setDuration(Math.toIntExact(TimeUnit.MILLISECONDS.toDays(DEFAULT_MAX_DURATION)), 0, 0, 0);

        final int durationInSeconds = term.getDurationInSeconds();

        assertEquals("Duration in Seconds did not match expected", TimeUnit.MILLISECONDS.toSeconds(DEFAULT_MAX_DURATION), durationInSeconds);
    }

    @Test
    public void setDurationInDays() throws Exception {
        OrcaReservationTerm term = new OrcaReservationTerm();
        term.setStart(new Date());
        term.setDuration(Math.toIntExact(TimeUnit.MILLISECONDS.toDays(DEFAULT_MAX_DURATION)), 0, 0, 0);

        final Date termEnd = term.end;

        Calendar termStart = Calendar.getInstance();
        termStart.setTime(term.start);

        termStart.add(Calendar.MILLISECOND, Math.toIntExact(DEFAULT_MAX_DURATION));

        assertEquals("Term end did not match expected value.", termStart.getTime(), termEnd);
    }

    @Test
    public void setDurationInHours() throws Exception {
        OrcaReservationTerm term = new OrcaReservationTerm();
        term.setStart(new Date());
        term.setDuration(0, Math.toIntExact(TimeUnit.MILLISECONDS.toHours(DEFAULT_MAX_DURATION)), 0, 0);

        final Date termEnd = term.end;

        Calendar termStart = Calendar.getInstance();
        termStart.setTime(term.start);

        termStart.add(Calendar.MILLISECOND, Math.toIntExact(DEFAULT_MAX_DURATION));

        assertEquals("Term end did not match expected value.", termStart.getTime(), termEnd);
    }

    @Test
    public void setDurationInMinutes() throws Exception {
        OrcaReservationTerm term = new OrcaReservationTerm();
        term.setStart(new Date());
        term.setDuration(0, 0, Math.toIntExact(TimeUnit.MILLISECONDS.toMinutes(DEFAULT_MAX_DURATION)), 0);

        final Date termEnd = term.end;

        Calendar termStart = Calendar.getInstance();
        termStart.setTime(term.start);

        termStart.add(Calendar.MILLISECOND, Math.toIntExact(DEFAULT_MAX_DURATION));

        assertEquals("Term end did not match expected value.", termStart.getTime(), termEnd);
    }

    @Test
    public void setDurationInSeconds() throws Exception {
        OrcaReservationTerm term = new OrcaReservationTerm();
        term.setStart(new Date());
        term.setDuration(0, 0, 0, Math.toIntExact(TimeUnit.MILLISECONDS.toSeconds(DEFAULT_MAX_DURATION)));

        final Date termEnd = term.end;

        Calendar termStart = Calendar.getInstance();
        termStart.setTime(term.start);

        termStart.add(Calendar.MILLISECOND, Math.toIntExact(DEFAULT_MAX_DURATION));

        assertEquals("Term end did not match expected value.", termStart.getTime(), termEnd);
    }

}
