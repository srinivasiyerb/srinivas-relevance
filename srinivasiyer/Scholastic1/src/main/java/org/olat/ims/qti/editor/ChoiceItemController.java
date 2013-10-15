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

package org.olat.ims.qti.editor;

import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.ControllerEventListener;
import org.olat.core.gui.control.DefaultController;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.dialog.DialogController;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Util;
import org.olat.ims.qti.editor.beecom.objects.ChoiceQuestion;
import org.olat.ims.qti.editor.beecom.objects.ChoiceResponse;
import org.olat.ims.qti.editor.beecom.objects.Item;
import org.olat.ims.qti.editor.beecom.objects.Material;
import org.olat.ims.qti.editor.beecom.objects.Mattext;
import org.olat.ims.qti.editor.beecom.objects.Question;
import org.olat.ims.qti.editor.beecom.objects.Response;

/**
 * Initial Date: Oct 21, 2004 <br>
 * 
 * @author mike
 */
public class ChoiceItemController extends DefaultController implements ControllerEventListener {
	/*
	 * Logging, Velocity
	 */
	private static final String PACKAGE = Util.getPackageName(ChoiceItemController.class);
	private static final String VC_ROOT = Util.getPackageVelocityRoot(PACKAGE);

	private VelocityContainer main;
	private Translator trnsltr;

	private Item item;
	private final QTIEditorPackage qtiPackage;
	private DialogController delYesNoCtrl;
	private final boolean restrictedEdit;
	private Material editQuestion;
	private Response editResponse;
	private CloseableModalController dialogCtr;
	private MaterialFormController matFormCtr;

	/**
	 * @param item
	 * @param qtiPackage
	 * @param trnsltr
	 * @param wControl
	 */
	public ChoiceItemController(final Item item, final QTIEditorPackage qtiPackage, final Translator trnsltr, final WindowControl wControl, final boolean restrictedEdit) {
		super(wControl);

		this.restrictedEdit = restrictedEdit;
		this.item = item;
		this.qtiPackage = qtiPackage;
		this.trnsltr = trnsltr;
		main = new VelocityContainer("scitem", VC_ROOT + "/tab_scItem.html", trnsltr, this);
		main.contextPut("question", item.getQuestion());
		main.contextPut("isSurveyMode", qtiPackage.getQTIDocument().isSurvey() ? "true" : "false");
		main.contextPut("isRestrictedEdit", restrictedEdit ? Boolean.TRUE : Boolean.FALSE);
		main.contextPut("mediaBaseURL", qtiPackage.getMediaBaseURL());
		if (item.getQuestion().getType() == Question.TYPE_MC) {
			main.setPage(VC_ROOT + "/tab_mcItem.html");
		} else if (item.getQuestion().getType() == Question.TYPE_KPRIM) {
			main.setPage(VC_ROOT + "/tab_kprimItem.html");
		}
		setInitialComponent(main);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == main) {
			// olat::: as: improve easy fix since almost all operations change the main vc.
			main.setDirty(true);
			final String cmd = event.getCommand();
			final String sPosid = ureq.getParameter("posid");
			int posid = 0;
			if (sPosid != null) {
				posid = Integer.parseInt(sPosid);
			}
			if (cmd.equals("up")) {
				if (posid > 0) {
					final List elements = item.getQuestion().getResponses();
					final Object obj = elements.remove(posid);
					elements.add(posid - 1, obj);
				}
			} else if (cmd.equals("down")) {
				final List elements = item.getQuestion().getResponses();
				if (posid < elements.size() - 1) {
					final Object obj = elements.remove(posid);
					elements.add(posid + 1, obj);
				}
			} else if (cmd.equals("editq")) {
				editQuestion = item.getQuestion().getQuestion();
				displayMaterialFormController(ureq, editQuestion, restrictedEdit);

			} else if (cmd.equals("editr")) {
				editResponse = ((Response) item.getQuestion().getResponses().get(posid));
				final Material responseMat = editResponse.getContent();
				displayMaterialFormController(ureq, responseMat, restrictedEdit);

			} else if (cmd.equals("addchoice")) {
				final ChoiceQuestion question = (ChoiceQuestion) item.getQuestion();
				final List choices = question.getResponses();
				final ChoiceResponse newChoice = new ChoiceResponse();
				newChoice.getContent().add(new Mattext(trnsltr.translate("newresponsetext")));
				newChoice.setCorrect(false);
				newChoice.setPoints(-1f); // default value is negative to make sure
				// people understand the meaning of this value
				choices.add(newChoice);
			} else if (cmd.equals("del")) {
				delYesNoCtrl = DialogController.createYesNoDialogController(ureq.getLocale(), trnsltr.translate("confirm.delete.element"), this, new Integer(posid));
				getWindowControl().pushAsModalDialog(delYesNoCtrl.getInitialComponent());
			} else if (cmd.equals("ssc")) { // submit sc
				final ChoiceQuestion question = (ChoiceQuestion) item.getQuestion();
				final List q_choices = question.getResponses();
				final String correctChoice = ureq.getParameter("correctChoice");
				for (int i = 0; i < q_choices.size(); i++) {
					final ChoiceResponse choice = (ChoiceResponse) q_choices.get(i);
					if (correctChoice != null && correctChoice.equals("value_q" + i)) {
						choice.setCorrect(true);
					} else {
						choice.setCorrect(false);
					}
					choice.setPoints(ureq.getParameter("points_q" + i));
				}
				question.setSingleCorrectScore(ureq.getParameter("single_score"));
			} else if (cmd.equals("smc")) { // submit mc
				final ChoiceQuestion question = (ChoiceQuestion) item.getQuestion();
				final List choices = question.getResponses();
				boolean hasZeroPointChoice = false;
				for (int i = 0; i < choices.size(); i++) {
					final ChoiceResponse choice = (ChoiceResponse) choices.get(i);
					if (ureq.getParameter("value_q" + i) != null && ureq.getParameter("value_q" + i).equalsIgnoreCase("true")) {
						choice.setCorrect(true);
					} else {
						choice.setCorrect(false);
					}
					choice.setPoints(ureq.getParameter("points_q" + i));
					if (choice.getPoints() == 0) {
						hasZeroPointChoice = true;
					}
				}
				if (hasZeroPointChoice && !question.isSingleCorrect()) {
					getWindowControl().setInfo(trnsltr.translate("editor.info.mc.zero.points"));
				}

				// set min/max before single_correct score
				// will be corrected by single_correct score afterwards
				question.setMinValue(ureq.getParameter("min_value"));
				question.setMaxValue(ureq.getParameter("max_value"));
				question.setSingleCorrect(ureq.getParameter("valuation_method").equals("single"));
				if (question.isSingleCorrect()) {
					question.setSingleCorrectScore(ureq.getParameter("single_score"));
				} else {
					question.setSingleCorrectScore(0);
				}

			} else if (cmd.equals("skprim")) { // submit kprim
				float maxValue = 0;
				try {
					maxValue = Float.parseFloat(ureq.getParameter("max_value"));
				} catch (final NumberFormatException e) {
					// invalid input, set maxValue 0
				}
				final ChoiceQuestion question = (ChoiceQuestion) item.getQuestion();
				final List q_choices = question.getResponses();
				for (int i = 0; i < q_choices.size(); i++) {
					final String correctChoice = ureq.getParameter("correctChoice_q" + i);
					final ChoiceResponse choice = (ChoiceResponse) q_choices.get(i);
					choice.setPoints(maxValue / 4);
					if ("correct".equals(correctChoice)) {
						choice.setCorrect(true);
					} else {
						choice.setCorrect(false);
					}
				}
				question.setMaxValue(maxValue);
			}
			qtiPackage.serializeQTIDocument();
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller controller, final Event event) {
		if (controller == matFormCtr) {
			if (event instanceof QTIObjectBeforeChangeEvent) {
				final QTIObjectBeforeChangeEvent qobce = (QTIObjectBeforeChangeEvent) event;
				final NodeBeforeChangeEvent nce = new NodeBeforeChangeEvent();
				if (editQuestion != null) {
					nce.setNewQuestionMaterial(qobce.getContent());
					nce.setItemIdent(item.getIdent());
					nce.setQuestionIdent(editQuestion.getId());
					nce.setMatIdent(qobce.getId());
					fireEvent(ureq, nce);
				} else if (editResponse != null) {
					nce.setNewResponseMaterial(qobce.getContent());
					nce.setItemIdent(item.getIdent());
					nce.setResponseIdent(editResponse.getIdent());
					nce.setMatIdent(qobce.getId());
					fireEvent(ureq, nce);
				}
			} else if (event == Event.DONE_EVENT || event == Event.CANCELLED_EVENT) {
				if (event == Event.DONE_EVENT) {
					// serialize document
					qtiPackage.serializeQTIDocument();
					// force rerendering of view
					main.setDirty(true);
					editQuestion = null;
					editResponse = null;
				}
				// dispose controllers
				dialogCtr.deactivate();
				dialogCtr.dispose();
				dialogCtr = null;
				matFormCtr.dispose();
				matFormCtr = null;
			}
		} else if (controller == dialogCtr) {
			if (event == Event.CANCELLED_EVENT) {
				dialogCtr.dispose();
				dialogCtr = null;
				matFormCtr.dispose();
				matFormCtr = null;
			}
		} else if (controller == delYesNoCtrl) {
			getWindowControl().pop();
			if (event == DialogController.EVENT_FIRSTBUTTON) {
				item.getQuestion().getResponses().remove(((Integer) delYesNoCtrl.getUserObject()).intValue());
				main.setDirty(true);// repaint
			}
		}
	}

	/**
	 * Displays the MaterialFormController in a closable box.
	 * 
	 * @param ureq
	 * @param mat
	 * @param isRestrictedEditMode
	 */
	private void displayMaterialFormController(final UserRequest ureq, final Material mat, final boolean isRestrictedEditMode) {
		matFormCtr = new MaterialFormController(ureq, getWindowControl(), mat, qtiPackage, isRestrictedEditMode);
		matFormCtr.addControllerListener(this);
		dialogCtr = new CloseableModalController(getWindowControl(), "close", matFormCtr.getInitialComponent());
		matFormCtr.addControllerListener(dialogCtr);
		dialogCtr.activate();
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		main = null;
		item = null;
		trnsltr = null;
		if (dialogCtr != null) {
			dialogCtr.dispose();
			dialogCtr = null;
		}
		if (matFormCtr != null) {
			matFormCtr.dispose();
			matFormCtr = null;
		}
	}

}