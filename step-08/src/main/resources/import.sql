INSERT INTO customer (id, firstName, lastName) VALUES (1, 'Barkus', 'Aurelius');
INSERT INTO customer (id, firstName, lastName) VALUES (2, 'Hairy', 'Pawter');
INSERT INTO customer (id, firstName, lastName) VALUES (3, 'Bark', 'Wahlberg');
INSERT INTO customer (id, firstName, lastName) VALUES (4, 'Jimmy', 'Chew');
INSERT INTO customer (id, firstName, lastName) VALUES (5, 'Mary', 'Puppins');

ALTER SEQUENCE customer_seq RESTART WITH 5;

-- Barkus Aurelius (id 1): three bookings exercising the cancellation policy relative to today.
-- Booking 1: starts in 30 days (> 11 days out), 5 days long (>= 4)  -> CANCELLABLE
INSERT INTO booking (id, customer_id, dateFrom, dateTo) VALUES (1, 1, CURRENT_DATE + 30, CURRENT_DATE + 35);
-- Booking 2: starts in 45 days but only 2 days long                 -> REFUSED (period < 4 days)
INSERT INTO booking (id, customer_id, dateFrom, dateTo) VALUES (2, 1, CURRENT_DATE + 45, CURRENT_DATE + 47);
-- Booking 3: starts in 5 days (< 11 days out)                       -> REFUSED (too late to cancel)
INSERT INTO booking (id, customer_id, dateFrom, dateTo) VALUES (3, 1, CURRENT_DATE + 5, CURRENT_DATE + 12);

INSERT INTO booking (id, customer_id, dateFrom, dateTo) VALUES (4, 2, CURRENT_DATE + 20, CURRENT_DATE + 25);
INSERT INTO booking (id, customer_id, dateFrom, dateTo) VALUES (5, 2, CURRENT_DATE + 60, CURRENT_DATE + 65);

INSERT INTO booking (id, customer_id, dateFrom, dateTo) VALUES (7, 3, CURRENT_DATE + 15, CURRENT_DATE + 20);
INSERT INTO booking (id, customer_id, dateFrom, dateTo) VALUES (8, 3, CURRENT_DATE + 90, CURRENT_DATE + 96);
INSERT INTO booking (id, customer_id, dateFrom, dateTo) VALUES (9, 3, CURRENT_DATE + 120, CURRENT_DATE + 126);

INSERT INTO booking (id, customer_id, dateFrom, dateTo) VALUES (10, 4, CURRENT_DATE + 25, CURRENT_DATE + 30);
INSERT INTO booking (id, customer_id, dateFrom, dateTo) VALUES (11, 4, CURRENT_DATE + 50, CURRENT_DATE + 55);
INSERT INTO booking (id, customer_id, dateFrom, dateTo) VALUES (12, 4, CURRENT_DATE + 75, CURRENT_DATE + 82);

ALTER SEQUENCE booking_seq RESTART WITH 12;
