#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""tools/art 의 그림(SVG)을 안드로이드 그림 파일(VectorDrawable)로 바꾼다.
그림을 고치면: python3 tools/svg2vd.py   (결과는 app/src/main/res/drawable/ 에 덮어쓴다)
쓰는 도형: path, circle, ellipse, rect(rx). 색은 #RRGGBB 또는 #RRGGBBAA."""
import glob, os, re
import xml.etree.ElementTree as ET

root = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
out_dir = os.path.join(root, "app", "src", "main", "res", "drawable")

def color(c):
    c = c.strip()
    if len(c) == 9:          # #RRGGBBAA → #AARRGGBB
        return "#" + c[7:9] + c[1:7]
    return "#FF" + c[1:]

def f(v):
    return ("%.3f" % v).rstrip("0").rstrip(".")

def ellipse(cx, cy, rx, ry):
    return "M%s,%sa%s,%s 0 1,0 %s,0a%s,%s 0 1,0 %s,0z" % (f(cx - rx), f(cy), f(rx), f(ry), f(2 * rx), f(rx), f(ry), f(-2 * rx))

def rect(x, y, w, h, r):
    r = min(r, w / 2, h / 2)
    if r <= 0:
        return "M%s,%sh%sv%sh%sz" % (f(x), f(y), f(w), f(h), f(-w))
    return ("M%s,%sh%sa%s,%s 0 0,1 %s,%sv%sa%s,%s 0 0,1 %s,%sh%sa%s,%s 0 0,1 %s,%sv%sa%s,%s 0 0,1 %s,%sz" % (
        f(x + r), f(y), f(w - 2 * r), f(r), f(r), f(r), f(r), f(h - 2 * r), f(r), f(r), f(-r), f(r),
        f(-(w - 2 * r)), f(r), f(r), f(-r), f(-r), f(-(h - 2 * r)), f(r), f(r), f(r), f(-r)))

for src in sorted(glob.glob(os.path.join(root, "tools", "art", "*.svg"))):
    svg = ET.parse(src).getroot()
    vx, vy, vw, vh = [float(v) for v in svg.get("viewBox").split()]
    name = os.path.splitext(os.path.basename(src))[0]
    lines = ['<?xml version="1.0" encoding="utf-8"?>',
             '<!-- tools/art/%s.svg 에서 만든 파일 (tools/svg2vd.py). 직접 고치지 말 것 -->' % name,
             '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
             '    android:width="%sdp" android:height="%sdp"' % (f(vw / 2), f(vh / 2)),
             '    android:viewportWidth="%s" android:viewportHeight="%s">' % (f(vw), f(vh)),
             '    <group android:translateX="%s" android:translateY="%s">' % (f(-vx), f(-vy))]
    for e in svg:
        tag = e.tag.split("}")[-1]
        g = lambda k, d=0: float(e.get(k, d))
        if tag == "path":
            d = re.sub(r"\s+", " ", e.get("d"))
        elif tag == "circle":
            d = ellipse(g("cx"), g("cy"), g("r"), g("r"))
        elif tag == "ellipse":
            d = ellipse(g("cx"), g("cy"), g("rx"), g("ry"))
        elif tag == "rect":
            d = rect(g("x"), g("y"), g("width"), g("height"), g("rx"))
        else:
            raise SystemExit("unsupported <%s> in %s" % (tag, src))
        attrs = ['android:pathData="%s"' % d]
        fill = e.get("fill", "#000000")
        if fill != "none":
            attrs.append('android:fillColor="%s"' % color(fill))
        if e.get("stroke"):
            attrs.append('android:strokeColor="%s"' % color(e.get("stroke")))
            attrs.append('android:strokeWidth="%s"' % e.get("stroke-width", "1"))
            if e.get("stroke-linecap"):
                attrs.append('android:strokeLineCap="%s"' % e.get("stroke-linecap"))
            if e.get("stroke-linejoin"):
                attrs.append('android:strokeLineJoin="%s"' % e.get("stroke-linejoin"))
        lines.append("        <path " + " ".join(attrs) + " />")
    lines += ["    </group>", "</vector>"]
    open(os.path.join(out_dir, name + ".xml"), "w", encoding="utf-8").write("\n".join(lines) + "\n")
    print(name, len(lines) - 8, "paths")
