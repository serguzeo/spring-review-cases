package review.reservation;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * При оформлении заказа нужно зарезервировать товар. Резерв и изменение остатка
 * применяются атомарно. Количество положительное, товар есть в каталоге.
 * Клиент после таймаута может повторить запрос с теми же параметрами и requestId.
 * Такой повтор должен вернуть идентификатор того же резерва. Ключи выдаются
 * отдельно для каждого клиента. Сервис работает в нескольких экземплярах.
 * PostgreSQL, изоляция READ COMMITTED.
 *
 * CREATE TABLE stock (
 *     sku text PRIMARY KEY,
 *     available integer NOT NULL CHECK (available >= 0)
 * );
 * CREATE TABLE reservations (
 *     id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 *     customer_id bigint NOT NULL,
 *     request_id text NOT NULL,
 *     sku text NOT NULL REFERENCES stock(sku),
 *     quantity integer NOT NULL CHECK (quantity > 0)
 * );
 */
@Service
public class StockReservation {
    private final JdbcTemplate jdbc;

    public StockReservation(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public long reserve(long customerId, String requestId, String sku, int quantity) {
        List<Long> existing = jdbc.queryForList("""
            SELECT id FROM reservations WHERE customer_id = ? AND request_id = ?
            """, Long.class, customerId, requestId);
        if (!existing.isEmpty()) {
            return existing.getFirst();
        }

        int available = jdbc.queryForObject(
            "SELECT available FROM stock WHERE sku = ?", Integer.class, sku);
        if (available < quantity) {
            throw new IllegalStateException("Insufficient stock");
        }

        jdbc.update("UPDATE stock SET available = ? WHERE sku = ?", available - quantity, sku);
        return jdbc.queryForObject("""
            INSERT INTO reservations (customer_id, request_id, sku, quantity)
            VALUES (?, ?, ?, ?)
            RETURNING id
            """, Long.class, customerId, requestId, sku, quantity);
    }
}
