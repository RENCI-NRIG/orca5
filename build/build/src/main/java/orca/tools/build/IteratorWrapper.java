package orca.tools.build;

import org.apache.tools.ant.types.resources.FileResource;

import java.util.Iterator;

class IteratorWrapper
    implements Iterator
{
    Iterator iter;
    Object next;

    public IteratorWrapper( Iterator iter )
    {
        this.iter = iter;
    }

    public boolean hasNext(  )
    {
        boolean result = true;
        boolean repeat = true;

        while ( repeat )
        {
            result = iter.hasNext(  );

            if ( result )
            {
                next = iter.next(  );

                if ( next != null )
                {
                    if ( next instanceof FileResource )
                    {
                        FileResource f = (FileResource) next;

                        //System.out.println("testing: "  + f.getFile().getAbsolutePath());
                        if ( f.getFile(  ).getAbsolutePath(  ).toString(  ).endsWith( ".pom" ) )
                        {
                            //System.out.println("skipping");
                            next = null;
                            result = false;

                            continue;
                        }
                    }
                }
            }

            repeat = false;
        }

        return result;
    }

    public Object next(  )
    {
        return next;
    }

    public void remove(  )
    {
        iter.remove(  );
    }
}
