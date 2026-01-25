from PIL import Image

TEXTURE_PATH = r"d:\minecraftModsD\Manaborn\src\main\resources\assets\manaborn\textures\item\spawn_egg_mage.png"
SCALE = 1.25  # stronger upscale

img = Image.open(TEXTURE_PATH)
width, height = img.size
new_size = (int(width * SCALE), int(height * SCALE))

scaled = img.resize(new_size, Image.NEAREST)

left = (new_size[0] - width) // 2
upper = (new_size[1] - height) // 2
right = left + width
lower = upper + height

cropped = scaled.crop((left, upper, right, lower))
cropped.save(TEXTURE_PATH)
print("Scaled by", SCALE, "and center-cropped to", (width, height))
