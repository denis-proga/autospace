import os
import psycopg2

DATABASE_URL = os.environ.get("DATABASE_URL")
if DATABASE_URL and DATABASE_URL.startswith("jdbc:"):
    DATABASE_URL = DATABASE_URL.removeprefix("jdbc:")

conn = psycopg2.connect(DATABASE_URL)
cur = conn.cursor()
cur.execute("SELECT id, question_key, image_filename FROM questions WHERE test_number = 2 ORDER BY id")
rows = cur.fetchall()
cur.close()
conn.close()

for i, (id_, key, filename) in enumerate(rows, start=1):
    print(f"{i}. id={id_} key={key} image_filename={filename}")