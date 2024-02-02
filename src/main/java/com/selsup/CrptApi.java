package com.selsup;

import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private static final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";

    private final ChronoUnit chronoUnit;

    private final int requestLimit;

    private Deque<LocalDateTime> requestTime;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        chronoUnit = timeUnit.toChronoUnit();
        this.requestLimit = requestLimit;
        requestTime = new ArrayDeque<>();
    }

    /**
     * Метод для обращения к API Четсного знака по следующему URL: "https://ismp.crpt.ru/api/v3/lk/documents/create",
     * с помощью HTTP метода POST.
     * @param json - JSON объект, который передается в теле метода POST при обращении к API.
     * @return Возвращает HTTP статус код.
     */
    public synchronized HttpStatusCode createDocumentForIntroductionGoodsProducedInRussia(JSONObject json) {
        if (!checkAbilityToAccessAPI()) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(json.toString(), headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(URL, request, String.class);
            return response.getStatusCode();
        } catch (HttpStatusCodeException e) {
            return e.getStatusCode();
        }
    }

    /**
     * Метод проверяет, возможно ли в данный момент получить доступ к API.
     * Доступ к API возможен, если за последний, указанный при создании экземпляра класса,
     * промежуток времени - timeUnit, было совершенно меньшее количество обращений,
     * чем значения поля requestLimit.
     *
     * @return Возвращает true, если доступ возможен, и false, если нет.
     */
    private synchronized boolean checkAbilityToAccessAPI() {
        LocalDateTime currentTime = LocalDateTime.now();

        while ((!requestTime.isEmpty()) && (chronoUnit.between(requestTime.peek(), currentTime) >= 1)) {
            requestTime.pop();
        }

        if (requestTime.size() < requestLimit) {
            requestTime.add(currentTime);
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        try {
            Path path = Path.of("src\\main\\resources\\request.json");
            JSONObject json = new JSONObject(Files.readString(path));
            CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 3);
            LocalDateTime start, end;
            for (int i = 0; i < 10; i++) {
                start = LocalDateTime.now();
                HttpStatusCode httpStatusCode = crptApi.createDocumentForIntroductionGoodsProducedInRussia(json);
                Thread.sleep(200);
                end = LocalDateTime.now();
                System.out.println("Время за которое выполнилась текущая итерация: "
                        + ChronoUnit.MILLIS.between(start, end) + "мс");
                System.out.println(httpStatusCode + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
