import java.util.*;

public class FileSystem
{
	private SuperBlock superblock;				//contains info. about the disk
	private Directory directory;				//links inode to filename
	private FileTable filetable;				//contains exsiting File Table Entries		
	
	public FileSystem(int diskBlocks)
	{	
		// create superblock and format disk with 64 inodes in default
		superblock = new SuperBlock(diskBlocks);
		
		// create directory and register "/" in directory entry 0
		directory = new Directory(superblock.totalInodes);
		
		// file table is created, and stores directory in the file table
		filetable = new FileTable(directory);
		
		// directory reconstruction
		FileTableEntry dirEnt = open("/", "r");
		int dirSize = fsize(dirEnt);
		if (dirSize > 0)
		{
			byte[] dirData = new byte[dirSize];
			read(dirEnt, dirData);
			directory.bytes2directory(dirData);
		}
		close(dirEnt);
	}
	
	/*************sync()***************
	 *  
	 * Syncs the directory in memory to 
	 * the disk's root directory,
	 * and syncs the superblock.
	 **********************************/
	//what needs to happen in superblock?
	void sync()
	{
		byte[] buffer = directory.directory2bytes();
		FileTableEntry root = open("/", "w");
		
		write(root, buffer);
		close(root);
		
		superblock.sync();	//assuming superblock has a sync
	}
	
	/***********format(int)************
	 *  
	 * Erases number of files and resets the 
	 * directory, filetable, and 
	 * superblock.
	 **********************************/
	//What needs to happen in superblock?
	boolean format(int files)
	{
		//format superblock
		superblock.format(files);
		
		//format directory
		directory = new Directory(superblock.totalInodes);
		
		//format filetable
		filetable = new FileTable(directory);
		
		return true;
	}
	
	/********open(String, String)******
	 *  
	 * Opens an existing file on the disk.
	 * -First string is the filename.
	 * -Second string is the mode.
	 **********************************/
	FileTableEntry open(String filename, String mode)
	{
		//Check if this is a new file
		boolean isNew;
		if (directory.namei(filename) == -1)
		{
			isNew = true;
		}
		else
		{
			isNew = false;
		}
		
		//create new filetable
		FileTableEntry fte = filetable.falloc(filename, mode);
		
		//determine the mode
		short flag;
		switch(mode)
		{
			//Read
			case "r":
				//Set flag if first thread
				if (fte.count == 1)
				{
					fte.inode.flag = Inode.READ;
				}
				//if the file is new, exit
				if (isNew)
					return null;
			//Write
			case "w":
				//Set flag if first thread
				if (fte.count == 1)
				{
					fte.inode.flag = Inode.WRITE;
				}
				//Delete previous data
				deallocAllBlock(fte);
				//Set isNew to true, since will behave as new file
				isNew = true;
				break;
			//Read & Write
			case "w+":
				//Set flag if first thread
				if (fte.count == 1)
				{
					fte.inode.flag = Inode.WRITE;
				}
				break;
			
			//Append
			case "a":
				//Set flag if first thread
				if (fte.count == 1)
				{
					fte.inode.flag = Inode.WRITE;
				}
				seek(fte, 0, SEEK_END)l
				break;
		}
		
		if (isNew)
		{
			int temp = superblock.getFreeBlock();
			if (temp != -1)
			{
				nextBlock(ftEnt, temp);
				fte.inode.toDisk(fte.inumber);
			}
			else
			{
				return null;
			}
		}
		return fte;
	}
	
	boolean close(FileTableEntry ftEnt)
	{
		if (ftEnt != null)
		{
			if (ftEnt.Count == 0)
			{
				//set flag back
				ftEnt.inode.flag = Inode.USED;
				ftEnt.inode.toDisk(ftEnt.iNumber);
				if (filetable.ffree(ftEnt))
				{
					return 0;
				}
				else
				{
					return -1;
				}
			}
		}
		else
		{
			return -1;
		}
		return 0;
	}
	
	int fsize(FileTableEntry ftEnt)
	{
		return ftEnt.inode.length;
	}
	
	int read(FileTableEntry ftEnt, byte[] buffer)
	{
		while (true)
		{
			//Flag is set to DELETE
			if (ftEnt.inode.flag == Inode.DELETE)
			{
				//Exit
				return -1;
			}
			//Flag is set to WRITE
			else if (ftEnt.inode.flag == Inode.WRITE)
			{
				//Try to wait()
				try
				{
					wait();
				}
				catch (IOException e)
				{
				}
				//break;
			}
			else
			{
				//Set flag
				ftEnt.inode.flag = Inode.READ;
				
				int fileSize = buffer.length;
				int amountRead = 0;			//How much has been read
				int amountToRead = 0;		//How much will be read in the loop
				int amountToReadStart = 0;	//Where to start reading from
				byte[] readBuffer = new byte[Disk.blockSize];
				while(amountRead < fileSize)
				{
					int blockNumber;
					if (ftEnt.seekPtr / Disk.blockSize < 0)
					{
						return -1;
					}
					else if (ftEnt.SeekPtr / Disk.blockSize < ftEnt.inode.directSize)
					{
						blockNumber = ftEnt.inode.direct[ftEnd.SeekPtr / Disk.blockSize];
						if (blockNumber == -1)
						{
							return -1;
						}
					}
					else
					{
						byte[] indirectBlock = new byte[Disk.blockSize];
						SysLib.rawread(indirect, indirectBlock);
						blockNumber = SysLib.bytes2short(indirectBlock, (ftEnt.SeekPtr/Disk.blockSize));
						if (blockNumber == -1)
						{
							return -1;
						}
					}
					//Read from disk onto the readBuffer
					int check = SysLib.rawread(blockNumber, readBuffer);
					if (check == -1)
					{
						return -1;
					}
					//Sets the amount to read
					if (((ftEnt.inode.length - ftEnd.seekPtr) < Disk.blockSize) || (ftEnt.inode.length == ftEnt.seekPtr))
					{
						amountToRead = ftEnt.inode.length - ftEnt.seekPtr;
					}
					else
					{
						amountToRead = Disk.blockSize; 
					}
					
					if (fileSize < (Disk.blockSize - ftEnt.seekPtr))
					{
						System.arraycopy(readBuffer, ftEnt.seekPtr, buffer, 0, fileSize);
						amountRead = fileSize;
					}
					else
					{
						System.arraycopy(readBuffer, 0, buffer, amountToReadStart, amountToRead);
						amountRead += amountToRead;
					}
					amountRead += amountToRead;
					seek(ftEnt, amountToRead, SEEK_CUR);
				}
				if (ftEnt.count > 0)
				{
					ftEnt.count--;
				}
				if (ftEnt.count > 0)
				{
					notifyAll();
				}
				else
				{
					ftEnt.inode.flag = Inode.USED;
				}
				return amountRead;
			}
		}
	}
	
	
	int write(FileTableEntry ftEnt, byte[] buffer)
	{
		if (ftEnt == null)
		{
			return -1;
		}
		short blockNumber = ftEnt.inode.findTargetBlock(fte.seekPtr / Disk.blockSize);
		int amountWritten = 0;
		int blockOffset = ftEnt.seekPtr % Disk.blockSize;
		int fileSize = buffer.length;
		
		while(true)
		{
			//Flag is set to DELETE
			if (ftEnt.inode.flag == Inode.DELETE)
			{
				//Exit
				return -1;
			}
			else if (ftEnt.inode.flag == Inode.READ)
			{
				//Try to wait()
				try
				{
					wait();
				}
				catch (IOException e)
				{
				}
				//break;
			}
			//Flag is set to WRITE
			else if (ftEnt.inode.flag == Inode.WRITE)
			{
				//Try to wait()
				try
				{
					wait();
				}
				catch (IOException e)
				{
				}
				//break;
			}
			else
			{
				//set Flag
				ftEnt.inode.flag = Inode.WRITE;
				byte[] writeBuffer = new byte[Disk.blockSize];
				short inodeOffset = ftEnt.findTargetBlock(ftEnt.seekPtr);
				
				while (amountWritten < fileSize)
				{
					inodeOffset = ftEnt.findTargetBlock(ftEnt.seekPtr);
					if ((ftEnt.inode.indirect < 1) && (inodeOffset >= (ftEnt.inode.directSize - 1)))
					{
						int temp = superblock.getFreeBlock();
						if (temp != -1)
						{
							ftEnt.inode.setIndexBlock(temp);
							ftEnt.inode.toDisk(ftEnt.iNumber);
						}
						else
						{
							return -1; //no space
						}
					}
					int amountLeft = fileSize - amountWritten;
					if ((amountWritten % Disk.blockSize >= 1 && amountLeft > = 1) || blockNumber = -1)
					{
						//set the block number to the next free block
						blockNumber = superblock.getFreeBlock();
						if (blockNumber != -1)
						{
							if (nextBlock(ftEnt, blockNumber))
							{
								ftEnt.inode.toDisk(ftEnt.iNumber);
							}
							else
							{
								return -1;
							}
						}
						else
						{
							return -1;
						}
					}
					SysLib.rawread(blockNumber, writeBuffer);
					int toWrite;
					if (amountLeft < (Disk.blockSize - blockOffset))
					{
						toWrite = amountLeft;
					}
					else
					{
						toWrite = Disk.blockSize - blockOffset;
					}
					System.arraycopy(buffer, amountWritten, writeBuffer, blockOffset, toWrite);
					SysLib.rawwrite(blockNumber, writeBuffer);
					blockNumber += 1;
					amountWritten += toWrite;
					ftEnt.seekPtr += toWrite;
					blockOffset = 0;
				}
				ftEnt.count -= 1;
				
				if (ftEnt.inode.length < ftEnt.seekPtr)
				{
					int temp = ftEnt.seekPtr - ftEnt.inode.length;
					ftEnt.inode.length += temp;
					ftEnt.inode.toDisk(ftEnt.iNumber);
				}
				if (ftEnt.count <= 0)
				{
					ftEnt.inode.flag = Inode.USED;
				}
				else
				{
					notifyAll();
				}
				return amountWritten;
			}
		}
	}
	
	private boolean deallocAllBlocks(FileTableEntry ftEnt)
	{
		if (ftEnt.inode.count == 1)
		{
			int directSize = 11;
			for (int i = 0; i < directSize; i++)
			{
				if (ftEnt.inode.direct[i] != -1)
				{
					ftEnt.inode.direct[i] = -1;
					superblock.returnBlock(i);
				}
			}
		byte[] buffer = ftEnt.inode.unregisterIndexBlock();
		if (buffer != null)
		{
			short tempId;
			while(tempId != -1)
			{
				tempId = SysLib.bytes2short(buffer, 0);
				superblock.returnBlock(tempId);
			}
			ftEnt.inode.toDisk(ftEnt.iNumber);
			return true;
		}
		}
		else
		{
			return false;
		}
	}
	
	public synchronized boolean delete(String filename)
	{
		tempNum = directory.namei(filename);
		if (tempNum != -1)
		{
			Inode deleteNode = new Inode(tempNum);
			inode.flag = Inode.DELETE;
			if (directory.ifree((short)tempNum))
			{
				deleteNode.flag = Inode.USED;
				deleteNode.count = 0;
				deleteNode.toDisk(tempNum);
			}
			else
			{
				false;
			}
		}
		else
		{
			false;
		}
		return true;
	}
	
	private final int SEEK_SET = 0;
	private final int SEEK_CUR = 1;
	private final int SEEK_END = 2;
	
	public int seek(FileTableEntry ftEnt, int offset, int whence)
	{
		int curr = ftEnd.seekPtr;
		
		//SEEK_SET
		if (whence == SEEK_SET)
		{
			curr = offset;
		}
		//SEEK_CUR
		else if (whence == SEEK_CUR)
		{
			curr += offset;
		}
		//SEEK_END
		else if (whence == SEEK_END)
		{
			curr = ftEnt.inode.length + offset;
		}
		else
		{
			return -1;
		}
		
		if (curr >= 0)
		{
			ftEnt.seekPtr = curr;
			return ftEnt.seekPtr;
		}
		else
		{
			return -1;
		}
	}
	
	private boolean nextBlock(FileTableEntry ftEnt, short blockNum)
	{
		int directSize = 11;
		int indirectSize = 256;
		for (int i = 0; i < directSize; i++)
		{
			if (ftEnt.inode.direct[i] < 1)
			{
				ftEnt.inode.direct[i] = blockNum;
				return true;
			}
		}
		byte[] indirectBlock = new byte[Disk.blockSize];
		SysLib.rawread(ftEnt.inode.indirect, indirectBlock);
		for (int i = 0; i < (2 * indirectSize); i += 2)
		{
			if (SysLib.bytes2short(indirectBlock, i) < 1)
			{
				SysLib.(blockNum, indirectBlock, i);
				return SysLib.rawwrite(ftEnt.inode.indirect, indirectBlock != -1);
			}
		}
		return false;
	}
	
}