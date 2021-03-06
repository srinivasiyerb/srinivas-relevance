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

package org.olat.course.run.scoring;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

/**
 * Description:<br>
 * The score calculator stores the expression which is used to calculate a users score and passed value for a structure node. The expression uses the same condition
 * interpreter as in the node conditions.
 * <P>
 * 
 * @author Felix Jost
 */
public class ScoreCalculator implements Serializable {
	private String scoreExpression;
	private String passedExpression;

	/** config flag: no passed configured **/
	public static final String PASSED_TYPE_NONE = "no";
	/** config flag: passed based on cutvalue **/
	public static final String PASSED_TYPE_CUTVALUE = "cut";
	/** config flag: passed inherited from other course nodes **/
	public static final String PASSED_TYPE_INHERIT = "inherit";

	private boolean expertMode = false;
	// easy mode variables
	// score configuration
	private List sumOfScoreNodes;
	// passed configuration
	private String passedType;
	private List passedNodes;
	private int passedCutValue;

	/**
	 * @param scoreExpression
	 * @param passedExpression
	 */
	public ScoreCalculator(final String scoreExpression, final String passedExpression) {
		this.scoreExpression = scoreExpression;
		this.passedExpression = passedExpression;
	}

	/**
	 * @return Returns the passedExpression. if null, then there is no expression to calculate
	 */
	public String getPassedExpression() {
		// always return expression, even if in easy mode! whenever something in the easy mode
		// hase been changed the one who changes something must also set the passedExpression
		// to the new correct value using something like
		// sc.setScoreExpression(sc.getScoreExpressionFromEasyModeConfiguration());
		return passedExpression;
	}

	/**
	 * @return Returns the scoreExpression. if null, then there is no expression to calculate
	 */
	public String getScoreExpression() {
		// always return expression, even if in easy mode! whenever something in the easy mode
		// hase been changed the one who changes something must also set the passedExpression
		// to the new correct value using something like
		// sc.setScoreExpression(sc.getScoreExpressionFromEasyModeConfiguration());
		return scoreExpression;
	}

	/**
	 * Calculate the score expression based on the easy mode configuration. This must not be used during calcualtion of a score but after changeing an expression in the
	 * editor to set the new score expression
	 * 
	 * @return String
	 */
	public String getScoreExpressionFromEasyModeConfiguration() {
		final StringBuilder sb = new StringBuilder();

		if (getSumOfScoreNodes() != null && getSumOfScoreNodes().size() > 0) {
			sb.append("(");
			final Iterator iter = getSumOfScoreNodes().iterator();
			while (iter.hasNext()) {
				final String nodeIdent = (String) iter.next();
				sb.append("getScore(\"");
				sb.append(nodeIdent);
				sb.append("\")");
				if (iter.hasNext()) {
					sb.append(" + ");
				}
			}
			sb.append(")");
		}

		if (sb.length() == 0) {
			return null;
		} else {
			return sb.toString();
		}
	}

	/**
	 * Calculate the passed expression based on the easy mode configuration. This must not be used during calcualtion of a passed but after changeing an expression in the
	 * editor to set the new passed expression
	 * 
	 * @return String
	 */
	public String getPassedExpressionFromEasyModeConfiguration() {
		if (getPassedType() == null || getPassedType().equals(PASSED_TYPE_NONE)) { return null; }
		final StringBuilder sb = new StringBuilder();
		if (getPassedType().equals(PASSED_TYPE_INHERIT) && getPassedNodes() != null && getPassedNodes().size() > 0) {
			sb.append("(");
			final Iterator iter = getPassedNodes().iterator();
			while (iter.hasNext()) {
				final String nodeIdent = (String) iter.next();
				sb.append("getPassed(\"");
				sb.append(nodeIdent);
				sb.append("\")");
				if (iter.hasNext()) {
					sb.append(" & ");
				}
			}
			sb.append(")");
		} else if (getPassedType().equals(PASSED_TYPE_CUTVALUE)) {
			sb.append(getScoreExpressionFromEasyModeConfiguration());
			sb.append(" >= ");
			sb.append(getPassedCutValue());
		}

		if (sb.length() == 0) {
			return null;
		} else {
			return sb.toString();
		}
	}

	/**
	 * @return boolean true when in expert mode, false when in easy mode
	 */
	public boolean isExpertMode() {
		return expertMode;
	}

	/**
	 * @param expertMode true when in expert mode, false when in easy mode
	 */
	public void setExpertMode(final boolean expertMode) {
		this.expertMode = expertMode;
	}

	/**
	 * @return List of nodeIdents as Strings
	 */
	public List getSumOfScoreNodes() {
		return sumOfScoreNodes;
	}

	/**
	 * @param sumOfScoreNodes
	 */
	public void setSumOfScoreNodes(final List sumOfScoreNodes) {
		this.sumOfScoreNodes = sumOfScoreNodes;
	}

	/**
	 * @param passedExpression
	 */
	public void setPassedExpression(final String passedExpression) {
		this.passedExpression = passedExpression;
	}

	/**
	 * @param scoreExpression
	 */
	public void setScoreExpression(final String scoreExpression) {
		this.scoreExpression = scoreExpression;
	}

	/**
	 * @return int
	 */
	public int getPassedCutValue() {
		return passedCutValue;
	}

	/**
	 * @param passedCutValue
	 */
	public void setPassedCutValue(final int passedCutValue) {
		this.passedCutValue = passedCutValue;
	}

	/**
	 * @return List of nodeIdents as Strings
	 */
	public List getPassedNodes() {
		return passedNodes;
	}

	/**
	 * @param passedNodes
	 */
	public void setPassedNodes(final List passedNodes) {
		this.passedNodes = passedNodes;
	}

	/**
	 * @return String
	 */
	public String getPassedType() {
		return passedType;
	}

	/**
	 * @param passedType
	 */
	public void setPassedType(final String passedType) {
		this.passedType = passedType;
	}

	/**
	 * clears the fields set in easy mode back to default values.
	 */
	public void clearEasyMode() {
		passedCutValue = 0;
		passedNodes = null;
		passedType = PASSED_TYPE_NONE;
		sumOfScoreNodes = null;
	}

}
