package orca.tools.build;

import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.selectors.FileSelector;

import java.util.Iterator;
import java.util.Vector;

public class FileSetNoPom extends FileSet {
    protected FileSet fset;
    protected Path path;

    public FileSetNoPom() {
    }

    public void add(FileSet fset) {
        this.fset = fset;
    }

    @Override
    public Iterator iterator() {
        if (fset != null) {
            return new IteratorWrapper(fset.iterator());
        }

        return null;
    }
}
