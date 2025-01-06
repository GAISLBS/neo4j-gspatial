package org.neo4j.gspatial;//package org.neo4j.gspatial;
//
//import org.junit.jupiter.api.AfterAll;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInstance;
//import org.neo4j.driver.Driver;
//import org.neo4j.driver.GraphDatabase;
//import org.neo4j.gspatial.procedures.SpatialProcedures;
//import org.neo4j.harness.Neo4j;
//import org.neo4j.harness.Neo4jBuilders;
//
///**
// * This class tests the spatial functions.
// * It sets up a Neo4j database with spatial procedures, loads test data, and executes various spatial operations.
// */
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//public class SpatialFunctionsTest {
//
//    private Neo4j embeddedDatabaseServer;
//    private Driver driver;
//
//    /**
//     * Sets up the test environment.
//     * Initializes the Neo4j database and loads the test data.
//     */
//    @BeforeAll
//    void setup() {
//        initializeNeo4jDatabase();
//        this.driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
//        setupTestData();
//    }
//
//    /**
//     * Initializes the Neo4j database.
//     * The database is set up with spatial procedures.
//     */
//    private void initializeNeo4jDatabase() {
//        this.embeddedDatabaseServer = Neo4jBuilders.newInProcessBuilder()
//                .withDisabledServer()
//                .withProcedure(SpatialProcedures.class)
//                .build();
//    }
//
//    /**
//     * Loads the test data into the Neo4j database.
//     */
//    private void setupTestData() {
//        TestIOUtility.loadData(this.driver, "AgendaArea");
//        TestIOUtility.loadData(this.driver, "Apartment");
//        TestIOUtility.loadData(this.driver, "GoodWayToWalk");
//    }
//
//    /**
//     * Tests the 'EQUALS' operation.
//     * The operation is performed between nodes of type 'AgendaArea'.
//     * The last argument is a boolean indicating whether operation takes single argument.
//     */
//    @Test
//    void testEquals() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "AgendaArea", "equals", false);
//    }
//
//    /**
//     * Tests the 'DISJOINT' operation.
//     * The operation is performed between nodes of type 'AgendaArea'.
//     * The last argument is a boolean indicating whether operation takes single argument.
//     */
//    @Test
//    void testDisjoint() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "AgendaArea", "disjoint", false);
//    }
//
//    /**
//     * Tests the 'INTERSECTS' operation.
//     * The operation is performed between nodes of type 'AgendaArea'.
//     * The last argument is a boolean indicating whether operation takes single argument.
//     */
//    @Test
//    void testIntersects() {
//        TestOperationUtility.executeOperation(this.driver, "Apartment", "AgendaArea", "intersects", false);
//    }
//
//    /**
//     * Tests the 'TOUCHES' operation.
//     * The operation is performed between nodes of type 'AgendaArea'.
//     * The last argument is a boolean indicating whether operation takes single argument.
//     */
//    @Test
//    void testCovers() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "AgendaArea", "covers", false);
//    }
//
//    /**
//     * Tests the 'COVERED_BY' operation.
//     * The operation is performed between nodes of type 'AgendaArea'.
//     * The last argument is a boolean indicating whether operation takes single argument.
//     */
//    @Test
//    void testCovered_By() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "AgendaArea", "covered_by", false);
//    }
//
//    /**
//     * Tests the 'CROSSES' operation.
//     * The operation is performed between nodes of type 'AgendaArea'.
//     * The last argument is a boolean indicating whether operation takes single argument.
//     */
//    @Test
//    void testCrosses() {
//        TestOperationUtility.executeOperation(this.driver, "GoodWayToWalk", "AgendaArea", "crosses", false);
//    }
//
//    /**
//     * Tests the 'WITHIN' operation.
//     * The operation is performed between nodes of type 'AgendaArea'.
//     * The last argument is a boolean indicating whether operation takes single argument.
//     */
//    @Test
//    void testOverlaps() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "AgendaArea", "overlaps", false);
//    }
//
//    /**
//     * Tests the 'CONTAINS' operation.
//     * The operation is performed between nodes of type 'AgendaArea'.
//     * The last argument is a boolean indicating whether operation takes single argument.
//     */
//    @Test
//    void testWithin() {
//        TestOperationUtility.executeOperation(this.driver, "Apartment", "AgendaArea", "within", false);
//    }
//
//    /**
//     * Tests the 'CONTAINS' operation.
//     * The operation is performed between nodes of type 'AgendaArea'.
//     * The last argument is a boolean indicating whether operation takes single argument.
//     */
//    @Test
//    void testContains() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "AgendaArea", "contains", false);
//    }
//
//    /**
//     * Tests the 'TOUCHES' operation.
//     * The operation is performed between nodes of type 'AgendaArea'.
//     * The last argument is a boolean indicating whether operation takes single argument.
//     */
//    @Test
//    void testTouches() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "AgendaArea", "touches", false);
//    }
//
//    /**
//     * Tests the 'DISTANCE' operation.
//     * The operation is performed between nodes of type 'Apartment'.
//     * The last argument is a boolean indicating whether operation takes single argument.
//     */
//    @Test
//    void testDistance() {
//        TestOperationUtility.executeOperation(this.driver, "Apartment", "AgendaArea", "distance", false);
//    }
//
//    /**
//     * Tests the 'BUFFER' operation.
//     * The operation is performed between nodes of type 'AgendaArea'.
//     * The last argument is a boolean indicating whether operation takes single argument.
//     */
//    @Test
//    void testBuffer() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "0.2", "buffer", false);
//    }
//
//    /**
//     * Tests the 'INTERSECTION' operation.
//     * The operation is performed between nodes of type 'AgendaArea'.
//     * The last argument is a boolean indicating whether operation takes single argument.
//     */
//    @Test
//    void testIntersection() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "AgendaArea", "intersection", false);
//    }
//
//    /**
//     * Tests the 'DIFFERENCE' operation.
//     * The operation is performed between nodes of type 'AgendaArea'.
//     * The last argument is a boolean indicating whether operation takes single argument.
//     */
//    @Test
//    void testDifference() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "AgendaArea", "difference", false);
//    }
//
//    /**
//     * Tests the 'UNION' operation.
//     * The operation is performed between nodes of type 'AgendaArea'.
//     * The last argument is a boolean indicating whether operation takes single argument.
//     */
//    @Test
//    void testUnion() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "AgendaArea", "union", false);
//    }
//
//    /**
//     * Tests the 'AREA' operation.
//     * The operation is performed between nodes of type 'AgendaArea'.
//     * The last argument is a boolean indicating whether operation takes single argument.
//     */
//    @Test
//    void testArea() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "", "area", true);
//    }
//
//    /**
//     * Tests the 'LENGTH' operation.
//     * The operation is performed nodes of type 'AgendaArea'.
//     * The last argument is a boolean indicating whether operation takes single argument.
//     */
//    @Test
//    void testLength() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "", "length", true);
//    }
//
//    /**
//     * Tests the 'ENVELOPE' operation.
//     * The operation is performed nodes of type 'AgendaArea'.
//     * The last argument is a boolean indicating whether operation takes single argument.
//     */
//    @Test
//    void testEnvelope() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "", "envelope", true);
//    }
//
//    /**
//     * Tests the 'CONVEX_HULL' operation.
//     * The operation is performed nodes of type 'AgendaArea'.
//     * The last argument is a boolean indicating whether operation takes single argument.
//     */
//    @Test
//    void testConvex_Hull() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "", "convex_hull", true);
//    }
//
//    /**
//     * Tests the 'BOUNDARY' operation.
//     * The operation is performed nodes of type 'AgendaArea'.
//     * The last argument is a boolean indicating whether operation takes single argument.
//     */
//    @Test
//    void testBoundary() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "", "boundary", true);
//    }
//
//    /**
//     * Tests the 'CENTROID' operation.
//     * The operation is performed nodes of type 'AgendaArea'.
//     * The last argument is a boolean indicating whether operation takes single argument.
//     */
//    @Test
//    void testCentroid() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "", "centroid", true);
//    }
//
//    /**
//     * Tests the 'DIMENSION' operation.
//     * The operation is performed nodes of type 'AgendaArea'.
//     * The last argument is a boolean indicating whether operation takes single argument.
//     */
//    @Test
//    void testDimension() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "", "dimension", true);
//    }
//
//    /**
//     * Tests the 'SRID' operation.
//     * The operation is performed nodes of type 'AgendaArea'.
//     * The last argument is a boolean indicating whether operation takes single argument.
//     */
//    @Test
//    void testSRID() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "", "srid", true);
//    }
//
//    /**
//     * Closes the Neo4j driver and the embedded database server.
//     */
//    @AfterAll
//    void close() {
//        if (this.driver != null) {
//            this.driver.close();
//        }
//        this.embeddedDatabaseServer.close();
//    }
//
//}
