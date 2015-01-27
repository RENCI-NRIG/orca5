package orca.shirako.util;

import orca.util.ErrorUtils;
import orca.util.persistence.Persistable;
import orca.util.persistence.Persistent;

public class Notice implements Persistable
{
	@Persistent
    protected String noticeString;
    
    public Notice() {
    }
    
    public Notice(Notice other) {
        this.noticeString = other.noticeString;
    }
    
    public Notice(String noticeString)
    {
        this.noticeString = noticeString;
    }
    
    public String getNotice() {
        return noticeString;
    }
    
    public void add(String other) {
        if (other == null) {
            return;
        }
        if (noticeString == null) {
            noticeString = other;
        }else {
            noticeString += "\n" + other;
        }
    }

    public void add(String msg, Exception e) {
        add(msg);
        if (e != null) {
            add(ErrorUtils.getStackTrace(e));
        }
    }
 
    public void add(Notice other) {
        if (!other.isEmpty()) {
            add(other.noticeString);
        }
    }
    
    public void clear() {
        noticeString = null;
    }
    
    public boolean isEmpty() {
        return noticeString == null;
    }
    
    public String toString() {
    	return noticeString;
    }
}
        