package neflo.dev.shared.persistence.queries;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import neflo.dev.group.api.GroupInsights;
import neflo.dev.group.api.GroupMemberBalanceDTO;
import neflo.dev.shared.exception.DatabaseException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class GroupQueries {

    public static final String INSIGHTS_MONTHLY = """
            SELECT EXTRACT('YEAR' FROM T.DATE) AS YEAR, EXTRACT('MONTH' FROM T.DATE) AS MONTH, SUM(T.DURATION_MINUTES) AS DRIVING_TIME, D.NICKNAME AS NICKNAME
            FROM GRP_TRIPS T
            JOIN GRP_GROUPS G ON T.GRP_ID = G.ID
            JOIN USR_USERS D ON T.DRIVER = D.ID
            WHERE G.ID = ? AND DATE_TRUNC('MONTH', T.DATE) = ?
            GROUP BY EXTRACT('YEAR' FROM T.DATE), EXTRACT('MONTH' FROM T.DATE), D.NICKNAME;
            """;

    public static final ResultSetExtractor<GroupInsights> MONTHLY_INSIGHTS_RSE = rs -> {
        if (!rs.next()) {
            throw new DatabaseException("no-result-request", "Database query returned no results.");
        }
        int year = rs.getInt("YEAR");
        Integer month = rs.getInt("MONTH");
        month = month == 0 ? null : month;

        Map<String, Integer> driverTimes = new HashMap<>();

        do {
            String nickName = rs.getString("NICKNAME");
            int drivingTime = rs.getInt("DRIVING_TIME");
            driverTimes.put(nickName, drivingTime);
        } while (rs.next());

        return new GroupInsights(
                year,
                month,
                driverTimes
        );
    };

    public static final String INSIGHTS_YEARLY = """
            SELECT EXTRACT('YEAR' FROM T.DATE) AS YEAR, SUM(T.DURATION_MINUTES) AS DRIVING_TIME, D.NICKNAME AS NICKNAME
            FROM GRP_TRIPS T
            JOIN GRP_GROUPS G ON T.GRP_ID = G.ID
            JOIN USR_USERS D ON T.DRIVER = D.ID
            WHERE G.ID = ? AND DATE_TRUNC('YEAR', T.DATE) = ?
            GROUP BY EXTRACT('YEAR' FROM T.DATE), D.NICKNAME;
            """;

    public static final ResultSetExtractor<GroupInsights> YEARLY_INSIGHTS_RSE = rs -> {
        if (!rs.next()) {
            throw new DatabaseException("no-result-request", "Database query returned no results.");
        }
        int year = rs.getInt("YEAR");

        Map<String, Integer> driverTimes = new HashMap<>();

        do {
            String nickName = rs.getString("NICKNAME");
            int drivingTime = rs.getInt("DRIVING_TIME");
            driverTimes.put(nickName, drivingTime);
        } while (rs.next());

        return new GroupInsights(
                year,
                null,
                driverTimes
        );
    };

    public static final String GET_GROUP_MEMBERS_BALANCE = """
            SELECT
            U.NICKNAME AS NICKNAME,
            R.TIME_BALANCE,
            R.KM_BALANCE
            FROM REL_GROUP_MEMBERS R
            JOIN USR_USERS U ON R.USR_ID = U.ID
            WHERE R.GRP_ID = ?;
            """;

    public static final RowMapper<GroupMemberBalanceDTO> GROUP_MEMBER_BALANCE_DTO_RM = (rs, rowNum) -> new GroupMemberBalanceDTO(
            rs.getString("NICKNAME"),
            rs.getInt("TIME_BALANCE"),
            rs.getInt("KM_BALANCE")
    );

}
