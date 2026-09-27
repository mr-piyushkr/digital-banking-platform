-- Reference data. The ROLE_ prefix is kept in the database because Spring
-- Security's hasRole() prepends it when matching authorities.
--
-- The seeded admin user arrives in V3, once the application owns a BCrypt
-- encoder and the hash can be generated rather than pasted.

INSERT INTO roles (name) VALUES
    ('ROLE_CUSTOMER'),
    ('ROLE_ADMIN'),
    ('ROLE_AUDITOR');
