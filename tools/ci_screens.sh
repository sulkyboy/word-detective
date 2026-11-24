#!/bin/bash
# 에뮬레이터에서 화면들을 띄워 찍는다 (GitHub Actions 의 screens 작업이 부른다).
# 결과는 shots/ 에 모이고, 작업이 screens 브랜치로 올린다.  공부 기록은 시험용 가짜 값을 넣는다.
set -u
PKG=kr.word.detective
OUT=shots
mkdir -p $OUT

adb wait-for-device
adb shell wm density 480              # 1080 px / 3 = 폭 360 dp : 갤럭시 기본 화면 폭과 같게
adb shell settings put system font_scale 1.0
adb shell settings put secure show_ime_with_hard_keyboard 0
adb shell settings put global hide_error_dialogs 1     # 에뮬레이터의 다른 앱이 멈췄다는 안내창이 화면을 가리지 않게
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb root || true                      # 내보내지 않은 화면을 직접 띄우려면 필요
sleep 4
adb wait-for-device
adb logcat -c || true

unblock() {   # '응답 없음' 안내창이 떠 있으면 [Wait] 를 눌러 치운다
  for k in 1 2 3; do
    adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
    adb exec-out cat /sdcard/ui.xml > "$OUT/ui.xml" 2>/dev/null
    grep -q "isn't responding\|keeps stopping" "$OUT/ui.xml" || return 0
    echo "dismissing system dialog"
    tap_now "Wait" || tap_now "Close app"
    sleep 1.5
  done
}

tap_now() {   # 방금 받아 둔 화면 정보에서 글자를 찾아 누른다
  xy=$(python3 - "$OUT/ui.xml" "$1" <<'PY'
import re, sys
x = open(sys.argv[1], encoding="utf-8", errors="replace").read()
for m in re.finditer(r'<node[^>]*?text="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', x):
    if m.group(1).strip() == sys.argv[2]:
        print((int(m.group(2)) + int(m.group(4))) // 2, (int(m.group(3)) + int(m.group(5))) // 2)
        break
PY
)
  [ -n "$xy" ] || return 1
  adb shell input tap $xy
}

shot() {   # 화면 찍기 + 화면에 보이는 글자 목록
  sleep "${2:-2.5}"
  unblock
  adb exec-out screencap -p > "$OUT/$1.png"
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb exec-out cat /sdcard/ui.xml > "$OUT/ui.xml" 2>/dev/null
  python3 - "$OUT/ui.xml" > "$OUT/$1.txt" <<'PY'
import re, sys
try:
    x = open(sys.argv[1], encoding="utf-8", errors="replace").read()
except Exception as e:
    print("no ui dump", e); sys.exit(0)
for m in re.finditer(r'<node[^>]*?text="([^"]*)"[^>]*?class="([^"]*)"[^>]*?bounds="([^"]*)"', x):
    if m.group(1).strip():
        print(m.group(3), m.group(2).split(".")[-1], m.group(1))
PY
}

tap() {    # 화면에 보이는 글자를 찾아 누른다
  unblock
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb exec-out cat /sdcard/ui.xml > "$OUT/ui.xml" 2>/dev/null
  xy=$(python3 - "$OUT/ui.xml" "$1" <<'PY'
import re, sys
x = open(sys.argv[1], encoding="utf-8", errors="replace").read()
for m in re.finditer(r'<node[^>]*?text="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', x):
    if m.group(1).strip() == sys.argv[2]:
        print((int(m.group(2)) + int(m.group(4))) // 2, (int(m.group(3)) + int(m.group(5))) // 2)
        break
PY
)
  if [ -n "$xy" ]; then adb shell input tap $xy; echo "tap '$1' at $xy"; else echo "tap '$1': not found"; fi
}

scroll_to() {   # 그 글자가 화면에 보일 때까지 위로 올린다 (최대 8번)
  for k in 1 2 3 4 5 6 7 8; do
    adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
    adb exec-out cat /sdcard/ui.xml > "$OUT/ui.xml" 2>/dev/null
    grep -q "text=\"$1\"" "$OUT/ui.xml" && return 0
    adb shell input swipe 540 1800 540 1100 400
    sleep 1
  done
  echo "scroll_to '$1': not found"
}

tap_desc() {   # 그림 단추를 설명 글(content-desc)로 찾아 누른다
  unblock
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb exec-out cat /sdcard/ui.xml > "$OUT/ui.xml" 2>/dev/null
  xy=$(python3 - "$OUT/ui.xml" "$1" <<'PY'
import re, sys
x = open(sys.argv[1], encoding="utf-8", errors="replace").read()
for m in re.finditer(r'<node[^>]*?content-desc="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', x):
    if m.group(1).strip() == sys.argv[2]:
        print((int(m.group(2)) + int(m.group(4))) // 2, (int(m.group(3)) + int(m.group(5))) // 2)
        break
PY
)
  if [ -n "$xy" ]; then adb shell input tap $xy; echo "tap desc '$1' at $xy"; else echo "tap desc '$1': not found"; fi
}

tap_in_row() {   # $1 글자가 있는 줄에서 $2 단추(▲ ▼ 등)를 누른다
  unblock
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb exec-out cat /sdcard/ui.xml > "$OUT/ui.xml" 2>/dev/null
  xy=$(python3 - "$OUT/ui.xml" "$1" "$2" <<'PY'
import re, sys
x = open(sys.argv[1], encoding="utf-8", errors="replace").read()
nodes = [(m.group(1).strip(), int(m.group(2)), int(m.group(3)), int(m.group(4)), int(m.group(5)))
         for m in re.finditer(r'<node[^>]*?text="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', x)]
label, button = sys.argv[2], sys.argv[3]
rows = [n for n in nodes if n[0] == button]
for t, x1, y1, x2, y2 in nodes:
    if t != label:
        continue
    cy = (y1 + y2) // 2
    # 같은 줄(세로로 겹치는)에서 가장 가까운 단추
    best = sorted(rows, key=lambda r: abs((r[2] + r[4]) // 2 - cy))
    if best and abs((best[0][2] + best[0][4]) // 2 - cy) < 90:
        b = best[0]
        print((b[1] + b[3]) // 2, (b[2] + b[4]) // 2)
    break
PY
)
  if [ -n "$xy" ]; then adb shell input tap $xy; echo "tap '$2' in row '$1' at $xy"; else echo "tap '$2' in row '$1': not found"; fi
}


seed() {   # 저장 파일 써넣기 ($1 = 파일, 없으면 빈 상태)
  adb shell am force-stop $PKG
  adb shell "run-as $PKG rm -f files/data.json"
  [ -n "${1:-}" ] && adb shell "run-as $PKG sh -c 'mkdir -p files && cat > files/data.json'" < "$1"
  true
}
start() { adb shell am start -W -n $PKG/.$1 "${@:2}" >/dev/null; }
swipe_up() { adb shell input swipe 540 1900 540 ${1:-700} 400; }
has() { grep -q "text=\"$1\"" "$OUT/ui.xml"; }

TODAY=$(python3 -c "
import sys, time
z = sys.argv[1].strip() or '+0000'
off = (int(z[1:3]) * 3600 + int(z[3:5]) * 60) * (-1 if z[0] == '-' else 1)
print((int(time.time()) + off) // 86400)" "$(adb shell date +%z | tr -d '\r')")
python3 - "$TODAY" > "$OUT/seed.json" <<'PY'
import json, sys
t = int(sys.argv[1])
src = [("apple","사과"),("beautiful","아름다운"),("library","도서관"),("run","달리다, 뛰다"),("vegetable","채소"),
       ("remember","기억하다"),("get up","일어나다"),("weather","날씨"),("different","다른"),("important","중요한")]
words = []
for i, (en, ko) in enumerate(src):
    w = {"en": en, "ko": ko, "seen": 1 if i < 7 else 0, "box": [0,1,2,3,4,0,1,0,0,0][i], "due": t - 1 if i < 7 else 0,
         "right": [1,2,3,4,6,0,1,0,0,0][i], "wrong": [3,1,0,1,0,2,0,0,0,0][i]}
    words.append(w)
d = {"intro": 0, "stage": 2, "mSolved": 7, "mPoints": 200, "hero": "지우", "points": 260, "solved": 7, "streak": 3, "lastDay": t - 1, "perSession": 5, "timer": 0, "sound": 1, "nextId": 3,
     "history": [{"d": t - 3, "a": 10, "r": 6}, {"d": t - 2, "a": 12, "r": 9}, {"d": t - 1, "a": 10, "r": 9}],
     "decks": [{"id": 1, "name": "Day 1 (3단원)", "words": words},
               {"id": 2, "name": "학원 단어 11월 4주", "words": [{"en": "bridge", "ko": "다리"}, {"en": "quiet", "ko": "조용한"}, {"en": "borrow", "ko": ""}]}]}
print(json.dumps(d, ensure_ascii=False))
PY

# ---- 1) 처음 깔았을 때
seed
start HomeActivity;      shot 00_intro_scene0 1.5
shot 00_intro_scene0b 1;  shot 00_intro_scene1 2.5;  shot 00_intro_scene2 2.5
adb shell input tap 540 900;  shot 00_intro_scene3 2.5        # 무대를 누르면 다음 장면
tap "다음  ▶";            shot 00_intro_scene4 1.5
tap "쪼코 찾으러 가기!";  shot 01_first_run 3
adb shell input text "Jiu"; tap "정했어요";  shot 02_home_empty 2
start DecksActivity;     shot 03_decks_empty
tap "직접 입력하거나 붙여넣기";            shot 04_type_in 2
adb shell input keyevent KEYCODE_BACK; adb shell input keyevent KEYCODE_BACK

# ---- 2) 단어장과 공부 기록이 있을 때
seed "$OUT/seed.json"
start HomeActivity;      shot 10_home
swipe_up;                shot 11_home_bottom 1.5
start DecksActivity;     shot 12_decks
start DeckActivity --el deck 1;   shot 13_deck
swipe_up;                shot 14_deck_bottom 1.5
start EditActivity --el deck 1;   shot 15_edit
adb shell input keyevent KEYCODE_BACK
start RecordActivity;    shot 16_record
swipe_up;                shot 17_record_bottom 1.5
start SettingsActivity;  shot 18_settings
swipe_up;                shot 19_settings_bottom 1.5
adb shell input keyevent KEYCODE_BACK

# ---- 3) 공부 한 판: 문제 종류가 섞여 나오므로 화면마다 찍으면서 끝까지 간다
adb shell pm grant $PKG android.permission.RECORD_AUDIO
start StudyActivity
n=0
for step in $(seq 1 26); do
  n=$((n+1)); shot "20_study_$(printf %02d $n)" 2
  if has "한 판 더!"; then break; fi
  if has "기억했어요!"; then tap "기억했어요!"
  elif has "다음 문제"; then tap "다음 문제"
  elif has "사건 마무리하기"; then tap "사건 마무리하기"
  elif has "보기에서 고르기 (단서가 절반)" && [ $((step % 3)) -ne 0 ]; then
    tap "보기에서 고르기 (단서가 절반)"; n=$((n+1)); shot "20_study_$(printf %02d $n)" 1.5
    # 보기 가운데 정답을 누른다 (시험 단어의 뜻/영어를 알고 있으므로)
    python3 - "$OUT/ui.xml" > "$OUT/pick.txt" <<'PY'
import re, sys
pairs = {"apple":"사과","beautiful":"아름다운","library":"도서관","run":"달리다, 뛰다","vegetable":"채소","remember":"기억하다","get up":"일어나다","weather":"날씨","different":"다른","important":"중요한","bridge":"다리","quiet":"조용한"}
x = open(sys.argv[1], encoding="utf-8", errors="replace").read()
texts = [m.group(1) for m in re.finditer(r'text="([^"]*)"', x) if m.group(1).strip()]
ans = ""
for t in texts:
    if t in pairs: ans = pairs[t]                         # 영어를 보여 주고 뜻을 묻는 문제
for k, v in pairs.items():
    if v in texts and k in texts and not ans: ans = k     # 뜻을 보여 주고 영어를 묻는 문제
print(ans)
PY
    tap "$(cat "$OUT/pick.txt")"
  elif has "쪼코에게 힌트 받기" && [ $((step % 2)) -eq 0 ]; then
    tap "쪼코에게 힌트 받기"; n=$((n+1)); shot "20_study_$(printf %02d $n)" 1.5; tap "모르겠어요"
  else tap "모르겠어요"
  fi
done
adb shell input keyevent KEYCODE_BACK

# ---- 4) 짝 맞추기
start MatchActivity --el deck 1;  shot 30_match
tap "apple"; tap "사과";           shot 31_match_one_right 1.5
tap "run"; tap "채소";             shot 32_match_wrong 0.3
adb shell input keyevent KEYCODE_BACK

# ---- 5) 미해결 사건, 휴대폰 글자를 크게 해 둔 경우
start StudyActivity --ez unsolved true;  shot 40_unsolved 2
adb shell input keyevent KEYCODE_BACK
adb shell am force-stop $PKG
adb shell settings put system font_scale 1.3
start HomeActivity;      shot 41_home_sysfont_13 3
start StudyActivity;     shot 42_study_sysfont_13 3
adb shell settings put system font_scale 1.0
adb shell am force-stop $PKG

# ---- 5b) 인트로 넘기기, 속임 장면 (요구를 다 들어준 상태로 바꿔서 연다)
sed -e 's/"intro": 0/"intro": 1/' "$OUT/seed.json" > "$OUT/seed_intro.json"
seed "$OUT/seed_intro.json"
start HomeActivity;      shot 60_intro_again 2
adb shell input tap 540 900; sleep 0.5; adb shell input tap 540 900; sleep 0.5; adb shell input tap 540 900;  shot 60b_intro_note 3
tap "넘기기  ▶▶";        shot 61_after_skip 2
sed -e 's/"mPoints": 200/"mPoints": 150/' "$OUT/seed.json" > "$OUT/seed_trick.json"
seed "$OUT/seed_trick.json"
start HomeActivity;      shot 62_trick_scene0 2.5
tap "다음  ▶";           shot 63_trick_scene1 2
tap "다음  ▶";           shot 64_trick_scene2 2.5
tap "좋아, 이번엔 꼭!";  shot 65_home_next_demand 2.5
rm -f "$OUT/seed_intro.json" "$OUT/seed_trick.json"

# ---- 6) 글자 인식: 시험용 단어장 그림을 실제 인식에 넣어 본다
adb shell mkdir -p /sdcard/Android/data/$PKG/files
adb push tools/ocr_test.png /sdcard/Android/data/$PKG/files/test.png
start OcrTestActivity;   shot 50_ocr 20
adb push tools/ocr_test2.jpg /sdcard/Android/data/$PKG/files/test2.jpg
adb shell am force-stop $PKG
start OcrTestActivity --es file test2.jpg;   shot 51_ocr_textbook 20
swipe_up;                shot 52_ocr_textbook_raw 1.5

# ---- 7) 새 단어장을 저장하면 바로 그 단어장 화면으로, 거기서 문제 풀이로
seed "$OUT/seed.json"
start DecksActivity;     sleep 2
tap "직접 입력하거나 붙여넣기"; sleep 1.5
adb shell input text "river"; sleep 0.5
tap "다음";              shot 70_new_deck_edit 2
tap "이대로 저장";       shot 71_after_save 2.5
tap "이 단어장으로 도전장 받기";  shot 72_study_from_new_deck 2.5

# ---- 기록: 앱이 죽은 흔적
adb logcat -d > "$OUT/logcat_all.txt" 2>/dev/null
grep -E "AndroidRuntime|FATAL|kr\.word|MLKit|mlkit" "$OUT/logcat_all.txt" | tail -300 > "$OUT/logcat.txt"
rm -f "$OUT/logcat_all.txt" "$OUT/ui.xml" "$OUT/pick.txt"

ls -la $OUT
exit 0
