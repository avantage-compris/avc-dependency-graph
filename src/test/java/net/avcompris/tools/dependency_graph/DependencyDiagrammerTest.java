package net.avcompris.tools.dependency_graph;

import static net.avcompris.tools.dependency_graph.DependencyDiagrammer.linesCross;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import net.avcompris.tools.dependency_graph.DependencyDiagrammer.Line;

import org.junit.Test;

public class DependencyDiagrammerTest {

	private static Line line(final int x1, final int y1, final int x2,
			final int y2) {

		return new Line(x1, y1, x2, y2);
	}

	private static void assertCross(final Line line1, final Line line2) {

		assertTrue(linesCross(line1, line2));
		assertTrue(linesCross(line2, line1));

		assertFalse(linesCross(line1, line1));
		assertFalse(linesCross(line2, line2));
	}

	private static void assertDontCross(final Line line1, final Line line2) {

		assertFalse(linesCross(line1, line2));
		assertFalse(linesCross(line2, line1));

		assertFalse(linesCross(line1, line1));
		assertFalse(linesCross(line2, line2));
	}

	@Test
	public void testLinesCross0() throws Exception {

		assertCross(line(50, 100, 50, 120), line(70, 100, 30, 120));
	}

	@Test
	public void testLinesCross1() throws Exception {

		assertDontCross(line(215, 150, 75, 249), line(75, 90, 75, 189));
	}

	@Test
	public void testLinesCross2() throws Exception {

		assertDontCross(line(215, 150, 75, 249), line(75, 90, 355, 189));
	}

	@Test
	public void testLinesCross3() throws Exception {

		assertCross(line(215, 150, 75, 189), line(75, 90, 355, 189));
	}

	@Test
	public void testLinesCross4() throws Exception {

		assertCross(line(90, 10, 90, 200), line(150, 10, 90, 130));
	}

	@Test
	public void testLinesCross5() throws Exception {

		assertDontCross(line(495, 210, 215, 249), line(75, 150, 75, 309));
	}

	@Test
	public void testLinesCrossVertical1() throws Exception {

		assertCross(line(10, 10, 10, 100), line(10, 10, 10, 50));
	}

	@Test
	public void testLinesCrossVertical2() throws Exception {

		assertCross(line(10, 50, 10, 100), line(10, 10, 10, 100));
	}
}
