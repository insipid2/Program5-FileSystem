/**
 * Created by mahokyin on 5/28/17.
 */
public class TCB {
    private static final int ERROR_CODE = -1;
    private static final int MAX_BLOCKS = 32;
    private Thread thread = null;
    private int tid = 0;
    private int pid = 0;
    private boolean terminated = false;

    // User file descriptor table:
    // each entry pointing to a file (structure) table entry
    public FileTableEntry[] ftEnt = null;

    public TCB( Thread newThread, int myTid, int parentTid ) {
        thread = newThread;
        tid = myTid;
        pid = parentTid;
        terminated = false;

        // The following code is added for the file system
        ftEnt = new FileTableEntry[MAX_BLOCKS];
        for ( int i = 0; i < MAX_BLOCKS; i++ ) {
            ftEnt[i] = null;         // all entries initialized to null
            // fd[0], fd[1], and fd[2] are kept null.
        }
    }

    /**
     * @return Thread Object
     */
    public synchronized Thread getThread( ) {
        return thread;
    }

    /**
     * @return Current Thread ID
     */
    public synchronized int getTid( ) {
        return tid;
    }

    /**
     * @return Parent Thread ID
     */
    public synchronized int getPid( ) {
        return pid;
    }

    /**
     * Terminate the TCB object
     * @return the result of variable terminated
     */
    public synchronized boolean setTerminated( ) {
        terminated = true;
        return terminated;
    }

    /**
     * Know if the TCB is terminated
     * @return the status of terminated
     */
    public synchronized boolean getTerminated( ) {
        return terminated;
    }

    // added for the file system
    public synchronized int getFd( FileTableEntry entry ) {
        if ( entry == null ) {
            return ERROR_CODE;
        }

        for ( int i = 3; i < MAX_BLOCKS; i++ ) {
            if ( ftEnt[i] == null ) {
                ftEnt[i] = entry;
                return i;
            }
        }

        return ERROR_CODE;
    }

    // added for the file system
    public synchronized FileTableEntry returnFd( int fd ) {
        if ( fd >= 3 && fd < MAX_BLOCKS ) {
            FileTableEntry oldEnt = ftEnt[fd];
            ftEnt[fd] = null;
            return oldEnt;
        } else {
            return null;
        }
    }

    // added for the file systme
    public synchronized FileTableEntry getFtEnt( int fd ) {
        if ( fd >= 3 && fd < MAX_BLOCKS )
            return ftEnt[fd];
        else
            return null;
    }
}
