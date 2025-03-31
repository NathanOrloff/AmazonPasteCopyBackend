package api.handlers;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.fasterxml.jackson.databind.ObjectMapper;

import api.constants.Constants;
import api.services.S3Service;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import api.services.DynamoDbService;
import api.models.Paste;
import api.models.PasteMetadata;

public class PostPaste implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final String tableName = System.getenv(Constants.dynamodbTableNameEnv);
    private final String bucketName = System.getenv(Constants.s3BucketNameEnv);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            // Extract file contents from request (base64 encoded)
            String body = request.getBody();
            if (body == null || body.isEmpty()) {
                response.setStatusCode(400);
                response.setBody("No body found in the request.");
                return response;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            Paste paste = objectMapper.readValue(body, Paste.class);

            // decode file contents
            byte[] decodedFile = Base64.getDecoder().decode(paste.fileContent());

            // generate uuid
            String uuid = UUID.randomUUID().toString();

            // upload file to s3
            CompletableFuture<PutObjectResponse> s3UploadFuture = S3Service.uploadFileBytes(bucketName, uuid, decodedFile);

            // create data record
            long createdTime = Instant.now().toEpochMilli();
            long deleteTime = createdTime + ((long)paste.storeDays() * 24 * 60 * 60 * 1000);
            PasteMetadata metadata = new PasteMetadata(uuid, bucketName, uuid, createdTime, deleteTime);

            // post data record to dynamo table
            CompletableFuture<PutItemResponse> dbWriteFuture = DynamoDbService.postPasteMetadata(metadata, tableName);

            // Ensure both operations complete before returning
            CompletableFuture.allOf(s3UploadFuture, dbWriteFuture).join();

            // set response data to success
            response.setStatusCode(201);
            response.setBody("Successfully uploaded file");

        } catch (Exception e) {
            response.setStatusCode(500);
            response.setBody("Error while processing the file: " + e.getMessage());
        }

        return response;
    }
}