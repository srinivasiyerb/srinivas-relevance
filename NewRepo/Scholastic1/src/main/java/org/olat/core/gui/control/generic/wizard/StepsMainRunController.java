/**
 * 
 */
package org.olat.core.gui.control.generic.wizard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Window;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.elements.FormLinkImpl;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.creator.ControllerCreator;
import org.olat.core.logging.AssertException;
import org.olat.core.util.event.GenericEventListener;

/**
 * @author patrickb
 */
public class StepsMainRunController extends FormBasicController implements GenericEventListener {

	public final static Step DONE_UNCHANGED = new Step() {

		@Override
		public Step nextStep() {
			throw new IllegalAccessError("not to be called on NOSTEP");
		}

		@Override
		public PrevNextFinishConfig getInitialPrevNextFinishConfig() {
			throw new IllegalAccessError("not to be called on NOSTEP");
		}

		@Override
		public FormItem getStepTitle() {
			throw new IllegalAccessError("not to be called on NOSTEP");
		}

		@Override
		public FormItem getStepShortDescription() {
			throw new IllegalAccessError("not to be called on NOSTEP");
		}

		@Override
		@SuppressWarnings("unused")
		public StepFormController getStepController(UserRequest ureq, WindowControl windowControl, StepsRunContext stepsRunContext, Form form) {
			throw new IllegalAccessError("not to be called on NOSTEP");
		}

	};
	public final static Step DONE_MODIFIED = new Step() {

		@Override
		public Step nextStep() {
			throw new IllegalAccessError("not to be called on NOSTEP");
		}

		@Override
		public PrevNextFinishConfig getInitialPrevNextFinishConfig() {
			throw new IllegalAccessError("not to be called on NOSTEP");
		}

		@Override
		public FormItem getStepTitle() {
			throw new IllegalAccessError("not to be called on NOSTEP");
		}

		@Override
		public FormItem getStepShortDescription() {
			throw new IllegalAccessError("not to be called on NOSTEP");
		}

		@Override
		@SuppressWarnings("unused")
		public StepFormController getStepController(UserRequest ureq, WindowControl windowControl, StepsRunContext stepsRunContext, Form form) {
			throw new IllegalAccessError("not to be called on NOSTEP");
		}

	};

	private FormLink prevButton;
	private FormLink nextButton;
	private FormLink finishButton;
	private FormLink cancelButton;
	private FormLink closeLink;
	private Step startStep;
	// private Stack<FormItem> stepTitleLinks;
	private List<FormItem> stepTitleLinks;
	private int currentStepIndex = 0;
	private Stack<FormItem> stepPages;
	private StepsRunContext stepsContext;
	private Stack<StepFormController> stepPagesController;
	private Stack<Step> steps;
	private Event lastEvent;
	private boolean doAfterDispatch;
	Step nextStep;
	private ControllerCreator nextChildCreator;
	private int maxSteps;
	private StepRunnerCallback cancel;
	private StepRunnerCallback finish;
	private boolean finishCycle = false;

	/**
	 * @param ureq
	 * @param control
	 */
	public StepsMainRunController(UserRequest ureq, WindowControl control, Step startStep, StepRunnerCallback finish, StepRunnerCallback cancel, String wizardTitle) {
		super(ureq, control, "stepslayout");

		this.finish = finish;
		this.cancel = cancel;
		flc.contextPut("wizardTitle", wizardTitle);

		this.startStep = startStep;
		steps = new Stack<Step>();
		stepTitleLinks = new ArrayList<FormItem>();
		stepPages = new Stack<FormItem>();
		stepPagesController = new Stack<StepFormController>();
		stepsContext = new StepsRunContext() {
			Map<String, Object> context = new HashMap<String, Object>();

			@Override
			public void put(String key, Object value) {
				context.put(key, value);
			}

			@Override
			public Object get(String key) {
				return context.get(key);
			}

			@Override
			public boolean containsKey(String key) {
				return context.containsKey(key);
			}

		};
		initForm(ureq);
		// add current step index to velocity
		flc.contextPut("currentStepPos", currentStepIndex + 1);

		getWindowControl().getWindowBackOffice().addCycleListener(this);
	}

	/*
	 * (non-Javadoc)
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#doDispose()
	 */
	@Override
	protected void doDispose() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formOK(org.olat.core.gui.UserRequest)
	 */
	@Override
	@SuppressWarnings("unused")
	protected void formOK(UserRequest ureq) {
		// unused
	}

	@Override
	@SuppressWarnings("unused")
	protected void formInnerEvent(UserRequest ureq, FormItem source, org.olat.core.gui.components.form.flexible.impl.FormEvent event) {
		int whichTitleClickedIndex = stepTitleLinks.indexOf(source);
		if (source == cancelButton || source == closeLink) {
			if (cancel != null) {
				// execute some cancel / rollback code
				// a wizard is expected not to touch / change data in the cancel
				// case undo your work here.
				Step returnStep = cancel.execute(ureq, getWindowControl(), stepsContext);
				if (returnStep != Step.NOSTEP) {
					// error case FIXME:pb finish wizard for this case
				} else {
					// fireEvent(ureq, Event.CANCELLED_EVENT);
				}
			}
			fireEvent(ureq, Event.CANCELLED_EVENT);
		} else if (source == nextButton) {
			// submit and let current unsaved step do its work
			flc.getRootForm().submit(ureq);
			// the current step decides whether to proceed to the next step or
			// not.
		} else if (source == finishButton) {
			// submit and let last unsaved step do its work
			finishCycle = true;
			flc.getRootForm().submit(ureq);
			// the current step decides whether to proceed or not
			// an end step will fire FINISH
			// a intermediate step will fire NEXT .. but NEXT && FINISHCYCLE
			// means also finish
		} else if (source == prevButton) {
			lastEvent = StepsEvent.ACTIVATE_PREVIOUS;
			doAfterDispatch = true;
		} else if (whichTitleClickedIndex >= 0) {
			// handle a step title link
			// remove all steps until the clicked one
			for (int from = currentStepIndex; from > whichTitleClickedIndex; from--) {
				stepPages.pop();
				steps.pop();
				currentStepIndex--;
				stepTitleLinks.get(currentStepIndex).setEnabled(false);// disable
				// "previous"
				// step.
				StepFormController controller = stepPagesController.pop();
				controller.back();
				removeAsListenerAndDispose(controller);
				// update current step index to velocity
				flc.contextPut("currentStepPos", currentStepIndex + 1);
			}
			flc.add("FFO_CURRENTSTEPPAGE", stepPages.peek());
			PrevNextFinishConfig pnfConf = steps.peek().getInitialPrevNextFinishConfig();
			prevButton.setEnabled(pnfConf.isBackIsEnabled());
			nextButton.setEnabled(pnfConf.isNextIsEnabled());
			finishButton.setEnabled(pnfConf.isFinishIsEnabled());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#initForm(org.olat.core.gui.components.form.flexible.FormItemContainer,
	 * org.olat.core.gui.control.Controller, org.olat.core.gui.UserRequest)
	 */
	@Override
	@SuppressWarnings("unused")
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		formLayout.add("stepLinks", stepTitleLinks);
		// steps/wizard navigation .. as start most of buttons are disabled
		// they must be enabled by the first step according to its rules
		// cancel button is not possible to disable
		prevButton = new FormLinkImpl("back");
		prevButton.setCustomEnabledLinkCSS("b_button b_wizard_button_prev");
		prevButton.setCustomDisabledLinkCSS("b_button b_wizard_button_prev");
		nextButton = new FormLinkImpl("next");
		nextButton.setCustomEnabledLinkCSS("b_button b_wizard_button_next");
		nextButton.setCustomDisabledLinkCSS("b_button b_wizard_button_next");
		finishButton = new FormLinkImpl("finish");
		finishButton.setCustomEnabledLinkCSS("b_button b_wizard_button_finish");
		finishButton.setCustomDisabledLinkCSS("b_button b_wizard_button_finish");
		cancelButton = new FormLinkImpl("cancel");
		cancelButton.setCustomEnabledLinkCSS("b_button b_wizard_button_cancel");
		cancelButton.setCustomDisabledLinkCSS("b_button b_wizard_button_cancel");
		closeLink = new FormLinkImpl("closeIcon", "close", "", Link.NONTRANSLATED);
		closeLink.setCustomEnabledLinkCSS("b_link_close");
		formLayout.add(prevButton);
		formLayout.add(nextButton);
		formLayout.add(finishButton);
		formLayout.add(cancelButton);
		formLayout.add(closeLink);
		// add all step titles, but disabled.
		Step tmp = startStep;
		maxSteps = 0;
		do {
			FormItem title = tmp.getStepTitle();
			title.setEnabled(false);
			stepTitleLinks.add(title);
			maxSteps++;
			tmp = tmp.nextStep();
		} while (tmp != Step.NOSTEP);
		// init buttons and the like
		currentStepIndex = -1;// start with -1 to be on zero after calling
		// update current step index to velocity
		flc.contextPut("currentStep", currentStepIndex + 1);
		// next step the first time
		addNextStep(startStep.getStepController(ureq, getWindowControl(), this.stepsContext, this.mainForm), startStep);
	}

	@SuppressWarnings("unused")
	private void addNextStep(StepFormController child, Step nextStep) {

		currentStepIndex++;

		if (!stepTitleLinks.isEmpty() && currentStepIndex > 0) {
			// enable previous step
			stepTitleLinks.get(currentStepIndex - 1).setEnabled(true);
		}
		// update current step index to velocity
		flc.contextPut("currentStepPos", currentStepIndex + 1);

		flc.add("stepLinks", stepTitleLinks);

		listenTo(child);
		steps.push(nextStep);
		stepPages.push(child.getStepFormItem());
		stepPagesController.push(child);
		flc.add("FFO_CURRENTSTEPPAGE", stepPages.peek());

		PrevNextFinishConfig pnfConf = nextStep.getInitialPrevNextFinishConfig();
		prevButton.setEnabled(pnfConf.isBackIsEnabled());
		nextButton.setEnabled(pnfConf.isNextIsEnabled());//
		finishButton.setEnabled(pnfConf.isFinishIsEnabled());
	}

	@Override
	/*
	 * (non-Javadoc)
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	protected void event(final UserRequest ureq, Controller source, Event event) {
		/*
		 * FIXME:pb:
		 */
		if (source == stepPagesController.peek()) {

			if (event == StepsEvent.ACTIVATE_NEXT && !finishCycle) {
				// intermediate steps wants to proceed - and next link was clicked
				lastEvent = event;
				doAfterDispatch = true;
				// activate next event on source
				Step current = steps.peek();
				// TODO:pb detach previous from "submit" cycle
				nextStep = current.nextStep();
				nextChildCreator = new ControllerCreator() {
					private final UserRequest ureqForAfterDispatch = ureq;

					@Override
					@SuppressWarnings("unused")
					public Controller createController(UserRequest lureq, WindowControl lwControl) {
						// lureq unused as the remembered ureqForAfterDispatch is
						// taken
						return nextStep.getStepController(ureqForAfterDispatch, lwControl, stepsContext, mainForm);
					}
				};
				// creation of controller and setting the controller is deferred to
				// the afterDispatch Cycle
				//
			} else if (event == StepsEvent.ACTIVATE_NEXT && finishCycle) {
				// intermediate step wants to proceed - but finish link was clicked
				// this means current step validated and we are ready to terminate
				// the wizard.
				finishWizard(ureq);
			} else if (event == StepsEvent.INFORM_FINISHED) {
				// finish link was clicked -> step form controller has valid data ->
				// fires
				// FINISH EVENT
				// all relevant data for finishing the wizards work must now be
				// present in the stepsContext
				finishWizard(ureq);
			}

		}

	}

	private void finishWizard(final UserRequest ureq) {
		if (finish == null) { throw new AssertException(
				"You must provide a finish callback - a wizard only makes sense to commit work at the end. Do not change data allong the steps."); }
		Step returnStep = finish.execute(ureq, getWindowControl(), stepsContext);
		if (returnStep == DONE_MODIFIED) {
			// finish tells that really some data was changed in this wizard
			fireEvent(ureq, Event.CHANGED_EVENT);
		} else if (returnStep == DONE_UNCHANGED) {
			// finish called but nothing was modified
			fireEvent(ureq, Event.DONE_EVENT);
		} else {
			// special step comes back
			throw new AssertException("FIXME:pb treat special error steps");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.olat.core.util.event.GenericEventListener#event(org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(Event event) {
		/*
		 * activate a new step immediate after dispatch - a new step form controller can only be added to surrounding steps runner after dispatching - otherwise a
		 * concurrent modification exception within the form container occurs.
		 */
		if (event == Window.END_OF_DISPATCH_CYCLE && doAfterDispatch) {
			doAfterDispatch = false;
			if (lastEvent == StepsEvent.ACTIVATE_NEXT) {
				// create with null as UserRequest, the controller creator was
				// created
				// during dispatching and controller creation was deferred to
				// the end of
				// dispatch cycle.
				addNextStep((StepFormController) nextChildCreator.createController(null, getWindowControl()), nextStep);
			} else if (lastEvent == StepsEvent.ACTIVATE_PREVIOUS) {
				stepPages.pop();
				steps.pop();
				currentStepIndex--;
				// update current step index to velocity
				flc.contextPut("currentStepPos", currentStepIndex + 1);
				// disable "previous" step.
				stepTitleLinks.get(currentStepIndex).setEnabled(false);
				StepFormController controller = stepPagesController.pop();
				controller.back();
				removeAsListenerAndDispose(controller);
				flc.add("FFO_CURRENTSTEPPAGE", stepPages.peek());
				PrevNextFinishConfig pnfConf = steps.peek().getInitialPrevNextFinishConfig();
				prevButton.setEnabled(pnfConf.isBackIsEnabled());
				nextButton.setEnabled(pnfConf.isNextIsEnabled());
				finishButton.setEnabled(pnfConf.isFinishIsEnabled());
			}
		}
	}
}
