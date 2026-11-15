package neflo.dev.shared.persistence.queries;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserQueries {

    public static final String UPDATE_USER_BALANCE = """
            UPDATE REL_GROUP_MEMBERS
            SET
            TIME_BALANCE = TIME_BALANCE + ?,
            KM_BALANCE = KM_BALANCE + ?
            WHERE
            GRP_ID = ?
            AND USR_ID = ?;
            """;

}
