from PIL import Image

img = Image.open("playstore-icon-512.png")

sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

import os

res_path = "app/androidApp/src/main/res"

for folder, size in sizes.items():
    target_dir = os.path.join(res_path, folder)
    os.makedirs(target_dir, exist_ok=True)
    resized = img.resize((size, size), Image.LANCZOS)
    resized.save(os.path.join(target_dir, "ic_launcher.png"))
    resized.save(os.path.join(target_dir, "ic_launcher_round.png"))

print("Готово — иконки разложены по mipmap-папкам")