package api.services;

import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import api.models.PasteMetadata;

public class DynamoDbService {

    private final DynamoDbAsyncClient dynamoDbClient;

    public DynamoDbService() {
        this.dynamoDbClient = DynamoDbAsyncClient.builder()
                .region(Region.US_WEST_2)
                .build();
    }

    public CompletableFuture<PutItemResponse> postPasteMetadata(PasteMetadata metadata, String tableName) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("pk", AttributeValue.builder().s(metadata.pk()).build());
        item.put("s3_bucket_name", AttributeValue.builder().s(metadata.s3BucketName()).build());
        item.put("s3_path", AttributeValue.builder().s(metadata.s3Path()).build());
        item.put("created_time", AttributeValue.builder().n(String.valueOf(metadata.createdTime())).build());
        item.put("delete_time", AttributeValue.builder().n(String.valueOf(metadata.deleteTime())).build());

        PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName(tableName)  // Specify your DynamoDB table name
                .item(item)
                .build();

        return dynamoDbClient.putItem(putItemRequest);
    }
    
}
