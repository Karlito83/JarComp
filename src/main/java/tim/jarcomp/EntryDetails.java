package tim.jarcomp;

/**
 * Class to represent a single entry in the jar file
 * for displaying in the comparison table
 */
public class EntryDetails
{
	/** Name of entry, including full path */
	private String name = null;
	/** Flag to show if it's present or not (might be zero length) */
	private final boolean[] present = new boolean[2];
	/** Sizes of this file, in bytes, in archives */
	private final long[] sizes = new long[2];
	/** Md5 sums in both archives */
	private final String[] md5Sums = new String[2];
	/** SizeChange */
	private final SizeChange sizeChange = new SizeChange();

	/** Constants for entry status */
	public enum EntryStatus
	{
		/** File not in first but in second    */ ADDED,
		/** File found in first, not in second */ REMOVED,
		/** File size different in two files   */ CHANGED_SIZE,
		/** File size same (md5 not checked)   */ SAME_SIZE,
		/** File checksum different            */ CHANGED_SUM,
		/** Files really equal                 */ EQUAL
	}
	// TODO: Each of these status flags needs an icon

	/**
	 * @return name of entry
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param inName name to set
	 */
	public void setName(String inName) {
		name = inName;
	}

	/**
	 * @param inIndex index, either 0 or 1
	 * @return size of this file in corresponding archive
	 */
	public long getSize(int inIndex)
	{
		if (inIndex < 0 || inIndex > 1) {return 0L;}
		return sizes[inIndex];
	}

	/**
	 * @param inIndex index, either 0 or 1
	 * @param inSize size of file in bytes
	 */
	public void setSize(int inIndex, long inSize)
	{
		if (inIndex==0 || inIndex==1)
		{
			sizes[inIndex] = inSize;
			present[inIndex] = true;
			sizeChange.update(sizes[1] - sizes[0], isChanged());
		}
	}

	/**
	 * @param inIndex index, either 0 or 1
	 * @param inMd5Sum md5 checksum of this file
	 */
	public void setMd5Sum(int inIndex, String inMd5Sum)
	{
		if (inIndex==0 || inIndex==1)
		{
			md5Sums[inIndex] = inMd5Sum;
			sizeChange.update(sizes[1] - sizes[0], isChanged());
		}
	}

	/**
	 * @return true if md5 sums have been generated for this entry
	 */
	public boolean getMd5Checked()
	{
		return (md5Sums[0] != null && md5Sums[1] != null);
	}

	/**
	 * @return status of entry
	 */
	public EntryStatus getStatus()
	{
		if (!present[0] && present[1]) {return EntryStatus.ADDED;}
		if (present[0] && !present[1]) {return EntryStatus.REMOVED;}
		if (sizes[0] != sizes[1]) {return EntryStatus.CHANGED_SIZE;}
		if (!getMd5Checked()) {return EntryStatus.SAME_SIZE;}
		// md5 sums have been checked
		if (!md5Sums[0].equals(md5Sums[1])) {return EntryStatus.CHANGED_SUM;}
		return EntryStatus.EQUAL;
	}

	/**
	 * @return size change object
	 */
	public SizeChange getSizeChange()
	{
		return sizeChange;
	}

	/**
	 * @return true if the row represents a change
	 */
	public boolean isChanged()
	{
		EntryStatus status = getStatus();
		return status != EntryStatus.SAME_SIZE && status != EntryStatus.EQUAL;
	}
}

