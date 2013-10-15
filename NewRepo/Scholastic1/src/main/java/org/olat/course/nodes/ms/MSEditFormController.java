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
 * Copyright (c) 2009 frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */

package org.olat.course.nodes.ms;

import java.util.Map;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.RichTextElement;
import org.olat.core.gui.components.form.flexible.elements.SelectionElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.StringHelper;
import org.olat.course.nodes.MSCourseNode;
import org.olat.modules.ModuleConfiguration;

/**
 * Provides a FlexiForm for the assesment settings dialog, including custom rules for when to show which dialog element.
 * 
 * @author twuersch
 */
public class MSEditFormController extends FormBasicController {

	/** Configuration this controller will modify. */
	private final ModuleConfiguration modConfig;

	/** whether score will be awarded or not. */
	private SelectionElement scoreGranted;

	/** Dropdown for choosing whether pass/fail will be displayed or not. */
	private SelectionElement displayPassed;

	/**
	 * whether pass and fail will be decided automatically or manually.
	 */
	private SingleSelection displayType;

	/** whether results will be commented individually. */
	private SelectionElement commentFlag;

	/** Text input element for the minimum score. */
	private TextElement minVal;

	/** Text input element for the maximum score. */
	private TextElement maxVal;

	/** Text input element for the passing score. */
	private TextElement cutVal;

	/** Rich text input element for a notice to all users. */
	private RichTextElement infotextUser;

	/** Rich text input element for a notice to all tutors. */
	private RichTextElement infotextCoach;

	/** The keys for true / false dropdowns. */
	private final String[] trueFalseKeys;

	/** The keys for yes/no dropdowns. */
	private final String[] yesNoValues;

	/** The keys for manual/automatic scoring dropdown. */
	private final String[] passedTypeValues;

	private static final String scoreRex = "^[0-9]+(\\.[0-9]+)?$";

	/**
	 * Creates this controller.
	 * 
	 * @param ureq
	 * @param wControl
	 * @param modConfig
	 */
	public MSEditFormController(final UserRequest ureq, final WindowControl wControl, final ModuleConfiguration modConfig) {
		super(ureq, wControl, FormBasicController.LAYOUT_DEFAULT);
		this.modConfig = modConfig;
		this.trueFalseKeys = new String[] { Boolean.TRUE.toString(), Boolean.FALSE.toString() };
		this.yesNoValues = new String[] { translate("form.yes"), translate("form.no") };

		this.passedTypeValues = new String[] { translate("form.passedtype.cutval"), translate("form.passedtype.manual") };
		initForm(ureq);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#doDispose ()
	 */
	@Override
	protected void doDispose() {
		// Don't dispose anything
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formOK (org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void formOK(final UserRequest ureq) {
		fireEvent(ureq, Event.DONE_EVENT);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formNOK (org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void formNOK(final UserRequest ureq) {
		fireEvent(ureq, Event.FAILED_EVENT);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formCancelled(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void formCancelled(final UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#initForm (org.olat.core.gui.components.form.flexible.FormItemContainer,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
		// Create the "score granted" field...
		scoreGranted = uifactory.addCheckboxesVertical("form.score", formLayout, new String[] { "xx" }, new String[] { null }, null, 1);
		scoreGranted.addActionListener(this, FormEvent.ONCLICK);
		final Boolean sf = (Boolean) modConfig.get(MSCourseNode.CONFIG_KEY_HAS_SCORE_FIELD);
		scoreGranted.select("xx", sf == null ? false : sf.booleanValue());

		// ...minimum value...
		Float min = (Float) modConfig.get(MSCourseNode.CONFIG_KEY_SCORE_MIN);
		if (min == null) {
			min = new Float(0.0);
		}
		minVal = uifactory.addTextElement("form.min", "form.min", 8, min.toString(), formLayout);
		minVal.setDisplaySize(5);
		minVal.setRegexMatchCheck(scoreRex, "form.error.wrongFloat");

		Float max = (Float) modConfig.get(MSCourseNode.CONFIG_KEY_SCORE_MAX);
		if (max == null) {
			max = new Float(0.0);
		}
		// ...and maximum value input.
		maxVal = uifactory.addTextElement("form.max", "form.max", 8, max.toString(), formLayout);
		maxVal.setDisplaySize(5);
		maxVal.setRegexMatchCheck(scoreRex, "form.error.wrongFloat");

		uifactory.addSpacerElement("spacer1", formLayout, false);

		// Create the "display passed / failed"
		displayPassed = uifactory.addCheckboxesVertical("form.passed", formLayout, new String[] { "xx" }, new String[] { null }, null, 1);
		displayPassed.addActionListener(this, FormEvent.ONCLICK);
		Boolean pf = (Boolean) modConfig.get(MSCourseNode.CONFIG_KEY_HAS_PASSED_FIELD);
		if (pf == null) {
			pf = Boolean.TRUE;
		}
		displayPassed.select("xx", pf);

		// ...the automatic / manual dropdown (note that TRUE means automatic and
		// FALSE means manually)...
		Float cut = (Float) modConfig.get(MSCourseNode.CONFIG_KEY_PASSED_CUT_VALUE);
		displayType = uifactory.addRadiosVertical("form.passed.type", formLayout, trueFalseKeys, passedTypeValues);
		displayType.addActionListener(this, FormEvent.ONCLICK);

		displayType.select(trueFalseKeys[1], true);
		if (cut != null) {
			displayType.select(trueFalseKeys[0], true);
		}

		// ...and the passing grade input field.
		if (cut == null) {
			cut = new Float(0.0);
		}
		cutVal = uifactory.addTextElement("form.cut", "form.cut", 8, cut.toString(), formLayout);
		cutVal.setDisplaySize(5);
		cutVal.setRegexMatchCheck(scoreRex, "form.error.wrongFloat");

		uifactory.addSpacerElement("spacer2", formLayout, false);

		// Create the "individual comment" dropdown.
		commentFlag = uifactory.addCheckboxesVertical("form.comment", formLayout, new String[] { "xx" }, new String[] { null }, null, 1);
		Boolean cf = (Boolean) modConfig.get(MSCourseNode.CONFIG_KEY_HAS_COMMENT_FIELD);
		if (cf == null) {
			cf = Boolean.TRUE;
		}
		commentFlag.select("xx", cf.booleanValue());

		uifactory.addSpacerElement("spacer3", formLayout, false);

		// Create the rich text fields.
		String infoUser = (String) modConfig.get(MSCourseNode.CONFIG_KEY_INFOTEXT_USER);
		if (infoUser == null) {
			infoUser = new String("");
		}
		infotextUser = uifactory.addRichTextElementForStringDataMinimalistic("infotextUser", "form.infotext.user", infoUser, 10, -1, false, formLayout,
				ureq.getUserSession(), getWindowControl());

		String infoCoach = (String) modConfig.get(MSCourseNode.CONFIG_KEY_INFOTEXT_COACH);
		if (infoCoach == null) {
			infoCoach = new String("");
		}
		infotextCoach = uifactory.addRichTextElementForStringDataMinimalistic("infotextCoach", "form.infotext.coach", infoCoach, 10, -1, false, formLayout,
				ureq.getUserSession(), getWindowControl());

		// Create submit and cancel buttons
		final FormLayoutContainer buttonLayout = FormLayoutContainer.createButtonLayout("buttonLayout", getTranslator());
		formLayout.add(buttonLayout);
		uifactory.addFormSubmitButton("submit", buttonLayout);
		uifactory.addFormCancelButton("cancel", buttonLayout, ureq, getWindowControl());

		update(ureq);
	}

	@Override
	protected void formInnerEvent(final UserRequest ureq, final FormItem source, final FormEvent event) {
		update(ureq);
	}

	private void update(final UserRequest ureq) {
		minVal.setVisible(scoreGranted.isSelected(0));
		maxVal.setVisible(scoreGranted.isSelected(0));
		displayType.setVisible(displayPassed.isSelected(0));
		cutVal.setVisible(displayType.isVisible() && displayType.isSelected(0));
		validateFormLogic(ureq);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#validateFormLogic(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected boolean validateFormLogic(final UserRequest ureq) {

		// coach info text
		if (infotextCoach.getValue().length() > 4000) {
			infotextCoach.setErrorKey("input.toolong", null);
			return false;
		} else {
			infotextCoach.clearError();
		}
		// user info text
		if (infotextUser.getValue().length() > 4000) {
			infotextUser.setErrorKey("input.toolong", null);
			return false;
		} else {
			infotextUser.clearError();
		}
		// score flag
		if (scoreGranted.isSelected(0)) {
			if (!minVal.getValue().matches(scoreRex)) {
				minVal.setErrorKey("form.error.wrongFloat", null);
				return false;
			} else {
				minVal.clearError();
			}
			if (!maxVal.getValue().matches(scoreRex)) {
				maxVal.setErrorKey("form.error.wrongFloat", null);
				return false;
			} else if (Float.parseFloat(minVal.getValue()) > Float.parseFloat(maxVal.getValue())) {
				maxVal.setErrorKey("form.error.minGreaterThanMax", null);
				return false;
			} else {
				maxVal.clearError();
			}
		}
		// display flag
		displayType.clearError();
		if (displayPassed.isSelected(0) && displayType.isSelected(0)) {
			if (Boolean.valueOf(displayType.getSelectedKey()).booleanValue() && !scoreGranted.isSelected(0)) {
				displayType.setErrorKey("form.error.cutButNoScore", null);
				return false;
			}
			if (!cutVal.getValue().matches(scoreRex)) {
				cutVal.setErrorKey("form.error.wrongFloat", null);
				return false;
			} else if (Float.parseFloat(cutVal.getValue()) < Float.parseFloat(minVal.getValue())
					|| Float.parseFloat(cutVal.getValue()) > Float.parseFloat(maxVal.getValue())) {
				cutVal.setErrorKey("form.error.cutOutOfRange", null);
				return false;
			} else {
				cutVal.clearError();
			}
		}
		return true;
	}

	/**
	 * Sets this form to be write-protected.
	 * 
	 * @param displayOnly
	 */
	public void setDisplayOnly(final boolean displayOnly) {
		final Map<String, FormItem> formItems = flc.getFormComponents();
		for (final String formItemName : formItems.keySet()) {
			// formItems.get(formItemName).setVisible(true);
			formItems.get(formItemName).setEnabled(!displayOnly);
		}
	}

	/**
	 * @param moduleConfiguration
	 */
	public void updateModuleConfiguration(final ModuleConfiguration moduleConfiguration) {
		// mandatory score flag
		final Boolean sf = new Boolean(scoreGranted.isSelected(0));
		moduleConfiguration.set(MSCourseNode.CONFIG_KEY_HAS_SCORE_FIELD, sf);
		if (sf.booleanValue()) {
			// do min/max value
			moduleConfiguration.set(MSCourseNode.CONFIG_KEY_SCORE_MIN, new Float(minVal.getValue()));
			moduleConfiguration.set(MSCourseNode.CONFIG_KEY_SCORE_MAX, new Float(maxVal.getValue()));
		} else {
			// remove old config
			moduleConfiguration.remove(MSCourseNode.CONFIG_KEY_SCORE_MIN);
			moduleConfiguration.remove(MSCourseNode.CONFIG_KEY_SCORE_MAX);
		}

		// mandatory passed flag
		final Boolean pf = new Boolean(displayPassed.isSelected(0));
		moduleConfiguration.set(MSCourseNode.CONFIG_KEY_HAS_PASSED_FIELD, pf);
		if (pf.booleanValue()) {
			// do cut value
			final Boolean cf = new Boolean(displayType.getSelectedKey());
			if (cf.booleanValue()) {
				moduleConfiguration.set(MSCourseNode.CONFIG_KEY_PASSED_CUT_VALUE, new Float(cutVal.getValue()));
			} else {
				// remove old config
				moduleConfiguration.remove(MSCourseNode.CONFIG_KEY_PASSED_CUT_VALUE);
			}
		} else {
			// remove old config
			moduleConfiguration.remove(MSCourseNode.CONFIG_KEY_PASSED_CUT_VALUE);
		}

		// mandatory comment flag
		moduleConfiguration.set(MSCourseNode.CONFIG_KEY_HAS_COMMENT_FIELD, new Boolean(commentFlag.isSelected(0)));

		// set info text only if something is in there
		final String iu = infotextUser.getValue();
		if (StringHelper.containsNonWhitespace(iu)) {
			moduleConfiguration.set(MSCourseNode.CONFIG_KEY_INFOTEXT_USER, iu);
		} else {
			// remove old config
			moduleConfiguration.remove(MSCourseNode.CONFIG_KEY_INFOTEXT_USER);
		}

		final String ic = infotextCoach.getValue();
		if (StringHelper.containsNonWhitespace(ic)) {
			moduleConfiguration.set(MSCourseNode.CONFIG_KEY_INFOTEXT_COACH, ic);
		} else {
			// remove old config
			moduleConfiguration.remove(MSCourseNode.CONFIG_KEY_INFOTEXT_COACH);
		}
	}

	/**
	 * @param config the module configuration
	 * @return true if valid, false otherwhise
	 */
	public static boolean isConfigValid(final ModuleConfiguration config) {
		boolean isValid = true;
		Object confElement;

		// score flag is mandatory
		confElement = config.get(MSCourseNode.CONFIG_KEY_HAS_SCORE_FIELD);
		if (confElement != null && confElement instanceof Boolean) {
			final Boolean hasScore = (Boolean) confElement;
			if (hasScore.booleanValue()) {
				// score min and max are mandatory if score flag is set to true
				confElement = config.get(MSCourseNode.CONFIG_KEY_SCORE_MIN);
				isValid = (confElement != null && confElement instanceof Float);
				confElement = config.get(MSCourseNode.CONFIG_KEY_SCORE_MAX);
				isValid = (confElement != null && confElement instanceof Float);
			}
		} else {
			return false;
		}

		// passed flag is mandatory
		confElement = config.get(MSCourseNode.CONFIG_KEY_HAS_PASSED_FIELD);
		if (confElement != null && confElement instanceof Boolean) {
			final Boolean hasPassed = (Boolean) confElement;
			if (hasPassed.booleanValue()) {
				// cut value is optional if passed flag set to true, but type must match
				confElement = config.get(MSCourseNode.CONFIG_KEY_PASSED_CUT_VALUE);
				if (!((confElement == null) || (confElement instanceof Float))) { return false; }
			}
		} else {
			return false;
		}

		// comment flag is mandatory
		confElement = config.get(MSCourseNode.CONFIG_KEY_HAS_COMMENT_FIELD);
		isValid = (confElement != null && confElement instanceof Boolean);

		// infotext is optional
		confElement = config.get(MSCourseNode.CONFIG_KEY_INFOTEXT_USER);
		if (!((confElement == null) || (confElement instanceof String))) { return false; }

		confElement = config.get(MSCourseNode.CONFIG_KEY_INFOTEXT_COACH);
		if (!((confElement == null) || (confElement instanceof String))) { return false; }

		return isValid;
	}
}
