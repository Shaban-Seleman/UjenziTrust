package com.uzenjitrust.support.integrity;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class IntegrityDbAssertions {

    private final JdbcTemplate jdbc;

    public IntegrityDbAssertions(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<UnbalancedJournal> findUnbalancedEntriesSince(Instant testStart) {
        String sql = """
                SELECT je.id,
                       SUM(CASE WHEN jl.line_type='DEBIT' THEN jl.amount ELSE 0 END) AS dr,
                       SUM(CASE WHEN jl.line_type='CREDIT' THEN jl.amount ELSE 0 END) AS cr
                FROM ledger.journal_entries je
                JOIN ledger.journal_lines jl ON jl.journal_entry_id = je.id
                WHERE je.created_at >= ?
                GROUP BY je.id
                HAVING SUM(CASE WHEN jl.line_type='DEBIT' THEN jl.amount ELSE 0 END)
                     <> SUM(CASE WHEN jl.line_type='CREDIT' THEN jl.amount ELSE 0 END)
                """;
        return jdbc.query(sql, (rs, rowNum) -> new UnbalancedJournal(
                UUID.fromString(rs.getString("id")),
                rs.getBigDecimal("dr"),
                rs.getBigDecimal("cr")
        ), Timestamp.from(testStart));
    }

    public List<HashChainMaterial> hashChainMaterialsSince(Instant testStart) {
        String chainSql = """
                SELECT hc.chain_index, hc.prev_hash, hc.hash, je.id AS journal_entry_id,
                       je.entry_type, je.reference_id, je.idempotency_key, je.actor_user_id
                FROM ledger.hash_chain hc
                JOIN ledger.journal_entries je ON je.id = hc.journal_entry_id
                WHERE je.created_at >= ?
                ORDER BY hc.chain_index
                """;

        List<HashChainMaterial> materials = jdbc.query(chainSql, (rs, rowNum) -> new HashChainMaterial(
                rs.getLong("chain_index"),
                rs.getString("prev_hash"),
                rs.getString("hash"),
                UUID.fromString(rs.getString("journal_entry_id")),
                rs.getString("entry_type"),
                rs.getString("reference_id"),
                rs.getString("idempotency_key"),
                rs.getString("actor_user_id"),
                new ArrayList<>())
        , Timestamp.from(testStart));

        String linesSql = """
                SELECT jl.journal_entry_id, acc.account_code, jl.line_type, jl.amount, jl.currency
                FROM ledger.journal_lines jl
                JOIN ledger.accounts acc ON acc.id = jl.account_id
                WHERE jl.journal_entry_id = ?
                """;

        for (HashChainMaterial material : materials) {
            List<HashLineMaterial> lines = jdbc.query(linesSql,
                    (rs, rowNum) -> new HashLineMaterial(
                            rs.getString("account_code"),
                            rs.getString("line_type"),
                            rs.getBigDecimal("amount"),
                            rs.getString("currency")),
                    material.journalEntryId());
            material.lines().addAll(lines);
        }

        return materials;
    }

    public long activeReservationCount(UUID propertyId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM market.property_reservations WHERE property_id=? AND status='ACTIVE'",
                Long.class,
                propertyId
        );
    }

    public long acceptedOfferCount(UUID propertyId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM market.offers WHERE property_id=? AND status='ACCEPTED'",
                Long.class,
                propertyId
        );
    }

    public long unsettledDisbursementCount(UUID milestoneId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM ops.disbursement_orders WHERE milestone_id=? AND status<>'SETTLED'",
                Long.class,
                milestoneId
        );
    }

    public long disbursementCountByBusinessKey(String businessKey) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM ops.disbursement_orders WHERE business_key=?",
                Long.class,
                businessKey
        );
    }

    public long escrowCountByBusinessKey(String businessKey) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM ops.escrows WHERE business_key=?",
                Long.class,
                businessKey
        );
    }

    public long outboxCountByIdempotencyKey(String idempotencyKey) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM ops.outbox_events WHERE idempotency_key=?",
                Long.class,
                idempotencyKey
        );
    }

    public long webhookCountByEventId(String eventId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM ops.webhook_events WHERE event_id=?",
                Long.class,
                eventId
        );
    }

    public long ledgerEntryCount(String entryType, String referenceId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM ledger.journal_entries WHERE entry_type=? AND reference_id=?",
                Long.class,
                entryType,
                referenceId
        );
    }

    public long ledgerEntryCountByIdempotency(String entryType, String referenceId, String idempotencyKey) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM ledger.journal_entries WHERE entry_type=? AND reference_id=? AND idempotency_key=?",
                Long.class,
                entryType,
                referenceId,
                idempotencyKey
        );
    }

    public long ledgerLineCountByEntryAndAccount(String entryType, String referenceId, String accountCode) {
        return jdbc.queryForObject(
                """
                SELECT count(*)
                FROM ledger.journal_entries je
                JOIN ledger.journal_lines jl ON jl.journal_entry_id = je.id
                JOIN ledger.accounts acc ON acc.id = jl.account_id
                WHERE je.entry_type = ?
                  AND je.reference_id = ?
                  AND acc.account_code = ?
                """,
                Long.class,
                entryType,
                referenceId,
                accountCode
        );
    }

    public BigDecimal ledgerLineAmountByEntryAccountAndType(String entryType,
                                                            String referenceId,
                                                            String accountCode,
                                                            String lineType) {
        BigDecimal amount = jdbc.queryForObject(
                """
                SELECT COALESCE(SUM(jl.amount), 0)
                FROM ledger.journal_entries je
                JOIN ledger.journal_lines jl ON jl.journal_entry_id = je.id
                JOIN ledger.accounts acc ON acc.id = jl.account_id
                WHERE je.entry_type = ?
                  AND je.reference_id = ?
                  AND acc.account_code = ?
                  AND jl.line_type = ?
                """,
                BigDecimal.class,
                entryType,
                referenceId,
                accountCode,
                lineType
        );
        return amount == null ? BigDecimal.ZERO : amount;
    }

    public record UnbalancedJournal(UUID journalEntryId, BigDecimal debits, BigDecimal credits) {
    }

    public record HashLineMaterial(String accountCode, String lineType, BigDecimal amount, String currency) {
    }

    public record HashChainMaterial(long chainIndex,
                                    String prevHash,
                                    String hash,
                                    UUID journalEntryId,
                                    String entryType,
                                    String referenceId,
                                    String idempotencyKey,
                                    String actorUserId,
                                    List<HashLineMaterial> lines) {
    }
}
