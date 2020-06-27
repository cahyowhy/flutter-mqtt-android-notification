package com.example.cahyo_research_two_bg_service;

class PluginRegistrantException extends RuntimeException {
    public PluginRegistrantException() {
        super(
                "PluginRegistrantCallback is not set. Did you forget to call "
                        + "AlarmService.setPluginRegistrant? See the README for instructions.");
    }
}