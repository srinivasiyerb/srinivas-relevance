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
package org.olat.search.ui;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.services.search.AbstractOlatDocument;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.DateChooser;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.Translator;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.ArrayHelper;
import org.olat.core.util.StringHelper;
import org.olat.search.service.SearchMetadataFieldsProvider;
import org.olat.search.service.document.ContextHelpDocument;
import org.olat.search.service.document.file.ExcelDocument;
import org.olat.search.service.document.file.HtmlDocument;
import org.olat.search.service.document.file.OpenDocument;
import org.olat.search.service.document.file.PdfDocument;
import org.olat.search.service.document.file.PowerPointDocument;
import org.olat.search.service.document.file.TextDocument;
import org.olat.search.service.document.file.UnkownDocument;
import org.olat.search.service.document.file.WordDocument;

/**
 * Description:<br>
 * Controller for the advanced search
 * <P>
 * Initial Date: 4 dec. 2009 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com
 */
public class AdvancedSearchInputController extends FormBasicController {
	private static final OLog log = Tracing.createLoggerFor(AdvancedSearchInputController.class);
	private final DateFormat format = new SimpleDateFormat("yyyyMMdd");

	private static final String HTML_TYPES = HtmlDocument.FILE_TYPE;
	private static final String WORD_TYPES = WordDocument.FILE_TYPE;
	private static final String CALC_TYPES = ExcelDocument.FILE_TYPE + " " + OpenDocument.SPEADSHEET_FILE_TYPE + " " + OpenDocument.FORMULA_FILE_TYPE;
	private static final String PRESENTATION_TYPES = PowerPointDocument.FILE_TYPE + " " + OpenDocument.PRESENTATION_FILE_TYPE;
	private static final String PDF_TYPES = PdfDocument.FILE_TYPE;
	private static final String WIKI_TYPES = "type.*.wiki";
	private static final String FORUM_TYPES = "type.*.forum.message";
	private static final String COURSE_TYPES = "type.course.node*";
	private static final String BLOG_PODCAST_TYPES = "type.*.podcast type.*.blog type.repository.entry.*.PODCAST type.repository.entry.*.BLOG";
	private static final String GROUP_TYPES = "type.group";
	private static final String USER_TYPES = "type.identity";
	private static final String PORTFOLIO_TYPES = "type.db.EP*Map* type.group.EP*Map*";
	private static final String OTHER_TYPES = TextDocument.FILE_TYPE + " " + OpenDocument.FORMULA_FILE_TYPE + " " + OpenDocument.GRAPHIC_FILE_TYPE + " "
			+ ContextHelpDocument.TYPE + " " + UnkownDocument.UNKOWN_TYPE + " " + OpenDocument.TEXT_FILE_TYPE;

	private final List<DocumentInfo> documentInfos = new ArrayList<DocumentInfo>();

	private FormLink searchButton;
	private TextElement searchInput;
	private TextElement authorQuery;
	private TextElement titleQuery;
	private TextElement descriptionQuery;
	private DateChooser createdDate;
	private DateChooser modifiedDate;
	private TextElement metadataQuery;
	private SingleSelection metadataType;
	private SingleSelection contextSelection;
	private MultipleSelectionElement documentTypeQuery;

	private boolean resourceContextEnable = true;
	private final Set<String> selectedDocumentTypes = new HashSet<String>();

	public AdvancedSearchInputController(final UserRequest ureq, final WindowControl wControl, final Form mainForm) {
		super(ureq, wControl, -1, null, mainForm);
		initForm(ureq);
	}

	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
		searchInput = uifactory.addTextElement("search_input", "search.title", 255, "", formLayout);
		authorQuery = uifactory.addTextElement("search_author", "form.search.label.author", 255, "", formLayout);
		titleQuery = uifactory.addTextElement("search_title", "form.search.label.title", 255, "", formLayout);
		descriptionQuery = uifactory.addTextElement("search_description", "form.search.label.description", 255, "", formLayout);
		createdDate = uifactory.addDateChooser("search_creation", "form.search.label.created.date", "", formLayout);
		modifiedDate = uifactory.addDateChooser("search_modification", "form.search.label.modified.date", "", formLayout);

		// document types
		initDocumentTypesKeysAndValues();
		final String[] documentTypeKeys = new String[documentInfos.size()];
		final String[] documentTypeValues = new String[documentInfos.size()];

		int j = 0;
		for (final DocumentInfo documentType : documentInfos) {
			documentTypeKeys[j] = documentType.getKey();
			documentTypeValues[j++] = documentType.getValue();
		}
		documentTypeQuery = uifactory.addCheckboxesHorizontal("doc_type", "form.search.label.documenttype", formLayout, documentTypeKeys, documentTypeValues, null);

		// metadatas
		final SearchMetadataFieldsProvider metadataProvider = (SearchMetadataFieldsProvider) CoreSpringFactory.getBean("SearchMetadataFieldsProvider");
		// The metadata key selection, e.g DC.language for doublin core language metadata
		final List<String> metaDataList = metadataProvider.getAdvancedSearchableFields();
		if (metaDataList.size() > 0) {
			final String[] metaDataFields = ArrayHelper.toArray(metaDataList);
			final String[] metaDataFieldsTranslated = new String[metaDataFields.length];
			final Translator metaTranslator = metadataProvider.createFieldsTranslator(getLocale());
			for (int i = 0; i < metaDataFields.length; i++) {
				final String key = metaDataFields[i];
				metaDataFieldsTranslated[i] = key + " (" + metaTranslator.translate(key) + ")";
			}
			metadataType = uifactory.addDropdownSingleselect("metadata_type", "form.search.label.metadatatype", formLayout, metaDataFields, metaDataFieldsTranslated,
					null);
			metadataQuery = uifactory.addTextElement("metadata_query", null, 255, "", formLayout);
		}

		contextSelection = uifactory.addRadiosHorizontal("context", "form.search.label.context", formLayout, new String[0], new String[0]);
		searchButton = uifactory.addFormLink("search", formLayout, Link.BUTTON_SMALL);
		searchButton.setEnabled(true);
	}

	public boolean isResourceContextEnable() {
		return resourceContextEnable;
	}

	public void setResourceContextEnable(final boolean resourceContextEnable) {
		if (contextSelection.isVisible() != resourceContextEnable) {
			contextSelection.setVisible(resourceContextEnable);
		}
		this.resourceContextEnable = resourceContextEnable;
	}

	/**
	 * Initialize drop-down value and key array.
	 */
	private void initDocumentTypesKeysAndValues() {
		documentInfos.clear();
		documentInfos.add(new DocumentInfo("html", translate("type.file.html"), null, HTML_TYPES));
		documentInfos.add(new DocumentInfo("word", translate("type.file.word"), null, WORD_TYPES));
		documentInfos.add(new DocumentInfo("table", translate("type.file.table"), null, CALC_TYPES));
		documentInfos.add(new DocumentInfo("powerpoint", translate("type.file.presentation"), null, PRESENTATION_TYPES));
		documentInfos.add(new DocumentInfo("pdf", translate("type.file.pdf"), null, PDF_TYPES));
		documentInfos.add(new DocumentInfo("wiki", translate("area.wikis"), WIKI_TYPES, null));
		documentInfos.add(new DocumentInfo("forum", translate("area.forums"), FORUM_TYPES, null));
		documentInfos.add(new DocumentInfo("course", translate("area.courses"), COURSE_TYPES, null));
		documentInfos.add(new DocumentInfo("blog", translate("area.blogs"), BLOG_PODCAST_TYPES, null));
		documentInfos.add(new DocumentInfo("group", translate("area.groups"), GROUP_TYPES, null));
		documentInfos.add(new DocumentInfo("user", translate("area.users"), USER_TYPES, null));
		documentInfos.add(new DocumentInfo("portfolio", translate("area.portfolio"), PORTFOLIO_TYPES, null));
		documentInfos.add(new DocumentInfo("others", translate("type.file.others"), null, OTHER_TYPES));
	}

	public String getSearchString() {
		return searchInput.getValue();
	}

	public void setSearchString(final String searchString) {
		searchInput.setValue(searchString);
	}

	public String getContext() {
		if (contextSelection.isOneSelected()) { return contextSelection.getSelectedKey(); }
		return null;
	}

	public void setContextKeysAndValues(final String[] keys, final String[] values) {
		contextSelection.setKeysAndValues(keys, values, null);
		if (keys.length > 0) {
			contextSelection.select(keys[keys.length - 1], true);
		}
	}

	public void load() {
		if (!selectedDocumentTypes.isEmpty()) {
			for (final String selected : selectedDocumentTypes) {
				documentTypeQuery.select(selected, true);
			}
		}
	}

	public void unload() {
		selectedDocumentTypes.clear();
		selectedDocumentTypes.addAll(documentTypeQuery.getSelectedKeys());
	}

	public List<String> getQueryStrings() {
		final List<String> queries = new ArrayList<String>();

		if (StringHelper.containsNonWhitespace(authorQuery.getValue())) {
			appendAnd(queries, AbstractOlatDocument.AUTHOR_FIELD_NAME, ":(", authorQuery.getValue(), ") ");
		}
		if (!documentTypeQuery.getSelectedKeys().isEmpty()) {
			buildDocumentTypeQuery(queries);
		}
		if (StringHelper.containsNonWhitespace(titleQuery.getValue())) {
			appendAnd(queries, AbstractOlatDocument.TITLE_FIELD_NAME, ":(", titleQuery.getValue(), ") ");
		}
		if (StringHelper.containsNonWhitespace(descriptionQuery.getValue())) {
			appendAnd(queries, AbstractOlatDocument.DESCRIPTION_FIELD_NAME, ":(", descriptionQuery.getValue(), ") ");
		}
		if (StringHelper.containsNonWhitespace(createdDate.getValue())) {
			appendAnd(queries, AbstractOlatDocument.CREATED_FIELD_NAME, ":(", format.format(createdDate.getDate()), ") ");
		}
		if (StringHelper.containsNonWhitespace(modifiedDate.getValue())) {
			appendAnd(queries, AbstractOlatDocument.CHANGED_FIELD_NAME, ":(", format.format(modifiedDate.getDate()), ") ");
		}
		// Check for null on metadata element since it might not be configured and initialized
		if (metadataQuery != null && StringHelper.containsNonWhitespace(metadataQuery.getValue())) {
			appendAnd(queries, metadataType.getSelectedKey(), ":(", metadataQuery.getValue(), ") ");
		}
		if (log.isDebug()) {
			log.debug("Advanced query=" + queries);
		}
		return queries;
	}

	/**
	 * Append 'AND' operation if buf is not empty.
	 * 
	 * @param buf
	 */
	private void appendAnd(final List<String> queries, final String... strings) {
		final StringBuilder query = new StringBuilder();
		for (final String string : strings) {
			query.append(string);
		}

		if (query.length() > 0) {
			queries.add(query.toString());
		}
	}

	public boolean isDocumentTypesSelected() {
		return documentTypeQuery.isMultiselect();
	}

	private void buildDocumentTypeQuery(final List<String> queries) {
		final Set<String> selectDocTypes = documentTypeQuery.getSelectedKeys();
		if (selectDocTypes.size() == documentInfos.size() || selectDocTypes.isEmpty()) {
			// all selected -> no constraints of the type
			return;
		}

		final List<String> docTypes = new ArrayList<String>();
		final List<String> fTypes = new ArrayList<String>();
		for (final String selectedocType : selectDocTypes) {
			for (final DocumentInfo info : documentInfos) {
				if (selectedocType.equals(info.getKey())) {
					if (info.hasDocumentType()) {
						docTypes.add(info.getDocumentType());
					}
					if (info.hasFileType()) {
						fTypes.add(info.getFileType());
					}
				}
			}
		}

		final StringBuilder buf = new StringBuilder();
		buf.append('(');
		if (!docTypes.isEmpty()) {
			buf.append(AbstractOlatDocument.DOCUMENTTYPE_FIELD_NAME);
			buf.append(":(");
			for (final String docType : docTypes) {
				buf.append(docType).append(' ');
			}
			buf.append(") ");
		}

		if (!fTypes.isEmpty()) {
			if (!docTypes.isEmpty()) {
				buf.append(' ');// don't need OR
			}

			buf.append(AbstractOlatDocument.FILETYPE_FIELD_NAME);
			buf.append(":(");
			for (final String fileType : fTypes) {
				buf.append(fileType).append(' ');
			}
			buf.append(")");
		}
		buf.append(") ");

		if (buf.length() > 4) {
			queries.add(buf.toString());
		}
	}

	public void getSearchProperties(final Properties props) {
		setSearchProperty(props, "aq", authorQuery.getValue());
		setSearchProperty(props, "tq", titleQuery.getValue());
		setSearchProperty(props, "dq", descriptionQuery.getValue());
		setSearchProperty(props, "cd", createdDate.getValue());
		setSearchProperty(props, "md", modifiedDate.getValue());
		setSearchProperty(props, "mtdq", metadataQuery.getValue());

		if (metadataType.isOneSelected()) {
			props.setProperty("mtdt", metadataType.getSelectedKey());
		} else {
			props.remove("mtdt");
		}

		final Set<String> selectedKeys = documentTypeQuery.getSelectedKeys();
		final StringBuilder sb = new StringBuilder();
		for (final String selectedKey : selectedKeys) {
			sb.append(selectedKey).append('|');
		}
		props.setProperty("dtypes", sb.toString());
	}

	private void setSearchProperty(final Properties props, final String key, final String value) {
		if (StringHelper.containsNonWhitespace(value)) {
			props.setProperty(key, value);
		} else {
			props.remove(key);
		}
	}

	public void setSearchProperties(final Properties props) {
		final String aq = props.getProperty("aq");
		if (StringHelper.containsNonWhitespace(aq)) {
			authorQuery.setValue(aq);
		}

		final String tq = props.getProperty("tq");
		if (StringHelper.containsNonWhitespace(tq)) {
			titleQuery.setValue(tq);
		}

		final String dq = props.getProperty("dq");
		if (StringHelper.containsNonWhitespace(aq)) {
			descriptionQuery.setValue(dq);
		}

		final String cd = props.getProperty("cd");
		if (StringHelper.containsNonWhitespace(cd)) {
			createdDate.setValue(cd);
		}

		final String md = props.getProperty("md");
		if (StringHelper.containsNonWhitespace(md)) {
			modifiedDate.setValue(md);
		}

		final String mtdq = props.getProperty("mtdq");
		if (StringHelper.containsNonWhitespace(mtdq)) {
			metadataQuery.setValue(mtdq);
		}

		final String mtdt = props.getProperty("mtdt");
		if (StringHelper.containsNonWhitespace(mtdt)) {
			metadataType.select(mtdt, true);
		}

		final String dtypes = props.getProperty("dtypes");
		if (StringHelper.containsNonWhitespace(dtypes)) {
			selectedDocumentTypes.clear();
			for (final DocumentInfo documentInfo : documentInfos) {
				final boolean selected = dtypes.indexOf(documentInfo.getKey()) >= 0;
				documentTypeQuery.select(documentInfo.getKey(), selected);
				if (selected) {
					selectedDocumentTypes.add(documentInfo.getKey());
				}
			}
		}
	}

	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void formOK(final UserRequest ureq) {
		//
	}

	public FormLink getSearchButton() {
		return searchButton;
	}

	public FormItem getFormItem() {
		return flc;
	}

	public class DocumentInfo {
		public String key;
		public String value;
		public String fileType;
		public String documentType;

		public DocumentInfo(final String key, final String value, final String docType, final String fType) {
			this.key = key;
			this.value = value;
			this.fileType = fType;
			this.documentType = docType;
		}

		public String getKey() {
			return key;
		}

		public String getValue() {
			return value;
		}

		public boolean hasDocumentType() {
			return StringHelper.containsNonWhitespace(documentType);
		}

		public String getDocumentType() {
			return documentType;
		}

		public boolean hasFileType() {
			return StringHelper.containsNonWhitespace(fileType);
		}

		public String getFileType() {
			return fileType;
		}
	}
}