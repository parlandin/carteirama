package com.parlandim.carteirama.export;

import java.util.List;

public record Sheet(String name, List<String> headers, List<List<Object>> rows) {
    public record Money(long cents) {
    }
}
