SET @add_image_url = IF(
    (
        SELECT COUNT(*)
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'fields'
          AND COLUMN_NAME = 'image_url'
    ) = 0,
    'ALTER TABLE fields ADD COLUMN image_url VARCHAR(500)',
    'SELECT 1'
);
PREPARE add_image_url_stmt FROM @add_image_url;
EXECUTE add_image_url_stmt;
DEALLOCATE PREPARE add_image_url_stmt;

SET @add_description = IF(
    (
        SELECT COUNT(*)
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'fields'
          AND COLUMN_NAME = 'description'
    ) = 0,
    'ALTER TABLE fields ADD COLUMN description TEXT',
    'SELECT 1'
);
PREPARE add_description_stmt FROM @add_description;
EXECUTE add_description_stmt;
DEALLOCATE PREPARE add_description_stmt;
