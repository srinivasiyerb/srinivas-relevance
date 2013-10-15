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

package org.olat.ims.qti.export;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.util.Formatter;
import org.olat.core.util.filter.FilterFactory;
import org.olat.ims.qti.editor.beecom.parser.ItemParser;
import org.olat.ims.qti.export.helper.QTIItemObject;

/**
 * Initial Date: May 23, 2006 <br>
 * 
 * @author Alexander Schneider
 */
public class QTIExportFormatterCSVType1 extends QTIExportFormatter {
	private final String fileNamePrefix = "TEST_";
	private final int type = 1;
	// Delimiters and file name suffix for the export file
	private final String sep; // fields separated by
	private final String emb; // fields embedded by
	private final String esc; // fields escaped by
	private final String car; // carriage return
	// Author can export the mattext without HTML tags
	// especially used for the results export of matrix questions created by QANT
	private final boolean tagless;
	private int row_counter = 1;

	// CELFI#107 (Header question max lenght)
	private final int cut = 30;

	/**
	 * @param locale
	 * @param type
	 * @param anonymizerCallback
	 * @param delimiter
	 * @param Map qtiExportFormatConfig with (QTIExportItemXYZ.class,IQTIExportItemFormatConfig)
	 */

	public QTIExportFormatterCSVType1(final Locale locale, final String sep, final String emb, final String esc, final String car, final boolean tagless) {
		super(locale, null);
		this.sep = sep;
		this.emb = emb;
		this.esc = esc;
		this.car = car;
		this.tagless = tagless;
	}

	@Override
	public void openReport() {
		if (qtiItemObjectList == null) { throw new OLATRuntimeException(null, "Can not format report when qtiItemObjectList is null", null); }

		if (mapWithExportItemConfigs == null) {
			// while deleting a test node the formatter has no config consiting of user input
			setDefaultQTIItemConfigs();
		}
		final QTIExportItemFactory qeif = new QTIExportItemFactory(mapWithExportItemConfigs);

		// // // Preparing HeaderRow 1 and HeaderRow2
		final StringBuilder hR1 = new StringBuilder();
		final StringBuilder hR2 = new StringBuilder();

		int i = 1;
		for (final Iterator iter = qtiItemObjectList.iterator(); iter.hasNext();) {
			final QTIItemObject item = (QTIItemObject) iter.next();

			if (displayItem(qeif.getExportItemConfig(item))) {
				hR1.append(emb);
				hR1.append(escape(item.getItemTitle()));

				// CELFI#107
				String question = item.getQuestionText();
				// question = FilterFactory.getHtmlTagsFilter().filter(question);
				question = FilterFactory.getXSSFilter(-1).filter(question);
				question = FilterFactory.getHtmlTagsFilter().filter(question);

				if (question.length() > cut) {
					question = question.substring(0, cut) + "...";
				}
				question = StringEscapeUtils.unescapeHtml(question);
				hR1.append(": " + escape(question));
				// CELFI#107 END

				hR1.append(emb);

				if (qeif.getExportItemConfig(item).hasResponseCols()) {
					final List responseColumnHeaders = item.getResponseColumnHeaders();
					for (final Iterator iterator = responseColumnHeaders.iterator(); iterator.hasNext();) {
						// HeaderRow1
						hR1.append(sep);
						// HeaderRow2
						final String columnHeader = (String) iterator.next();
						hR2.append(i);
						hR2.append("_");
						hR2.append(columnHeader);
						hR2.append(sep);
					}
				}

				if (qeif.getExportItemConfig(item).hasPositionsOfResponsesCol()) {
					// HeaderRow1
					hR1.append(sep);
					// HeaderRow2
					hR2.append(i);
					hR2.append("_");
					hR2.append(translator.translate("item.positions"));
					hR2.append(sep);
				}

				if (qeif.getExportItemConfig(item).hasPointCol()) {
					// HeaderRow1
					hR1.append(sep);
					// HeaderRow2
					hR2.append(i);
					hR2.append("_");
					hR2.append(translator.translate("item.score"));
					hR2.append(sep);
				}

				if (qeif.getExportItemConfig(item).hasTimeCols()) {
					// HeaderRow1
					hR1.append(sep + sep);
					// HeaderRow2
					hR2.append(i);
					hR2.append("_");
					hR2.append(translator.translate("item.start"));
					hR2.append(sep);

					hR2.append(i);
					hR2.append("_");
					hR2.append(translator.translate("item.duration"));
					hR2.append(sep);
				}
				i++;
			}
		}
		// // // HeaderRow1Intro
		sb.append(createHeaderRow1Intro());

		// // // HeaderRow1
		sb.append(hR1.toString());
		sb.append(car);

		// // // HeaderRow2Intro
		sb.append(createHeaderRow2Intro());

		// // // HeaderRow2
		sb.append(hR2.toString());
		sb.append(car);

	}

	@Override
	public void openResultSet(final QTIExportSet set) {

		final String firstName = set.getFirstName();
		final String lastName = set.getLastName();
		final String login = set.getLogin();
		String instUsrIdent = set.getInstitutionalUserIdentifier();
		if (instUsrIdent == null) {
			instUsrIdent = translator.translate("column.field.notavailable");
		}
		final float assessPoints = set.getScore();
		final boolean isPassed = set.getIsPassed();

		sb.append(row_counter);
		sb.append(sep);
		sb.append(lastName);
		sb.append(sep);
		sb.append(firstName);
		sb.append(sep);
		sb.append(login);
		sb.append(sep);
		sb.append(instUsrIdent);
		sb.append(sep);
		sb.append(assessPoints);
		sb.append(sep);
		sb.append(isPassed);
		sb.append(sep);
		sb.append(set.getIp());
		sb.append(sep);

		// datatime
		final Date date = set.getLastModified();
		sb.append(Formatter.formatDatetime(date));
		sb.append(sep);

		final Long assessDuration = set.getDuration();
		// since there are resultsets created before alter table adding the field duration
		if (assessDuration != null) {
			sb.append(Math.round(assessDuration.longValue() / 1000));
		} else {
			sb.append("n/a");
		}
		sb.append(sep);

	}

	@Override
	public void visit(final QTIExportItem eItem) {
		final List responseColumns = eItem.getResponseColumns();
		final QTIExportItemFormatConfig itemFormatConfig = eItem.getConfig();

		if (displayItem(itemFormatConfig)) {

			if (itemFormatConfig.hasResponseCols()) {
				for (final Iterator iter = responseColumns.iterator(); iter.hasNext();) {
					final String responseColumn = (String) iter.next();
					sb.append(emb);
					sb.append(escape(responseColumn));
					sb.append(emb);
					sb.append(sep);
				}
			}

			if (itemFormatConfig.hasPositionsOfResponsesCol()) {
				sb.append(eItem.getPositionsOfResponses());
				sb.append(sep);
			}

			if (eItem.hasResult()) {
				if (itemFormatConfig.hasPointCol()) {
					// points
					sb.append(eItem.getScore());
					sb.append(sep);
				}
				if (itemFormatConfig.hasTimeCols()) {
					// startdatetime
					if (eItem.getTimeStamp().getTime() > 0) {
						sb.append(Formatter.formatDatetime(eItem.getTimeStamp()));
					} else {
						sb.append("n/a");
					}
					sb.append(sep);

					// column duration
					final Long itemDuration = eItem.getDuration();

					if (itemDuration != null) {
						sb.append(itemDuration.longValue() / 1000);
					} else {
						sb.append("n/a");
					}
					sb.append(sep);
				}
			} else {
				// points
				if (itemFormatConfig.hasPointCol()) {
					sb.append(sep);
				}
				// startdatetime, column duration
				if (itemFormatConfig.hasTimeCols()) {
					sb.append(sep + sep);
				}
			}
		}
	}

	@Override
	public void closeResultSet() {
		sb.append(car);
		row_counter++;
	}

	@Override
	public void closeReport() {
		if (qtiItemObjectList == null) { throw new OLATRuntimeException(null, "Can not format report when qtiItemObjectList is null", null); }
		final String legend = translator.translate("legend");
		sb.append(car + car);
		sb.append(legend);
		sb.append(car + car);
		int y = 1;
		for (final Iterator iter = qtiItemObjectList.iterator(); iter.hasNext();) {
			final QTIItemObject element = (QTIItemObject) iter.next();

			sb.append(element.getItemIdent());
			sb.append(sep);
			sb.append(emb);
			sb.append(escape(element.getItemTitle()));
			sb.append(emb);
			sb.append(car);

			sb.append(sep + sep);
			sb.append("minValue");
			sb.append(sep);
			sb.append(element.getItemMinValue());
			sb.append(car);

			sb.append(sep + sep);
			sb.append("maxValue");
			sb.append(sep);
			sb.append(element.getItemMaxValue());
			sb.append(car);

			sb.append(sep + sep);
			sb.append("cutValue");
			sb.append(sep);
			sb.append(element.getItemCutValue());
			sb.append(car);

			// CELFI#107
			sb.append(sep + sep + sep + sep);
			String question = element.getQuestionText();
			if (tagless) {
				question = FilterFactory.getXSSFilter(-1).filter(question);
				question = FilterFactory.getHtmlTagsFilter().filter(question);
			}
			question = StringEscapeUtils.unescapeHtml(question);
			sb.append(question);
			sb.append(car);
			// CELFI#107 END

			final List responseLabelMaterials = element.getResponseLabelMaterials();

			for (int i = 0; i < element.getResponseIdentifier().size(); i++) {
				sb.append(sep + sep);
				sb.append(y);
				sb.append("_");
				sb.append(element.getItemType());
				sb.append(i + 1);
				sb.append(sep);
				sb.append(element.getResponseIdentifier().get(i));
				sb.append(sep);

				if (responseLabelMaterials != null) {
					String s = (String) responseLabelMaterials.get(i);
					s = StringEscapeUtils.unescapeHtml(s);
					if (tagless) {
						s = s.replaceAll("\\<.*?\\>", "");
					}
					sb.append(Formatter.stripTabsAndReturns(s));
				}
				sb.append(car);
			}
			y++;
		}

		sb.append(car + car);
		sb.append("SCQ");
		sb.append(sep);
		sb.append("Single Choice Question");
		sb.append(car);
		sb.append("MCQ");
		sb.append(sep);
		sb.append("Multiple Choice Question");
		sb.append(car);
		sb.append("FIB");
		sb.append(sep);
		sb.append("Fill in the blank");
		sb.append(car);
		sb.append("ESS");
		sb.append(sep);
		sb.append("Essay");
		sb.append(car);
		sb.append("KPR");
		sb.append(sep);
		sb.append("Kprim (K-Type)");

		sb.append(car + car);
		sb.append("R:");
		sb.append(sep);
		sb.append("Radio button (SCQ)");
		sb.append(car);
		sb.append("C:");
		sb.append(sep);
		sb.append("Check box (MCQ or KPR)");
		sb.append(car);
		sb.append("B:");
		sb.append(sep);
		sb.append("Blank (FIB)");
		sb.append(car);
		sb.append("A:");
		sb.append(sep);
		sb.append("Area (ESS)");

		sb.append(car + car);
		sb.append("x_Ry");
		sb.append(sep);
		sb.append("Radio Button y of SCQ x, e.g. 1_R1");
		sb.append(car);
		sb.append("x_Cy");
		sb.append(sep);
		sb.append("Check Box y of MCQ x or two Radio Buttons y of KPR x, e.g. 3_C2");
		sb.append(car);
		sb.append("x_By");
		sb.append(sep);
		sb.append("Blank y of FIB x, e.g. 17_B2");
		sb.append(car);
		sb.append("x_Ay");
		sb.append(sep);
		sb.append("Area y of ESS x, e.g. 4_A1");

		sb.append(car + car);
		sb.append("Kprim:");
		sb.append(sep);
		sb.append("'+' = yes");
		sb.append(sep);
		sb.append("'-' = no");
		sb.append(sep);
		sb.append("'.' = no answer");
		sb.append(sep);

	}

	@Override
	public String getReport() {
		return sb.toString();
	}

	private String createHeaderRow1Intro() {
		return sep + sep + sep + sep + sep + sep + sep + sep + sep + sep;
	}

	/**
	 * Creates header line for all types
	 * 
	 * @param theType
	 * @return header line for download
	 */
	private String createHeaderRow2Intro() {

		final StringBuilder hr2Intro = new StringBuilder();

		// header for personalized download (iqtest)
		final String sequentialNumber = translator.translate("column.header.seqnum");

		final String lastName = translator.translate("column.header.name");
		final String firstName = translator.translate("column.header.vorname");
		final String login = translator.translate("column.header.login");
		final String instUsrIdent = translator.translate("column.header.instUsrIdent");
		final String assessPoint = translator.translate("column.header.assesspoints");
		final String passed = translator.translate("column.header.passed");
		final String ipAddress = translator.translate("column.header.ipaddress");
		final String date = translator.translate("column.header.date");
		final String duration = translator.translate("column.header.duration");

		hr2Intro.append(sequentialNumber);
		hr2Intro.append(sep);
		hr2Intro.append(lastName);
		hr2Intro.append(sep);
		hr2Intro.append(firstName);
		hr2Intro.append(sep);
		hr2Intro.append(login);
		hr2Intro.append(sep);
		hr2Intro.append(instUsrIdent);
		hr2Intro.append(sep);
		hr2Intro.append(assessPoint);
		hr2Intro.append(sep);
		hr2Intro.append(passed);
		hr2Intro.append(sep);
		hr2Intro.append(ipAddress);
		hr2Intro.append(sep);
		hr2Intro.append(date);
		hr2Intro.append(sep);
		hr2Intro.append(duration);
		hr2Intro.append(sep);

		return hr2Intro.toString();
	}

	@Override
	public String getFileNamePrefix() {
		return fileNamePrefix;
	}

	/*
	 * (non-Javadoc)
	 * @see org.olat.ims.qti.export.QTIExportFormatter#getType()
	 */
	@Override
	public int getType() {
		return this.type;
	}

	private boolean displayItem(final QTIExportItemFormatConfig c) {
		return !(!c.hasResponseCols() && !c.hasPointCol() && !c.hasTimeCols() && !c.hasPositionsOfResponsesCol());
	}

	private String escape(final String s) {
		return s.replaceAll(emb, esc + emb);
	}

	private void setDefaultQTIItemConfigs() {
		final Map itConfigs = new HashMap();

		for (final Iterator iter = qtiItemObjectList.iterator(); iter.hasNext();) {
			final QTIItemObject item = (QTIItemObject) iter.next();
			if (item.getItemIdent().startsWith(ItemParser.ITEM_PREFIX_SCQ)) {
				if (itConfigs.get(QTIExportSCQItemFormatConfig.class) == null) {
					final QTIExportSCQItemFormatConfig confSCQ = new QTIExportSCQItemFormatConfig(true, false, false, false);
					itConfigs.put(QTIExportSCQItemFormatConfig.class, confSCQ);
				}
			} else if (item.getItemIdent().startsWith(ItemParser.ITEM_PREFIX_MCQ)) {
				if (itConfigs.get(QTIExportMCQItemFormatConfig.class) == null) {
					final QTIExportMCQItemFormatConfig confMCQ = new QTIExportMCQItemFormatConfig(true, false, false, false);
					itConfigs.put(QTIExportMCQItemFormatConfig.class, confMCQ);
				}
			} else if (item.getItemIdent().startsWith(ItemParser.ITEM_PREFIX_KPRIM)) {
				if (itConfigs.get(QTIExportKPRIMItemFormatConfig.class) == null) {
					final QTIExportKPRIMItemFormatConfig confKPRIM = new QTIExportKPRIMItemFormatConfig(true, false, false, false);
					itConfigs.put(QTIExportKPRIMItemFormatConfig.class, confKPRIM);
				}
			} else if (item.getItemIdent().startsWith(ItemParser.ITEM_PREFIX_ESSAY)) {
				if (itConfigs.get(QTIExportEssayItemFormatConfig.class) == null) {
					final QTIExportEssayItemFormatConfig confEssay = new QTIExportEssayItemFormatConfig(true, false);
					itConfigs.put(QTIExportEssayItemFormatConfig.class, confEssay);
				}
			} else if (item.getItemIdent().startsWith(ItemParser.ITEM_PREFIX_FIB)) {
				if (itConfigs.get(QTIExportFIBItemFormatConfig.class) == null) {
					final QTIExportFIBItemFormatConfig confFIB = new QTIExportFIBItemFormatConfig(true, false, false);
					itConfigs.put(QTIExportFIBItemFormatConfig.class, confFIB);
				}
			}
			// if cannot find the type via the ItemParser, look for the QTIItemObject type
			else if (item.getItemType().equals(QTIItemObject.TYPE.A)) {
				final QTIExportEssayItemFormatConfig confEssay = new QTIExportEssayItemFormatConfig(true, false);
				itConfigs.put(QTIExportEssayItemFormatConfig.class, confEssay);
			} else if (item.getItemType().equals(QTIItemObject.TYPE.R)) {
				final QTIExportSCQItemFormatConfig confSCQ = new QTIExportSCQItemFormatConfig(true, false, false, false);
				itConfigs.put(QTIExportSCQItemFormatConfig.class, confSCQ);
			} else if (item.getItemType().equals(QTIItemObject.TYPE.C)) {
				final QTIExportMCQItemFormatConfig confMCQ = new QTIExportMCQItemFormatConfig(true, false, false, false);
				itConfigs.put(QTIExportMCQItemFormatConfig.class, confMCQ);
			} else if (item.getItemType().equals(QTIItemObject.TYPE.B)) {
				final QTIExportFIBItemFormatConfig confFIB = new QTIExportFIBItemFormatConfig(true, false, false);
				itConfigs.put(QTIExportFIBItemFormatConfig.class, confFIB);
			} else {
				throw new OLATRuntimeException(null, "Can not resolve QTIItem type='" + item.getItemType() + "'", null);
			}
		}
		mapWithExportItemConfigs = itConfigs;
	}
}
