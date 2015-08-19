package net.avcompris.tools.dependency_graph;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class DependencyAnalysisTest {

	@Test(expected = IllegalArgumentException.class)
	public void testSanityCheckBadName() throws Exception {

		new DependencyAnalysis(ImmutableMap.of("toto", new Module("John")));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSanityCheckBadDownstream() throws Exception {

		new DependencyAnalysis(ImmutableMap.of("toto",
				new Module("toto").addToDownstreamModules("John")));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSanityCheckBadUpstream() throws Exception {

		new DependencyAnalysis(ImmutableMap.of("toto",
				new Module("toto").addToUpstreamModules("John")));
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testSanityCheckDownstreamCycle() throws Exception {

		new DependencyAnalysis(ImmutableMap.of("toto",
				new Module("toto").addToDownstreamModules("toto")));
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void testSanityCheckUpstreamCycle() throws Exception {

		new DependencyAnalysis(ImmutableMap.of("toto",
				new Module("toto").addToUpstreamModules("toto")));
	}

	@Test
	public void testSanityCheckOK() throws Exception {

		new DependencyAnalysis(ImmutableMap.of("toto", new Module("toto")));
	}
}
