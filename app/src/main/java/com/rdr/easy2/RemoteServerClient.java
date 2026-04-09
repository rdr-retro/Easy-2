package com.rdr.easy2;

import android.location.Location;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class RemoteServerClient {
    private static final int CONNECT_TIMEOUT_MS = 6000;
    private static final int READ_TIMEOUT_MS = 8000;

    private RemoteServerClient() {
    }

    public static RegistrationResult registerClient(
            String baseUrl,
            String authToken,
            String deviceModel,
            String displayName,
            String age
    ) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("auth_token", authToken);
        payload.put("device_model", emptyToDefault(deviceModel, "Android"));
        payload.put("display_name", emptyToDefault(displayName, "Cliente Easy 2"));
        payload.put("age", emptyToDefault(age, ""));

        JSONObject response = postJson(buildApiUrl(baseUrl, "/api/clients/register"), payload);
        return new RegistrationResult(
                response.optString("client_id", ""),
                response.optBoolean("created", false)
        );
    }

    public static void pushLocation(
            String baseUrl,
            String clientId,
            String authToken,
            Location location,
            int batteryPercent,
            boolean isCharging,
            String deviceModel
    ) throws Exception {
        if (location == null || TextUtils.isEmpty(clientId)) {
            return;
        }

        JSONObject payload = new JSONObject();
        payload.put("auth_token", authToken);
        payload.put("latitude", location.getLatitude());
        payload.put("longitude", location.getLongitude());
        payload.put("accuracy", location.hasAccuracy() ? location.getAccuracy() : JSONObject.NULL);
        payload.put("provider", emptyToDefault(location.getProvider(), ""));
        payload.put("battery_percent", batteryPercent);
        payload.put("is_charging", isCharging);
        payload.put("device_model", emptyToDefault(deviceModel, ""));
        payload.put("recorded_at", System.currentTimeMillis());

        postJson(
                buildApiUrl(baseUrl, "/api/clients/" + clientId + "/location"),
                payload
        );
    }

    public static String buildDashboardUrl(String baseUrl) {
        return buildApiUrl(baseUrl, "/admin?embedded=1");
    }

    public static String normalizeBaseUrl(String baseUrl) {
        return LauncherPreferences.sanitizeServerUrl(baseUrl);
    }

    private static String buildApiUrl(String baseUrl, String path) {
        String normalizedBaseUrl = normalizeBaseUrl(baseUrl);
        if (TextUtils.isEmpty(normalizedBaseUrl)) {
            throw new IllegalArgumentException("Server URL is empty");
        }
        return normalizedBaseUrl + path;
    }

    private static JSONObject postJson(String endpoint, JSONObject payload) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = openConnection(endpoint);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setDoOutput(true);

            byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(body);
            }

            int statusCode = connection.getResponseCode();
            String responseBody = readResponseBody(connection, statusCode);
            if (statusCode < 200 || statusCode >= 300) {
                throw new HttpStatusException(statusCode, responseBody);
            }

            if (TextUtils.isEmpty(responseBody)) {
                return new JSONObject();
            }

            return new JSONObject(responseBody);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static JSONArray getJsonArray(String endpoint) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = openConnection(endpoint);
            connection.setRequestMethod("GET");

            int statusCode = connection.getResponseCode();
            String responseBody = readResponseBody(connection, statusCode);
            if (statusCode < 200 || statusCode >= 300) {
                throw new HttpStatusException(statusCode, responseBody);
            }

            return new JSONArray(responseBody);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static HttpURLConnection openConnection(String endpoint) throws Exception {
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setUseCaches(false);
        connection.setRequestProperty("Accept", "application/json");
        return connection;
    }

    private static String readResponseBody(HttpURLConnection connection, int statusCode)
            throws Exception {
        InputStream inputStream = statusCode >= 200 && statusCode < 400
                ? connection.getInputStream()
                : connection.getErrorStream();

        if (inputStream == null) {
            return "";
        }

        try (
                InputStreamReader inputStreamReader =
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader)
        ) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        }
    }

    private static String emptyToDefault(String value, String defaultValue) {
        return TextUtils.isEmpty(value) ? defaultValue : value.trim();
    }

    public static final class RegistrationResult {
        public final String clientId;
        public final boolean created;

        public RegistrationResult(String clientId, boolean created) {
            this.clientId = clientId;
            this.created = created;
        }
    }

    public static final class HttpStatusException extends Exception {
        public final int statusCode;

        public HttpStatusException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }
    }
}
