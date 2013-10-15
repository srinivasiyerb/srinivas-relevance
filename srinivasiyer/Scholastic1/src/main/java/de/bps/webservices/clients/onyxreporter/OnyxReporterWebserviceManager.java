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
 * Copyright (c) 1999-2007 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */
package de.bps.webservices.clients.onyxreporter;

import java.io.File;
import java.io.FilenameFilter;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.olat.core.gui.UserRequest;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.Tracing;
import org.olat.core.util.WebappHelper;
import org.olat.course.nodes.AssessableCourseNode;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.iq.IQEditController;
import org.olat.fileresource.FileResourceManager;
import org.olat.repository.RepositoryEntry;

import de.bps.onyx.plugin.OnyxResultManager;
import de.bps.webservices.clients.onyxreporter.stubs.OnyxReporterStub;
import de.bps.webservices.clients.onyxreporter.stubs.OnyxReporterStub.ArmInputElement;
import de.bps.webservices.clients.onyxreporter.stubs.OnyxReporterStub.ArmSitePayload;
import de.bps.webservices.clients.onyxreporter.stubs.OnyxReporterStub.ContentPackage;
import de.bps.webservices.clients.onyxreporter.stubs.OnyxReporterStub.InputPartElement;
import de.bps.webservices.clients.onyxreporter.stubs.OnyxReporterStub.InputPartPayload;
import de.bps.webservices.clients.onyxreporter.stubs.OnyxReporterStub.OutputPartElement;
import de.bps.webservices.clients.onyxreporter.stubs.OnyxReporterStub.PostGetParams;
import de.bps.webservices.clients.onyxreporter.stubs.OnyxReporterStub.ResultValuesInput;
import de.bps.webservices.clients.onyxreporter.stubs.OnyxReporterStub.ResultValuesInputE;
import de.bps.webservices.clients.onyxreporter.stubs.OnyxReporterStub.ResultValuesResponse;
import de.bps.webservices.clients.onyxreporter.stubs.OnyxReporterStub.ResultVariableCandidatesInput;
import de.bps.webservices.clients.onyxreporter.stubs.OnyxReporterStub.ResultVariableCandidatesResponse;
import de.bps.webservices.clients.onyxreporter.stubs.OnyxReporterStub.Student;
import de.bps.webservices.clients.onyxreporter.stubs.OnyxReporterStub.UserDataPayload;

/**
 * Description:<br>
 * TODO: thomasw Class Description for OnyxReporterWebserviceManager
 * <P>
 * Initial Date: 28.08.2009 <br>
 * 
 * @author thomasw@bps-system.de
 */
public class OnyxReporterWebserviceManager {

	private OnyxReporterWebserviceClient client;

	private String target;

	private long assassmentId;

	/**
	 * The assasmentId is used to identify a special result xml.
	 * 
	 * @param assasmentId The assasmentId to set.
	 */
	public void setAssassmentId(final long assasmentId) {
		this.assassmentId = assasmentId;
	}

	/**
	 * Path to the folder in the olat data where the results to this survey are stored
	 */
	private String surveyFolderPath;

	public final static String STUDENTSVIEW_1 = "studentsview_1";

	public OnyxReporterWebserviceManager(final String target) throws Exception {
		this.target = target;
		client = new OnyxReporterWebserviceClient(target);
	}

	/**
	 * @return Returns the client.
	 */
	public OnyxReporterWebserviceClient getClient() {
		return client;
	}

	/**
	 * @param client The client to set.
	 */
	public void setClient(final OnyxReporterWebserviceClient client) {
		this.client = client;
	}

	/**
	 * @return Returns the target.
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * @param target The target to set.
	 */
	public void setTarget(final String target) {
		this.target = target;
	}

	public String startReporterForSurvey(final UserRequest ureq, final CourseNode node, final String resultsPath) throws RemoteException, OnyxReporterException {
		this.surveyFolderPath = resultsPath;
		return startReporter(ureq, null, node, false);
	}

	/**
	 * This method starts the OnyxReporter and returns the link to it.
	 * 
	 * @param ureq The UserRequest for getting the identity and role of the current user.
	 * @param students The students to show the results for.
	 * @param node The AssessableCourseNode to get the nodeId and to get the (OnyxTest) RepositoryEntry.
	 * @param studentview True if OnyxReporter shall display the detail view of the one given student, false if it shall display the overall results for all given
	 *            students.
	 * @return the Link to the reporter.
	 */
	public String startReporter(final UserRequest ureq, final List<Identity> students, final CourseNode node, final boolean studentview) throws RemoteException,
			OnyxReporterException {
		String link = "";
		final RepositoryEntry repositoryEntry = node.getReferencedRepositoryEntry();
		final OnyxReporterStub stub = client.getOnyxReporterStub();

		/** ARM SITE **/
		final String secret = "" + new Random().nextLong();
		final String sessionId = stub.armSite(getArmInputElement(ureq, secret)).getArmOutputElement().getUserSessionId();

		/** INITIATE SITE */
		final OutputPartElement outputPartElement = stub.initiateSite(getInputPartElement(repositoryEntry, secret, sessionId, students, node, ureq, null));

		link = outputPartElement.getOutputPartElement().getLink();
		final PostGetParams[] additionalParameters = outputPartElement.getOutputPartElement().getAdditionalParameters();

		// add params
		link = link + "?userId=" + sessionId + "&secret=" + secret;

		final StringBuffer paramString = new StringBuffer();
		if (additionalParameters != null) {
			for (int i = 0; i < additionalParameters.length; i++) {
				// exclude "view" param of unknown source because it conflicts with the view param added later
				if (!additionalParameters[i].getParamName().equals("view")) {
					paramString.append("&").append(additionalParameters[i].getParamName()).append("=").append(additionalParameters[i].getParamValue());
				}
			}
			link = link + paramString.toString();
		}

		// switch to the student view of a specified student
		if (studentview) {
			link += "&studentId=" + students.get(0).getKey() + "&view=" + STUDENTSVIEW_1;
		}

		return link;
	}

	public List<String[]> getResults(final File resultXml, final AssessableCourseNode node) throws RemoteException {
		return getResults(resultXml, node, null);
	}

	public List<String[]> getResults(final AssessableCourseNode node, final Identity identity) throws RemoteException {
		return getResults(null, node, identity);
	}

	public List<String[]> getResults(final File resultXml, final AssessableCourseNode node, final Identity identity) throws RemoteException {
		final RepositoryEntry repositoryEntry = node.getReferencedRepositoryEntry();
		final OnyxReporterStub stub = client.getOnyxReporterStub();

		/** ARM SITE **/
		final String secret = "" + new Random().nextLong();
		final String sessionId = stub.armSite(getArmInputElement(null, secret)).getArmOutputElement().getUserSessionId();

		List<Identity> student = null;
		if (identity != null) {
			student = new ArrayList<Identity>();
			student.add(identity);
		}

		/** INITIATE SITE **/
		try {
			final OutputPartElement outputPartElement = stub.initiateSite(getInputPartElement(repositoryEntry, secret, sessionId, student, node, null, resultXml));
		} catch (final OnyxReporterException e) {
			Tracing.createLoggerFor(this.getClass()).error("Unable to get onyx results for reporter. ", e);
		}

		final ResultValuesInputE resultValuesInputE = new ResultValuesInputE();

		final UserDataPayload userDataPayload = new UserDataPayload();
		userDataPayload.setSharedSecret(secret);
		userDataPayload.setUserSessionId(sessionId);

		final ResultValuesInput resultValuesInput = new ResultValuesInput();
		resultValuesInput.setUserData(userDataPayload);
		resultValuesInputE.setResultValuesInput(resultValuesInput);

		final ResultValuesResponse response = stub.resultValues(resultValuesInputE);
		final PostGetParams[] params = response.getResultValuesResponse().getReturnValues();
		final List<String[]> result = new ArrayList<String[]>();
		for (final PostGetParams param : params) {
			final String[] keyValue = new String[2];
			keyValue[0] = param.getParamName();
			keyValue[1] = param.getParamValue();
			result.add(keyValue);
		}
		return result;
	}

	/**
	 * Delivers a map with all possible outcome-variables of this onyx test.
	 * 
	 * @param node The course node with the Onyx test.
	 * @return A map with all outcome-variables with name as key and type as value.
	 */
	public Map<String, String> getOutcomes(final AssessableCourseNode node) throws RemoteException {
		final Map<String, String> outcomes = new HashMap<String, String>();
		final OnyxReporterStub stub = client.getOnyxReporterStub();

		final File onyxTestZip = getCP(node.getReferencedRepositoryEntry());

		// Get outcomes
		final ContentPackage cp = new ContentPackage();
		cp.setContentPackage(new DataHandler(new FileDataSource(onyxTestZip)));
		final ResultVariableCandidatesInput resultVariableCandidatesInput = new ResultVariableCandidatesInput();
		resultVariableCandidatesInput.setResultVariableCandidatesInput(cp);
		final ResultVariableCandidatesResponse response = stub.resultVariableCandidates(resultVariableCandidatesInput);
		final PostGetParams[] params = response.getResultVariableCandidatesResponse().getReturnValues();

		for (final PostGetParams param : params) {
			outcomes.put(param.getParamName(), param.getParamValue());
		}

		return outcomes;
	}

	/**
	 * Gets the result xml file from the file system.
	 * 
	 * @param username
	 * @param assessmentType
	 * @param nodeId
	 * @return
	 */
	private File getResultXml(final String username, final String assessmentType, final String nodeId) {
		File xml = null;
		String filename;
		final File fUserdataRoot = new File(WebappHelper.getUserDataRoot());
		final String path = OnyxResultManager.getResReporting() + File.separator + username + File.separator + assessmentType + File.separator;
		// if an assassment id was given, use the corresponding file
		if (assassmentId != 0) {
			filename = nodeId + "v" + assassmentId + ".xml";
			xml = new File(fUserdataRoot, path + filename);
		}
		// otherwise search the newest result file with this node id in this directory
		if (xml == null || !(xml.exists())) {
			final File directory = new File(fUserdataRoot, path);
			final String[] allXmls = directory.list(new myFilenameFilter(nodeId));
			if (allXmls != null && allXmls.length > 0) {
				File newestXml = new File(fUserdataRoot, path + allXmls[0]);
				for (final String xmlFileName : allXmls) {
					final File xmlFile = new File(fUserdataRoot, path + xmlFileName);
					if (xmlFile.lastModified() > newestXml.lastModified()) {
						newestXml = xmlFile;
					}
				}
				xml = newestXml;
			}
		}

		return xml;
	}

	/**
	 * Generates the ArmInputELement.
	 * 
	 * @param ureq
	 * @param secret
	 * @return
	 */
	private ArmInputElement getArmInputElement(final UserRequest ureq, final String secret) {
		final ArmInputElement armInputElement = new ArmInputElement();

		final ArmSitePayload armSitePayload = new ArmSitePayload();
		armSitePayload.setSecretToShare(secret);

		if (ureq != null) {
			final Identity user = ureq.getIdentity();
			armSitePayload.setUserFirstName(user.getUser().getProperty(UserConstants.FIRSTNAME, ureq.getLocale()));
			armSitePayload.setUserLastName(user.getUser().getProperty(UserConstants.LASTNAME, ureq.getLocale()));
			armSitePayload.setUserId("" + user.getKey());
			final String role = ureq.getUserSession().getRoles().isAuthor() ? "author" : "student";
			armSitePayload.setRole(role);
		} else {
			armSitePayload.setUserId("no id");
		}

		armInputElement.setArmInputElement(armSitePayload);
		return armInputElement;
	}

	/**
	 * Generates the InputPartElement.
	 * 
	 * @param repositoryEntry
	 * @param secret
	 * @param sessionId
	 * @param students
	 * @param node
	 * @param ureq
	 * @return
	 */
	private InputPartElement getInputPartElement(final RepositoryEntry repositoryEntry, final String secret, final String sessionId, final List<Identity> students,
			final CourseNode node, final UserRequest ureq, final File resultXml) throws OnyxReporterException {
		final InputPartElement inputPartElement = new InputPartElement();
		final InputPartPayload inputPartPayload = new InputPartPayload();

		final File onyxTestZip = getCP(repositoryEntry);

		inputPartPayload.setContentPackage(new DataHandler(new FileDataSource(onyxTestZip)));

		final UserDataPayload userDataPayload = new UserDataPayload();
		userDataPayload.setSharedSecret(secret);
		userDataPayload.setUserSessionId(sessionId);
		inputPartPayload.setUserData(userDataPayload);

		List<Student> serviceStudents = new ArrayList<Student>();

		if (students != null) {
			for (int i = 0; i < students.size(); i++) {
				final Student serviceStudent = new Student();
				serviceStudent.setStudentId("" + students.get(i).getKey());
				if (ureq != null) {
					serviceStudent.setStudentFirstName(students.get(i).getUser().getProperty(UserConstants.FIRSTNAME, ureq.getLocale()));
					serviceStudent.setStudentSureName(students.get(i).getUser().getProperty(UserConstants.LASTNAME, ureq.getLocale()));
				}
				final String assessmentType = node.getModuleConfiguration().get(IQEditController.CONFIG_KEY_TYPE).toString();
				final File resultFile = getResultXml(students.get(i).getName(), assessmentType, "" + node.getIdent());
				if (resultFile != null && resultFile.exists()) {
					serviceStudent.setResultFile(new DataHandler(new FileDataSource(resultFile)));
					serviceStudents.add(serviceStudent);
				}
			}
		} else {
			if (resultXml != null && resultXml.exists()) {
				// if a result file was given, rather then a List of students, construct a
				// "dummy" student with the result xml to get the results
				final Student serviceStudent = new Student();
				serviceStudent.setStudentId("dummyId");
				serviceStudent.setResultFile(new DataHandler(new FileDataSource(resultXml)));
				serviceStudents.add(serviceStudent);
			} else {
				// no students and no result file are given -> this must be a survey
				if (this.surveyFolderPath != null) {
					serviceStudents = getAnonymizedStudentsWithResultsForSurvey(node.getIdent());
				}
			}
		}

		if (serviceStudents.size() == 0) { throw new OnyxReporterException("noresults"); }
		inputPartPayload.setStudents(serviceStudents.toArray(new Student[0]));

		inputPartElement.setInputPartElement(inputPartPayload);

		return inputPartElement;
	}

	/**
	 * Generates a file object for the given re.
	 * 
	 * @param repositoryEntry
	 * @return
	 */
	private File getCP(final RepositoryEntry repositoryEntry) {
		// get content-package (= onyx test zip-file)
		final OLATResourceable fileResource = repositoryEntry.getOlatResource();
		final String unzipedDir = FileResourceManager.getInstance().unzipFileResource(fileResource).getAbsolutePath();
		final String zipdirName = FileResourceManager.ZIPDIR;
		final String testName = repositoryEntry.getResourcename();
		final String pathToFile = unzipedDir.substring(0, unzipedDir.indexOf(zipdirName));
		final File onyxTestZip = new File(pathToFile + testName);
		return onyxTestZip;
	}

	/**
	 * For every result xml file found in the survey folder a dummy student is created.
	 * 
	 * @param nodeId
	 * @return
	 */
	private List<Student> getAnonymizedStudentsWithResultsForSurvey(final String nodeId) {
		final List<Student> serviceStudents = new ArrayList<Student>();

		final File directory = new File(this.surveyFolderPath);
		if (directory.exists()) {
			final String[] allXmls = directory.list(new myFilenameFilter(nodeId));
			if (allXmls != null && allXmls.length > 0) {
				int id = 0;
				for (final String xmlFileName : allXmls) {
					final File xmlFile = new File(this.surveyFolderPath + xmlFileName);
					final Student serviceStudent = new Student();
					serviceStudent.setResultFile(new DataHandler(new FileDataSource(xmlFile)));
					serviceStudent.setStudentId("st" + id);
					serviceStudents.add(serviceStudent);
					id++;
				}
			}
		}
		return serviceStudents;
	}

	/**
	 * Description:<br>
	 * Filters the filenames of the "File.list()" method so that only files witch passes the method "accept" are returned.
	 * <P>
	 * Initial Date: 25.09.2009 <br>
	 * 
	 * @author thomasw@bps-system.de
	 */
	private class myFilenameFilter implements FilenameFilter {

		private final String nodeId;

		public myFilenameFilter(final String nodeId) {
			this.nodeId = nodeId;
		}

		/**
		 * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
		 */
		@Override
		public boolean accept(final File diretory, final String name) {
			if (name.startsWith(nodeId)) {
				return true;
			} else {
				return false;
			}
		}
	}
}
