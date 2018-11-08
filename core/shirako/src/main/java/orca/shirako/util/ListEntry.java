package orca.shirako.util;

/**
 * <code>ListEntry</code> represents a simple double linked list.
 * @author aydan
 *
 */
public class ListEntry
{
    /**
     * Next entry in the list.
     */
    protected ListEntry next;
    /**
     * Previous entry in the list.
     */
    protected ListEntry previous;
    
    /**
     * Initializes the head of a linked list.
     * @param list list
     */
    public static void InitializeListHead(ListEntry list)    
    {
        assert list != null;
        
        list.next = list;
        list.previous = list;
    }
    
    /**
     * Inserts entry before the list head.
     * @param head list head
     * @param entry entry to insert
     */
    public static void InsertBefore(ListEntry head, ListEntry entry)
    {   
        assert head != null;
        assert entry != null;
        
        entry.next = head; 
        entry.previous = head.previous;
     
        head.previous.next = entry;
        head.previous = entry;
    }
    
    /**
     * Inserts entry after the list tail.
     * @param tail tail of the list
     * @param entry entry to insert
     */
    public static void InsertAfter(ListEntry tail, ListEntry entry)
    {
        assert tail != null;
        assert entry != null;
        
        entry.next = tail.next;
        entry.previous = tail;
        
        tail.next.previous = entry;
        tail.next = entry;
    }
    
    /**
     * Removes the specified entry from the linked list.
     * @param entry entry to remove
     */
    public static void RemoveEntry(ListEntry entry)
    {
        assert entry != null;
        assert entry.previous != null;
        assert entry.next != null;
        
        entry.previous.next = entry.next;
        entry.next.previous = entry.previous;
        
        entry.previous = null;
        entry.next = null;
    }
}

