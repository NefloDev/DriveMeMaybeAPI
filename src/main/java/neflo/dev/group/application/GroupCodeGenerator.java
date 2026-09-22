package neflo.dev.group.application;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class GroupCodeGenerator {

    private static final char[] AVAILABLE_ALPHABET = "ABDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private static final int CODE_LENGTH = 9;
    private static final int PART_LENGTH = 4;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        char[] code = new char[CODE_LENGTH];

        for (int i = 0; i < PART_LENGTH; i++) {
            code[i] = randomChar();
            code[i + PART_LENGTH + 1] = randomChar();
        }
        code[PART_LENGTH] = '-';

        return new String(code);
    }

    private char randomChar() {
        return AVAILABLE_ALPHABET[
                random.nextInt(AVAILABLE_ALPHABET.length)
                ];
    }

}
