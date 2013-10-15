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

package org.olat.fileresource;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.vfs.LocalFileImpl;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.fileresource.types.ImsCPFileResource;
import org.olat.fileresource.types.ScormCPFileResource;
import org.olat.ims.qti.fileresource.SurveyFileResource;
import org.olat.ims.qti.fileresource.TestFileResource;
import org.olat.ims.qti.process.QTIHelper;

/**
 * Initial Date: Apr 19, 2004
 * 
 * @author Mike Stock
 */
public class FileDetailsForm extends FormBasicController {

	private final OLATResourceable res;
	private final File file;

	/**
	 * Create details form with values from resourceable res.
	 * 
	 * @param name
	 * @param locale
	 * @param res
	 */
	public FileDetailsForm(final UserRequest ureq, final WindowControl wControl, final OLATResourceable res) {
		super(ureq, wControl);
		this.res = res;
		file = FileResourceManager.getInstance().getFileResource(res);
		initForm(ureq);
	}

	@Override
	protected void formOK(final UserRequest ureq) {
		//
	}

	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
		uifactory.addStaticTextElement("size", "fr.size", new Long(file.length() / 1024).toString() + " KB", formLayout);
		final DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, getLocale());
		uifactory.addStaticTextElement("last", "fr.last", df.format(new Date(file.lastModified())), formLayout);

		final String resType = res.getResourceableTypeName();
		if (resType.equals(TestFileResource.TYPE_NAME) || resType.equals(SurveyFileResource.TYPE_NAME)) {

			final FileResourceManager frm = FileResourceManager.getInstance();
			final File unzippedRoot = frm.unzipFileResource(res);

			// with VFS FIXME:pb:c: remove casts to LocalFileImpl and LocalFolderImpl if no longer needed.
			final VFSContainer vfsUnzippedRoot = new LocalFolderImpl(unzippedRoot);
			final VFSItem vfsQTI = vfsUnzippedRoot.resolve("qti.xml");
			// getDocument(..) ensures that InputStream is closed in every case.
			final Document doc = QTIHelper.getDocument((LocalFileImpl) vfsQTI);
			// if doc is null an error loading the document occured (IOException, qti.xml does not exist)
			if (doc != null) {

				// extract title
				final Element el_assess = (Element) doc.selectSingleNode("questestinterop/assessment");
				final String title = el_assess.attributeValue("title");
				uifactory.addStaticTextElement("title", "qti.title", title == null ? "-" : title, formLayout);

				// extract objectives
				// HTMLTextAreaElement htmlTA = new HTMLTextAreaElement("qti.objectives", 10, 60);
				// htmlTA.setReadOnly(true);

				String obj = "-";
				final Element el_objectives = (Element) doc.selectSingleNode("//questestinterop/assessment/objectives");
				if (el_objectives != null) {
					final Element el_mat = (Element) el_objectives.selectSingleNode("material/mattext");
					if (el_mat != null) {
						obj = el_mat.getTextTrim();
					}
				}
				uifactory.addStaticTextElement("obj", "qti.objectives", obj, formLayout);

				// extract num of questions
				final List items = doc.selectNodes("//item");
				uifactory.addStaticTextElement("qti.questions", "qti.questions", items.size() > 0 ? "" + items.size() : "-", formLayout);

				// extract time limit
				String tl = "-";
				final Element el_duration = (Element) el_assess.selectSingleNode("duration");
				if (el_duration != null) {
					final long dur = QTIHelper.parseISODuration(el_duration.getTextTrim());
					final long min = dur / 1024 / 60;
					final long sec = (dur - (min * 60 * 1024)) / 1024;
					tl = "" + min + "' " + sec + "''";
				}
				uifactory.addStaticTextElement("qti.timelimit", "qti.timelimit", tl, formLayout);
			}
		} else if (resType.equals(ImsCPFileResource.TYPE_NAME) || resType.equals(ScormCPFileResource.TYPE_NAME)) {
			//
		}

		flc.setEnabled(false);
	}

	@Override
	protected void doDispose() {
		//
	}

}
