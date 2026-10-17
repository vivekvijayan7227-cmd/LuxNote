import os
import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter

def create_hires_studio_icon(size=1024, is_round=False):
    # Supersampling 2x for ultra smooth curves
    scale = 2
    dim = size * scale
    center = dim / 2
    
    # 1. Base Image
    im = Image.new("RGBA", (dim, dim), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)
    
    # Define Clipping Mask for Round vs Squircle
    mask = Image.new("L", (dim, dim), 0)
    mask_draw = ImageDraw.Draw(mask)
    margin = int(16 * scale)
    radius = int(190 * scale) # Squircle corner radius
    
    if is_round:
        mask_draw.ellipse([margin, margin, dim - margin, dim - margin], fill=255)
    else:
        # High-end squircle
        mask_draw.rounded_rectangle([margin, margin, dim - margin, dim - margin], radius=radius, fill=255)
        
    # 2. Draw Obsidian Gradient Background on a separate layer
    bg = Image.new("RGBA", (dim, dim), (0, 0, 0, 255))
    bg_draw = ImageDraw.Draw(bg)
    
    # Radial dark blue-obsidian glow
    max_r = dim * 0.72
    for r in range(int(max_r), 0, -3):
        frac = r / max_r
        # #07090E to #182033
        red = int(7 + (22 - 7) * (1.0 - frac**1.4))
        green = int(9 + (30 - 9) * (1.0 - frac**1.4))
        blue = int(14 + (48 - 14) * (1.0 - frac**1.4))
        bg_draw.ellipse([center - r, center - r, center + r, center + r], fill=(red, green, blue, 255))
        
    # Concentric Vinyl / Acoustic Grooves
    groove_radii = [0.42, 0.40, 0.38, 0.36, 0.34, 0.32, 0.30]
    for gr in groove_radii:
        r = int(dim * gr)
        bg_draw.ellipse([center - r, center - r, center + r, center + r], 
                        outline=(0, 240, 255, 14), width=int(1.2 * scale))
        
    # Outer Bezel / Rim Accent (Gold Chamfer)
    if is_round:
        bg_draw.ellipse([margin + 4, margin + 4, dim - margin - 4, dim - margin - 4], 
                        outline=(229, 169, 60, 180), width=int(4 * scale))
        bg_draw.ellipse([margin + 12, margin + 12, dim - margin - 12, dim - margin - 12], 
                        outline=(0, 240, 255, 60), width=int(1.5 * scale))
    else:
        bg_draw.rounded_rectangle([margin + 4, margin + 4, dim - margin - 4, dim - margin - 4], 
                                  radius=radius - 4, outline=(229, 169, 60, 180), width=int(4 * scale))
        bg_draw.rounded_rectangle([margin + 12, margin + 12, dim - margin - 12, dim - margin - 12], 
                                  radius=radius - 12, outline=(0, 240, 255, 60), width=int(1.5 * scale))
        
    # Apply Mask to Background
    im.paste(bg, (0, 0), mask)
    
    # 3. Draw Foreground: Studio Headphone + Spectrum Bars + Hi-Res Emblem
    fg = Image.new("RGBA", (dim, dim), (0, 0, 0, 0))
    fg_draw = ImageDraw.Draw(fg)
    
    # --- Headphone Arc ---
    arc_box = [center - 270 * scale, center - 290 * scale, center + 270 * scale, center + 250 * scale]
    # Outer arc
    fg_draw.arc(arc_box, start=195, end=345, fill=(35, 42, 60, 255), width=int(32 * scale))
    # Titanium / Gold Top Arc Accent
    arc_box_inner = [center - 270 * scale, center - 290 * scale, center + 270 * scale, center + 250 * scale]
    fg_draw.arc(arc_box_inner, start=205, end=335, fill=(229, 169, 60, 255), width=int(6 * scale))
    
    # --- Headphone Gimbal Pivots ---
    left_x = center - 245 * scale
    right_x = center + 245 * scale
    gimbal_y = center - 40 * scale
    
    # Gimbal Brackets (Gold & Titanium)
    for gx in [left_x, right_x]:
        fg_draw.rounded_rectangle([gx - 14 * scale, gimbal_y - 30 * scale, gx + 14 * scale, gimbal_y + 20 * scale],
                                  radius=int(6 * scale), fill=(25, 30, 42, 255), outline=(229, 169, 60, 255), width=int(3 * scale))
        fg_draw.ellipse([gx - 6 * scale, gimbal_y - 8 * scale, gx + 6 * scale, gimbal_y + 8 * scale],
                        fill=(0, 240, 255, 255))
        
    # --- Studio Earcups ---
    cup_w = 64 * scale
    cup_h = 175 * scale
    cup_y = center + 30 * scale
    
    # Left Earcup
    fg_draw.rounded_rectangle([left_x - cup_w / 2, cup_y - cup_h / 2, left_x + cup_w / 2, cup_y + cup_h / 2],
                              radius=int(32 * scale), fill=(18, 22, 32, 255), outline=(229, 169, 60, 255), width=int(4 * scale))
    # Left Cushion inner laser cyan ring
    fg_draw.rounded_rectangle([left_x - cup_w / 2 + 8 * scale, cup_y - cup_h / 2 + 10 * scale, 
                               left_x + cup_w / 2 - 8 * scale, cup_y + cup_h / 2 - 10 * scale],
                              radius=int(24 * scale), fill=(10, 12, 18, 255), outline=(0, 240, 255, 180), width=int(2 * scale))
                              
    # Right Earcup
    fg_draw.rounded_rectangle([right_x - cup_w / 2, cup_y - cup_h / 2, right_x + cup_w / 2, cup_y + cup_h / 2],
                              radius=int(32 * scale), fill=(18, 22, 32, 255), outline=(229, 169, 60, 255), width=int(4 * scale))
    # Right Cushion inner laser cyan ring
    fg_draw.rounded_rectangle([right_x - cup_w / 2 + 8 * scale, cup_y - cup_h / 2 + 10 * scale, 
                               right_x + cup_w / 2 - 8 * scale, cup_y + cup_h / 2 - 10 * scale],
                              radius=int(24 * scale), fill=(10, 12, 18, 255), outline=(0, 240, 255, 180), width=int(2 * scale))
                              
    # --- Center Dynamic Audio Equalizer Spectrum Bars (7 Studio Bars) ---
    bar_width = int(22 * scale)
    bar_spacing = int(14 * scale)
    num_bars = 7
    total_bars_width = num_bars * bar_width + (num_bars - 1) * bar_spacing
    start_x = center - total_bars_width / 2
    
    # Symmetrical studio VU levels
    bar_heights = [70, 120, 180, 230, 180, 120, 70]
    base_y = center + 55 * scale
    
    colors = [
        (0, 240, 255, 240),    # Bar 1: Laser Cyan
        (0, 240, 255, 255),    # Bar 2: Cyan
        (0, 230, 180, 255),    # Bar 3: Cyan-Green
        (255, 215, 0, 255),    # Bar 4: Master Center Gold Peak
        (0, 230, 180, 255),    # Bar 5: Cyan-Green
        (0, 240, 255, 255),    # Bar 6: Cyan
        (0, 240, 255, 240)     # Bar 7: Laser Cyan
    ]
    
    for i in range(num_bars):
        bx = start_x + i * (bar_width + bar_spacing)
        bh = bar_heights[i] * scale
        by1 = base_y - bh / 2
        by2 = base_y + bh / 2
        col = colors[i]
        
        # Draw bar with rounded pill caps
        fg_draw.rounded_rectangle([bx, by1, bx + bar_width, by2], radius=bar_width / 2, fill=col)
        # Bar center core highlight
        fg_draw.rounded_rectangle([bx + 4 * scale, by1 + 6 * scale, bx + bar_width - 4 * scale, by2 - 6 * scale],
                                  radius=bar_width / 4, fill=(255, 255, 255, 90))
                                  
    # --- Hi-Res Gold Badge below Spectrum ---
    badge_y = center + 185 * scale
    badge_w = 190 * scale
    badge_h = 44 * scale
    fg_draw.rounded_rectangle([center - badge_w / 2, badge_y - badge_h / 2, center + badge_w / 2, badge_y + badge_h / 2],
                              radius=int(10 * scale), fill=(18, 22, 32, 240), outline=(229, 169, 60, 255), width=int(2.5 * scale))
                              
    # "HI-RES" Text or Graphic Emblem inside Badge
    # Draw custom crisp geometric "HI-RES" letters using vector shapes
    letter_y = badge_y
    # H
    hx = center - 62 * scale
    fg_draw.line([(hx, letter_y - 12 * scale), (hx, letter_y + 12 * scale)], fill=(255, 215, 0, 255), width=int(3 * scale))
    fg_draw.line([(hx + 14 * scale, letter_y - 12 * scale), (hx + 14 * scale, letter_y + 12 * scale)], fill=(255, 215, 0, 255), width=int(3 * scale))
    fg_draw.line([(hx, letter_y), (hx + 14 * scale, letter_y)], fill=(255, 215, 0, 255), width=int(3 * scale))
    # I
    ix = center - 38 * scale
    fg_draw.line([(ix, letter_y - 12 * scale), (ix, letter_y + 12 * scale)], fill=(255, 215, 0, 255), width=int(3 * scale))
    # Dot
    fg_draw.ellipse([center - 24 * scale, letter_y - 2 * scale, center - 20 * scale, letter_y + 2 * scale], fill=(0, 240, 255, 255))
    # R
    rx = center - 12 * scale
    fg_draw.line([(rx, letter_y - 12 * scale), (rx, letter_y + 12 * scale)], fill=(255, 215, 0, 255), width=int(3 * scale))
    fg_draw.arc([rx - 2 * scale, letter_y - 13 * scale, rx + 14 * scale, letter_y + 1 * scale], start=270, end=90, fill=(255, 215, 0, 255), width=int(3 * scale))
    fg_draw.line([(rx + 5 * scale, letter_y), (rx + 14 * scale, letter_y + 12 * scale)], fill=(255, 215, 0, 255), width=int(3 * scale))
    # E
    ex = center + 12 * scale
    fg_draw.line([(ex, letter_y - 12 * scale), (ex, letter_y + 12 * scale)], fill=(255, 215, 0, 255), width=int(3 * scale))
    fg_draw.line([(ex, letter_y - 12 * scale), (ex + 12 * scale, letter_y - 12 * scale)], fill=(255, 215, 0, 255), width=int(3 * scale))
    fg_draw.line([(ex, letter_y), (ex + 9 * scale, letter_y)], fill=(255, 215, 0, 255), width=int(3 * scale))
    fg_draw.line([(ex, letter_y + 12 * scale), (ex + 12 * scale, letter_y + 12 * scale)], fill=(255, 215, 0, 255), width=int(3 * scale))
    # S
    sx = center + 34 * scale
    fg_draw.arc([sx, letter_y - 12 * scale, sx + 14 * scale, letter_y], start=90, end=270, fill=(255, 215, 0, 255), width=int(3 * scale))
    fg_draw.arc([sx, letter_y, sx + 14 * scale, letter_y + 12 * scale], start=270, end=90, fill=(255, 215, 0, 255), width=int(3 * scale))
    fg_draw.line([(sx + 7 * scale, letter_y - 12 * scale), (sx + 14 * scale, letter_y - 12 * scale)], fill=(255, 215, 0, 255), width=int(3 * scale))
    fg_draw.line([(sx, letter_y + 12 * scale), (sx + 7 * scale, letter_y + 12 * scale)], fill=(255, 215, 0, 255), width=int(3 * scale))
    
    # Paste foreground onto base image
    im.paste(fg, (0, 0), fg)
    
    # Downsample to target size with highest quality Lanczos resampling
    final_icon = im.resize((size, size), Image.Resampling.LANCZOS)
    return final_icon

def create_adaptive_foreground(size=1024):
    # Adaptive foreground has transparent background and icon centered in inner 66%
    scale = 2
    dim = size * scale
    center = dim / 2
    
    fg = Image.new("RGBA", (dim, dim), (0, 0, 0, 0))
    fg_draw = ImageDraw.Draw(fg)
    
    # Scale content down slightly to fit within Android safe zone (inner 66%)
    content_scale = 0.85
    cs = scale * content_scale
    
    # Headphone Arc
    arc_box = [center - 270 * cs, center - 290 * cs, center + 270 * cs, center + 250 * cs]
    fg_draw.arc(arc_box, start=195, end=345, fill=(40, 48, 68, 255), width=int(34 * cs))
    fg_draw.arc(arc_box, start=205, end=335, fill=(229, 169, 60, 255), width=int(7 * cs))
    
    # Headphone Gimbal Pivots
    left_x = center - 245 * cs
    right_x = center + 245 * cs
    gimbal_y = center - 40 * cs
    
    for gx in [left_x, right_x]:
        fg_draw.rounded_rectangle([gx - 14 * cs, gimbal_y - 30 * cs, gx + 14 * cs, gimbal_y + 20 * cs],
                                  radius=int(6 * cs), fill=(25, 30, 42, 255), outline=(229, 169, 60, 255), width=int(3 * cs))
        fg_draw.ellipse([gx - 6 * cs, gimbal_y - 8 * cs, gx + 6 * cs, gimbal_y + 8 * cs],
                        fill=(0, 240, 255, 255))
        
    # Studio Earcups
    cup_w = 66 * cs
    cup_h = 180 * cs
    cup_y = center + 30 * cs
    
    # Left Earcup
    fg_draw.rounded_rectangle([left_x - cup_w / 2, cup_y - cup_h / 2, left_x + cup_w / 2, cup_y + cup_h / 2],
                              radius=int(33 * cs), fill=(20, 25, 36, 255), outline=(229, 169, 60, 255), width=int(4 * cs))
    fg_draw.rounded_rectangle([left_x - cup_w / 2 + 8 * cs, cup_y - cup_h / 2 + 10 * cs, 
                               left_x + cup_w / 2 - 8 * cs, cup_y + cup_h / 2 - 10 * cs],
                              radius=int(25 * cs), fill=(10, 12, 18, 255), outline=(0, 240, 255, 180), width=int(2 * cs))
                              
    # Right Earcup
    fg_draw.rounded_rectangle([right_x - cup_w / 2, cup_y - cup_h / 2, right_x + cup_w / 2, cup_y + cup_h / 2],
                              radius=int(33 * cs), fill=(20, 25, 36, 255), outline=(229, 169, 60, 255), width=int(4 * cs))
    fg_draw.rounded_rectangle([right_x - cup_w / 2 + 8 * cs, cup_y - cup_h / 2 + 10 * cs, 
                               right_x + cup_w / 2 - 8 * cs, cup_y + cup_h / 2 - 10 * cs],
                              radius=int(25 * cs), fill=(10, 12, 18, 255), outline=(0, 240, 255, 180), width=int(2 * cs))
                              
    # Center Spectrum Bars (7 Studio Bars)
    bar_width = int(22 * cs)
    bar_spacing = int(14 * cs)
    num_bars = 7
    total_bars_width = num_bars * bar_width + (num_bars - 1) * bar_spacing
    start_x = center - total_bars_width / 2
    
    bar_heights = [70, 120, 180, 230, 180, 120, 70]
    base_y = center + 55 * cs
    
    colors = [
        (0, 240, 255, 240),
        (0, 240, 255, 255),
        (0, 230, 180, 255),
        (255, 215, 0, 255),
        (0, 230, 180, 255),
        (0, 240, 255, 255),
        (0, 240, 255, 240)
    ]
    
    for i in range(num_bars):
        bx = start_x + i * (bar_width + bar_spacing)
        bh = bar_heights[i] * cs
        by1 = base_y - bh / 2
        by2 = base_y + bh / 2
        col = colors[i]
        
        fg_draw.rounded_rectangle([bx, by1, bx + bar_width, by2], radius=bar_width / 2, fill=col)
        fg_draw.rounded_rectangle([bx + 4 * cs, by1 + 6 * cs, bx + bar_width - 4 * cs, by2 - 6 * cs],
                                  radius=bar_width / 4, fill=(255, 255, 255, 90))
                                  
    # Hi-Res Gold Badge
    badge_y = center + 185 * cs
    badge_w = 190 * cs
    badge_h = 44 * cs
    fg_draw.rounded_rectangle([center - badge_w / 2, badge_y - badge_h / 2, center + badge_w / 2, badge_y + badge_h / 2],
                              radius=int(10 * cs), fill=(18, 22, 32, 240), outline=(229, 169, 60, 255), width=int(2.5 * cs))
                              
    letter_y = badge_y
    # H
    hx = center - 62 * cs
    fg_draw.line([(hx, letter_y - 12 * cs), (hx, letter_y + 12 * cs)], fill=(255, 215, 0, 255), width=int(3 * cs))
    fg_draw.line([(hx + 14 * cs, letter_y - 12 * cs), (hx + 14 * cs, letter_y + 12 * cs)], fill=(255, 215, 0, 255), width=int(3 * cs))
    fg_draw.line([(hx, letter_y), (hx + 14 * cs, letter_y)], fill=(255, 215, 0, 255), width=int(3 * cs))
    # I
    ix = center - 38 * cs
    fg_draw.line([(ix, letter_y - 12 * cs), (ix, letter_y + 12 * cs)], fill=(255, 215, 0, 255), width=int(3 * cs))
    # Dot
    fg_draw.ellipse([center - 24 * cs, letter_y - 2 * cs, center - 20 * cs, letter_y + 2 * cs], fill=(0, 240, 255, 255))
    # R
    rx = center - 12 * cs
    fg_draw.line([(rx, letter_y - 12 * cs), (rx, letter_y + 12 * cs)], fill=(255, 215, 0, 255), width=int(3 * cs))
    fg_draw.arc([rx - 2 * cs, letter_y - 13 * cs, rx + 14 * cs, letter_y + 1 * cs], start=270, end=90, fill=(255, 215, 0, 255), width=int(3 * cs))
    fg_draw.line([(rx + 5 * cs, letter_y), (rx + 14 * cs, letter_y + 12 * cs)], fill=(255, 215, 0, 255), width=int(3 * cs))
    # E
    ex = center + 12 * cs
    fg_draw.line([(ex, letter_y - 12 * cs), (ex, letter_y + 12 * cs)], fill=(255, 215, 0, 255), width=int(3 * cs))
    fg_draw.line([(ex, letter_y - 12 * cs), (ex + 12 * cs, letter_y - 12 * cs)], fill=(255, 215, 0, 255), width=int(3 * cs))
    fg_draw.line([(ex, letter_y), (ex + 9 * cs, letter_y)], fill=(255, 215, 0, 255), width=int(3 * cs))
    fg_draw.line([(ex, letter_y + 12 * cs), (ex + 12 * cs, letter_y + 12 * cs)], fill=(255, 215, 0, 255), width=int(3 * cs))
    # S
    sx = center + 34 * cs
    fg_draw.arc([sx, letter_y - 12 * cs, sx + 14 * cs, letter_y], start=90, end=270, fill=(255, 215, 0, 255), width=int(3 * cs))
    fg_draw.arc([sx, letter_y, sx + 14 * cs, letter_y + 12 * cs], start=270, end=90, fill=(255, 215, 0, 255), width=int(3 * cs))
    fg_draw.line([(sx + 7 * cs, letter_y - 12 * cs), (sx + 14 * cs, letter_y - 12 * cs)], fill=(255, 215, 0, 255), width=int(3 * cs))
    fg_draw.line([(sx, letter_y + 12 * cs), (sx + 7 * cs, letter_y + 12 * cs)], fill=(255, 215, 0, 255), width=int(3 * cs))
    
    final_fg = fg.resize((size, size), Image.Resampling.LANCZOS)
    return final_fg

def main():
    res_dir = r"d:\AudioPlayer - Copy\HiResPlayer-Android\app\src\main\res"
    
    # Density sizes:
    densities = {
        "mdpi": 48,
        "hdpi": 72,
        "xhdpi": 96,
        "xxhdpi": 144,
        "xxxhdpi": 192
    }
    
    print("Generating Studio-Grade Audiophile Icons...")
    
    # Generate 512x512 Master Preview
    master_icon = create_hires_studio_icon(512, is_round=False)
    master_path = os.path.join(r"d:\AudioPlayer - Copy", "app_icon_512.png")
    master_icon.save(master_path, "PNG")
    print(f"Master icon saved: {master_path}")
    
    # Generate each density
    for name, size in densities.items():
        folder = os.path.join(res_dir, f"mipmap-{name}")
        os.makedirs(folder, exist_ok=True)
        
        # Standard squircle icon
        sq = create_hires_studio_icon(size, is_round=False)
        sq_path = os.path.join(folder, "ic_launcher.webp")
        sq.save(sq_path, "WEBP", quality=100)
        
        # Round icon
        rd = create_hires_studio_icon(size, is_round=True)
        rd_path = os.path.join(folder, "ic_launcher_round.webp")
        rd.save(rd_path, "WEBP", quality=100)
        
        print(f"Generated {name} ({size}x{size}): {sq_path} and {rd_path}")
        
    print("All raster mipmaps generated successfully!")

if __name__ == "__main__":
    main()
