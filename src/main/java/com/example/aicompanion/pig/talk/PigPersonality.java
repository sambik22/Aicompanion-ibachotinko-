package com.example.aicompanion.pig.talk;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.Pig;

/**
 * Ім'я і характер свині. Видаються випадково при першій розмові
 * й зберігаються у світі разом зі свинею.
 */
public final class PigPersonality {

    private static final String NAME_KEY = "aicompanion_name";
    private static final String TRAIT_KEY = "aicompanion_trait";

    private static final String[] NAMES = {
            "Боря", "Хрюша", "Пепа", "Фуня", "П'ятачок", "Ковбаска", "Бекончик", "Рожа", "Шкварка", "Ніфніф"
    };

    /** Характер = як свиня говорить. Ключ використовує і Claude, і офлайн-мозок. */
    public enum Trait {
        LAZY("ліниве", "лінива, все робить неохоче, часто позіхає"),
        CURIOUS("допитливе", "допитлива, ставить зустрічні запитання, все хоче знати"),
        POLITE("ввічливе", "дуже ввічлива і шляхетна, звертається на «ви»"),
        SARCASTIC("саркастичне", "саркастична, жартує, але слухається"),
        SHY("боязке", "боязка, легко лякається, говорить тихо і невпевнено"),
        BRAGGART("хвальковите", "хвалькувата, вважає себе найкращою свинею світу");

        public final String shortName;
        public final String description;

        Trait(String shortName, String description) {
            this.shortName = shortName;
            this.description = description;
        }
    }

    private PigPersonality() {}

    public static String name(Pig pig) {
        if (pig.hasCustomName() && pig.getCustomName() != null) {
            return pig.getCustomName().getString();
        }
        ensure(pig);
        return pig.getPersistentData().getStringOr(NAME_KEY, "Хрюша");
    }

    public static Trait trait(Pig pig) {
        ensure(pig);
        String raw = pig.getPersistentData().getStringOr(TRAIT_KEY, Trait.CURIOUS.name());
        try {
            return Trait.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return Trait.CURIOUS;
        }
    }

    private static void ensure(Pig pig) {
        CompoundTag data = pig.getPersistentData();
        if (data.getStringOr(NAME_KEY, "").isEmpty()) {
            data.putString(NAME_KEY, NAMES[pig.getRandom().nextInt(NAMES.length)]);
        }
        if (data.getStringOr(TRAIT_KEY, "").isEmpty()) {
            Trait[] traits = Trait.values();
            data.putString(TRAIT_KEY, traits[pig.getRandom().nextInt(traits.length)].name());
        }
    }
}
