import java.util.Vector;

/**
 * Created by mahokyin on 5/28/17.
 */
public class FileTable {

    // For debug purpose
    private static final int UNUSED = 0;
    private static final int USED = 1;
    private static final int READ = 2;
    private static final int WRITE = 3;
    ////////////////////////

    private Vector<FileTableEntry> table;         // the actual entity of this file table
    private Directory dir;        // the root directory

    public FileTable( Directory directory ) { // constructor
        table = new Vector<>();     // instantiate a file (structure) table
        dir = directory;           // receive a reference to the Director
    }                             // from the file system

    // major public methods

    // allocate a new file (structure) table entry for this file name
    // allocate/retrieve and register the corresponding inode using dir
    // increment this inode's count
    // immediately write back this inode to the disk
    // return a reference to this file (structure) table entry
    public synchronized FileTableEntry falloc( String filename, String mode ) {
        // Debug use
        // SysLib.cout( "\nFileTableEntry: falloc() is running \n" );

        short iNumber; // inode number
        Inode inode; // Inode reference

        while (true) {
            // get the inumber form the inode for given file name
            iNumber = (filename.equals("/") ? (short) 0 : dir.namei(filename));

            // if the inode for the given file exist
            if (iNumber >= 0) {
                inode = new Inode(iNumber);

                // if the file is requesting for reading
                if (mode.equals("r")) {

                    switch (inode.flag) {
                        case WRITE:
                            try {
                                wait();
                            } catch (InterruptedException e) { }
                            break;

                        default:
                            // change the flag of the node to read and break
                            inode.flag = READ;
                            break;
                    }
                    break;

                } else {

                    // if file is used, change the flag to write
                    if (inode.flag == USED || inode.flag == UNUSED) {
                        inode.flag = WRITE;
                        break;
                        // if the flag is read or write, wait until they finish
                    } else {
                        try {
                            wait();
                        } catch (InterruptedException e) { }
                    }
                }

                // if the node for the given file does not exist,
                // create a new inode for that file, use the alloc function from
                // directory to get the inumber
            } else if (!mode.equals("r")) {
                iNumber = dir.ialloc(filename);
                inode = new Inode(iNumber);
                inode.flag = WRITE;
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

    // receive a file table entry reference
    // save the corresponding inode to the disk
    // free this file table entry.
    // return true if this file table entry found in my table
    public synchronized boolean ffree(FileTableEntry tableEntry) {
        // // Debug use
        // SysLib.cout( "\nFileTableEntry: ffree() is running \n" );

        Inode inode = new Inode(tableEntry.iNumber);

        // Remove FTE if it is in table, the remove will return true
        if (table.removeElement(tableEntry)) {
            switch (inode.flag) {
                case READ:
                    if (inode.count == 1) {
                        notify();
                        inode.flag = USED;
                    }
                    break;

                case WRITE:
                    inode.flag = USED;
                    notifyAll();
                    break;
            }

            inode.count--;
            inode.toDisk(tableEntry.iNumber);
            return true;
        }

        return false;
    }

    // Return true if there is not any obj inside the file table, otherwise return false
    public synchronized boolean fempty( ) {
        // Debug use
        // SysLib.cout( "\nFileTableEntry: fempty() is running \n" );
        return table.isEmpty( );  // return if table is empty
    }
}
