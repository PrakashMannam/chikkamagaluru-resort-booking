package com.companya.resort;

import com.companya.resort.util.CsvExporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExporterTest {

    @Test
    @DisplayName("Escapes standard alphanumeric values without modification")
    void testStandardValues() {
        assertThat(CsvExporter.escapeCell("BKG-ABC12345")).isEqualTo("BKG-ABC12345");
        assertThat(CsvExporter.escapeCell("CONFIRMED")).isEqualTo("CONFIRMED");
        assertThat(CsvExporter.escapeCell("15000.00")).isEqualTo("15000.00");
    }

    @Test
    @DisplayName("Handles null and empty values cleanly")
    void testEmptyAndNullValues() {
        assertThat(CsvExporter.escapeCell(null)).isEqualTo("");
        assertThat(CsvExporter.escapeCell("")).isEqualTo("");
    }

    @Test
    @DisplayName("Encloses values containing commas in double quotes")
    void testCommas() {
        assertThat(CsvExporter.escapeCell("Hegde, Ananya")).isEqualTo("\"Hegde, Ananya\"");
        assertThat(CsvExporter.escapeCell("Estate, Chikmagalur, Karnataka")).isEqualTo("\"Estate, Chikmagalur, Karnataka\"");
    }

    @Test
    @DisplayName("Doubles embedded double quotes according to RFC 4180")
    void testEmbeddedDoubleQuotes() {
        assertThat(CsvExporter.escapeCell("Villa \"Whispering Pines\"")).isEqualTo("\"Villa \"\"Whispering Pines\"\"\"");
        assertThat(CsvExporter.escapeCell("\"Already Quoted\"")).isEqualTo("\"\"\"Already Quoted\"\"\"");
    }

    @Test
    @DisplayName("Encloses values containing newlines and carriage returns")
    void testNewlinesAndCarriageReturns() {
        assertThat(CsvExporter.escapeCell("Line 1\nLine 2")).isEqualTo("\"Line 1\nLine 2\"");
        assertThat(CsvExporter.escapeCell("Line 1\r\nLine 2")).isEqualTo("\"Line 1\r\nLine 2\"");
    }

    @Test
    @DisplayName("Preserves UTF-8 and Unicode characters accurately")
    void testUnicodeText() {
        String kannadaName = "ಪ್ರಕಾಶ್ ಹೆಗ್ಡೆ";
        assertThat(CsvExporter.escapeCell(kannadaName)).isEqualTo(kannadaName);

        String nameWithComma = "ರಾಮ್, ಕುಮಾರ್";
        assertThat(CsvExporter.escapeCell(nameWithComma)).isEqualTo("\"ರಾಮ್, ಕುಮಾರ್\"");
    }

    @Test
    @DisplayName("Neutralizes formula injection starting with '='")
    void testFormulaInjectionEquals() {
        String injection = "=cmd|' /C calc'!A0";
        assertThat(CsvExporter.escapeCell(injection)).isEqualTo("'=cmd|' /C calc'!A0");

        String sumFormula = "=SUM(1+1)";
        assertThat(CsvExporter.escapeCell(sumFormula)).isEqualTo("'=SUM(1+1)");
    }

    @Test
    @DisplayName("Neutralizes formula injection starting with '+'")
    void testFormulaInjectionPlus() {
        String phone = "+919876543210";
        assertThat(CsvExporter.escapeCell(phone)).isEqualTo("'+919876543210");
    }

    @Test
    @DisplayName("Neutralizes formula injection starting with '-'")
    void testFormulaInjectionMinus() {
        String discount = "-1000";
        assertThat(CsvExporter.escapeCell(discount)).isEqualTo("'-1000");
    }

    @Test
    @DisplayName("Neutralizes formula injection starting with '@'")
    void testFormulaInjectionAt() {
        String atMention = "@SUM(A1:A5)";
        assertThat(CsvExporter.escapeCell(atMention)).isEqualTo("'@SUM(A1:A5)");
    }

    @Test
    @DisplayName("Neutralizes formula injection starting with tab and carriage return")
    void testFormulaInjectionTabAndCr() {
        String tabValue = "\tcmd";
        assertThat(CsvExporter.escapeCell(tabValue)).isEqualTo("'\tcmd");

        String crValue = "\rcmd";
        assertThat(CsvExporter.escapeCell(crValue)).isEqualTo("\"'\rcmd\"");
    }

    @Test
    @DisplayName("Builds complete CSV with RFC 4180 CRLF line endings")
    void testBuildCsv() {
        List<String> headers = List.of("Reference", "Guest Name", "Phone", "Total");
        List<List<String>> rows = List.of(
                List.of("BKG-001", "Ananya, H.", "+919876543210", "12500.00"),
                List.of("BKG-002", "=calc", "9876543211", "24000.00")
        );

        String csv = CsvExporter.buildCsv(headers, rows);
        String[] lines = csv.split("\r\n");

        assertThat(lines).hasSize(3);
        assertThat(lines[0]).isEqualTo("Reference,Guest Name,Phone,Total");
        assertThat(lines[1]).isEqualTo("BKG-001,\"Ananya, H.\",'+919876543210,12500.00");
        assertThat(lines[2]).isEqualTo("BKG-002,'=calc,9876543211,24000.00");
    }
}
