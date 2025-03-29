package api.models;

public record PasteMetadata(
    String pk,
    String s3BucketName,
    String s3Path,
    long   createdTime,
    long   deleteTime
) {
    
}
