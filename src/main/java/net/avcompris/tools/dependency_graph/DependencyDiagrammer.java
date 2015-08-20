package net.avcompris.tools.dependency_graph;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicStampedReference;

import javax.annotation.Nullable;

import net.avcompris.tools.diagrammer.SVGDiagrammer;

import com.avcompris.lang.NotImplementedException;
import com.google.common.collect.Iterables;

public class DependencyDiagrammer {

	private final DependencyAnalysis analysis;

	private final int levelCount;

	private final int maxModuleCountOnAnyLevel;

	public DependencyDiagrammer(final DependencyAnalysis analysis) {

		this.analysis = checkNotNull(analysis, "analysis");

		levelCount = analysis.sizeOfModuleLevels();

		maxModuleCountOnAnyLevel = calculateMaxModuleCountOnAnyLevel(analysis);
	}

	private static int calculateMaxModuleCountOnAnyLevel(
			final DependencyAnalysis analysis) {

		int maxModuleCountOnAnyLevel = 1;

		for (final Collection<String> modulesOnLevel : analysis
				.getModuleLevels()) {

			final int moduleCountOnThisLevel = modulesOnLevel.size();

			if (moduleCountOnThisLevel > maxModuleCountOnAnyLevel) {

				maxModuleCountOnAnyLevel = moduleCountOnThisLevel;
			}
		}

		switch (maxModuleCountOnAnyLevel) {

		case 0:
		case 1:
		case 2:
		case 3:

			return maxModuleCountOnAnyLevel + 1; // give more room

		default:

			return maxModuleCountOnAnyLevel;
		}
	}

	private static final int WIDTH = 150; // TODO parameterize this
	private static final int HEIGHT = 20;
	private static final int V_SPACE = 40;

	public ModulePosition[] drawTo(final boolean optimize, final File svgFile) throws IOException {

		final Set<ModulePosition> result = new HashSet<ModulePosition>();

		new SVGDiagrammer() {

			@Override
			protected void body() throws Exception {

				final Map<String, ModulePositionImpl> modulePoss = new HashMap<String, ModulePositionImpl>();

				int y = 10;

				// 1. FIRST DRAFT

				for (final Iterable<String> modulesOnLevel : analysis
						.getModuleLevels()) {

					int x = 10;

					for (final String moduleName : modulesOnLevel) {

						final ModulePositionImpl modulePos = new ModulePositionImpl(
								moduleName, x, y, WIDTH, HEIGHT);

						modulePoss.put(moduleName, modulePos);

						x += WIDTH + 10;
					}

					y += 60;
				}

				// 2. SECOND THOUGHT

				loop: do {

					for (final ModulePositionImpl modulePos : modulePoss
							.values()) {

						final Collection<String> upstreams = analysis
								.getDirectUpstreams(modulePos.moduleName);

						if (upstreams.size() != 1) {
							continue;
						}

						final String upstream = upstreams.iterator().next();

						final Collection<String> downstreams = analysis
								.getDirectDownstreams(upstream);

						if (downstreams.size() != 1) {
							continue;
						}

						final ModulePositionImpl upstreamPosition = modulePoss
								.get(upstream);

						final ModulePositionImpl current = getModulePositionAtXY(
								modulePoss, upstreamPosition.x, modulePos.y);

						if (current != null && current != modulePos) {

							swapModulePositions(modulePoss, current, modulePos);

							continue loop;
						}
					}

				} while (false);

				// 3. METRICS

				final DiagramMetrics metrics = calculateMetrics(modulePoss);

				System.out.println(metrics);

				final Map<String, ModulePositionImpl> modulePoss2 = optimize ? attainMinimumMetrics(modulePoss)
						: modulePoss;

				final DiagramMetrics metrics2 = calculateMetrics(modulePoss2);

				System.out.println(metrics2);

				// 9. DRAWING

				// 9.1. RECTS

				for (final ModulePositionImpl modulePos : modulePoss2.values()) {

					result.add(modulePos);

					final String moduleName = modulePos.moduleName;

					rect().x(modulePos.x - 0.5).y(modulePos.y - 0.5)
							.width(modulePos.width).height(modulePos.height)
							.stroke("#000").fill("#ffc").close();

					text(moduleName).x(modulePos.x + modulePos.width / 2)
							.y(modulePos.y + 13).textAnchor("middle")
							.fill("#000").fontFamily("Helvetica").fontSize(11).
							//property("shape-rendering","crispEdges").
							close();
				}

				// 9.2. LINES

				for (final Line line : calculateLines(modulePoss2)) {

					line().x1(line.x1).y1(line.y1).x2(line.x2).y2(line.y2)
							.stroke("#000").close();
				}
			}

		}.addOutputFile(svgFile)
				.printToSystemOut(false)
				.run(10 + maxModuleCountOnAnyLevel * (WIDTH + 10),
						20 + levelCount * HEIGHT + (levelCount - 1) * V_SPACE);

		// END

		return Iterables.toArray(result, ModulePosition.class);
	}

	@Nullable
	private static ModulePositionImpl getModulePositionAtXY(
			final Map<String, ModulePositionImpl> modulePoss, final int x,
			final int y) {

		for (final ModulePositionImpl modulePos : modulePoss.values()) {

			if (modulePos.x == x && modulePos.y == y) {

				return modulePos;
			}
		}

		return null;
	}

	private static void swapModulePositions(
			final Map<String, ModulePositionImpl> modulePoss,
			final ModulePositionImpl modulePos1,
			final ModulePositionImpl modulePos2) {

		modulePoss.put(modulePos1.moduleName, new ModulePositionImpl(
				modulePos1.moduleName, modulePos2));

		modulePoss.put(modulePos2.moduleName, new ModulePositionImpl(
				modulePos2.moduleName, modulePos1));
	}

	private static class ModulePositionImpl implements ModulePosition {

		public final String moduleName;
		public final int x;
		public final int y;
		public final int width;
		public final int height;
		public final int middleX;
		public final int top;
		public final int bottom;

		private ModulePositionImpl(
				final String moduleName,
				final int x,
				final int y,
				final int width,
				final int height) {

			this.moduleName = checkNotNull(moduleName, "moduleName");
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;

			middleX = x + width / 2;
			top = y - 1;
			bottom = y + height;
		}

		public ModulePositionImpl(
				final String moduleName,
				final ModulePositionImpl modulePos2) {

			this(moduleName, modulePos2.x, modulePos2.y, modulePos2.width,
					modulePos2.height);
		}

		public ModulePositionImpl(
				final ModulePositionImpl modulePos2,
				final int x) {

			this(modulePos2.moduleName, x, modulePos2.y, modulePos2.width,
					modulePos2.height);
		}

		@Override
		public int hashCode() {

			return moduleName.hashCode();
		}

		@Override
		public boolean equals(@Nullable final Object o) {

			if (o == null || !ModulePositionImpl.class.equals(o)) {
				return false;
			}

			return moduleName.equals(((ModulePositionImpl) o).moduleName);
		}

		@Override
		public String toString() {

			return moduleName;
		}

		@Override
		public String getModuleName() {

			return moduleName;
		}

		@Override
		public int getX() {

			return x;
		}

		@Override
		public int getY() {

			return y;
		}
	}

	private DiagramMetrics calculateMetrics(
			final Map<String, ModulePositionImpl> modulePoss) {

		int howManyLinesCross = 0;
		double slopeScore = 0.0;

		final Iterable<Line> lines = calculateLines(modulePoss);

		for (final Line line1 : lines) {

			final int line1_dx = line1.x2 - line1.x1;

			slopeScore += (line1_dx * line1_dx);

			for (final Line line2 : lines) {

				if (linesCross(line1, line2)) {

					if (!linesCross(line2, line1)) {
						throw new RuntimeException("Lines should cross: "
								+ line1 + ", " + line2);
					}

					++howManyLinesCross;

				} else { // => (!linesCross(line1, line2))

					if (linesCross(line2, line1)) {
						throw new RuntimeException("Lines should not cross: "
								+ line1 + ", " + line2);
					}
				}
			}
		}

		howManyLinesCross /= 2; // Because each line was parsed as 1 and 2 

		double xWeight = 0.0;

		//for (final ModulePosition modulePos : modulePoss.values()) {
		//
		//	xWeight += modulePos.x * modulePos.x;
		//}

		return new DiagramMetrics(howManyLinesCross, slopeScore, xWeight);
	}

	private static boolean signumsEqual(final double dx1, final double dx2) {

		if (dx1 == 0 || dx2 == 0) {

			return true;
		}

		return Math.signum(dx1) == Math.signum(dx2);
	}

	/**
	 * return a weight, or 0.0 if the lines do not cross.
	 */
	static boolean linesCross(final Line line1, final Line line2) {

		if (line1 == line2 || line1.y2 <= line2.y1 || line2.y2 <= line1.y1) {
			return false;
		}

		if (line1.equals(line2)) {

			return true;

		} else if (line1.x1 == line2.x1 && line1.y1 == line2.y1) {

			return line1.hasSameSlope(line2);

		} else if (line1.x2 == line2.x2 && line1.y2 == line2.y2) {

			return line1.hasSameSlope(line2);

		} else if (line1.y1 == line2.y1) {

			if (line1.y2 == line2.y2) {

				return signumsEqual(line1.x1 - line2.x1, line2.x2 - line1.x2);

			} else if (line1.y2 > line2.y2) {

				final double extrapLine1_x2 = line1.x1 + (line1.x2 - line1.x1)
						* (line2.y2 - line2.y1)
						/ (double) (line1.y2 - line1.y1);

				return signumsEqual(line1.x1 - line2.x1, line2.x2
						- extrapLine1_x2);

			} else { // => (line2.y2 > line1.y2)

				final double extrapLine2_x2 = line2.x1 + (line2.x2 - line2.x1)
						* (line1.y2 - line2.y1)
						/ (double) (line2.y2 - line2.y1);

				return signumsEqual(line1.x1 - line2.x1, extrapLine2_x2
						- line1.x2);
			}

		} else if (line1.y2 == line2.y2) {

			if (line1.y1 > line2.y1) {

				final double extrapLine2_x1 = line2.x2 - (line2.x2 - line2.x1)
						* (line1.y2 - line2.y1)
						/ (double) (line2.y2 - line2.y1);

				return signumsEqual(line1.x1 - extrapLine2_x1, line2.x2
						- line1.x2);

			} else { // => (line2.y1 > line1.y1)

				final double extrapLine1_x1 = line1.x2 - (line1.x2 - line1.x1)
						* (line2.y2 - line1.y1)
						/ (double) (line1.y2 - line1.y1);

				return signumsEqual(extrapLine1_x1 - line2.x1, line2.x2
						- line1.x2);
			}

		} else if (line1.y1 > line2.y1 && line2.y2 > line1.y1
				&& line1.y2 > line2.y2) {

			final double extrapLine1_x2 = line1.x1 + (line1.x2 - line1.x1)
					* (line2.y2 - line1.y1) / (double) (line1.y2 - line1.y1);

			final double extrapLine2_x1 = line2.x2 - (line2.x2 - line2.x1)
					* (line2.y2 - line1.y1) / (double) (line2.y2 - line2.y1);

			return signumsEqual(line1.x1 - extrapLine2_x1, line2.x2
					- extrapLine1_x2);

		} else if (line2.y1 > line1.y1 && line1.y2 > line2.y1
				&& line2.y2 > line1.y2) {

			final double extrapLine2_x2 = line2.x1 + (line2.x2 - line2.x1)
					* (line1.y2 - line2.y1) / (double) (line2.y2 - line2.y1);

			final double extrapLine1_x1 = line1.x2 - (line1.x2 - line1.x1)
					* (line1.y2 - line2.y1) / (double) (line1.y2 - line1.y1);

			return signumsEqual(extrapLine1_x1 - line2.x1, extrapLine2_x2
					- line1.x2);

		} else if (line1.y1 > line2.y1 && line2.y2 > line1.y2) {

			final double extrapLine2_x1 = line2.x1 + (line2.x2 - line2.x1)
					* (line1.y1 - line2.y1) / (double) (line2.y2 - line2.y1);

			final double extrapLine2_x2 = line2.x2 - (line2.x2 - line2.x1)
					* (line2.y2 - line1.y2) / (double) (line2.y2 - line2.y1);

			return signumsEqual(line1.x1 - extrapLine2_x1, extrapLine2_x2
					- line1.x2);

		} else if (line2.y1 > line1.y1 && line1.y2 > line2.y2) {

			final double extrapLine1_x1 = line1.x1 + (line1.x2 - line1.x1)
					* (line2.y1 - line1.y1) / (double) (line1.y2 - line1.y1);

			final double extrapLine1_x2 = line1.x2 - (line1.x2 - line1.x1)
					* (line1.y2 - line2.y2) / (double) (line1.y2 - line1.y1);

			return signumsEqual(line2.x1 - extrapLine1_x1, extrapLine1_x2
					- line2.x2);

		} else {

			throw new NotImplementedException(line1 + ", " + line2);
		}
	}

	private static class DiagramMetrics {

		public final double howManyLinesCross;
		public final double slopeScore;
		public final double xWeight;

		public DiagramMetrics(
				final double howManyLinesCross,
				final double slopeScore,
				final double xWeight) {

			this.howManyLinesCross = howManyLinesCross;
			this.slopeScore = slopeScore;
			this.xWeight = xWeight;
		}

		@Override
		public String toString() {

			return "{howManyLinesCross: " + howManyLinesCross
					+ ", slopeScore: " + slopeScore + ", xWeight: " + xWeight
					+ "}";
		}

		public boolean hasLessLineCrossingsThan(final DiagramMetrics metrics2) {

			return howManyLinesCross < metrics2.howManyLinesCross;
		}

		public boolean isBetterThan(final DiagramMetrics metrics2) {

			if (howManyLinesCross < metrics2.howManyLinesCross) {

				return true;

			} else if (howManyLinesCross == metrics2.howManyLinesCross) {

				if (slopeScore < metrics2.slopeScore) {

					return true;

				} else if (slopeScore == metrics2.slopeScore) {

					if (xWeight < metrics2.xWeight) {

						return true;
					}
				}
			}

			return false;
		}
	}

	private Iterable<Line> calculateLines(
			final Map<String, ModulePositionImpl> modulePoss) {

		final List<Line> lines = new ArrayList<Line>();

		for (final ModulePositionImpl modulePos : modulePoss.values()) {

			final String moduleName = modulePos.moduleName;

			for (final String upstream : analysis
					.getDirectUpstreams(moduleName)) {

				final ModulePositionImpl upstreamPosition = modulePoss
						.get(upstream);

				lines.add(new Line(modulePos.middleX, modulePos.top,
						upstreamPosition.middleX, upstreamPosition.bottom));
			}
		}

		return lines;
	}

	static class Line {

		@Override
		public String toString() {

			return "(" + x1 + "," + y1 + ")-(" + x2 + "," + y2 + ")";
		}

		public final int x1;
		public final int y1;
		public final int x2;
		public final int y2;

		public Line(final int x1, final int y1, final int x2, final int y2) {

			// Make y1 always the higher one in the diagram.

			if (y1 > y2) {

				this.x1 = x2;
				this.y1 = y2;
				this.x2 = x1;
				this.y2 = y1;

			} else if (y1 == y2) {

				throw new IllegalArgumentException(
						"y1 should always be different than y2, but was: ("
								+ x1 + "," + y1 + ")-(" + x2 + "," + y2 + ")");

			} else {

				this.x1 = x1;
				this.y1 = y1;
				this.x2 = x2;
				this.y2 = y2;
			}
		}

		@Override
		public int hashCode() {

			return x1 + x2 + y1 + y2;
		}

		@Override
		public boolean equals(@Nullable final Object o) {

			if (o == null || !Line.class.equals(o)) {
				return false;
			}

			final Line line = (Line) o;

			return x1 == line.x1 && y1 == line.y1 && x2 == line.x2
					&& y2 == line.y2;
		}

		public boolean hasSameSlope(final Line line) {

			checkNotNull(line, "line");

			return (line.x2 - line.x1) * (y2 - y1) == (x2 - x1)
					* (line.y2 - line.y1);
		}
	}

	private Map<String, ModulePositionImpl> attainMinimumMetrics(
			final Map<String, ModulePositionImpl> modulePoss) {

		final AtomicStampedReference<DiagramMetrics> metrics = new AtomicStampedReference<DiagramMetrics>(
				calculateMetrics(modulePoss), 0);

		final ModulePositionImpl[][] modulePosArray = new ModulePositionImpl[levelCount][];

		int i = 0;

		for (final Collection<String> modulesOnLevel : analysis
				.getModuleLevels()) {

			final int moduleCountOnThisLevel = modulesOnLevel.size();

			modulePosArray[i] = new ModulePositionImpl[moduleCountOnThisLevel];

			int j = 0;

			for (final String moduleName : modulesOnLevel) {

				modulePosArray[i][j] = modulePoss.get(moduleName);

				++j;
			}

			++i;
		}

		final AtomicStampedReference<Map<String, ModulePositionImpl>> placeHolder = new AtomicStampedReference<Map<String, ModulePositionImpl>>(
				modulePoss, 0);

		final Integer[][] posGrid = new Integer[levelCount][maxModuleCountOnAnyLevel];

		parse(modulePosArray, posGrid, 0, 0, metrics, placeHolder);

		return placeHolder.getReference();
	}

	private int count = 0;

	private static final int DELAY = 4000;

	private long next = System.currentTimeMillis() + DELAY;

	private void parse(
			final ModulePositionImpl[][] modulePosArray,
			final Integer[][] posGrid,
			final int level,
			final int i,
			final AtomicStampedReference<DiagramMetrics> metrics,
			final AtomicStampedReference<Map<String, ModulePositionImpl>> placeHolder) {

		if (level >= levelCount) {

			++count;

			if (System.currentTimeMillis() > next) {

				System.out.println(count + "...");

				next = System.currentTimeMillis() + DELAY;
			}

			final Map<String, ModulePositionImpl> modulePoss2 = new HashMap<String, ModulePositionImpl>();

			for (int y = 0; y < levelCount; ++y) {

				for (int x = 0; x < maxModuleCountOnAnyLevel; ++x) {

					final Integer pos = posGrid[y][x];

					if (pos == null) {
						continue;
					}

					final ModulePositionImpl modulePos = modulePosArray[y][pos];

					modulePoss2.put(modulePos.moduleName,
							new ModulePositionImpl(modulePos, 10 + x
									* (WIDTH + 10)));
				}
			}

			final DiagramMetrics metrics2 = calculateMetrics(modulePoss2);

			final int[] metricsStamp = new int[1];

			synchronized (placeHolder) {

				while (true) {

					final DiagramMetrics metrics0 = metrics.get(metricsStamp);

					if (metrics2.isBetterThan(metrics0)) {

						final boolean compareAndSet = metrics.compareAndSet(
								metrics0, metrics2, metricsStamp[0],
								metricsStamp[0] + 1);

						if (!compareAndSet) {

							continue;
						}

						System.out.println(metrics2);

						final int placeHolderStamp = placeHolder.getStamp();

						placeHolder.set(modulePoss2, placeHolderStamp + 2);
					}

					break;
				}
			}

			return;
		}

		if (i >= maxModuleCountOnAnyLevel) {

			parse(modulePosArray, posGrid, level + 1, 0, metrics, placeHolder);

			return;
		}

		final Map<String, ModulePositionImpl> partialModulePositions = new HashMap<String, ModulePositionImpl>();

		for (int y = 0; y <= level; ++y) {

			for (int x = 0; x < maxModuleCountOnAnyLevel; ++x) {

				if (y == level && x == i) {

					break;
				}

				final Integer pos = posGrid[y][x];

				if (pos == null) {
					continue;
				}

				final ModulePositionImpl modulePos = modulePosArray[y][pos];

				partialModulePositions
						.put(modulePos.moduleName, new ModulePositionImpl(
								modulePos, 10 + x * (WIDTH + 10)));
			}
		}

		final DiagramMetrics partialMetrics = calculateMetrics(partialModulePositions);

		// Perform the recursive parsing only if we have hope that it 
		// will be better than what we already have.

		if (metrics.getReference().hasLessLineCrossingsThan(partialMetrics)) {

			return;
		}

		final List<Integer> remaining = new ArrayList<Integer>();

		for (int j = 0; j < modulePosArray[level].length; ++j) {

			remaining.add(j);
		}

		int howManyNulls = 0;

		for (int j = 0; j < i; ++j) {

			final Integer pos = posGrid[level][j];

			if (pos == null) {

				++howManyNulls;

			} else {

				remaining.remove(pos);
			}
		}

		if (howManyNulls < maxModuleCountOnAnyLevel
				- modulePosArray[level].length) {

			remaining.add(-1);
		}

		for (final Integer r : remaining) {

			posGrid[level][i] = (r == -1) ? null : r;

			parse(modulePosArray, posGrid, level, i + 1, metrics, placeHolder);
		}
	}
}
