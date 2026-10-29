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
- 📖 **Multiple libraries** — separate shelves for home, office, college…
- 🧠 **Smart duplicate check** — know before you buy a second copy.
- ✍️ **Never lose work** — unsaved entries become drafts automatically, and deletes can be undone.
- ☁️ **Automatic backups** — to a folder you choose (e.g. Google Drive).
- 🎨 **10 themes** — light and dark.

### 🧰 What it does
| Feature | What you get |
|---|---|
| 📖 **Books** | Title, author, edition, year, ISBN (auto-formatted), category. Tap ▾ on any card for details. |
| 🗂️ **Organise** | Categories you can drag into your own order, plus Favourites, Drafts and Duplicates. |
| 🔎 **Find** | Search by title, author or ISBN and sort four ways — on every list. |
| ✅ **Bulk actions** | Long-press, pick several, then Favourite / Draft / Move / Delete. |
| 🏠 **Many libraries** | Separate shelves with separate data. Optionally pin each one to a real-world place. |
| 📍 **Location nudge** | Walk into another saved place and the app offers to switch library (only while it's open). |
| 🔁 **Export & import** | JSON or CSV — you choose what's included. Imports get a preview and duplicate check. |
| ☁️ **Backups** | Manual or automatic, into a folder you pick (Google Drive works). Keep the last N, then browse and restore from inside the app. |
| 🎨 **Looks** | 10 themes, optional numbered lists, light animations. |

### 💡 Quick tips
- **Details / options / select:** ▾ opens a card, ⋮ shows options, long-press starts selecting.
- **Drafts save themselves:** leave the add dialog without saving and your work becomes a draft. Press **Discard** if you really want it gone.
- **Duplicate warning:** *Save Anyway* parks the book under **Duplicates** (in the **Categories** screen) until you edit it into something unique.
- **Reorder categories:** drag the chips on the home screen (All and Favourites stay put).
- **Oops-proof deletes:** tap **Undo** within 6 seconds.

### 🔐 Permissions
| Permission | Needed? | Why |
|---|---|---|
| Location (precise & approximate) | Optional | Only to save a library's location and suggest switching. It never leaves your phone. Say no and everything else still works. |
| A backup folder (Android's folder picker) | Optional | Lets the app write backups into **one folder you choose**. No storage permission needed. |
| Internet | ❌ Not requested | The app can't go online. |
| Camera, contacts, microphone, storage | ❌ Not requested | — |

*Android may list a few standard background-work entries for the backup scheduler; none of them give internet access.*

### 📱 What your phone needs
- Android 8.0 (Oreo) or newer
- Tiny footprint — text only, no images
- Google Drive app signed in (only for Drive backups; any storage provider that offers folders works)
- Location switched on (only for the location features)
- To install an APK: allow "Install unknown apps" for your browser or file manager
- For on-time automatic backups: phone Settings > Apps > Library > Battery > **Unrestricted**

### ⚠️ Good to know
1. **Your data lives only on your phone.** There is no sync, and uninstalling the app deletes it. Back up first, and move data between phones with Export/Import or Backup/Restore.
2. **Automatic backups are "roughly on time".** Android may delay them (battery saver; some brands are strict). Unrestricted battery helps.
3. **Old backups are deleted automatically** (default: keep the last 10), but only files this app created and only after a successful backup. Pick **Keep all backups** to turn it off.
4. **Backups are plain JSON.** Anyone with access to that folder can read them, so choose a private folder.
5. **Restore replaces the current library entirely** (with a confirmation, and all-or-nothing). Restoring "names & locations" does not bring back books.
6. **Drafts and duplicates are hidden from the main list on purpose.** Find them in the **Categories** screen (Drafts shows a count badge). Duplicate rule: same title (case and extra spaces ignored) and same edition, or one edition left blank.
7. **Auto-draft protects new entries and existing drafts only.** Editing a published book and backing out changes nothing.
8. **Location works only while the app is open** (checked about every 45 seconds, default radius 150 m). Accuracy depends on your phone's GPS.
9. **Deleting a whole library erases its books for good** (no undo). The interface is English-only for now.

### 🚀 Get it
- Download the latest APK from [Releases](https://github.com/Abdul-Mueed-S/Library/releases), open it and allow the install.
- Or build it yourself: open the project in Android Studio, sync, run.

<details>
<summary><b>For developers</b></summary>

- Package `com.evu.library` · minSdk 26 · targetSdk 34 · compileSdk 35
- Kotlin 1.9.24 · KSP 1.9.24-1.0.20 · Room 2.6.1 (DB version 5) · WorkManager 2.9.1 · Material Components · View Binding
- One Room database file per library; library names and locations live in SharedPreferences.
- Open the project with **JDK 21** (Gradle 8.9 does not support JDK 25).
- Versions are pinned together — don't bump AGP, Kotlin, KSP or Room casually.
- Every schema change needs a Room `Migration` and a committed `schemas/` file. Update the full-backup JSON in the same change.
- Backup files: `library_backup_<library>_<yyyy-MM-dd_HHmm>.json` and `library_vaults_metadata_<yyyy-MM-dd_HHmm>.json`.

</details>

---

<a id="urdu"></a>

<div dir="rtl" lang="ur">

## اردو

**اپنی کتابوں کا ذاتی ذخیرہ — مکمل آف لائن۔ نہ انٹرنیٹ، نہ اکاؤنٹ، نہ اشتہارات۔**

### ✨ خاص باتیں
- 🔒 **100٪ آف لائن اور نجی** — ایپ کے پاس انٹرنیٹ کی اجازت ہی نہیں۔
- 📖 **ایک سے زیادہ لائبریریاں** — گھر، دفتر، کالج، سب کے لیے الگ الگ شیلف۔
- 🧠 **ذہین ڈپلیکیٹ جانچ** — دوسری کاپی خریدنے سے پہلے ہی پتا چل جائے۔
- ✍️ **کام ضائع نہیں ہوتا** — ادھورا اندراج خود بخود ڈرافٹ بن جاتا ہے، اور حذف کیا ہوا واپس لایا جا سکتا ہے۔
- ☁️ **خودکار بیک اپ** — آپ کی چنی ہوئی فولڈر میں (مثلاً Google Drive)۔
- 🎨 **10 تھیمز** — ہلکے بھی، گہرے بھی۔

### 🧰 کیا کچھ کر سکتی ہے
| خصوصیت | کیا ملتا ہے |
|---|---|
| 📖 **کتابیں** | عنوان، مصنف، ایڈیشن، سال، ISBN (خود بخود فارمیٹ)، زمرہ۔ کسی بھی کارڈ پر ▾ دبا کر تفصیل دیکھیں۔ |
| 🗂️ **ترتیب** | اپنی مرضی کی ترتیب میں ڈریگ ہونے والے زمرے، اور Favourites، Drafts، Duplicates۔ |
| 🔎 **تلاش** | عنوان، مصنف یا ISBN سے تلاش، اور چار طرح کی ترتیب — ہر فہرست میں۔ |
| ✅ **اجتماعی کام** | دیر تک دبائیں، کئی کتابیں چنیں، پھر Favourite / Draft / Move / Delete۔ |
| 🏠 **کئی لائبریریاں** | الگ شیلف، الگ ڈیٹا۔ چاہیں تو ہر ایک کو حقیقی مقام سے جوڑ دیں۔ |
| 📍 **مقام کا مشورہ** | کسی اور محفوظ مقام پر پہنچیں تو ایپ لائبریری بدلنے کی پیشکش کرتی ہے (صرف ایپ کھلی ہو تب)۔ |
| 🔁 **ایکسپورٹ اور امپورٹ** | JSON یا CSV — کیا شامل ہو، آپ طے کریں۔ امپورٹ سے پہلے پیش منظر اور ڈپلیکیٹ جانچ۔ |
| ☁️ **بیک اپ** | دستی یا خودکار، آپ کی چنی ہوئی فولڈر میں (Google Drive بھی چلتا ہے)۔ آخری N بیک اپ رکھیں، پھر ایپ کے اندر سے دیکھیں اور ریسٹور کریں۔ |
| 🎨 **انداز** | 10 تھیمز، نمبر والی فہرست کا آپشن، ہلکی اینیمیشنز۔ |

### 💡 فوری مشورے
- **تفصیل / آپشنز / انتخاب:** ▾ سے کارڈ کھلتا ہے، ⋮ سے آپشنز، اور دیر تک دبانے سے انتخاب شروع ہوتا ہے۔
- **ڈرافٹ خود محفوظ ہو جاتے ہیں:** اندراج کا ڈائیلاگ بغیر محفوظ کیے چھوڑ دیں تو کام ڈرافٹ بن جاتا ہے۔ مٹانا ہو تو **Discard** دبائیں۔
- **ڈپلیکیٹ کی وارننگ:** *Save Anyway* دبانے پر کتاب **Categories** اسکرین کے **Duplicates** میں رکھی جاتی ہے، جب تک آپ اسے منفرد نہ بنا دیں۔
- **زمروں کی ترتیب بدلیں:** ہوم اسکرین پر چپس کو ڈریگ کریں (All اور Favourites اپنی جگہ رہتے ہیں)۔
- **حذف کرنے میں بھی احتیاط:** 6 سیکنڈ کے اندر **Undo** دبائیں۔

### 🔐 اجازتیں
| اجازت | ضروری؟ | کس لیے |
|---|---|---|
| مقام (درست اور تقریبی) | اختیاری | صرف لائبریری کا مقام محفوظ کرنے اور لائبریری بدلنے کا مشورہ دینے کے لیے۔ یہ آپ کے فون سے باہر نہیں جاتا۔ نہ دیں تو باقی سب کچھ چلتا رہتا ہے۔ |
| بیک اپ فولڈر (اینڈرائیڈ کے فولڈر پکر سے) | اختیاری | ایپ کو **صرف آپ کی چنی ہوئی ایک فولڈر** میں بیک اپ لکھنے دیتی ہے۔ اسٹوریج کی اجازت درکار نہیں۔ |
| انٹرنیٹ | ❌ نہیں مانگی جاتی | ایپ خود آن لائن جا ہی نہیں سکتی۔ |
| کیمرا، رابطے، مائیکروفون، اسٹوریج | ❌ نہیں مانگی جاتی | — |

*اینڈرائیڈ بیک اپ شیڈولر کے لیے کچھ معیاری بیک گراؤنڈ اندراجات دکھا سکتا ہے؛ ان میں سے کوئی بھی انٹرنیٹ تک رسائی نہیں دیتی۔*

### 📱 آپ کے فون کو کیا چاہیے
- Android 8.0 (Oreo) یا اس سے نیا ورژن
- بہت ہلکی ایپ — صرف متن، کوئی تصویر نہیں
- Google Drive ایپ میں سائن اِن (صرف Drive بیک اپ کے لیے؛ فولڈر دینے والا کوئی بھی اسٹوریج پرووائیڈر چلے گا)
- لوکیشن آن (صرف مقام والی خصوصیات کے لیے)
- APK انسٹال کرنے کے لیے براؤزر یا فائل مینیجر کو **Install unknown apps** کی اجازت دیں
- وقت پر خودکار بیک اپ کے لیے: فون کی Settings میں Apps > Library > Battery کو **Unrestricted** پر رکھیں

### ⚠️ جاننے کی باتیں
1. **آپ کا ڈیٹا صرف آپ کے فون میں ہے۔** سنک نہیں ہوتا، اور ایپ ان انسٹال کرنے سے ڈیٹا مٹ جاتا ہے۔ پہلے بیک اپ لیں، اور ایک فون سے دوسرے میں منتقلی Export/Import یا Backup/Restore سے کریں۔
2. **خودکار بیک اپ "تقریباً" وقت پر ہوتے ہیں۔** اینڈرائیڈ انہیں دیر سے چلا سکتا ہے (بیٹری سیور، کچھ برانڈز زیادہ سختی کرتے ہیں)۔ بیٹری کو Unrestricted رکھنے سے مدد ملتی ہے۔
3. **پرانے بیک اپ خود حذف ہوتے ہیں** (ڈیفالٹ: آخری 10 رکھے جاتے ہیں) — صرف وہ فائلیں جو اسی ایپ نے بنائیں، اور صرف کامیاب بیک اپ کے بعد۔ بند کرنے کے لیے **Keep all backups** چنیں۔
4. **بیک اپ سادہ JSON فائلیں ہیں۔** اس فولڈر تک رسائی رکھنے والا کوئی بھی انہیں پڑھ سکتا ہے، اس لیے نجی فولڈر چنیں۔
5. **ریسٹور موجودہ لائبریری کو مکمل بدل دیتا ہے** (تصدیق کے بعد، اور یا سب کچھ ہوتا ہے یا کچھ نہیں)۔ "Names & Locations" ریسٹور کرنے سے کتابیں واپس نہیں آتیں۔
6. **ڈرافٹ اور ڈپلیکیٹ مرکزی فہرست سے جان بوجھ کر چھپائے جاتے ہیں۔** انہیں **Categories** اسکرین میں دیکھیں (Drafts پر گنتی کا بیج ہوتا ہے)۔ ڈپلیکیٹ کا اصول: ایک ہی عنوان (بڑے/چھوٹے حروف اور اضافی خالی جگہ نظر انداز) اور ایک ہی ایڈیشن — یا کسی ایک کا ایڈیشن خالی۔
7. **خودکار ڈرافٹ صرف نئے اندراج اور پہلے سے موجود ڈرافٹ کو بچاتا ہے۔** شائع شدہ کتاب میں ترمیم کر کے بغیر محفوظ کیے نکل جائیں تو کچھ نہیں بدلتا۔
8. **مقام صرف ایپ کھلی ہو تب کام کرتا ہے** (تقریباً ہر 45 سیکنڈ میں جانچ، ڈیفالٹ رداس 150 میٹر)۔ درستگی آپ کے فون کے GPS پر منحصر ہے۔
9. **پوری لائبریری حذف کرنے سے اس کی کتابیں ہمیشہ کے لیے مٹ جاتی ہیں** (Undo نہیں)۔ ایپ فی الحال صرف انگریزی انٹرفیس میں ہے۔

### 🚀 حاصل کریں
- [Releases](https://github.com/Abdul-Mueed-S/Library/releases) سے تازہ APK ڈاؤن لوڈ کریں، کھولیں اور انسٹال کی اجازت دیں۔
- یا خود بنائیں: Android Studio میں پروجیکٹ کھولیں، Sync کریں، اور چلائیں۔

*(ڈویلپرز کے لیے تفصیل انگریزی سیکشن میں ہے۔)*

</div>

---

<a id="hinglish"></a>

## Hinglish

**Apni kitabon ka personal collection — bilkul offline. Na internet, na account, na ads.**

### ✨ Kyun pasand aayegi
- 🔒 **100% offline aur private** — app ke paas internet permission hi nahi hai.
- 📖 **Ek se zyada libraries** — ghar, office, college, sabke liye alag shelf.
- 🧠 **Smart duplicate check** — doosri copy kharidne se pehle hi pata chal jaaye.
- ✍️ **Kaam waste nahi hota** — adhura entry khud draft ban jaati hai, aur delete kiya hua wapas aa sakta hai.
- ☁️ **Automatic backup** — aapke chune hue folder mein (jaise Google Drive).
- 🎨 **10 themes** — light bhi, dark bhi.

### 🧰 Kya-kya karti hai
| Feature | Kya milta hai |
|---|---|
| 📖 **Kitabein** | Title, author, edition, saal, ISBN (auto-format), category. Kisi bhi card par ▾ dabao, poori detail dikhegi. |
| 🗂️ **Organise** | Categories ko drag karke apne order mein lagao, plus Favourites, Drafts aur Duplicates. |
| 🔎 **Dhundo** | Title, author ya ISBN se search, aur 4 tarah ki sorting — har list mein. |
| ✅ **Bulk actions** | Long-press karo, kai kitabein chuno, phir Favourite / Draft / Move / Delete. |
| 🏠 **Kai libraries** | Alag shelf, alag data. Chaho to har ek ko asli jagah se jod do. |
| 📍 **Location nudge** | Kisi doosri saved jagah pahuncho to app library badalne ko poochti hai (sirf jab app khuli ho). |
| 🔁 **Export & import** | JSON ya CSV — kya shamil ho, aap chuno. Import se pehle preview aur duplicate check. |
| ☁️ **Backups** | Manual ya automatic, aapke chune hue folder mein (Google Drive chalta hai). Last N backups rakho, phir app ke andar se dekho aur restore karo. |
| 🎨 **Look** | 10 themes, numbered list ka option, halki animations. |

### 💡 Quick tips
- **Details / options / select:** ▾ se card khulta hai, ⋮ se options, aur long-press se selection shuru hota hai.
- **Drafts khud save hote hain:** add dialog bina save kiye chhod do to kaam draft ban jaata hai. Sach mein hatana ho to **Discard** dabao.
- **Duplicate warning:** *Save Anyway* dabane par kitab **Categories** screen ke **Duplicates** mein rehti hai, jab tak aap use unique na bana do.
- **Categories ka order badlo:** home screen par chips ko drag karo (All aur Favourites apni jagah rehte hain).
- **Delete bhi safe:** 6 second ke andar **Undo** dabao.

### 🔐 Permissions
| Permission | Zaroori? | Kyun |
|---|---|---|
| Location (precise & approximate) | Optional | Sirf library ki location save karne aur library badalne ka suggestion dene ke liye. Phone se bahar nahi jaati. Mana kar do to baaki sab chalta rahega. |
| Backup folder (Android ke folder picker se) | Optional | App ko **sirf aapke chune hue ek folder** mein backup likhne deta hai. Storage permission ki zaroorat nahi. |
| Internet | ❌ Nahi maangi jaati | App khud online ja hi nahi sakti. |
| Camera, contacts, microphone, storage | ❌ Nahi maangi jaati | — |

*Android backup scheduler ke liye kuch standard background-work entries dikha sakta hai; unme se koi bhi internet access nahi deti.*

### 📱 Phone ko kya chahiye
- Android 8.0 (Oreo) ya usse naya
- Bahut halki app — sirf text, koi images nahi
- Google Drive app mein sign-in (sirf Drive backup ke liye; folder dene wala koi bhi storage provider chalega)
- Location on (sirf location wale features ke liye)
- APK install karne ke liye browser ya file manager ko **Install unknown apps** ki permission do
- Time par automatic backup ke liye: phone Settings > Apps > Library > Battery > **Unrestricted**

### ⚠️ Dhyaan rakhne wali baatein
1. **Aapka data sirf aapke phone mein hai.** Sync nahi hota, aur app uninstall karne par data mit jaata hai. Pehle backup lo, aur ek phone se doosre mein data Export/Import ya Backup/Restore se le jao.
2. **Automatic backups "lagbhag" time par hote hain.** Android unhe late chala sakta hai (battery saver; kuch brands zyada strict hain). Battery Unrestricted rakhne se madad milti hai.
3. **Purane backups khud delete hote hain** (default: last 10 rakhe jaate hain) — sirf wahi files jo isi app ne banayi, aur sirf successful backup ke baad. Band karne ke liye **Keep all backups** chuno.
4. **Backups plain JSON files hain.** Us folder tak access wala koi bhi unhe padh sakta hai, isliye private folder chuno.
5. **Restore current library ko poori tarah replace karta hai** (confirmation ke baad, aur ya sab kuch hota hai ya kuch nahi). "Names & Locations" restore karne se kitabein wapas nahi aati.
6. **Drafts aur duplicates main list se jaan-boojhkar chhupaye jaate hain.** Unhe **Categories** screen mein dekho (Drafts par count badge dikhta hai). Duplicate ka rule: same title (capital/small aur extra space ignore) aur same edition — ya kisi ek ka edition khaali.
7. **Auto-draft sirf naye entries aur pehle se maujood drafts ko bachata hai.** Published kitab edit karke bina save kiye nikal jao to kuch nahi badalta.
8. **Location sirf tab kaam karti hai jab app khuli ho** (lagbhag har 45 second mein check, default radius 150 m). Accuracy aapke phone ke GPS par depend karti hai.
9. **Poori library delete karne se uski kitabein hamesha ke liye mit jaati hain** (Undo nahi). App abhi sirf English interface mein hai.

### 🚀 Kaise lein
- [Releases](https://github.com/Abdul-Mueed-S/Library/releases) se latest APK download karo, kholo aur install ki permission do.
- Ya khud banao: Android Studio mein project kholo, Sync karo, run karo.

*(Developers ke liye details English section mein hain.)*