package api.services;

import software.amazon.awssdk.core.async.AsyncRequestBody;
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


    // // List all objects in the S3 bucket
    // public List<String> listObjects() {
    //     ListObjectsV2Request request = ListObjectsV2Request.builder()
    //             .bucket(bucketName)
    //             .build();
    //     ListObjectsV2Response response = s3.listObjectsV2(request);
        
    //     return response.contents().stream()
    //             .map(S3Object::key)
    //             .collect(Collectors.toList());
    // }

    // // Upload a file to S3
    // public void uploadFile(String key, String filePath) {
    //     PutObjectRequest request = PutObjectRequest.builder()
    //             .bucket(bucketName)
    //             .key(key)
    //             .build();
        
    //     s3.putObject(request, RequestBody.fromFile(new File(filePath)));
    //     System.out.println("File uploaded: " + key);
    // }

    // // Download a file from S3
    // public void downloadFile(String key, String destinationPath) {
    //     GetObjectRequest request = GetObjectRequest.builder()
    //             .bucket(bucketName)
    //             .key(key)
    //             .build();
        
    //     s3.getObject(request, Paths.get(destinationPath));
    //     System.out.println("File downloaded to: " + destinationPath);
    // }

    // // Delete a file from S3
    // public void deleteFile(String key) {
    //     DeleteObjectRequest request = DeleteObjectRequest.builder()
    //             .bucket(bucketName)
    //             .key(key)
    //             .build();
        
    //     s3.deleteObject(request);
    //     System.out.println("File deleted: " + key);
    // }
    
}
