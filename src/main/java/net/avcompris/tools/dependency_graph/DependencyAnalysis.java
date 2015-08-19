package net.avcompris.tools.dependency_graph;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

public class DependencyAnalysis {

	public final ImmutableMap<String, Module> modules;

	private final List<Set<String>> moduleLevels = new ArrayList<Set<String>>();

	public int sizeOfModuleLevels() {

		return moduleLevels.size();
	}

	public Iterable<Set<String>> getModuleLevels() {

		return moduleLevels;
	}

	public DependencyAnalysis(final Module... modules) {

		this(toMap(modules));
	}

	public DependencyAnalysis(final Iterable<Module> modules) {

		this(toMap(modules));
	}

	private static Map<String, Module> toMap(final Module[] modules) {

		checkNotNull(modules, "modules");

		return toMap(Arrays.asList(modules));
	}

	private static Map<String, Module> toMap(final Iterable<Module> modules) {

		checkNotNull(modules, "modules");

		final Map<String, Module> map = new HashMap<String, Module>();

		for (final Module module : modules) {

			if (module == null) {
				throw new NullPointerException(
						"Module array should not contain any null value.");
			}

			if (map.containsKey(module.name)) {
				throw new IllegalArgumentException("Duplicate module name: \""
						+ module.name + "\"");
			}

			map.put(module.name, module);
		}

		return map;
	}

	public DependencyAnalysis(final Map<String, Module> m) {

		checkNotNull(m, "modules");

		this.modules = ImmutableMap.copyOf(m);

		final int moduleCount = modules.size();

		// --------------------------------------------------------------------- 
		//     SANITY CHECKS
		// --------------------------------------------------------------------- 

		for (final Map.Entry<String, Module> entry : modules.entrySet()) {

			final String name = entry.getKey();
			final Module module = entry.getValue();

			if (!name.equals(module.name)) {
				throw new IllegalArgumentException("Module is registered as \""
						+ name + "\", but has name: \"" + name + "\"");
			}
		}

		for (final Module module : modules.values()) {

			for (final String downstream : module.getDownstreamModules()) {

				if (!modules.containsKey(downstream)) {
					throw new IllegalArgumentException(
							"Module is declared as a downstream module (for \""
									+ module.name
									+ "\"), but cannot be found: \""
									+ downstream + "\"");
				}
			}

			for (final String upstream : module.getUpstreamModules()) {

				if (!modules.containsKey(upstream)) {
					throw new IllegalArgumentException(
							"Module is declared as an upstream module (for \""
									+ module.name
									+ "\"), but cannot be found: \"" + upstream
									+ "\"");
				}
			}

			countTransitiveDownstreams(module);

			countTransitiveUpstreams(module);
		}

		// --------------------------------------------------------------------- 
		//     ANALYZE TREE
		// --------------------------------------------------------------------- 

		final Set<String> modulesAlreadyVisited = new HashSet<String>();

		int i = 0; // For error detection

		while (modulesAlreadyVisited.size() < modules.size()) {

			++i;

			if (i > moduleCount) {
				throw new RuntimeException("i: " + i + " > moduleCount: "
						+ moduleCount);
			}

			System.out.println("---------------------------------------------");

			final Set<String> modulesOnThisLevel = new HashSet<String>();

			moduleLevels.add(modulesOnThisLevel);

			for (final Module module : modules.values()) {

				final String moduleName = module.name;

				if (modulesAlreadyVisited.contains(moduleName)) {
					continue;
				}

				final Collection<String> upstreams = getDirectUpstreams(moduleName);

				if (modulesAlreadyVisited.containsAll(upstreams)) {

					modulesOnThisLevel.add(moduleName);
				}
			}

			for (final String moduleName : modulesOnThisLevel) {

				System.out.print(moduleName);

				boolean start = true;

				for (final String upstream : getDirectUpstreams(moduleName)) {

					if (start) {

						System.out.print(" -> ");

						start = false;

					} else {

						System.out.print(", ");
					}

					System.out.print(upstream);
				}

				System.out.println();
			}

			for (final String moduleName : modulesOnThisLevel) {

				modulesAlreadyVisited.add(moduleName);
			}
		}

		System.out.println("---------------------------------------------");
	}

	private int countTransitiveDownstreams(final Module module) {

		checkNotNull(module, "module");

		return countTransitiveDownstreams(module.name, module.name,
				modules.size());
	}

	private int countTransitiveUpstreams(final Module module) {

		checkNotNull(module, "module");

		return countTransitiveUpstreams(module.name, module.name,
				modules.size());
	}

	private int countTransitiveDownstreams(final String initialModuleName,
			final String currentModuleName, final int limit) {

		checkNotNull(initialModuleName, "initialModuleName");
		checkNotNull(currentModuleName, "currentModuleName");

		if (limit <= 0) {
			throw new ArrayIndexOutOfBoundsException(
					"Unlimited transitive downstreams found (probably a cycle) for module: \""
							+ initialModuleName + "\"");
		}

		int max = 0;

		for (final String downstream : modules.get(currentModuleName)
				.getDownstreamModules()) {

			final int countTransitiveDownstreams = countTransitiveDownstreams(
					initialModuleName, downstream, limit - 1);

			if (countTransitiveDownstreams > max) {
				max = countTransitiveDownstreams;
			}
		}

		return max + 1;
	}

	private int countTransitiveUpstreams(final String initialModuleName,
			final String currentModuleName, final int limit) {

		checkNotNull(initialModuleName, "initialModuleName");
		checkNotNull(currentModuleName, "currentModuleName");

		if (limit <= 0) {
			throw new ArrayIndexOutOfBoundsException(
					"Unlimited transitive upstreams found (probably a cycle) for module: \""
							+ initialModuleName + "\"");
		}

		int max = 0;

		for (final String upstream : modules.get(currentModuleName)
				.getUpstreamModules()) {

			final int countTransitiveUpstreams = countTransitiveDownstreams(
					initialModuleName, upstream, limit - 1);

			if (countTransitiveUpstreams > max) {
				max = countTransitiveUpstreams;
			}
		}

		return max + 1;
	}

	private Map<String, Collection<String>> moduleDirectUpstreams = new HashMap<String, Collection<String>>();

	public Collection<String> getDirectUpstreams(final String moduleName) {

		final Collection<String> cached = moduleDirectUpstreams.get(moduleName);

		if (cached != null) {
			return cached;
		}

		final Set<String> upstreams = new HashSet<String>(
				getUpstreams(moduleName));

		moduleDirectUpstreams.put(moduleName, upstreams);

		final String[] us = Iterables.toArray(upstreams, String.class);

		for (final String u : us) {

			for (final String u2 : us) {

				if (isUpstream(u, u2)) {

					upstreams.remove(u);
				}
			}
		}

		return upstreams;
	}

	private Map<String, Collection<String>> moduleUpstreams = new HashMap<String, Collection<String>>();

	public boolean isUpstream(final String u, final String u2) {

		return getUpstreams(u2).contains(u);
	}

	private Collection<String> getUpstreams(final String moduleName) {

		final Collection<String> cached = moduleUpstreams.get(moduleName);

		if (cached != null) {
			return cached;
		}

		final Module module = modules.get(moduleName);

		final Collection<String> upstreams = new HashSet<String>();

		moduleUpstreams.put(moduleName, upstreams);

		for (final String upstream : module.getUpstreamModules()) {

			upstreams.add(upstream);
		}

		for (final Module m : modules.values()) {

			for (final String downstream : m.getDownstreamModules()) {

				if (downstream.equals(moduleName)) {

					upstreams.add(m.name);
				}
			}
		}

		return upstreams;
	}

	private Map<String, Collection<String>> moduleDirectDownstreams = new HashMap<String, Collection<String>>();

	public Collection<String> getDirectDownstreams(final String moduleName) {

		final Collection<String> cached = moduleDirectDownstreams
				.get(moduleName);

		if (cached != null) {
			return cached;
		}

		final Collection<String> downstreams = new HashSet<String>();

		moduleDirectDownstreams.put(moduleName, downstreams);

		for (final Module module : modules.values()) {

			if (getDirectUpstreams(module.name).contains(moduleName)) {

				downstreams.add(moduleName);
			}
		}

		return downstreams;
	}
}
