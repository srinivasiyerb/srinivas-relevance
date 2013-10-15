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
 * Copyright (c) 1999-2006 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */
package org.olat.core.gui.components.form.flexible.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadBase.SizeLimitExceededException;
import org.apache.commons.fileupload.MultipartStream.MalformedStreamException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.olat.core.gui.GUIInterna;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.Container;
import org.olat.core.gui.components.form.flexible.FormBaseComponentIdProvider;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.translator.Translator;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.LogDelegator;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.util.ArrayHelper;
import org.olat.core.util.CodeHelper;
import org.olat.core.util.FileUtils;
import org.olat.core.util.ValidationStatus;
import org.olat.core.util.ValidationStatusHelper;
import org.olat.core.util.component.FormComponentTraverser;
import org.olat.core.util.component.FormComponentVisitor;

/**
 * <h4>Description:<h4>This Form is responsible for creation of the form header and footer. It can not hold form elements. Instead one has to create a form container and
 * put the form elements there. E.g. use FormLayoutContainer or FormVelocityContainer.
 * <p>
 * This Form is the Component which gets dispatched by the framework. It then dispatches further to the really clicked FormComponent. The Form implements the following
 * phases:
 * <ol>
 * <li>let all Formelements evaluate the Formrequest</li>
 * <li>dispatch to the correct FormComponent</li>
 * <li>the dispatched FormComponent may decide to SUBMIT the form, e.g. let all FormComponents validate its input and report error, or taken other actions.</li>
 * <li>during the validatition phase each FormComponent can register an action</li>
 * <li>after the validation, all actions are applied</li>
 * <li>an event is thrown if the form validated or not</li>
 * </ol>
 * <p>
 * FormComponent and FormContainer form the same composite pattern as already used for the core.Component and core.Container, and take notice that the FormComponent
 * itself is also a core.Component!<br>
 * As a consequence of this, each element which want to live inside of a form must be a FormComponent but has also a Component side to the rendering framework.
 * <p>
 * The goals of the new form infrastructure are
 * <ul>
 * <li>to allow complete freedom for layouting forms.</li>
 * <li>easy-migration path for existing forms</li>
 * <li>easy-usage for developers</li>
 * <li>easy-layouting for designers</li>
 * <li>allow subworkflows in forms without loosing form input</li>
 * <li>allow AJAX features for form elements (completion, on blur etc.)</li>
 * </ul>
 * Some extra care had to be taken to fullfill these requirements and still beeing compliant with the already existing AJAX component replacement.<br>
 * It was decided that a FormComponent consist of
 * <ul>
 * <li>Formelement, e.g. input field, radio button, select box, a link!</li>
 * <li>Label for the Formelement</li>
 * <li>Error for the Formelement</li>
 * <li>Example for the Formelement</li>
 * </ul>
 * <p>
 * <h4>Multipart and file upload</h4> Since release 6.1 the form infrastructure does also support multipart form data (file uploads). The form switches to the multipart
 * mode as soon as a form element of type FormMultipartItem is added. In this case, all file uploads and form parameters are parsed by the form class and added to the
 * requestParams and requestMultipartFiles maps. If no multipart element is in the form, the normal non-multipart way is used (less overhead, stability).
 * <p>
 * Therefore it is important to always use the form.getParameter() methods and not the getParameter() methods from the user request directly. Normally you don't have to
 * deal with this because the implemented form elements already take care of this issue.
 * <p>
 * All submitted files are saved to a temporary location in the userdata/tmp/ directory. During the dispatch phase in evalFormRequest() this files can be access using the
 * getMultipartFilesSet() and getMultipartFile() methods. The files must be moved to another location within the execution of the evalFormRequest() because at the end of
 * the method call, the temporary files will be removed. The temporary files have a random file name, use the getMultipartFileName() to retrieve the original file name.
 * <p>
 * When using the FileElement this is all already encapsulated, see the documentation there.
 * <p>
 * Initial Date: 27.11.2006 <br>
 * 
 * @author patrickb
 */
public class Form extends LogDelegator {
	//
	public final static String FORMCMD = "fid";
	public final static String FORMID = "ofo_";
	public final static String FORM_UNDEFINED = "undefined";

	public final static int REQUEST_ERROR_NO_ERROR = -1;
	public final static int REQUEST_ERROR_GENERAL = 1;
	public final static int REQUEST_ERROR_FILE_EMPTY = 2;
	public final static int REQUEST_ERROR_UPLOAD_LIMIT_EXCEEDED = 3;

	private String formName;
	private String dispatchFieldId;
	private String eventFieldId;
	private Map actionListeners = new HashMap(5);

	// the real form
	private FormItemContainer formLayout = null;
	private FormWrapperContainer formWrapperComponent;
	private Integer action;
	private boolean hasAlreadyFired;
	private List<FormBasicController> formListeners;
	private boolean isValidAndSubmitted = true;
	private FormItem submitFormItem = null;
	private boolean isDirtyMarking = true;
	private boolean multipartEnabled = false;
	private int multipartUploadMaxSizeKB = 0;
	// temporary form data, only valid within execution of evalFormRequest()
	private Map<String, String[]> requestParams = new HashMap<String, String[]>();
	private Map<String, File> requestMultipartFiles = new HashMap<String, File>();
	private Map<String, String> requestMultipartFileNames = new HashMap<String, String>();
	private Map<String, String> requestMultipartFileMimeTypes = new HashMap<String, String>();
	private int requestError = REQUEST_ERROR_NO_ERROR;

	private Form(Controller listener) {
		// internal use only

		if (GUIInterna.isLoadPerformanceMode()) {
			initReplayIdCounter(listener);
		}
	}

	/**
	 * create a new form, where the caller is attached as component listener. Caller receives form validation success or failure events.
	 * 
	 * @param name
	 * @param translator
	 * @param rootFormContainer if null the default layout is choosen, otherwise the given layouting container is taken.
	 * @param listener the component listener of this form, typically the caller
	 * @return
	 */
	public static Form create(String name, FormItemContainer formLayout, Controller listener) {
		Form form = new Form(listener);
		// this is where the formitems go to
		form.formLayout = formLayout;
		form.formLayout.setRootForm(form);
		form.formListeners = new ArrayList<FormBasicController>();
		if (listener instanceof FormBasicController) {
			form.formListeners.add((FormBasicController) listener);
		}
		Translator translator = formLayout.getTranslator();
		if (translator == null) { throw new AssertException("please provide a translator in the FormItemContainer <" + formLayout.getName() + ">"); }
		// renders header + <formLayout> + footer of html form
		form.formWrapperComponent = new FormWrapperContainer(name, translator, form);
		form.formWrapperComponent.addListener(listener);
		form.formWrapperComponent.put(formLayout.getComponent().getComponentName(), formLayout.getComponent());
		// generate name for form and dispatch uri hidden field

		form.formName = Form.FORMID + form.formWrapperComponent.getDispatchID();
		form.dispatchFieldId = form.formName + "_dispatchuri";
		form.eventFieldId = form.formName + "_eventval";

		return form;
	}

	/**
	 * @param ureq
	 */
	public void evalFormRequest(UserRequest ureq) {
		// Initialize temporary request parameters
		doInitRequestParameterAndMulipartData(ureq);

		String dispatchUri = getRequestParameter("dispatchuri");
		String dispatchAction = getRequestParameter("dispatchevent");
		boolean invalidDispatchUri = dispatchUri == null || dispatchUri.equals(FORM_UNDEFINED);
		boolean invalidDispatchAct = dispatchAction == null || dispatchAction.equals(FORM_UNDEFINED);
		boolean implicitFormSubmit = false;// see also OLAT-3141
		if (invalidDispatchAct && invalidDispatchUri) {
			// case if:
			// enter was pressed in Safari / IE
			// crawler tries form links
			if (submitFormItem != null) {
				// if we have submit form item
				// assume a click on this item
				dispatchUri = FormBaseComponentIdProvider.DISPPREFIX + submitFormItem.getComponent().getDispatchID();
				action = FormEvent.ONCLICK;
			} else {
				// instead of
				// throw new AssertException("You have an input field but no submit item defined! this is no good if enter is pressed.");
				// assume a desired implicit form submit
				// see also OLAT-3141
				implicitFormSubmit = true;
			}
		} else {
			action = Integer.valueOf(dispatchAction);
		}
		hasAlreadyFired = false;
		isValidAndSubmitted = false;
		//
		// step 1: call evalFormRequest(ureq) on each FormComponent this gives
		// ....... for each element the possibility to intermediate save a value.
		// ....... As a sideeffect the formcomponent to be dispatched is found.
		EvaluatingFormComponentVisitor efcv = new EvaluatingFormComponentVisitor(dispatchUri);
		FormComponentTraverser ct = new FormComponentTraverser(efcv, formLayout, false);
		ct.visitAll(ureq);
		// step 2: dispatch to the form component
		// ......... only one component to be dispatched can be found, e.g. clicked
		// ......... element....................................................
		// ......... dispatch changes server model -> rerendered
		// ......... dispatch may also request a form validation by
		// ......... calling the submit
		FormItem dispatchFormItem = efcv.getDispatchToComponent();
		// .......... doDispatchFormRequest is called on the found item
		// .......... which in turn may call submit(UserRequest ureq).
		// .......... After submitting, which fires a ok/nok event
		// .......... the code goes further with step 3.........................
		if (implicitFormSubmit) {
			// implicit Submit (Press Enter without on a Field without submit item.)
			// see also OLAT-3141
			submit(ureq);
		} else {
			if (dispatchFormItem == null) {
				// source not found. This "never happens". Try to produce some hints.
				String fbc = new String();
				for (FormBasicController i : formListeners) {
					if (fbc.length() > 0) {
						fbc += ",";
					}
					fbc += (i.getClass().getName());
				}
				logWarn("OLAT-5061: Could not determine request source in FlexiForm >" + formName + "<. Check >" + fbc + "<", null);

				// TODO: what now?
				// Assuming the same as "implicitFormSubmit" for now.
				submit(ureq);

			} else {
				// ****************************************
				// explicit Submit or valid form dispatch *
				// ****************************************
				dispatchFormItem.doDispatchFormRequest(ureq);
				// step 3: find parent container of dispatched component
				// .......... check dependency rules
				// .......... apply dependency rules
				FindParentFormComponentVisitor fpfcv = new FindParentFormComponentVisitor(dispatchFormItem);
				ct = new FormComponentTraverser(fpfcv, formLayout, false);
				ct.visitAll(ureq);
				fpfcv.getParent().evalDependencyRuleSetFor(ureq, dispatchFormItem);
			}
		}
		//
		action = -1;

		// End of request dispatch: cleanup temp files: ureq requestParams and multipart files
		doClearRequestParameterAndMultipartData();
	}

	/**
	 * Internal helper to initialize the request parameter map an to temporary store the uploaded files when a multipart request is used. The files are stored to a
	 * temporary location and a filehandle is added to the requestMultipartFiles map for later retrieval by the responsible FormItem.
	 * 
	 * @param ureq
	 */
	private void doInitRequestParameterAndMulipartData(UserRequest ureq) {
		// First fill parameter map either from multipart data or standard http request
		if (isMultipartEnabled() && ServletFileUpload.isMultipartContent(ureq.getHttpReq())) {
			long uploadSize = -1; // default unlimited
			// Limit size of complete upload form: upload size limit + 500k for
			// other input fields
			if (multipartUploadMaxSizeKB > -1) {
				uploadSize = (multipartUploadMaxSizeKB * 1024l * 1024l) + 512000l;
			}

			// Create a new file upload handler, use commons fileupload streaming
			// API to save files right to the tmp location
			ServletFileUpload uploadParser = new ServletFileUpload();
			uploadParser.setSizeMax(uploadSize);
			// Parse the request
			try {
				FileItemIterator iter = uploadParser.getItemIterator(ureq.getHttpReq());
				while (iter.hasNext()) {
					FileItemStream item = iter.next();
					String itemName = item.getFieldName();
					InputStream itemStream = item.openStream();
					if (item.isFormField()) {
						// Normal form item
						// analog to ureq.getParameter in non-multipart mode
						String value = Streams.asString(itemStream, "UTF-8");
						addRequestParameter(itemName, value);
					} else {
						// File item, store it to temp location
						String fileName = item.getName();
						// Cleanup IE filenames that are absolute
						int slashpos = fileName.lastIndexOf("/");
						if (slashpos != -1) fileName = fileName.substring(slashpos + 1);
						slashpos = fileName.lastIndexOf("\\");
						if (slashpos != -1) fileName = fileName.substring(slashpos + 1);

						File tmpFile = new File(System.getProperty("java.io.tmpdir") + File.separator + "upload-" + CodeHelper.getGlobalForeverUniqueID());

						try {
							FileUtils.save(itemStream, tmpFile);
							// Only save non-empty file transfers, ignore empty transfers
							// (e.g. already submitted in a previous form submit, not an error!)

							// Removing empty file check for now ... was introduced to cope with
							// browser trouble which probably is not there any more ...
							// so empty fileName means nothing selected in the file element

							// if (tmpFile.length() > 0) {
							if (fileName.length() > 0) {
								// a file was selected
								// Save file and also original file name
								requestMultipartFiles.put(itemName, tmpFile);
								requestMultipartFileNames.put(itemName, fileName);
								requestMultipartFileMimeTypes.put(itemName, item.getContentType());
							} else {
								if (tmpFile.exists()) tmpFile.delete();
							}
						} catch (OLATRuntimeException e) {
							// Could not save stream for whatever reason, cleanup temp file and delegate exception
							if (tmpFile.exists()) tmpFile.delete();

							if (e.getCause() instanceof MalformedStreamException) {
								logWarn("Could not read uploaded file >" + fileName
										+ "< from stream. Possibly an attempt to upload a directory instead of a file (happens on Mac)", e);
								return;
							}

							throw new OLATRuntimeException("Could not save uploaded file", e);
						}
					}
				}
			} catch (SizeLimitExceededException sizeLimitExceededException) {
				logError("Error while dispatching multipart form: file limit (" + uploadSize + ") exceeded", sizeLimitExceededException);
				requestError = REQUEST_ERROR_UPLOAD_LIMIT_EXCEEDED;
			} catch (IOException e) {
				logWarn("Error while dispatching multipart form: ioexception", e);
				requestError = REQUEST_ERROR_GENERAL;
			} catch (Exception e) {
				logError("Error while dispatching multipart form: general exception", e);
				requestError = REQUEST_ERROR_GENERAL;
			}
		} else {
			// Get parameters the standard way
			logDebug("Dispatching non-multipart form", null);
			Set<String> keys = ureq.getParameterSet();
			for (String key : keys) {
				String[] values = ureq.getHttpReq().getParameterValues(key);
				if (values != null) {
					requestParams.put(key, values);
				} else {
					addRequestParameter(key, ureq.getParameter(key));
				}
			}
		}
	}

	/**
	 * Internal helper to add the request parameters to the request param map. Takes care of multi value parameters
	 * 
	 * @param key
	 * @param value
	 */
	private void addRequestParameter(String key, String value) {
		String[] values = requestParams.get(key);
		if (values == null) {
			// First element for this key
			values = new String[] { value };
			// use log.debug instead of System.out.println("PARAMS:"+key+" :: "+value);
		} else {
			// A multi-key element (e.g. radio button)
			values = ArrayHelper.addToArray(values, value, true);
			// use log.debug instead of System.out.println("PARAMS:"+key+" ::[array of values]");
		}

		requestParams.put(key, values);
	}

	/**
	 * Internal helper to clear the temporary request parameter and file maps. Will delete all uploaded files if they have not been removed by the responsible FormItem.
	 */
	private void doClearRequestParameterAndMultipartData() {
		for (Entry<String, File> entry : requestMultipartFiles.entrySet()) {
			File tmpFile = entry.getValue();
			if (tmpFile.exists()) tmpFile.delete();
		}
		requestMultipartFiles.clear();
		requestMultipartFileNames.clear();
		requestMultipartFileMimeTypes.clear();
		requestParams.clear();
		requestError = REQUEST_ERROR_NO_ERROR;
	}

	/**
	 * Check if there was an error while parsing this request. See REQUEST_ERROR_* constants
	 * 
	 * @return the last error code
	 */
	public int getLastRequestError() {
		return requestError;
	}

	/**
	 * @param ureq
	 */
	public void submit(UserRequest ureq) {
		ValidatingFormComponentVisitor vfcv = new ValidatingFormComponentVisitor();
		FormComponentTraverser ct = new FormComponentTraverser(vfcv, formLayout, false);
		ct.visitAll(ureq);
		// validate all form elements and gather validation status
		ValidationStatus[] status = vfcv.getStatus();
		//
		boolean isValid = status == null || status.length == 0;
		// let the businesslogic validate this is implemented by the outside listener

		for (Iterator<FormBasicController> iterator = formListeners.iterator(); iterator.hasNext();) {
			FormBasicController fbc = iterator.next();
			// let all listeners validate and calc the total isValid
			// let further validate even if one fails. TODO:pb discuss with cg
			isValid = fbc.validateFormLogic(ureq) && isValid;
		}

		//
		formWrapperComponent.fireValidation(ureq, isValid);
		isValidAndSubmitted = isValid;
		hasAlreadyFired = true;
	}

	/**
	 * @param ureq
	 */
	public void reset(UserRequest ureq) {
		ResettingFormComponentVisitor rfcv = new ResettingFormComponentVisitor();
		FormComponentTraverser ct = new FormComponentTraverser(rfcv, formLayout, false);
		ct.visitAll(ureq);// calls reset on all elements!
		//
		evalAllFormDependencyRules(ureq);
		//
		formWrapperComponent.fireFormEvent(ureq, FormEvent.RESET);
		hasAlreadyFired = true;
	}

	/**
	 * @return
	 */
	Container getFormLayout() {
		return (Container) formLayout.getComponent();
	}

	public Container getInitialComponent() {
		return formWrapperComponent;
	}

	/**
	 * add another listener then the default listener, which is added at construction time.
	 * 
	 * @param listener
	 */
	public void addListener(Controller listener) {
		formWrapperComponent.addListener(listener);
	}

	public void removeListener(Controller listener) {
		formWrapperComponent.removeListener(listener);
	}

	/**
	 * Return the form parameter for a certain key. This takes care if a multipart form has been used or a normal form.
	 * <p>
	 * LiveCycle scope: only within one call of evalFormRequest() !
	 * 
	 * @param key
	 * @return the value of the parameter with key 'key'
	 */
	public String getRequestParameter(String key) {
		String[] values = requestParams.get(key);
		if (values != null) return values[0];
		else return null;
	}

	/**
	 * Return the form parameter values for a certain key. This takes care if a multipart form has been used or a normal form. <br />
	 * This method is used to retrieve multi-value elements, e.g. radio buttons.<br />
	 * Use the getRequestParameter() to retrieve single value elements, e.g. input type=text elements
	 * 
	 * @param key
	 * @return Array of values for this key
	 */
	public String[] getRequestParameterValues(String key) {
		return requestParams.get(key);
	}

	/**
	 * Return the form parameter set. This takes care if a multipart form has been used or a normal form.
	 * <p>
	 * LiveCycle scope: only within one call of evalFormRequest() !
	 * 
	 * @return the Set of parameters
	 */
	public Set<String> getRequestParameterSet() {
		return requestParams.keySet();
	}

	/**
	 * Return the multipart file for this key
	 * <p>
	 * LiveCycle scope: only within one call of evalFormRequest() !
	 * 
	 * @param key
	 * @return
	 */
	public File getRequestMultipartFile(String key) {
		return requestMultipartFiles.get(key);
	}

	/**
	 * Return the multipart file name for this key:
	 * <p>
	 * LiveCycle scope: only within one call of evalFormRequest() !
	 * 
	 * @param key
	 * @return
	 */
	public String getRequestMultipartFileName(String key) {
		return requestMultipartFileNames.get(key);
	}

	/**
	 * Return the multipart file mime type (content type) for this key:
	 * <p>
	 * LiveCycle scope: only within one call of evalFormRequest() !
	 * 
	 * @param key
	 * @return
	 */
	public String getRequestMultipartFileMimeType(String key) {
		return requestMultipartFileMimeTypes.get(key);
	}

	/**
	 * @return The set of multipart file identifyers
	 */
	public Set<String> getRequestMultipartFilesSet() {
		return requestMultipartFiles.keySet();
	}

	/**
	 * Description:<br>
	 * TODO: patrickb Class Description for EvaluatingFormComponentVisitor
	 * <P>
	 * Initial Date: 04.12.2006 <br>
	 * 
	 * @author patrickb
	 */
	private class EvaluatingFormComponentVisitor implements FormComponentVisitor {

		private boolean foundDispatchItem = false;
		private FormItem dispatchFormItem = null;
		private String dispatchId;

		public EvaluatingFormComponentVisitor(String dispatchUri) {
			this.dispatchId = dispatchUri;
		}

		public FormItem getDispatchToComponent() {
			return dispatchFormItem;
		}

		@Override
		public boolean visit(FormItem fi, UserRequest ureq) {
			/*
			 * check if this is the FormItem to be dispatched
			 */
			Component tmp = fi.getComponent();

			String tmpD;
			if (GUIInterna.isLoadPerformanceMode()) {
				tmpD = FormBaseComponentIdProvider.DISPPREFIX + Long.toString(getReplayableDispatchID(fi.getComponent()));
			} else {
				tmpD = FormBaseComponentIdProvider.DISPPREFIX + tmp.getDispatchID();
			}

			if (!foundDispatchItem && tmpD.equals(dispatchId)) {
				dispatchFormItem = fi;
				foundDispatchItem = true; //
			}

			/*
			 * let the form item evaluate the form request, e.g. get out its data
			 */
			fi.evalFormRequest(ureq);
			return true;// visit further
		}

	}

	private class FindParentFormComponentVisitor implements FormComponentVisitor {

		private FormItem child;
		private FormItemContainer parentContainer = formLayout;

		public FindParentFormComponentVisitor(FormItem child) {
			this.child = child;
		}

		public FormItemContainer getParent() {
			return parentContainer;
		}

		@Override
		public boolean visit(FormItem comp, UserRequest ureq) {
			if (comp instanceof FormItemContainer) {
				FormItemContainer new_name = (FormItemContainer) comp;
				if (new_name.getFormComponents().containsValue(child)) {
					parentContainer = (FormItemContainer) comp;
					return false;
				}
			}
			return true;
		}

	}

	private class FormDependencyRulesInitComponentVisitor implements FormComponentVisitor {

		@Override
		public boolean visit(FormItem comp, UserRequest ureq) {
			if (comp instanceof FormItemContainer) {
				FormItemContainer fic = (FormItemContainer) comp;
				Map<String, FormItem> pairs = fic.getFormComponents();
				// go to next container if no elements inside
				if (pairs == null || pairs.size() == 0) return true;
				// otherwise iterate overall elements and evaluate dependency rules
				Iterable<FormItem> elms = pairs.values();
				for (FormItem item : elms) {
					fic.evalDependencyRuleSetFor(ureq, item);
				}
			}
			return true;
		}

	}

	/**
	 * Description:<br>
	 * TODO: patrickb Class Description for ValidatingFormComponentVisitor
	 * <P>
	 * Initial Date: 07.12.2006 <br>
	 * 
	 * @author patrickb
	 */
	private class ValidatingFormComponentVisitor implements FormComponentVisitor {

		List tmp = new ArrayList();

		public ValidationStatus[] getStatus() {
			return ValidationStatusHelper.sort(tmp);
		}

		@Override
		public boolean visit(FormItem comp, UserRequest ureq) {
			if (comp.isVisible() && comp.isEnabled()) {
				// validate only if form item is visible and enabled
				comp.validate(tmp);
			}
			return true;
		}
	}

	/**
	 * Description:<br>
	 * TODO: patrickb Class Description for ValidatingFormComponentVisitor
	 * <P>
	 * Initial Date: 07.12.2006 <br>
	 * 
	 * @author patrickb
	 */
	private class ResettingFormComponentVisitor implements FormComponentVisitor {

		@Override
		public boolean visit(FormItem comp, UserRequest ureq) {
			// reset all fields including also non visible and disabled form items!
			comp.reset();
			return true;
		}
	}

	public String getDispatchFieldId() {
		return dispatchFieldId;
	}

	public String getFormName() {
		return formName;
	}

	public void fireFormEvent(UserRequest ureq, FormEvent event) {
		formWrapperComponent.fireFormEvent(ureq, event);
		hasAlreadyFired = true;
	}

	boolean hasAlreadyFired() {
		return hasAlreadyFired;
	}

	/**
	 * @return Returns the eventFieldId.
	 */
	public String getEventFieldId() {
		return eventFieldId;
	}

	public int getAction() {
		return action;
	}

	/**
	 * @param ureq
	 */
	void evalAllFormDependencyRules(UserRequest ureq) {
		FormDependencyRulesInitComponentVisitor fdrocv = new FormDependencyRulesInitComponentVisitor();
		FormComponentTraverser ct = new FormComponentTraverser(fdrocv, formLayout, false);
		ct.visitAll(ureq);// visit all container and eval container with its elements!
	}

	public boolean isSubmittedAndValid() {
		return isValidAndSubmitted;
	}

	void registerSubmit(FormItem submFormItem) {
		this.submitFormItem = submFormItem;
	}

	/**
	 * true if the form should not loose unsubmitted changes, if another link is clicked which throws away the changes.
	 * 
	 * @return
	 */
	public boolean isDirtyMarking() {
		return isDirtyMarking;
	}

	public void setDirtyMarking(boolean isDirtyMarking) {
		this.isDirtyMarking = isDirtyMarking;
	}

	public void addSubFormListener(FormBasicController formBasicController) {
		this.formListeners.add(formBasicController);
		addListener(formBasicController);
	}

	public void removeSubFormListener(FormBasicController formBasicController) {
		this.formListeners.remove(formBasicController);
		removeListener(formBasicController);
	}

	public void setMultipartEnabled(boolean multipartEnabled, int multipartUploadMaxSizeKB) {
		this.multipartEnabled = multipartEnabled;
		// Add upload size to already existing upload limit (two uploads are summed up)
		this.multipartUploadMaxSizeKB += multipartUploadMaxSizeKB;
	}

	public boolean isMultipartEnabled() {
		return multipartEnabled;
	}

	// replayableID Counter
	private long replayIdCount;

	/**
	 * Make replayID distinct for distinct forms by inserting the number of Form Objects created during the current session at the 10000 position. So the replayID will
	 * look like o_fi900060004 for the fourth FormItem of the sixth Form. The last four digits represent the running number of Items in the current Form.
	 * 
	 * @param form
	 * @param name
	 */

	@SuppressWarnings("unchecked")
	private void initReplayIdCounter(Controller listener) {

		Integer formNum = null;
		Object o;
		Map m;

		o = GUIInterna.getReplayModeData().get("formsSeen");

		if (o == null) {
			o = new HashMap();
			GUIInterna.getReplayModeData().put("formsSeen", o);
		}

		m = (Map) o;
		String k = listener.getClass().getName();
		if (m.containsKey(k)) {
			formNum = (Integer) m.get(k);
		}

		if (formNum == null) {
			o = GUIInterna.getReplayModeData().get("formNum");
			formNum = (o == null) ? 1 : (Integer) o + 1;
			GUIInterna.getReplayModeData().put("formNum", formNum);
			m = (Map) GUIInterna.getReplayModeData().get("formsSeen");
			m.put(listener.getClass().getName(), formNum);
		}

		replayIdCount = 900000000L + formNum * 10000L;
		// max 10000 items can be in a form
		// max 10000 forms can appear in a load test run
	}

	// Map to replayableID real dispatchID
	private HashMap<Long, Long> replayIdMap = new HashMap<Long, Long>();

	/**
	 * Get the replayableID for a component, for use only in urlReplay mode. The replayableID is set the first time this method is called for a particular component.
	 * 
	 * @param component
	 * @return replayableID
	 */

	public long getReplayableDispatchID(Component comp) {
		Long oid = comp.getDispatchID();
		Long id = replayIdMap.get(oid);
		if (id != null) return id.longValue();
		id = new Long(++replayIdCount);

		if (id >= 999900000L) { throw new AssertException("Form count limit for a loadtest reached"); }
		if (id % 999990000L == 9999) { throw new AssertException("Form item countlimit for a form reached"); }

		replayIdMap.put(oid, id);
		if (comp instanceof FormBaseComponentImpl) {
			// set the replayID which was just determined because it is convenient
			((FormBaseComponentImpl) comp).setReplayableDispatchID(id);
		} // else it is a VelocityContainer
		return id.longValue();
	}
}
