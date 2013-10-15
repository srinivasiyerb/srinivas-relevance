package org.olat.restapi.repository.course.config;

import org.olat.course.ICourse;
import org.olat.course.nodes.CourseNode;
import org.olat.ims.qti.process.AssessmentInstance;
import org.olat.modules.ModuleConfiguration;
import org.olat.repository.RepositoryEntry;
import org.olat.restapi.repository.course.AbstractCourseNodeWebService.CustomConfigDelegate;
import org.olat.restapi.repository.course.config.CustomConfigFactory.ICustomConfigCreator;

import de.bps.onyx.plugin.course.nodes.iq.IQEditController;

public class OlatCustomConfigCreator implements ICustomConfigCreator {

	public OlatCustomConfigCreator() {
		//
	}

	@Override
	public CustomConfigDelegate getTestCustomConfig(final RepositoryEntry repoEntry) {
		return new OlatTestCustomConfig(repoEntry);
	}

	@Override
	public CustomConfigDelegate getSurveyCustomConfig(final RepositoryEntry repoEntry) {
		return new OlatSurveyCustomConfig(repoEntry);
	}

	/* CustomConfigDelegate implementations */
	public class OlatTestCustomConfig implements CustomConfigDelegate {
		private final RepositoryEntry testRepoEntry;

		@Override
		public boolean isValid() {
			return testRepoEntry != null;
		}

		public OlatTestCustomConfig(final RepositoryEntry testRepoEntry) {
			this.testRepoEntry = testRepoEntry;
		}

		@Override
		public void configure(final ICourse course, final CourseNode newNode, final ModuleConfiguration moduleConfig) {
			moduleConfig.set(IQEditController.CONFIG_KEY_REPOSITORY_SOFTKEY, testRepoEntry.getSoftkey());
		}
	}

	public class OlatSurveyCustomConfig implements CustomConfigDelegate {
		private final RepositoryEntry surveyRepoEntry;

		public OlatSurveyCustomConfig(final RepositoryEntry surveyRepoEntry) {
			this.surveyRepoEntry = surveyRepoEntry;
		}

		@Override
		public boolean isValid() {
			return true;
		}

		@Override
		public void configure(final ICourse course, final CourseNode newNode, final ModuleConfiguration moduleConfig) {
			moduleConfig.set(IQEditController.CONFIG_KEY_REPOSITORY_SOFTKEY, surveyRepoEntry.getSoftkey());
			moduleConfig.set(IQEditController.CONFIG_KEY_ENABLEMENU, new Boolean(true));
			moduleConfig.set(IQEditController.CONFIG_KEY_SEQUENCE, AssessmentInstance.QMD_ENTRY_SEQUENCE_ITEM);
			moduleConfig.set(IQEditController.CONFIG_KEY_TYPE, AssessmentInstance.QMD_ENTRY_TYPE_SURVEY);
			moduleConfig.set(IQEditController.CONFIG_KEY_SUMMARY, AssessmentInstance.QMD_ENTRY_SUMMARY_NONE);
		}
	}

}
