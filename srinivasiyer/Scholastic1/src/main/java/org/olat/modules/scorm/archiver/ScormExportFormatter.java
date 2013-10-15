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
 * Copyright (c) 2009 frentix GmbH, Switzerland<br>
 * <p>
 */
package org.olat.modules.scorm.archiver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olat.core.gui.translator.Translator;

/**
 * Description:<br>
 * An implementation of the ScormExportVisitor which create a tab separated file with the results of the visited scos.
 * <P>
 * Initial Date: 17 august 2009 <br>
 * 
 * @author srosse
 */
public class ScormExportFormatter implements ScormExportVisitor {
	private final Translator translator;
	private final List<ScoDatas> datas = new ArrayList<ScoDatas>();

	private final Map<String, CmiColumn> interactionColsMap = new HashMap<String, CmiColumn>();
	private final Map<String, CmiColumn> objectivesColsMap = new HashMap<String, CmiColumn>();

	public ScormExportFormatter(final Translator translator) {
		this.translator = translator;
	}

	/**
	 * Collect the datas and count the number of interactions and objectives
	 * 
	 * @see org.olat.modules.scorm.archiver.ScormExportVisitor#visit(org.olat.modules.scorm.archiver.ScoDatas)
	 */
	@Override
	public void visit(final ScoDatas data) {
		datas.add(data);

		for (final ScoInteraction interaction : data.getInteractions()) {
			CmiColumn col = interactionColsMap.get(interaction.getInteractionId());
			if (col == null) {
				col = new CmiColumn(interaction);
				interactionColsMap.put(interaction.getInteractionId(), col);
			}
		}

		for (final ScoObjective objective : data.getObjectives()) {
			CmiColumn col = interactionColsMap.get(objective.getId());
			if (col == null) {
				col = new CmiColumn(objective);
				objectivesColsMap.put(objective.getId(), col);
			}
		}
	}

	/**
	 * Build the file with the collected datas.
	 * 
	 * @return
	 */
	public String export() {
		final StringBuilder sb = new StringBuilder();

		final List<CmiColumn> interactionsCols = new ArrayList<CmiColumn>(interactionColsMap.values());
		Collections.sort(interactionsCols);

		final List<CmiColumn> objectivesCols = new ArrayList<CmiColumn>(objectivesColsMap.values());
		Collections.sort(objectivesCols);

		// header
		String headerVal = translator.translate("results.table.header.username");
		sb.append(headerVal);

		headerVal = translator.translate("results.table.header.itemId");
		sb.append('\t').append(headerVal);

		headerVal = translator.translate("results.table.header.rawScore");
		sb.append('\t').append(headerVal);

		headerVal = translator.translate("results.table.header.lessonStatus");
		sb.append('\t').append(headerVal);

		headerVal = translator.translate("results.table.header.totalTime");
		sb.append('\t').append(headerVal);

		for (int i = 0; i < interactionsCols.size(); i++) {
			final int count = i + 1;
			final String headerInteraction = translator.translate("results.table.header.interaction", new String[] { Integer.toString(count) });
			sb.append('\t').append(headerInteraction);
			sb.append('\t').append("SR").append(' ').append(count);
			sb.append('\t').append("CR").append(' ').append(count);
		}

		for (int i = 0; i < objectivesCols.size(); i++) {
			final int count = i + 1;
			final String headerObjective = translator.translate("results.table.header.objective", new String[] { Integer.toString(count) });
			sb.append('\t').append(headerObjective);
		}

		headerVal = translator.translate("results.table.header.comments");
		sb.append('\t').append(headerVal);

		sb.append('\n');

		// data
		for (final ScoDatas data : datas) {
			sb.append(clean(data.getUsername()));
			sb.append('\t').append(clean(data.getItemId()));
			sb.append('\t').append(clean(data.getRawScore()));
			sb.append('\t').append(clean(data.getLessonStatus()));
			sb.append('\t').append(clean(data.getTotalTime()));

			// interactions
			for (final CmiColumn col : interactionsCols) {
				final int pos = col.getPosition();
				final ScoInteraction interactions = data.getInteraction(pos);
				sb.append('\t').append(clean(interactions.getResult()));
				sb.append('\t').append(clean(interactions.getStudentResponse()));
				sb.append('\t').append(clean(interactions.getCorrectResponse()));
			}

			// objectives
			for (final CmiColumn col : objectivesCols) {
				final String id = col.getId();
				final ScoObjective objective = data.getObjective(id);
				if (objective == null) {
					sb.append('\t');
				} else {
					sb.append('\t').append(objective.getScoreRaw());
				}
			}

			sb.append('\t').append(clean(data.getComments()));
			sb.append('\n');
		}

		// footer
		sb.append('\n').append('\n').append('\n');

		String footerVal = translator.translate("results.table.foot.cr");
		sb.append("SR:").append('\t').append(footerVal).append('\n');

		footerVal = translator.translate("results.table.footer.sr");
		sb.append("CR:").append('\t').append(footerVal).append('\n');

		return sb.toString();
	}

	private String clean(final String str) {
		return str == null ? "" : str;
	}

	@Override
	public String toString() {
		return export();
	}

	public class CmiColumn implements Comparable<CmiColumn> {
		private final int position;
		private final String id;

		public CmiColumn(final ScoObjective objective) {
			this.id = objective.getId();
			this.position = objective.getPosition();
		}

		public CmiColumn(final ScoInteraction interaction) {
			this.id = interaction.getInteractionId();
			this.position = interaction.getPosition();
		}

		public String getId() {
			return id;
		}

		public int getPosition() {
			return position;
		}

		@Override
		public int compareTo(final CmiColumn o) {
			int c = position - o.position;
			if (c == 0) {
				if (id == null) {
					return -1;
				} else if (o.id == null) { return 1; }
				c = id.compareTo(o.id);
			}
			return c;
		}

		@Override
		public int hashCode() {
			return id == null ? 257869 : id.hashCode();
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			} else if (obj instanceof CmiColumn) {
				final CmiColumn col = (CmiColumn) obj;
				return id != null && id.equals(col.id);
			}
			return false;
		}
	}
}
