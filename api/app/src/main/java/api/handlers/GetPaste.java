package api.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import api.constants.Constants;
import api.services.S3Service;
import api.services.DynamoDbService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;
import java.util.Base64;

public class GetPaste implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

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

            // get file from s3 (uuid is unique, only one file)
            List<CompletableFuture<Void>> s3DownloadFutures = new ArrayList<>();
            queryResponse.items().forEach(item -> {
                String s3Path = item.get("s3_path").s();
                CompletableFuture<ResponseBytes<GetObjectResponse>> bytesFuture = S3Service.downloadFileBytes(bucketName, s3Path);
                s3DownloadFutures.add(bytesFuture.thenAccept(bytes -> {
                    byte[] fileBytes = bytes.asByteArray();
                    String base64Encoded = Base64.getEncoder().encodeToString(fileBytes);
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/octet-stream");
                    headers.put("Content-Disposition", "attachment; filename=\"" + uuid +"\"");
                    headers.put("Access-Control-Allow-Origin", "*");
                    response.setStatusCode(200);
                    response.setHeaders(headers);
                    response.setIsBase64Encoded(true);
                    response.setBody(base64Encoded);
                }));
            });

            CompletableFuture<Void> allDownloads = CompletableFuture.allOf(s3DownloadFutures.toArray(new CompletableFuture[0]));
            allDownloads.join();  // Blocks until all S3 downloads are finished

        } catch (Exception e) {
            response.setStatusCode(500);
            response.setBody("Error while processing the file: " + e.getMessage());
        }

        return response;
    }
}