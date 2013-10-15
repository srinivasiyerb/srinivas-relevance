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
 * Copyright (c) 1999-2008 at frentix GmbH, Switzerland, http://www.frentix.com
 * <p>
 */
package org.olat.core.commons.modules.glossary;

import java.net.URI;
import java.text.Collator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Description:<br>
 * Represents a single entry in glossary.
 * <P>
 * Initial Date: 11.12.2008 <br>
 * 
 * @author Roman Haag, frentix GmbH, roman.haag@frentix.com
 */
public class GlossaryItem implements Comparable<Object> {

	private String glossTerm;
	private String glossDef;
	private ArrayList<GlossaryItem> glossSeeAlso;
	private ArrayList<String> glossFlexions;
	private ArrayList<String> glossSynonyms;
	private ArrayList<URI> glossLinks;

	public GlossaryItem(String glossTerm, String glossDef) {
		super();
		this.glossTerm = glossTerm;
		this.glossDef = glossDef;
	}

	/**
	 * returns first character from the Term as a String should return an alphanumerical in uppercase. make sure its uppercased only, if not numerical
	 */
	public String getIndex() {
		if (getGlossTerm().length() != 0) {
			String firstChar = getGlossTerm();
			firstChar = Normalizer.normalize(firstChar, Normalizer.Form.NFD).substring(0, 1);
			return firstChar.toUpperCase();
		} else {
			return "";
		}
	}

	/**
	 * Comparison of two GlossaryItem objects is based on the Term
	 * 
	 * @param arg0
	 * @return
	 */
	@Override
	public int compareTo(Object arg0) {
		// only compare against other GlossaryItem objects
		if (arg0 instanceof GlossaryItem) {
			GlossaryItem arg0Marker = (GlossaryItem) arg0;
			return Collator.getInstance(Locale.ENGLISH).compare(this.getGlossTerm(), arg0Marker.getGlossTerm());
		}
		return 0;
	}

	/**
	 * Check only term and ignore case
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof GlossaryItem) {
			GlossaryItem glossItem = (GlossaryItem) obj;
			if (getGlossTerm().equalsIgnoreCase(glossItem.getGlossTerm())) { return true; }
		}
		return false;
	}

	/**
	 * Delivers a List with all terms, which afterwards need to be highlighted in Text. contains glossTermn, glossSynonyms and glossFlexions.
	 * 
	 * @return allStrings
	 */
	public ArrayList<String> getAllStringsToMarkup() {
		ArrayList<String> allStrings = new ArrayList<String>();
		allStrings.add(getGlossTerm());
		allStrings.addAll(getGlossSynonyms());
		allStrings.addAll(getGlossFlexions());
		return allStrings;
	}

	/**
	 * @see java.lang.Object#toString() mainly used for debug reason
	 */
	@Override
	public String toString() {
		return getGlossTerm();
	}

	/**
	 * @return Returns the glossFlexions.
	 */
	public ArrayList<String> getGlossFlexions() {
		if (glossFlexions == null) return new ArrayList<String>();
		return glossFlexions;
	}

	/**
	 * @param glossFlexions The glossFlexions to set.
	 */
	public void setGlossFlexions(ArrayList<String> glossFlexions) {
		this.glossFlexions = glossFlexions;
	}

	/**
	 * @return Returns the glossSynonyms.
	 */
	public ArrayList<String> getGlossSynonyms() {
		if (glossSynonyms == null) return new ArrayList<String>();
		return glossSynonyms;
	}

	/**
	 * @param glossSynonyms The glossSynonyms to set.
	 */
	public void setGlossSynonyms(ArrayList<String> glossSynonyms) {
		this.glossSynonyms = glossSynonyms;
	}

	/**
	 * @return Returns the glossDef.
	 */
	public String getGlossDef() {
		return glossDef;
	}

	/**
	 * @param glossDef The glossDef to set.
	 */
	public void setGlossDef(String glossDef) {
		this.glossDef = glossDef;
	}

	/**
	 * @return Returns the glossLinks.
	 */
	public ArrayList<URI> getGlossLinks() {
		return glossLinks;
	}

	/**
	 * @param glossLinks The glossLinks to set.
	 */
	public void setGlossLinks(ArrayList<URI> glossLinks) {
		this.glossLinks = glossLinks;
	}

	/**
	 * @return Returns the glossSeeAlso.
	 */
	public ArrayList<GlossaryItem> getGlossSeeAlso() {
		return glossSeeAlso;
	}

	/**
	 * @param glossSeeAlso The glossSeeAlso to set.
	 */
	public void setGlossSeeAlso(ArrayList<GlossaryItem> glossSeeAlso) {
		this.glossSeeAlso = glossSeeAlso;
	}

	/**
	 * @return Returns the glossTerm.
	 */
	public String getGlossTerm() {
		return glossTerm;
	}

	/**
	 * @param glossTerm The glossTerm to set.
	 */
	public void setGlossTerm(String glossTerm) {
		this.glossTerm = glossTerm;
	}

}
