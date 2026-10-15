package com.example.aicompanion.pig.talk;

import net.minecraft.util.RandomSource;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Простий мозок без інтернету: шукає в репліці ключові слова і відповідає
 * заготовками з урахуванням характеру. Використовується, коли немає api_key
 * або Claude API недоступний.
 */
public final class OfflinePigBrain {

    // Порядок важливий: перевіряємо зверху вниз
    private static final List<Map.Entry<PigAction, List<String>>> KEYWORDS = List.of(
            Map.entry(PigAction.LEAVE, List.of("відпуска", "release", "ти вільн")),
            Map.entry(PigAction.BEFRIEND, List.of("друж", "друг", "friend", "будь моєю", "будеш моєю")),
            Map.entry(PigAction.COME, List.of("сюди", "до мене", "come", "here", "іди сюди")),
            Map.entry(PigAction.FOLLOW, List.of("за мною", "follow", "ходімо", "пішли")),
            Map.entry(PigAction.STAY, List.of("стій", "чекай", "сиди", "stay", "wait", "стоп")),
            Map.entry(PigAction.GUARD, List.of("захищ", "охорон", "guard", "protect", "бий", "атакуй")),
            Map.entry(PigAction.FORAGE, List.of("шукай", "збирай", "їж", "трюфел", "forage", "food", "їжу")),
            Map.entry(PigAction.WANDER, List.of("гуляй", "погуляй", "wander", "відпочин")),
            Map.entry(PigAction.JUMP, List.of("стриб", "jump", "танцюй", "dance"))
    );

    private OfflinePigBrain() {}

    public static PigDecision decide(String message, boolean isOwner, PigPersonality.Trait trait, RandomSource random) {
        String text = message.toLowerCase(Locale.ROOT);
        PigAction action = PigAction.NONE;
        for (Map.Entry<PigAction, List<String>> entry : KEYWORDS) {
            if (entry.getValue().stream().anyMatch(text::contains)) {
                action = entry.getKey();
                break;
            }
        }

        // чужа свиня слухається лише прохань подружитись, підійти і стрибнути
        if (!isOwner && action != PigAction.BEFRIEND && action != PigAction.COME && action != PigAction.JUMP) {
            action = PigAction.NONE;
        }
        // офлайн-мозок дружить без золотої моркви лише з 1 шансом із 3
        if (action == PigAction.BEFRIEND && (isOwner || random.nextInt(3) != 0)) {
            return new PigDecision(isOwner ? "Хрю! Ми вже друзі!" : "Хрю... Принеси золоту моркву, тоді подумаю.", PigAction.NONE);
        }
        return new PigDecision(reply(action, trait, random), action);
    }

    private static String reply(PigAction action, PigPersonality.Trait trait, RandomSource random) {
        String base = switch (action) {
            case FOLLOW -> "Хрю! Іду за тобою.";
            case STAY -> "Добре, стою тут.";
            case GUARD -> "Хто тебе образить — матиме справу з моїм п'ятачком!";
            case FORAGE -> "Піду пошукаю щось смачненьке.";
            case WANDER -> "Піду погуляю.";
            case COME -> "Біжу!";
            case JUMP -> "Хрю-хрю! Дивись, як я вмію!";
            case BEFRIEND -> "Гаразд, тепер ми друзі. Хрю!";
            case LEAVE -> "Ех... Бувай, людино.";
            case NONE -> {
                String[] grunts = {"Хрю?", "Хрю-хрю.", "Я свиня, я не все розумію. Хрю.", "Хрррю!"};
                yield grunts[random.nextInt(grunts.length)];
            }
        };
        return switch (trait) {
            case LAZY -> base + " *позіхає*";
            case POLITE -> "Перепрошую. " + base;
            case SARCASTIC -> base + " Ну звісно ж.";
            case SHY -> "Е-е... " + base;
            case BRAGGART -> base + " Я ж найкраща свиня!";
            case CURIOUS -> base + " А навіщо?";
        };
    }
}
