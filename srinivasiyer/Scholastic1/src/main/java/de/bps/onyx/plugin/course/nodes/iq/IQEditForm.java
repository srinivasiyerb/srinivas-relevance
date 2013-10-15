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

package de.bps.onyx.plugin.course.nodes.iq;

import java.util.Date;
import java.util.Scanner;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.DateChooser;
import org.olat.core.gui.components.form.flexible.elements.IntegerElement;
import org.olat.core.gui.components.form.flexible.elements.SelectionElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.Translator;
import org.olat.core.logging.Tracing;
import org.olat.core.util.Util;
import org.olat.ims.qti.process.AssessmentInstance;
import org.olat.modules.ModuleConfiguration;

import de.bps.onyx.plugin.OnyxModule;
import de.bps.onyx.plugin.OnyxModule.PlayerTemplate;

/**
 * Test configuration form. Used for configuring Test, Self-test, and Questionnaire(aka Survey).
 * <p>
 * Initial Date: Mar 3, 2004
 * 
 * @author Mike Stock
 */
public class IQEditForm extends FormBasicController {

	private SelectionElement enableMenu;
	private SelectionElement displayMenu;
	private SelectionElement displayScoreProgress;
	private SelectionElement displayQuestionProgress;
	private SelectionElement displayQuestionTitle;
	private SingleSelection sequence;
	private SelectionElement enableCancel;
	private SelectionElement enableSuspend;
	private SingleSelection summary;
	private SelectionElement limitAttempts;
	private IntegerElement attempts;
	private SingleSelection menuRenderOptions;
	private SelectionElement scoreInfo;
	private SelectionElement showResultsDateDependentButton;
	private DateChooser startDateElement;
	private DateChooser endDateElement;
	private SelectionElement showResultsAfterFinishTest;
	private SelectionElement showResultsOnHomePage;

	private final ModuleConfiguration modConfig;

	private final String[] menuRenderOptKeys, menuRenderOptValues;
	private final String[] sequenceKeys, sequenceValues;
	private final String configKeyType;

	private SingleSelection template;
	private TextElement cutValue;
	private boolean isOnyx;

	private final boolean isAssessment, isSelfTest, isSurvey;

	/**
	 * Constructor for the qti configuration form
	 * 
	 * @param ureq
	 * @param wControl
	 * @param modConfig
	 */
	public IQEditForm(final UserRequest ureq, final WindowControl wControl, final ModuleConfiguration modConfig) {
		super(ureq, wControl);

		final Translator translator = Util.createPackageTranslator(org.olat.course.nodes.iq.IQEditController.class, getTranslator().getLocale(), getTranslator());
		setTranslator(translator);

		this.modConfig = modConfig;

		configKeyType = (String) modConfig.get(IQEditController.CONFIG_KEY_TYPE);

		isAssessment = configKeyType.equals(AssessmentInstance.QMD_ENTRY_TYPE_ASSESS);
		isSelfTest = configKeyType.equals(AssessmentInstance.QMD_ENTRY_TYPE_SELF);
		isSurvey = configKeyType.equals(AssessmentInstance.QMD_ENTRY_TYPE_SURVEY);

		isOnyx = false;
		if (modConfig.get(IQEditController.CONFIG_KEY_TYPE_QTI) != null) {
			final String qtiType = (String) modConfig.get(IQEditController.CONFIG_KEY_TYPE_QTI);
			isOnyx = (IQEditController.CONFIG_VALUE_QTI2.equals(qtiType)) ? true : false;
		}

		menuRenderOptKeys = new String[] { Boolean.FALSE.toString(), Boolean.TRUE.toString() };
		menuRenderOptValues = new String[] { translate("qti.form.menurender.allquestions"), translate("qti.form.menurender.sectionsonly") };
		sequenceKeys = new String[] { AssessmentInstance.QMD_ENTRY_SEQUENCE_ITEM, AssessmentInstance.QMD_ENTRY_SEQUENCE_SECTION };
		sequenceValues = new String[] { translate("qti.form.sequence.item"), translate("qti.form.sequence.section") };

		initForm(ureq);
	}

	@Override
	protected boolean validateFormLogic(final UserRequest ureq) {

		startDateElement.clearError();
		endDateElement.clearError();

		if (startDateElement.isVisible()) {
			if (startDateElement.isEmpty()) {
				startDateElement.setErrorKey("qti.form.date.start.error.mandatory", null);
				return false;
			} else {
				if (startDateElement.getDate() == null) {
					startDateElement.setErrorKey("qti.form.date.error.format", null);
					return false;
				}
			}

			if (!endDateElement.isEmpty()) {
				if (endDateElement.getDate() == null) {
					endDateElement.setErrorKey("qti.form.date.error.format", null);
					return false;
				}

				if (endDateElement.getDate().before(startDateElement.getDate())) {
					endDateElement.setErrorKey("qti.form.date.error.endbeforebegin", null);
					return false;
				}
			}
		}

		return true;
	}

	@Override
	protected void formOK(final UserRequest ureq) {
		fireEvent(ureq, Event.DONE_EVENT);
	}

	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {

		limitAttempts = uifactory.addCheckboxesVertical("limitAttempts", "qti.form.limit.attempts", formLayout, new String[] { "xx" }, new String[] { null }, null, 1);

		Integer confAttempts = (Integer) modConfig.get(IQEditController.CONFIG_KEY_ATTEMPTS);
		if (confAttempts == null) {
			confAttempts = new Integer(0);
		}
		attempts = uifactory.addIntegerElement("qti.form.attempts", confAttempts, formLayout);
		attempts.setDisplaySize(2);
		attempts.setMinValueCheck(1, null);
		attempts.setMaxValueCheck(20, null);

		uifactory.addSpacerElement("s1", formLayout, true);

		// Only assessments have a limitation on number of attempts
		if (isAssessment) {
			limitAttempts.select("xx", confAttempts > 0);
			limitAttempts.addActionListener(this, FormEvent.ONCLICK);
		} else {
			limitAttempts.select("xx", false);
			limitAttempts.setVisible(false);
			attempts.setVisible(false);
		}

		if (isOnyx) {
			// select onyx template
			final String[] values = new String[OnyxModule.PLAYERTEMPLATES.size()];
			final String[] keys = new String[OnyxModule.PLAYERTEMPLATES.size()];

			int i = 0;
			for (final PlayerTemplate pt : OnyxModule.PLAYERTEMPLATES) {
				keys[i] = pt.id;
				values[i] = getTranslator().translate(pt.i18nkey);
				++i;
			}
			template = uifactory.addDropdownSingleselect("qti.form.onyx.template", formLayout, keys, values, null);
			try {
				if (modConfig.get(IQEditController.CONFIG_KEY_TEMPLATE) != null) {
					template.select(modConfig.get(IQEditController.CONFIG_KEY_TEMPLATE).toString(), true);
				}
			} catch (final RuntimeException e) {
				Tracing.logWarn("Template not found", e, this.getClass());
			}

			// the cutvalue
			if (!isSurvey) {
				cutValue = uifactory.addTextElement("qti.form.onyx.cutvalue", "qti.form.onyx.cutvalue", 20, "", formLayout);
				if (modConfig.get(IQEditController.CONFIG_KEY_CUTVALUE) != null) {
					cutValue.setValue(modConfig.get(IQEditController.CONFIG_KEY_CUTVALUE).toString());
				} else {
					cutValue.setValue("");
				}
			}

		} else {

			final Boolean CdisplayMenu = (Boolean) modConfig.get(IQEditController.CONFIG_KEY_DISPLAYMENU);
			displayMenu = uifactory.addCheckboxesVertical("qti_displayMenu", "qti.form.menudisplay", formLayout, new String[] { "xx" }, new String[] { null }, null, 1);
			displayMenu.select("xx", CdisplayMenu == null ? true : CdisplayMenu);
			displayMenu.addActionListener(this, FormEvent.ONCLICK);

			final Boolean CenableMenu = (Boolean) modConfig.get(IQEditController.CONFIG_KEY_ENABLEMENU);
			enableMenu = uifactory.addCheckboxesVertical("qti_enableMenu", "qti.form.menuenable", formLayout, new String[] { "xx" }, new String[] { null }, null, 1);
			enableMenu.select("xx", CenableMenu == null ? true : CenableMenu);

			menuRenderOptions = uifactory.addRadiosVertical("qti_form_menurenderoption", "qti.form.menurender", formLayout, menuRenderOptKeys, menuRenderOptValues);
			menuRenderOptions.setVisible(displayMenu.isSelected(0));
			Boolean renderSectionsOnly;
			if (modConfig.get(IQEditController.CONFIG_KEY_RENDERMENUOPTION) == null) {
				// migration
				modConfig.set(IQEditController.CONFIG_KEY_RENDERMENUOPTION, Boolean.FALSE);
				renderSectionsOnly = Boolean.FALSE;
			} else {
				renderSectionsOnly = (Boolean) modConfig.get(IQEditController.CONFIG_KEY_RENDERMENUOPTION);
			}
			menuRenderOptions.select(renderSectionsOnly.toString(), true);

			// sequence type
			sequence = uifactory.addRadiosVertical("qti_form_sequence", "qti.form.sequence", formLayout, sequenceKeys, sequenceValues);
			String confSequence = (String) modConfig.get(IQEditController.CONFIG_KEY_SEQUENCE);
			if (confSequence == null) {
				confSequence = AssessmentInstance.QMD_ENTRY_SEQUENCE_ITEM;
			}
			sequence.select(confSequence, true);

			final Boolean bDisplayQuestionTitle = (Boolean) modConfig.get(IQEditController.CONFIG_KEY_QUESTIONTITLE);
			final boolean confDisplayQuestionTitle = (bDisplayQuestionTitle != null) ? bDisplayQuestionTitle.booleanValue() : true;
			displayQuestionTitle = uifactory.addCheckboxesVertical("qti_displayQuestionTitle", "qti.form.questiontitle", formLayout, new String[] { "xx" },
					new String[] { null }, null, 1);
			displayQuestionTitle.select("xx", confDisplayQuestionTitle);

			// question progress
			final Boolean bEnableQuestionProgress = (Boolean) modConfig.get(IQEditController.CONFIG_KEY_QUESTIONPROGRESS);
			final boolean confEnableQuestionProgress = (bEnableQuestionProgress != null) ? bEnableQuestionProgress.booleanValue() : true;
			displayQuestionProgress = uifactory.addCheckboxesVertical("qti_enableQuestionProgress", "qti.form.questionprogress", formLayout, new String[] { "xx" },
					new String[] { null }, null, 1);
			displayQuestionProgress.select("xx", confEnableQuestionProgress);
			displayQuestionProgress.setVisible(!isSurvey);

			// score progress
			final Boolean bEnableScoreProgress = (Boolean) modConfig.get(IQEditController.CONFIG_KEY_SCOREPROGRESS);
			final boolean confEnableScoreProgress = (bEnableScoreProgress != null) ? bEnableScoreProgress.booleanValue() : true;
			displayScoreProgress = uifactory.addCheckboxesVertical("resultTitle", "qti.form.scoreprogress", formLayout, new String[] { "xx" }, new String[] { null },
					null, 1);

			if (isAssessment || isSelfTest) {
				displayScoreProgress.select("xx", confEnableScoreProgress);
			} else {
				displayScoreProgress.select("xx", false);
				displayScoreProgress.setEnabled(false);
				displayScoreProgress.setVisible(false);
			}

			// enable cancel
			final Boolean bEnableCancel = (Boolean) modConfig.get(IQEditController.CONFIG_KEY_ENABLECANCEL);
			boolean confEnableCancel = true;
			if (bEnableCancel != null) {
				// if defined use config value
				confEnableCancel = bEnableCancel.booleanValue();
			} else {
				// undefined... migrate according to old behaviour
				if (configKeyType != null && configKeyType.equals(AssessmentInstance.QMD_ENTRY_TYPE_ASSESS)) {
					confEnableCancel = false;
				}
			}
			enableCancel = uifactory
					.addCheckboxesVertical("qti_enableCancel", "qti.form.enablecancel", formLayout, new String[] { "xx" }, new String[] { null }, null, 1);
			enableCancel.select("xx", confEnableCancel);

			if (isSelfTest) {
				enableCancel.select("xx", true);
				enableCancel.setVisible(false);
				enableCancel.setEnabled(false);
			}

			// enable suspend
			final Boolean bEnableSuspend = (Boolean) modConfig.get(IQEditController.CONFIG_KEY_ENABLESUSPEND);
			final boolean confEnableSuspend = (bEnableSuspend != null) ? bEnableSuspend.booleanValue() : false;
			enableSuspend = uifactory.addCheckboxesVertical("qti_enableSuspend", "qti.form.enablesuspend", formLayout, new String[] { "xx" }, new String[] { null },
					null, 1);
			enableSuspend.select("xx", confEnableSuspend);

			uifactory.addSpacerElement("s2", formLayout, true);

			// end of "!isOnyx"
		}

		// Show score infos on start page
		final Boolean bEnableScoreInfos = (Boolean) modConfig.get(IQEditController.CONFIG_KEY_ENABLESCOREINFO);
		final boolean enableScoreInfos = (bEnableScoreInfos != null) ? bEnableScoreInfos.booleanValue() : true;
		scoreInfo = uifactory.addCheckboxesVertical("qti_scoreInfo", "qti.form.scoreinfo", formLayout, new String[] { "xx" }, new String[] { null }, null, 1);
		scoreInfo.select("xx", enableScoreInfos);
		if (isAssessment || isSelfTest) {
			scoreInfo.select("xx", enableScoreInfos);
			scoreInfo.addActionListener(this, FormEvent.ONCLICK);
		} else {
			// isSurvey
			scoreInfo.setVisible(false);
		}

		// migration: check if old tests have no summary
		final String configuredSummary = (String) modConfig.get(IQEditController.CONFIG_KEY_SUMMARY);
		final boolean noSummary = configuredSummary != null && configuredSummary.equals(AssessmentInstance.QMD_ENTRY_SUMMARY_NONE) ? true : false;

		final Boolean showResultOnHomePage = (Boolean) modConfig.get(IQEditController.CONFIG_KEY_RESULT_ON_HOME_PAGE);
		boolean confEnableShowResultOnHomePage = (showResultOnHomePage != null) ? showResultOnHomePage.booleanValue() : false;
		confEnableShowResultOnHomePage = !noSummary && confEnableShowResultOnHomePage;
		showResultsOnHomePage = uifactory.addCheckboxesVertical("qti_enableResultsOnHomePage", "qti.form.results.onhomepage", formLayout, new String[] { "xx" },
				new String[] { null }, null, 1);
		showResultsOnHomePage.select("xx", confEnableShowResultOnHomePage);
		showResultsOnHomePage.addActionListener(this, FormEvent.ONCLICK);
		showResultsOnHomePage.setVisible(!isSurvey);

		final Boolean showResultsActive = (Boolean) modConfig.get(IQEditController.CONFIG_KEY_DATE_DEPENDENT_RESULTS);
		boolean showResultsDateDependent = false; // default false
		if (showResultsActive != null) {
			showResultsDateDependent = showResultsActive.booleanValue();
		}

		showResultsDateDependentButton = uifactory.addCheckboxesVertical("qti_showresult", "qti.form.show.results", formLayout, new String[] { "xx" },
				new String[] { null }, null, 1);
		if (isAssessment || isSelfTest) {
			showResultsDateDependentButton.select("xx", showResultsDateDependent);
			showResultsDateDependentButton.addActionListener(this, FormEvent.ONCLICK);
		} else {
			showResultsDateDependentButton.setEnabled(false);
		}

		final Date startDate = (Date) modConfig.get(IQEditController.CONFIG_KEY_RESULTS_START_DATE);
		startDateElement = uifactory.addDateChooser("qti_form_start_date", "qti.form.date.start", "", formLayout);
		startDateElement.setDateChooserTimeEnabled(true);
		startDateElement.setDateChooserDateFormat("%d.%m.%Y %H:%M");
		startDateElement.setCustomDateFormat("dd.MM.yyyy HH:mm");
		if (startDate != null) {
			startDateElement.setDate(startDate);
		}
		startDateElement.setMandatory(true);

		final Date endDate = (Date) modConfig.get(IQEditController.CONFIG_KEY_RESULTS_END_DATE);
		endDateElement = uifactory.addDateChooser("qti_form_end_date", "qti.form.date.end", "", formLayout);
		endDateElement.setDateChooserTimeEnabled(true);
		endDateElement.setDateChooserDateFormat("%d.%m.%Y %H:%M");
		endDateElement.setCustomDateFormat("dd.MM.yyyy HH:mm");
		if (endDate != null) {
			endDateElement.setDate(endDate);
		}

		final Boolean showResultOnFinish = (Boolean) modConfig.get(IQEditController.CONFIG_KEY_RESULT_ON_FINISH);
		boolean confEnableShowResultOnFinish = (showResultOnFinish != null) ? showResultOnFinish.booleanValue() : true;
		confEnableShowResultOnFinish = !noSummary && confEnableShowResultOnFinish;
		showResultsAfterFinishTest = uifactory.addCheckboxesVertical("qti_enableResultsOnFinish", "qti.form.results.onfinish", formLayout, new String[] { "xx" },
				new String[] { null }, null, 1);
		showResultsAfterFinishTest.select("xx", confEnableShowResultOnFinish);
		showResultsAfterFinishTest.addActionListener(this, FormEvent.ONCLICK);

		final String[] summaryKeys = new String[] { AssessmentInstance.QMD_ENTRY_SUMMARY_COMPACT, AssessmentInstance.QMD_ENTRY_SUMMARY_SECTION,
				AssessmentInstance.QMD_ENTRY_SUMMARY_DETAILED };

		final String[] summaryValues = new String[] { translate("qti.form.summary.compact"), translate("qti.form.summary.section"),
				translate("qti.form.summary.detailed") };

		if (!isOnyx) {

			summary = uifactory.addRadiosVertical("qti_form_summary", "qti.form.summary", formLayout, summaryKeys, summaryValues);
			String confSummary = (String) modConfig.get(IQEditController.CONFIG_KEY_SUMMARY);
			uifactory.addSpacerElement("spcSummary", formLayout, true);
			if (confSummary == null || noSummary) {
				confSummary = AssessmentInstance.QMD_ENTRY_SUMMARY_COMPACT;
			}
			if (isAssessment || isSelfTest) {
				summary.select(confSummary, true);
			} else {
				summary.setEnabled(false);
			}

		}

		uifactory.addSpacerElement("submitSpacer", formLayout, true);
		uifactory.addFormSubmitButton("submit", formLayout);

		update();
	}

	@SuppressWarnings("unused")
	@Override
	protected void formInnerEvent(final UserRequest ureq, final FormItem source, final FormEvent event) {
		update();
	}

	private void update() {

		if (!isOnyx) {
			enableMenu.setVisible(displayMenu.isSelected(0));
			menuRenderOptions.setVisible(displayMenu.isSelected(0));
			summary.setVisible(showResultsAfterFinishTest.isSelected(0) || showResultsOnHomePage.isSelected(0));
		}

		if (!limitAttempts.isSelected(0)) {
			attempts.setIntValue(0);
		}
		attempts.setVisible(limitAttempts.isVisible() && limitAttempts.isSelected(0));
		attempts.setMandatory(attempts.isVisible());
		attempts.clearError();

		showResultsDateDependentButton.setVisible(showResultsOnHomePage.isSelected(0));

		if (!startDateElement.isVisible()) {
			startDateElement.setValue("");
		}
		startDateElement.clearError();
		startDateElement.setVisible(showResultsDateDependentButton.isVisible() && showResultsDateDependentButton.isSelected(0));

		endDateElement.clearError();
		if (!endDateElement.isVisible()) {
			endDateElement.setValue("");
		}
		endDateElement.setVisible(startDateElement.isVisible());
	}

	/**
	 * @return true: menu is enabled
	 */
	public boolean isEnableMenu() {
		return enableMenu.isSelected(0);
	}

	/**
	 * @return true: menu should be displayed
	 */
	public boolean isDisplayMenu() {
		return displayMenu.isSelected(0);
	}

	/**
	 * @return true: score progress is enabled
	 */
	public boolean isDisplayScoreProgress() {
		return displayScoreProgress.isSelected(0);
	}

	/**
	 * @return true: score progress is enabled
	 */
	public boolean isDisplayQuestionProgress() {
		return displayQuestionProgress.isSelected(0);
	}

	/**
	 * @return true: question title is enabled
	 */
	public boolean isDisplayQuestionTitle() {
		return displayQuestionTitle.isSelected(0);
	}

	/**
	 * @return sequence configuration: section or item
	 */
	public String getSequence() {
		return sequence.getSelectedKey();
	}

	/**
	 * @return true: cancel is enabled
	 */
	public boolean isEnableCancel() {
		return enableCancel.isSelected(0);
	}

	/**
	 * @return true: suspend is enabled
	 */
	public boolean isEnableSuspend() {
		return enableSuspend.isSelected(0);
	}

	/**
	 * @return summary type: compact or detailed
	 */
	public String getSummary() {
		return summary.getSelectedKey();
	}

	/**
	 * @return number of max attempts
	 */
	public Integer getAttempts() {
		final Integer a = attempts.getIntValue();
		return a == 0 ? null : attempts.getIntValue();
	}

	/**
	 * @return true if only section title should be rendered
	 */
	public Boolean isMenuRenderSectionsOnly() {
		return Boolean.valueOf(menuRenderOptions.getSelectedKey());
	}

	/**
	 * @return true: score-info on start-page is enabled
	 */
	public boolean isEnableScoreInfo() {
		return scoreInfo.isSelected(0);
	}

	/**
	 * @return true is the results are shown date dependent
	 */
	public boolean isShowResultsDateDependent() {
		return showResultsDateDependentButton.isSelected(0);
	}

	/**
	 * @return Returns the start date for the result visibility.
	 */
	public Date getShowResultsStartDate() {
		return startDateElement.getDate();
	}

	/**
	 * @return Returns the end date for the result visibility.
	 */
	public Date getShowResultsEndDate() {
		return endDateElement.getDate();
	}

	/**
	 * @return Returns true if the results are shown after test finished.
	 */
	public boolean isShowResultsAfterFinishTest() {
		return showResultsAfterFinishTest.isSelected(0);
	}

	/**
	 * @return Returns true if the results are shown on the test home page.
	 */
	public boolean isShowResultsOnHomePage() {
		return showResultsOnHomePage.isSelected(0);
	}

	/**
	 * @return Returns the points needes to pass an onyx test.
	 */
	public Integer getCutValue() {
		final Scanner cutValueScanner = new Scanner(cutValue.getValue());
		if (cutValueScanner.hasNextInt()) {
			return new Integer(cutValueScanner.nextInt());
		} else {
			return null;
		}
	}

	/**
	 * @return Returns the chosen template of an onyx test.
	 */
	public String getTemplate() {
		return template.getSelectedKey();
	}

	@Override
	protected void doDispose() {
		//
	}

}
