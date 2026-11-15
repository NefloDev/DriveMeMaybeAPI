package neflo.dev.group.application;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class GroupCodeGeneratorTest {

    private final GroupCodeGenerator underTest = new GroupCodeGenerator();
    private static final Pattern FORMAT = Pattern.compile("^[ABDEFGHJKLMNPQRSTUVWXYZ23456789]{4}-[ABDEFGHJKLMNPQRSTUVWXYZ23456789]{4}$");

    @Test
    void should_generateNineCharCode_when_called() {
        String code = underTest.generate();

        assertThat(code).hasSize(9);
        assertThat(code.charAt(4)).isEqualTo('-');
    }

    @Test
    void should_useAllowedAlphabet_when_called() {
        String code = underTest.generate();

        assertThat(FORMAT.matcher(code).matches()).isTrue();
    }

    @RepeatedTest(5)
    void should_generateFreshCode_when_calledRepeatedly() {
        assertThat(FORMAT.matcher(underTest.generate()).matches()).isTrue();
    }

    @Test
    void should_generateDistinctCodes_when_calledManyTimes() {
        Set<String> codes = new HashSet<>();

        for (int i = 0; i < 50; i++) {
            codes.add(underTest.generate());
        }

        assertThat(codes.size()).isGreaterThan(45);
    }
}
