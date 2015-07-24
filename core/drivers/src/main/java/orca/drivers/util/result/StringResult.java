/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.drivers.util.result;

public class StringResult extends Result
{
    protected String result;

    public StringResult(int code, String result)
    {
        super(code);
        this.result = result;
    }

    public String getResult()
    {
        return result;
    }
}