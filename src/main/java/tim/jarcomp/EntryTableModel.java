package tim.jarcomp;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import tim.jarcomp.EntryDetails.EntryStatus;

/**
 * Class to hold the table model for the comparison table
 */
public class EntryTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;
	/** list of entries */
	private ArrayList<EntryDetails> entries = null;

	/**
	 * Clear list to start a new comparison
	 */
	public void reset() {
		entries = new ArrayList<>();
	}

	/**
	 * Reset the table with the given list
	 *
	 * @param inList list of EntryDetails objects
	 */
	public void setEntryList(ArrayList<EntryDetails> inList) {
		entries = inList;
		fireTableDataChanged();
	}

	/**
	 * @return number of columns in table
	 */
	@Override
	public int getColumnCount() {
		return 3;
		// TODO: Columns for size1, size2, status (as icon), size difference
	}

	/**
	 * @return class of column, needed for sorting the Longs properly
	 */
	@Override
	public Class<?> getColumnClass(int inColNum) {
		return getValueAt(0, inColNum).getClass();
	}

	/**
	 * @return column name
	 */
	@Override
	public String getColumnName(int inColNum) {
		if (inColNum == 0) {
			return "Filename";
		} else if (inColNum == 1) {
			return "Status";
		}
		return "Size Change";
	}

	/**
	 * @return number of rows in the table
	 */
	@Override
	public int getRowCount() {
		if (entries == null) {
			return 0;
		}
		return entries.size();
	}

	/**
	 * @return object at specified row and column
	 */
	@Override
	public Object getValueAt(int inRowNum, int inColNum) {
		if (inRowNum >= 0 && inRowNum < getRowCount()) {
			EntryDetails entry = entries.get(inRowNum);
			if (inColNum == 0)
				return entry.getName();
			else if (inColNum == 1)
				return getText(entry.getStatus());
			return entry.getSizeChange();
		}
		return null;
	}

	/**
	 * Convert an entry status into text
	 *
	 * @param inStatus entry status
	 * @return displayable text
	 */
	private static String getText(EntryStatus inStatus) {
		switch (inStatus) {
		case ADDED:
			return "Added";
		case CHANGED_SIZE:
			return "Changed size";
		case CHANGED_SUM:
			return "Changed sum";
		case EQUAL:
			return "=";
		case REMOVED:
			return "Removed";
		case SAME_SIZE:
			return "Same size";
		}
		return inStatus.toString();
	}

	/**
	 * @return true if specified row represents a difference between the two files
	 */
	public boolean areDifferent(int inRowNum) {
		if (inRowNum >= 0 && inRowNum < getRowCount()) {
			return entries.get(inRowNum).isChanged();
		}
		return false;
	}
}
