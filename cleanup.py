import os
import psycopg2

DATABASE_URL = os.environ.get("DATABASE_URL")
if DATABASE_URL and DATABASE_URL.startswith("jdbc:"):
    DATABASE_URL = DATABASE_URL.removeprefix("jdbc:")

conn = psycopg2.connect(DATABASE_URL)
cur = conn.cursor()
cur.execute("DELETE FROM questions WHERE question_key = %s", ("question-47-b",))
print(f"Удалено записей: {cur.rowcount}")
conn.commit()
cur.close()
conn.close()