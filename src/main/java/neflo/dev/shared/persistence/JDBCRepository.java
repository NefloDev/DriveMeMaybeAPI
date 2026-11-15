package neflo.dev.shared.persistence;

import lombok.RequiredArgsConstructor;
import neflo.dev.group.api.GroupInsights;
import neflo.dev.group.api.GroupMemberBalanceDTO;
import neflo.dev.shared.dto.YearMonth;
import neflo.dev.shared.exception.DatabaseException;
import neflo.dev.shared.persistence.queries.GroupQueries;
import neflo.dev.shared.persistence.queries.UserQueries;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JDBCRepository {

    private final JdbcTemplate db;

    public GroupInsights getMonthlyGroupInsights(UUID groupId, YearMonth yearMonth) {
        try {
            Object[] params = new Object[2];
            params[0] = groupId;
            params[1] = LocalDate.of(yearMonth.year(), yearMonth.month(), 1);

            return db.query(GroupQueries.INSIGHTS_MONTHLY, GroupQueries.MONTHLY_INSIGHTS_RSE, params);
        } catch (DatabaseException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabaseException("monthly-insights-error", "There was an error trying to retrieve requested's month group's insights.", e);
        }
    }

    public GroupInsights getYearlyGroupInsights(UUID groupId, int year) {
        try {
            Object[] params = new Object[2];
            params[0] = groupId;
            params[1] = LocalDate.of(year, 1, 1);

            return db.query(GroupQueries.INSIGHTS_YEARLY, GroupQueries.YEARLY_INSIGHTS_RSE, params);
        } catch (DatabaseException e) {
            throw e;
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

            db.update(UserQueries.UPDATE_USER_BALANCE, params);
        } catch (DatabaseException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabaseException("balance-update-error", "There was an error trying to update some user's time and distance balance.", e);
        }
    }

    public List<GroupMemberBalanceDTO> getGroupMembersBalanceInfo(UUID groupUuid) {
        try {
            Object[] params = new Object[1];
            params[0] = groupUuid;

            return db.query(GroupQueries.GET_GROUP_MEMBERS_BALANCE, GroupQueries.GROUP_MEMBER_BALANCE_DTO_RM, params);
        } catch (DatabaseException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabaseException("members-balance-error", "There was an error trying to retrieve requested's group's members' balance information.", e);
        }
    }

}
