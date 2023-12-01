package org.neo4j.gspatial;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class represents the result of reading a CSV file.
 * It includes a list of data maps and a list of headers.
 */
class CSVResult {
    List<Map<String, String>> dataList;
    List<String> headers;

    public CSVResult(List<Map<String, String>> dataList, List<String> headers) {
        this.dataList = dataList;
        this.headers = headers;
    }
}

/**
 * This class provides a method for reading a CSV file.
 * It includes a method for reading a CSV file into a CSVResult object.
 */
class CSVReader {
    private static final String DELIMITER = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";

    /**
     * Reads a CSV file and returns a CSVResult object.
     *
     * @param filePath        the path of the CSV file
     * @param requiredColumns the columns to include in the result
     * @return a CSVResult object representing the CSV file
     * @throws IOException if an I/O error occurs
     */
    public CSVResult read(String filePath, Set<String> requiredColumns) throws IOException {
        List<Map<String, String>> dataList = new ArrayList<>();
        List<String> headers;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            headers = Arrays.asList(br.readLine().split(DELIMITER, -1));

            List<Integer> requiredIndexes = headers.stream()
                    .filter(requiredColumns::contains)
                    .map(headers::indexOf)
                    .collect(Collectors.toList());

            String line;
            while ((line = br.readLine()) != null) {
                dataList.add(parseLine(headers, line, requiredIndexes));
            }
        }
        return new CSVResult(dataList, headers);
    }

    /**
     * Parses a line from a CSV file and returns a map of the required columns and their values.
     *
     * @param headers         the headers of the CSV file
     * @param line            the line to parse
     * @param requiredIndexes the indexes of the required columns
     * @return a map of the required columns and their values
     */
    private Map<String, String> parseLine(List<String> headers, String line, List<Integer> requiredIndexes) {
        String[] values = line.split(DELIMITER, -1);
        Map<String, String> dataMap = new HashMap<>();
        for (int index : requiredIndexes) {
            dataMap.put(headers.get(index), values[index]);
        }
        return dataMap;
    }
}

/**
 * This class provides a method for writing data to a CSV file.
 */
class CSVWriter {
    private static final String RESOURCE_PATH = "src/test/resources";

    /**
     * Writes the given data to a CSV file.
     *
     * @param fileName the name of the CSV file
     * @param dataList the data to write
     * @throws RuntimeException if an I/O error occurs
     */
    public void write(String fileName, List<Map<String, Object>> dataList) {
        try (FileWriter writer = new FileWriter(Paths.get(RESOURCE_PATH, fileName).toString())) {
            if (!dataList.isEmpty()) {
                String header = String.join(",", dataList.get(0).keySet());
                writer.write(header + "\n");
            }
            for (Map<String, Object> data : dataList) {
                String row = data.values().stream()
                        .map(value -> {
                            String str = value.toString();
                            if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
                                str = "\"" + str.replace("\"", "\"\"") + "\"";
                            }
                            return str;
                        })
                        .collect(Collectors.joining(","));
                writer.write(row + "\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write data: " + e.getMessage());
        }
    }
}

/**
 * This class provides a method for loading data into a Neo4j database.
 */
class Neo4jLoader {
    /**
     * Loads the given data into a Neo4j database.
     *
     * @param csvResult the data to load
     * @param label     the label of the nodes to create
     * @param session   the Neo4j session
     */
    public void loadToNeo4j(CSVResult csvResult, String label, Session session) {
        for (Map<String, String> data : csvResult.dataList) {
            String insertQuery = buildInsertQuery(label, data);
            session.run(insertQuery);
        }
    }

    /**
     * Builds a Cypher query for inserting a node with the given label and data.
     *
     * @param label the label of the node
     * @param data  the data of the node
     * @return the generated Cypher query
     */
    private String buildInsertQuery(String label, Map<String, String> data) {
        StringBuilder properties = new StringBuilder();
        for (RequiredColumns column : RequiredColumns.values()) {
            String header = column.getColumnName();
            String value = data.get(header);
            if (value != null) {
                Object convertedValue = column.getConverter().apply(value);
                properties.append(String.format("%s: %s, ", header, convertedValue));
            }
        }
        if (properties.length() > 0) {
            properties.setLength(properties.length() - 2);
        }
        return String.format("CREATE (a:%s {%s})", label, properties);
    }
}

/**
 * This class provides a method for converting data.
 */
class DataConverter {
    /**
     * Converts the given data to the appropriate format.
     *
     * @param data the data to convert
     * @return the converted data
     */
    public Map<String, Object> convert(Map<String, String> data) {
        Map<String, Object> convertedData = new HashMap<>();
        for (RequiredColumns column : RequiredColumns.values()) {
            String key = column.getColumnName();
            String value = data.get(key);
            if (value != null) {
                Object convertedValue = column.getConverter().apply(value);
                convertedData.put(key, convertedValue);
            }
        }
        return convertedData;
    }
}

/**
 * This class provides utility methods for loading data into a Neo4j database and converting data for comparison.
 */
public class TestIOUtility {
    private static final String RESOURCE_PATH = "src/test/resources";
    private static Set<String> requiredColumns = Arrays.stream(RequiredColumns.values())
            .map(RequiredColumns::getColumnName)
            .collect(Collectors.toSet());

    /**
     * Reads a CSV file and returns a CSVResult object.
     *
     * @param fileName        the name of the CSV file
     * @param requiredColumns the columns to include in the result
     * @return a CSVResult object representing the CSV file
     * @throws IOException if an I/O error occurs
     */
    public static CSVResult readCSVFile(String fileName, Set<String> requiredColumns) throws IOException {
        String csvFilePath = Paths.get(RESOURCE_PATH, fileName.toLowerCase() + ".csv").toString();
        CSVReader csvReader = new CSVReader();
        return csvReader.read(csvFilePath, requiredColumns);
    }

    /**
     * Loads data into a Neo4j database.
     *
     * @param driver the Neo4j driver
     * @param label  the label of the nodes to create
     */
    public static void loadData(Driver driver, String label) {
        try (Session session = driver.session()) {
            CSVResult csvResult = readCSVFile(label, requiredColumns);
            Neo4jLoader loader = new Neo4jLoader();
            loader.loadToNeo4j(csvResult, label, session);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load data: " + e.getMessage());
        }
    }

    /**
     * Loads data for comparison.
     *
     * @param fileName the name of the CSV file
     * @return a list of maps representing the data
     */
    public static List<Map<String, Object>> loadDataForComparison(String fileName) {
        try {
            CSVResult csvResult = readCSVFile(fileName, requiredColumns);

            DataConverter converter = new DataConverter();
            List<Map<String, Object>> dataList = new ArrayList<>();
            for (Map<String, String> data : csvResult.dataList) {
                dataList.add(converter.convert(data));
            }
            return dataList;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
