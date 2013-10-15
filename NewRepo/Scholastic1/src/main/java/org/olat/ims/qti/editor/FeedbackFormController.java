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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.RichTextElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.richText.RichTextConfiguration;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.util.Util;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.ims.qti.editor.beecom.objects.ChoiceResponse;
import org.olat.ims.qti.editor.beecom.objects.Control;
import org.olat.ims.qti.editor.beecom.objects.Item;
import org.olat.ims.qti.editor.beecom.objects.Material;
import org.olat.ims.qti.editor.beecom.objects.Question;

/**
 * This is the form controller responsible for setting user feedback (i.e. is the answer right, explanations etc.)
 * <P>
 * Initial Date: Jul 16, 2009 <br>
 * 
 * @author gwassmann
 */
public class FeedbackFormController extends FormBasicController {

	private static final String NO = "no";
	private static final String YES = "yes";
	private final Item item;
	private final QTIEditorPackage qtiPackage;
	private final boolean isRestrictedEditMode;
	private CloseableModalController dialogCtr;
	private MaterialFormController materialCtr;
	private SingleSelection feedbackSwitch;
	private final Control control;
	private final HashMap<Material, RichTextElement> textElements = new HashMap<Material, RichTextElement>();
	private final HashMap<Material, String> identities = new HashMap<Material, String>();
	private final HashMap<RichTextElement, Material> materialsByText = new HashMap<RichTextElement, Material>();
	private final HashMap<FormLink, Material> materialsByLink = new HashMap<FormLink, Material>();
	private FormLayoutContainer overallFeedbackLayout, responseLevelHintsLayout;
	private final String mediaBaseUrl;
	private Material masteryMat, failureMat;

	public FeedbackFormController(final UserRequest ureq, final WindowControl wControl, final QTIEditorPackage qtiPackage, final Item item,
			final boolean isRestrictedEditMode) {
		super(ureq, wControl, FormBasicController.LAYOUT_VERTICAL);
		this.qtiPackage = qtiPackage;
		this.item = item;
		this.mediaBaseUrl = qtiPackage.getMediaBaseURL();

		control = QTIEditHelper.getControl(item);
		this.isRestrictedEditMode = isRestrictedEditMode;
		initForm(ureq);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#doDispose()
	 */
	@Override
	protected void doDispose() {
		// nothing so far
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == materialCtr) {
			if (event instanceof QTIObjectBeforeChangeEvent) {
				final NodeBeforeChangeEvent nce = new NodeBeforeChangeEvent();
				nce.setItemIdent(item.getIdent());
				nce.setQuestionIdent(item.getQuestion().getQuestion().getId());
				fireEvent(ureq, nce);
			} else if (event == Event.DONE_EVENT || event == Event.CANCELLED_EVENT) {
				if (event == Event.DONE_EVENT) {
					final Material mat = materialCtr.getMaterial();
					final String html = mat.renderAsHtmlForEditor();
					if (mat == masteryMat) {
						QTIEditHelper.setFeedbackMastery(item, html);
					} else if (mat == failureMat) {
						QTIEditHelper.setFeedbackFail(item, html);
					} else {
						QTIEditHelper.setFeedbackOlatResp(item, html, identities.get(mat));
					}

					// update view
					final RichTextElement text = textElements.get(mat);
					// the html code is embbeded in the panel and as not a proper baseurl
					text.setValue(mat.renderAsHtml(mediaBaseUrl));
					// serialize document
					qtiPackage.serializeQTIDocument();
				}
				// dispose controllers
				dialogCtr.deactivate();
				removeAsListenerAndDispose(dialogCtr);
				dialogCtr = null;
				removeAsListenerAndDispose(materialCtr);
				materialCtr = null;
			}
		} else if (source == dialogCtr) {
			if (event == Event.CANCELLED_EVENT) {
				removeAsListenerAndDispose(dialogCtr);
				dialogCtr = null;
				removeAsListenerAndDispose(materialCtr);
				materialCtr = null;
			}
		}
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formOK(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void formOK(final UserRequest ureq) {
		// there's no submit button in this form
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formInnerEvent(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.form.flexible.FormItem, org.olat.core.gui.components.form.flexible.impl.FormEvent)
	 */
	@Override
	protected void formInnerEvent(final UserRequest ureq, final FormItem source, final FormEvent event) {
		if (source instanceof RichTextElement) {
			displayMaterialFormController(ureq, materialsByText.get(source), isRestrictedEditMode);
		} else if (source instanceof FormLink) {
			displayMaterialFormController(ureq, materialsByLink.get(source), isRestrictedEditMode);
		} else if (source == feedbackSwitch) {
			// feedbackSwitch takes values in {0 = on/yes, 1 = off/no}
			// control.feedback takes values in {0 = undef, 1 = on, 2 = off}
			control.setFeedback(feedbackSwitch.getSelected() + 1);
			qtiPackage.serializeQTIDocument();
			showHideFeedbackFields();
		}
	}

	/**
	 * shows or hides the feedback input fields depending on if feedback is enabled
	 */
	private void showHideFeedbackFields() {
		final boolean feedbackEnabled = control.isFeedback();
		overallFeedbackLayout.setVisible(feedbackEnabled);
		responseLevelHintsLayout.setVisible(feedbackEnabled);
		this.flc.setDirty(true);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#initForm(org.olat.core.gui.components.form.flexible.FormItemContainer,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
		setFormTitle("fieldset.legend.feedback");
		setFormContextHelp("org.olat.ims.qti.editor", "qed-feedback.html", "help.hover.qti-feedback");

		final FormLayoutContainer switchLayout = FormLayoutContainer.createDefaultFormLayout("switchLayout", getTranslator());
		overallFeedbackLayout = FormLayoutContainer.createDefaultFormLayout("overallFeedbackLayout", getTranslator());
		responseLevelHintsLayout = FormLayoutContainer.createCustomFormLayout("responseLevelHintsLayout", getTranslator(), Util.getPackageVelocityRoot(this.getClass())
				+ "/response_level_feedback.html");
		// add the layouts to the custom layout
		formLayout.add(switchLayout);
		formLayout.add(overallFeedbackLayout);
		formLayout.add(responseLevelHintsLayout);

		String[] yesNoKeys, yesNoValues;
		yesNoKeys = new String[] { YES, NO };
		yesNoValues = new String[] { translate(YES), translate(NO) };

		feedbackSwitch = uifactory.addRadiosHorizontal("feedbackswitch", switchLayout, yesNoKeys, yesNoValues);
		feedbackSwitch.addActionListener(this, FormEvent.ONCLICK);
		if (control.isFeedback()) {
			feedbackSwitch.select(yesNoKeys[0], true);
		} else {
			// defaults to 'not showing feedback'
			feedbackSwitch.select(yesNoKeys[1], true);
		}

		responseLevelHintsLayout.contextPut("mediaBaseUrl", mediaBaseUrl);

		masteryMat = QTIEditHelper.getFeedbackMasteryMaterial(item);
		masteryMat = masteryMat == null ? new Material() : masteryMat;
		failureMat = QTIEditHelper.getFeedbackFailMaterial(item);
		failureMat = failureMat == null ? new Material() : failureMat;

		final VFSContainer baseContainer = qtiPackage.getBaseDir();

		// Mastery Layout
		final FormLayoutContainer masteryEditLayout = FormLayoutContainer.createCustomFormLayout("masteryEditLayout", getTranslator(),
				Util.getPackageVelocityRoot(this.getClass()) + "/rich_text_and_edit_link.html");
		masteryEditLayout.setLabel("item_feedback_mastery", null);
		overallFeedbackLayout.add(masteryEditLayout);
		final RichTextElement masteryFeedback = uifactory.addRichTextElementForStringData("richTextElement", "item_feedback_mastery",
				masteryMat.renderAsHtml(mediaBaseUrl), 4, -1, true, true, baseContainer, null, masteryEditLayout, ureq.getUserSession(), getWindowControl());
		final FormLink editLink = uifactory.addFormLink("editLink", masteryEditLayout, Link.NONTRANSLATED + Link.LINK_CUSTOM_CSS);
		((Link) editLink.getComponent()).setCustomDisplayText("");
		((Link) editLink.getComponent()).setCustomEnabledLinkCSS("b_small_icon b_edit_icon qti_edit_link");
		registerFeedbackElement(masteryMat, masteryFeedback, editLink);

		// One Failure Layout
		final FormLayoutContainer failureEditLayout = FormLayoutContainer.createCustomFormLayout("failureEditLayout", getTranslator(),
				Util.getPackageVelocityRoot(this.getClass()) + "/rich_text_and_edit_link.html");
		failureEditLayout.setLabel("item_feedback_fail", null);
		overallFeedbackLayout.add(failureEditLayout);
		final RichTextElement failureFeedback = uifactory.addRichTextElementForStringData("richTextElement", "item_feedback_fail", failureMat.renderAsHtml(mediaBaseUrl),
				4, -1, true, true, baseContainer, null, failureEditLayout, ureq.getUserSession(), getWindowControl());
		failureFeedback.setLabel("item_feedback_fail", null);
		final FormLink failureLink = uifactory.addFormLink("editLink", failureEditLayout, Link.NONTRANSLATED + Link.LINK_CUSTOM_CSS);
		((Link) failureLink.getComponent()).setCustomDisplayText("");
		((Link) failureLink.getComponent()).setCustomEnabledLinkCSS("b_small_icon b_edit_icon");
		registerFeedbackElement(failureMat, failureFeedback, failureLink);

		// Feedback for each response when single or multiple choice question
		final List<Material> responses = new ArrayList<Material>();
		boolean hasResponseLevelHints = false;
		if (item.getQuestion().getType() <= Question.TYPE_MC) {
			int i = 1;
			for (final Object obj : item.getQuestion().getResponses()) {
				final ChoiceResponse response = (ChoiceResponse) obj;
				// response-level feedback
				Material responseFeedbackMat = QTIEditHelper.getFeedbackOlatRespMaterial(item, response.getIdent());
				responseFeedbackMat = responseFeedbackMat == null ? new Material() : responseFeedbackMat;
				identities.put(responseFeedbackMat, response.getIdent());
				final RichTextElement responseHintText = uifactory.addRichTextElementForStringData("feedback_" + i, null, responseFeedbackMat.renderAsHtml(mediaBaseUrl),
						4, -1, true, true, baseContainer, null, responseLevelHintsLayout, ureq.getUserSession(), getWindowControl());
				final FormLink link = uifactory.addFormLink("link_" + i, responseLevelHintsLayout, Link.NONTRANSLATED + Link.LINK_CUSTOM_CSS);
				((Link) link.getComponent()).setCustomDisplayText("");
				((Link) link.getComponent()).setCustomEnabledLinkCSS("b_small_icon b_edit_icon");
				registerFeedbackElement(responseFeedbackMat, responseHintText, link);
				// get response for displaying
				final Material responseMat = response.getContent();
				responses.add(responseMat);
				i++;
			}
			// If 'i' is strictly greater than the initial value, there's at least one
			// response.
			hasResponseLevelHints = i > 1;
		}

		this.flc.contextPut("hasResponseLevelHints", hasResponseLevelHints);
		responseLevelHintsLayout.contextPut("responses", responses);
		showHideFeedbackFields();
	}

	/**
	 * @param masteryMat
	 * @param masteryFeedback
	 */
	private void registerFeedbackElement(final Material mat, final RichTextElement textElement, final FormLink link) {
		textElement.setEnabled(false);
		textElement.addActionListener(this, FormEvent.ONCLICK);
		link.addActionListener(this, FormEvent.ONCLICK);
		textElements.put(mat, textElement);
		// allow scripts...
		textElement.getEditorConfiguration().setQuotedConfigValue(RichTextConfiguration.INVALID_ELEMENTS,
				RichTextConfiguration.INVALID_ELEMENTS_FORM_FULL_VALUE_UNSAVE_WITH_SCRIPT);
		textElement.getEditorConfiguration().setQuotedConfigValue(RichTextConfiguration.EXTENDED_VALID_ELEMENTS, "script[src,type,defer]");

		materialsByText.put(textElement, mat);
		materialsByLink.put(link, mat);
	}

	/**
	 * Displays the MaterialFormController in a closable box.
	 * 
	 * @param ureq
	 * @param mat
	 * @param isRestrictedEditMode
	 */
	private void displayMaterialFormController(final UserRequest ureq, final Material mat, final boolean isRestrictedEditMode) {
		materialCtr = new MaterialFormController(ureq, getWindowControl(), mat, qtiPackage, isRestrictedEditMode);
		listenTo(materialCtr);
		dialogCtr = new CloseableModalController(getWindowControl(), "close", materialCtr.getInitialComponent());
		listenTo(dialogCtr);
		dialogCtr.activate();
	}
}
