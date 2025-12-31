package com.pastebinlite.model;

public class PasteResult {
    private final Paste paste;
    private final String errorMessage;

    private PasteResult(Paste paste, String errorMessage) {
        this.paste = paste;
        this.errorMessage = errorMessage;
    }


    public static PasteResult success(Paste paste) {
        return new PasteResult(paste, null);
    }


    public static PasteResult error(String message) {
        return new PasteResult(null, message);
    }


    public boolean isSuccess() {
        return paste != null;
    }

    public boolean isError() {
        return errorMessage != null;
    }


    public Paste getPaste() {
        return paste;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}