package net.avcompris.tools.dependency_graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.avcompris.binding.annotation.XPath;
import net.avcompris.binding.dom.helper.DomBinderUtils;
import net.avcompris.tools.dependency_graph.JenkinsDependencyGraphTest.JenkinsXMLConfig.Job;

import org.junit.Test;

public class JenkinsDependencyGraphTest {

	private static ModulePosition[] generateDependencyGraph(
			final long optimizeTimeoutMs, final File file) throws Exception {

		// 1. LOAD CONFIGS

		final Map<String, JobXMLConfig> jobConfigs = DomBinderUtils
				.xmlContentToJava(file, JobXMLConfigs.class).getJobConfigs();

		// 2. ANALYSIS

		final Set<Module> modules = new HashSet<Module>();

		for (final JobXMLConfig jobConfig : jobConfigs.values()) {

			final Module module = new Module(jobConfig.getName());

			modules.add(module);

			for (final Job downstreamProject : jobConfig
					.getDownstreamProjects()) {
				module.addToDownstreamModules(downstreamProject.getName());
			}

			for (final Job upstreamProject : jobConfig.getUpstreamProjects()) {
				module.addToUpstreamModules(upstreamProject.getName());
			}
		}

		final DependencyAnalysis analysis = new DependencyAnalysis(modules);

		// 3. SVG OUTPUT

		return new DependencyDiagrammer(analysis).drawTo(optimizeTimeoutMs,
				new File("target", file.getName().replace(".xml", ".svg")));
	}

	@XPath("/jobConfigs")
	private interface JobXMLConfigs {

		@XPath(value = "jobConfig", mapKeysType = String.class, mapKeysXPath = "@jobName", mapValuesType = JobXMLConfig.class, mapValuesXPath = ".")
		Map<String, JobXMLConfig> getJobConfigs();
	}

	@XPath("/hudson")
	public interface JenkinsXMLConfig {

		@XPath("job")
		Job[] getJobs();

		interface Job {

			@XPath("name")
			String getName();

			@XPath("url")
			String getUrl();
		}
	}

	@XPath("/mavenModuleSet")
	private interface JobXMLConfig {

		@XPath("name")
		String getName();

		@XPath("downstreamProject")
		Job[] getDownstreamProjects();

		@XPath("upstreamProject")
		Job[] getUpstreamProjects();
	}

	private static void assertModulePosition(final ModulePosition[] positions,
			final int x, final int y, final String moduleName) {

		for (final ModulePosition position : positions) {

			if (!moduleName.equals(position.getModuleName())) {
				continue;
			}

			assertEquals(moduleName + ".x", 10 + x * 160, position.getX());
			assertEquals(moduleName + ".y", 10 + y * 60, position.getY());

			return;
		}

		fail("Cannot find module: " + moduleName);
	}

	@Test
	public void testJenkinsDependencyGraph_001() throws Exception {

		final ModulePosition[] positions = generateDependencyGraph(-1,
				new File("src/test/xml", "jobsConfig-001.xml"));

		assertEquals(16, positions.length);

		assertModulePosition(positions, 1, 0, "aed-parent");
		assertModulePosition(positions, 0, 1, "aed-web");
		assertModulePosition(positions, 1, 1, "aed-dockerfile-testutil");
		assertModulePosition(positions, 2, 1, "aed-sysadmin");
		assertModulePosition(positions, 3, 1, "aed-common");
		assertModulePosition(positions, 1, 2, "aed-base-dockerfile");
		assertModulePosition(positions, 3, 2, "aed-api-common");
		assertModulePosition(positions, 4, 2, "aed-workers-common");
		assertModulePosition(positions, 0, 3, "aed-web-dockerfile");
		assertModulePosition(positions, 1, 3, "aed-mq0-dockerfile");
		assertModulePosition(positions, 2, 3, "aed-data0-dockerfile");
		assertModulePosition(positions, 3, 3, "aed-monitoring-api");
		assertModulePosition(positions, 4, 3, "aed-monitoring-workers");
		assertModulePosition(positions, 0, 4, "aed-web-it");
		assertModulePosition(positions, 3, 4, "aed-monitoring-web");
		assertModulePosition(positions, 2, 5, "aed-monitoring-dockerfile");
	}

	@Test
	public void testJenkinsDependencyGraph_001_timeout120s() throws Exception {

		final ModulePosition[] positions = generateDependencyGraph(120000L,
				new File("src/test/xml", "jobsConfig-001.xml"));

		assertEquals(16, positions.length);

		assertModulePosition(positions, 1, 0, "aed-parent");
		assertModulePosition(positions, 0, 1, "aed-web");
		assertModulePosition(positions, 1, 1, "aed-dockerfile-testutil");
		assertModulePosition(positions, 2, 1, "aed-sysadmin");
		assertModulePosition(positions, 3, 1, "aed-common");
		assertModulePosition(positions, 1, 2, "aed-base-dockerfile");
		assertModulePosition(positions, 3, 2, "aed-api-common");
		assertModulePosition(positions, 4, 2, "aed-workers-common");
		assertModulePosition(positions, 0, 3, "aed-web-dockerfile");
		assertModulePosition(positions, 1, 3, "aed-mq0-dockerfile");
		assertModulePosition(positions, 2, 3, "aed-data0-dockerfile");
		assertModulePosition(positions, 3, 3, "aed-monitoring-api");
		assertModulePosition(positions, 4, 3, "aed-monitoring-workers");
		assertModulePosition(positions, 0, 4, "aed-web-it");
		assertModulePosition(positions, 3, 4, "aed-monitoring-web");
		assertModulePosition(positions, 2, 5, "aed-monitoring-dockerfile");
	}

	@Test
	public void testJenkinsDependencyGraph_001_timeout1ms() throws Exception {

		final ModulePosition[] positions = generateDependencyGraph(1, new File(
				"src/test/xml", "jobsConfig-001.xml"));

		assertEquals(16, positions.length);

		assertModulePosition(positions, 0, 0, "aed-parent");
		assertModulePosition(positions, 0, 1, "aed-sysadmin");
		assertModulePosition(positions, 1, 1, "aed-dockerfile-testutil");
		assertModulePosition(positions, 2, 1, "aed-web");
		assertModulePosition(positions, 3, 1, "aed-common");
		assertModulePosition(positions, 0, 2, "aed-base-dockerfile");
		assertModulePosition(positions, 1, 2, "aed-api-common");
		assertModulePosition(positions, 2, 2, "aed-workers-common");
		assertModulePosition(positions, 0, 3, "aed-data0-dockerfile");
		assertModulePosition(positions, 1, 3, "aed-web-dockerfile");
		assertModulePosition(positions, 2, 3, "aed-monitoring-workers");
		assertModulePosition(positions, 3, 3, "aed-monitoring-api");
		assertModulePosition(positions, 4, 3, "aed-mq0-dockerfile");
		assertModulePosition(positions, 0, 4, "aed-web-it");
		assertModulePosition(positions, 1, 4, "aed-monitoring-web");
		assertModulePosition(positions, 0, 5, "aed-monitoring-dockerfile");
	}

	@Test
	public void testJenkinsDependencyGraph_002() throws Exception {

		final ModulePosition[] positions = generateDependencyGraph(-1,
				new File("src/test/xml", "jobsConfig-002.xml"));

		assertEquals(10, positions.length);

		assertModulePosition(positions, 0, 0, "avc-webapp-it-commons");
		assertModulePosition(positions, 1, 0, "avc-dbqueries");
		assertModulePosition(positions, 3, 0, "avc-manifest-commons");
		assertModulePosition(positions, 0, 1, "avc-node-commons");
		assertModulePosition(positions, 2, 1, "avc-core-commons");
		assertModulePosition(positions, 3, 1, "avc-webapp-scenarios-plugin");
		assertModulePosition(positions, 1, 2, "avc-workers-commons");
		assertModulePosition(positions, 2, 2, "avc-webapp-commons");
		assertModulePosition(positions, 3, 2, "avc-dbdescribe");
		assertModulePosition(positions, 0, 3, "avc-workers-plugin");
	}

	@Test
	public void testJenkinsDependencyGraph_002_dontOptimize() throws Exception {

		final ModulePosition[] positions = generateDependencyGraph(0, new File(
				"src/test/xml", "jobsConfig-002.xml"));

		assertEquals(10, positions.length);

		assertModulePosition(positions, 0, 0, "avc-dbqueries");
		assertModulePosition(positions, 1, 0, "avc-webapp-it-commons");
		assertModulePosition(positions, 2, 0, "avc-manifest-commons");
		assertModulePosition(positions, 0, 1, "avc-webapp-scenarios-plugin");
		assertModulePosition(positions, 1, 1, "avc-core-commons");
		assertModulePosition(positions, 2, 1, "avc-node-commons");
		assertModulePosition(positions, 0, 2, "avc-dbdescribe");
		assertModulePosition(positions, 1, 2, "avc-webapp-commons");
		assertModulePosition(positions, 2, 2, "avc-workers-commons");
		assertModulePosition(positions, 0, 3, "avc-workers-plugin");
	}

	@Test
	public void testJenkinsDependencyGraph_003() throws Exception {

		final ModulePosition[] positions = generateDependencyGraph(-1,
				new File("src/test/xml", "jobsConfig-003.xml"));

		assertEquals(17, positions.length);

		assertModulePosition(positions, 0, 0, "gco-parent");
		assertModulePosition(positions, 1, 0, "avc-work-parent");
		assertModulePosition(positions, 0, 1, "avc-common-parent");
		assertModulePosition(positions, 1, 1, "avc-commons-databeans");
		assertModulePosition(positions, 2, 1, "avc-plugin-common");
		assertModulePosition(positions, 1, 2, "avc-common");
		assertModulePosition(positions, 3, 2, "avc-plugin-parent");
		assertModulePosition(positions, 0, 3, "avc-common-http");
		assertModulePosition(positions, 1, 3, "avc-common-httpclient");
		assertModulePosition(positions, 2, 3, "avc-common-testutil");
		assertModulePosition(positions, 3, 3, "avc-xmldata-core");
		assertModulePosition(positions, 4, 3, "avc-common-plugin-parent");
		assertModulePosition(positions, 0, 4, "avc-common-htmlparse");
		assertModulePosition(positions, 1, 4, "avci-parent");
		assertModulePosition(positions, 2, 4, "avc-marshalling");
		assertModulePosition(positions, 3, 4, "avc-xmldata-testutil");
		assertModulePosition(positions, 2, 5, "avc-xmldata");
	}
}
