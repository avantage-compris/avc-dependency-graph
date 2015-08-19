package net.avcompris.tools.dependency_graph;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * This class holds the dependency information that will be analyzed and
 * put into a diagram. 
 * 
 * @author David Andrianavalontsalama
 */
public class Module {

	public final String name;

	public Module(final String name) {

		this.name = checkNotNull(name, "name");
	}

	private final Set<String> downstreamModules = new HashSet<String>();

	private final Set<String> upstreamModules = new HashSet<String>();

	/**
	 * add a dependency: This module is declared as depending
	 * on an other module, named "downstreamModuleName"
	 */
	public void addToDownstreamModules(final String downstreamModuleName) {

		checkNotNull(downstreamModuleName, "downstreamModuleName");

		downstreamModules.add(downstreamModuleName);
	}

	/**
	 * add a reverse dependency: This module is declared as being a dependance
	 * for on an other module, named "upstreamModuleName"
	 */
	public void addToUpstreamModules(final String upstreamModuleName) {

		checkNotNull(upstreamModuleName, "upstreamModuleName");

		upstreamModules.add(upstreamModuleName);
	}

	public Iterable<String> getDownstreamModules() {

		return downstreamModules;
	}

	public Iterable<String> getUpstreamModules() {

		return upstreamModules;
	}
}
