package org.olat.upgrade;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;

/**
 * Goes through all ical files and migrates the 'X-OLAT-LINK' which have been written in a wrong (non-utf8) encoding in 6.3.0 until and including 6.3.2. With 6.3.3 the
 * encoding was fixed to utf8 and this would conflict with the written ical files.
 * <P>
 * Initial Date: 23.08.2010 <br>
 * 
 * @author Patrick
 */
public class CalendarXOlatLinkUTF8Fix {

	public static String newline = "\r\n";// iCal newline

	private static OLog log = Tracing.createLoggerFor(CalendarXOlatLinkUTF8Fix.class);

	/**
	 * @param args
	 */
	public static void migrate(final File folder) {
		if (folder == null || !folder.exists() || !folder.isDirectory()) { throw new IllegalArgumentException("Directory null or not found: " + folder); }

		log.audit("search in: " + folder);

		final String pathToFolder = folder.getAbsolutePath();
		final String[] files = folder.list(new FilenameFilter() {

			@Override
			public boolean accept(final File dir, final String name) {
				return name.endsWith(".ics");
			}
		});
		log.audit("Filecnt: " + files.length);
		for (final String filename : files) {
			final File file = new File(pathToFolder + File.separator + filename);
			if (checkFile(file)) {
				log.audit("Migrated: " + filename);
			}
		}

	}

	private static boolean checkFile(final File file) {
		BufferedReader br = null;
		final StringBuffer copy = new StringBuffer();
		try {
			br = getFileReader(file);

			String strLine = null;
			boolean wasConverted = false;
			while ((strLine = br.readLine()) != null) {

				String nextLine = strLine;
				while (nextLine.startsWith("X-OLAT-LINK:")) {

					final List<String> unfoldedAndNextLine = unfoldICalLine(br, nextLine);
					final String unfoldedLine = unfoldedAndNextLine.get(0);

					final String convertedLine = fixUTF8(unfoldedLine);
					wasConverted = wasConverted || !unfoldedLine.equals(convertedLine);

					final String foldedLine = foldICalLine(convertedLine);
					copy.append(foldedLine);

					nextLine = unfoldedAndNextLine.get(1);// after X-OLAT-LINK lines there is always at least iCal END:VEVENT
				}
				copy.append(nextLine).append(newline);

			}
			// finished file read
			br.close();

			if (wasConverted) {
				writeConvertedFile(file, copy);
			}

			return wasConverted;

		} catch (final Exception e) {
			log.error("Error file i/o, try closing now : " + e, e);
			try {
				if (br != null) {
					br.close();
				}
			} catch (final Exception e1) {
				log.error("Error in closing: " + e1, e1);
			}
		}
		return false;

	}

	/**
	 * open file for reading
	 * 
	 * @param file
	 * @return
	 */
	private static BufferedReader getFileReader(final File file) {

		FileInputStream fstream = null;
		DataInputStream in = null;
		BufferedReader br = null;
		try {
			fstream = new FileInputStream(file);
			in = new DataInputStream(fstream);
			br = new BufferedReader(new InputStreamReader(in, "ISO-8859-1"));

		} catch (final UnsupportedEncodingException e) {
			log.error("getFileReader: UnsupportedEncodingException", e);
			if (br != null) {
				try {
					br.close();
					if (in != null) {
						in.close();
					}
					if (fstream != null) {
						fstream.close();
					}
				} catch (final IOException e1) {
					log.error("getFileReader: IOException on close in catch(UnsupportedEncodingException)", e1);
				}
			}
		} catch (final FileNotFoundException e) {
			log.error("getFileReader: FileNotFoundException", e);
			if (br != null) {
				try {
					br.close();
					if (in != null) {
						in.close();
					}
					if (fstream != null) {
						fstream.close();
					}
				} catch (final IOException e1) {
					log.error("getFileReader: IOException on close in catch(FileNotFoundException)", e1);
				}
			}
		}

		return br;

	}

	/**
	 * remove wrong UTF-8 chars
	 * 
	 * @param unfoldedLine
	 * @return
	 */
	private static String fixUTF8(String unfoldedLine) {
		final String origLine = unfoldedLine;
		String convertedLine = "";

		int index = unfoldedLine.indexOf("Ã");
		int index2 = unfoldedLine.indexOf("Â§");
		while (index2 != -1 && index != -1) {
			if (index2 - index > 2) {
				// then there is another utf8 character encoded with the A tilde
				convertedLine = convertedLine + unfoldedLine.substring(0, index + 2);
				unfoldedLine = unfoldedLine.substring(index + 2);
				index = unfoldedLine.indexOf("Ã");
				index2 = unfoldedLine.indexOf("Â§");
				continue;
			}
			if (index2 - index < 2) {
				// this situation only occurs with lines which do not need conversion
				// hence we can safely break out of the while loop here
				break;
			}

			// otherwise we do the conversion now
			convertedLine = convertedLine + unfoldedLine.substring(0, index) + "Â§";
			unfoldedLine = unfoldedLine.substring(index + 4);

			index = unfoldedLine.indexOf("Ã");
			index2 = unfoldedLine.indexOf("Â§");
		}
		// append last piece the unfolded line
		convertedLine = convertedLine + unfoldedLine;

		return convertedLine;
	}

	/**
	 * iCal saves "one line of content" in mutliple lines not longer then 73 chars. see http://tools.ietf.org/html/rfc5545#section-3.1
	 * 
	 * @param lines
	 * @param foldedLineStart
	 * @return
	 * @throws IOException
	 */
	private static List<String> unfoldICalLine(final BufferedReader lines, final String foldedLineStart) throws IOException {
		final List<String> unfoldedAndNextLine = new ArrayList<String>();
		String unfoldedLine = foldedLineStart;
		String strLine = null;
		while ((strLine = lines.readLine()) != null) {
			// see http://tools.ietf.org/html/rfc5545#section-3.1
			if (strLine.startsWith(" ") || strLine.startsWith("\t")) {
				unfoldedLine = unfoldedLine + strLine.substring(1);
			} else {
				// folded line terminated
				break;
			}
		}

		unfoldedAndNextLine.add(unfoldedLine);
		unfoldedAndNextLine.add(strLine);

		return unfoldedAndNextLine;
	}

	/**
	 * see unfoldICalLine
	 * 
	 * @param convertedLine
	 * @return
	 */
	private static String foldICalLine(String convertedLine) {
		String foldedLine = null;
		final int MAXLEN = 73;

		final int partCnt = convertedLine.length() / MAXLEN + 1;
		final String[] parts = new String[partCnt];
		for (int i = 0; i < parts.length; i++) {
			int min = 0;
			if (i == 0) {
				min = Math.min(MAXLEN, convertedLine.length());
			} else {
				min = Math.min(MAXLEN - 1, convertedLine.length());
			}
			parts[i] = convertedLine.substring(0, min);
			convertedLine = convertedLine.substring(min);
			if (i == 0) {
				foldedLine = parts[i] + newline;
			} else {
				foldedLine = foldedLine + " " + parts[i] + newline;
			}
		}

		return foldedLine;
	}

	private static void writeConvertedFile(final File file, final StringBuffer copy) throws FileNotFoundException, UnsupportedEncodingException, IOException {
		final FileOutputStream fos = new FileOutputStream(file);
		final DataOutputStream out = new DataOutputStream(fos);
		final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out, "ISO-8859-1"));
		bw.write(copy.toString());
		bw.flush();
		bw.close();
		out.close();
		fos.close();
	}

}
