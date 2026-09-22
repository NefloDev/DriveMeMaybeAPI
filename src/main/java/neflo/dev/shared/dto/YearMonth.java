package neflo.dev.shared.dto;

public record YearMonth(
        int year,
        int month
) {

    public static YearMonth of(int year, int month) {
        return new YearMonth(year, month);
    }

}
