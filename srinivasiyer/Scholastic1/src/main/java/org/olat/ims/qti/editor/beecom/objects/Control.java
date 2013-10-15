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

package org.olat.ims.qti.editor.beecom.objects;

import org.dom4j.Element;

public class Control implements QTIObject {

	public static final int CTRL_UNDEF = 0;
	public static final int CTRL_YES = 1;
	public static final int CTRL_NO = 2;

	private int feedback;
	private int hint;
	private int solution;
	private String view = null;

	public Control() {
		feedback = CTRL_UNDEF;
		hint = CTRL_UNDEF;
		solution = CTRL_UNDEF;
	}

	/**
	 * Constructor for Switches.
	 */
	public Control(final int feedback, final int hint, final int solution) {
		setSwitches(feedback, hint, solution);
	}

	public Control(final String feedback, final String hint, final String solution) {
		setSwitches(feedback, hint, solution);
	}

	public void setSwitches(final int feedback, final int hint, final int solution) {
		this.feedback = feedback;
		this.hint = hint;
		this.solution = solution;
	}

	public void setSwitches(final String feedback, final String hint, final String solution) {
		if (feedback == null || feedback.length() == 0) {
			this.feedback = CTRL_UNDEF;
		} else if (feedback.equalsIgnoreCase("yes")) {
			this.feedback = CTRL_YES;
		} else if (feedback.equalsIgnoreCase("no")) {
			this.feedback = CTRL_NO;
		}

		if (hint == null || hint.length() == 0) {
			this.hint = CTRL_UNDEF;
		} else if (hint.equalsIgnoreCase("yes")) {
			this.hint = CTRL_YES;
		} else if (hint.equalsIgnoreCase("no")) {
			this.hint = CTRL_NO;
		}

		if (solution == null || solution.length() == 0) {
			this.solution = CTRL_UNDEF;
		} else if (solution.equalsIgnoreCase("yes")) {
			this.solution = CTRL_YES;
		} else if (solution.equalsIgnoreCase("no")) {
			this.solution = CTRL_NO;
		}
	}

	/**
	 * @see org.olat.ims.qti.editor.beecom.QTIObject#addToElement(org.dom4j.Element)
	 */
	@Override
	public void addToElement(final Element root) {
		if (feedback == CTRL_UNDEF && hint == CTRL_UNDEF && solution == CTRL_UNDEF) { return; }

		final String name = root.getName();
		final Element control = root.addElement(name + "control");
		if (feedback != CTRL_UNDEF) {
			control.addAttribute("feedbackswitch", feedback == CTRL_YES ? "Yes" : "No");
		}
		if (hint != CTRL_UNDEF) {
			control.addAttribute("hintswitch", hint == CTRL_YES ? "Yes" : "No");
		}
		if (solution != CTRL_UNDEF) {
			control.addAttribute("solutionswitch", solution == CTRL_YES ? "Yes" : "No");
		}

		if (name.equalsIgnoreCase("item") && this.getView() != null) {
			control.addAttribute("view", this.getView());
		}
	}

	/**
	 * Returns the feedback.
	 * 
	 * @return boolean
	 */
	public boolean isFeedback() {
		return feedback == CTRL_YES;
	}

	/**
	 * Returns the hints.
	 * 
	 * @return boolean
	 */
	public boolean isHint() {
		return hint == CTRL_YES;
	}

	/**
	 * Returns the solutions.
	 * 
	 * @return boolean
	 */
	public boolean isSolution() {
		return solution == CTRL_YES;
	}

	/**
	 * Sets the feedback.
	 * 
	 * @param feedback The feedback to set
	 */
	public void setFeedback(final int feedback) {
		this.feedback = feedback;
	}

	/**
	 * Sets the hints.
	 * 
	 * @param hints The hints to set
	 */
	public void setHint(final int hint) {
		this.hint = hint;
	}

	/**
	 * Sets the solutions.
	 * 
	 * @param solutions The solutions to set
	 */
	public void setSolution(final int solution) {
		this.solution = solution;
	}

	/**
	 * Returns the view.
	 * 
	 * @return String
	 */
	public String getView() {
		return view;
	}

	/**
	 * Sets the view.
	 * 
	 * @param view The view to set
	 */
	public void setView(final String view) {
		this.view = view;
	}

	/**
	 * @return
	 */
	public int getFeedback() {
		return feedback;
	}

	/**
	 * @return
	 */
	public int getHint() {
		return hint;
	}

	/**
	 * @return
	 */
	public int getSolution() {
		return solution;
	}

}
