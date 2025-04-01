package api.services;

import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import api.models.PasteMetadata;

public class DynamoDbService {

    private static DynamoDbAsyncClient dynamoDbClient;

    public static DynamoDbAsyncClient getDynamoDbClient() {
        if (dynamoDbClient == null) {
            dynamoDbClient = DynamoDbAsyncClient.builder()
                    .region(Region.US_WEST_2)
                    .build();
        }
        return dynamoDbClient;
    }

    public static CompletableFuture<PutItemResponse> postPasteMetadata(PasteMetadata metadata, String tableName) {
        getDynamoDbClient();
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

    public static CompletableFuture<QueryResponse> getPasteMetadataByUuid(String tableName, String uuid) {
        getDynamoDbClient();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":pk", AttributeValue.builder().s(uuid).build());

        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tableName)  // Specify your DynamoDB table name
                .keyConditionExpression("pk = :pk")
                .expressionAttributeValues(expressionValues)
                .build();

        return dynamoDbClient.query(queryRequest);
    }

    public static CompletableFuture<QueryResponse> getExpiredPasteMetadata(String tableName, long datetime) {
        getDynamoDbClient();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":delete_time", AttributeValue.builder().n(String.valueOf(datetime)).build());

        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tableName)  // Specify your DynamoDB table name
                .keyConditionExpression("delete_time <= :delete_time")
                .expressionAttributeValues(expressionValues)
                .build();

        return dynamoDbClient.query(queryRequest);
    }

    public static CompletableFuture<DeleteItemResponse> deletePasteMetadataByUuid(String tableName, String uuid) {
        getDynamoDbClient();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put("pk", AttributeValue.builder().s(uuid).build());

        DeleteItemRequest queryRequest = DeleteItemRequest.builder()
                .tableName(tableName)  // Specify your DynamoDB table name
                .key(expressionValues)
                .build();

        return dynamoDbClient.deleteItem(queryRequest);
    }
    
}
