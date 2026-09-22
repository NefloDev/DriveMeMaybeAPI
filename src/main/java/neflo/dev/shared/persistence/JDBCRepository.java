package neflo.dev.shared.persistence;

import lombok.RequiredArgsConstructor;
import neflo.dev.group.api.GroupInsights;
import neflo.dev.group.api.GroupMemberBalanceDTO;
import neflo.dev.shared.dto.YearMonth;
import neflo.dev.shared.exception.DatabaseException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JDBCRepository {

    private static final String INSIGHTS_MONTHLY_QUERY = """
            SELECT EXTRACT('YEAR' FROM T.DATE) AS YEAR, EXTRACT('MONTH' FROM T.DATE) AS MONTH, SUM(T.DURATION_MINUTES) AS DRIVING_TIME, D.NICKNAME AS NICKNAME
            FROM GRP_TRIPS T
            JOIN GRP_GROUPS G ON T.GRP_ID = G.ID
            JOIN USR_USERS D ON T.DRIVER = D.ID
            WHERE G.ID = ? AND DATE_TRUNC('MONTH', T.DATE) = ?
            GROUP BY EXTRACT('YEAR' FROM T.DATE), EXTRACT('MONTH' FROM T.DATE), D.NICKNAME;
            """;

    private static final String INSIGHTS_YEARLY_QUERY = """
            SELECT EXTRACT('YEAR' FROM T.DATE) AS YEAR, SUM(T.DURATION_MINUTES) AS DRIVING_TIME, D.NICKNAME AS NICKNAME
            FROM GRP_TRIPS T
            JOIN GRP_GROUPS G ON T.GRP_ID = G.ID
            JOIN USR_USERS D ON T.DRIVER = D.ID
            WHERE G.ID = ? AND DATE_TRUNC('YEAR', T.DATE) = ?
            GROUP BY EXTRACT('YEAR' FROM T.DATE), D.NICKNAME;
            """;

    private static final String UPDATE_USER_BALANCE = """
            UPDATE REL_GROUP_MEMBERS
            SET
            TIME_BALANCE = TIME_BALANCE + ?,
            KM_BALANCE = KM_BALANCE + ?
            WHERE
            GRP_ID = ?
            AND USR_ID = ?;
            """;

    private static final String GET_GROUP_MEMBERS_QUERY = """
            SELECT
            U.NICKNAME AS NICKNAME,
            R.TIME_BALANCE,
            R.KM_BALANCE
            FROM REL_GROUP_MEMBERS R
            JOIN USR_USERS U ON R.USR_ID = U.ID
            WHERE R.GRP_ID = ?;
            """;

    private static final ResultSetExtractor<GroupInsights> MONTHLY_INSIGHTS_RSE = rs -> {
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
    private static final ResultSetExtractor<GroupInsights> YEARLY_INSIGHTS_RSE = rs -> {
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

    private static final RowMapper<GroupMemberBalanceDTO> GROUP_MEMBER_BALANCE_DTO_RM = (rs, rowNum) -> new GroupMemberBalanceDTO(
            rs.getString("NICKNAME"),
            rs.getInt("TIME_BALANCE"),
            rs.getInt("KM_BALANCE")
    );

    private final JdbcTemplate db;

    public GroupInsights getMonthlyGroupInsights(UUID groupId, YearMonth yearMonth) {
        try {
            Object[] params = new Object[2];
            params[0] = groupId;
            params[1] = LocalDate.of(yearMonth.year(), yearMonth.month(), 1);

            return db.query(INSIGHTS_MONTHLY_QUERY, MONTHLY_INSIGHTS_RSE, params);
        } catch (Exception e) {
            throw new DatabaseException("monthly-insights-error", "There was an error trying to retrieve requested's month group's insights.", e);
        }
    }

    public GroupInsights getYearlyGroupInsights(UUID groupId, int year) {
        try {
            Object[] params = new Object[2];
            params[0] = groupId;
            params[1] = LocalDate.of(year, 1, 1);

            return db.query(INSIGHTS_YEARLY_QUERY, YEARLY_INSIGHTS_RSE, params);
        } catch (Exception e) {
            throw new DatabaseException("yearly-insights-error", "There was an error trying to retrieve requested's year group's insights.", e);
        }
    }

    public void updateGroupMemberBalance(UUID userUuid, UUID groupUuid, int durationMinutes, int distanceKm) {
        try {
            Object[] params = new Object[4];
            params[0] = durationMinutes;
            params[1] = distanceKm;
            params[2] = groupUuid;
            params[3] = userUuid;

            db.update(UPDATE_USER_BALANCE, params);
        } catch (Exception e) {
            throw new DatabaseException("balance-update-error", "There was an error trying to update some user's time and distance balance.", e);
        }
    }

    public List<GroupMemberBalanceDTO> getGroupMembersBalanceInfo(UUID groupUuid) {
        try {
            Object[] params = new Object[1];
            params[0] = groupUuid;

            return db.query(GET_GROUP_MEMBERS_QUERY, GROUP_MEMBER_BALANCE_DTO_RM, params);
        } catch (Exception e) {
            throw new DatabaseException("members-balance-error", "There was an error trying to retrieve requested's group's members' balance information.", e);
        }
    }

}
