package org.neo4j.gspatial;

import java.util.function.Function;

public enum RequiredColumns {
    IDX("idx", Integer::parseInt),
    N_IDX("n.idx", Long::parseLong),
    M_IDX("m.idx", Long::parseLong),
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
    GEOMETRY("geometry", value -> String.format("'%s'", value.replace("\"", "")));

    private final String columnName;
    private final Function<String, Object> converter;

    RequiredColumns(String columnName, Function<String, Object> converter) {
        this.columnName = columnName;
        this.converter = converter;
    }

    public String getColumnName() {
        return columnName;
    }

    public Function<String, Object> getConverter() {
        return converter;
    }
}
