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

import java.util.Date;
import java.util.Properties;

import junit.framework.Assert;
import junit.framework.TestCase;
import orca.util.persistence.PersistenceUtils;


public class TermTest extends TestCase
{
    public TermTest()
    {
        // disable cycle setting, so that we do not
        // try to create a shirako container.
        Term.SetCycles = false;
    }

    /**
     * Tests <code>Term</code> creation.
     */
    public void testCreate()
    {
        Term t = null;
        Term t1 = null;

        long length = 500;
        long start = 1000;
        long newStart = 1500;
        long end = 1999;
        long oldEnd = 1499;

        // Term()
        t = new Term();
        check(t);

        // T(start)
        t = new Term(new Date(start));
        check(start, start, Long.MAX_VALUE, t);
        t1 = new Term(t);
        check(start, start, Long.MAX_VALUE, t1);

        // T(start, length);
        t = new Term(new Date(start), length);
        check(start, start, oldEnd, t);
        Assert.assertEquals(length, t.getLength());
        t1 = new Term(t);
        check(start, start, oldEnd, t1);

        // T(start, end)
        t = new Term(new Date(start), new Date(oldEnd));
        check(start, start, oldEnd, t);
        Assert.assertEquals(length, t.getLength());
        t1 = new Term(t);
        check(start, start, oldEnd, t1);

        // T(start, end, newStart)
        t = new Term(new Date(start), new Date(end), new Date(newStart));
        check(start, newStart, end, t);
        // NOTE: length is measured only relative to newStartTime!!!
        Assert.assertEquals(length, t.getLength());
        t1 = new Term(t);
        check(start, newStart, end, t1);
    }

    protected void check(Term t)
    {
        Assert.assertNull(t.startTime);
        Assert.assertNull(t.newStartTime);
        Assert.assertNull(t.endTime);
        Assert.assertNull(t.getStartTime());
        Assert.assertNull(t.getNewStartTime());
        Assert.assertNull(t.getEndTime());
    }

    protected void check(long start, long newStart, long end, Term t)
    {
        Assert.assertNotNull(t.startTime);
        Assert.assertNotNull(t.newStartTime);
        Assert.assertNotNull(t.endTime);
        Assert.assertNotNull(t.getStartTime());
        Assert.assertNotNull(t.getNewStartTime());
        Assert.assertNotNull(t.getEndTime());

        Assert.assertEquals(start, t.startTime.getTime());
        Assert.assertEquals(start, t.getStartTime().getTime());
        Assert.assertEquals(newStart, t.newStartTime.getTime());
        Assert.assertEquals(newStart, t.getNewStartTime().getTime());
        Assert.assertEquals(end, t.endTime.getTime());
        Assert.assertEquals(end, t.getEndTime().getTime());
    }

    /**
     * Tests shifting terms.
     */
    public void testShift()
    {
        long start = 1000;
        long end = 1499;
        long length = 500;

        Term t = new Term(new Date(start), new Date(end));
        Assert.assertEquals(length, t.getLength());

        Term t1 = t.shift(new Date(start - 500));
        check(start - 500, start - 500, end - 500, t1);
        Assert.assertEquals(length, t1.getLength());

        t1 = t.shift(new Date(start));
        check(start, start, end, t1);
        Assert.assertEquals(length, t1.getLength());

        t1 = t.shift(new Date(start + 500));
        check(start + 500, start + 500, end + 500, t1);
        Assert.assertEquals(length, t1.getLength());
    }

    /**
     * Tests changing term length.
     */
    public void testChange()
    {
        long start = 1000;
        long end = 1499;
        long length = 500;

        Term t = new Term(new Date(start), new Date(end));
        Assert.assertEquals(length, t.getLength());
        check(start, start, end, t);

        Term t1 = t.changeLength(2 * length);
        check(start, start, end + length, t1);
        Assert.assertEquals(2 * length, t1.getLength());

        t1 = t.changeLength(length / 2);
        check(start, start, end - (length / 2), t1);
        Assert.assertEquals(length / 2, t1.getLength());
    }

    /**
     * Checks term extension
     */
    public void testExtend()
    {
        long start = 1000;
        long end = 1499;
        long length = 500;

        Term t = new Term(new Date(start), new Date(end));
        Assert.assertEquals(length, t.getLength());
        check(start, start, end, t);

        // extend with same length
        Term t1 = t.extend();
        check(start, end + 1, end + length, t1);
        Assert.assertEquals(length, t1.getLength());
        Assert.assertEquals(2 * length, t1.getFullLength());
        Assert.assertTrue(t1.extendsTerm(t));

        // extend multiple times
        for (int i = 0; i < 10; i++) {
            Term t2 = t1.extend();
            check(t1.getStartTime().getTime(),
                  t1.getEndTime().getTime() + 1,
                  t1.getEndTime().getTime() + length,
                  t2);
            Assert.assertEquals(length, t2.getLength());
            Assert.assertEquals(t1.getFullLength() + length, t2.getFullLength());
            Assert.assertTrue(t2.extendsTerm(t1));
            t1 = t2;
        }

        // extend with 1000
        long l = 1000;
        t1 = t.extend(l);
        check(start, end + 1, end + l, t1);
        Assert.assertEquals(l, t1.getLength());
        Assert.assertEquals(l + length, t1.getFullLength());

        // extend multiple times
        for (int i = 0; i < 10; i++) {
            Term t2 = t1.extend();
            check(t1.getStartTime().getTime(),
                  t1.getEndTime().getTime() + 1,
                  t1.getEndTime().getTime() + l,
                  t2);
            Assert.assertEquals(l, t2.getLength());
            Assert.assertEquals(t1.getFullLength() + l, t2.getFullLength());
            Assert.assertTrue(t2.extendsTerm(t1));
            t1 = t2;
        }
    }

    /**
     * Tests serialization/de-serialization.
     * @throws Exception
     */
    public void testSaveReset() throws Exception
    {
        long start = 1000;
        long end = 1499;
        long length = 500;

        Term t = new Term(new Date(start), new Date(end));
        check(start, start, end, t);
        Assert.assertEquals(length, t.getLength());

        Properties p = PersistenceUtils.save(t);

        Term t1 = PersistenceUtils.restore(p);
        
        check(start, start, end, t1);
        Assert.assertEquals(length, t1.getLength());

        t = new Term();
        check(t);
        p = PersistenceUtils.save(t);

        t1 = PersistenceUtils.restore(p);
        check(t1);
    }

    /**
     * Tests:
     * <ol>
     *  <li>copy</li>
     *  <li>clone</li>
     * </ol>
     */
    public void testMisc()
    {
        long start = 1000;
        long end = 1499;
        long length = 500;

        Term t = new Term(new Date(start), new Date(end));
        check(start, start, end, t);
        Assert.assertEquals(length, t.getLength());

        Term t1 = t.copy();
        check(start, start, end, t1);
        Assert.assertEquals(length, t1.getLength());

        t1 = (Term) t.clone();
        check(start, start, end, t1);
        Assert.assertEquals(length, t1.getLength());
    }

    public void testContains()
    {
        long start = 1000;
        long end = 1499;
        long length = 500;

        Term t = new Term(new Date(start), new Date(end));
        check(start, start, end, t);
        Assert.assertEquals(length, t.getLength());
        // self -> true
        Assert.assertTrue(t.contains(t));
        // self - 1 from the right
        Assert.assertTrue(t.contains(new Term(new Date(start), length - 1)));
        // self + 1 from the left
        Assert.assertTrue(t.contains(new Term(new Date(start + 1), new Date(end))));
        // self +1 from the left, -1 from the right
        Assert.assertTrue(t.contains(new Term(new Date(start + 1), new Date(end - 1))));

        Assert.assertFalse(t.contains(new Term(new Date(start - 1), new Date(end))));
        Assert.assertFalse(t.contains(new Term(new Date(start - 100), new Date(start))));

        Assert.assertFalse(t.contains(new Term(new Date(start), new Date(end + 1))));
        Assert.assertFalse(t.contains(new Term(new Date(end), new Date(end + 100))));

        Assert.assertFalse(t.contains(new Term(new Date(start - 100), new Date(end + 100))));

        Assert.assertTrue(t.contains(new Date(start)));
        Assert.assertTrue(t.contains(new Date(end)));
        Assert.assertTrue(t.contains(new Date((start + end) / 2)));

        Assert.assertFalse(t.contains(new Date(start - 1)));
        Assert.assertFalse(t.contains(new Date(end + 1)));
    }

    public void testEnds()
    {
        long start = 1000;
        long end = 1499;
        long length = 500;

        Term t = new Term(new Date(start), new Date(end));
        check(start, start, end, t);
        Assert.assertEquals(length, t.getLength());
        // term cannot end before it started
        Assert.assertFalse(t.endsBefore(t.getStartTime()));
        Assert.assertFalse(t.expired(t.getStartTime()));
        Assert.assertTrue(t.endsAfter(t.getStartTime()));

        Assert.assertFalse(t.endsBefore(t.getEndTime()));
        Assert.assertFalse(t.endsAfter(t.getEndTime()));
        Assert.assertFalse(t.expired(t.getEndTime()));

        Assert.assertFalse(t.endsBefore(new Date(start - 100)));
        Assert.assertTrue(t.endsAfter(new Date(start - 100)));
        Assert.assertFalse(t.expired(new Date(start - 100)));

        Assert.assertTrue(t.endsBefore(new Date(end + 1)));
        Assert.assertTrue(t.expired(new Date(end + 1)));
        Assert.assertFalse(t.endsAfter(new Date(end + 1)));
    }

    public void testEquals()
    {
        long start = 1000;
        long end = 1499;
        long length = 500;

        Term t = new Term(new Date(start), new Date(end));
        check(start, start, end, t);
        Assert.assertEquals(length, t.getLength());

        Assert.assertEquals(t, t);
        Assert.assertEquals(t, t.clone());
        Assert.assertEquals(t, t.copy());

        Term t1 = new Term(new Date(start), new Date(end));
        check(start, start, end, t1);
        Assert.assertEquals(length, t1.getLength());

        Assert.assertEquals(t, t1);
        Assert.assertFalse(t.equals(null));
        Assert.assertFalse(t.equals(new Object()));
        Assert.assertFalse(t.equals(new Term()));
    }

    public void testValidate() throws Exception
    {
        checkValid(new Term(new Date(10), new Date(100), new Date(10)));
        checkValid(new Term(new Date(10), new Date(100), new Date(99)));
        checkValid(new Term(new Date(100), new Date(1000), new Date(1000)));

        checkNotValid(new Term());
        checkNotValid(new Term(new Date(100), new Date(10)));
        checkNotValid(new Term(new Date(100), new Date(100)));
        checkNotValid(new Term(new Date(100), new Date(1000), new Date(10)));
        checkNotValid(new Term(new Date(100), new Date(1000), new Date(10000)));
    }

    public void checkValid(Term t) throws Exception
    {
        t.validate();
    }

    public void checkNotValid(Term t) throws Exception
    {
        boolean failed = false;

        try {
            t.validate();
        } catch (Exception e) {
            failed = true;
        }

        Assert.assertTrue(failed);
    }
}