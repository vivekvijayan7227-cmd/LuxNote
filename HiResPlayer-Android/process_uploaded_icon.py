import os
import shutil
from PIL import Image, ImageDraw

def process_icon():
    src_path = r"C:/Users/Vivek/.gemini/antigravity/brain/03051d66-ab2f-4159-9a29-b77e7e06d9e2/.user_uploaded/media_1792163194555_b0d80521.jpg"
    orig = Image.open(src_path).convert("RGBA")
    w, h = orig.size # 1024, 1024
    
    res_dir = r"d:\AudioPlayer - Copy\HiResPlayer-Android\app\src\main\res"
    
    # 1. Update ic_launcher_background.xml
    bg_xml_path = os.path.join(res_dir, "drawable", "ic_launcher_background.xml")
    bg_xml_content = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#000000"
        android:pathData="M0,0h108v108h-108z" />
</vector>
"""
    with open(bg_xml_path, "w", encoding="utf-8") as f:
        f.write(bg_xml_content)
    print("Updated ic_launcher_background.xml to pure black background.")
    
    # 2. Remove any old ic_launcher_foreground.xml to prevent AAPT2 collision
    fg_xml_path = os.path.join(res_dir, "drawable", "ic_launcher_foreground.xml")
    if os.path.exists(fg_xml_path):
        os.remove(fg_xml_path)
        print("Removed ic_launcher_foreground.xml to allow bitmap foreground.")
        
    # 3. Create Adaptive Foreground (Centered on a 108dp canvas, safe zone 72dp)
    # The vinyl squircle is scaled to 84% so that the entire vinyl disc and glow
    # sit cleanly inside the 72dp circular / squircle safe zone.
    # At 432x432 (xxxhdpi 108dp):
    adaptive_densities = {
        "mdpi": 108,
        "hdpi": 162,
        "xhdpi": 216,
        "xxhdpi": 324,
        "xxxhdpi": 432
    }
    
    # Create base 1080x1080 adaptive canvas
    base_canvas = Image.new("RGBA", (1080, 1080), (0, 0, 0, 0))
    scale_factor = 0.84
    content_dim = int(1080 * scale_factor) # ~907px
    scaled_orig = orig.resize((content_dim, content_dim), Image.Resampling.LANCZOS)
    offset = (1080 - content_dim) // 2
    base_canvas.paste(scaled_orig, (offset, offset))
    
    for density_name, px_size in adaptive_densities.items():
        draw_dir = os.path.join(res_dir, f"drawable-{density_name}")
        os.makedirs(draw_dir, exist_ok=True)
        target_img = base_canvas.resize((px_size, px_size), Image.Resampling.LANCZOS)
        target_path = os.path.join(draw_dir, "ic_launcher_foreground.png")
        target_img.save(target_path, "PNG")
        print(f"Saved adaptive foreground ({px_size}x{px_size}): {target_path}")
        
    # Also save fallback in drawable/
    fallback_fg = base_canvas.resize((432, 432), Image.Resampling.LANCZOS)
    fallback_fg.save(os.path.join(res_dir, "drawable", "ic_launcher_foreground.png"), "PNG")
    
    # 4. Create Standard & Round Mipmap Icons
    # Anti-aliased Squircle Mask for standard icon
    mask_dim = 2048
    mask_im = Image.new("L", (mask_dim, mask_dim), 0)
    draw_mask = ImageDraw.Draw(mask_im)
    draw_mask.rounded_rectangle([100, 100, 1948, 1948], radius=420, fill=255)
    mask_1024 = mask_im.resize((w, h), Image.Resampling.LANCZOS)
    
    squircle_icon = orig.copy()
    squircle_icon.putalpha(mask_1024)
    
    # Anti-aliased Round Mask for circular icon
    round_mask_im = Image.new("L", (mask_dim, mask_dim), 0)
    round_draw = ImageDraw.Draw(round_mask_im)
    round_draw.ellipse([100, 100, 1948, 1948], fill=255)
    round_mask_1024 = round_mask_im.resize((w, h), Image.Resampling.LANCZOS)
    
    round_icon = orig.copy()
    round_icon.putalpha(round_mask_1024)
    
    mipmap_densities = {
        "mdpi": 48,
        "hdpi": 72,
        "xhdpi": 96,
        "xxhdpi": 144,
        "xxxhdpi": 192
    }
    
    for name, size in mipmap_densities.items():
        folder = os.path.join(res_dir, f"mipmap-{name}")
        os.makedirs(folder, exist_ok=True)
        
        sq_sized = squircle_icon.resize((size, size), Image.Resampling.LANCZOS)
        sq_path = os.path.join(folder, "ic_launcher.webp")
        sq_sized.save(sq_path, "WEBP", quality=100)
        
        rd_sized = round_icon.resize((size, size), Image.Resampling.LANCZOS)
        rd_path = os.path.join(folder, "ic_launcher_round.webp")
        rd_sized.save(rd_path, "WEBP", quality=100)
        print(f"Generated mipmap {name} ({size}x{size}): {sq_path} & {rd_path}")
        
    # 5. Update mipmap-anydpi-v26 XML files
    anydpi_dir = os.path.join(res_dir, "mipmap-anydpi-v26")
    os.makedirs(anydpi_dir, exist_ok=True)
    
    adaptive_xml = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
"""
    with open(os.path.join(anydpi_dir, "ic_launcher.xml"), "w", encoding="utf-8") as f:
        f.write(adaptive_xml)
    with open(os.path.join(anydpi_dir, "ic_launcher_round.xml"), "w", encoding="utf-8") as f:
        f.write(adaptive_xml)
    print("Updated adaptive XMLs in mipmap-anydpi-v26.")
    
    # 6. Save 512x512 preview image to project root and artifact directory
    p512 = squircle_icon.resize((512, 512), Image.Resampling.LANCZOS)
    p512.save(r"d:\AudioPlayer - Copy\app_icon_512.png", "PNG")
    
    artifact_dir = r"C:\Users\Vivek\.gemini\antigravity\brain\03051d66-ab2f-4159-9a29-b77e7e06d9e2"
    p512.save(os.path.join(artifact_dir, "app_icon_512.png"), "PNG")
    print("Exported 512x512 master icon.")

if __name__ == "__main__":
    process_icon()
