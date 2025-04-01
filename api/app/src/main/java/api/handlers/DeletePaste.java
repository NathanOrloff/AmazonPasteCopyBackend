package api.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import api.constants.Constants;
import api.services.DynamoDbService;
import api.services.S3Service;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;

public class DeletePaste implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final String tableName = System.getenv(Constants.dynamodbTableNameEnv);
    private final String bucketName = System.getenv(Constants.s3BucketNameEnv);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            // get params
            Map<String, String> queryParams = request.getQueryStringParameters();

            // Check if query parameters exist
            if (queryParams == null || !queryParams.containsKey("uuid")) {
                throw new Exception("No uuid found in the request.");
            }

            // isolate uuid
            String uuid = queryParams.get("uuid");

            // get uuid metadata
            CompletableFuture<QueryResponse> queryResponseFuture = DynamoDbService.getPasteMetadataByUuid(tableName, uuid);
            QueryResponse queryResponse = queryResponseFuture.get();

            // delete file from s3 (uuid is unique, only one file)
            List<CompletableFuture<?>> s3DeleteFutures = new ArrayList<>();
            queryResponse.items().forEach(item -> {
                String s3Path = item.get("s3_path").s();
                CompletableFuture<DeleteObjectResponse> deleteObjectFuture = S3Service.deleteFile(bucketName, s3Path);
                s3DeleteFutures.add(deleteObjectFuture);
            });

            // delete metadata from dynamo
            CompletableFuture<DeleteItemResponse> dataDeleteFuture = DynamoDbService.deletePasteMetadataByUuid(tableName, uuid);
            s3DeleteFutures.add(dataDeleteFuture);

            CompletableFuture<Void> allDownloads = CompletableFuture.allOf(s3DeleteFutures.toArray(new CompletableFuture[0]));
            allDownloads.join();

            response.setStatusCode(200);
            response.setBody("Successfully deleted " + uuid);

        } catch (Exception e) {
            response.setStatusCode(500);
            response.setBody("Error while processing the file: " + e.getMessage());
        }

        return response;
    }
}