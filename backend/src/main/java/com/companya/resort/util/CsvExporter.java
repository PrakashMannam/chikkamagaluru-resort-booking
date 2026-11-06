package com.companya.resort.util;

import java.util.List;

/**
 * Utility for producing RFC 4180 compliant CSVs with defense against
 * CSV Formula Injection (CWE-1236).
 */
public final class CsvExporter {

    private CsvExporter() {
    }

    /**
     * Sanitizes a cell value to prevent CSV Formula Injection (CWE-1236)
     * and applies RFC 4180 escaping.
     *
     * Dangerous leading characters for spreadsheet formula execution:
     * '=', '+', '-', '@', '\t', '\r'.
     *
     * Values starting with these characters are prefixed with a single quote (').
     * If the value contains commas, double quotes, LF or CR, it is wrapped in double quotes
     * with embedded double quotes doubled ("").
     */
    public static String escapeCell(String rawValue) {
        if (rawValue == null) {
            return "";
        }

        String value = rawValue;

        // Formula injection defense: detect leading dangerous formula characters
        if (!value.isEmpty()) {
            char firstChar = value.charAt(0);
            if (firstChar == '=' || firstChar == '+' || firstChar == '-' || firstChar == '@'
                    || firstChar == '\t' || firstChar == '\r') {
                value = "'" + value;
            }
        }

        // RFC 4180 escaping: check if quoting is required
        boolean needsQuotes = value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r");

        if (needsQuotes) {
            value = "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }

    /**
     * Builds a complete CSV formatted string using RFC 4180 CRLF line terminators.
     */
    public static String buildCsv(List<String> headers, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();

        if (headers != null && !headers.isEmpty()) {
            for (int i = 0; i < headers.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(escapeCell(headers.get(i)));
            }
            sb.append("\r\n");
        }

        if (rows != null) {
            for (List<String> row : rows) {
                for (int i = 0; i < row.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(escapeCell(row.get(i)));
                }
                sb.append("\r\n");
            }
        }

        return sb.toString();
    }
}
