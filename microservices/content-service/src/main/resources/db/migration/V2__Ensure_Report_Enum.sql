-- Ensure reports columns use ENUM matching @Enumerated(EnumType.STRING) entities.
-- Hibernate 6 MySQL dialect prefers ENUM; this formalises the prior ddl-auto drift.
ALTER TABLE reports MODIFY COLUMN target_type ENUM('POST','COMMENT') NOT NULL;
ALTER TABLE reports MODIFY COLUMN status ENUM('PENDING','REVIEWED','RESOLVED','DISMISSED') NOT NULL DEFAULT 'PENDING';
