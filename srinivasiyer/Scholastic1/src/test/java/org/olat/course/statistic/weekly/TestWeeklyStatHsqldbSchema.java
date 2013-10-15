package org.olat.course.statistic.weekly;

import java.sql.Connection;
import java.sql.Statement;

import junit.framework.TestCase;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.olat.course.statistic.HibernateUtil;

public class TestWeeklyStatHsqldbSchema extends TestCase {

	@Override
	protected void setUp() throws Exception {
		final Configuration config = new Configuration().setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect")
				.setProperty("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver").setProperty("hibernate.connection.url", "jdbc:hsqldb:mem:baseball")
				.setProperty("hibernate.connection.username", "sa").setProperty("hibernate.connection.password", "").setProperty("hibernate.connection.pool_size", "1")
				.setProperty("hibernate.connection.autocommit", "true").setProperty("hibernate.cache.provider_class", "org.hibernate.cache.HashtableCacheProvider")
				.setProperty("hibernate.hbm2ddl.auto", "create-drop").setProperty("hibernate.show_sql", "true").addClass(WeeklyStat.class);

		HibernateUtil.setSessionFactory(config.buildSessionFactory());

		// uncomment the following if you want to launch a the dbmanager gui (while debugging through this class probably)
		/*
		 * Runnable r = new Runnable() {
		 * @Override public void run() { org.hsqldb.util.DatabaseManagerSwing.main(new String[0]); } }; Thread th = new Thread(r); th.setDaemon(true); th.start();
		 */}

	public void testHibernateSave() throws Exception {
		final WeeklyStat ds = new WeeklyStat();
		ds.setBusinessPath("test businesspath");
		ds.setWeek("2010-03");
		ds.setResId(100);
		ds.setValue(1);
		HibernateUtil.save(ds);
	}

	public void testRawInsert() throws Exception {
		final Session session = HibernateUtil.getSession();
		try {
			final Connection connection = session.connection();
			final Statement statement = connection.createStatement();
			try {
				statement.executeUpdate("insert into o_stat_weekly (businesspath,resid,week,value) values ('businesspath',101,'2010-03',202)");
				connection.commit();
			} finally {
				statement.close();
			}
		} finally {
			session.close();
		}

	}

}
