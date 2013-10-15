package org.olat.modules.scorm.server.sequence;

/**
 * RELOAD TOOLS Copyright (c) 2003 Oleg Liber, Bill Olivier, Phillip Beauvoir, Paul Sharples Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to
 * the following conditions: The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software. THE SOFTWARE
 * IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. Project Management Contact: Oleg Liber Bolton
 * Institute of Higher Education Deane Road Bolton BL3 5AB UK e-mail: o.liber@bolton.ac.uk Technical Contact: Phillip Beauvoir e-mail: p.beauvoir@bolton.ac.uk Web:
 * http://www.reload.ac.uk
 */

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.olat.core.logging.Tracing;
import org.olat.core.manager.BasicManager;
import org.olat.modules.scorm.ISettingsHandler;
import org.olat.modules.scorm.server.servermodels.SequencerModel;

import bsh.EvalError;
import bsh.Interpreter;

/**
 * A class to handle prerequisites found for items in the manifest. Uses http://www.beanshell.org/
 * 
 * @author Paul Sharples
 */
public class PrerequisiteManager extends BasicManager {
	// A hashtable of all key (scoIDs) and values (status)
	private Hashtable _prereqTable = new Hashtable();
	/**
	 * the disk version of the model
	 */
	protected SequencerModel _sequencerModel;

	private final ISettingsHandler settings;

	/**
	 * Constructor which allows the disk model to be loaded into the manager
	 * 
	 * @param org
	 */
	public PrerequisiteManager(final String org, final ISettingsHandler settings) {
		this.settings = settings;
		if (!populateFromDisk(org)) {
			System.out.println("could not load in tracking model");
		}
	}

	/**
	 * Method to get the disk version of the package (what/what hasn't has been completed)
	 * 
	 * @param org
	 * @return true is successful
	 */
	protected boolean populateFromDisk(final String org) {
		_sequencerModel = new SequencerModel(settings.getScoItemSequenceFile(), settings);
		_prereqTable = _sequencerModel.getItemsAsHash(org);
		return (_prereqTable != null);
	}

	/**
	 * A method to allow us to keep our prerequisite table up-to-date
	 * 
	 * @param sco - the id of the sco
	 * @param status - its current status
	 * @param persist
	 */
	public void updatePrerequisites(final String sco, final String status, final boolean persist) {
		_prereqTable.put(sco, status);
		if (persist) {
			_sequencerModel.updateDiskModel(sco, status);
		}
	}

	/**
	 * Method to find if an item should be launched 1. has it already been completed 2. does it rely on any prerequisites
	 * 
	 * @param sco
	 * @param prerequisites
	 * @return true if possible
	 */
	public boolean canLaunchItem(final String sco, final String prerequisites) {
		if (!doesItemExist(sco)) { return false; }
		final String stat = (String) _prereqTable.get(sco);
		if (stat.equals(SequencerModel.ITEM_COMPLETED) || stat.equals(SequencerModel.ITEM_PASSED)) { return false; }
		if (!prerequisites.equals("")) { return checkPrerequisites(prerequisites); }
		return true;
	}

	/**
	 * Method to find if an item should be launched 1. has it already been completed
	 * 
	 * @param sco
	 * @return true if item is completed
	 */
	public boolean hasItemBeenCompleted(final String sco) {
		if (!doesItemExist(sco)) { return true; }
		final String stat = (String) _prereqTable.get(sco);
		return (stat.equals(SequencerModel.ITEM_COMPLETED) || stat.equals(SequencerModel.ITEM_PASSED));
	}

	/**
	 * @param prereq
	 * @return true if ???
	 */
	public boolean checkPrerequisites(String prereq) {
		try {
			final Interpreter i = new Interpreter(); // Construct an interpreter
			final StringTokenizer st1 = new StringTokenizer(prereq, "&|()~");
			StringTokenizer itemAndValue;
			String aToken = "";
			while (st1.hasMoreTokens()) {
				aToken = st1.nextToken();
				// if theres no value in quotes to check against..
				if (aToken.indexOf("=") == -1 && aToken.indexOf("<>") == -1) {
					// get boolean status
					if (!doesItemExist(aToken)) {
						// System.out.println("item does not exist"+ aToken);
						return true;// identifer does not exist, so junk.
					}
					// item exists in prerequisites table. Has it been completed
					// or passed -grab the answer and declare the var for the interpreter
					i.set(aToken.replaceAll("-", "_"), checkStatus(aToken));
				} else {
					itemAndValue = new StringTokenizer(aToken, "=<>");
					final String anToken = itemAndValue.nextToken();
					if (!doesItemExist(anToken)) { return true; // identifer does not
					// exist, so junk..
					}
					// item exists in prerequisites table. Has it been
					// completed or passed -grab the answer and declare
					// the var for the interpreter...
					i.set(anToken.replaceAll("-", "_"), getStatus(anToken));
				}
			}
			// System.out.println("0. "+prereq);
			prereq = prereq.replaceAll("-", "_");
			prereq = prereq.replaceAll("&", "&&");
			prereq = prereq.replaceAll("\\|", "||");
			prereq = prereq.replaceAll("~", "!");
			prereq = prereq.replaceAll("<>\\\"", "@");
			prereq = prereq.replaceAll("=\\\"", "^");
			prereq = prereq.replaceAll("\\\"", "\")");
			prereq = prereq.replaceAll("\\^", "=\\\"");
			prereq = prereq.replaceAll("=", ".equals(");
			// System.out.println("1. "+prereq);
			if (prereq.indexOf("@") != -1) {
				final Vector v = new Vector();
				final StringBuilder sb = new StringBuilder();
				sb.append(prereq);
				boolean notSymbFound = false;
				// go backwards char at a time. if you find &}"|& then insert a
				// !)
				for (int j = sb.length() - 1; j > -1; j--) {
					if (sb.charAt(j) == '@') {
						notSymbFound = true;
					}
					if (notSymbFound) {
						if (sb.charAt(j) == '|' || sb.charAt(j) == '&' || sb.charAt(j) == '"' || sb.charAt(j) == '}' || sb.charAt(j) == ')') {
							notSymbFound = false;
							v.add(j + 1 + "");
						}
					}
				}
				final Iterator it = v.iterator();
				while (it.hasNext()) {
					sb.insert(Integer.parseInt(it.next().toString()), '!');
				}
				if (notSymbFound) {
					// the ! must be at the beginning
					prereq = "!" + sb.toString();
				} else {
					prereq = sb.toString();
				}
			}
			prereq = prereq.replaceAll("@", ".equals(\"");
			final Object result = i.eval(prereq);
			final String a = result.toString();
			final boolean retVal = Boolean.valueOf(a).booleanValue();
			if (Tracing.isDebugEnabled(PrerequisiteManager.class)) {
				Tracing.logDebug("eval: " + prereq + " result was: " + retVal, PrerequisiteManager.class);
			}
			return retVal;
		} catch (final EvalError ex) {
			Tracing.logError("Could not parse prerequisites: ", ex, PrerequisiteManager.class);
			/*
			 * __FIXME:gs:a:[pb] guido is it correct to send true in the exception case? Could please leave a comment why you return true, although an exception occured.
			 * thx gs: prerequisites decides about going further in scorm navigation to the next sco. I think returning true may assure that in a failing case you can
			 * continue.
			 */
			return true;
		}
	}

	protected boolean doesItemExist(final String sco) {
		return (_prereqTable.containsKey(sco));
	}

	protected String getStatus(final String sco) {
		return (String) _prereqTable.get(sco);
	}

	protected boolean checkStatus(final String sco) {
		final String stat = (String) _prereqTable.get(sco);
		return (stat.equals(SequencerModel.ITEM_COMPLETED) || stat.equals(SequencerModel.ITEM_PASSED));
	}

	/**
	 * A utillity method for testing purposes - prints out the current state of the prerequisites table.
	 */
	public void showPreReqTable() {
		System.out.println("-----------------------");
		System.out.println("-------prereq table ---");
		System.out.println("-----------------------");
		if (_prereqTable != null) {
			final Enumeration keys = _prereqTable.keys();
			while (keys.hasMoreElements()) {
				final String scoID = (String) keys.nextElement();
				final String theStatus = (String) _prereqTable.get(scoID);
				System.out.println("SCO ID: " + scoID + "  status: " + theStatus);
			}
		}
		System.out.println("-----------------------\n\n");
	}

	/**
	 * A utillity method to test a prerequisite string to see if it is legal. (mainly to check that there are no illegal chars in it) More rubust checking will be made
	 * once the prerequisite is parsed. Note: Because sets are not yet supported, ie 2*{S1,S2,S3} we will not flag for ,*{} characters. This will be addressed in a later
	 * version.
	 * 
	 * @param aprereq
	 * @return true or false
	 */
	public static boolean isValid(final String aprereq) {
		Pattern pattern;
		Matcher matcher;
		// now lets do a little pattern matching - look for illegal chars
		// String pat =
		// "[^a-zA-Z0-9\\&\\|\\~\\=\\<\\>\\\"\\*\\,\\{\\}\\-\\_\\(\\)\\s]+"; // with
		// sets
		final String pat = "[^a-zA-Z0-9\\&\\|\\~\\=\\<\\>\\\"\\-\\_\\(\\)\\s]+"; // without
		// sets
		pattern = Pattern.compile(pat);
		matcher = pattern.matcher(aprereq);
		// make sure there is an even number of quotes
		final int quoteCount = countOccurences(aprereq, "\"");
		if (quoteCount % 2 != 0) {
			// System.out.println("odd number of quotes" + quoteCount);
			return false;
		}
		/*
		 * // Sets not yet supported // make sure there is an even number of opening and closing curly braces int openCurlyCount = countOccurences(aprereq, "{"); int
		 * closeCurlyCount = countOccurences(aprereq, "}"); if (openCurlyCount != closeCurlyCount){ //System.out.println("incorrect number of opening and closing curly
		 * brackets: " //+ openCurlyCount + " :" + closeCurlyCount); return false; }
		 */
		// make sure there is an even number of opening and closing bracketss
		final int openBracketCount = countOccurences(aprereq, "(");
		final int closeBracketCount = countOccurences(aprereq, ")");
		if (openBracketCount != closeBracketCount) {
			// System.out.println("incorrect number of opening and closing brackets: "
			// + openBracketCount + " :" + closeBracketCount);
			return false;
		}
		// if illegal chars found
		if (matcher.find()) {
			// System.out.println("illegal string");
			return false;
		}
		return true;
	}

	/**
	 * Method to search a string to see how many occurences of a substring exist within it.
	 * 
	 * @param base - string to search
	 * @param searchFor - what to search for
	 * @return - the number found
	 */
	public static int countOccurences(final String base, final String searchFor) {
		final int len = searchFor.length();
		int result = 0;

		if (len > 0) { // search only if there is something
			int start = base.indexOf(searchFor);
			while (start != -1) {
				result++;
				start = base.indexOf(searchFor, start + len);
			}
		}
		return result;
	}

}
