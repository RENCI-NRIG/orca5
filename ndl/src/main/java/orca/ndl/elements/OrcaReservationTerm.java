/*
* Copyright (c) 2011 RENCI/UNC Chapel Hill 
*
* @author Ilia Baldine
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
* and/or hardware specification (the "Work") to deal in the Work without restriction, including 
* without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or 
* sell copies of the Work, and to permit persons to whom the Work is furnished to do so, subject to 
* the following conditions:  
* The above copyright notice and this permission notice shall be included in all copies or 
* substantial portions of the Work.  
*
* THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
* NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
* HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
* OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS 
* IN THE WORK.
*/
package orca.ndl.elements;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * ORCA reservation term is described by a start time/date and duration (d,hr,min)
 * @author ibaldin
 *
 */
public class OrcaReservationTerm {
	protected Date start = null, end=null;
	protected int dDays, dHours, dMins, dSecs;
	
	/**
	 * Default is starting now for 24 hours
	 */
	public OrcaReservationTerm() {
		dDays = 0;
		dHours = 24;
		dMins = 0;
		dSecs = 0;
		start = new Date();
		_setEnd(dDays, dHours, dMins, dSecs);
	}
	
	/**
	 * set the end term based on duration
	 * @param d
	 * @param h
	 * @param m
	 * @param s
	 */
	private void _setEnd(int d, int h, int m, int s) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(start);

		cal.add(Calendar.DAY_OF_YEAR, d);
		cal.add(Calendar.HOUR, h);
		cal.add(Calendar.MINUTE, m);
		cal.add(Calendar.SECOND, s);
		end = cal.getTime();
	}
	
	public int getDurationInMinutes() {
                return dDays*24*60 + dHours *60 + dMins;
    }
	
	public int getDurationInSeconds() {
        return 60*getDurationInMinutes()+dSecs;
	}
	
	private int durationInMinutes(int d, int h, int m) {
		return d*24*60 + h *60 + m;
	}
	
	public void setStart(Date s) {
		start = s;
		// be sure to change end date as well
		_setEnd(dDays, dHours, dMins, dSecs);
	}
	
	public Date getStart() {
		return start;
	}
	
	public Date getEnd() {
		return end;
	}
	
	/**
	 * Set the duration. normalization must be performed explicitly (if desired)
	 * @param d
	 * @param h
	 * @param m
	 */
	public void setDuration(int d, int h, int m,int s) {
		if ((d < 0) || (h < 0) || (m < 0) || (s<0) )
			return;

		if((d == 0) && (h == 0) && (m == 0) && (s==0))
			return;
		
		if (start == null)
			return;
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(start);

		cal.add(Calendar.DAY_OF_YEAR, d);
		cal.add(Calendar.HOUR, h);
		cal.add(Calendar.MINUTE, m);
		cal.add(Calendar.SECOND, s);
		
		modifyTerm(cal.getTime());
	}
	
	public void modifyTerm(Date newEnd){
		if(start==null)
			start=new Date();
		long newDuration = newEnd.getTime() - start.getTime();

		dDays = (int) TimeUnit.MILLISECONDS.toDays(newDuration);
		newDuration -= TimeUnit.DAYS.toMillis(dDays);

		dHours = (int) TimeUnit.MILLISECONDS.toHours(newDuration);
		newDuration -= TimeUnit.HOURS.toMillis(dHours);

		dMins = (int) TimeUnit.MILLISECONDS.toMinutes(newDuration);
		newDuration -= TimeUnit.MINUTES.toMillis(dMins);

		dSecs =(int)  TimeUnit.MILLISECONDS.toSeconds(newDuration);
		end = newEnd;
	}
	
	/**
	 * Normalizes the duration values (hours < 24, minutes < 60)
	 */
	public void normalizeDuration() {
		if (durationInMinutes(dDays, dHours, dMins) == 0)
			dHours = 24;
		
		int tmpMins = dMins;
		dMins = tmpMins % 60;
		int tmpHours = (int)Math.floor((double)tmpMins / 60.0) + dHours;
		dHours = tmpHours % 24;
		dDays += (int)Math.floor((double)tmpHours / 24.0);
	}
	
	public int getDurationDays() {
		return dDays;
	}
	
	public int getDurationHours() {
		return dHours;
	}
	
	public int getDurationMins() {
		return dMins;
	}
	
	public int getDurationSecs() {
		return dSecs;
	}
	
	@Override
	public String toString() {
		return "start: " + start + " duration: " + dDays + " days " + dHours + " hours " + dMins + " minutes " + dSecs + " seconds end: " + end;
	}
}
