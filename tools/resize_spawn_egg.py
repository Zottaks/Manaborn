from PIL import Image

TEXTURE_PATH = r"d:\minecraftModsD\Manaborn\src\main\resources\assets\manaborn\textures\item\spawn_egg_mage.png"

img = Image.open(TEXTURE_PATH)
img = img.resize((32, 32), Image.NEAREST)
img.save(TEXTURE_PATH)
print("Resized to 32x32:", TEXTURE_PATH)
