package org.neo4j.gspatial;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Integration tests for R-Tree queries.
 * 
 * IMPORTANT: These tests require a running Neo4j instance.
 * Before running, create a .env file in the project root with:
 *   NEO4J_URI=bolt://localhost:7687
 *   NEO4J_USER=neo4j
 *   NEO4J_PASSWORD=your_password
 * 
 * See .env.example for reference.
 */
public class RTreeQueryTest {

    private static Driver driver;

    @BeforeAll
    static void initializeDatabaseConnection() {
        Properties props = loadEnvProperties();
        String uri = props.getProperty("NEO4J_URI", "bolt://localhost:7687");
        String user = props.getProperty("NEO4J_USER", "neo4j");
        String password = props.getProperty("NEO4J_PASSWORD", "");
        
        if (password.isEmpty()) {
            throw new IllegalStateException("NEO4J_PASSWORD not set. Please create a .env file. See .env.example for reference.");
        }
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
    }

    private static Properties loadEnvProperties() {
        Properties props = new Properties();
        Path envPath = Path.of(".env");
        if (Files.exists(envPath)) {
            try {
                props.load(Files.newBufferedReader(envPath));
            } catch (IOException e) {
                throw new RuntimeException("Failed to load .env file", e);
            }
        }
        return props;
    }

    @Test
    void testRangeBufferQuery() {
        RTreeTestUtils.executeRangeQuery(driver, new String[]{"Building", "Highway", "LandUse", "Natural", "Place", "Shop"}, new double[]{-73.985428, 40.748817}, 0.1);
    }

    @Test
    void testJoinQuery() {
        RTreeTestUtils.executeJoinQuery(driver, "Highway", "Building", "contains");
    }

    @Test
    void testKnnQuery() {
        RTreeTestUtils.executeKnnQuery(driver, new String[]{"Building", "Highway", "LandUse", "Natural", "Place", "Shop"}, new double[]{-73.985428, 40.748817}, 125);
    }

    @Test
    void testRangeBufferTraverseQuery() {
        RTreeTestUtils.executeRangeTraverseQuery(driver, new String[]{"Building", "Highway", "LandUse", "Natural", "Place", "Shop"}, new double[]{-73.985428, 40.748817}, 0.1);
    }

    @Test
    void testStrTreeRangeBufferTraverseQuery() {
        RTreeTestUtils.executeStrTreeRangeTraverseQuery(driver, new String[]{"Building", "Highway", "LandUse", "Natural", "Place", "Shop"}, new double[]{-73.985428, 40.748817}, 0.1);
    }

    @AfterAll
    static void closeDriver() {
        if (driver != null) {
            driver.close();
        }
    }
}
