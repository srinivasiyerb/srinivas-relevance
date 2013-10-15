/**
 * OLAT - Online Learning and Training<br>
 * http://www.olat.org
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Copyright (c) 1999-2006 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.core.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.olat.core.id.Identity;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.AssertException;

/**
 * enclosing_type Description: <br>
 * helper class for formating Strings (not locale specific)
 * 
 * @author Felix Jost
 */
public class StringHelper {

	private static final NumberFormat numFormatter;
	private static final String WHITESPACE_REGEXP = "^\\s*$";
	private static final Pattern WHITESPACE_PATTERN = Pattern.compile(WHITESPACE_REGEXP);

	private static final Pattern p1 = Pattern.compile("\\+");
	private static final Pattern p2 = Pattern.compile("%2F");

	/**
	 * regex for not allowing <code>;,:</code> <code>ALL_WITHOUT_COMMA_2POINT_STRPNT</code>
	 */
	public static final String ALL_WITHOUT_COMMA_2POINT_STRPNT = "^[^,;:]*$";
	private static final Pattern ALL_WITHOUT_COMMA_2POINT_STRPNT_PATTERN = Pattern.compile(ALL_WITHOUT_COMMA_2POINT_STRPNT);
	private static final String X_MAC_ENC = "x-mac-";
	private static final String MAC_ENC = "mac";

	static {
		DecimalFormatSymbols dfs = new DecimalFormatSymbols();
		dfs.setDecimalSeparator('.');
		numFormatter = new DecimalFormat("#.#", dfs);
	}

	/**
	 * unused
	 * 
	 * @param in
	 * @param delim
	 * @return List
	 */
	public static List<String> getParts(String in, String delim) {
		List<String> li = new ArrayList<String>();
		String part;
		int delimlen = delim.length();
		int oldpos = 0;
		int k;
		while ((k = in.indexOf(delim, oldpos)) != -1) {
			part = in.substring(oldpos, k);
			li.add(part);
			oldpos = k + delimlen;
		}
		if (oldpos != 0) { // min. ein Trennzeichen -> nimm rest
			part = in.substring(oldpos);
			li.add(part);
		}
		return li;
	}

	/**
	 * @param date
	 * @param locale
	 * @return formatted date
	 */
	public static String formatLocaleDate(long date, Locale locale) {
		if (date == -1) return "-";
		return DateFormat.getDateInstance(DateFormat.SHORT, locale).format(new Date(date));
	}

	/**
	 * @param date
	 * @param locale
	 * @return formatted date
	 */
	public static String formatLocaleDateFull(long date, Locale locale) {
		if (date == -1) return "-";
		return DateFormat.getDateInstance(DateFormat.FULL, locale).format(new Date(date));
	}

	/**
	 * @param date
	 * @param locale
	 * @return formatted date/time
	 */
	public static String formatLocaleDateTime(long date, Locale locale) {
		if (date == -1) return "-";
		return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale).format(new Date(date));
	}

	/**
	 * @param time
	 * @param locale
	 * @return formatted time
	 */
	public static String formatLocaleTime(long time, Locale locale) {
		if (time == -1) return "-";
		return DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(new Date(time));
	}

	/**
	 * @param mem
	 * @return formatted memory
	 */
	public static String formatMemory(long mem) {
		long kb = mem / 1024;
		long mb = kb / 1024;
		if (mb > 0) return mb + " MB";
		else if (kb > 0) return kb + " KB";
		else return mem + " B";
	}

	/**
	 * @param f
	 * @param fractionDigits
	 * @return formatted float
	 */
	public static String formatFloat(float f, int fractionDigits) {
		numFormatter.setMaximumFractionDigits(fractionDigits);
		return numFormatter.format(f);
	}

	/**
	 * @param url
	 * @return encoded string
	 */
	public static String urlEncodeISO88591(String url) {
		String part;
		try {
			part = URLEncoder.encode(url, "iso-8859-1");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("encoding failed (iso-8859-1) for :" + url);
		}
		return part;
	}

	/**
	 * @param url
	 * @return encoded string
	 */
	public static String urlEncodeUTF8(String url) {
		String encodedURL;
		try {
			encodedURL = URLEncoder.encode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			/*
			 * from java.nio.Charset Standard charsets Every implementation of the Java platform is required to support the following standard charsets... ... UTF-8
			 * Eight-bit UCS Transformation Format ...
			 */
			throw new AssertException("utf-8 encoding is needed for proper encoding, but not offered on this java platform????");
		}
		encodedURL = p1.matcher(encodedURL).replaceAll("%20");
		encodedURL = p2.matcher(encodedURL).replaceAll("/");
		return encodedURL;
	}

	/**
	 * Converts all keys of a hash map to a string array.
	 * 
	 * @param m The (hash) map with the key and values
	 * @return The string array containing all keys for this map
	 */
	public static String[] getMapKeysAsStringArray(Map m) {
		return (String[]) m.keySet().toArray(new String[m.size()]);
	}

	/**
	 * Converts all values of a hash map to a string array.
	 * 
	 * @param m The (hash) map with the key and values
	 * @return The string array containing all values for this map
	 */
	public static String[] getMapValuesAsStringArray(Map m) {
		return (String[]) m.values().toArray(new String[m.size()]);
	}

	/**
	 * matches any but ^[^,;:]*$
	 * 
	 * @param s
	 * @return true if does not match regexp
	 */
	public static boolean containsNoneOfCoDouSemi(String s) {
		if (s == null) return false;
		Matcher m = ALL_WITHOUT_COMMA_2POINT_STRPNT_PATTERN.matcher(s);
		return m.find();
	}

	/**
	 * Checks if a string has anything in it to display. Will return true if the string is not null and does contain at least one none-whitespace character.
	 * 
	 * @param s The String to be evaluated
	 * @return true if the string contains any non-whitespace character, false otherwhise
	 */
	public static boolean containsNonWhitespace(String s) {
		if (s == null) return false;

		Matcher matcher = WHITESPACE_PATTERN.matcher(s);

		// if string matches whitespace pattern then string does not
		// contain non-whitespace
		return !matcher.find();
	}

	/**
	 * takes an array of Identies and converts them to a String containing the Identity-Emails separated by a <b>, </b>. The returned String can be fed directly to the
	 * e-mailer helper as the e-mail to field. <br>
	 * <ul>
	 * <li>Entries in the parameter emailRecipientIdentites are expected to be not null.</li>
	 * </ul>
	 * 
	 * @param emailRecipientIdentities
	 * @return "email1, email2, email3," or null if emailRecipientIdentites was null
	 */
	public static String formatIdentitesAsEmailToString(final Identity[] emailRecipientIdentities) {
		int elCnt = emailRecipientIdentities.length;
		// 2..n recipients
		StringBuilder tmpDET = new StringBuilder();
		for (int i = 0; i < elCnt; i++) {
			tmpDET.append(emailRecipientIdentities[i].getUser().getProperty(UserConstants.EMAIL, null));
			if (i < elCnt - 1) {
				tmpDET.append(", ");
			}
		}
		return tmpDET.toString();
	}

	/**
	 * takes a List containing email Strings and converts them to a String containing the Email Strings separated by a <b>, </b>. The returned String can be fed directly
	 * to the e-mailer helper as the e-mail to field. <br>
	 * <ul>
	 * <li>Entries in the parameter emailRecipients are expected to be not null and of Type String.</li>
	 * </ul>
	 * 
	 * @param emailRecipients
	 * @param delimiter
	 * @return "email1, email2, email3," or null if emailRecipientIdentites was null
	 */
	public static String formatIdentitesAsEmailToString(final List emailRecipients, String delimiter) {
		int elCnt = emailRecipients.size();
		// 2..n recipients
		StringBuilder tmpDET = new StringBuilder();
		for (int i = 0; i < elCnt; i++) {
			tmpDET.append((String) emailRecipients.get(i));
			if (i < elCnt - 1) {
				tmpDET.append(delimiter);
			}
		}
		return tmpDET.toString();
	}

	/**
	 * @param cellValue
	 * @return stripped string
	 */
	public static String stripLineBreaks(String cellValue) {
		cellValue = cellValue.replace('\n', ' ');
		cellValue = cellValue.replace('\r', ' ');
		return cellValue;
	}

	/**
	 * transforms a displayname to a name that causes no problems on the filesystem (e.g. Webclass Energie 2004/2005 -> Webclass_Energie_2004_2005)
	 * 
	 * @param s
	 * @return transformed string
	 */
	public static String transformDisplayNameToFileSystemName(String s) {
		s = s.replace('?', '_');
		s = s.replace('/', '_');
		s = s.replace(' ', '_');
		return s;
	}

	/**
	 * @param extractedCharset
	 * @return
	 */
	public static String check4xMacRoman(String extractedCharset) {
		// OLAT-1844
		// TODO:pb: why do http encoding names not match java encoding names?
		// the encoding name 'x-mac-roman' must be translated to javas 'x-MacRoman'
		// but it must be x-mac-roman for browser and htmleditor.. weird naming problem.
		if (extractedCharset == null) return null;
		if (extractedCharset.toLowerCase().startsWith(X_MAC_ENC)) {
			String tmp = extractedCharset.substring(6);
			String first = tmp.substring(0, 1);
			tmp = tmp.substring(1);
			// e.g. convert 'x-mac-roman' to 'x-MacRoman'
			extractedCharset = "x-Mac" + first.toUpperCase() + tmp;
			return extractedCharset;
		} else if (extractedCharset.toLowerCase().startsWith(MAC_ENC)) {
			// word for macintosh creates charset=macintosh which java does not know, load with iso-8859-1
			return "iso-8859-1";
		}
		return extractedCharset;
	}

	/**
	 * set of strings to one string comma separated.<br>
	 * e.g. ["a","b","c","s"] -> "a,b,c,s"
	 * 
	 * @param selection
	 * @return
	 */
	public static String formatAsCSVString(Set<String> entries) {
		boolean isFirst = true;
		String csvStr = null;
		for (Iterator<String> iter = entries.iterator(); iter.hasNext();) {
			String group = iter.next();
			if (isFirst) {
				csvStr = group;
				isFirst = false;
			} else {
				csvStr += ", " + group;
			}
		}
		return csvStr;
	}

	/**
	 * list of strings to one string comma separated.<br>
	 * e.g. ["a","b","c","s"] -> "a,b,c,s"
	 * 
	 * @param selection
	 * @return
	 */
	public static String formatAsCSVString(List<String> entries) {
		boolean isFirst = true;
		String csvStr = null;
		for (Iterator<String> iter = entries.iterator(); iter.hasNext();) {
			String group = iter.next();
			if (isFirst) {
				csvStr = group;
				isFirst = false;
			} else {
				csvStr += ", " + group;
			}
		}
		return csvStr;
	}

	/**
	 * list of strings to one string comma separated.<br>
	 * e.g. ["z","a","b","c","s","a"] -> "a, b, c, s, z" No duplicates, alphabetically sorted
	 * 
	 * @param selection
	 * @return
	 */
	public static String formatAsSortUniqCSVString(List<String> s) {

		Map<String, String> u = new HashMap<String, String>();
		for (Iterator<String> si = s.iterator(); si.hasNext();) {
			u.put(si.next().trim(), null);
		}

		List<String> rv = new ArrayList<String>();
		rv.addAll(u.keySet());
		rv.remove("");
		Collections.sort(rv);

		return formatAsCSVString(rv);
	}

	/**
	 * list of strings to one string comma separated.<br>
	 * e.g. ["z","a","b","c","s","a"] -> "a, b, c, s, z" No duplicates, alphabetically sorted
	 * 
	 * @param selection
	 * @return
	 */
	public static String formatAsSortUniqCSVString(Set<String> s) {

		Map<String, String> u = new HashMap<String, String>();
		for (Iterator<String> si = s.iterator(); si.hasNext();) {
			u.put(si.next().trim(), null);
		}

		List<String> rv = new ArrayList<String>();
		rv.addAll(u.keySet());
		rv.remove("");
		Collections.sort(rv);

		return formatAsCSVString(rv);
	}
}