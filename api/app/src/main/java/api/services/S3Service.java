package api.services;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.regions.Region;

import java.util.concurrent.CompletableFuture;


public class S3Service {

    private static S3AsyncClient s3;

    public static S3AsyncClient getS3Client() {
        if (s3 == null) {
            s3 = S3AsyncClient.builder()
                    .region(Region.US_WEST_2)
                    .build();
        }
        return s3;
    }

    public static CompletableFuture<PutObjectResponse> uploadFileBytes(String bucket, String key, byte[] file) {
        getS3Client();
        PutObjectRequest s3Request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        return s3.putObject(s3Request, AsyncRequestBody.fromBytes(file));
    }

    public static CompletableFuture<ResponseBytes<GetObjectResponse>> downloadFileBytes(String bucket, String key) {
        getS3Client();
        GetObjectRequest s3Request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        return s3.getObject(s3Request, AsyncResponseTransformer.toBytes());
    }

    public static CompletableFuture<DeleteObjectResponse> deleteFile(String bucket, String key) {
        getS3Client();
        DeleteObjectRequest s3Request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        return s3.deleteObject(s3Request);
    }
    
}
