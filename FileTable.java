import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by mahokyin on 5/28/17.
 */
public class FileTable {

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

        short iNumber; // inode number
        Inode inode; // Inode reference

        while (true) {
            // get the inumber form the directory method
            iNumber = (filename.equals("/") ? (short) 0 : dir.namei(filename));

            // if the Inode for the given file exist
            if (iNumber >= 0) {
                inode = new Inode(iNumber);

                // if the file is requesting for reading
                if (mode.equals("r")) {

                    // and its flag is read or used or unused
                    if (inode.flag == inode.READ || inode.flag == inode.USED || inode.flag == inode.UNUSED) {

                        // change the flag of the node to read
                        inode.flag = inode.READ;

                        // No need to wait by breaking the loop directly
                        break;

                    // if the file is already written by another thread
                    } else if (inode.flag == inode.WRITE) {

                        //  wait until finish
                        try {
                            wait();
                        } catch (InterruptedException ignored) {

                        }
                    }

                // if the file is requested for writing / append
                } else {

                    // the flag of that file is marked as "used" or "unused"
                    if (inode.flag == inode.USED || inode.flag == inode.UNUSED) {

                        // Change the flag to write
                        inode.flag = inode.WRITE;
                        break;

                    } else {

                        // otherwise wait until finish
                        try {
                            wait();
                        } catch (InterruptedException ignored) { }
                    }
                }
            } else if (!mode.equals("r")) {
                // if the node for the given file does not exist,
                // create a new inode for that file, use the alloc function from directory to get the inumber
                iNumber = dir.ialloc(filename);
                inode = new Inode(iNumber);
                inode.flag = inode.WRITE;
                break;
            } else {
                return null;
            }
        }


        inode.count++;
        inode.toDisk(iNumber);
        FileTableEntry entry = new FileTableEntry(inode, iNumber, mode);
        table.addElement(entry);
        return entry;
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
            inode.flag = inode.UNUSED;
        }

        // notify waiting threads
        if (inode.flag == inode.READ || inode.flag == inode.WRITE)
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
