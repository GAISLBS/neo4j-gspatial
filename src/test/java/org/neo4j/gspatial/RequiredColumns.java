package org.neo4j.gspatial;

import java.util.function.Function;

/**
 * This enum represents the required columns for spatial operations.
 * Each enum value is associated with a column name and a converter function that converts the column value to the appropriate format.
 */
public enum RequiredColumns {
    // The 'idx' column, which is converted to an Integer
    IDX("idx", Integer::parseInt),
    // The 'n.idx' column, which is converted to a Long
    N_IDX("n.idx", Long::parseLong),
    // The 'm.idx' column, which is converted to a Long
    M_IDX("m.idx", Long::parseLong),
    // The 'result' column, which is converted to a Long, Double, or String as appropriate
    RESULT("result", value -> {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException ex) {
                return value;
            }
        }
    }),
    // The 'geometry' column, which is converted to a String with quotes removed
    GEOMETRY("geometry", value -> String.format("'%s'", value.replace("\"", "")));

    private final String columnName;
    private final Function<String, Object> converter;

    RequiredColumns(String columnName, Function<String, Object> converter) {
        this.columnName = columnName;
        this.converter = converter;
    }

    /**
     * Returns the name of the column associated with this enum value.
     *
     * @return the name of the column
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * Returns the converter function associated with this enum value.
     * The converter function converts the column value to the appropriate format.
     *
     * @return the converter function
     */
    public Function<String, Object> getConverter() {
        return converter;
    }
}
