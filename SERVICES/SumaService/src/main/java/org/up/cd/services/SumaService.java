package org.up.cd.services;

import java.nio.charset.StandardCharsets;

public class SumaService {
    public byte[] execute(byte[] input) {
        try {
            String expr = new String(input, StandardCharsets.UTF_8).trim();
            String[] parts = expr.split("\\+");
            double a = Double.parseDouble(parts[0].trim());
            double b = Double.parseDouble(parts[1].trim());
            double result = a + b;
            String out = (result == Math.floor(result)) ? String.valueOf((long) result) : String.valueOf(result);
            return out.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return ("ERROR: " + e.getMessage()).getBytes(StandardCharsets.UTF_8);
        }
    }
}
