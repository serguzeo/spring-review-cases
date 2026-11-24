package review.orders;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/*
 * История показывает по 50 заказов текущего клиента вместе с позициями.
 * Сначала идут новые заказы. При неизменных данных соседние страницы должны
 * содержать разные заказы. Существующий HTTP-фильтр после аутентификации
 * устанавливает customerId. В таблице около 10 млн заказов.
 *
 * CREATE TABLE orders (
 *     id bigint PRIMARY KEY, customer_id bigint NOT NULL, created_at timestamptz NOT NULL
 * );
 * CREATE TABLE order_items (
 *     id bigint PRIMARY KEY, order_id bigint NOT NULL REFERENCES orders(id),
 *     sku text NOT NULL, quantity integer NOT NULL
 * );
 * CREATE INDEX order_items_order_idx ON order_items (order_id);
 */
@RestController
public class OrderHistory {
    private static final int PAGE_SIZE = 50;
    private final EntityManager entityManager;

    public OrderHistory(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @GetMapping("/orders")
    @Transactional(readOnly = true)
    public List<OrderView> list(@RequestAttribute("customerId") long customerId,
                                @RequestParam(name = "offset", defaultValue = "0") int offset) {
        if (offset < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid offset");
        }

        return entityManager.createQuery("""
            SELECT o FROM CustomerOrder o
            WHERE o.customerId = :customerId
            ORDER BY o.createdAt DESC
            """, CustomerOrder.class)
            .setParameter("customerId", customerId)
            .setFirstResult(offset)
            .setMaxResults(PAGE_SIZE)
            .getResultList().stream()
            .map(order -> new OrderView(order.id, order.createdAt, order.items.stream()
                .map(item -> new ItemView(item.sku, item.quantity))
                .toList()))
            .toList();
    }

    public record OrderView(long id, Instant createdAt, List<ItemView> items) {}

    public record ItemView(String sku, int quantity) {}

    @Entity
    @Table(name = "orders")
    public static class CustomerOrder {
        @Id
        private long id;
        @Column(name = "customer_id")
        private long customerId;
        @Column(name = "created_at")
        private Instant createdAt;
        @OneToMany(fetch = FetchType.LAZY)
        @JoinColumn(name = "order_id")
        private List<OrderItem> items;
    }

    @Entity
    @Table(name = "order_items")
    public static class OrderItem {
        @Id
        private long id;
        private String sku;
        private int quantity;
    }
}
