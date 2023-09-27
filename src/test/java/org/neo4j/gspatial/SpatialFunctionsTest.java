package org.neo4j.gspatial;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.gspatial.procedures.SpatialProcedures;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SpatialFunctionsTest {

    private Neo4j embeddedDatabaseServer;
    private Driver driver;

    @BeforeAll
    void setup() {
        initializeNeo4jDatabase();
        this.driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
        setupTestData();
    }

    private void initializeNeo4jDatabase() {
        this.embeddedDatabaseServer = Neo4jBuilders.newInProcessBuilder()
                .withDisabledServer()
                .withProcedure(SpatialProcedures.class)
                .build();
    }

    private void setupTestData() {
        TestIOUtility.loadData(this.driver, "AgendaArea");
        TestIOUtility.loadData(this.driver, "Apartment");
        TestIOUtility.loadData(this.driver, "GoodWayToWalk");
        TestOperationUtility.executeHashTree(this.driver, "AgendaArea");
        TestOperationUtility.executeHashTree(this.driver, "Apartment");
        TestOperationUtility.executeHashTree(this.driver, "GoodWayToWalk");
    }

    @Test
    void testEquals() {
        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "AgendaArea", "equals", false);
    }

    @Test
    void testDisjoint() {
        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "AgendaArea", "disjoint", false);
    }

    @Test
    void testIntersects() {
        TestOperationUtility.executeOperation(this.driver, "Apartment", "AgendaArea", "intersects", false);
    }

    @Test
    void testCovers() {
        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "AgendaArea", "covers", false);
    }

    @Test
    void testCovered_By() {
        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "AgendaArea", "covered_by", false);
    }

    @Test
    void testCrosses() {
        TestOperationUtility.executeOperation(this.driver, "GoodWayToWalk", "AgendaArea", "crosses", false);
    }

    @Test
    void testOverlaps() {
        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "AgendaArea", "overlaps", false);
    }

    @Test
    void testWithin() {
        TestOperationUtility.executeOperation(this.driver, "Apartment", "AgendaArea", "within", false);
    }

    @Test
    void testContains() {
        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "AgendaArea", "contains", false);
    }

    @Test
    void testTouches() {
        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "AgendaArea", "touches", false);
    }
//
//    @Test
//    void testDistance() {
//        TestOperationUtility.executeOperation(this.driver, "Apartment", "AgendaArea", "distance", false);
//    }
//
//    @Test
//    void testBuffer() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "0.2", "buffer", false);
//    }
//
//    @Test
//    void testIntersection() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "AgendaArea", "intersection", false);
//    }
//
////    @Test
////    void testDifference() {
////        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "AgendaArea", "difference", false);
////    }
////
////    @Test
////    void testUnion() {
////        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "AgendaArea", "union", false);
////    }
//
//    @Test
//    void testArea() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "", "area", true);
//    }
//
//    @Test
//    void testLength() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "", "length", true);
//    }
//
//    @Test
//    void testEnvelope() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "", "envelope", true);
//    }
//
//    @Test
//    void testConvex_Hull() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "", "convex_hull", true);
//    }
//
//    @Test
//    void testBoundary() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "", "boundary", true);
//    }
//
//    @Test
//    void testCentroid() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "", "centroid", true);
//    }
//
//    @Test
//    void testDimension() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "", "dimension", true);
//    }
//
//    @Test
//    void testSRID() {
//        TestOperationUtility.executeOperation(this.driver, "AgendaArea", "", "srid", true);
//    }
//
//    @Test
//    void testSetHashTrees() {
//        TestOperationUtility.executeHashTreeTest(this.driver, "AgendaArea");
//    }

    @AfterAll
    void close() {
        if (this.driver != null) {
            this.driver.close();
        }
        this.embeddedDatabaseServer.close();
    }

}
