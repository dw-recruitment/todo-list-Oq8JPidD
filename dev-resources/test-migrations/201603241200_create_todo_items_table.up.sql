CREATE TABLE todo_items (
  id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  description VARCHAR(140),
  status TINYINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
