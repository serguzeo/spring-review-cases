package review.invoice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;

/*
 * Итог счёта равен сумме стоимостей позиций за вычетом скидки на весь счёт.
 * Округление до копеек по HALF_UP выполняется один раз, после применения скидки.
 * Две позиции по 19,95 ₽ при скидке 10% дают итог 35,91 ₽.
 * Контроллер передаёт проверенные значения. Цены приходят в рублях с точностью
 * до копеек, количество положительное, скидка задана целым числом от 0 до 100.
 */
@Service
public class InvoiceTotal {
    private double subtotal;

    public BigDecimal calculate(List<Line> lines, int discountPercent) {
        subtotal = 0;
        for (Line line : lines) {
            subtotal += line.unitPrice().doubleValue() * line.quantity();
        }

        double total = subtotal * (1 - discountPercent / 100.0);
        return BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP);
    }

    public record Line(BigDecimal unitPrice, int quantity) {}
}
