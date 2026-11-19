package com.parlandim.carteirama.export;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

final class SheetWriters {

    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private SheetWriters() {
    }

    static byte[] csv(Sheet sheet) {
        StringBuilder sb = new StringBuilder("﻿");
        appendLine(sb, sheet.headers().stream().map(h -> (Object) h).toList());
        for (List<Object> row : sheet.rows()) {
            appendLine(sb, row);
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    static byte[] xlsx(Sheet sheet) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Workbook wb = new Workbook(out, "Carteirama", "1.0");
            Worksheet ws = wb.newWorksheet(sheet.name());
            for (int c = 0; c < sheet.headers().size(); c++) {
                ws.value(0, c, sheet.headers().get(c));
                ws.style(0, c).bold().set();
            }
            for (int r = 0; r < sheet.rows().size(); r++) {
                List<Object> row = sheet.rows().get(r);
                for (int c = 0; c < row.size(); c++) {
                    Object v = row.get(c);
                    int line = r + 1;
                    switch (v) {
                        case null -> { }
                        case Sheet.Money m -> {
                            ws.value(line, c, BigDecimal.valueOf(m.cents(), 2));
                            ws.style(line, c).format("#,##0.00").set();
                        }
                        case LocalDate d -> {
                            ws.value(line, c, d);
                            ws.style(line, c).format("dd/mm/yyyy").set();
                        }
                        case Number n -> ws.value(line, c, n);
                        default -> ws.value(line, c, v.toString());
                    }
                }
            }
            wb.finish();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void appendLine(StringBuilder sb, List<Object> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(escape(format(cells.get(i))));
        }
        sb.append("\r\n");
    }

    private static String format(Object v) {
        return switch (v) {
            case null -> "";
            case Sheet.Money m -> BigDecimal.valueOf(m.cents(), 2).toPlainString().replace('.', ',');
            case LocalDate d -> d.format(BR_DATE);
            default -> v.toString();
        };
    }

    private static String escape(String s) {
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        if (s.contains(";") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
