package task;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import jakarta.ws.rs.core.MediaType;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


@Slf4j
public class CrptApi {

    private final Semaphore semaphore;
    private final HttpClient httpClient;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(ApiConstant.THREAD_POOL_SIZE);
        if (requestLimit <= 0) {
            throw new IllegalArgumentException("Количество запросов в указанном промежутке времени должно быть больше 0");
        }
        this.semaphore = new Semaphore(requestLimit);
        this.httpClient = HttpClient.newHttpClient();

        long delay = timeUnit.toMillis(1);

        scheduler.scheduleAtFixedRate(() -> semaphore.release(requestLimit - semaphore.availablePermits()),
                delay, delay, TimeUnit.MILLISECONDS);
    }

    public static class UrlConstant {

        public static final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";

        private UrlConstant() {
        }
    }

    public static class ApiConstant {
        private static final int THREAD_POOL_SIZE = 1;
    }

    @Data
    @Builder
    public static class Document {

        private Description description;

        @SerializedName("doc_id")
        private String docId;

        @SerializedName("doc_status")
        private String docStatus;

        @SerializedName("doc_type")
        private String docType;

        @SerializedName("importRequest")
        private boolean importRequest;

        @SerializedName("owner_inn")
        private String ownerInn;

        @SerializedName("participant_inn")
        private String participantInn;

        @SerializedName("producer_inn")
        private String producerInn;

        @SerializedName("production_date")
        private Date productionDate;

        @SerializedName("production_type")
        private String productionType;

        private List<Product> products;

        @SerializedName("reg_date")
        private Date regDate;

        @SerializedName("reg_number")
        private String regNumber;

        @Data
        @Builder
        public static class Description {
            @SerializedName("participantInn")
            private String participantInn;
        }

        @Data
        @Builder
        public static class Product {
            @SerializedName("certificate_document")
            private String certificateDocument;

            @SerializedName("certificate_document_date")
            private Date certificateDocumentDate;

            @SerializedName("certificate_document_number")
            private String certificateDocumentNumber;

            @SerializedName("owner_inn")
            private String ownerInn;

            @SerializedName("producer_inn")
            private String producerInn;

            @SerializedName("production_date")
            private Date productionDate;

            @SerializedName("tnved_code")
            private String tnvedCode;

            @SerializedName("uit_code")
            private String uitCode;

            @SerializedName("uitu_code")
            private String uituCode;

        }
    }

    public static class JsonConverter {
        private static final Gson gson = new Gson();

        private JsonConverter() {
        }

        public static String toJson(Object object) {
            return gson.toJson(object);
        }
    }

    public void createDocument(Document document, String signature) throws InterruptedException, IOException {
        semaphore.acquire();

        String json = JsonConverter.toJson(document);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(UrlConstant.URL))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .header("Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        log.debug("Sending HTTP request: {}", request);

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        log.info("Response status code: {}", response.statusCode());
        log.debug("Response body: {}", response.body());
    }
}
