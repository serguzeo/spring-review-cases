# Код-ревью Java и Spring Boot

Каждое упражнение занимает один Java-файл. Комментарий в начале описывает задачу и содержит схему PostgreSQL там, где она нужна для ревью.

1. [Резервирование товара](src/main/java/review/reservation/StockReservation.java)
2. [Импорт прайс-листа](src/main/java/review/importing/PriceListImport.java)
3. [История заказов](src/main/java/review/orders/OrderHistory.java)
4. [Подтверждение отгрузки](src/main/java/review/shipment/ShipmentConfirmation.java)
5. [Расчёт суммы счёта](src/main/java/review/invoice/InvoiceTotal.java)

Для проверки компиляции нужен JDK 25. Запустите `./gradlew build` в корне проекта. PostgreSQL и брокер для сборки не требуются.
