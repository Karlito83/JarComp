package tim.jarcomp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.w3c.tools.crypt.Md5;

/**
 * Class to do the actual comparison of jar files, populating a list of
 * EntryDetails objects
 */
public abstract class Comparer {

	private Comparer() {
		// Just here to hide the implicit public default constructor
	}

	/**
	 * Compare the two given files and return the results
	 *
	 * @param inFile1 first file
	 * @param inFile2 second file
	 * @param inMd5   true to also check md5 sums
	 * @return results of comparison
	 */
	public static CompareResults compare(File inFile1, File inFile2, boolean inMd5) {
		// Make results object and compare file sizes
		CompareResults results = new CompareResults();
		results.setSize(0, inFile1.length());
		results.setSize(1, inFile2.length());
		// Make empty list
		ArrayList<EntryDetails> entryList = new ArrayList<>();
		// load first file, make entrydetails object for each one
		final int numFiles1 = makeEntries(entryList, inFile1, 0);
		results.setNumFiles(0, numFiles1);
		// load second file, try to find entrydetails for each file or make new one
		final int numFiles2 = makeEntries(entryList, inFile2, 1);
		results.setNumFiles(1, numFiles2);
		results.setEntryList(entryList);

		// Check md5 sums if necessary
		if (inMd5) {
			calculateMd5(results, inFile1, 0);
			calculateMd5(results, inFile2, 1);
		}
		return results;
	}

	// TODO: Maybe we need to add an option to ignore path, just look at filenames?

	/**
	 * Make entrydetails objects for each entry in the given file and put in list
	 *
	 * @param inList  list of entries so far
	 * @param inFile  zip/jar file to search through
	 * @param inIndex 0 for first file, 1 for second
	 * @return number of files found
	 */
	private static int makeEntries(ArrayList<EntryDetails> inList, File inFile, int inIndex) {
		boolean checkList = !inList.isEmpty();
		int numFiles = 0;
		try (ZipFile zip = new ZipFile(inFile)) {
			Enumeration<?> zipEntries = zip.entries();
			while (zipEntries.hasMoreElements()) {
				ZipEntry ze = (ZipEntry) zipEntries.nextElement();
				if (ze.isDirectory()) {
					continue; // ignore these
				}
				numFiles++;
				String name = ze.getName();
				EntryDetails details = null;
				if (checkList) {
					details = getEntryFromList(inList, name);
				}
				// Construct new details object if necessary
				if (details == null) {
					details = new EntryDetails();
					details.setName(name);
					inList.add(details);
				}
				// set size
				details.setSize(inIndex, ze.getSize());
			}
		} catch (IOException ioe) {
			System.err.println("Ouch: " + ioe.getMessage());
		}
		return numFiles;
	}

	/**
	 * Look up the given name in the list
	 *
	 * @param inList list of EntryDetails objects
	 * @param inName name to look up
	 */
	private static EntryDetails getEntryFromList(ArrayList<EntryDetails> inList, String inName) {
		for (EntryDetails details : inList) {
			if (details.getName() != null && details.getName().equals(inName)) {
				return details;
			}
		}
		return null;
	}

	/**
	 * Calculate the md5 sums of all relevant entries
	 *
	 * @param inResults results from preliminary check
	 * @param inFile    file to read
	 * @param inIndex   file index, either 0 or 1
	 */
	private static void calculateMd5(CompareResults inResults, File inFile, int inIndex) {
		ArrayList<EntryDetails> list = inResults.getEntryList();
		try (ZipFile zip = new ZipFile(inFile)) {
			for (EntryDetails entry : list) {
				if (entry.getStatus() == EntryDetails.EntryStatus.SAME_SIZE) {
					// Must be present in both archives if size is the same
					ZipEntry zipEntry = zip.getEntry(entry.getName());
					if (zipEntry == null) {
						System.err.println("zipEntry for " + entry.getName() + " shouldn't be null!");
					} else {
						Md5 hasher = new Md5(zip.getInputStream(zipEntry));
						byte[] digest = hasher.getDigest();
						if (digest != null) {
							entry.setMd5Sum(inIndex, hasher.getStringDigest());
						}
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Exception: " + e.getMessage());
		}
	}
}
