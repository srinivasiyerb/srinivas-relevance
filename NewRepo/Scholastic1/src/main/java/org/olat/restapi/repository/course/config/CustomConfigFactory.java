package org.olat.restapi.repository.course.config;

import org.olat.repository.RepositoryEntry;
import org.olat.restapi.repository.course.AbstractCourseNodeWebService.CustomConfigDelegate;

public class CustomConfigFactory {

	static ICustomConfigCreator creator = null;

	public CustomConfigFactory(final ICustomConfigCreator creator) {
		CustomConfigFactory.creator = creator;
	}

	public static CustomConfigDelegate getTestCustomConfig(final RepositoryEntry repoEntry) {
		return CustomConfigFactory.creator.getTestCustomConfig(repoEntry);
	}

	public static CustomConfigDelegate getSurveyCustomConfig(final RepositoryEntry repoEntry) {
		return CustomConfigFactory.creator.getSurveyCustomConfig(repoEntry);
	}

	public interface ICustomConfigCreator {
		public CustomConfigDelegate getTestCustomConfig(RepositoryEntry repoEntry);

		public CustomConfigDelegate getSurveyCustomConfig(RepositoryEntry repoEntry);
	}
}