package net.avcompris.tools.dependency_graph;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.avcompris.binding.annotation.XPath;
import net.avcompris.binding.dom.helper.DomBinderUtils;
import net.avcompris.tools.dependency_graph.JenkinsDependencyGraphTest.JenkinsXMLConfig.Job;

import org.junit.Test;

public class JenkinsDependencyGraphTest {

	private static void generateDependencyGraph(final File file) throws Exception {

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

		final boolean optimize = true;

		new DependencyDiagrammer(analysis).drawTo(optimize, new File("target",
				file.getName().replace(".xml", ".svg")));
	}

	@Test
	public void testJenkinsDependencyGraph_001() throws Exception {

		generateDependencyGraph(new File("src/test/xml", "jobsConfig-001.xml"));
	}

	@Test
	public void testJenkinsDependencyGraph_002() throws Exception {

		generateDependencyGraph(new File("src/test/xml", "jobsConfig-002.xml"));
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
}
