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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */
package org.olat.ims.qti.editor;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.IntegerElement;
import org.olat.core.gui.components.form.flexible.elements.RichTextElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.StaticTextElement;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.richText.RichTextConfiguration;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.filter.Filter;
import org.olat.core.util.filter.FilterFactory;
import org.olat.ims.qti.editor.beecom.objects.ChoiceQuestion;
import org.olat.ims.qti.editor.beecom.objects.Control;
import org.olat.ims.qti.editor.beecom.objects.Duration;
import org.olat.ims.qti.editor.beecom.objects.Item;
import org.olat.ims.qti.editor.beecom.objects.Question;

/**
 * This is the form controller responsible for item metadata like title and description but also for setting feedback information and hints and display properties. Fires:
 * Event.DONE_EVENT, NodeBeforeChangeEvent
 * <P>
 * Initial Date: Jul 17, 2009 <br>
 * 
 * @author gwassmann
 */
public class ItemMetadataFormController extends FormBasicController {

	private final Item item;
	private final boolean isSurvey, isRestrictedEditMode;
	private TextElement title;
	private RichTextElement desc, hint, solution;
	private SingleSelection layout, limitAttempts, limitTime, shuffle, showHints, showSolution;
	private IntegerElement attempts, timeMin, timeSec;
	private final QTIEditorPackage qti;

	public ItemMetadataFormController(final UserRequest ureq, final WindowControl control, final Item item, final QTIEditorPackage qti, final boolean restrictedEdit) {
		super(ureq, control);
		this.item = item;
		this.qti = qti;
		this.isSurvey = qti.getQTIDocument().isSurvey();
		this.isRestrictedEditMode = restrictedEdit;
		initForm(ureq);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#doDispose()
	 */
	@Override
	protected void doDispose() {
		// still to come
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formInnerEvent(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.form.flexible.FormItem, org.olat.core.gui.components.form.flexible.impl.FormEvent)
	 */
	@Override
	protected void formInnerEvent(final UserRequest ureq, final FormItem source, final FormEvent event) {
		if (source == limitAttempts) {
			toggle(attempts);
		} else if (source == limitTime) {
			toggle(timeMin);
			toggle(timeSec);
		} else if (source == showHints) {
			toggle(hint);
		} else if (source == showSolution) {
			toggle(solution);
		}
	}

	@Override
	protected boolean validateFormLogic(final UserRequest ureq) {
		if (title.getValue().trim().isEmpty()) { // Remove empty title to fix OLAT-2296
			title.setValue("");
			title.setErrorKey("form.imd.error.empty.title", null);
			return false;
		}
		return true;
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formOK(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void formOK(final UserRequest ureq) {
		// Fire Change Event
		final String newTitle = title.getValue();
		final String oldTitle = item.getTitle();
		final boolean hasTitleChange = newTitle != null && !newTitle.equals(oldTitle);
		String newObjectives = desc.getRawValue(); // trust authors, don't do XSS filtering
		// Remove any conditional comments due to strange behavior in test (OLAT-4518)
		final Filter conditionalCommentFilter = FilterFactory.getConditionalHtmlCommentsFilter();
		newObjectives = conditionalCommentFilter.filter(newObjectives);
		final String oldObjectives = item.getObjectives();
		final boolean hasObjectivesChange = newObjectives != null && !newObjectives.equals(oldObjectives);
		final NodeBeforeChangeEvent nce = new NodeBeforeChangeEvent();
		if (hasTitleChange) {
			nce.setNewTitle(newTitle);
		}
		if (hasObjectivesChange) {
			nce.setNewObjectives(newObjectives);
		}
		if (hasTitleChange || hasObjectivesChange) {
			// create a memento first
			nce.setItemIdent(item.getIdent());
			nce.setQuestionIdent(item.getQuestion().getQuestion().getId());
			fireEvent(ureq, nce);
		}

		// Update item
		item.setTitle(newTitle);
		item.setObjectives(newObjectives); // trust authors, don't to XSS filtering
		final Question q = item.getQuestion();
		if (layout != null) {
			((ChoiceQuestion) q).setFlowLabelClass(layout.getSelectedKey() == "h" ? ChoiceQuestion.BLOCK : ChoiceQuestion.LIST);
		}
		if (!isSurvey && !isRestrictedEditMode) {
			q.setShuffle(shuffle.getSelected() == 0);
			final Control itemControl = (Control) item.getItemcontrols().get(0);
			itemControl.setFeedback(itemControl.getFeedback() == Control.CTRL_UNDEF ? Control.CTRL_NO : itemControl.getFeedback());
			itemControl.setHint(showHints.getSelected() == 0 ? Control.CTRL_YES : Control.CTRL_NO);
			itemControl.setSolution(showSolution.getSelected() == 0 ? Control.CTRL_YES : Control.CTRL_NO);
			q.setHintText(conditionalCommentFilter.filter(hint.getRawValue())); // trust authors, don't to XSS filtering
			q.setSolutionText(conditionalCommentFilter.filter(solution.getRawValue())); // trust authors, don't to XSS filtering
			if (limitTime.getSelectedKey().equals("y")) {
				item.setDuration(new Duration(1000 * timeSec.getIntValue() + 1000 * 60 * timeMin.getIntValue()));
			} else {
				item.setDuration(null);
			}
			if (limitAttempts.getSelectedKey().equals("y")) {
				item.setMaxattempts(attempts.getIntValue());
			} else {
				item.setMaxattempts(0);
			}
		}
		fireEvent(ureq, Event.DONE_EVENT);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#initForm(org.olat.core.gui.components.form.flexible.FormItemContainer,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
		this.setFormTitle("fieldset.legend.metadata");

		final int t = item.getQuestion().getType();

		if (isSurvey) {
			this.setFormContextHelp("org.olat.ims.qti.editor", "qed-meta-surv-" + t + ".html", "help.hover.qti-meta-" + t);
		} else {
			this.setFormContextHelp("org.olat.ims.qti.editor", "qed-meta-test-" + t + ".html", "help.hover.qti-meta-" + t);
		}

		// Title
		title = uifactory.addTextElement("title", "form.imd.title", -1, item.getTitle(), formLayout);
		title.setMandatory(true);
		title.setNotEmptyCheck("form.imd.error.empty.title");

		// Question Type
		final String typeName = getType();
		final StaticTextElement type = uifactory.addStaticTextElement("type", "form.imd.type", typeName, formLayout);

		// Description
		desc = uifactory.addRichTextElementForStringData("desc", "form.imd.descr", item.getObjectives(), 8, -1, false, true, null, null, formLayout,
				ureq.getUserSession(), getWindowControl());
		final RichTextConfiguration richTextConfig = desc.getEditorConfiguration();
		// set upload dir to the media dir
		richTextConfig.setFileBrowserUploadRelPath("media");

		// Layout/Alignment
		final Question q = item.getQuestion();
		// alignment of KPRIM is only horizontal
		if (q instanceof ChoiceQuestion && item.getQuestion().getType() != Question.TYPE_KPRIM) {
			final String[] layoutKeys = new String[] { "h", "v" };
			final String[] layoutValues = new String[] { translate("form.imd.layout.horizontal"), translate("form.imd.layout.vertical") };
			// layout = uifactory.addDropdownSingleselect("form.imd.layout", formLayout, layoutKeys, layoutValues, null);
			layout = uifactory.addRadiosHorizontal("layout", "form.imd.layout", formLayout, layoutKeys, layoutValues);
			layout.select(((ChoiceQuestion) q).getFlowLabelClass() == ChoiceQuestion.BLOCK ? "h" : "v", true);
		}

		if (!isSurvey && !isRestrictedEditMode) {
			final String[] yesnoKeys = new String[] { "y", "n" };
			final String[] yesnoValues = new String[] { translate("yes"), translate("no") };

			// Attempts
			limitAttempts = uifactory.addRadiosHorizontal("form.imd.limittries", formLayout, yesnoKeys, yesnoValues);
			limitAttempts.addActionListener(this, FormEvent.ONCLICK); // Radios/Checkboxes need onclick because of IE bug OLAT-5753
			attempts = uifactory.addIntegerElement("form.imd.tries", 0, formLayout);
			attempts.setDisplaySize(3);
			if (item.getMaxattempts() > 0) {
				limitAttempts.select("y", true);
				attempts.setIntValue(item.getMaxattempts());
			} else {
				limitAttempts.select("n", true);
				attempts.setVisible(false);
			}

			// Time Limit
			limitTime = uifactory.addRadiosHorizontal("form.imd.limittime", formLayout, yesnoKeys, yesnoValues);
			limitTime.addActionListener(this, FormEvent.ONCLICK); // Radios/Checkboxes need onclick because of IE bug OLAT-5753
			timeMin = uifactory.addIntegerElement("form.imd.time.min", 0, formLayout);
			timeMin.setDisplaySize(3);
			timeSec = uifactory.addIntegerElement("form.imd.time.sek", 0, formLayout);
			timeSec.setDisplaySize(3);
			if (item.getDuration() != null && item.getDuration().isSet()) {
				limitTime.select("y", true);
				timeMin.setIntValue(item.getDuration().getMin());
				timeSec.setIntValue(item.getDuration().getSec());
			} else {
				limitTime.select("n", true);
				timeMin.setVisible(false);
				timeSec.setVisible(false);
			}

			// Shuffle Answers
			shuffle = uifactory.addRadiosHorizontal("shuffle", "form.imd.shuffle", formLayout, yesnoKeys, yesnoValues);
			if (item.getQuestion().isShuffle()) {
				shuffle.select("y", true);
			} else {
				shuffle.select("n", true);
			}

			// Hints
			final Control itemControl = (Control) item.getItemcontrols().get(0);
			showHints = uifactory.addRadiosHorizontal("showHints", "form.imd.solutionhints.show", formLayout, yesnoKeys, yesnoValues);
			showHints.addActionListener(this, FormEvent.ONCLICK); // Radios/Checkboxes need onclick because of IE bug OLAT-5753

			hint = uifactory.addRichTextElementForStringData("hint", "form.imd.solutionhints", item.getQuestion().getHintText(), 8, -1, false, true, qti.getBaseDir(),
					null, formLayout, ureq.getUserSession(), getWindowControl());
			// set upload dir to the media dir
			hint.getEditorConfiguration().setFileBrowserUploadRelPath("media");
			if (itemControl.isHint()) {
				showHints.select("y", true);
			} else {
				showHints.select("n", true);
				hint.setVisible(false);
			}

			// Solution
			showSolution = uifactory.addRadiosHorizontal("showSolution", "form.imd.correctsolution.show", formLayout, yesnoKeys, yesnoValues);
			showSolution.addActionListener(this, FormEvent.ONCLICK); // Radios/Checkboxes need onclick because of IE bug OLAT-5753
			solution = uifactory.addRichTextElementForStringData("solution", "form.imd.correctsolution", item.getQuestion().getSolutionText(), 8, -1, false, true,
					qti.getBaseDir(), null, formLayout, ureq.getUserSession(), getWindowControl());
			// set upload dir to the media dir
			hint.getEditorConfiguration().setFileBrowserUploadRelPath("media");
			if (itemControl.isSolution()) {
				showSolution.select("y", true);
			} else {
				showSolution.select("n", true);
				solution.setVisible(false);
			}
		}
		// Submit Button
		uifactory.addFormSubmitButton("submit", formLayout);
	}

	/**
	 * @return The translated type string of the item
	 */
	private String getType() {
		final int questionType = item.getQuestion().getType();
		String questionTypeLabel = "n/a";
		switch (questionType) {
			case Question.TYPE_SC:
				questionTypeLabel = translate("item.type.sc");
				break;
			case Question.TYPE_MC:
				questionTypeLabel = translate("item.type.mc");
				break;
			case Question.TYPE_FIB:
				questionTypeLabel = translate("item.type.fib");
				break;
			case Question.TYPE_KPRIM:
				questionTypeLabel = translate("item.type.kprim");
				break;
			case Question.TYPE_ESSAY:
				questionTypeLabel = translate("item.type.essay");
				break;
		}
		return questionTypeLabel;
	}

	/**
	 * @param item The item to be shown or hidden
	 */
	private void toggle(final FormItem formItem) {
		formItem.setVisible(!formItem.isVisible());
		this.flc.setDirty(true);
	}

}
