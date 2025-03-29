package api.models;

public record Paste(
    String  fileContent,
    int     storeDays
) {
    
}
