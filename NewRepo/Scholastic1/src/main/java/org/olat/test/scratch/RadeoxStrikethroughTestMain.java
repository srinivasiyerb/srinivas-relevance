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
 * Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.test.scratch;

/**
 * Description:<br>
 * Test driver to test radeox regexp for strikethrough and the multiline / dotall issues
 * <P>
 * Initial Date: Nov 8, 2005 <br>
 * 
 * @author gnaegi
 */
public class RadeoxStrikethroughTestMain {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {

		// origninal radeox stike pattern
		final String originalPatternString = "([^!-]|^)--(?=.*--)((?:(?:[^-]+)-?)+)--([^-]|$)";
		// modified radeox stike pattern
		final String modifiedPatternString = "([^-]|^)--([^-$^]+)--([^-]|$)";

		final String easyText = "--\r\n1234567890----\r\nAnd now --strike-- and now a --multiline \r\nstrike--";
		final String complicatedText = "--\r\n12345678901234567890----\r\nAnd now --strike-- and now a --multiline \r\nstrike--";
		final String moreComplicatedText = "--\r\n123456789012345678901234567890----\r\nAnd now --strike-it-- and now a --multiline \r\nstrike--\r\nasdf --\r\n--";

		// works well
		doStrikeFilter(modifiedPatternString, easyText);
		// performance issue starts
		doStrikeFilter(modifiedPatternString, complicatedText);
		// does not work at all
		doStrikeFilter(modifiedPatternString, moreComplicatedText);

		// now with the original pattern
		// works well
		doStrikeFilter(originalPatternString, easyText);
		// performance issue starts
		doStrikeFilter(originalPatternString, complicatedText);
		// does not work at all
		doStrikeFilter(originalPatternString, moreComplicatedText);

	}

	private static void doStrikeFilter(final String patternString, final String text) {
		System.out.println("doStrikeFilter(" + patternString + "," + text + ");");
		System.out.println("*** START ***");
		final String replacePattern = "$1<strike class=\"strike\">$2</strike>$3";

		// works well using MULTILINE but not DOTALL
		System.out.println("** Using only MULTILINE");
		final java.util.regex.Pattern jdkPattern = java.util.regex.Pattern.compile(patternString, java.util.regex.Pattern.MULTILINE);
		final java.util.regex.Matcher jdkMatcher = jdkPattern.matcher(text);
		System.out.println(jdkMatcher.replaceAll(replacePattern));

		// works sort of, but very, very, very slow
		System.out.println("** Using DOTALL but not multiline");
		final java.util.regex.Pattern jdkPattern2 = java.util.regex.Pattern.compile(patternString, java.util.regex.Pattern.DOTALL);
		final java.util.regex.Matcher jdkMatcher2 = jdkPattern2.matcher(text);
		System.out.println(jdkMatcher2.replaceAll(replacePattern));

		// same with radeox code, internaly using DOTALL
		System.out.println("** Using RADEOX style, multiline disabled");
		final org.radeox.regex.Pattern radeoxPattern = new org.radeox.regex.JdkPattern(patternString, false);
		final org.radeox.regex.Matcher radeoxMatcher = org.radeox.regex.Matcher.create(text, radeoxPattern);
		System.out.println(radeoxMatcher.substitute(replacePattern));

		System.out.println("** DONE **\n\n");
	}

}
