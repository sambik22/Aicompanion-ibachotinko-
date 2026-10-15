package com.example.aicompanion.pig.talk;

/** Відповідь "мозку": що сказати і що зробити. */
public record PigDecision(String reply, PigAction action) {}
