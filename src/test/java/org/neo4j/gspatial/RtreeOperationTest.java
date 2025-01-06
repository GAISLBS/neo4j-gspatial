//package org.neo4j.gspatial;
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
// * This class tests the R-tree operations.
// * It sets up a Neo4j database with spatial procedures, and executes R-tree operations.
// */
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//public class RtreeOperationTest {
//
//    private Neo4j embeddedDatabaseServer;
//    private Driver driver;
//
//    @BeforeAll
//    void setup() {
//        initializeNeo4jDatabase();
//        this.driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
//        setupTestData();
//    }
//
//    private void initializeNeo4jDatabase() {
//        this.embeddedDatabaseServer = Neo4jBuilders
//                .newInProcessBuilder()
//                .withDisabledServer()
//                .withProcedure(SpatialProcedures.class)
//                .build();
//    }
//
//    private void setupTestData() {
//        TestIOUtility.loadData(this.driver, "AgendaArea");
//        TestIOUtility.loadData(this.driver, "Apartment");
////        TestIOUtility.loadData(this.driver, "GoodWayToWalk");
//        TestIOUtility.loadData(this.driver, "LandCoverMap1m");
//    }
//
//    /**
//     * These tests must be run separately. Because Test Operations cannot be ordered.
//     */
////    @Test
////    void testRtreeDeleteN() {
////        RTreeTestUtils.executeDeleteN(driver, "AgendaArea", "n.idx = 24");
////    }
////
////    @Test
////    void testRtreeDeleteAll() {
////        RTreeTestUtils.executeDeleteAll(driver, "AgendaArea");
////    }
//    @Test
//    void testRtreeInsert() {
//        RTreeTestUtils.executeInsert(driver, "Apartment", "AgendaArea");
//        RTreeTestUtils.verifyRootBoundingBoxValidity(driver, "AgendaArea");
//    }
//
//    @Test
//    void testDeleteALeafNode() {
//        RTreeTestUtils.executeInsert(driver, "Apartment", "AgendaArea");
//        RTreeTestUtils.executeDeleteALeafNode(driver, "AgendaArea");
//        RTreeTestUtils.verifyRootBoundingBoxValidity(driver, "AgendaArea");
//    }
//
//    @Test
//    void testRangeQuery() {
//        RTreeTestUtils.executeInsert(driver, "Apartment", "AgendaArea", "LandCoverMap1m");
//        RTreeTestUtils.executeRangeQuery(driver, new String[]{"Apartment", "AgendaArea", "LandCoverMap1m"}, new double[]{126.9776747745794, 127.003822009184, 37.48940647166388, 37.5139237487866});
//    }
//
//    @Test
//    void testRangeBufferQuery() {
//        RTreeTestUtils.executeInsert(driver, "Apartment", "AgendaArea");
//        RTreeTestUtils.executeRangeQuery(driver, new String[]{"Apartment", "AgendaArea"}, new double[]{127.003822009184, 37.5139237487866}, 0.01);
//    }
//
//    @Test
//    void testJoinQuery() {
//        RTreeTestUtils.executeInsert(driver, "LandCoverMap1m", "AgendaArea");
//        RTreeTestUtils.executeJoinQuery(driver, "LandCoverMap1m", "AgendaArea", "disjoint");
//    }
//
//    @Test
//    void testKnnQuery() {
//        RTreeTestUtils.executeInsert(driver, "Apartment", "AgendaArea", "LandCoverMap1m");
//        RTreeTestUtils.executeKnnQuery(driver, new String[]{"Apartment", "AgendaArea", "LandCoverMap1m"}, new double[]{127.003822009184, 37.48940647166388}, 5);
//    }
//
//    @Test
//    void testStrTreeInsert() {
//        RTreeTestUtils.executeStrTreeInsert(driver, "Apartment", "AgendaArea", "LandCoverMap1m");
//    }
//
//    @Test
//    void testStrTreeRangeQuery() {
//        RTreeTestUtils.executeStrTreeRangeQuery(driver, new String[]{"Apartment", "AgendaArea", "LandCoverMap1m"}, new double[]{126.9776747745794, 127.003822009184, 37.48940647166388, 37.5139237487866});
//    }
//
//    @Test
//    void testStrTreeRangeBufferQuery() {
//        RTreeTestUtils.executeStrTreeRangeQuery(driver, new String[]{"Apartment", "AgendaArea"}, new double[]{127.003822009184, 37.5139237487866}, 0.01);
//    }
//
//    @AfterAll
//    void close() {
//        if (this.driver != null) {
//            this.driver.close();
//        }
//        this.embeddedDatabaseServer.close();
//    }
//}
