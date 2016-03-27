INSERT INTO todo_list (id, name) VALUES
  (DEFAULT, 'Todo List of things todo');

--;;

INSERT INTO todo_items (id, description, status, todo_list_id) VALUES
  (DEFAULT, 'Create todo list', 1, IDENTITY()),
  (DEFAULT, 'Mail in ballot', 1, IDENTITY()),
  (DEFAULT, 'Get job at Democracy Works', 0, IDENTITY()),
  (DEFAULT, 'Get desperately needed coffee', 0, IDENTITY());
