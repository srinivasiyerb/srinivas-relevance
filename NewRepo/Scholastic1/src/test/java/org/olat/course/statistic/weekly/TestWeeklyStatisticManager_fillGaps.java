package org.olat.course.statistic.weekly;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class TestWeeklyStatisticManager_fillGaps extends TestCase {

	private WeeklyStatisticManager manager_;

	@Override
	protected void setUp() throws Exception {
		manager_ = new WeeklyStatisticManager();
	}

	private void runTest(final List<String> input, final List<String> expectedOutput) throws Exception {
		final List<String> resultingTestset = manager_.fillGapsInColumnHeaders(input);
		assertListEquals(expectedOutput, resultingTestset);
	}

	public void testNull() throws Exception {
		assertNull(manager_.fillGapsInColumnHeaders(null));
	}

	public void testOne() throws Exception {
		for (int i = 0; i < 50; i++) {
			String s;
			if (i < 10) {
				s = "2010-0" + i;
			} else {
				s = "2010-" + i;
			}
			final List<String> resultingTestset = manager_.fillGapsInColumnHeaders(createList(s));
			assertNull(resultingTestset);
		}
	}

	public void testSimple() throws Exception {
		runTest(createList("2010-01", "2010-02", "2010-03"), createList("2010-01", "2010-02", "2010-03"));
	}

	public void testYearChange() throws Exception {
		runTest(createList("2009-50", "2010-01", "2010-02", "2010-03"), createList("2009-50", "2009-51", "2009-52", "2009-53", "2010-01", "2010-02", "2010-03"));
	}

	public void testAllYearChanges() throws Exception {
		for (int i = 2000; i < 2200; i++) {
			final List<String> input = createList(i + "-50", (i + 1) + "-03");
			final List<String> output = manager_.fillGapsInColumnHeaders(input);

			final List<String> outputVariant1 = createList(i + "-50", i + "-51", i + "-52", (i + 1) + "-01", (i + 1) + "-02", (i + 1) + "-03");
			final List<String> outputVariant2 = createList(i + "-50", i + "-51", i + "-52", i + "-53", (i + 1) + "-01", (i + 1) + "-02", (i + 1) + "-03");
			final List<String> outputVariant3 = createList(i + "-50", i + "-51", i + "-52", i + "-53", /* WeeklyStatisticManager left out week 01... */(i + 1) + "-02",
					(i + 1) + "-03");

			final boolean matchesVariant1 = matches(input, outputVariant1);
			final boolean matchesVariant2 = matches(input, outputVariant2);
			final boolean matchesVariant3 = matches(input, outputVariant3);

			if (matchesVariant1 && !matchesVariant2 && !matchesVariant3) {
				// perfecto
			} else if (!matchesVariant1 && matchesVariant2 && !matchesVariant3) {
				// perfecto
			} else if (!matchesVariant1 && !matchesVariant2 && matchesVariant3) {
				// perfecto
			} else {
				fail("failed with input " + input);
			}
		}
	}

	public void testWronglyFormatted() throws Exception {
		runTest(createList("2010-1", "2010-2", "2010-4"), createList("2010-1", "2010-02", "2010-2", "2010-03", "2010-04", "2010-4"));
	}

	public void testGapsA() throws Exception {
		runTest(createList("2010-01", "2010-02", "2010-04"), createList("2010-01", "2010-02", "2010-03", "2010-04"));
	}

	public void testGapsB() throws Exception {
		runTest(createList("2010-01", "2010-02", "2010-04", "2010-07"), createList("2010-01", "2010-02", "2010-03", "2010-04", "2010-05", "2010-06", "2010-07"));
	}

	public void testBigGap() throws Exception {
		runTest(createList("2009-50", "2010-12"),
				createList("2009-50", "2009-51", "2009-52", "2009-53", "2010-01", "2010-02", "2010-03", "2010-04", "2010-05", "2010-06", "2010-07", "2010-08", "2010-09",
						"2010-10", "2010-11", "2010-12"));
	}

	public void testWrongInputParams() throws Exception {
		final List<String> resultingTestset = manager_.fillGapsInColumnHeaders(createList("2010-50", "2010-12"));
		assertNull(resultingTestset);
	}

	private void assertListEquals(final List<String> testset, final List<String> resultingTestset) {
		if (testset == null || resultingTestset == null) { throw new IllegalArgumentException("testset and resultingtestset must not be empty"); }
		assertEquals("size mismatch", testset.size(), resultingTestset.size());
		for (int i = 0; i < testset.size(); i++) {
			final String expectedStr = testset.get(i);
			final String actualStr = resultingTestset.get(i);
			assertEquals("string at position " + i + " mismatch", expectedStr, actualStr);
		}
	}

	private boolean matches(final List<String> input, final List<String> output) {
		if (input.size() != output.size()) { return false; }
		for (int i = 0; i < input.size(); i++) {
			final String expectedStr = input.get(i);
			final String actualStr = output.get(i);
			if (!expectedStr.equals(actualStr)) { return false; }
		}
		return true;
	}

	private List<String> createList(final String... strings) {
		if (strings == null || strings.length == 0) { throw new IllegalArgumentException("strings must not be empty"); }

		final List<String> result = new ArrayList<String>(strings.length);
		for (int i = 0; i < strings.length; i++) {
			final String aStr = strings[i];
			result.add(aStr);
		}
		return result;
	}
}
