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
package org.olat.course.editor;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.wizard.BasicStep;
import org.olat.core.gui.control.generic.wizard.PrevNextFinishConfig;
import org.olat.core.gui.control.generic.wizard.Step;
import org.olat.core.gui.control.generic.wizard.StepFormBasicController;
import org.olat.core.gui.control.generic.wizard.StepFormController;
import org.olat.core.gui.control.generic.wizard.StepsEvent;
import org.olat.core.gui.control.generic.wizard.StepsRunContext;

/**
 * Description:<br>
 * TODO: patrickb Class Description for PublishStep00b
 * <P>
 * Initial Date: 24.01.2008 <br>
 * 
 * @author patrickb
 */
class PublishStep00b extends BasicStep implements Step {

	public PublishStep00b(final UserRequest ureq) {
		super(ureq);
		setI18nTitleAndDescr("publish.step.title.confirm", null);
		// not setting setNextStep(...) -> Step.NOSTEP is used.
	}

	/**
	 * @see org.olat.core.gui.control.generic.wizard.BasicStep#getInitialPrevNextFinishConfig()
	 */
	@Override
	public PrevNextFinishConfig getInitialPrevNextFinishConfig() {
		return PrevNextFinishConfig.BACK_FINISH;
	}

	/**
	 * @see org.olat.core.gui.control.generic.wizard.BasicStep#getStepController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl,
	 *      org.olat.core.gui.control.generic.wizard.StepsRunContext, org.olat.core.gui.components.form.flexible.impl.Form)
	 */
	@Override
	public StepFormController getStepController(final UserRequest ureq, final WindowControl windowControl, final StepsRunContext stepsRunContext, final Form form) {
		return new PublishStep00bForm(ureq, windowControl, form, stepsRunContext);
	}

	/**
	 * @see org.olat.core.gui.control.generic.wizard.BasicStep#nextStep()
	 */
	@Override
	public Step nextStep() {
		// could use the default implementation, but to be on the save side and
		// for the sake of documentation - this makes clear that next step is nostep.
		return Step.NOSTEP;
	}

	class PublishStep00bForm extends StepFormBasicController {

		public PublishStep00bForm(final UserRequest ureq, final WindowControl control, final Form rootForm, final StepsRunContext runContext) {
			super(ureq, control, rootForm, runContext, LAYOUT_VERTICAL, null);

			initForm(ureq);
		}

		@Override
		protected void doDispose() {
			// TODO Auto-generated method stub

		}

		@Override
		protected void formOK(final UserRequest ureq) {
			fireEvent(ureq, StepsEvent.ACTIVATE_NEXT);
		}

		@Override
		@SuppressWarnings("unused")
		protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
			final PublishProcess pp = (PublishProcess) getFromRunContext("publishProcess");
			final String confirmMsg = pp.assemblePublishConfirmation();
			uifactory.addStaticTextElement("message", null, confirmMsg, formLayout);// null > no label
		}

	}

}
