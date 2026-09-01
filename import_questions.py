import sys
import os
import openpyxl
import psycopg2

DATABASE_URL = os.environ.get("DATABASE_URL")
if DATABASE_URL and DATABASE_URL.startswith("jdbc:"):
    DATABASE_URL = DATABASE_URL.removeprefix("jdbc:")
if not DATABASE_URL:
    print("Переменная DATABASE_URL не задана")
    sys.exit(1)

XLSX_PATH = sys.argv[1] if len(sys.argv) > 1 else "Вопросы_ПДД_шаблон.xlsx"

wb = openpyxl.load_workbook(XLSX_PATH, data_only=True)
ws = wb["Вопросы"]

conn = psycopg2.connect(DATABASE_URL)
cur = conn.cursor()

inserted = 0
updated = 0
skipped = 0

for row in ws.iter_rows(min_row=2, values_only=True):
    test_number, image_filename, question_text, option_a, option_b, option_c, option_d, correct_option, explanation = row[:9]

    if not test_number or not image_filename or not question_text:
        skipped += 1
        continue

    filename = str(image_filename).strip()
    if not filename.lower().endswith(".jpg"):
        filename += ".jpg"

    cur.execute("SELECT id FROM questions WHERE image_filename = %s", (filename,))
    existing = cur.fetchone()

    if existing:
        cur.execute("""
            UPDATE questions
            SET test_number = %s, question_text = %s, option_a = %s, option_b = %s,
                option_c = %s, option_d = %s, correct_option = %s, explanation = %s
            WHERE image_filename = %s
        """, (test_number, question_text, option_a, option_b, option_c, option_d, correct_option, explanation, filename))
        updated += 1
    else:
        cur.execute("""
            INSERT INTO questions
            (test_number, image_filename, question_text, option_a, option_b, option_c, option_d, correct_option, explanation)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
        """, (test_number, filename, question_text, option_a, option_b, option_c, option_d, correct_option, explanation))
        inserted += 1

conn.commit()
cur.close()
conn.close()

print(f"Добавлено: {inserted}, обновлено: {updated}, пропущено (пустые строки): {skipped}")