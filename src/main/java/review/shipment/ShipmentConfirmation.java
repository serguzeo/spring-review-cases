package review.shipment;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * Склад подтверждает отгрузку. После перехода READY → CONFIRMED сервис доставки
 * должен получить событие, в том числе после перезапуска отправителя. Повторная
 * доставка одного события допустима. Если изменение статуса откатилось,
 * события быть не должно. Сервис доставки обрабатывает события по мере получения.
 * Вызов publish ждёт подтверждения брокера и при ошибке бросает RuntimeException.
 *
 * CREATE TABLE shipments (id bigint PRIMARY KEY, status text NOT NULL);
 */
@Service
public class ShipmentConfirmation {
    private final JdbcTemplate jdbc;
    private final ShipmentEvents events;

    public ShipmentConfirmation(JdbcTemplate jdbc, ShipmentEvents events) {
        this.jdbc = jdbc;
        this.events = events;
    }

    @Transactional
    public boolean confirm(long shipmentId) {
        int updated = jdbc.update("""
            UPDATE shipments SET status = 'CONFIRMED' WHERE id = ? AND status = 'READY'
            """, shipmentId);
        if (updated == 0) {
            return false;
        }

        events.publish(new ShipmentConfirmed(shipmentId));
        return true;
    }

    public record ShipmentConfirmed(long shipmentId) {}

    public interface ShipmentEvents {
        void publish(ShipmentConfirmed event);
    }
}
