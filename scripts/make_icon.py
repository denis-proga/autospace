from PIL import Image

img = Image.open("playstore-icon-512.png")
img.save("app_icon.ico", sizes=[(16,16), (32,32), (48,48), (64,64), (128,128), (256,256)])
print("Готово: app_icon.ico")