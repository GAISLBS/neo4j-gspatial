package org.neo4j.gspatial;//package org.neo4j.gspatial;
//
//import org.junit.jupiter.api.AfterAll;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import org.neo4j.driver.AuthTokens;
//import org.neo4j.driver.Driver;
//import org.neo4j.driver.GraphDatabase;
//
//
//public class RTreeQueryTest {
//
//    private static Driver driver;
//
//    @BeforeAll
//    static void initializeDatabaseConnection() {
//        String uri = "bolt://localhost:7687";
//        String user = "neo4j";
//        String password = "neo4j0000";
//        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
//    }
//
//    @Test
//    void testRangeBufferQuery() {
//        RTreeTestUtils.executeRangeQuery(driver, new String[]{"Building", "Highway", "LandUse", "Natural", "Place", "Shop"}, new double[]{-73.985428, 40.748817}, 0.1);
//    }
//
//    @Test
//    void testJoinQuery() {
//        RTreeTestUtils.executeJoinQuery(driver, "Highway", "Building", "contains");
//    }
//
//    @Test
//    void testKnnQuery() {
//        RTreeTestUtils.executeKnnQuery(driver, new String[]{"Building", "Highway", "LandUse", "Natural", "Place", "Shop"}, new double[]{-73.985428, 40.748817}, 125);
//    }
//
//    @Test
//    void testRangeBufferTraverseQuery() {
//        RTreeTestUtils.executeRangeTraverseQuery(driver, new String[]{"Building", "Highway", "LandUse", "Natural", "Place", "Shop"}, new double[]{-73.985428, 40.748817}, 0.1);
//    }
//
//    @Test
//    void testStrTreeRangeBufferTraverseQuery() {
//        RTreeTestUtils.executeStrTreeRangeTraverseQuery(driver, new String[]{"Building", "Highway", "LandUse", "Natural", "Place", "Shop"}, new double[]{-73.985428, 40.748817}, 0.1);
//    }
//
//    @AfterAll
//    static void closeDriver() {
//        if (driver != null) {
//            driver.close();
//        }
//    }
//}
