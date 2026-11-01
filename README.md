<h1 align="center">📚 Library</h1>
<p align="center"><b>Your personal book collection — fully offline.</b><br>No internet. No accounts. No ads.</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white" alt="Android 8.0+">
  <img src="https://img.shields.io/badge/Works-100%25%20offline-blue" alt="Offline">
  <img src="https://img.shields.io/badge/Kotlin-Room%20%7C%20WorkManager-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin">
</p>

<p align="center">
  <a href="#english">English</a> · <a href="#urdu">اردو</a> · <a href="#hinglish">Hinglish</a>
</p>

---

<a id="english"></a>

## English

### ✨ Why you'll like it
- 🔒 **100% offline & private** — the app has no internet permission at all.
- 📖 **Many libraries** — separate shelves for home, office, college…
- 🧠 **Smart duplicate check** — know before you buy a second copy, or ask **"Do I own this?"** across every library.
- ✍️ **Never lose work** — unsaved entries become drafts, deletes go to Trash, and you can undo.
- 📸 **Snapshots & backups** — go back in time, or let the app back up to a folder for you.
- 🎨 **Make it yours** — 10 themes, plus separate colours for buttons, badges, cards, text and more.

### 🧰 What it does
| Feature | What you get |
|---|---|
| 📖 **Books** | Title, author, edition, year, ISBN, category, **number of copies** (badge shows ×3). Tap ▾ for details. |
| 🗂️ **Organise** | Drag-to-sort categories with **count badges**; Favourites, Drafts, Duplicates, Lent Out, Series, Trash. |
| 📚 **Series** | Pick a series from a dropdown (or add one), set a volume number and total — see what you **own and what's missing**. |
| 🔎 **Find** | Search by title, author or ISBN, four ways to sort — on every list. |
| ✅ **Bulk actions** | Long-press, pick several, then Favourite / Draft / Move / Delete. |
| 🤝 **Lending** | Lend to someone with an optional due date; **Lent / Overdue** badges; mark returned. |
| 🗑️ **Trash & undo** | Deleted books wait 30 days in Trash; **Undo** for 6 seconds. |
| 🏠 **Libraries** | Separate data per library; optionally pin each to a real place and get a switch suggestion (while the app is open). |
| 🔁 **Export & import** | JSON or CSV with a preview and duplicate check. |
| ☁️ **Backups** | Manual or automatic into a folder you pick (Google Drive works); keep the last N; browse & restore inside the app. |
| 📸 **Snapshots** | Point-in-time copies of **all libraries**, kept on the phone **and** in your backup folder. |
| 🖨️ **PDF catalogue** | A printable A4 list of your library, made on your phone. |
| 🎨 **Themes** | 10 base themes + per-element colours, corner style, text size, compact cards, animations switch, auto light/dark. |

### 💡 Quick tips
- **▾** opens a card, **⋮** shows options, **long-press** starts selecting.
- **Drafts save themselves:** leave the add dialog without saving and it becomes a draft. Press **Discard** to really drop it.
- **Series:** in the add/edit dialog choose a series (or **+ New Series**) and a **Volume #**. Manage series from **Categories → Series**.
- **Do I own this?** is in the menu — and on the app icon (long-press).
- **Themes:** open **Settings → Themes**. Each element (buttons, badges, background, cards, text…) has its own colour list; **Theme default** puts it back.

### 🔐 Permissions
| Permission | Needed? | Why |
|---|---|---|
| Location (precise & approximate) | Optional | Only to save a library's location and suggest switching. It never leaves your phone. |
| A backup folder (Android's folder picker) | Optional | Lets the app write backups and snapshot copies into **one folder you choose**. |
| Internet | ❌ Not requested | The app can't go online. |
| Camera, contacts, microphone, storage | ❌ Not requested | — |

### 📱 What your phone needs
- Android 8.0 (Oreo) or newer · tiny footprint (text only, no images)
- Google Drive app signed in — only for Drive backups
- Location on — only for the location features
- To install an APK: allow "Install unknown apps" for your browser or file manager
- For on-time automatic backups: Settings → Apps → Library → Battery → **Unrestricted**

### ⚠️ Good to know
1. **Your data lives only on your phone.** Uninstalling deletes it, and there is no sync. Back up first.
2. **Automatic backups are "roughly on time"** — Android may delay them. Unrestricted battery helps.
3. **Old backups are deleted automatically** (default: keep the last 10) — only files this app created, only after a successful backup. Choose **Keep all backups** to switch it off.
4. **Backups and snapshots are plain JSON.** Anyone with access to that folder can read them — pick a private folder.
5. **Restore replaces the library** (all-or-nothing per library). Snapshots are manual: take one first if you may want to go back.
6. **Trash** keeps deleted books 30 days, then removes them for good. Backups skip Trash; snapshots keep it.
7. **Drafts and duplicates are hidden from the main list on purpose** — find them under **Categories**.
8. **Export/Import (JSON/CSV) doesn't carry copies, series or lending.** Use Full Backup or Snapshots for everything.
9. **Colour choices are curated palettes**, not a free colour wheel. Pick a background and a text colour that contrast well.
10. **Location works only while the app is open** (checked about every 45 seconds, default radius 150 m).
11. **Deleting a whole library erases its books for good.** The interface is English-only for now.

### 🚀 Get it
- Download the latest APK from [Releases](https://github.com/Abdul-Mueed-S/Library/releases), open it and allow the install.
- Or build it: open the project in Android Studio, sync, run.

<details>
<summary><b>For developers</b></summary>

- Package `com.evu.library` · minSdk 26 · targetSdk 34
- Kotlin 1.9.24 · KSP 1.9.24-1.0.20 · Room 2.6.1 (**DB version 7**: `books`, `categories`, `series`) · WorkManager 2.9.1 · Material Components · View Binding
- One Room database per library; library names and locations live in SharedPreferences.
- Open the project with **JDK 21**. Versions are pinned together — don't bump AGP, Kotlin, KSP or Room casually.
- Every schema change needs a Room `Migration` and a committed `schemas/` file; update the full-backup JSON in the same change.
- Theming: base themes in `themes.xml`; per-element colour overlays in `theme_overlays.xml` (`Ovr.<Role>.<Colour>`), applied by `AppPrefs.applyTheme()` from `BaseActivity`. A palette needs both an XML overlay and a `ThemePalettes.kt` entry.
- Backup files: `library_backup_<library>_<date>.json`, `library_vaults_metadata_<date>.json`, `library_snapshot_<date>_<id>_<name>.json`.

</details>

---

<a id="urdu"></a>

<div dir="rtl" lang="ur">

## اردو

**اپنی کتابوں کا ذاتی ذخیرہ — مکمل آف لائن۔ نہ انٹرنیٹ، نہ اکاؤنٹ، نہ اشتہارات۔**

### ✨ خاص باتیں
- 🔒 **100٪ آف لائن اور نجی** — ایپ کے پاس انٹرنیٹ کی اجازت ہی نہیں۔
- 📖 **کئی لائبریریاں** — گھر، دفتر، کالج، سب کے لیے الگ شیلف۔
- 🧠 **ذہین ڈپلیکیٹ جانچ** — اور **"Do I own this?"** سے ہر لائبریری میں فوراً پتا کریں۔
- ✍️ **کام ضائع نہیں ہوتا** — ادھورا اندراج ڈرافٹ بن جاتا ہے، حذف کیا ہوا Trash میں جاتا ہے، اور Undo بھی ہے۔
- 📸 **اسنیپ شاٹس اور بیک اپ** — پچھلی حالت پر واپس جائیں، یا ایپ سے خودکار بیک اپ لیں۔
- 🎨 **اپنی پسند کا انداز** — 10 تھیمز، اور بٹن، بیج، کارڈ، متن وغیرہ کے الگ الگ رنگ۔

### 🧰 کیا کچھ کر سکتی ہے
| خصوصیت | کیا ملتا ہے |
|---|---|
| 📖 **کتابیں** | عنوان، مصنف، ایڈیشن، سال، ISBN، زمرہ، **کاپیوں کی تعداد** (بیج پر ×3)۔ تفصیل کے لیے ▾ دبائیں۔ |
| 🗂️ **ترتیب** | ڈریگ ہونے والے زمرے **گنتی کے بیج** کے ساتھ؛ Favourites، Drafts، Duplicates، Lent Out، Series، Trash۔ |
| 📚 **سیریز** | ڈراپ ڈاؤن سے سیریز چنیں (یا نئی بنائیں)، والیوم نمبر اور کل والیومز لکھیں — دیکھیں **کیا ہے اور کیا باقی ہے**۔ |
| 🔎 **تلاش** | عنوان، مصنف یا ISBN سے تلاش، اور چار طرح کی ترتیب — ہر فہرست میں۔ |
| ✅ **اجتماعی کام** | دیر تک دبائیں، کئی کتابیں چنیں، پھر Favourite / Draft / Move / Delete۔ |
| 🤝 **ادھار** | کسی کو ادھار دیں (واپسی کی تاریخ اختیاری)؛ **Lent / Overdue** بیج؛ واپسی کا نشان۔ |
| 🗑️ **ٹریش اور Undo** | حذف شدہ کتابیں 30 دن ٹریش میں رہتی ہیں؛ 6 سیکنڈ تک **Undo**۔ |
| 🏠 **لائبریریاں** | ہر لائبریری کا الگ ڈیٹا؛ چاہیں تو حقیقی مقام سے جوڑیں اور (ایپ کھلی ہو تو) بدلنے کا مشورہ پائیں۔ |
| 🔁 **ایکسپورٹ اور امپورٹ** | JSON یا CSV، پیش منظر اور ڈپلیکیٹ جانچ کے ساتھ۔ |
| ☁️ **بیک اپ** | دستی یا خودکار، آپ کی چنی ہوئی فولڈر میں (Google Drive بھی چلتا ہے)؛ آخری N رکھیں؛ ایپ کے اندر سے دیکھیں اور ریسٹور کریں۔ |
| 📸 **اسنیپ شاٹس** | **تمام لائبریریوں** کی ایک وقت کی نقل، فون میں **اور** بیک اپ فولڈر میں۔ |
| 🖨️ **PDF کیٹلاگ** | آپ کی لائبریری کی قابلِ پرنٹ A4 فہرست، فون ہی پر تیار۔ |
| 🎨 **تھیمز** | 10 بنیادی تھیمز + ہر عنصر کا الگ رنگ، کونوں کا انداز، متن کا سائز، کمپیکٹ کارڈ، اینیمیشن کا سوئچ، خودکار لائٹ/ڈارک۔ |

### 💡 فوری مشورے
- **▾** سے کارڈ کھلتا ہے، **⋮** سے آپشنز، اور **دیر تک دبانے** سے انتخاب شروع ہوتا ہے۔
- **ڈرافٹ خود محفوظ ہوتے ہیں:** ڈائیلاگ بغیر محفوظ کیے چھوڑ دیں تو کام ڈرافٹ بن جاتا ہے۔ مٹانا ہو تو **Discard** دبائیں۔
- **سیریز:** اندراج کے ڈائیلاگ میں سیریز (یا **+ New Series**) اور **Volume #** چنیں۔ سیریز کا انتظام **Categories ← Series** میں ہے۔
- **Do I own this?** مینو میں ہے — اور ایپ کے آئیکن کو دیر تک دبانے پر بھی۔
- **تھیمز:** **Settings ← Themes**۔ ہر عنصر (بٹن، بیج، پس منظر، کارڈ، متن…) کی اپنی رنگ فہرست ہے؛ **Theme default** سے اصل رنگ واپس۔

### 🔐 اجازتیں
| اجازت | ضروری؟ | کس لیے |
|---|---|---|
| مقام (درست اور تقریبی) | اختیاری | صرف لائبریری کا مقام محفوظ کرنے اور بدلنے کا مشورہ دینے کے لیے۔ یہ فون سے باہر نہیں جاتا۔ |
| بیک اپ فولڈر (اینڈرائیڈ کے فولڈر پکر سے) | اختیاری | ایپ کو **صرف آپ کی چنی ہوئی ایک فولڈر** میں بیک اپ اور اسنیپ شاٹ کی نقل لکھنے دیتی ہے۔ |
| انٹرنیٹ | ❌ نہیں مانگی جاتی | ایپ خود آن لائن جا ہی نہیں سکتی۔ |
| کیمرا، رابطے، مائیکروفون، اسٹوریج | ❌ نہیں مانگی جاتی | — |

### 📱 آپ کے فون کو کیا چاہیے
- Android 8.0 (Oreo) یا نیا · بہت ہلکی ایپ (صرف متن، تصویریں نہیں)
- Google Drive ایپ میں سائن اِن — صرف Drive بیک اپ کے لیے
- لوکیشن آن — صرف مقام والی خصوصیات کے لیے
- APK انسٹال کرنے کے لیے براؤزر یا فائل مینیجر کو **Install unknown apps** کی اجازت دیں
- وقت پر خودکار بیک اپ کے لیے: Settings ← Apps ← Library ← Battery ← **Unrestricted**

### ⚠️ جاننے کی باتیں
1. **آپ کا ڈیٹا صرف آپ کے فون میں ہے۔** ان انسٹال کرنے سے مٹ جاتا ہے، اور سنک نہیں ہوتا۔ پہلے بیک اپ لیں۔
2. **خودکار بیک اپ "تقریباً" وقت پر ہوتے ہیں** — اینڈرائیڈ انہیں دیر سے چلا سکتا ہے۔ بیٹری Unrestricted رکھیں۔
3. **پرانے بیک اپ خود حذف ہوتے ہیں** (ڈیفالٹ: آخری 10) — صرف اسی ایپ کی بنائی ہوئی فائلیں، اور صرف کامیاب بیک اپ کے بعد۔ بند کرنے کے لیے **Keep all backups** چنیں۔
4. **بیک اپ اور اسنیپ شاٹ سادہ JSON فائلیں ہیں۔** اس فولڈر تک رسائی والا انہیں پڑھ سکتا ہے — نجی فولڈر چنیں۔
5. **ریسٹور لائبریری کو بدل دیتا ہے** (ہر لائبریری کے لیے یا سب کچھ یا کچھ نہیں)۔ اسنیپ شاٹ دستی ہیں: واپسی کا امکان ہو تو پہلے ایک بنا لیں۔
6. **ٹریش** حذف شدہ کتابیں 30 دن رکھتا ہے، پھر ہمیشہ کے لیے ہٹا دیتا ہے۔ بیک اپ ٹریش چھوڑ دیتے ہیں؛ اسنیپ شاٹ اسے رکھتے ہیں۔
7. **ڈرافٹ اور ڈپلیکیٹ مرکزی فہرست سے جان بوجھ کر چھپائے جاتے ہیں** — انہیں **Categories** میں دیکھیں۔
8. **ایکسپورٹ/امپورٹ (JSON/CSV) میں کاپیاں، سیریز اور ادھار شامل نہیں۔** سب کچھ کے لیے Full Backup یا اسنیپ شاٹ استعمال کریں۔
9. **رنگوں کا انتخاب چنی ہوئی فہرستوں سے ہوتا ہے**، آزاد کلر وہیل سے نہیں۔ ایسا پس منظر اور متن چنیں جو آپس میں واضح نظر آئیں۔
10. **مقام صرف ایپ کھلی ہو تب کام کرتا ہے** (تقریباً ہر 45 سیکنڈ میں جانچ، ڈیفالٹ رداس 150 میٹر)۔
11. **پوری لائبریری حذف کرنے سے اس کی کتابیں ہمیشہ کے لیے مٹ جاتی ہیں۔** ایپ فی الحال صرف انگریزی انٹرفیس میں ہے۔

### 🚀 حاصل کریں
- [Releases](https://github.com/Abdul-Mueed-S/Library/releases) سے تازہ APK ڈاؤن لوڈ کریں، کھولیں اور انسٹال کی اجازت دیں۔
- یا خود بنائیں: Android Studio میں پروجیکٹ کھولیں، Sync کریں، چلائیں۔

*(ڈویلپرز کے لیے تفصیل انگریزی سیکشن میں ہے۔)*

</div>

---

<a id="hinglish"></a>

## Hinglish

**Apni kitabon ka personal collection — bilkul offline. Na internet, na account, na ads.**

### ✨ Kyun pasand aayegi
- 🔒 **100% offline aur private** — app ke paas internet permission hi nahi hai.
- 📖 **Kai libraries** — ghar, office, college, sabke liye alag shelf.
- 🧠 **Smart duplicate check** — aur **"Do I own this?"** se har library mein turant check karo.
- ✍️ **Kaam waste nahi hota** — adhura entry draft ban jaati hai, delete kiya hua Trash mein jaata hai, aur Undo bhi hai.
- 📸 **Snapshots aur backup** — purani halat par wapas jao, ya app se automatic backup lo.
- 🎨 **Apna style** — 10 themes, aur buttons, badges, cards, text ke alag-alag colours.

### 🧰 Kya-kya karti hai
| Feature | Kya milta hai |
|---|---|
| 📖 **Kitabein** | Title, author, edition, saal, ISBN, category, **copies ki ginti** (badge par ×3). Detail ke liye ▾ dabao. |
| 🗂️ **Organise** | Drag hone wali categories **count badge** ke saath; Favourites, Drafts, Duplicates, Lent Out, Series, Trash. |
| 📚 **Series** | Dropdown se series chuno (ya nayi banao), volume number aur total likho — dekho **kya hai aur kya missing hai**. |
| 🔎 **Dhundo** | Title, author ya ISBN se search, aur 4 tarah ki sorting — har list mein. |
| ✅ **Bulk actions** | Long-press karo, kai kitabein chuno, phir Favourite / Draft / Move / Delete. |
| 🤝 **Udhaar (Lending)** | Kisi ko udhaar do (due date optional); **Lent / Overdue** badge; wapas aane par mark karo. |
| 🗑️ **Trash aur Undo** | Delete ki hui kitabein 30 din Trash mein rehti hain; 6 second tak **Undo**. |
| 🏠 **Libraries** | Har library ka alag data; chaho to asli jagah se jodo aur (app khuli ho tab) switch ka suggestion pao. |
| 🔁 **Export & import** | JSON ya CSV, preview aur duplicate check ke saath. |
| ☁️ **Backups** | Manual ya automatic, aapke chune hue folder mein (Google Drive chalta hai); last N rakho; app ke andar se dekho aur restore karo. |
| 📸 **Snapshots** | **Saari libraries** ki ek waqt ki copy, phone mein **aur** backup folder mein. |
| 🖨️ **PDF catalogue** | Aapki library ki printable A4 list, phone par hi ban jaati hai. |
| 🎨 **Themes** | 10 base themes + har element ka alag colour, corner style, text size, compact cards, animations switch, auto light/dark. |

### 💡 Quick tips
- **▾** se card khulta hai, **⋮** se options, aur **long-press** se selection shuru hota hai.
- **Drafts khud save hote hain:** add dialog bina save kiye chhod do to kaam draft ban jaata hai. Sach mein hatana ho to **Discard** dabao.
- **Series:** add/edit dialog mein series (ya **+ New Series**) aur **Volume #** chuno. Series ka management **Categories → Series** mein hai.
- **Do I own this?** menu mein hai — aur app icon ko long-press karne par bhi.
- **Themes:** **Settings → Themes**. Har element (buttons, badges, background, cards, text…) ki apni colour list hai; **Theme default** se original wapas.

### 🔐 Permissions
| Permission | Zaroori? | Kyun |
|---|---|---|
| Location (precise & approximate) | Optional | Sirf library ki location save karne aur switch suggest karne ke liye. Phone se bahar nahi jaati. |
| Backup folder (Android ke folder picker se) | Optional | App ko **sirf aapke chune hue ek folder** mein backup aur snapshot ki copy likhne deta hai. |
| Internet | ❌ Nahi maangi jaati | App khud online ja hi nahi sakti. |
| Camera, contacts, microphone, storage | ❌ Nahi maangi jaati | — |

### 📱 Phone ko kya chahiye
- Android 8.0 (Oreo) ya naya · bahut halki app (sirf text, koi images nahi)
- Google Drive app mein sign-in — sirf Drive backup ke liye
- Location on — sirf location wale features ke liye
- APK install karne ke liye browser ya file manager ko **Install unknown apps** ki permission do
- Time par automatic backup ke liye: Settings → Apps → Library → Battery → **Unrestricted**

### ⚠️ Dhyaan rakhne wali baatein
1. **Aapka data sirf aapke phone mein hai.** Uninstall karne par mit jaata hai, aur sync nahi hota. Pehle backup lo.
2. **Automatic backups "lagbhag" time par hote hain** — Android unhe late chala sakta hai. Battery Unrestricted rakho.
3. **Purane backups khud delete hote hain** (default: last 10) — sirf isi app ki banayi files, aur sirf successful backup ke baad. Band karne ke liye **Keep all backups** chuno.
4. **Backups aur snapshots plain JSON files hain.** Us folder tak access wala koi bhi unhe padh sakta hai — private folder chuno.
5. **Restore library ko replace kar deta hai** (har library ke liye ya sab kuch ya kuch nahi). Snapshots manual hain: wapas jaane ka mann ho to pehle ek bana lo.
6. **Trash** delete ki hui kitabein 30 din rakhta hai, phir hamesha ke liye hata deta hai. Backups Trash skip karte hain; snapshots rakhte hain.
7. **Drafts aur duplicates main list se jaan-boojhkar chhupaye jaate hain** — unhe **Categories** mein dekho.
8. **Export/Import (JSON/CSV) mein copies, series aur lending nahi jaate.** Sab kuch ke liye Full Backup ya Snapshots use karo.
9. **Colour choices curated palettes se hote hain**, free colour wheel se nahi. Aisa background aur text chuno jo saaf dikhe.
10. **Location sirf tab kaam karti hai jab app khuli ho** (lagbhag har 45 second mein check, default radius 150 m).
11. **Poori library delete karne se uski kitabein hamesha ke liye mit jaati hain.** App abhi sirf English interface mein hai.

### 🚀 Kaise lein
- [Releases](https://github.com/Abdul-Mueed-S/Library/releases) se latest APK download karo, kholo aur install ki permission do.
- Ya khud banao: Android Studio mein project kholo, Sync karo, run karo.

*(Developers ke liye details English section mein hain.)*