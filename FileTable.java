import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by mahokyin on 5/28/17.
 */
public class FileTable {

    private static final int UNUSED = 0;
    private static final int USED = 1;
    private static final int READ = 2;
    private static final int WRITE = 3;

    private Vector<FileTableEntry> table;         // the actual entity of this file table
    private Directory dir;        // the root directory

    public FileTable( Directory directory ) { // constructor
        table = new Vector<>();     // instantiate a file (structure) table
        dir = directory;           // receive a reference to the Director
    }                             // from the file system

    // major public methods
    public synchronized FileTableEntry falloc( String filename, String mode ) {
        // allocate a new file (structure) table entry for this file name
        // allocate/retrieve and register the corresponding inode using dir
        // increment this inode's count
        // immediately write back this inode to the disk
        // return a reference to this file (structure) table entry
    }

    public synchronized boolean ffree(FileTableEntry tableEntry) {
        // receive a file table entry reference
        // save the corresponding inode to the disk
        // free this file table entry.
        // return true if this file table entry found in my table

        // Check if the obj is null first
        if (tableEntry == null) {
            return true;
        }

        // the object was not found in my table
        if (!table.removeElement(tableEntry)) {
            return false;
        }

        // receive a file table entry reference
        Inode inode = new Inode(tableEntry.iNumber);

        if (inode.count == 0) {
            inode.flag = UNUSED;
        }

        // notify waiting threads
        if (inode.flag == READ || inode.flag == WRITE)
            notify();

        if (inode.count > 0) {
            inode.count--;
        }

        // save the corresponding iNode to the disk
        inode.toDisk(tableEntry.iNumber);

        return true;
    }

    public synchronized boolean fempty( ) {
        return table.isEmpty( );  // return if table is empty
    }
}
