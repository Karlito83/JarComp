package tim.jarcomp;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;

/**
 * Class to manage the main compare window
 */
public class CompareWindow {
	/** Main window object */
	private final JFrame mainWindow;
	/** Two files to compare */
	private final File[] filesToCompare = new File[2];
	/** Displays for jar file details */
	private JarDetailsDisplay[] detailsDisplays = null;
	/** Label for compare status */
	private JLabel statusLabel = null;
	/** Second label for contents status */
	private JLabel statusLabel2 = null;
	/** Table model */
	private EntryTableModel tableModel = null;
	/** File chooser */
	private JFileChooser fileChooser = null;
	/** Button to check md5 sums */
	private JButton md5Button = null;
	/** Refresh button to repeat comparison */
	private JButton refreshButton = null;
	/** Flag to process md5 sums */
	private boolean checkMd5 = false;

	/**
	 * Constructor
	 */
	public CompareWindow() {
		mainWindow = new JFrame("Jar Comparer");
		mainWindow.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		mainWindow.getContentPane().add(makeComponents());
		mainWindow.pack();
		mainWindow.setVisible(true);
	}

	/**
	 * Make the GUI components for the main dialog
	 *
	 * @return JPanel containing GUI components
	 */
	private JPanel makeComponents() {
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());

		// Top panel
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
		// Button panel
		JPanel buttonPanel = new JPanel();
		JButton compareButton = new JButton("Compare ...");
		compareButton.addActionListener(e -> startCompare());
		buttonPanel.add(compareButton);
		refreshButton = new JButton("Refresh");
		refreshButton.setEnabled(false);
		refreshButton.addActionListener(e -> startCompare(filesToCompare[0], filesToCompare[1], false));
		buttonPanel.add(refreshButton);
		buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		topPanel.add(buttonPanel);

		JPanel detailsPanel = new JPanel();
		detailsPanel.setLayout(new GridLayout(1, 2, 5, 5));
		detailsDisplays = new JarDetailsDisplay[2];
		detailsDisplays[0] = new JarDetailsDisplay();
		detailsPanel.add(detailsDisplays[0], BorderLayout.WEST);
		detailsDisplays[1] = new JarDetailsDisplay();
		detailsPanel.add(detailsDisplays[1], BorderLayout.EAST);
		topPanel.add(detailsPanel);
		detailsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		statusLabel = new JLabel("");
		statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		statusLabel.setBorder(new EmptyBorder(5, 10, 1, 1));
		topPanel.add(statusLabel);
		statusLabel2 = new JLabel("");
		statusLabel2.setAlignmentX(Component.LEFT_ALIGNMENT);
		statusLabel2.setBorder(new EmptyBorder(1, 10, 5, 1));
		topPanel.add(statusLabel2);
		mainPanel.add(topPanel, BorderLayout.NORTH);

		// main table panel
		tableModel = new EntryTableModel();
		JTable table = new JTable(tableModel) {
			private static final long serialVersionUID = 1L;

			/** Modify the renderer according to the row status */
			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
				Component c = super.prepareRenderer(renderer, row, column);
				if (!isRowSelected(row)) {
					int modelRow = convertRowIndexToModel(row);
					boolean isChange = ((EntryTableModel) getModel()).areDifferent(modelRow);
					c.setBackground(isChange ? java.awt.Color.YELLOW : getBackground());
				}
				return c;
			}
		};
		table.getColumnModel().getColumn(0).setPreferredWidth(300);
		table.getColumnModel().getColumn(1).setPreferredWidth(70);
		table.getColumnModel().getColumn(2).setPreferredWidth(70);
		// Table sorting by clicking on column headings
		table.setAutoCreateRowSorter(true);
		mainPanel.add(new JScrollPane(table), BorderLayout.CENTER);

		// button panel at bottom
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		md5Button = new JButton("Check Md5 sums");
		md5Button.setEnabled(false);
		md5Button.addActionListener(arg0 -> startCompare(filesToCompare[0], filesToCompare[1], true));
		bottomPanel.add(md5Button);
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(arg0 -> System.exit(0));
		bottomPanel.add(closeButton);
		mainPanel.add(bottomPanel, BorderLayout.SOUTH);
		return mainPanel;
	}

	/**
	 * Start the comparison process by prompting for two files
	 */
	public void startCompare() {
		startCompare(null, null, false);
	}

	/**
	 * Start the comparison using the two specified files
	 *
	 * @param inFile1 first file
	 * @param inFile2 second file
	 * @param inMd5   true to check Md5 sums as well
	 */
	public void startCompare(File inFile1, File inFile2, boolean inMd5) {
		// Clear table model
		tableModel.reset();

		File file1 = inFile1;
		File file2 = inFile2;
		if (file1 == null || !file1.exists() || !file1.canRead()) {
			file1 = selectFile("Select first file", null);
		}
		// Bail if cancel pressed
		if (file1 == null) {
			return;
		}
		// Select second file if necessary
		if (file2 == null || !file2.exists() || !file2.canRead()) {
			file2 = selectFile("Select second file", file1);
		}
		// Bail if cancel pressed
		if (file2 == null) {
			return;
		}
		filesToCompare[0] = file1;
		filesToCompare[1] = file2;

		// Clear displays
		detailsDisplays[0].clear();
		detailsDisplays[1].clear();
		statusLabel.setText("comparing...");

		// Start separate thread to compare files
		checkMd5 = inMd5;
		new Thread(this::doCompare).start();
	}

	/**
	 * Compare method, to be done in separate thread
	 */
	private void doCompare() {
		CompareResults results = Comparer.compare(filesToCompare[0], filesToCompare[1], checkMd5);
		tableModel.setEntryList(results.getEntryList());
		final boolean archivesDifferent = (results.getStatus() == EntryDetails.EntryStatus.CHANGED_SIZE);
		if (archivesDifferent) {
			statusLabel
					.setText("Archives have different size (" + results.getSize(0) + ", " + results.getSize(1) + ")");
		} else {
			statusLabel.setText("Archives have the same size (" + results.getSize(0) + ")");
		}
		detailsDisplays[0].setContents(filesToCompare[0], results, 0);
		detailsDisplays[1].setContents(filesToCompare[1], results, 1);

		if (results.getEntriesDifferent()) {
			statusLabel2.setText((archivesDifferent ? "and" : "but") + " the files have different contents");
		} else {
			if (results.getEntriesMd5Checked()) {
				statusLabel2.setText((archivesDifferent ? "but" : "and") + " the files have exactly the same contents");
			} else {
				statusLabel2
						.setText((archivesDifferent ? "but" : "and") + " the files appear to have the same contents");
			}
		}
		md5Button.setEnabled(!results.getEntriesMd5Checked());
		checkMd5 = false;
		refreshButton.setEnabled(true);
		// Possibilities:
		// Jars have same size, same md5 sum, same contents
		// Jars have same size but different md5 sum, different contents
		// Jars have different size, different md5 sum, but same contents
		// Individual files have same size but different md5 sum
		// Jars have absolutely nothing in common

		// Maybe poll each minute to check if last modified has changed, then prompt to
		// refresh?
	}

	/**
	 * Select a file for the comparison
	 *
	 * @param inTitle     title of dialog
	 * @param inFirstFile File to compare selected file with (or null)
	 * @return selected File, or null if cancelled
	 */
	private File selectFile(String inTitle, File inFirstFile) {
		if (fileChooser == null) {
			fileChooser = new JFileChooser();
			fileChooser.setFileFilter(new GenericFileFilter("Jar files and Zip files", new String[] { "jar", "zip" }));
		}
		fileChooser.setDialogTitle(inTitle);
		File file = null;
		boolean rechoose = true;
		while (rechoose) {
			file = null;
			rechoose = false;
			int result = fileChooser.showOpenDialog(mainWindow);
			if (result == JFileChooser.APPROVE_OPTION) {
				file = fileChooser.getSelectedFile();
				rechoose = (!file.exists() || !file.canRead());
			}
			// Check it's not the same as the first file, if any
			if (file != null && file.equals(inFirstFile)) {
				JOptionPane.showMessageDialog(mainWindow,
						"The second file is the same as the first file!\n"
								+ "Please select another file to compare with '" + inFirstFile.getName() + "'",
						"Two files equal", JOptionPane.ERROR_MESSAGE);
				rechoose = true;
			}
		}
		return file;
	}
}
