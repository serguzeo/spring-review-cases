package review.importing;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * Загрузка прайс-листа обновляет цены существующих товаров. Если хотя бы одна
 * строка содержит неположительную цену или неизвестный SKU, все цены должны
 * остаться прежними. Оператор получает номер первой ошибочной строки.
 * CSV уже разобран. Строки передаются в порядке файла. Нумерация учитывает заголовок.
 * Контроллер вызывает importPrices через Spring-бин, своей транзакции у него нет.
 *
 * CREATE TABLE product_prices (
 *     sku text PRIMARY KEY,
 *     price numeric NOT NULL CHECK (price > 0)
 * );
 */
@Service
public class PriceListImport {
    private final JdbcTemplate jdbc;

    public PriceListImport(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int importPrices(List<PriceRow> rows) throws InvalidPriceRowException {
        applyPrices(rows);
        return rows.size();
    }

    @Transactional
    public void applyPrices(List<PriceRow> rows) throws InvalidPriceRowException {
        for (PriceRow row : rows) {
            if (row.price().signum() <= 0) {
                throw new InvalidPriceRowException(row.number(), "Price must be positive");
            }

            int updated = jdbc.update(
                "UPDATE product_prices SET price = ? WHERE sku = ?", row.price(), row.sku());
            if (updated == 0) {
                throw new InvalidPriceRowException(row.number(), "Unknown SKU");
            }
        }
    }

    public record PriceRow(int number, String sku, BigDecimal price) {}

    public static class InvalidPriceRowException extends Exception {
        public InvalidPriceRowException(int row, String reason) {
            super("Row %d: %s".formatted(row, reason));
        }
    }
}
