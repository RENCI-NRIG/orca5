package orca.ndl.elements;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class OrcaReservationTermTest {

    public static final int NUM_DAYS = 1;

    @Test
    public void getDurationInMinutes() throws Exception {
        OrcaReservationTerm term = new OrcaReservationTerm();
        term.setStart(new Date());
        term.setDuration(NUM_DAYS, 0, 0, 0);

        final int durationInMinutes = term.getDurationInMinutes();

        assertEquals("Duration in Minutes did not match expected", TimeUnit.DAYS.toMinutes(NUM_DAYS),
                durationInMinutes);
    }

    @Test
    public void getDurationInSeconds() throws Exception {
        OrcaReservationTerm term = new OrcaReservationTerm();
        term.setStart(new Date());
        term.setDuration(NUM_DAYS, 0, 0, 0);

        final int durationInSeconds = term.getDurationInSeconds();

        assertEquals("Duration in Seconds did not match expected", TimeUnit.DAYS.toSeconds(NUM_DAYS),
                durationInSeconds);
    }

    @Test
    public void setDurationInDays() throws Exception {
        OrcaReservationTerm term = new OrcaReservationTerm();
        term.setStart(new Date());
        term.setDuration(NUM_DAYS, 0, 0, 0);

        final Date termEnd = term.end;

        Calendar termStart = Calendar.getInstance();
        termStart.setTime(term.start);

        termStart.add(Calendar.DAY_OF_YEAR, NUM_DAYS);

        assertEquals("Term end did not match expected value.", termStart.getTime(), termEnd);
    }

    @Test
    public void setDurationInHours() throws Exception {
        OrcaReservationTerm term = new OrcaReservationTerm();
        term.setStart(new Date());
        term.setDuration(0, Math.toIntExact(TimeUnit.DAYS.toHours(NUM_DAYS)), 0, 0);

        final Date termEnd = term.end;

        Calendar termStart = Calendar.getInstance();
        termStart.setTime(term.start);

        termStart.add(Calendar.DAY_OF_YEAR, NUM_DAYS);

        assertEquals("Term end did not match expected value.", termStart.getTime(), termEnd);
    }

    @Test
    public void setDurationInMinutes() throws Exception {
        OrcaReservationTerm term = new OrcaReservationTerm();
        term.setStart(new Date());
        term.setDuration(0, 0, Math.toIntExact(TimeUnit.DAYS.toMinutes(NUM_DAYS)), 0);

        final Date termEnd = term.end;

        Calendar termStart = Calendar.getInstance();
        termStart.setTime(term.start);

        termStart.add(Calendar.DAY_OF_YEAR, NUM_DAYS);

        assertEquals("Term end did not match expected value.", termStart.getTime(), termEnd);
    }

    @Test
    public void setDurationInSeconds() throws Exception {
        OrcaReservationTerm term = new OrcaReservationTerm();
        term.setStart(new Date());
        term.setDuration(0, 0, 0, Math.toIntExact(TimeUnit.DAYS.toSeconds(NUM_DAYS)));

        final Date termEnd = term.end;

        Calendar termStart = Calendar.getInstance();
        termStart.setTime(term.start);

        termStart.add(Calendar.DAY_OF_YEAR, NUM_DAYS);

        assertEquals("Term end did not match expected value.", termStart.getTime(), termEnd);
    }

}
