package com.example.aicompanion.pig.talk;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Мозок свині на Claude (Messages API).
 *
 * Чому сирий HTTP, а не офіційний Java SDK: мод — це один jar у папці mods, і SDK
 * довелося б вшивати разом з OkHttp/Kotlin/Jackson, які можуть конфліктувати з іншими
 * модами. java.net.http і Gson уже є в грі.
 *
 * Модель відповідає структурованим JSON (output_config.format): {"reply": ..., "action": ...},
 * тож гра завжди точно знає, яку дію виконати.
 */
public final class ClaudePigBrain {

    private static final URI ENDPOINT = URI.create("https://api.anthropic.com/v1/messages");

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final String SYSTEM_PROMPT = """
            You are a pig in Minecraft, chatting with a player in the in-game chat.
            The user turn gives you a JSON block with your name, your personality, your state and the world around you,
            followed by what the player said. Stay in character as that pig: speak in its personality,
            mention what you see or feel when it fits, and oink now and then.

            Reply in the same language the player used (usually Ukrainian). Keep the reply short enough for game chat:
            one or two sentences, at most 200 characters, plain text with no markdown.

            Choose exactly one action. Only the pig's owner can give orders:
            - follow: walk after the owner
            - stay: stand still where you are
            - guard: protect the owner and ram whoever attacks them
            - forage: search the ground for food and bring it to the owner
            - wander: walk around freely
            - leave: the owner releases you and you are no longer theirs
            For anyone (owner or not):
            - come: walk up to the player who is talking
            - jump: jump on the spot, for fun or to show emotion
            - befriend: only if you have no owner yet and the player has genuinely won you over
              (kind words, a treat, a good joke). Do not befriend just because you were asked once.
            - none: just talk

            If a player who is not your owner gives an order, refuse in character and use none.
            If the player asks for something a pig cannot do, say so in character and use none.
            """;

    private static final JsonObject OUTPUT_SCHEMA = buildSchema();

    private ClaudePigBrain() {}

    /**
     * @param history попередні репліки як пари {гравець, свиня}
     * @param context опис свині й світу (JSON-текст)
     */
    public static CompletableFuture<PigDecision> ask(List<String[]> history, String context, String playerMessage) {
        JsonArray messages = new JsonArray();
        for (String[] turn : history) {
            messages.add(message("user", turn[0]));
            messages.add(message("assistant", turn[1]));
        }
        messages.add(message("user", "<pig_context>\n" + context + "\n</pig_context>\n\nPlayer says: " + playerMessage));

        JsonObject format = new JsonObject();
        format.addProperty("type", "json_schema");
        format.add("schema", OUTPUT_SCHEMA);
        JsonObject outputConfig = new JsonObject();
        outputConfig.addProperty("effort", AiConfig.effort());
        outputConfig.add("format", format);

        JsonObject body = new JsonObject();
        body.addProperty("model", AiConfig.model());
        body.addProperty("max_tokens", 4096);
        body.addProperty("system", SYSTEM_PROMPT);
        body.add("messages", messages);
        body.add("output_config", outputConfig);
        // якщо модель відмовить відповідати, API сам повторить запит на запасній моделі
        body.addProperty("fallbacks", "default");

        HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                .timeout(Duration.ofSeconds(60))
                .header("content-type", "application/json")
                .header("x-api-key", AiConfig.apiKey())
                .header("anthropic-version", "2023-06-01")
                .header("anthropic-beta", "server-side-fallback-2026-07-01")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(ClaudePigBrain::parse);
    }

    private static PigDecision parse(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Claude API HTTP " + response.statusCode() + ": " + response.body());
        }
        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();

        String stopReason = root.has("stop_reason") && !root.get("stop_reason").isJsonNull()
                ? root.get("stop_reason").getAsString() : "";
        if ("refusal".equals(stopReason)) {
            return new PigDecision("Хрю... Про це я говорити не буду.", PigAction.NONE);
        }
        if ("max_tokens".equals(stopReason)) {
            throw new IllegalStateException("Claude API: відповідь обрізана (max_tokens)");
        }

        // у content можуть бути thinking-блоки; потрібен текстовий блок з нашим JSON
        for (JsonElement el : root.getAsJsonArray("content")) {
            JsonObject block = el.getAsJsonObject();
            if (!"text".equals(block.get("type").getAsString())) continue;

            JsonObject decision = JsonParser.parseString(block.get("text").getAsString()).getAsJsonObject();
            String reply = decision.get("reply").getAsString().trim();
            if (reply.length() > 256) reply = reply.substring(0, 256) + "...";
            return new PigDecision(reply, PigAction.byId(decision.get("action").getAsString()));
        }
        throw new IllegalStateException("Claude API: немає текстової відповіді");
    }

    private static JsonObject message(String role, String text) {
        JsonObject msg = new JsonObject();
        msg.addProperty("role", role);
        msg.addProperty("content", text);
        return msg;
    }

    private static JsonObject buildSchema() {
        JsonObject reply = new JsonObject();
        reply.addProperty("type", "string");
        reply.addProperty("description", "What the pig says in chat, in character");

        JsonArray actions = new JsonArray();
        for (PigAction action : PigAction.values()) actions.add(action.id());
        JsonObject action = new JsonObject();
        action.addProperty("type", "string");
        action.add("enum", actions);

        JsonObject props = new JsonObject();
        props.add("reply", reply);
        props.add("action", action);

        JsonArray required = new JsonArray();
        required.add("reply");
        required.add("action");

        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", props);
        schema.add("required", required);
        schema.addProperty("additionalProperties", false);
        return schema;
    }
}
